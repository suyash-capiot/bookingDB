package com.coxandkings.travel.bookingengine.db.controller;

import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.FlightArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.GeneralArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.HotelArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.BookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.FailureBookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.productsharing.ProductSharingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.manageproductupdates.FlightUpdatesSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.mergebooking.MergeBookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.orchestrator.SearchBookingsServiceImpl;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListFlightInfo;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListGeneralInfo;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListHotelInfo;
import com.coxandkings.travel.bookingengine.db.resource.managecheaperprices.CheaperPriceBookingInfo;
import com.coxandkings.travel.bookingengine.db.resource.managefailures.FailureDetailsResource;
import com.coxandkings.travel.bookingengine.db.resource.manageproductupdates.ProductUpdateFlightInfo;
import com.coxandkings.travel.bookingengine.db.resource.manageproductupdates.ProductUpdateFlightResponse;
import com.coxandkings.travel.bookingengine.db.resource.productreview.ProductReviewInfo;
import com.coxandkings.travel.bookingengine.db.resource.productsharing.ProductSharingInfo;
import com.coxandkings.travel.bookingengine.db.resource.searchviewfilter.BookingSearchResponseItem;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.orchestrator.BookingServiceImpl;
import com.coxandkings.travel.bookingengine.db.orchestrator.Constants;

@RestController
@RequestMapping("/BookingService")
public class BookingController implements Constants {

    @Autowired
    private BookingServiceImpl service;

    @Autowired
    private SearchBookingsServiceImpl searchBookingsService;

    @GetMapping(value = "/getBooking/{bookID}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getBookingByBookID(@PathVariable("bookID") String bookID) {
        String res = service.getByBookID(bookID, "false");

        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @GetMapping(value = "/getBookingID/{bookID}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getNewBookingByBookingID(@PathVariable("bookID") String bookID) {
        String res = service.getByBookID(bookID, "true");

        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @GetMapping(value = "/getAmendments/{bookID}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAmendMentsByBookID(@PathVariable("bookID") String bookID) {
        String res = service.getAmendmentsByBookID(bookID);

        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @GetMapping(value = "/getCancellations/{bookID}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getCancellationsByBookID(@PathVariable("bookID") String bookID) {
        String res = service.getCancellationsByBookID(bookID);

        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @PostMapping(value = "/getBookingsByUserID/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getBookingByUserID(InputStream req) {

        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = service.getByUserID(reqJson.getJSONObject(JSON_PROP_REQHEADER).getString(JSON_PROP_USERID));
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @GetMapping(value = "/getBookings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getBookingByStatus(@RequestParam String status) {
        String res = service.getByStatus(status);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @PutMapping(value = "/update/{updateType}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateDetails(InputStream req, @PathVariable("updateType") String updateType)
            throws BookingEngineDBException {

        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = service.updateOrder(reqJson, updateType);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @PostMapping(value = "/getBetweenRange", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getOrdersBySuppIDAndRange(InputStream req) throws BookingEngineDBException {
        {
            JSONTokener jsonTok = new JSONTokener(req);
            JSONObject reqJson = new JSONObject(jsonTok);
            String res = service.getOrdersInRange(reqJson);
            return new ResponseEntity<String>(res, HttpStatus.OK);
        }
    }


    @PostMapping(value = "/getDocumentForOrder", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDocumentDetails(InputStream req) throws BookingEngineDBException {
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = service.getDocumentDetails(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @PostMapping(value = "/acquireLock", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> acquireLock(InputStream req) throws BookingEngineDBException {
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = service.acquireLock(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);

    }

    @GetMapping(value = "/getPolicies/{bookID}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPolicies(@PathVariable("bookID") String bookID) {
        String res = service.getPolicies(bookID);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @PostMapping(value = "/releaseLock", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> releaseLock(InputStream req) throws BookingEngineDBException {
        JSONTokener jsonTok = new JSONTokener(req);
        JSONObject reqJson = new JSONObject(jsonTok);
        String res = service.releaseLock(reqJson);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @PostMapping(value = "/getBookingsByCriteria", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> findBySearchCriteria(@RequestBody BookingSearchCriteria bookingCriteria) {
        String searchResult = searchBookingsService.findBySearchCriteria(bookingCriteria);
        return new ResponseEntity<String>(searchResult, HttpStatus.OK);
    }

    @PostMapping(value = "/searchBookings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<BookingSearchResponseItem>> searchCriteria(@RequestBody BookingSearchCriteria
                                                                                  bookingCriteria) {
        List<BookingSearchResponseItem> bookingSearchResponseItemList = searchBookingsService.searchBookings(bookingCriteria);
        return new ResponseEntity<List<BookingSearchResponseItem>>(bookingSearchResponseItemList, HttpStatus.OK);
    }

    @GetMapping(value = "/searchCheaperPriceBookings/{productSubCategory}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<? extends CheaperPriceBookingInfo>> searchCheaperPriceBookings(@PathVariable("productSubCategory") String productSubCategory) {
        List<? extends CheaperPriceBookingInfo> cheaperPriceBookingInfoList = searchBookingsService.searchCheaperPriceBookings(productSubCategory);
        return new ResponseEntity<List<? extends CheaperPriceBookingInfo>>(cheaperPriceBookingInfoList, HttpStatus.OK);
    }

    @PostMapping(value = "/searchProductSharingBookings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProductSharingInfo>> searchProductSharingBookings(@RequestBody ProductSharingSearchCriteria productSharingSearchCriteria) {
        List<ProductSharingInfo> productSharingInfoList = searchBookingsService.searchProductSharingBookings(productSharingSearchCriteria);
        return new ResponseEntity<List<ProductSharingInfo>>(productSharingInfoList, HttpStatus.OK);
    }


    @PostMapping(value = "/searchArrivalListHotel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ArrivalListHotelInfo>> searchArrivalListHotel
            (@RequestBody HotelArrivalListSearchCriteria arrivalListSearchCriteria) throws BookingEngineDBException {
        List<ArrivalListHotelInfo> arrivalListInfoList = searchBookingsService.searchArrivalListHotel(arrivalListSearchCriteria);
        return new ResponseEntity<List<ArrivalListHotelInfo>>(arrivalListInfoList, HttpStatus.OK);
    }

    @PostMapping(value = "/searchArrivalListFlight", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ArrivalListFlightInfo>> searchArrivalListFlight
            (@RequestBody FlightArrivalListSearchCriteria arrivalListSearchCriteria) throws BookingEngineDBException {
        List<ArrivalListFlightInfo> arrivalListInfoList = searchBookingsService.searchArrivalListFlight(arrivalListSearchCriteria);
        return new ResponseEntity<List<ArrivalListFlightInfo>>(arrivalListInfoList, HttpStatus.OK);
    }


    @PostMapping(value = "/searchGeneralArrivalList", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ArrivalListGeneralInfo>> searchGeneralArrivalList(@RequestBody GeneralArrivalListSearchCriteria
                                                                                         generalArrivalListSearchCriteria) {
        List<ArrivalListGeneralInfo> arrivalListInfoList = searchBookingsService.searchGeneralArrivalList(generalArrivalListSearchCriteria);
        return new ResponseEntity<List<ArrivalListGeneralInfo>>(arrivalListInfoList, HttpStatus.OK);
    }


    @PostMapping(value = "/searchMergeBookings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchGeneralArrivalList(@RequestBody MergeBookingSearchCriteria
                                                                   mergeBookingSearchCriteria) {
        String mergeBookingInfo = searchBookingsService.searchForMergeBookings(mergeBookingSearchCriteria);
        return new ResponseEntity<String>(mergeBookingInfo, HttpStatus.OK);
    }
    
    @GetMapping(value = "/getMergeBookings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getMergeBookings() {
        String mergeBookingInfo = searchBookingsService.getMergeBookings();
        return new ResponseEntity<String>(mergeBookingInfo, HttpStatus.OK);
    }

    @PostMapping(value = "/searchFlightsForProductUpdates", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductUpdateFlightResponse> searchGeneralArrivalList(@RequestBody FlightUpdatesSearchCriteria
                                                                                          flightUpdatesSearchCriteria) {
        ProductUpdateFlightResponse aProductUpdateFlightResponse = searchBookingsService.searchFlightsForProductUpdates(flightUpdatesSearchCriteria);
        return new ResponseEntity<ProductUpdateFlightResponse>(aProductUpdateFlightResponse, HttpStatus.OK);
    }

    @GetMapping(value = "/searchProductsToReview", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProductReviewInfo>> searchGeneralArrivalList(@RequestParam String productEndDate) {
        List<ProductReviewInfo> productReviewInfo = searchBookingsService.searchFlightsForProductUpdates(productEndDate);
        return new ResponseEntity<List<ProductReviewInfo>>(productReviewInfo, HttpStatus.OK);
    }


    @GetMapping(value = "/searchDuplicateBookings/{bookId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> searchDuplicateBookings(@PathVariable String bookId) {
        String duplicateBookings = searchBookingsService.searchDuplicateBookings(bookId);
        return new ResponseEntity<String>(duplicateBookings, HttpStatus.OK);
    }


    @PostMapping(value = "/searchFailedBookings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FailureDetailsResource>> searchFailedBookings(@RequestBody
                                                     FailureBookingSearchCriteria failureBookingSearchCriteria) {
        List<FailureDetailsResource> failedBookings = searchBookingsService.searchFailedBookings(failureBookingSearchCriteria);
        return new ResponseEntity<List<FailureDetailsResource>>(failedBookings, HttpStatus.OK);
    }

    @GetMapping(value = "/getDocumentForBooking/{bookID}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDocumentForBooking(@PathVariable String bookID) throws BookingEngineDBException {
        String res = service.getDocumentForBooking(bookID);
        return new ResponseEntity<String>(res, HttpStatus.OK);
    }

    @GetMapping(value = "/getBookingDocuments/{bookID}")
    public ResponseEntity<String> getBookingDocuments(@PathVariable("bookID") String bookID){
        return  new ResponseEntity<>(service.getBookingDocuments(bookID),HttpStatus.OK);


    }
}
