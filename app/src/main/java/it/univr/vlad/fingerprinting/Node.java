package it.univr.vlad.fingerprinting;

public abstract class Node {

    private String id;      /// Mac Address
    private int value;      /// Rssi

    protected Node(String id, int value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public String toString() {
        return "id: " + id + " value: " + value;
    }
}
