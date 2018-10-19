package com.coxandkings.travel.bookingengine.db.orchestrator;


import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.BusAmCl;
import com.coxandkings.travel.bookingengine.db.model.BusOrders;

import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;

import com.coxandkings.travel.bookingengine.db.repository.BusAmclRepository;
import com.coxandkings.travel.bookingengine.db.repository.BusDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly = false)
public class BusBookingServiceImpl implements Constants,CancelAmendTypes,ErrorConstants {

	@Qualifier("Bus")
	@Autowired
	private BusDatabaseRepository busRepository;
	
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	@Autowired
	@Qualifier("BusAmCl")
	private BusAmclRepository busAmClRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	JSONObject response = new JSONObject();
	
	public JSONArray process(Booking booking, String flag) {
		List<BusOrders> busOrders = busRepository.findByBooking(booking);
		JSONArray busOrdersJson = getBusOrdersJson(busOrders,flag);
		return busOrdersJson;
	}

	public JSONArray getBusOrdersJson(List<BusOrders> orders, String flag) {
		
		JSONArray busArray = new JSONArray();
		JSONObject busJson = new JSONObject();
		for (BusOrders order : orders) {
			
			
			if("getOrdersInRange".equalsIgnoreCase(Thread.currentThread().getStackTrace()[2].getMethodName()))
				busJson = getBusOrderJson(order,flag,true);
				else
					busJson = getBusOrderJson(order,flag,false);	
			busArray.put(busJson);
		}
		return busArray;
	
	}

	public JSONArray getCancellationsByBooking(Booking booking) {

		List<BusOrders> busOrders = busRepository.findByBooking(booking);
		JSONArray busOrdersJson = getBusOrdersCancellations(busOrders, "cancel");
		
		return busOrdersJson;

	}
	public JSONObject getBusOrderJson(BusOrders order, String flag,boolean getBetween) {

		JSONObject busJson = new JSONObject();
		
		if(getBetween) {
			busJson.put(JSON_PROP_BOOKID, order.getBooking().getBookID());
			busJson.put(JSON_PROP_BOOKINGDATE, order.getBooking().getCreatedAt());
		}
		//TODO: to check from where will we get these details from WEM
		busJson.put(JSON_PROP_CREDENTIALSNAME, "");
		
		
		//TODO: added these fields on the suggestions of operations
		busJson.put(JSON_PROP_SUPPLIERRECONFIRMATIONSTATUS, order.getSuppReconfirmStatus());
		busJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());
		
		//TODO: we need to check how will SI send us the details for Enabler Supplier and source supplier
		busJson.put(JSON_PROP_ENABLERSUPPLIERNAME, order.getSupplierID());
		busJson.put(JSON_PROP_SOURCESUPPLIERNAME, order.getSupplierID());

		
		//TODO: to check what value to sent when it has not reach the cancel/amend stage
		busJson.put(JSON_PROP_CANCELDATE, "");
		busJson.put(JSON_PROP_AMENDDATE, "");
		busJson.put(JSON_PROP_INVENTORY, "N");


		busJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
		busJson.put(JSON_PROP_PRODUCTSUBCATEGORY, JSON_PROP_BUS_PRODUCTSUBCATEGORY);
		busJson.put(JSON_PROP_ORDERID, order.getId());
		busJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		busJson.put(JSON_PROP_STATUS, order.getStatus());
		busJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());
		busJson.put(JSON_PROP_ROE, order.getRoe());
		
		JSONObject orderDetails = new JSONObject();
//		orderDetails.put(JSON_PROP_SUPPCOMM, getSuppComms(order));
//		orderDetails.put(JSON_PROP_CLIENTCOMM,getClientComms(order));
		if(flag=="false")
		{	
		orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
		orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS,getClientComms(order));
		orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getSuppPriceInfoJson(order));
	    }
		orderDetails.put(JSON_PROP_BUS_BUSDETAILS, new JSONObject(order.getBusDetails()));
		orderDetails.put(JSON_PROP_PAXINFO, getPassInfoJson(order));
		orderDetails.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson(order));
		orderDetails.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson(order));
		orderDetails.put("ticketNo", order.getTicketNo());
		orderDetails.put("PNRNo", order.getBusPNR());
		busJson.put(JSON_PROP_ORDERDETAILS, orderDetails);
		return busJson;
	}

	private BusOrders saveOrder(BusOrders order,String prevOrder) {
		BusOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, BusOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return busRepository.saveOrder(orderObj,prevOrder);
	}
	
//	private BusPassengerDetails savePaxDetails(BusPassengerDetails pax) {
//		BusPassengerDetails orderObj = null;
//		try {
//			orderObj = CopyUtils.copy(pax, BusPassengerDetails.class);
//
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//
//			e.printStackTrace();
//		}
//		return busPassReposiory.saveOrder(orderObj);
//	}
	
	
	
	private JSONObject getTotalPriceInfoJson(BusOrders order) {

		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put("TotalFare", new BigDecimal(order.getTotalPrice()));
		totalPriceJson.put("TotalFareCurrency", order.getTotalPriceCurrencyCode());
		totalPriceJson.put("paxTypeFares", order.getTotalPaxTypeFares());
		return totalPriceJson;
	}

	private JSONObject getSuppPriceInfoJson(BusOrders order) {
		JSONObject suppPriceJson = new JSONObject();

//		suppPriceJson.put(JSON_PROP_SUPPPRICE, order.getSupplierTotalPrice());
		suppPriceJson.put(JSON_PROP_SUPPPRICE, new BigDecimal(order.getSupplierTotalPrice()));
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, order.getSupplierPriceCurrencyCode());
		suppPriceJson.put("paxTypeFares", order.getSuppPaxTypeFares());
		return suppPriceJson;
	}

	private JSONArray getPassInfoJson(BusOrders order) {

//		JSONArray passJsonArray = new JSONArray();
//		JSONObject passJson =new JSONObject();
//		
//		for(BusPassengerDetails guest: order.getPassengerDetails()) {
//			passJson.put(JSON_PROP_TITLE,guest.getTitle());
//			passJson.put("Name",guest.getName());
//			passJson.put("IdNumber",guest.getIdNumber());
//			passJson.put("IdType",guest.getIdType());
//			passJson.put("Age",guest.getAge());
//			passJson.put("Gender",guest.getGender());
//			passJson.put("SeatNo",guest.getSeatNo());
//			passJson.put("seatTypesList",guest.getSeatTypesList());
//			passJson.put("seatTypeIds",guest.getSeatTypeIds());
//
//			passJsonArray.put(passJson);
//		}
//		return passJsonArray;
		
		
		JSONArray paxJsonArray = new JSONArray();
		
		for (Object paxId : new JSONArray(order.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject)paxId;
			
			PassengerDetails guest  = passengerRepository.findOne(paxIdJson.getString(JSON_PROP_PAXID));
			JSONObject paxJson = new JSONObject();
			paxJson.put(JSON_PROP_AIR_PASSENGERID,guest.getPassanger_id());
			paxJson.put(JSON_PROP_FIRSTNAME, guest.getFirstName());
			paxJson.put(JSON_PROP_MIDDLENAME, guest.getMiddleName());
			paxJson.put(JSON_PROP_LASTNAME, guest.getLastName());
			paxJson.put(JSON_PROP_BIRTHDATE, guest.getBirthDate());
			paxJson.put(JSON_PROP_STATUS, guest.getStatus());
			paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONArray(guest.getContactDetails()));
			paxJson.put("status", guest.getStatus());
			paxJson.put(JSON_PROP_BUS_SEATNO, paxIdJson.get(JSON_PROP_BUS_SEATNO));
			paxJsonArray.put(paxJson);
		}
		return paxJsonArray;

	}

	private JSONArray getClientComms(BusOrders order) {

//		JSONObject clientCommJson = new JSONObject();
		JSONArray clientCommArray = new JSONArray();
		
		for(ClientCommercial clientComm: order.getClientCommercial()) {
			
			JSONObject clientCommJson = new JSONObject();
//			clientCommJson.put(JSON_PROP_COMMERCIALNAME, clientComm.getCommercialName());
//			clientCommJson.put(JSON_PROP_COMMERCIALTYPE, clientComm.getCommercialType());
//			clientCommJson.put(JSON_PROP_COMMERCIALCURRENCY, clientComm.getCommercialCurrency());
//			clientCommJson.put(JSON_PROP_COMMERCIALTOTALAMOUNT, new BigDecimal(clientComm.getCommercialAmount()));
			
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

	private JSONArray getSuppComms(BusOrders order) {
		
//		JSONObject suppCommJson = new JSONObject();
		JSONArray suppCommArray = new JSONArray();
		for(SupplierCommercial suppComm: order.getSuppcommercial()) {
			
			JSONObject suppCommJson = new JSONObject();
			suppCommJson.put(JSON_PROP_COMMERCIALNAME, suppComm.getCommercialName());
			suppCommJson.put(JSON_PROP_COMMERCIALTYPE, suppComm.getCommercialType());
//			suppCommJson.put(JSON_PROP_BEFORECOMMERCIALAMOUNT, suppComm.getBeforeCommercialAmount());
//			suppCommJson.put("beofreCommercialAmount", suppComm.getBeforeCommercialAmount());
//			suppCommJson.put(JSON_PROP_COMMERCIALCALCULATIONPERCENTAGE, suppComm.getCommercialCalculationPercentage());
//			suppCommJson.put(JSON_PROP_COMMERCIALCALCULATIONAMOUNT, suppComm.getCommercialName());
//			suppCommJson.put(JSON_PROP_COMMERCIALFARECOMPONENT, suppComm.getCommercialFareComponent());
			suppCommJson.put(JSON_PROP_COMMERCIALCURRENCY, suppComm.getCommercialCurrency());
			suppCommJson.put(JSON_PROP_COMMERCIALTOTALAMOUNT, new BigDecimal(suppComm.getCommercialAmount()));
//			suppCommJson.put(JSON_PROP_AFTERCOMMERCIALTOTALAMOUNT, suppComm.getAfterCommercialAmount());
//			suppCommJson.put(JSON_PROP_AFTERCOMMERCIALBASEFARE, suppComm.getAfterCommercialBaseFare());
//			suppCommJson.put(JSON_PROP_AFTERCOMMERCIALTAXDETAILS, suppComm.getAfterCommercialTaxDetails());
//			suppCommJson.put(JSON_PROP_RECIEPTNUMBER, suppComm.getRecieptNumber());
//			suppCommJson.put(JSON_PROP_INVOICENUMBER, suppComm.getInVoiceNumber());
			
			suppCommArray.put(suppCommJson);
		}		
		return suppCommArray;
	}

	
	private JSONArray getBusOrdersCancellations(List<BusOrders> busOrders,String type )
	{
		JSONArray response = new JSONArray();
		for (BusOrders order : busOrders)
		{
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			
			
			 List<String> eids = new  ArrayList<String>();
			 eids.add(orderId);
			JSONArray entityIds =new JSONArray(eids.toString());
			List<BusAmCl> cancelBusOrders = busAmClRepository.findByEntity(JSON_PROP_BUS_CANCELTYPE_FULLCANCEL, entityIds.toString(), "cancel");
			
			if(cancelBusOrders.size()>0) {
				orderJson.put(JSON_PROP_ORDERID, orderId);
				JSONArray orderCancelArray = new JSONArray();

				for (BusAmCl cancelBusOrder : cancelBusOrders) {
					JSONObject cancelOrderJson = new JSONObject();
					cancelOrderJson.put("supplierCancelCharges", cancelBusOrder.getSupplierCharges());
					cancelOrderJson.put("companyCancelCharges", cancelBusOrder.getCompanyCharges());
					cancelOrderJson.put("refundAmount", cancelBusOrder.getRefundAmount());
					cancelOrderJson.put("currency", cancelBusOrder.getRefundAmountCurrency());
					cancelOrderJson.put("supplierCancelChargesCurrencyCode",
							cancelBusOrder.getSupplierChargesCurrencyCode());
					cancelOrderJson.put("companyCancelChargesCurrencyCode",
							cancelBusOrder.getCompanyChargesCurrencyCode());
					cancelOrderJson.put("cancelType", cancelBusOrder.getDescription());
					cancelOrderJson.put("createdAt", cancelBusOrder.getCreatedAt());
					cancelOrderJson.put("lastModifiedAt", cancelBusOrder.getLastModifiedAt());
					cancelOrderJson.put("lastModifiedBy", cancelBusOrder.getLastModifiedBy());
					orderCancelArray.put(cancelOrderJson);
				}

				orderJson.put("orderCancellations", orderCancelArray);
			}
			
				
				eids = new  ArrayList<String>();
				JSONArray paxArr = new JSONArray(order.getPaxDetails());
				for(int i=0;i<paxArr.length();i++)
				{
					JSONObject paxJson =  paxArr.getJSONObject(i);
					PassengerDetails pax = passengerRepository.findOne(paxJson.getString("paxID"));
					if(pax == null)
					{
						myLogger.info(String.format("Bus getCancellation request failed since paxID not found for req "));
						eids.add("");
					}
					else
					{
						if(pax.getStatus().equals(OrderStatus.XL.getProductStatus()))
						{
							eids.add(pax.getPassanger_id().toString());
						}
					}
				}
				entityIds =new JSONArray(eids.toString());
				
				String abc = entityIds.toString().replace("\"", "");
				List<BusAmCl> cancelPaxOrders = busAmClRepository.findByEntity(JSON_PROP_BUS_CANCELTYPE_CANCELPAX, entityIds.toString(), "cancel");
				
				if(cancelPaxOrders.size()>0) {
					JSONArray orderCancelPaxArray = new JSONArray();
					for (BusAmCl cancelPaxOrder : cancelPaxOrders) {
						JSONObject cancelPaxOrderJson = new JSONObject();
						cancelPaxOrderJson.put("supplierCancelCharges", cancelPaxOrder.getSupplierCharges());
						cancelPaxOrderJson.put("companyCancelCharges", cancelPaxOrder.getCompanyCharges());
						cancelPaxOrderJson.put("refundAmount", cancelPaxOrder.getRefundAmount());
						cancelPaxOrderJson.put("currency", cancelPaxOrder.getRefundAmountCurrency());
						cancelPaxOrderJson.put("supplierCancelChargesCurrencyCode",
								cancelPaxOrder.getSupplierChargesCurrencyCode());
						cancelPaxOrderJson.put("companyCancelChargesCurrencyCode",
								cancelPaxOrder.getCompanyChargesCurrencyCode());
						cancelPaxOrderJson.put("cancelType", cancelPaxOrder.getDescription());
						cancelPaxOrderJson.put("createdAt", cancelPaxOrder.getCreatedAt());
						cancelPaxOrderJson.put("lastModifiedAt", cancelPaxOrder.getLastModifiedAt());
						cancelPaxOrderJson.put("lastModifiedBy", cancelPaxOrder.getLastModifiedBy());
						String entityIDs = cancelPaxOrder.getEntityID();
						
						cancelPaxOrderJson.put("paxIDs", new JSONArray(entityIDs));
						orderCancelPaxArray.put(cancelPaxOrderJson);
						
					}
						
					orderJson.put("paxCancellations", orderCancelPaxArray);
					}
				response.put(orderJson);
		
		
		
	}
		return response;

	}
	
	public JSONArray getOrdersInRange(ZonedDateTime startdateTime, ZonedDateTime enddateTime, String suppRef) {
		  List<BusOrders> temp= busRepository.getOrdersInRange(startdateTime,enddateTime,suppRef);
		  JSONArray busOrders = getBusOrdersJson(temp,"false");
		  return busOrders;
	}
	
	public JSONArray getPoliciesByBooking(Booking booking) {

		List<BusOrders> busOrders = busRepository.findByBooking(booking);
		JSONArray policies = getPolicies(busOrders);
		return policies;

	}
	
	public JSONArray getPolicies(List<BusOrders> busOrders) {
		JSONArray policiesArr = new JSONArray();
		
		for(BusOrders order:busOrders)
		{
			JSONObject policyJson = new JSONObject();
		policyJson.put(JSON_PROP_POLICIES,new JSONArray(order.getCancellationPolicy()));
		policyJson.put(JSON_PROP_BUS_SERVICEID, new JSONObject(order.getBusDetails()).get(JSON_PROP_BUS_SERVICEID));
		policiesArr.put(policyJson);
		}
		return policiesArr;
	}

	public JSONArray getDocumentDetails(JSONObject reqJson) {

		JSONArray docDtlsJsonArr = new JSONArray();
		BusOrders order = busRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		JSONArray paxDetailsArr= new JSONArray(order.getPaxDetails());
		for(int i=0;i<paxDetailsArr.length();i++)
		{
			JSONObject documentJson=new JSONObject();
			JSONObject paxJson = paxDetailsArr.getJSONObject(i);
			PassengerDetails currPax = passengerRepository.findOne(paxJson.getString(JSON_PROP_PAXID));
			if(currPax.getDocumentDetails()!=null)
				documentJson.put(JSON_PROP_DOCUMENTDETAILS, new JSONObject(currPax.getDocumentDetails()));
			docDtlsJsonArr.put(documentJson);
		}
		return docDtlsJsonArr;
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
		BusOrders order = busRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"Bus updateOrderAttribute failed to update since Order details  not found for  orderid  %s ",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setBookingAttribute(
					reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("bookingAttribute").toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            BusOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Bus orderAttribute updated Successfully for  orderId  %s = %s",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID),
					updatedclientReconfirmDetails.toString()));
			return "Bus Booking Attribute Updated Successfully";
		}
	}

	private String updateTimeLimitDate(JSONObject reqJson) throws BookingEngineDBException {

        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        BusOrders order = busRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("Bus time limit expiry date failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setTimeLimitExpiryDate(reqJson.getString(JSON_PROP_EXPIRYTIMELIMIT));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            BusOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
            myLogger.info(String.format("Bus time limit expiry date updated Successfully for  orderId  %s = %s", orderID, updatedclientReconfirmDetails.toString()));
            return "Bus time limit expiry date updated Successfully";
        }
    
	}
	
	private String updateVouchers(JSONObject reqJson) throws BookingEngineDBException {

		BusOrders order = busRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"updateVouchers failed to update since Bus Order details  not found for  orderid  %s ",
					reqJson.getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setVouchers(
					reqJson.getJSONArray(JSON_PROP_VOUCHERIDS).toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            BusOrders updatedVoucherDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Bus vouchers updated Successfully for  orderId  %s = %s",
					reqJson.getString(JSON_PROP_ORDERID),
					updatedVoucherDetails.toString()));
			
			return "Bus vouchers Updated Successfully";
		}
	
	}

	
	
	private String updateClientReconfirmDate(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		BusOrders order = busRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmdate failed to update since Order details  not found for Bus orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmDate(reqJson.getString(JSON_PROP_CLIENTRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

			BusOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Bus client reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Bus order client reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmDate(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		BusOrders order = busRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmDate failed to update since Order details  not found for Bus orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmDate(reqJson.getString(JSON_PROP_SUPPRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			BusOrders updatedSuppReconfirmDateDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Bus supplier reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Bus order supplier reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmStatus(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		BusOrders order = busRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmStatus failed to update since Order details  not found for Bus  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmStatus(reqJson.getString("suppReconfirmStatus"));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			BusOrders updatedSuppReconfirmDateDetails = saveOrder(order, prevOrder);
			myLogger.info(
					String.format("Bus supplier reconfirmation status updated Successfully for  orderId  %s = %s",
							orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Bus order supplier reconfirmation status updated Successfully";
		}
	}

	private String updateClientReconfirmStatus(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		BusOrders order = busRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmStatus failed to update since Order details  not found for Bus orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmStatus(reqJson.getString(JSON_PROP_CLIENTRECONFIRMSTATUS));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			BusOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Bus client reconfirmation status updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Bus order client reconfirmation status updated Successfully";
		}
	}

	private String updateStatus(JSONObject reqJson) {
		     String orderID = reqJson.getString(JSON_PROP_ORDERID);
		     BusOrders order = busRepository.findOne(orderID);
		        if (order == null) {
		            response.put("ErrorCode", "BE_ERR_004");
		            response.put("ErrorMsg", BE_ERR_004);
		            myLogger.warn(String
		                    .format("Status  failed to update since Bus order details  not found for  orderid  %s ", orderID));
		            return (response.toString());
		        }
		        String prevOrder = order.toString();
		        order.setStatus(reqJson.getString(JSON_PROP_STATUS));
		        order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		        order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
		        BusOrders updatedStatusObj = saveOrder(order, prevOrder);
		        myLogger.info(String.format("Status updated Successfully for Bus orderID %s = %s", orderID,
		                updatedStatusObj.toString()));
		        return "Bus Order status updated Successfully";
		
	}

}
