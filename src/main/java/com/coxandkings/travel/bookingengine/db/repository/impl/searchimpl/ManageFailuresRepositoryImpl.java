package com.coxandkings.travel.bookingengine.db.repository.impl.searchimpl;

import com.coxandkings.travel.bookingengine.db.config.DBConfig;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.BookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.FailureBookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.ProductDetailsFilter;
import com.coxandkings.travel.bookingengine.db.enums.BookingAttribute;
import com.coxandkings.travel.bookingengine.db.enums.ProductCategory;
import com.coxandkings.travel.bookingengine.db.enums.ProductSubCategory;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.search.ManageFailuresRepository;
import com.coxandkings.travel.bookingengine.db.resource.managefailures.*;
import com.coxandkings.travel.bookingengine.db.utils.SearchUtil;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Qualifier("ManageFailuresRepository")
@Repository
public class ManageFailuresRepositoryImpl extends SimpleJpaRepository<Booking, Serializable>
        implements ManageFailuresRepository {


    private EntityManager em;

    public ManageFailuresRepositoryImpl(EntityManager em) {
        super(Booking.class, em);
        this.em = em;
    }


    @Override
    public String searchDuplicateBookings(List<String> searchCriteria, String bookId)
            throws SQLException {

        //the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);

        HashMap<String, Integer> bookIdCount = new HashMap<>();
        List<String> duplicateBookIds = new ArrayList<>();


        JSONArray duplicateBookings = new JSONArray();
        //Begin calling Stored Proc
        session.doWork((Connection connection) -> {

            PreparedStatement pstmt = connection.prepareStatement
                    ("select * from " + DBConfig.schemaName +
                            ".SearchDuplicateBookings(?, ?::" + DBConfig.schemaName +
                            ".duplicateFlightSearchCriteria[], ?::" + DBConfig.schemaName
                            + ".duplicateaccosearchcriteria[])");

            pstmt.setNull(1, Types.VARCHAR);
            pstmt.setString(2, searchCriteria.get(0)); // 0 -> Air
            pstmt.setString(3, searchCriteria.get(1));  // 1 -> Hotel

            ResultSet results = pstmt.executeQuery();
            while (results.next()) {
                String output = results.getString(1);
                String matchBookId = output.trim();
                Integer count = bookIdCount.get(matchBookId);
                bookIdCount.put(matchBookId, (count == null) ? 1 : count + 1);
            }
        });

        for (Map.Entry<String, Integer> e : bookIdCount.entrySet()) {
            String key = e.getKey();
            Integer value = e.getValue();
            if (!(bookId.equalsIgnoreCase(key)) && bookIdCount.get(bookId) == value) {
                duplicateBookings.put(key);
            }
        }

        JSONObject bookings = new JSONObject().put("duplicateBookIDs", duplicateBookings);

        return bookings.toString();
    }


    @Override
    public List<FailedBookingsSearchResponse> searchFailedBookings(FailureBookingSearchCriteria aBookingSearchCriteria) {
        // the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);
        SearchUtil filterUtil = new SearchUtil();

        List<FailedBookingsSearchResponse> failedBookingsSearchResponseList = new ArrayList<>();

        String opsDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";
        SimpleDateFormat dateFormatter = new SimpleDateFormat(opsDateFormat);

        // Begin calling Stored Proc
        session.doWork((Connection connection) -> {

            try (
                    final CallableStatement aCallableStatement = connection.prepareCall(
                            // Call Stored procedure - has 7 IN parameters, returns REFCursor as OUT param
                            "{ ? = call " + DBConfig.schemaName + ".SearchFailedBookings( ?, ?, ?, ?, ?, ?, ?, ?, ? ) }")) {

                // First param to stored Proc for PostGres is always REFCursor
                aCallableStatement.registerOutParameter(1, Types.OTHER);

                // Prepare filter for BookingDetailsFilter
                PGobject bookingDetailsInParam = new PGobject();
                bookingDetailsInParam.setType("BookingSearchCriteria");
                bookingDetailsInParam.setValue(filterUtil.getBookingDetailsFilter(aBookingSearchCriteria.getBookingBasedFilter()));

                // Prepare filter for CompanyDetailsFilter
                PGobject companyDetailsInParam = new PGobject();
                companyDetailsInParam.setType("CompaniesSearchCriteria");
                companyDetailsInParam.setValue(filterUtil.getCompaniesSearchingCriteria(aBookingSearchCriteria.getCompanyBasedFilter()));

                // Prepare filter for Client Pax details
                PGobject clientPaxInParam = new PGobject();
                clientPaxInParam.setType("ClientPaxSearchCriteria");
                clientPaxInParam.setValue(filterUtil.getClientPaxSearchingCriteria(aBookingSearchCriteria.getClientPxBasedFilter()));


                aCallableStatement.setNull(2, Types.INTEGER); // start index
                aCallableStatement.setNull(3, Types.INTEGER); // end index
                aCallableStatement.setNull(4, Types.VARCHAR); // sort field
                aCallableStatement.setNull(5, Types.VARCHAR); // p_failureFlag
                aCallableStatement.setObject(6, bookingDetailsInParam, Types.OTHER); // BookingDetailsFilter IN param
                aCallableStatement.setObject(7, companyDetailsInParam, Types.OTHER); // CompanyDetailsFilter IN param
                aCallableStatement.setObject(8, clientPaxInParam, Types.OTHER);  // ClientPaxFilter IN param
                aCallableStatement.setNull(9, Types.OTHER); // Set null initially for Flight, override in switch block below
                aCallableStatement.setNull(10, Types.OTHER); // Set null initially for Flight, override in switch block below

                if(!(StringUtils.isEmpty(aBookingSearchCriteria.getFailureFlag()))) {
                    String failureFlag =  aBookingSearchCriteria.getFailureFlag();
                    if(failureFlag.equalsIgnoreCase(BookingAttribute.BF.getBookingAttribute())) {
                        aCallableStatement.setObject(5,  "{\"BF\":\"Failure Booking\"}" , Types.VARCHAR); // p_failureFlag
                    }
                }

                ProductDetailsFilter prodDetails = aBookingSearchCriteria.getProductBasedFilter();

                if( prodDetails != null && !(StringUtils.isEmpty(prodDetails.getProductCategoryId())) &&
                        StringUtils.isEmpty(prodDetails.getProductCategorySubTypeId())) {

                    ProductCategory aProdCategory = ProductCategory.getProductCategory( prodDetails.getProductCategoryId() );
                    switch ( aProdCategory )    {
                        case PRODUCT_CATEGORY_TRANSPORTATION:   {
                            // Prepare filter for Flight details filter
                            aBookingSearchCriteria.getProductBasedFilter().setProductCategorySubTypeId(ProductSubCategory.AIR.getProductSubCategory());
                            PGobject flightDetailsInParam = new PGobject();
                            flightDetailsInParam.setType("FlightSearchCriteria");
                            flightDetailsInParam.setValue(filterUtil.getFlightSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(9, flightDetailsInParam, Types.OTHER); // FlightSearch filter IN param
                            // Add for Cars, Bus, etc. in future
                        }
                        break;

                        case PRODUCT_CATEGORY_ACCOMMODATION:    {
                            // Prepare filter for Hotel details filter
                            aBookingSearchCriteria.getProductBasedFilter().setProductCategorySubTypeId(ProductSubCategory.HOTEL.getProductSubCategory());
                            PGobject hotelDetailsInParam = new PGobject();
                            hotelDetailsInParam.setType("HotelSearchCriteria");
                            hotelDetailsInParam.setValue(filterUtil.getHotelSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(10, hotelDetailsInParam, Types.OTHER); // Hotel Search Filter IN param
                        }
                        break;
                    }
                }

                if (prodDetails != null && !(StringUtils.isEmpty(prodDetails.getProductCategorySubTypeId()) )) {
                    ProductSubCategory aProdSubCategory = ProductSubCategory.fromString(prodDetails.getProductCategorySubTypeId());
                    switch (aProdSubCategory) {
                        case AIR: {
                            // Prepare filter for Flight details filter
                            PGobject flightDetailsInParam = new PGobject();
                            flightDetailsInParam.setType("FlightSearchCriteria");
                            flightDetailsInParam.setValue(filterUtil.getFlightSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(9, flightDetailsInParam, Types.OTHER); // FlightSearch filter IN param
                        }
                        break;

                        case HOTEL: {
                            // Prepare filter for Hotel details filter
                            PGobject hotelDetailsInParam = new PGobject();
                            hotelDetailsInParam.setType("HotelSearchCriteria");
                            hotelDetailsInParam.setValue(filterUtil.getHotelSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(10, hotelDetailsInParam, Types.OTHER); // Hotel Search Filter IN param
                        }
                        break;
                    }
                }

                if( (StringUtils.isEmpty(prodDetails.getProductCategorySubTypeId())) &&
                        (StringUtils.isEmpty(prodDetails.getProductCategorySubTypeId())) )  {
                    // Prepare filter for Flight details filter
                    PGobject flightDetailsInParam = new PGobject();
                    flightDetailsInParam.setType("FlightSearchCriteria");
                    flightDetailsInParam.setValue(filterUtil.getFlightSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                    aCallableStatement.setObject(9, flightDetailsInParam, Types.OTHER); // FlightSearch filter IN param

                    PGobject hotelDetailsInParam = new PGobject();
                    hotelDetailsInParam.setType("HotelSearchCriteria");
                    hotelDetailsInParam.setValue(filterUtil.getHotelSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                    aCallableStatement.setObject(10, hotelDetailsInParam, Types.OTHER); // Hotel Search Filter IN param
                }

                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);
                while (results.next()) {

                    String bookID = results.getString("bookid");
                    java.sql.Timestamp bookingDate = results.getTimestamp("createdat");
                    String clientID = results.getString("clientid"); //get ClientName from it from client ID
                    String clientType = results.getString("clienttype");
                    String pointOfSale = results.getString("pointofsale");
                    String companyDetails = results.getString("companyid"); // Company mapped to companyID
                    String fileHandlerId = results.getString("staffid"); // staffid mapped to fileHandlerId
                    JSONObject bookingAttribute = null;
                    if(results.getString("bookingAttribute") != null) {
                        bookingAttribute = new JSONObject(results.getString("bookingAttribute"));
                    }

                    String productSubCategory = results.getString("productsubcategory");
                    String orderStatus = results.getString("orderStatus");
                    String orderId = results.getString("orderId");
                    String travelDate = results.getString("travelDate");
                    String detailsSummary = results.getString("detailsSummary");
                    String supplierId = results.getString("supplierid");

                    FailedBookingsSearchResponse failedBookingsSearchResponse = new FailedBookingsSearchResponse();
                    failedBookingsSearchResponse.setBookID(bookID);
                    failedBookingsSearchResponse.setBookingDate(dateFormatter.format(bookingDate));
                    failedBookingsSearchResponse.setClientID(clientID);
                    failedBookingsSearchResponse.setClientType(clientType);
                    failedBookingsSearchResponse.setPointOfSale(pointOfSale);
                    failedBookingsSearchResponse.setCompanyDetails(companyDetails);
                    failedBookingsSearchResponse.setFileHandlerId(fileHandlerId);

                    if( bookingAttribute != null && !(bookingAttribute.isNull("BF") )) {
                        failedBookingsSearchResponse.setBookingAttribute(bookingAttribute.getString("BF"));
                    }

                    failedBookingsSearchResponse.setProductSubCategory(productSubCategory);
                    failedBookingsSearchResponse.setOrderStatus(orderStatus);
                    failedBookingsSearchResponse.setOrderId(orderId);
                    failedBookingsSearchResponse.setTravelDate(travelDate);
                    failedBookingsSearchResponse.setDetailsSummary(detailsSummary);
                    failedBookingsSearchResponse.setSupplierId(supplierId);

                    failedBookingsSearchResponseList.add(failedBookingsSearchResponse);

                }
                results.close();
                aCallableStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                //TODO How to close statement here? We have no reference of Statement object
                // Can Spring Aspect help here??
            } finally {
            }
        });
        return failedBookingsSearchResponseList;
    }

}
