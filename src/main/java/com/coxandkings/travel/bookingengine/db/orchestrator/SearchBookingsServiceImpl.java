package com.coxandkings.travel.bookingengine.db.orchestrator;

import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.FlightArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.GeneralArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.HotelArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.BookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.FailureBookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.managefailures.DuplicateAccommodationBookingsSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.managefailures.DuplicateFlightBookingsSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.manageproductupdates.FlightUpdatesSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.mergebooking.MergeBookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.productsharing.ProductSharingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.enums.ProductSubCategory;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.*;
import com.coxandkings.travel.bookingengine.db.repository.AccoDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.AirDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.repository.search.*;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListFlightInfo;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListGeneralInfo;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListHotelInfo;
import com.coxandkings.travel.bookingengine.db.resource.managecheaperprices.CheaperPriceBookingInfo;
import com.coxandkings.travel.bookingengine.db.resource.managefailures.*;
import com.coxandkings.travel.bookingengine.db.resource.manageproductupdates.ProductUpdateFlightInfo;
import com.coxandkings.travel.bookingengine.db.resource.manageproductupdates.ProductUpdateFlightResponse;
import com.coxandkings.travel.bookingengine.db.resource.productreview.ProductReviewInfo;
import com.coxandkings.travel.bookingengine.db.resource.productsharing.ProductSharingInfo;
import com.coxandkings.travel.bookingengine.db.resource.searchviewfilter.BookingSearchResponseItem;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;
import com.coxandkings.travel.bookingengine.db.utils.SearchUtil;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SearchBookingsServiceImpl implements Constants, ErrorConstants {

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
    @Qualifier("Booking")
    private BookingDatabaseRepository bookRepository;

    @Autowired
    @Qualifier("ProductUpdatesSearchRepository")
    private ProductUpdatesSearchRepository productUpdatesSearchRepository;

    @Autowired
    @Qualifier("ProductSharingSearchRepository")
    private ProductSharingSearchRepository productSharingRepository;

    @Autowired
    @Qualifier("ArrivalListSearchRepository")
    private ArrivalListSearchRepository arrivalListSearchRepository;

    @Autowired
    @Qualifier("MergeBookingSearchRepository")
    private MergeBookingSearchRepository mergeBookingSearchRepository;

    @Autowired
    @Qualifier("ProductReviewSearchRepository")
    private ProductReviewSearchRepository productReviewSearchRepository;

    @Autowired
    @Qualifier("ManageFailuresRepository")
    private ManageFailuresRepository manageFailuresRepository;

    @Autowired
    @Qualifier("Passenger")
    private PassengerRepository passengerRepository;

    @Autowired
    private BookingServiceImpl bookingService;

    @Qualifier("Acco")
    @Autowired
    private AccoDatabaseRepository accoRepository;

    @Qualifier("Air")
    @Autowired
    private AirDatabaseRepository airRepository;

    Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
    JSONObject response = new JSONObject();

    @Transactional
    public String findBySearchCriteria(BookingSearchCriteria bookingCriteria) {
        List<Booking> bookings = bookRepository.findBySearchCriteria(bookingCriteria);
        JSONArray bookingArray = new JSONArray();
        Iterator<Booking> bookingsItr = bookings.iterator();
        while (bookingsItr.hasNext()) {

            Booking booking = bookingsItr.next();
            String bookID = booking.getBookID();
            if (booking == null) {
                response.put("ErrorCode", "BE_ERR_001");
                response.put("ErrorMsg", BE_ERR_001);
                myLogger.warn(String.format("No booking details found for bookid  %s ", bookID));
                return (response.toString());
            }

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
            clientContext.put(JSON_PROP_COMPANY, booking.getCompanyId());

            resHdr.put(JSON_PROP_CLIENTCONTEXT, clientContext);

            resBody.put(JSON_PROP_BOOKID, bookID);
            resBody.put(JSON_PROP_STATUS, booking.getStatus());
            resBody.put(JSON_PROP_PKGS_QCSTATUS, booking.getQCStatus());
            resBody.put(JSON_PROP_MERGEID, booking.getMergeID());
            if (booking.getCreatedAt() != null) {
                ZonedDateTime zdt = booking.getCreatedAt();

                String bookingDT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(zdt);
                resBody.put(JSON_PROP_BOOKINGDATE, booking.getCreatedAt());

                resBody.put(JSON_PROP_ISHOLIDAYBOOKING, booking.isHolidayBooking());

                resBody.put(JSON_PROP_PAYMENTINFO, getPaymentInfo(booking));


                HashMap<String, Boolean> matchConditions = conditionsToMatchForProduct(bookingCriteria);

                Boolean missed = false;
                JSONArray productsArray = new JSONArray();
                JSONArray accoArray = accoService.process(booking, "false");
                for (int i = 0, size = accoArray.length(); i < size; i++) {
                    JSONObject objectInArray = accoArray.getJSONObject(i);
                    matchConditions = parseJSONObject(objectInArray, bookingCriteria, matchConditions);
                }


                JSONArray airArray = airService.process(booking, "false");
                for (int i = 0, size = airArray.length(); i < size; i++) {
                    JSONObject objectInArray = airArray.getJSONObject(i);
                    matchConditions = parseJSONObject(objectInArray, bookingCriteria, matchConditions);
                }

//			JSONArray activitiesArray = activitiesService.process(booking);
//			for (int i = 0, size = activitiesArray.length(); i < size; i++) {
//				JSONObject objectInArray = activitiesArray.getJSONObject(i);
//				matchConditions = parseJSONObject(objectInArray, bookingCriteria, matchConditions);
//			}
//
//			JSONArray busArray  = busService.process(booking);
//			for (int i = 0, size = busArray.length(); i < size; i++) {
//				JSONObject objectInArray = busArray.getJSONObject(i);
//				matchConditions = parseJSONObject(objectInArray, bookingCriteria, matchConditions);
//			}

                missed = checkMatchConditionMissed(matchConditions);
                if (missed) {
                    bookingsItr.remove();
                    continue;
                }

                for (int i = 0; i < accoArray.length(); i++) {
                    productsArray.put(accoArray.get(i));
                }
                for (int i = 0; i < airArray.length(); i++) {
                    productsArray.put(airArray.get(i));
                }
//			for(int i=0;i<activitiesArray.length();i++) {
//				productsArray.put(activitiesArray.get(i));
//			}
//			for(int i=0;i<busArray.length();i++) {
//				productsArray.put(busArray.get(i));
//			}

                resBody.put("products", productsArray);
                resJson.put(JSON_PROP_RESHEADER, resHdr);
                resJson.put(JSON_PROP_RESBODY, resBody);
                myLogger.info(String.format("Bookings retrieved for bookID %s = %s", bookID, resJson.toString()));


                bookingArray.put(resJson);
            }

        }

        return bookingArray.toString();
    }

    //TODO: remove this method when, you remove function findBySearchCriteria()
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

    private Boolean checkMatchConditionMissed(HashMap matchConditions) {
        Boolean missed = false;
        Iterator it = matchConditions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            if (pair.getValue().equals(false)) {
                missed = true;
                break;
            }
        }
        return missed;
    }

    private void parseJSONArray(JSONArray aArray, BookingSearchCriteria
            bookingCriteria, HashMap<String, Boolean> matchConditions) {
        for (int i = 0, size = aArray.length(); i < size; i++) {
            if (aArray.get(i) instanceof JSONObject) {
                JSONObject objectInArray = aArray.getJSONObject(i);
                parseJSONObject(objectInArray, bookingCriteria, matchConditions);
            }
        }
    }

    private HashMap<String, Boolean> parseJSONObject(JSONObject objectInArray, BookingSearchCriteria bookingCriteria,
                                                     HashMap<String, Boolean> matchConditions) {

        String[] elementNames = JSONObject.getNames(objectInArray);
        for (String elementName : elementNames) {
            Object aObj = objectInArray.get(elementName);
            if (aObj instanceof String) {
                String value = objectInArray.getString(elementName);
                switch (elementName) {
                    case "sourceSupplierName":
                        if (!StringUtils.isEmpty(bookingCriteria.getProductBasedFilter().getSupplierName())) {
                            if (value.contains(bookingCriteria.getProductBasedFilter().getSupplierName())) {
                                matchConditions.put("supplierName", true);
                            }
                        }
                        break;
                    case "productCategory":
                        if (!StringUtils.isEmpty(bookingCriteria.getProductBasedFilter().getProductCategoryId())) {
                            if (value.equalsIgnoreCase(bookingCriteria.getProductBasedFilter().getProductCategoryId())) {
                                matchConditions.put("productCategoryId", true);
                            }
                        }
                        break;
                    case "productSubCategory":
                        if (!StringUtils.isEmpty(bookingCriteria.getProductBasedFilter().getProductCategorySubTypeId())) {
                            if (value.equalsIgnoreCase(bookingCriteria.getProductBasedFilter().getProductCategorySubTypeId())) {
                                matchConditions.put("productCategorySubTypeId", true);
                            }
                        }
                        break;
                    case "firstName":
                        if (!StringUtils.isEmpty(bookingCriteria.getClientPxBasedFilter().getPassengerName())) {
                            if (value.contains(bookingCriteria.getClientPxBasedFilter().getPassengerName())) {
                                matchConditions.put("passengerName", true);
                            }
                        }
                        break;
                }
            }
            if (aObj instanceof JSONObject) {
                parseJSONObject(objectInArray.getJSONObject(elementName), bookingCriteria, matchConditions);
            }
            if (aObj instanceof JSONArray) {
                parseJSONArray(objectInArray.getJSONArray(elementName), bookingCriteria, matchConditions);
            }
        }
        return matchConditions;
    }

    private HashMap<String, Boolean> conditionsToMatchForProduct(BookingSearchCriteria bookingCriteria) {

        HashMap<String, Boolean> conditionsToMatch = new HashMap<>();

        if (!(StringUtils.isEmpty(bookingCriteria.getProductBasedFilter().getSupplierName()))) {
            conditionsToMatch.put("supplierName", false);
        }

        if (!(StringUtils.isEmpty(bookingCriteria.getProductBasedFilter().getProductCategoryId()))) {
            conditionsToMatch.put("productCategoryId", false);
        }

        if (!(StringUtils.isEmpty(bookingCriteria.getProductBasedFilter().getProductCategorySubTypeId()))) {
            conditionsToMatch.put("productCategorySubTypeId", false);
        }


        if (!(StringUtils.isEmpty(bookingCriteria.getClientPxBasedFilter().getPassengerName()))) {
            conditionsToMatch.put("passengerName", false);
        }

        return conditionsToMatch;
    }

    public List<? extends CheaperPriceBookingInfo> searchCheaperPriceBookings(String productSubCategory) {
        List<? extends CheaperPriceBookingInfo> bookingsResults = new ArrayList<>();
        try {
            bookingsResults = productUpdatesSearchRepository.searchCheaperPriceBookings(productSubCategory);
        } catch (SQLException e) {
            e.printStackTrace();
            bookingsResults = new ArrayList<>(0);
        }
        return bookingsResults;
    }

    public List<BookingSearchResponseItem> searchBookings(BookingSearchCriteria bookingSearchCriteria) {
        List<BookingSearchResponseItem> bookingSearchResponseItems = bookRepository.searchBookings(bookingSearchCriteria);
        return bookingSearchResponseItems;
    }


    public List<ProductSharingInfo> searchProductSharingBookings(ProductSharingSearchCriteria productSharingSearchCriteria) {
        List<ProductSharingInfo> productSharingInfoResults = new ArrayList<>();
        try {
            productSharingInfoResults = productSharingRepository.searchProductSharingBookings(productSharingSearchCriteria);
        } catch (SQLException e) {
            e.printStackTrace();
            productSharingInfoResults = new ArrayList<>(0);
        }
        return productSharingInfoResults;
    }

    public List<ArrivalListHotelInfo> searchArrivalListHotel(HotelArrivalListSearchCriteria arrivalListSearchCriteria)
            throws BookingEngineDBException {
        List<ArrivalListHotelInfo> arrivalListHotelInfoResults = new ArrayList<>();
        try {
            arrivalListHotelInfoResults = arrivalListSearchRepository.searchHotelArrivalList(arrivalListSearchCriteria);
        } catch (SQLException e) {
            e.printStackTrace();
            arrivalListHotelInfoResults = new ArrayList<>(0);
        }
        return arrivalListHotelInfoResults;
    }

    public List<ArrivalListFlightInfo> searchArrivalListFlight(FlightArrivalListSearchCriteria arrivalListSearchCriteria)
            throws BookingEngineDBException {
        List<ArrivalListFlightInfo> arrivalListFlightInfoResults = new ArrayList<>();

        try {
            arrivalListFlightInfoResults = arrivalListSearchRepository.searchFlightArrivalList(arrivalListSearchCriteria);
        } catch (SQLException e) {
            e.printStackTrace();
            arrivalListFlightInfoResults = new ArrayList<>(0);
        }
        return arrivalListFlightInfoResults;
    }


    public List<ArrivalListGeneralInfo> searchGeneralArrivalList(GeneralArrivalListSearchCriteria generalArrivalListSearchCriteria) {
        List<ArrivalListGeneralInfo> arrivalListGeneralInfoList = new ArrayList<>();

        try {
            arrivalListGeneralInfoList = arrivalListSearchRepository.searchGeneralArrivalList(generalArrivalListSearchCriteria);
        } catch (SQLException e) {
            e.printStackTrace();
            arrivalListGeneralInfoList = new ArrayList<>(0);
        }
        return arrivalListGeneralInfoList;
    }


    public String searchForMergeBookings(MergeBookingSearchCriteria mergeBookingSearchCriteria) {
        String mergeBookingInfoList = null;

        try {
            mergeBookingInfoList = mergeBookingSearchRepository.searchMergeBookings(mergeBookingSearchCriteria);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mergeBookingInfoList;
    }
    
    
    public String getMergeBookings() {
        String mergeBookingInfoList = null;

        try {
            mergeBookingInfoList = mergeBookingSearchRepository.getMergeBookings();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mergeBookingInfoList;
    }

    public ProductUpdateFlightResponse searchFlightsForProductUpdates(FlightUpdatesSearchCriteria flightUpdatesSearchCriteria) {
        ProductUpdateFlightResponse aProductUpdateFlightResponse = new ProductUpdateFlightResponse();

        try {
            aProductUpdateFlightResponse =
                    productUpdatesSearchRepository.searchFlightsForProductUpdates(flightUpdatesSearchCriteria);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return aProductUpdateFlightResponse;
    }


    public List<ProductReviewInfo> searchFlightsForProductUpdates(String productEndDate) {
        List<ProductReviewInfo> productReviewInfoList = new ArrayList<>();

        try {
            productReviewInfoList =
                    productReviewSearchRepository.searchGeneralArrivalList(productEndDate);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productReviewInfoList;
    }

    //TODO : Optimize it by taking only the booking which match the previously matched order.
    @Transactional
    public String searchDuplicateBookings(String bookId) {
        String duplicateBookings = null;
        SearchUtil filterUtil = new SearchUtil();

        Booking booking = bookRepository.findOne(bookId);
        if (booking == null) {
            response.put("ErrorCode", "BE_ERR_001");
            response.put("ErrorMsg", BE_ERR_001);
            myLogger.warn(String.format("No booking details found for bookid  %s ", bookId));
            return (response.toString());
        }

        List<AccoOrders> accoOrders = accoRepository.findByBooking(booking);
        List<AirOrders> airOrders = airRepository.findByBooking(booking);

        List<String> searchCriteriaList = new ArrayList<>();

        DuplicateFlightBookingsSearchCriteria duplicateFlightBookingsSearchCriteria
                = new DuplicateFlightBookingsSearchCriteria();

        StringBuilder flightArrayBuffer = new StringBuilder();
        StringBuilder accoArrayBuffer = new StringBuilder();
        Integer lastFlightIndex = flightArrayBuffer.length();

        flightArrayBuffer.append("{");
        accoArrayBuffer.append("{");
        for (AirOrders airOrder : airOrders) {

            for (Object paxId : new JSONArray(airOrder.getPaxDetails())) {
                JSONObject paxIdJson = (JSONObject) paxId;
                PassengerDetails guest = passengerRepository.findOne(paxIdJson.getString(JSON_PROP_PAXID));
                duplicateFlightBookingsSearchCriteria.setFirstName(guest.getFirstName());
                duplicateFlightBookingsSearchCriteria.setLastName(guest.getLastName());

                JSONObject flightDetails = new JSONObject(airOrder.getFlightDetails());
                JSONArray originDestinationOptions = flightDetails.getJSONArray("originDestinationOptions");
                for (Object originDestinationOption : originDestinationOptions) {
                    if (originDestinationOption instanceof JSONObject) {
                        JSONArray flightSegment = ((JSONObject) originDestinationOption).getJSONArray("flightSegment");
                        for (Object o : flightSegment) {
                            if (o instanceof JSONObject) {

                                String flightNumber = ((JSONObject) o).getJSONObject("operatingAirline")
                                        .getString("flightNumber");

                                duplicateFlightBookingsSearchCriteria.setFlightNumber(flightNumber);
                                String fromSector = ((JSONObject) o).getString("originLocation");
                                String toSector = ((JSONObject) o).getString("destinationLocation");
                                String travelFromDate = ((JSONObject) o).getString("departureDate");
                                String travelToDate = ((JSONObject) o).getString("arrivalDate");
                                String cabinType = ((JSONObject) o).getString("cabinType");


                                duplicateFlightBookingsSearchCriteria.setFlightNumber(flightNumber);
                                duplicateFlightBookingsSearchCriteria.setFromSector(fromSector);
                                duplicateFlightBookingsSearchCriteria.setToSector(toSector);
                                duplicateFlightBookingsSearchCriteria.setTravelFromDate(travelFromDate);
                                duplicateFlightBookingsSearchCriteria.setTravelToDate(travelToDate);
                                duplicateFlightBookingsSearchCriteria.setCabinType(cabinType);

                                flightArrayBuffer.append(filterUtil.getDuplicateFlightBookingsCriteria(
                                        duplicateFlightBookingsSearchCriteria));
                            }
                        }
                    }
                }
            }
        }

        for (AccoOrders accoOrder : accoOrders) {
            DuplicateAccommodationBookingsSearchCriteria
                    duplicateAccoBookingsSearchCriteria = new DuplicateAccommodationBookingsSearchCriteria();

            Set<AccoRoomDetails> accoRoomDetails = accoOrder.getRoomDetails();

            for (AccoRoomDetails eachRoomDetail : accoRoomDetails) {
                duplicateAccoBookingsSearchCriteria.setCheckInDate(eachRoomDetail.getCheckInDate());
                duplicateAccoBookingsSearchCriteria.setCheckOutDate(eachRoomDetail.getCheckOutDate());
                duplicateAccoBookingsSearchCriteria.setHotelCode(eachRoomDetail.getHotelCode());

                for (Object paxId : new JSONArray(eachRoomDetail.getPaxDetails())) {
                    JSONObject paxIdJson = (JSONObject) paxId;
                    PassengerDetails guest = passengerRepository.findOne(paxIdJson.getString(JSON_PROP_PAXID));
                    duplicateAccoBookingsSearchCriteria.setFirstName(guest.getFirstName());
                    duplicateAccoBookingsSearchCriteria.setLastName(guest.getLastName());
                }

                accoArrayBuffer.append(filterUtil.getDuplicateAccoBookingsCriteria(
                        duplicateAccoBookingsSearchCriteria));
            }
        }

        if(flightArrayBuffer.length() == 1) {
            flightArrayBuffer.append("}");
        }else if(flightArrayBuffer.length() > 1 ) {
            flightArrayBuffer.replace(flightArrayBuffer.length() - 1, flightArrayBuffer.length(), "}");
        }


        searchCriteriaList.add(flightArrayBuffer.toString());

        if(accoArrayBuffer.length() == 1) {
            accoArrayBuffer.append("}");
        }else if(accoArrayBuffer.length() > 1){
            accoArrayBuffer.replace(accoArrayBuffer.length() - 1, accoArrayBuffer.length(), "}");
        }

        searchCriteriaList.add(accoArrayBuffer.toString());


        try {
            duplicateBookings = manageFailuresRepository.searchDuplicateBookings(searchCriteriaList, bookId);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return duplicateBookings;

    }

    public List<FailureDetailsResource> searchFailedBookings(FailureBookingSearchCriteria bookingSearchCriteria) {

        List<FailedBookingsSearchResponse> failedBookingsSearchResponseList
                = manageFailuresRepository.searchFailedBookings(bookingSearchCriteria);

        Iterator<FailedBookingsSearchResponse> failedBookingsSearchResponseItr = failedBookingsSearchResponseList.iterator();

        List<FailureDetailsResource> failureDetailsResourceList = new ArrayList<>();


        while (failedBookingsSearchResponseItr.hasNext()) {

            FailedBookingsSearchResponse failedBookingsSearchResponse = failedBookingsSearchResponseItr.next();

            Optional<FailureDetailsResource> optionalFailureDetailsResource = failureDetailsResourceList.stream().filter(
                    aFailureDetailsResource -> aFailureDetailsResource.getBookID().equals(failedBookingsSearchResponse.getBookID()))
                    .findFirst();



            if (optionalFailureDetailsResource.isPresent()) {
                FailureDetailsResource failureDetailsResourceItem = optionalFailureDetailsResource.get();
                Optional<ProductSummary> optionalProductSummary = failureDetailsResourceItem.getProductSummary().
                        stream().filter(aProductSummary -> aProductSummary.getProductSubCategory().equalsIgnoreCase(
                        failedBookingsSearchResponse.getProductSubCategory())).findFirst();

                if (optionalProductSummary.isPresent()) {
                    ProductSummary productSummary = optionalProductSummary.get();
                    List<OrderSummary> orderSummaries = new ArrayList<>();
                    OrderSummary orderSummary = new OrderSummary();
                    orderSummary.setDetailsSummary(failedBookingsSearchResponse.getDetailsSummary());
                    orderSummary.setSupplierId(failedBookingsSearchResponse.getSupplierId());
                    orderSummary.setTravelDate(failedBookingsSearchResponse.getTravelDate());

                    OrderDetailsResource orderDetailsResource = new OrderDetailsResource();
                    orderDetailsResource.setOrderId(failedBookingsSearchResponse.getOrderId());
                    orderDetailsResource.setOrderStatus(failedBookingsSearchResponse.getOrderStatus());

                    if(orderDetailsResource.getOrderSummary()!= null) {
                        orderDetailsResource.getOrderSummary().add(orderSummary);
                    }else{
                        orderSummaries.add(orderSummary);
                        orderDetailsResource.setOrderSummary(orderSummaries);
                    }

                    productSummary.setProductSubCategory(failedBookingsSearchResponse.getProductSubCategory());
                    productSummary.getOrders().add(orderDetailsResource);

                    failureDetailsResourceItem.getProductSummary().add(productSummary);
                }
                else
                {

                    List<ProductSummary> productSummaryList = new ArrayList<>();

                    ProductSummary productSummary = new ProductSummary();

                    List<OrderSummary> orderSummaries = new ArrayList<>();
                    OrderSummary orderSummary = new OrderSummary();
                    orderSummary.setDetailsSummary(failedBookingsSearchResponse.getDetailsSummary());
                    orderSummary.setSupplierId(failedBookingsSearchResponse.getSupplierId());
                    orderSummary.setTravelDate(failedBookingsSearchResponse.getTravelDate());

                    OrderDetailsResource orderDetailsResource = new OrderDetailsResource();
                    orderDetailsResource.setOrderId(failedBookingsSearchResponse.getOrderId());
                    orderDetailsResource.setOrderStatus(failedBookingsSearchResponse.getOrderStatus());

                    if(orderDetailsResource.getOrderSummary()!= null) {
                        orderDetailsResource.getOrderSummary().add(orderSummary);
                    }else{
                        orderSummaries.add(orderSummary);
                        orderDetailsResource.setOrderSummary(orderSummaries);
                    }

                    List<OrderDetailsResource> orderDetailsResources = new ArrayList<>();
                    orderDetailsResources.add(orderDetailsResource);

                    productSummary.setProductSubCategory(failedBookingsSearchResponse.getProductSubCategory());
                    productSummary.setOrders(orderDetailsResources);

                    if( failureDetailsResourceItem.getProductSummary() != null) {
                        failureDetailsResourceItem.getProductSummary().add(productSummary);
                    }else{
                        productSummaryList.add(productSummary);
                        failureDetailsResourceItem.setProductSummary(productSummaryList);

                    }

                }

            } else {
                FailureDetailsResource failureDetailsResourceItem = new FailureDetailsResource();
                failureDetailsResourceItem.setBookID(failedBookingsSearchResponse.getBookID());
                failureDetailsResourceItem.setClientType(failedBookingsSearchResponse.getClientType());
                //TODO : Set failure Flag
                failureDetailsResourceItem.setFailureFlag(failedBookingsSearchResponse.getBookingAttribute());
                failureDetailsResourceItem.setClientId(failedBookingsSearchResponse.getCompanyDetails());
                failureDetailsResourceItem.setClientType(failedBookingsSearchResponse.getClientType());
                failureDetailsResourceItem.setCompanyDetails(failedBookingsSearchResponse.getCompanyDetails());
                failureDetailsResourceItem.setBookingDate(failedBookingsSearchResponse.getBookingDate());
                failureDetailsResourceItem.setPointOfSale(failedBookingsSearchResponse.getPointOfSale());

                String duplicateBookingJSON = searchDuplicateBookings(failedBookingsSearchResponse.getBookID());
                JSONArray duplicateBookingsArray = new JSONObject(duplicateBookingJSON).getJSONArray("duplicateBookIDs");
                List<String> duplicateExists = new ArrayList<>();
                if(duplicateBookingsArray!=null && duplicateBookingsArray.length()>0) {
                    for (int i = 0; i < duplicateBookingsArray.length(); i++) {
                        duplicateExists.add(duplicateBookingsArray.getString(i));
                    }
                }

                failureDetailsResourceItem.setDuplicateExists(duplicateExists);
                List<ProductSummary> productSummaryList = new ArrayList<>();
                ProductSummary productSummary = new ProductSummary();

                List<OrderSummary> orderSummaries = new ArrayList<>();
                OrderSummary orderSummary = new OrderSummary();
                orderSummary.setDetailsSummary(failedBookingsSearchResponse.getDetailsSummary());
                orderSummary.setSupplierId(failedBookingsSearchResponse.getSupplierId());
                orderSummary.setTravelDate(failedBookingsSearchResponse.getTravelDate());

                orderSummaries.add(orderSummary);

                List<OrderDetailsResource> orderDetailsResources = new ArrayList<>();
                OrderDetailsResource orderDetailsResource = new OrderDetailsResource();
                orderDetailsResource.setOrderId(failedBookingsSearchResponse.getOrderId());
                orderDetailsResource.setOrderStatus(failedBookingsSearchResponse.getOrderStatus());

                orderDetailsResource.setOrderSummary(orderSummaries);

                orderDetailsResources.add(orderDetailsResource);
                productSummary.setOrders(orderDetailsResources);
                productSummary.setProductSubCategory(failedBookingsSearchResponse.getProductSubCategory());

                productSummaryList.add(productSummary);

                failureDetailsResourceItem.setProductSummary(productSummaryList);

                failureDetailsResourceList.add(failureDetailsResourceItem);
            }


        }

        return failureDetailsResourceList;
    }

}
