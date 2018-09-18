from cloudant.document import Document


class Node:

    def __init__(self, node_id, x, y, borders):
        self.id = node_id
        self.x = int(x)
        self.y = int(y)
        self.borders = borders

    def __eq__(self, other):
        return isinstance(other, Node) and self.id == other.id

    def __hash__(self):
        return self.id.__hash__() ^ self.x.__hash__() ^ self.y.__hash__()

    def __str__(self):
        return "Node: " + str(self.id) + " X: " + str(self.x) + " Y: " + str(self.y) + " Borders: " + self.borders

    def is_neighbor_of(self, source, distance):
        return self.x in range(source.x - distance, source.x + distance + 1) \
               and self.y in range(source.y - distance, source.y + distance + 1) \
               and self != source

    def save_into(self, database, doc_id):
        with Document(database, doc_id) as document:
            if document.exists():
                document.delete()
            document['fingerprint'] = self.id
            document['type'] = 'result_doc'
            document['x'] = self.x
            document['y'] = self.y
        # document saved
