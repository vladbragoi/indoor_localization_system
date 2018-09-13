import configparser
import networkx


class Graph:

    _graph = None   # Directed graph
    _max_width = 0
    _max_height = 0

    def __init__(self):
        config = configparser.ConfigParser(empty_lines_in_values=False)
        config.read('setup.ini')
        self._max_width = config['Graph']['max_width']
        self._max_height = config['Graph']['max_height']
        self._graph = networkx.DiGraph()

    def add_nodes_to_graph(self, nodes):
        for row in range(self._max_height):
            for col in range(self._max_width):
                node = nodes[row][col]
                if node is not None:
                    self._graph.add_node(node.id, x=node.x, y=node.y, borders=node.borders)

    def _add_edge(self, nodes, source, i, j, borders=tuple()):
        if source is not None and i in range(0, self._max_height) and j in range(0, self._max_width) \
                and all(border not in source.borders for border in borders):
            target = nodes[i][j]
            if target is not None:
                self._graph.add_edge(source.id, target.id, dir=borders)

    def add_edges_to_graph(self, nodes):
        # individuo gli archi tra i nodi scorrendo la matrice (NOTA: 0=NORTH, 1=SOUTH, 2=EAST, 3=WEST)
        for row in range(self._max_height):
            for col in range(self._max_width):
                node = nodes[row][col]
                self._add_edge(nodes, node, row - 2, col - 2, ('3', '0'))
                self._add_edge(nodes, node, row, col - 2, ('3',))
                self._add_edge(nodes, node, row + 2, col - 2, ('1', '3'))
                self._add_edge(nodes, node, row + 2, col, ('1',))
                self._add_edge(nodes, node, row + 2, col + 2, ('1', '2'))
                self._add_edge(nodes, node, row, col + 2, ('2',))
                self._add_edge(nodes, node, row - 2, col + 2, ('2', '0'))
                self._add_edge(nodes, node, row - 2, col, ('0',))

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
