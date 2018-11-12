package it.univr.vlad.fingerprinting.diffutil;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

import it.univr.vlad.fingerprinting.templates.Node;

// TODO: fix items position in recycler view
public class NodesDiffCallback extends DiffUtil.Callback{

    private List<Node> oldNodes;
    private List<Node> newNodes;

    public NodesDiffCallback(List<Node> newNodes, List<Node> oldNodes) {
        this.newNodes = newNodes;
        this.oldNodes = oldNodes;
    }

    @Override
    public int getOldListSize() {
        return oldNodes.size();
    }

    @Override
    public int getNewListSize() {
        return newNodes.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        Node oldItem = oldNodes.get(oldItemPosition);
        Node newItem = newNodes.get(newItemPosition);

        return oldItem.getType().equals(newItem.getType())
                && oldItem.getId().equals(newItem.getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldNodes.get(oldItemPosition).getValue() == newNodes.get(newItemPosition).getValue();
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        Node newNode = newNodes.get(newItemPosition);

        return newNode.getValue();

        //return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}