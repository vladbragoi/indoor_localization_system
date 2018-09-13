import configparser
from cloudant.client import CouchDB, CouchDatabase
from cloudant.design_document import DesignDocument

_client = None
_loc_db_instance = None


def config():
    global _url, _username, _password, _loc_db_name, _fing_db_name
    config = configparser.ConfigParser()
    config.read('setup.ini')
    _url = config['Database']['url']
    _username = config['Database']['username']
    _password = config['Database']['password']
    _loc_db_name = config['Database']['localization_db']
    _fing_db_name = config['Database']['fingerprinting_db']


def connect():
    global _client, _loc_db_instance
    _client = CouchDB(_username, _password, url=_url, connect=True)
    #_client.connect()
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


def changes():
    return _loc_db_instance.infinite_changes(
        feed='continuous',
        include_docs=True,
        filter="online/dataDoc",
        since='now')


def get_all_nodes():
    client = CouchDB(_username, _password, url=_url)
    client.connect()
    db = CouchDatabase(client, _fing_db_name)
    print(db.keys(remote=True))

        # # TODO
        # print("Downloading...")
        # rows = db.view('_all_docs', include_docs=True)
        # rows = [row.doc for row in rows]
        #
        # for row in rows:
        #     row = cdb.Document(row)
        #     try:
        #         node = Node(id=row['_id'], x=row['X position'], y=row['Y position'],
        #                     borders=set_borders(row['Borders']))
        #         nodes[node.y][node.x] = node
        #     except KeyError:
        #         pass
    client.disconnect()

