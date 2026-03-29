package com.vinayak.medireach;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.vinayak.medireach.adapters.HospitalCardAdapter;
import com.vinayak.medireach.models.Hospital;
import com.vinayak.medireach.utils.LocationUtils;
import com.vinayak.medireach.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PatientDashboardActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final long LOCATION_LOADING_TIMEOUT_MS = 8_000L;

    private RecyclerView recyclerViewHospitals;
    private TextView textViewEmptyState;
    private TextView locationStatusBar;
    private TextView textViewManualMode;
    private TextView textViewLastRefreshed;
    private TextView textViewLoadingStatus;
    private EditText editTextSearch;
    private Spinner spinnerSort;
    private ProgressBar progressLocation;
    private ProgressBar progressData;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View buttonManualLocation;
    private View buttonEmergencyFab;
    private View buttonDebugShowAllHospitals;
    private ImageView imageViewLogout;
    private ImageView imageViewProfile;
    private Chip chipAll;
    private Chip chipIcu;
    private Chip chipOxygen;
    private Chip chipBlood;
    private Chip chipVentilator;

    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private ListenerRegistration hospitalsListener;
    private LocationCallback locationCallback;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Hospital> hospitalList = new ArrayList<>();
    private final List<Hospital> filteredList = new ArrayList<>();

    private HospitalCardAdapter hospitalCardAdapter;

    private double userLatitude = 0.0;
    private double userLongitude = 0.0;
    private boolean manualMode = false;
    private String manualQuery = "";
    private String selectedResourceFilter = "all";
    private long lastRefreshMs = 0L;

    private boolean locationReady = false;
    private boolean hospitalsLoaded = false;
    private boolean locationTimedOut = false;
    private boolean locationDenied = false;
    private boolean firstLocationReceived = false;

    private final Runnable locationTimeoutRunnable = () -> {
        if (!locationReady) {
            locationTimedOut = true;
            locationStatusBar.setText("Showing all hospitals - location loading...");
            showLocationLoading(false);
            tryDisplayHospitals();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_dashboard);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        initRecycler();
        initListeners();
        initBackPress();

        showDataLoading(true);
        showLocationLoading(true);
        textViewLoadingStatus.setVisibility(View.VISIBLE);
        textViewLoadingStatus.setText("Finding hospitals near you...");
        locationStatusBar.setText("📍 Detecting your location...");

        startHospitalsListener();
        checkAndRequestLocationPermission();
        handler.postDelayed(locationTimeoutRunnable, LOCATION_LOADING_TIMEOUT_MS);
    }

    private void initViews() {
        recyclerViewHospitals = findViewById(R.id.recyclerViewHospitals);
        textViewEmptyState = findViewById(R.id.textViewEmptyState);
        locationStatusBar = findViewById(R.id.locationStatusBar);
        textViewManualMode = findViewById(R.id.textViewManualMode);
        textViewLastRefreshed = findViewById(R.id.textViewLastRefreshed);
        textViewLoadingStatus = findViewById(R.id.textViewLoadingStatus);
        editTextSearch = findViewById(R.id.editTextSearchHospital);
        spinnerSort = findViewById(R.id.spinnerSort);
        progressLocation = findViewById(R.id.progressLocation);
        progressData = findViewById(R.id.progressData);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        buttonManualLocation = findViewById(R.id.buttonManualLocation);
        buttonEmergencyFab = findViewById(R.id.fabEmergency);
        buttonDebugShowAllHospitals = findViewById(R.id.buttonDebugShowAllHospitals);
        imageViewLogout = findViewById(R.id.imageViewLogout);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        chipAll = findViewById(R.id.chipAll);
        chipIcu = findViewById(R.id.chipIcu);
        chipOxygen = findViewById(R.id.chipOxygen);
        chipBlood = findViewById(R.id.chipBlood);
        chipVentilator = findViewById(R.id.chipVentilator);

        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Sort by Distance", "Sort by Availability"}
        );
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);
        textViewLastRefreshed.setText("Last refreshed: just now");
    }

    private void initRecycler() {
        recyclerViewHospitals.setLayoutManager(new LinearLayoutManager(this));
        hospitalCardAdapter = new HospitalCardAdapter(filteredList, this, userLatitude, userLongitude, false);
        recyclerViewHospitals.setAdapter(hospitalCardAdapter);
    }

    private void initListeners() {
        imageViewLogout.setOnClickListener(v -> showLogoutDialog());
        imageViewProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));

        locationStatusBar.setOnClickListener(v -> {
            if (locationDenied) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        });

        buttonManualLocation.setOnClickListener(v -> showManualLocationDialog());
        buttonEmergencyFab.setOnClickListener(v -> showEmergencyActions());
        buttonDebugShowAllHospitals.setOnClickListener(v -> fetchAllHospitalsWithoutFilter());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            textViewLoadingStatus.setVisibility(View.VISIBLE);
            textViewLoadingStatus.setText("Refreshing hospitals...");
            refreshHospitalsOnce();
        });

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndDisplayHospitals();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipAll.setOnClickListener(v -> setResourceFilter("all"));
        chipIcu.setOnClickListener(v -> setResourceFilter("icu"));
        chipOxygen.setOnClickListener(v -> setResourceFilter("oxygen"));
        chipBlood.setOnClickListener(v -> setResourceFilter("blood"));
        chipVentilator.setOnClickListener(v -> setResourceFilter("ventilator"));

        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterAndDisplayHospitals();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void initBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(PatientDashboardActivity.this)
                        .setTitle("Exit")
                        .setMessage("Do you want to exit the app?")
                        .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                        .setNegativeButton("Stay", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });
    }

    private void setResourceFilter(String filter) {
        selectedResourceFilter = filter;
        chipAll.setChecked("all".equals(filter));
        chipIcu.setChecked("icu".equals(filter));
        chipOxygen.setChecked("oxygen".equals(filter));
        chipBlood.setChecked("blood".equals(filter));
        chipVentilator.setChecked("ventilator".equals(filter));
        filterAndDisplayHospitals();
    }

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted
            onLocationPermissionGranted();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Show explanation dialog first
            showLocationPermissionRationaleDialog();
        } else {
            // Request permission directly
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, LOCATION_REQUEST_CODE);
        }
    }

    private void showLocationPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("📍 Location Permission Needed")
                .setMessage("MediReach needs your location to:\n\n"
                        + "• Sort hospitals from nearest to farthest\n"
                        + "• Show your exact distance to each hospital\n"
                        + "• Help you find the closest available resources\n\n"
                        + "Please allow location access on the next screen.")
                .setPositiveButton("Allow Location", (dialog, which) -> {
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            }, LOCATION_REQUEST_CODE);
                })
                .setNegativeButton("Skip for Now", (dialog, which) -> {
                    onLocationPermissionDenied();
                })
                .setCancelable(false)
                .show();
    }

    private void onLocationPermissionGranted() {
        locationStatusBar.setText("📍 Detecting your location...");
        locationStatusBar.setBackgroundColor(
                getResources().getColor(android.R.color.transparent, getTheme()));
        startLiveLocationUpdates();
        fetchAllHospitals();
    }

    private void onLocationPermissionDenied() {
        locationStatusBar.setText(
                "📍 Location unavailable — Showing all hospitals");
        locationStatusBar.setBackgroundColor(
                getResources().getColor(android.R.color.transparent, getTheme()));
        userLatitude = 0.0;
        userLongitude = 0.0;
        fetchAllHospitals();
    }

    private void onLocationPermissionPermanentlyDenied() {
        locationStatusBar.setText(
                "📍 Location blocked — Tap here to enable in Settings");
        locationStatusBar.setBackgroundColor(
                getResources().getColor(android.R.color.transparent, getTheme()));
        locationStatusBar.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });
        fetchAllHospitals();
    }

    private void requestLocationPermissionFlow() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 1001);
        } else {
            startLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLiveLocationUpdates() {
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 15000)
                .setMinUpdateIntervalMillis(10000)
                .setMaxUpdateDelayMillis(20000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                Location location = result.getLastLocation();
                if (location != null) {
                    double newLat = location.getLatitude();
                    double newLon = location.getLongitude();

                    // Only update if location changed significantly
                    double change = LocationUtils.calculateDistance(
                            userLatitude, userLongitude, newLat, newLon);

                    userLatitude = newLat;
                    userLongitude = newLon;
                    locationReady = true;

                    // Update location status bar
                    locationStatusBar.setText("📍 Live: "
                            + String.format("%.4f", userLatitude)
                            + ", "
                            + String.format("%.4f", userLongitude));
                    locationStatusBar.setBackgroundColor(
                            getResources().getColor(android.R.color.transparent, getTheme()));

                    // Re-sort hospitals with new location
                    filterAndDisplayHospitals();

                    // Show brief update message if moved
                    if (change > 0.1) {
                        Snackbar.make(recyclerViewHospitals,
                                "📍 Location updated — List refreshed",
                                Snackbar.LENGTH_SHORT).show();
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback,
                    Looper.getMainLooper());

            // Also get last known location immediately for faster
            // initial display
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            userLatitude = location.getLatitude();
                            userLongitude = location.getLongitude();
                            locationReady = true;
                            filterAndDisplayHospitals();
                        }
                    });
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        Log.d("MediReach_Location", "GPS started");

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L)
                .setMinUpdateIntervalMillis(10_000L)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    userLatitude = location.getLatitude();
                    userLongitude = location.getLongitude();
                    locationReady = true;
                    locationDenied = false;
                    locationTimedOut = false;
                    manualMode = false;
                    textViewManualMode.setVisibility(View.GONE);

                    locationStatusBar.setText("Your location: "
                            + String.format(Locale.US, "%.4f", userLatitude)
                            + ", "
                            + String.format(Locale.US, "%.4f", userLongitude));

                    Log.d("MediReach", "User location: " + userLatitude + ", " + userLongitude);
                    Log.d("MediReach_Location", "Lat: " + userLatitude + " Lon: " + userLongitude);

                    showLocationLoading(false);
                    hospitalCardAdapter.updateUserLocation(userLatitude, userLongitude, true);
                    if (firstLocationReceived) {
                        Snackbar.make(findViewById(android.R.id.content), "Location updated", Snackbar.LENGTH_SHORT).show();
                    }
                    firstLocationReceived = true;
                    tryDisplayHospitals();
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback,
                    Looper.getMainLooper());
        }
    }

    private void startHospitalsListener() {
        if (hospitalsListener != null) {
            hospitalsListener.remove();
        }

        hospitalsListener = db.collection("hospitals")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        swipeRefreshLayout.setRefreshing(false);
                        showDataLoading(false);
                        return;
                    }
                    hospitalList.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Hospital hospital = doc.toObject(Hospital.class);
                            if (hospital != null) {
                                hospital.setHospitalId(doc.getId());
                                hospitalList.add(hospital);
                            }
                        }
                    }
                    hospitalsLoaded = true;
                    lastRefreshMs = System.currentTimeMillis();
                    updateLastRefreshedLabel();
                    Log.d("MediReach_Hospitals", "Total fetched from Firestore: " + hospitalList.size());
                    tryDisplayHospitals();
                });
    }

    private void fetchAllHospitals() {
        showDataLoading(true);
        textViewLoadingStatus.setVisibility(View.VISIBLE);
        textViewLoadingStatus.setText("Loading all hospitals...");
    }

    private void refreshHospitalsOnce() {
        db.collection("hospitals")
                .get()
                .addOnSuccessListener(snapshots -> {
                    hospitalList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Hospital hospital = doc.toObject(Hospital.class);
                        if (hospital != null) {
                            hospital.setHospitalId(doc.getId());
                            hospitalList.add(hospital);
                        }
                    }
                    hospitalsLoaded = true;
                    lastRefreshMs = System.currentTimeMillis();
                    updateLastRefreshedLabel();
                    tryDisplayHospitals();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to refresh hospitals", Toast.LENGTH_SHORT).show())
                .addOnCompleteListener(task -> swipeRefreshLayout.setRefreshing(false));
    }

    private void tryDisplayHospitals() {
        if (!hospitalsLoaded) {
            return;
        }

        if (locationReady) {
            filterAndDisplayHospitals();
            return;
        }

        if (locationDenied || locationTimedOut) {
            manualMode = true;
            filterAndDisplayHospitals();
        }
    }

    private void filterAndDisplayHospitals() {
        String query = editTextSearch.getText() == null ? "" : editTextSearch.getText().toString().trim().toLowerCase(Locale.US);
        filteredList.clear();

        Log.d("MediReach", "User location: " + userLatitude + ", " + userLongitude);

        // Apply filters but NO 10km distance restriction
        for (Hospital hospital : hospitalList) {
            // Apply search filter
            if (!matchesSearch(hospital, query)) {
                continue;
            }
            // Apply resource filter
            if (!matchesResourceFilter(hospital)) {
                continue;
            }
            // Add ALL hospitals that pass filters (no distance limit)
            filteredList.add(hospital);
        }

        Log.d("MediReach_Hospitals", "After filtering (no 10km limit): " + filteredList.size());

        // Sort by distance if location is available
        if (userLatitude != 0.0 || userLongitude != 0.0) {
            Collections.sort(filteredList, (h1, h2) -> {
                double d1 = LocationUtils.calculateDistance(
                        userLatitude, userLongitude,
                        h1.getLatitude(), h1.getLongitude());
                double d2 = LocationUtils.calculateDistance(
                        userLatitude, userLongitude,
                        h2.getLatitude(), h2.getLongitude());
                return Double.compare(d1, d2);
            });
        }

        // Update adapter with coordinates
        hospitalCardAdapter.updateList(filteredList, userLatitude, userLongitude);
        hospitalCardAdapter.notifyDataSetChanged();

        showDataLoading(false);
        textViewLoadingStatus.setVisibility(View.GONE);

        if (filteredList.isEmpty()) {
            textViewEmptyState.setVisibility(View.VISIBLE);
            recyclerViewHospitals.setVisibility(View.GONE);
            Log.d("MediReach", "No hospitals found. Total fetched: " + hospitalList.size());
        } else {
            textViewEmptyState.setVisibility(View.GONE);
            recyclerViewHospitals.setVisibility(View.VISIBLE);
            Log.d("MediReach", "Showing hospitals: " + filteredList.size());
        }

        if (manualMode) {
            textViewManualMode.setVisibility(View.VISIBLE);
            String note;
            if (locationDenied) {
                note = "Location unavailable - showing all hospitals";
            } else if (!TextUtils.isEmpty(manualQuery)) {
                note = "Showing hospitals near " + manualQuery + "\nGPS unavailable - showing all hospitals";
            } else {
                note = "GPS unavailable - showing all hospitals";
            }
            textViewManualMode.setText(note);
        } else {
            textViewManualMode.setVisibility(View.GONE);
        }
    }

    private boolean matchesSearch(Hospital hospital, String query) {
        if (TextUtils.isEmpty(query)) {
            return true;
        }
        String name = hospital.getHospitalName() == null ? "" : hospital.getHospitalName().toLowerCase(Locale.US);
        return name.contains(query);
    }

    private boolean matchesResourceFilter(Hospital hospital) {
        switch (selectedResourceFilter) {
            case "icu":
                return hospital.getIcuBeds() > 0;
            case "oxygen":
                return hospital.getOxygenCylinders() > 0;
            case "blood":
                return hasBloodUnits(hospital.getBloodUnits());
            case "ventilator":
                return hospital.getVentilators() > 0;
            default:
                return true;
        }
    }

    private boolean hasBloodUnits(Map<String, Integer> bloodUnits) {
        if (bloodUnits == null) {
            return false;
        }
        for (Integer value : bloodUnits.values()) {
            if (value != null && value > 0) {
                return true;
            }
        }
        return false;
    }

    private void sortHospitals(List<Hospital> hospitals) {
        String sortType = spinnerSort.getSelectedItem() == null ? "Sort by Distance" : spinnerSort.getSelectedItem().toString();
        if ("Sort by Availability".equals(sortType)) {
            Collections.sort(hospitals, Comparator.comparingInt(this::availabilityScore).reversed());
            return;
        }
        Collections.sort(hospitals, (h1, h2) -> {
            if (!locationReady || (userLatitude == 0.0 && userLongitude == 0.0)) {
                return 0;
            }
            double d1 = LocationUtils.calculateDistance(userLatitude, userLongitude, h1.getLatitude(), h1.getLongitude());
            double d2 = LocationUtils.calculateDistance(userLatitude, userLongitude, h2.getLatitude(), h2.getLongitude());
            return Double.compare(d1, d2);
        });
    }

    private int availabilityScore(Hospital hospital) {
        int blood = 0;
        if (hospital.getBloodUnits() != null) {
            for (Integer value : hospital.getBloodUnits().values()) {
                blood += value == null ? 0 : value;
            }
        }
        return hospital.getIcuBeds() + hospital.getOxygenCylinders() + hospital.getVentilators() + blood;
    }

    private void updateLastRefreshedLabel() {
        if (lastRefreshMs <= 0) {
            return;
        }
        long diffMinutes = Math.max(0L, (System.currentTimeMillis() - lastRefreshMs) / 60000L);
        if (diffMinutes == 0) {
            textViewLastRefreshed.setText("Last refreshed: just now");
        } else {
            textViewLastRefreshed.setText("Last refreshed: " + diffMinutes + " minutes ago");
        }
    }

    private void showLocationLoading(boolean show) {
        progressLocation.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showDataLoading(boolean show) {
        progressData.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> SessionManager.logoutAndOpenLogin(this))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showManualLocationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_location, null, false);
        EditText editTextManual = dialogView.findViewById(R.id.editTextManualLocation);

        new AlertDialog.Builder(this)
                .setTitle("Enter Location Manually")
                .setView(dialogView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    manualMode = true;
                    locationDenied = true;
                    manualQuery = editTextManual.getText() == null ? "" : editTextManual.getText().toString().trim();
                    locationStatusBar.setText("Location unavailable - showing all hospitals");
                    tryDisplayHospitals();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showEmergencyActions() {
        String[] options = {
                "Call 108 - Ambulance",
                "Call 102 - Medical Helpline",
                "Call nearest hospital"
        };
        new AlertDialog.Builder(this)
                .setTitle("Emergency Quick Call")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openDialer("108");
                    } else if (which == 1) {
                        openDialer("102");
                    } else {
                        callNearestHospital();
                    }
                })
                .show();
    }

    private void callNearestHospital() {
        if (filteredList.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), "No nearby hospital available", Snackbar.LENGTH_SHORT).show();
            return;
        }
        Hospital nearest = filteredList.get(0);
        openDialer(nearest.getEmergencyContact());
    }

    private void openDialer(String number) {
        if (TextUtils.isEmpty(number)) {
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
    }

    private void fetchAllHospitalsWithoutFilter() {
        db.collection("hospitals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    Toast.makeText(this, "Total hospitals in database: " + count, Toast.LENGTH_LONG).show();
                    hospitalList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Hospital hospital = doc.toObject(Hospital.class);
                        if (hospital != null) {
                            hospital.setHospitalId(doc.getId());
                            hospitalList.add(hospital);
                        }
                    }
                    manualMode = true;
                    hospitalsLoaded = true;
                    tryDisplayHospitals();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch hospitals", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onLocationPermissionGranted();
            } else {
                // Check if permanently denied
                if (!ActivityCompat
                        .shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    onLocationPermissionPermanentlyDenied();
                } else {
                    onLocationPermissionDenied();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hospitalsListener != null) {
            hospitalsListener.remove();
        }
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        handler.removeCallbacksAndMessages(null);
    }
}
