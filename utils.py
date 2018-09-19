def inherit_docstring(from_function, sep="\n"):
    """
    Decorator: Copy the docstring of `from_function`
    """
    def _decorator(func):
        source_doc = from_function.__doc__
        if func.__doc__ is None:
            func.__doc__ = source_doc
        else:
            func.__doc__ = sep.join([source_doc, func.__doc__])
        return func
    return _decorator
