package com.vinayak.medireach.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.vinayak.medireach.HospitalDetailActivity;
import com.vinayak.medireach.R;
import com.vinayak.medireach.models.Hospital;
import com.vinayak.medireach.utils.LocationUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying hospital information cards.
 * Shows hospital resources with color coding based on availability.
 */
public class HospitalCardAdapter extends RecyclerView.Adapter<HospitalCardAdapter.HospitalViewHolder> {

    private List<Hospital> hospitals;
    private final Context context;
    private double userLatitude;
    private double userLongitude;
    private boolean hasUserLocation;

    /**
     * Constructor for HospitalCardAdapter.
     *
     * @param hospitals       List of hospitals to display
     * @param context         The application context
     * @param userLatitude    User's current latitude
     * @param userLongitude   User's current longitude
     * @param hasUserLocation Flag indicating if the user location is available
     */
    public HospitalCardAdapter(List<Hospital> hospitals, Context context, double userLatitude, double userLongitude, boolean hasUserLocation) {
        this.hospitals = hospitals;
        this.context = context;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
        this.hasUserLocation = hasUserLocation;
    }

    /**
     * Updates the user location and refreshes the displayed data.
     *
     * @param latitude           New latitude of the user
     * @param longitude          New longitude of the user
     * @param locationAvailable  Flag indicating if the location is available
     */
    public void updateUserLocation(double latitude, double longitude, boolean locationAvailable) {
        this.userLatitude = latitude;
        this.userLongitude = longitude;
        this.hasUserLocation = locationAvailable;
        notifyDataSetChanged();
    }

    /**
     * Updates the hospital list and user coordinates for distance calculations.
     *
     * @param newList    New list of hospitals to display
     * @param userLat    User's latitude
     * @param userLon    User's longitude
     */
    public void updateList(List<Hospital> newList, double userLat, double userLon) {
        this.hospitals = newList;
        this.userLatitude = userLat;
        this.userLongitude = userLon;
        this.hasUserLocation = !(userLat == 0.0 && userLon == 0.0);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HospitalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hospital_card, parent, false);
        return new HospitalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HospitalViewHolder holder, int position) {
        holder.bind(hospitals.get(position));
    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

    /**
     * ViewHolder for hospital card items.
     */
    class HospitalViewHolder extends RecyclerView.ViewHolder {

        private final TextView textViewHospitalName;
        private final TextView textViewDistance;
        private final TextView textViewStatus;
        private final TextView textViewIcuBeds;
        private final ImageView imageViewIcuIndicator;
        private final TextView textViewOxygen;
        private final ImageView imageViewOxygenIndicator;
        private final TextView textViewVentilator;
        private final ImageView imageViewVentilatorIndicator;
        private final TextView textViewEmergencyContact;
        private final TextView textViewLastUpdated;
        private final ImageView imageViewCall;
        private final Button buttonDirections;
        private final LinearLayout cardContainer;

        /**
         * Constructor for HospitalViewHolder.
         *
         * @param itemView The view for a single hospital card
         */
        HospitalViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewHospitalName = itemView.findViewById(R.id.textViewHospitalName);
            textViewDistance = itemView.findViewById(R.id.textViewDistance);
            textViewStatus = itemView.findViewById(R.id.textViewStatusBadge);
            textViewIcuBeds = itemView.findViewById(R.id.textViewIcuBeds);
            imageViewIcuIndicator = itemView.findViewById(R.id.imageViewIcuIndicator);
            textViewOxygen = itemView.findViewById(R.id.textViewOxygenCylinders);
            imageViewOxygenIndicator = itemView.findViewById(R.id.imageViewOxygenIndicator);
            textViewVentilator = itemView.findViewById(R.id.textViewVentilators);
            imageViewVentilatorIndicator = itemView.findViewById(R.id.imageViewVentilatorsIndicator);
            textViewEmergencyContact = itemView.findViewById(R.id.textViewEmergencyContact);
            textViewLastUpdated = itemView.findViewById(R.id.textViewLastUpdated);
            imageViewCall = itemView.findViewById(R.id.imageViewCallIcon);
            buttonDirections = itemView.findViewById(R.id.buttonDirections);
            cardContainer = itemView.findViewById(R.id.linearLayoutCard);
        }

        /**
         * Binds the hospital data to the view.
         *
         * @param hospital The hospital to display
         */
        void bind(Hospital hospital) {
            textViewHospitalName.setText(defaultValue(hospital.getHospitalName(), "Hospital"));

            // Always calculate and display distance
            if (hasUserLocation && userLatitude != 0.0 && userLongitude != 0.0) {
                double distance = LocationUtils.calculateDistance(userLatitude, userLongitude, hospital.getLatitude(), hospital.getLongitude());
                textViewDistance.setText(formatDistance(distance));
            } else {
                textViewDistance.setText("📍 Distance unavailable");
            }

            int icu = hospital.getIcuBeds();
            int oxygen = hospital.getOxygenCylinders();
            int ventilator = hospital.getVentilators();
            textViewIcuBeds.setText(String.valueOf(icu));
            textViewOxygen.setText(String.valueOf(oxygen));
            textViewVentilator.setText(String.valueOf(ventilator));

            setResourceColor(imageViewIcuIndicator, icu);
            setResourceColor(imageViewOxygenIndicator, oxygen);
            setResourceColor(imageViewVentilatorIndicator, ventilator);
            applyStatusBadge(hospital);

            textViewEmergencyContact.setText(defaultValue(hospital.getEmergencyContact(), "N/A"));

            if (hospital.getLastUpdated() != null) {
                String date = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(hospital.getLastUpdated());
                textViewLastUpdated.setText("Updated: " + date);
            } else {
                textViewLastUpdated.setText("Updated: N/A");
            }

            imageViewCall.setOnClickListener(v -> openDialer(hospital.getEmergencyContact()));
            textViewEmergencyContact.setOnClickListener(v -> openDialer(hospital.getEmergencyContact()));
            buttonDirections.setOnClickListener(v -> openDirections(hospital.getLatitude(), hospital.getLongitude()));
            cardContainer.setOnClickListener(v -> openHospitalDetails(hospital.getHospitalId()));
        }

        /**
         * Applies the status badge (FULL, LIMITED, OPEN) based on hospital resources.
         *
         * @param hospital The hospital object
         */
        private void applyStatusBadge(Hospital hospital) {
            int total = hospital.getIcuBeds() + hospital.getOxygenCylinders() + hospital.getVentilators();
            if (total <= 0) {
                textViewStatus.setText("FULL");
                textViewStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                return;
            }
            int limitedCount = 0;
            if (hospital.getIcuBeds() < 3) {
                limitedCount++;
            }
            if (hospital.getOxygenCylinders() < 3) {
                limitedCount++;
            }
            if (hospital.getVentilators() < 3) {
                limitedCount++;
            }
            if (limitedCount >= 2) {
                textViewStatus.setText("LIMITED");
                textViewStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
            } else {
                textViewStatus.setText("OPEN");
                textViewStatus.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            }
        }

        /**
         * Sets the color indicator based on resource count.
         * Green (count > 3), Yellow (1-3), Red (0)
         *
         * @param imageView The indicator view
         * @param count     The resource count
         */
        private void setResourceColor(ImageView imageView, int count) {
            int color;
            if (count > 3) {
                color = ContextCompat.getColor(context, android.R.color.holo_green_dark);
            } else if (count >= 1) {
                color = ContextCompat.getColor(context, android.R.color.holo_orange_dark);
            } else {
                color = ContextCompat.getColor(context, android.R.color.holo_red_dark);
            }
            imageView.setColorFilter(color);
        }

        /**
         * Opens the phone dialer with the emergency contact number.
         *
         * @param phoneNumber The phone number to call
         */
        private void openDialer(String phoneNumber) {
            if (TextUtils.isEmpty(phoneNumber)) {
                return;
            }
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
            context.startActivity(dialIntent);
        }

        /**
         * Opens the directions in Google Maps or browser.
         *
         * @param latitude  The latitude of the destination
         * @param longitude The longitude of the destination
         */
        private void openDirections(double latitude, double longitude) {
            String navigation = "google.navigation:q=" + latitude + "," + longitude;
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(navigation));
            mapIntent.setPackage("com.google.android.apps.maps");
            try {
                context.startActivity(mapIntent);
            } catch (Exception e) {
                String browserMap = String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%f,%f", latitude, longitude);
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(browserMap)));
            }
        }

        /**
         * Opens HospitalDetailActivity with the selected hospital ID.
         *
         * @param hospitalId The ID of the selected hospital
         */
        private void openHospitalDetails(String hospitalId) {
            Intent intent = new Intent(context, HospitalDetailActivity.class);
            intent.putExtra("hospitalId", hospitalId);
            context.startActivity(intent);
        }

        /**
         * Returns a default value if the given value is empty.
         *
         * @param value    The value to check
         * @param fallback The fallback value
         * @return The original value or the fallback value
         */
        private String defaultValue(String value, String fallback) {
            return TextUtils.isEmpty(value) ? fallback : value;
        }

        /**
         * Formats distance in kilometers to a readable string.
         * Shows meters for distances < 1 km, and kilometers for >= 1 km.
         *
         * @param distanceKm Distance in kilometers
         * @return Formatted distance string with emoji and unit
         */
        private String formatDistance(double distanceKm) {
            if (distanceKm < 1.0) {
                int meters = (int)(distanceKm * 1000);
                return "📍 " + meters + " m away";
            } else {
                return "📍 " + String.format("%.1f", distanceKm) + " km away";
            }
        }
    }
}

