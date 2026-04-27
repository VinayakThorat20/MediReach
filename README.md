# MediReach

MediReach is an Android healthcare resource availability app built in **Java** for **Android Studio Panda**. It helps:

- **Patients** find nearby hospitals and view live resource availability
- **Hospital Admins** publish hospital resources such as ICU beds, oxygen cylinders, ventilators, and blood units
- **Donors** register and share blood donation availability

The app uses **Firebase Authentication** for login and **Cloud Firestore** for storing live data. It also supports **multiple languages** and **location-based hospital sorting**.

---

## Project Structure

```text
MediReach/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties
├── README.md
├── DEVELOPER_GUIDE.md
├── FEATURES_IMPLEMENTED.md
├── TESTING_GUIDE.md
├── gradle/
│   └── libs.versions.toml
└── app/
	├── build.gradle.kts
	├── google-services.json
	├── proguard-rules.pro
	└── src/
		├── main/
		│   ├── AndroidManifest.xml
		│   ├── java/com/vinayak/medireach/
		│   │   ├── MainActivity.java
		│   │   ├── LanguageSelectionActivity.java
		│   │   ├── SplashActivity.java
		│   │   ├── LoginActivity.java
		│   │   ├── RegisterActivity.java
		│   │   ├── RoleSelectionActivity.java
		│   │   ├── OnboardingActivity.java
		│   │   ├── PatientDashboardActivity.java
		│   │   ├── HospitalAdminDashboardActivity.java
		│   │   ├── DonorDashboardActivity.java
		│   │   ├── HospitalSetupActivity.java
		│   │   ├── HospitalDetailActivity.java
		│   │   ├── DonorSearchActivity.java
		│   │   ├── ProfileActivity.java
		│   │   ├── EditProfileActivity.java
		│   │   ├── adapter/
		│   │   │   └── LanguageAdapter.java
		│   │   ├── adapters/
		│   │   │   ├── HospitalCardAdapter.java
		│   │   │   └── DonorAdapter.java
		│   │   ├── model/
		│   │   │   └── Language.java
		│   │   ├── models/
		│   │   │   ├── Hospital.java
		│   │   │   └── Donor.java
		│   │   └── utils/
		│   │       ├── LocaleHelper.java
		│   │       ├── LocationUtils.java
		│   │       ├── SessionManager.java
		│   │       └── NetworkUtil.java
		│   └── res/
		│       ├── layout/
		│       │   ├── activity_main.xml
		│       │   ├── activity_language_selection.xml
		│       │   ├── activity_splash.xml
		│       │   ├── activity_login.xml
		│       │   ├── activity_register.xml
		│       │   ├── activity_role_selection.xml
		│       │   ├── activity_onboarding.xml
		│       │   ├── activity_patient_dashboard.xml
		│       │   ├── activity_hospital_admin_dashboard.xml
		│       │   ├── activity_donor_dashboard.xml
		│       │   ├── activity_hospital_setup.xml
		│       │   ├── activity_hospital_detail.xml
		│       │   ├── activity_donor_search.xml
		│       │   ├── activity_profile.xml
		│       │   ├── activity_edit_profile.xml
		│       │   ├── item_language.xml
		│       │   ├── item_onboarding_page.xml
		│       │   ├── item_hospital_card.xml
		│       │   ├── item_donor_card.xml
		│       │   └── dialog_manual_location.xml
		│       ├── menu/
		│       │   └── menu_dashboard_actions.xml
		│       ├── values/
		│       │   ├── strings.xml
		│       │   ├── themes.xml
		│       │   └── colors.xml
		│       ├── values-night/
		│       │   └── themes.xml
		│       ├── values-hi/
		│       │   └── strings.xml
		│       ├── values-mr/
		│       │   └── strings.xml
		│       ├── xml/
		│       │   ├── backup_rules.xml
		│       │   └── data_extraction_rules.xml
		│       └── drawable/
		│           ├── gradient_splash.xml
		│           ├── gradient_patient.xml
		│           ├── gradient_hospital.xml
		│           ├── gradient_donor.xml
		│           ├── contact_background.xml
		│           ├── button_background.xml
		│           ├── button_outline_red.xml
		│           ├── ic_medical_cross.xml
		│           ├── ic_patient.xml
		│           ├── ic_hospital.xml
		│           ├── ic_donor.xml
		│           ├── ic_launcher_foreground.xml
		│           ├── ic_launcher_background.xml
		│           └── baseline_arrow_back_24.xml
		├── test/
		│   └── java/com/vinayak/medireach/ExampleUnitTest.java
		└── androidTest/
			└── java/com/vinayak/medireach/ExampleInstrumentedTest.java
```

---

## How to Run the Project

### In Android Studio

1. Open the `MediReach` folder in **Android Studio Panda**.
2. Wait for **Gradle Sync** to finish.
3. Make sure Firebase is connected through `app/google-services.json`.
4. Run the app on an emulator or a physical Android phone.
5. The launcher activity is **`LanguageSelectionActivity`**.
6. Choose a language, then continue through splash, login, and the role-based screens.

### Build from terminal

```powershell
cd C:\Users\Dell\AndroidStudioProjects\MediReach
.\gradlew assembleDebug
```

### Run tests

```powershell
.\gradlew test
.\gradlew connectedAndroidTest
```

---

## Android Concepts Explained Simply

### 1. Activity
An **Activity** is one screen in Android.

Examples in MediReach:
- `LanguageSelectionActivity` → language picker
- `SplashActivity` → startup routing screen
- `LoginActivity` → login form
- `RegisterActivity` → account creation
- `RoleSelectionActivity` → choose Patient / Hospital Admin / Donor
- `PatientDashboardActivity` → hospital list and search
- `HospitalAdminDashboardActivity` → hospital resource publishing
- `DonorDashboardActivity` → donor profile screen
- `ProfileActivity` → account details and actions
- `EditProfileActivity` → edit profile data
- `OnboardingActivity` → first-time onboarding screens
- `DonorSearchActivity` → donor search and filters
- `HospitalSetupActivity` → hospital setup form
- `HospitalDetailActivity` → detailed hospital view

### 2. Activity lifecycle
The lifecycle is the set of states an Activity goes through.

Used in the project:
- `onCreate()` → setup screen and views
- `onResume()` → restart location updates in `PatientDashboardActivity`
- `onPause()` → stop location updates
- `onDestroy()` → clean up listeners and callbacks

### 3. Intent
An **Intent** is a message used to open another screen or action.

Used for:
- opening another Activity
- opening the phone dialer
- opening Google Maps
- opening Android Settings

### 4. Explicit Intent
Used when the app knows the exact screen.

Example:

```java
Intent intent = new Intent(this, LoginActivity.class);
startActivity(intent);
```

### 5. Implicit Intent
Used when Android must find an app to handle the action.

Example:

```java
Intent dialIntent = new Intent(Intent.ACTION_DIAL);
dialIntent.setData(Uri.parse("tel:" + number));
startActivity(dialIntent);
```

### 6. View
A **View** is any UI element on screen.

Used Views include:
- `TextView`
- `EditText`
- `Button`
- `ImageView`
- `RecyclerView`
- `CardView`
- `Spinner`
- `Switch`
- `CheckBox`
- `ProgressBar`
- `FloatingActionButton`
- `Chip`

### 7. Layout
A **Layout** decides how views are arranged.

Used layouts include:
- `LinearLayout` → places views in a row or column
- `ConstraintLayout` → flexible layout positioning
- `ScrollView` → lets content scroll
- `FrameLayout` → stacks views on top of each other

### 8. RecyclerView
RecyclerView shows a list efficiently.

Used for:
- languages
- hospitals
- donors

### 9. Adapter
An adapter connects data to RecyclerView item cards.

Adapters in the project:
- `LanguageAdapter`
- `HospitalCardAdapter`
- `DonorAdapter`

### 10. ViewHolder
A ViewHolder stores references to the views inside one card so Android can reuse them quickly.

### 11. SharedPreferences
SharedPreferences stores small saved data like language, role, and setup flags.

Keys used:
- `app_language`
- `user_role`
- `hospital_setup_done`
- `onboarding_done`

### 12. Permissions
Permissions allow the app to use sensitive features.

Used permissions:
- `INTERNET`
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `ACCESS_NETWORK_STATE`
- `CALL_PHONE`

### 13. GPS and location services
MediReach uses `FusedLocationProviderClient` to get the user’s live location and sort hospitals by distance.

### 14. Toolbar and menu
The app uses top-bar actions such as logout, profile, and find donors.

### 15. Dialogs
Dialogs are pop-up confirmation boxes.

### 16. Locale and language support
Locale is how Android knows which language to show.

### 17. ProgressBar
Used to show loading during login, registration, saving, and GPS detection.

### 18. Chip
Used as filter buttons in the patient dashboard.

### 19. Switch and CheckBox
Used in the donor screen and edit profile screen.

### 20. FloatingActionButton
Used for emergency call actions in the patient dashboard.

---

## Firebase Explanation

### What Firebase does in MediReach
Firebase is the backend of the app.

It handles:
- user authentication
- storing user profiles
- storing hospitals
- storing donors
- real-time updates

### Firebase Authentication
Firebase Auth lets users register and log in with email and password.

Example from `RegisterActivity.java`:

```java
firebaseAuth.createUserWithEmailAndPassword(email, password)
	.addOnSuccessListener(authResult -> {
		saveUserToFirestore(fullName, email, authResult.getUser());
	});
```

Example from `LoginActivity.java`:

```java
firebaseAuth.signInWithEmailAndPassword(email, password)
	.addOnSuccessListener(authResult -> {
		routeUserAfterLogin(authResult.getUser());
	});
```

### Firestore
Firestore is the database that stores app data.

Main collections:
- `users`
- `hospitals`
- `donors`

### Real-time listener
Used in `PatientDashboardActivity` so hospital updates appear automatically.

```java
db.collection("hospitals")
	.addSnapshotListener((value, error) -> {
		if (error != null) return;
		hospitalList.clear();
		for (DocumentSnapshot doc : value.getDocuments()) {
			Hospital hospital = doc.toObject(Hospital.class);
			hospital.setHospitalId(doc.getId());
			hospitalList.add(hospital);
		}
		tryDisplayHospitals();
	});
```

### Saving hospital resources
Example from `HospitalAdminDashboardActivity.java`:

```java
firestore.collection("hospitals").document(currentUser.getUid())
	.set(data, SetOptions.merge())
	.addOnSuccessListener(unused -> {
		Toast.makeText(this, "Data updated successfully", Toast.LENGTH_SHORT).show();
	});
```

### Distance calculation
Example from `LocationUtils.java`:

```java
public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
	double latRad1 = Math.toRadians(lat1);
	double lonRad1 = Math.toRadians(lon1);
	double latRad2 = Math.toRadians(lat2);
	double lonRad2 = Math.toRadians(lon2);

	double dLat = latRad2 - latRad1;
	double dLon = lonRad2 - lonRad1;

	double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
			   Math.cos(latRad1) * Math.cos(latRad2) *
			   Math.sin(dLon / 2) * Math.sin(dLon / 2);

	double c = 2 * Math.asin(Math.sqrt(a));
	return 6371.0 * c;
}
```

---

## Role-Based Flow in the App

### Saved role navigation
The app remembers the user role in SharedPreferences and uses it to open the correct screen.

Example from `SessionManager.java`:

```java
public static Intent intentForRole(Context context, String role) {
	String normalized = normalizeRole(role);
	if (ROLE_PATIENT.equals(normalized)) {
		return new Intent(context, PatientDashboardActivity.class);
	}
	if (ROLE_HOSPITAL_ADMIN.equals(normalized)) {
		if (!isHospitalSetupDone(context)) {
			return new Intent(context, HospitalSetupActivity.class);
		}
		return new Intent(context, HospitalAdminDashboardActivity.class);
	}
	if (ROLE_DONOR.equals(normalized)) {
		return new Intent(context, DonorDashboardActivity.class);
	}
	return new Intent(context, RoleSelectionActivity.class);
}
```

### Logout

```java
public static void logoutAndOpenLogin(android.app.Activity activity) {
	FirebaseAuth.getInstance().signOut();
	SharedPreferences preferences = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	preferences.edit().remove(KEY_USER_ROLE).apply();

	Intent intent = new Intent(activity, LoginActivity.class);
	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
	activity.startActivity(intent);
	activity.finishAffinity();
	activity.finish();
}
```

---

## Hospital and Donor Data Models

### `Hospital.java`
Stores hospital details like:
- name
- address
- coordinates
- ICU beds
- oxygen cylinders
- ventilators
- blood units
- emergency contact
- last updated date

### `Donor.java`
Stores donor details like:
- full name
- blood group
- city
- phone
- donation date
- available status
- organ donor status

### `Language.java`
Stores language name and language code.

---

## Manifest and Permissions

The `AndroidManifest.xml` file:
- registers all app screens
- declares permissions
- sets `LanguageSelectionActivity` as the launcher screen

Permissions declared:
- `INTERNET` → connect to Firebase
- `ACCESS_FINE_LOCATION` → exact GPS
- `ACCESS_COARSE_LOCATION` → approximate location
- `ACCESS_NETWORK_STATE` → check network availability
- `CALL_PHONE` → phone call actions

---

## Themes and Dark Mode

The project uses:
- `res/values/themes.xml`
- `res/values-night/themes.xml`

This allows Android to switch automatically between light and dark mode depending on the device theme.

---

## Helpful Notes for Beginners

- **XML** is used to design screens.
- **Java** is used to control the app logic.
- **Firebase** stores data online.
- **SharedPreferences** stores small settings on the device.
- **RecyclerView** is used for lists.
- **Intents** move between screens.
- **Location services** help find nearby hospitals.

---

## Testing Files

- `app/src/test/java/com/vinayak/medireach/ExampleUnitTest.java`
- `app/src/androidTest/java/com/vinayak/medireach/ExampleInstrumentedTest.java`

These are sample test files included by Android Studio.

---

## Documentation Files

- `DEVELOPER_GUIDE.md` → technical reference and setup guide
- `FEATURES_IMPLEMENTED.md` → what has been built in the app
- `TESTING_GUIDE.md` → test steps and troubleshooting

---

## Summary

MediReach is a **Java Android healthcare app** that combines:

- multi-role login
- multilingual support
- live hospital data
- donor search
- GPS-based hospital sorting
- Firebase backend

It is a strong example of an Android app that uses **activities, intents, RecyclerView, SharedPreferences, location services, and Firebase** together in one project.


