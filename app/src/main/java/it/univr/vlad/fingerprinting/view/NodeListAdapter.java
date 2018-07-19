package it.univr.vlad.fingerprinting.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.univr.vlad.fingerprinting.Node;
import it.univr.vlad.fingerprinting.diffutil.NodesDiffCallback;
import it.univr.vlad.fingerprinting.R;
import it.univr.vlad.fingerprinting.mv.MagneticVector;

public class NodeListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int wifiCardBackground;
    private final int beaconCardBackground;
    private final int mvCardBackground;

    private MagneticVector mv;
    private List<Node> nodes = new ArrayList<>();

    NodeListAdapter(Context context) {
        wifiCardBackground = ContextCompat.getColor(context, R.color.cardWifiBackground);
        beaconCardBackground = ContextCompat.getColor(context, R.color.cardBeaconBackground);
        mvCardBackground = ContextCompat.getColor(context, R.color.cardMvBackground);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(viewType, parent, false);

        if (viewType == R.layout.magnetic_vector)
            return new MvViewHolder(view);
        else
            return new NodesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position == 0 && holder instanceof MvViewHolder && mv != null) {
            MvViewHolder mvHolder = (MvViewHolder) holder;

            mvHolder.x.setText(String.format(Locale.ITALY, "%.1f", mv.getValues()[0]));
            mvHolder.y.setText(String.format(Locale.ITALY, "%.1f", mv.getValues()[1]));
            mvHolder.z.setText(String.format(Locale.ITALY, "%.1f", mv.getValues()[2]));
            mvHolder.cardView.setCardBackgroundColor(mvCardBackground);
        }
        else if (holder.getItemViewType() == R.layout.single_node && nodes != null) {
            if (position != 1 && position < nodes.size()) {
                Node node = nodes.get(position);
                NodesViewHolder nodeHolder = (NodesViewHolder) holder;

                nodeHolder.type.setText(node.getType());
                nodeHolder.bssid.setText(node.getId());
                nodeHolder.value.setText(String.valueOf(node.getValue()));

                switch (node.getType()) {
                    case "WIFI":
                        nodeHolder.cardView.setCardBackgroundColor(wifiCardBackground);
                        break;
                    case "BLE":
                        nodeHolder.cardView.setCardBackgroundColor(beaconCardBackground);
                        break;
                    default:
                        break;
                }
            }
        }
    }

   /* @Override
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
*/
    public void setNodes(List<Node> newNodes) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new NodesDiffCallback(this.nodes, newNodes), true);
        this.nodes.clear();
        this.nodes.addAll(newNodes);
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
    }

    public void setMv(MagneticVector mv) {
        this.mv = mv;
        notifyItemChanged(0);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return R.layout.magnetic_vector;
        else
            return R.layout.single_node;
    }

    @Override
    public int getItemCount() {
        return nodes.size() + 2;
    }

    public static class NodesViewHolder extends RecyclerView.ViewHolder {

        CardView cardView;
        TextView bssid;
        TextView value;
        TextView type;

        NodesViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            bssid = itemView.findViewById(R.id.bssid);
            value = itemView.findViewById(R.id.value);
            type = itemView.findViewById(R.id.type);
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
