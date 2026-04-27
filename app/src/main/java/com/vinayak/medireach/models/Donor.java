package com.vinayak.medireach.models;

public class Donor {

    private String fullName;
    private String bloodGroup;
    private String city;
    private String phone;
    private String phoneNumber;
    private String lastDonationDate;
    private boolean isAvailable;
    private boolean organDonor;
    private String uid;

    public Donor() {
    }

    public Donor(String fullName, String bloodGroup, String city, String phone,
                 String lastDonationDate, boolean isAvailable, boolean organDonor, String uid) {
        this.fullName = fullName;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.phone = phone;
        this.phoneNumber = phone;
        this.lastDonationDate = lastDonationDate;
        this.isAvailable = isAvailable;
        this.organDonor = organDonor;
        this.uid = uid;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() {
        if (phone != null && !phone.isEmpty()) return phone;
        return phoneNumber;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        this.phoneNumber = phone;
    }

    public String getPhoneNumber() {
        if (phoneNumber != null && !phoneNumber.isEmpty()) return phoneNumber;
        return phone;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.phone = phoneNumber;
    }

    public String getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(String lastDonationDate) { this.lastDonationDate = lastDonationDate; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public boolean isOrganDonor() { return organDonor; }
    public void setOrganDonor(boolean organDonor) { this.organDonor = organDonor; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
}

