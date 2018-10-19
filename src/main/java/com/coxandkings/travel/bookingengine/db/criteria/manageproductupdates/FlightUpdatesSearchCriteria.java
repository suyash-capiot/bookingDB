package com.coxandkings.travel.bookingengine.db.criteria.manageproductupdates;

public class FlightUpdatesSearchCriteria {
    private Integer pageNumber;
    private Integer size;
    private String airlineName;
    private String fromSector;
    private String toSector;
    private String flightNumber;
    private String flightTimingFrom;
    private String flightTimingTo;
    private String supplierId;

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getAirlineName() {
        return airlineName;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = airlineName;
    }

    public String getFromSector() {
        return fromSector;
    }

    public void setFromSector(String fromSector) {
        this.fromSector = fromSector;
    }

    public String getToSector() {
        return toSector;
    }

    public void setToSector(String toSector) {
        this.toSector = toSector;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getFlightTimingFrom() {
        return flightTimingFrom;
    }

    public void setFlightTimingFrom(String flightTimingFrom) {
        this.flightTimingFrom = flightTimingFrom;
    }

    public String getFlightTimingTo() {
        return flightTimingTo;
    }

    public void setFlightTimingTo(String flightTimingTo) {
        this.flightTimingTo = flightTimingTo;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }
}
