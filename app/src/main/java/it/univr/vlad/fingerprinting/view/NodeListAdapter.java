package it.univr.vlad.fingerprinting.view;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
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

        if (viewType == R.layout.magnetic_vector)
            return new MvViewHolder(view);
        else if (viewType == R.layout.wifi_node)
            return new NodesViewHolder(view, NodeType.WIFI);
        else
            return new NodesViewHolder(view, NodeType.BEACON);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Node node = null;
        if (position == 0 && holder.getItemViewType() == R.layout.magnetic_vector && mv != null) {
            MvViewHolder mvHolder = (MvViewHolder) holder;
            float[] values = mv.getValues();

            mvHolder.x.setText(String.format(Locale.getDefault(), "%.1f", values[0]));
            mvHolder.y.setText(String.format(Locale.getDefault(), "%.1f", values[1]));
            mvHolder.z.setText(String.format(Locale.getDefault(), "%.1f", values[2]));
            return;
        }
        else if (holder.getItemViewType() == R.layout.beacon_node) {
            node = beaconNodes.get(position - 1);
        }
        else if (holder.getItemViewType() == R.layout.wifi_node) {
            node = wifiNodes.get(position - beaconNodes.size() - 1);
        }

        if (node != null) {
            NodesViewHolder nodeHolder = (NodesViewHolder) holder;
            nodeHolder.type.setText(node.getType());
            nodeHolder.bssid.setText(node.getId().toUpperCase());
            nodeHolder.value.setText(String.valueOf(node.getValue()));
        }
    }

     @Override
     public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
         if (payloads.isEmpty()) {
             super.onBindViewHolder(holder, position, payloads);
         }
         else if (holder instanceof NodesViewHolder){
             Bundle o = (Bundle) payloads.get(0);
             NodesViewHolder nodeHolder = (NodesViewHolder) holder;
             for (String key : o.keySet()) {
                 if (key.equals("value")) {
                     nodeHolder.value.setText(String.valueOf(o.getInt("value")));
                 }
             }
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
        if (nodeType == NodeType.WIFI) oldNodes.addAll(wifiNodes);
        else oldNodes.addAll(beaconNodes);

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

    // TODO: fix items position in recycler view
    private void dispatchUpdates(NodeType nodeType, List<Node> newNodes, DiffUtil.DiffResult diffResult) {
        diffResult.dispatchUpdatesTo(this);
        if (nodeType == NodeType.WIFI) {
            wifiNodes.clear();
            wifiNodes.addAll(newNodes);
        } else {
            beaconNodes.clear();
            beaconNodes.addAll(newNodes);
        }

        diffResult.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition, toPosition);
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                notifyItemRangeChanged(position, count, payload);
            }
        });
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
        handler.post(() -> notifyItemChanged(0));
    }

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
    }
}
