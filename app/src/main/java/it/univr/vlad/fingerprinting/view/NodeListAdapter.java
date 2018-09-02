package it.univr.vlad.fingerprinting.view;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.NodeType;
import it.univr.vlad.fingerprinting.diffutil.NodesDiffCallback;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.mv.MagneticVector;

public class NodeListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private MagneticVector mv;
    private List<Node> wifiNodes = new ArrayList<>();
    private List<Node> beaconNodes = new ArrayList<>();
    private Queue<List<Node>> pendingUpdates = new ArrayDeque<>();

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(viewType, parent, false);

        switch (viewType) {
            case R.layout.magnetic_vector:
                return new MvViewHolder(view);
            case R.layout.wifi_node:
                return new NodesViewHolder(view, NodeType.WIFI);
            default:
                return new NodesViewHolder(view, NodeType.BEACON);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Node node = null; // just a node

        switch (holder.getItemViewType()) {
            case R.layout.magnetic_vector:
                if (mv != null) {
                    ((MvViewHolder) holder).bindView(mv.getValues());
                    return;
                }
                break;

            case R.layout.beacon_node:
                if (beaconNodes != null) node = beaconNodes.get(position - 1);
                break;

            case R.layout.wifi_node:
                if (wifiNodes != null) node = wifiNodes.get(position - beaconNodes.size() - 1);
                break;
        }

        if (node != null) ((NodesViewHolder) holder).bindView(node);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }

        switch (holder.getItemViewType()) {
            case R.layout.magnetic_vector:
                if (payloads.get(0) instanceof float[]){
                    float[] values = (float[]) payloads.get(0);
                    ((MvViewHolder) holder).bindView(values);
                }
                break;
            case R.layout.beacon_node:
            case R.layout.wifi_node:
                if (payloads.get(0) instanceof Integer) {
                    int values = (int) payloads.get(0);
                    ((NodesViewHolder) holder).updateView(values);
                }
                break;
        }
    }

    public void addWifiNodes(List<Node> newNodes) {
        pendingUpdates.add(newNodes);
        if (pendingUpdates.size() > 1) return;
        updateNodesInternal(NodeType.WIFI, newNodes);
    }

    public void addBeaconNodes(List<Node> newNodes) {
        pendingUpdates.add(newNodes);
        if (pendingUpdates.size() > 1) return;
        updateNodesInternal(NodeType.BEACON, newNodes);
    }

    private void updateNodesInternal(NodeType nodeType, List<Node> newNodes) {
        final List<Node> oldNodes = new ArrayList<>();
        if (nodeType == NodeType.WIFI) {
            oldNodes.addAll(wifiNodes);
        }
        else {
            oldNodes.addAll(beaconNodes);
        }

        final Handler handler = new Handler();
        new Thread(() -> {
            final DiffUtil.DiffResult diffResult =
                    DiffUtil.calculateDiff(new NodesDiffCallback(newNodes, oldNodes), true);
            handler.post(() -> applyDiffResult(nodeType, newNodes, diffResult));
        }).start();
    }

    private void applyDiffResult(NodeType nodeType, List<Node> newNodes, DiffUtil.DiffResult diffResult) {
        pendingUpdates.remove();
        dispatchUpdates(nodeType, newNodes, diffResult);
        if (pendingUpdates.size() > 0)
            updateNodesInternal(nodeType, pendingUpdates.peek());
    }

    private void dispatchUpdates(NodeType nodeType, List<Node> newNodes, DiffUtil.DiffResult diffResult) {
        if (nodeType == NodeType.WIFI) {
            wifiNodes.clear();
            wifiNodes.addAll(newNodes);
        } else {
            beaconNodes.clear();
            beaconNodes.addAll(newNodes);
        }

        diffResult.dispatchUpdatesTo(this);
    }

    /* OLD METHOD
    public void addBeaconNodes(List<Node> newNodes) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new NodesDiffCallback(beaconNodes, newNodes), true);
        beaconNodes.clear();
        beaconNodes.addAll(newNodes);
        diffResult.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position + 1, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position + 1, count + 1);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition + 1, toPosition + 1);
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                notifyItemRangeChanged(position + 1, count, payload);
            }
        });
    }*/

    public void setMv(MagneticVector mv) {
        this.mv = mv;
        final Handler handler = new Handler();
        handler.post(() -> notifyItemChanged(0, mv.getValues()));
    }

    // TODO: fix item's type
    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return R.layout.magnetic_vector;
        else if (position > 0 && position <= beaconNodes.size())
            return R.layout.beacon_node;
        else
            return R.layout.wifi_node;
    }

    @Override
    public int getItemCount() {
        return wifiNodes.size() + beaconNodes.size() + 1;
    }

    public static class NodesViewHolder extends RecyclerView.ViewHolder {

        NodeType nodeType;
        CardView cardView;
        TextView bssid;
        TextView value;
        TextView type;

        NodesViewHolder(View itemView, NodeType type) {
            super(itemView);
            this.nodeType = type;
            this.cardView = itemView.findViewById(R.id.cardView);
            this.bssid = itemView.findViewById(R.id.bssid);
            this.value = itemView.findViewById(R.id.value);
            this.type = itemView.findViewById(R.id.type);
        }

        void updateView(int values) {
            value.setText(String.valueOf(values));
        }

        void bindView(Node node) {
            type.setText(node.getType());
            bssid.setText(node.getId().toUpperCase());
            value.setText(String.valueOf(node.getValue()));
        }
    }

    public static class MvViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView x;
        TextView y;
        TextView z;

        MvViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            x = itemView.findViewById(R.id.xValue);
            y = itemView.findViewById(R.id.yValue);
            z = itemView.findViewById(R.id.zValue);
        }

        void bindView(float[] values) {
            x.setText(String.format(Locale.getDefault(), "%.1f", values[0]));
            y.setText(String.format(Locale.getDefault(), "%.1f", values[1]));
            z.setText(String.format(Locale.getDefault(), "%.1f", values[2]));
        }
    }
}
