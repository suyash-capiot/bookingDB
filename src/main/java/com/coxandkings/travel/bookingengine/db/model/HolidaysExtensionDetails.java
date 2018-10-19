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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.coxandkings.travel.bookingengine.db.orchestrator.HolidaysAccomodationComponentMethods;
import com.coxandkings.travel.bookingengine.db.orchestrator.HolidaysComponentsCommonMethods;
import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;



@Entity
@Table(name ="HOLIDAYSEXTENSIONDETAILS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class HolidaysExtensionDetails extends ProductOrder implements Serializable {
	
    private static final long serialVersionUID = 1L;
    
    @Id
    @Column()
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid",strategy = "uuid")
    protected String id;
    
    @OneToMany(mappedBy="extensionOrders", cascade=CascadeType.ALL)
	private Set<AccoRoomDetails> roomDetails;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="holidays_order_id", nullable=true)
    private HolidaysOrders holidaysOrders;
	
	@Column
    @Type(type = "StringJsonObject")
    private String paxDetails;
	
	@Column
    private String configType;
	
	@Column
	private String extensionType;//pre-night or post-night
	
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
	private String receivables;
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
    
	@Column
	private String amendDate;
	@Column
	private String cancelDate;
	@Column
	private String lastModifiedBy;
	@Transient
	private String operationType;
	
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
  public String getExtensionType() {
    return extensionType;
  }
  public void setExtensionType(String extensionType) {
    this.extensionType = extensionType;
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
  public Set<AccoRoomDetails> getRoomDetails() {
	return roomDetails;
  }
  public void setRoomDetails(Set<AccoRoomDetails> roomDetails) {
	this.roomDetails = roomDetails;
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
public String getOperationType() {
	return operationType;
}
public void setOperationType(String operationType) {
	this.operationType = operationType;
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
