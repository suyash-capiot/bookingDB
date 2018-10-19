package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.enums.BookingStatus;
import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.ProductOrder;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("BookingService")
@Transactional(readOnly = false)
public class BookingDataBaseServiceImpl implements BookingDatabaseService,Constants {
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	


	
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	public Booking processBookRequest(JSONObject bookRequestJson, boolean isHolidayBooking) throws BookingEngineDBException {
		try {
		Booking order =new Booking();
		order.setBookID(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		order.setStatus(BookingStatus.RQ.getBookingStatus());
		
		//Setting fields needed by ops and finance
		order.setCompanyId(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).optString(JSON_PROP_COMPANYID));
		order.setGroupCompanyID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).optString(JSON_PROP_GROUP_COMPANYID));
		order.setGroupOfComapniesId(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).optString(JSON_PROP_GROUP_COMPANIESID));
		order.setBU(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).optString(JSON_PROP_BU));
		order.setSBU(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).optString(JSON_PROP_SBU));
		
		order.setLastModifiedBy(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		
		order.setClientCurrency(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		order.setClientID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		order.setPos(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_POINTOFSALE));
		order.setClientType(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));
		order.setSessionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_SESSIONID));
		order.setTransactionID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_TRANSACTID));
		order.setHolidayBooking(isHolidayBooking);
		order.setUserID(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
		order.setClientLanguage(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTLANGUAGE));
		order.setClientMarket(bookRequestJson.getJSONObject(JSON_PROP_REQHEADER).getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTMARKET));
		
		JSONObject payment = (JSONObject)(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAYMENTINFO).get(0));
		order.setProductsCount(payment.optString("noOfProducts"));
		
		order.setPaymentInfo(readPaymentInfo(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray(JSON_PROP_PAYMENTINFO),order));
		
		
		
		return order;
	}
		catch(Exception e)
		{
			myLogger.fatal("Failed to populate Booking Data "+ e);
			throw new BookingEngineDBException("Failed to populate Booking Data");
		}
	}
	
	//TODO: currently we are using optString once finalized we will change it to getString
	private Set<PaymentInfo> readPaymentInfo(JSONArray PaymentInfo, Booking booking) {

		Set<PaymentInfo> paymentInfoSet = new HashSet<PaymentInfo>();

		for (int i = 0; i < PaymentInfo.length(); i++) {
			
			PaymentInfo paymentInfo = new PaymentInfo();
			JSONObject currentPaymentInfo = PaymentInfo.getJSONObject(i);
			
			paymentInfo.setPaymentMethod(currentPaymentInfo.optString(JSON_PROP_PAYMENTMETHOD));
			paymentInfo.setAmountPaid(currentPaymentInfo.optString("amountPaid"));
			paymentInfo.setPaymentType(currentPaymentInfo.optString(JSON_PROP_PAYMENTTYPE));
			paymentInfo.setAmountCurrency(currentPaymentInfo.optString(JSON_PROP_AMOUNTCURRENCY));
			paymentInfo.setPaymentStatus(currentPaymentInfo.optString("paymentStatus"));
			paymentInfo.setTotalAmount(currentPaymentInfo.optString("totalAmount"));
			paymentInfo.setTransactionDate(currentPaymentInfo.optString("transactionDate"));
			paymentInfo.setTransactionRefNumber(currentPaymentInfo.optString("transactionRefNumber"));
			

			paymentInfo.setLastModifiedBy(booking.getUserID());
			paymentInfo.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			paymentInfo.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			
			
			JSONObject paymentAttrs = new JSONObject();
			paymentAttrs.put("merchantId", currentPaymentInfo.optString("merchantId"));
			paymentAttrs.put("bankName", currentPaymentInfo.optString("bankName"));
			paymentAttrs.put("toBankName", currentPaymentInfo.optString("toBankName"));
			paymentAttrs.put("cardType", currentPaymentInfo.optString("cardType"));
			paymentAttrs.put("nameOnCard", currentPaymentInfo.optString("nameOnCard"));
			paymentAttrs.put("Ifsc/SwiftCode", currentPaymentInfo.optString("Ifsc/SwiftCode"));
			paymentAttrs.put("IntermedaryBankIfsc/SwiftCode", currentPaymentInfo.optString("IntermedaryBankIfsc/SwiftCode"));
			paymentAttrs.put("IvrDetails", currentPaymentInfo.optString("IvrDetails"));
			paymentAttrs.put("contactPerson", currentPaymentInfo.optString("contactPerson"));
			paymentAttrs.put("address", currentPaymentInfo.optString("address"));
			paymentAttrs.put("mobileNumber", currentPaymentInfo.optString("mobileNumber"));
			paymentAttrs.put("cheque/DDNumber", currentPaymentInfo.optString("cheque/DDNumber"));
	      
			paymentInfo.setPaymentAttributes(paymentAttrs.toString());
			paymentInfo.setBooking(booking);
			paymentInfoSet.add(paymentInfo);

		}
		return paymentInfoSet;
	}


	@Override
	public String processBookResponse(JSONObject bookResponseJson) throws BookingEngineDBException {
		
		Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		String prevBooking = booking.toString();
		
		Set<ProductOrder> productOrders = booking.getProductOrders();
		int size = 0;
		for(ProductOrder prodOrder:productOrders ) {
			String s = prodOrder.getStatus();
			if(prodOrder.getStatus().equalsIgnoreCase("Confirmed"))
				size++;		
		}
		
		if(size==productOrders.size()) {
		booking.setStatus(BookingStatus.CNF.getBookingStatus());
		
		booking.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		booking.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		saveBookingOrder(booking,prevBooking);
		
		return "SUCCESS";
		}
		
		return "FAILED";
	}

	public Booking saveBookingOrder(Booking order, String prevBooking) throws BookingEngineDBException {
		Booking orderObj = null;
		
			try {
				orderObj = CopyUtils.copy(order, Booking.class);
			} catch (InvocationTargetException | IllegalAccessException e) {
				 myLogger.fatal("Error while saving booking  object : " + e);
				 //myLogger.error("Error while saving order object: " + e);
				throw new BookingEngineDBException("Failed to save booking object");
			}

		return bookingRepository.saveOrder(orderObj,prevBooking);
	}


	@Override
	public String processAmClResponse(JSONObject resJson) throws BookingEngineDBException {
		Booking booking = bookingRepository.findOne(resJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		String prevBooking = booking.toString();
		
		Set<ProductOrder> productOrders = booking.getProductOrders();
		int size = 0;
		for(ProductOrder prodOrder:productOrders ) {
			String s = prodOrder.getStatus();
			if(prodOrder.getStatus().equalsIgnoreCase(OrderStatus.XL.getProductStatus()))
				size++;		
		}
		
		if(size==productOrders.size()) {
		booking.setStatus(BookingStatus.XL.getBookingStatus());
		
		booking.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		booking.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		saveBookingOrder(booking,prevBooking);
		
		return "SUCCESS";
		}
		
		return "FAILED";
	}


}
