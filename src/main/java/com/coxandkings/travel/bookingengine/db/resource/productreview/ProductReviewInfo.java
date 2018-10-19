package com.coxandkings.travel.bookingengine.db.resource.productreview;

public class ProductReviewInfo {
    private String bookId;
    private String orderId;
    private String productSubCategory;
    private String clientId;
    private String clientType;
    private String groupNameId;

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductSubCategory() {
        return productSubCategory;
    }

    public void setProductSubCategory(String productSubCategory) {
        this.productSubCategory = productSubCategory;
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

    public String getGroupNameId() {
        return groupNameId;
    }

    public void setGroupNameId(String groupNameId) {
        this.groupNameId = groupNameId;
    }
}
