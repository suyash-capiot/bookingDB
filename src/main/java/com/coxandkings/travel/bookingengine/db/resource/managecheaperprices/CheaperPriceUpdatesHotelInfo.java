package com.coxandkings.travel.bookingengine.db.resource.managecheaperprices;

public class CheaperPriceUpdatesHotelInfo extends CheaperPriceBookingInfo {

    private String productSubCategory;

    private String checkInDate;

    private String checkoutDate;

    private String roomCategoryID;

    private String roomCategoryName;

    private String roomType;

    private String mealPlan;

    private String mealCode;

    private String rating;

    private String status;

    public CheaperPriceUpdatesHotelInfo()    {
    }

    public String getProductSubCategory() {
        return productSubCategory;
    }

    public void setProductSubCategory(String productSubCategory) {
        this.productSubCategory = productSubCategory;
    }

    public String getRoomCategoryID() {
        return roomCategoryID;
    }

    public void setRoomCategoryID(String roomCategoryID) {
        this.roomCategoryID = roomCategoryID;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getMealPlan() {
        return mealPlan;
    }

    public void setMealPlan(String mealPlan) {
        this.mealPlan = mealPlan;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getMealCode() {
        return mealCode;
    }

    public void setMealCode(String mealCode) {
        this.mealCode = mealCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getCheckoutDate() {
        return checkoutDate;
    }

    public void setCheckoutDate(String checkoutDate) {
        this.checkoutDate = checkoutDate;
    }

    public String getRoomCategoryName() {
        return roomCategoryName;
    }

    public void setRoomCategoryName(String roomCategoryName) {
        this.roomCategoryName = roomCategoryName;
    }
}
