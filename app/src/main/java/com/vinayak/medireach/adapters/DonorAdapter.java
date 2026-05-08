package com.vinayak.medireach.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vinayak.medireach.R;
import com.vinayak.medireach.models.Donor;

import java.util.List;

public class DonorAdapter extends RecyclerView.Adapter<DonorAdapter.DonorViewHolder> {

    private List<Donor> donorList;
    private Context context;

    public DonorAdapter(List<Donor> donorList, Context context) {
        this.donorList = donorList;
        this.context = context;
    }

    @NonNull
    @Override
    public DonorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_donor_card, parent, false);
        return new DonorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonorViewHolder holder, int position) {
        Donor donor = donorList.get(position);
        holder.textViewName.setText(donor.getFullName());
        holder.textViewBloodGroup.setText(donor.getBloodGroup());
        holder.textViewCity.setText("City: " + donor.getCity());
        holder.textViewPhone.setText("Phone: " + donor.getPhone());
        holder.textViewAvailability.setText("Status: " + (donor.getIsAvailable() ? "Available" : "Unavailable"));

        holder.buttonCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + donor.getPhone()));
            context.startActivity(intent);
        });

        holder.buttonMessage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + donor.getPhone()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return donorList.size();
    }

    public void updateList(List<Donor> newList) {
        this.donorList = newList;
        notifyDataSetChanged();
    }

    public static class DonorViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewBloodGroup, textViewCity, textViewPhone, textViewAvailability;
        Button buttonCall, buttonMessage;

        public DonorViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewDonorName);
            textViewBloodGroup = itemView.findViewById(R.id.textViewBloodGroup);
            textViewCity = itemView.findViewById(R.id.textViewCity);
            textViewPhone = itemView.findViewById(R.id.textViewPhone);
            textViewAvailability = itemView.findViewById(R.id.textViewAvailability);
            buttonCall = itemView.findViewById(R.id.buttonCallDonor);
            buttonMessage = itemView.findViewById(R.id.buttonMessageDonor);
        }
    }
}
