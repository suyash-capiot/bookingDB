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
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.enums.BookingAttribute;
import com.coxandkings.travel.bookingengine.db.enums.BookingStatus;
import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;

import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.BusAmCl;
import com.coxandkings.travel.bookingengine.db.model.BusOrders;

import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;

import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.BusAmclRepository;
import com.coxandkings.travel.bookingengine.db.repository.BusDatabaseRepository;

import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;


@Service
@Qualifier("Bus")
@Transactional(readOnly=false)
public class BusDatabaseServiceImpl implements DataBaseService,Constants,ErrorConstants,CancelAmendTypes{
	
	@Autowired
	@Qualifier("Bus")
	private BusDatabaseRepository busRepository;
	
//	@Autowired
//	@Qualifier("BusPassenger")
//	private BusPassengerRepository buspaxRepository;
	
	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingService;
	
//	@Autowired
//	@Qualifier("AccoAmCl")
//	private AmClRepository AmClRepository;
	@Autowired
	@Qualifier("BusAmCl")
	private BusAmclRepository busAmClRepository;
	
	
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	JSONObject response=new JSONObject();
	@Override
	public boolean isResponsibleFor(String product) {
	
		return "Bus".equalsIgnoreCase(product);
	}

	@Override
	public String processBookRequest(JSONObject bookRequestJson) throws BookingEngineDBException {

		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		

		if(booking==null)
			booking = bookingService.processBookRequest(bookRequestJson, false);
		
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
//		JSONArray serviceArr = bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("Service");

		
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_BUS_SERVICE))
		{
			BusOrders order = populateBusData((JSONObject) orderJson, bookRequestHeader,booking);
			order.setProductSubCategory(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString("product"));
			saveOrder(order,"");
			
//			System.out.println("before update id - ->"+order.getId());
//			updatePaxDetails((JSONObject) orderJson,booking,order); //TODO: modified...
//			saveOrder(order,"");
//			System.out.println("after update id - ->"+order.getId());
		}
		
//		saveBookingOrder(booking);
		myLogger.info(String.format("bus Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));

		return "success";
	}

//	private BusOrders updatePaxDetails(JSONObject orderJson, Booking booking,BusOrders order) {
//
//		order.setBooking(booking);
//		
//		Set<BusPassengerDetails> busPassDetails =order.getPassengerDetails();
//		
//		Iterator iterator = busPassDetails.iterator(); 
//		
//		while(iterator.hasNext())
//		{
//			BusPassengerDetails passDtls = (BusPassengerDetails) iterator.next();
//			passDtls.setBus_order_id(order.getId());
//			
//		}
//		order.setPaxDetails(busPassDetails);
//		
//		return order;
//	}

	private BusOrders saveOrder(BusOrders order, String prevOrder) {
		
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
	

	
	private BusAmCl saveBusAmcl(BusAmCl cancelOrder, String prevOrder)
	{
		BusAmCl orderObj = null;
		try
		{
			orderObj = CopyUtils.copy(cancelOrder, BusAmCl.class);
		}
		catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Bus Amend Cancel order object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			
		}
		return busAmClRepository.saveOrder(orderObj, prevOrder);
		
	}

	private BusOrders populateBusData(JSONObject serviceorderJson, JSONObject bookRequestHeader,Booking booking) throws BookingEngineDBException {
		
		BusOrders order=new BusOrders();
		order.setBooking(booking);
		
		
		order.setLastModifiedBy(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setStatus(OrderStatus.RQ.getProductStatus());
		
		//TODO: for now populating all suppliers as offline. Later have  this logic after operations is concrete.
		order.setSupplierType("online");
				
//		order.setClientIATANumber(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTIATANUMBER));
		order.setClientCurrency(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setClientType(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
		order.setSupplierID(serviceorderJson.getString(JSON_PROP_SUPPREF));
		
		order.setSupplierTotalPrice(serviceorderJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_BUS_SUPPLIERTOTALPRICINGINFO).getJSONObject(JSON_PROP_BUS_SERVICETOTALFARE).getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setTotalPrice(serviceorderJson.getJSONObject(JSON_PROP_BUS_BUSTOTALPRICINGINFO).getJSONObject(JSON_PROP_BUS_SERVICETOTALFARE).getBigDecimal(JSON_PROP_AMOUNT).toString());
        order.setSupplierPriceCurrencyCode(serviceorderJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_BUS_SUPPLIERTOTALPRICINGINFO).getJSONObject(JSON_PROP_BUS_SERVICETOTALFARE).getString("currency"));
        order.setTotalPriceCurrencyCode(serviceorderJson.getJSONObject(JSON_PROP_BUS_BUSTOTALPRICINGINFO).getJSONObject(JSON_PROP_BUS_SERVICETOTALFARE).optString(JSON_PROP_CURRENCYCODE));
        order.setBusDetails(readBusDetails(serviceorderJson));
        
		order.setSuppPaxTypeFares(serviceorderJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_BUS_SUPPLIERTOTALPRICINGINFO).getJSONArray(JSON_PROP_BUS_PAXSEATFARES).toString());
		order.setTotalPaxTypeFares(serviceorderJson.getJSONObject(JSON_PROP_BUS_BUSTOTALPRICINGINFO).getJSONArray(JSON_PROP_BUS_PAXSEATFARES).toString());
		
		order.setTotalPriceBaseFare(serviceorderJson.getJSONObject(JSON_PROP_BUS_BUSTOTALPRICINGINFO).getJSONObject(JSON_PROP_BUS_SERVICETOTALFARE).getJSONObject(JSON_PROP_BASEFARE).toString());
		order.setTotalPriceReceivables(serviceorderJson.getJSONObject(JSON_PROP_BUS_BUSTOTALPRICINGINFO).getJSONObject(JSON_PROP_BUS_SERVICETOTALFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
		
        Set<PassengerDetails> setPassDetails = new HashSet<PassengerDetails>();
		setPassDetails = readPassengerDetails(order,serviceorderJson);
		
		JSONArray paxIds = new JSONArray();
		
		order.setRoe(serviceorderJson.optString("rateOfExchange"));
		
		JSONArray paxDetailsArr = new JSONArray();
		paxDetailsArr = serviceorderJson.getJSONArray(JSON_PROP_PAXDETAILS);
		int i=0;
		for(PassengerDetails pax:setPassDetails ) {
			
			JSONObject paxJson = new JSONObject();
			paxJson.put(JSON_PROP_PAXID, pax.getPassanger_id());
			paxJson.put(JSON_PROP_BUS_SEATNO, paxDetailsArr.getJSONObject(i).getString(JSON_PROP_BUS_SEATNO));
			paxJson.put("seatTypesList", paxDetailsArr.getJSONObject(i).getString("seatTypesList"));
			paxJson.put("seatTypeIds", paxDetailsArr.getJSONObject(i).getString("seatTypeIds"));
			i++;
			paxIds.put(paxJson);
		}

		order.setPaxDetails(paxIds.toString());
		
		
		
		Set<SupplierCommercial> suppComms =  new HashSet<SupplierCommercial>();
		suppComms = readSuppCommercials(serviceorderJson.getJSONObject(JSON_PROP_AIR_SUPPINFO).getJSONObject(JSON_PROP_BUS_SUPPLIERTOTALPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS),order);
		
		 Set<ClientCommercial> clientComms =  new HashSet<ClientCommercial>();
        clientComms = readClientCommercials(serviceorderJson.getJSONObject(JSON_PROP_BUS_BUSTOTALPRICINGINFO).getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS),order);
        
        
		order.setClientCommercial(clientComms);
		order.setSuppcommercial(suppComms);
		
		order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		return order;
	}

	private String readBusDetails(JSONObject serviceorderJson) {

		JSONObject BusDetails = new JSONObject();
		 
		BusDetails.put("routeScheduleId", serviceorderJson.get("routeScheduleId"));
		BusDetails.put(JSON_PROP_BUS_SOURCE, serviceorderJson.get(JSON_PROP_BUS_SOURCE));
		BusDetails.put(JSON_PROP_BUS_DESTINATION, serviceorderJson.get(JSON_PROP_BUS_DESTINATION));
		BusDetails.put(JSON_PROP_BUS_SERVICEID, serviceorderJson.get(JSON_PROP_BUS_SERVICEID));
		BusDetails.put(JSON_PROP_BUS_LAYOUTID, serviceorderJson.get(JSON_PROP_BUS_LAYOUTID));
		BusDetails.put(JSON_PROP_BUS_OPERATORID, serviceorderJson.get(JSON_PROP_BUS_OPERATORID));
		BusDetails.put("boardingPointID", serviceorderJson.get("boardingPointID"));
		BusDetails.put("droppingPointID", serviceorderJson.get("droppingPointID"));

		return BusDetails.toString();
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, BusOrders order) {
		
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

		clientCommercials.setProduct(JSON_PROP_PRODUCTBUS);
		clientCommercials.setOrder(order);
		clientCommercialsSet.add(clientCommercials);
		}
		}
		return clientCommercialsSet;
	}

private Set<SupplierCommercial> readSuppCommercials(JSONArray suppCommsJsonArray, BusOrders order) {
		 
		
		Set<SupplierCommercial> suppCommercialsSet =new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;
		
		for(int i=0;i<suppCommsJsonArray.length();i++)	{
		JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);
		
		suppCommercials =new SupplierCommercial();
		suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
		suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
		suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
		suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));
		
	
		suppCommercials.setProduct(JSON_PROP_PRODUCTBUS);
		suppCommercials.setOrder(order);
		suppCommercialsSet.add(suppCommercials);
		}
		return suppCommercialsSet;
	}

	private Set<PassengerDetails> readPassengerDetails(BusOrders order,JSONObject serviceorderJson) throws BookingEngineDBException {


		
		Set<PassengerDetails> paxDetailsSet = new HashSet<PassengerDetails>();
		
		JSONArray passDetailsJsonArr = serviceorderJson.getJSONArray(JSON_PROP_PAXDETAILS);
		for(int i=0;i<passDetailsJsonArr.length();i++)
		{
			JSONObject currenntPaxDetails = passDetailsJsonArr.getJSONObject(i);
			PassengerDetails paxDetails =new PassengerDetails();
			
			paxDetails.setTitle(currenntPaxDetails.getString(JSON_PROP_TITLE));
			paxDetails.setFirstName(currenntPaxDetails.getString(JSON_PROP_FIRSTNAME) );
			
			paxDetails.setGender(currenntPaxDetails.getString(JSON_PROP_GENDER));
			if(currenntPaxDetails.get("isPrimary").equals(true))
			{
				paxDetails.setIsLeadPax(true);
			}
			else
			{
				paxDetails.setIsLeadPax(false);
			}
			
			paxDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			paxDetails.setDocumentDetails(currenntPaxDetails.getJSONObject(JSON_PROP_DOCUMENTDETAILS).toString());
			paxDetails.setStatus(OrderStatus.RQ.getProductStatus());
			
			
			paxDetails.setLastModifiedBy("");
			paxDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			paxDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			
			
			savePaxDetails(paxDetails,"");
			paxDetailsSet.add(paxDetails);
			
		}
		return paxDetailsSet;
	}

	private PassengerDetails savePaxDetails(PassengerDetails paxDetails, String prevPaxDetails) throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(paxDetails, PassengerDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving passenger object : " + e);
			 //myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save passenger object");
		}
		return passengerRepository.saveOrder(orderObj,prevPaxDetails);
		
	}

	@Override
	public String processBookResponse(JSONObject bookResponseJson) {
		
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
//		String prevOrder=booking.toString();
		if(booking==null)
		{
			myLogger.warn(String.format("Bus Booking Response could not be populated since no bookings found for req with bookID %s", bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID)));
			response.put("ErrorCode","BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			return response.toString();
		}
		
//		saveBookingOrder(booking,"");
		else
		{
			List<BusOrders> orders = busRepository.findByBooking(booking);
			if (orders.size() == 0) {
				myLogger.warn(String.format("Bus Booking Response could not be populated since no bus orders found for req with bookID %s",
						bookResponseJson.getJSONObject("responseBody").getString("bookID")));
				response.put("ErrorCode", "BE_ERR_BUS_005");
				response.put("ErrorMsg", BE_ERR_BUS_003);
				return response.toString();
		} 
			JSONArray bookTktArr = bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("bookTicket");
			
			booking.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			booking.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			for(int i=0;i<bookTktArr.length();i++)
			{
				BusOrders order;
				try {
					 order = orders.get(i);
				}catch(ArrayIndexOutOfBoundsException e) {
					///Idealy code will never come here.
					myLogger.warn(String.format("Order no.%d from req could not be confirmed since corresponding carOrder for BookID %s not found in DB", i, 
							bookResponseJson.getJSONObject("responseBody").getString("bookID")));
					response.put("ErrorCode", "BE_ERR_CAR_005");
					response.put("ErrorMsg", BE_ERR_CAR_005);
					continue;
				}
				JSONObject bookTktJson = bookTktArr.getJSONObject(i);
				if(bookTktJson.has("errorMessage"))
				{
					myLogger.info("book response not populated for one of the request");
					
				}
				else
				{
					order.setStatus(OrderStatus.OK.getProductStatus());
					order.setBusPNR(bookTktJson.getString(JSON_PROP_BUS_PNRNO));
					order.setTicketNo(bookTktJson.getString(JSON_PROP_BUS_TICKETNO));
					order.setCancellationPolicy(bookTktJson.getJSONArray("cancellationPolicy").toString());
					order.setBookingDate(new Date().toString());
					busRepository.save(order);
				}
				
			}
			myLogger.info(String.format("Bus Booking Response populated successfully for req with bookID %s = %s", bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID),bookResponseJson.toString()));
			return "SUCCESS";

		}
		
		
		
	}
	
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException 
	{
		String type = reqJson.getJSONObject(JSON_PROP_REQBODY).getString("type");
		BusAmCl cancelOrder = new BusAmCl();
		cancelOrder.setEntityName(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("type"));
		cancelOrder.setRequestType(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("requestType"));
	
		cancelOrder.setBookId(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		cancelOrder.setDescription(type);
		cancelOrder.setSupplierCharges("0");
		cancelOrder.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		cancelOrder.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		cancelOrder.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		cancelOrder.setStatus(OrderStatus.RXL.getProductStatus());
		

		
		JSONObject reqBodyJson = reqJson.getJSONObject("requestBody");

		JSONObject serviceJson = reqBodyJson.getJSONObject("service");
		JSONArray cancelSeatsArr = serviceJson.getJSONArray("seatsToCancel");
			

				
				List<BusOrders> cancelOrders = busRepository.findByTktNo(serviceJson.getString("ticketNo"));
				if(cancelOrders==null || cancelOrders.isEmpty())
				{
					myLogger.info(String.format("bus cancel request failed since ticketNo is not found  for req %s",reqJson));
					response.put("ErrorCode", "BE_ERR_BUS_001");
					response.put("ErrorMsg", BE_ERR_BUS_001);
					return response.toString();
				}
				for(BusOrders order:cancelOrders)
				{
				
					cancelOrder.setOrderID(order.getId());
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
					bookingAttributeObj.put(BookingAttribute.RAMD.toString(),BookingAttribute.RAMD.getBookingAttribute());
					bookingAttributeArray.put(bookingAttributeObj);
					order.setBookingAttribute(bookingAttributeArray.toString());

					List<String> seatNos = new ArrayList<String>();
					
					
					
					for(int i=0;i<cancelSeatsArr.length();i++)
					{
						JSONObject paxJson = cancelSeatsArr.getJSONObject(i);
						
						seatNos.add(paxJson.getString("seatNo"));
					}
					
					 switch(type)
			        {
			           
			        	case JSON_PROP_BUS_CANCELTYPE_FULLCANCEL:
			        	{
			        		return setPassengersStatusforfullCancel(cancelOrder,order,order.getId(),reqJson);

			        	}
			        		
			           
			        	
			        	case JSON_PROP_BUS_CANCELTYPE_CANCELPAX:
			        	{

			        		return setPassengersStatusforPartialCancel(cancelOrder,order,order.getId(),reqJson,seatNos);
			        	}
			                
			        		  
			            default:
			            	order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			        		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			        		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			        		saveOrder(order, "");
			        		cancelOrder.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			    			cancelOrder.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			    			cancelOrder.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			    			saveBusAmcl(cancelOrder, "");
			                return "no match for cancel/amend type";
			        }
					
				}
				
				
				
				
//			}
//		}
		

		
		
		
		return "SUCCESS";
		
	}

	private String setPassengersStatusforPartialCancel(BusAmCl cancelOrder,BusOrders order,String id,JSONObject reqJson,List<String> seatNos) throws BookingEngineDBException {
		
		String type = reqJson.getJSONObject(JSON_PROP_REQBODY).getString("type");
		Booking booking = bookingRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		booking.setStatus("onCancelRequest");
		
		JSONArray paxInfo = new JSONArray();
//		StringBuilder entityIds = new StringBuilder();
	    List<String> eids = new  ArrayList<String>();
	    
		JSONArray paxArr = new JSONArray(order.getPaxDetails());
		
		for(int i=0;i<paxArr.length();i++)
		{
			JSONObject paxJson =  paxArr.getJSONObject(i);
			for(String seatNo:seatNos)
			{
				if(seatNo.equalsIgnoreCase(paxJson.getString("seatNo")))
				{
					PassengerDetails pax = passengerRepository.findOne(paxJson.getString("paxID"));
					if(pax == null)
					{
						myLogger.info(String.format("Bus cancel request failed since paxID not found for req %s",reqJson));
						response.put("ErrorCode", "BE_ERR_BUS_002");
						response.put("ErrorMsg", BE_ERR_BUS_002);
						return response.toString();
					}
					else
					{
						pax.setStatus(OrderStatus.XL.getProductStatus());
						savePaxDetails(pax,"");
						
						eids.add(paxJson.getString("paxID"));
						JSONObject paxAmclJson = new JSONObject();
						paxAmclJson.put(JSON_PROP_PAXID, paxJson.getString("paxID"));
						paxAmclJson.put(JSON_PROP_BUS_SEATNO, paxJson.getString(JSON_PROP_BUS_SEATNO));
						paxInfo.put(paxAmclJson);
					}
					
					
				}
			}
		}
		
		JSONArray entityIds =new JSONArray(eids.toString());
//		String entityId = entityIds.toString();
		
		
		cancelOrder.setEntityID(entityIds.toString());
		cancelOrder.setStatus(OrderStatus.XL.getProductStatus());
		cancelOrder.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		cancelOrder.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		cancelOrder.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		saveBusAmcl(cancelOrder, "");
		
		order.setStatus("Pax cancelled");
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		
		saveOrder(order, "");
		myLogger.info(String.format(" Bus passenger cancellation req populated  for request %s",reqJson));
		return "SUCCESS";
		
	}

	private String setPassengersStatusforfullCancel(BusAmCl cancelOrder,BusOrders order,String orderId,JSONObject reqJson) throws BookingEngineDBException {
		
		String type = reqJson.getJSONObject(JSON_PROP_REQBODY).getString("type");
		Booking booking = bookingRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		if(booking==null)
		{
			myLogger.warn(String.format("Bus Booking Response could not be populated since no bookings found for req with bookID %s", reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID)));
			response.put("ErrorCode","BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			return response.toString();
		}
		else
		{
			booking.setStatus("onCancelRequest");
			
			
			JSONArray paxArr = new JSONArray(order.getPaxDetails());
			for(int i=0;i<paxArr.length();i++)
			{
				
				PassengerDetails pax = passengerRepository.findOne(paxArr.getJSONObject(i).getString("paxID"));
				if(pax == null)
				{
					myLogger.info(String.format("Bus cancel request failed since paxID not found for req %s",reqJson));
					response.put("ErrorCode", "BE_ERR_BUS_002");
					response.put("ErrorMsg", BE_ERR_BUS_002);
					return response.toString();
				}
				else
				{
					pax.setStatus(OrderStatus.XL.getProductStatus());
					savePaxDetails(pax,"");
				}
				
			}
			
			 List<String> eids = new  ArrayList<String>();
			 eids.add(orderId);
			JSONArray entityIds =new JSONArray(eids.toString());
			
			cancelOrder.setEntityID(entityIds.toString());
			cancelOrder.setStatus(OrderStatus.XL.getProductStatus());
			cancelOrder.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			
			cancelOrder.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			cancelOrder.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			saveBusAmcl(cancelOrder, "");
			
			order.setStatus(OrderStatus.XL.getProductStatus());
			order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			
			order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			
			
			saveOrder(order, "");
			myLogger.info(String.format(" Bus full cancellation req populated  for request %s",reqJson));

			
			return "SUCCEESS";
		}
		
	}

	

	


	@Override
	public String processAmClResponse(JSONObject resJson) {
		
		Booking booking = bookingRepository.findOne(resJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		List<BusOrders> orders = busRepository.findByBooking(booking);
		
			
		
		for(BusOrders order:orders)
		{
			
//		List<BusAmCl> cancelEntries  = busAmClRepository.findResponse(resJson.getJSONObject(JSON_PROP_RESBODY).getString("bookID"), order.getId(),resJson.getJSONObject(JSON_PROP_RESBODY).getString("requestType"), resJson.getJSONObject(JSON_PROP_RESBODY).getString("type"));
			 List<String> eids = new  ArrayList<String>();
			
			JSONArray paxDtlsArr = new JSONArray(order.getPaxDetails());
		JSONArray seatsToCancelArr = resJson.getJSONObject(JSON_PROP_RESBODY).getJSONObject("service").getJSONArray("seatsToCancel");
		List<String> seatNos = new ArrayList<String>();
		for(int i=0;i<seatsToCancelArr.length();i++)
		{
			JSONObject seatJson = seatsToCancelArr.getJSONObject(i);
			seatNos.add(seatJson.getString("seatNo"));
		}
		String type = resJson.getJSONObject(JSON_PROP_RESBODY).getString("type");
		List<BusAmCl> amendEntries = new ArrayList<BusAmCl>();
		int count=0;
		if(type.equalsIgnoreCase(JSON_PROP_BUS_CANCELTYPE_CANCELPAX))
		{
			boolean flag=false;
			for(int i=0;i<paxDtlsArr.length();i++)
			{
				JSONObject paxJson = paxDtlsArr.getJSONObject(i);
				for(String seatNo:seatNos)
				{
					if(seatNo.equals(paxJson.getString("seatNo")))
					{
						 eids.add(paxJson.getString(JSON_PROP_PAXID));
						 JSONArray entityIds =new JSONArray(eids.toString());
						amendEntries = busAmClRepository.findforResponseUpdate(
								resJson.getJSONObject(JSON_PROP_RESBODY).getString("entityName"),
								entityIds.toString(),
								resJson.getJSONObject(JSON_PROP_RESBODY).getString("type"),
								resJson.getJSONObject(JSON_PROP_RESBODY).getString("requestType"));
						count++;
							if(count==seatsToCancelArr.length())
								{
								flag=true;
								break;
								}
								
					}
				}
				
				if(flag)
					break;
			}
			
		}
		else if(type.equals(JSON_PROP_BUS_CANCELTYPE_FULLCANCEL))
		{
			 eids.add(order.getId());
			 JSONArray entityIds =new JSONArray(eids.toString()); 
			amendEntries = busAmClRepository.findforResponseUpdate(
					resJson.getJSONObject(JSON_PROP_RESBODY).getString("entityName"),
					entityIds.toString(),
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
			BusAmCl cancelOrder = amendEntries.get(0);	
			String prevOrder = cancelOrder.toString();//TODO:doubt???
			
			cancelOrder.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			cancelOrder.setLastModifiedBy(resJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));
			cancelOrder.setRefundAmount(resJson.getJSONObject(JSON_PROP_RESBODY).getJSONObject("service").getBigDecimal("refundAmount").toString());
			cancelOrder.setRefundAmountCurrency(resJson.getJSONObject(JSON_PROP_RESBODY).getJSONObject("service").getString("currency"));
			
			cancelOrder.setStatus("cancelled");
			saveBusAmcl(cancelOrder, prevOrder);
		}
	}
		return "SUCCESS";
	}

}
