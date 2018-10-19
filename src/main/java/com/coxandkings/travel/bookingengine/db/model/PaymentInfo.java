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
import org.json.JSONObject;



@Entity
@Table(name="PAYMENTINFO")
public class PaymentInfo implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Id
	@Column()
	@GeneratedValue(generator="system-uuid")
	@GenericGenerator(name="system-uuid",strategy = "uuid")
	private String payment_info_id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="booking_id")
    private Booking booking; 
	

	@Column
	private String totalAmount;
	@Column
	private String amountCurrency;
	@Column
	private String transactionRefNumber;
	@Column
	private String transactionDate;
	@Column
	private String amountPaid;
	@Column
	private String paymentType;
	@Column
	private String paymentMethod;
	@Column
	private String paymentStatus;
	
	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime createdAt;

	@Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
	private ZonedDateTime lastModifiedAt;
	
	@Column
	private String lastModifiedBy;
	
	@Column
  	@Type(type = "StringJsonObject")
  	private String paymentAttributes;
   
	
	
	public String getPayment_info_id() {
		return payment_info_id;
	}



	public void setPayment_info_id(String payment_info_id) {
		this.payment_info_id = payment_info_id;
	}



	public Booking getBooking() {
		return booking;
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



	public void setBooking(Booking booking) {
		this.booking = booking;
	}



	public String getTotalAmount() {
		return totalAmount;
	}



	public void setTotalAmount(String totalAmount) {
		this.totalAmount = totalAmount;
	}



	public String getAmountCurrency() {
		return amountCurrency;
	}



	public void setAmountCurrency(String amountCurrency) {
		this.amountCurrency = amountCurrency;
	}



	public String getTransactionRefNumber() {
		return transactionRefNumber;
	}



	public void setTransactionRefNumber(String transactionRefNumber) {
		this.transactionRefNumber = transactionRefNumber;
	}



	public String getTransactionDate() {
		return transactionDate;
	}



	public void setTransactionDate(String transactionDate) {
		this.transactionDate = transactionDate;
	}



	public String getAmountPaid() {
		return amountPaid;
	}



	public void setAmountPaid(String amountPaid) {
		this.amountPaid = amountPaid;
	}



	public String getPaymentType() {
		return paymentType;
	}



	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}



	public String getPaymentMethod() {
		return paymentMethod;
	}



	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}



	public String getPaymentStatus() {
		return paymentStatus;
	}



	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}



	public String getPaymentAttributes() {
		return paymentAttributes;
	}



	public void setPaymentAttributes(String paymentAttributes) {
		this.paymentAttributes = paymentAttributes;
	}



	@Override
	public String toString() {
   
		JSONObject paymentInfo = new JSONObject();
		paymentInfo.put("payment_info_id", payment_info_id);
		paymentInfo.put("bookID", booking.getBookID());
		
		return paymentInfo.toString();
	
	}
    
	
	
}
