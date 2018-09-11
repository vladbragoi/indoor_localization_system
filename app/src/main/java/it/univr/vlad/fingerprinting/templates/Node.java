package it.univr.vlad.fingerprinting.templates;

import android.support.annotation.NonNull;

public abstract class Node implements Comparable{

    /**
     * {@link NodeType
     */
    private String type;

    private String id;      /// Mac Address

    private int value;      /// Rssi

    protected Node(String id, int value, String type) {
        this.type = type;
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Node node = (Node) obj;

        return this.id.equals(node.id)
                && this.type.equals(node.type)
                && this.value == node.value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@NonNull Object o) {
        Node tmp = (Node) o;

        return  this.id.compareTo(tmp.id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = value;
        result = result ^ (id != null ? id.hashCode() : 0);
        result = result ^ (type != null ? type.hashCode() : 0);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "id: " + id + " value: " + value;
    }
}
