package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.ActivitiesOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.ActivitiesDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
public class ActivitiesBookingServiceImpl implements Constants, ErrorConstants {

	@Autowired
	@Qualifier("Activity")
	private ActivitiesDatabaseRepository activitiesDatabaseRepository;

	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;

	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	JSONObject response = new JSONObject();

	public JSONArray process(Booking booking, String flag) {
		List<ActivitiesOrders> activitiesOrders = activitiesDatabaseRepository.findByBooking(booking);
		JSONArray activitiesOrdersJson = getActivitiesOrdersJson(activitiesOrders, flag);

		return activitiesOrdersJson;
	}

	private JSONArray getActivitiesOrdersJson(List<ActivitiesOrders> orders, String flag) {

		JSONArray activitiesArray = new JSONArray();
		JSONObject activitiesJson = new JSONObject();

		for (ActivitiesOrders order : orders) {

			if ("getOrdersInRange".equalsIgnoreCase(Thread.currentThread().getStackTrace()[2].getMethodName())) {
				activitiesJson = getActivitiesOrderJson(order, flag, true);
			} else {
				activitiesJson = getActivitiesOrderJson(order, flag, false);
				activitiesArray.put(activitiesJson);
			}

		}

		return activitiesArray;
	}

	private JSONObject getActivitiesOrderJson(ActivitiesOrders order, String flag, boolean getBetween) {

		JSONObject activitiesJson = new JSONObject();

		if (getBetween) {
			activitiesJson.put(JSON_PROP_BOOKID, order.getBooking().getBookID());
			activitiesJson.put(JSON_PROP_BOOKINGDATE, order.getBooking().getCreatedAt());
		}

		// TODO: to check from where will we get these details from WEM
		activitiesJson.put(JSON_PROP_CREDENTIALSNAME, "");

		// TODO: added these fields on the suggestions of operations
		activitiesJson.put(JSON_PROP_SUPPLIERRECONFIRMATIONSTATUS, order.getSuppReconfirmStatus());
		activitiesJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());

		// TODO: we need to check how will SI send us the details for Enabler Supplier
		// and source supplier
		activitiesJson.put(JSON_PROP_ENABLERSUPPLIERNAME, order.getSupplierID());
		activitiesJson.put(JSON_PROP_SOURCESUPPLIERNAME, order.getSupplierID());

		// TODO: to check what value to sent when it has not reach the cancel/amend
		// stage
		activitiesJson.put(JSON_PROP_CANCELDATE, "");
		activitiesJson.put(JSON_PROP_AMENDDATE, "");
		activitiesJson.put(JSON_PROP_INVENTORY, "N");

		activitiesJson.put("productCategory", Constants.JSON_PROP_ACTIVITIES_CATEGORY);

		// TODO : Need to chk on it
		activitiesJson.put("productSubCategory", Constants.JSON_PROP_ACTIVITIES_SUBCATEGORY);
		
		activitiesJson.put(JSON_PROP_ORDERID, order.getId());
		activitiesJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		activitiesJson.put(JSON_PROP_STATUS, order.getStatus());
		activitiesJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());
		activitiesJson.put(JSON_PROP_ROE, order.getRoe());
		
		
		JSONObject orderDetails = new JSONObject();
//		orderDetails.put(JSON_PROP_SUPPCOMM, getSuppComms(order));
//		orderDetails.put(JSON_PROP_CLIENTCOMM,getClientComms(order));
		if(flag=="false")
		{	
		orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
		orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS,getClientComms(order));
		orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getSuppPriceInfoJson(order));
	    }
		orderDetails.put("activityDetails", getActivitiesDetails(order));
		orderDetails.put(JSON_PROP_PAXDETAILS, getPaxInfoJson(order));
		
		orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getTotalPriceInfoJson(order));
		
//		orderDetails.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson(order));
//		orderDetails.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson(order));
		activitiesJson.put(JSON_PROP_ORDERDETAILS, orderDetails);
		// activitiesJson.put("productCategory",
		// Constants.JSON_PROP_ACTIVITIES_CATEGORY);
		//
		// // TODO : Need to chk on it
		// activitiesJson.put("productSubCategory",
		// Constants.JSON_PROP_ACTIVITIES_SUBCATEGORY);
		// activitiesJson.put("lastUpdatedBy", order.getLastUpdatedBy());
		// activitiesJson.put("cancelDate", order.getCancelDate());
		// activitiesJson.put("supplierID", order.getSupplierID());
		// activitiesJson.put("orderID", order.getId());
		// // TODO : Need to check
		// activitiesJson.put("supplierRateType", "");
		// activitiesJson.put("amendDate", order.getAmendDate());
		//
		// // TODO : Not Known as of now
		// activitiesJson.put("reconfirmationDate", "");
		// // TODO : Need to check
		// activitiesJson.put("inventory", "");
		// activitiesJson.put("enamblerSupplierName", order.getSupplierID());
		// // TODO : Need to check
		// activitiesJson.put("suppReconfirmationDate", "");
		// activitiesJson.put("createdAt", order.getCreatedAt());
		// activitiesJson.put("sourceSupplierName", order.getSupplierID());
		// // TODO : Need to check
		// activitiesJson.put("credentialsName", "");
		// activitiesJson.put("status", order.getStatus());
		//
		// // JSONObject orderDetails = new JSONObject();
		// // orderDetails.put("orderDetails", getActivitiesDetails(order));
		// activitiesJson.put("orderDetails", getActivitiesDetails(order));

		return activitiesJson;
	}

	private JSONObject getTotalPriceInfoJson(ActivitiesOrders order) {

		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put("TotalFare", new BigDecimal(order.getTotalPrice()));
		totalPriceJson.put("TotalFareCurrency", order.getTotalPriceCurrencyCode());
		totalPriceJson.put("paxTypeFares", new JSONArray(order.getTotalPaxTypeFares()));
		return totalPriceJson;
	}
	
	private JSONObject getSuppPriceInfoJson(ActivitiesOrders order) {
		JSONObject suppPriceJson = new JSONObject();

//		suppPriceJson.put(JSON_PROP_SUPPPRICE, order.getSupplierTotalPrice());
		suppPriceJson.put(JSON_PROP_SUPPPRICE, new BigDecimal(order.getSupplierTotalPrice()));
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, order.getSupplierPriceCurrencyCode());
		suppPriceJson.put("paxTypeFares", new JSONArray(order.getSuppPaxTypeFares()));
		return suppPriceJson;
	}
	
	private JSONObject getActivitiesDetails(ActivitiesOrders order) {
//		JSONObject activitiesDetails = new JSONObject();

		JSONObject activitiesJson = new JSONObject();

		activitiesJson.put("adultCount", order.getAdultCount());
		activitiesJson.put("answers", new JSONArray(order.getAnswers()));
		activitiesJson.put("bookingDateTime", order.getBookingDateTime());
		activitiesJson.put("childCount", order.getChildCount());
		activitiesJson.put("cityCode", order.getCityCode());

		activitiesJson.put("clientCurrency", order.getClientCurrency());
		// activitiesJson.put("clientIATANumber", order.getClientIATANumber());
		activitiesJson.put("clientID", order.getClientID());
		activitiesJson.put("clientType", order.getClientType());
//		activitiesJson.put("orderTotalPriceInfo", getOrderTotalPriceInfo(order));

		// activitiesJson.put("orderTotalPriceInfo", new
		// JSONArray(order.getCommercialPaxTypeFares()));

		// Need to convert it to Array.
		activitiesJson.put("contactDetail", new JSONObject(order.getContactDetail()));

		activitiesJson.put("countryCode", order.getCountryCode());

		activitiesJson.put("endDate", order.getEndDate());

		activitiesJson.put("name", order.getName());

		activitiesJson.put("paxInfo", getPaxInfoJson(order));

		activitiesJson.put("pickupDropoff", new JSONObject(order.getPickupDropoff()));
		activitiesJson.put("POS", new JSONObject(order.getPOS()));

		activitiesJson.put("shipping_Details", new JSONObject(order.getShipping_Details()));
		activitiesJson.put("startDate", order.getStartDate());
		activitiesJson.put("status", order.getStatus());
		activitiesJson.put("supp_booking_reference", order.getSupp_booking_reference());

		activitiesJson.put("supplier_Details", new JSONObject(order.getSupplier_Details()));
		activitiesJson.put("supplierBrandCode", order.getSupplierBrandCode());
		activitiesJson.put("supplierID", order.getSupplierID());
		activitiesJson.put("supplierProductCode", order.getSupplierProductCode());

//		activitiesJson.put("supplierPriceInfo", new JSONArray(order.getSuppPaxTypeFares()));
		activitiesJson.put("timeSlotDetails", new JSONArray(order.getTimeSlotDetails()));
		activitiesJson.put("tourLanguage", new JSONArray(order.getTourLanguage()));

		// activitiesJson.put("orderSupplierPriceInfo", new
		// JSONArray(order.getSupplierPaxTypeFares()));

//		activitiesJson.put("orderClientTotalPriceInfo", new JSONArray(order.getTotalPaxTypeFares()));

//		activitiesDetails.put("activitiesDetails", activitiesJson);
//		// TODO :client and SupplierComercial pending now
//		activitiesDetails.put("orderClientCommercials", getClientComms(order));
//		// TODO : client and SupplierComercial pending now
//		activitiesDetails.put("orderSupplierCommercials", getSuppComms(order));
//
//		// TODO : code in progress in continuation from here
//		JSONObject orderSupplierPriceInfo = getOrderSupplierPriceInfo(order);
//
//		activitiesDetails.put("orderSupplierPriceInfo", orderSupplierPriceInfo);

		return activitiesJson;
	}

	/**
	 * @param order
	 * @return
	 */
//	private JSONObject getOrderSupplierPriceInfo(ActivitiesOrders order) {
//		JSONObject commercialPaxTypeFares = new JSONObject(order.getCommercialPaxTypeFares());
//		JSONObject totalFaresSUMJson = readTotalFares(commercialPaxTypeFares.getJSONArray("totalFares"), "SUMMARY");
//
//		JSONObject orderSupplierPriceInfo = new JSONObject();
//		orderSupplierPriceInfo.put("supplierPrice",
//				totalFaresSUMJson.getJSONObject("baseFare").getBigDecimal("amount"));
//		orderSupplierPriceInfo.put("currencyCode",
//				totalFaresSUMJson.getJSONObject("baseFare").getString("currencyCode"));
//		orderSupplierPriceInfo.put("paxTypeFares", readPaxTypeFaresArr(commercialPaxTypeFares));
//		return orderSupplierPriceInfo;
//	}

	private JSONArray getClientComms(ActivitiesOrders order) {
		JSONArray clientCommArray = new JSONArray();

		for (ClientCommercial clientComm : order.getClientCommercial()) {
			JSONObject clientCommJson = new JSONObject();

			clientCommJson.put("commercialName", clientComm.getCommercialName());
			clientCommJson.put("commercialType", clientComm.getCommercialType());

			clientCommJson.put("commercialAmount", clientComm.getCommercialAmount());
			clientCommJson.put("commercialCurrency", clientComm.getCommercialCurrency());
			clientCommJson.put("clientID", clientComm.getClientID());
			clientCommJson.put("parentClientID", clientComm.getParentClientID());
			clientCommJson.put("commercialEntityID", clientComm.getCommercialEntityID());
			clientCommJson.put("commercialEntityType", clientComm.getCommercialEntityType());
			clientCommJson.put("companyFlag", clientComm.isCompanyFlag());

			clientCommArray.put(clientCommJson);
		}
		return clientCommArray;
	}

	private JSONArray getSuppComms(ActivitiesOrders order) {
		JSONArray suppCommArray = new JSONArray();

		for (SupplierCommercial suppComm : order.getSuppcommercial()) {
			JSONObject suppCommJson = new JSONObject();
			suppCommJson.put("commercialName", suppComm.getCommercialName());
			suppCommJson.put("commercialType", suppComm.getCommercialType());

			suppCommJson.put("commercialAmount", suppComm.getCommercialAmount());
			suppCommJson.put("commercialCurrency", suppComm.getCommercialCurrency());

			suppCommArray.put(suppCommJson);
		}
		return suppCommArray;
	}

	private JSONArray getPaxInfoJson(ActivitiesOrders order) {
		JSONArray paxJsonArray = new JSONArray();
		for (Object paxId : new JSONArray(order.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject) paxId;

			PassengerDetails guest = passengerRepository.findOne(paxIdJson.getString("paxId"));
			JSONObject paxJson = new JSONObject();

			paxJson.put(JSON_PROP_PAXID, guest.getPassanger_id());
			paxJson.put(JSON_PROP_PAX_TYPE, guest.getPaxType());
			paxJson.put(JSON_PROP_ISLEADPAX, guest.getIsLeadPax());
			paxJson.put(JSON_PROP_TITLE, guest.getTitle());
			paxJson.put(JSON_PROP_FIRSTNAME, guest.getFirstName());
			paxJson.put(JSON_PROP_MIDDLENAME, guest.getMiddleName());
			paxJson.put(JSON_PROP_LASTNAME, guest.getLastName());
			paxJson.put(JSON_PROP_BIRTHDATE, guest.getBirthDate());

			// TODO : PaxInfo JSON have changed from Array to object
			JSONObject contactDetails = new JSONObject(guest.getContactDetails());
			JSONArray contactDetailsJSONArray = new JSONArray();
			contactDetailsJSONArray.put(contactDetails);
			paxJson.put(JSON_PROP_CONTACTDETAILS, contactDetailsJSONArray);
			if (guest.getIsLeadPax())
				paxJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(guest.getAddressDetails()));

			paxJsonArray.put(paxJson);
		}
		return paxJsonArray;
	}

//	private JSONObject getOrderTotalPriceInfo(ActivitiesOrders order) {
//
//		JSONObject orderTotalPriceInfoJson = new JSONObject();
//
//		JSONObject commercialPaxTypeFares = new JSONObject(order.getCommercialPaxTypeFares());
//
//		orderTotalPriceInfoJson.put("baseFare",
//				readBaseFare(commercialPaxTypeFares.getJSONArray("suppFares"), "SUMMARY"));
//
//		JSONObject totalFaresSUMJson = readTotalFares(commercialPaxTypeFares.getJSONArray("totalFares"), "SUMMARY");
//
//		orderTotalPriceInfoJson.put("totalPrice", totalFaresSUMJson.getJSONObject("baseFare").getBigDecimal("amount"));
//		orderTotalPriceInfoJson.put("currencyCode",
//				totalFaresSUMJson.getJSONObject("baseFare").getString("currencyCode"));
//
//		orderTotalPriceInfoJson.put("receivables", readReceivables(totalFaresSUMJson));
//
//		// ------Adding paxTypeFares in order total price info json---------
//
//		orderTotalPriceInfoJson.put("paxTypeFares", readPaxTypeFaresArr(commercialPaxTypeFares));
//		// -----------------------------------------------------------------------------------
//
//		return orderTotalPriceInfoJson;
//	}

	private JSONArray readPaxTypeFaresArr(JSONObject commercialPaxTypeFares) {
		JSONArray paxTypeFaresArr = new JSONArray();

		JSONArray suppFaresArr = commercialPaxTypeFares.getJSONArray("suppFares");
		for (int i = 0; i < suppFaresArr.length(); i++) {

			JSONObject suppFaresJson = suppFaresArr.getJSONObject(i);

			String paxType = suppFaresJson.getString("paxType");

			if (!paxType.equalsIgnoreCase("SUMMARY")) {

				JSONObject paxTypeFaresJson = new JSONObject();
				paxTypeFaresJson.put("paxType", suppFaresJson.getString("paxType"));
				paxTypeFaresJson.put("baseFare", suppFaresJson.getJSONObject("baseFare"));

				JSONObject totalFaresPaxTypeJson = readTotalFares(commercialPaxTypeFares.getJSONArray("totalFares"),
						paxType);
				paxTypeFaresJson.put("totalFare", totalFaresPaxTypeJson.getJSONObject("baseFare"));
				paxTypeFaresJson.put("clientEntityCommercials",
						totalFaresPaxTypeJson.getJSONArray("clientEntityCommercials"));
				paxTypeFaresJson.put("receivable", readReceivables(totalFaresPaxTypeJson));

				paxTypeFaresArr.put(paxTypeFaresJson);
			}

		}

		return paxTypeFaresArr;
	}

	private JSONObject readReceivables(JSONObject totalFaresJson) {

		JSONObject receivablesJson = new JSONObject();

		BigDecimal receivablesAmount = new BigDecimal(0);
		String clientComCurrency = new String();
		String commercialType = "Receivable";

		JSONArray receivablesArr = new JSONArray();
		JSONArray clientCommercialsArr = totalFaresJson.getJSONArray("clientEntityCommercials").getJSONObject(0)
				.getJSONArray("clientCommercials");

		for (int i = 0; i < clientCommercialsArr.length(); i++) {

			JSONObject clientCommercialsJson = clientCommercialsArr.getJSONObject(i);

			if (clientCommercialsJson.getString("commercialType").equalsIgnoreCase(commercialType)) {

				receivablesAmount = receivablesAmount.add(clientCommercialsJson.getBigDecimal("commercialAmount"));

				JSONObject receivablesArrJson = new JSONObject();
				receivablesArrJson.put("amount", clientCommercialsJson.getBigDecimal("commercialAmount"));
				receivablesArrJson.put("code", clientCommercialsJson.getString("commercialName"));
				receivablesArrJson.put("currencyCode", clientCommercialsJson.getString("commercialCurrency"));

				receivablesArr.put(receivablesArrJson);
			}

			if (clientCommercialsJson.getString("commercialName").equalsIgnoreCase("MarkUp")) {
				clientComCurrency = clientCommercialsJson.getString("commercialCurrency");
			}
		}

		// Putting overall client commercial currency same as mark up currency
		receivablesJson.put("currencyCode", clientComCurrency);

		receivablesJson.put("amount", receivablesAmount);

		receivablesJson.put("receivable", receivablesArr);

		return receivablesJson;
	}

	private JSONObject readTotalFares(JSONArray totalFaresArr, String paxType) {

		for (int i = 0; i < totalFaresArr.length(); i++) {
			JSONObject totalFaresJson = totalFaresArr.getJSONObject(i);
			if (totalFaresJson.getString("paxType").equalsIgnoreCase(paxType)) {
				return totalFaresJson;
			}
		}

		return null;
	}

	private JSONObject readBaseFare(JSONArray suppFaresArr, String paxType) {

		for (int i = 0; i < suppFaresArr.length(); i++) {
			JSONObject suppFaresJson = suppFaresArr.getJSONObject(i);
			if (suppFaresJson.getString("paxType").equalsIgnoreCase(paxType)) {
				return suppFaresJson.getJSONObject("baseFare");
			}

		}
		return null;
	}

	private JSONObject getPriceFor(JSONArray orderTotalPriceInfoArr, String participantCategory) {

		for (int i = 0; i < orderTotalPriceInfoArr.length(); i++) {
			JSONObject orderTotalPriceInfoJson = orderTotalPriceInfoArr.getJSONObject(i);
			if (orderTotalPriceInfoJson.getString("participantCategory").equalsIgnoreCase(participantCategory)) {
				return orderTotalPriceInfoJson;
			}

		}
		return null;
	}

	public String updateOrder(JSONObject reqJson, String updateType) throws BookingEngineDBException {
		switch (updateType) {
		case JSON_PROP_STATUS:
			return updateStatus(reqJson);
		case JSON_PROP_CLIENTRECONFIRMSTATUS:
			return updateClientReconfirmStatus(reqJson);
		case JSON_PROP_SUPPRECONFIRMSTATUS:
			return updateSuppReconfirmStatus(reqJson);
		case JSON_PROP_SUPPRECONFIRMDATE:
			return updateSuppReconfirmDate(reqJson);
		case JSON_PROP_CLIENTRECONFIRMDATE:
			return updateClientReconfirmDate(reqJson);
		case JSON_PROP_EXPIRYTIMELIMIT:
			return updateTimeLimitDate(reqJson);
		case JSON_PROP_VOUCHERS:
			return updateVouchers(reqJson);
		case JSON_PROP_ORDERATTRIBUTE:
			return updateOrderAttribute(reqJson);
		default:
			return "no match for update type";
		}
	}

	private String updateOrderAttribute(JSONObject reqJson) {
		ActivitiesOrders order = activitiesDatabaseRepository
				.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"Activities updateOrderAttribute failed to update since Order details  not found for  orderid  %s ",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setBookingAttribute(
					reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("bookingAttribute").toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			ActivitiesOrders updatedclientReconfirmDetails = saveActivitiesOrder(order, prevOrder);
			myLogger.info(String.format("Activities orderAttribute updated Successfully for  orderId  %s = %s",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID),
					updatedclientReconfirmDetails.toString()));
			return "Activities Booking Attribute Updated Successfully";
		}
	}

	private String updateTimeLimitDate(JSONObject reqJson) throws BookingEngineDBException {

		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		ActivitiesOrders order = activitiesDatabaseRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"Activities time limit expiry date failed to update since Order details  not found for  orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setTimeLimitExpiryDate(reqJson.getString(JSON_PROP_EXPIRYTIMELIMIT));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			ActivitiesOrders updatedclientReconfirmDetails = saveActivitiesOrder(order, prevOrder);
			myLogger.info(String.format("Activities time limit expiry date updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Activities time limit expiry date updated Successfully";
		}

	}

	private String updateVouchers(JSONObject reqJson) throws BookingEngineDBException {

		ActivitiesOrders order = activitiesDatabaseRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"updateVouchers failed to update since Activities Order details  not found for  orderid  %s ",
					reqJson.getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setVouchers(reqJson.getJSONArray(JSON_PROP_VOUCHERIDS).toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			ActivitiesOrders updatedVoucherDetails = saveActivitiesOrder(order, prevOrder);
			myLogger.info(String.format("Activities vouchers updated Successfully for  orderId  %s = %s",
					reqJson.getString(JSON_PROP_ORDERID), updatedVoucherDetails.toString()));

			return "Activities vouchers Updated Successfully";
		}

	}

	private String updateClientReconfirmDate(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		ActivitiesOrders order = activitiesDatabaseRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmdate failed to update since Order details  not found for Activities orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmDate(reqJson.getString(JSON_PROP_CLIENTRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

			ActivitiesOrders updatedclientReconfirmDetails = saveActivitiesOrder(order, prevOrder);
			myLogger.info(
					String.format("Activities client reconfirmation date updated Successfully for  orderId  %s = %s",
							orderID, updatedclientReconfirmDetails.toString()));
			return "Activities order client reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmDate(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		ActivitiesOrders order = activitiesDatabaseRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmDate failed to update since Order details  not found for Activities orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmDate(reqJson.getString(JSON_PROP_SUPPRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			ActivitiesOrders updatedSuppReconfirmDateDetails = saveActivitiesOrder(order, prevOrder);
			myLogger.info(
					String.format("Activities supplier reconfirmation date updated Successfully for  orderId  %s = %s",
							orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Activities order supplier reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmStatus(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		ActivitiesOrders order = activitiesDatabaseRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmStatus failed to update since Order details  not found for Activities  orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmStatus(reqJson.getString("suppReconfirmStatus"));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			ActivitiesOrders updatedSuppReconfirmDateDetails = saveActivitiesOrder(order, prevOrder);
			myLogger.info(String.format(
					"Activities supplier reconfirmation status updated Successfully for  orderId  %s = %s", orderID,
					updatedSuppReconfirmDateDetails.toString()));
			return "Activities order supplier reconfirmation status updated Successfully";
		}
	}

	private String updateClientReconfirmStatus(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		ActivitiesOrders order = activitiesDatabaseRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmStatus failed to update since Order details  not found for Activities orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmStatus(reqJson.getString(JSON_PROP_CLIENTRECONFIRMSTATUS));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			ActivitiesOrders updatedclientReconfirmDetails = saveActivitiesOrder(order, prevOrder);
			myLogger.info(
					String.format("Activities client reconfirmation status updated Successfully for  orderId  %s = %s",
							orderID, updatedclientReconfirmDetails.toString()));
			return "Activities order client reconfirmation status updated Successfully";
		}
	}

	private String updateStatus(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		ActivitiesOrders order = activitiesDatabaseRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"Status  failed to update since Activities order details   not found for  orderid  %s ", orderID));
			return (response.toString());
		}
		String prevOrder = order.toString();
		order.setStatus(reqJson.getString(JSON_PROP_STATUS));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		ActivitiesOrders updatedStatusObj = saveActivitiesOrder(order, prevOrder);
		myLogger.info(String.format("Status updated Successfully for Activities orderID %s = %s", orderID,
				updatedStatusObj.toString()));
		return "Activities Order status updated Successfully";

	}

	private ActivitiesOrders saveActivitiesOrder(ActivitiesOrders order, String prevOrder) {

		ActivitiesOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, ActivitiesOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return activitiesDatabaseRepository.saveOrder(orderObj, prevOrder);

	}
}
