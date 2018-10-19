package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.coxandkings.travel.bookingengine.db.orchestrator.HolidaysComponentsCommonMethods;
import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;



@Entity
@Table(name ="HOLIDAYSEXTRASDETAILS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class HolidaysExtrasDetails extends ProductOrder implements Serializable,HolidaysComponentsCommonMethods {
	
    private static final long serialVersionUID = 1L;
    
    @Id
    @Column()
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid",strategy = "uuid")
    protected String id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="holidays_order_id",nullable=true)
    private HolidaysOrders holidaysOrders;
	
	@Column
    @Type(type = "StringJsonObject")
    private String paxDetails;
	
	@Column
    private String configType;
	
	@Column
	private String extraType;
	
	@Column
    private String status;
	
	//suppPriceInfo
	@Column
	private String supplierPriceBeforeTax;
	@Column
	private String supplierPriceAfterTax;
	@Column
	private String supplierTaxAmount;
	@Column
	@Type(type = "StringJsonObject")
	private String supplierTaxBreakup;
	@Column
	private String supplierPriceCurrencyCode;
	@Column
	@Type(type = "StringJsonObject")
	private String supplierPerPaxFare;
	
	@Column
	@Type(type = "StringJsonObject")
	private String supplierCommercials;
	@Column
	@Type(type = "StringJsonObject")
	private String clientCommercials;
	
	//totalPriceInfo
	@Column
	private String totalPriceBeforeTax;
	@Column
	private String totalPriceAfterTax;
	@Column
	private String totalTaxAmount;
	@Column
	@Type(type = "StringJsonObject")
	private String totalTaxBreakup;
	@Column
	private String totalPriceCurrencyCode;
	@Column
	@Type(type = "StringJsonObject")
	private String totalPaxTypeFare;
	@Column
	@Type(type = "StringJsonObject")
	private String receivables;
	
/*	//Adding Supplier and Client commercials
    @Column
    @Type(type = "StringJsonObject")
    private String suppCommercials;
    @Column
    @Type(type = "StringJsonObject")
    private String clientCommercials;*/
	
	//extrasInfo
	@Column
	private String availabilityStatus;
	@Column
	private String extraName;
	@Column
	private String extraCode;
	@Column
	private String extraQuantity;
	@Column
	private String extraDescription;
	
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	@Column
	private String lastModifiedBy;
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public HolidaysOrders getHolidaysOrders() {
    return holidaysOrders;
  }
  public void setHolidaysOrders(HolidaysOrders holidaysOrders) {
    this.holidaysOrders = holidaysOrders;
  }
  public String getConfigType() {
    return configType;
  }
  public void setConfigType(String configType) {
    this.configType = configType;
  }
  public String getExtraType() {
    return extraType;
  }
  public void setExtraType(String extraType) {
    this.extraType = extraType;
  }
  public String getSupplierTaxAmount() {
    return supplierTaxAmount;
  }
  public void setSupplierTaxAmount(String supplierTaxAmount) {
    this.supplierTaxAmount = supplierTaxAmount;
  }
  public String getSupplierTaxBreakup() {
    return supplierTaxBreakup;
  }
  public void setSupplierTaxBreakup(String supplierTaxBreakup) {
    this.supplierTaxBreakup = supplierTaxBreakup;
  }
  public String getSupplierPriceCurrencyCode() {
    return supplierPriceCurrencyCode;
  }
  public void setSupplierPriceCurrencyCode(String supplierPriceCurrencyCode) {
    this.supplierPriceCurrencyCode = supplierPriceCurrencyCode;
  }
  public String getTotalTaxAmount() {
    return totalTaxAmount;
  }
  public void setTotalTaxAmount(String totalTaxAmount) {
    this.totalTaxAmount = totalTaxAmount;
  }
  public String getTotalTaxBreakup() {
    return totalTaxBreakup;
  }
  public void setTotalTaxBreakup(String totalTaxBreakup) {
    this.totalTaxBreakup = totalTaxBreakup;
  }
  public String getTotalPriceCurrencyCode() {
    return totalPriceCurrencyCode;
  }
  public void setTotalPriceCurrencyCode(String totalPriceCurrencyCode) {
    this.totalPriceCurrencyCode = totalPriceCurrencyCode;
  }
/*  public String getSuppCommercials() {
    return suppCommercials;
  }
  public void setSuppCommercials(String suppCommercials) {
    this.suppCommercials = suppCommercials;
  }
  public String getClientCommercials() {
    return clientCommercials;
  }
  public void setClientCommercials(String clientCommercials) {
    this.clientCommercials = clientCommercials;
  }*/
  public String getAvailabilityStatus() {
    return availabilityStatus;
  }
  public void setAvailabilityStatus(String availabilityStatus) {
    this.availabilityStatus = availabilityStatus;
  }
  public String getExtraName() {
    return extraName;
  }
  public void setExtraName(String extraName) {
    this.extraName = extraName;
  }
  public String getExtraCode() {
    return extraCode;
  }
  public void setExtraCode(String extraCode) {
    this.extraCode = extraCode;
  }
  public String getExtraQuantity() {
    return extraQuantity;
  }
  public void setExtraQuantity(String extraQuantity) {
    this.extraQuantity = extraQuantity;
  }
  public String getExtraDescription() {
    return extraDescription;
  }
  public void setExtraDescription(String extraDescription) {
    this.extraDescription = extraDescription;
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
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }
  public static long getSerialversionuid() {
    return serialVersionUID;
  }
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }
  public String getPaxDetails() {
    return paxDetails;
  }
  public void setPaxDetails(String paxDetails) {
    this.paxDetails = paxDetails;
  }
  public String getStart() {
	//Added to make this Model interface compliant
	return null;
  }
  public String getDuration() {
	//Added to make this Model interface compliant
	return null;
  }
  public String getEnd() {
	//Added to make this Model interface compliant
	return null;
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
public String getSupplierPaxTypeFares() {
	return supplierPerPaxFare;
}
public void setSupplierPaxTypeFares(String supplierPerPaxFare) {
	this.supplierPerPaxFare = supplierPerPaxFare;
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
public String getTotalPaxTypeFares() {
	return totalPaxTypeFare;
}
public void setTotalPaxTypeFares(String totalPerPaxFare) {
	this.totalPaxTypeFare = totalPerPaxFare;
}
public String getSupplierPerPaxFare() {
	return supplierPerPaxFare;
}
public void setSupplierPerPaxFare(String supplierPerPaxFare) {
	this.supplierPerPaxFare = supplierPerPaxFare;
}
public String getReceivables() {
	return receivables;
}
public void setReceivables(String receivables) {
	this.receivables = receivables;
}
public String getSupplierCommercials() {
	return supplierCommercials;
}
public void setSupplierCommercials(String supplierCommercials) {
	this.supplierCommercials = supplierCommercials;
}
public String getClientCommercials() {
	return clientCommercials;
}
public void setClientCommercials(String clientCommercials) {
	this.clientCommercials = clientCommercials;
}
}
