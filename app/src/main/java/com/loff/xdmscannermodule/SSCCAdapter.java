package com.loff.xdmscannermodule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SSCCAdapter extends RecyclerView.Adapter<SSCCAdapter.viewHolder> {
    private ArrayList<SSCCCard> mSSCCList;

    public static class viewHolder extends RecyclerView.ViewHolder{
        public ImageView imageView;
        public TextView ssccLastFour;
        public TextView ssccFull;
        public TextView ssccDetails;
        public boolean isHR;

        public viewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.card_image);
            ssccLastFour = itemView.findViewById(R.id.card_text_lastfour);
            ssccFull = itemView.findViewById(R.id.card_text_sscc_full);
            ssccDetails = itemView.findViewById(R.id.card_text_details);
        }
    }

    public SSCCAdapter(ArrayList<SSCCCard> SSCCList) {
        mSSCCList = SSCCList;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.sscc_card, parent, false);
        viewHolder vh = new viewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        SSCCCard currentCard = mSSCCList.get(position);

        holder.imageView.setImageResource(currentCard.getImage());
        holder.ssccLastFour.setText(currentCard.getLastFour());
        holder.ssccFull.setText(currentCard.getSSCC());
        holder.ssccDetails.setText(currentCard.getDetails());

        if (currentCard.getIsHR()) {
            //TODO
        }
    }

    @Override
    public int getItemCount() {
        return mSSCCList.size();
    }
}
