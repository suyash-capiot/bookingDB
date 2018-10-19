package com.coxandkings.travel.bookingengine.db.resource.arrivallist;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ArrivalListHotelInfo {
    private String bookID;
    private String supplierReferenceNumber;
    private String productCategory;
    private String productSubCategory;
    private String supplierId;
    private String checkInDate;
    private String checkOutDate;
    private String roomCategory;
    private String roomType;
    private String paxCount;
    private String firstName;
    private String lastName;
    private String paxType;
    private String totalRoomsInOrder;

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

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
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

    public String getPaxCount() {
        return paxCount;
    }

    public void setPaxCount(String paxCount) {
        this.paxCount = paxCount;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPaxType() {
        return paxType;
    }

    public void setPaxType(String paxType) {
        this.paxType = paxType;
    }

    public String getTotalRoomsInOrder() {
        return totalRoomsInOrder;
    }

    public void setTotalRoomsInOrder(String totalRoomsInOrder) {
        this.totalRoomsInOrder = totalRoomsInOrder;
    }
}
