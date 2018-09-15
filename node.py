class Node:
    id = int
    x = int
    y = int
    borders = str

    def __init__(self, id, x, y, borders):
        self.id = int(id)
        self.x = int(x)
        self.y = int(y)
        self.borders = borders

    def __eq__(self, other):
        return isinstance(other, Node) and self.id == other.id

    def __cmp__(self, other):
        return isinstance(other, Node) and self.id == other.id

    def __hash__(self):
        return self.id.__hash__() ^ self.x.__hash__() ^ self.y.__hash__()

    def __str__(self):
        return "Node: " + str(self.id) + " X: " + str(self.x) + " Y: " + str(self.y) + " Borders: " + self.borders

    def is_neighbor_of(self, source, distance):
        return self.x in range(source.x - distance, source.x + distance + 1) \
               and self.y in range(source.y - distance, source.y + distance + 1) \
               and self != source