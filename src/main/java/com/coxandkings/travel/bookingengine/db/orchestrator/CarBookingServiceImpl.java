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
import org.springframework.transaction.annotation.Transactional;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CarAmCl;
import com.coxandkings.travel.bookingengine.db.model.CarOrders;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.CarAmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.CarDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly = false)
public class CarBookingServiceImpl implements Constants,ErrorConstants{
	
	@Qualifier("Car")
	@Autowired
	private CarDatabaseRepository carRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	@Autowired
	@Qualifier("CarAmCl")
	private CarAmClRepository carAmClRepository;
	
	JSONObject response = new JSONObject(); 
	
    Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	public JSONArray process(Booking booking, String flag) {
		List<CarOrders> carOrders = carRepository.findByBooking(booking);
		JSONArray carOrdersJson = getCarOrdersJson(carOrders, flag);
		return carOrdersJson;
	}
	
	
	public String getBysuppID(String suppID) {
		
		List<CarOrders> orders = carRepository.findBysuppID(suppID);
		
		if(orders.size()==0){
			response.put("ErrorCode", "BE_ERR_CAR_006");
			response.put("ErrorMsg", BE_ERR_CAR_006);
			myLogger.warn(String.format("Car Orders not present for suppID %s", suppID));
			return response.toString();
		}
		else{
			JSONArray ordersArray = getCarOrdersJson(orders, "false");
			myLogger.info(String.format("Car Orders retrieved for suppID %s = %s", suppID, ordersArray.toString()));
			return ordersArray.toString();
		}
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
		CarOrders order = carRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"Car updateOrderAttribute failed to update since Order details  not found for  orderid  %s ",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setBookingAttribute(
					reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("bookingAttribute").toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            CarOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Car orderAttribute updated Successfully for  orderId  %s = %s",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID),
					updatedclientReconfirmDetails.toString()));
			return "Car Booking Attribute Updated Successfully";
		}
	}

	private String updateTimeLimitDate(JSONObject reqJson) throws BookingEngineDBException {

        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        CarOrders order = carRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("Car time limit expiry date failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setTimeLimitExpiryDate(reqJson.getString(JSON_PROP_EXPIRYTIMELIMIT));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            CarOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
            myLogger.info(String.format("Car time limit expiry date updated Successfully for  orderId  %s = %s", orderID, updatedclientReconfirmDetails.toString()));
            return "Car time limit expiry date updated Successfully";
        }
	}
	
	private String updateVouchers(JSONObject reqJson) throws BookingEngineDBException {

		CarOrders order = carRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"updateVouchers failed to update since Car Order details  not found for  orderid  %s ",
					reqJson.getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setVouchers(
					reqJson.getJSONArray(JSON_PROP_VOUCHERIDS).toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            CarOrders updatedVoucherDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Car vouchers updated Successfully for  orderId  %s = %s",
					reqJson.getString(JSON_PROP_ORDERID),
					updatedVoucherDetails.toString()));
			
			return "Car vouchers Updated Successfully";
		}
	
	}

	
	
	private String updateClientReconfirmDate(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		CarOrders order = carRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmdate failed to update since Order details  not found for Car orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmDate(reqJson.getString(JSON_PROP_CLIENTRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

			CarOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Car client reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Car order client reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmDate(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		CarOrders order = carRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmDate failed to update since Order details  not found for Car orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmDate(reqJson.getString(JSON_PROP_SUPPRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			CarOrders updatedSuppReconfirmDateDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Car supplier reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Car order supplier reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		CarOrders order = carRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmStatus failed to update since Order details  not found for Car  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmStatus(reqJson.getString("suppReconfirmStatus"));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			CarOrders updatedSuppReconfirmDateDetails = saveOrder(order, prevOrder);
			myLogger.info(
					String.format("Car supplier reconfirmation status updated Successfully for  orderId  %s = %s",
							orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Car order supplier reconfirmation status updated Successfully";
		}
	}

	private String updateClientReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		CarOrders order = carRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmStatus failed to update since Order details  not found for Car orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmStatus(reqJson.getString(JSON_PROP_CLIENTRECONFIRMSTATUS));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			CarOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Car client reconfirmation status updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Car order client reconfirmation status updated Successfully";
		}
	}

	private String updateStatus(JSONObject reqJson) throws BookingEngineDBException {
		     String orderID = reqJson.getString(JSON_PROP_ORDERID);
		        CarOrders order = carRepository.findOne(orderID);
		        if (order == null) {
		            response.put("ErrorCode", "BE_ERR_004");
		            response.put("ErrorMsg", BE_ERR_004);
		            myLogger.warn(String
		                    .format("Status  failed to update since Car order details   not found for  orderid  %s ", orderID));
		            return (response.toString());
		        }
		        String prevOrder = order.toString();
		        order.setStatus(reqJson.getString(JSON_PROP_STATUS));
		        order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		        order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		        CarOrders updatedStatusObj = saveOrder(order, prevOrder);
		        myLogger.info(String.format("Status updated Successfully for Car orderID %s = %s", orderID,
		                updatedStatusObj.toString()));
		        return "Car Order status updated Successfully";
		
	}


	public JSONArray getCarOrdersJson(List<CarOrders> orders, String flag) {
		
		JSONArray carArray = new JSONArray();
		JSONObject carJson = new JSONObject();
		for (CarOrders order : orders) {
			if("getOrdersInRange".equalsIgnoreCase(Thread.currentThread().getStackTrace()[2].getMethodName()))
				carJson = getCarOrderJson(order, flag, true);
			else
				carJson = getCarOrderJson(order, flag, false);
			carArray.put(carJson);
		}
		return carArray;	
	}
	
	public JSONObject getCarOrderJson(CarOrders order, String flag,  boolean getBetween) {

		JSONObject carJson = new JSONObject();
		if(getBetween) {
			carJson.put(JSON_PROP_BOOKID, order.getBooking().getBookID());
			carJson.put(JSON_PROP_BOOKINGDATE, order.getBooking().getCreatedAt());
		}
		
		carJson.put(JSON_PROP_CREDENTIALSNAME, "");
		
		
		//TODO: we need to check how will SI send us the details for Enabler Supplier and source supplier
		//TODO: to check what value to sent when it has not reach the cancel/amend stage
		carJson.put(JSON_PROP_CANCELDATE, "");
		carJson.put(JSON_PROP_AMENDDATE, "");
		carJson.put(JSON_PROP_INVENTORY, "N");
		
		carJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
		carJson.put(JSON_PROP_PRODUCTSUBCATEGORY, order.getProductSubCategory());
		carJson.put(JSON_PROP_ORDERID, order.getId());
		carJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		carJson.put(JSON_PROP_STATUS, order.getStatus());
		carJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());
		carJson.put(JSON_PROP_ROE, order.getRoe());
		if(order.getCancelPolicy()!=null)
			carJson.put(JSON_PROP_CANCELLATIONPOLICY, new JSONObject(order.getCancelPolicy()));
		
		JSONObject orderDetails = new JSONObject();
		
		//TODO: These is set at credential name and supplier level
		orderDetails.put(JSON_PROP_CAR_TRIPTYPE, order.getTripType());
		orderDetails.put(JSON_PROP_CAR_RESERVATIONID, order.getCarReservationId());
		String refernces = order.getCarReferences();
		
		orderDetails.put(JSON_PROP_CAR_REFERENCES, new JSONArray(refernces!=null ? refernces : "[]"));
		
		JSONObject carDetails = new JSONObject(order.getRentalDetails());
		carDetails.put(JSON_PROP_CAR_VEHICLEINFO, new JSONObject(order.getCarDetails()));
		
		orderDetails.put(JSON_PROP_CAR_CARDETAILS, carDetails);
		
		if(flag.equals("false")) {
			orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
			orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS, getClientComms(order));
			orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO,  new JSONObject(order.getSuppFares()));
		}
		
		orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getTotalFareJson(order));
		orderDetails.put(JSON_PROP_PAXDETAILS, getPassengerInfo(order));
		
		carJson.put(JSON_PROP_ORDERDETAILS, orderDetails);
		return carJson;
	}
	
	private JSONObject getTotalFareJson(CarOrders order) {
		
		JSONObject totalFareJson = new JSONObject();
		
		totalFareJson.put(JSON_PROP_AMOUNT, new BigDecimal(order.getTotalPrice()));
		totalFareJson.put(JSON_PROP_CURRENCYCODE, order.getTotalPriceCurrencyCode());
		
		totalFareJson.put(JSON_PROP_BASEFARE, new JSONObject(order.getTotalBaseFare()));
		totalFareJson.put(JSON_PROP_FEES, new JSONObject(order.getTotalPriceFees()));
		totalFareJson.put(JSON_PROP_TAXES, new JSONObject(order.getTotalPriceTaxes()));
		totalFareJson.put(JSON_PROP_RECEIVABLES, new JSONObject(order.getTotalPriceReceivables()));
		totalFareJson.put(JSON_PROP_COMPANYTAXES, new JSONObject(order.getTotalPriceCompanyTaxes()));
		totalFareJson.put(JSON_PROP_CAR_SPLEQUIPS, new JSONObject(order.getExtraEquipments()));
		totalFareJson.put(JSON_PROP_CAR_PRICEDCOVERAGES, new JSONObject(order.getPricedCoverages()));

		return totalFareJson;
	}

	private JSONArray getSuppComms(CarOrders order) {
		
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
	
	private JSONArray getClientComms(CarOrders order) {
		
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
	
	private JSONArray getPassengerInfo(CarOrders order) {
		
		JSONArray paxJsonArray = new JSONArray();
		for (Object paxId : new JSONArray(order.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject)paxId;
			
			PassengerDetails customer  = passengerRepository.findOne(paxIdJson.getString("paxId"));
			JSONObject paxJson = new JSONObject();
			paxJson.put(JSON_PROP_CAR_PASSENGERID, customer.getPassanger_id());
			paxJson.put(JSON_PROP_CAR_ISLEADPAX, customer.getIsLeadPax());
			paxJson.put(JSON_PROP_TITLE,customer.getTitle());
			paxJson.put(JSON_PROP_FIRSTNAME, customer.getFirstName());
			paxJson.put(JSON_PROP_MIDDLENAME, customer.getMiddleName());
			paxJson.put(JSON_PROP_LASTNAME, customer.getLastName());
			paxJson.put(JSON_PROP_GENDER, customer.getGender());
			paxJson.put(JSON_PROP_BIRTHDATE, customer.getBirthDate());
			paxJson.put(JSON_PROP_STATUS, customer.getStatus());
			paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONArray(customer.getContactDetails()));
			paxJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(customer.getAddressDetails()));
			
			paxJsonArray.put(paxJson);
		}
		return paxJsonArray;
		
	}
	
	public JSONArray getCancellationsByBooking(Booking booking) {

		List<CarOrders> carOrders = carRepository.findByBooking(booking);
		JSONArray carOrdersJson = getCarOrdersCancellations(carOrders, "cancel");
		return carOrdersJson;

	}
	
	public JSONArray getAmendmentsByBooking(Booking booking) {

		List<CarOrders> carOrders = carRepository.findByBooking(booking);
		JSONArray carOrdersJson = getCarOrdersAmendments(carOrders, "amend");
		return carOrdersJson;
	}
	
	private JSONArray getCarOrdersAmendments(List<CarOrders> carOrders, String type) {
		JSONArray response = new JSONArray();
		for (CarOrders order : carOrders) {
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			orderJson.put(JSON_PROP_ORDERID, orderId);
			List<CarAmCl> amclCarOrders = carAmClRepository.findByEntity("order", orderId, type);
			JSONArray orderAmClArray = new JSONArray();

			for (CarAmCl amclCarOrder : amclCarOrders) {
				JSONObject amclOrderJson = new JSONObject();
				amclOrderJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, amclCarOrder.getSupplierCharges());
				amclOrderJson.put(JSON_PROP_COMPANYAMENDCHARGES, amclCarOrder.getCompanyCharges());
				amclOrderJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE, amclCarOrder.getSupplierChargesCurrencyCode());
				amclOrderJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE, amclCarOrder.getCompanyChargesCurrencyCode());
				amclOrderJson.put(JSON_PROP_AMENDTYPE, amclCarOrder.getDescription());
				amclOrderJson.put(JSON_PROP_CREATEDAT, amclCarOrder.getCreatedAt().toString().substring(0, amclCarOrder.getCreatedAt().toString().indexOf('[')));
				amclOrderJson.put(JSON_PROP_LASTMODIFIEDAT, amclCarOrder.getLastModifiedAt().toString().substring(0, amclCarOrder.getLastModifiedAt().toString().indexOf('[')));
				amclOrderJson.put(JSON_PROP_LASTMODIFIEDBY, amclCarOrder.getLastModifiedBy());
				orderAmClArray.put(amclOrderJson);
			}

			orderJson.put(JSON_PROP_ORDERAMENDS, orderAmClArray);

			JSONArray amClPaxJsonArray = new JSONArray();
			JSONArray paxJsonArray = new JSONArray();
		
			List<CarAmCl> amClPaxOrders = carAmClRepository.findByEntity("pax", orderId, type);
			
			JSONArray entityIds = null;
			for (CarAmCl amClPaxOrder : amClPaxOrders) {
				entityIds = new JSONArray(amClPaxOrder.getEntityID());
				JSONObject amClPaxJson = new JSONObject();
				amClPaxJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, amClPaxOrder.getSupplierCharges());
				amClPaxJson.put(JSON_PROP_COMPANYAMENDCHARGES, amClPaxOrder.getCompanyCharges());
				amClPaxJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE, amClPaxOrder.getSupplierChargesCurrencyCode());
				amClPaxJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE, amClPaxOrder.getCompanyChargesCurrencyCode());
				amClPaxJson.put(JSON_PROP_AMENDTYPE, amClPaxOrder.getDescription());
				amClPaxJson.put(JSON_PROP_CREATEDAT, amClPaxOrder.getCreatedAt().toString().substring(0, amClPaxOrder.getCreatedAt().toString().indexOf('[')));
				amClPaxJson.put(JSON_PROP_LASTMODIFIEDAT, amClPaxOrder.getLastModifiedAt().toString().substring(0, amClPaxOrder.getLastModifiedAt().toString().indexOf('[')));
				amClPaxJson.put(JSON_PROP_LASTMODIFIEDBY, amClPaxOrder.getLastModifiedBy());
				amClPaxJson.put("paxIds", entityIds);
				amClPaxJsonArray.put(amClPaxJson);
			}

			if (amClPaxJsonArray != null && amClPaxJsonArray.length()!=0) {
				JSONObject paxJson = new JSONObject();
				paxJson.put(JSON_PROP_PAXAMENDMENTS, amClPaxJsonArray);
				paxJsonArray.put(paxJson);
			}
			if(orderAmClArray.length()>0) {
				orderJson.put(JSON_PROP_ORDERID, orderId);
				orderJson.put(JSON_PROP_ORDERAMENDS, orderAmClArray);
			}
			if(paxJsonArray.length()>0) {
				orderJson.put(JSON_PROP_ORDERID, orderId);
				orderJson.put("Passengers", paxJsonArray);
			}
	       if(orderJson.length()>0)
		    	  response.put(orderJson);
		}
		return response;
	}

	private JSONArray getCarOrdersCancellations(List<CarOrders> carOrders,String type ) {
		JSONArray response = new JSONArray();
		for (CarOrders order : carOrders) {
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			
			List<CarAmCl> cancelCarOrders = carAmClRepository.findByEntity("order", orderId, type);
			if(cancelCarOrders.size()>0) {
				orderJson.put(JSON_PROP_ORDERID, orderId);
				JSONArray orderCancelArray = new JSONArray();
	
				for (CarAmCl cancelCarOrder : cancelCarOrders) {
					JSONObject cancelOrderJson = new JSONObject();
					cancelOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelCarOrder.getSupplierCharges());
					cancelOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelCarOrder.getCompanyCharges());
					cancelOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE, cancelCarOrder.getSupplierChargesCurrencyCode());
					cancelOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE, cancelCarOrder.getCompanyChargesCurrencyCode());
					cancelOrderJson.put(JSON_PROP_CANCELTYPE, cancelCarOrder.getDescription());
					cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelCarOrder.getCreatedAt().toString().substring(0, cancelCarOrder.getCreatedAt().toString().indexOf('[')));
					cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelCarOrder.getLastModifiedAt().toString().substring(0, cancelCarOrder.getLastModifiedAt().toString().indexOf('[')));
					cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelCarOrder.getLastModifiedBy());
					orderCancelArray.put(cancelOrderJson);
				}
	
				orderJson.put(JSON_PROP_ORDERCANCELLATIONS, orderCancelArray);
			
				List<CarAmCl> cancelPaxOrders = carAmClRepository.findByEntity("pax", orderId, type);
				if(cancelPaxOrders.size()>0) {
					JSONArray orderCancelPaxArray = new JSONArray();
					for (CarAmCl cancelPaxOrder : cancelPaxOrders) {
						JSONObject cancelPaxOrderJson = new JSONObject();
						cancelPaxOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelPaxOrder.getSupplierCharges());
						cancelPaxOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelPaxOrder.getCompanyCharges());
						cancelPaxOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE,
								cancelPaxOrder.getSupplierChargesCurrencyCode());
						cancelPaxOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
								cancelPaxOrder.getCompanyChargesCurrencyCode());
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
	
	private CarOrders saveOrder(CarOrders order, String prevOrder) throws BookingEngineDBException {
		CarOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, CarOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Car order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save car order object");
		}
		return carRepository.saveOrder(orderObj, prevOrder);
	}

	
}
