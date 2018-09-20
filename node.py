from cloudant.document import Document


class Node:

    def __init__(self, node_id, x, y, borders):
        """A node is an object characterized by: ID, X, Y and Borders.
        :param node_id: node's number,
        :param x: horizontal position,
        :param y: vertical position,
        :param borders: the existence of borders in any cardinal directions (N, S, E, W)
        """
        self.id = node_id
        self.x = int(x)
        self.y = int(y)
        self.borders = borders

    def __eq__(self, other):
        """Two nodes are equals if they have same ID number.
        :param other: another node
        :return: True if they have same ID"""
        return isinstance(other, Node) and self.id == other.id

    def __hash__(self):
        return self.id.__hash__() ^ self.x.__hash__() ^ self.y.__hash__()

    def __str__(self):
        return "Node: " + str(self.id) + " X: " + str(self.x) + " Y: " + str(self.y) + " Borders: " + self.borders

    def is_neighbor_of(self, source, distance):
        """Two nodes are neighbors if their distance is less or equal to the distance param.
        :param source: source node
        :param distance: the distance between nodes to be neighbors
        """
        return isinstance(source, Node) \
            and self.x in range(source.x - distance, source.x + distance + 1) \
            and self.y in range(source.y - distance, source.y + distance + 1) \
            and self != source

    def save_into(self, database, doc_id, node_type='result_doc'):
        """Saves the node in the specified database with specified document id.
        :param database: the database instance where to save the node
        :param doc_id: the document in which save the node
        :param node_type: the type of the node: by default it is 'result_doc'.
        """
        with Document(database, doc_id) as document:
            if document.exists():
                document.delete()
            document['fingerprint'] = self.id
            document['type'] = node_type
            document['x'] = self.x
            document['y'] = self.y
        # document saved
