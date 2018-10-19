package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.AccoRoomDetails;
import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;

import com.coxandkings.travel.bookingengine.db.repository.AirDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.impl.AirGetByClass;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly = false)
public class AirBookingServiceImpl implements Constants, ErrorConstants {

    @Qualifier("Air")
    @Autowired
    private AirDatabaseRepository airRepository;

    @Autowired
    @Qualifier("Passenger")
    private PassengerRepository passengerRepository;

    @Autowired
    @Qualifier("updatestat")
    private AirGetByClass getByClass;

    @Autowired
    @Qualifier("AccoAmCl")
    private AmClRepository accoAmClRepository;

    Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());

    JSONObject response = new JSONObject();

    public JSONArray process(Booking booking, String flag) {
        List<AirOrders> airOrders = airRepository.findByBooking(booking);
        JSONArray airOrdersJson = getAirOrdersJson(airOrders, flag);
        return airOrdersJson;
    }

    public String getBysuppID(String suppID) {

        List<AirOrders> orders = airRepository.findBysuppID(suppID);
        if (orders.size() == 0) {
            response.put("ErrorCode", "BE_ERR_AIR_006");
            response.put("ErrorMsg", BE_ERR_AIR_006);
            myLogger.warn(String.format("Air Orders not present  for suppID %s", suppID));
            return response.toString();
        } else {
            JSONArray ordersArray = getAirOrdersJson(orders, "false");
            myLogger.info(String.format("Air Orders retrieved for suppID %s = %s", suppID, ordersArray.toString()));
            return ordersArray.toString();
        }
    }

    public JSONArray getAirOrdersJson(List<AirOrders> orders, String flag) {

        JSONArray airArray = new JSONArray();
        JSONObject airJson = new JSONObject();
        for (AirOrders order : orders) {
            if ("getOrdersInRange".equalsIgnoreCase(Thread.currentThread().getStackTrace()[2].getMethodName()))
                airJson = getAirOrderJson(order, flag, true);
            else
                airJson = getAirOrderJson(order, flag, false);
            airArray.put(airJson);
        }
        return airArray;
    }

    public JSONObject getAirOrderJson(AirOrders order, String flag, boolean getBetween) {

        JSONObject airJson = new JSONObject();

        if (getBetween) {
            airJson.put(JSON_PROP_BOOKID, order.getBooking().getBookID());
            airJson.put(JSON_PROP_BOOKINGDATE, order.getBooking().getCreatedAt());
        }
        // TODO: to check from where will we get these details from WEM/BE
        airJson.put(JSON_PROP_CREDENTIALSNAME, order.getCredentialsName());

        // TODO: added these fields on the suggestions of operations and finance
        airJson.put(JSON_PROP_SUPPLIERRECONFIRMATIONSTATUS, order.getSuppReconfirmStatus());
        airJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());
        airJson.put(JSON_PROP_SUPPRECONFIRMDATE, order.getSuppReconfirmDate());
        airJson.put(JSON_PROP_CLIENTRECONFIRMDATE, order.getClientReconfirmDate());
        airJson.put(JSON_PROP_AIR_TICKETNUMBER, order.getTicketNumber() != null ? order.getTicketNumber() : "");
        airJson.put(JSON_PROP_AIR_TICKETISSUEDATE, order.getTicketIssueDate());
        airJson.put("suppTransactionId", order.getSuppTransactionId());

        // TODO: we need to check how will SI send us the details for Enabler Supplier
        // and source supplier
        airJson.put(JSON_PROP_ENABLERSUPPLIERNAME, order.getSupplierID());
        airJson.put(JSON_PROP_SOURCESUPPLIERNAME, order.getSupplierID());

        // TODO: Need to confirm whether we really need these fields in here as we have
        // seperate getCancellation/getAmenddments methods
        airJson.put(JSON_PROP_CANCELDATE, order.getCancelDate() != null ? order.getCancelDate() : "");
        airJson.put(JSON_PROP_AMENDDATE, order.getAmendDate() != null ? order.getAmendDate() : "");

        // TODO: check logic for having inventory.once integrated with Viimal's system,
        // put that logic here.
        airJson.put(JSON_PROP_INVENTORY, "N");

        String createdAt = order.getCreatedAt().toString().substring(0, order.getCreatedAt().toString().indexOf('['));

        airJson.put(JSON_PROP_CREATEDAT, createdAt);

        airJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
        airJson.put(JSON_PROP_PRODUCTSUBCATEGORY, JSON_PROP_AIR_PRODUCTSUBCATEGORY);
        airJson.put(JSON_PROP_ORDERID, order.getId());
        airJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
        airJson.put(JSON_PROP_SUPPTYPE, order.getSupplierType());
        airJson.put(JSON_PROP_STATUS, order.getStatus());
        airJson.put(JSON_PROP_LASTMODIFIEDBY, order.getLastModifiedBy());
        airJson.put(JSON_PROP_ROE, order.getRoe());

        JSONObject orderDetails = new JSONObject();

        orderDetails.put(JSON_PROP_AIR_GDSPNR, order.getGDSPNR());
        orderDetails.put(JSON_PROP_AIR_AIRLINEPNR, order.getAirlinePNR());
        orderDetails.put(JSON_PROP_AIR_TICKETPNR, order.getTicketingPNR());

        // TODO: These is set at credential name and supplier level
        orderDetails.put(JSON_PROP_TICKETINGPCC, order.getTicketingPCC());
        orderDetails.put(JSON_PROP_BOOKINGPCC, order.getBookingPCC());
        
        //orderDetails.put(JSON_PROP_BOOKINGTYPE, order.getBookingType());
        orderDetails.put(JSON_PROP_AIR_TRIPTYPE, order.getTripType());
        orderDetails.put(JSON_PROP_AIR_TRIPINDICATOR, order.getTripIndicator());
        orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS, getClientComms(order));
        if (flag == "false") {
            orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
            orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getSuppPriceInfoJson(order));
        }
        orderDetails.put(JSON_PROP_AIR_FLIGHTDETAILS, new JSONObject(order.getFlightDetails()));
        orderDetails.put(JSON_PROP_PAXINFO, getPaxInfoJson(order));

        orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getTotalPriceInfoJson(order, flag));

        airJson.put(JSON_PROP_ORDERDETAILS, orderDetails);


        airJson.put(JSON_PROP_BOOKINGATTRIBUTE,order.getBookingAttribute() != null ?
                new JSONArray(order.getBookingAttribute()):null);

        airJson.put(JSON_PROP_VOUCHERS, order.getVouchers() != null ? new JSONArray(order.getVouchers()) : null);

        airJson.put(JSON_PROP_EXPIRYTIMELIMIT, order.getTimeLimitExpiryDate());

        return airJson;
    }

    private JSONArray getSuppComms(AirOrders order) {

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

    private JSONArray getClientComms(AirOrders order) {

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

    private JSONArray getPaxInfoJson(AirOrders order) {

        JSONArray paxJsonArray = new JSONArray();
        for (Object paxId : new JSONArray(order.getPaxDetails())) {
            JSONObject paxIdJson = (JSONObject) paxId;

            PassengerDetails guest = passengerRepository.findOne(paxIdJson.getString(JSON_PROP_PAXID));
            JSONObject paxJson = new JSONObject();
            paxJson.put(JSON_PROP_AIR_PASSENGERID, guest.getPassanger_id());
            paxJson.put(JSON_PROP_PAX_TYPE, guest.getPaxType());
            paxJson.put(JSON_PROP_TITLE, guest.getTitle());
            if(guest.getSeatMap()!=null)
            paxJson.put(JSON_PROP_SEATMAP, new JSONArray(guest.getSeatMap()));
            paxJson.put(JSON_PROP_ISLEADPAX, guest.getIsLeadPax());
            paxJson.put(JSON_PROP_FIRSTNAME, guest.getFirstName());
            paxJson.put(JSON_PROP_MIDDLENAME, guest.getMiddleName());
            paxJson.put(JSON_PROP_LASTNAME, guest.getLastName());
            paxJson.put(JSON_PROP_BIRTHDATE, guest.getBirthDate());
            paxJson.put(JSON_PROP_STATUS, guest.getStatus());
            paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONArray(guest.getContactDetails()));
            if (Pax_ADT.equals(guest.getPaxType()))
                paxJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(guest.getAddressDetails()));
            if (guest.getSpecialRequests() != null)
            paxJson.put(JSON_PROP_SPECIALREQUESTS, new JSONObject(guest.getSpecialRequests()));
            
            if (guest.getAncillaryServices() != null)
				paxJson.put(JSON_PROP_ANCILLARYSERVICES, new JSONObject(guest.getAncillaryServices()));
			if(guest.getDocumentDetails()!=null)
				paxJson.put(JSON_PROP_DOCUMENTDETAILS, new JSONObject(guest.getDocumentDetails()));

            paxJsonArray.put(paxJson);
        }

        return paxJsonArray;
    }

    private JSONObject getSuppPriceInfoJson(AirOrders order) {

        JSONObject suppPriceJson = new JSONObject();

        suppPriceJson.put(JSON_PROP_SUPPPRICE, order.getSupplierPrice());
        suppPriceJson.put(JSON_PROP_CURRENCYCODE, order.getSupplierPriceCurrencyCode());
        suppPriceJson.put(JSON_PROP_AIR_PAXTYPEFARES, new JSONArray(order.getSuppPaxTypeFares()));
        // TODO: to confirm if we will get tax details in air book req?
        /*
         * suppPriceJson.put("taxAmount", order.getSupplierTaxAmount());
         * suppPriceJson.put("taxBreakup", new
         * JSONArray(order.getSupplierTaxBreakup()));
         */

        return suppPriceJson;
    }

    private JSONObject getTotalPriceInfoJson(AirOrders order, String flag) {

        JSONObject totalPriceJson = new JSONObject();

        totalPriceJson.put(JSON_PROP_TOTALPRICE, order.getTotalPrice());
        totalPriceJson.put(JSON_PROP_CURRENCYCODE, order.getTotalPriceCurrencyCode());
        if (flag == "false") {
            totalPriceJson.put(JSON_PROP_AIR_PAXTYPEFARES, new JSONArray(order.getTotalPaxTypeFares()));
        } else {
            JSONArray tempPaxTypeFares = new JSONArray(order.getTotalPaxTypeFares());
            for (int i = 0; i < tempPaxTypeFares.length(); i++) {
                JSONObject currPaxTypeFares = tempPaxTypeFares.getJSONObject(i);
                currPaxTypeFares.remove(JSON_PROP_TRANSFERS_CLIENTENTITYCOMMS);
            }
            totalPriceJson.put(JSON_PROP_AIR_PAXTYPEFARES, tempPaxTypeFares);
        }
        totalPriceJson.put(JSON_PROP_BASEFARE, new JSONObject(order.getTotalPriceBaseFare()));
        if (order.getTotalPriceReceivables() != null)
            totalPriceJson.put(JSON_PROP_RECEIVABLES, new JSONObject(order.getTotalPriceReceivables()));
        if (order.getTotalPriceFees() != null)
            totalPriceJson.put(JSON_PROP_FEES, new JSONObject(order.getTotalPriceFees()));
        totalPriceJson.put(JSON_PROP_TAXES, new JSONObject(order.getTotalPriceTaxes()));
        if(order.getCompanyTaxes()!=null)
        	totalPriceJson.put(JSON_PROP_COMPANYTAXES, order.getCompanyTaxes());
        if(order.getIncentives()!=null)
        	totalPriceJson.put(JSON_PROP_INCENTIVES, order.getIncentives());
        if(order.getDiscounts()!=null)
            totalPriceJson.put(JSON_PROP_DISCOUNTS,order.getDiscounts());
        return totalPriceJson;
    }

    private AirOrders saveOrder(AirOrders order, String prevOrder) throws BookingEngineDBException {
        AirOrders orderObj = null;
        try {
            orderObj = CopyUtils.copy(order, AirOrders.class);

        } catch (InvocationTargetException | IllegalAccessException e) {
            myLogger.fatal("Error while saving Air order object : " + e);
            throw new BookingEngineDBException("Failed to save air order object");
        }
        return airRepository.saveOrder(orderObj, prevOrder);
    }

    private PassengerDetails savePaxDetails(PassengerDetails pax, String prevPaxDetails)
            throws BookingEngineDBException {
        PassengerDetails orderObj = null;
        try {
            orderObj = CopyUtils.copy(pax, PassengerDetails.class);

        } catch (InvocationTargetException | IllegalAccessException e) {
            myLogger.fatal("Error while saving Air passenger object : " + e);
            throw new BookingEngineDBException("Failed to save Air passenger object");
        }
        return passengerRepository.saveOrder(orderObj, prevPaxDetails);
    }

    public String updateOrder(JSONObject reqJson, String updateType) throws BookingEngineDBException {

        switch (updateType) {
            case JSON_PROP_AIR_FLIGHTDETAILS:
                return updateFlightDetails(reqJson);
            case JSON_PROP_PRICES:
                return updatePriceDetails(reqJson);
            case JSON_PROP_STATUS:
                return updateStatus(reqJson);
            case JSON_PROP_SPECIALREQUESTS:
                return updateSpecialRequest(reqJson);
            case JSON_PROP_CLIENTRECONFIRMSTATUS:
                return updateClientReconfirmStatus(reqJson);
            case JSON_PROP_SUPPRECONFIRMSTATUS:
                return updateSuppReconfirmStatus(reqJson);
            case JSON_PROP_DOB:
                return updateBirthDate(reqJson);
            case JSON_PROP_TICKETINGPCC:
                return updateTicketingPCC(reqJson);
            case JSON_PROP_ORDERATTRIBUTE:
                return updateOrderAttribute(reqJson);
            case JSON_PROP_SUPPRECONFIRMDATE:
                return updateSuppReconfirmDate(reqJson);
            case JSON_PROP_CLIENTRECONFIRMDATE:
                return updateClientReconfirmDate(reqJson);
            case JSON_PROP_PAXDETAILS:
                return updatePaxDetails(reqJson);
            case JSON_PROP_PAXDOCUMENT:
                return updatePaxDocument(reqJson);
            case JSON_PROP_VOUCHERS:
    			return updateVouchers(reqJson);
            case JSON_PROP_PNR:
            	 return updatePNR(reqJson);
            case JSON_PROP_EXPIRYTIMELIMIT:
            	return updateTimeLimitDate(reqJson);

            default:
                return "no match for update type";
        }
    }

    private String updateTimeLimitDate(JSONObject reqJson) throws BookingEngineDBException {

        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AirOrders order = airRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("air time limit expiry date failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setTimeLimitExpiryDate(reqJson.getString(JSON_PROP_EXPIRYTIMELIMIT));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            AirOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
            myLogger.info(String.format("Air time limit expiry date updated Successfully for  orderId  %s = %s", orderID, updatedclientReconfirmDetails.toString()));
            return "Air time limit expiry date updated Successfully";
        }
    
	}

	private String updatePNR(JSONObject reqJson) throws BookingEngineDBException {

        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AirOrders order = airRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("PNRs failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setAirlinePNR(reqJson.getString(JSON_PROP_AIR_AIRLINEPNR));
            order.setGDSPNR(reqJson.getString(JSON_PROP_AIR_GDSPNR));
            order.setTicketingPNR(reqJson.getString(JSON_PROP_AIR_TICKETPNR));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            AirOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
            myLogger.info(String.format("Air PNRs updated Successfully for  orderId  %s = %s", orderID, updatedclientReconfirmDetails.toString()));
            return "Air PNRs updated Successfully";
        }
    
	}

	private String updatePaxDocument(JSONObject reqJson) throws BookingEngineDBException {
        String paxId = reqJson.getString(JSON_PROP_PASSENGERID);
        PassengerDetails paxDetails = passengerRepository.findOne(paxId);
        if (paxDetails == null) {
            response.put("ErrorCode", "BE_ERR_AIR_001");
            response.put("ErrorMsg", BE_ERR_AIR_001);
            myLogger.warn(String.format("Pax details  not found for  paxid  %s ", paxId));
            return (response.toString());
        } else {
            String prevPaxDetails = paxDetails.toString();
            paxDetails.setDocumentDetails(reqJson.getJSONArray(JSON_PROP_PAXDOCUMENT).toString());
            paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
            paxDetails.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            PassengerDetails updatedPaxDetails = savePaxDetails(paxDetails, prevPaxDetails);
            myLogger.info(String.format("Pax details updated successfully for  paxid  %s = %s", paxId,
                    updatedPaxDetails.toString()));
            return "pax details updated Successfully";

        }

    }

    private String updatePriceDetails(JSONObject reqJson) throws BookingEngineDBException {
        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AirOrders order = airRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("prices failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            Set<ClientCommercial> clientCommSet = updateOrderClientComms(reqJson.getJSONArray(JSON_PROP_ORDER_CLIENTCOMMS),order);
            Set<SupplierCommercial> suppCommSet = updateOrderSuppComms(reqJson.getJSONArray(JSON_PROP_ORDER_SUPPCOMMS),order);
            order.setClientCommercial(clientCommSet);
            order.setSuppcommercial(suppCommSet);

            order.setTotalPrice(reqJson.getJSONObject(JSON_PROP_ORDER_TOTALPRICEINFO).getString(JSON_PROP_TOTALPRICE));
            order.setTotalPriceCurrencyCode(reqJson.getJSONObject(JSON_PROP_ORDER_TOTALPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
            order.setTotalPriceTaxes(reqJson.getJSONObject(JSON_PROP_ORDER_TOTALPRICEINFO).getJSONObject(JSON_PROP_TAXES).toString());
            order.setTotalPriceBaseFare(reqJson.getJSONObject(JSON_PROP_ORDER_TOTALPRICEINFO).getJSONObject(JSON_PROP_BASEFARE).toString());
            order.setTotalPaxTypeFares(reqJson.getJSONObject(JSON_PROP_ORDER_TOTALPRICEINFO).getJSONArray(JSON_PROP_AIR_PAXTYPEFARES).toString());
            order.setTotalPriceFees(reqJson.getJSONObject(JSON_PROP_ORDER_TOTALPRICEINFO).getJSONObject(JSON_PROP_FEES).toString());
            order.setTotalPriceReceivables(reqJson.getJSONObject(JSON_PROP_ORDER_TOTALPRICEINFO).getJSONObject(JSON_PROP_RECEIVABLES).toString());

            order.setSupplierPrice(reqJson.getJSONObject(JSON_PROP_ORDER_SUPPLIERPRICEINFO).getString(JSON_PROP_SUPPPRICE));
            order.setSupplierPriceCurrencyCode(reqJson.getJSONObject(JSON_PROP_ORDER_SUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
            order.setSuppPaxTypeFares(reqJson.getJSONObject(JSON_PROP_ORDER_SUPPLIERPRICEINFO).getJSONArray(JSON_PROP_AIR_PAXTYPEFARES).toString());

            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

            saveOrder(order, prevOrder);

            myLogger.info(String.format("Air prices updated Successfully for  orderId  %s = %s", orderID, order.toString()));
            return "Air prices updated Successfully";
        }
    }

    private Set<SupplierCommercial> updateOrderSuppComms(JSONArray suppCommJsonArray, AirOrders order) {
        Set<SupplierCommercial> suppCommSet = new HashSet<SupplierCommercial>();

        for (Object object : suppCommJsonArray) {

            JSONObject suppCommJson = (JSONObject) object;
            SupplierCommercial suppComm = new SupplierCommercial();

            suppComm.setSupp_commercial_id(suppCommJson.getString("suppCommId"));
            suppComm.setCommercialName(suppCommJson.getString(JSON_PROP_COMMERCIALNAME));
            suppComm.setCommercialType(suppCommJson.getString(JSON_PROP_COMMERCIALTYPE));
            suppComm.setCommercialAmount(suppCommJson.getString(JSON_PROP_COMMAMOUNT));
            suppComm.setCommercialCurrency(suppCommJson.getString(JSON_PROP_COMMERCIALCURRENCY));
            suppComm.setRecieptNumber(suppCommJson.optString(JSON_PROP_RECIEPTNUMBER));
            suppComm.setInVoiceNumber(suppCommJson.optString(JSON_PROP_INVOICENUMBER));
            suppComm.setProduct(JSON_PROP_PRODUCTAIR);
            suppComm.setOrder(order);
            suppCommSet.add(suppComm);
        }
        return suppCommSet;
    }

    private Set<ClientCommercial> updateOrderClientComms(JSONArray orderClientCommJsonArray, AirOrders order) {

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
            clientComm.setProduct(JSON_PROP_PRODUCTAIR);
            clientComm.setOrder(order);
            clientCommSet.add(clientComm);
        }
        return clientCommSet;
    }


    private String updateClientReconfirmDate(JSONObject reqJson) throws BookingEngineDBException {
        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AirOrders order = airRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("ClientReconfirmdate failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setClientReconfirmDate(reqJson.getString(JSON_PROP_CLIENTRECONFIRMDATE));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            AirOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
            myLogger.info(String.format("Air client reconfirmation date updated Successfully for  orderId  %s = %s", orderID, updatedclientReconfirmDetails.toString()));
            return "Air order client reconfirmation date updated Successfully";
        }
    }

    private String updateSuppReconfirmDate(JSONObject reqJson) throws BookingEngineDBException {
        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AirOrders order = airRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("SuppReconfirmDate failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setSuppReconfirmDate(reqJson.getString(JSON_PROP_SUPPRECONFIRMDATE));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            AirOrders updatedSuppReconfirmDateDetails = saveOrder(order, prevOrder);
            myLogger.info(String.format("Air supplier reconfirmation date updated Successfully for  orderId  %s = %s", orderID, updatedSuppReconfirmDateDetails.toString()));
            return "Air order supplier reconfirmation date updated Successfully";
        }
    }

    private String updateClientReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AirOrders order = airRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("ClientReconfirmStatus failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setClientReconfirmStatus(reqJson.getString(JSON_PROP_CLIENTRECONFIRMSTATUS));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            AirOrders updatedclientReconfirmDetails = saveOrder(order, prevOrder);
            myLogger.info(String.format("Air client reconfirmation status updated Successfully for  orderId  %s = %s", orderID, updatedclientReconfirmDetails.toString()));
            return "Air order client reconfirmation status updated Successfully";
        }
    }

    private String updateSuppReconfirmStatus(JSONObject reqJson) throws BookingEngineDBException {
        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AirOrders order = airRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("SuppReconfirmStatus failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setSuppReconfirmStatus(reqJson.getString("suppReconfirmStatus"));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            AirOrders updatedSuppReconfirmDateDetails = saveOrder(order, prevOrder);
            myLogger.info(String.format("Air supplier reconfirmation status updated Successfully for  orderId  %s = %s", orderID, updatedSuppReconfirmDateDetails.toString()));
            return "Air order supplier reconfirmation status updated Successfully";
        }
    }

    private String updateOrderAttribute(JSONObject reqJson) throws BookingEngineDBException {
        AirOrders order = airRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format(
                    "Booking attribute  failed to update since Air order details   not found for  orderid  %s ",
                    reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID)));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setBookingAttribute(
                    reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_BOOKINGATTRIBUTE).toString());
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            AirOrders updatedBookingAttribute = saveOrder(order, prevOrder);
            myLogger.info(String.format("Booking attribute updated Successfully for orderID  %s = %s",
                    reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID),
                    updatedBookingAttribute.toString()));
            return "Booking Attribute Updated Successfully";
        }
    }

    private String updateTicketingPCC(JSONObject reqJson) throws BookingEngineDBException {
        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AirOrders order = airRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format(
                    "TicketingPCC  failed to update since Air order details   not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setTicketingPCC(reqJson.getString(JSON_PROP_TICKETINGPCC));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            AirOrders updatedTicketingPCCObj = saveOrder(order, prevOrder);
            myLogger.info(String.format("Air Ticketing PCC updated Successfully for orderID  %s = %s", orderID,
                    updatedTicketingPCCObj.toString()));
            return "Air order ticketing PCC updated Successfully";
        }
    }

    private String updateBirthDate(JSONObject reqJson) throws BookingEngineDBException {
        String paxID = reqJson.getString(JSON_PROP_PAXID);
        PassengerDetails paxDetails = passengerRepository.findOne(paxID);
        if (paxDetails == null) {
            response.put("ErrorCode", "BE_ERR_AIR_001");
            response.put("ErrorMsg", BE_ERR_AIR_001);
            myLogger.warn(String.format(
                    "BirthDate  failed to update since Air Passenger details   not found for paxid  %s ", paxID));
            return (response.toString());
        } else {
            String prevPaxDetails = paxDetails.toString();
            paxDetails.setBirthDate(reqJson.getString(JSON_PROP_BIRTHDATE));
            paxDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            paxDetails.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            PassengerDetails updatedPaxDetails = savePaxDetails(paxDetails, prevPaxDetails);
            myLogger.info(String.format("BirthDate updated Successfully for Air paxID  %s = %s", paxID,
                    updatedPaxDetails.toString()));
            return "passsenger birth date updated Successfully";
        }
    }


    private String updateSpecialRequest(JSONObject reqJson) throws BookingEngineDBException {
        String paxID = reqJson.getString(JSON_PROP_PAXID);
        PassengerDetails paxDetails = passengerRepository.findOne(paxID);
        if (paxDetails == null) {
            response.put("ErrorCode", "BE_ERR_AIR_001");
            response.put("ErrorMsg", BE_ERR_AIR_001);
            myLogger.warn(String.format(
                    "Special Request  failed to update since Air Passenger details   not found for paxid  %s ", paxID));
            return (response.toString());
        } else {
            String prevPaxDetails = paxDetails.toString();
            paxDetails.setSpecialRequests(reqJson.getJSONObject(JSON_PROP_SPECIALREQUESTS).toString());
            paxDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            paxDetails.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            PassengerDetails updatedPaxDetails = savePaxDetails(paxDetails, prevPaxDetails);
            myLogger.info(String.format("BirthDate updated Successfully for Air paxID  %s = %s", paxID,
                    updatedPaxDetails.toString()));
            return "Passenger's special request updated Successfully";
        }
    }

    private String updateFlightDetails(JSONObject reqJson) throws BookingEngineDBException {
        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AirOrders order = airRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format(
                    "Flight Details  failed to update since Air order details   not found for  orderid  %s ", orderID));
            return (response.toString());
        }
        String prevOrder = order.toString();
        order.setFlightDetails(reqJson.getJSONObject(JSON_PROP_AIR_FLIGHTDETAILS).toString());
        order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
        order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
        AirOrders updatedFlightDetailsObj = saveOrder(order, prevOrder);
        myLogger.info(String.format("Flight Details  updated Successfully for Air orderID %s = %s", orderID,
                updatedFlightDetailsObj.toString()));
        return "Flight Details updated Successfully";
    }
    
    private String updateVouchers(JSONObject reqJson) throws BookingEngineDBException {

		AirOrders order = airRepository
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
			AirOrders updatedVoucherDetails = saveOrder(order, prevOrder);
			myLogger.info(String.format("Air vouchers updated Successfully for  orderId  %s = %s",
					reqJson.getString(JSON_PROP_ORDERID),
					updatedVoucherDetails.toString()));
			
			return "vouchers Updated Successfully";
		}
	
	}

    private String updateStatus(JSONObject reqJson) throws BookingEngineDBException {
        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        AirOrders order = airRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String
                    .format("Status  failed to update since Air order details   not found for  orderid  %s ", orderID));
            return (response.toString());
        }
        String prevOrder = order.toString();
        order.setStatus(reqJson.getString(JSON_PROP_STATUS));
        order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
        order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
        AirOrders updatedStatusObj = saveOrder(order, prevOrder);
        myLogger.info(String.format("Status updated Successfully for Air orderID %s = %s", orderID,
                updatedStatusObj.toString()));
        return "Air Order status updated Successfully";
    }

    public String getByClass(String flightclass) {
        List<AirOrders> orders = getByClass.findByClass(flightclass);
        if (orders.size() > 0) {
        	
            JSONArray ordersArray = getAirOrdersJson(orders, "false");
            myLogger.info(String.format("Air bookings retrieved with status %s", flightclass));
            return ordersArray.toString();
        } else {
            myLogger.warn(String.format("Air bookings not found with status %s", flightclass));
            return "No orders with such class found";
        }
    }

    public JSONArray getCancellationsByBooking(Booking booking) {

        List<AirOrders> airOrders = airRepository.findByBooking(booking);
        JSONArray airOrdersJson = getAirOrdersCancellations(airOrders, "cancel");
        return airOrdersJson;

    }

    public JSONArray getAmendmentsByBooking(Booking booking) {

        List<AirOrders> airOrders = airRepository.findByBooking(booking);
        JSONArray airOrdersJson = getAirOrdersAmendments(airOrders, "amend");
        return airOrdersJson;

    }

    private JSONArray getAirOrdersAmendments(List<AirOrders> airOrders, String type) {
        JSONArray response = new JSONArray();
        for (AirOrders order : airOrders) {
            String orderId = order.getId();
            JSONObject orderJson = new JSONObject();

            List<AmCl> cancelAirOrders = accoAmClRepository.findByEntity(JSON_PROP_ENTITYTYPE_ORDER, orderId, type);
            JSONArray orderCancelArray = new JSONArray();

            for (AmCl cancelAirOrder : cancelAirOrders) {
                JSONObject cancelOrderJson = new JSONObject();
                cancelOrderJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, cancelAirOrder.getSupplierCharges());
                cancelOrderJson.put(JSON_PROP_COMPANYAMENDCHARGES, cancelAirOrder.getCompanyCharges());
                cancelOrderJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE,
                        cancelAirOrder.getSupplierChargesCurrencyCode());
                cancelOrderJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE,
                        cancelAirOrder.getCompanyChargesCurrencyCode());
                cancelOrderJson.put(JSON_PROP_AMENDTYPE, cancelAirOrder.getDescription());
                cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelAirOrder.getCreatedAt().toString().substring(0,
                        cancelAirOrder.getCreatedAt().toString().indexOf('[')));
                cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelAirOrder.getLastModifiedAt().toString().substring(0,
                        cancelAirOrder.getLastModifiedAt().toString().indexOf('[')));
                cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelAirOrder.getLastModifiedBy());
                orderCancelArray.put(cancelOrderJson);
            }

            JSONArray cancelPAxJsonArray = new JSONArray();
            JSONArray paxJsonArray = new JSONArray();
            List<AmCl> cancelPaxOrders = accoAmClRepository.findByEntity(JSON_PROP_ENTITYTYPE_PAX, orderId, type);

            for (AmCl cancelPaxOrder : cancelPaxOrders) {
                JSONObject cancelPaxJson = new JSONObject();
                cancelPaxJson.put(JSON_PROP_SUPPLIERAMENDCHARGES, cancelPaxOrder.getSupplierCharges());
                cancelPaxJson.put(JSON_PROP_COMPANYAMENDCHARGES, cancelPaxOrder.getCompanyCharges());
                cancelPaxJson.put(JSON_PROP_SUPPAMENDCHARGESCURRENCYCODE,
                        cancelPaxOrder.getSupplierChargesCurrencyCode());
                cancelPaxJson.put(JSON_PROP_COMPANYAMENDCHARGESCURRENCYCODE,
                        cancelPaxOrder.getCompanyChargesCurrencyCode());
                cancelPaxJson.put(JSON_PROP_AMENDTYPE, cancelPaxOrder.getDescription());
                cancelPaxJson.put(JSON_PROP_CREATEDAT, cancelPaxOrder.getCreatedAt().toString().substring(0,
                        cancelPaxOrder.getCreatedAt().toString().indexOf('[')));
                cancelPaxJson.put(JSON_PROP_LASTMODIFIEDAT, cancelPaxOrder.getLastModifiedAt().toString().substring(0,
                        cancelPaxOrder.getLastModifiedAt().toString().indexOf('[')));
                cancelPaxJson.put(JSON_PROP_LASTMODIFIEDBY, cancelPaxOrder.getLastModifiedBy());
                JSONArray entityIds = new JSONArray(cancelPaxOrder.getEntityID());
                StringBuffer newEntityIds = new StringBuffer();
                for (int z = 0; z < entityIds.length(); z++) {
                    if (z == 0) {
                        newEntityIds.append(entityIds.getJSONObject(z).getString(JSON_PROP_ENTITYID));
                    } else {
                        newEntityIds.append("\\,").append(entityIds.getJSONObject(z).getString(JSON_PROP_ENTITYID));
                    }
                    cancelPaxJson.put("paxIDs", newEntityIds);
                }
                cancelPAxJsonArray.put(cancelPaxJson);
            }

            if (cancelPAxJsonArray != null && cancelPAxJsonArray.length() != 0) {
                JSONObject paxJson = new JSONObject();
                // paxJson.put(JSON_PROP_PAXID, paxID);
                // paxJson.put(JSON_PROP_PAXAMENDMENTS, cancelPAxJsonArray);
                paxJsonArray.put(paxJson);
            }
            if (orderCancelArray.length() > 0) {
                orderJson.put(JSON_PROP_ORDERID, orderId);
                orderJson.put(JSON_PROP_ORDERAMENDS, orderCancelArray);
            }
            if (paxJsonArray.length() > 0) {
                orderJson.put(JSON_PROP_ORDERID, orderId);
                orderJson.put(JSON_PROP_PAXAMENDMENTS, cancelPAxJsonArray);
            }
            if (orderJson.length() > 0) {
                orderJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
                orderJson.put(JSON_PROP_PRODUCTSUBCATEGORY, JSON_PROP_AIR_PRODUCTSUBCATEGORY);
                response.put(orderJson);
            }
        }

        return response;
    }

    private JSONArray getAirOrdersCancellations(List<AirOrders> airOrders, String type) {
        JSONArray response = new JSONArray();
        for (AirOrders order : airOrders) {
            String orderId = order.getId();
            JSONObject orderJson = new JSONObject();

            List<AmCl> cancelAirOrders = accoAmClRepository.findByEntity(JSON_PROP_ENTITYTYPE_ORDER, orderId, type);
            if (cancelAirOrders.size() > 0) {
                orderJson.put(JSON_PROP_ORDERID, orderId);
                JSONArray orderCancelArray = new JSONArray();

                for (AmCl cancelAirOrder : cancelAirOrders) {
                    JSONObject cancelOrderJson = new JSONObject();
                    cancelOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelAirOrder.getSupplierCharges());
                    cancelOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelAirOrder.getCompanyCharges());
                    cancelOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE, cancelAirOrder.getSupplierChargesCurrencyCode());
                    cancelOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
                            cancelAirOrder.getCompanyChargesCurrencyCode());
                    cancelOrderJson.put(JSON_PROP_CANCELTYPE, cancelAirOrder.getDescription());
                    cancelOrderJson.put(JSON_PROP_CREATEDAT, cancelAirOrder.getCreatedAt().toString().substring(0,
                            cancelAirOrder.getCreatedAt().toString().indexOf('[')));
                    cancelOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelAirOrder.getLastModifiedAt().toString()
                            .substring(0, cancelAirOrder.getLastModifiedAt().toString().indexOf('[')));
                    cancelOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelAirOrder.getLastModifiedBy());
                    orderCancelArray.put(cancelOrderJson);
                }
                orderJson.put(JSON_PROP_ORDERCANCELLATIONS, orderCancelArray);
            }

            // JSONArray paxJsonArray = new JSONArray();
            List<AmCl> cancelPaxOrders = accoAmClRepository.findByEntity(JSON_PROP_ENTITYTYPE_PAX, orderId, type);
            if (cancelPaxOrders.size() > 0) {
                orderJson.put(JSON_PROP_ORDERID, orderId);
                JSONArray orderCancelPaxArray = new JSONArray();
                for (AmCl cancelPaxOrder : cancelPaxOrders) {
                    JSONObject cancelPaxOrderJson = new JSONObject();
                    cancelPaxOrderJson.put(JSON_PROP_SUPPLIERCANCELCHARGES, cancelPaxOrder.getSupplierCharges());
                    cancelPaxOrderJson.put(JSON_PROP_COMPANYCANCELCHARGES, cancelPaxOrder.getCompanyCharges());
                    cancelPaxOrderJson.put(JSON_PROP_SUPPCANCCHARGESCODE,
                            cancelPaxOrder.getSupplierChargesCurrencyCode());
                    cancelPaxOrderJson.put(JSON_PROP_COMPANYCANCCHARGESCODE,
                            cancelPaxOrder.getCompanyChargesCurrencyCode());
                    cancelPaxOrderJson.put(JSON_PROP_CANCELTYPE, cancelPaxOrder.getDescription());
                    cancelPaxOrderJson.put(JSON_PROP_CREATEDAT, cancelPaxOrder.getCreatedAt().toString().substring(0,
                            cancelPaxOrder.getCreatedAt().toString().indexOf('[')));
                    cancelPaxOrderJson.put(JSON_PROP_LASTMODIFIEDAT, cancelPaxOrder.getLastModifiedAt().toString()
                            .substring(0, cancelPaxOrder.getLastModifiedAt().toString().indexOf('[')));
                    cancelPaxOrderJson.put(JSON_PROP_LASTMODIFIEDBY, cancelPaxOrder.getLastModifiedBy());
                    JSONArray entityIDArray = new JSONArray(cancelPaxOrder.getEntityID());
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
                    cancelPaxOrderJson.put("paxIDs", newEntityIds);
                    orderCancelPaxArray.put(cancelPaxOrderJson);

                }

                orderJson.put(JSON_PROP_PAXCANCELLATIONS, orderCancelPaxArray);
            }
            if (orderJson.length() > 0) {
                orderJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_PRODUCTCATEGORY_TRANSPORTATION);
                orderJson.put(JSON_PROP_PRODUCTSUBCATEGORY, JSON_PROP_AIR_PRODUCTSUBCATEGORY);
                response.put(orderJson);
            }
        }
        return response;

    }


	private String updatePaxDetails(JSONObject reqJson) throws BookingEngineDBException {
		String paxId = reqJson.getString(JSON_PROP_PASSENGERID);
		PassengerDetails paxDetails = passengerRepository.findOne(paxId);
		if (paxDetails == null) {
			response.put("ErrorCode", "BE_ERR_AIR_001");
			response.put("ErrorMsg", BE_ERR_AIR_001);
			myLogger.warn(String.format("Air Pax details  not found for  paxid  %s ", paxId));
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
            myLogger.info(String.format("Air Pax details updated successfully for  paxid  %s = %s", paxId,
                    updatedPaxDetails.toString()));
            return "Air pax details updated Successfully";

        }
    }

    public String getDocumentDetails(JSONObject reqJson) {
        JSONArray resJson = new JSONArray();
        AirOrders order = airRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format(
                    "TicketingPCC  failed to update since Air order details   not found for  orderid  %s ",
                    reqJson.getString(JSON_PROP_ORDERID)));
            return (response.toString());
        } else {
            JSONArray paxDetails = new JSONArray(order.getPaxDetails());
            JSONObject paxObj;
            for (int i = 0; i < paxDetails.length(); i++) {
                JSONObject paxDocObj = new JSONObject();
                paxObj = paxDetails.getJSONObject(i);
                PassengerDetails currPaxObj = passengerRepository.findOne(paxObj.getString(JSON_PROP_PAXID));
                paxDocObj.put(JSON_PROP_PAXID, paxObj.getString(JSON_PROP_PAXID));
                if (currPaxObj.getDocumentDetails() != null)
                    paxDocObj.put(JSON_PROP_DOCUMENTDETAILS, new JSONObject(currPaxObj.getDocumentDetails()));
                resJson.put(paxDocObj);
            }
            return resJson.toString();
        }
    }

    public JSONArray getOrdersInRange(ZonedDateTime startdateTime, ZonedDateTime enddateTime, String suppRef) {
        List<AirOrders> temp = airRepository.getOrdersInRange(startdateTime, enddateTime, suppRef);
        JSONArray airOrders = getAirOrdersJson(temp, "false");
        return airOrders;
    }

    public String getByGDS(String gdsPNR) {
        List<AirOrders> temp = airRepository.getAirGDSDetailsForGDSPNR(gdsPNR);
        JSONObject airGDSDetails = new JSONObject();

        /** temp.get(0), As for one GDSPNR there should be exactly one row in table */
        airGDSDetails.put("id", temp.get(0).getId());
        airGDSDetails.put("bookId", temp.get(0).getBooking().getBookID());
        return airGDSDetails.toString();
    }

    public String getPaxDetailsGDS(String paxId) {
        PassengerDetails temp = passengerRepository.findOne(paxId);
        JSONObject passengerDetails = new JSONObject();

        passengerDetails.put("name", temp.getFirstName() + " " + temp.getMiddleName() + " " + temp.getLastName());
        return passengerDetails.toString();
    }
}
