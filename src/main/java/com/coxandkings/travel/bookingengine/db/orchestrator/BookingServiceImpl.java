package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.coxandkings.travel.bookingengine.db.model.*;
import com.coxandkings.travel.bookingengine.db.repository.*;
import com.coxandkings.travel.bookingengine.db.utils.JsonObjectProvider;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.config.DBConfig;
import com.coxandkings.travel.bookingengine.db.enums.ProductSubCategory;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;


@Service
@Transactional(readOnly = false)
public class BookingServiceImpl implements Constants, ErrorConstants {

    @Autowired
    private AccoBookingServiceImpl accoService;
    @Autowired
    private AirBookingServiceImpl airService;
    @Autowired
    private ActivitiesBookingServiceImpl activitiesService;
    @Autowired
    private BusBookingServiceImpl busService;
    @Autowired
    private CarBookingServiceImpl carService;
    @Autowired
    private TransfersBookingServiceImpl transfersService;
    @Autowired
    private CruiseBookingServiceImpl cruiseService;
    @Autowired
    private HolidaysBookingServiceImpl holidayService;

    @Autowired
    @Qualifier("Passenger")
    private PassengerRepository passengerRepository;

    @Autowired
    @Qualifier("Booking")
    private BookingDatabaseRepository bookRepository;

    @Autowired
    private AccoDatabaseRepository accoDatabaseRepository;

    @Autowired
    private ActivitiesDatabaseRepository activitiesDatabaseRepository;

    @Autowired
    private AirDatabaseRepository airDatabaseRepository;
    @Autowired
    private HolidaysDatabaseRepository holidaysDatabaseRepository;

    @Autowired
    private JsonObjectProvider jsonObjectProvider;

    @Autowired
    @Qualifier("UpdateLock")
    private UpdateLockRepository lockRepository;

    @Autowired 
    @Qualifier("PaymentInfo")
    private PaymentInfoRepository paymentRepo;
    
    Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
    JSONObject response = new JSONObject();

    public String getCancellationsByBookID(String bookID) {

        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("No booking details found for bookid  %s ", bookID));
            return (response.toString());
        } else {
            JSONObject resJson = new JSONObject();
            JSONArray productsArray = new JSONArray();
            JSONArray accoArray = accoService.getCancellationsByBooking(booking);
            JSONArray airArray = airService.getCancellationsByBooking(booking);
            JSONArray busArray = busService.getCancellationsByBooking(booking);
            JSONArray carArray = carService.getCancellationsByBooking(booking);
            JSONArray transArray = transfersService.getCancellationsByBooking(booking);

            for (int i = 0; i < accoArray.length(); i++) {
                productsArray.put(accoArray.get(i));
            }
            for (int i = 0; i < airArray.length(); i++) {
                productsArray.put(airArray.get(i));
            }
            for (int i = 0; i < busArray.length(); i++) {
                productsArray.put(busArray.get(i));
            }
            for (int i = 0; i < carArray.length(); i++) {
                productsArray.put(carArray.get(i));
            }
            for (int i = 0; i < transArray.length(); i++) {
                productsArray.put(transArray.get(i));
            }
            if (productsArray.length() > 0) {
                resJson.put(JSON_PROP_BOOKID, bookID);
                //resJson.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
                resJson.put("products", productsArray);
                myLogger.info(String.format("Cancellations retrieved for bookID %s = %s", bookID, resJson.toString()));
                return resJson.toString();
            } else {
                myLogger.info(String.format("No Cancellations  for bookID %s found", bookID));
                response.put("ErrorCode", "BE_ERR_007");
                response.put("ErrorMsg", BE_ERR_007);
                return response.toString();
            }
        }
    }

    public String getAmendmentsByBookID(String bookID) {

        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("No booking details found for bookid  %s ", bookID));
            return (response.toString());
        } else {
            JSONObject resJson = new JSONObject();
            JSONArray productsArray = new JSONArray();
            JSONArray accoArray = accoService.getAmendmentsByBooking(booking);
            JSONArray airArray = airService.getAmendmentsByBooking(booking);
            JSONArray carArray = carService.getAmendmentsByBooking(booking);
            JSONArray cruiseArray = cruiseService.getAmendmentsByBooking(booking);

            for (int i = 0; i < accoArray.length(); i++) {
                productsArray.put(accoArray.get(i));
            }

            for (int i = 0; i < airArray.length(); i++) {
                productsArray.put(airArray.get(i));
            }

            for (int i = 0; i < carArray.length(); i++) {
                productsArray.put(carArray.get(i));
            }
            for (int i = 0; i < cruiseArray.length(); i++) {
                productsArray.put(cruiseArray.get(i));
            }
            if (productsArray.length() > 0) {
                resJson.put(JSON_PROP_BOOKID, bookID);
                //resJson.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
                resJson.put("products", productsArray);
                myLogger.info(String.format("Amendments retrieved for bookID %s = %s", bookID, resJson.toString()));
                return resJson.toString();
            } else {
                myLogger.info(String.format("No amendments  for bookID %s found", bookID));
                response.put("ErrorCode", "BE_ERR_008");
                response.put("ErrorMsg", BE_ERR_008);
                return response.toString();
            }

        }
    }

    public String getByBookID(String bookID, String flag) {

        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("No booking details found for bookid  %s ", bookID));
            return (response.toString());
        } else {
            JSONObject resJson = new JSONObject();
            JSONObject resBody = new JSONObject();
            JSONObject resHdr = new JSONObject();
            JSONObject clientContext = new JSONObject();
            resHdr.put(JSON_PROP_SESSIONID, booking.getSessionID());
            resHdr.put(JSON_PROP_TRANSACTID, booking.getTransactionID());
            resHdr.put(JSON_PROP_USERID, booking.getUserID());

            clientContext.put(JSON_PROP_CLIENTID, booking.getClientID());
            clientContext.put(JSON_PROP_CLIENTTYPE, booking.getClientType());
            clientContext.put(JSON_PROP_CLIENTCURRENCY, booking.getClientCurrency());
            clientContext.put(JSON_PROP_CLIENTIATANUMBER, booking.getClientIATANumber());

            clientContext.put(JSON_PROP_CLIENTLANGUAGE, booking.getClientLanguage());
            clientContext.put(JSON_PROP_CLIENTMARKET, booking.getClientMarket());
            clientContext.put(JSON_PROP_CLIENTNATIONALITY, booking.getClientNationality());

            if (flag.equalsIgnoreCase("false")) {

                clientContext.put(JSON_PROP_COMPANY, booking.getCompanyId());
                clientContext.put(JSON_PROP_GROUP_COMPANYID, booking.getGroupCompanyID());
                clientContext.put(JSON_PROP_GROUP_COMPANIESID, booking.getGroupOfComapniesId());
                clientContext.put(JSON_PROP_BU, booking.getBU());
                clientContext.put(JSON_PROP_SBU, booking.getSBU());

            }

            if (booking.getPos() != null)
                clientContext.put(JSON_PROP_POINTOFSALE, booking.getPos());

            resHdr.put(JSON_PROP_CLIENTCONTEXT, clientContext);

            resBody.put(JSON_PROP_BOOKID, bookID);
            resBody.put(JSON_PROP_STATUS, booking.getStatus());
            resBody.put(JSON_PROP_PKGS_QCSTATUS, booking.getQCStatus());
            resBody.put(JSON_PROP_MERGEID, booking.getMergeID());
            resBody.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
            resBody.put(JSON_PROP_STAFFID, booking.getStaffID());
            resBody.put(JSON_PROP_BOOKINGTYPE,booking.getBookingType());
            resBody.put(JSON_PROP_ISHOLIDAYBOOKING, booking.isHolidayBooking());

            resBody.put(JSON_PROP_PAYMENTINFO, getPaymentInfo(booking));

            JSONArray productsArray = new JSONArray();

            //TODO: have a logic in place so that you only call services of the product that are there in the booking.
            JSONArray accoArray = accoService.process(booking, flag);
            JSONArray airArray = airService.process(booking, flag);
            JSONArray activitiesArray = activitiesService.process(booking, flag);
            JSONArray busArray = busService.process(booking, flag);
            JSONArray carArray = carService.process(booking, flag);
            JSONArray transArray = transfersService.process(booking,flag);
            JSONArray cruiseArray = cruiseService.process(booking);
            JSONArray holidaysArray = holidayService.process(booking, flag);

            for (int i = 0; i < accoArray.length(); i++) {
                productsArray.put(accoArray.get(i));
            }
            for (int i = 0; i < airArray.length(); i++) {
                productsArray.put(airArray.get(i));
            }
            for (int i = 0; i < activitiesArray.length(); i++) {
                productsArray.put(activitiesArray.get(i));
            }
            for (int i = 0; i < busArray.length(); i++) {
                productsArray.put(busArray.get(i));
            }
            for (int i = 0; i < carArray.length(); i++) {
                productsArray.put(carArray.get(i));
            }
            for (int i = 0; i < transArray.length(); i++) {
                productsArray.put(transArray.get(i));
            }
            for (int i = 0; i < cruiseArray.length(); i++) {
                productsArray.put(cruiseArray.get(i));
            }
            for (int i = 0; i < holidaysArray.length(); i++) {
                productsArray.put(holidaysArray.get(i));
            }
            resBody.put("products", productsArray);
            resJson.put(JSON_PROP_RESHEADER, resHdr);
            resJson.put(JSON_PROP_RESBODY, resBody);
            //myLogger.info(String.format("Bookings retrieved for bookID %s = %s", bookID, resJson.toString()));
            return resJson.toString();
        }

    }

    public String getByBookIDAndSupplierId(String bookID,String supplierId, String flag) {

        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("No booking details found for bookid  %s ", bookID));
            return (response.toString());
        } else {
            JSONObject resJson = new JSONObject();
            JSONObject resBody = new JSONObject();
            JSONObject resHdr = new JSONObject();
            JSONObject clientContext = new JSONObject();
            resHdr.put(JSON_PROP_SESSIONID, booking.getSessionID());
            resHdr.put(JSON_PROP_TRANSACTID, booking.getTransactionID());
            resHdr.put(JSON_PROP_USERID, booking.getUserID());

            clientContext.put(JSON_PROP_CLIENTID, booking.getClientID());
            clientContext.put(JSON_PROP_CLIENTTYPE, booking.getClientType());
            clientContext.put(JSON_PROP_CLIENTCURRENCY, booking.getClientCurrency());
            clientContext.put(JSON_PROP_CLIENTIATANUMBER, booking.getClientIATANumber());

            clientContext.put(JSON_PROP_CLIENTLANGUAGE, booking.getClientLanguage());
            clientContext.put(JSON_PROP_CLIENTMARKET, booking.getClientMarket());
            clientContext.put(JSON_PROP_CLIENTNATIONALITY, booking.getClientNationality());

            if (flag.equalsIgnoreCase("false")) {

                clientContext.put(JSON_PROP_COMPANY, booking.getCompanyId());
                clientContext.put(JSON_PROP_GROUP_COMPANYID, booking.getGroupCompanyID());
                clientContext.put(JSON_PROP_GROUP_COMPANIESID, booking.getGroupOfComapniesId());
                clientContext.put(JSON_PROP_BU, booking.getBU());
                clientContext.put(JSON_PROP_SBU, booking.getSBU());

            }

            if (booking.getPos() != null)
                clientContext.put(JSON_PROP_POINTOFSALE, booking.getPos());

            resHdr.put(JSON_PROP_CLIENTCONTEXT, clientContext);

            resBody.put(JSON_PROP_BOOKID, bookID);
            resBody.put(JSON_PROP_STATUS, booking.getStatus());
            resBody.put(JSON_PROP_PKGS_QCSTATUS, booking.getQCStatus());
            resBody.put(JSON_PROP_MERGEID, booking.getMergeID());
            resBody.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
            resBody.put(JSON_PROP_STAFFID, booking.getStaffID());
            resBody.put(JSON_PROP_BOOKINGTYPE,booking.getBookingType());
            resBody.put(JSON_PROP_ISHOLIDAYBOOKING, booking.isHolidayBooking());

            resBody.put(JSON_PROP_PAYMENTINFO, getPaymentInfo(booking));

            JSONArray productsArray = new JSONArray();

            //TODO: have a logic in place so that you only call services of the product that are there in the booking.
         
            
            resBody.put("products", productsArray);
            resJson.put(JSON_PROP_RESHEADER, resHdr);
            resJson.put(JSON_PROP_RESBODY, resBody);
            //myLogger.info(String.format("Bookings retrieved for bookID %s = %s", bookID, resJson.toString()));
            return resJson.toString();
        }

    }
    
    public String getDocumentDetails(JSONObject reqJson) {
        JSONObject response = new JSONObject();
        String productSubCat = reqJson.getString(JSON_PROP_PRODUCTSUBCATEGORY);

        ProductSubCategory prodSubCat = ProductSubCategory.fromString(productSubCat);
        JSONArray documentDetails = null;

        switch (prodSubCat) {
            case HOTEL:
                documentDetails = new JSONArray(accoService.getDocumentDetails(reqJson));
                break;
            case AIR:
                documentDetails = new JSONArray(airService.getDocumentDetails(reqJson));
                break;
            case BUS:
                documentDetails = new JSONArray(busService.getDocumentDetails(reqJson));
                break;

        }
        if (documentDetails.length() > 0) {
            response.put("PassengerDocumentDetails", documentDetails);
            myLogger.info(String.format("Document details retrieved for orderID in req %s = %s", reqJson.getString(JSON_PROP_ORDERID), reqJson.toString()));
        }
        return response.toString();
    }


    private JSONArray getPaymentInfo(Booking booking) {
        JSONArray paymentArray = new JSONArray();
        JSONObject paymentJson = new JSONObject();
        for (PaymentInfo payment : booking.getPaymentInfo()) {      	
            paymentJson.put(JSON_PROP_PAYMENTID, payment.getPayment_info_id());
            paymentJson.put(JSON_PROP_PAYMENTMETHOD, payment.getPaymentMethod());
            paymentJson.put("amountPaid", payment.getAmountPaid());
            paymentJson.put(JSON_PROP_PAYMENTTYPE, payment.getPaymentType());
            paymentJson.put(JSON_PROP_AMOUNTCURRENCY, payment.getAmountCurrency());
            paymentJson.put("paymentStatus", payment.getPaymentStatus());
            paymentJson.put("totalAmount", payment.getTotalAmount());
            paymentJson.put("transactionDate", payment.getTransactionDate());
            paymentJson.put("transactionRefNumber", payment.getTransactionRefNumber());
            
            if(payment.getPaymentAttributes()!=null&&payment.getPaymentAttributes().isEmpty())
            paymentJson.put("paymentAttributes", new JSONObject(payment.getPaymentAttributes()));
         
            paymentArray.put(paymentJson);
        }
        return paymentArray;
    }

    public String getByUserID(String userID) {


        List<Booking> bookings = bookRepository.findByUserID(userID);
        if (bookings.size() == 0) {
            response.put("ErrorCode", "BE_ERR_002");
            response.put("ErrorMsg", BE_ERR_002);
            myLogger.warn(String.format("No booking details found for userId  %s ", userID));
            return response.toString();
        } else {
            JSONArray bookingArray = new JSONArray();

            for (Booking booking : bookings) {

                JSONObject bookingJson = new JSONObject();
                bookingJson.put(JSON_PROP_BOOKID, booking.getBookID());
                bookingJson.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
                bookingJson.put(JSON_PROP_CLIENTID, booking.getClientID());
                bookingJson.put(JSON_PROP_CLIENTTYPE, booking.getClientType());
                bookingJson.put(JSON_PROP_CLIENTCURRENCY, booking.getClientCurrency());
                bookingJson.put(JSON_PROP_CLIENTIATANUMBER, booking.getClientIATANumber());
                bookingJson.put(JSON_PROP_ISHOLIDAYBOOKING, booking.isHolidayBooking());
                bookingJson.put(JSON_PROP_SESSIONID, booking.getSessionID());
                bookingJson.put(JSON_PROP_TRANSACTID, booking.getTransactionID());
                bookingJson.put(JSON_PROP_PAYMENTINFO, getPaymentInfo(booking));

                JSONArray productsArray = new JSONArray();
                JSONArray accoArray = accoService.process(booking, "false");
                JSONArray airArray = airService.process(booking, "false");
                JSONArray activitiesArray = activitiesService.process(booking, "false");
                JSONArray carArray = carService.process(booking, "false");
                JSONArray busArray = busService.process(booking, "false");
                JSONArray transArray = transfersService.process(booking, "false");
                JSONArray holidaysArray = holidayService.process(booking, "false");

                for (int i = 0; i < accoArray.length(); i++) {
                    productsArray.put(accoArray.get(i));
                }
                for (int i = 0; i < airArray.length(); i++) {
                    productsArray.put(airArray.get(i));
                }
                for (int i = 0; i < activitiesArray.length(); i++) {
                    productsArray.put(activitiesArray.get(i));
                }
                for (int i = 0; i < carArray.length(); i++) {
                    productsArray.put(carArray.get(i));
                }
                for (int i = 0; i < busArray.length(); i++) {
                    productsArray.put(busArray.get(i));
                }
                for (int i = 0; i < transArray.length(); i++) {
                    productsArray.put(transArray.get(i));
                }
                for (int i = 0; i < holidaysArray.length(); i++) {
                    productsArray.put(holidaysArray.get(i));
                }

                bookingJson.put("products", productsArray);
                bookingArray.put(bookingJson);
            }
            myLogger.info(String.format("Bookings retrieved for userID %s = %s", userID, bookingArray.toString()));
            return bookingArray.toString();
        }
    }

    public String getByStatus(String status) {


        List<Booking> bookings = bookRepository.findByStatus(status);
        if (bookings.size() == 0) {
            response.put("ErrorCode", "BE_ERR_003");
            response.put("ErrorMsg", BE_ERR_003);
            myLogger.warn(String.format("No booking details found with status  %s ", status));
            return response.toString();
        } else {
            JSONArray bookingArray = new JSONArray();

            for (Booking booking : bookings) {
                JSONObject bookingJson = new JSONObject();
                bookingJson.put(JSON_PROP_BOOKID, booking.getBookID());
                bookingJson.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());
                bookingJson.put(JSON_PROP_CLIENTID, booking.getClientID());
                bookingJson.put(JSON_PROP_CLIENTTYPE, booking.getClientType());
                bookingJson.put(JSON_PROP_CLIENTCURRENCY, booking.getClientCurrency());
                bookingJson.put(JSON_PROP_CLIENTIATANUMBER, booking.getClientIATANumber());
                bookingJson.put(JSON_PROP_ISHOLIDAYBOOKING, booking.isHolidayBooking());
                bookingJson.put(JSON_PROP_SESSIONID, booking.getSessionID());
                bookingJson.put(JSON_PROP_TRANSACTID, booking.getTransactionID());
                bookingJson.put(JSON_PROP_PAYMENTINFO, getPaymentInfo(booking));

                JSONArray productsArray = new JSONArray();
                JSONArray accoArray = accoService.process(booking, "false");
                JSONArray airArray = airService.process(booking, "false");
                JSONArray activitiesArray = activitiesService.process(booking, "false");
                JSONArray carArray = carService.process(booking, "false");
                JSONArray busArray = busService.process(booking, "false");
                JSONArray transArray = transfersService.process(booking, "false");

                for (int i = 0; i < accoArray.length(); i++) {
                    productsArray.put(accoArray.get(i));
                }
                for (int i = 0; i < airArray.length(); i++) {
                    productsArray.put(airArray.get(i));
                }
                for (int i = 0; i < activitiesArray.length(); i++) {
                    productsArray.put(activitiesArray.get(i));
                }
                for (int i = 0; i < carArray.length(); i++) {
                    productsArray.put(carArray.get(i));
                }
                for (int i = 0; i < busArray.length(); i++) {
                    productsArray.put(busArray.get(i));
                }
                for (int i = 0; i < transArray.length(); i++) {
                    productsArray.put(transArray.get(i));
                }

                bookingJson.put("products", productsArray);
                bookingArray.put(bookingJson);
            }
            myLogger.info(String.format("Bookings retrieved with status  %s = %s", status, bookingArray.toString()));
            return bookingArray.toString();
        }

    }


    public String updateOrder(JSONObject reqJson, String updateType) throws BookingEngineDBException {

        switch (updateType) {
            case JSON_PROP_STATUS:
                return updateStatus(reqJson);
            case JSON_PROP_MERGEID:
                return updateMergeID(reqJson);
            case JSON_PROP_PKGS_QCSTATUS:
                return updateQCStatus(reqJson);
            case JSON_PROP_ORDERATTRIBUTE:
                return updateOrderAttribute(reqJson, updateType);
            case JSON_PROP_DOCUMENTS:
                return updateDocumentId(reqJson);
            case JSON_PROP_STAFFID:
                return updateStaffIDs(reqJson);
            case JSON_PROP_VOUCHERS:
    			return updateVouchers(reqJson,updateType);
            case JSON_PROP_EXPIRYTIMELIMIT:
            	return updateTimeLimitDate(reqJson, updateType);
            case JSON_PROP_NOTES:
            	return updateNotes(reqJson);
            case JSON_PROP_PAYMENTINFO:
            	return updatePaymentInfo(reqJson);
            default:
                response.put("ErrorCode", "BE_ERR_000");
                response.put("ErrorMsg", BE_ERR_000);
                myLogger.warn(String.format("Update type %s for req %s not found", updateType, reqJson.toString()));
                return "no match for update type";
        }
    }

    private String updatePaymentInfo(JSONObject reqJson) throws BookingEngineDBException {
		
    	String paymentID = reqJson.getString(JSON_PROP_PAYMENTID);
        PaymentInfo payment = paymentRepo.findOne(paymentID);
        
        if (payment == null) {
            response.put("ErrorCode", "BE_ERR_009");
            response.put("ErrorMsg", BE_ERR_009);
            myLogger.warn(String.format("Failed to update payment since payment not found for id %s", paymentID));
            return response.toString();
        } else {
        	
            String prevPayment= payment.toString();
            
            payment.setPaymentMethod(reqJson.getString(JSON_PROP_PAYMENTMETHOD));
            payment.setPaymentStatus(reqJson.getString(JSON_PROP_PAYMENTMETHOD));
            payment.setPaymentType(reqJson.getString(JSON_PROP_PAYMENTTYPE));
            payment.setPaymentAttributes(reqJson.getJSONObject("paymentAttributes").toString());
            payment.setTotalAmount(reqJson.getString("totalAmount"));
            payment.setAmountCurrency(reqJson.getString(JSON_PROP_AMOUNTCURRENCY));
            payment.setAmountPaid(reqJson.getString("amountPaid"));
            payment.setTransactionDate(reqJson.getString("transactionDate"));
            payment.setTransactionRefNumber(reqJson.getString("transactionRefNumber"));  
            payment.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            payment.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            
            PaymentInfo updatedPaymentObj = savePaymentOrder(payment, prevPayment);
            myLogger.info(String.format("payment updated for booking with pymentID %s = %s", paymentID, updatedPaymentObj));
            return "payment updated Successfully";
        }
    
    	
	}

	public String updateNotes(JSONObject reqJson) throws BookingEngineDBException {
        String bookID = reqJson.getString(JSON_PROP_BOOKID);
        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("Failed to update status since booking not found for bookid %s", bookID));
            return response.toString();
        } else {
            String prevBooking = booking.toString();
            booking.setNotes(reqJson.getJSONArray(JSON_PROP_NOTES).toString());
            booking.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            booking.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            Booking updatedBookingObj = saveBookingOrder(booking, prevBooking);
            myLogger.info(String.format("Status updated for booking with bookid %s = %s", bookID, updatedBookingObj));
            return "booking status updated Successfully";
        }
    }
    private String updateTimeLimitDate(JSONObject reqJson, String updateType) throws BookingEngineDBException {
        String product = reqJson.getString(JSON_PROP_PRODUCTSUBCATEGORY);
        ProductSubCategory prodSubCat = ProductSubCategory.fromString(product);
        switch (prodSubCat) {
            case HOTEL:
                return accoService.updateOrder(reqJson, updateType);
            case AIR:
                return airService.updateOrder(reqJson, updateType);
            default:
                response.put("ErrorCode", "BE_ERR_000");
                response.put("ErrorMsg", BE_ERR_000);
                myLogger.warn(String.format("Update type %s for req %s not found", updateType, reqJson.toString()));
                return "no match for update type";
        }
    }

	private String updateOrderAttribute(JSONObject reqJson, String updateType) throws BookingEngineDBException {
        String product = reqJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_PRODUCT);
        ProductSubCategory prodSubCat = ProductSubCategory.fromString(product);
        switch (prodSubCat) {
            case HOTEL:
                return accoService.updateOrder(reqJson, updateType);
            case AIR:
                return airService.updateOrder(reqJson, updateType);
            default:
                response.put("ErrorCode", "BE_ERR_000");
                response.put("ErrorMsg", BE_ERR_000);
                myLogger.warn(String.format("Update type %s for req %s not found", updateType, reqJson.toString()));
                return "no match for update type";
        }
    }
    
    private String updateVouchers(JSONObject reqJson, String updateType) throws BookingEngineDBException {
        String product = reqJson.getString(JSON_PROP_PRODUCTSUBCATEGORY);
        ProductSubCategory prodSubCat = ProductSubCategory.fromString(product);
        switch (prodSubCat) {
            case HOTEL:
                return accoService.updateOrder(reqJson, updateType);
            case AIR:
                return airService.updateOrder(reqJson, updateType);
            default:
                response.put("ErrorCode", "BE_ERR_000");
                response.put("ErrorMsg", BE_ERR_000);
                myLogger.warn(String.format("Update type %s for req %s not found", updateType, reqJson.toString()));
                return "no match for update type";
        }
    }

    public String updateStatus(JSONObject reqJson) throws BookingEngineDBException {
        String bookID = reqJson.getString(JSON_PROP_BOOKID);
        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("Failed to update status since booking not found for bookid %s", bookID));
            return response.toString();
        } else {
            String prevBooking = booking.toString();
            booking.setStatus(reqJson.getString(JSON_PROP_STATUS));
            booking.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            booking.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            Booking updatedBookingObj = saveBookingOrder(booking, prevBooking);
            myLogger.info(String.format("Status updated for booking with bookid %s = %s", bookID, updatedBookingObj));
            return "booking status updated Successfully";
        }
    }

    public String updateQCStatus(JSONObject reqJson) throws BookingEngineDBException {
        String bookID = reqJson.getString(JSON_PROP_BOOKID);
        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("Failed to update status since booking not found for bookid %s", bookID));
            return response.toString();
        } else {
            String prevBooking = booking.toString();
            booking.setQCStatus(reqJson.getString(JSON_PROP_PKGS_QCSTATUS));
            booking.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            booking.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            Booking updatedBookingObj = saveBookingOrder(booking, prevBooking);
            myLogger.info(String.format("Status updated for booking with bookid %s = %s", bookID, updatedBookingObj));
            return "booking QC status updated Successfully";
        }
    }

    public String updateDocumentId(JSONObject reqJson) throws BookingEngineDBException {
        String bookID = reqJson.getString(JSON_PROP_BOOKID);
        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("Failed to update documentIds since booking not found for bookid %s", bookID));
            return response.toString();
        } else {
            String prevBooking = booking.toString();
            booking.setDocumentIds((reqJson.getJSONArray(JSON_PROP_DOCUMENTS).toString()));
            booking.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
            booking.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
            Booking updatedBookingObj = saveBookingOrder(booking, prevBooking);
            myLogger.info(String.format("DocumentIds updated for booking with bookid %s = %s", bookID, updatedBookingObj));
            return "booking documentIds updated Successfully";
        }

    }

    public String updateMergeID(JSONObject reqJson) throws BookingEngineDBException {
        JSONArray responseArray = new JSONArray();
        for (Object boookIdObject : reqJson.getJSONArray("bookIDs")) {

            JSONObject updateResponse = new JSONObject();
            String bookID = (String) boookIdObject;
            Booking booking = bookRepository.findOne(bookID);
            if (booking == null) {
                updateResponse.put("bookID", bookID);
                updateResponse.put("ErrorCode", "BE_ERR_001");
                updateResponse.put("ErrorMsg", BE_ERR_001);
                responseArray.put(updateResponse);
                myLogger.warn(String.format("Failed to update status since booking not found for bookid %s", bookID));
            } else {

                String prevBooking = booking.toString();
                booking.setMergeID(reqJson.getString("mergeID"));
                booking.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
                booking.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
                Booking updatedBookingObj = saveBookingOrder(booking, prevBooking);
                updateResponse.put("bookID", bookID);
                updateResponse.put("successMessage", "booking merge ID updated Successfully");
                responseArray.put(updateResponse);
                myLogger.info(
                        String.format("Status updated for booking with bookid %s = %s", bookID, updatedBookingObj));
            }
        }
        return "booking merge ID updated Successfully";
    }

    public String updateStaffIDs(JSONObject reqJson) throws BookingEngineDBException {
        JSONArray responseArray = new JSONArray();
        for (Object boookIdObject : reqJson.getJSONArray("bookIDs")) {

            JSONObject updateResponse = new JSONObject();
            String bookID = (String) boookIdObject;
            Booking booking = bookRepository.findOne(bookID);
            if (booking == null) {
                updateResponse.put("bookID", bookID);
                updateResponse.put("ErrorCode", "BE_ERR_001");
                updateResponse.put("ErrorMsg", BE_ERR_001);
                responseArray.put(updateResponse);
                myLogger.warn(String.format("Failed to update status since booking not found for bookid %s", bookID));
            } else {

                String prevBooking = booking.toString();
                booking.setStaffID(reqJson.getString(JSON_PROP_STAFFID));
                booking.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
                booking.setLastModifiedBy(reqJson.getString(JSON_PROP_USERID));
                Booking updatedBookingObj = saveBookingOrder(booking, prevBooking);
                updateResponse.put("bookID", bookID);
                updateResponse.put("successMessage", "booking staff ID updated Successfully");
                responseArray.put(updateResponse);
                myLogger.info(
                        String.format("Status updated for booking with bookid %s = %s", bookID, updatedBookingObj));
            }
        }
        return "booking status ID updated Successfully";

    }

    public Booking saveBookingOrder(Booking order, String prevBooking) throws BookingEngineDBException {
        Booking orderObj = null;

        try {
            orderObj = CopyUtils.copy(order, Booking.class);
        } catch (InvocationTargetException | IllegalAccessException e) {
            myLogger.fatal("Error while saving Acco Passenger order object : " + e);
            throw new BookingEngineDBException("Failed to save order object");
        }

        return bookRepository.saveOrder(orderObj, prevBooking);
    }
    
    public PaymentInfo savePaymentOrder(PaymentInfo order, String prevBooking) throws BookingEngineDBException {
    	PaymentInfo orderObj = null;

        try {
            orderObj = CopyUtils.copy(order, PaymentInfo.class);
        } catch (InvocationTargetException | IllegalAccessException e) {
            myLogger.fatal("Error while saving Acco Passenger order object : " + e);
            throw new BookingEngineDBException("Failed to save payment object");
        }

        return paymentRepo.saveOrder(orderObj, prevBooking);
    }

    public String getOrdersInRange(JSONObject reqJson) throws BookingEngineDBException {
        String product = reqJson.getString("product");
        String strtDate = reqJson.getString(JSON_PROP_STARTDATE);
        String strDate = strtDate.substring(0, strtDate.indexOf('['));
        ZonedDateTime StartdateTime = ZonedDateTime.parse(strtDate);
        String endDate = reqJson.getString(JSON_PROP_ENDDATE);
        String enDate = endDate.substring(0, endDate.indexOf('['));
        ZonedDateTime EnddateTime = ZonedDateTime.parse(endDate);
        String suppRef = reqJson.optString(JSON_PROP_SUPPREF);
        int res;
        switch (product) {
            case "ACCO":
                JSONArray accoOrders = accoService.getOrdersInRange(StartdateTime, EnddateTime, suppRef);
                res = accoOrders.length();
                if (res > 0) {
                    myLogger.info(String.format("No. of orders for supplier %s  between %s and %s = %d", suppRef, strDate, enDate, res));
                    reqJson.put("orderCount", res);
                    reqJson.put("orders", accoOrders);
                    return reqJson.toString();
                } else {
                    myLogger.info(String.format("No orders found for given data %s", reqJson));
                    return "No orders for given data found";
                }
            case "AIR":
                JSONArray airOrders = airService.getOrdersInRange(StartdateTime, EnddateTime, suppRef);
                res = airOrders.length();
                if (res > 0) {
                    myLogger.info(String.format("No. of orders for supplier %s  between %s and %s = %d", suppRef, strDate, enDate, res));
                    reqJson.put("orderCount", res);
                    reqJson.put("orders", airOrders);
                    return reqJson.toString();
                } else {
                    myLogger.info(String.format("No orders found for given data %s", reqJson));
                    return "No orders for given data found";
                }
            case "BUS":
                JSONArray busOrders = busService.getOrdersInRange(StartdateTime, EnddateTime, suppRef);
                res = busOrders.length();
                if (res > 0) {
                    myLogger.info(String.format("No. of orders for supplier %s  between %s and %s = %d", suppRef, strDate, enDate, res));
                    reqJson.put("orderCount", res);
                    reqJson.put("orders", busOrders);
                    return reqJson.toString();
                } else {
                    myLogger.info(String.format("No orders found for given data %s", reqJson));
                    return "No orders for given data found";
                }
            default:
                myLogger.warn(String.format("Requested product not found for req %s", reqJson.toString()));
                throw new BookingEngineDBException("Requested Product not found");
        }

    }

    public String getPolicies(String bookID) {
        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("No booking details found for bookid  %s ", bookID));
            return (response.toString());
        } else {
            JSONArray productsArray = new JSONArray();
            JSONArray accoArray = accoService.getPoliciesByBooking(booking);
            JSONArray busArray = busService.getPoliciesByBooking(booking);
            JSONObject productobj = new JSONObject();
            if (accoArray.length() > 0)
                productobj.put(JSON_PROP_PRODUCTACCO, accoArray);
            if (busArray.length() > 0)
                productobj.put(JSON_PROP_PRODUCTBUS, busArray);

            if (productobj.length() > 0)
                productsArray.put(productobj);
            if (productsArray.length() > 0) {
                myLogger.info(String.format("Policies retrieved for bookid %s = %s", bookID, productsArray));
                return productsArray.toString();
            } else {
                response.put("ErrorCode", "BE_ERR_006");
                response.put("ErrorMsg", BE_ERR_006);
                myLogger.warn(String.format("No policies found for given bookid  %s ", bookID));
                return (response.toString());
            }
        }
    }

    public String acquireLock(JSONObject reqJson) throws BookingEngineDBException {
        UpdateLock lockObject = lockRepository.findOne(reqJson.getString("orderId"));

        if (lockObject != null && Duration.between(lockObject.getCreatedAt(), ZonedDateTime.now(ZoneOffset.UTC)).toMinutes() < DBConfig.getLockValidity() && !(reqJson.getString("appId").equals(lockObject.getAppId())
                && reqJson.getString("userId").equals(lockObject.getUserId()))) {
            JSONObject errorjson = new JSONObject();
            errorjson.put("isLockAcquired", false);
            errorjson.put("orderId", reqJson.getString("orderId"));
            errorjson.put("errorMessage", "lock is already acquired");
            errorjson.put("acquiredByUser", lockObject.getUserId());
            errorjson.put("acquiredByApp", lockObject.getAppId());
            myLogger.warn(String.format("Lock failed to acquire for req  %s = %s", reqJson, errorjson));
            return errorjson.toString();
        }

        lockObject = new UpdateLock();
        lockObject.setAppId(reqJson.getString("appId"));
        lockObject.setOrderId(reqJson.getString("orderId"));
        lockObject.setUserId(reqJson.getString("userId"));
        lockObject.setSessionId(reqJson.getString("sessionId"));
        lockObject.setBookId(reqJson.optString("bookId"));
        lockObject.setLockAcquired(true);
        ZonedDateTime createdAt = ZonedDateTime.now(ZoneOffset.UTC);
        lockObject.setCreatedAt(createdAt);
        saveUpdateLock(lockObject, "");

        // creating the response to be sent back

        reqJson.put("isLockAcquired", true);
        reqJson.put("lockAcquiredAt", createdAt.toString());
        reqJson.put("lockvalidity", DBConfig.getLockValidity());

        return reqJson.toString();
    }

    public String releaseLock(JSONObject reqJson) throws BookingEngineDBException {
        UpdateLock lockObject = lockRepository.findOne(reqJson.getString("orderId"));
        JSONObject errorjson = new JSONObject();
        if(lockObject==null)
        {
        	errorjson.put("isLockReleased", false);
            errorjson.put("orderId", reqJson.getString("orderId"));
            errorjson.put("errorMessage", "Lock is not acquired for given orderId");
            return errorjson.toString();
        }
        else {
        String prevOrder = lockObject.toString();
        boolean appIdTest = reqJson.getString("appId").equalsIgnoreCase(lockObject.getAppId());
        boolean userIdTest = reqJson.getString("userId").equalsIgnoreCase(lockObject.getUserId());
        if (lockObject != null && reqJson.getString("appId").equalsIgnoreCase(lockObject.getAppId()) && reqJson.getString("userId").equalsIgnoreCase(lockObject.getUserId())) {
            lockObject.setLockAcquired(false);
            saveUpdateLock(lockObject, prevOrder);
            reqJson.put("isLockReleased", true);
            reqJson.put("lockReleasedAt", ZonedDateTime.now(ZoneOffset.UTC));
            myLogger.info(String.format("Lock released for request %s", reqJson));
        } else {
        	 if(!appIdTest)
        	{
        		errorjson.put("isLockReleased", false);
                errorjson.put("orderId", reqJson.getString("orderId"));
                errorjson.put("errorMessage", "AppId doesn't match");
        	}
        	else if (!userIdTest)
        	{
        		errorjson.put("isLockReleased", false);
                errorjson.put("orderId", reqJson.getString("orderId"));
                errorjson.put("errorMessage", "UserId doesn't match");
        	}
        	else
        	{
            errorjson.put("isLockReleased", false);
            errorjson.put("orderId", reqJson.getString("orderId"));
            errorjson.put("errorMessage", "lock is already released");
        	}
            myLogger.info(String.format("Lock failed to release for request %s = %s", reqJson, errorjson));
            return errorjson.toString();
        }
        }

        // creating the response to be sent back


        //reqJson.put("lockvalidity", DBConfig.getLockValidity());

        return reqJson.toString();
    }

    public UpdateLock saveUpdateLock(UpdateLock order, String prevOrder) throws BookingEngineDBException {
        UpdateLock orderObj = null;

        try {
            orderObj = CopyUtils.copy(order, UpdateLock.class);
        } catch (InvocationTargetException | IllegalAccessException e) {
            myLogger.fatal("Error while saving Acco Passenger order object : " + e);
            throw new BookingEngineDBException("Failed to save order object");
        }
        return lockRepository.saveOrder(orderObj, prevOrder);
    }


    public String getDocumentForBooking(String bookID) {

        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("No booking details found for bookid  %s ", bookID));
            return (response.toString());
        }

        return booking.getDocumentIds();
    }


    public String getBookingDocuments(String bookID) {

        Booking booking = bookRepository.findOne(bookID);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("No booking details found for bookid  %s ", bookID));
            return (response.toString());
        }

        String bookingDocuments = "[";
        String roomDocument = "[";
        String paxDocumentDetails = "[";

        //code for booking level document
        bookingDocuments = getBookingLevelDocument(bookID, bookingDocuments);


        //Air
        List<AirOrders> airOrders = null;
        try {
            airOrders = airDatabaseRepository.findByBooking(booking);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (AirOrders airOrder : airOrders) {
            String paxDetails = airOrder.getPaxDetails();
            if (null != paxDetails) {
                paxDocumentDetails = paxDocumentDetails.concat(getPaxDocuments(paxDetails));
            }

        }

        /**Code for getting room level document **/
        List<AccoOrders> accoOrders = accoDatabaseRepository.findByBooking(booking);
        for (AccoOrders accoOrder : accoOrders) {
            Set<AccoRoomDetails> roomDetails = accoOrder.getRoomDetails();

            for (AccoRoomDetails accoRoomDetails : roomDetails) {
                String documentIds = accoRoomDetails.getDocumentIds();
                if (null != documentIds) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("roomID", accoRoomDetails.getId());
                    JSONArray jsonArray = new JSONArray("[" + documentIds + "]");
                    jsonObject.put("documentInfo", jsonArray.get(0));
                    roomDocument = roomDocument.concat(jsonObject.toString()).concat(",");
                }
                String paxDetails = accoRoomDetails.getPaxDetails();
                if (null != paxDetails) {
                    paxDocumentDetails = paxDocumentDetails.concat(getPaxDocuments(paxDetails));
                }

            }
        }

        //activities
        List<ActivitiesOrders> activitiesOrders = activitiesDatabaseRepository.findByBooking(booking);
        for (ActivitiesOrders activitiesOrder : activitiesOrders) {
            String paxDetails = activitiesOrder.getPaxDetails();
            if (null != paxDetails) {
                paxDocumentDetails = getPaxDocuments(paxDetails);
            }

        }

        //holiday
        List<HolidaysOrders> holidaysOrders = holidaysDatabaseRepository.findByBooking(booking);
        for (HolidaysOrders holidaysOrder : holidaysOrders) {
            String paxDocuments = getPaxDocuments(holidaysOrder.getPaxDetails());
            if (null != paxDocuments) {
                paxDocumentDetails = paxDocumentDetails.concat(paxDocuments);
            }
        }

        if (paxDocumentDetails.endsWith(",")) {
            paxDocumentDetails = paxDocumentDetails.substring(0, paxDocumentDetails.length() - 1);
        }
        if (roomDocument.endsWith(",")) {
            roomDocument = roomDocument.substring(0, roomDocument.length() - 1);
        }
        if (null != paxDocumentDetails) {
            return "{\"bookingDocuments\":".concat(bookingDocuments).concat("],\"roomDocument\": ").concat(roomDocument).concat("],\"paxDocument\" :").concat(paxDocumentDetails).concat("]}");
        } else {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("No booking documents found for bookid  %s ", bookID));
            return (response.toString());
        }
    }

    /**
     * This code will provide booking level document
     *
     * @param bookID
     * @param bookingDocuments
     * @return
     */
    private String getBookingLevelDocument(String bookID, String bookingDocuments) {
        String documentForBooking = getDocumentForBooking(bookID);
        if (null != documentForBooking) {

            List<String> documentList = new ArrayList<>();
            documentList.add(documentForBooking);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("bookID", bookID);

            JSONArray jsonArray = new JSONArray("[" + documentForBooking + "]");

            jsonObject.put("documentInfo", jsonArray.get(0));
            bookingDocuments = bookingDocuments.concat(jsonObject.toString());
        }
        return bookingDocuments;
    }

    private String getPaxDocuments(String paxDetails) {
        List<String> paxList = jsonObjectProvider.getChildrenCollection(paxDetails, "$.*.paxID", String.class);
        String paxDocumentDetails = "";
        if (null != paxList) {
            for (String paxId : paxList) {
                PassengerDetails paxDetail = passengerRepository.findOne(paxId);
                String documentDetails = paxDetail.getDocumentDetails();
                if (null != documentDetails) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("paxID", paxId);
                    try {
                        jsonObject.put("documentInfo", jsonObjectProvider.getChildObject(paxDetail.getDocumentDetails(), "$.documentInfo.*", Object.class));
                    } catch (Exception e) {
                        myLogger.error("DocumentInfo not stored in proper format " + paxDetail.getDocumentDetails());
                    }
                    /*paxDocumentDetails=paxDocumentDetails.concat("{\"paxID\":\""+paxId+"\",");
                    paxDocumentDetails = paxDocumentDetails.concat(paxDetail.getDocumentDetails()).concat("},");*/
                    paxDocumentDetails = paxDocumentDetails.concat(jsonObject.toString()).concat(",");
                }

            }


        }
        return paxDocumentDetails;
    }


}
