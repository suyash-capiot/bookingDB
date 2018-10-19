package com.coxandkings.travel.bookingengine.db.resource.managefailures;

import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.util.List;


public class FailureDetailsResource {

    private String failureFlag;

    private String bookID;

    private List<String> duplicateExists;

    private String bookingDate;

    private String clientId; //get it from client ID

    private String clientType;

    private String pointOfSale;

    private String companyDetails; // mapped to filed company

    private List<ProductSummary> productSummary;

    private String paymentStatus; // not in BE DB

    private String refundStatus; // not in BE DB

    private String reasonForFailure; // not in BE DB

    private List<ActionsResource>  actions; // not in BE DB

    private String fileHandlerName; //mapped to staff id

    public String getFailureFlag() {
        return failureFlag;
    }

    public void setFailureFlag(String failureFlag) {
        this.failureFlag = failureFlag;
    }

    public String getBookID() {
        return bookID;
    }

    public void setBookID(String bookID) {
        this.bookID = bookID;
    }

    public List<String> getDuplicateExists() {
        return duplicateExists;
    }

    public void setDuplicateExists(List<String> duplicateExists) {
        this.duplicateExists = duplicateExists;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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

    public List<ProductSummary> getProductSummary() {
        return productSummary;
    }

    public void setProductSummary(List<ProductSummary> productSummary) {
        this.productSummary = productSummary;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getReasonForFailure() {
        return reasonForFailure;
    }

    public void setReasonForFailure(String reasonForFailure) {
        this.reasonForFailure = reasonForFailure;
    }

    public List<ActionsResource> getActions() {
        return actions;
    }

    public void setActions(List<ActionsResource> actions) {
        this.actions = actions;
    }

    public String getFileHandlerName() {
        return fileHandlerName;
    }

    public void setFileHandlerName(String fileHandlerName) {
        this.fileHandlerName = fileHandlerName;
    }
}
