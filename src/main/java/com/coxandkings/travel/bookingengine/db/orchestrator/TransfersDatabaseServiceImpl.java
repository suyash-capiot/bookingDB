package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
import com.coxandkings.travel.bookingengine.db.model.TransfersOrders;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.enums.BookingAttribute;
import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AirOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CarOrders;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.model.TransfersAmCl;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.TransfersAmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.TransfersDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;


@Service
@Qualifier("Transfers")
@Transactional(readOnly=false)
public class TransfersDatabaseServiceImpl implements DataBaseService,Constants, ErrorConstants{

	


	@Autowired
	@Qualifier("Transfers")   
	private TransfersDatabaseRepository transfersRepository;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingService;
	
	@Autowired
	@Qualifier("TransfersAmCl")
	private TransfersAmClRepository transfersAmClRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	JSONObject response = new JSONObject();
	
	public boolean isResponsibleFor(String product) {
        return "transfers".equalsIgnoreCase(product);
    }

	public String processBookRequest(JSONObject bookRequestJson) throws JSONException, BookingEngineDBException  {
		
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null)
		booking = bookingService.processBookRequest(bookRequestJson,false);
		
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_TRANSFERS_TRANSINFO)) {

			TransfersOrders order = populateTransfersData((JSONObject)orderJson, bookRequestHeader, booking);
			saveOrder(order,"");
		}
	
		return "success";
	}

	public TransfersOrders populateTransfersData(JSONObject bookReq, JSONObject bookRequestHeader, Booking booking) throws BookingEngineDBException  {

		TransfersOrders order=new TransfersOrders();
		
		order.setBooking(booking);
		//TODO: change the client ID to userID once you get in header
		order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setStatus("OnRequest");
	//	order.setClientIATANumber(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTIATANUMBER));
		order.setClientCurrency(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setClientType(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
		order.setSupplierID(bookReq.getString(JSON_PROP_SUPPREF));
		order.setUniqueID(bookReq.getString(JSON_PROP_UNIQUEID));
		
		order.setTripType(bookReq.getString(JSON_PROP_TRANSFERS_TRIPTYPE));
		order.setTripIndicator(bookReq.getString(JSON_PROP_TRANSFERS_TRIPINDICATOR));
		
		order.setSuppFares(bookReq.getJSONObject(JSON_PROP_TRANSFERS_SUPPFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSSUPPTOTALFARE).toString());
		order.setSupplierTotalPrice(bookReq.getJSONObject(JSON_PROP_TRANSFERS_SUPPFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSSUPPTOTALFARE).getBigDecimal(JSON_PROP_TRANSFERS_AMOUNT).toString());
		order.setSupplierPriceCurrencyCode(bookReq.getJSONObject(JSON_PROP_TRANSFERS_SUPPFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSSUPPTOTALFARE).getString(JSON_PROP_TRANSFERS_CURRENCYCODE));

		order.setTotalFares(bookReq.getJSONObject(JSON_PROP_TRANSFERS_TOTALFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSTOTALFARE).toString());
		order.setTotalPrice(bookReq.getJSONObject(JSON_PROP_TRANSFERS_TOTALFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSTOTALFARE).getBigDecimal(JSON_PROP_TRANSFERS_AMOUNT).toString());
		order.setTotalPriceCurrencyCode(bookReq.getJSONObject(JSON_PROP_TRANSFERS_TOTALFARES).getJSONObject(JSON_PROP_TRANSFERS_TRANSTOTALFARE).getString(JSON_PROP_TRANSFERS_CURRENCYCODE));
		
		order.setRoe(bookReq.optString("roe"));
		
		JSONObject vehicleDetails = new JSONObject(bookReq.toString());
		vehicleDetails.remove(JSON_PROP_PAXDETAILS);
		vehicleDetails.remove(JSON_PROP_TRANSFERS_SUPPFARES);
		vehicleDetails.remove(JSON_PROP_TRANSFERS_TOTALFARES);
		/*vehicleDetails.remove(JSON_PROP_TRANSFERS_SUPPFARES);
		vehicleDetails.remove(JSON_PROP_TRANSFERS_TOTALFARES);*/
		
		/*JSONArray extraEquips = vehicleDetails.getJSONArray(JSON_PROP_CAR_SPLEQUIPS);*/
		
		System.out.println(bookReq.toString());
		order.setTransfersDetails(vehicleDetails.toString());
/*		order.setExtraEquipments(extraEquips.toString());*/
		//TODO: to set client comms later and also check if we need to add other fields in supplier comms
		
		Set<PassengerDetails> setPaxDetails = new HashSet<PassengerDetails>();
		setPaxDetails = readPassengerDetails(bookReq.getJSONArray(JSON_PROP_PAXDETAILS), order);
		
		Set<SupplierCommercial> suppComms =  new HashSet<SupplierCommercial>();
        suppComms = readSuppCommercials(bookReq.getJSONObject(JSON_PROP_TRANSFERS_SUPPFARES).getJSONArray(JSON_PROP_TRANSFERS_SUPPLIERCOMMS), order);
        
        Set<ClientCommercial> clientComms =  new HashSet<ClientCommercial>();
        clientComms = readClientCommercials(bookReq.getJSONObject(JSON_PROP_TRANSFERS_TOTALFARES).getJSONArray(JSON_PROP_TRANSFERS_CLIENTENTITYCOMMS), order);
        JSONArray paxIds = new JSONArray();
		for(PassengerDetails pax:setPaxDetails ) {
			JSONObject paxJson = new JSONObject();
			paxJson.put("paxId", pax.getPassanger_id());
			paxIds.put(paxJson);
		}

		order.setPaxDetails(paxIds.toString());
        order.setClientCommercial(clientComms);
		order.setSuppcommercial(suppComms);
		
		return order;
		
	}
	
	private Set<SupplierCommercial> readSuppCommercials(JSONArray suppCommsJsonArray, TransfersOrders order) {
		 
		
		Set<SupplierCommercial> suppCommercialsSet =new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;
		
		for(int i=0;i<suppCommsJsonArray.length();i++)	{
		JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);
		
		suppCommercials =new SupplierCommercial();
		suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
		suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
		//TODO : CommercialAmount put under TotalCommercialAmount, Why?
		suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
		suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));
		suppCommercials.setProduct(JSON_PROP_PRODUCTTRANSFER);
		suppCommercials.setOrder(order);
		suppCommercialsSet.add(suppCommercials);
		}
		return suppCommercialsSet;
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, TransfersOrders order) {
		Set<ClientCommercial> clientCommercialsSet =new HashSet<ClientCommercial>();
		ClientCommercial clientCommercials;
		
		for(int i=0;i<clientCommsJsonArray.length();i++)	{
			
			JSONObject totalClientComm = clientCommsJsonArray.getJSONObject(i);
			
			 String clientID = totalClientComm.getString(JSON_PROP_CLIENTID);
			 String parentClientID = totalClientComm.getString(JSON_PROP_PARENTCLIENTID);;		
			 String commercialEntityType = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYTYPE);;		
			 String commercialEntityID = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYID);;
			
			 boolean companyFlag = (i==0)? true: false;
			
		
			JSONArray clientComms = totalClientComm.getJSONArray(JSON_PROP_TRANSFERS_CLIENTCOMMINFO);
			
			for(int j=0;j<clientComms.length();j++) {
			
				JSONObject clientComm = clientComms.getJSONObject(j);
				
				clientCommercials = new ClientCommercial();
				clientCommercials.setCommercialName(clientComm.getString(JSON_PROP_COMMERCIALNAME));
				clientCommercials.setCommercialType(clientComm.getString(JSON_PROP_COMMERCIALTYPE));
				clientCommercials.setCommercialAmount(clientComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
				clientCommercials.setCommercialCurrency(clientComm.getString(JSON_PROP_COMMERCIALCURRENCY));
				clientCommercials.setClientID(clientID);
				clientCommercials.setParentClientID(parentClientID);
				clientCommercials.setCommercialEntityType(commercialEntityType);
				clientCommercials.setCommercialEntityID(commercialEntityID);
				clientCommercials.setCompanyFlag(companyFlag);
		
				clientCommercials.setProduct(JSON_PROP_PRODUCTTRANSFER);
				clientCommercials.setOrder(order);
				clientCommercialsSet.add(clientCommercials);
			}
		}
		return clientCommercialsSet;
	}
	
	private Set<PassengerDetails> readPassengerDetails(JSONArray paxJsonArray, TransfersOrders transfersOrder) throws BookingEngineDBException  {
		 
		Set<PassengerDetails> paxDetailsSet = new HashSet<PassengerDetails>();
		PassengerDetails paxDetails;
		for(int i=0;i<paxJsonArray.length();i++)	{
		JSONObject currentPaxDetails = paxJsonArray.getJSONObject(i);
		paxDetails = new PassengerDetails();
		//TODO : Set isLead Traveler.
		JSONObject primaryJson = currentPaxDetails.getJSONObject(JSON_PROP_TRANSPRIMARY);
		
		
		paxDetails.setRph(primaryJson.getString(JSON_PROP_TRANSFERS_RPH) );
		paxDetails.setPaxType(primaryJson.getString(JSON_PROP_PAX_TYPE) );
		paxDetails.setAge(primaryJson.getString(JSON_PROP_TRANSFERS_AGE));
		paxDetails.setQuantity(primaryJson.optString(JSON_PROP_TRANSFERS_QUANTITY));
		paxDetails.setContactDetails(primaryJson.optString(JSON_PROP_TRANSFERS_CONTACTNUMBER));
		paxDetails.setEmail(primaryJson.optString(JSON_PROP_TRANSFERS_EMAIL));
		paxDetails.setPersonName(primaryJson.optJSONObject(JSON_PROP_TRANSFERS_PERSONNAME).toString());
		
		JSONObject additionalJson = currentPaxDetails.getJSONObject(JSON_PROP_TRANSFERS_ADDITIONAL);
		paxDetails.setPersonName(additionalJson.getJSONObject(JSON_PROP_TRANSFERS_PERSONNAME).toString());
		paxDetails.setRph(additionalJson.getString(JSON_PROP_TRANSFERS_RPH) );
		paxDetails.setPaxType(additionalJson.getString(JSON_PROP_PAX_TYPE) );
		paxDetails.setAge(additionalJson.getString(JSON_PROP_TRANSFERS_AGE));
		paxDetails.setQuantity(additionalJson.optString(JSON_PROP_TRANSFERS_QUANTITY));
		paxDetails.setContactDetails(primaryJson.optString(JSON_PROP_TRANSFERS_CONTACTNUMBER));
		paxDetails.setEmail(additionalJson.optString(JSON_PROP_TRANSFERS_EMAIL));
		/*paxDetails.setFirstName(currentPaxDetails.getString(JSON_PROP_FIRSTNAME));
		paxDetails.setStatus("OnRequest");
		paxDetails.setMiddleName(currentPaxDetails.getString(JSON_PROP_MIDDLENAME));
		paxDetails.setSurname(currentPaxDetails.getString(JSON_PROP_SURNAME));
		paxDetails.setBirthDate(currentPaxDetails.getString(JSON_PROP_CAR_DOB));
		paxDetails.setGender(currentPaxDetails.getString(JSON_PROP_GENDER));
		paxDetails.setContactDetails(currentPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
		paxDetails.setAddressDetails(currentPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());*/
		
		//TODO:change it to userID later 
		paxDetails.setLastModifiedBy("");
		paxDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		savePaxDetails(paxDetails,"");
			paxDetailsSet.add(paxDetails);
		
		}
		return paxDetailsSet;
	}

	public String processBookResponse(JSONObject bookResponseJson) {
		
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		List<TransfersOrders> orders = transfersRepository.findByBooking(booking);
		String prevOrder = booking.toString();
		booking.setStatus("confirmed");
		booking.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		booking.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		saveBookingOrder(booking,prevOrder);
	    JSONArray groundServiceArr = bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("groundBookRSWrapper");
	    for(int i = 0 ;i < groundServiceArr.length() ; i++) {
	    	JSONObject groundServiceJson = groundServiceArr.getJSONObject(i);
	    	
	    	
		
		/*	
		List<TransfersOrders> orders = transfersRepository.findByBooking(booking);
		int count=0;
		for(TransfersOrders order:orders) {
			order.setStatus("confirmed");
			// TODO : May Need to Change Later
			String reservationID = "";
			//String suppResNum = "";
			JSONObject suppBookRef = groundServiceJson.getJSONObject("reservationIds");
			for(int j = 0;j < suppBookRef.length();j++) {
			for(Object bookRef: suppBookRef) {
			reservationID = ((JSONObject) suppBookRef).optString(JSON_PROP_RESERVATIONID);
					//suppResNum = ((JSONObject) bookRef).optString("supplierReservationNumber");
			JSONObject suppBookRefs = new JSONObject();
			suppBookRefs.put(JSON_PROP_RESERVATIONID, reservationID);
			//order.setSuppBookRef(bookId);
			//String reservationId = suppBookRef.optString(JSON_PROP_RESERVATIONID);
			order.setSuppBookRefs(suppBookRefs.toString());
			order.setBookingDateTime(new Date().toString());
			order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			count++;
			transfersRepository.save(order);*/
	    	//JSONArray reservationArr = bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_CAR_RESERVATION);
			//for (int i = 0; i < reservationArr.length(); i++) {
				//JSONObject currCarInfoObj = reservationArr.getJSONObject(i);
				TransfersOrders order = null;
				try {
					order = orders.get(i);
				}catch(ArrayIndexOutOfBoundsException e) {
					///Ideally code will never come here.
					myLogger.warn(String.format("Order no.%d from req could not be confirmed since corresponding transfersOrder for BookID %s not found in DB", i, 
							bookResponseJson.getJSONObject("responseBody").getString("bookID")));
					response.put("ErrorCode", "BE_ERR_TRAN_005");
					response.put("ErrorMsg", BE_ERR_TRAN_005);
					continue;
				}
				order.setStatus(OrderStatus.OK.getProductStatus());
				// TODO : May Need to Change Later
				JSONArray references = groundServiceJson.getJSONObject("reservationIds").getJSONArray(JSON_PROP_TRANSFERS_REFERENCES);
				String reservationId = groundServiceJson.getJSONObject("reservationIds").getString(JSON_PROP_TRANSFERS_RESERVATIONID);
				JSONObject suppBookRefs = new JSONObject();
				suppBookRefs.put(JSON_PROP_TRANSFERS_REFERENCES, references);
				suppBookRefs.put(JSON_PROP_TRANSFERS_RESERVATIONID, reservationId);
				
				order.setSuppBookRefs(suppBookRefs.toString());
				order.setBookingDateTime(new Date().toString());
				order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
				transfersRepository.save(order);
		
		}
	    
	    myLogger.info(String.format("Transfers Booking Response populated successfully for req with bookID %s = %s",
				bookResponseJson.getJSONObject("responseBody").getString("bookID"),
				bookResponseJson.toString()));
		return "SUCCESS";
}
	
	
	public  Booking saveBookingOrder(Booking order, String prevOrder) {
		
		Booking orderObj=null;
		try {
			orderObj = CopyUtils.copy(order, Booking.class);
			
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
		
			e.printStackTrace();
		}
		return bookingRepository.saveOrder(orderObj,prevOrder);
	}
	
	private String fullCancel(TransfersAmCl cancelEntry, TransfersOrders order, String orderId, JSONObject reqJson) throws BookingEngineDBException {

		/*TransfersOrders order = transfersRepository.findOne(reqBodyJson.getJSONArray("entityIds").getJSONObject(0).getString(JSON_PROP_ENTITYID));	
//		String prevOrder = order.toString();
		order.setStatus(OrderStatus.XL.getProductStatus());
		order.setLastModifiedBy(modifyReq.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

		saveOrder(order, "");
		return "SUCCESS";
	}*/
			//Booking booking = bookingRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
//			booking.setStatus("updated");
			
		String type = reqJson.getJSONObject(JSON_PROP_REQBODY).getString("type");
		Booking booking = bookingRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		booking.setStatus("onCancelRequest");
		
			/*TransfersOrders order = transfersRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("orderId"));
//			order.setBooking(booking);
			System.out.println(order.toString());
			String prevOrder = order.toString();
			order.setStatus("orderCancelled");
			order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			
			saveOrder(order, prevOrder);
			return "SUCCESS";*/
		 List<String> eids = new  ArrayList<String>();
		 eids.add(orderId);
		JSONArray entityIds =new JSONArray(eids.toString());
		
		cancelEntry.setEntityID(entityIds.toString());
		//cancelEntry.setEntityID(orderId);
		cancelEntry.setStatus("cancelled");
		cancelEntry.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		cancelEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		cancelEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		saveTransfersAmcl(cancelEntry, "");
		
		
		return "SUCCEESS";
		}
	
	public TransfersOrders saveOrder(TransfersOrders order, String prevOrder) {
		TransfersOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, TransfersOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return transfersRepository.saveOrder(orderObj,prevOrder);
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
		
	//	JSONArray cancelRqArr =	reqJson.getJSONObject("requestBody").getJSONArray("cancelRequests");
		
			JSONObject reqBodyJson = reqJson.getJSONObject(JSON_PROP_REQBODY);
			JSONObject modifyReq = reqBodyJson.getJSONObject("reservationIds");
			String type = reqBodyJson.getString("type");
			TransfersAmCl cancelEntry = new TransfersAmCl();
			
			cancelEntry.setEntityID(reqBodyJson.getJSONArray("entityIds").toString());
			cancelEntry.setEntityName(reqBodyJson.getString("entityName"));
			cancelEntry.setType(reqBodyJson.getString("type"));
			cancelEntry.setOrderID(reqBodyJson.getString(JSON_PROP_ORDERID));
			cancelEntry.setBookId(reqBodyJson.getString("bookID"));
			cancelEntry.setRequestType(reqBodyJson.getString("requestType"));
			cancelEntry.setSupplierCharges("0");
			cancelEntry.setDescription(type);
			cancelEntry.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			cancelEntry.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			cancelEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
			cancelEntry.setStatus(OrderStatus.RXL.getProductStatus());
			
			TransfersOrders torder = transfersRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
			String bookingAttribute = torder.getBookingAttribute();
			JSONArray bookingAttributeArray ;
			if(bookingAttribute != null) {
		    bookingAttributeArray = new JSONArray(bookingAttribute);
		    }
			else
			{
				 bookingAttributeArray=new JSONArray();
			}
			JSONObject bookingAttributeObj=new JSONObject();
			bookingAttributeObj.put(BookingAttribute.RAMD.toString(),BookingAttribute.RAMD.getBookingAttribute());
			bookingAttributeArray.put(bookingAttributeObj);
			torder.setBookingAttribute(bookingAttributeArray.toString());
			saveOrder(torder, "");
			saveAmCl(cancelEntry, "");
			myLogger.info(String.format(" Transfers AMCL req populated  for request %s",reqJson));
			
			/*cancelEntry.setSupplierID(cancelRqJson.getString("supplierRef"));
			cancelEntry.setUniqueIDs(cancelRqJson.getJSONArray("uniqueID").toString());
			cancelEntry.setPaxVerificationInfo(cancelRqJson.getJSONObject("verification").toString());
			cancelEntry.setCnclOvrrides(cancelRqJson.getJSONArray("cancellationOverrides").toString());*/
			
			//Getting the Order by querying with the supplier Book reference. 
			//------------------
			/*String id= modifyReq.getString(JSON_PROP_RESERVATIONID);
			
			String suppId = reqBodyJson.optString("supplierRef");
			List<TransfersOrders> transfersOrder = transfersRepository.findBySuppBookRef(suppId, id);
			if(transfersOrder.size()==0) {
				myLogger.info(String.format("No Order found for the given req %s", reqJson));
				return String.format("No Order found for the given req %s", reqJson);
			}
			
			//Ideally different orders won't have same suppBookRef.So no need for loop
			TransfersOrders tOrder =  transfersOrder.get(0);
			String bookingAttribute = tOrder.getBookingAttribute();
			JSONArray bookingAttributeArray = bookingAttribute != null ?  new JSONArray(bookingAttribute) : new JSONArray();
			JSONObject bookingAttributeObj = new JSONObject();
			bookingAttributeObj.put(BookingAttribute.RAMD.toString(),BookingAttribute.RAMD.getBookingAttribute());
			bookingAttributeArray.put(bookingAttributeObj);
			tOrder.setBookingAttribute(bookingAttributeArray.toString());
			saveOrder(tOrder, "");
			cancelEntry.setOrderID(tOrder.getId());
			cancelEntry.setEntityID(new JSONArray().put(tOrder.getId()).toString());*/
			
			
			//Booking booking = bookingRepository.findOne(reqBodyJson.getString(JSON_PROP_BOOKID));
			
			/*JSONArray uniqueArr = reqBodyJson.getJSONArray("uniqueID");
					for(int i = 0 ;i < uniqueArr.length() ; i++) {
						
					
			JSONObject uniqueJson = uniqueArr.getJSONObject(i);
				if(uniqueJson.getString("type").equalsIgnoreCase("14")) {
			String orderId;
			List<TransfersOrders> cancelOrders = transfersRepository.findByUniqueID(uniqueJson.getString("supplierReferenceNumber"));
			for(TransfersOrders order:cancelOrders) {
				
				String prevOrder = order.toString();
				cancelEntry.setOrderID(order.getId());
	
					
			saveAmCl(cancelEntry, "");*/
			/*switch(type)
	        {
			
	        	case JSON_PROP_TRANSFERS_CANCELTYPE_FULLCANCEL:
	        		
	                return fullCancel(reqJson);
	            default:
	                return "no match for cancel/amend type";
	                
	        }*/
			 switch(type)
		        {
		           
		        	case JSON_PROP_TRANSFERS_CANCELTYPE_FULLCANCEL:
		        	{
		        		torder.setStatus("cancelled");
		        		torder.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		        		
		        		torder.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		        		torder.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		        		
		        		
		        		saveOrder(torder, "");
						
		        		return fullCancel(cancelEntry,torder,torder.getId(),reqJson);

				
						
		        	}
		        		
		           
		        	
		        	/*case JSON_PROP_TRANSFERS_CANNCELTYPE_CANCELPAX:
		        	{
		        		order.setStatus("Pax cancelled");
		        		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		        		
		        		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		        		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		        		
		        		
		        		saveOrder(order, "");
		        		return setPassengersStatusforPartialCancel(cancelEntry,order,order.getId(),reqJson,seatNos);
		        	}*/
		                
		        		  
		            default:
		                return "no match for cancel/amend type";
		        }
				
	}
					
	

	public TransfersAmCl saveAmCl(TransfersAmCl currentOrder, String prevOrder) throws BookingEngineDBException {
		TransfersAmCl orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder,TransfersAmCl.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Transfer Cancel order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save Transfer Cancel order object");
		}
		return transfersAmClRepository.saveOrder(orderObj, prevOrder);
	}

	@Override
	public String processAmClResponse(JSONObject resJson) {
		
		Booking booking = bookingRepository.findOne(resJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		List<TransfersOrders> orders = transfersRepository.findByBooking(booking);
		
			
		
		for(TransfersOrders order:orders)
		{
//		List<TransfersAmCl> cancelEntries  = transfersAmClRepository.findResponse(resJson.getJSONObject(JSON_PROP_RESBODY).getString("bookID"), order.getId(),resJson.getJSONObject(JSON_PROP_RESBODY).getString("requestType"), resJson.getJSONObject(JSON_PROP_RESBODY).getString("type"));
		JSONArray paxDtlsArr = new JSONArray(order.getPaxDetails());
		JSONArray seatsToCancelArr = resJson.getJSONObject(JSON_PROP_RESBODY).getJSONObject("service").getJSONArray("seatsToCancel");
		List<String> seatNos = new ArrayList<String>();
		for(int i=0;i<seatsToCancelArr.length();i++)
		{
			JSONObject seatJson = seatsToCancelArr.getJSONObject(i);
			seatNos.add(seatJson.getString("seatNo"));
		}
		String type = resJson.getJSONObject(JSON_PROP_RESBODY).getString("type");
		List<TransfersAmCl> amendEntries = new ArrayList<TransfersAmCl>();
		/*if(type.equalsIgnoreCase(JSON_PROP_BUS_CANNCELTYPE_CANCELPAX))
		{
			boolean flag=false;
			for(int i=0;i<paxDtlsArr.length();i++)
			{
				JSONObject paxJson = paxDtlsArr.getJSONObject(i);
				for(String seatNo:seatNos)
				{
					if(seatNo.equals(paxJson.getString("seatNo")))
					{
						amendEntries = transfersAmClRepository.findforResponseUpdate(
								resJson.getJSONObject(JSON_PROP_RESBODY).getString("entityName"),
								order.getId(),
								resJson.getJSONObject(JSON_PROP_RESBODY).getString("type"),
								resJson.getJSONObject(JSON_PROP_RESBODY).getString("requestType"));
								flag=true;
								break;
					}
				}
				
				if(flag)
					break;
			}
			
		}
		else*/ 
		if(type.equals(JSON_PROP_TRANSFERS_CANCELTYPE_FULLCANCEL))
		{
			 amendEntries = transfersAmClRepository.findforResponseUpdate(
					resJson.getJSONObject(JSON_PROP_RESBODY).getString("entityName"),
					order.getId(),
					resJson.getJSONObject(JSON_PROP_RESBODY).getString("type"),
					resJson.getJSONObject(JSON_PROP_RESBODY).getString("requestType"));
		}
			
		if(amendEntries.size()==0) {
			//TODO: handle this before it goes in prod
			System.out.println("no amend entry found. Request might not have been populated");
		}
		
		else if(amendEntries.size()>1) {
			//TODO: handle this before it goes in prod
			System.out.println("multiple amend entries found. Dont know which one to update");
		}
		else
		{
			TransfersAmCl cancelOrder = amendEntries.get(0);	
			String prevOrder = cancelOrder.toString();//TODO:doubt???
			
			cancelOrder.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			cancelOrder.setLastModifiedBy(resJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));
//			/cancelOrder.setRefundAmount(resJson.getJSONObject(JSON_PROP_RESBODY).getJSONObject("service").getBigDecimal("refundAmount").toString());
			//cancelOrder.setRefundAmountCurrency(resJson.getJSONObject(JSON_PROP_RESBODY).getJSONObject("service").getString("currency"));
			//cancelOrder.setLastModifiedBy(resJson.getJSONObject(JSON_PROP_RESBODY).getString("userID"));
			cancelOrder.setStatus("cancelled");
			saveTransfersAmcl(cancelOrder, prevOrder);
		}
	}
		return "SUCCESS";
	}
	
	private TransfersAmCl saveTransfersAmcl(TransfersAmCl cancelOrder, String prevOrder)
	{
		TransfersAmCl orderObj = null;
		try
		{
			orderObj = CopyUtils.copy(cancelOrder, TransfersAmCl.class);
		}
		catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Transfer Amend Cancel order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			
		}
		return transfersAmClRepository.saveOrder(orderObj, prevOrder);
		
	}
	


	

}
