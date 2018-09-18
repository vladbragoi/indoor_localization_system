class Queue(list):

    def dequeue(self):
        return self.pop(0)

    def enqueue(self, obj):
        self.append(obj)

    def add_all(self, obj_list):
        if len(obj_list) <= 0:
            raise IndexError
        elif type(obj_list) != list and type(obj_list) != tuple:
            raise TypeError
        else:
            for element in obj_list:
                self.enqueue(element)


if __name__ == '__main__':
    q = Queue()
    q.add_all([1, 2, 3])
    q.dequeue()
    q.enqueue(8)
    print(q)
