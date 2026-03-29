# MediReach Developer Quick Reference

## 🚀 Quick Start

### Build the Project
```bash
cd C:\Users\Dell\AndroidStudioProjects\MediReach
.\gradlew assembleDebug
```

**Output**: `app/build/outputs/apk/debug/app-debug.apk` (9.56 MB)

### Run Tests
```bash
.\gradlew test
.\gradlew connectedAndroidTest
```

---

## 🔑 Key Classes & Methods

### SessionManager - Role & Session Management
**Location**: `com.vinayak.medireach.utils.SessionManager`

**Key Methods**:
```java
// Role normalization (converts variants to standard format)
normalizeRole(String role) → "patient" | "hospital_admin" | "donor"

// Get cached role from SharedPreferences
getSavedRole(Context context) → String

// Save role to cache
saveRole(Context context, String role) → void

// Get Intent for role-based navigation
intentForRole(Context context, String role) → Intent

// Complete logout procedure
logoutAndOpenLogin(Activity activity) → void

// Hospital setup gate checks
isHospitalSetupDone(Context context) → boolean
setHospitalSetupDone(Context context, boolean done) → void
```

**Usage Example**:
```java
// Navigate user to correct dashboard
String role = SessionManager.getSavedRole(this);
if (role != null) {
    Intent intent = SessionManager.intentForRole(this, role);
    startActivity(intent);
    finish();
}

// Logout everywhere
SessionManager.logoutAndOpenLogin(this);
```

---

### LocationUtils - Distance Calculations
**Location**: `com.vinayak.medireach.utils.LocationUtils`

**Key Methods**:
```java
// Calculate distance between two coordinates (Haversine formula)
calculateDistance(double lat1, double lon1, double lat2, double lon2) → double (kilometers)
```

**Usage Example**:
```java
double distanceKm = LocationUtils.calculateDistance(
    userLatitude, userLongitude,
    hospitalLatitude, hospitalLongitude
);

// Format for display
String distance = distanceKm < 1.0 
    ? (int)(distanceKm * 1000) + " m away"
    : String.format("%.1f km away", distanceKm);
```

---

### HospitalCardAdapter - Hospital List Rendering
**Location**: `com.vinayak.medireach.adapters.HospitalCardAdapter`

**Key Methods**:
```java
// Constructor
HospitalCardAdapter(
    List<Hospital> hospitals, 
    Context context,
    double userLatitude, 
    double userLongitude, 
    boolean hasUserLocation
)

// Update hospital list and user coordinates
updateList(List<Hospital> newList, double userLat, double userLon) → void

// Resource color coding
setResourceColor(ImageView imageView, int count) → void
// Green: > 3, Yellow: 1-3, Red: 0
```

**Usage Example** (in PatientDashboardActivity):
```java
hospitalCardAdapter.updateList(
    hospitalList,
    userLatitude, userLongitude
);
```

---

## 🎯 Activity Flow Diagrams

### Authentication & Role-Based Navigation
```
LanguageSelectionActivity (LAUNCHER)
    ↓
Check if user logged in?
    ├─ NO → LoginActivity
    │        ↓
    │    Enter credentials
    │        ↓
    │    Role selected? 
    │    ├─ NO → RoleSelectionActivity
    │    └─ YES → [Cache role]
    │             ↓
    │    Go to [Role Dashboard]
    │
    └─ YES → SplashActivity
            ↓
        Get cached role
        ├─ Cache hit → Direct to [Role Dashboard]
        └─ Cache miss → Fetch from Firestore → Cache → [Role Dashboard]
```

### Role Routing with Hospital Setup Gate
```
SessionManager.intentForRole(context, role)
    ├─ PATIENT → PatientDashboardActivity
    ├─ HOSPITAL_ADMIN
    │    └─ Setup done?
    │        ├─ NO → HospitalSetupActivity → Set flag → HospitalAdminDashboard
    │        └─ YES → HospitalAdminDashboardActivity
    └─ DONOR → DonorDashboardActivity
```

---

## 📍 Location Permission & GPS Flow

### Three Possible Outcomes
```
checkAndRequestLocationPermission()
    ├─ ALREADY GRANTED
    │  └─ onLocationPermissionGranted()
    │     └─ startLiveLocationUpdates()
    │        └─ FusedLocationProviderClient (every 15 sec)
    │           └─ Update location status bar & sort hospitals
    │
    ├─ NEVER ASKED (Show rationale)
    │  └─ showLocationPermissionRationaleDialog()
    │     ├─ "Allow Location" → requestPermissions()
    │     └─ "Skip for Now" → onLocationPermissionDenied()
    │        └─ Show all hospitals without distance
    │
    └─ TIMEOUT (8 seconds)
       └─ onLocationTimeout()
          └─ Show all hospitals, wait for GPS
```

### Location Update & Re-sorting
```
LocationCallback.onLocationResult()
    ├─ New location received
    ├─ Update userLatitude, userLongitude
    ├─ Update location status bar
    ├─ Recalculate distances (Haversine)
    ├─ Re-sort hospital list by distance
    └─ Show Snackbar: "📍 Location updated — List refreshed"
```

---

## 🗑️ Account Deletion Flow

### 1. User Taps "Delete Account" Button
```
ProfileActivity.buttonDeleteAccount.onClick()
    → showDeleteConfirmationDialog()
    → Get role-specific warning message
    → Show AlertDialog with "DELETE" button
```

### 2. Role-Based Firestore Cleanup
```
performRoleBasedFirestoreDelete(uid, role, progressDialog)
    
    HOSPITAL_ADMIN:
    ├─ Delete users/{uid}
    └─ Delete hospitals/{uid}  [Hospital disappears from patient search]
    
    DONOR:
    ├─ Delete users/{uid}
    └─ Delete donors/{uid}
    
    PATIENT:
    └─ Delete users/{uid}
    
    ↓
    
    Delete Firebase Auth account
    ├─ Catch FirebaseAuthRecentLoginRequiredException
    │  └─ Redirect to LoginActivity with require_reauth=true
    │     → User logs in
    │     → LoginActivity detects flag
    │     → Routes to ProfileActivity with retry_delete=true
    │     → ProfileActivity auto-retries deletion
    │     ↓
    │     SUCCESS: User fully deleted
    └─ SUCCESS: Logout, route to LoginActivity
```

---

## 🏥 Hospital Resource Status Codes

### Color Coding System (HospitalCardAdapter)
```
Count > 3    → 🟢 GREEN (HOLO_GREEN_DARK)     - Sufficient supply
Count 1-3    → 🟡 YELLOW (HOLO_ORANGE_DARK)  - Limited supply
Count 0      → 🔴 RED (HOLO_RED_DARK)        - Out of stock
```

### Hospital Status Badge
```
Limited:  2+ resource types with count < 3
          └─ Status: "LIMITED" (Orange background)

Open:     Fewer than 2 resource types low
          └─ Status: "OPEN" (Green background)
```

---

## 🔄 Parallel Loading Pattern (Patient Dashboard)

### Startup Sequence
```
onCreate()
    ├─ Initialize views & Firebase
    ├─ Start Thread 1: checkAndRequestLocationPermission()
    │                  → startLiveLocationUpdates() 
    │                  → Sets locationReady = true when location arrives
    │
    └─ Start Thread 2: startHospitalsListener()
                       → Firestore real-time listener
                       → Sets hospitalsLoaded = true when data arrives
                       ↓
                       Both threads run independently

tryDisplayHospitals() [Gate method]
    ├─ Wait for: locationReady || (hospitalsLoaded && timeout)
    ├─ Call filterAndDisplayHospitals()
    ├─ Populate RecyclerView via HospitalCardAdapter
    └─ Hidden: textViewLoadingStatus, progressBars
```

**Timeout**: 8 seconds (LOCATION_LOADING_TIMEOUT_MS)
- If GPS not detected after 8 sec, show all hospitals anyway
- GPS can still arrive later and trigger re-sort

---

## 🔑 SharedPreferences Keys (SessionManager)

```java
PREFS_NAME = "medireach_prefs"

KEY_USER_ROLE = "user_role"
  // Stores: "patient" | "hospital_admin" | "donor"
  // Cleared on logout

KEY_HOSPITAL_SETUP_DONE = "hospital_setup_done"
  // Stores: true | false
  // Set to true after HospitalSetupActivity saves location
  // Used to gate hospital admins

KEY_HOSPITAL_NAME = "hospital_name"
  // Stores: Hospital name for quick access
```

---

## 📊 Firestore Collections Structure

```
users/
├─ {uid}
│  ├─ email: "user@example.com"
│  ├─ fullName: "John Doe"
│  ├─ role: "patient" | "hospital_admin" | "donor"
│  └─ createdAt: Timestamp
│
hospitals/
├─ {uid}  [Only hospital admins have a doc here]
│  ├─ hospitalName: "City General Hospital"
│  ├─ latitude: 28.6139
│  ├─ longitude: 77.2090
│  ├─ icuBeds: 5
│  ├─ oxygenCylinders: 20
│  ├─ ventilators: 3
│  ├─ emergencyContact: "1234567890"
│  ├─ bloodAPositive: 10
│  ├─ bloodANegative: 5
│  ├─ bloodBPositive: 8
│  ├─ bloodBNegative: 3
│  ├─ bloodOPositive: 15
│  ├─ bloodONegative: 7
│  ├─ bloodABPositive: 4
│  ├─ bloodABNegative: 2
│  ├─ lastUpdated: Timestamp
│  └─ createdAt: Timestamp
│
donors/
└─ {uid}  [Only donors have a doc here]
   ├─ fullName: "Jane Smith"
   ├─ bloodGroup: "AB+"
   ├─ city: "Mumbai"
   ├─ phoneNumber: "9876543210"
   ├─ lastDonationDate: "2024-01-15"
   ├─ isAvailable: true
   ├─ isOrganDonor: true
   ├─ createdAt: Timestamp
   └─ lastUpdated: Timestamp
```

---

## 🎨 Layout File Structure

### Patient Dashboard Layout
```xml
activity_patient_dashboard.xml
├─ SwipeRefreshLayout (pull-to-refresh)
│  └─ LinearLayout (vertical)
│     ├─ locationStatusBar (TextView with GPS status)
│     ├─ SearchBar (EditText)
│     ├─ Filter Chips (ICU, Oxygen, Blood, Ventilator)
│     ├─ Sort Spinner (Distance, Name, Availability)
│     ├─ textViewLoadingStatus (progress message)
│     ├─ RecyclerView (hospital list)
│     │  └─ HospitalCardAdapter → hospital_card_item.xml
│     ├─ buttonEmergencyFab (floating action button)
│     └─ progressLocation, progressData (hidden progress bars)
```

### Hospital Card Item
```xml
hospital_card_item.xml
├─ CardView
│  └─ LinearLayout (vertical)
│     ├─ textViewHospitalName (bold, large)
│     ├─ textViewHospitalAddress (address, city)
│     ├─ textViewDistance (📍 2.3 km away)
│     ├─ LinearLayout (horizontal - resources)
│     │  ├─ ImageView + TextView (ICU Beds)
│     │  ├─ ImageView + TextView (Oxygen)
│     │  ├─ ImageView + TextView (Ventilators)
│     │  └─ ImageView + TextView (Blood Units)
│     ├─ textViewStatus (LIMITED/OPEN badge)
│     └─ LinearLayout (buttons)
│        ├─ buttonViewDetails
│        └─ buttonEmergencyCall
```

---

## 🧪 Common Debugging Tips

### Location Not Updating?
```java
// Check in logcat for "MediReach_Location" tags
Log.d("MediReach_Location", "Location update: " + location);

// Verify permissions in Settings > Apps > MediReach > Permissions
// Ensure Location permission is "Allow all the time" or "Allow only while using app"
```

### Role Caching Issues?
```java
// Check SharedPreferences
SharedPreferences prefs = context.getSharedPreferences("medireach_prefs", Context.MODE_PRIVATE);
String cachedRole = prefs.getString("user_role", "");
Log.d("MediReach_Role", "Cached role: " + cachedRole);

// Check Firestore for actual role
db.collection("users").document(uid).get()
    .addOnSuccessListener(doc -> 
        Log.d("MediReach_Role", "Firestore role: " + doc.getString("role"))
    );
```

### Firebase Connection Slow?
```java
// Enable Firestore offline persistence
FirebaseFirestore.getInstance().setPersistenceEnabled(true);

// Check network connectivity
ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
```

### Distance Calculation Wrong?
```java
// Verify Haversine formula
double distanceKm = LocationUtils.calculateDistance(
    startLat, startLon,
    endLat, endLon
);
// Should return value in kilometers
// If too high/low, check lat/lon sign (N/S, E/W)
```

---

## 📝 Code Style Guidelines

### Naming Conventions
```java
// Activities
PatientDashboardActivity, HospitalAdminDashboardActivity

// Utilities/Helpers
SessionManager, LocationUtils, LocaleHelper

// Models
Hospital, User, Donor

// Adapters
HospitalCardAdapter, OnboardingPagerAdapter

// Views (in Activity fields)
textViewHospitalName        // TextView
editTextSearch              // EditText
buttonSaveAndPublish        // Button
imageViewLogout             // ImageView
recyclerViewHospitals       // RecyclerView
spinnerSort                 // Spinner
chipIcu                     // Chip
swipeRefreshLayout          // SwipeRefreshLayout
progressLocation            // ProgressBar
```

### Comment Style
```java
/**
 * Public methods should have JavaDoc
 * 
 * @param context The application context
 * @return The normalized role string
 */
public static String normalizeRole(String role) {
    // Private implementation details can use single-line comments
    return role.toLowerCase();
}
```

---

## 📦 Dependencies

### Firebase
```gradle
implementation 'com.google.firebase:firebase-auth-ktx'
implementation 'com.google.firebase:firebase-firestore-ktx'
implementation 'com.google.firebase:firebase-storage-ktx'
```

### Google Play Services
```gradle
implementation 'com.google.android.gms:play-services-location'
```

### AndroidX
```gradle
implementation 'androidx.appcompat:appcompat'
implementation 'androidx.recyclerview:recyclerview'
implementation 'androidx.swiperefreshlayout:swiperefreshlayout'
implementation 'androidx.cardview:cardview'
implementation 'androidx.viewpager2:viewpager2'
```

### Material Design
```gradle
implementation 'com.google.android.material:material'
```

---

## 🚀 Deployment Checklist

- [ ] All API keys in `google-services.json` are valid
- [ ] Firebase project configured for Android
- [ ] Firestore security rules deployed
- [ ] Location permissions granted in test device
- [ ] Network connectivity verified
- [ ] APK signed with release keystore
- [ ] Tested on Android 7.0+ device/emulator
- [ ] All activities tested end-to-end
- [ ] Logout tested across all roles
- [ ] GPS permission flow tested
- [ ] Account deletion tested with reauthentication

---

## 💡 Future Enhancement Ideas

1. **Real-time Notifications**: Push notifications for resource availability changes
2. **Hospital Ratings**: Allow patients to rate hospitals
3. **Appointment System**: Book hospital slots
4. **Dark Mode**: System-wide dark theme support
5. **Multiple Languages**: Full i18n support beyond English
6. **Emergency SOS**: One-tap emergency alert to nearest hospitals
7. **Offline Support**: Firestore offline persistence for cached data
8. **Analytics**: Track user behavior and resource trends

---

**Last Updated**: March 30, 2026  
**Maintainer**: MediReach Development Team  
**Status**: ✅ Production Ready

