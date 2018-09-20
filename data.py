import configparser
import re

DIRECTION_KEY = 'direction'
MV_X_KEY = 'mv[x]'
MV_Y_KEY = 'mv[y]'
MV_Z_KEY = 'mv[z]'


class Data:

    def __init__(self):
        """Initialize a wrapper of dict in order to preserve associations with `_fields` list."""
        self._dictionary = Data.get_initialized_dict()
        self._fields = [DIRECTION_KEY] + Data.get_ap5ghz() + Data.get_ap24ghz() + [MV_X_KEY, MV_Y_KEY, MV_Z_KEY]

    def __str__(self):
        return str(self.to_list())

    def add_wifi_list(self, wifi_list):
        """Adds a list of wifi nodes to the dictionary.

        :param wifi_list: a wifi node list"""
        for wifi_node in wifi_list:
            if wifi_node['id'] in self._dictionary.keys():
                self._dictionary[wifi_node['id']] = wifi_node['value']

    def add_mv(self, mv):
        """Adds magnetic vector to the dictionary.

        :param mv: a magnetic vector of form [x, y, z]"""
        self._dictionary[MV_X_KEY] = mv[0]
        self._dictionary[MV_Y_KEY] = mv[1]
        self._dictionary[MV_Z_KEY] = mv[2]

    def add_direction(self, direction):
        """Adds """
        # noinspection PyTypeChecker
        self._dictionary[DIRECTION_KEY] = Data.convert_direction(direction)

    def to_list(self):
        dict_list = sorted(self._dictionary.items(), key=lambda pair: self._fields.index(pair[0]))
        return [float(x[1]) for x in dict_list]

    @staticmethod
    def convert_direction(direction):
        """Converts the direction string in input to tuple type using conventions below:

        1 -> NORTH (N)
        2 -> SOUTH (S)
        3 -> EAST (E)
        4 -> WEST (W).

        :param direction: usually a string of form NORTH or N, S, ...
        :return: a tuple such as ('1', '2', '3') or a string e.g. 'N', 'S', etc.
        """
        pattern = re.compile('[NSEW]')
        conventions = {'N': '1', 'S': '2', 'E': '3', 'W': '4'}
        out_direction = []
        if ',' in direction:
            direction = direction.split(',')
        elif ' ' in direction:
            direction = direction.split(' ')

        if type(direction) == list or type(direction) == tuple:
            for dir in direction:
                dir = dir.strip().upper()
                if pattern.match(dir[0]):
                    out_direction.append(conventions[dir[0]])
            return tuple(out_direction)
        else:
            return conventions[direction[0].upper()]

    @staticmethod
    def get_initialized_dict():
        """Initializes a dictionary with pairs: ap mac address and -110 default rssi value.

        :return: a dictionary populated by ap mac addresses and -110 default rssi values
        """
        dictionary = {
            DIRECTION_KEY: '',
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
        return [ap.replace('\n', '').strip() for ap in str(config['Access Points']['ap5ghz']).split(',')]

    @staticmethod
    def get_ap24ghz():
        config = configparser.ConfigParser()
        config.read('setup.ini')
        return [ap.replace('\n', '').strip() for ap in str(config['Access Points']['ap24ghz']).split(',')]


if __name__ == '__main__':
    # TESTS
    data = Data()
    data.add_direction('NORTH')
    print(data._dictionary)
    print(data)
    print(Data.convert_direction("N,S"))