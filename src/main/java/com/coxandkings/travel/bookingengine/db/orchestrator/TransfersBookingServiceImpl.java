package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.model.TransfersAmCl;
import com.coxandkings.travel.bookingengine.db.model.TransfersOrders;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.TransfersAmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.TransfersDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly = false)
public class TransfersBookingServiceImpl implements Constants , ErrorConstants{
	
	@Qualifier("Transfers")
	@Autowired
	private TransfersDatabaseRepository transfersRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	@Autowired
	@Qualifier("TransfersAmCl")
	private TransfersAmClRepository transfersAmClRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	JSONObject response = new JSONObject(); 
	
	public JSONArray process(Booking booking, String flag) {
		List<TransfersOrders> transferOrders = transfersRepository.findByBooking(booking);
		JSONArray transfersOrdersJson = getTransferOrdersJson(transferOrders,flag);
		return transfersOrdersJson;
	}
	
	public String getBysuppID(String suppID) {
		
  List<TransfersOrders> orders = transfersRepository.findBysuppID(suppID);
		
		if(orders.size()==0){
			response.put("ErrorCode", "BE_ERR_TRAN_006");
			response.put("ErrorMsg", BE_ERR_TRAN_006);
			myLogger.warn(String.format("Transfers Orders not present for suppID %s", suppID));
			return response.toString();
		}
		else{
			JSONArray ordersArray = getTransferOrdersJson(orders, "false");
			myLogger.info(String.format("Transfers Orders retrieved for suppID %s = %s", suppID, ordersArray.toString()));
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
		TransfersOrders order = transfersRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"Transfers updateOrderAttribute failed to update since Order details  not found for  orderid  %s ",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setBookingAttribute(
					reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("bookingAttribute").toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            TransfersOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Transfers orderAttribute updated Successfully for  orderId  %s = %s",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID),
					updatedclientReconfirmDetails.toString()));
			return "Transfers Booking Attribute Updated Successfully";
		}
	}

	private String updateTimeLimitDate(JSONObject reqJson) throws BookingEngineDBException {

        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        TransfersOrders order = transfersRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("Transfers time limit expiry date failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setTimeLimitExpiryDate(reqJson.getString(JSON_PROP_EXPIRYTIMELIMIT));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            TransfersOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
            myLogger.info(String.format("Transfers time limit expiry date updated Successfully for  orderId  %s = %s", orderID, updatedclientReconfirmDetails.toString()));
            return "Transfers time limit expiry date updated Successfully";
        }
    
	}
	
	private String updateVouchers(JSONObject reqJson) throws BookingEngineDBException {

		TransfersOrders order = transfersRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"updateVouchers failed to update since Transfers Order details  not found for  orderid  %s ",
					reqJson.getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setVouchers(
					reqJson.getJSONArray(JSON_PROP_VOUCHERIDS).toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            TransfersOrders updatedVoucherDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Transfers vouchers updated Successfully for  orderId  %s = %s",
					reqJson.getString(JSON_PROP_ORDERID),
					updatedVoucherDetails.toString()));
			
			return "Transfers vouchers Updated Successfully";
		}
	
	}

	
	
	private String updateClientReconfirmDate(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		TransfersOrders order = transfersRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmdate failed to update since Order details  not found for Transfers orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmDate(reqJson.getString(JSON_PROP_CLIENTRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

			TransfersOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Transfers client reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Transfers order client reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmDate(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		TransfersOrders order = transfersRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmDate failed to update since Order details  not found for Transfers orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmDate(reqJson.getString(JSON_PROP_SUPPRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			TransfersOrders updatedSuppReconfirmDateDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Transfers supplier reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Transfers order supplier reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		TransfersOrders order = transfersRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmStatus failed to update since Order details  not found for Transfers  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmStatus(reqJson.getString("suppReconfirmStatus"));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			TransfersOrders updatedSuppReconfirmDateDetails = saveOrder(order, prevOrder);
			myLogger.info(
					String.format("Transfers supplier reconfirmation status updated Successfully for  orderId  %s = %s",
							orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Transfers order supplier reconfirmation status updated Successfully";
		}
	}

	private String updateClientReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		TransfersOrders order = transfersRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmStatus failed to update since Order details  not found for Transfers orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmStatus(reqJson.getString(JSON_PROP_CLIENTRECONFIRMSTATUS));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			TransfersOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Transfers client reconfirmation status updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Transfers order client reconfirmation status updated Successfully";
		}
	}


	
	private String updateStatus(JSONObject reqJson) throws BookingEngineDBException {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		TransfersOrders order = transfersRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String
                    .format("Status  failed to update since Transfers order details   not found for  orderid  %s ", orderID));
            return (response.toString());
        }
        String prevOrder = order.toString();
        order.setStatus(reqJson.getString(JSON_PROP_STATUS));
        order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
        order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
        TransfersOrders updatedStatusObj = saveOrder(order, prevOrder);
        myLogger.info(String.format("Status updated Successfully for Transfers orderID %s = %s", orderID,
                updatedStatusObj.toString()));
        return "Transfers Order status updated Successfully";
	}

	
	public JSONArray getTransferOrdersJson(List<TransfersOrders> orders, String flag) {
		
		JSONArray transfersArray = new JSONArray();
		JSONObject transfersJson = new JSONObject();
		for (TransfersOrders order : orders) {
			
			if("getOrdersInRange".equalsIgnoreCase(Thread.currentThread().getStackTrace()[2].getMethodName()))
				transfersJson = getTransfersOrdersJson(order,flag,true);
				else
					transfersJson = getTransfersOrdersJson(order,flag,false);	
			transfersArray.put(transfersJson);
		}
		return transfersArray;	
	}
	
	public JSONObject getTransfersOrdersJson(TransfersOrders order, String flag,boolean getBetween) {

		JSONObject TransfersJson = new JSONObject();
		TransfersJson.put(JSON_PROP_CREDENTIALSNAME, "");
		
		
		//TODO: we need to check how will SI send us the details for Enabler Supplier and source supplier
		//TODO: to check what value to sent when it has not reach the cancel/amend stage
		TransfersJson.put(JSON_PROP_CANCELDATE, "");
		TransfersJson.put(JSON_PROP_AMENDDATE, "");
		TransfersJson.put(JSON_PROP_INVENTORY, "N");
		
		TransfersJson.put(JSON_PROP_PRODUCTCATEGORY, "Transportation");
		TransfersJson.put(JSON_PROP_PRODUCTSUBCATEGORY, "Transfers");
		TransfersJson.put(JSON_PROP_ORDERID, order.getId());
		TransfersJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		TransfersJson.put(JSON_PROP_UNIQUEID, order.getUniqueID());
		TransfersJson.put(JSON_PROP_STATUS, order.getStatus());
		TransfersJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());
		TransfersJson.put(JSON_PROP_ROE, order.getRoe());
		
		JSONObject orderDetails = new JSONObject();
		
		if(flag=="false") 
			{	
			orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
			orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS, getClientComms(order));
			orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO,  new JSONObject(order.getSuppFares()));
			   }
		
		
		//TODO: These is set at credential name and supplier level
		orderDetails.put(JSON_PROP_TRANSFERS_TRIPTYPE, order.getTripType());
		orderDetails.put(JSON_PROP_SUPPBOOKREF, order.getSuppBookRefs());
		orderDetails.put(JSON_PROP_TRANSFERS_TRANSFERSDETAILS, new JSONObject(order.getTransfersDetails()));
		//orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getTotalFareJson(order));
		orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO,  new JSONObject(order.getSuppFares()));
		orderDetails.put(JSON_PROP_TRANSFERS_TRIPINDICATOR,order.getTripIndicator());
		orderDetails.put(JSON_PROP_PAXDETAILS, getPassengerInfo(order));
		
		TransfersJson.put(JSON_PROP_ORDERDETAILS, orderDetails);
		return TransfersJson;
	}
	


	private JSONObject getTotalFareJson(TransfersOrders order) {
		JSONObject totalFareJson = new JSONObject();
		
		totalFareJson.put(JSON_PROP_AMOUNT, new BigDecimal(order.getTotalPrice()));
		totalFareJson.put(JSON_PROP_CURRENCYCODE, order.getTotalPriceCurrencyCode());
		//totalFareJson.put(JSON_PROP_BASEFARE, new JSONObject(order.getTotalBaseFare()));
		totalFareJson.put(JSON_PROP_RECEIVABLES, new JSONObject(order.getTotalPriceReceivables()));
		
		return totalFareJson;
	}

	private JSONArray getSuppComms(TransfersOrders order) {
		
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
	
	private JSONArray getClientComms(TransfersOrders order) {
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
	
	private JSONArray getPassengerInfo(TransfersOrders order) {
		
		JSONArray paxJsonArray = new JSONArray();

		for (Object paxId : new JSONArray(order.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject)paxId;
			
			PassengerDetails guest  = passengerRepository.findOne(paxIdJson.getString("paxId"));
			JSONObject paxJson = new JSONObject();
			
			
			paxJson.put(JSON_PROP_TRANS_PASSENGERID, guest.getPassanger_id());
		//	paxJson.put(JSON_PROP_TRANS_ISLEADPAX, guest.getIsLeadPax());
			paxJson.put(JSON_PROP_TRANSFERS_RPH,guest.getRph());
			paxJson.put(JSON_PROP_PAX_TYPE,guest.getPaxType());
			paxJson.put(JSON_PROP_TRANSFERS_AGE,guest.getAge());
			paxJson.put(JSON_PROP_TRANSFERS_QUANTITY,guest.getQuantity());
			paxJson.put(JSON_PROP_TRANSFERS_PHONENUMBER,guest.getContactDetails());
			paxJson.put(JSON_PROP_TRANSFERS_EMAIL,guest.getEmail());
			paxJson.put(JSON_PROP_TRANSFERS_PERSONNAME,new JSONObject(guest.getPersonName()));
			
			paxJsonArray.put(paxJson);
		}
		return paxJsonArray;
		
	}
	
	public JSONArray getCancellationsByBooking(Booking booking) {

		List<TransfersOrders> TransfersOrders = transfersRepository.findByBooking(booking);
		JSONArray transfersOrdersJson = gettransfersOrdersCancellations(TransfersOrders, "cancel");
		return transfersOrdersJson;
	}
	
	private JSONArray gettransfersOrdersCancellations(List<TransfersOrders> transfersOrders,String type ) {
		JSONArray response = new JSONArray();
		for (TransfersOrders order : transfersOrders) {
			String orderId = order.getId();
			JSONObject orderJson = new JSONObject();
			
			
			List<String> ids = new  ArrayList<String>();
			ids.add(orderId);
			JSONArray entityIds =new JSONArray(ids.toString());
				
			List<TransfersAmCl> cancelTransfersOrders = transfersAmClRepository.findByEntity("passenger", entityIds.toString(), "cancel");
			if(cancelTransfersOrders.size()>0) {
				orderJson.put(JSON_PROP_ORDERID, orderId);
				JSONArray orderCancelArray = new JSONArray();
	
				for (TransfersAmCl cancelTransfersOrder : cancelTransfersOrders) {
					JSONObject cancelOrderJson = new JSONObject();
					cancelOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelTransfersOrder.getSupplierCharges());
					cancelOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelTransfersOrder.getCompanyCharges());
					cancelOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE, cancelTransfersOrder.getSupplierChargesCurrencyCode());
					cancelOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE, cancelTransfersOrder.getCompanyChargesCurrencyCode());
					cancelOrderJson.put(JSON_PROP_CANCELTYPE, cancelTransfersOrder.getDescription());
					cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelTransfersOrder.getCreatedAt().toString().substring(0, cancelTransfersOrder.getCreatedAt().toString().indexOf('[')));
					cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelTransfersOrder.getLastModifiedAt().toString().substring(0, cancelTransfersOrder.getLastModifiedAt().toString().indexOf('[')));
					cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelTransfersOrder.getLastModifiedBy());
					orderCancelArray.put(cancelOrderJson);
				}
	
				orderJson.put(JSON_PROP_ORDERCANCELLATIONS, orderCancelArray);
			
				List<TransfersAmCl> cancelPaxOrders = transfersAmClRepository.findByEntity("passenger",  entityIds.toString(), "cancel");
					//	AmClRepository.findByEntity("pax", orderId, type);
				if(cancelPaxOrders.size()>0) {
					JSONArray orderCancelPaxArray = new JSONArray();
					for (TransfersAmCl cancelPaxOrder : cancelPaxOrders) {
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
	
	private TransfersOrders saveOrder(TransfersOrders order, String prevOrder) {
		TransfersOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, TransfersOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return transfersRepository.saveOrder(orderObj, prevOrder);
	}

	
}
