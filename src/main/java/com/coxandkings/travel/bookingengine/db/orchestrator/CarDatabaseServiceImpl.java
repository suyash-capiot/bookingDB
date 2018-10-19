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
import com.coxandkings.travel.bookingengine.db.model.CarOrders;
import com.coxandkings.travel.bookingengine.db.enums.BookingAttribute;
import com.coxandkings.travel.bookingengine.db.enums.BookingStatus;
import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CarAmCl;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.CarDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.CarAmClRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("Car")
@Transactional(readOnly = false)
public class CarDatabaseServiceImpl implements Constants, ErrorConstants, CancelAmendTypes, DataBaseService {

	@Autowired
	@Qualifier("Car")
	private CarDatabaseRepository carRepository;

	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;

	@Autowired
	@Qualifier("CarAmCl")
	private CarAmClRepository carAmClRepository;
	
	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingService;

	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;

	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());

	JSONObject response = new JSONObject();

	public boolean isResponsibleFor(String product) {
		return "car".equalsIgnoreCase(product);
	}

	public String processBookRequest(JSONObject bookRequestJson) throws JSONException, BookingEngineDBException {

		Booking booking = bookingRepository
				.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_CAR_BOOKID));

		if (booking == null)
			booking = bookingService.processBookRequest(bookRequestJson, false);

		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_CAR_CARRENTALARR)) {

			CarOrders order = populateCarData((JSONObject) orderJson, bookRequestJson, booking);
			saveOrder(order, "");
		}

		return "success";
	}

	public CarOrders populateCarData(JSONObject bookReq, JSONObject bookRequestJson, Booking booking)
			throws BookingEngineDBException {

		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		CarOrders order = new CarOrders();

		order.setBooking(booking);
		// TODO: change the client ID to userID once you get in header
		order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
		order.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setStatus(OrderStatus.RQ.getProductStatus());
		order.setClientCurrency(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setClientType(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
		order.setSupplierID(bookReq.getString(JSON_PROP_SUPPREF));
		order.setTripType(bookReq.optString(JSON_PROP_CAR_TRIPTYPE));

		order.setProductSubCategory(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_CAR_PRODSUBCATTYPE));
		
		JSONObject suppTotalFare = bookReq.getJSONObject(JSON_PROP_CAR_SUPPPRICEINFO).getJSONObject(JSON_PROP_CAR_TOTALFARE);
		// suppTotalFare.remove(JSON_PROP_CAR_PRICEDCOVERAGES);
		// suppTotalFare.remove(JSON_PROP_CAR_SPLEQUIPS);

		order.setSuppFares(suppTotalFare.toString());
		order.setSupplierTotalPrice(suppTotalFare.getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setSupplierPriceCurrencyCode(suppTotalFare.getString(JSON_PROP_CAR_CURRENCYCODE));
		order.setRoe(bookReq.getBigDecimal(JSON_PROP_ROE).toString());

		JSONObject totalFare = bookReq.getJSONObject(JSON_PROP_CAR_TOTALPRICEINFO).getJSONObject(JSON_PROP_CAR_TOTALFARE);
		JSONObject extraEquips = totalFare.optJSONObject(JSON_PROP_CAR_SPLEQUIPS);
		order.setExtraEquipments(extraEquips == null ? new JSONObject().toString() : extraEquips.toString());

		JSONObject pricedCovrgs = totalFare.optJSONObject(JSON_PROP_CAR_PRICEDCOVERAGES);
		order.setPricedCoverages(pricedCovrgs == null ? new JSONObject().toString() : pricedCovrgs.toString());
		
		//Cancellation Policy
		JSONObject cancelPolicy = bookReq.optJSONObject(JSON_PROP_CANCELLATIONPOLICY);
		order.setCancelPolicy(cancelPolicy==null ? new JSONObject().toString() : cancelPolicy.toString());
		
		totalFare.remove(JSON_PROP_CAR_PRICEDCOVERAGES);
		totalFare.remove(JSON_PROP_CAR_SPLEQUIPS);

		// TODO : To Check if to insert TotalFare, breakups are getting populated
		// order.setTotalFares(totalFare.toString());
		order.setTotalPrice(totalFare.getBigDecimal(JSON_PROP_AMOUNT).toString());
		order.setTotalPriceCurrencyCode(totalFare.getString(JSON_PROP_CAR_CURRENCYCODE));
		order.setTotalBaseFare(totalFare.getJSONObject(JSON_PROP_BASEFARE).toString());

		JSONObject receivables = totalFare.optJSONObject(JSON_PROP_RECEIVABLES);
		order.setTotalPriceReceivables(receivables==null ? new JSONObject().toString() :receivables.toString());
		
		JSONObject companyTaxes = totalFare.optJSONObject(JSON_PROP_COMPANYTAXES);
		order.setTotalPriceCompanyTaxes(companyTaxes==null ? new JSONObject().toString() : companyTaxes.toString());
		
		JSONObject fees = totalFare.optJSONObject(JSON_PROP_FEES);
		order.setTotalPriceFees(fees == null ? new JSONObject().toString() : fees.toString());

		JSONObject taxes = totalFare.optJSONObject(JSON_PROP_TAXES);
		order.setTotalPriceTaxes(taxes == null ? new JSONObject().toString() : taxes.toString());

		JSONObject vehicleDetails = bookReq.getJSONObject(JSON_PROP_CAR_VEHICLEINFO);
		JSONObject rentalDetails = new JSONObject(bookReq.toString());
		rentalDetails.remove(JSON_PROP_PAXDETAILS);
		rentalDetails.remove(JSON_PROP_CAR_TOTALPRICEINFO);
		rentalDetails.remove(JSON_PROP_CAR_SUPPPRICEINFO);
		rentalDetails.remove(JSON_PROP_CAR_VEHICLEINFO);

		order.setRentalDetails(rentalDetails.toString());
		order.setCarDetails(vehicleDetails.toString());

		Set<PassengerDetails> setPaxDetails = new HashSet<PassengerDetails>();
		setPaxDetails = readPassengerDetails(bookReq.getJSONArray(JSON_PROP_PAXDETAILS), order);
		JSONArray paxIds = new JSONArray();
		for (PassengerDetails pax : setPaxDetails) {
			JSONObject paxJson = new JSONObject();
			paxJson.put("paxId", pax.getPassanger_id());
			paxIds.put(paxJson);
		}

		order.setPaxDetails(paxIds.toString());
		Set<SupplierCommercial> suppComms = new HashSet<SupplierCommercial>();
		suppComms = readSuppCommercials(bookReq.getJSONObject(JSON_PROP_CAR_SUPPPRICEINFO).getJSONArray(JSON_PROP_CAR_SUPPLIERCOMMS), order);

		Set<ClientCommercial> clientComms = new HashSet<ClientCommercial>();
		clientComms = readClientCommercials(bookReq.getJSONObject(JSON_PROP_CAR_TOTALPRICEINFO)
				.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS), order);
		order.setClientCommercial(clientComms);
		order.setSuppcommercial(suppComms);

		return order;

	}

	private Set<SupplierCommercial> readSuppCommercials(JSONArray suppCommsJsonArray, CarOrders order) {

		Set<SupplierCommercial> suppCommercialsSet = new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;

		for (int i = 0; i < suppCommsJsonArray.length(); i++) {
			JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);

			suppCommercials = new SupplierCommercial();
			suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
			suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
			suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
			suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));

			suppCommercials.setProduct(JSON_PROP_PRODUCTCAR);
			suppCommercials.setOrder(order);
			suppCommercialsSet.add(suppCommercials);
		}
		return suppCommercialsSet;
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, CarOrders order) {

		Set<ClientCommercial> clientCommercialsSet = new HashSet<ClientCommercial>();
		ClientCommercial clientCommercials;

		for (int i = 0; i < clientCommsJsonArray.length(); i++) {

			JSONObject totalClientComm = clientCommsJsonArray.getJSONObject(i);

			String clientID = totalClientComm.getString(JSON_PROP_CLIENTID);
			String parentClientID = totalClientComm.getString(JSON_PROP_PARENTCLIENTID);
			String commercialEntityType = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYTYPE);
			String commercialEntityID = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYID);

			boolean companyFlag = (i == 0) ? true : false;

			JSONArray clientComms = totalClientComm.getJSONArray(JSON_PROP_CLIENTCOMMERCIALSTOTAL);

			for (int j = 0; j < clientComms.length(); j++) {

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

				clientCommercials.setProduct(JSON_PROP_PRODUCTCAR);
				clientCommercials.setOrder(order);
				clientCommercialsSet.add(clientCommercials);
			}
		}
		return clientCommercialsSet;
	}

	private Set<PassengerDetails> readPassengerDetails(JSONArray paxJsonArray, CarOrders carOrder)
			throws BookingEngineDBException {

		Set<PassengerDetails> paxDetailsSet = new HashSet<PassengerDetails>();
		PassengerDetails paxDetails;
		for (int i = 0; i < paxJsonArray.length(); i++) {

			JSONObject currentPaxDetails = paxJsonArray.getJSONObject(i);
			paxDetails = new PassengerDetails();
			// TODO : Set isLead Traveler.
			paxDetails.setIsLeadPax(currentPaxDetails.optBoolean(JSON_PROP_ISLEADPAX));
			paxDetails.setTitle(currentPaxDetails.getString(JSON_PROP_TITLE));
			paxDetails.setFirstName(currentPaxDetails.getString(JSON_PROP_FIRSTNAME));
			paxDetails.setStatus(OrderStatus.RQ.getProductStatus());
			paxDetails.setMiddleName(currentPaxDetails.optString(JSON_PROP_MIDDLENAME));
			paxDetails.setLastName(currentPaxDetails.optString(JSON_PROP_SURNAME));
			paxDetails.setBirthDate(currentPaxDetails.optString(JSON_PROP_CAR_DOB));
			paxDetails.setGender(currentPaxDetails.getString(JSON_PROP_GENDER));
			paxDetails.setContactDetails(currentPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			paxDetails.setAddressDetails(currentPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());

			// TODO:change it to userID later
			paxDetails.setLastModifiedBy("");
			paxDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			paxDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

			savePaxDetails(paxDetails, "");
			paxDetailsSet.add(paxDetails);

		}
		return paxDetailsSet;
	}

	public String processBookResponse(JSONObject bookResponseJson) {

		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		List<CarOrders> orders = carRepository.findByBooking(booking);
		if (booking == null) {
			myLogger.warn(String.format(
					"CAR Booking Response could not be populated since no bookings found for req with bookID %s",
					bookResponseJson.getJSONObject("responseBody").getString("bookID")));
			response.put("ErrorCode", "BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			return response.toString();
		}
		if (orders.size() == 0) {
				myLogger.warn(String.format("Car Booking Response could not be populated since no car orders found for req with bookID %s",
						bookResponseJson.getJSONObject("responseBody").getString("bookID")));
				response.put("ErrorCode", "BE_ERR_CAR_005");
				response.put("ErrorMsg", BE_ERR_CAR_005);
				return response.toString();
		} 
		
		JSONArray reservationArr = bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_CAR_RESERVATION);
		for (int i = 0; i < reservationArr.length(); i++) {
			JSONObject currCarInfoObj = reservationArr.getJSONObject(i);
			CarOrders order = null;
			try {
				order = orders.get(i);
			}catch(ArrayIndexOutOfBoundsException e) {
				///Ideally code will never come here.
				myLogger.warn(String.format("Order no.%d from req could not be confirmed since corresponding carOrder for BookID %s not found in DB", i, 
						bookResponseJson.getJSONObject("responseBody").getString("bookID")));
				response.put("ErrorCode", "BE_ERR_CAR_005");
				response.put("ErrorMsg", BE_ERR_CAR_005);
				continue;
			}
			order.setStatus(OrderStatus.OK.getProductStatus());
			// TODO : May Need to Change Later
			JSONArray references = currCarInfoObj.optJSONArray(JSON_PROP_CAR_REFERENCES);
			order.setCarReferences(references!=null ? references.toString() : "[]");
			String reservationId = currCarInfoObj.getString(JSON_PROP_CAR_RESERVATIONID);
			order.setCarReservationId(reservationId);
			
			order.setBookingDateTime(new Date().toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			carRepository.save(order);
			
		}
		myLogger.info(String.format("Car Booking Response populated successfully for req with bookID %s = %s",
				bookResponseJson.getJSONObject("responseBody").getString("bookID"),
				bookResponseJson.toString()));
	
		return "SUCCESS";
	}
	

	// This is to process cancel/amend request for Car
	public String processAmClRequest(JSONObject req) throws BookingEngineDBException {
		
		JSONObject reqBodyJson = req.getJSONObject(JSON_PROP_REQBODY);
		JSONObject modifyReq = reqBodyJson.getJSONObject(JSON_PROP_CAR_CARRENTALARR);
		String type = reqBodyJson.getString("type");
		// TODO: Check if order level status needs to be updated for each request

		CarAmCl amClEntry = new CarAmCl();
		amClEntry.setEntityName(reqBodyJson.getString("entityName"));
		amClEntry.setRequestType(reqBodyJson.getString("requestType"));
		amClEntry.setSupplierCharges("0");
		amClEntry.setDescription(type);
		amClEntry.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
		amClEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		amClEntry.setLastModifiedBy(req.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		amClEntry.setStatus(OrderStatus.RQ.getProductStatus());
		
		String id = modifyReq.getString(JSON_PROP_CAR_RESERVATIONID);
		
		//Getting the Order by querying with the supplier Book reference. 
		String suppId = modifyReq.optString("supplierRef");
		List<CarOrders> carOrder = carRepository.findBySuppReservationId(suppId, id);
		if(carOrder.size()==0) {
			myLogger.info(String.format("No Order found for the given req %s", req));
			return String.format("No Order found for the given req %s", req);
		}
		
		//Ideally different orders won't have same suppBookRef.So no need for loop
		CarOrders cOrder = carOrder.get(0);
		String bookingAttribute = cOrder.getBookingAttribute();
		JSONArray bookingAttributeArray = bookingAttribute != null ? new JSONArray(bookingAttribute) : new JSONArray();
		JSONObject bookingAttributeObj = new JSONObject();
		bookingAttributeObj.put(BookingAttribute.RAMD.toString(),BookingAttribute.RAMD.getBookingAttribute());
		bookingAttributeArray.put(bookingAttributeObj);
		cOrder.setBookingAttribute(bookingAttributeArray.toString());
		saveOrder(cOrder, "");
		amClEntry.setOrderID(cOrder.getId());
		amClEntry.setEntityID(new JSONArray().put(cOrder.getId()).toString());
		
		switch (type) {

			case JSON_PROP_CAR_CANCELTYPE_ADDANCILLARY:
				return addAncillary(req, modifyReq, cOrder, amClEntry);
			case  JSON_PROP_CAR_CANCELTYPE_CANCELANCILLARY:
				return removeAncillary(req, modifyReq, cOrder, amClEntry);
			case  JSON_PROP_CAR_CANCELTYPE_PAXINFOUPDATE:
				//Set entityId to paxIds inside
				return updatePaxDetails(req, modifyReq, type, amClEntry);
			//TODO  : Remove this type ,No longer supported
			case  JSON_PROP_CAR_CANCELTYPE_FULLCANCEL:
				return fullCancel(req, modifyReq, cOrder, amClEntry);
			default:
            	response.put("ErrorCode", "BE_ERR_005");
            	response.put("ErrorMsg", BE_ERR_005);
            	myLogger.info(String.format("Update type %s not found for req %s", type, modifyReq.toString()));
                return (response.toString());
		}
	
	}

	private String upgradeCar(JSONObject req, JSONObject modifyReq, CarOrders order ,CarAmCl amClEntry) throws BookingEngineDBException {

		String prevOrder = order.toString();
		order.setStatus("Amended");
		order.setLastModifiedBy(req.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

		JSONObject vehicleInfo = modifyReq.getJSONObject(JSON_PROP_CAR_VEHICLEINFO);
		vehicleInfo.put(JSON_PROP_STATUS, "Amended");
		order.setCarDetails(vehicleInfo.toString());

		saveOrder(order, prevOrder);
		saveCarAmCl(amClEntry, "");
		myLogger.info(String.format("Car AMCL Request populated successfully for req %s", req));
		return "SUCCESS";

	}

	private String changeRentalInfo(JSONObject req, JSONObject modifyReq, CarOrders order, CarAmCl amClEntry)
			throws BookingEngineDBException {

		String prevOrder = order.toString();
		order.setStatus("Amended");
		order.setLastModifiedBy(req.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

		JSONObject rentalInfo = new JSONObject(order.getRentalDetails());
		rentalInfo.put(JSON_PROP_CAR_PICKUPDATE, modifyReq.getString(JSON_PROP_CAR_PICKUPDATE));
		rentalInfo.put(JSON_PROP_CAR_PICKUPLOCCODE, modifyReq.getString(JSON_PROP_CAR_PICKUPLOCCODE));
		rentalInfo.put(JSON_PROP_CAR_RETURNDATE, modifyReq.getString(JSON_PROP_CAR_RETURNDATE));
		rentalInfo.put(JSON_PROP_CAR_RETURNLOCCODE, modifyReq.getString(JSON_PROP_CAR_RETURNLOCCODE));
		rentalInfo.put("city", modifyReq.getString("city"));
		rentalInfo.put(JSON_PROP_STATUS, "Amended");

		order.setRentalDetails(rentalInfo.toString());

		saveOrder(order, prevOrder);
		saveCarAmCl(amClEntry, "");
		myLogger.info(String.format("Car AMCL Request populated successfully for req %s", req));
		return "SUCCESS";

	}

	public String processAmClResponse(JSONObject resJson) throws Exception{
		
		JSONObject resBodyJson = resJson.getJSONObject(JSON_PROP_RESBODY); 
		JSONObject subRes = resBodyJson.getJSONObject(JSON_PROP_CAR_CARRENTALARR);
		
		String reservationId = subRes.getString(JSON_PROP_CAR_RESERVATIONID);
		
		//Getting the Order by querying with the supplier Book reference. 
		String suppId = subRes.optString("supplierRef");
		List<CarOrders> carOrder = carRepository.findBySuppReservationId(suppId, reservationId);
		if(carOrder.size()==0) {
			myLogger.info(String.format("No Order found for the given req %s", resJson));
			return String.format("No Order found for the given req %s", resJson);
		}
		CarOrders order = carOrder.get(0);
		String bookingAttribute = order.getBookingAttribute();
		JSONArray bookingAttributeArray = bookingAttribute != null ? new JSONArray(bookingAttribute) : new JSONArray();
	   
		JSONObject bookingAttributeObj=new JSONObject();
		bookingAttributeObj.put(BookingAttribute.AMENDED.toString(), BookingAttribute.AMENDED.getBookingAttribute());
		bookingAttributeArray.put(bookingAttributeObj);
		order.setBookingAttribute(bookingAttributeArray.toString());
		saveOrder(order, "");
		
		List<CarAmCl> amendEntries = carAmClRepository.findforResponseUpdate(resBodyJson.getString("entityName"),
				new JSONArray().put(order.getId()).toString(), resBodyJson.getString("type"), resBodyJson.getString("requestType"));

		if (amendEntries.size() == 0) {
			// TODO: handle this before it goes in prod
			System.out.println("no amend entry found. Request might not have been populated");
		}

		else if (amendEntries.size() > 1) {
			// TODO: handle this before it goes in prod
			System.out.println("multiple amend entries found. Dont know which one to update");
		}

		else {
			CarAmCl amendEntry = amendEntries.get(0);
			String prevOrder = amendEntry.toString();
			amendEntry.setCompanyCharges(subRes.optString("companyCharges"));
			amendEntry.setSupplierCharges(subRes.optString("supplierCharges"));
			amendEntry.setSupplierChargesCurrencyCode(subRes.optString("supplierChargesCurrencyCode"));
			amendEntry.setCompanyChargesCurrencyCode(subRes.optString("companyChargesCurrencyCode"));
			amendEntry.setStatus(OrderStatus.OK.getProductStatus());
			amendEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			amendEntry.setLastModifiedBy(resJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));

			JSONObject totalFare = subRes.getJSONObject(JSON_PROP_CAR_TOTALPRICEINFO).getJSONObject(JSON_PROP_CAR_TOTALFARE);
			
			JSONObject totalFeesJson = totalFare.optJSONObject(JSON_PROP_FEES);
			JSONObject totaltaxesJson = totalFare.optJSONObject(JSON_PROP_TAXES);
			JSONObject totalPricedCovgsJson = totalFare.optJSONObject(JSON_PROP_CAR_PRICEDCOVERAGES);
			JSONObject totalSplEquipsJson = totalFare.optJSONObject(JSON_PROP_CAR_SPLEQUIPS);
			
			amendEntry.setTotalBaseFare(totalFare.getJSONObject(JSON_PROP_BASEFARE).toString());
			amendEntry.setTotalPrice(totalFare.getBigDecimal(JSON_PROP_AMOUNT).toString());
			amendEntry.setTotalPriceCurrencyCode(totalFare.getString(JSON_PROP_CURRENCYCODE).toString());
			amendEntry.setTotalPriceFees(totalFeesJson!=null ? totalFeesJson.toString() : new JSONObject().toString());
			amendEntry.setTotalPriceTaxes(totaltaxesJson!=null ? totaltaxesJson.toString() : new JSONObject().toString());

			amendEntry.setPricedCoverages(totalPricedCovgsJson!=null ? totalPricedCovgsJson.toString() : new JSONObject().toString());
			amendEntry.setExtraEquipments(totalSplEquipsJson!=null ? totalSplEquipsJson.toString() : new JSONObject().toString());

			// TODO: also set the currency codes and breakups before saving
			saveCarAmCl(amendEntry, prevOrder);
		}
		return "SUCCESS";

	}

	private String fullCancel(JSONObject req, JSONObject modifyReq, CarOrders order, CarAmCl amClEntry) throws BookingEngineDBException {

		String prevOrder = order.toString();
		order.setStatus(OrderStatus.XL.getProductStatus());
		order.setLastModifiedBy(req.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		
		saveOrder(order, prevOrder);
		saveCarAmCl(amClEntry, "");
		myLogger.info(String.format("Car AMCL Request populated successfully for req %s", req));
		return "SUCCESS";
	}

	private String addAncillary(JSONObject req, JSONObject modifyReq, CarOrders order, CarAmCl amClEntry) throws BookingEngineDBException {

		String prevOrder = order.toString();
		order.setLastModifiedBy(req.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setStatus("Amended");

		JSONArray reqEquipsArr = modifyReq.optJSONArray(JSON_PROP_CAR_SPLEQUIPS);
		JSONArray pricedCovrgsArr = modifyReq.optJSONArray(JSON_PROP_CAR_PRICEDCOVERAGES);
		// Adding SpecialEquipments
		if (reqEquipsArr != null) {
			JSONObject extraEquip = new JSONObject(order.getExtraEquipments());
			JSONArray dBEquipsArr = extraEquip.optJSONArray(JSON_PROP_CAR_SPLEQUIP) != null
					? extraEquip.getJSONArray(JSON_PROP_CAR_SPLEQUIP)
					: new JSONArray();

			for (int i = 0; i < reqEquipsArr.length(); i++) {
				JSONObject reqEquipJson = reqEquipsArr.getJSONObject(i);
				String equipType = reqEquipJson.getString("equipType");
				boolean isPresent = false;
				for (int j = 0; j < dBEquipsArr.length(); j++) {
					JSONObject dBEquipJson = dBEquipsArr.getJSONObject(j);
					// If already present increase quantity
					if (dBEquipJson.getString("equipType").equalsIgnoreCase(equipType)) {
						Integer quantity = dBEquipJson.getInt(JSON_PROP_QTY);
						dBEquipJson.put(JSON_PROP_QTY, reqEquipJson.optInt(JSON_PROP_QTY, 1) + quantity);
						dBEquipJson.put(JSON_PROP_STATUS, "Amended");
						isPresent = true;
					}
				}
				if (isPresent == false) {
					reqEquipJson.put(JSON_PROP_STATUS, "Added");
					dBEquipsArr.put(reqEquipJson);
				}
			}
			order.setExtraEquipments(extraEquip.toString());
		}
		// Adding PricedCoverages
		if (pricedCovrgsArr != null) {
			JSONObject pricedCovrgs = new JSONObject(order.getExtraEquipments());
			JSONArray dBpricedCovrgArr = pricedCovrgs.optJSONArray(JSON_PROP_CAR_PRICEDCOVERAGE) != null
					? pricedCovrgs.getJSONArray(JSON_PROP_CAR_PRICEDCOVERAGE)
					: new JSONArray();

			for (int i = 0; i < pricedCovrgsArr.length(); i++) {
				JSONObject pricedCovrgsJson = pricedCovrgsArr.getJSONObject(i);
				String coverageType = pricedCovrgsJson.getString("coverageType");
				boolean isPresent = false;
				for (int j = 0; j < dBpricedCovrgArr.length(); j++) {
					JSONObject dBpricedCovrgJson = dBpricedCovrgArr.getJSONObject(j);
					if (dBpricedCovrgJson.getString("coverageType").equalsIgnoreCase(coverageType)) {
						isPresent = true;
					}
				}
				// If not already Present then Coverage is added
				if (isPresent == false) {
					pricedCovrgsJson.put(JSON_PROP_STATUS, "Added");
					dBpricedCovrgArr.put(pricedCovrgsJson);
				}
			}
			order.setPricedCoverages(pricedCovrgs.toString());
		}
		saveOrder(order, prevOrder);
		saveCarAmCl(amClEntry, "");
		myLogger.info(String.format("Car AMCL Request populated successfully for req %s", req));
		return "SUCCESS";
	}

	private String removeAncillary(JSONObject req, JSONObject modifyReq, CarOrders order, CarAmCl amClEntry) throws BookingEngineDBException {

		String prevOrder = order.toString();
		order.setLastModifiedBy(req.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setStatus("Amended");

		JSONArray reqEquipsArr = modifyReq.optJSONArray(JSON_PROP_CAR_SPLEQUIPS);
		JSONArray pricedCovrgsArr = modifyReq.optJSONArray(JSON_PROP_CAR_PRICEDCOVERAGES);
		// Removing SpecialEquipments
		if (reqEquipsArr != null) {
			JSONObject extraEquip = new JSONObject(order.getExtraEquipments());
			JSONArray dBEquipsArr = extraEquip.optJSONArray(JSON_PROP_CAR_SPLEQUIP) != null
					? extraEquip.getJSONArray(JSON_PROP_CAR_SPLEQUIP)
					: new JSONArray();

			for (int i = 0; i < reqEquipsArr.length(); i++) {
				JSONObject reqEquipJson = reqEquipsArr.getJSONObject(i);
				String equipType = reqEquipJson.getString("equipType");
				Boolean isPresent = false;
				for (int j = 0; j < dBEquipsArr.length(); j++) {
					JSONObject dBEquipJson = dBEquipsArr.getJSONObject(j);
					// If already present increase quantity
					if (dBEquipJson.getString("equipType").equalsIgnoreCase(equipType)) {
						Integer quantity = dBEquipJson.getInt(JSON_PROP_QTY);
						if (reqEquipJson.optInt(JSON_PROP_QTY) == 0)
							dBEquipJson.put(JSON_PROP_STATUS, "Cancelled");
						else {
							Integer newQuantity = quantity - reqEquipJson.getInt(JSON_PROP_QTY);
							dBEquipJson.put(JSON_PROP_QTY, newQuantity < 0 ? 0 : newQuantity);
							dBEquipJson.put(JSON_PROP_STATUS, "Amended");
						}
						isPresent = true;
					}
				}
				if (isPresent == false) {
					myLogger.info(String.format("Equipment %s cannot be removed as it was not added", equipType));
				}
			}
			order.setExtraEquipments(extraEquip.toString());
		}
		// Removing PricedCoverages
		if (pricedCovrgsArr != null) {
			JSONObject pricedCovrgs = new JSONObject(order.getExtraEquipments());
			JSONArray dBpricedCovrgArr = pricedCovrgs.optJSONArray(JSON_PROP_CAR_PRICEDCOVERAGE) != null
					? pricedCovrgs.getJSONArray(JSON_PROP_CAR_PRICEDCOVERAGE)
					: new JSONArray();

			for (int i = 0; i < pricedCovrgsArr.length(); i++) {
				JSONObject pricedCovrgsJson = pricedCovrgsArr.getJSONObject(i);
				String coverageType = pricedCovrgsJson.getString("coverageType");
				boolean isPresent = false;
				for (int j = 0; j < dBpricedCovrgArr.length(); j++) {
					JSONObject dBpricedCovrgJson = dBpricedCovrgArr.getJSONObject(j);
					if (dBpricedCovrgJson.getString("coverageType").equalsIgnoreCase(coverageType)) {
						pricedCovrgsJson.put("status", "Cancelled");
						isPresent = true;
					}
				}
				// If not already Present then Coverage is added
				if (isPresent == false) {
					myLogger.info(String.format("Coverage %s cannot be removed as it is not added", coverageType));
				}
			}
			order.setPricedCoverages(pricedCovrgs.toString());
		}

		saveOrder(order, prevOrder);
		saveCarAmCl(amClEntry, "");
		myLogger.info(String.format("Car AMCL Request populated successfully for req %s", req));
		return "SUCCESS";
	}

	// TODO: Check for what statuses we need to have in pax table
	private String updatePaxDetails(JSONObject req, JSONObject modifyReq, String type, CarAmCl amClEntry)
			throws BookingEngineDBException {

		String prevOrder;
		JSONArray paxIds = new JSONArray();
		JSONArray paxArr = modifyReq.getJSONArray(JSON_PROP_PAXDETAILS);
		for (int i = 0; i < paxArr.length(); i++) {

			JSONObject currentPaxDetails = paxArr.getJSONObject(i);
			paxIds.put(currentPaxDetails.getString("paxId"));
			PassengerDetails paxDetails;
			if (type.equals(JSON_PROP_CAR_CANCELTYPE_ADDDRIVER)) {
				paxDetails = new PassengerDetails();
				paxDetails.setFirstName(currentPaxDetails.getString(JSON_PROP_FIRSTNAME));
				paxDetails.setMiddleName(currentPaxDetails.getString(JSON_PROP_MIDDLENAME));
				paxDetails.setLastName(currentPaxDetails.getString(JSON_PROP_SURNAME));
				prevOrder = "";
				paxDetails.setStatus("Added");
				paxDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			} else if (type.equals(JSON_PROP_CAR_CANCELTYPE_PAXINFOUPDATE)) {
				paxDetails = passengerRepository.findOne(currentPaxDetails.getString("paxId"));
				prevOrder = paxDetails.toString();
				// Suppliers does not allow to update
				// First Name and Last Name
				paxDetails.setStatus("Updated");
			} else {
				paxDetails = passengerRepository.findOne(currentPaxDetails.getString("paxId"));
				paxDetails.setFirstName(currentPaxDetails.getString(JSON_PROP_FIRSTNAME));
				paxDetails.setMiddleName(currentPaxDetails.getString(JSON_PROP_MIDDLENAME));
				paxDetails.setLastName(currentPaxDetails.getString(JSON_PROP_SURNAME));
				prevOrder = paxDetails.toString();
				paxDetails.setStatus("Cancelled");
			}

			paxDetails.setTitle(currentPaxDetails.getString(JSON_PROP_TITLE));
			paxDetails.setBirthDate(currentPaxDetails.getString(JSON_PROP_DOB));
			paxDetails.setIsLeadPax(currentPaxDetails.optBoolean(JSON_PROP_ISLEADPAX));
			paxDetails.setContactDetails(currentPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			paxDetails.setAddressDetails(currentPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
			paxDetails.setLastModifiedBy(req.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
			paxDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			
			amClEntry.setEntityID(paxIds.toString());
			savePaxDetails(paxDetails, prevOrder);
			saveCarAmCl(amClEntry, "");
			myLogger.info(String.format("Car AMCL Request populated successfully for req %s", req));
		}
		return "SUCCESS";
	}

	public Booking saveBookingOrder(Booking order, String prevOrder) throws BookingEngineDBException {
		Booking orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, Booking.class);
		} catch (InvocationTargetException | IllegalAccessException e) {
			myLogger.fatal("Error while saving Car Booking object : " + e);
			// myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save Car Booking object");
		}
		return bookingRepository.saveOrder(orderObj, prevOrder);
	}

	public CarAmCl saveCarAmCl(CarAmCl currentOrder, String prevOrder) {
		CarAmCl orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, CarAmCl.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return carAmClRepository.saveOrder(orderObj, prevOrder);
	}

	private CarOrders saveOrder(CarOrders order, String prevOrder) throws BookingEngineDBException {
		CarOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, CarOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			myLogger.fatal("Error while saving Car order object : " + e);
			// myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save car order object");
		}
		return carRepository.saveOrder(orderObj, prevOrder);
	}

	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevPaxDetails)
			throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			myLogger.fatal("Error while saving Car passenger object : " + e);
			// myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save Car passenger object");
		}
		return passengerRepository.saveOrder(orderObj, prevPaxDetails);
	}

}
