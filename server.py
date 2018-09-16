from queue import Queue
from graph import Graph
import matlab.engine
import configparser
import database


def run():
    id = None
    rps_queue = Queue()

    changes = database.changes(database._loc_db_instance, filter_function="online/dataDoc")
    for change in changes:
        doc = change['doc']

        H = _graph.copy(as_view=False)
        data = _matlab_engine.double(data.get_list_from(doc))
        data = data[1:]
        result = _matlab_engine.findRP(data, 2, nargout=8)

        # update_weights(H, [result[1], result[2]], set_borders(doc['direction'][0]))

        print(result)

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


def main():
    global _graph, _matlab_engine
    config = configparser.ConfigParser()
    config.read('setup.ini')
    fingerprint_size = int(config['Graph']['fingerprint_size'])

    _graph = Graph(fingerprint_size)
    nodes = database.get_nodes()
    _graph.add_nodes(nodes)
    _graph.add_edges(nodes)

    print("Starting matlab...")
    _matlab_engine = matlab.engine.start_matlab()
    print("Matlab started")

    run()


if __name__ == '__main__':
    database.initialize()
    main()
    database.close()
