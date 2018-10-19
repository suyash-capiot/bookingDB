package com.coxandkings.travel.bookingengine.db.model;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.json.JSONArray;
import org.json.JSONObject;

import com.coxandkings.travel.bookingengine.db.postgres.common.StringJsonUserType;


@Entity
@Table(name = "ACCOORDERS")
@TypeDefs({@TypeDef(name = "StringJsonObject", typeClass = StringJsonUserType.class)})
public class AccoOrders extends ProductOrder implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public AccoOrders() {
        super();
        operationType = "update";
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holidays_order_id", nullable = true)
    private HolidaysOrders holidaysOrders;

    @OneToMany(mappedBy = "accoOrders", cascade = CascadeType.ALL)
    private Set<AccoRoomDetails> roomDetails;


    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime createdAt;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime lastModifiedAt;
	

	/*@Column
	private String supp_booking_reference;*/

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
    private String ticketingPCC;
    @Column
    private String credentialsName;
    @Column
    private String accoRefNumber;
    @Column
    private String supplierRateType;
    @Column
    private String inventory;

    @Column
    private String supplierPrice;
    @Column
    private String supplierPriceCurrencyCode;
    @Column
    private String totalPrice;
    @Column
    private String totalPriceCurrencyCode;
    @Column
    @Type(type = "StringJsonObject")
    private String totalPriceTaxes;
    @Column
    @Type(type = "StringJsonObject")
    private String suppPriceTaxes;
    @Column
    @Type(type = "StringJsonObject")
    private String incentives;
    @Column
    @Type(type = "StringJsonObject")
    private String discounts;
    @Column
    private String suppierReservationId;
    @Column
    private String supplierReferenceId;
    @Column
    private String clientReferenceId;
    @Column
    private String supplierCancellationId;

    @Transient
    private String operationType;

    //Fields for packages
  	@Column
  	private String supplierPriceBeforeTax;
  	@Column
  	private String supplierPriceAfterTax;
  	@Column
  	private String supplierTaxAmount;
  	
  	@Column
  	@Type(type = "StringJsonObject")
  	private String supplierCommercials;
  	@Column
  	@Type(type = "StringJsonObject")
  	private String clientCommercials;
  	
  	@Column
  	private String totalPriceBeforeTax;
  	@Column
  	private String totalPriceAfterTax;
  	@Column
  	private String totalTaxAmount;
  	
  	@Column
  	@Type(type = "StringJsonObject")
  	private String receivables;
  	@Column
  	private String name;
  	@Column
  	private String tourCruID;
  	@Column
  	private String accomodationType;
  	@Column
  	@Type(type = "StringJsonObject")
  	private String companyTaxes;
  	
  	

    public String getCompanyTaxes() {
		return companyTaxes;
	}

	public void setCompanyTaxes(String companyTaxes) {
		this.companyTaxes = companyTaxes;
	}

	public String getDiscounts() {
		return discounts;
	}

	public void setDiscounts(String discounts) {
		this.discounts = discounts;
	}

	
	public String getIncentives() {
		return incentives;
	}

	public void setIncentives(String incentives) {
		this.incentives = incentives;
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

    public String getSupplierPrice() {
        return supplierPrice;
    }

    public void setSupplierPrice(String supplierPrice) {
        this.supplierPrice = supplierPrice;
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

    public String getTotalPriceCurrencyCode() {
        return totalPriceCurrencyCode;
    }

    public void setTotalPriceCurrencyCode(String totalPriceCurrencyCode) {
        this.totalPriceCurrencyCode = totalPriceCurrencyCode;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }


    public String getCredentialsName() {
        return credentialsName;
    }

    public void setCredentialsName(String credentialsName) {
        this.credentialsName = credentialsName;
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

    public String getAccoRefNumber() {
        return accoRefNumber;
    }

    public void setAccoRefNumber(String accoRefNumber) {
        this.accoRefNumber = accoRefNumber;
    }

    public String getSupplierRateType() {
        return supplierRateType;
    }

    public void setSupplierRateType(String supplierRateType) {
        this.supplierRateType = supplierRateType;
    }

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }

    public String getSupplierID() {
        return supplierID;
    }

    public void setSupplierID(String supplierID) {
        this.supplierID = supplierID;
    }

    public Set<AccoRoomDetails> getRoomDetails() {
        return roomDetails;
    }

    public void setRoomDetails(Set<AccoRoomDetails> roomDetails) {
        this.roomDetails = roomDetails;
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


    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public String getTicketingPCC() {
        return ticketingPCC;
    }

    public void setTicketingPCC(String ticketingPCC) {
        this.ticketingPCC = ticketingPCC;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTourCruID() {
        return tourCruID;
    }

    public void setTourCruID(String tourCruID) {
        this.tourCruID = tourCruID;
    }

    public String getAccomodationType() {
        return accomodationType;
    }

    public void setAccomodationType(String accomodationType) {
        this.accomodationType = accomodationType;
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

	@Override
    public String toString() {


        JSONObject accoJson = new JSONObject();

        accoJson.put("bookID", booking.getBookID());
        accoJson.put("lastModifiedAt", lastModifiedAt);

        JSONObject testjson = new JSONObject();

        testjson.put("bookID", booking.getBookID());
        testjson.put("lastModifiedAt", lastModifiedAt);
        testjson.put("id", id);
        testjson.put("createdAt", createdAt);


        testjson.put("rateOfExchange", this.getRoe());
        testjson.put("supplierCancellationId", supplierCancellationId);
        testjson.put("supplierReferenceId", supplierReferenceId);
        testjson.put("suppierReservationId", suppierReservationId);
        testjson.put("clientReferenceId", clientReferenceId);
        testjson.put("totalPrice", totalPrice);
        testjson.put("totalPriceCurrencyCode", totalPriceCurrencyCode);
        testjson.put("supplierPriceCurrencyCode", supplierPriceCurrencyCode);
        testjson.put("supplierPrice", supplierPrice);

        testjson.put("suppPriceTaxes", new JSONObject(suppPriceTaxes));
        testjson.put("totalPriceTaxes", new JSONObject(totalPriceTaxes));


        testjson.put("amendDate", amendDate);
        testjson.put("cancelDate", cancelDate);
        testjson.put("createdAt", createdAt);
        testjson.put("lastModifiedAt", lastModifiedAt);
        testjson.put("lastModifiedBy", lastModifiedBy);
        testjson.put("supplierID", supplierID);


        testjson.put("ticketingPCC", ticketingPCC);
        testjson.put("credentialsName", credentialsName);

        testjson.put("accoRefNumber", accoRefNumber);
        testjson.put("supplierRateType", supplierRateType);
        testjson.put("inventory", inventory);


        if (operationType.equalsIgnoreCase("insert")) {
            JSONObject roomJson = new JSONObject();
            JSONArray roomArray = new JSONArray();
            for (AccoRoomDetails room : roomDetails) {
                roomJson = new JSONObject(room.toString());
                roomJson.put("paxDetails", new JSONArray(room.getPaxDetails().toString()));
                roomJson.put("supplierComms", new JSONArray(this.getSuppcommercial().toString()));
                roomJson.put("clientsCommercials", new JSONArray(this.getClientCommercial().toString()));
                roomArray.put(roomJson);
            }
            testjson.put("RoomDetails", roomArray);

            JSONObject bookingJson = new JSONObject(booking.toString());
            bookingJson.put("paymentInfo", new JSONArray(booking.getPaymentInfo().toString()));
            testjson.put("bookingInfo", bookingJson);
        }

        accoJson.put("data_value", testjson);

        return accoJson.toString();


    }


}
