import configparser
import csv
from node import Node

DIRECTION_KEY = 'direction'
MV_X_KEY = 'mv[x]'
MV_Y_KEY = 'mv[y]'
MV_Z_KEY = 'mv[z]'


class Data:

    def __init__(self):
        self._dictionary = Data.get_initialized_dict()
        self._fields = [DIRECTION_KEY] + Data.get_ap5ghz() + Data.get_ap24ghz() + [MV_X_KEY, MV_Y_KEY, MV_Z_KEY]

    def __str__(self):
        return str(self.to_list())

    def add_wifi_list(self, wifi_list):
        for wifi_node in wifi_list:
            if wifi_node['id'] in self._dictionary.keys():
                self._dictionary[wifi_node['id']] = wifi_node['value']

    def add_mv(self, mv):
        self._dictionary[MV_X_KEY] = mv[0]
        self._dictionary[MV_Y_KEY] = mv[1]
        self._dictionary[MV_Z_KEY] = mv[2]

    def add_direction(self, direction):
        # noinspection PyTypeChecker
        self._dictionary[DIRECTION_KEY] = Data.convert_direction(direction)

    def to_list(self):
        dict_list = sorted(self._dictionary.items(), key=lambda pair: self._fields.index(pair[0]))
        return [float(x[1]) for x in dict_list]

    @staticmethod
    def load_nodes_from_file(nodes, path):
        with open(path, 'r') as csv_file:
            rows = csv.DictReader(csv_file)
            for row in rows:
                node = Node(node_id=row['id'], x=row['x'], y=row['y'], borders=row['borders'])
                nodes[node.y][node.x] = node

    @staticmethod
    def convert_direction(direction):
        """Converts the direction string in input to tuple type using conventions below:
        0 -> NORTH (N)
        1 -> SOUTH (S)
        2 -> EAST (E)
        3 -> WEST (W).
        :param direction: usually a string of form NORTH or N, S, ...
        :return: a tuple such as ('0', '1', '3') or a string e.g. 'N', 'S', etc.
        """
        conventions = {'N': '0', 'S': '1', 'E': '2', 'W': '3'}
        out_direction = []
        if ',' in direction:
            direction = direction.split(',')
        elif ' ' in direction:
            direction = direction.split(' ')

        if type(direction) == list or type(direction) == tuple:
            for dir in direction:
                out_direction.append(conventions[dir.strip()[0].upper()])
            return tuple(out_direction)
        else:
            return conventions[direction[0].upper()]

    @staticmethod
    def get_initialized_dict():
        """Initializes a dictionary with pairs: ap mac address and -110 default rssi value.
        :return: a dictionary populated by ap mac addresses and -110 default rssi values
        """
        dictionary = {DIRECTION_KEY: '',
                      MV_X_KEY: 0,
                      MV_Y_KEY: 0,
                      MV_Z_KEY: 0
                      }
        for mac in Data.get_ap5ghz():
            dictionary[mac] = -110
        for mac in Data.get_ap24ghz():
            dictionary[mac] = -110
        return dictionary

    @staticmethod
    def get_ap5ghz():
        config = configparser.ConfigParser()
        config.read('setup.ini')
        return [ap.replace('\n', '') for ap in str(config['Access Points']['ap5ghz']).split(',')]

    @staticmethod
    def get_ap24ghz():
        config = configparser.ConfigParser()
        config.read('setup.ini')
        return [ap.replace('\n', '') for ap in str(config['Access Points']['ap24ghz']).split(',')]


if __name__ == '__main__':
    data = Data()
    data.add_direction('NORTH')
    print(data._dictionary)
    print(data.to_list())
