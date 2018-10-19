package com.coxandkings.travel.bookingengine.db.resource.searchviewfilter;

import java.util.List;

public class BookingSearchResponseItem {

    private String bookID;
    private String createdDate;
    private String clientType;
    private String clientID;
    private String clientName;
    private String company;
    private String companyName;
    private String status ;
    private String staffID;
    private String staffName;
    private String paymentStatus;
    private String productSummary;
    private String pointOfSale;
    private Integer numberOfPages;

    private List<String> productSubCategories;

    public String getBookID() {
        return bookID;
    }

    public String getClientType() {
        return clientType;
    }

    public String getClientID() {
        return clientID;
    }

    public String getCompany() {
        return company;
    }

    public String getStatus() {
        return status;
    }


    public void setBookID(String bookID) {
        this.bookID = bookID;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public List<String> getProductSubCategories() {
        return productSubCategories;
    }

    public void setProductSubCategories(List<String> productSubCategories) {
        this.productSubCategories = productSubCategories;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getStaffID() {
        return staffID;
    }

    public void setStaffID(String staffID) {
        this.staffID = staffID;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getProductSummary() {
        return productSummary;
    }

    public void setProductSummary(String productSummary) {
        this.productSummary = productSummary;
    }

    public String getPointOfSale() {
        return pointOfSale;
    }

    public void setPointOfSale(String pointOfSale) {
        this.pointOfSale = pointOfSale;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }
}
