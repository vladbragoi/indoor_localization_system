from queue import Queue
from graph import Graph
from data import Data
from functools import reduce
import matlab.engine
import configparser
import database


def loop():
    id = None
    queue = Queue()

    for change in database.changes(database.localization_db(), filter_function="online/dataDoc"):
        doc = change['doc']

        data = Data()

        data.add_direction(doc['direction'])
        ble = doc['measures']['ble']
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

        result = _matlab_engine.findRP(mat_data, 1, nargout=8)

        print(result)
        # H = _graph.copy(as_view=False)
        # data = _matlab_engine.double(data.get_list_from(doc))
        # data = data[1:]
        # result = _matlab_engine.findRP(data, 2, nargout=8)
        #
        # # update_weights(H, [result[1], result[2]], set_borders(doc['direction'][0]))
        #
        # print(result)

        """
        rps_fifo.append(result[1:])  # take 2nd and 3rd elements from result list
        while len(rps_fifo) > 2:
            rps_fifo.pop()

        if len(rps_fifo) >= 2:
            sources = [int(rp) for rp in rps_fifo[0]]
            targets = [int(rp) for rp in rps_fifo[1]]
            id = target_lighter_route(H, sources, targets)

        if id is not None:     # empty queue
            doc_id = doc['_id'] + "_result"
            fingerprint_node = H.node[id]

            #db.create_document({'_id': doc_id,
                                #'type': 'online_result'}, throw_on_exists=True)

            with Document(db, doc_id) as document:
                if document.exists():
                    document.fetch()
                document['fingerprint'] = id
                document['type'] = 'online_result'
                document['X position'] = fingerprint_node['x']
                document['Y position'] = fingerprint_node['y']
            print("Node selected:", id, "=", fingerprint_node)
        """


def run(update):
    global _graph, _matlab_engine
    config = configparser.ConfigParser()
    config.read('setup.ini')
    fingerprint_size = int(config['Graph']['fingerprint_size'])

    print("Configuring graph...")
    _graph = Graph(fingerprint_size)

    if update:
        nodes = database.get_nodes()
        _graph.add_nodes(nodes)
        _graph.add_edges(nodes)
        _graph.write_to_json_file()
    else:
        _graph.load_from_json_file()

    print("Starting matlab...")
    _matlab_engine = matlab.engine.start_matlab()
    _matlab_engine.addpath('matlab')
    print("Matlab started")
    loop()


def main():
    database.initialize()
    run(update=False)
    database.close()


if __name__ == '__main__':
    main()
