class Queue(list):

    def dequeue(self):
        """Removes and return the first element inserted in the queue.
        :return: the first element inserted"""
        return self.pop(0)

    def enqueue(self, obj):
        """Inserts an element at the bottom of the queue.
        :param obj: the element to be inserted
        """
        self.append(obj)

    def add_all(self, obj_list):
        """Inserts a list of objects at the bottom of the queue in order.
        :param obj_list: list of objects to be inserted
        """
        if len(obj_list) <= 0:
            raise IndexError
        elif type(obj_list) != list and type(obj_list) != tuple:
            raise TypeError
        else:
            for element in obj_list:
                self.enqueue(element)


if __name__ == '__main__':
    # TESTS
    q = Queue()
    q.add_all([1, 2, 3])
    q.dequeue()
    q.enqueue(8)
    print(q)
