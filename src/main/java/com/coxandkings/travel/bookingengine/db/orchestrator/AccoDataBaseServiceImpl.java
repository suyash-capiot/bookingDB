package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

import com.coxandkings.travel.bookingengine.db.enums.BookingAttribute;
import com.coxandkings.travel.bookingengine.db.enums.BookingStatus;
import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.AccoRoomDetails;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.AccoDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.AccoRoomRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;



@Service
@Qualifier("Acco")
@Transactional(readOnly = false)
public class AccoDataBaseServiceImpl implements DataBaseService,Constants,ErrorConstants,CancelAmendTypes {

	@Autowired
	@Qualifier("Acco")
	private AccoDatabaseRepository accoRepository;
	
	@Autowired
	@Qualifier("AccoAmCl")
	private AmClRepository accoAmClRepository;
	
	@Qualifier("AccoRoom")
	@Autowired
	private AccoRoomRepository roomRepository;

	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingService;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	JSONObject response=new JSONObject(); 
	
	public boolean isResponsibleFor(String product) {
		return "ACCO".equalsIgnoreCase(product);
	}

	public String processBookRequest(JSONObject bookRequestJson) throws BookingEngineDBException {

		try {
		
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null)
		booking = bookingService.processBookRequest(bookRequestJson,false);
		
		
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_ACCO_ACCOMODATIONINFO)) {

			AccoOrders order = populateAccoData((JSONObject) orderJson,
					booking,bookRequestJson);
			saveAccoOrder(order,"");
			
		}
		myLogger.info(String.format("Acco Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));
		return "success";
		}
		catch(Exception e)
		{
			myLogger.info(e.getMessage()+"for AccBookRequest "+bookRequestJson);
			throw new BookingEngineDBException(e.getMessage());
		}
	}

	public AccoOrders populateAccoData(JSONObject accoInfo, Booking booking, JSONObject bookRequestJson) throws BookingEngineDBException {

		try {
		AccoOrders order = new AccoOrders();
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		order.setProductSubCategory(accoInfo.getString(JSON_PROP_ACCOMODATIONSUBTYPE));
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setBooking(booking);
		order.setLastModifiedBy(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
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
		order.setStatus(OrderStatus.RQ.getProductStatus());
		
		order.setSupplierID(accoInfo.getString(JSON_PROP_SUPPREF));
		order.setOperationType("insert");
		order.setRoe(accoInfo.optString(JSON_PROP_ROE));
		order.setSupplierPrice(accoInfo.getJSONObject(JSON_PROP_SUPPLIERBOOKINGPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setSupplierPriceCurrencyCode(accoInfo.getJSONObject(JSON_PROP_SUPPLIERBOOKINGPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
		order.setTotalPrice(accoInfo.getJSONObject(JSON_PROP_BOOKINGPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setTotalPriceCurrencyCode(accoInfo.getJSONObject(JSON_PROP_BOOKINGPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
        if(accoInfo.getJSONObject(JSON_PROP_BOOKINGPRICEINFO).has(JSON_PROP_DISCOUNTS))
		order.setDiscounts(accoInfo.getJSONObject(JSON_PROP_BOOKINGPRICEINFO).getJSONObject(JSON_PROP_DISCOUNTS).toString());
        if(accoInfo.getJSONObject(JSON_PROP_BOOKINGPRICEINFO).has(JSON_PROP_INCENTIVES))
        order.setIncentives(accoInfo.getJSONObject(JSON_PROP_BOOKINGPRICEINFO).getJSONObject(JSON_PROP_INCENTIVES).toString());
        if(accoInfo.getJSONObject(JSON_PROP_BOOKINGPRICEINFO).has(JSON_PROP_COMPANYTAXES))
        order.setCompanyTaxes(accoInfo.getJSONObject(JSON_PROP_BOOKINGPRICEINFO).getJSONObject(JSON_PROP_COMPANYTAXES).toString());	
		order.setSuppPriceTaxes(accoInfo.getJSONObject(JSON_PROP_BOOKINGPRICEINFO).getJSONObject(JSON_PROP_TAXES).toString());
		order.setTotalPriceTaxes(accoInfo.getJSONObject(JSON_PROP_BOOKINGPRICEINFO).getJSONObject(JSON_PROP_TAXES).toString());
		
		
		//TODO: for now populating all suppliers as offline. Later have  this logic after operations is concrete.
		order.setSupplierType("online");
		

		Set<AccoRoomDetails> setRoomDetails = new HashSet<AccoRoomDetails>();
		setRoomDetails = readRoomDetails(accoInfo, order, booking);
		Set<SupplierCommercial> setSuppComms = new HashSet<SupplierCommercial>();
		setSuppComms = readSuppCommercials(accoInfo, order);
		
		
		Set<ClientCommercial> setClientComms = new HashSet<ClientCommercial>();
		setClientComms = readClientCommercials(accoInfo.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS), order);
		
		order.setClientCommercial(setClientComms);
		order.setSuppcommercial(setSuppComms);
		
		order.setRoomDetails(setRoomDetails);

		return order;
		}
		catch(Exception e)
		{
			
			myLogger.fatal("Failed to populate Acco Data "+ e);
			throw new BookingEngineDBException("Failed to populate Acco Data");
		}
	}

	private Set<AccoRoomDetails> readRoomDetails(JSONObject requestBody, AccoOrders accoOrder, Booking booking) throws BookingEngineDBException {

		JSONArray roomConfigJsonArray = requestBody.getJSONArray(JSON_PROP_ACCO_ROOMCONFIG);
		Set<AccoRoomDetails> roomDetailsSet = new HashSet<AccoRoomDetails>();
		AccoRoomDetails roomDetails;

		for (int i = 0; i < roomConfigJsonArray.length(); i++) {
			roomDetails = new AccoRoomDetails();
                        JSONObject currentRoomDetails = roomConfigJsonArray.getJSONObject(i);
			
                        roomDetails.setCheckInDate(requestBody.getString(JSON_PROP_ACCO_CHKIN));
			roomDetails.setCheckOutDate(requestBody.getString(JSON_PROP_ACCO_CHKOUT));
			roomDetails.setCityCode(requestBody.getString(JSON_PROP_CITYCODE));
			roomDetails.setCountryCode(requestBody.getString(JSON_PROP_COUNTRYCODE));
			roomDetails.setSupplierName(requestBody.getString(JSON_PROP_SUPPREF));
			roomDetails.setStatus(OrderStatus.RQ.getProductStatus());
			JSONArray roomDetailsArr = currentRoomDetails.optJSONArray(JSON_PROP_POLICIES);
			if(roomDetailsArr!=null) {
			roomDetails.setCancellationPolicies(currentRoomDetails.optJSONArray(JSON_PROP_POLICIES).toString());
			}
			roomDetails.setMealCode(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_MEALINFO).getString(JSON_PROP_ACCO_MEALCODE));
			roomDetails.setMealName(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_MEALINFO).getString(JSON_PROP_ACCO_MEALNAME));
			roomDetails.setRoomCategoryID(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_ROOMTYPEINFO)
					.getString(JSON_PROP_ACCO_ROOMCATEGORYCODE));
			roomDetails.setRoomCategoryName(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_ROOMTYPEINFO)
					.getString(JSON_PROP_ACCO_ROOMCATEGNAME));
			roomDetails.setRoomRef(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_ROOMTYPEINFO).getString(JSON_PROP_ACCO_ROOMREF));
			roomDetails.setRoomTypeCode(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_ROOMTYPEINFO)
					.getString(JSON_PROP_ACCO_ROOMTYPECODE));
			roomDetails.setRoomTypeName(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_ROOMTYPEINFO)
					.getString(JSON_PROP_ACCO_ROOMTYPENAME));
			roomDetails.setHotelCode(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_HOTELINFO).getString(JSON_PROP_ACCO_HOTELCODE));
			roomDetails.setHotelName(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_HOTELINFO).getString(JSON_PROP_ACCO_HOTELNAME));
			roomDetails.setRatePlanName(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_RATEPLANINFO)
					.getString(JSON_PROP_ACCO_RATEPLANNAME));
			roomDetails.setRatePlanCode(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_RATEPLANINFO)
					.getString(JSON_PROP_ACCO_RATEPLANCODE));
			roomDetails.setRatePlanRef(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_RATEPLANINFO)
					.getString(JSON_PROP_ACCO_RATEPLANREF));
			roomDetails.setBookingRef(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMINFO).getJSONObject(JSON_PROP_ACCO_RATEPLANINFO).getString(JSON_PROP_ACCO_BOOKINGREF));
			roomDetails.setOccupancyInfo(currentRoomDetails.getJSONArray(JSON_PROP_OCCUPANCYINFO).toString());
			Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
			setGuestDetails = readGuestDetails(currentRoomDetails, roomDetails);
			
			JSONArray paxIds = new JSONArray();
			for(PassengerDetails pax:setGuestDetails ) {
				JSONObject paxJson = new JSONObject();
				paxJson.put(JSON_PROP_PAXID, pax.getPassanger_id());
				paxIds.put(paxJson);
			}

			roomDetails.setPaxDetails(paxIds.toString());
			
			roomDetails.setTotalPrice(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
			if(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).has(JSON_PROP_COMPANYTAXES))
			roomDetails.setCompanyTaxes(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).getJSONObject(JSON_PROP_COMPANYTAXES).toString());
			if(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).has(JSON_PROP_INCENTIVES))
				roomDetails.setCompanyTaxes(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).getJSONObject(JSON_PROP_INCENTIVES).toString());
			if(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).has(JSON_PROP_DISCOUNTS))
				roomDetails.setCompanyTaxes(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).getJSONObject(JSON_PROP_DISCOUNTS).toString());
			roomDetails.setTotalPriceCurrencyCode(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
			roomDetails.setTotalTaxBreakup(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMPRICEINFO).getJSONObject(JSON_PROP_TAXES).toString());
			roomDetails.setSupplierPrice(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMSUPPLIERPRICEINFO).getBigDecimal(JSON_PROP_AMOUNT).toString());
			roomDetails.setSupplierPriceCurrencyCode(
					currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMSUPPLIERPRICEINFO).getString(JSON_PROP_CURRENCYCODE));
			
			roomDetails.setSupplierTaxBreakup(currentRoomDetails.getJSONObject(JSON_PROP_ACCO_ROOMSUPPLIERPRICEINFO).getJSONObject(JSON_PROP_TAXES)
					.toString());
			
			roomDetails.setSuppCommercials(currentRoomDetails.getJSONArray(JSON_PROP_SUPPCOMM).toString());
			roomDetails.setClientCommercials(currentRoomDetails.getJSONArray(JSON_PROP_CLIENTCOMMSENTITYDETAILS).toString());
            roomDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			
			roomDetails.setAccoOrders(accoOrder);
			
			roomDetailsSet.add(roomDetails);
		}
		return roomDetailsSet;
	}

	private Set<PassengerDetails> readGuestDetails(JSONObject roomConfigJson, AccoRoomDetails roomDetails) throws BookingEngineDBException {

		JSONArray guestRoomJsonArray = roomConfigJson.getJSONArray(JSON_PROP_PAXINFO);

		Set<PassengerDetails> guestDetailsSet = new HashSet<PassengerDetails>();
		PassengerDetails guestDetails;
		for (int i = 0; i < guestRoomJsonArray.length(); i++) {
			JSONObject currenntPaxDetails = guestRoomJsonArray.getJSONObject(i);
			guestDetails = new PassengerDetails();
			
			//TODO: Put a logic to create the primary key for pax
			
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
			guestDetails.setLastModifiedBy("");
			guestDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			guestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			savePaxDetails(guestDetails,"");

			guestDetailsSet.add(guestDetails);

		}
		return guestDetailsSet;
	}

	private Set<SupplierCommercial> readSuppCommercials(JSONObject accoInfoJson, AccoOrders order) {

		JSONArray suppCommsJsonArray = accoInfoJson.getJSONArray(JSON_PROP_SUPPCOMMTOTALS);
		Set<SupplierCommercial> suppCommercialsSet = new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;
		for (int i = 0; i < suppCommsJsonArray.length(); i++) {
			JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);

			suppCommercials = new SupplierCommercial();
			suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
			suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
			suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
			suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));
			
			suppCommercials.setProduct(JSON_PROP_PRODUCTACCO);
			suppCommercials.setOrder(order);
			suppCommercialsSet.add(suppCommercials);

		}
		return suppCommercialsSet;
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, AccoOrders order) {
		 
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

		clientCommercials.setProduct(JSON_PROP_PRODUCTACCO);
		clientCommercials.setOrder(order);
		clientCommercialsSet.add(clientCommercials);
		}
		}
		return clientCommercialsSet;
		}

	public String processBookResponse(JSONObject bookResponseJson) throws BookingEngineDBException {

		//TODO: We need to put logic to update status for booking based on the statuses of individual products.
		
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		if(booking==null)
		{
			myLogger.warn(String.format("Acco Booking Response could not be populated since no bookings found for req with bookID %s", bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID)));
			response.put("ErrorCode","BE_ERR_ACCO_004");
			response.put("ErrorMsg", BE_ERR_ACCO_004);
			return response.toString();
		}
		else
		{
		JSONArray accoInfoArray = bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_ACCO_ACCOMODATIONINFO);
		for(int k=0;k<accoInfoArray.length();k++)
		{
		JSONObject currAccoInfoObj = bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_ACCO_ACCOMODATIONINFO).getJSONObject(k);
		List<AccoOrders> orders = accoRepository.findByBooking(booking);
		if(orders.size()==0)
		{
			myLogger.warn(String.format("Acco Booking Response could not be populated since no acco orders found for req with bookID %s", bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID)));
			response.put("ErrorCode", "BE_ERR_ACCO_005");
			response.put("ErrorMsg", BE_ERR_ACCO_005);
			return response.toString();
		}
		else
		{
		int count =0;
		for(int l=0;l<orders.size();l++) {
			if(l==k) {
	        AccoOrders order=orders.get(l);	
			String prevOrder = order.toString();
			List<AccoRoomDetails> list = null;
			if(currAccoInfoObj.opt(JSON_PROP_SUPPLIERRESERVATIONID).equals(""))
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
			order.setSuppierReservationId(currAccoInfoObj.optString(JSON_PROP_SUPPLIERRESERVATIONID));
			order.setSupplierReferenceId(currAccoInfoObj.optString(JSON_PROP_SUPPLIERREFERENCEID));
			order.setClientReferenceId(currAccoInfoObj.optString(JSON_PROP_CLIENTREFERENCEID));
			order.setSupplierCancellationId(currAccoInfoObj.optString(JSON_PROP_SUPPLIERCANCELLATIONID));
			count++;
			order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			JSONArray suppRoomRefArray=currAccoInfoObj.getJSONArray(JSON_PROP_ACCO_SUPPLIERROOMREFERENCES);
	        for(int i=0;i<suppRoomRefArray.length();i++) {
	        	JSONObject currSuppRoomRefObject = suppRoomRefArray.getJSONObject(i);
	        	list = new ArrayList<AccoRoomDetails>(order.getRoomDetails());
	        	Collections.sort(list);
	        	for(int j=0;j<list.size();j++) {
	        		if(i==j) {
	        		   AccoRoomDetails room=list.get(j);
	        		   room.setStatus(OrderStatus.OK.getProductStatus());
	        		   room.setSupplierRoomIndex(currSuppRoomRefObject.getString(JSON_PROP_ACCO_SUPPLIERROOMINDEX));
	        		   JSONArray paxDetailsArr = new JSONArray(room.getPaxDetails());
	        		   for(int m=0;m<paxDetailsArr.length();m++)
	        		   {
	        			   JSONObject currPaxDetObj=paxDetailsArr.getJSONObject(m);
	        			   PassengerDetails currPax=passengerRepository.findOne(currPaxDetObj.getString(JSON_PROP_PAXID));
	        			   String prevPaxDetails=currPax.toString();
	        			   currPax.setStatus(OrderStatus.OK.getProductStatus());
	        			   savePaxDetails(currPax, prevPaxDetails);
	        		   }
	        	}
	        	}
	       }
	      
	        if(suppRoomRefArray.length()>0)
	        order.setRoomDetails(new HashSet<AccoRoomDetails>(list));
			}
		     saveAccoOrder(order, prevOrder);
		}
		}
		}
		myLogger.info(String.format("Acco Booking Response populated successfully for req with bookID %s = %s", bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID),bookResponseJson.toString()));
		
		}
		return "SUCCESS";
		}
	}

	
	

	//This is to process cancel/amend request for Acco
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException {

		String type = reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_TYPE);
		//TODO: Check if order level status needs to be updated for each request
		
		AmCl amendEntry = new AmCl();
		
		amendEntry.setOrderID(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
		amendEntry.setEntityName(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ENTITYNAME));
		amendEntry.setRequestType(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_REQUESTTYPE));
		amendEntry.setSupplierCharges("0");
		amendEntry.setDescription(type);
		amendEntry.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		amendEntry.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		amendEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		amendEntry.setStatus(OrderStatus.RQ.getProductStatus());
		AccoOrders order = accoRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
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
		if(type.equalsIgnoreCase(JSON_PROP_ACCO_CANCELTYPE_FULLCANCEL))
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
		String result;
		switch(type)
        {
           case JSON_PROP_ACCO_AMENDTYPE_ADDPAX:
                result= updatePaxDetails(reqJson,type);
                break;
        	case JSON_PROP_ACCO_AMENDTYPE_CANCELPAX:
        		  result = cancelPaxDetails(reqJson,type);
        		  break;
            case JSON_PROP_ACCO_AMENDTYPE_UPDATEPAX:
            	result =updatePaxDetails(reqJson,type);
            	break;
            case JSON_PROP_ACCO_AMENDTYPE_UPDATEROOM:
                 result =updateRoom(reqJson,type);
                 break;
            /*case JSON_PROP_ACCO_CANNCELTYPE_UPDATESTAYDATES:
                 return updateStayDates(reqJson); */
        	case  JSON_PROP_ACCO_CANCELTYPE_CANCELROOM :
           result = updateRoom(reqJson,type);
           break;
        	case JSON_PROP_ACCO_CANCELTYPE_FULLCANCEL:
                result = fullCancel(reqJson);
              break;  
        	case JSON_PROP_ACCO_AMENDTYPE_CHANGEPERIODOFSTAY:
        		result = changePeriodOfStay(reqJson);
        		break;
            default:
            	response.put("ErrorCode", "BE_ERR_005");
            	response.put("ErrorMsg", BE_ERR_005);
            	myLogger.info(String.format("Update type %s for req %s not found", type, reqJson.toString()));
                result= (response.toString());
                break;
        }
		amendEntry.setEntityID(reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_ENTITYIDS).toString());
		saveAccoOrder(order, "");
		saveAccoAmCl(amendEntry, "");
		myLogger.info(String.format("Acco AMCL Request populated successfully for req  %s",reqJson));
		return result;
		 
		 
		
	}
	
	private String changePeriodOfStay(JSONObject reqJson) throws BookingEngineDBException {
		AccoOrders order = accoRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
		if(order == null)
		{
		  myLogger.warn("Requested EntityID not found"+reqJson );
		  throw new BookingEngineDBException("Requested entityId Not found");	
		}
		else
		{
		String prevOrder = order.toString();
        Set<AccoRoomDetails> roomDetails = order.getRoomDetails();
        for(AccoRoomDetails currRoom : roomDetails)
        {
        	AccoRoomDetails room = roomRepository.findOne(currRoom.getId());
        	String prevRoom = room.toString();
        	room.setCheckInDate(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ACCO_CHKIN));
        	room.setCheckOutDate(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ACCO_CHKOUT));
        	saveRoomDetails(room, prevRoom);
        }
		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		saveAccoOrder(order, prevOrder);
		}
		myLogger.info(String.format("Change Period of Stay of Acco Order done for req %s",reqJson.toString()));
		return "success";
	}

	private String cancelPaxDetails(JSONObject reqJson, String type) throws BookingEngineDBException {
		AccoRoomDetails room = roomRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_ENTITYIDS).getJSONObject(0).getString(JSON_PROP_ENTITYID));
		if(room == null)
		{
		  myLogger.warn("Requested EntityID not found"+reqJson );
		  throw new BookingEngineDBException("Requested entityId Not found");	
		}
		else
		{
		String prevOrder;
		
		JSONArray guestRoomJsonArray = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAXINFO);
		JSONArray roomJsonArray = new JSONArray(room.getPaxDetails());
		JSONArray newResGuestArray=new JSONArray();
		int k=roomJsonArray.length();
		int l=guestRoomJsonArray.length(); 
		for (int i = 0; i < k; i++)
        {
            int j;
             
            for (j = 0; j < l; j++)
                if ((roomJsonArray.getJSONObject(i).getString(JSON_PROP_PAXID)).equalsIgnoreCase(guestRoomJsonArray.getJSONObject(j).getString(JSON_PROP_PAXID)))
                    break;
 
            if (j == l)
            	newResGuestArray.put(roomJsonArray.getJSONObject(i));
        }
		
	    JSONArray entityIDArr=reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_ENTITYIDS);
	    JSONArray paxIDs= new JSONArray();
		System.out.println(newResGuestArray);
		for (int i = 0; i < newResGuestArray.length(); i++) {
			JSONObject currenntPaxDetails = newResGuestArray.getJSONObject(i);
        JSONObject newPaxIDObj=new JSONObject();
        newPaxIDObj.put(JSON_PROP_PAXID, currenntPaxDetails.getString(JSON_PROP_PAXID));
        paxIDs.put(newPaxIDObj);
		PassengerDetails guestDetails;	
		guestDetails = passengerRepository.findOne(currenntPaxDetails.getString(JSON_PROP_PAXID));
		if(guestDetails == null)
		{
			myLogger.info(String.format("Acco cancel PAX request failed since paxID not found with id %s for req %s",currenntPaxDetails.getString(JSON_PROP_PAXID),reqJson));
			response.put("ErrorCode", "BE_ERR_ACCO_001");
			response.put("ErrorMsg", BE_ERR_ACCO_001);
			return response.toString();
		}
		else
		{
		prevOrder = guestDetails.toString();
		guestDetails.setStatus("Cancelled");
        guestDetails.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		guestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		PassengerDetails updatedPaxDetails = savePaxDetails(guestDetails,prevOrder);
		myLogger.info(String.format("Pax cancelled for Acco pax with paxid %s for request %s", updatedPaxDetails.getPassanger_id(),reqJson));
	}
	}
		entityIDArr.getJSONObject(0).put("paxIDs", paxIDs);
	}	
		
		return "success";
	}

	//TODO: for add pax how will we get entity ID
	public String processAmClResponse(JSONObject reqJson) throws Exception {
		
		List<AmCl> amendEntries  = accoAmClRepository.findforResponseUpdate(reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_ORDERID),reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_ENTITYNAME),reqJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("entityIDs").toString(), reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_TYPE), reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_REQUESTTYPE));
		
		if(amendEntries.size()==0) {
			//TODO: handle this before it goes in prod
			  myLogger.warn("no amend entry found. Request might not have been populated"+reqJson );
			System.out.println("no amend entry found. Request might not have been populated");
			throw new BookingEngineDBException("no amend entry found. Request might not have been populated");
		}
		
		else if(amendEntries.size()>1) {
			//TODO: handle this before it goes in prod
			  myLogger.warn("multiple amend entries found. Dont know which one to update"+reqJson );
			System.out.println("multiple amend entries found. Dont know which one to update");
			throw new BookingEngineDBException("multiple amend entries found. Dont know which one to update");
		}
		
		else {
		AccoOrders order = accoRepository.findOne(reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_ORDERID));
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
		reqJson.getJSONObject(JSON_PROP_RESBODY).put("bookID",order.getBooking().getBookID());
		if(reqJson.getJSONObject(JSON_PROP_RESBODY).has(JSON_PROP_ERRCODE))
		{
			if(reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_TYPE).equalsIgnoreCase(JSON_PROP_ACCO_CANCELTYPE_FULLCANCEL))
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
			if(reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_TYPE).equalsIgnoreCase(JSON_PROP_ACCO_CANCELTYPE_FULLCANCEL))
			{
				   order.setStatus(OrderStatus.XL.getProductStatus());	
			}
			else
			{
				bookingAttributeObj.put(BookingAttribute.AMENDED.toString(), BookingAttribute.AMENDED.getBookingAttribute());
			    bookingAttributeArray.put(bookingAttributeObj);
				order.setBookingAttribute(bookingAttributeArray.toString());
		     }
	
		saveAccoOrder(order, "");
		AmCl amendEntry = amendEntries.get(0);	
		String prevOrder = amendEntry.toString();
		amendEntry.setCompanyCharges(reqJson.getJSONObject(JSON_PROP_RESBODY).get(JSON_PROP_COMPANYCHARGES).toString());
		amendEntry.setSupplierCharges(reqJson.getJSONObject(JSON_PROP_RESBODY).get(JSON_PROP_SUPPCHARGES).toString());
		amendEntry.setSupplierChargesCurrencyCode(reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_SUPPCHARGESCURRENCYCODE));
		amendEntry.setCompanyChargesCurrencyCode(reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_COMPANYCHARGESCURRENCYCODE));
		amendEntry.setStatus(OrderStatus.OK.getProductStatus());
		amendEntry.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		amendEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_RESHEADER).getString(JSON_PROP_USERID));
		
		//TODO: also set the currency codes and breakups before saving
		saveAccoAmCl(amendEntry, prevOrder);
		}
		myLogger.info(String.format("AMCL Response populated successfully for req %s",reqJson.toString()));
		//AmendNotificationToOps.sendAmendNotificationToOps(reqJson);
		return "SUCCESS";
		}	
	}
	
	private String fullCancel(JSONObject reqJson) throws BookingEngineDBException {
		AccoOrders order = accoRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
		if(order == null)
		{
		  myLogger.warn("Requested EntityID not found"+reqJson );
		  throw new BookingEngineDBException("Requested entityId Not found");	
		}
		else
		{
		String prevOrder = order.toString();
		order.setStatus(OrderStatus.RXL.getProductStatus());
		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		saveAccoOrder(order, prevOrder);
		}
		myLogger.info(String.format("Full Cancellation of Acco Order done for req %s",reqJson.toString()));
		return "SUCCESS";
	}

	//TODO: check what status we need to have in room table
	private String updateRoom(JSONObject reqJson, String type) throws BookingEngineDBException {
		AccoRoomDetails room = roomRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("entityIDs").getJSONObject(0).getString(JSON_PROP_ENTITYID));	
		if(room == null)
		{
		  myLogger.warn("Requested EntityID not found"+reqJson );
		  throw new BookingEngineDBException("Requested entityId Not found");	
		}
		else
		{
		String prevOrder = room.toString();

		if(type.equals(JSON_PROP_ACCO_AMENDTYPE_UPDATEROOM)) {
		room.setRoomTypeCode(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ACCO_ROOMTYPECODE));
		room.setRatePlanCode(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ACCO_RATEPLANCODE));
		room.setStatus("Amended");
		room.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		}
		else {
		
			room.setStatus(OrderStatus.XL.getProductStatus());
			room.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			room.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		}	
		
			saveRoomDetails(room, prevOrder);
		
		myLogger.info(String.format("Acco Room updated  for req %s",reqJson.toString()));
		return "SUCCESS";
		}
	}

	//TODO: Check for what statuses we need to have in pax table, Also check if we need to update room table'ss status as well here.
	private String updatePaxDetails(JSONObject reqJson, String type) throws BookingEngineDBException {
		AccoRoomDetails room = roomRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("entityIDs").getJSONObject(0).getString(JSON_PROP_ENTITYID));
		if(room == null)
		{
		  myLogger.warn("Requested EntityID not found"+reqJson );
		  throw new BookingEngineDBException("Requested entityId Not found");	
		}
		else
		{
		String prevOrder;
		JSONArray entityIDArr=reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("entityIDs");
		JSONArray paxIDs= new JSONArray();
		JSONArray guestRoomJsonArray = reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAXINFO);
		for (int i = 0; i < guestRoomJsonArray.length(); i++) {
			
		JSONObject currenntPaxDetails = guestRoomJsonArray.getJSONObject(i);
        //add here
		JSONObject newPaxIDObj=new JSONObject();
		  
		PassengerDetails guestDetails;	

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
			newPaxIDObj.put(JSON_PROP_PAXID, updatedPaxDetails.getPassanger_id());
			paxIDs.put(newPaxIDObj);
		    JSONObject paxJson = new JSONObject();
			paxJson.put(JSON_PROP_PAXID, guestDetails.getPassanger_id());
			JSONArray paxDetails = new JSONArray(room.getPaxDetails());
			paxDetails.put(paxJson);
			room.setPaxDetails(paxDetails.toString());
			myLogger.info(String.format("Pax Details added for ACCO pax with paxid %s for request %s", updatedPaxDetails.getPassanger_id(),reqJson));
		}
		else if(type.equals(JSON_PROP_ACCO_AMENDTYPE_UPDATEPAX)) {
			guestDetails = passengerRepository.findOne(currenntPaxDetails.getString(JSON_PROP_PAXID));
			if(guestDetails == null)
			{
				myLogger.info(String.format("Acco update PAX request failed since paxID not found with id %s for req %s",currenntPaxDetails.getString(JSON_PROP_PAXID),reqJson));
				response.put("ErrorCode", "BE_ERR_ACCO_001");
				response.put("ErrorMsg", BE_ERR_ACCO_001);
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

			if (currenntPaxDetails.getString(JSON_PROP_PAX_TYPE).equals(Pax_ADT)&& currenntPaxDetails.getJSONObject(JSON_PROP_DOCUMENTDETAILS)!=null)
			guestDetails.setDocumentDetails(currenntPaxDetails.getJSONObject(JSON_PROP_DOCUMENTDETAILS).toString());
	                                    
			guestDetails.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			
			guestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

			PassengerDetails updatedPaxDetails = savePaxDetails(guestDetails,prevOrder);
			newPaxIDObj.put(JSON_PROP_PAXID, updatedPaxDetails.getPassanger_id());
			paxIDs.put(newPaxIDObj);
			myLogger.info(String.format("Pax Details updated for Acco pax with paxid %s for request %s", updatedPaxDetails.getPassanger_id(),reqJson));
		}
		}
		}	
		entityIDArr.getJSONObject(0).put("paxIDs", paxIDs);
		}
		
		return "SUCCESS";
	}
	
	
	public AccoOrders saveAccoOrder(AccoOrders currentOrder, String prevOrder) throws BookingEngineDBException {
		AccoOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, AccoOrders.class);
		}
		catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Acco order  object : " + e);
			throw new BookingEngineDBException("Failed to save ACCO order object");
		}
		return accoRepository.saveOrder(orderObj, prevOrder);
	}
	
	public AmCl saveAccoAmCl(AmCl currentOrder, String prevOrder) throws BookingEngineDBException {
		AmCl orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, AmCl.class);
		}
			catch (InvocationTargetException | IllegalAccessException e) {
				 myLogger.fatal("Error while saving Acco AMCL  object : " + e);
				throw new BookingEngineDBException("Failed to save ACCO AMCL object");
			}
		return accoAmClRepository.saveOrder(orderObj, prevOrder);
	}
	
	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevOrder) throws BookingEngineDBException 
	{
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);
		}
		catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Acco Passenger order object : " + e);
			throw new BookingEngineDBException("Failed to save order object");
		}
		return passengerRepository.saveOrder(orderObj,prevOrder);
	}
	
	private AccoRoomDetails saveRoomDetails(AccoRoomDetails room, String prevOrder) throws BookingEngineDBException 
	{
		AccoRoomDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(room, AccoRoomDetails.class);
		}
		catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Acco Room  object : " + e);
			throw new BookingEngineDBException("Failed to save acco room object");
		}
		return roomRepository.saveOrder(orderObj,prevOrder);
	}
	
	
}
