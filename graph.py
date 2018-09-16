import networkx


class Graph(networkx.DiGraph):

    def __init__(self, neighbor_distance, **attr):
        """A directed graph object that extends a networkx DiGraph.
        :param neighbor_distance: distance of neighborhood
        """
        super().__init__(**attr)
        self._distance = neighbor_distance

    def add_nodes(self, nodes):
        """Adds nodes from nodes list.
        :param nodes: a list of nodes
        """
        for node in nodes:
            self.add_node(node.id, x=node.x, y=node.y, borders=node.borders)

    def add_edges(self, nodes):
        """Adds edges iterating the nodes list and decorating each edge with direction (dir) and a
        specific weight given by a static probability: 100 / node out-degree, where out-degree is
        the length of the target node list.
        :param nodes: a list of nodes
        """
        for source in nodes:
            # filter the list querying for neighbors
            target_list = list(filter(lambda target: target.is_neighbor_of(source, self._distance), nodes))
            weight = 100 / len(target_list)
            list(map(lambda target: self.add_edge(source.id,
                                                  target.id,
                                                  dir=self._calculate_direction(source, target),
                                                  weight=round(weight, 2)), target_list))
            # print("source =", source.id, "targets =", [x.id for x in target_list])

    def update_weights(self, nodes, direction):
        """ Updates weight of edges in nodes list.
        @param nodes: a list of nodes
        @param direction: a tuple of direction strings ('0', '1', ...)
        """
        for node in nodes:
            adj_list = self.edges(node)
            for edge in adj_list:
                if self[edge[0]][edge[1]]["dir"] == direction:
                    self[edge[0]][edge[1]]["weight"] = 0

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
