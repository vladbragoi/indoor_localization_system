import networkx


class Graph:

    _graph = None   # Directed graph
    _max_width = 0
    _max_height = 0

    def __init__(self, width, height, fingerprint_size):
        self._max_width = width
        self._max_height = height
        self._fingerprint_size = fingerprint_size
        self._graph = networkx.DiGraph()

    def add_nodes(self, nodes):
        for node in nodes:
            self._graph.add_node(node.id, x=node.x, y=node.y, borders=node.borders)

    def __iter__(self):
        return self._graph.__iter__()

    def edges(self):
        return self._graph.edges

    def nodes(self):
        return self._graph.nodes(data=True)

    def add_edges(self, nodes):
        # x = filter(lambda node: node.x > 0, l)
        # individuo gli archi tra i nodi scorrendo la matrice (NOTA: 0=NORTH, 1=SOUTH, 2=EAST, 3=WEST)
        for node in nodes:
            target_list = list(filter(lambda target: target.is_neighbor_of(node, self._fingerprint_size) , nodes))
            list(map(lambda target: self._graph.add_edge(node.id, target.id), target_list))
            # print("source = " , node.id, "targets = ", [x.id for x in target_list])

    def add_weights(self):
        """ ADD EDGES WEIGHTS
        @param graph the graph object
        """
        # probabilità statica: assegno un peso pari a 50 / grado del nodo a ciascun arco
        for node in self._graph.nodes.keys():
            degree = self._graph.out_degree(node)
            # print("Nodo:" ,node, "grado", degree)
            adj_list = self._graph.edges(node)
            # print("Lista di adiacenza:", adj_list)
            for nodes_of_an_edge in adj_list:
                self._graph.add_edge(nodes_of_an_edge[0], nodes_of_an_edge[1], weight=round(100 / degree, 2))

    def update_weights(self, rps, direction):
        """ UPDATE EDGES WEIGHTS
        @param graph the graph object
        @param rps a list of nodes
        @param direction a tuple of strings
        """
        for rp in rps:
            adj_list = self._graph.edges(rp)
            for edge in adj_list:
                if self._graph[edge[0]][edge[1]]["dir"] == direction:
                    self._graph[edge[0]][edge[1]]["weight"] = 0
