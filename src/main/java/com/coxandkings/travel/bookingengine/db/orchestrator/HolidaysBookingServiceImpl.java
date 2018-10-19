package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.HolidaysExtensionDetails;
import com.coxandkings.travel.bookingengine.db.model.HolidaysExtrasDetails;
import com.coxandkings.travel.bookingengine.db.model.HolidaysOrders;
import com.coxandkings.travel.bookingengine.db.model.InsuranceOrders;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.AccoRoomDetails;
import com.coxandkings.travel.bookingengine.db.model.ActivitiesOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.model.TransfersOrders;
import com.coxandkings.travel.bookingengine.db.repository.HolidaysDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;

import org.apache.logging.log4j.Logger;

@Service
@Transactional(readOnly = false)
public class HolidaysBookingServiceImpl implements Constants,ErrorConstants{
	
	@Qualifier("Holidays")
	@Autowired
	private HolidaysDatabaseRepository holidaysRepository;

	@Autowired
	@Qualifier("Passenger")
	private PassengerRepository passengerRepository;

	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	JSONObject response=new JSONObject(); 

	public JSONArray process(Booking booking, String flag) {
		List<HolidaysOrders> holidaysOrder = holidaysRepository.findByBooking(booking);
		JSONArray holidaysOrderJson = getHolidaysOrdersJson(holidaysOrder, flag);
		myLogger.info(String.format("Holidays Bookings retrieved for bookID %s = %s", booking.getBookID(),holidaysOrderJson.toString()));
		return holidaysOrderJson;
	}

	private JSONArray getHolidaysOrdersJson(List<HolidaysOrders> holidaysOrder, String flag) {
		JSONArray holidaysArray = new JSONArray();

		for (HolidaysOrders order : holidaysOrder) {
			JSONObject holidaysJSON = new JSONObject();
			holidaysJSON = getHolidaysOrderJson(order, flag);
			holidaysArray.put(holidaysJSON);

		}
		return holidaysArray;
	}

	private JSONObject getHolidaysOrderJson(HolidaysOrders order, String flag) {

		JSONObject holidaysJson = new JSONObject();
		
		holidaysJson.put(JSON_PROP_LASTUPDATEDBY, order.getLastModifiedBy());
		holidaysJson.put(JSON_PROP_CANCELDATE, "");
		holidaysJson.put(JSON_PROP_AMENDDATE, "");
		holidaysJson.put(JSON_PROP_SUPPLIERID, order.getSupplierID());
		holidaysJson.put(JSON_PROP_ORDERID, order.getId());
		holidaysJson.put(JSON_PROP_SUPPLIERRATETYPE, "");
		holidaysJson.put(JSON_PROP_INVENTORY, "N");
		holidaysJson.put(JSON_PROP_ENABLERSUPPLIERNAME, order.getSupplierID());
		holidaysJson.put(JSON_PROP_SOURCESUPPLIERNAME, order.getSupplierID());
		holidaysJson.put(JSON_PROP_PRODUCTCATEGORY, JSON_PROP_HOLIDAYS);
		holidaysJson.put(JSON_PROP_PRODUCTSUBCATEGORY, order.getProductSubCategory());
		holidaysJson.put(JSON_PROP_CREATEDAT, order.getCreatedAt().toString().substring(0, order.getCreatedAt().toString().indexOf('[')));
		holidaysJson.put(JSON_PROP_CREDENTIALSNAME, "");
		holidaysJson.put(JSON_PROP_CLIENTRECONFIRMATIONSTATUS, order.getClientReconfirmStatus());
		holidaysJson.put(JSON_PROP_SUPPLIERRECONFIRMATIONSTATUS, order.getSuppReconfirmStatus());
		holidaysJson.put(JSON_PROP_PKGS_CLIENTRECONFIRMATIONDATE, order.getClientReconfirmDate());
		holidaysJson.put(JSON_PROP_PKGS_SUPPRECONFIRMATIONDATE, order.getSuppReconfirmDate());
		holidaysJson.put(JSON_PROP_HOLIDAYS_PRODUCTNAME, order.getProductName());
		holidaysJson.put(JSON_PROP_HOLIDAYS_PRODUCTFLAVOURNAME, order.getProductFlavourName());
		holidaysJson.put(JSON_PROP_ROE, order.getRoe());
		holidaysJson.put(JSON_PROP_SUPPTYPE, order.getSupplierType());
		
		holidaysJson.put(JSON_PROP_SUPPLIERBOOKINGREF, order.getSupp_booking_reference());
		holidaysJson.put(JSON_PROP_HOLIDAYS_BRAND, order.getBrand());
		
		JSONObject orderDetails = new JSONObject();
		JSONObject holidayDetails = new JSONObject();
		
		if(flag == "false") {
		orderDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
		orderDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getOrderSuppPriceInfoJson(order));
		}
		orderDetails.put(JSON_PROP_ORDER_CLIENTCOMMS, getClientComms(order));
		orderDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getOrderTotalPriceInfoJson(order));
		//orderDetails.put(JSON_PROP_HOLIDAYS_COMPONENTPRICING, getPricesForEachComponent(order, flag));
		
		holidayDetails.put(JSON_PROP_HOLIDAYS_PACKAGETYPE, order.getPackageType());
		holidayDetails.put(JSON_PROP_HOLIDAYS_COMPANYPACKAGENAME, order.getCompanyPackageName());
		holidayDetails.put(JSON_PROP_AMENDDATE, "");
		holidayDetails.put(JSON_PROP_ROE, order.getRoe());
		holidayDetails.put(JSON_PROP_SUPPLIERRESERVATIONID, order.getSuppierReservationId());
		//holidayDetails.put(JSON_PROP_SUPPLIERREFERENCEID, order.getSupplierReferenceId());
		//holidayDetails.put(JSON_PROP_CLIENTREFERENCEID, order.getClientReferenceId());
		//holidayDetails.put(JSON_PROP_SUPPLIERCANCELLATIONID, order.getSupplierCancellationId());
		holidayDetails.put(JSON_PROP_STATUS, order.getStatus());
		holidayDetails.put(JSON_PROP_HOLIDAYS_TOURTYPE, order.getTourType());
		holidayDetails.put(JSON_PROP_SUPPTYPE, order.getSupplierType());
		holidayDetails.put(JSON_PROP_PKGS_TOURCODE, order.getTourCode());
		holidayDetails.put(JSON_PROP_PKGS_SUBTOURCODE, order.getSubTourCode());
		holidayDetails.put(JSON_PROP_PKGS_BRANDNAME, order.getBrandName());
		holidayDetails.put(JSON_PROP_PKGS_TOURSTART, order.getTourStart());
		holidayDetails.put(JSON_PROP_PKGS_TOUREND, order.getTourEnd());
		holidayDetails.put(JSON_PROP_PKGS_TOURSTARTCITY, order.getTourStartCity());
		holidayDetails.put(JSON_PROP_PKGS_TOURENDCITY, order.getTourEndCity());
		holidayDetails.put(JSON_PROP_PKGS_TOURNAME, order.getTourName());
		holidayDetails.put(JSON_PROP_PKGS_TIMESPANTRAVELSTART, order.getTravelStartDate());
		holidayDetails.put(JSON_PROP_PKGS_TIMESPANTRAVELEND, order.getTravelEndDate());
		holidayDetails.put(JSON_PROP_HOLIDAYS_DESTINATION, order.getDestination());
		holidayDetails.put(JSON_PROP_HOLIDAYS_COUNTRY, order.getCountry());
		holidayDetails.put(JSON_PROP_HOLIDAYS_CITY, order.getCity());
		holidayDetails.put(JSON_PROP_HOLIDAYS_NOOFNIGHTS, order.getNoOfNights());
		holidayDetails.put(JSON_PROP_HOLIDAYS_SUPPREFNO, order.getSupplierReferenceNumber());
		holidayDetails.put(JSON_PROP_HOLIDAYS_SUPPNAME, order.getSupplierName());
		holidayDetails.put("multiCurrencyBooking", order.getMultiCurrencyBooking());

		//Actual Components
		holidayDetails.put(JSON_PROP_PKGS_ACCODETAILS, getAccomodationJson(order, flag));
		holidayDetails.put(JSON_PROP_PKGS_EXTENSIONNIGHTSDETAILS, getExtensionNightsJson(order, flag));
		holidayDetails.put(JSON_PROP_PKGS_INSURANCEDETAILS, getInsuranceJson(order, flag));
		holidayDetails.put(JSON_PROP_PKGS_TRANSFERDETAILS, getTransferJson(order, flag));
		holidayDetails.put(JSON_PROP_PKGS_ACTIVITYDETAILS, getActivityJson(order, flag));
		holidayDetails.put(JSON_PROP_PKGS_EXTRASDETAILS, getExtrasJson(order, flag));
		if(flag == "false") {
		holidayDetails.put(JSON_PROP_ORDER_SUPPCOMMS, getSuppComms(order));
		holidayDetails.put(JSON_PROP_ORDER_SUPPLIERPRICEINFO, getOrderSuppPriceInfoJson(order));
		}
		holidayDetails.put(JSON_PROP_ORDER_CLIENTCOMMS, getClientComms(order));
		holidayDetails.put(JSON_PROP_ORDER_TOTALPRICEINFO, getOrderTotalPriceInfoJson(order));
		holidayDetails.put(JSON_PROP_HOLIDAYS_COMPONENTPRICING, getPricesForEachComponent(order, flag));
		
		orderDetails.put(JSON_PROP_HOLIDAYS_HOLIDAYSDETAILS, holidayDetails);
		holidaysJson.put(JSON_PROP_ORDERDETAILS, orderDetails);

		return holidaysJson;

	}

	//This was done in haste, try to write a single method for all the models
	private JSONObject getPricesForEachComponent(HolidaysOrders order, String flag) {
		JSONObject componentPricing = new JSONObject();
		
		//Acco
		AccoOrders aacoOrder = order.getAccoOrders();
		JSONObject accomodationDetails = new JSONObject();
		JSONObject accoSupplier = new JSONObject();
		JSONObject accoTotal = new JSONObject();
		
		accoSupplier.put("amountAfterTax", Double.parseDouble(aacoOrder.getSupplierPriceAfterTax()));
		accoSupplier.put("taxAmount", aacoOrder.getSupplierTaxAmount());
		accoSupplier.put("taxes", new JSONObject(aacoOrder.getSuppPriceTaxes()));
		accoSupplier.put("currencyCode", aacoOrder.getSupplierPriceCurrencyCode());
		
		accoTotal.put("amountAfterTax", Double.parseDouble(aacoOrder.getTotalPriceAfterTax()));
		accoTotal.put("taxAmount", aacoOrder.getTotalTaxAmount());
		accoTotal.put("taxes", new JSONObject (aacoOrder.getTotalPriceTaxes()));
		accoTotal.put("currencyCode", aacoOrder.getTotalPriceCurrencyCode());

		if(flag == "false") {
		accomodationDetails.put("supplierPriceBreakup",accoSupplier);
		accomodationDetails.put("orderSupplierCommercials",new JSONArray (aacoOrder.getSupplierCommercials()));
		}
		accomodationDetails.put("sellingPriceBreakup",accoTotal);
		accomodationDetails.put("orderClientCommercials", getModifiedClientCommercials(aacoOrder.getClientCommercials()));
		accomodationDetails.put("orderID", aacoOrder.getId());
		
		//Extension Nights
		Set<HolidaysExtensionDetails> extnOrders = order.getHolidaysExtensionDetails();
		for(HolidaysExtensionDetails preOrPost : extnOrders) {
			JSONObject extensionNightsDetails = new JSONObject();
			JSONObject extnSupplier = new JSONObject();
			JSONObject extnTotal = new JSONObject();
			
			extnSupplier.put("amountAfterTax", Double.parseDouble(preOrPost.getSupplierPriceAfterTax()));
			extnSupplier.put("taxAmount", preOrPost.getSupplierTaxAmount());
			extnSupplier.put("taxes", new JSONObject(preOrPost.getSupplierTaxBreakup()));
			extnSupplier.put("currencyCode", preOrPost.getSupplierPriceCurrencyCode());
			
			extnTotal.put("amountAfterTax", Double.parseDouble(preOrPost.getTotalPriceAfterTax()));
			extnTotal.put("taxAmount", preOrPost.getTotalTaxAmount());
			extnTotal.put("taxes", new JSONObject (preOrPost.getTotalTaxBreakup()));
			extnTotal.put("currencyCode", preOrPost.getTotalPriceCurrencyCode());
				
			if(flag == "false") {
				extensionNightsDetails.put("supplierPriceBreakup", extnSupplier);
				extensionNightsDetails.put("orderSupplierCommercials",new JSONArray (preOrPost.getSupplierCommercials()));
			}
			extensionNightsDetails.put("sellingPriceBreakup", extnTotal);
			extensionNightsDetails.put("orderClientCommercials", getModifiedClientCommercials(preOrPost.getClientCommercials()));
			extensionNightsDetails.put("orderID", preOrPost.getId());
				
			if(preOrPost.getExtensionType().equals("preNight")) {
				componentPricing.put("preNightDetails", extensionNightsDetails);
				}
			else {
				componentPricing.put("postNightDetails", extensionNightsDetails);
			}
		}
		
		
		
		//Insurance
		Set<InsuranceOrders> ins = order.getInsuranceOrders();
		JSONObject insuranceDetails = new JSONObject();
		Double insSupplierPrice = 0.0;
		Double insTotalPrice = 0.0;
		for(InsuranceOrders in : ins) {
			String supp = in.getSupplierPriceAfterTax();
			String tot = in.getTotalPriceAfterTax();
			insSupplierPrice = insSupplierPrice + Double.parseDouble(supp);
			insTotalPrice = insTotalPrice + Double.parseDouble(tot);
		}
		if(flag == "false")
		insuranceDetails.put(JSON_PROP_SUPPPRICE, insSupplierPrice);
		insuranceDetails.put(JSON_PROP_HOLIDAYS_SELLINGPRICE, insTotalPrice);
		
		
		//Transfers
		Set<TransfersOrders> trans = order.getTransfersOrders();
		JSONObject transferDetails = new JSONObject();
		Double transSupplierPrice = 0.0;
		Double transTotalPrice = 0.0;
		for(TransfersOrders trn : trans) {
			String supp = trn.getSupplierPriceAfterTax();
			String tot = trn.getTotalPriceAfterTax();
			transSupplierPrice = transSupplierPrice + Double.parseDouble(supp);
			transTotalPrice = transTotalPrice + Double.parseDouble(tot);
		}
		if(flag == "false")
		transferDetails.put(JSON_PROP_SUPPPRICE, transSupplierPrice);
		transferDetails.put(JSON_PROP_HOLIDAYS_SELLINGPRICE, transTotalPrice);
		
		
		//Activities
		Set<ActivitiesOrders> act = order.getActivitiesOrders();
		JSONObject activityDetails = new JSONObject();
		Double actSupplierPrice = 0.0;
		Double actTotalPrice = 0.0;
		for(ActivitiesOrders ac : act) {
			String supp = ac.getSupplierPriceAfterTax();
			String tot = ac.getTotalPriceAfterTax();
			actSupplierPrice = actSupplierPrice + Double.parseDouble(supp);
			actTotalPrice = actTotalPrice + Double.parseDouble(tot);
		}
		if(flag == "false")
		activityDetails.put(JSON_PROP_SUPPPRICE, actSupplierPrice);
		activityDetails.put(JSON_PROP_HOLIDAYS_SELLINGPRICE, actTotalPrice);
		
		
		//Extras
		Set<HolidaysExtrasDetails> ext = order.getHolidaysExtrasDetails();
		JSONObject extrasDetails = new JSONObject();
		Double extrasSupplierPrice = 0.0;
		Double extrasTotalPrice = 0.0;
		for(HolidaysExtrasDetails ex : ext) {
			String supp = ex.getSupplierPriceAfterTax();
			String tot = ex.getTotalPriceAfterTax();
			extrasSupplierPrice = extrasSupplierPrice + Double.parseDouble(supp);
			extrasTotalPrice = extrasTotalPrice + Double.parseDouble(tot);
		}
		if(flag == "false")
		extrasDetails.put(JSON_PROP_SUPPPRICE, extrasSupplierPrice);
		extrasDetails.put(JSON_PROP_HOLIDAYS_SELLINGPRICE, extrasTotalPrice);
		
		
		componentPricing.put(JSON_PROP_PKGS_ACCODETAILS, accomodationDetails);
		componentPricing.put(JSON_PROP_PKGS_INSURANCEDETAILS, insuranceDetails);
		componentPricing.put(JSON_PROP_PKGS_TRANSFERDETAILS, transferDetails);
		componentPricing.put(JSON_PROP_PKGS_ACTIVITYDETAILS, activityDetails);
		componentPricing.put(JSON_PROP_PKGS_EXTRASDETAILS, extrasDetails);
		
		return componentPricing;
	}

	private JSONArray getModifiedClientCommercials(String clientCommercials) {
		JSONArray clientCommercial = new JSONArray(clientCommercials);
		JSONArray modifiedCCArray = new JSONArray();
		for(int i=0; i<clientCommercial.length(); i++) {
			JSONObject cccJson = clientCommercial.getJSONObject(i);
			JSONArray ccTotal = cccJson.getJSONArray("clientCommercials");
			for(int j = 0; j<ccTotal.length(); j++) {
				JSONObject modifiedCCJson = ccTotal.getJSONObject(j);
				modifiedCCJson.put("clientID", cccJson.getString("clientID"));
				modifiedCCJson.put("parentClientID", cccJson.getString("parentClientID"));
				modifiedCCJson.put("commercialEntityType", cccJson.getString("commercialEntityType"));
				modifiedCCJson.put("commercialEntityID", cccJson.getString("commercialEntityID"));
				modifiedCCArray.put(modifiedCCJson);
			}
		}
		
		return modifiedCCArray;
	}

	private JSONObject getOrderTotalPriceInfoJson(HolidaysOrders order) {

		JSONObject totalPriceJson = new JSONObject();

		totalPriceJson.put(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX, order.getTotalPriceBeforeTax());
		totalPriceJson.put(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX, order.getTotalPriceAfterTax());
		totalPriceJson.put(JSON_PROP_CURRENCYCODE, order.getTotalPriceCurrencyCode());
		totalPriceJson.put(JSON_PROP_TAXES, new JSONObject(order.getTotalPriceTaxes()));
		totalPriceJson.put(JSON_PROP_RECEIVABLES, new JSONObject (order.getReceivables()));
		
		// TODO: to confirm if we will get other charges and fees details in Holidays like we get in Air.

		return totalPriceJson;
	}

	private JSONObject getOrderSuppPriceInfoJson(HolidaysOrders order) {

		JSONObject suppPriceJson = new JSONObject();

		suppPriceJson.put(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX, order.getSupplierPriceBeforeTax());
		suppPriceJson.put(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX, order.getSupplierPriceAfterTax());
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, order.getSupplierPriceCurrencyCode());
		suppPriceJson.put(JSON_PROP_TAXES, new JSONObject(order.getSuppPriceTaxes()));

		// TODO: to confirm if we will get other charges and fees details in holidays like we get in Air.

		return suppPriceJson;
	}

	private JSONArray getClientComms(HolidaysOrders order) {


		JSONArray clientCommArray = new JSONArray();

		for (ClientCommercial clientComm : order.getClientCommercial()) {
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

	private JSONArray getSuppComms(HolidaysOrders order) {


		JSONArray suppCommArray = new JSONArray();

		for (SupplierCommercial suppComm : order.getSuppcommercial()) {
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

	private JSONArray getExtrasJson(HolidaysOrders order, String flag) {


		JSONArray extrasArray = new JSONArray();
		
		for (HolidaysExtrasDetails ext : order.getHolidaysExtrasDetails()) {
				JSONObject activityJson = new JSONObject();
				
				activityJson.put(JSON_PROP_PKGS_CONFIGTYPE, ext.getConfigType());
				activityJson.put(JSON_PROP_PKGS_EXTRASTYPE, ext.getExtraType());
				activityJson.put(JSON_PROP_PKGS_EXTRASID, ext.getId());
				if(flag == "false") {
				activityJson.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson((HolidaysComponentsCommonMethods) ext));
				activityJson.put(JSON_PROP_SUPPCOMM, new JSONArray(ext.getSupplierCommercials()));
				}
				activityJson.put("clientEntityCommercials", getModifiedClientCommercials(ext.getClientCommercials()));
				//activityJson.put("clientEntityCommercials", new JSONArray(ext.getClientCommercials()));
				activityJson.put(JSON_PROP_PAXINFO, getPaxInfoJson(ext));
				activityJson.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson((HolidaysComponentsCommonMethods) ext));
				activityJson.put(JSON_PROP_PKGS_EXTRASINFO, getExtrasInfo(ext));
				
			extrasArray.put(activityJson);
		}

		return extrasArray;
	}

	private JSONObject getExtrasInfo(HolidaysExtrasDetails ext) {
		JSONObject extrasInfo = new JSONObject();
		//JSONObject extrasDetails = new JSONObject();
		
		extrasInfo.put(JSON_PROP_PKGS_NAME, ext.getExtraName());
		extrasInfo.put(JSON_PROP_PKGS_CODE, ext.getExtraCode());
		extrasInfo.put(JSON_PROP_PKGS_QUANTITY, ext.getExtraQuantity());
		extrasInfo.put(JSON_PROP_PKGS_DESCRIPTION, ext.getExtraDescription());
		//extrasInfo.put(JSON_PROP_PKGS_EXTRASDETAILS, extrasDetails);
		
		return extrasInfo;
	}

	private JSONArray getActivityJson(HolidaysOrders order, String flag) {


		JSONArray activityArray = new JSONArray();
		
		for (ActivitiesOrders act : order.getActivitiesOrders()) {
				JSONObject activityJson = new JSONObject();
				
				activityJson.put(JSON_PROP_PKGS_ACTIVITYTYPE, act.getActivityType());		
				activityJson.put(JSON_PROP_PKGS_ACTIVITYID, act.getId());
				if(flag == "false") {
				activityJson.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson((HolidaysComponentsCommonMethods) act));
				activityJson.put(JSON_PROP_SUPPCOMM, new JSONArray(act.getSupplierCommercials()));
				}
				activityJson.put("clientEntityCommercials", getModifiedClientCommercials(act.getClientCommercials()));
				//activityJson.put("clientEntityCommercials", new JSONArray(act.getClientCommercials()));
				activityJson.put(JSON_PROP_PAXINFO, getPaxInfoJson(act));
				activityJson.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson((HolidaysComponentsCommonMethods) act));
				activityJson.put(JSON_PROP_PKGS_ACTIVITYINFO, getActivityInfo(act));
				
			activityArray.put(activityJson);
		}

		return activityArray;
	}

	private JSONObject getActivityInfo(ActivitiesOrders act) {
		JSONObject actInfo = new JSONObject();
		//JSONObject activityDetails = new JSONObject();
		
		actInfo.put(JSON_PROP_PKGS_NAME, act.getName());
		actInfo.put(JSON_PROP_PKGS_CODE, act.getActivityCode());
		actInfo.put(JSON_PROP_PKGS_QUANTITY, act.getQuantity());
		actInfo.put(JSON_PROP_PKGS_DESCRIPTION, act.getDescription());
		actInfo.put(JSON_PROP_PKGS_TYPE, act.getActivityType());
		
		actInfo.put(JSON_PROP_PKGS_TIMESPANSTART, act.getStart());
		actInfo.put(JSON_PROP_PKGS_TIMESPANDURATION, act.getDuration());
		actInfo.put(JSON_PROP_PKGS_TIMESPANEND, act.getEnd());
		
		//actInfo.put(JSON_PROP_PKGS_ACTIVITYDETAILS, activityDetails);
		//actInfo.put(JSON_PROP_PKGS_TIMESPAN,getTimeSpan((HolidaysComponentsCommonMethods) act));
		
		return actInfo;
	}

	private JSONArray getTransferJson(HolidaysOrders order, String flag) {

		JSONArray transferArray = new JSONArray();
		
		for (TransfersOrders trans : order.getTransfersOrders()) {
				JSONObject transferJson = new JSONObject();
				
				transferJson.put(JSON_PROP_PKGS_TRANSFERTYPE, trans.getTransferType());		
				transferJson.put(JSON_PROP_PKGS_TRANSFERID, trans.getId());
				if(flag == "false") {
				transferJson.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson((HolidaysComponentsCommonMethods) trans));
				transferJson.put(JSON_PROP_SUPPCOMM, new JSONArray(trans.getSupplierCommercials()));
				}
				transferJson.put("clientEntityCommercials", getModifiedClientCommercials(trans.getClientCommercialss()));
				//transferJson.put("clientEntityCommercials", new JSONArray(trans.getClientCommercialss()));
				transferJson.put(JSON_PROP_PAXINFO, getPaxInfoJson(trans));
				transferJson.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson((HolidaysComponentsCommonMethods) trans));
				transferJson.put(JSON_PROP_PKGS_TRANSFERINFO, getTransferDetails(trans));
				
			transferArray.put(transferJson);
		}

		return transferArray;
		}

	private JSONObject getTransferDetails(TransfersOrders trans) {
		
		JSONObject transfer = new JSONObject();
		
		transfer.put(JSON_PROP_PKGS_DESCRIPTION, trans.getTransferDescription());
		transfer.put(JSON_PROP_PKGS_NAME, trans.getTransferName());
		transfer.put(JSON_PROP_PKGS_TRANSFERDEPARTURECITY, trans.getDepartureCity());
		transfer.put(JSON_PROP_PKGS_TRANSFERARRIVALCITY, trans.getArrivalCity());
		transfer.put(JSON_PROP_PKGS_TRANSFERDEPARTUREDATE, trans.getDepartureDate());
		transfer.put(JSON_PROP_PKGS_TRANSFERARRIVALDATE, trans.getArrivalDate());
		
		transfer.put(JSON_PROP_PKGS_TRANSFERPICKUPLOCATION, trans.getPickUpLocation());
		transfer.put(JSON_PROP_PKGS_TRANSFERAIRPORTNAME, trans.getAirportName());
		
		transfer.put(JSON_PROP_PKGS_TIMESPANSTART, trans.getStart());
		transfer.put(JSON_PROP_PKGS_TIMESPANDURATION, trans.getDuration());
		transfer.put(JSON_PROP_PKGS_TIMESPANEND, trans.getEnd());
		
		//transfer.put(JSON_PROP_PKGS_TRANSFERLOCATION, getTransferLocationJson(trans));
		//transfer.put(JSON_PROP_PKGS_TRANSFERDETAILS, getTransferDetailsJson(trans));
		//transfer.put(JSON_PROP_PKGS_TIMESPAN, getTimeSpan((HolidaysComponentsCommonMethods) trans));
		return transfer;
	}

/*	private JSONObject getTransferDetailsJson(TransfersOrders trans) {
		JSONObject transfer = new JSONObject();
		
		transfer.put(JSON_PROP_PKGS_DESCRIPTION, trans.getTransferDescription());
		transfer.put(JSON_PROP_PKGS_NAME, trans.getTransferName());
		transfer.put(JSON_PROP_PKGS_TRANSFERDEPARTURECITY, trans.getDepartureCity());
		transfer.put(JSON_PROP_PKGS_TRANSFERARRIVALCITY, trans.getArrivalCity());
		transfer.put(JSON_PROP_PKGS_TRANSFERDEPARTUREDATE, trans.getDepartureDate());
		transfer.put(JSON_PROP_PKGS_TRANSFERARRIVALDATE, trans.getArrivalDate());
		
		return transfer;
	}*/

/*	private JSONObject getTransferLocationJson(TransfersOrders trans) {
		JSONObject location = new JSONObject();
		
		location.put(JSON_PROP_PKGS_TRANSFERPICKUPLOCATION, trans.getPickUpLocation());
		location.put(JSON_PROP_PKGS_TRANSFERAIRPORTNAME, trans.getAirportName());
		//location.put("codeContext", trans.getCodeContext());
		return location;
	}*/

	private JSONArray getInsuranceJson(HolidaysOrders order, String flag) {

		JSONArray insuranceArray = new JSONArray();
		
		for (InsuranceOrders ins : order.getInsuranceOrders()) {
				JSONObject insuranceJson = new JSONObject();
				
				insuranceJson.put(JSON_PROP_PKGS_INSURANCETYPE, ins.getInsuranceType());		
				insuranceJson.put(JSON_PROP_PKGS_INSURANCEID, ins.getId());
				if(flag == "false") {
				insuranceJson.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson((HolidaysComponentsCommonMethods) ins));
				insuranceJson.put(JSON_PROP_SUPPCOMM, new JSONArray(ins.getSupplierCommercials()));
				}
				insuranceJson.put("clientEntityCommercials", getModifiedClientCommercials(ins.getClientCommercials()));
				//insuranceJson.put("clientEntityCommercials", new JSONArray(ins.getClientCommercials()));
				insuranceJson.put(JSON_PROP_PAXINFO, getPaxInfoJson(ins));
				insuranceJson.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson((HolidaysComponentsCommonMethods) ins));
				insuranceJson.put(JSON_PROP_PKGS_INSURANCEINFO, getCoverageDetails(ins));
				
			insuranceArray.put(insuranceJson);
		}

		return insuranceArray;
	}

	private JSONObject getCoverageDetails(InsuranceOrders ins) {
		JSONObject coverage = new JSONObject();
		
		coverage.put(JSON_PROP_PKGS_NAME,ins.getInsName());
		coverage.put(JSON_PROP_PKGS_DESCRIPTION,ins.getInsDescription());
		coverage.put(JSON_PROP_PKGS_INSURANCEINSID,ins.getInsId());
		
		return coverage;
	}

	private JSONArray getExtensionNightsJson(HolidaysOrders order, String flag) {
		//Set<HolidaysExtensionDetails> extnOrder = order.getHolidaysExtensionDetails();
		JSONArray extensionArray = new JSONArray();
		
		for(HolidaysExtensionDetails extnsionOrder : order.getHolidaysExtensionDetails()) {
		//AccoOrders aacoOrder = order.getAccoOrders();
		
		for (AccoRoomDetails room : extnsionOrder.getRoomDetails()) {
				JSONObject extensionJson = new JSONObject();
				
				extensionJson.put(JSON_PROP_PKGS_EXTENSIONTYPE, extnsionOrder.getExtensionType());		
				extensionJson.put(JSON_PROP_PKGS_EXTENSIONID, room.getId());
				if(flag == "false") {
				extensionJson.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson(room));
				extensionJson.put(JSON_PROP_SUPPCOMM, new JSONArray(room.getSuppCommercials()));
				}
				extensionJson.put("clientEntityCommercials", new JSONArray(room.getClientCommercials()));
				extensionJson.put(JSON_PROP_PAXINFO, getPaxInfoJson(room));
				extensionJson.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson(room));
				extensionJson.put(JSON_PROP_PKGS_EXTENSIONROOMINFO, getRoomInfoJson(room));
				
			extensionArray.put(extensionJson);
		}}

		return extensionArray;
	}

	private JSONArray getAccomodationJson(HolidaysOrders order, String flag) {
		//JSONObject accommodationConfig = new JSONObject();
		//TODO: 	
		JSONArray configArray = new JSONArray();
		AccoOrders aacoOrder = order.getAccoOrders();
		
		
		for (AccoRoomDetails accoDetail : aacoOrder.getRoomDetails()) {
				JSONObject roomDetail = new JSONObject();
				
				roomDetail.put(JSON_PROP_PKGS_ACCOTYPE, accoDetail.getAccomodationType());		
				roomDetail.put("roomID", accoDetail.getId());
				if(flag == "false") {
				roomDetail.put(JSON_PROP_SUPPLIERPRICEINFO, getSuppPriceInfoJson(accoDetail));
				roomDetail.put(JSON_PROP_SUPPCOMM, new JSONArray(accoDetail.getSuppCommercials()));
				}
				roomDetail.put("clientEntityCommercials", new JSONArray(accoDetail.getClientCommercials()));
				roomDetail.put(JSON_PROP_PAXINFO, getPaxInfoJson(accoDetail));
				roomDetail.put(JSON_PROP_TOTALPRICEINFO, getTotalPriceInfoJson(accoDetail));
				roomDetail.put("roomInfo", getRoomInfoJson(accoDetail));
				
			configArray.put(roomDetail);
		}

		return configArray;
	}

	private JSONObject getRoomInfoJson(HolidaysAccomodationComponentMethods room) {
		JSONObject roomInfoJson = new JSONObject();
			
			if(room.getHotelInfo() != null) {
			JSONArray hotelInfo = new JSONArray(room.getHotelInfo());
			if(hotelInfo.length() != 0)
			roomInfoJson.put(JSON_PROP_PKGS_ACCOHOTELINFO,new JSONArray(room.getHotelInfo()));}
			if(room.getOccupancyInfo() != null)
			roomInfoJson.put("occupancyInfo",new JSONArray(room.getOccupancyInfo()));
			roomInfoJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(((room.getAddress() != null) ? (room.getAddress()) : "{}")));
			roomInfoJson.put(JSON_PROP_ACCO_ROOMTYPEINFO, getRoomTypeInfoJson(room));
			roomInfoJson.put(JSON_PROP_ACCO_RATEPLANINFO, getRatePlanInfoJson(room));
			roomInfoJson.put(JSON_PROP_PKGS_TIMESPAN, getTimeSpan((HolidaysComponentsCommonMethods) room) );
		return roomInfoJson;
	}

	private JSONObject getTimeSpan(HolidaysComponentsCommonMethods component) {
		JSONObject timeSpan= new JSONObject();
		
			timeSpan.put(JSON_PROP_PKGS_TIMESPANSTART, component.getStart());
			timeSpan.put(JSON_PROP_PKGS_TIMESPANDURATION, component.getDuration());
			timeSpan.put(JSON_PROP_PKGS_TIMESPANEND, component.getEnd());
		
		return timeSpan;
	}

	//Not to be used if HotelInfo is fetched as JSONString
/* private JSONObject getHotelInfo(HolidaysAccomodationComponentMethods room) {
		JSONObject hotelInfoJson = new JSONObject();

			hotelInfoJson.put("hotelCode", new JSONArray(room.getHotelCode()));
			hotelInfoJson.put("hotelName", room.getHotelName());
			hotelInfoJson.put("hotelRef", room.getHotelRef());
			hotelInfoJson.put("hotelSegmentCategoryCode", room.getHotelSegmentCategoryCode());
		
		return hotelInfoJson;
	}*/

	private JSONObject getTotalPriceInfoJson(HolidaysComponentsCommonMethods component) {
		JSONObject totalPriceJson = new JSONObject();
		
		totalPriceJson.put(JSON_PROP_HOLIDAYS_TOTALPBT, component.getTotalPriceBeforeTax());
		totalPriceJson.put(JSON_PROP_HOLIDAYS_TOTALPAT, component.getTotalPriceAfterTax());
		totalPriceJson.put(JSON_PROP_TAXAMOUNT, component.getTotalTaxAmount());
		totalPriceJson.put(JSON_PROP_HOLIDAYS_PAXTYPEFARES, new JSONArray (component.getTotalPaxTypeFares()));
		totalPriceJson.put(JSON_PROP_CURRENCYCODE, component.getTotalPriceCurrencyCode());
		totalPriceJson.put(JSON_PROP_TAXES, new JSONObject(component.getTotalTaxBreakup()));
		if(component.getReceivables() != null)
		totalPriceJson.put(JSON_PROP_RECEIVABLES, new JSONObject(component.getReceivables()));
		
		return totalPriceJson;
	}

	public JSONObject getSuppPriceInfoJson(HolidaysComponentsCommonMethods component) {
		JSONObject suppPriceJson = new JSONObject();
		
		suppPriceJson.put(JSON_PROP_HOLIDAYS_SUPPLIERPBT, component.getSupplierPriceBeforeTax());
		suppPriceJson.put(JSON_PROP_HOLIDAYS_SUPPLIERPAT, component.getSupplierPriceAfterTax());
		suppPriceJson.put(JSON_PROP_TAXAMOUNT, component.getSupplierTaxAmount());
		suppPriceJson.put(JSON_PROP_HOLIDAYS_PAXTYPEFARES, new JSONArray (component.getSupplierPaxTypeFares()));
		suppPriceJson.put(JSON_PROP_CURRENCYCODE, component.getSupplierPriceCurrencyCode());
		suppPriceJson.put(JSON_PROP_TAXES, new JSONObject(component.getSupplierTaxBreakup()));
				
		return suppPriceJson;
	}

	private JSONObject getRatePlanInfoJson(HolidaysAccomodationComponentMethods room) {
		JSONObject ratePlanJson = new JSONObject();
		
		ratePlanJson.put(JSON_PROP_ACCO_RATEPLANCODE, room.getRatePlanCode());
		ratePlanJson.put(JSON_PROP_ACCO_RATEPLANNAME, room.getRatePlanName());
		ratePlanJson.put(JSON_PROP_PKGS_ACCOBOOKINGREF, room.getBookingRef());
		
		return ratePlanJson;
	}

	private JSONObject getRoomTypeInfoJson(HolidaysAccomodationComponentMethods room) {
		JSONObject roomTypeJson = new JSONObject();
		
		roomTypeJson.put(JSON_PROP_PKGS_ACCOROOMNAME, room.getRoomName());
		roomTypeJson.put(JSON_PROP_PKGS_ACCOROOMCATEGORYID, room.getRoomCategory());
		roomTypeJson.put(JSON_PROP_PKGS_ACCOROOMTYPECODE, room.getRoomType());
		roomTypeJson.put(JSON_PROP_PKGS_ACCOINVBLOCKCODE, room.getInvBlockCode());
		roomTypeJson.putOpt(JSON_PROP_PKGS_ACCOCABINNUMBER, room.getCabinNumber());

		return roomTypeJson;
	}
	
	private JSONArray getPaxInfoJson(HolidaysComponentsCommonMethods component) {

		JSONArray paxJsonArray = new JSONArray();

		for (Object paxId : new JSONArray(component.getPaxDetails())) {
			JSONObject paxIdJson = (JSONObject)paxId;
			
			PassengerDetails guest  = passengerRepository.findOne(paxIdJson.getString("paxId"));
			JSONObject paxJson = new JSONObject();

			paxJson.put(JSON_PROP_PAXID, guest.getPassanger_id());
			paxJson.put(JSON_PROP_PAX_TYPE, guest.getPaxType());
			paxJson.put(JSON_PROP_ISLEADPAX, guest.getIsLeadPax());
			paxJson.put(JSON_PROP_PKGS_RESGUESTRPH, guest.getRph());
			paxJson.put(JSON_PROP_TITLE, guest.getTitle());
			paxJson.put(JSON_PROP_FIRSTNAME, guest.getFirstName());
			paxJson.put(JSON_PROP_MIDDLENAME, guest.getMiddleName());
			paxJson.put(JSON_PROP_LASTNAME, guest.getLastName());
			paxJson.put(JSON_PROP_BIRTHDATE, guest.getBirthDate());
			
			paxJson.put(JSON_PROP_CONTACTDETAILS, new JSONArray(guest.getContactDetails()));
			paxJson.put(JSON_PROP_ADDRESSDETAILS, new JSONObject(guest.getAddressDetails()));
			paxJson.put(JSON_PROP_DOCUMENTDETAILS, new JSONArray(guest.getDocumentDetails()));
			JSONObject ancillaryServices = new JSONObject();
			ancillaryServices.put(JSON_PROP_HOLIDAYS_ANCILLARYINFO,new JSONArray (guest.getSpecialRequests()));
			paxJson.put(JSON_PROP_ANCILLARYSERVICES, ancillaryServices);
			
			paxJsonArray.put(paxJson);
		}

		return paxJsonArray;
	}

	public String updateOrder(JSONObject reqJson, String updateType) throws BookingEngineDBException {

		switch (updateType) {
		/*case JSON_PROP_PAXDETAILS:
			return updatePaxDetails(reqJson);
		case JSON_PROP_PRICES:
			return updatePriceDetails(reqJson);
		case JSON_PROP_ACCO_STAYDATES:
			return updateStayDates(reqJson);
		case JSON_PROP_TICKETINGPCC:
			return updateTicketingPCC(reqJson);
		case JSON_PROP_ISSHARABLE:
			return updateIsSharable(reqJson);
		case JSON_PROP_ROOM_DOCUMENTS:
			return updateRoomDocument(reqJson);*/
		case "ROE" :
			return updateSellingPricesByRoe(reqJson);
		case JSON_PROP_STATUS:
			return updateStatus(reqJson);
		case JSON_PROP_CLIENTRECONFIRMSTATUS:
			return updateClientReconfirmStatus(reqJson);
		case JSON_PROP_SUPPRECONFIRMSTATUS:
			return updateSuppReconfirmStatus(reqJson);
		case JSON_PROP_ORDERATTRIBUTE:
			return updateOrderAttribute(reqJson);
		case JSON_PROP_SUPPRECONFIRMDATE:
			return updateSuppReconfirmDate(reqJson);
		case JSON_PROP_CLIENTRECONFIRMDATE:
			return updateClientReconfirmDate(reqJson);
		case JSON_PROP_VOUCHERS:
			return updateVouchers(reqJson);

		default:
			response.put("ErrorCode", "BE_ERR_HOLIDAYS_000");
			response.put("ErrorMsg", BE_ERR_000);
			myLogger.info(String.format("Update type %s for req %s not found", updateType, reqJson.toString()));
			return (response.toString());
		}
	}

	private String updateOrderAttribute(JSONObject reqJson) {
		HolidaysOrders order = holidaysRepository.findOne(reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_ACCO_003");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"Holidays updateOrderAttribute failed to update since Order details  not found for  orderid  %s ",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setBookingAttribute(
					reqJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("bookingAttribute").toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            HolidaysOrders updatedclientReconfirmDetails = saveHolidaysOrder(order, prevOrder);
			myLogger.info(String.format("Holidays orderAttribute updated Successfully for  orderId  %s = %s",
					reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_ORDERID),
					updatedclientReconfirmDetails.toString()));
			return "Holidays Booking Attribute Updated Successfully";
		}
	}

	private String updateTimeLimitDate(JSONObject reqJson) throws BookingEngineDBException {

        String orderID = reqJson.getString(JSON_PROP_ORDERID);
        HolidaysOrders order = holidaysRepository.findOne(orderID);
        if (order == null) {
            response.put("ErrorCode", "BE_ERR_004");
            response.put("ErrorMsg", BE_ERR_004);
            myLogger.warn(String.format("Holidays time limit expiry date failed to update since Order details  not found for  orderid  %s ", orderID));
            return (response.toString());
        } else {
            String prevOrder = order.toString();
            order.setTimeLimitExpiryDate(reqJson.getString(JSON_PROP_EXPIRYTIMELIMIT));
            order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            HolidaysOrders updatedclientReconfirmDetails = saveHolidaysOrder(order, prevOrder);
            myLogger.info(String.format("Holidays time limit expiry date updated Successfully for  orderId  %s = %s", orderID, updatedclientReconfirmDetails.toString()));
            return "Holidays time limit expiry date updated Successfully";
        }
    
	}
	
	private String updateVouchers(JSONObject reqJson) throws BookingEngineDBException {

		HolidaysOrders order = holidaysRepository.findOne(reqJson.getString(JSON_PROP_ORDERID));
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"updateVouchers failed to update since Holidays Order details  not found for  orderid  %s ",
					reqJson.getString(JSON_PROP_ORDERID)));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setVouchers(
					reqJson.getJSONArray(JSON_PROP_VOUCHERIDS).toString());
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            HolidaysOrders updatedVoucherDetails = saveHolidaysOrder(order, prevOrder);
			myLogger.info(String.format("Holidays vouchers updated Successfully for  orderId  %s = %s",
					reqJson.getString(JSON_PROP_ORDERID),
					updatedVoucherDetails.toString()));
			
			return "Holidays vouchers Updated Successfully";
		}
	
	}

	
	
	private String updateClientReconfirmDate(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		HolidaysOrders order = holidaysRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmdate failed to update since Order details  not found for Holidays orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmDate(reqJson.getString(JSON_PROP_CLIENTRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));

			HolidaysOrders updatedclientReconfirmDetails = saveHolidaysOrder(order, prevOrder);
			myLogger.info(String.format("Holidays client reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Holidays order client reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmDate(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		HolidaysOrders order = holidaysRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmDate failed to update since Order details  not found for Holidays orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmDate(reqJson.getString(JSON_PROP_SUPPRECONFIRMDATE));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			HolidaysOrders updatedSuppReconfirmDateDetails = saveHolidaysOrder(order, prevOrder);
			myLogger.info(String.format("Holidays supplier reconfirmation date updated Successfully for  orderId  %s = %s",
					orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Holidays order supplier reconfirmation date updated Successfully";
		}
	}

	private String updateSuppReconfirmStatus(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		HolidaysOrders order = holidaysRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"SuppReconfirmStatus failed to update since Order details  not found for Holidays  orderid  %s ", orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setSuppReconfirmStatus(reqJson.getString("suppReconfirmStatus"));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			HolidaysOrders updatedSuppReconfirmDateDetails = saveHolidaysOrder(order, prevOrder);
			myLogger.info(
					String.format("Holidays supplier reconfirmation status updated Successfully for  orderId  %s = %s",
							orderID, updatedSuppReconfirmDateDetails.toString()));
			return "Holidays order supplier reconfirmation status updated Successfully";
		}
	}

	private String updateClientReconfirmStatus(JSONObject reqJson) {
		String orderID = reqJson.getString(JSON_PROP_ORDERID);
		HolidaysOrders order = holidaysRepository.findOne(orderID);
		if (order == null) {
			response.put("ErrorCode", "BE_ERR_004");
			response.put("ErrorMsg", BE_ERR_004);
			myLogger.warn(String.format(
					"ClientReconfirmStatus failed to update since Order details  not found for Holidays orderid  %s ",
					orderID));
			return (response.toString());
		} else {
			String prevOrder = order.toString();
			order.setClientReconfirmStatus(reqJson.getString(JSON_PROP_CLIENTRECONFIRMSTATUS));
			order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
			order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
			HolidaysOrders updatedclientReconfirmDetails = saveHolidaysOrder(order, prevOrder);
			myLogger.info(String.format("Holidays client reconfirmation status updated Successfully for  orderId  %s = %s",
					orderID, updatedclientReconfirmDetails.toString()));
			return "Holidays order client reconfirmation status updated Successfully";
		}
	}
	
	private String updateStatus(JSONObject reqJson) {
	     String orderID = reqJson.getString(JSON_PROP_ORDERID);
	        HolidaysOrders order = holidaysRepository.findOne(orderID);
	        if (order == null) {
	            response.put("ErrorCode", "BE_ERR_004");
	            response.put("ErrorMsg", BE_ERR_004);
	            myLogger.warn(String
	                    .format("Status  failed to update since Holidays order details   not found for  orderid  %s ", orderID));
	            return (response.toString());
	        }
	        String prevOrder = order.toString();
	        order.setStatus(reqJson.getString(JSON_PROP_STATUS));
	        order.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
	        order.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
	        HolidaysOrders updatedStatusObj = saveHolidaysOrder(order, prevOrder);
	        myLogger.info(String.format("Status updated Successfully for Holidays orderID %s = %s", orderID,
	                updatedStatusObj.toString()));
	        return "Holidays Order status updated Successfully";
	}

	private String updateSellingPricesByRoe(JSONObject reqJson) {		
	
	String orderID = reqJson.getString("orderID");
	HolidaysOrders order = holidaysRepository.findOne(orderID);
	if (order == null) {
		response.put("ErrorCode", "BE_ERR_HOLIDAYS_002");
		response.put("ErrorMsg", BE_ERR_HOLIDAYS_002);
		myLogger.warn(String.format("ROE failed to update since Holiday Order not found for orderID  %s", orderID));
		return (response.toString());
	}
	else {
		BigDecimal roe = new BigDecimal(reqJson.getString("roe"));
		AccoOrders aacoOrder = order.getAccoOrders();
		
		for(HolidaysExtensionDetails extnsionOrder : order.getHolidaysExtensionDetails()) {
			
		} 
		
		return orderID;
	}
	
	}

	public String getBysuppID(String suppID) {
		// TODO To be catered as per OPS team's requirement
		return null;
	}

	
	private HolidaysOrders saveHolidaysOrder(HolidaysOrders order, String prevOrder) {
		HolidaysOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, HolidaysOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return holidaysRepository.saveOrder(orderObj, prevOrder);
		
	}
	
}
