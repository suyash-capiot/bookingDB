package com.coxandkings.travel.bookingengine.db.criteria.managefailures;

public class DuplicateFlightBookingsSearchCriteria {

    private String airLineName;
    private String flightNumber;
    private String firstName;
    private String lastName;
    private String fromSector;
    private String toSector;
    private String travelFromDate;
    private String travelToDate;
    private String cabinType;

    public String getAirLineName() {
        return airLineName;
    }

    public void setAirLineName(String airLineName) {
        this.airLineName = airLineName;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
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

    public String getTravelFromDate() {
        return travelFromDate;
    }

    public void setTravelFromDate(String travelFromDate) {
        this.travelFromDate = travelFromDate;
    }

    public String getTravelToDate() {
        return travelToDate;
    }

    public void setTravelToDate(String travelToDate) {
        this.travelToDate = travelToDate;
    }

    public String getCabinType() {
        return cabinType;
    }

    public void setCabinType(String cabinType) {
        this.cabinType = cabinType;
    }
}
