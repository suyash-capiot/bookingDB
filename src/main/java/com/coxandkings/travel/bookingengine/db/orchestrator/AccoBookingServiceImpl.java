
package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.AccoRoomDetails;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.AccoDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.AccoRoomRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

import org.apache.logging.log4j.Logger;

@Service
@Transactional(readOnly = false)
public class AccoBookingServiceImpl implements Constants, ErrorConstants {

	@Qualifier("Acco")
	@Autowired
	private AccoDatabaseRepository accoRepository;

	@Qualifier("AccoRoom")
	@Autowired
	private AccoRoomRepository roomRepository;

	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;

	@Autowired
	@Qualifier("AccoAmCl")
	private AmClRepository amClRepository;

	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());

	JSONObject response = new JSONObject();

	public JSONArray process(Booking booking, String flag) {
		List<AccoOrders> accoOrders = accoRepository.findByBooking(booking);
		JSONArray accoOrdersJson = getAccoOrdersJson(accoOrders, flag);
		// myLogger.info(String.format("Acco Bookings retrieved for bookID %s = %s",
		// booking.getBookID(),accoOrdersJson.toString()));
		return accoOrdersJson;
	}

	public String getBysuppID(String suppID) {

		List<AccoOrders> orders = accoRepository.findBysuppID(suppID);
		if (orders.size() == 0) {
			response.put("ErrorCode", "BE_ERR_ACCO_006");
			response.put("ErrorMsg", BE_ERR_ACCO_006);
			myLogger.warn(String.format("Acco Orders not present  for suppID %s", suppID));
			return response.toString();
		} else {
			JSONArray ordersArray = getAccoOrdersJson(orders, "false");
			myLogger.info(String.format("Acco Orders retrieved for suppID %s = %s", suppID, ordersArray.toString()));
			return ordersArray.toString();
		}
	}

	private JSONArray getAccoOrdersJson(List<AccoOrders> orders, String flag) {

		JSONArray accoArray = new JSONArray();

		for (AccoOrders order : orders) {
			JSONObject accoJson = new JSONObject();
			if ("getOrdersInRange".equalsIgnoreCase(Thread.currentThread().getStackTrace()[2].getMethodName()))
				accoJson = getAccoOrderJson(order, flag, true);
			else
				accoJson = getAccoOrderJson(order, flag, false);
			accoArray.put(accoJson);

		}
		return accoArray;
	}

	public JSONObject getAccoOrderJson(AccoOrders order, String flag, boolean getBetween) {

		JSONObject accoJson = new JSONObject();

		if (getBetween) {
			accoJson.put(JSON_PROP_BOOKID, order.getBooking().getBookID());
			accoJson.put(JSON_PROP_BOOKINGDATE, order.getBooking().getCreatedAt());
		}
		// TODO: to check from where will we get these details from WEM
		accoJson.put(JSON_PROP_CREDENTIALSNAME, order.getCredentialsName());

		// TODO: we need to check how will SI send us the details for Enabler Supplier
		// and source supplier
		accoJson.put(JSON_PROP_ENABLERSUPPLIERNAME, order.getSupplierID());
		accoJson.put(JSON_PROP_SOURCESUPPLIERNAME, order.getSupplierID());

		// TODO: confirm from where these are going to come :(
		accoJson.put(JSON_PROP_SUPPLIERRATETYPE, "");
		// TODO: check for logic in case of inventory
		accoJson.put(JSON_PROP_INVENTORY, "N");

		// TODO: added these fields on the suggestions of operations
		accoJson.put(JSON_PROP_SUPPLIERRECONFIRMATIONSTATUS, order.getSuppReconfirmStatus());
		accoJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());

		accoJson.put(JSON_PROP_SUPPRECONFIRMDATE, order.getSuppReconfirmDate());
		accoJson.put(JSON_PROP_CLIENTRECONFIRMDATE, order.getClientReconfirmDate());
		//accoJson.put(JSON_PROP_BOOKINGTYPE, order.getBookingType());

		// TODO: check if we can move out these dates from order table
		accoJson.put(JSON_PROP_CANCELDATE, "");
		accoJson.put(JSON_PROP_AMENDDATE, "");

		String createdAt = order.getCreatedAt().toString().substring(0, order.getCreatedAt().toString().indexOf('['));

		accoJson.put(JSON_PROP_CREATEDAT, createdAt);
		accoJson.put(JSON_PROP_PRODUCTCATEGORY, "Accommodation");
		accoJson.put(JSON_PROP_PRODUCTSUBCATEGORY, order.getProductSubCategory());
		accoJson.put(JSON_PROP_ORDERID, order.getId());
		accoJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		accoJson.put(JSON_PROP_SUPPTYPE, order.getSupplierType());
		accoJson.put(JSON_PROP_ROE, order.getRoe());
		accoJson.put(JSON_PROP_SUPPLIERRESERVATIONID, order.getSuppierReservationId());
		accoJson.put(JSON_PROP_SUPPLIERREFERENCEID, order.getSupplierReferenceId());
		accoJson.put(JSON_PROP_CLIENTREFERENCEID, order.getClientReferenceId());
		accoJson.put(JSON_PROP_SUPPLIERCANCELLATIONID, order.getSupplierCancellationId());
		accoJson.put(JSON_PROP_STATUS, order.getStatus());
		accoJson.put(JSON_PROP_LASTUPDATEDBY, order.getLastModifiedBy());

		JSONObject orderDetails = new JSONObject();
		orderDetails.put(JSON_PROP_ACCO_HOTELDETAILS, getHotelJson(order, flag));
		orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS, getClientComms(order));
		if (flag == "false") {
			orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
			orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getOrderSuppPriceInfoJson(order));
		}
		orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getOrderTotalPriceInfoJson(order));

		accoJson.put(JSON_PROP_ORDERDETAILS, orderDetails);

		accoJson.put(JSON_PROP_BOOKINGATTRIBUTE, order.getBookingAttribute()!=null ?
				new JSONArray(order.getBookingAttribute()): null) ;

		accoJson.put(JSON_PROP_VOUCHERS, order.getVouchers() != null ? new JSONArray(order.getVouchers()) : null);

		accoJson.put(JSON_PROP_EXPIRYTIMELIMIT, order.getTimeLimitExpiryDate());

		return accoJson;

	}

	private JSONObject getHotelJson(AccoOrders order, String flag) {
		JSONObject hotelJson = new JSONObject();
		JSONArray roomArray = new JSONArray();

		int i = 0;
		for (AccoRoomDetails room : order.getRoomDetails()) {
			JSONObject roomJson = new JSONObject();
			if (i == 0) {
				i++;
				hotelJson.put(JSON_PROP_COUNTRYCODE, room.getCountryCode());
				hotelJson.put(JSON_PROP_CITYCODE, room.getCityCode());
				hotelJson.put(JSON_PROP_ACCO_HOTELCODE, room.getHotelCode());
				hotelJson.put(JSON_PROP_ACCO_HOTELNAME, room.getHotelName());
			}

			roomJson = getRoomJson(room, flag);
			roomArray.put(roomJson);
		}
		hotelJson.put(JSON_PROP_ACCO_ROOMS, roomArray);

		return hotelJson;
	}

	private JSONObject getRoomJson(AccoRoomDetails room, String flag) {
		JSONObject roomJson = new JSONObject();
		roomJson.put(JSON_PROP_ACCO_ROOMID, room.getId());
		roomJson.put(JSON_PROP_ACCO_CHKIN, room.getCheckInDate());
		roomJson.put(JSON_PROP_ACCO_CHKOUT, room.getCheckOutDate());
		roomJson.put(JSON_PROP_ACCO_ROOMTYPEINFO, getRoomTypeInfoJson(room));
		roomJson.put(JSON_PROP_ACCO_RATEPLANINFO, getRatePlanInfoJson(room));
		roomJson.put(JSON_PROP_ACCO_SUPPLIERROOMINDEX, room.getSupplierRoomIndex());
		roomJson.put(JSON_PROP_ACCO_MEALINFO, getmealInfojson(room));
		roomJson.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson(room));
		roomJson.put(JSON_PROP_PAXINFO, getPaxInfoJson(room));
		roomJson.put(JSON_PROP_CLIENTCOMM, new JSONArray(room.getClientCommercials()));
		if (flag == "false") {
			roomJson.put(JSON_PROP_SUPPCOMM, new JSONArray(room.getSuppCommercials()));
			roomJson.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson(room));
		}
		if (room.getCancellationPolicies() != null)
			roomJson.put(JSON_PROP_POLICIES, new JSONArray(room.getCancellationPolicies()));
		
		if(room.getOccupancyInfo() != null) {
			roomJson.put(JSON_PROP_OCCUPANCYINFO, new JSONArray(room.getOccupancyInfo()));
		}
		return roomJson;
	}

	private JSONObject getRoomTypeInfoJson(AccoRoomDetails room) {

		JSONObject roomTypeJson = new JSONObject();
		roomTypeJson.put(JSON_PROP_ACCO_ROOMTYPECODE, room.getRoomTypeCode());
		roomTypeJson.put(JSON_PROP_ACCO_ROOMTYPENAME, room.getRoomTypeName());
		roomTypeJson.put(JSON_PROP_ACCO_ROOMCATEGID, room.getRoomCategoryID());
		roomTypeJson.put(JSON_PROP_ACCO_ROOMCATEGNAME, room.getRoomCategoryName());
		roomTypeJson.put(JSON_PROP_ACCO_ROOMREF, room.getRoomRef());

		return roomTypeJson;
	}

	private JSONObject getRatePlanInfoJson(AccoRoomDetails room) {

		JSONObject ratePlanJson = new JSONObject();

		ratePlanJson.put(JSON_PROP_ACCO_RATEPLANCODE, room.getRatePlanCode());
		ratePlanJson.put(JSON_PROP_ACCO_RATEPLANNAME, room.getRatePlanName());
		ratePlanJson.put(JSON_PROP_ACCO_RATEPLANREF, room.getRatePlanRef());
		ratePlanJson.put(JSON_PROP_ACCO_BOOKINGREF, room.getRatePlanRef());

		return ratePlanJson;
	}

	private JSONObject getSuppPriceInfoJson(AccoRoomDetails room) {

		JSONObject suppPriceJson = new JSONObject();

		suppPriceJson.put(JSON_PROP_SUPPPRICE, room.getSupplierPrice());
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, room.getSupplierPriceCurrencyCode());
		suppPriceJson.put(JSON_PROP_TAXES, new JSONObject(room.getSupplierTaxBreakup()));

		return suppPriceJson;
	}

	private JSONObject getmealInfojson(AccoRoomDetails room) {

		JSONObject mealInfoJson = new JSONObject();

		mealInfoJson.put(JSON_PROP_ACCO_MEALID, room.getMealCode());
		mealInfoJson.put(JSON_PROP_ACCO_MEALNAME, room.getMealName());

		return mealInfoJson;
	}

	private JSONObject getTotalPriceInfoJson(AccoRoomDetails room) {

		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put(JSON_PROP_TOTALPRICE, room.getTotalPrice());
		totalPriceJson.put(JSON_PROP_CURRENCYCODE, room.getTotalPriceCurrencyCode());
		totalPriceJson.put(JSON_PROP_TAXES, new JSONObject(room.getTotalTaxBreakup()));
		if(room.getCompanyTaxes()!=null)
	    totalPriceJson.put(JSON_PROP_COMPANYTAXES, new JSONObject(room.getCompanyTaxes()));
	    if(room.getIncentives()!=null)
		totalPriceJson.put(JSON_PROP_INCENTIVES, new JSONObject(room.getIncentives()));
		if(room.getDiscounts()!=null)
		totalPriceJson.put(JSON_PROP_DISCOUNTS, new JSONObject(room.getDiscounts()));
		return totalPriceJson;
	}

	private JSONArray getPaxInfoJson(AccoRoomDetails room) {

		JSONArray paxJsonArray = new JSONArray();

		for (Object paxId : new JSONArray(room.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject) paxId;

			PassengerDetails guest = passengerRepository.findOne(paxIdJson.getString(JSON_PROP_PAXID));
			JSONObject paxJson = new JSONObject();

			paxJson.put(JSON_PROP_PAXID, guest.getPassanger_id());
			paxJson.put(JSON_PROP_PAX_TYPE, guest.getPaxType());
			paxJson.put(JSON_PROP_ISLEADPAX, guest.getIsLeadPax());
			paxJson.put(JSON_PROP_TITLE, guest.getTitle());
			paxJson.put(JSON_PROP_FIRSTNAME, guest.getFirstName());
			paxJson.put(JSON_PROP_MIDDLENAME, guest.getMiddleName());
			paxJson.put(JSON_PROP_LASTNAME, guest.getLastName());
			paxJson.put(JSON_PROP_BIRTHDATE, guest.getBirthDate());

			if(guest.getContactDetails() != null) {
				paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONArray(guest.getContactDetails()));
			}
			if (Pax_ADT.equals(guest.getPaxType())&&guest.getAddressDetails()!=null)
				paxJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(guest.getAddressDetails()));
			if (guest.getAncillaryServices() != null)
				paxJson.put(JSON_PROP_ANCILLARYSERVICES, new JSONObject(guest.getAncillaryServices()));
			if(guest.getDocumentDetails()!=null)
				paxJson.put(JSON_PROP_DOCUMENTDETAILS, new JSONObject(guest.getDocumentDetails()));

			paxJsonArray.put(paxJson);
		}

		return paxJsonArray;
	}

	private JSONArray getSuppComms(AccoOrders order) {

		JSONArray suppCommArray = new JSONArray();

		for (SupplierCommercial suppComm : order.getSuppcommercial()) {
			JSONObject suppCommJson = new JSONObject();
			
			suppCommJson.put(JSON_PROP_SUPPCOMMERCIALID, suppComm.getSupp_commercial_id());
			suppCommJson.put(JSON_PROP_COMMERCIALNAME, suppComm.getCommercialName());
			suppCommJson.put(JSON_PROP_COMMERCIALTYPE, suppComm.getCommercialType());

			suppCommJson.put(JSON_PROP_COMMAMOUNT, suppComm.getCommercialAmount());
			suppCommJson.put(JSON_PROP_COMMERCIALCURRENCY, suppComm.getCommercialCurrency());
			suppCommJson.put(JSON_PROP_RECIEPTNUMBER, suppComm.getRecieptNumber());
			suppCommJson.put(JSON_PROP_INVOICENUMBER, suppComm.getInVoiceNumber());

			suppCommArray.put(suppCommJson);
		}
		return suppCommArray;
	}

	private JSONArray getClientComms(AccoOrders order) {

		JSONArray clientCommArray = new JSONArray();

		for (ClientCommercial clientComm : order.getClientCommercial()) {
			JSONObject clientCommJson = new JSONObject();
			
			clientCommJson.put(JSON_PROP_CLIENTCOMMERCIALID, clientComm.getClient_commercial_id());
			clientCommJson.put(JSON_PROP_COMMERCIALNAME, clientComm.getCommercialName());
			clientCommJson.put(JSON_PROP_COMMERCIALTYPE, clientComm.getCommercialType());

			clientCommJson.put(JSON_PROP_COMMAMOUNT, clientComm.getCommercialAmount());
			clientCommJson.put(JSON_PROP_COMMERCIALCURRENCY, clientComm.getCommercialCurrency());
			clientCommJson.put(JSON_PROP_CLIENTID, clientComm.getClientID());
			clientCommJson.put(JSON_PROP_PARENTCLIENTID, clientComm.getParentClientID());
			clientCommJson.put(JSON_PROP_COMMERCIALENTITYID, clientComm.getCommercialEntityID());
			clientCommJson.put(JSON_PROP_COMMERCIALENTITYTYPE, clientComm.getCommercialEntityType());
			clientCommJson.put(JSON_PROP_COMPANYFLAG, clientComm.isCompanyFlag());

			clientCommJson.put(JSON_PROP_RECIEPTNUMBER, clientComm.getRecieptNumber());
			clientCommJson.put(JSON_PROP_INVOICENUMBER, clientComm.getInVoiceNumber());

			clientCommArray.put(clientCommJson);
		}
		return clientCommArray;
	}

	private JSONObject getOrderSuppPriceInfoJson(AccoOrders order) {

		JSONObject suppPriceJson = new JSONObject();

		suppPriceJson.put(JSON_PROP_SUPPPRICE, order.getSupplierPrice());
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, order.getSupplierPriceCurrencyCode());
		suppPriceJson.put(JSON_PROP_TAXES, new JSONObject(order.getSuppPriceTaxes()));

		return suppPriceJson;
	}

	private JSONObject getOrderTotalPriceInfoJson(AccoOrders order) {

		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put(JSON_PROP_TOTALPRICE, order.getTotalPrice());
		totalPriceJson.put(JSON_PROP_CURRENCYCODE, order.getTotalPriceCurrencyCode());
		totalPriceJson.put(JSON_PROP_TAXES, new JSONObject(order.getTotalPriceTaxes()));
		if(order.getCompanyTaxes()!=null)
        totalPriceJson.put(JSON_PROP_COMPANYTAXES, new JSONObject(order.getCompanyTaxes()));
		if(order.getIncentives()!=null)
	    totalPriceJson.put(JSON_PROP_INCENTIVES, new JSONObject(order.getIncentives()));
		if(order.getDiscounts()!=null)
		totalPriceJson.put(JSON_PROP_DISCOUNTS, new JSONObject(order.getDiscounts()));
		return totalPriceJson;
	}

	public String updateOrder(JSONObject reqJson, String updateType) throws BookingEngineDBException {

		switch (updateType) {
		case JSON_PROP_PAXDETAILS:
			return updatePaxDetails(reqJson);
		case JSON_PROP_PRICES:
			return updatePriceDetails(reqJson);
		case JSON_PROP_ACCO_STAYDATES:
			return updateStayDates(reqJson);
		case JSON_PROP_CLIENTRECONFIRMSTATUS:
			return updateClientReconfirmStatus(reqJson);
		case JSON_PROP_SUPPRECONFIRMSTATUS:
			return updateSuppReconfirmStatus(reqJson);
		case JSON_PROP_TICKETINGPCC:
			return updateTicketingPCC(reqJson);
		case JSON_PROP_STATUS:
			return updateStatus(reqJson);
		case JSON_PROP_ORDERATTRIBUTE:
			return updateOrderAttribute(reqJson);
		case JSON_PROP_ISSHARABLE:
			return updateIsSharable(reqJson);
		case JSON_PROP_SUPPRECONFIRMDATE:
			return updateSuppReconfirmDate(reqJson);
		case JSON_PROP_CLIENTRECONFIRMDATE:
			return updateClientReconfirmDate(reqJson);
		case JSON_PROP_ROOM_DOCUMENTS:
			return updateRoomDocument(reqJson);
		case JSON_PROP_VOUCHERS:
			return updateVouchers(reqJson);
		case JSON_PROP_SUPPREFERENCES:
			return updateSuppReferences(reqJson);
		 case JSON_PROP_EXPIRYTIMELIMIT:
         	return updateTimeLimitDate(reqJson);


		default:
			response.put("ErrorCode", "BE_ERR_000");
			response.put("ErrorMsg", BE_ERR_000);
			myLogger.info(String.format("Update type %s for req %s not found", updateType, reqJson.toString()));
			return (response.toString());
		}
	}
	
	private String updateTimeLimitDate(JSONObject reqJson) throws BookingEngineDBException {

        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AccoOrders order = accoRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("Acco time limit expiry date failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setTimeLimitExpiryDate(reqJson.getString(JSON_PROP_EXPIRYTIMELIMIT));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            AccoOrders updatedclientReconfirmDetails = saveAccoOrder(order, prevOrder);
            myLogger.info(String.format("Acco time limit expiry date updated Successfully for  orderId  %s = %s", orderID, updatedclientReconfirmDetails.toString()));
            return "Acco time limit expiry date updated Successfully";
        }
    
	}

	private String updateSuppReferences(JSONObject reqJson) throws BookingEngineDBException {
		


		AccoOrders order = accoRepository
				.findOne(reqJson.getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"update suppiler refernces failed to update since Order details  not found for  orderid  %s ",
					reqJson.getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSupplierReferenceId(reqJson.getString(JSON_PROP_SUPPLIERREFERENCEID));
			order.setSuppierReservationId(reqJson.getString(JSON_PROP_SUPPLIERRESERVATIONID));
			order.setClientReferenceId(reqJson.getString(JSON_PROP_CLIENTREFERENCEID));
			order.setSupplierCancellationId(reqJson.getString(JSON_PROP_SUPPLIERCANCELLATIONID));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            
			AccoOrders updatedVoucherDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("suppiler refernces updated Successfully for  orderId  %s = %s",
					reqJson.getString(JSON_PROP_ORDERID),
					updatedVoucherDetails.toString()));
			
			return "suppiler refernces Updated Successfully";
		}
	
	
		
	}

	private String updateVouchers(JSONObject reqJson) throws BookingEngineDBException {

		AccoOrders order = accoRepository
				.findOne(reqJson.getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"updateVouchers failed to update since Order details  not found for  orderid  %s ",
					reqJson.getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setVouchers(
					reqJson.getJSONArray(JSON_PROP_VOUCHERIDS).toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoOrders updatedVoucherDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco vouchers updated Successfully for  orderId  %s = %s",
					reqJson.getString(JSON_PROP_ORDERID),
					updatedVoucherDetails.toString()));
			
			return "Acco vouchers Updated Successfully";
		}
	
	}

	private String updateRoomDocument(JSONObject reqJson) throws BookingEngineDBException {
		String roomID = reqJson.getString(JSON_PROP_ROOMID);
		AccoRoomDetails room = roomRepository.findOne(roomID);
		if (room == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_002");
			response.put("ErrorMsg", BE_ERR_ACCO_002);
			myLogger.warn(
					String.format("Document update since room details  not found for  roomID  %s ", roomID));
			return (response.toString());
		} else {
			String prevOrder = room.toString();
			room.setDocumentIds(reqJson.getJSONArray(JSON_PROP_ROOM_DOCUMENTS).toString());
			room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			room.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			roomRepository.saveOrder(room,prevOrder);

			myLogger.info(
					String.format("Room document updated Successfully for  orderId  %s = %s", roomID, room.toString()));
			return "Room document updated Successfully";
		}

	}

	private String updatePriceDetails(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(
					String.format("prices failed to update since Order details  not found for  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();

			Set<SupplierCommercial> suppCommSet = updateOrderSuppComms(reqJson.getJSONArray(JSON_PROP_ORDER_SUPPCOMMS),order);
			Set<ClientCommercial> clientCommSet = updateOrderClientComms(
					reqJson.getJSONArray(JSON_PROP_ORDER_CLIENTCOMMS),order);
			order.setClientCommercial(clientCommSet);
			order.setSuppcommercial(suppCommSet);

			Set<AccoRoomDetails> roomdetailsSet = updateRoomPrices(reqJson.getJSONArray(JSON_PROP_ACCO_ROOMS));
			order.setRoomDetails(roomdetailsSet);
			order.setTotalPrice(reqJson.getJSONObject(JSON_PROP_ORDER_TOTALPRICEINFO).getString(JSON_PROP_TOTALPRICE));
			order.setTotalPriceCurrencyCode(
					reqJson.getJSONObject(JSON_PROP_ORDER_TOTALPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
			order.setTotalPriceTaxes(
					reqJson.getJSONObject(JSON_PROP_ORDER_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).toString());

			order.setSupplierPrice(
					reqJson.getJSONObject(JSON_PROP_ORDER_SUPPLIERPRICEINFO).getString(JSON_PROP_SUPPPRICE));
			order.setSupplierPriceCurrencyCode(
					reqJson.getJSONObject(JSON_PROP_ORDER_SUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
			order.setSuppPriceTaxes(
					reqJson.getJSONObject(JSON_PROP_ORDER_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).toString());

			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

			saveAccoOrder(order, prevOrder);

			myLogger.info(
					String.format("Acco prices updated Successfully for  orderId  %s = %s", orderID, order.toString()));
			return "Acco prices updated Successfully";
		}
	}

	private Set<AccoRoomDetails> updateRoomPrices(JSONArray rooms) {

		Set<AccoRoomDetails> roomDetailsSet = new HashSet<AccoRoomDetails>();
		for (Object object : rooms) {

			JSONObject roomJson = (JSONObject) object;
			AccoRoomDetails room = new AccoRoomDetails();
			room.setId(roomJson.getString(JSON_PROP_ACCO_ROOMID));
			room.setTotalPrice(roomJson.getJSONObject(JSON_PROP_TOTALPRICEINFO).getString(JSON_PROP_TOTALPRICE));
			room.setTotalPriceCurrencyCode(
					roomJson.getJSONObject(JSON_PROP_TOTALPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
			room.setTotalTaxBreakup(
					roomJson.getJSONObject(JSON_PROP_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).toString());

			room.setClientCommercials(roomJson.getJSONArray(JSON_PROP_CLIENTCOMM).toString());
			room.setSuppCommercials(roomJson.getJSONArray(JSON_PROP_SUPPCOMM).toString());
			room.setSupplierPrice(roomJson.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getString(JSON_PROP_SUPPPRICE));
			room.setSupplierPriceCurrencyCode(
					roomJson.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
			room.setSupplierTaxBreakup(
					roomJson.getJSONObject(JSON_PROP_SUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES).toString());

			roomDetailsSet.add(room);
		}

		return roomDetailsSet;
	}

	private Set<SupplierCommercial> updateOrderSuppComms(JSONArray suppCommJsonArray, AccoOrders order) {
		Set<SupplierCommercial> suppCommSet = new HashSet<SupplierCommercial>();

		for (Object object : suppCommJsonArray) {

			JSONObject suppCommJson = (JSONObject) object;
			SupplierCommercial suppComm = new SupplierCommercial();

			suppComm.setSupp_commercial_id(suppCommJson.getString(JSON_PROP_SUPPCOMMERCIALID));
			suppComm.setCommercialName(suppCommJson.getString(JSON_PROP_COMMERCIALNAME));
			suppComm.setCommercialType(suppCommJson.getString(JSON_PROP_COMMERCIALTYPE));
			suppComm.setCommercialAmount(suppCommJson.getString(JSON_PROP_COMMAMOUNT));
			suppComm.setCommercialCurrency(suppCommJson.getString(JSON_PROP_COMMERCIALCURRENCY));
			suppComm.setRecieptNumber(suppCommJson.optString(JSON_PROP_RECIEPTNUMBER));
			suppComm.setInVoiceNumber(suppCommJson.optString(JSON_PROP_INVOICENUMBER));
			suppComm.setOrder(order);
			suppComm.setProduct(JSON_PROP_PRODUCTACCO);
			suppCommSet.add(suppComm);
		}
		return suppCommSet;
	}

	private Set<ClientCommercial> updateOrderClientComms(JSONArray orderClientCommJsonArray, AccoOrders order) {

		Set<ClientCommercial> clientCommSet = new HashSet<ClientCommercial>();

		for (Object object : orderClientCommJsonArray) {
			JSONObject clientCommJson = (JSONObject) object;

			ClientCommercial clientComm = new ClientCommercial();
			clientComm.setClient_commercial_id(clientCommJson.getString(JSON_PROP_CLIENTCOMMERCIALID));
			clientComm.setCommercialName(clientCommJson.getString(JSON_PROP_COMMERCIALNAME));
			clientComm.setCommercialType(clientCommJson.getString(JSON_PROP_COMMERCIALTYPE));
			clientComm.setCommercialAmount(clientCommJson.getString(JSON_PROP_COMMAMOUNT));
			clientComm.setCommercialCurrency(clientCommJson.getString(JSON_PROP_COMMERCIALCURRENCY));
			clientComm.setClientID(clientCommJson.getString(JSON_PROP_CLIENTID));
			clientComm.setParentClientID(clientCommJson.getString(JSON_PROP_PARENTCLIENTID));
			clientComm.setCommercialEntityID(clientCommJson.getString(JSON_PROP_COMMERCIALENTITYID));
			clientComm.setCommercialEntityType(clientCommJson.getString(JSON_PROP_COMMERCIALENTITYTYPE));
			clientComm.setCompanyFlag(clientCommJson.getBoolean(JSON_PROP_COMPANYFLAG));
			clientComm.setRecieptNumber(clientCommJson.optString(JSON_PROP_RECIEPTNUMBER));
			clientComm.setInVoiceNumber(clientCommJson.optString(JSON_PROP_INVOICENUMBER));
            		clientComm.setProduct(JSON_PROP_PRODUCTACCO);
            		clientComm.setOrder(order);
			clientCommSet.add(clientComm);
		}
		return clientCommSet;
	}

	private String updateClientReconfirmDate(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmdate failed to update since Order details  not found for  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmDate(reqJson.getString(JSON_PROP_CLIENTRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

			AccoOrders updatedclientReconfirmDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco client reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Acco order client reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmDate(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmDate failed to update since Order details  not found for  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmDate(reqJson.getString(JSON_PROP_SUPPRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoOrders updatedSuppReconfirmDateDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco supplier reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Acco order supplier reconfirmation date updated Successfully";
		}
	}

	private String updateIsSharable(JSONObject reqJson) throws BookingEngineDBException {
		String roomId = reqJson.getString(JSON_PROP_ACCO_ROOMID);
		AccoRoomDetails room = roomRepository.findOne(roomId);
		if (room == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_002");
			response.put("ErrorMsg", BE_ERR_ACCO_002);
			myLogger.warn(String
					.format("isSharable failed to update since Acco Room details  not found for  roomid  %s", roomId));
			return (response.toString());
		} else {
			String prevRoomDetails = room.toString();
			room.setSharable(reqJson.getBoolean(JSON_PROP_ISSHARABLE));
			room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			room.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoRoomDetails updatedRoomDetails = saveRoomDetails(room, prevRoomDetails);
			myLogger.info(String.format("Acco Room isSharable details successfully for  roomid  %s = %s", roomId,
					updatedRoomDetails.toString()));
			return "Acco Room isSharable updated Successfully";
		}
	}

	private String updateOrderAttribute(JSONObject reqJson) throws BookingEngineDBException {
		AccoOrders order = accoRepository
				.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"Acco updateOrderAttribute failed to update since Order details  not found for  orderid  %s ",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setBookingAttribute(
					reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_BOOKINGATTRIBUTE).toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoOrders updatedclientReconfirmDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco orderAttribute updated Successfully for  orderId  %s = %s",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID),
					updatedclientReconfirmDetails.toString()));
			return "Acco Booking Attribute Updated Successfully";
		}
	}

	private String updatePaxDetails(JSONObject reqJson) throws BookingEngineDBException {
		String paxId = reqJson.getString(JSON_PROP_PAXID);
		PassengerDetails paxDetails = passengerRepository.findOne(paxId);
		if (paxDetails == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_001");
			response.put("ErrorMsg", BE_ERR_ACCO_001);
			myLogger.warn(String.format("Acco Pax details  not found for  paxid  %s ", paxId));
			return (response.toString());
		} else {
			String prevPaxDetails = paxDetails.toString();
			if (!(reqJson.isNull(JSON_PROP_TITLE))) {
				paxDetails.setTitle(reqJson.getString(JSON_PROP_TITLE));
			}

			if (!(reqJson.isNull(JSON_PROP_FIRSTNAME))) {
				paxDetails.setFirstName(reqJson.getString(JSON_PROP_FIRSTNAME));
			}

			if (!(reqJson.isNull(JSON_PROP_MIDDLENAME))) {
				paxDetails.setMiddleName(reqJson.getString(JSON_PROP_MIDDLENAME));
			}

			if (!(reqJson.isNull(JSON_PROP_LASTNAME))) {
				paxDetails.setLastName(reqJson.getString(JSON_PROP_LASTNAME));
			}

			if (!(reqJson.isNull(JSON_PROP_BIRTHDATE))) {
				paxDetails.setBirthDate(reqJson.getString(JSON_PROP_BIRTHDATE));
			}

			if (!(reqJson.isNull(JSON_PROP_ISLEADPAX))) {
				paxDetails.setIsLeadPax(reqJson.getBoolean(JSON_PROP_ISLEADPAX));
			}

			if (!(reqJson.isNull(JSON_PROP_PAX_TYPE))) {
				paxDetails.setPaxType(reqJson.getString(JSON_PROP_PAX_TYPE));
			}

			if (!(reqJson.isNull(JSON_PROP_CONTACTDETAILS))) {
				paxDetails.setContactDetails(reqJson.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			}

			if (!(reqJson.isNull(JSON_PROP_ADDRESSDETAILS))) {
				paxDetails.setAddressDetails(reqJson.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
			}

			
			// Service in JSON
			// if (paxDetails.getPaxType().equals(Pax_ADT))
			// paxDetails.setDocumentDetails(reqJson.getJSONObject(JSON_PROP_DOCUMENTDETAILS).toString());
			// paxDetails.setAncillaryServices(reqJson.getJSONObject(JSON_PROP_ANCILLARYSERVICES).toString());

			paxDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			paxDetails.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

			PassengerDetails updatedPaxDetails = savePaxDetails(paxDetails, prevPaxDetails);
			myLogger.info(String.format("Acco Pax details updated successfully for  paxid  %s = %s", paxId,
					updatedPaxDetails.toString()));
			return "Acco pax details updated Successfully";

		}
	}

	private String updateStayDates(JSONObject reqJson) throws BookingEngineDBException {
		String roomId = reqJson.getString(JSON_PROP_ACCO_ROOMID);
		AccoRoomDetails room = roomRepository.findOne(roomId);
		if (room == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_002");
			response.put("ErrorMsg", BE_ERR_ACCO_002);
			myLogger.warn(String
					.format("Stay dates failed to update since Acco Room details  not found for  roomid  %s", roomId));
			return (response.toString());
		} else {
			String prevRoomDetails = room.toString();
			room.setCheckInDate(reqJson.getString(JSON_PROP_ACCO_CHKIN));
			room.setCheckOutDate(reqJson.getString(JSON_PROP_ACCO_CHKOUT));
			room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			room.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoRoomDetails updatedRoomDetails = saveRoomDetails(room, prevRoomDetails);
			myLogger.info(String.format("Acco Room check in/out details updated successfully for  roomid  %s = %s",
					roomId, updatedRoomDetails.toString()));
			return "Acco Room check in/out details updated Successfully";
		}
	}

	private String updateClientReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmStatus failed to update since Order details  not found for  orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmStatus(reqJson.getString(JSON_PROP_CLIENTRECONFIRMSTATUS));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoOrders updatedclientReconfirmDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco client reconfirmation status updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Acco order client reconfirmation status updated Successfully";
		}
	}

	private String updateSuppReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmStatus failed to update since Order details  not found for  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmStatus(reqJson.getString("suppReconfirmStatus"));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoOrders updatedSuppReconfirmDateDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(
					String.format("Acco supplier reconfirmation status updated Successfully for  orderId  %s = %s",
							orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Acco order supplier reconfirmation status updated Successfully";
		}
	}

	private String updateTicketingPCC(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String
					.format("TicketingPCC failed to update since Order details  not found for  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setTicketingPCC(reqJson.getString(JSON_PROP_TICKETINGPCC));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			AccoOrders updatedTicketingPCCDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco ticketingPCC updated Successfully for  orderId  %s = %s", orderID,
					updatedTicketingPCCDetails.toString()));
			return "Acco order ticketing PCC updated Successfully";
		}
	}

	private String updateStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		AccoOrders order = accoRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(
					String.format("Status failed to update since Order details  not found for  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			order.setStatus(reqJson.getString(JSON_PROP_STATUS));
			AccoOrders updatedStatusDetails = saveAccoOrder(order, prevOrder);
			myLogger.info(String.format("Acco Order status updated Successfully for  orderId  %s = %s", orderID,
					updatedStatusDetails.toString()));
			return "Acco Order status updated Successfully";
		}
	}

	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevOrder) throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			myLogger.fatal("Error while saving Acco Passenger order object : " + e);
			throw new BookingEngineDBException("Failed to save order object");
		}
		return passengerRepository.saveOrder(orderObj, prevOrder);
	}

	private AccoRoomDetails saveRoomDetails(AccoRoomDetails room, String prevRoomDetails)
			throws BookingEngineDBException {
		AccoRoomDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(room, AccoRoomDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			myLogger.fatal("Error while saving Acco Room  object : " + e);
			throw new BookingEngineDBException("Failed to save acco room object");
		}
		return roomRepository.saveOrder(orderObj, prevRoomDetails);
	}

	public AccoOrders saveAccoOrder(AccoOrders currentOrder, String prevOrder) throws BookingEngineDBException {
		AccoOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, AccoOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			myLogger.fatal("Error while saving Acco order object : " + e);
			throw new BookingEngineDBException("Failed to save order object");
		}
		return accoRepository.saveOrder(orderObj, prevOrder);
	}

	/*
	 * public SupplierCommercial saveSupplierCommercial(SupplierCommercial
	 * currentOrder, String prevOrder) throws BookingEngineDBException {
	 * SupplierCommercial orderObj = null; try { orderObj =
	 * CopyUtils.copy(currentOrder, SupplierCommercial.class);
	 * 
	 * } catch (InvocationTargetException | IllegalAccessException e) {
	 * myLogger.fatal("Error while saving Acco order object : " + e); throw new
	 * BookingEngineDBException("Failed to save order object"); } return
	 * suppCommRepo.saveOrder(orderObj,prevOrder); }
	 * 
	 * public ClientCommercial saveClientCommercial(ClientCommercial currentOrder,
	 * String prevOrder) throws BookingEngineDBException { ClientCommercial orderObj
	 * = null; try { orderObj = CopyUtils.copy(currentOrder,
	 * ClientCommercial.class);
	 * 
	 * } catch (InvocationTargetException | IllegalAccessException e) {
	 * myLogger.fatal("Error while saving Acco order object : " + e); throw new
	 * BookingEngineDBException("Failed to save order object"); } return
	 * clientCommRepo.saveOrder(orderObj,prevOrder); }
	 */

	public JSONArray getCancellationsByBooking(Booking booking) {

		List<AccoOrders> accoOrders = accoRepository.findByBooking(booking);
		JSONArray accoOrdersJson = getAccoOrdersCancellation(accoOrders, "cancel");
		return accoOrdersJson;

	}

	public JSONArray getPoliciesByBooking(Booking booking) {

		List<AccoOrders> accoOrders = accoRepository.findByBooking(booking);
		JSONArray policies = getPoliciesByBooking(accoOrders);
		return policies;

	}

	private JSONArray getPoliciesByBooking(List<AccoOrders> accoOrders) {
		JSONArray orders = new JSONArray();
		JSONObject orderObj;
		JSONArray roomArray;
		JSONObject roomObj;
		for (AccoOrders order : accoOrders) {
			orderObj = new JSONObject();
			roomArray = new JSONArray();
			for (AccoRoomDetails room : order.getRoomDetails()) {
				roomObj = new JSONObject();
				if (room.getCancellationPolicies() != null) {
					roomObj.put(JSON_PROP_POLICIES, new JSONArray(room.getCancellationPolicies()));
					roomObj.put(JSON_PROP_ACCO_ROOMID, room.getId());
				}
				if (roomObj.length() > 0)
					roomArray.put(roomObj);
			}
			if (roomArray.length() > 0) {
				orderObj.put(JSON_PROP_ORDERID, order.getId());
				orderObj.put(JSON_PROP_ROOMPOLICIES, roomArray);
			}
			if (orderObj.length() > 0)
				orders.put(orderObj);
		}
		return orders;
	}

	public JSONArray getAmendmentsByBooking(Booking booking) {

		List<AccoOrders> accoOrders = accoRepository.findByBooking(booking);
		JSONArray accoOrdersJson = getAccoOrdersAmendments(accoOrders, "amend");
		return accoOrdersJson;

	}

	private JSONArray getAccoOrdersCancellation(List<AccoOrders> accoOrders, String type) {

		JSONArray response = new JSONArray();
		for (AccoOrders order : accoOrders) {
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();

			List<AmCl> cancelAccoOrders = amClRepository.findByEntity("order", orderId, type);
			JSONObject cancelOrderJson = new JSONObject();
			if (cancelAccoOrders.size() > 0) {
				orderJson.put(JSON_PROP_ORDERCANCELLATIONS, cancelOrderJson);
				// There will be never two cancellation entries for same order. so loop will
				// only run once
				for (AmCl cancelAccoOrder : cancelAccoOrders) {

					cancelOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelAccoOrder.getSupplierCharges());
					cancelOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelAccoOrder.getCompanyCharges());
					cancelOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE,
							cancelAccoOrder.getSupplierChargesCurrencyCode());
					cancelOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
							cancelAccoOrder.getCompanyChargesCurrencyCode());
					cancelOrderJson.put(JSON_PROP_CANCELTYPE, cancelAccoOrder.getDescription());
					cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelAccoOrder.getCreatedAt().toString().substring(0,
							cancelAccoOrder.getCreatedAt().toString().indexOf('[')));
					cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelAccoOrder.getLastModifiedAt().toString()
							.substring(0, cancelAccoOrder.getLastModifiedAt().toString().indexOf('[')));
					cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelAccoOrder.getLastModifiedBy());
				}

				orderJson.put(JSON_PROP_ORDERID, orderId);

			}
			/*
			 * Set<AccoRoomDetails> rooms =
			 * accoRepository.findOne(orderId).getRoomDetails();
			 * 
			 * JSONArray roomJsonArray = new JSONArray();
			 * 
			 * for (AccoRoomDetails room : rooms) {
			 * 
			 * String roomID = room.getId();
			 */
			JSONArray roomJsonArray = new JSONArray();
			List<AmCl> cancelRoomOrders = amClRepository.findByEntity(JSON_PROP_ENTITYTYPE_ROOM, orderId, type);
			if (cancelRoomOrders.size() > 0) {
				JSONObject cancelRoomJson;
				orderJson.put(JSON_PROP_ORDERID, orderId);
				// There will be never two cancellation entries for same order. so loop will
				// only run once
				for (AmCl cancelRoomOrder : cancelRoomOrders) {
					cancelRoomJson = new JSONObject();
					cancelRoomJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelRoomOrder.getSupplierCharges());
					cancelRoomJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelRoomOrder.getCompanyCharges());
					cancelRoomJson.put(JSON_PROP_SUPPCANCCHARGESCODE, cancelRoomOrder.getSupplierChargesCurrencyCode());
					cancelRoomJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
							cancelRoomOrder.getCompanyChargesCurrencyCode());
					cancelRoomJson.put(JSON_PROP_CANCELTYPE, cancelRoomOrder.getDescription());
					cancelRoomJson.put(JSON_PROP_CREATEDAT, cancelRoomOrder.getCreatedAt().toString().substring(0,
							cancelRoomOrder.getCreatedAt().toString().indexOf('[')));
					cancelRoomJson.put(JSON_PROP_LASTMODIFIEDAT, cancelRoomOrder.getLastModifiedAt().toString()
							.substring(0, cancelRoomOrder.getLastModifiedAt().toString().indexOf('[')));
					cancelRoomJson.put(JSON_PROP_LASTMODIFIEDBY, cancelRoomOrder.getLastModifiedBy());

					if (cancelRoomJson != null && (cancelRoomJson.has(JSON_PROP_SUPPLIERCANCELCHARGES))) {
						// JSONObject roomJson = new JSONObject();

						JSONArray entityIDArray = new JSONArray(cancelRoomOrder.getEntityID());
						StringBuffer newEntityIds = new StringBuffer();
						for (int i = 0; i < entityIDArray.length(); i++) {

							String currEntityId = entityIDArray.getJSONObject(i).getString(JSON_PROP_ENTITYID);
							if (i == 0) {
								newEntityIds.append(currEntityId);
							} else {
								newEntityIds.append(",").append(currEntityId);
								// String newEntityIds = entityIDs.replaceAll("\\|", "\\,");
							}
						}
						cancelRoomJson.put(JSON_PROP_ACCO_ROOMID, newEntityIds);
						// roomJson.put("roomCancellation", cancelRoomJson);
						roomJsonArray.put(cancelRoomJson);
					}

				}

				orderJson.put(JSON_PROP_ACCO_ROOMCANCELLATIONS, roomJsonArray);
			}
			if (orderJson.length() > 0) {
				orderJson.put(JSON_PROP_PRODUCTCATEGORY, "Accommodation");
				orderJson.put(JSON_PROP_PRODUCTSUBCATEGORY, order.getProductSubCategory());
				response.put(orderJson);
			}
		}

		return response;
	}

	private JSONArray getAccoOrdersAmendments(List<AccoOrders> accoOrders, String type) {
		JSONArray response = new JSONArray();
		for (AccoOrders order : accoOrders) {
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			List<AmCl> cancelAccoOrders = amClRepository.findByEntity(JSON_PROP_ENTITYTYPE_ORDER, orderId, type);
			JSONArray orderCancelArray = new JSONArray();

			for (AmCl cancelAccoOrder : cancelAccoOrders) {
				JSONObject cancelOrderJson = new JSONObject();
				cancelOrderJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, cancelAccoOrder.getSupplierCharges());
				cancelOrderJson.put(JSON_PROP_COMPANYAMENDCHARGES, cancelAccoOrder.getCompanyCharges());
				cancelOrderJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE,
						cancelAccoOrder.getSupplierChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE,
						cancelAccoOrder.getCompanyChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_AMENDTYPE, cancelAccoOrder.getDescription());
				cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelAccoOrder.getCreatedAt().toString().substring(0,
						cancelAccoOrder.getCreatedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelAccoOrder.getLastModifiedAt().toString()
						.substring(0, cancelAccoOrder.getLastModifiedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelAccoOrder.getLastModifiedBy());
				orderCancelArray.put(cancelOrderJson);
			}

			// Set<AccoRoomDetails> rooms =
			// accoRepository.findOne(orderId).getRoomDetails();
			JSONArray roomJsonArray = new JSONArray();

			List<AmCl> cancelRoomOrders = amClRepository.findByEntity(JSON_PROP_ENTITYTYPE_ROOM, orderId, type);

			for (AmCl cancelRoomOrder : cancelRoomOrders) {
				JSONObject cancelRoomJson = new JSONObject();
				cancelRoomJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, cancelRoomOrder.getSupplierCharges());
				cancelRoomJson.put(JSON_PROP_COMPANYAMENDCHARGES, cancelRoomOrder.getCompanyCharges());
				cancelRoomJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE,
						cancelRoomOrder.getSupplierChargesCurrencyCode());
				cancelRoomJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE,
						cancelRoomOrder.getCompanyChargesCurrencyCode());
				cancelRoomJson.put(JSON_PROP_AMENDTYPE, cancelRoomOrder.getDescription());
				cancelRoomJson.put(JSON_PROP_CREATEDAT, cancelRoomOrder.getCreatedAt().toString().substring(0,
						cancelRoomOrder.getCreatedAt().toString().indexOf('[')));
				cancelRoomJson.put(JSON_PROP_LASTMODIFIEDAT, cancelRoomOrder.getLastModifiedAt().toString().substring(0,
						cancelRoomOrder.getLastModifiedAt().toString().indexOf('[')));
				cancelRoomJson.put(JSON_PROP_LASTMODIFIEDBY, cancelRoomOrder.getLastModifiedBy());

				if (cancelRoomJson != null && (cancelRoomJson.has(JSON_PROP_SUPPLIERAMENDCHARGES))) {
					// JSONObject roomJson = new JSONObject();
					JSONArray entityIDArray = new JSONArray(cancelRoomOrder.getEntityID());
					StringBuffer newEntityIds = new StringBuffer();
					StringBuffer newPaxIds = new StringBuffer();
					for (int i = 0; i < entityIDArray.length(); i++) {

						String currEntityId = entityIDArray.getJSONObject(i).getString(JSON_PROP_ENTITYID);
						JSONArray currPaxIds = entityIDArray.getJSONObject(i).optJSONArray("paxIDs");
						if (currPaxIds != null && currPaxIds.length() > 0) {
							for (int z = 0; z < currPaxIds.length(); z++) {
								String currPaxId = currPaxIds.getJSONObject(z).getString(JSON_PROP_PAXID);
								if (z == 0) {
									newPaxIds.append(currPaxId);
								} else {
									newPaxIds.append(",").append(currPaxId);
								}
							}
						}
						if (i == 0) {
							newEntityIds.append(currEntityId);
						} else {
							newEntityIds.append(",").append(currEntityId);
							// String newEntityIds = entityIDs.replaceAll("\\|", "\\,");
						}
					}
					if (newPaxIds.length() > 0)
						cancelRoomJson.put(JSON_PROP_PAXID, newPaxIds);
					cancelRoomJson.put(JSON_PROP_ACCO_ROOMID, newEntityIds);
					// roomJson.put(JSON_PROP_ACCO_ROOMAMENDMENTS, cancelRoomJson);
					roomJsonArray.put(cancelRoomJson);
				}

			}
			if (orderCancelArray.length() > 0) {
				orderJson.put(JSON_PROP_PRODUCTCATEGORY, "Accommodation");
				orderJson.put(JSON_PROP_PRODUCTSUBCATEGORY, order.getProductSubCategory());
				orderJson.put(JSON_PROP_ORDERID, orderId);
				orderJson.put(JSON_PROP_ORDERAMENDS, orderCancelArray);
			} else if (roomJsonArray.length() > 0) {
				orderJson.put(JSON_PROP_ORDERID, orderId);
				orderJson.put(JSON_PROP_ACCO_ROOMAMENDMENTS, roomJsonArray);
				orderJson.put(JSON_PROP_PRODUCTCATEGORY, "Accommodation");
				orderJson.put(JSON_PROP_PRODUCTSUBCATEGORY, order.getProductSubCategory());
			}
			if (orderJson.length() > 0)
				response.put(orderJson);

		}

		return response;

	}

	public JSONArray getOrdersInRange(ZonedDateTime startdateTime, ZonedDateTime enddateTime, String suppRef) {
		List<AccoOrders> temp = accoRepository.getOrdersInRange(startdateTime, enddateTime, suppRef);
		JSONArray accoOrdersArray = getAccoOrdersJson(temp, "false");
		return accoOrdersArray;
	}

	public String getDocumentDetails(JSONObject reqJson) {
		JSONArray resJson = new JSONArray();
		JSONArray paxDetails;
		AccoOrders order = accoRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(
					String.format("TicketingPCC failed to update since Order details  not found for  orderid  %s ",
							reqJson.getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			for (AccoRoomDetails room : order.getRoomDetails()) {
				paxDetails = new JSONArray(room.getPaxDetails());
				for (int i = 0; i < paxDetails.length(); i++) {
					JSONObject paxDocObj = new JSONObject();
					JSONObject pax = paxDetails.getJSONObject(i);
					PassengerDetails paxObj = passengerRepository.findOne(pax.getString(JSON_PROP_PAXID));
					paxDocObj.put(JSON_PROP_PAXID, pax.getString(JSON_PROP_PAXID));
					if (paxObj.getDocumentDetails() != null)
						paxDocObj.put(JSON_PROP_DOCUMENTDETAILS, new JSONObject(paxObj.getDocumentDetails()));
					resJson.put(paxDocObj);
				}
			}
			myLogger.info(
					String.format("Document details retrieved for orderID %s ", reqJson.getString(JSON_PROP_ORDERID)));
			return resJson.toString();
		}
	}
}
