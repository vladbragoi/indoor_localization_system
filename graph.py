import json
import networkx
from sys import maxsize
from networkx.readwrite import json_graph


class Graph(networkx.DiGraph):

    @staticmethod
    def _calculate_direction(source, target):
        """ Direction could be one or more between [1..4]
        and represents the direction of the edge between source and target.
        [1..4] indices could be 1->NORTH, 2->SOUTH, 3->EAST and 4->WEST
        and depends on your the system reference.
        """
        direction = []
        x_direction = target.x - source.x
        y_direction = target.y - source.y
        # print("source:", source.id, "target:", target.id, "x", x_direction, "y", y_direction)

        if x_direction < 0:
            direction.append('4')
        elif x_direction > 0:
            direction.append('3')

        if y_direction < 0:
            direction.append('1')
        elif y_direction > 0:
            direction.append('2')

        return tuple(direction)

    def __init__(self, node_distance=0, **attr):
        """A directed graph object that extends a networkx DiGraph.

        :param neighbor_distance: distance of neighborhood
        """
        super().__init__(**attr)
        self._distance = node_distance

    def copy(self, as_view=False):
        super(Graph, self).copy()
        return self

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
        """ Updates weight of edges in nodes list that match direction specified.

        :param nodes: a list of nodes
        :param direction: a tuple of direction strings ('0', '1', ...)
        """
        # print("UPDATE WEIGHTS: ", nodes, direction)
        for node in nodes:
            if node not in self.nodes:
                continue
            adj_list = self.edges(node)
            for edge in adj_list:
                if self[edge[0]][edge[1]]["dir"] == direction:
                    self[edge[0]][edge[1]]["weight"] = 0

    def lighter_route(self, sources, targets):
        """Returns the rp in targets list, that have minimum path length
        from sources to each targets.

        :param sources: a list of source nodes
        :param targets: a list of target nodes
        :return: the target with min path length
        """
        min_target = None
        min_path_length = maxsize

        for source in sources:
            for target in targets:
                # source and target are nodes of the graph
                source = str(source)
                target = str(target)
                if source != target and all(str(x) in self.nodes for x in (source, target)):
                    path_length = networkx.dijkstra_path_length(self, source, target)
                    if path_length < min_path_length:
                        min_path_length = path_length
                        min_target = target
                elif min_target is None and str(target) in self.nodes:
                    min_target = target
        return min_target

    def load_from_json_file(self, filename):
        """Loads graph from json file."""
        with open(filename, 'r') as json_file:
            data = json.load(json_file)
            loaded_graph = json_graph.node_link_graph(data)
            self.add_nodes_from(loaded_graph.nodes(data=True))
            self.add_edges_from(loaded_graph.edges(data=True))

    def write_to_json_file(self, filename):
        """Writes graph to json file."""
        data = json_graph.node_link_data(self)
        with open(filename, 'w') as json_file:
            json.dump(data, json_file)

    def write_in_dot_notation(self, filename):
        networkx.nx_agraph.write_dot(self, filename.replace('json', 'dot'))


if __name__ == '__main__':
    # TESTS
    G = Graph(9)
    G.add_nodes_from([1, 2, 3])
    G.add_edges_from(((1, 2), (2, 3), (3, 1)))
    print(G.nodes, G.edges, "distance", G._distance, sep='\t')

    H = G.copy()
    H.add_nodes_from([4, 5, 6])
    print(H.nodes, H.edges, "distance", H._distance, sep='\t')

    G.write_in_dot_notation()
