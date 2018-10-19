package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;

@Entity
@Table(name="BOOKING")
@TypeDefs( {@TypeDef( name= "StringJsonObject", typeClass = StringJsonUserType.class)})
public class Booking implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name="BOOKID")
	private String bookID;
	
	@OneToMany(mappedBy="booking", cascade=CascadeType.ALL)
	private Set<PaymentInfo> paymentInfo;
	
	@OneToMany(mappedBy="booking", cascade=CascadeType.ALL)
	private Set<ProductOrder> productOrders;

	@Column
	private String userID;
	@Column
	private String sessionID;
	@Column
	private String transactionID;
	@Column
	private String clientID;
	@Column
	private String branchID;
	@Column
	private String staffID;
	@Column
	private String travelAgentID;
	@Column
	private String clientType;
	@Column
	private String clientCurrency;
	@Column
	private String clientLanguage;
	@Column
	private String clientMarket;
	@Column
	private String clientNationality;
	@Column
	private String clientIATANumber;
	@Column
	private String status;
	@Column
	private boolean isHolidayBooking;
	@Column
	private String enquiryID;
	@Column
	private String quoteID;
    @Column 
    private String pos;
    @Column
	@Type(type = "StringJsonObject")
    private String documentIds;
    @Column
   	@Type(type = "StringJsonObject")
    private String notes;

	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	@Column
	private String lastModifiedBy;
	
	@Column
	private String QCStatus;
	
	@Column
	private String mergeID;
	
	@Column
	private String companyId;
	@Column
	private String groupOfComapniesId;
	@Column
	private String groupCompanyID;
	@Column
	private String BU;
	@Column
	private String SBU;

	@Column
	private String channel;
	
	@Column 
	@Type(type = "StringJsonObject")
	private String duplicateOf;

	@Column
	private String bookingType;
	
	@Column
	private String productsCount;

	/*@Column
	private String userID;*/

	
	//Fields added after ops team's suggestions



	public String getMergeID() {
		return mergeID;
	}


	public String getNotes() {
		return notes;
	}


	public void setNotes(String notes) {
		this.notes = notes;
	}


	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getDuplicateOf() {
		return duplicateOf;
	}

	public void setDuplicateOf(String duplicateOf) {
		this.duplicateOf = duplicateOf;
	}

	public String getDocumentIds() {
		return documentIds;
	}

	public void setDocumentIds(String documentIds) {
		this.documentIds = documentIds;
	}

	public String getPos() {
		return pos;
	}
	public void setPos(String pos) {
		this.pos = pos;
	}
	public void setMergeID(String mergeID) {
		this.mergeID = mergeID;
	}
	public Set<ProductOrder> getProductOrders() {
		return productOrders;
	}
	public void setProductOrders(Set<ProductOrder> productOrders) {
		this.productOrders = productOrders;
	}
	public Set<PaymentInfo> getPaymentInfo() {
		return paymentInfo;
	}
	public void setPaymentInfo(Set<PaymentInfo> paymentInfo) {
		this.paymentInfo = paymentInfo;
	}
	public String getBookID() {
		return bookID;
	}
	public void setBookID(String bookID) {
		this.bookID = bookID;
	}
	
	public String getBU() {
		return BU;
	}


	public void setBU(String bU) {
		BU = bU;
	}


	public String getSBU() {
		return SBU;
	}


	public void setSBU(String sBU) {
		SBU = sBU;
	}


	public String getSessionID() {
		return sessionID;
	}
	public String getQCStatus() {
		return QCStatus;
	}
	public void setQCStatus(String qCStatus) {
		QCStatus = qCStatus;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
	public String getTransactionID() {
		return transactionID;
	}
	public void setTransactionID(String transactionID) {
		this.transactionID = transactionID;
	}
	public String getClientID() {
		return clientID;
	}
	
	public void setClientID(String clientID) {
		this.clientID = clientID;
	}
	public String getBranchID() {
		return branchID;
	}
	public void setBranchID(String branchID) {
		this.branchID = branchID;
	}
	public String getStaffID() {
		return staffID;
	}
	public void setStaffID(String staffID) {
		this.staffID = staffID;
	}
	public String getTravelAgentID() {
		return travelAgentID;
	}
	public void setTravelAgentID(String travelAgentID) {
		this.travelAgentID = travelAgentID;
	}
	public String getClientType() {
		return clientType;
	}
	public void setClientType(String clientType) {
		this.clientType = clientType;
	}
	public String getClientCurrency() {
		return clientCurrency;
	}
	public void setClientCurrency(String clientCurrency) {
		this.clientCurrency = clientCurrency;
	}
	public String getClientIATANumber() {
		return clientIATANumber;
	}
	public void setClientIATANumber(String clientIATANumber) {
		this.clientIATANumber = clientIATANumber;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	public boolean isHolidayBooking() {
		return isHolidayBooking;
	}

	public void setHolidayBooking(boolean isHolidayBooking) {
		this.isHolidayBooking = isHolidayBooking;
	}


	public String getProductsCount() {
		return productsCount;
	}


	public void setProductsCount(String productsCount) {
		this.productsCount = productsCount;
	}


	public String getLastModifiedBy() {
		return lastModifiedBy;
	}
	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}
	public String getEnquiryID() {
		return enquiryID;
	}
	public void setEnquiryID(String enquiryID) {
		this.enquiryID = enquiryID;
	}
	public String getQuoteID() {
		return quoteID;
	}
	public void setQuoteID(String quoteID) {
		this.quoteID = quoteID;
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
	
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public String getClientLanguage() {
		return clientLanguage;
	}
	public void setClientLanguage(String clientLanguage) {
		this.clientLanguage = clientLanguage;
	}
	public String getClientMarket() {
		return clientMarket;
	}
	public void setClientMarket(String clientMarket) {
		this.clientMarket = clientMarket;
	}
	public String getClientNationality() {
		return clientNationality;
	}
	public void setClientNationality(String clientNationality) {
		this.clientNationality = clientNationality;
	}
	
	public String getGroupOfComapniesId() {
		return groupOfComapniesId;
	}
	public void setGroupOfComapniesId(String groupOfComapniesId) {
		this.groupOfComapniesId = groupOfComapniesId;
	}
	
	public String getGroupCompanyID() {
		return groupCompanyID;
	}


	public void setGroupCompanyID(String groupCompanyID) {
		this.groupCompanyID = groupCompanyID;
	}


	public String getCompanyId() {
		return companyId;
	}


	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	public String getBookingType() {
		return bookingType;
	}

	public void setBookingType(String bookingType) {
		this.bookingType = bookingType;
	}

	@Override
	public String toString() {
		
	    JSONObject bookingJson = new JSONObject();
		
	    bookingJson.put("bookID", getBookID());
	    bookingJson.put("lastModifiedAt", lastModifiedAt);
		
		JSONObject testjson = new JSONObject();
		testjson.put("bookID", bookID);
		testjson.put("sessionID",sessionID);
		testjson.put("transactionID",transactionID);
		testjson.put("clientID",clientID);
		testjson.put("branchID",branchID);
		testjson.put("staffID",staffID);
		testjson.put("travelAgentID",travelAgentID);
		testjson.put("clientType",clientType);
		testjson.put("clientCurrency",clientCurrency);
		testjson.put("clientIATANumber",clientIATANumber);
		testjson.put("status",status);
		testjson.put("isHolidayBooking",isHolidayBooking);
		testjson.put("enquiryID",enquiryID);
		testjson.put("quoteID",quoteID);
		testjson.put("createdAt",createdAt);
		testjson.put("lastModifiedAt",lastModifiedAt);
		testjson.put("lastModifiedBy", lastModifiedBy);
		testjson.put("userID", userID);
		testjson.put("clientLanguage", clientLanguage);
		testjson.put("clientMarket", clientMarket);
		testjson.put("clientNationality",clientNationality);
		testjson.put("QCStatus",QCStatus);
		testjson.put("mergeID", mergeID);
		testjson.put("bookingType",bookingType);
		//testjson.put("paymentInfo", paymentInfo);
		bookingJson.put("data_value", testjson);
		
		return bookingJson.toString();
		
	}
	
}

