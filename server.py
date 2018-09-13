from queue import Queue
import matlab.engine
import configparser
import database
import data

database.config()
database.connect()

# config = configparser.ConfigParser(empty_lines_in_values=False)
# config.read('setup.ini')
# print([str(ap).replace("\n", "") for ap in str(config['Access Points']['ap5ghz']).split(',')])


# noinspection PyUnresolvedReferences
def spin(db, orig_graph, matlab_eng):
    id = None
    rps_fifo = Queue()

    changes = db.infinite_changes(feed='continuous', include_docs=True, filter="online/onlineDocs", since='now')
    for change in changes:
        doc = change['doc']

        H = orig_graph.copy(as_view=False)
        data = matlab.double(data.get_list_from(doc))
        data = data[1:]
        result = matlab_eng.findRP(data, 2, nargout=8)

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
    pass


if __name__ == '__main__':
    main()