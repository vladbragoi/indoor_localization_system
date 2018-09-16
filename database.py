import configparser
from cloudant.client import CouchDB, CouchDatabase
from cloudant.design_document import DesignDocument
from cloudant.query import Query
from cloudant.result import Result
from node import Node

_client = None
_loc_db_instance = None
_fing_db_instance = None


def initialize():
    global _client, _loc_db_name, _fing_db_name, _fingerprint_size
    config = configparser.ConfigParser()
    config.read('setup.ini')
    url = config['Database']['url']
    username = config['Database']['username']
    password = config['Database']['password']
    _loc_db_name = config['Database']['localization_db']
    _fing_db_name = config['Database']['fingerprinting_db']
    _fingerprint_size = int(config['Graph']['fingerprint_size'])
    _client = CouchDB(username, password, url=url, connect=True)


def _init_localization_db():
    global _loc_db_instance
    if _loc_db_instance is None:
        _loc_db_instance = CouchDatabase(_client, _loc_db_name)

        # Add filter function
        ddoc = DesignDocument(_loc_db_instance, '_design/online')
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
        _loc_db_instance.set_revision_limit(10)


def _init_fingerprinting_db():
    global _fing_db_instance
    if _fing_db_instance is None:
        _fing_db_instance = CouchDatabase(_client, _fing_db_name)


def close():
    _client.disconnect()


def changes(database, filter_function):
    return database.infinite_changes(
        feed='continuous',
        include_docs=True,
        filter=filter_function,
        since='now')


def get_nodes():
    _init_fingerprinting_db()
    query = Query(_fing_db_instance, selector={'_id': {'$gte': 0}}, fields=['_id', 'x', 'y', 'borders'])
    # return [doc for doc in query.result]  # return a list of dicts
    return [Node(id=doc['_id'], x=doc['x'], y=doc['y'], borders=doc['borders']) for doc in query.result]


def add_nodes_to(graph):
    _init_fingerprinting_db()

    # TODO: not using a matrix, but querying instead
    query = Query(_fing_db_instance, selector={'_id': {'$gte': 0}}, fields=['_id', 'x', 'y', 'borders'])
    for doc in query.result:
        x = int(doc['x']) / _fingerprint_size
        y = int(doc['y']) / _fingerprint_size
        node = Node(id=doc['_id'], x=doc['x'], y=doc['y'], borders=doc['borders'])
