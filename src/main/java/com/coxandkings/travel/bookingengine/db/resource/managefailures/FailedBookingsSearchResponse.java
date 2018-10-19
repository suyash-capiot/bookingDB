package com.coxandkings.travel.bookingengine.db.resource.managefailures;

import org.json.JSONObject;

public class FailedBookingsSearchResponse {
    private String bookID;
    private String bookingDate;
    private String clientID;
    private String clientType;
    private String pointOfSale;
    private String companyDetails;
    private String fileHandlerId;
    private String bookingAttribute;
    private String productSubCategory ;
    private String orderStatus;
    private String orderId;
    private String travelDate;
    private String detailsSummary;
    private String supplierId;

    public String getBookID() {
        return bookID;
    }

    public void setBookID(String bookID) {
        this.bookID = bookID;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getPointOfSale() {
        return pointOfSale;
    }

    public void setPointOfSale(String pointOfSale) {
        this.pointOfSale = pointOfSale;
    }

    public String getCompanyDetails() {
        return companyDetails;
    }

    public void setCompanyDetails(String companyDetails) {
        this.companyDetails = companyDetails;
    }

    public String getFileHandlerId() {
        return fileHandlerId;
    }

    public void setFileHandlerId(String fileHandlerId) {
        this.fileHandlerId = fileHandlerId;
    }


    public String getBookingAttribute() {
        return bookingAttribute;
    }

    public void setBookingAttribute(String bookingAttribute) {
        this.bookingAttribute = bookingAttribute;
    }

    public String getProductSubCategory() {
        return productSubCategory;
    }

    public void setProductSubCategory(String productSubCategory) {
        this.productSubCategory = productSubCategory;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(String travelDate) {
        this.travelDate = travelDate;
    }

    public String getDetailsSummary() {
        return detailsSummary;
    }

    public void setDetailsSummary(String detailsSummary) {
        this.detailsSummary = detailsSummary;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }
}
