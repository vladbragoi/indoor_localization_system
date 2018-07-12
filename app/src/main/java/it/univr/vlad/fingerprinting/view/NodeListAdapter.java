package it.univr.vlad.fingerprinting.view;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.diffutil.NodesDiffCallback;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.mv.MagneticVector;

public class NodeListAdapter extends RecyclerView.Adapter<NodeListAdapter.ViewHolder> {

    private MagneticVector mv;
    private List<Node> nodes = new ArrayList<>();

    public void setNodes(List<Node> newNodes) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new NodesDiffCallback(this.nodes, newNodes), true);
        diffResult.dispatchUpdatesTo(this);
        this.nodes.clear();
        this.nodes.addAll(newNodes);
    }

    public void setMv(MagneticVector mv) {
        this.mv = mv;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_node, parent, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Node node = nodes.get(position);

        holder.getType().setText(node.getType());
        holder.getBssid().setText(node.getId());
        holder.getValue().setText(String.valueOf(node.getValue()));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        }
        else {
            Bundle o = (Bundle) payloads.get(0);
            for (String key : o.keySet()) {
                if (key.equals("value")) {
                    holder.getValue().setText(String.valueOf(o.getInt("value")));
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return nodes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView bssid;
        private TextView value;
        private TextView type;

        ViewHolder(View itemView) {
            super(itemView);
            bssid = itemView.findViewById(R.id.bssid);
            value = itemView.findViewById(R.id.value);
            type = itemView.findViewById(R.id.type);
        }

        public TextView getBssid() {
            return bssid;
        }

        public TextView getValue() {
            return value;
        }

        public TextView getType() {
            return type;
        }
    }
}
