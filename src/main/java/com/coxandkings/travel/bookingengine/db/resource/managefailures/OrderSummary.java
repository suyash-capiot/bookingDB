package com.coxandkings.travel.bookingengine.db.resource.managefailures;

public class OrderSummary {

    private String travelDate;

    private String detailsSummary;

    private String supplierId; //mapped to supplier id


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
