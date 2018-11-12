import argparse
import signal
import sys

from graph import Graph
from fifo import Queue
from node import Node
from data import Data
from functools import reduce
import matlab.engine
import configparser
import database


def loop(mode=2):
    queue = Queue()
    for change in database.changes(database.localization_db, filter_function="online/dataDoc"):
        doc = change['doc']

        data = Data()
        updated_graph = _graph.copy()

        direction = doc['direction']
        data.add_direction(direction)
        # ble = doc['measures']['ble']  # BLE Beacons feature is not used yet
        wifi = doc['measures']['wifi']
        mv = doc['measures']['mv']

        mv_x = list(map(lambda x: x['values'][0], mv))
        mv_y = list(map(lambda y: y['values'][1], mv))
        mv_z = list(map(lambda z: z['values'][2], mv))
        mv_mean = [round(reduce(lambda x, y: x + y, mv_x) / len(mv_x), 4),
                   round(reduce(lambda x, y: x + y, mv_y) / len(mv_y), 4),
                   round(reduce(lambda x, y: x + y, mv_z) / len(mv_z), 4)]
        data.add_mv(mv_mean)
        data.add_wifi_list(wifi.pop())

        mat_data = matlab.double(data.to_list())

        ###############################################################################
        #                   MACHINE LEARNING ALGORITHM HERE
        ###############################################################################
        result = _matlab_engine.findRP(mat_data, mode, nargout=8)

        updated_graph.update_weights(result[4:], Data.convert_direction(direction))

        queue.enqueue(result[4:])  # take from the 4th to 8th element from result list
        while len(queue) > 2:
            queue.dequeue()

        if len(queue) >= 2:
            sources = [int(rp) for rp in queue[0]]
            targets = [int(rp) for rp in queue[1]]
            print(sources, targets)
            node_id = str(sources[0])
            node = updated_graph.node[node_id]
            node = Node(node_id=node_id, x=node['x'], y=node['y'], borders="")
            node.save_into(database.get_localization_db(), doc['_id'] + "_result")

            # PAY ATTENTION HERE
            # TODO: graph usage need to be improved
            node_id = updated_graph.lighter_route(sources, targets)
            if node_id is not None:
                node = updated_graph.node[node_id]
                node = Node(node_id=node_id, x=node['x'], y=node['y'], borders="")
                node.save_into(database.get_localization_db(), doc['_id'] + "_result")


def run(mode, input_file, output_file):
    global _graph, _matlab_engine
    config = configparser.ConfigParser()
    config.read('config.ini')
    fingerprint_size = int(config['Graph']['fingerprint_size'])

    print("Configuring graph...")
    _graph = Graph(fingerprint_size)

    if input_file is not None:
        _graph.load_from_json_file(input_file)
    else:
        nodes = database.get_nodes()
        _graph.add_nodes(nodes)
        _graph.add_edges(nodes)

    if output_file is not None:
        _graph.write_to_json_file(output_file)
        _graph.write_in_dot_notation(output_file)

    print("Starting matlab...")
    _matlab_engine = matlab.engine.start_matlab()
    _matlab_engine.addpath('matlab')
    print("Matlab started")
    print("Waiting for values...")
    loop(mode)


def signal_handler(sig, frame):
    if sig == signal.SIGINT:
        print('Closing...')
        database.close()
        sys.exit(0)


def main():
    signal.signal(signal.SIGINT, signal_handler)
    database.initialize()

    # MENU
    parser = argparse.ArgumentParser()
    parser.add_argument("-m", "--mode", default=2, metavar="INT-VALUE", type=int,
                        help="""specify the mode of the Voting algorithm: 
                        2 (default) both RSSI and MV are used to create a single classifier;
                        1 both RSSI and MV are used to create a separated classifier.""")
    parser.add_argument("-i", "--input", metavar="INPUT-FILE", type=str,
                        help="specify a file in JSON format to load the graph from.")
    parser.add_argument("-o", "--output", metavar="OUTPUT-FILE", type=str,
                        help="specify a file where to save the graph in JSON format.")
    args = parser.parse_args()

    run(mode=args.mode, input_file=args.input, output_file=args.output)
    database.close()    # close db connection


if __name__ == '__main__':
    main()
