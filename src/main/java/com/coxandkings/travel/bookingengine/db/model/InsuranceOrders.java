package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.coxandkings.travel.bookingengine.db.orchestrator.HolidaysComponentsCommonMethods;
import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;



@Entity
@Table(name ="INSURANCEORDERS")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class InsuranceOrders extends ProductOrder implements Serializable,HolidaysComponentsCommonMethods {
	
    private static final long serialVersionUID = 1L;
    
   /* @Id
    @Column()
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid",strategy = "uuid")
    protected String id;*/
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="holidays_order_id", nullable=true)
    private HolidaysOrders holidaysOrders;
	
    @Column
	private String supplierID;
    
    @Column
    @Type(type = "StringJsonObject")
    private String paxDetails;
	
	@Column
	private String configType;
	
	@Column
    private String insuranceType;
	
	@Column
    private String status;
	
	//suppPriceInfo
	@Column
	private String supplierPriceBeforeTax;
	@Column
	private String supplierPriceAfterTax;
	@Column
	@Type(type = "StringJsonObject")
	private String suppPaxTypeFares;
	@Column
	private String supplierPrice;
	@Column
	private String supplierTaxAmount;
	@Column
	@Type(type = "StringJsonObject")
	private String supplierPriceTaxBreakup;
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
	@Type(type = "StringJsonObject")
	private String totalPaxTypeFares;
	@Column
	@Type(type = "StringJsonObject")
	private String receivables;
	@Column
	private String totalPrice;
	@Column
	private String totalTaxAmount;
	@Column
	@Type(type = "StringJsonObject")
	private String totalPriceTaxBreakup;
	@Column
	private String totalPriceCurrencyCode;
	
	//extrasInfo
	@Column
	private String insName;
	@Column
	private String insDescription;
	@Column
	private String insId;
		
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	@Column
	private String lastModifiedBy;
	
	@Column
	private String policyNumber;
	@Column
	private String policyNumberType;
	@Column
	private String refNumber;
	@Column
	private String refNumberType;
	@Column
	@Type(type = "StringJsonObject")
	private String planRestrictionCode;
	@Column
	private String policyDetailURL;
	@Column
	private String pfbPlanID;
	@Column
	private String pfbTypeID;
	@Column
	@Type(type = "StringJsonObject")
	private String coveredTrips;
	//Do I need to save the PlanCost of the request?
	@Column
	@Type(type = "StringJsonObject")
	private String insuCustDtls;
	
	public String getPfbPlanID() {
		return pfbPlanID;
	}
	public void setPfbPlanID(String pfbPlanID) {
		this.pfbPlanID = pfbPlanID;
	}
	public String getPfbTypeID() {
		return pfbTypeID;
	}
	public void setPfbTypeID(String pfbTypeID) {
		this.pfbTypeID = pfbTypeID;
	}
	public String getCoveredTrips() {
		return coveredTrips;
	}
	public void setCoveredTrips(String coveredTrips) {
		this.coveredTrips = coveredTrips;
	}
	public String getInsuCustDtls() {
		return insuCustDtls;
	}
	public void setInsuCustDtls(String insuCustDtls) {
		this.insuCustDtls = insuCustDtls;
	}
	public String getPolicyDetailURL() {
		return policyDetailURL;
	}
	public void setPolicyDetailURL(String policyDetailURL) {
		this.policyDetailURL = policyDetailURL;
	}
	public String getPolicyNumber() {
		return policyNumber;
	}
	public void setPolicyNumber(String policyNumber) {
		this.policyNumber = policyNumber;
	}
	public String getPolicyNumberType() {
		return policyNumberType;
	}
	public void setPolicyNumberType(String policyNumberType) {
		this.policyNumberType = policyNumberType;
	}
	public String getRefNumber() {
		return refNumber;
	}
	public void setRefNumber(String refNumber) {
		this.refNumber = refNumber;
	}
	public String getRefNumberType() {
		return refNumberType;
	}
	public void setRefNumberType(String refNumberType) {
		this.refNumberType = refNumberType;
	}
	public String getPlanRestrictionCode() {
		return planRestrictionCode;
	}
	public void setPlanRestrictionCode(String planRestrictionCode) {
		this.planRestrictionCode = planRestrictionCode;
	}
	public String getSupplierID() {
		return supplierID;
	}
	public void setSupplierID(String supplierID) {
		this.supplierID = supplierID;
	}
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getConfigType() {
    return configType;
  }
  public void setConfigType(String configType) {
    this.configType = configType;
  }
  public String getInsuranceType() {
    return insuranceType;
  }
  public void setInsuranceType(String insuranceType) {
    this.insuranceType = insuranceType;
  }
  public String getSupplierPrice() {
    return supplierPrice;
  }
  public void setSupplierPrice(String supplierPrice) {
    this.supplierPrice = supplierPrice;
  }
  public String getSupplierTaxAmount() {
    return supplierTaxAmount;
  }
  public void setSupplierTaxAmount(String supplierTaxAmount) {
    this.supplierTaxAmount = supplierTaxAmount;
  }
  public String getSupplierTaxBreakup() {
    return supplierPriceTaxBreakup;
  }
  public void setSupplierTaxBreakup(String supplierTaxBreakup) {
    this.supplierPriceTaxBreakup = supplierTaxBreakup;
  }
  public String getSupplierPriceCurrencyCode() {
    return supplierPriceCurrencyCode;
  }
  public void setSupplierPriceCurrencyCode(String supplierPriceCurrencyCode) {
    this.supplierPriceCurrencyCode = supplierPriceCurrencyCode;
  }
  public String getTotalPrice() {
    return totalPrice;
  }
  public void setTotalPrice(String totalPrice) {
    this.totalPrice = totalPrice;
  }
  public String getTotalTaxAmount() {
    return totalTaxAmount;
  }
  public void setTotalTaxAmount(String totalTaxAmount) {
    this.totalTaxAmount = totalTaxAmount;
  }
  public String getTotalTaxBreakup() {
    return totalPriceTaxBreakup;
  }
  public void setTotalTaxBreakup(String totalTaxBreakup) {
    this.totalPriceTaxBreakup = totalTaxBreakup;
  }
  public String getTotalPriceCurrencyCode() {
    return totalPriceCurrencyCode;
  }
  public void setTotalPriceCurrencyCode(String totalPriceCurrencyCode) {
    this.totalPriceCurrencyCode = totalPriceCurrencyCode;
  }
  public String getInsName() {
    return insName;
  }
  public void setInsName(String insName) {
    this.insName = insName;
  }
  public String getInsDescription() {
    return insDescription;
  }
  public void setInsDescription(String insDescription) {
    this.insDescription = insDescription;
  }
  public String getInsId() {
    return insId;
  }
  public void setInsId(String insId) {
    this.insId = insId;
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
  public HolidaysOrders getHolidaysOrders() {
    return holidaysOrders;
  }
  public void setHolidaysOrders(HolidaysOrders holidaysOrders) {
    this.holidaysOrders = holidaysOrders;
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
	return suppPaxTypeFares;
}
public void setSuppPaxTypeFares(String suppPaxTypeFares) {
	this.suppPaxTypeFares = suppPaxTypeFares;
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
	return totalPaxTypeFares;
}
public void setTotalPaxTypeFares(String totalPaxTypeFares) {
	this.totalPaxTypeFares = totalPaxTypeFares;
}
public String getStart() {
	// Added to make this model compliant with interface
	return null;
}
public String getDuration() {
	// Added to make this model compliant with interface
	return null;
}
public String getEnd() {
	// Added to make this model compliant with interface
	return null;
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
