package com.coxandkings.travel.bookingengine.db.criteria.arrivallist;

import com.coxandkings.travel.bookingengine.db.enums.ProductSubCategory;

public class HotelArrivalListSearchCriteria {
    private String checkInDate;
    private String bookingDateTime;
    private String supplierId;
    private String clientType;
    private String clientGroupId;
    private String clientId;
    private String continent; //Hotel Continent not availiable in address
    private String country; //Hotel country not avaliable in AccoOrders.address---
    private String city;//Hotel city not avaliable in AccoOrders.address---
    private String productName; //
    private String chain;
    private String isMysteryProduct;

    public String getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }

    public String getBookingDateTime() {
        return bookingDateTime;
    }

    public void setBookingDateTime(String bookingDateTime) {
        this.bookingDateTime = bookingDateTime;
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

    public String getContinent() {
        return continent;
    }

    public void setContinent(String continent) {
        this.continent = continent;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public String getIsMysteryProduct() {
        return isMysteryProduct;
    }

    public void setIsMysteryProduct(String isMysteryProduct) {
        this.isMysteryProduct = isMysteryProduct;
    }

}

