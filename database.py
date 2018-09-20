import configparser
import csv

from cloudant.client import CouchDB, CouchDatabase
from cloudant.design_document import DesignDocument
from cloudant.query import Query

from data import Data
from utils import inherit_docstring
from node import Node

ID_KEY = 'id'
X_KEY = 'x'
Y_KEY = 'y'
BORDERS_KEY = 'borders'
DIRECTION_KEY = 'direction'
MEASURE_KEY = 'measure'
MV_X_KEY = 'mv[x]'
MV_Y_KEY = 'mv[y]'
MV_Z_KEY = 'mv[z]'
# TYPE_KEY = 'type'     # feature not used
CSV_FIELDS = [ID_KEY, X_KEY, Y_KEY, BORDERS_KEY, DIRECTION_KEY, MEASURE_KEY, MV_X_KEY, MV_Y_KEY, MV_Z_KEY] \
         + Data.get_ap5ghz() + Data.get_ap24ghz()

_client = None
fingerprints_db = ""
localization_db = ""


def initialize():
    """Starts the connection with the server, which parameters
    are specified in the configuration file: setup.ini.
    """
    global _client, fingerprints_db, localization_db
    config = configparser.ConfigParser()
    config.read('setup.ini')

    url = config['Database']['url']
    username = config['Database']['username']
    password = config['Database']['password']

    localization_db = config['Database']['localization_db']
    fingerprints_db = config['Database']['fingerprinting_db']

    _client = CouchDB(username, password, url=url, connect=True)


def get_localization_db():
    """This function creates a localization db instance and returns it to the caller.

    :return localization_db_instance: the instance
    """
    localization_db_instance = _start(localization_db)

    # Add filter function
    d_doc = DesignDocument(localization_db_instance, '_design/online')
    if not d_doc.exists():
        # ignore documents that are deleted or having type != `data_doc`
        d_doc['filters'] = {
            'dataDoc': 'function(doc) { '
                       'if (doc._deleted) { return false; } '
                       'if (doc.type == \'data_doc\') { return true; }'
                       'return false; '
                       '}'
        }
        d_doc.save()
    localization_db_instance.set_revision_limit(10)
    return localization_db_instance


def _start(db_name):
    """This function creates an instance of the database specified and returns it to the caller.

    :return: the CouchDatabase instance
    """
    if _client is None:
        raise Exception("Should launch initialize method before.")
    return CouchDatabase(_client, db_name)


def close():
    """Closes connection with server."""
    _client.disconnect()


@inherit_docstring(CouchDatabase.infinite_changes)
def changes(db_name, filter_function):
    """
    :param db_name: the source database name for changes
    :param filter_function: function for filtering documents in changes
    :return: an infinite_changes object

    .. seealso:: :ref:`CouchDatabase.infinite_changes()`
    """
    database = _start(db_name)
    return database.infinite_changes(
        feed='continuous',
        include_docs=True,
        filter=filter_function,
        since='now')


def get_nodes(db_name=None):
    """Returns a list of nodes from the specified database.
    If None is passed, default fingerprinting db will be used.

    :param db_name: the database name
    :return: a list of nodes
    """
    if db_name is None:
        db_name = fingerprints_db
    query = Query(_start(db_name),
                  selector={'_id': {'$gt': None}},
                  fields=['_id', 'x', 'y', 'borders'],
                  use_index='_all_docs')
    # return list(query.result)  # return a list of dicts
    return [Node(doc['_id'], x=doc['x'], y=doc['y'], borders=doc['borders']) for doc in query.result]


def load_nodes_from_csv_file(path):
    """Return a list of nodes, loaded from specified csv file.

    :param path: the path to the file
    :return: a list of nodes
    """
    with open(path, 'r') as csv_file:
        rows = csv.DictReader(csv_file)
        return [Node(row['id'], x=row['x'], y=row['y'], borders=row['borders']) for row in rows]


def _convert_old_document_type(filename, db_name):
    database = _start(db_name)
    query = Query(database, selector={'_id': {'$gt': None}}, fields=['_id'], use_index='_all_docs')

    with open(filename, mode='w') as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=CSV_FIELDS)
        writer.writeheader()
        doc_list = [doc['_id'] for doc in query.result]
        doc_list.sort(key=lambda x: int(x))
        for doc_id in doc_list:
            print("Document", doc_id)
            document = database[doc_id]
            directions = document['Misurazioni']
            for direction in directions.keys():
                measures = directions[direction]
                measure_keys = list(measures.keys())
                measure_keys.sort(key=lambda x: int(x.replace('Misurazione ', '')))
                for measure in measure_keys:
                    mv = measures[measure]['Vettore Magnetico']
                    dictionary = get_initialized_dict()
                    dictionary[ID_KEY] = doc_id
                    dictionary[X_KEY] = document['X position']
                    dictionary[Y_KEY] = document['Y position']
                    dictionary[BORDERS_KEY] = ''.join(Data.convert_direction(document['Borders']))
                    dictionary[DIRECTION_KEY] = ''.join(Data.convert_direction(direction))
                    dictionary[MEASURE_KEY] = measure.replace('Misurazione ', '')
                    dictionary[MV_X_KEY] = mv[0]
                    dictionary[MV_Y_KEY] = mv[1]
                    dictionary[MV_Z_KEY] = mv[2]

                    # WIFI LIST
                    rssi_list = list(measures[measure].keys())
                    if 'Vettore Magnetico' in rssi_list:
                        rssi_list.remove('Vettore Magnetico')
                    rssi_list.sort(key=lambda x: int(x.replace('RSSI ', '')))  # order list on number base
                    for rssi_key in rssi_list:
                        rssi = measures[measure][rssi_key]
                        if rssi['id'].strip() in CSV_FIELDS:
                            dictionary[rssi['id']] = rssi['value']
                    writer.writerow(dictionary)
                print("\t", direction, "converted.")


def _convert_new_document_type(filename, db_name):
    """ ``todo:: ble beacons and magnetic field need to be saved to the csv file ``
    """
    database = _start(db_name)
    query = Query(database, selector={'_id': {'$gt': None}}, fields=['_id'], use_index='_all_docs')

    with open(filename, mode='w') as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=CSV_FIELDS)
        writer.writeheader()
        doc_list = [doc['_id'] for doc in query.result]
        doc_list.sort(key=lambda x: int(x))
        for doc_id in doc_list:
            print("Document", doc_id)
            document = database[doc_id]
            measures = document['measures']
            for direction in measures.keys():
                wifi_list = measures[direction]['wifi']
                # mv_list = measures[direction]['mv']
                # ble = measures[direction]['ble']  # feature not used
                index = 1
                for measure in wifi_list:
                    dictionary = get_initialized_dict()
                    dictionary[ID_KEY] = document['_id']
                    dictionary[X_KEY] = document['x']
                    dictionary[Y_KEY] = document['y']
                    dictionary[BORDERS_KEY] = ''.join(Data.convert_direction(document['borders']))
                    dictionary[DIRECTION_KEY] = ''.join(Data.convert_direction(direction))
                    dictionary[MEASURE_KEY] = index
                    for wifi_node in measure:
                        if wifi_node['id'] in dictionary.keys():
                            dictionary[wifi_node['id']] = wifi_node['value']
                    writer.writerow(dictionary)
                    index += 1
                print("\t", direction, "converted.")


def export_db_to_csv(filename, doc_type='new', db_name=None):
    """Exports all documents from specified database to a csv file.

    :param filename: the csv file where to export documents
    :param doc_type: the structure type of documents
    :param db_name: the database to export

    .. note:: Use 'new' as doc_type for new json document structure, 'old', for old one.
    .. seealso:: :ref:`legacy.py` for the new json document structure
    """
    if _client is None:
        initialize()
    if db_name is None:
        db_name = fingerprints_db

    if doc_type == 'new':
        _convert_new_document_type(filename, db_name)
    else:
        _convert_old_document_type(filename, db_name)


def get_initialized_dict():
    """Initializes a dictionary with pairs: ap mac address and -110 default rssi value.

    :return: a dictionary populated by ap mac addresses and -110 default rssi values
    """
    dictionary = {}.fromkeys(CSV_FIELDS)
    for mac in Data.get_ap5ghz():
        dictionary[mac] = -110
    for mac in Data.get_ap24ghz():
        dictionary[mac] = -110
    return dictionary


if __name__ == '__main__':
    # TESTS
    initialize()
    export_db_to_csv("fingerprints.csv", doc_type='old', db_name='fingerprints_backup')
    close()
