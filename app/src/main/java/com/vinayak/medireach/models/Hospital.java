package com.vinayak.medireach.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Hospital model class representing a hospital entity in the MediReach system.
 * Contains information about hospital resources, location, and blood inventory.
 */
public class Hospital {

    // Hospital Information
    private String hospitalId;
    private String hospitalName;
    private String address;
    private String emergencyContact;

    // Location Information
    private double latitude;
    private double longitude;

    // Medical Resources
    private int icuBeds;
    private int oxygenCylinders;
    private int ventilators;

    // Blood Inventory
    private Map<String, Integer> bloodUnits;

    // Metadata
    private Date lastUpdated;
    private String adminUid;

    /**
     * Empty constructor for Hospital.
     * Initializes bloodUnits as an empty HashMap.
     */
    public Hospital() {
        this.bloodUnits = new HashMap<>();
        this.lastUpdated = new Date();
    }

    /**
     * Full constructor for Hospital.
     *
     * @param hospitalId       The unique identifier for the hospital
     * @param hospitalName     The name of the hospital
     * @param address          The address of the hospital
     * @param emergencyContact The emergency contact number
     * @param latitude         The latitude of hospital location
     * @param longitude        The longitude of hospital location
     * @param icuBeds          Number of ICU beds available
     * @param oxygenCylinders  Number of oxygen cylinders available
     * @param ventilators      Number of ventilators available
     * @param bloodUnits       Map containing blood unit inventory
     * @param lastUpdated      Timestamp of last update
     * @param adminUid         The Firebase UID of the hospital admin
     */
    public Hospital(String hospitalId, String hospitalName, String address, String emergencyContact,
                    double latitude, double longitude, int icuBeds, int oxygenCylinders, int ventilators,
                    Map<String, Integer> bloodUnits, Date lastUpdated, String adminUid) {
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.address = address;
        this.emergencyContact = emergencyContact;
        this.latitude = latitude;
        this.longitude = longitude;
        this.icuBeds = icuBeds;
        this.oxygenCylinders = oxygenCylinders;
        this.ventilators = ventilators;
        this.bloodUnits = bloodUnits != null ? bloodUnits : new HashMap<>();
        this.lastUpdated = lastUpdated;
        this.adminUid = adminUid;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    @PropertyName("name")
    public void setNameAlias(String nameAlias) {
        if ((hospitalName == null || hospitalName.trim().isEmpty()) && nameAlias != null) {
            hospitalName = nameAlias;
        }
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @PropertyName("lat")
    public void setLatAlias(double latAlias) {
        if (latitude == 0d && latAlias != 0d) {
            latitude = latAlias;
        }
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @PropertyName("lng")
    public void setLngAlias(double lngAlias) {
        if (longitude == 0d && lngAlias != 0d) {
            longitude = lngAlias;
        }
    }

    public int getIcuBeds() {
        return icuBeds;
    }

    public void setIcuBeds(int icuBeds) {
        this.icuBeds = icuBeds;
    }

    @PropertyName("icu_beds")
    public void setIcuBedsAlias(int value) {
        if (icuBeds == 0 && value > 0) {
            icuBeds = value;
        }
    }

    public int getOxygenCylinders() {
        return oxygenCylinders;
    }

    public void setOxygenCylinders(int oxygenCylinders) {
        this.oxygenCylinders = oxygenCylinders;
    }

    @PropertyName("oxygen_cylinders")
    public void setOxygenCylindersAlias(int value) {
        if (oxygenCylinders == 0 && value > 0) {
            oxygenCylinders = value;
        }
    }

    public int getVentilators() {
        return ventilators;
    }

    public void setVentilators(int ventilators) {
        this.ventilators = ventilators;
    }

    public Map<String, Integer> getBloodUnits() {
        return bloodUnits;
    }

    public void setBloodUnits(Map<String, Integer> bloodUnits) {
        this.bloodUnits = bloodUnits != null ? bloodUnits : new HashMap<>();
    }

    @PropertyName("blood_units")
    public void setBloodUnitsAlias(Map<String, Integer> bloodUnitsAlias) {
        if ((bloodUnits == null || bloodUnits.isEmpty()) && bloodUnitsAlias != null) {
            bloodUnits = bloodUnitsAlias;
        }
    }

    /**
     * Gets the blood unit count for a specific blood type.
     *
     * @param bloodType The blood type (e.g., "A+", "O-")
     * @return The count of blood units, or 0 if not found
     */
    @Exclude
    public int getBloodUnitCount(String bloodType) {
        return bloodUnits.getOrDefault(bloodType, 0);
    }

    /**
     * Sets the blood unit count for a specific blood type.
     *
     * @param bloodType The blood type (e.g., "A+", "O-")
     * @param count     The count of blood units
     */
    @Exclude
    public void setBloodUnitCount(String bloodType, int count) {
        bloodUnits.put(bloodType, count);
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getAdminUid() {
        return adminUid;
    }

    public void setAdminUid(String adminUid) {
        this.adminUid = adminUid;
    }

    @Override
    public String toString() {
        return "Hospital{" +
                "hospitalId='" + hospitalId + '\'' +
                ", hospitalName='" + hospitalName + '\'' +
                ", address='" + address + '\'' +
                ", emergencyContact='" + emergencyContact + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", icuBeds=" + icuBeds +
                ", oxygenCylinders=" + oxygenCylinders +
                ", ventilators=" + ventilators +
                ", bloodUnits=" + bloodUnits +
                ", lastUpdated=" + lastUpdated +
                ", adminUid='" + adminUid + '\'' +
                '}';
    }
}

