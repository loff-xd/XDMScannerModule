package com.loff.xdmscannermodule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    // CONSTRUCTOR
    Context context;
    ArrayList<Backend.SSCC> ssccs;
    OnSSCCClickedListener listener;

    public ListAdapter(Context context, ArrayList<Backend.SSCC> ssccs, OnSSCCClickedListener listener){
        this.context = context;
        this.ssccs = ssccs;
        this.listener = listener;
    }

    interface OnSSCCClickedListener {
        void onSSCCClicked(Backend.SSCC sscc);
    }

    @NonNull
    @Override
    public ListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.recycler_view_item, parent, false);
        return new ListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListAdapter.ViewHolder holder, int position) {
        holder.tvsscc.setText(ssccs.get(position).ssccID);
        holder.tvsubtext.setText(ssccs.get(position).description);
        holder.tvssccshort.setText(ssccs.get(position).ssccID.substring(ssccs.get(position).ssccID.length() - 4));

        // IMAGE FOR SCANNED STATUS + HR FLAG
        if (ssccs.get(position).scanned) {
            holder.scanned_img.setImageResource(R.drawable.ic_scanned);
        } else {
            holder.scanned_img.setImageResource(R.drawable.ic_unscanned);
        }

        if (ssccs.get(position).highRisk) {
            holder.hr_img.setVisibility(View.VISIBLE);
        } else {
            holder.hr_img.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return ssccs.size();
    }

    // VIEWHOLDER ASSIGNMENT
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        ImageView scanned_img, hr_img;
        TextView tvsscc, tvsubtext, tvssccshort;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // CLICK LISTENERS
            itemView.setOnLongClickListener(this);
            itemView.setOnClickListener(v -> Toast.makeText(v.getContext(), "Long press to open", Toast.LENGTH_SHORT).show());

            // RES ASSIGNMENT
            scanned_img = itemView.findViewById(R.id.img_scanned_status);
            hr_img = itemView.findViewById(R.id.img_highriskmarker);
            tvsscc = itemView.findViewById(R.id.tv_item_sscc);
            tvsubtext = itemView.findViewById(R.id.tv_item_subtext);
            tvssccshort = itemView.findViewById(R.id.tv_sscc_short);
        }

        @Override
        public boolean onLongClick(View v) {
            listener.onSSCCClicked(ssccs.get(getAdapterPosition()));
            return true;
        }
    }
}
