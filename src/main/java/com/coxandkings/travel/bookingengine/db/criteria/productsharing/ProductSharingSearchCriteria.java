package com.coxandkings.travel.bookingengine.db.criteria.productsharing;

public class ProductSharingSearchCriteria {

    private String productNameSubTypeFlavour;
    private String checkInDate;
    private String checkOutDate;
    private String roomCategory;
    private String roomType;
    private String gender;
    private String isSharable;

    public String getProductNameSubTypeFlavour() {
        return productNameSubTypeFlavour;
    }

    public void setProductNameSubTypeFlavour(String productNameSubTypeFlavour) {
        this.productNameSubTypeFlavour = productNameSubTypeFlavour;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public String getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(String roomCategory) {
        this.roomCategory = roomCategory;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getIsSharable() {
        return isSharable;
    }

    public void setIsSharable(String isSharable) {
        this.isSharable = isSharable;
    }
}


