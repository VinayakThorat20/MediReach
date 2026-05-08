# Walkthrough - Navigation, Donor Search, and Blood Alerts

I have implemented the requested fixes and features to improve the MediReach app's navigation, add a donor search feature for patients, and enable blood alert broadcasts for hospitals.

## Fix 1: App Startup & Role-Based Navigation

The app now follows a consistent role-based navigation flow starting from `SplashActivity`.

- **SplashActivity**:
    - If not logged in, redirects to `LoginActivity`.
    - If logged in, checks `SharedPreferences` for `user_role`.
    - If role is missing, fetches from Firestore and saves it.
    - Navigates to the appropriate dashboard based on role (Hospital Admin, Patient, Donor).
    - For Hospital Admins, it also checks `hospital_setup_done` to decide between `HospitalSetupActivity` and `HospitalAdminDashboardActivity`.
- **LoginActivity & RegisterActivity**: Updated to fetch/save the role and navigate using the same consistent logic.
- **RoleSelectionActivity**: Updated to save the selected role to both Firestore and `SharedPreferences` before navigating.

## Fix 2: Find Blood Donors Feature

Patients can now search for available blood donors and contact them directly.

- **PatientDashboardActivity**: Added "Find Blood Donors" to the emergency actions menu.
- **DonorSearchActivity**: A new activity where patients can:
    - View a list of available donors.
    - Filter donors by blood group and city.
    - City filters are populated dynamically from the donor data.
- **DonorAdapter**: Displays donor information (Name, Blood Group, City, Phone) and includes a "Call Donor" button that opens the system dialer.

## Fix 3: Hospital Blood Alert Broadcast

Hospitals can now broadcast emergency blood requirements to matching donors via SMS.

- **Hospital Admin Dashboard**: Added a "Blood Requirement Alert" section with a descriptive header and broadcast button.
- **Broadcast Flow**:
    - Admins select a blood group and specify units needed.
    - The app fetches all available donors with the matching blood group from Firestore.
    - It then opens the system SMS app with the recipients and a pre-filled emergency message including the hospital name and requirements.

## Verification Summary

- **Build**: Successfully executed `./gradlew assembleDebug`.
- **Navigation**: Verified that `SplashActivity` correctly routes users based on their role and setup status.
- **Donor Search**: Verified the dynamic city list and filtering logic in `DonorSearchActivity`.
- **Blood Alerts**: Verified the SMS intent construction and recipient collection in `HospitalAdminDashboardActivity`.
