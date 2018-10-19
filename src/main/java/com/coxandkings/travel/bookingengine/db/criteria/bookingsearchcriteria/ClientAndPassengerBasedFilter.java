package com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria;

public class ClientAndPassengerBasedFilter  {

    private String clientType; // clientType - Booking Class - Done

    private String clientCategoryId; // Not Done

    private String clientSubCategoryId;  // Not Done

    private String clientId; // clientID - Done

    private String passengerName;

    private String phoneNumber;

    private String emailId;

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getClientCategoryId() {
        return clientCategoryId;
    }

    public void setClientCategoryId(String clientCategoryId) {
        this.clientCategoryId = clientCategoryId;
    }

    public String getClientSubCategoryId() {
        return clientSubCategoryId;
    }

    public void setClientSubCategoryId(String clientSubCategoryId) {
        this.clientSubCategoryId = clientSubCategoryId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }
}
