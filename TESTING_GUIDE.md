# MediReach - Testing & Troubleshooting Guide

## 🧪 Testing Instructions

### Prerequisites
- Android device or emulator running Android 7.0+
- USB cable (for physical device testing)
- Google account for Firebase sync
- Location services enabled on device

### Setup Steps

#### 1. Install the APK
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or directly drag-drop in Android Studio Device Explorer.

#### 2. First Launch
- **First Screen**: Language Selection
- **Next Screen**: Login/Register
- **Choose**: Patient, Hospital Admin, or Donor role

#### 3. Grant Permissions
- Allow location permission when prompted
- Allow optional features as needed

---

## 🧬 Test Scenarios

### Scenario 1: Patient User Complete Flow

**Objective**: Test patient dashboard, GPS, search, filters, and logout

**Steps**:
1. Register as **Patient** user
   - Email: `patient@test.com`
   - Password: `Test@1234`
   - Full Name: `John Patient`

2. Tap "Allow Location" in permission dialog
   - Location status bar should update: "📍 Live: 28.6139, 77.2090"
   - Hospital list populates with distances
   - Cards show: "📍 2.3 km away"

3. Test Search
   - Type hospital name in search box
   - List filters in real-time

4. Test Filters (Chips)
   - Tap "🏥 ICU" → Show only hospitals with ICU beds
   - Tap "💨 Oxygen" → Show only hospitals with oxygen
   - Tap "All" → Reset filter

5. Test Sorting (Spinner)
   - Select "Distance" → Sorted by nearest first
   - Select "Name" → Sorted alphabetically
   - Select "Availability" → Open hospitals first

6. Test Pull-to-Refresh
   - Swipe down on hospital list
   - Should refresh hospital data

7. Test Emergency Call
   - Tap call button on any hospital card
   - Dial app should open with hospital phone number

8. Test Profile
   - Tap profile icon (top right)
   - View email, role, registration date
   - Tap "Edit Profile" → Edit name and info
   - Tap "Change Language" → Language selection

9. Test Logout
   - Tap logout button
   - Confirm logout in dialog
   - Routed back to login
   - **Re-login**: Should bypass role selection, go directly to Patient Dashboard

**Expected Result**: ✅ PASS

---

### Scenario 2: Hospital Admin User Complete Flow

**Objective**: Test hospital setup, resource publishing, and quick updates

**Steps**:
1. Register as **Hospital Admin** user
   - Email: `admin@hospital.com`
   - Password: `Admin@1234`

2. First Login: **Hospital Setup Screen**
   - Tap "Detect My Hospital Location"
   - Wait 5 seconds for GPS to lock
   - Location should populate lat/lon fields
   - OR manually enter:
     - Latitude: `28.6139`
     - Longitude: `77.2090`

3. Fill Hospital Info
   - Hospital Name: `City General Hospital`
   - Address: `123 Main Street`
   - City: `New Delhi`
   - Pincode: `110001`
   - Type: `General`
   - Tap "Save & Continue"

4. Setup Complete → Routed to Hospital Admin Dashboard

5. Test Resource Publishing
   - Enter values:
     - ICU Beds: `5`
     - Oxygen Cylinders: `20`
     - Ventilators: `3`
     - Blood units (all types): Vary between 0-15
   - Tap "Save & Publish"
   - Should show success message

6. Test Quick Update
   - Select "ICU Beds" from dropdown
   - Enter new value: `8`
   - Tap "Update"
   - Should update immediately

7. Test Profile Edit
   - Tap profile icon
   - View hospital name (read-only)
   - Edit emergency contact
   - Tap save

8. Test Logout
   - Tap logout icon
   - Confirm logout
   - **Re-login**: Should bypass role selection AND hospital setup, go directly to Admin Dashboard

**Expected Result**: ✅ PASS

---

### Scenario 3: Donor User Complete Flow

**Objective**: Test donor profile, availability toggle, and donation tracking

**Steps**:
1. Register as **Donor** user
   - Email: `donor@test.com`
   - Password: `Donor@1234`
   - Full Name: `Jane Donor`

2. Complete Donor Profile
   - Blood Group: `AB+`
   - City: `Mumbai`
   - Phone: `9876543210`
   - Last Donation Date: `2024-01-15` (pick a date)
   - Available for Donation: Toggle ON
   - Organ Donor: Check box
   - Tap "Save Profile"

3. Verify Profile Saved
   - Donor summary should show: "AB+ | Mumbai | Active: Yes"

4. Test Profile Edit
   - Tap profile icon
   - Modify blood group to `O-`
   - Modify city to `Delhi`
   - Tap save
   - Verify changes

5. Test Availability Toggle
   - Toggle "Available" OFF
   - Save (if needed)
   - Toggle ON again

6. Test Logout
   - Tap logout icon
   - **Re-login**: Should bypass role selection, go directly to Donor Dashboard

**Expected Result**: ✅ PASS

---

### Scenario 4: Account Deletion Flow

**Objective**: Test account deletion with reauthentication

**Steps**:
1. Login as any user
2. Tap Profile icon
3. Tap "Delete Account"
4. Confirm deletion in dialog

**If Recently Logged In**:
- Account deletes immediately
- User logged out and routed to login screen

**If Session Expired**:
- Routed to Login screen
- User must re-login
- After login, automatically retried deletion
- Account deleted
- Logged out

**Expected Result**: ✅ PASS

---

### Scenario 5: Location Permission Handling

**Objective**: Test all three location permission outcomes

#### Test Case A: Permission Granted
1. Launch app as Patient
2. Tap "Allow Location" when prompted
3. Wait 2 seconds
4. **Expected**: Location status bar updates, hospitals show distances

#### Test Case B: Permission Denied
1. Launch app as Patient
2. Tap "Skip for Now"
3. **Expected**: All hospitals shown, no distances, "Detecting location..." still visible

#### Test Case C: Timeout (8 seconds)
1. Launch app as Patient
2. Don't grant permission (ignore dialog)
3. Wait 8+ seconds
4. **Expected**: Dialog dismissed, hospitals shown, status bar changes to timeout message

**Expected Result**: ✅ PASS

---

### Scenario 6: Role Memory Test

**Objective**: Verify role caching works correctly

**Steps**:
1. Register and login as **Patient**
2. Logout
3. Re-login with same credentials
   - **Expected**: Bypass role selection, go directly to Patient Dashboard
4. Logout
5. Register/login as **Hospital Admin**
6. Complete setup
7. Logout
8. Re-login as Hospital Admin
   - **Expected**: Bypass both role selection AND setup, go directly to Admin Dashboard
9. Logout
10. Register/login as **Donor**
11. Logout
12. Re-login as Donor
    - **Expected**: Bypass role selection, go directly to Donor Dashboard

**Expected Result**: ✅ PASS

---

## 🐛 Common Issues & Solutions

### Issue 1: GPS Location Not Updating

**Symptoms**:
- "📍 Detecting your location..." message doesn't change after 8 seconds
- Distance shows as "0 km" or "0 m"
- Location status bar stays yellow

**Causes**:
- Location permission denied
- GPS disabled on device
- No satellite lock (in building)

**Solutions**:
```
1. Settings > Apps > MediReach > Permissions > Location
   → Set to "Allow all the time"

2. Settings > Location
   → Turn ON Location
   → Set to High Accuracy

3. For emulator:
   → Extended controls > Location
   → Set latitude/longitude manually
   → Or use "Send" button

4. Physical device:
   → Go outside for better GPS signal
   → Wait 2-3 minutes for satellite lock
```

---

### Issue 2: Login Shows Role Selection Screen

**Symptoms**:
- After login, role selection screen appears (should skip it)
- Happens on every login

**Cause**:
- Role cache cleared or not saved

**Solutions**:
```
1. Clear app cache & data:
   Settings > Apps > MediReach > Storage > Clear Cache
   Settings > Apps > MediReach > Storage > Clear Data
   (Then re-register)

2. Check SharedPreferences in Android Studio:
   Device Explorer > data > data > 
   com.vinayak.medireach > shared_prefs > 
   medireach_prefs.xml
   (Should contain <string name="user_role">patient</string>)

3. If Firestore role missing:
   Firebase Console > Firestore > users > {uid}
   Add field: role = "patient" (or appropriate role)
```

---

### Issue 3: Hospital Setup Screen Appears on Admin Re-login

**Symptoms**:
- Hospital admin logs in twice
- Second login shows hospital setup screen (should skip to dashboard)

**Cause**:
- Hospital setup flag not set or cleared

**Solutions**:
```
1. Complete hospital setup process:
   → Enter location (GPS or manual)
   → Tap "Save & Continue"
   → Flag `hospital_setup_done` should be set

2. Check flag in SharedPreferences:
   medireach_prefs.xml should contain:
   <boolean name="hospital_setup_done">true</boolean>

3. If flag missing:
   - Manually add flag via Android Studio
   - OR re-run setup process
   - OR clear app data and re-register
```

---

### Issue 4: Account Deletion Fails

**Symptoms**:
- Tap "Delete Account"
- Dialog shows, but nothing happens on confirmation
- Error message: "Failed to delete account"

**Cause**:
- Session expired (requires reauthentication)
- Firestore permissions issue
- No network connection

**Solutions**:
```
1. If session expired:
   → Will be routed to LoginActivity
   → Login again
   → Auto-retry deletion
   → Should succeed

2. If Firestore permission error:
   → Check Firebase security rules
   → Rule should allow user to delete own docs
   → Example rule:
     allow delete: if request.auth.uid == resource.id;

3. If network error:
   → Check internet connection
   → Try again
   → Check Firebase status page
```

---

### Issue 5: Search/Filter Not Working

**Symptoms**:
- Type in search box → list doesn't filter
- Tap filter chips → nothing changes
- Spinner sort → list doesn't re-sort

**Cause**:
- RecyclerView adapter not updating
- Filter logic not triggered

**Solutions**:
```
1. Check logcat for errors:
   adb logcat | grep "MediReach"

2. Verify adapter is set:
   recyclerViewHospitals.setAdapter(hospitalCardAdapter)

3. Force update after search:
   EditText listener should call:
   hospitalCardAdapter.updateList(filteredList, lat, lon)
   hospitalCardAdapter.notifyDataSetChanged()

4. Clear app cache and reinstall:
   adb uninstall com.vinayak.medireach
   adb install app-debug.apk
```

---

### Issue 6: Firebase Connection Issues

**Symptoms**:
- Hospitals not loading
- Profile data doesn't save
- Error: "Failed to fetch hospitals"

**Cause**:
- Firebase config missing
- No internet connection
- Firebase project not properly configured

**Solutions**:
```
1. Verify google-services.json:
   - Located at: app/google-services.json
   - Contains valid Firebase project ID
   - Check in Firebase Console

2. Check network:
   Settings > Network > WiFi/Mobile
   Should show active connection

3. Verify Firebase Console:
   → Authentication: Email/password enabled
   → Firestore: Database created
   → Security rules: Configured

4. Restart Firebase emulator/backend:
   → If using local emulator
   → Kill and restart processes
   → Restart emulator

5. Check adb logcat:
   adb logcat | grep "Firebase"
   Look for error messages
```

---

### Issue 7: Reauthentication Not Working

**Symptoms**:
- Try to delete account
- Routed to LoginActivity
- After login, doesn't retry deletion
- Just shows profile screen

**Cause**:
- Intent extra `require_reauth=true` not passed
- Intent extra `retry_delete=true` not detected

**Solutions**:
```
1. Check intents in code:
   
   LoginActivity when routed from ProfileActivity:
   Intent retryDeleteIntent = new Intent(this, ProfileActivity.class);
   retryDeleteIntent.putExtra("retry_delete", true);
   startActivity(retryDeleteIntent);
   
2. Check in ProfileActivity onCreate:
   if (getIntent().getBooleanExtra("retry_delete", false)) {
       deleteUserAccount();
   }

3. If manual fix needed:
   - Check logcat for intent passing
   - Verify extras are set
   - Verify getIntent().getBooleanExtra() logic
```

---

## 📝 Manual Testing Checklist

```
[ ] Registration with all 3 roles works
[ ] Login works for all roles
[ ] Role selection skipped on re-login (cached role)
[ ] Hospital admin sees setup screen on first login
[ ] Hospital admin skips setup on re-login
[ ] Patient sees hospital list with GPS
[ ] GPS permission dialog shows rationale
[ ] Hospital distances calculated correctly
[ ] Search filters hospitals in real-time
[ ] Resource filter chips work
[ ] Sort spinner changes order
[ ] Pull-to-refresh loads new data
[ ] Emergency call opens dialer
[ ] Profile shows correct user info
[ ] Edit profile saves changes
[ ] Password change works
[ ] Email change works
[ ] Delete account deletes all data
[ ] Reauthentication redirects work
[ ] Logout clears session
[ ] Re-login routes correctly
[ ] No crashes or ANRs
[ ] All buttons respond
[ ] All text inputs accept data
[ ] Database updates reflect in UI
```

---

## 📊 Performance Testing

### Location Update Frequency
- Expected: Every 15 seconds when location changes
- Test: Open logcat, filter "MediReach_Location"
- Should see updates approximately every 15 seconds

### Firestore Sync Speed
- Expected: Hospital list updates within 2-3 seconds
- Test: Publish resource change from admin, observe patient list
- Should reflect within 3 seconds

### Network Latency
- Expected: < 2 seconds for Firebase operations
- Test: Profile save, hospital publish, etc.
- If slower, check network quality

---

## 🔍 Debug Logging

Enable detailed logging by checking logcat:

```bash
adb logcat | grep "MediReach"
```

**Key log tags**:
- `MediReach_Location` - Location updates
- `MediReach_Hospitals` - Hospital list operations
- `MediReach_Role` - Role caching/routing
- `MediReach_Auth` - Authentication events
- `MediReach_Profile` - Profile operations

---

## 📞 Firebase Console Verification

After testing, verify data in Firebase Console:

1. **Authentication Tab**:
   - Should show all test users created
   - Each with correct email and role

2. **Firestore Database**:
   - `users/{uid}` - Contains email, fullName, role, createdAt
   - `hospitals/{uid}` - Contains hospital data (for admins)
   - `donors/{uid}` - Contains donor data (for donors)

3. **Deleted User Data**:
   - After account deletion test
   - Users/{uid} should be deleted
   - hospitals/{uid} or donors/{uid} should be deleted

---

## ✅ Sign-Off Checklist

- [ ] All 3 roles can register and login
- [ ] Role memory works (cache + Firestore)
- [ ] GPS location updates show distances
- [ ] Hospital admin setup works one-time
- [ ] Profile editing works for all roles
- [ ] Account deletion cleans up data
- [ ] Reauthentication flow works
- [ ] Logout works from all dashboards
- [ ] No crashes observed
- [ ] Data persists in Firestore
- [ ] All permissions work correctly
- [ ] App ready for production

---

**Last Updated**: March 30, 2026  
**Status**: Ready for QA Testing  
**Build**: app-debug.apk (9.56 MB)

