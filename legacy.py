from cloudant.client import CouchDB
from cloudant.database import CouchDatabase
from cloudant.result import Result
import configparser
import gc


_client = None
_source_db = None
_target_db = None


def load_source_db(db_name):
    global _client, _source_db
    config = configparser.ConfigParser()
    config.read('setup.ini')
    url = config['Database']['url']
    username = config['Database']['username']
    password = config['Database']['password']
    _client = CouchDB(username, password, url=url, connect=True)
    _source_db = CouchDatabase(_client, db_name)


def load_target_db(db_name):
    global _target_db
    _target_db = CouchDatabase(_client, db_name)


def close():
    _client.disconnect()


def convert_and_save_to_target(document):
    directions = document['Misurazioni']
    new_measurations = {}

    for direction in directions.keys():
        new_measurations[direction] = {
            'ble': [],
            'mv': [],
            'wifi': []
        }
        measurations = directions[direction]
        measuration_keys = list(measurations.keys())
        measuration_keys.sort(key=lambda x: int(x.replace('Misurazione ', '')))  # order list on number base

        for measuration in measuration_keys:
            # MAGNETIC VECTOR
            magnetic_vector = measurations[measuration]['Vettore Magnetico']
            magnetic_list = new_measurations[direction]['mv']
            magnetic_list.append({'values': magnetic_vector})
            new_measurations[direction]['mv'] = magnetic_list
            # WIFI LIST
            rssi_list = list(measurations[measuration].keys())
            if 'Vettore Magnetico' in rssi_list:    # Magnetic Vector just added
                rssi_list.remove('Vettore Magnetico')
            rssi_list.sort(key=lambda x: int(x.replace('RSSI ', '')))   # order list on number base

            node_list = []
            for rssi_key in rssi_list:
                wifi_node = measurations[measuration][rssi_key]
                wifi_node['type'] = "WIFI"
                node_list.append(wifi_node)

            wifi_list = new_measurations[direction]['wifi']
            wifi_list.append(node_list)
            new_measurations[direction]['wifi'] = wifi_list

    data = {
        '_id': document['_id'],
        'x': document['X position'],
        'y': document['Y position'],
        'borders': document['Borders'],
        'measurations': new_measurations
    }
    new_document = _target_db.create_document(data)
    if new_document.exists():
        print(new_document['_id'], "converted.")
        del directions
        del data
        del new_measurations
        del new_document


def main():
    load_source_db("fingerprints_backup")
    load_target_db("fingerprints_converted")
    result_collection = Result(_source_db.all_docs)
    doc_list = [doc['id'] for doc in result_collection]

    for doc_id in doc_list:
        document = _source_db[doc_id]
        convert_and_save_to_target(document)
        del document
        gc.collect()


if __name__ == '__main__':
    main()
    close()
