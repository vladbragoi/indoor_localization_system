from cloudant.client import CouchDB
from cloudant.database import CouchDatabase
from cloudant.document import Document
from cloudant.result import Result
import configparser
import gc

"""This script should be used to convert old json document structure to the new one:

document {
    "_id": "x",
    "_rev": "xxx",
    "borders": "0",
    "x": "1",
    "y": "3",
    "measures": {
    "NORTH": {
      "wifi": [
        [
          {
            "id": "34:db:fd:a4:cd:0e",
            "value": -45,
            "type": "WIFI"
          },
          ...,
          {<->}
        ],
        [<->]
      ],
      "ble": [<->],
      "mv": [
        {
          "values": [
            -21.440125,
            0.29144287,
            -39.4516
          ]
        },
        {<->}
      ]
    },
    "SOUTH": {<->},
    "EAST": {<->},
    "WEST": {<->}
    }
For old document structure see at the end of this file.
"""

_client = None
_source_db = None
_target_db = None


def initialize():
    """Initializes connection to server."""
    global _client
    config = configparser.ConfigParser()
    config.read('setup.ini')
    url = config['Database']['url']
    username = config['Database']['username']
    password = config['Database']['password']
    _client = CouchDB(username, password, url=url, connect=True)


def _init_source_db(db_name):
    """Gets source db instance.

    :param db_name: the source database name"""
    global _source_db
    _source_db = CouchDatabase(_client, db_name)


def _init_target_db(db_name):
    """Gets target db instance.

    :param db_name: the target database name"""
    global _target_db
    _target_db = CouchDatabase(_client, db_name)


def close():
    """Closes the connection."""
    _client.disconnect()


def convert_and_save_to_target(document):
    directions = document['Misurazioni']
    new_measures = {}

    for direction in directions.keys():
        new_measures[direction] = {
            'ble': [],
            'mv': [],
            'wifi': []
        }
        measures = directions[direction]
        measure_keys = list(measures.keys())
        measure_keys.sort(key=lambda x: int(x.replace('Misurazione ', '')))  # order list on number base

        for measure in measure_keys:
            # MAGNETIC VECTOR
            magnetic_vector = measures[measure]['Vettore Magnetico']
            magnetic_list = new_measures[direction]['mv']
            magnetic_list.append({'values': magnetic_vector})
            new_measures[direction]['mv'] = magnetic_list
            # WIFI LIST
            rssi_list = list(measures[measure].keys())
            if 'Vettore Magnetico' in rssi_list:    # Magnetic Vector just added
                rssi_list.remove('Vettore Magnetico')
            rssi_list.sort(key=lambda x: int(x.replace('RSSI ', '')))   # order list on number base

            node_list = []
            for rssi_key in rssi_list:
                wifi_node = measures[measure][rssi_key]
                wifi_node['type'] = "WIFI"
                node_list.append(wifi_node)

            wifi_list = new_measures[direction]['wifi']
            wifi_list.append(node_list)
            new_measures[direction]['wifi'] = wifi_list

    data = {
        '_id': document['_id'],
        'x': document['X position'],
        'y': document['Y position'],
        'borders': document['Borders'],
        'measures': new_measures
    }

    with Document(_target_db, data['_id']) as document:
        document.update(data)

    if document.exists():
        print(document['_id'], "converted.")

    del directions
    del data
    del new_measures
    del document


def start():
    source_db_name = str(input("Insert name of source DB: "))
    target_db_name = str(input("Insert name of target DB: "))
    _init_source_db(source_db_name)
    _init_target_db(target_db_name)
    result_collection = Result(_source_db.all_docs)
    doc_list = [doc['id'] for doc in result_collection]

    for doc_id in doc_list:
        document = _source_db[doc_id]
        convert_and_save_to_target(document)
        del document
        gc.collect()


def main():
    initialize()
    start()
    close()


if __name__ == '__main__':
    main()

"""Old document structure:
document {
    "_id": "x",
    "_rev": "xxx",
    "Misurazioni": {
        "NORTH": {
            "Misurazione 0": {
                "RSSI 0": {
                "id": "34:a8:4e:70:cd:1f",
                "value": -84
                },
                ...
                "RSSI N": {<->}
            }, 
            ...
            "Misurazione M": {<->}
        },
        "SOUTH": {<->},
        "EAST": {<->},
        "WEST": {<->}
    },
    "X position": "1",
    "Borders": "N, W",
    "Y position": "1"
}
"""