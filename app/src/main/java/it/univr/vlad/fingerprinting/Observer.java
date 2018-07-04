package it.univr.vlad.fingerprinting;

import java.util.List;

public interface Observer<T> {
    void update(List<T> results);
}
