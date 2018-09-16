import configparser
from cloudant.client import CouchDB, CouchDatabase
from cloudant.design_document import DesignDocument
from cloudant.query import Query
from node import Node


_client = None
_fingerprints_db_name = ""
_localization_db_name = ""
_localization_db_instance = None
_fingerprinting_db_instance = None


def initialize():
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
    global _fingerprinting_db_instance
    if _fingerprinting_db_instance is None:
        _fingerprinting_db_instance = CouchDatabase(_client, _fingerprints_db_name)
    return _fingerprinting_db_instance


def close():
    _client.disconnect()


def changes(database, filter_function):
    return database.infinite_changes(
        feed='continuous',
        include_docs=True,
        filter=filter_function,
        since='now')


def get_nodes():
    query = Query(fingerprinting_db(), selector={'_id': {'$gte': 0}}, fields=['_id', 'x', 'y', 'borders'])
    # return list(query.result)  # return a list of dicts
    return [Node(doc['_id'], x=doc['x'], y=doc['y'], borders=doc['borders']) for doc in query.result]
