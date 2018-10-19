package com.coxandkings.travel.bookingengine.db.resource.arrivallist;

public class ArrivalListGeneralInfo {

    private String bookID;
    private String supplierReferenceNumber;
    private String productCategory;
    private String productSubCategory;
    private String clientType;
    private String groupNameId;
    private String supplierId;
    private String clientId;//TODO : yet to add in stored proc

    public String getBookID() {
        return bookID;
    }

    public void setBookID(String bookID) {
        this.bookID = bookID;
    }

    public String getSupplierReferenceNumber() {
        return supplierReferenceNumber;
    }

    public void setSupplierReferenceNumber(String supplierReferenceNumber) {
        this.supplierReferenceNumber = supplierReferenceNumber;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getProductSubCategory() {
        return productSubCategory;
    }

    public void setProductSubCategory(String productSubCategory) {
        this.productSubCategory = productSubCategory;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getGroupNameId() {
        return groupNameId;
    }

    public void setGroupNameId(String groupNameId) {
        this.groupNameId = groupNameId;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
