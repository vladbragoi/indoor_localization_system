import csv
from node import Node


# config = configparser.ConfigParser(empty_lines_in_values=False)
# config.read('setup.ini')
# print([str(ap).replace("\n", "") for ap in str(config['Access Points']['ap5ghz']).split(',')])

def load_nodes_from_file(nodes, path):
    with open(path, 'r') as csv_file:
        rows = csv.DictReader(csv_file)
        for row in rows:
            node = Node(id=row['id'], x=row['x'], y=row['y'], borders=row['borders'])
            nodes[node.y][node.x] = node


def load_nodes_from_db():
    pass


def get_list_from(doc):
    pass


def get_data(doc):
    """ Returns a list of data matching this pattern: [Direction RSSI Magnetic_X Magnetic_Y Magnetic_Z] """
    l = []
    dictionary = {}
    initialize(dictionary)
    try:
        direction = set_borders(doc['direction'][0])
        dictionary['direction'] = direction
        add_values(dictionary, doc['values'])
        dictionary['mV[x]'] = doc['mag vector'][0]
        dictionary['mV[y]'] = doc['mag vector'][1]
        dictionary['mV[z]'] = doc['mag vector'][2]
    except KeyError:
        pass

    # sort the list in order to preserve campi's order
    l = sorted(dictionary.items(), key=lambda pair: campi.index(pair[0]))
    l = [float(x[1]) for x in l]

    return l
