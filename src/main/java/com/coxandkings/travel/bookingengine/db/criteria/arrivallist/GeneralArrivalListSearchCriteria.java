package com.coxandkings.travel.bookingengine.db.criteria.arrivallist;

public class GeneralArrivalListSearchCriteria {

    private String travelDateTime;
    private String bookingDateTime;
    private String productSubCategory;
    private String supplierId;
    private String clientType;
    private String clientGroupId;
    private String clientId;


    public String getTravelDateTime() {
        return travelDateTime;
    }

    public void setTravelDateTime(String travelDateTime) {
        this.travelDateTime = travelDateTime;
    }

    public String getBookingDateTime() {
        return bookingDateTime;
    }

    public void setBookingDateTime(String bookingDateTime) {
        this.bookingDateTime = bookingDateTime;
    }

    public String getProductSubCategory() {
        return productSubCategory;
    }

    public void setProductSubCategory(String productSubCategory) {
        this.productSubCategory = productSubCategory;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getClientGroupId() {
        return clientGroupId;
    }

    public void setClientGroupId(String clientGroupId) {
        this.clientGroupId = clientGroupId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
