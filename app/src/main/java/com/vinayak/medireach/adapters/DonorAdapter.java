package com.vinayak.medireach.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.vinayak.medireach.R;
import com.vinayak.medireach.models.Donor;

import java.util.List;
import java.util.Locale;

public class DonorAdapter extends RecyclerView.Adapter<DonorAdapter.DonorViewHolder> {

    private final List<Donor> donors;
    private final Context context;

    public DonorAdapter(List<Donor> donors, Context context) {
        this.donors = donors;
        this.context = context;
    }

    @NonNull
    @Override
    public DonorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_donor_card, parent, false);
        return new DonorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonorViewHolder holder, int position) {
        holder.bind(donors.get(position));
    }

    @Override
    public int getItemCount() {
        return donors.size();
    }

    class DonorViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewName;
        private final TextView textViewBloodGroupBadge;
        private final TextView textViewCity;
        private final Button buttonCallDonor;
        private final TextView textViewLastDonationDate;
        private final TextView textViewOrganDonorBadge;
        private final TextView textViewAvailabilityBadge;

        DonorViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewDonorName);
            textViewBloodGroupBadge = itemView.findViewById(R.id.textViewBloodGroupBadge);
            textViewCity = itemView.findViewById(R.id.textViewDonorCity);
            buttonCallDonor = itemView.findViewById(R.id.buttonCallDonor);
            textViewLastDonationDate = itemView.findViewById(R.id.textViewLastDonationDate);
            textViewOrganDonorBadge = itemView.findViewById(R.id.textViewOrganDonorBadge);
            textViewAvailabilityBadge = itemView.findViewById(R.id.textViewAvailabilityBadge);
        }

        void bind(Donor donor) {
            textViewName.setText(valueOrDefault(donor.getFullName(), "Donor"));
            textViewBloodGroupBadge.setText(valueOrDefault(donor.getBloodGroup(), "N/A"));
            textViewCity.setText("City: " + valueOrDefault(donor.getCity(), "N/A"));
            textViewLastDonationDate.setText("Last Donation: " + valueOrDefault(donor.getLastDonationDate(), "N/A"));

            if (donor.isOrganDonor()) {
                textViewOrganDonorBadge.setVisibility(View.VISIBLE);
                textViewOrganDonorBadge.setText("♥ Organ Donor");
            } else {
                textViewOrganDonorBadge.setVisibility(View.GONE);
            }

            textViewAvailabilityBadge.setText("✅ Available");
            textViewAvailabilityBadge.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));

            String phone = valueOrDefault(donor.getPhoneNumber(), donor.getPhone());
            buttonCallDonor.setOnClickListener(v -> {
                if (!TextUtils.isEmpty(phone)) {
                    context.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
                }
            });
        }

        private String valueOrDefault(String value, String fallback) {
            return TextUtils.isEmpty(value) ? fallback : value;
        }
    }
}

