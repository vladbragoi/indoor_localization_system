package it.univr.vlad.fingerprinting;

import java.util.List;

public interface Observer {
    void update(List<Node> results);
    void update(float[] mv);
}
