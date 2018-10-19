package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.criteria.Order;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.enums.BookingAttribute;
import com.coxandkings.travel.bookingengine.db.enums.BookingStatus;
import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.AirDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("Air")
@Transactional(readOnly=false)
public class AirDataBaseServiceImpl implements DataBaseService,Constants,ErrorConstants,CancelAmendTypes {

	@Autowired
	@Qualifier("Air")
	private AirDatabaseRepository airRepository;
	
	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingService;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("AccoAmCl")
	private AmClRepository accoAmClRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
    Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	JSONObject response=new JSONObject();
	
	public boolean isResponsibleFor(String product) {
        return "air".equalsIgnoreCase(product);
    }

	public String processBookRequest(JSONObject bookRequestJson) throws JSONException, BookingEngineDBException {
		
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null)
		booking = bookingService.processBookRequest(bookRequestJson,false);
		
		JSONArray paxDetailsJson = bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAXDETAILS);
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_AIR_PRICEDITINERARY)) {

			AirOrders order = populateAirData((JSONObject) orderJson, paxDetailsJson, bookRequestJson,booking);
			
			System.out.println(order);
			saveOrder(order,"");

		}
		myLogger.info(String.format("Air Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));
		return "success";
	}

	public AirOrders populateAirData(JSONObject pricedItineraryJson,JSONArray paxDetailsJson,JSONObject bookRequestJson ,Booking booking ) throws BookingEngineDBException {
		try {
		AirOrders order=new AirOrders();
		String tripIndicator=bookRequestJson.getJSONObject(JSON_PROP_REQBODY).optString(JSON_PROP_AIR_TRIPINDICATOR);
		String tripType=bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_AIR_TRIPTYPE);
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		order.setBooking(booking);
		order.setProductSubCategory("Flight");
		order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setStatus(OrderStatus.RQ.getProductStatus());
		if(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).has(JSON_PROP_TIMELIMITEXPIRYDATE))
		{	
			String bookingAttribute = order.getBookingAttribute();
			JSONArray bookingAttributeArray ;
			if(bookingAttribute != null) {
		    bookingAttributeArray = new JSONArray(bookingAttribute);
		    }
			else
			{
				 bookingAttributeArray=new JSONArray();
			}
			JSONObject bookingAttributeObj=new JSONObject();
			bookingAttributeObj.put(BookingAttribute.BOOKING_TYPE_TIME_LIMIT.toString(),BookingAttribute.BOOKING_TYPE_TIME_LIMIT.getBookingAttribute());
			bookingAttributeArray.put(bookingAttributeObj);
			order.setBookingAttribute(bookingAttributeArray.toString());
		}
		//TODO: for now populating all suppliers as offline. Later have  this logic after operations is concrete.
		order.setSupplierType("online");
		
		order.setSupplierID(pricedItineraryJson.getString(JSON_PROP_SUPPREF));
		
		//TODO: trip indicator and tripType will need to be moved inside priced itinerary?
		order.setTripIndicator(tripIndicator);
		order.setTripType(tripType);
		
		order.setSuppTransactionId(pricedItineraryJson.optString("transactionID"));
		order.setRoe(pricedItineraryJson.optString(JSON_PROP_ROE));
		order.setSuppPaxTypeFares(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_AIR_SUPPITINERARYPRICINGINFO).getJSONArray(JSON_PROP_AIR_PAXTYPEFARES).toString());
		order.setSupplierPrice(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_AIR_SUPPITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setSupplierPriceCurrencyCode(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_AIR_SUPPITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getString(JSON_PROP_CURRENCYCODE));
		if(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).has(JSON_PROP_COMPANYTAXES))
		order.setCompanyTaxes(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_COMPANYTAXES).toString());	
		if(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).has(JSON_PROP_INCENTIVES))
		order.setIncentives(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_INCENTIVES).toString());
		if(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).has(JSON_PROP_DISCOUNTS))
		order.setCompanyTaxes(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_DISCOUNTS).toString());
		order.setTotalPaxTypeFares(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONArray(JSON_PROP_AIR_PAXTYPEFARES).toString());
		order.setTotalPrice(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setTotalPriceCurrencyCode(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getString(JSON_PROP_CURRENCYCODE));
		
		//TODO: Confirm if we need any other price components here for air 
		
		order.setTotalPriceBaseFare(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getJSONObject(JSON_PROP_BASEFARE).toString());
		if(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).has(JSON_PROP_RECEIVABLES))
		order.setTotalPriceReceivables(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
		if(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).has(JSON_PROP_FEES))
		order.setTotalPriceFees(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getJSONObject(JSON_PROP_FEES).toString());
		order.setTotalPriceTaxes(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONObject(JSON_PROP_AIR_ITINTOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
		
		//TODO: Do we need to set adult count and child count as well?
		Set<PassengerDetails> setPaxDetails = new HashSet<PassengerDetails>();
		setPaxDetails = readPassengerDetails(paxDetailsJson, order);
		
		JSONArray paxIds = new JSONArray();
		for(PassengerDetails pax:setPaxDetails ) {
			JSONObject paxJson = new JSONObject();
			paxJson.put(JSON_PROP_PAXID, pax.getPassanger_id());
			paxIds.put(paxJson);
		}

		order.setPaxDetails(paxIds.toString());
		order.setFlightDetails(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARY).toString());
		Set<SupplierCommercial> suppComms =  new HashSet<SupplierCommercial>();
        suppComms = readSuppCommercials(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_AIR_SUPPITINERARYPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS),order);
        
        Set<ClientCommercial> clientComms =  new HashSet<ClientCommercial>();
        clientComms = readClientCommercials(pricedItineraryJson.getJSONObject(JSON_PROP_AIR_ITINERARYPRICINGINFO).getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS),order);
		order.setClientCommercial(clientComms);
		order.setSuppcommercial(suppComms);
		
		return order;
	}catch(Exception e)
		{
		myLogger.fatal("Failed to populate Air Data "+ e);
		throw new BookingEngineDBException("Failed to populate Air Data");
	}
}
	
	

	private Set<PassengerDetails> readPassengerDetails(JSONArray paxJsonArray, AirOrders airOrder) throws BookingEngineDBException {
		 
		Set<PassengerDetails> paxDetailsSet = new HashSet<PassengerDetails>();
		PassengerDetails paxDetails;
		for(int i=0;i<paxJsonArray.length();i++)	{
		JSONObject currenntPaxDetails = paxJsonArray.getJSONObject(i);
		paxDetails =new PassengerDetails();
		
	    if(currenntPaxDetails.has(JSON_PROP_SEATMAP))
	    paxDetails.setSeatMap(currenntPaxDetails.getJSONArray(JSON_PROP_SEATMAP).toString());
		paxDetails.setTitle(currenntPaxDetails.getString(JSON_PROP_TITLE) );
		paxDetails.setFirstName(currenntPaxDetails.getString(JSON_PROP_FIRSTNAME) );
		if(i==0)
		{
		paxDetails.setIsLeadPax(true);
		}
		else
		{
			paxDetails.setIsLeadPax(false);
		}
		paxDetails.setStatus(OrderStatus.RQ.getProductStatus());
		paxDetails.setMiddleName(currenntPaxDetails.getString(JSON_PROP_MIDDLENAME) );
		paxDetails.setLastName(currenntPaxDetails.getString(JSON_PROP_SURNAME));
		paxDetails.setBirthDate(currenntPaxDetails.getString(JSON_PROP_DOB));
		paxDetails.setPaxType(currenntPaxDetails.getString(JSON_PROP_PAX_TYPE) );
		paxDetails.setGender(currenntPaxDetails.getString(JSON_PROP_GENDER));
		paxDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
		paxDetails.setAddressDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
                if(Pax_ADT.equals(currenntPaxDetails.getString(JSON_PROP_PAX_TYPE)))		
                paxDetails.setDocumentDetails(currenntPaxDetails.getJSONObject(JSON_PROP_DOCUMENTDETAILS).toString());
                
        if(currenntPaxDetails.has(JSON_PROP_SPECIALREQUESTS))
		paxDetails.setSpecialRequests(currenntPaxDetails.getJSONObject(JSON_PROP_SPECIALREQUESTS).toString());
		paxDetails.setAncillaryServices(currenntPaxDetails.getJSONObject(JSON_PROP_ANCILLARYSERVICES).toString());
		
		//TODO:change it to userID later 
		paxDetails.setLastModifiedBy("");
		paxDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
	    savePaxDetails(paxDetails,"");
		//TODO: later check if we are going to get any paxKey in BE
		//paxDetails[i].setPaxkey(currenntPaxDetails.getString("paxKey") );
		
		paxDetailsSet.add(paxDetails);
		
		}
		return paxDetailsSet;
	}
	
	private Set<SupplierCommercial> readSuppCommercials(JSONArray suppCommsJsonArray, AirOrders order) {
		 
		
		Set<SupplierCommercial> suppCommercialsSet =new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;
		
		for(int i=0;i<suppCommsJsonArray.length();i++)	{
		JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);
		
		suppCommercials =new SupplierCommercial();
		suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
		suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
		suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
		suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));
		
	
		suppCommercials.setProduct(JSON_PROP_PRODUCTAIR);
		suppCommercials.setOrder(order);
		suppCommercialsSet.add(suppCommercials);
		}
		return suppCommercialsSet;
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, AirOrders order) {
	 
	Set<ClientCommercial> clientCommercialsSet =new HashSet<ClientCommercial>();
	ClientCommercial clientCommercials;
	
	for(int i=0;i<clientCommsJsonArray.length();i++)	{
		
		JSONObject totalClientComm = clientCommsJsonArray.getJSONObject(i);
		
		 String clientID = totalClientComm.getString(JSON_PROP_CLIENTID);
		 String parentClientID = totalClientComm.getString(JSON_PROP_PARENTCLIENTID);;		
		 String commercialEntityType = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYTYPE);;		
		 String commercialEntityID = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYID);;
		
		 boolean companyFlag = (i==0)?true:false;
		
	
	JSONArray clientComms = totalClientComm.getJSONArray(JSON_PROP_CLIENTCOMMERCIALSTOTAL);
	
	for(int j=0;j<clientComms.length();j++) {
	
	JSONObject clientComm = clientComms.getJSONObject(j);
	
	clientCommercials =new ClientCommercial();
	clientCommercials.setCommercialName(clientComm.getString(JSON_PROP_COMMERCIALNAME));
	clientCommercials.setCommercialType(clientComm.getString(JSON_PROP_COMMERCIALTYPE));
	clientCommercials.setCommercialAmount(clientComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
	clientCommercials.setCommercialCurrency(clientComm.getString(JSON_PROP_COMMERCIALCURRENCY));
	clientCommercials.setClientID(clientID);
	clientCommercials.setParentClientID(parentClientID);
	clientCommercials.setCommercialEntityType(commercialEntityType);
	clientCommercials.setCommercialEntityID(commercialEntityID);
	clientCommercials.setCompanyFlag(companyFlag);

	clientCommercials.setProduct(JSON_PROP_PRODUCTAIR);
	clientCommercials.setOrder(order);
	clientCommercialsSet.add(clientCommercials);
	}
	}
	return clientCommercialsSet;
	}
	

	public String processBookResponse(JSONObject bookResponseJson) {

		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		if(booking==null)
		{
			myLogger.warn(String.format("AIR Booking Response could not be populated since no bookings found for req with bookID %s", bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID)));
			response.put("ErrorCode","BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			return response.toString();
		}
		else
		{
		List<AirOrders> orders = airRepository.findByBooking(booking);
		JSONArray suppBookRefArray = bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_SUPPLIERBOOKREFERENCES);
		for(int i=0;i<suppBookRefArray.length();i++) {
			JSONObject suppBookObj = new JSONObject();
			suppBookObj=suppBookRefArray.getJSONObject(i);
			
		for(int j=0;j<orders.size();j++) {
		 AirOrders order=orders.get(j);
			if(j==i) {
			if(suppBookObj.opt(JSON_PROP_AIR_AIRLINEPNR).equals(""))
			{
				order.setStatus(OrderStatus.RQ.getProductStatus());
				String bookingAttribute = order.getBookingAttribute();
				JSONArray bookingAttributeArray ;
				if(bookingAttribute != null) {
			    bookingAttributeArray = new JSONArray(bookingAttribute);
			    }
				else
				{
					 bookingAttributeArray=new JSONArray();
				}
				JSONObject bookingAttributeObj=new JSONObject();
				bookingAttributeObj.put(BookingAttribute.BF.toString(),BookingAttribute.BF.getBookingAttribute());
				bookingAttributeArray.put(bookingAttributeObj);
				order.setBookingAttribute(bookingAttributeArray.toString());
			
			}
			else
			{
			order.setStatus(OrderStatus.OK.getProductStatus());
			}
			order.setAirlinePNR(suppBookObj.getString(JSON_PROP_AIR_AIRLINEPNR));
			order.setGDSPNR(suppBookObj.getString("gdsPNR"));
			order.setTicketingPNR(suppBookObj.getString(JSON_PROP_AIR_TICKETPNR));
			order.setBookingDateTime(new Date().toString());
			airRepository.save(order);
			}
		}
		}
		myLogger.info(String.format("Air Booking Response populated successfully for req with bookID %s = %s", bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID),bookResponseJson.toString()));
		return "SUCCESS";
		}
	}
	
	public String processAmClResponse(JSONObject reqJson) throws Exception {
		JSONArray suppBookReferences= reqJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_SUPPLIERBOOKREFERENCES);
		for(int i=0;i<suppBookReferences.length();i++) {
		JSONObject currentSuppBookRefObj = suppBookReferences.getJSONObject(i);
		List<AmCl> amendEntries  = accoAmClRepository.findforResponseUpdate(currentSuppBookRefObj.getString(JSON_PROP_ORDERID),reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_ENTITYNAME),currentSuppBookRefObj.getJSONArray(JSON_PROP_ENTITYIDS).toString(), reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_TYPE), reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_REQUESTTYPE));
		
		if(amendEntries.size()==0) {
			//TODO: handle this before it goes in prod
			System.out.println("no amend entry found. Request might not have been populated");
			 myLogger.warn("no amend entry found. Request might not have been populated"+reqJson );
		}
		
		else if(amendEntries.size()>1) {
			//TODO: handle this before it goes in prod
			System.out.println("multiple amend entries found. Dont know which one to update");
			  myLogger.warn("multiple amend entries found. Dont know which one to update"+reqJson );
		}
		
		else {
			AirOrders order = airRepository.findOne(currentSuppBookRefObj.getString(JSON_PROP_ORDERID));
			reqJson.getJSONObject(JSON_PROP_RESBODY).put("bookID",order.getBooking().getBookID());
			String bookingAttribute = order.getBookingAttribute();
			JSONArray bookingAttributeArray ;
			if(bookingAttribute != null) {
		    bookingAttributeArray = new JSONArray(bookingAttribute);
		    }
			else
			{
				 bookingAttributeArray=new JSONArray();
			}
			JSONObject bookingAttributeObj=new JSONObject();
			if(reqJson.getJSONObject(JSON_PROP_RESBODY).has(JSON_PROP_ERRCODE))
			{
				if(reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_TYPE).equalsIgnoreCase(JSON_PROP_AIR_CANCELTYPE_FULLCANCEL))
				{
					bookingAttributeObj.put(BookingAttribute.CF.toString(), BookingAttribute.CF.getBookingAttribute());
					bookingAttributeArray.put(bookingAttributeObj);
					order.setBookingAttribute(bookingAttributeArray.toString());
				}
				else
				{
					bookingAttributeObj.put(BookingAttribute.AF.toString(), BookingAttribute.AF.getBookingAttribute());
					bookingAttributeArray.put(bookingAttributeObj);
					order.setBookingAttribute(bookingAttributeArray.toString());
				}
				order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
				order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_RESHEADER).getString(JSON_PROP_USERID));
			}
			else
			{
				if(reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_TYPE).equalsIgnoreCase(JSON_PROP_AIR_CANCELTYPE_FULLCANCEL))
				{
					   order.setStatus(OrderStatus.XL.getProductStatus());	
				}
				else
				{
					bookingAttributeObj.put(BookingAttribute.AMENDED.toString(), BookingAttribute.AMENDED.getBookingAttribute());
				    bookingAttributeArray.put(bookingAttributeObj);
					order.setBookingAttribute(bookingAttributeArray.toString());
			     }
		
			AmCl amendEntry = amendEntries.get(0);	
		String prevOrder = amendEntry.toString();
		amendEntry.setCompanyCharges(currentSuppBookRefObj.getString(JSON_PROP_COMPANYCHARGES));
		amendEntry.setSupplierCharges(currentSuppBookRefObj.getString(JSON_PROP_SUPPCHARGES));
		amendEntry.setSupplierChargesCurrencyCode(currentSuppBookRefObj.getString(JSON_PROP_SUPPCHARGESCURRENCYCODE));
		amendEntry.setCompanyChargesCurrencyCode(currentSuppBookRefObj.getString(JSON_PROP_COMPANYCHARGESCURRENCYCODE));
		amendEntry.setStatus(OrderStatus.OK.getProductStatus());
		amendEntry.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		amendEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_RESHEADER).getString(JSON_PROP_USERID));
		
		//TODO: also set the currency codes and breakups before saving
		saveAirAmCl(amendEntry, prevOrder);
		//AmendNotificationToOps.sendAmendNotificationToOps(reqJson);
		myLogger.info(String.format("AMCL Response populated successfully for req %s",reqJson.toString()));
		}
			saveOrder(order, "");
		}
		}
		return "SUCCESS";
		
	}
	
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException {
		JSONArray suppBookReferences= reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_SUPPLIERBOOKREFERENCES);
		String res = null;
		for(int i=0;i<suppBookReferences.length();i++) {
		JSONObject currentSuppBookRefObj = suppBookReferences.getJSONObject(i);
		String type = reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_TYPE);
		//TODO: Check if order level status needs to be updated for each request
		
		AmCl amendEntry = new AmCl();
		amendEntry.setEntityID(currentSuppBookRefObj.getJSONArray(JSON_PROP_ENTITYIDS).toString());
		amendEntry.setOrderID(currentSuppBookRefObj.getString(JSON_PROP_ORDERID));
		amendEntry.setEntityName(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ENTITYNAME));
		amendEntry.setRequestType(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_REQUESTTYPE));
		amendEntry.setSupplierCharges("0");
		amendEntry.setDescription(type);
		amendEntry.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		amendEntry.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		amendEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		amendEntry.setStatus(OrderStatus.RQ.getProductStatus());
		AirOrders order = airRepository.findOne(currentSuppBookRefObj.getString(JSON_PROP_ORDERID));
		String bookingAttribute = order.getBookingAttribute();
		JSONArray bookingAttributeArray ;
		if(bookingAttribute != null) {
	    bookingAttributeArray = new JSONArray(bookingAttribute);
	    }
		else
		{
			 bookingAttributeArray=new JSONArray();
		}
		JSONObject bookingAttributeObj=new JSONObject();
		if(type.equals(JSON_PROP_AIR_CANCELTYPE_FULLCANCEL))
		{
			order.setCancelDate(ZonedDateTime.now( ZoneOffset.UTC ).toString());
		}
		else
		{
		order.setAmendDate(ZonedDateTime.now( ZoneOffset.UTC ).toString());
		bookingAttributeObj.put(BookingAttribute.RAMD.toString(),BookingAttribute.RAMD.getBookingAttribute());
		bookingAttributeArray.put(bookingAttributeObj);
		order.setBookingAttribute(bookingAttributeArray.toString());
		}
		saveOrder(order, "");
		saveAirAmCl(amendEntry, "");
		myLogger.info(String.format(" Air AMCL req populated  for request %s",reqJson));
		
		
		 switch(type)
	        {
	           
	        	case JSON_PROP_AIR_CANCELTYPE_CANCELPAX:
	        		res= updatePaxDetails(reqJson,currentSuppBookRefObj,type);
	        		break;
	        	case JSON_PROP_AIR_AMENDTYPE_UPDATEPAX:
	        		res= updatePaxDetails(reqJson,currentSuppBookRefObj,type);
	        		break;
	           /* case JSON_PROP_AIR_CANNCELTYPE_CANCELJOU:
	            	return updateFlightDetails(reqJson,type);*/
	            case JSON_PROP_AIR_CANCELTYPE_CANCELSSR:
	            	res= cancelSSR(reqJson,currentSuppBookRefObj);
	            	break;
	        	case JSON_PROP_AIR_CANCELTYPE_FULLCANCEL:
	                res= fullCancel(reqJson,currentSuppBookRefObj);
	                break;
	        	case JSON_PROP_AIR_CANCELTYPE_CANCELJOU:
	        		res= cancelODO(reqJson,currentSuppBookRefObj);
	        		break;
	        	case JSON_PROP_AIR_AMENDTYPE_SSR:
	        		res= amendSSR(reqJson,currentSuppBookRefObj);
	        		break;
	        	case JSON_PROP_AIR_AMENDTYPE_REM:
	        		res= amendREM(reqJson,currentSuppBookRefObj);
	        		break;
	         	case JSON_PROP_AIR_AMENDTYPE_PIS:
	         		res= amendPIS(reqJson,currentSuppBookRefObj);
	         		break;
	            default:
	                res= "no match for cancel/amend type";
	                break;
	        }	
		}
		if(res.equalsIgnoreCase("success"))
		return "success ";
		else if(res.equalsIgnoreCase("no match for cancel/amend type"))
		return "no match for cancel/amend type";
			else
			return "failed";
	}
	

private String amendPIS(JSONObject reqJson, JSONObject currentSuppBookRefObj) throws BookingEngineDBException {
	   JSONArray paxDetails = currentSuppBookRefObj.getJSONArray(JSON_PROP_PAXDETAILS);
			for(int j=0;j<paxDetails.length();j++)
			{
				JSONObject currPaxObj = paxDetails.getJSONObject(j);
	PassengerDetails paxDetailsObj = passengerRepository.findOne(currPaxObj.getString(JSON_PROP_PAXID));
	if(paxDetailsObj==null) {
		myLogger.info(String.format("Air amend PIS request failed since paxID not found with id %s for req %s",reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_AIR_PASSENGERID),reqJson)); 
		response.put("ErrorCode", "BE_ERR_AIR_001");
		response.put("ErrorMsg", BE_ERR_AIR_001);
		return response.toString();
	}
	String prevOrder = paxDetailsObj.toString();
	String contactDetails=paxDetailsObj.getContactDetails();
	JSONArray contactDetailsArr=new JSONArray(contactDetails);
    JSONObject contactDetailsObj=contactDetailsArr.getJSONObject(0);
    
	JSONObject contactDetailsNewArr=currPaxObj.getJSONObject(JSON_PROP_PAXDATAAMENDINFO);
	contactDetailsObj.getJSONObject(JSON_PROP_CONTACTINFO).remove(JSON_PROP_MOBILENO);
	contactDetailsObj.getJSONObject(JSON_PROP_CONTACTINFO).remove(JSON_PROP_EMAIL);
    contactDetailsObj.getJSONObject(JSON_PROP_CONTACTINFO).put(JSON_PROP_MOBILENO,(contactDetailsNewArr.getJSONObject(JSON_PROP_TELEPHONEINFO)));
    contactDetailsObj.getJSONObject(JSON_PROP_CONTACTINFO).put(JSON_PROP_EMAIL,(contactDetailsNewArr.getJSONObject(JSON_PROP_EMAILINFO)));
    paxDetailsObj.setContactDetails(contactDetailsArr.toString());
	savePaxDetails(paxDetailsObj, prevOrder);
			}
		return "SUCCESS";
	}

private String amendREM(JSONObject reqJson, JSONObject currentSuppBookRefObj) throws BookingEngineDBException {
	
	AirOrders order = airRepository.findOne(currentSuppBookRefObj.getString(JSON_PROP_ORDERID));
	if(order == null) {
		 myLogger.info(String.format("Air AMEND rem request failed since air order not found with id %s for req %s",reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID),reqJson)); 
		 response.put("ErrorCode", "BE_ERR_004");
		 response.put("ErrorMsg", BE_ERR_004);
		return response.toString();
	}
	else
	{
	String prevOrder = order.toString();
	order.setRemark(currentSuppBookRefObj.getJSONArray(JSON_PROP_PAXREMARKINFO).toString());
	order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
	order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
	AirOrders updatedOrderDetails = saveOrder(order, prevOrder);
	myLogger.info(String.format(" Air REM amended for orderID %s for request %s", updatedOrderDetails.getId(),reqJson));
	return "SUCCESS";
	}
	}

private String cancelODO(JSONObject reqJson, JSONObject currentSuppBookRefObj) throws BookingEngineDBException {
	  JSONObject currFlightSeg,origDestObj1,operatingAirlineObj;
	 JSONArray origDestArray = currentSuppBookRefObj.getJSONArray(JSON_PROP_AIR_ORIGINDESTINATIONOPTIONS);
	 String orderID = currentSuppBookRefObj.getString(JSON_PROP_ORDERID);
	 AirOrders order = airRepository.findOne(orderID);
	 if(order == null)
	 {
		 myLogger.info(String.format("Air cancelODO request failed since air order not found with id %s for req %s",orderID,reqJson)); 
		 response.put("ErrorCode", "BE_ERR_004");
		 response.put("ErrorMsg", BE_ERR_004);
		 return response.toString();
	 }
	 else
	 {
	 JSONObject flightDetails = new JSONObject(order.getFlightDetails());
	 JSONArray originDestinationOptionsArray = flightDetails.getJSONArray(JSON_PROP_AIR_ORIGINDESTINATIONOPTIONS);
		
	 String prevOrder = order.toString();
	 for(int i=0;i<origDestArray.length();i++)
		{
			JSONObject origDestObj = origDestArray.getJSONObject(i);
			JSONArray flightSegArr = origDestObj.getJSONArray(JSON_PROP_AIR_FLIGHTSEGMENT);
			for(int j=0;j<flightSegArr.length();j++)
			{
				JSONObject flightSegObj = flightSegArr.getJSONObject(j);
				JSONObject operatingAirlineObjCan = flightSegObj.getJSONObject(JSON_PROP_AIR_OPERATINGAIRLINE);
				for(int k=0;k<originDestinationOptionsArray.length();k++)//DB
				{
                origDestObj1 = originDestinationOptionsArray.getJSONObject(k);
				    JSONArray flightSegArr2 = 	origDestObj1.getJSONArray(JSON_PROP_AIR_FLIGHTSEGMENT);
				    for(int l=0;l<flightSegArr2.length();l++)
				    {
				    	     currFlightSeg = flightSegArr2.getJSONObject(l);
				      		 operatingAirlineObj = currFlightSeg.getJSONObject(JSON_PROP_AIR_OPERATINGAIRLINE);
				      		  if((operatingAirlineObjCan.getString(JSON_PROP_AIR_AIRLINECODE).equalsIgnoreCase(operatingAirlineObj.getString(JSON_PROP_AIR_AIRLINECODE)))  && (operatingAirlineObjCan.getString(JSON_PROP_AIR_FLIGHTNUMBER).equalsIgnoreCase(operatingAirlineObj.getString(JSON_PROP_AIR_FLIGHTNUMBER))))
				      		  {
				      			currFlightSeg.put("status", "Cancel");
				      		  }
				      }
				    
				}
				 
			}
			order.setFlightDetails(flightDetails.toString());
			AirOrders order1 = saveOrder(order, prevOrder);
			myLogger.info(String.format(" Air order cancelled with orderID %s for request %s", order1.getId(),reqJson));
		
		}

	 }

	
	 return "SUCCESS";
		
	}
	private String fullCancel(JSONObject reqJson, JSONObject currentSuppBookRefObj) throws BookingEngineDBException {
		
		AirOrders order = airRepository.findOne(currentSuppBookRefObj.getString(JSON_PROP_ORDERID));
		if(order == null) {
			 myLogger.info(String.format("Air fullcancel request failed since air order not found with id %s for req %s",reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID),reqJson)); 
			 response.put("ErrorCode", "BE_ERR_004");
			 response.put("ErrorMsg", BE_ERR_004);
			return response.toString();
		}
		else
		{
		String prevOrder = order.toString();
		order.setStatus(OrderStatus.RXL.getProductStatus());
		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		AirOrders updatedOrderDetails = saveOrder(order, prevOrder);
		myLogger.info(String.format(" Air ODO cancelled for orderID %s for request %s", updatedOrderDetails.getId(),reqJson));
		return "SUCCESS";
		}
	}
	
	private String cancelSSR(JSONObject reqJson, JSONObject currentSuppBookRefObj) throws BookingEngineDBException {
		
		    /*JSONArray supplierBookRefArray =  reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_SUPPLIERBOOKREFERENCES);
			for(int i=0;i<supplierBookRefArray.length();i++) {
			JSONObject currSuppBookRefObj = supplierBookRefArray.getJSONObject(i);*/
		    JSONArray paxDetails = currentSuppBookRefObj.getJSONArray(JSON_PROP_PAXDETAILS);
			for(int j=0;j<paxDetails.length();j++)
			{
				JSONObject currPaxObj = paxDetails.getJSONObject(j);
				
		PassengerDetails paxDetailsObj = passengerRepository.findOne(currPaxObj.getString(JSON_PROP_PAXID));
		if(paxDetailsObj==null) {
			myLogger.info(String.format("Air cancel SSR request failed since paxID not found with id %s for req %s",currPaxObj.getString(JSON_PROP_AIR_PASSENGERID),reqJson)); 
			response.put("ErrorCode", "BE_ERR_AIR_001");
			response.put("ErrorMsg", BE_ERR_AIR_001);
			return response.toString();
		}
		String prevOrder = paxDetailsObj.toString();
		JSONArray SSrCan = currPaxObj.getJSONArray(JSON_PROP_SPECIALREQUESTINFO);
		for(int m=0;m<SSrCan.length();m++)
		{
		JSONObject currSSRcAN = SSrCan.getJSONObject(m);
		String delSsr = currSSRcAN.getString(JSON_PROP_AIR_SSRCODE);
		JSONObject ssr = new JSONObject(paxDetailsObj.getSpecialRequests());
		JSONArray ssrArray = ssr.getJSONArray(JSON_PROP_SPECIALREQUESTINFO);
		for (int k = 0; k < ssrArray.length(); k++) 
		{
			JSONObject currentssr = ssrArray.getJSONObject(k);	
			   if(currentssr.getString(JSON_PROP_AIR_SSRCODE).equalsIgnoreCase(delSsr))
			   {
				   currentssr.put("status", "Cancel");
			   }
			
		}
		paxDetailsObj.setSpecialRequests(ssr.toString());
		}
		paxDetailsObj.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		paxDetailsObj.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		PassengerDetails updatedPaxDetails = savePaxDetails(paxDetailsObj, prevOrder);
		myLogger.info(String.format("Special Request cancelled for Air pax with paxid %s for request %s", updatedPaxDetails.getPassanger_id(),reqJson));
		}
			
			return "SUCCESS";
	}
	
	private String amendSSR(JSONObject reqJson, JSONObject currentSuppBookRefObj) throws BookingEngineDBException {
		
	   
	    JSONArray paxDetails = currentSuppBookRefObj.getJSONArray(JSON_PROP_PAXDETAILS);
		for(int j=0;j<paxDetails.length();j++)
		{
			JSONObject currPaxObj = paxDetails.getJSONObject(j);
			
	PassengerDetails paxDetailsObj = passengerRepository.findOne(currPaxObj.getString(JSON_PROP_PAXID));
	if(paxDetailsObj==null) {
		myLogger.info(String.format("Air cancel SSR request failed since paxID not found with id %s for req %s",currPaxObj.getString(JSON_PROP_AIR_PASSENGERID),reqJson)); 
		response.put("ErrorCode", "BE_ERR_AIR_001");
		response.put("ErrorMsg", BE_ERR_AIR_001);
		return response.toString();
	}
	String prevOrder = paxDetailsObj.toString();
	JSONArray SSrCan = currPaxObj.getJSONArray(JSON_PROP_SPECIALREQUESTINFO);
	JSONObject ssr = new JSONObject(paxDetailsObj.getSpecialRequests());
	JSONArray ssrArray = ssr.getJSONArray(JSON_PROP_SPECIALREQUESTINFO);
	for(int m=0;m<SSrCan.length();m++)
	{
	JSONObject currSSRcAN = SSrCan.getJSONObject(m);
	ssrArray.put(currSSRcAN);
	paxDetailsObj.setSpecialRequests(ssr.toString());
	}
	paxDetailsObj.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
	paxDetailsObj.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
	PassengerDetails updatedPaxDetails = savePaxDetails(paxDetailsObj, prevOrder);
	myLogger.info(String.format("Special Request cancelled for Air pax with paxid %s for request %s", updatedPaxDetails.getPassanger_id(),reqJson));
	}
		
		return "SUCCESS";
}
	
	private String updatePaxDetails(JSONObject reqJson, JSONObject currentSuppBookRefObj, String type) throws BookingEngineDBException {
		//AirOrders airOrder = airRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("entityId"));
		String prevOrder = null;
		
		JSONArray guestRoomJsonArray = currentSuppBookRefObj.getJSONArray(JSON_PROP_ENTITYIDS);
		for (int i = 0; i < guestRoomJsonArray.length(); i++) {
			
		JSONObject currenntPaxDetails = guestRoomJsonArray.getJSONObject(i);

		PassengerDetails guestDetails = null;	

		if(type.equals(JSON_PROP_ACCO_AMENDTYPE_ADDPAX)) {
			guestDetails = new PassengerDetails();
			prevOrder = "";
			guestDetails.setStatus("Added");
			guestDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));

			guestDetails.setTitle(currenntPaxDetails.getString(JSON_PROP_TITLE));
			guestDetails.setFirstName(currenntPaxDetails.getString(JSON_PROP_FIRSTNAME));
			guestDetails.setMiddleName(currenntPaxDetails.getString(JSON_PROP_MIDDLENAME));
			guestDetails.setLastName(currenntPaxDetails.getString(JSON_PROP_SURNAME));
			guestDetails.setBirthDate(currenntPaxDetails.getString(JSON_PROP_DOB));
			guestDetails.setIsLeadPax(currenntPaxDetails.getBoolean(JSON_PROP_ISLEADPAX));
			guestDetails.setPaxType(currenntPaxDetails.getString(JSON_PROP_PAX_TYPE));
			guestDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			guestDetails.setAddressDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());

	/*		if (currenntPaxDetails.getString(JSON_PROP_PAX_TYPE).equals(Pax_ADT))
				guestDetails.setDocumentDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ACCO_DOCUMENTDETAILS).toString());
			        guestDetails.setAncillaryServices(currenntPaxDetails.getJSONObject(JSON_PROP_ANCILLARYSERVICES).toString());*/
	                    //TODO: to be checked whether we are going to get the special requests for ACCO
			//guestDetails.setSpecialRequests(currenntPaxDetails.getJSONObject(JSON_PROP_SPECIALREQUESTS).toString());
	                    // TODO:change it to userID later
			guestDetails.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			
			guestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			PassengerDetails updatedPaxDetails = savePaxDetails(guestDetails,prevOrder);
			myLogger.info(String.format("Pax Details added for Air pax with paxid %s for request %s", updatedPaxDetails.getPassanger_id(),reqJson));
		}
		else if(type.equals(JSON_PROP_ACCO_AMENDTYPE_UPDATEPAX)) {
		
			guestDetails = passengerRepository.findOne(currenntPaxDetails.getString(JSON_PROP_ENTITYID));
			if(guestDetails == null)
			{
				myLogger.info(String.format("Air update PAX request failed since paxID not found with id %s for req %s",currenntPaxDetails.getString(JSON_PROP_PAXID),reqJson));
				response.put("ErrorCode", "BE_ERR_AIR_001");
				response.put("ErrorMsg", BE_ERR_AIR_001);
				return response.toString();
			}
			else
			{
			prevOrder = guestDetails.toString();
			guestDetails.setStatus("Updated");

			guestDetails.setTitle(currenntPaxDetails.getString(JSON_PROP_TITLE));
			guestDetails.setFirstName(currenntPaxDetails.getString(JSON_PROP_FIRSTNAME));
			guestDetails.setMiddleName(currenntPaxDetails.getString(JSON_PROP_MIDDLENAME));
			guestDetails.setLastName(currenntPaxDetails.getString(JSON_PROP_SURNAME));
			guestDetails.setBirthDate(currenntPaxDetails.getString(JSON_PROP_DOB));
			guestDetails.setIsLeadPax(currenntPaxDetails.getBoolean(JSON_PROP_ISLEADPAX));
			guestDetails.setPaxType(currenntPaxDetails.getString(JSON_PROP_PAX_TYPE));
			guestDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			guestDetails.setAddressDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());

	/*		if (currenntPaxDetails.getString(JSON_PROP_PAX_TYPE).equals(Pax_ADT))
				guestDetails.setDocumentDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ACCO_DOCUMENTDETAILS).toString());
			        guestDetails.setAncillaryServices(currenntPaxDetails.getJSONObject(JSON_PROP_ANCILLARYSERVICES).toString());*/
	                    //TODO: to be checked whether we are going to get the special requests for ACCO
			//guestDetails.setSpecialRequests(currenntPaxDetails.getJSONObject(JSON_PROP_SPECIALREQUESTS).toString());
	                    // TODO:change it to userID later
			guestDetails.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			
			guestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			PassengerDetails updatedPaxDetails = savePaxDetails(guestDetails,prevOrder);
			myLogger.info(String.format("Pax Details updated for Air pax with paxid %s for request %s", updatedPaxDetails.getPassanger_id(),reqJson));
			}
		}
		else  {
			String paxid = currenntPaxDetails.getString(JSON_PROP_ENTITYID);
			guestDetails = passengerRepository.findOne(paxid);
			if(guestDetails == null)
			{
				myLogger.info(String.format("Air cancel PAX request failed since paxID not found with id %s for req %s",paxid,reqJson));
				response.put("ErrorCode", "BE_ERR_AIR_001");
				response.put("ErrorMsg", BE_ERR_AIR_001);
				return response.toString();
			}
			else
			{
			prevOrder = guestDetails.toString();
			guestDetails.setStatus("Cancelled");

			//guestDetails.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			
			guestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			PassengerDetails updatedPaxDetails = savePaxDetails(guestDetails,prevOrder);
			myLogger.info(String.format("Pax cancelled for AIR pax with paxid %s for request %s", updatedPaxDetails.getPassanger_id(),reqJson));
		}
		}
		}
		return "SUCCESS";
	}
	
	
	
	
	public AmCl saveAirAmCl(AmCl amendEntry, String prevOrder) throws BookingEngineDBException {
		AmCl orderObj = null;
		try {
			orderObj = CopyUtils.copy(amendEntry, AmCl.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Air Amend Cancel order object : " + e);
			throw new BookingEngineDBException("Failed to save Air Amend Cancel order object");
		}
		return accoAmClRepository.saveOrder(orderObj, prevOrder);
	}
	
	public  Booking saveBookingOrder(Booking order, String prevOrder) throws BookingEngineDBException {
		Booking orderObj=null;
	try {
		orderObj = CopyUtils.copy(order, Booking.class);
		
	} catch (InvocationTargetException | IllegalAccessException e) {
		 myLogger.fatal("Error while saving Air Booking object : " + e);
		throw new BookingEngineDBException("Failed to save Air Booking object");
	}
    return bookingRepository.saveOrder(orderObj,prevOrder);
	}
	
	public AirOrders saveOrder(AirOrders order, String prevOrder) throws BookingEngineDBException {
		AirOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, AirOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Air order object : " + e);
			throw new BookingEngineDBException("Failed to save air order object");
		}
		return airRepository.saveOrder(orderObj,prevOrder);
	}
	
	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevPaxDetails) throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving passenger object : " + e);
			throw new BookingEngineDBException("Failed to save passenger object");
		}
		return passengerRepository.saveOrder(orderObj,prevPaxDetails);
	}



}
