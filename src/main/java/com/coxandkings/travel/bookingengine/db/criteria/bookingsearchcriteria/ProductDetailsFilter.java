package com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria;

public class ProductDetailsFilter {

    private String productCategoryId; // productCategory - Done

    private String productCategorySubTypeId; //productSubCategory - Done

    private String supplierName; // source supplier name - DONE

    private String travelFromDate; // travelFromDate - flights

    private String  travelToDate; //travelToDate - flights

    private String airlinePNR; // airlinePNR - Done

    private String airlineName;

    private String gsdPnr; //GdsPNR - fights - Done

    private String ticketNumber; //ticketingpnr - flights - Done
	
	private String productFlavourName; // productFlavourName - for packages
    
    private String packageType; // packageType - for packages
    
    private String brand; // brand - for packages
    
    private String companyPackageName; // companyPackageName - for packages
    
    private String noOfNights; // noOfNights - for packages

    private String destination; // destination - for packages

    private String country; // countrycode - hotel - done

    private String city; // citycode - hotel - done

    private String SupplierReferenceNumber; // supplierreferenceid - hotel - done

    private String productName;


    public String getProductCategoryId() {
        return productCategoryId;
    }

    public void setProductCategoryId(String productCategoryId) {
        this.productCategoryId = productCategoryId;
    }

    public String getProductCategorySubTypeId() {
        return productCategorySubTypeId;
    }

    public void setProductCategorySubTypeId(String productCategorySubTypeId) {
        this.productCategorySubTypeId = productCategorySubTypeId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getAirlinePNR() {
        return airlinePNR;
    }

    public void setAirlinePNR(String airlinePNR) {
        this.airlinePNR = airlinePNR;
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

    public String getGsdPnr() {
        return gsdPnr;
    }

    public void setGsdPnr(String gsdPnr) {
        this.gsdPnr = gsdPnr;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
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

    public String getSupplierReferenceNumber() {
        return SupplierReferenceNumber;
    }

    public void setSupplierReferenceNumber(String supplierReferenceNumber) {
        SupplierReferenceNumber = supplierReferenceNumber;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getAirlineName() {
        return airlineName;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = airlineName;
    }
	
	    public String getProductFlavourName() {
      return productFlavourName;
    }

    public void setProductFlavourName(String productFlavourName) {
      this.productFlavourName = productFlavourName;
    }

    public String getPackageType() {
      return packageType;
    }

    public void setPackageType(String packageType) {
      this.packageType = packageType;
    }

    public String getBrand() {
      return brand;
    }

    public void setBrand(String brand) {
      this.brand = brand;
    }

    public String getCompanyPackageName() {
      return companyPackageName;
    }

    public void setCompanyPackageName(String companyPackageName) {
      this.companyPackageName = companyPackageName;
    }

    public String getNoOfNights() {
      return noOfNights;
    }

    public void setNoOfNights(String noOfNights) {
      this.noOfNights = noOfNights;
    }

    public String getDestination() {
      return destination;
    }

    public void setDestination(String destination) {
      this.destination = destination;
    }
    
 
}
