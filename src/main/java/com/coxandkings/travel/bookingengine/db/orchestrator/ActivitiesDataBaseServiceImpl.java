package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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

import com.coxandkings.travel.bookingengine.db.enums.BookingStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AmCl;
import com.coxandkings.travel.bookingengine.db.model.ActivitiesOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.repository.AmClRepository;
import com.coxandkings.travel.bookingengine.db.repository.ActivitiesDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("Activity")
@Transactional(readOnly = false)
public class ActivitiesDataBaseServiceImpl implements DataBaseService, Constants, ErrorConstants, CancelAmendTypes {

	Logger logger = LoggerUtil.getLoggerInstance(this.getClass());

	@Autowired
	@Qualifier("Activity")
	private ActivitiesDatabaseRepository activitiesDatabaseRepository;

	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;

	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;

	@Autowired
	@Qualifier("AccoAmCl")
	private AmClRepository amClRepository;
	
	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingService;

	@Override
	public boolean isResponsibleFor(String product) {
		return Constants.JSON_PROP_ACTIVITIES_CATEGORY.equals(product);
	}

	@Override
	public String processBookRequest(JSONObject bookRequestJson) throws BookingEngineDBException {
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject("requestHeader");
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject("requestBody").getString("bookID"));

		if (booking == null)
			booking = bookingService.processBookRequest(bookRequestJson,false);

		for (Object orderJson : bookRequestJson.getJSONObject("requestBody").getJSONArray("reservations")) {

			ActivitiesOrders order = populateActivitiesData((JSONObject) orderJson, bookRequestHeader, booking);
			System.out.println(order);
			saveActivitiesOrder(order, "");
		}

		return "SUCCESS";
	}

	// TODO : orderID is not getting set in amCl Table. Need to look into it now.
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException {
		JSONObject reqBody = reqJson.getJSONObject(JSON_PROP_REQBODY);

		JSONArray amendCancelReqs = reqBody.getJSONArray("activityInfo");

		for (int amendCancelCount = 0; amendCancelCount < amendCancelReqs.length(); amendCancelCount++) {
			JSONObject amendCancelJSONObject = amendCancelReqs.getJSONObject(amendCancelCount);
			/**
			 * type will be : ADDPASSENGER , CANCELPASSENGER ,UPDATEPASSENGER
			 * ,FULLCANCELLATION
			 */
			String type = amendCancelJSONObject.getString("type");
			// TODO: Check if order level status needs to be updated for each request

			AmCl amendEntry = new AmCl();

			JSONArray entityIDJSONArr = new JSONArray();
			JSONObject entityID = new JSONObject();
			if ("pax".equals(amendCancelJSONObject.getString("entityName"))) {
				String paxConcatenatedID = amendCancelJSONObject.getString("entityId");
				String[] paxID = paxConcatenatedID.split("\\|");
				for (int i = 0; i < paxID.length; i++) {
					JSONObject paxIDJSONObject = new JSONObject();
					paxIDJSONObject.put("entityId", paxID[i]);
					entityIDJSONArr.put(paxIDJSONObject);
				}
			} else {
				entityID.put("entityId", amendCancelJSONObject.getString("entityId"));
				entityIDJSONArr.put(entityID);
			}

			amendEntry.setEntityID(entityIDJSONArr.toString());
			amendEntry.setEntityName(amendCancelJSONObject.getString("entityName"));

			/** TODO below */
			// TODO : What is orderID here? if this is id of current row in Activities Order
			// table ,
			// Then request cannot provide orderID As request from booking Engine can't have
			// the auto
			// -generated id from db with itself
			// amendEntry.setOrderID(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
			/** TODO up */

			/** BookRefId we are sending from both Amend and cancel */
			String suppBookRefr = amendCancelJSONObject.getString("bookRefId");
			List<ActivitiesOrders> activityOrderList = activitiesDatabaseRepository.findOrderID(suppBookRefr);

			if (null != activityOrderList)
				for (int activitiesCount = 0; activitiesCount < activityOrderList.size(); activitiesCount++) {
					String orderID = activityOrderList.get(activitiesCount).getId();
					amendEntry.setOrderID(orderID);
				}

			/** requestType will be : Amend/Cancel */
			amendEntry.setRequestType(reqJson.getJSONObject(JSON_PROP_REQBODY).getString("requestType"));

			amendEntry.setSupplierCharges("0");
			amendEntry.setDescription(type);
			amendEntry.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			amendEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			amendEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
			amendEntry.setStatus("OnRequest");
			saveAccoAmCl(amendEntry, "");

			switch (type) {
			case JSON_PROP_ACCO_AMENDTYPE_ADDPAX:
				updatePaxDetails(reqJson, amendCancelJSONObject, type);
				// TODO : continue needed here instead of
				break;
			case JSON_PROP_ACCO_AMENDTYPE_CANCELPAX:
				updatePaxDetails(reqJson, amendCancelJSONObject, type);
				break;
			case JSON_PROP_ACCO_AMENDTYPE_UPDATEPAX:
				updatePaxDetails(reqJson, amendCancelJSONObject, type);
				break;

			/*
			 * case JSON_PROP_ACCO_CANNCELTYPE_UPDATESTAYDATES: return
			 * updateStayDates(reqJson);
			 */

			case JSON_PROP_ACCO_CANCELTYPE_FULLCANCEL:
				fullCancel(reqJson);
				break;

			default:
				return "no match for cancel/amend type";
			}

		}
		return "SUCCESS";

	}

	private String fullCancel(JSONObject reqJson) {
		/**
		 * Code to get id based on supplierBokkingRefrence Processed from KafkaResponse
		 * during cancel request
		 */

		JSONObject reqBody = reqJson.getJSONObject(JSON_PROP_REQBODY);
		JSONArray cancelRefrences = reqBody.getJSONArray("activityInfo");
		for (int count = 0; count < cancelRefrences.length(); count++) {
			String supp_Booking_refrence = cancelRefrences.getJSONObject(count).getString("bookRefId");
			List<ActivitiesOrders> activityOrderList = activitiesDatabaseRepository.findOrderID(supp_Booking_refrence);
			if (null != activityOrderList)
				for (int activitiesCount = 0; activitiesCount < activityOrderList.size(); activitiesCount++) {
					String orderID = activityOrderList.get(activitiesCount).getId();
					/** entityId will be orderID */
					ActivitiesOrders order = activitiesDatabaseRepository.findOne(orderID);
					String prevOrder = order.toString();
					order.setStatus("Cancelled");
					order.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));
					order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
					saveActivitiesOrder(order, prevOrder);
				}
		}
		//
		return "SUCCESS";
	}

	private String updatePaxDetails(JSONObject reqJson, JSONObject amendCancelJSONObject, String type)
			throws BookingEngineDBException {
		String prevOrder;

		JSONArray paxDetailsJSONArray = amendCancelJSONObject.getJSONArray("participantInfo");
		for (int i = 0; i < paxDetailsJSONArray.length(); i++) {
			JSONObject currenntPaxDetails = paxDetailsJSONArray.getJSONObject(i);
			PassengerDetails passengerDetails;

			if (type.equals(JSON_PROP_ACCO_AMENDTYPE_ADDPAX)) {
				passengerDetails = new PassengerDetails();
				prevOrder = "";
				passengerDetails.setStatus("Added");
				passengerDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			} else if (type.equals(JSON_PROP_ACCO_AMENDTYPE_UPDATEPAX)) {
				passengerDetails = passengerRepository.findOne(currenntPaxDetails.getString("paxid"));
				prevOrder = passengerDetails.toString();
				passengerDetails.setStatus("Updated");
			} else {
				passengerDetails = passengerRepository.findOne(currenntPaxDetails.getString("paxid"));
				prevOrder = passengerDetails.toString();
				passengerDetails.setStatus("Cancelled");
			}

			passengerDetails.setTitle(currenntPaxDetails.getString("nameTitle"));
			passengerDetails.setFirstName(currenntPaxDetails.getString("givenName"));
			passengerDetails.setMiddleName(currenntPaxDetails.getString(JSON_PROP_MIDDLENAME));
			passengerDetails.setLastName(currenntPaxDetails.getString(JSON_PROP_SURNAME));

			// TODO : Need to check if DOB will come from UI in case of Amend. Right now
			// it is not present.
			// passengerDetails.setBirthDate(currenntPaxDetails.getString("DOB"));

			// TODO : check if it is already added to Amend in swagger and in Amend request.
			// It needs to be present there.
			passengerDetails.setIsLeadPax(currenntPaxDetails.getBoolean("isLeadPax"));

			passengerDetails.setPaxType(currenntPaxDetails.getString("qualifierInfo"));

			// TODO : ContactDetails and AddressDetails needs to come from WEM. Right now it
			// is not coming from WEM.
			// passengerDetails.setContactDetails(currenntPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
			// passengerDetails.setAddressDetails(currenntPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());

			passengerDetails.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString("userID"));

			passengerDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

			savePaxDetails(passengerDetails, prevOrder);

		}
		return "SUCCESS";
	}

	private AmCl saveAccoAmCl(AmCl currentOrder, String prevOrder) {
		AmCl orderObj = null;
		try {
			orderObj = CopyUtils.copy(currentOrder, AmCl.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return amClRepository.saveOrder(orderObj, prevOrder);

	}

	private ActivitiesOrders saveActivitiesOrder(ActivitiesOrders order, String prevOrder) {

		ActivitiesOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, ActivitiesOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return activitiesDatabaseRepository.saveOrder(orderObj, prevOrder);

	}

	private ActivitiesOrders populateActivitiesData(JSONObject reservations, JSONObject bookRequestHeader,
			Booking booking) throws BookingEngineDBException {
		ActivitiesOrders order = new ActivitiesOrders();

		order.setBooking(booking);
		order.setLastUpdatedBy(bookRequestHeader.getJSONObject("clientContext").getString("clientID"));
		order.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		order.setStatus("OnRequest");
		order.setClientIATANumber(bookRequestHeader.getJSONObject("clientContext").getString("clientIATANumber"));
		order.setClientCurrency(bookRequestHeader.getJSONObject("clientContext").getString("clientCurrency"));
		order.setClientID(bookRequestHeader.getJSONObject("clientContext").getString("clientID"));
		order.setClientType(bookRequestHeader.getJSONObject("clientContext").getString("clientType"));
		order.setPOS(bookRequestHeader.getJSONObject("clientContext").optString("pointOfSale"));

		order.setSupplierID(reservations.getString("supplierID"));
		order.setProductSubCategory(Constants.JSON_PROP_ACTIVITIES_SUBCATEGORY);
		readOrderDetails(order, bookRequestHeader, reservations);

		Set<PassengerDetails> setPassengerDetails = readPassengerDetails(reservations, order);

		// Set<PassengerDetails> setPassengerDetails = new
		// HashSet<PassengerDetails>(Arrays.asList(passengerDetails));
		// order.setPassengerDetails(setPassengerDetails);

		JSONArray paxIds = new JSONArray();
		for (PassengerDetails pax : setPassengerDetails) {
			JSONObject paxJson = new JSONObject();
			paxJson.put("paxId", pax.getPassanger_id());
			paxIds.put(paxJson);
		}

		order.setPaxDetails(paxIds.toString());

		return order;
	}

	/**
	 * @param order
	 * @param reservation
	 */
	private void readOrderDetails(ActivitiesOrders order, JSONObject bookRequestHeader, JSONObject reservation) {
		order.setSupplierProductCode(reservation.getJSONObject("basicInfo").getString("supplierProductCode"));
		order.setSupplierBrandCode(reservation.getJSONObject("basicInfo").getString("supplierBrandCode"));
		order.setName(reservation.getJSONObject("basicInfo").getString("name"));
		order.setSupplier_Details(reservation.getJSONObject("basicInfo").getJSONObject("supplier_Details").toString());
		order.setTourLanguage(reservation.getJSONObject("basicInfo").getJSONArray("tourLanguage").toString());
		order.setAnswers(reservation.getJSONObject("basicInfo").getJSONArray("answers").toString());
		order.setShipping_Details(reservation.getJSONObject("basicInfo").getJSONObject("shipping_Details").toString());

		order.setPOS(reservation.getJSONObject("basicInfo").getJSONObject("POS").toString());
		order.setTimeSlotDetails(reservation.getJSONObject("basicInfo").getJSONArray("timeSlotDetails").toString());
		order.setStartDate(readStartDateEndDate(reservation.getJSONObject("schedule").getString("start")));
		order.setEndDate(readStartDateEndDate(reservation.getJSONObject("schedule").getString("end")));
		JSONObject pickupDropoff = readPickupDropoff(reservation);
		order.setPickupDropoff(pickupDropoff.toString());

		order.setCountryCode(reservation.getString("countryCode"));
		order.setCityCode(reservation.getString("cityCode"));

		order.setContactDetail(reservation.getJSONObject("contactDetail").toString());

		order.setPaxInfo(reservation.getJSONArray("paxInfo").toString());

		order.setSupplierTotalPrice(reservation.getJSONObject("suppPriceInfo")
				.getJSONObject("activitySupplierSummaryPrice").getBigDecimal("amount").toString());
		order.setSupplierPriceCurrencyCode(reservation.getJSONObject("suppPriceInfo")
				.getJSONObject("activitySupplierSummaryPrice").getString("currencyCode"));

		order.setTotalPrice(reservation.getJSONObject("activityTotalPricingInfo").getJSONObject("activitySummaryPrice")
				.getBigDecimal("amount").toString());
		order.setTotalPriceCurrencyCode(reservation.getJSONObject("activityTotalPricingInfo")
				.getJSONObject("activitySummaryPrice").getString("currencyCode"));

		order.setSuppPaxTypeFares(reservation.getJSONObject("suppPriceInfo").getJSONArray("paxPriceInfo").toString());
		order.setTotalPaxTypeFares(
				reservation.getJSONObject("activityTotalPricingInfo").getJSONArray("paxPriceInfo").toString());

		order.setTotalPriceBaseFare(reservation.getJSONObject("activityTotalPricingInfo")
				.getJSONObject("activitySummaryPrice").getJSONObject("baseFare").getBigDecimal("amount").toString());

		// order.setTotalPriceBaseFare(reservation.getJSONObject("activityTotalPricingInfo")
		// .getJSONObject("activitySummaryPrice").getJSONObject(JSON_PROP_RECEIVABLES).toString());

		order.setPaxDetails(reservation.getJSONArray("participantInfo").toString());

		Set<SupplierCommercial> suppComms = new HashSet<SupplierCommercial>();
		suppComms = readSuppCommercials(
				reservation.getJSONObject("suppPriceInfo").getJSONArray("supplierCommercialsTotals"), order);

		Set<ClientCommercial> clientComms = new HashSet<ClientCommercial>();
		clientComms = readClientCommercials(
				reservation.getJSONObject("activityTotalPricingInfo").getJSONArray("clientEntityTotalCommercials"),
				order);

		order.setClientCommercial(clientComms);
		order.setSuppcommercial(suppComms);

		order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
		order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));

		// order.setTotalPriceReceivables(totalPriceReceivables); //TODO:

		if (reservation.getJSONObject("activityTotalPricingInfo").getJSONObject("activitySummaryPrice")
				.getJSONObject("companyTaxes") != null) {
			order.setCompanyTaxes(reservation.getJSONObject("activityTotalPricingInfo")
					.getJSONObject("activitySummaryPrice").getJSONObject("companyTaxes").toString());

		}

	}

	private Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, ActivitiesOrders order) {

		Set<ClientCommercial> clientCommercialsSet = new HashSet<ClientCommercial>();
		ClientCommercial clientCommercials;

		for (int i = 0; i < clientCommsJsonArray.length(); i++) {

			JSONObject totalClientComm = clientCommsJsonArray.getJSONObject(i);

			String clientID = totalClientComm.getString(JSON_PROP_CLIENTID);
			String parentClientID = totalClientComm.getString(JSON_PROP_PARENTCLIENTID);
			;
			String commercialEntityType = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYTYPE);
			;
			String commercialEntityID = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYID);
			;

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

				clientCommercials.setProduct("Activities");
				clientCommercials.setOrder(order);
				clientCommercialsSet.add(clientCommercials);
			}
		}
		return clientCommercialsSet;
	}

	private Set<SupplierCommercial> readSuppCommercials(JSONArray suppCommsJsonArray, ActivitiesOrders order) {

		Set<SupplierCommercial> suppCommercialsSet = new HashSet<SupplierCommercial>();
		SupplierCommercial suppCommercials;

		for (int i = 0; i < suppCommsJsonArray.length(); i++) {
			JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);

			suppCommercials = new SupplierCommercial();
			suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
			suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
			suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
			suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));

			suppCommercials.setProduct("Activities");
			suppCommercials.setOrder(order);
			suppCommercialsSet.add(suppCommercials);
		}
		return suppCommercialsSet;
	}

	private JSONObject readCommercialPaxTypeFares(JSONObject reservation) {
		JSONObject commercialPaxTypeFares = new JSONObject();

		commercialPaxTypeFares.put("totalPriceInfo", reservation.getJSONArray("totalPriceInfo"));
		commercialPaxTypeFares.put("suppFares", getSuppFares(reservation));
		commercialPaxTypeFares.put("totalFares", getTotalFares(reservation));

		// TODO Auto-generated method stub
		return commercialPaxTypeFares;
	}

	private JSONArray getTotalPaxTypeFares(JSONObject reservation) {
		JSONArray pricingDetails = reservation.getJSONArray("suppPriceInfo");
		JSONArray totalPaxtypeFares = new JSONArray();
		for (int pricingDetailsCount = 0; pricingDetailsCount < pricingDetails.length(); pricingDetailsCount++) {
			// TODO : get clarification , that in suppPaxTypeFares only Adult and Child
			// details will go
			// or summary details will go too. Based on that a "IF THEN ELSE" will come here
			// with check on
			// participantCategory
			if (pricingDetails.getJSONObject(pricingDetailsCount).has("participantCategory") && !"Summary"
					.equals(pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory"))) {
				JSONObject totalPaxtypeFare = new JSONObject();

				// TODO : Check if Adult is good or the value will be "ADT" for Adult and "CHD"
				// for Child
				totalPaxtypeFare.put("paxType",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory"));

				JSONObject basefares = new JSONObject();
				BigDecimal totalPrice = pricingDetails.getJSONObject(pricingDetailsCount).getBigDecimal("totalPrice");
				BigDecimal markupPrice = new BigDecimal(0);
				JSONArray clientCommercials = pricingDetails.getJSONObject(pricingDetailsCount)
						.optJSONArray("clientCommercials");

				// TODO : check if single entityCommercial will only present in ClientCommercial
				// array
				// TODO : what needs to be done when, more then one entityCommercial array will
				// present here
				JSONArray entityCommercials = null;
				if (clientCommercials != null && clientCommercials.optJSONObject(0) != null)
					entityCommercials = clientCommercials.optJSONObject(0).optJSONArray("entityCommercials");

				if (entityCommercials != null)
					for (int entityCommercialcount = 0; entityCommercialcount < entityCommercials
							.length(); entityCommercialcount++) {
						JSONObject entityCommercial = entityCommercials.getJSONObject(entityCommercialcount);
						if ("MarkUp".equals(entityCommercial.getString("commercialName"))) {
							markupPrice = entityCommercial.getBigDecimal("commercialAmount");
							break;
						}
					}

				if (totalPrice != null)
					totalPrice = totalPrice.add(markupPrice);
				basefares.put("amount", totalPrice);
				basefares.put("currencyCode",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));

				totalPaxtypeFare.put("baseFare", basefares);

				JSONArray clientEntityCommercials = new JSONArray();
				BigDecimal totalFarePrice = new BigDecimal(0);
				// totalFarePrice = totalFarePrice.add(totalPrice);

				if (clientCommercials != null)
					for (int clientCommercialscount = 0; clientCommercialscount < clientCommercials
							.length(); clientCommercialscount++) {
						JSONObject clientEntityCommercial = new JSONObject();

						// TODO : Check is clientID, parentClientID, CoomercialEntityType and
						// commercialEntityID are same for all the elements
						// e.g: Markup, ManagementFees, Discount , IssuanceFees etc. are same. Or they
						// are different. Check calculatePrices method
						// in activitySearchProcessor
						String clientID = clientCommercials.optJSONObject(clientCommercialscount)
								.optJSONArray("entityCommercials").optJSONObject(0).optString("clientID");
						String parentClientID = clientCommercials.optJSONObject(clientCommercialscount)
								.optJSONArray("entityCommercials").optJSONObject(0).optString("parentClientID");
						String commercialEntityType = clientCommercials.optJSONObject(clientCommercialscount)
								.optJSONArray("entityCommercials").optJSONObject(0).optString("commercialEntityType");
						String commercialEntityID = clientCommercials.optJSONObject(clientCommercialscount)
								.optJSONArray("entityCommercials").optJSONObject(0).optString("commercialEntityID");

						clientEntityCommercial.put("clientID", clientID);
						clientEntityCommercial.put("parentClientID", parentClientID);
						clientEntityCommercial.put("commercialEntityID", commercialEntityID);
						clientEntityCommercial.put("commercialEntityType", commercialEntityType);

						JSONArray entityCommercialsForClientCommercials = clientCommercials
								.optJSONObject(clientCommercialscount).optJSONArray("entityCommercials");
						JSONArray clientEntityCommercialsArray = new JSONArray();

						for (int entityCommercialsForClientCommercialsCount = 0; entityCommercialsForClientCommercialsCount < entityCommercialsForClientCommercials
								.length(); entityCommercialsForClientCommercialsCount++) {

							JSONObject entityCommercialsForClientCommercial = new JSONObject();
							entityCommercialsForClientCommercial.put("commercialCurrency",
									entityCommercialsForClientCommercials
											.getJSONObject(entityCommercialsForClientCommercialsCount)
											.optString("commercialCurrency"));
							entityCommercialsForClientCommercial.put("commercialType",
									entityCommercialsForClientCommercials
											.getJSONObject(entityCommercialsForClientCommercialsCount)
											.optString("commercialType"));
							entityCommercialsForClientCommercial.put("commercialAmount",
									entityCommercialsForClientCommercials
											.getJSONObject(entityCommercialsForClientCommercialsCount)
											.optString("commercialAmount"));
							entityCommercialsForClientCommercial.put("commercialName",
									entityCommercialsForClientCommercials
											.getJSONObject(entityCommercialsForClientCommercialsCount)
											.optString("commercialName"));
							clientEntityCommercialsArray.put(entityCommercialsForClientCommercial);

							// if("Receivable".equals(entityCommercialsForClientCommercials.getJSONObject(entityCommercialsForClientCommercialsCount).optString("commercialType")))
							// {
							// totalFarePrice =
							// totalFarePrice.add(entityCommercialsForClientCommercials.getJSONObject(entityCommercialsForClientCommercialsCount).optBigDecimal("commercialAmount",
							// new BigDecimal(0)));
							// }
						}

						clientEntityCommercial.put("clientCommercials", clientEntityCommercialsArray);
						clientEntityCommercials.put(clientEntityCommercial);

					}

				totalPaxtypeFare.put("clientEntityCommercials", clientEntityCommercials);

				JSONArray totalPriceInfo = reservation.getJSONArray("totalPriceInfo");
				for (int totalPriceInfoCount = 0; totalPriceInfoCount < totalPriceInfo
						.length(); totalPriceInfoCount++) {

					// TODO : "ADT" or "Adult" needs to standardize. "Child" or "CHD" needs to
					// standardize
					if (pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory").equals(
							totalPriceInfo.getJSONObject(totalPriceInfoCount).getString("participantCategory"))) {
						totalFarePrice = totalPriceInfo.getJSONObject(totalPriceInfoCount).getBigDecimal("totalPrice");
						break;
					}
				}

				JSONObject totalFares = new JSONObject();
				totalFares.put("amount", totalFarePrice);
				totalFares.put("currencyCode",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));

				totalPaxtypeFare.put("totalFare", totalFares);
				totalPaxtypeFares.put(totalPaxtypeFare);
			}
		}

		JSONArray totalPriceInfoAndCompanyTaxDetails = reservation.getJSONArray("totalPriceInfo");

		for (int count = 0; count < totalPriceInfoAndCompanyTaxDetails.length(); count++) {
			JSONObject totalPriceInfoAndCompanyTaxDetail = totalPriceInfoAndCompanyTaxDetails.getJSONObject(count);
			if (totalPriceInfoAndCompanyTaxDetail.has("companyTaxes")) {
				totalPaxtypeFares.put(totalPriceInfoAndCompanyTaxDetail);
			}
		}

		return totalPaxtypeFares;
	}

	/**
	 * @param reservation
	 * @return
	 */
	private JSONArray getSuppPaxTypeFares(JSONObject reservation) {
		JSONArray pricingDetails = reservation.getJSONArray("suppPriceInfo");
		JSONArray suppPaxtypeFares = new JSONArray();
		for (int pricingDetailsCount = 0; pricingDetailsCount < pricingDetails.length(); pricingDetailsCount++) {
			// TODO : get clarification , that in suppPaxTypeFares only Adult and Child
			// details will go
			// or summary details will go too. Based on that a "IF THEN ELSE" will come here
			// with check on
			// participantCategory
			if (pricingDetails.getJSONObject(pricingDetailsCount).has("participantCategory") && !"Summary"
					.equals(pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory"))) {
				JSONObject suppPaxTypeFare = new JSONObject();

				// TODO : Check if Adult is good or the value will be "ADT" for Adult and "CHD"
				// for Child
				suppPaxTypeFare.put("paxType",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory"));

				JSONObject basefares = new JSONObject();
				basefares.put("amount", pricingDetails.getJSONObject(pricingDetailsCount).getBigDecimal("totalPrice"));
				basefares.put("currencyCode",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));

				suppPaxTypeFare.put("baseFare", basefares);

				// TODO : totalFares in SuppPaxTypeFare is the sum of tax and fees we are
				// receiving. Right now we are not receiving
				// both, Hence it is same as basetypeFares. In case tax and fees starts coming,
				// the code below needs modification.
				JSONObject totalFares = new JSONObject();
				totalFares.put("amount", pricingDetails.getJSONObject(pricingDetailsCount).getBigDecimal("totalPrice"));
				totalFares.put("currencyCode",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));

				suppPaxTypeFare.put("totalFare", totalFares);

				JSONArray supplierCommercials = pricingDetails.getJSONObject(pricingDetailsCount)
						.optJSONArray("supplierCommercials");

				suppPaxTypeFare.put("supplierCommercials", supplierCommercials);
				suppPaxtypeFares.put(suppPaxTypeFare);
			}

		}
		return suppPaxtypeFares;
	}

	/**
	 * @param reservation
	 * @return
	 */
	private JSONObject readPickupDropoff(JSONObject reservation) {

		JSONObject pickupDropoff = new JSONObject();

		String dateInString = reservation.getJSONObject("pickupDropoff").getString("dateTime");

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Instant instant = sdf.parse(dateInString).toInstant();

			// TODO: done
			ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.UTC);
			String stringDate = zonedDateTime.toString();
			pickupDropoff.put("dateTime", zonedDateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		pickupDropoff.put("locationName", reservation.getJSONObject("pickupDropoff").get("locationName"));

		return pickupDropoff;
	}

	private ZonedDateTime readStartDateEndDate(String stringInDate) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Instant instant = sdf.parse(stringInDate).toInstant();

			// TODO: done
			ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.UTC);
			// String stringOutDate = zonedDateTime.toString();
			return zonedDateTime;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	private Set<ClientCommercial> readClientCommercials(JSONArray suppPriceInfo, Set<ClientCommercial> setClientComms,
			ActivitiesOrders order) {
		ClientCommercial clientCommercial = null;
		for (int commercialCount = 0; commercialCount < suppPriceInfo.length(); commercialCount++) {

			// TODO: The supplier and Client Commercials for Adult and Child will not go
			// into
			// Supplier and Client Commercial table. Adult and child will be put into
			// order/passengers table.
			if ("Summary".equals(suppPriceInfo.getJSONObject(commercialCount).getString("participantCategory"))) {
				JSONArray clientCommercials = suppPriceInfo.getJSONObject(commercialCount)
						.optJSONArray("clientCommercials");
				if (clientCommercials != null && clientCommercials.length() > 0) {
					for (int clientCommercialCount = 0; clientCommercialCount < clientCommercials
							.length(); clientCommercialCount++) {
						JSONArray entityCommercials = clientCommercials.getJSONObject(clientCommercialCount)
								.optJSONArray("entityCommercials");

						for (int entityCommercialCount = 0; entityCommercialCount < entityCommercials
								.length(); entityCommercialCount++) {
							clientCommercial = new ClientCommercial();

							JSONObject entityCommercial = entityCommercials.getJSONObject(entityCommercialCount);
							clientCommercial.setClientID(entityCommercial.getString("clientID"));
							clientCommercial
									.setCommercialEntityType(entityCommercial.getString("commercialEntityType"));
							clientCommercial.setParentClientID(entityCommercial.getString("parentClientID"));
							clientCommercial.setCommercialCurrency(entityCommercial.getString("commercialCurrency"));
							clientCommercial.setCommercialType(entityCommercial.getString("commercialType"));
							clientCommercial.setCommercialEntityID(entityCommercial.getString("commercialEntityID"));
							clientCommercial
									.setCommercialAmount(entityCommercial.getBigDecimal("commercialAmount").toString());
							clientCommercial.setCommercialName(entityCommercial.getString("commercialName"));

							clientCommercial.setProduct(Constants.JSON_PROP_ACTIVITIES_CATEGORY);
							clientCommercial.setOrder(order);

							setClientComms.add(clientCommercial);

						}
					}
				}

			}
		}

		return setClientComms;
	}

	private Set<SupplierCommercial> readSuppCommercials(JSONArray suppPriceInfo, Set<SupplierCommercial> setSuppComms,
			ActivitiesOrders order) {
		SupplierCommercial supplierCommercial = null;
		for (int commercialCount = 0; commercialCount < suppPriceInfo.length(); commercialCount++) {
			// TODO: The supplier and Client Commercials for Adult and Child will not go
			// into
			// Supplier and Client Commercial table. Adult and child will be put into
			// order/passengers table.
			if ("Summary".equals(suppPriceInfo.getJSONObject(commercialCount).getString("participantCategory"))) {
				JSONArray supplierCommercials = suppPriceInfo.getJSONObject(commercialCount)
						.optJSONArray("supplierCommercials");
				if (supplierCommercials != null && supplierCommercials.length() > 0) {

					for (int supplierCommercialCount = 0; supplierCommercialCount < supplierCommercials
							.length(); supplierCommercialCount++) {
						JSONObject supplierCommercialsJSON = supplierCommercials.getJSONObject(supplierCommercialCount);
						supplierCommercial = new SupplierCommercial();
						supplierCommercial.setCommercialType(supplierCommercialsJSON.getString("commercialType"));
						supplierCommercial.setCommercialAmount(
								supplierCommercialsJSON.getBigDecimal("commercialAmount").toString());
						supplierCommercial.setCommercialName(supplierCommercialsJSON.getString("commercialName"));
						supplierCommercial
								.setCommercialCurrency(supplierCommercialsJSON.getString("commercialCurrency"));
						supplierCommercial.setProduct(Constants.JSON_PROP_ACTIVITIES_CATEGORY);
						supplierCommercial.setOrder(order);

						// TODO: Commented out by Pritish check and make changes accordingly
						/*
						 * supplierCommercial.setBeforeCommercialAmount(
						 * suppPriceInfo.getJSONObject(commercialCount).getBigDecimal("totalPrice").
						 * toString()); supplierCommercial.setTotalCommercialAmount(supplierCommercials
						 * .getJSONObject(supplierCommercialCount).getBigDecimal("commercialAmount").
						 * toString());
						 */

						setSuppComms.add(supplierCommercial);

					}
				}
			}

		}

		return setSuppComms;
	}

	private Set<PassengerDetails> readPassengerDetails(JSONObject reservations, ActivitiesOrders order)
			throws BookingEngineDBException {
		JSONArray passengetDetailsJsonArray = reservations.getJSONArray("participantInfo");
		Set<PassengerDetails> passengetDetails = new HashSet<PassengerDetails>();
		PassengerDetails passengetDetail;

		for (int i = 0; i < passengetDetailsJsonArray.length(); i++) {
			JSONObject currenntPaxDetails = passengetDetailsJsonArray.getJSONObject(i);
			passengetDetail = new PassengerDetails();

			passengetDetail.setBirthDate(currenntPaxDetails.getString("DOB"));
			passengetDetail.setIsLeadPax(currenntPaxDetails.getBoolean("isLeadPax"));
			passengetDetail.setContactDetails(currenntPaxDetails.getJSONObject("contactDetails").toString());
			passengetDetail.setFirstName(currenntPaxDetails.getJSONObject("personName").getString("givenName"));
			passengetDetail.setMiddleName(currenntPaxDetails.getJSONObject("personName").getString("middleName"));
			// passengetDetail.setTitle(passengetDetailsJsonArray.getJSONObject(i).getJSONObject("personName").getString("namePrefix"));
			passengetDetail.setTitle(currenntPaxDetails.getJSONObject("personName").getString("nameTitle"));

			/**
			 * Need to make it constant throughout Code Such as "ADT" instead of Adult and
			 * "CHD" instead of "Child"
			 */
			passengetDetail.setPaxType(currenntPaxDetails.getString("qualifierInfo"));
			passengetDetail.setLastName(currenntPaxDetails.getJSONObject("personName").getString("surname"));
			// passengetDetail.setActivitiesOrders(order);

			if(currenntPaxDetails.getBoolean("isLeadPax")) {
				JSONObject contactDetail = reservations.getJSONObject("contactDetail");
				passengetDetail.setAddressDetails(contactDetail.getJSONObject("address").toString());
			}
			
			
			passengetDetail.setLastModifiedBy("");
			passengetDetail.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
			passengetDetail.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			savePaxDetails(passengetDetail, "");

			passengetDetails.add(passengetDetail);

		}

		return passengetDetails;
	}

	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevOrder) throws BookingEngineDBException {
		PassengerDetails orderObj = null;
		try {
			orderObj = CopyUtils.copy(pax, PassengerDetails.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			logger.fatal("Error while saving Acco Passenger order object : " + e);
			// myLogger.error("Error while saving order object: " + e);
			throw new BookingEngineDBException("Failed to save order object");
		}
		return passengerRepository.saveOrder(orderObj, prevOrder);

	}

	@Override
	public String processBookResponse(JSONObject bookResponseJson) {
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject("responseBody").getString("bookID"));
		String prevOrder = booking.toString();
		booking.setStatus("confirmed");
		booking.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
		booking.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
		saveBookingOrder(booking, prevOrder);

		List<ActivitiesOrders> orders = activitiesDatabaseRepository.findByBooking(booking);

		for (ActivitiesOrders order : orders) {
			order.setStatus("confirmed");

			// TODO : how to identify which confirmation code is for which order
			order.setSupp_booking_reference(bookResponseJson.getJSONObject("responseBody")
					.getJSONArray("supplierBookReferences").getJSONObject(0).getString("bookRefId"));
			order.setBookingDateTime(new Date().toString());
			activitiesDatabaseRepository.save(order);
		}

		return "SUCCESS";
	}

	private Booking saveBookingOrder(Booking order, String prevOrder) {

		Booking orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, Booking.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return bookingRepository.saveOrder(orderObj, prevOrder);

	}

	@Override
	public String processAmClResponse(JSONObject reqJson) throws BookingEngineDBException {

		JSONObject responseBody = reqJson.getJSONObject(JSON_PROP_RESBODY);
		String requestType = responseBody.getString("requestType");
		if ("amend".equalsIgnoreCase(requestType)) {
			JSONArray activityInfoArray = responseBody.getJSONArray("activityInfo");
			for (int activityInfoCount = 0; activityInfoCount < activityInfoArray.length(); activityInfoCount++) {
				JSONObject tourActivityInfo = responseBody.getJSONArray("activityInfo").getJSONObject(activityInfoCount)
						.getJSONArray("tourActivityInfo").getJSONObject(0);

				String type = tourActivityInfo.getString("type");
				String entityID = tourActivityInfo.getString("entityId");
				String entityname = tourActivityInfo.getString("entityName");

				JSONObject entityIDCompleteObj = new JSONObject();
				JSONArray entityIDJSONArr = new JSONArray();
				JSONObject entityIDJSONobj = new JSONObject();
				if ("pax".equals(entityname)) {
					String paxConcatenatedID = entityID;
					String[] paxID = paxConcatenatedID.split("\\|");
					for (int i = 0; i < paxID.length; i++) {
						JSONObject paxIDJSONObject = new JSONObject();
						paxIDJSONObject.put("entityId", paxID[i]);
						entityIDJSONArr.put(paxIDJSONObject);
					}
				} else {
					entityIDJSONobj.put("entityId", entityID);
					entityIDJSONArr.put(entityIDJSONobj);
				}

				entityIDCompleteObj.put("entityIds", entityIDJSONArr);

				List<AmCl> amendEntries = amClRepository.findforResponseUpdate(
						reqJson.getJSONObject(JSON_PROP_RESBODY).getString("orderID"), entityname,
						entityIDCompleteObj.getJSONArray("entityIds").toString(), type, requestType);

				if (amendEntries.size() == 0) {
					// TODO: handle this before it goes in prod
					System.out.println("no amend entry found. Request might not have been populated");
				}

				else if (amendEntries.size() > 1) {
					// TODO: handle this before it goes in prod
					System.out.println("multiple amend entries found. Dont know which one to update");
				}

				else {
					AmCl amendEntry = amendEntries.get(0);
					String prevOrder = amendEntry.toString();

					// JSONArray amendCancelReqs =
					// reqJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("activityInfo");

					amendEntry.setCompanyCharges(tourActivityInfo.getString("companyCharges"));
					amendEntry.setSupplierCharges(tourActivityInfo.getString("supplierCharges"));
					amendEntry
							.setSupplierChargesCurrencyCode(tourActivityInfo.getString("supplierChargesCurrencyCode"));
					amendEntry.setCompanyChargesCurrencyCode(tourActivityInfo.getString("companyChargesCurrencyCode"));
					amendEntry.setStatus("Confirmed");
					amendEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
					amendEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));

					// TODO: also set the currency codes and breakups before saving
					saveAccoAmCl(amendEntry, prevOrder);
				}
			}

		} else {
			JSONArray supplierCancelReferences = responseBody.getJSONArray("supplierCancelReferences");
			for (int supplierCancelReferencesCount = 0; supplierCancelReferencesCount < supplierCancelReferences
					.length(); supplierCancelReferencesCount++) {
				JSONObject supplierCancelRefrence = supplierCancelReferences
						.getJSONObject(supplierCancelReferencesCount);

				/** Set the below values in supplierCancelRefrences */
				// TODO : Cancel Kafka request needs type, entityId,entityName
				String type = supplierCancelRefrence.getString("type");
				String entityID = supplierCancelRefrence.getString("entityId");
				String entityname = supplierCancelRefrence.getString("entityName");
				/** Set the upper values in supplierCancelRefrences */

				JSONObject entityIDCompleteObj = new JSONObject();
				JSONArray entityIDJSONArr = new JSONArray();
				JSONObject entityIDJSONobj = new JSONObject();
				if ("pax".equals(entityname)) {
					String paxConcatenatedID = entityID;
					String[] paxID = paxConcatenatedID.split("\\|");
					for (int i = 0; i < paxID.length; i++) {
						JSONObject paxIDJSONObject = new JSONObject();
						paxIDJSONObject.put("entityId", paxID[i]);
						entityIDJSONArr.put(paxIDJSONObject);
					}
				} else {
					entityIDJSONobj.put("entityId", entityID);
					entityIDJSONArr.put(entityIDJSONobj);
				}

				entityIDCompleteObj.put("entityIds", entityIDJSONArr);

				List<AmCl> cancelEntries = amClRepository.findforResponseUpdate(
						reqJson.getJSONObject(JSON_PROP_RESBODY).getString("orderID"), entityname,
						entityIDCompleteObj.getJSONArray("entityIds").toString(), type, requestType);
				if (cancelEntries.size() == 0) {
					// TODO: handle this before it goes in prod
					System.out.println("no amend entry found. Request might not have been populated");
				} else if (cancelEntries.size() > 1) {
					// TODO: handle this before it goes in prod
					System.out.println("multiple amend entries found. Dont know which one to update");
				} else {
					AmCl cancelEntry = cancelEntries.get(0);
					String prevOrder = cancelEntry.toString();

					// TODO : Get clarification at this point about from where to receive
					// TODO : Company , Supplier charges and Company/Supplier Currency Codes
					/**
					 * Company charges and Supplier Charges are not present in response during
					 * cancel
					 */
					cancelEntry.setCompanyCharges(supplierCancelRefrence.optString("companyCharges"));
					cancelEntry.setSupplierCharges(supplierCancelRefrence.optString("supplierCharges"));
					cancelEntry.setSupplierChargesCurrencyCode(
							supplierCancelRefrence.optString("supplierChargesCurrencyCode"));
					cancelEntry.setCompanyChargesCurrencyCode(
							supplierCancelRefrence.optString("companyChargesCurrencyCode"));
					cancelEntry.setStatus("Confirmed");
					cancelEntry.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
					cancelEntry.setLastModifiedBy(reqJson.getJSONObject(JSON_PROP_RESHEADER).getString("userID"));

					// TODO: also set the currency codes and breakups before saving
					saveAccoAmCl(cancelEntry, prevOrder);

				}

			}

		}

		return "SUCCESS";

	}

	private JSONArray getTotalFares(JSONObject reservation) {
		JSONArray pricingDetails = reservation.getJSONArray("suppPriceInfo");
		JSONArray totalPaxtypeFares = new JSONArray();
		for (int pricingDetailsCount = 0; pricingDetailsCount < pricingDetails.length(); pricingDetailsCount++) {
			// TODO : get clarification , that in suppPaxTypeFares only Adult and Child
			// details will go
			// or summary details will go too. Based on that a "IF THEN ELSE" will come here
			// with check on
			// participantCategory
			if (pricingDetails.getJSONObject(pricingDetailsCount).has("participantCategory")) {
				JSONObject totalPaxtypeFare = new JSONObject();

				// TODO : Check if Adult is good or the value will be "ADT" for Adult and "CHD"
				// for Child
				totalPaxtypeFare.put("paxType",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory"));

				JSONObject basefares = new JSONObject();
				BigDecimal totalPrice = pricingDetails.getJSONObject(pricingDetailsCount).getBigDecimal("totalPrice");
				BigDecimal markupPrice = new BigDecimal(0);
				JSONArray clientCommercials = pricingDetails.getJSONObject(pricingDetailsCount)
						.optJSONArray("clientCommercials");

				// TODO : check if single entityCommercial will only present in ClientCommercial
				// array
				// TODO : what needs to be done when, more then one entityCommercial array will
				// present here
				JSONArray entityCommercials = null;
				if (clientCommercials != null && clientCommercials.optJSONObject(0) != null)
					entityCommercials = clientCommercials.optJSONObject(0).optJSONArray("entityCommercials");

				if (entityCommercials != null)
					for (int entityCommercialcount = 0; entityCommercialcount < entityCommercials
							.length(); entityCommercialcount++) {
						JSONObject entityCommercial = entityCommercials.getJSONObject(entityCommercialcount);
						if ("MarkUp".equals(entityCommercial.getString("commercialName"))) {
							markupPrice = entityCommercial.getBigDecimal("commercialAmount");
							break;
						}
					}

				if (totalPrice != null)
					totalPrice = totalPrice.add(markupPrice);
				basefares.put("amount", totalPrice);
				basefares.put("currencyCode",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));

				totalPaxtypeFare.put("baseFare", basefares);

				JSONArray clientEntityCommercials = new JSONArray();
				BigDecimal totalFarePrice = new BigDecimal(0);
				// totalFarePrice = totalFarePrice.add(totalPrice);

				if (clientCommercials != null)
					for (int clientCommercialscount = 0; clientCommercialscount < clientCommercials
							.length(); clientCommercialscount++) {
						JSONObject clientEntityCommercial = new JSONObject();

						// TODO : Check is clientID, parentClientID, CoomercialEntityType and
						// commercialEntityID are same for all the elements
						// e.g: Markup, ManagementFees, Discount , IssuanceFees etc. are same. Or they
						// are different. Check calculatePrices method
						// in activitySearchProcessor
						String clientID = clientCommercials.optJSONObject(clientCommercialscount)
								.optJSONArray("entityCommercials").optJSONObject(0).optString("clientID");
						String parentClientID = clientCommercials.optJSONObject(clientCommercialscount)
								.optJSONArray("entityCommercials").optJSONObject(0).optString("parentClientID");
						String commercialEntityType = clientCommercials.optJSONObject(clientCommercialscount)
								.optJSONArray("entityCommercials").optJSONObject(0).optString("commercialEntityType");
						String commercialEntityID = clientCommercials.optJSONObject(clientCommercialscount)
								.optJSONArray("entityCommercials").optJSONObject(0).optString("commercialEntityID");

						clientEntityCommercial.put("clientID", clientID);
						clientEntityCommercial.put("parentClientID", parentClientID);
						clientEntityCommercial.put("commercialEntityID", commercialEntityID);
						clientEntityCommercial.put("commercialEntityType", commercialEntityType);

						JSONArray entityCommercialsForClientCommercials = clientCommercials
								.optJSONObject(clientCommercialscount).optJSONArray("entityCommercials");
						JSONArray clientEntityCommercialsArray = new JSONArray();

						for (int entityCommercialsForClientCommercialsCount = 0; entityCommercialsForClientCommercialsCount < entityCommercialsForClientCommercials
								.length(); entityCommercialsForClientCommercialsCount++) {

							JSONObject entityCommercialsForClientCommercial = new JSONObject();
							entityCommercialsForClientCommercial.put("commercialCurrency",
									entityCommercialsForClientCommercials
											.getJSONObject(entityCommercialsForClientCommercialsCount)
											.optString("commercialCurrency"));
							entityCommercialsForClientCommercial.put("commercialType",
									entityCommercialsForClientCommercials
											.getJSONObject(entityCommercialsForClientCommercialsCount)
											.optString("commercialType"));
							entityCommercialsForClientCommercial.put("commercialAmount",
									entityCommercialsForClientCommercials
											.getJSONObject(entityCommercialsForClientCommercialsCount)
											.optString("commercialAmount"));
							entityCommercialsForClientCommercial.put("commercialName",
									entityCommercialsForClientCommercials
											.getJSONObject(entityCommercialsForClientCommercialsCount)
											.optString("commercialName"));
							clientEntityCommercialsArray.put(entityCommercialsForClientCommercial);

							// if("Receivable".equals(entityCommercialsForClientCommercials.getJSONObject(entityCommercialsForClientCommercialsCount).optString("commercialType")))
							// {
							// totalFarePrice =
							// totalFarePrice.add(entityCommercialsForClientCommercials.getJSONObject(entityCommercialsForClientCommercialsCount).optBigDecimal("commercialAmount",
							// new BigDecimal(0)));
							// }
						}

						clientEntityCommercial.put("clientCommercials", clientEntityCommercialsArray);
						clientEntityCommercials.put(clientEntityCommercial);

					}

				totalPaxtypeFare.put("clientEntityCommercials", clientEntityCommercials);

				JSONArray totalPriceInfo = reservation.getJSONArray("totalPriceInfo");
				for (int totalPriceInfoCount = 0; totalPriceInfoCount < totalPriceInfo
						.length(); totalPriceInfoCount++) {

					// TODO : "ADT" or "Adult" needs to standardize. "Child" or "CHD" needs to
					// standardize
					if (pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory").equals(
							totalPriceInfo.getJSONObject(totalPriceInfoCount).getString("participantCategory"))) {
						totalFarePrice = totalPriceInfo.getJSONObject(totalPriceInfoCount).getBigDecimal("totalPrice");
						break;
					}
				}

				JSONObject totalFares = new JSONObject();
				totalFares.put("amount", totalFarePrice);
				totalFares.put("currencyCode",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));

				totalPaxtypeFare.put("totalFare", totalFares);
				totalPaxtypeFares.put(totalPaxtypeFare);
			}
		}

		JSONArray totalPriceInfoAndCompanyTaxDetails = reservation.getJSONArray("totalPriceInfo");

		for (int count = 0; count < totalPriceInfoAndCompanyTaxDetails.length(); count++) {
			JSONObject totalPriceInfoAndCompanyTaxDetail = totalPriceInfoAndCompanyTaxDetails.getJSONObject(count);
			if (totalPriceInfoAndCompanyTaxDetail.has("companyTaxes")) {
				totalPaxtypeFares.put(totalPriceInfoAndCompanyTaxDetail);
			}
		}

		return totalPaxtypeFares;
	}

	private JSONArray getSuppFares(JSONObject reservation) {
		JSONArray pricingDetails = reservation.getJSONArray("suppPriceInfo");
		JSONArray suppPaxtypeFares = new JSONArray();
		for (int pricingDetailsCount = 0; pricingDetailsCount < pricingDetails.length(); pricingDetailsCount++) {
			// TODO : get clarification , that in suppPaxTypeFares only Adult and Child
			// details will go
			// or summary details will go too. Based on that a "IF THEN ELSE" will come here
			// with check on
			// participantCategory
			if (pricingDetails.getJSONObject(pricingDetailsCount).has("participantCategory")) {
				JSONObject suppPaxTypeFare = new JSONObject();

				// TODO : Check if Adult is good or the value will be "ADT" for Adult and "CHD"
				// for Child
				suppPaxTypeFare.put("paxType",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("participantCategory"));

				JSONObject basefares = new JSONObject();
				basefares.put("amount", pricingDetails.getJSONObject(pricingDetailsCount).getBigDecimal("totalPrice"));
				basefares.put("currencyCode",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));

				suppPaxTypeFare.put("baseFare", basefares);

				// TODO : totalFares in SuppPaxTypeFare is the sum of tax and fees we are
				// receiving. Right now we are not receiving
				// both, Hence it is same as basetypeFares. In case tax and fees starts coming,
				// the code below needs modification.
				JSONObject totalFares = new JSONObject();
				totalFares.put("amount", pricingDetails.getJSONObject(pricingDetailsCount).getBigDecimal("totalPrice"));
				totalFares.put("currencyCode",
						pricingDetails.getJSONObject(pricingDetailsCount).getString("currencyCode"));

				suppPaxTypeFare.put("totalFare", totalFares);

				JSONArray supplierCommercials = pricingDetails.getJSONObject(pricingDetailsCount)
						.optJSONArray("supplierCommercials");

				suppPaxTypeFare.put("supplierCommercials", supplierCommercials);
				suppPaxtypeFares.put(suppPaxTypeFare);
			}

		}
		return suppPaxtypeFares;
	}

}
