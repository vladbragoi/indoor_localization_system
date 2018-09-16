import networkx


class Graph:

    _graph = None   # Directed graph

    def __init__(self, distance):
        self._distance = distance
        self._graph = networkx.DiGraph()

    def __iter__(self):
        return self._graph.__iter__()

    def nodes(self):
        """Gets all nodes in the graph with their specified attributes.
        :return: a list of tuple with node-id and node attributes.
        """
        return self._graph.nodes(data=True)

    def edges(self):
        """Gets all edges in the graph with their specified attributes.
        :return: a list of tuple with node-ids of the edge and the edge's attributes.
        """
        return self._graph.edges(data=True)

    def add_nodes(self, nodes):
        """Adds nodes from nodes list.
        :param nodes: a list of nodes
        """
        for node in nodes:
            self._graph.add_node(node.id, x=node.x, y=node.y, borders=node.borders)

    def add_edges(self, nodes):
        """Adds edges iterating the nodes list.
        :param nodes: a list of nodes
        """
        for source in nodes:
            # filter the list querying for neighbors
            target_list = list(filter(lambda target: target.is_neighbor_of(source, self._distance), nodes))
            list(map(lambda target: self._graph.add_edge(source.id,
                                                         target.id,
                                                         dir=self._calculate_direction(source, target)), target_list))
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

    @staticmethod
    def _calculate_direction(source, target):
        """ Direction could be one or more between [0..3]
        and represents the direction of the edge between source and target.
        [0..3] indices could be 0->NORTH, 1->SOUTH, 2->EAST and 3->WEST
        and depends on the reference system specified in the 'setup.ini'
        configuration file.
        """
        direction = []
        x_direction = target.x - source.x
        y_direction = target.y - source.y
        # print("source:", source.id, "target:", target.id, "x", x_direction, "y", y_direction)

        if x_direction < 0:
            direction.append('3')
        elif x_direction > 0:
            direction.append('2')

        if y_direction < 0:
            direction.append('0')
        elif y_direction > 0:
            direction.append('1')

        return tuple(direction)
