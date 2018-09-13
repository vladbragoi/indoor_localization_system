import csv
from node import Node


def read_data_from_file(nodes, path):
    with open(path, 'r') as csv_file:
        rows = csv.DictReader(csv_file)
        for row in rows:
            node = Node(id=row['id'], x=row['x'], y=row['y'], borders=row['borders'])
            nodes[node.y][node.x] = node


def get_list_from(doc):
    pass


