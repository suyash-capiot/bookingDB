package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;
import com.fasterxml.jackson.annotation.JsonBackReference;



@Entity
@Table(name=  "HOLIDAYSORDERS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class HolidaysOrders extends ProductOrder implements Serializable{
	
    public HolidaysOrders() {
        super();
        operationType="update";
    }
  
	private static final long serialVersionUID = 1L;
	
	@Column
    @Type(type = "StringJsonObject")
    private String paxDetails;
	
	@OneToOne(mappedBy="holidaysOrders", cascade=CascadeType.ALL)
	private AccoOrders holidaysAccoDetails;
	
	@OneToMany(mappedBy="holidaysOrders", cascade=CascadeType.ALL)
	private Set<HolidaysExtensionDetails> holidaysExtensionDetails;
	
	@OneToMany(mappedBy="holidaysOrders", cascade=CascadeType.ALL)
	private Set<HolidaysExtrasDetails> holidaysExtrasDetails;
	
	@OneToMany(mappedBy="holidaysOrders", cascade=CascadeType.ALL)
	private Set<TransfersOrders> holidaysTransferDetails;
	
	@OneToMany(mappedBy="holidaysOrders", cascade=CascadeType.ALL)
	private Set<InsuranceOrders> holidaysInsuranceDetails;
	
	@OneToMany(mappedBy="holidaysOrders", cascade=CascadeType.ALL)
	private Set<ActivitiesOrders> holidaysActivitiesDetails;
	

	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	@Column
	private String supp_booking_reference;
	
	@Column
	private String suppierReservationId;
	@Column
	private String supplierReferenceId;
	@Column
	private String clientReferenceId;
	@Column
	private String supplierCancellationId;
	
	@Column
	private String status;
	@Column
	private String amendDate;
	@Column
	private String cancelDate;
	@Column
	private String lastModifiedBy;
	@Column
	private String supplierID;
	
	//TODO: these are the new fields added after ops team's ssuggestion
    @Column
    private String destination;
    @Column
    private String multiCurrencyBooking;
    @Column
    private String country;
    @Column
    private String city;
    @Column
    private String noOfNights;
    @Column
    private String productName;
    @Column
    private String productFlavourName;
    @Column
    private String packageType;
    @Column
    private String brand;
    @Column
    private String companyPackageName;
    @Column
    private String supplierReferenceNumber;
    @Column
    private String supplierName ;
	
	
    @Column
    @Type(type = "StringJsonObject")
    private String companyTaxes;
    @Column
    private String supplierPriceBeforeTax;
    @Column
    private String supplierPriceAfterTax;
    @Column
    private String supplierPriceCurrencyCode;
    @Column
    private String supplierTaxAmount;
    @Column
    private String totalPriceBeforeTax;
    @Column
    private String totalPriceAfterTax;
    @Column
    private String totalPriceCurrencyCode;  
    @Column
    private String totalTaxAmount;
    @Column
    @Type(type = "StringJsonObject")
    private String totalPriceTaxes;
    @Column
    @Type(type = "StringJsonObject")
    private String suppPriceTaxes;
	@Column
	@Type(type = "StringJsonObject")
	private String receivables;
	
    @Transient
    private String operationType;
    
    //fields specific to packages
    @Column
	private String tourType;
	@Column
	private String brandName;
	@Column
	private String tourCode;
	@Column
	private String subTourCode;
	@Column
	private String travelStartDate;
	@Column
	private String travelEndDate;
	@Column
	private String tourName;
	@Column
	private String tourStart;
	@Column
	private String tourEnd;
	@Column
	private String tourStartCity;
	@Column
	private String tourEndCity;
	
 public AccoOrders getAccoOrders() {
    return holidaysAccoDetails;
  }
  public void setAccoOrders(AccoOrders holidaysAccoDetails) {
    this.holidaysAccoDetails = holidaysAccoDetails;
  }
  public Set<HolidaysExtensionDetails> getHolidaysExtensionDetails() {
    return holidaysExtensionDetails;
  }
  public void setHolidaysExtensionDetails(Set<HolidaysExtensionDetails> holidaysExtensionDetails) {
    this.holidaysExtensionDetails = holidaysExtensionDetails;
  }
  public Set<HolidaysExtrasDetails> getHolidaysExtrasDetails() {
    return holidaysExtrasDetails;
  }
  public void setHolidaysExtrasDetails(Set<HolidaysExtrasDetails> holidaysExtrasDetails) {
    this.holidaysExtrasDetails = holidaysExtrasDetails;
  }
  public Set<TransfersOrders> getTransfersOrders() {
    return holidaysTransferDetails;
  }
  public void setTransfersOrders(Set<TransfersOrders> holidaysTransferDetails) {
    this.holidaysTransferDetails = holidaysTransferDetails;
  }
  public Set<InsuranceOrders> getInsuranceOrders() {
    return holidaysInsuranceDetails;
  }
  public void setInsuranceOrders(Set<InsuranceOrders> holidaysInsuranceDetails) {
    this.holidaysInsuranceDetails = holidaysInsuranceDetails;
  }
  public Set<ActivitiesOrders> getActivitiesOrders() {
    return holidaysActivitiesDetails;
  }
  public void setActivitiesOrders(Set<ActivitiesOrders> holidaysActivitiesDetails) {
    this.holidaysActivitiesDetails = holidaysActivitiesDetails;
  }
  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }
  public ZonedDateTime getLastModifiedAt() {
    return lastModifiedAt;
  }
  public void setLastModifiedAt(ZonedDateTime lastModifiedAt) {
    this.lastModifiedAt = lastModifiedAt;
  }
  public String getSupp_booking_reference() {
    return supp_booking_reference;
  }
  public void setSupp_booking_reference(String supp_booking_reference) {
    this.supp_booking_reference = supp_booking_reference;
  }
  @Override
  public String getStatus() {
    return status;
  }
  @Override
  public void setStatus(String status) {
    this.status = status;
  }
  public String getAmendDate() {
    return amendDate;
  }
  public void setAmendDate(String amendDate) {
    this.amendDate = amendDate;
  }
  public String getCancelDate() {
    return cancelDate;
  }
  public void setCancelDate(String cancelDate) {
    this.cancelDate = cancelDate;
  }
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }
  public String getSupplierID() {
    return supplierID;
  }
  public void setSupplierID(String supplierID) {
    this.supplierID = supplierID;
  }
  public String getSupplierPriceCurrencyCode() {
    return supplierPriceCurrencyCode;
  }
  public void setSupplierPriceCurrencyCode(String supplierPriceCurrencyCode) {
    this.supplierPriceCurrencyCode = supplierPriceCurrencyCode;
  }
  public String getSupplierPriceBeforeTax() {
	return supplierPriceBeforeTax;
}
public void setSupplierPriceBeforeTax(String supplierPriceBeforeTax) {
	this.supplierPriceBeforeTax = supplierPriceBeforeTax;
}
public String getSupplierPriceAfterTax() {
	return supplierPriceAfterTax;
}
public void setSupplierPriceAfterTax(String supplierPriceAfterTax) {
	this.supplierPriceAfterTax = supplierPriceAfterTax;
}
public String getTotalPriceBeforeTax() {
	return totalPriceBeforeTax;
}
public void setTotalPriceBeforeTax(String totalPriceBeforeTax) {
	this.totalPriceBeforeTax = totalPriceBeforeTax;
}
public String getTotalPriceAfterTax() {
	return totalPriceAfterTax;
}
public void setTotalPriceAfterTax(String totalPriceAfterTax) {
	this.totalPriceAfterTax = totalPriceAfterTax;
}
public String getTotalPriceCurrencyCode() {
    return totalPriceCurrencyCode;
  }
  public void setTotalPriceCurrencyCode(String totalPriceCurrencyCode) {
    this.totalPriceCurrencyCode = totalPriceCurrencyCode;
  }
  public String getTotalPriceTaxes() {
    return totalPriceTaxes;
  }
  public void setTotalPriceTaxes(String totalPriceTaxes) {
    this.totalPriceTaxes = totalPriceTaxes;
  }
  public String getSuppPriceTaxes() {
    return suppPriceTaxes;
  }
  public void setSuppPriceTaxes(String suppPriceTaxes) {
    this.suppPriceTaxes = suppPriceTaxes;
  }
  public String getOperationType() {
    return operationType;
  }
  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }
  public String getBrandName() {
    return brandName;
  }
  public void setBrandName(String brandName) {
    this.brandName = brandName;
  }
  public String getTourCode() {
    return tourCode;
  }
  public void setTourCode(String tourCode) {
    this.tourCode = tourCode;
  }
  public String getSubTourCode() {
    return subTourCode;
  }
  public void setSubTourCode(String subTourCode) {
    this.subTourCode = subTourCode;
  }
  public String getTravelStartDate() {
    return travelStartDate;
  }
  public void setTravelStartDate(String travelStartDate) {
    this.travelStartDate = travelStartDate;
  }
  public String getTravelEndDate() {
    return travelEndDate;
  }
  public void setTravelEndDate(String travelEndDate) {
    this.travelEndDate = travelEndDate;
  }
  public String getTourName() {
    return tourName;
  }
  public void setTourName(String tourName) {
    this.tourName = tourName;
  }
  public String getTourStart() {
    return tourStart;
  }
  public void setTourStart(String tourStart) {
    this.tourStart = tourStart;
  }
  public String getTourEnd() {
    return tourEnd;
  }
  public void setTourEnd(String tourEnd) {
    this.tourEnd = tourEnd;
  }
  public String getTourStartCity() {
    return tourStartCity;
  }
  public void setTourStartCity(String tourStartCity) {
    this.tourStartCity = tourStartCity;
  }
  public String getTourEndCity() {
    return tourEndCity;
  }
  public void setTourEndCity(String tourEndCity) {
    this.tourEndCity = tourEndCity;
  }
  public static long getSerialversionuid() {
    return serialVersionUID;
  }
  public String getPaxDetails() {
    return paxDetails;
  }
  public void setPaxDetails(String paxDetails) {
    this.paxDetails = paxDetails;
  }
public String getSupplierTaxAmount() {
	return supplierTaxAmount;
}
public void setSupplierTaxAmount(String supplierTaxAmount) {
	this.supplierTaxAmount = supplierTaxAmount;
}
public String getTotalTaxAmount() {
	return totalTaxAmount;
}
public void setTotalTaxAmount(String totalTaxAmount) {
	this.totalTaxAmount = totalTaxAmount;
}
public String getDestination() {
	return destination;
}
public void setDestination(String destination) {
	this.destination = destination;
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
public String getNoOfNights() {
	return noOfNights;
}
public void setNoOfNights(String noOfNights) {
	this.noOfNights = noOfNights;
}
public String getProductName() {
	return productName;
}
public void setProductName(String productName) {
	this.productName = productName;
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
public String getSupplierReferenceNumber() {
	return supplierReferenceNumber;
}
public void setSupplierReferenceNumber(String supplierReferenceNumber) {
	this.supplierReferenceNumber = supplierReferenceNumber;
}
public String getSupplierName() {
	return supplierName;
}
public void setSupplierName(String supplierName) {
	this.supplierName = supplierName;
}
public String getTourType() {
	return tourType;
}
public void setTourType(String tourType) {
	this.tourType = tourType;
}
public String getReceivables() {
	return receivables;
}
public void setReceivables(String receivables) {
	this.receivables = receivables;
}
public String getCompanyTaxes() {
	return companyTaxes;
}
public void setCompanyTaxes(String companyTaxes) {
	this.companyTaxes = companyTaxes;
}
public String getSuppierReservationId() {
	return suppierReservationId;
}
public void setSuppierReservationId(String suppierReservationId) {
	this.suppierReservationId = suppierReservationId;
}
public String getSupplierReferenceId() {
	return supplierReferenceId;
}
public void setSupplierReferenceId(String supplierReferenceId) {
	this.supplierReferenceId = supplierReferenceId;
}
public String getClientReferenceId() {
	return clientReferenceId;
}
public void setClientReferenceId(String clientReferenceId) {
	this.clientReferenceId = clientReferenceId;
}
public String getSupplierCancellationId() {
	return supplierCancellationId;
}
public void setSupplierCancellationId(String supplierCancellationId) {
	this.supplierCancellationId = supplierCancellationId;
}
public String getMultiCurrencyBooking() {
	return multiCurrencyBooking;
}
public void setMultiCurrencyBooking(String multiCurrencyFlag) {
	this.multiCurrencyBooking = multiCurrencyFlag;
}
}
