package com.coxandkings.travel.bookingengine.db.resource.mergebooking;

import org.json.JSONObject;

public class MergeBookingInfo {
    private String bookId;
    private String productCategory;
    private String productSubCategory;
    private String orderId;
    private String cityCode;
    private String countryCode;
    private String hotelCode;
    private String hotelName;
    private String roomId;
    private String checkInDate;
    private String checkOutDate;
    private String paxID;
    private String isLeadPax;
    private String roomTypeCode;
    private String roomCategoryId;
    private String roomRef;
    private String roomTypeName;
    private String roomCategoryName;
    private String mealCode;
    private String mealName;
    private String totalPrice;
    private JSONObject totalTaxBreakup;
    private String totalPriceCurrencyCode;
    private JSONObject supplierTaxBreakup;
    private String supplierPrice;
    private String supplierPriceCurrencyCode;


    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
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

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getHotelCode() {
        return hotelCode;
    }

    public void setHotelCode(String hotelCode) {
        this.hotelCode = hotelCode;
    }

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
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

    public String getPaxID() {
        return paxID;
    }

    public void setPaxID(String paxID) {
        this.paxID = paxID;
    }

    public String getIsLeadPax() {
        return isLeadPax;
    }

    public void setIsLeadPax(String isLeadPax) {
        this.isLeadPax = isLeadPax;
    }

    public String getRoomTypeCode() {
        return roomTypeCode;
    }

    public void setRoomTypeCode(String roomTypeCode) {
        this.roomTypeCode = roomTypeCode;
    }

    public String getRoomCategoryId() {
        return roomCategoryId;
    }

    public void setRoomCategoryId(String roomCategoryId) {
        this.roomCategoryId = roomCategoryId;
    }

    public String getRoomRef() {
        return roomRef;
    }

    public void setRoomRef(String roomRef) {
        this.roomRef = roomRef;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public String getRoomCategoryName() {
        return roomCategoryName;
    }

    public void setRoomCategoryName(String roomCategoryName) {
        this.roomCategoryName = roomCategoryName;
    }

    public String getMealCode() {
        return mealCode;
    }

    public void setMealCode(String mealCode) {
        this.mealCode = mealCode;
    }

    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public String getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(String totalPrice) {
        this.totalPrice = totalPrice;
    }

    public JSONObject getTotalTaxBreakup() {
        return totalTaxBreakup;
    }

    public void setTotalTaxBreakup(JSONObject totalTaxBreakup) {
        this.totalTaxBreakup = totalTaxBreakup;
    }

    public String getTotalPriceCurrencyCode() {
        return totalPriceCurrencyCode;
    }

    public void setTotalPriceCurrencyCode(String totalPriceCurrencyCode) {
        this.totalPriceCurrencyCode = totalPriceCurrencyCode;
    }

    public JSONObject getSupplierTaxBreakup() {
        return supplierTaxBreakup;
    }

    public void setSupplierTaxBreakup(JSONObject supplierTaxBreakup) {
        this.supplierTaxBreakup = supplierTaxBreakup;
    }

    public String getSupplierPrice() {
        return supplierPrice;
    }

    public void setSupplierPrice(String supplierPrice) {
        this.supplierPrice = supplierPrice;
    }

    public String getSupplierPriceCurrencyCode() {
        return supplierPriceCurrencyCode;
    }

    public void setSupplierPriceCurrencyCode(String supplierPriceCurrencyCode) {
        this.supplierPriceCurrencyCode = supplierPriceCurrencyCode;
    }
}
