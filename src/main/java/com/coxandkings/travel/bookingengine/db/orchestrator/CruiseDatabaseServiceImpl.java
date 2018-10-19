package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.BusOrders;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.CruiseAmCl;
import com.coxandkings.travel.bookingengine.db.model.CruiseOrders;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.CruiseAmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.CruiseDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("Cruise")
@Transactional(readOnly=false)
public class CruiseDatabaseServiceImpl implements DataBaseService,Constants,ErrorConstants {

	@Autowired
	@Qualifier("Cruise")
	private CruiseDatabaseRepository cruiseRepository;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("CruiseAmCl")
	private CruiseAmClRepository amClRepository;
	
	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingService;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	@Override
	public boolean isResponsibleFor(String product) {
		return "cruise".equalsIgnoreCase(product);
	}

	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	@Override
	public String processBookRequest(JSONObject bookRequestJson) throws JSONException, BookingEngineDBException{
		// TODO Auto-generated method stub
		
//		CruiseOrders cruiseOrders =	cruiseRepository.findByReservationIDandBookID("13009553","28394");
//		System.out.println(cruiseOrders.getSupplierID());
		
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
			
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("cruiseDetails")) {
			
			JSONObject orderJsonObj = new JSONObject(orderJson.toString());
			
			if(booking==null)
			booking = bookingService.processBookRequest(bookRequestJson,false);
			JSONArray paxDetailsJson = orderJsonObj.getJSONArray("Guests");
			
			CruiseOrders order = populateCruiseData((JSONObject) orderJson, paxDetailsJson, bookRequestHeader,booking);
			order.setProductSubCategory(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString("product"));
			saveOrder(order,"");
		}
		myLogger.info(String.format("Air Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));
		return "success";
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
	
	public CruiseOrders populateCruiseData(JSONObject cruiseDetailsJson,JSONArray paxDetailsJson, JSONObject bookRequestHeader, Booking booking) throws BookingEngineDBException {
		
		try {
			CruiseOrders order = new CruiseOrders();
			
			order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC));
			
			order.setBooking(booking);
			order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
			order.setStatus(OrderStatus.RQ.getProductStatus());
			
			order.setSupplierType("online");
			
			order.setSupplierID(cruiseDetailsJson.getString(JSON_PROP_SUPPREF));
			
			order.setRoe("");//TODO Need to Populate ROE in my request
			
			order.setSuppPaxTypeFares(cruiseDetailsJson.getJSONObject("suppPricingInfo").getJSONArray("suppPaxTypeFare").toString());
			order.setSupplierPrice(cruiseDetailsJson.getJSONObject("suppPricingInfo").getJSONObject("suppTotalInfo").getBigDecimal(JSON_PROP_AMOUNT).toString());
			order.setSupplierPriceCurrencyCode(cruiseDetailsJson.getJSONObject("suppPricingInfo").getJSONObject("suppTotalInfo").getString(JSON_PROP_CURRENCYCODE));
			
			order.setTotalPaxTypeFares(cruiseDetailsJson.getJSONObject("pricingInfo").getJSONArray("paxTypeFare").toString());
			order.setTotalPrice(cruiseDetailsJson.getJSONObject("pricingInfo").getJSONObject("totalInfo").getBigDecimal(JSON_PROP_AMOUNT).toString());
			order.setTotalPriceCurrencyCode(cruiseDetailsJson.getJSONObject("pricingInfo").getJSONObject("totalInfo").optString(JSON_PROP_CURRENCYCODE));
			
			order.setTotalPriceBaseFare(cruiseDetailsJson.getJSONObject("pricingInfo").getJSONObject("totalInfo").optJSONObject(JSON_PROP_BASEFARE).toString());
			order.setTotalPriceReceivables(cruiseDetailsJson.getJSONObject("pricingInfo").getJSONArray("clientEntityTotalCommercials").toString());
			
			Set<PassengerDetails> setPaxDetails = new HashSet<PassengerDetails>();
			setPaxDetails = readPassengerDetails(paxDetailsJson,order);
			
			JSONArray paxIds = new JSONArray();
			for(PassengerDetails pax:setPaxDetails ) {
				JSONObject paxJson = new JSONObject();
				paxJson.put(JSON_PROP_PAXID, pax.getPassanger_id());
				paxIds.put(paxJson);
			}
			order.setCruiseDetails(cruiseDetailsJson.optJSONObject("sailingInfo").toString());
			
			Set<ClientCommercial> clientComms =  new HashSet<ClientCommercial>();
			//CLIENT COMMS AND SUPPLIER COMMS POPULATE
	        clientComms = readClientCommercials(cruiseDetailsJson.getJSONObject("pricingInfo").getJSONArray("clientEntityTotalCommercials"),order);
			order.setPaxDetails(paxIds.toString());
			order.setClientCommercial(clientComms);
			
			Set<SupplierCommercial> suppComms = new HashSet<SupplierCommercial>();
			suppComms = readSuppCommercials(cruiseDetailsJson.getJSONObject("suppPricingInfo").getJSONArray("supplierCommercialsTotals"),order);
			order.setSuppcommercial(suppComms);
			
			order.setVoyageID(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedSailing").getString("voyageId"));
			order.setItineraryID(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedCategory").optJSONObject("selectedCabin").getString("itineraryId"));
			order.setSailingID(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedCategory").getString("sailingID"));
			
			order.setCabinNo(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedCategory").optJSONObject("selectedCabin").getString("CabinNumber"));
			order.setPricedCategoryCode(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedCategory").getString("pricedCategoryCode"));
			order.setFareCode(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedCategory").getString("fareCode"));
			order.setSailingStartDate(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedSailing").getString("start"));
			
			order.setShipCode(cruiseDetailsJson.optJSONObject("sailingInfo").optJSONObject("selectedSailing").optJSONObject("cruiseLine").optString("shipCode"));
			
			return order;
		} catch (Exception e) {
			// TODO: handle exception
			myLogger.fatal("Failed to populate Cruise Data "+ e);
			throw new BookingEngineDBException("Failed to populate Cruise Data");
		}
		
	}
	
	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, CruiseOrders order) {
		 
		Set<ClientCommercial> clientCommercialsSet =new HashSet<ClientCommercial>();
		ClientCommercial clientCommercials;
		
		for(int i=0;i<clientCommsJsonArray.length();i++)	{
			
			JSONObject clientCommsJson = clientCommsJsonArray.getJSONObject(i);
		
			String clientID = clientCommsJson.getString(JSON_PROP_CLIENTID);
			String parentClientID = clientCommsJson.getString(JSON_PROP_PARENTCLIENTID);;		
			String commercialEntityType = clientCommsJson.getString(JSON_PROP_COMMERCIALENTITYTYPE);;		
			String commercialEntityID = clientCommsJson.getString(JSON_PROP_COMMERCIALENTITYID);;
			
			JSONArray clientComms = clientCommsJson.getJSONArray("clientCommercials");
			
			for(int j=0;j<clientComms.length();j++) 
			{
				boolean companyFlag = (j==0)?true:false;
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
	
	private Set<SupplierCommercial> readSuppCommercials(JSONArray suppCommsJsonArray, CruiseOrders order) {
		
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
	
	private Set<PassengerDetails> readPassengerDetails(JSONArray paxJsonArray, CruiseOrders cruiseOrder) throws BookingEngineDBException {
		
		Set<PassengerDetails> paxDetailsSet = new HashSet<PassengerDetails>();
		PassengerDetails paxDetails;
		for(int i=0;i<paxJsonArray.length();i++)	{
		JSONObject currentPaxDetails = paxJsonArray.getJSONObject(i);
		
		paxDetails =new PassengerDetails();
		paxDetails.setTitle(currentPaxDetails.getJSONObject("guestName").getString("surName"));
		paxDetails.setFirstName(currentPaxDetails.getJSONObject("guestName").getString("givenName"));
		paxDetails.setIsLeadPax(currentPaxDetails.optBoolean(JSON_PROP_ISLEADPAX, false));
//		paxDetails.setIsLeadPax(false);
		paxDetails.setStatus("OnRequest");
		paxDetails.setMiddleName(currentPaxDetails.getJSONObject("guestName").getString("middleName"));
		paxDetails.setLastName(currentPaxDetails.getJSONObject("guestName").getString("surName"));
		paxDetails.setBirthDate(currentPaxDetails.getString("personBirthDate"));
		paxDetails.setPaxType("ADT");
		paxDetails.setGender(currentPaxDetails.getString(JSON_PROP_GENDER));
		paxDetails.setContactDetails(currentPaxDetails.getJSONObject("Telephone").toString());
		paxDetails.setAddressDetails(currentPaxDetails.getJSONObject("Address").toString());
        paxDetails.setDocumentDetails(currentPaxDetails.getJSONObject("TravelDocument").toString());
                
        if(currentPaxDetails.has("SelectedDining"))
		paxDetails.setSpecialRequests(currentPaxDetails.getJSONObject("SelectedDining").toString());
		
		//TODO:change it to userID later 
		paxDetails.setLastModifiedBy("");
		paxDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		//TODO: later check if we are going to get any paxKey in BE
		//paxDetails[i].setPaxkey(currenntPaxDetails.getString("paxKey") );
		savePaxDetails(paxDetails,"");
		paxDetailsSet.add(paxDetails);
		
		}
		return paxDetailsSet;
		
	}
	
	JSONObject response=new JSONObject();
	@Override
	public String processBookResponse(JSONObject bookResponseJson) {//TODO Check Air and Make changes
		// TODO Auto-generated method stub
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null){
			myLogger.warn(String.format("CRUISE Booking Response could not be populated since no bookings found for req with bookID %s","" ));
			response.put("ErrorCode","BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			return response.toString();
		}
		else{
			List<CruiseOrders> orders = cruiseRepository.findByBooking(booking);
			JSONArray suppBookRefArray = bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("bookResp");
			
			for(int i=0;i<suppBookRefArray.length();i++)
			{
				JSONObject suppBookObj = new JSONObject();
				suppBookObj=suppBookRefArray.getJSONObject(i);
				
				for(int j=0;j<orders.size();j++) {
					CruiseOrders order = orders.get(j);
					if(j==i)
					{
						order.setStatus(OrderStatus.OK.getProductStatus());
						order.setReservationID(suppBookObj.getJSONArray("reservationID").getJSONObject(0).getString("id"));
						order.setBookingDateTime(new Date().toString());
						order.setBookingCompanyName(suppBookObj.getJSONArray("reservationID").getJSONObject(0).getString("companyName"));
						order.setBookingPayment(suppBookObj.getJSONObject("bookingPayment").toString());
						
						cruiseRepository.save(order);
					}
				}
			}
			myLogger.info(String.format("CRUISE Booking Response populated successfully for req with bookID %s = %s", "",bookResponseJson.toString()));
			return "SUCCESS";
		}
	}
	
	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevPaxDetails) throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving passenger object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save passenger object");
		}
		return passengerRepository.saveOrder(orderObj,prevPaxDetails);
	}

	@Override
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException {
		// TODO Auto-generated method stub
		
		JSONObject cancelRqJson = reqJson.getJSONObject("requestBody");
		
		String type = cancelRqJson.getString("type");
		CruiseAmCl cancelEntry = new CruiseAmCl();
		cancelEntry.setEntityName(cancelRqJson.getString("entityName"));
		cancelEntry.setRequestType(cancelRqJson.getString("requestType"));
		cancelEntry.setSupplierCharges("0");
		cancelEntry.setDescription(type);
		cancelEntry.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
		cancelEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		cancelEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		cancelEntry.setStatus(OrderStatus.RQ.getProductStatus());
		
		cancelEntry.setSupplierID(cancelRqJson.getString("supplierRef"));
		
//		saveAmCl(cancelEntry, "");
		switch(type)
        {
        	case JSON_PROP_CRUISE_CANCELTYPE_FULLCANCEL:
                return fullCancel(cancelEntry,reqJson,cancelRqJson);
        	case JSON_PROP_CRUISE_AMENDTYPE_UPDATEPAXADDRESS:
        		return updatePaxAddress(cancelEntry,reqJson,cancelRqJson);
            default:
                return "no match for cancel/amend type";
        }
	}

	private String updatePaxAddress(CruiseAmCl cruiseAmCl,JSONObject reqJson,JSONObject cancelRqJson) throws BookingEngineDBException {
		
		CruiseOrders order = cruiseRepository.findByReservationID(cancelRqJson.getJSONObject("reservationID").getString("id"));
		
		JSONArray paxArr = new JSONArray(order.getPaxDetails());
		
		for(int i=0;i<paxArr.length();i++)
		{
			JSONObject paxJsonObj =	paxArr.getJSONObject(i);
			String paxStr = paxJsonObj.getString("paxID");
			
			PassengerDetails passengerDetails =	passengerRepository.findOne(paxStr);
			
			if(passengerDetails.getIsLeadPax())
			{
				JSONObject guestDtlJson = reqJson.getJSONObject("requestBody").getJSONObject("guestDetails").getJSONObject("address");
				
				JSONObject addressDtlsJson = new JSONObject(passengerDetails.getAddressDetails());
				
				addressDtlsJson.put("AddressLine", guestDtlJson.getString("addressLine"));
				addressDtlsJson.put("CityName", guestDtlJson.getString("cityName"));
				addressDtlsJson.put("PostalCode", guestDtlJson.getString("postalCode"));
				addressDtlsJson.put("StateProv", guestDtlJson.getString("stateProv"));
				
				passengerDetails.setAddressDetails(addressDtlsJson.toString());
				
				JSONObject entityIDJson = new JSONObject();
				entityIDJson.put("entityID", paxStr);
				
				cruiseAmCl.setEntityID(entityIDJson.toString());
				cruiseAmCl.setOrderID(order.getId());
				saveAmCl(cruiseAmCl, "");
				
				savePaxDetails(passengerDetails, "");
				return "SUCESS";
			}
		}
		return "FAILED";
	}
	
	private String fullCancel(CruiseAmCl cruiseAmCl, JSONObject reqJson,JSONObject cancelRqJson) throws BookingEngineDBException {
		
//		CruiseOrders order = cruiseRepository.findOne(cancelRqJson.getString("entityId"));
		CruiseOrders order = cruiseRepository.findByReservationID(cancelRqJson.getJSONObject("reservationID").getString("id"));
		String prevOrder = order.toString();
		order.setStatus("orderCancelled");
		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		JSONObject entityIDJson = new JSONObject();
		entityIDJson.put("entityID", order.getId());
		
		cruiseAmCl.setEntityID(entityIDJson.toString());
		cruiseAmCl.setOrderID(order.getId());
		saveAmCl(cruiseAmCl, "");
		
		saveOrder(order, prevOrder);
		return "SUCCESS";
	}
	
	@Override
	public String processAmClResponse(JSONObject resJson) throws BookingEngineDBException {
		// TODO Auto-generated method stub
		
		String type = resJson.getJSONObject("responseBody").getString("type");
		
//		List<CruiseAmCl> amendEntries  = amClRepository.findforResponseUpdate(reqJson.getJSONObject(JSON_PROP_RESBODY).getString("entityName"),reqJson.getJSONObject(JSON_PROP_RESBODY).getString("entityId"), reqJson.getJSONObject(JSON_PROP_RESBODY).getString("type"), reqJson.getJSONObject(JSON_PROP_RESBODY).getString("requestType"));
//		Booking booking = bookingRepository.findOne(resJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
//		List<CruiseOrders> orders = cruiseRepository.findByBooking(booking);
		
		CruiseOrders order = cruiseRepository.findByReservationID(resJson.getJSONObject("responseBody").getJSONObject("reservationID").getString("id"));
		
//		for(CruiseOrders order : orders)
		{
			JSONObject testObj = new JSONObject();
			testObj.put("entityID", order.getId());
			
			List<CruiseAmCl> amendEntries = amClRepository.findforResponseUpdate1(resJson.getJSONObject("responseBody").getString("entityName"), testObj.toString(), resJson.getJSONObject("responseBody").getString("requestType"), resJson.getJSONObject("responseBody").getString("type"));
			
			if(amendEntries.size()==0) {
				//TODO: handle this before it goes in prod
				System.out.println("no amend entry found. Request might not have been populated");
			}
			/*else if(amendEntries.size()>1) {
				//TODO: handle this before it goes in prod
				System.out.println("multiple amend entries found. Dont know which one to update");
			}*/
			else if(type.equalsIgnoreCase(JSON_PROP_CRUISE_AMENDTYPE_UPDATEPAXADDRESS))
			{
				CruiseAmCl amendEntry = getLatestAmendEntry(amendEntries);
				
				amendEntry.setStatus("Confirmed");
				amendEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
				amendEntry.setLastModifiedBy(resJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));
				
				saveAmCl(amendEntry, "");
			}
			else {
//				CruiseAmCl amendEntry = amendEntries.get(0);	
				
				CruiseAmCl amendEntry = getLatestAmendEntry(amendEntries);
				
				String prevOrder = amendEntry.toString();
				amendEntry.setCompanyCharges(resJson.getJSONObject(JSON_PROP_RESBODY).optString("companyCharges"));
				amendEntry.setSupplierCharges(resJson.getJSONObject(JSON_PROP_RESBODY).optString("supplierCharges"));
				amendEntry.setSupplierChargesCurrencyCode(resJson.getJSONObject(JSON_PROP_RESBODY).optString("supplierChargesCurrencyCode"));
				amendEntry.setCompanyChargesCurrencyCode(resJson.getJSONObject(JSON_PROP_RESBODY).optString("companyChargesCurrencyCode"));
				amendEntry.setStatus("Confirmed");
				amendEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
				amendEntry.setLastModifiedBy(resJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));
				amendEntry.setCancelRules(resJson.getJSONObject("responseBody").getJSONObject("cancelInfo").getJSONArray("cancelRules").toString());
				amendEntry.setCancelInfoIDs(resJson.getJSONObject("responseBody").getJSONObject("cancelInfo").getJSONArray("uniqueID").toString());
			
				//TODO: also set the currency codes and breakups before saving
				saveAmCl(amendEntry, prevOrder);
			}
		}
		return "SUCCESS";
	}

	private CruiseAmCl getLatestAmendEntry(List<CruiseAmCl> amendEntries) {
		CruiseAmCl amendEntry = null;
		ZonedDateTime dateTime = null;
		for(CruiseAmCl amCl : amendEntries)
		{
			if(dateTime!=null)
			{
				if(amCl.getCreatedAt().isAfter(dateTime))
				{
					amendEntry = amCl;
				}
			}
			else
			{
				amendEntry = amCl;
				dateTime = amCl.getCreatedAt();
			}
		}
		return amendEntry;
	}
	
	public CruiseAmCl saveAmCl(CruiseAmCl currentOrder, String prevOrder) throws BookingEngineDBException {
		CruiseAmCl orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, CruiseAmCl.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Cruise Cancel order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save Cruise Cancel order object");
		}
		return amClRepository.saveOrder(orderObj, prevOrder);
	}
	
}
