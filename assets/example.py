from collections import deque
from itertools import islice


def window(iterable, size=2, cast=tuple):
    iterable = iter(iterable)
    d = deque(islice(iterable, size), size)
    if cast:
        yield cast(d)
        for x in iterable:
            d.append(x)
            yield cast(d)
    else:
        yield d
        for x in iterable:
            d.append(x)
            yield d


assert list(window([1, 2, 3])) == [(1, 2), (2, 3)]
