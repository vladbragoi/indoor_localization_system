import configparser
import csv

from cloudant.client import CouchDB, CouchDatabase
from cloudant.design_document import DesignDocument
from cloudant.query import Query
from utils import inherit_docstring
from node import Node

_client = None
_fingerprints_db_name = ""
_localization_db_name = ""
_localization_db_instance = None
_fingerprinting_db_instance = None


def initialize():
    """Starts the connection with the server, which parameters
    are specified in the configuration file: setup.ini.
    """
    global _client, _fingerprints_db_name, _localization_db_name
    config = configparser.ConfigParser()
    config.read('setup.ini')

    url = config['Database']['url']
    username = config['Database']['username']
    password = config['Database']['password']

    _localization_db_name = config['Database']['localization_db']
    _fingerprints_db_name = config['Database']['fingerprinting_db']

    _client = CouchDB(username, password, url=url, connect=True)


def localization_db():
    """As a singleton, this function creates a localization db instance
    and returns it to the caller.
    :return _localization_db_instance: the instance
    """
    if _client is None:
        raise Exception("Should launch initialize method before.")

    global _localization_db_instance
    if _localization_db_instance is None:
        _localization_db_instance = CouchDatabase(_client, _localization_db_name)

        # Add filter function
        ddoc = DesignDocument(_localization_db_instance, '_design/online')
        if not ddoc.exists():
            # ignore documents that are deleted or having type != `data_doc`
            ddoc['filters'] = {
                'dataDoc': 'function(doc) { '
                           'if (doc._deleted) { return false; } '
                           'if (doc.type == \'data_doc\') { return true; }'
                           'return false; '
                           '}'
            }
            ddoc.save()
        _localization_db_instance.set_revision_limit(10)
    return _localization_db_instance


def fingerprinting_db():
    """As a singleton, this function creates a localization db instance
    and returns it to the caller.
    :return _fingerprinting_db_instance: the instance
    """
    if _client is None:
        raise Exception("Should launch initialize method before.")

    global _fingerprinting_db_instance
    if _fingerprinting_db_instance is None:
        _fingerprinting_db_instance = CouchDatabase(_client, _fingerprints_db_name)
    return _fingerprinting_db_instance


def close():
    """Closes connection with server."""
    _client.disconnect()


@inherit_docstring(CouchDatabase.infinite_changes)
def changes(database, filter_function):
    """
    :param database: the source database for changes
    :param filter_function: function for filtering documents in changes
    :return: an infinite_changes object
    """
    return database.infinite_changes(
        feed='continuous',
        include_docs=True,
        filter=filter_function,
        since='now')


def get_nodes(database=None):
    """Returns a list of nodes from the specified database.
    If None is passed, default fingerprinting db will be used.
    :param database: a database instance
    :return: a list of nodes
    """
    if database is None:
        database = fingerprinting_db()
    query = Query(database, selector={'_id': {'$gt': None}}, fields=['_id', 'x', 'y', 'borders'])
    # return list(query.result)  # return a list of dicts
    return [Node(doc['_id'], x=doc['x'], y=doc['y'], borders=doc['borders']) for doc in query.result]


def load_nodes_from_file(path):
    """Return a list of nodes, loaded from specified file.
    :param path: the path to the file
    :return: a list of nodes
    """
    with open(path, 'r') as csv_file:
        rows = csv.DictReader(csv_file)
        return [Node(row['id'], x=row['x'], y=row['y'], borders=row['borders']) for row in rows]


if __name__ == '__main__':
    initialize()
    print([str(x) for x in get_nodes()])
