package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.CruiseAmCl;
import com.coxandkings.travel.bookingengine.db.model.CruiseOrders;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.CruiseAmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.CruiseDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly = false)
public class CruiseBookingServiceImpl implements Constants,ErrorConstants {

	@Qualifier("Cruise")
	@Autowired
	private CruiseDatabaseRepository cruiseRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	@Autowired
	@Qualifier("CruiseAmCl")
	private CruiseAmClRepository cruiseAmClRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
		
	JSONObject response=new JSONObject();
	
	public JSONArray process(Booking booking) {
		List<CruiseOrders> cruiseOrders = cruiseRepository.findByBooking(booking);
		JSONArray cruiseOrdersJson = getCruiseOrdersJson(cruiseOrders);
		return cruiseOrdersJson;
	}
	
	public String getBysuppID(String suppID) {
		List<CruiseOrders> orders = cruiseRepository.findBysuppID(suppID);
		//TODO: check if they need bookID inside each air order
		if(orders.size()==0)
		{
			response.put("ErrorCode", "BE_ERR_CRUISE_001");
			response.put("ErrorMsg", BE_ERR_CRUISE_001);
			myLogger.warn(String.format("Cruise Orders not present  for suppID %s", suppID));
			return response.toString();
		}
		else
		{
			JSONArray ordersArray =getCruiseOrdersJson(orders);
			myLogger.info(String.format("Cruise Orders retrieved for suppID %s = %s", suppID, ordersArray.toString()));
			return ordersArray.toString();
		}
	}
	
	public JSONArray getCruiseOrdersJson(List<CruiseOrders> orders) {
		JSONArray cruiseArray = new JSONArray();
		JSONObject cruiseJson = new JSONObject();
		for (CruiseOrders order : orders) {
			cruiseJson = getCruiseOrderJson(order);
			cruiseArray.put(cruiseJson);
		}
		return cruiseArray;	
	}
	
	public JSONObject getCruiseOrderJson(CruiseOrders order)
	{
		JSONObject cruiseJson = new JSONObject();
		
		//TODO: to check from where will we get these details from WEM/BE
		cruiseJson.put(JSON_PROP_CREDENTIALSNAME, "");
		
		//TODO: added these fields on the suggestions of operations
//		cruiseJson.put("QCStatus",order.getQCStatus());
		cruiseJson.put(JSON_PROP_SUPPLIERRECONFIRMATIONSTATUS, order.getSuppReconfirmStatus());
		cruiseJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());
		
		//TODO: we need to check how will SI send us the details for Enabler Supplier and source supplier
		cruiseJson.put(JSON_PROP_ENABLERSUPPLIERNAME, order.getSupplierID());
		cruiseJson.put(JSON_PROP_SOURCESUPPLIERNAME, order.getSupplierID());
		
		//TODO: to check what value to sent when it has not reach the cancel/amend stage
		cruiseJson.put(JSON_PROP_CANCELDATE, "");
		cruiseJson.put(JSON_PROP_AMENDDATE, "");
		cruiseJson.put(JSON_PROP_INVENTORY, "N");

		//TODO: check for the category and subcategory constants.
		cruiseJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
		cruiseJson.put(JSON_PROP_PRODUCTSUBCATEGORY, JSON_PROP_AIR_PRODUCTSUBCATEGORY);
		cruiseJson.put(JSON_PROP_ORDERID, order.getId());
		cruiseJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		cruiseJson.put(JSON_PROP_STATUS, order.getStatus());
		cruiseJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());
		
		JSONObject orderDetails = new JSONObject();
		orderDetails.put("reservationID", order.getReservationID());
		orderDetails.put("companyName", order.getBookingCompanyName());
		orderDetails.put("bookingDate", order.getBookingDateTime());
		
		orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
		orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS,getClientComms(order));
		orderDetails.put("cruiseDetails", new JSONObject(order.getCruiseDetails()));
		orderDetails.put(JSON_PROP_PAXINFO, getPaxInfoJson(order));
		orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getSuppPriceInfoJson(order));
		orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getTotalPriceInfoJson(order));
		
		cruiseJson.put(JSON_PROP_ORDERDETAILS, orderDetails);
		return cruiseJson;
	}
	
	private  JSONObject getTotalPriceInfoJson(CruiseOrders order) {
		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put(JSON_PROP_TOTALPRICE, order.getTotalPrice());
		totalPriceJson.put(JSON_PROP_CURRENCYCODE, order.getTotalPriceCurrencyCode());
		totalPriceJson.put(JSON_PROP_AIR_PAXTYPEFARES, new JSONArray(order.getTotalPaxTypeFares()));
		
		totalPriceJson.put(JSON_PROP_BASEFARE, new JSONObject(order.getTotalPriceBaseFare()));
		totalPriceJson.put(JSON_PROP_RECEIVABLES, new JSONArray(order.getTotalPriceReceivables()));
		if(order.getTotalPriceFees()!=null)
		totalPriceJson.put(JSON_PROP_FEES, new JSONObject(order.getTotalPriceFees()));
		if(order.getTotalPriceTaxes()!=null)
		totalPriceJson.put(JSON_PROP_TAXES, new JSONObject(order.getTotalPriceTaxes()));
		
		return totalPriceJson;
	}
	
	private  JSONObject getSuppPriceInfoJson(CruiseOrders order) {

		JSONObject suppPriceJson = new JSONObject();

		suppPriceJson.put(JSON_PROP_SUPPPRICE, order.getSupplierPrice());
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, order.getSupplierPriceCurrencyCode());
		suppPriceJson.put(JSON_PROP_AIR_PAXTYPEFARES, new JSONArray(order.getSuppPaxTypeFares()));

		return suppPriceJson;
	}
	
	private  JSONArray getPaxInfoJson(CruiseOrders order) {
		
		JSONArray paxJsonArray = new JSONArray();
		
		for (Object paxId : new JSONArray(order.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject)paxId;
			
			PassengerDetails guest  = passengerRepository.findOne(paxIdJson.getString(JSON_PROP_PAXID));
			JSONObject paxJson = new JSONObject();
			paxJson.put(JSON_PROP_AIR_PASSENGERID,guest.getPassanger_id());
			paxJson.put(JSON_PROP_PAX_TYPE, guest.getPaxType());
			paxJson.put(JSON_PROP_TITLE,guest.getTitle());
			paxJson.put(JSON_PROP_ISLEADPAX, guest.getIsLeadPax());
			paxJson.put(JSON_PROP_FIRSTNAME, guest.getFirstName());
			paxJson.put(JSON_PROP_MIDDLENAME, guest.getMiddleName());
			paxJson.put(JSON_PROP_LASTNAME, guest.getLastName());
			paxJson.put(JSON_PROP_BIRTHDATE, guest.getBirthDate());
			paxJson.put(JSON_PROP_STATUS, guest.getStatus());
			paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONObject(guest.getContactDetails()));
			if(Pax_ADT.equals(guest.getPaxType()))
			paxJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(guest.getAddressDetails()));
			//TODO: Confirm if we are going to move it to flight details section?
			if(guest.getSpecialRequests()!=null)
			paxJson.put(JSON_PROP_SPECIALREQUESTS, new JSONObject(guest.getSpecialRequests()));
//			paxJson.put(JSON_PROP_ANCILLARYSERVICES, new JSONObject(guest.getAncillaryServices()));
			
			paxJsonArray.put(paxJson);
		}
		
		return paxJsonArray;
	}
	
	private JSONArray getSuppComms(CruiseOrders order) {
		
		JSONArray suppCommArray = new JSONArray();
		for(SupplierCommercial suppComm: order.getSuppcommercial()) {
			JSONObject suppCommJson = new JSONObject();
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
	
	private JSONArray getClientComms(CruiseOrders order) {
		
		JSONArray clientCommArray = new JSONArray();
		
		for(ClientCommercial clientComm: order.getClientCommercial()) {
			JSONObject clientCommJson = new JSONObject();
			
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
	
	public JSONArray getCancellationsByBooking(Booking booking) {

		List<CruiseOrders> cruiseOrders = cruiseRepository.findByBooking(booking);
		JSONArray cruiseOrdersJson = getCruiseOrdersCancellations(cruiseOrders, "cancel");
		return cruiseOrdersJson;
	}
	
	private JSONArray getCruiseOrdersCancellations(List<CruiseOrders> cruiseOrders,String type) {
		JSONArray response = new JSONArray();
		for (CruiseOrders order : cruiseOrders) {
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			
			List<CruiseAmCl> cancelCruiseOrders = cruiseAmClRepository.findByEntity("order", orderId, type);
			if(cancelCruiseOrders.size()>0) {
			orderJson.put(JSON_PROP_ORDERID, orderId);
			JSONArray orderCancelArray = new JSONArray();

			for (CruiseAmCl cancelAirOrder : cancelCruiseOrders) {
				JSONObject cancelOrderJson = new JSONObject();
				cancelOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelAirOrder.getSupplierCharges());
				cancelOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelAirOrder.getCompanyCharges());
				cancelOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE,
				cancelOrderJson.put(JSON_PROP_SUPPCHARGESCURRENCYCODE, cancelAirOrder.getSupplierChargesCurrencyCode()));
				cancelOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
				cancelOrderJson.put(JSON_PROP_COMPANYCHARGESCURRENCYCODE, cancelAirOrder.getCompanyChargesCurrencyCode()));
				cancelOrderJson.put(JSON_PROP_CANCELTYPE, cancelAirOrder.getDescription());
				cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelAirOrder.getCreatedAt().toString().substring(0, cancelAirOrder.getCreatedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelAirOrder.getLastModifiedAt().toString().substring(0, cancelAirOrder.getLastModifiedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelAirOrder.getLastModifiedBy());
				orderCancelArray.put(cancelOrderJson);
			}

			orderJson.put(JSON_PROP_ORDERCANCELLATIONS, orderCancelArray);
		
			List<CruiseAmCl> cancelPaxOrders = cruiseAmClRepository.findByEntity("pax", orderId, type);
			if(cancelPaxOrders.size()>0) {
				JSONArray orderCancelPaxArray = new JSONArray();
				for (CruiseAmCl cancelPaxOrder : cancelPaxOrders) {
					JSONObject cancelPaxOrderJson = new JSONObject();
					cancelPaxOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelPaxOrder.getSupplierCharges());
					cancelPaxOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelPaxOrder.getCompanyCharges());
					cancelPaxOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE,
					cancelPaxOrderJson.put(JSON_PROP_SUPPCHARGESCURRENCYCODE, cancelPaxOrder.getSupplierChargesCurrencyCode()));
					cancelPaxOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
					cancelPaxOrderJson.put(JSON_PROP_COMPANYCHARGESCURRENCYCODE, cancelPaxOrder.getCompanyChargesCurrencyCode()));
					cancelPaxOrderJson.put(JSON_PROP_CANCELTYPE, cancelPaxOrder.getDescription());
					cancelPaxOrderJson.put(JSON_PROP_CREATEDAT, cancelPaxOrder.getCreatedAt().toString().substring(0, cancelPaxOrder.getCreatedAt().toString().indexOf('[')));
					cancelPaxOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelPaxOrder.getLastModifiedAt().toString().substring(0, cancelPaxOrder.getLastModifiedAt().toString().indexOf('[')));
					cancelPaxOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelPaxOrder.getLastModifiedBy());
					String entityIDs = cancelPaxOrder.getEntityID();
					String newEntityIds = entityIDs.replaceAll("\\|", "\\,");
					cancelPaxOrderJson.put("paxIDs", newEntityIds);
					orderCancelPaxArray.put(cancelPaxOrderJson);
				}
				orderJson.put(JSON_PROP_PAXCANCELLATIONS, orderCancelPaxArray);
			}
			response.put(orderJson);
			}
			
	
		}
		return response;
	}
	
	public JSONArray getAmendmentsByBooking(Booking booking) {

		List<CruiseOrders> cruiseOrders = cruiseRepository.findByBooking(booking);
		JSONArray cruiseOrdersJson = getCruiseOrdersAmendments(cruiseOrders, "amend");
		return cruiseOrdersJson;
	}
	
	private JSONArray getCruiseOrdersAmendments(List<CruiseOrders> cruiseOrders,String type) {
		
		JSONArray response = new JSONArray();
		for (CruiseOrders order : cruiseOrders) {
			String orderId = order.getId();
			
			JSONObject testJson = new JSONObject();
			testJson.put("entityID", orderId);
			
			JSONObject orderJson = new JSONObject();
		
			List<CruiseAmCl> cancelCruiseOrders = cruiseAmClRepository.findByEntity(JSON_PROP_ENTITYTYPE_ORDER, orderId, type);
			JSONArray orderCancelArray = new JSONArray();
		
			for (CruiseAmCl cancelCruiseOrder : cancelCruiseOrders) {//for the future
				JSONObject cancelOrderJson = new JSONObject();
				cancelOrderJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, cancelCruiseOrder.getSupplierCharges());
				cancelOrderJson.put(JSON_PROP_COMPANYAMENDCHARGES, cancelCruiseOrder.getCompanyCharges());
				cancelOrderJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE,
						cancelCruiseOrder.getSupplierChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE,
						cancelCruiseOrder.getCompanyChargesCurrencyCode());
				cancelOrderJson.put(JSON_PROP_AMENDTYPE, cancelCruiseOrder.getDescription());
				cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelCruiseOrder.getCreatedAt().toString().substring(0, cancelCruiseOrder.getCreatedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelCruiseOrder.getLastModifiedAt().toString().substring(0, cancelCruiseOrder.getLastModifiedAt().toString().indexOf('[')));
				cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelCruiseOrder.getLastModifiedBy());
				orderCancelArray.put(cancelOrderJson);
			}
			
			JSONArray cancelPAxJsonArray = new JSONArray();
			JSONArray paxJsonArray = new JSONArray();
		     List<CruiseAmCl> cancelPaxOrders = cruiseAmClRepository.findByEntity(JSON_PROP_ENTITYTYPE_PAX, orderId, type);

				

			for (CruiseAmCl cancelPaxOrder : cancelPaxOrders) {
				JSONObject cancelPaxJson = new JSONObject();
				cancelPaxJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, cancelPaxOrder.getSupplierCharges());
				cancelPaxJson.put(JSON_PROP_COMPANYAMENDCHARGES, cancelPaxOrder.getCompanyCharges());
				cancelPaxJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE,
					cancelPaxOrder.getSupplierChargesCurrencyCode());
				cancelPaxJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE,
					cancelPaxOrder.getCompanyChargesCurrencyCode());
				cancelPaxJson.put(JSON_PROP_AMENDTYPE, cancelPaxOrder.getDescription());
				cancelPaxJson.put(JSON_PROP_CREATEDAT, cancelPaxOrder.getCreatedAt().toString().substring(0, cancelPaxOrder.getCreatedAt().toString().indexOf('[')));
				cancelPaxJson.put(JSON_PROP_LASTMODIFIEDAT, cancelPaxOrder.getLastModifiedAt().toString().substring(0, cancelPaxOrder.getLastModifiedAt().toString().indexOf('[')));
				cancelPaxJson.put(JSON_PROP_LASTMODIFIEDBY, cancelPaxOrder.getLastModifiedBy());
//				JSONArray entityIds=new JSONArray(cancelPaxOrder.getEntityID());
//				StringBuffer newEntityIds=new StringBuffer();
				/*for (int z=0;z<entityIds.length();z++) {
					if(z==0) {
						newEntityIds.append(entityIds.getJSONObject(z).getString("entityID"));
					}
					else
					{
						newEntityIds.append("\\,").append(entityIds.getJSONObject(z).getString("entityID"));
					}
				cancelPaxJson.put("paxIDs", newEntityIds);
				}*/
				cancelPaxJson.put("paxIDs", cancelPaxOrder.getEntityID());
				cancelPAxJsonArray.put(cancelPaxJson);
			}
			if (cancelPAxJsonArray != null && cancelPAxJsonArray.length()!=0) {
				JSONObject paxJson = new JSONObject();
				paxJsonArray.put(paxJson);
			}
			if(orderCancelArray.length()>0) {
				orderJson.put(JSON_PROP_ORDERID, orderId);
				orderJson.put(JSON_PROP_ORDERAMENDS, orderCancelArray);
			}
			if(paxJsonArray.length()>0) {
				orderJson.put(JSON_PROP_ORDERID, orderId);
				orderJson.put(JSON_PROP_PAXAMENDMENTS, cancelPAxJsonArray);
			}
			
			if(orderJson.length()>0)
	        {
		    	orderJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
				orderJson.put(JSON_PROP_PRODUCTSUBCATEGORY, JSON_PROP_PRODUCTCRUISE);
				response.put(orderJson);
	        }
			
		}
		return response;
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

	private String updateOrderAttribute(JSONObject reqJson) throws BookingEngineDBException {
		CruiseOrders order = cruiseRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"Cruise updateOrderAttribute failed to update since Order details  not found for  orderid  %s ",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setBookingAttribute(
					reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("bookingAttribute").toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            CruiseOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Cruise orderAttribute updated Successfully for  orderId  %s = %s",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID),
					updatedclientReconfirmDetails.toString()));
			return "Cruise Booking Attribute Updated Successfully";
		}
	}

	private String updateTimeLimitDate(JSONObject reqJson) throws BookingEngineDBException {

        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        CruiseOrders order = cruiseRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("Cruise time limit expiry date failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setTimeLimitExpiryDate(reqJson.getString(JSON_PROP_EXPIRYTIMELIMIT));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            CruiseOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
            myLogger.info(String.format("Cruise time limit expiry date updated Successfully for  orderId  %s = %s", orderID, updatedclientReconfirmDetails.toString()));
            return "Cruise time limit expiry date updated Successfully";
        }
    
	}
	
	private String updateVouchers(JSONObject reqJson) throws BookingEngineDBException {

		CruiseOrders order = cruiseRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"updateVouchers failed to update since Cruise Order details  not found for  orderid  %s ",
					reqJson.getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setVouchers(
					reqJson.getJSONArray(JSON_PROP_VOUCHERIDS).toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            CruiseOrders updatedVoucherDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Cruise vouchers updated Successfully for  orderId  %s = %s",
					reqJson.getString(JSON_PROP_ORDERID),
					updatedVoucherDetails.toString()));
			
			return "Cruise vouchers Updated Successfully";
		}
	
	}

	
	
	private String updateClientReconfirmDate(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		CruiseOrders order = cruiseRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmdate failed to update since Order details  not found for Cruise orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmDate(reqJson.getString(JSON_PROP_CLIENTRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

			CruiseOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Cruise client reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Cruise order client reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmDate(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		CruiseOrders order = cruiseRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmDate failed to update since Order details  not found for Cruise orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmDate(reqJson.getString(JSON_PROP_SUPPRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			CruiseOrders updatedSuppReconfirmDateDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Cruise supplier reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Cruise order supplier reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		CruiseOrders order = cruiseRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmStatus failed to update since Order details  not found for Cruise  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmStatus(reqJson.getString("suppReconfirmStatus"));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			CruiseOrders updatedSuppReconfirmDateDetails = saveOrder(order, prevOrder);
			myLogger.info(
					String.format("Cruise supplier reconfirmation status updated Successfully for  orderId  %s = %s",
							orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Cruise order supplier reconfirmation status updated Successfully";
		}
	}

	private String updateClientReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		CruiseOrders order = cruiseRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmStatus failed to update since Order details  not found for Cruise orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmStatus(reqJson.getString(JSON_PROP_CLIENTRECONFIRMSTATUS));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			CruiseOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Cruise client reconfirmation status updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Cruise order client reconfirmation status updated Successfully";
		}
	}


	
	private String updateStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
        CruiseOrders order = cruiseRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String
                    .format("Status  failed to update since Cruise order details   not found for  orderid  %s ", orderID));
            return (response.toString());
        }
        String prevOrder = order.toString();
        order.setStatus(reqJson.getString(JSON_PROP_STATUS));
        order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
        order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
        CruiseOrders updatedStatusObj = saveOrder(order, prevOrder);
        myLogger.info(String.format("Status updated Successfully for Cruise orderID %s = %s", orderID,
                updatedStatusObj.toString()));
        return "Cruise Order status updated Successfully";
	}
	
	public CruiseOrders saveOrder(CruiseOrders order, String prevOrder) throws BookingEngineDBException {
		CruiseOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, CruiseOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Cruise order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save cruise order object");
		}
		return cruiseRepository.saveOrder(orderObj,prevOrder);
	}
}
