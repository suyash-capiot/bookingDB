package com.coxandkings.travel.bookingengine.db.repository.impl.searchimpl;

import com.coxandkings.travel.bookingengine.db.config.DBConfig;
import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.FlightArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.HotelArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.enums.ProductSubCategory;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.search.ArrivalListSearchRepository;
import com.coxandkings.travel.bookingengine.db.criteria.arrivallist.GeneralArrivalListSearchCriteria;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListFlightInfo;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListGeneralInfo;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListHotelInfo;
import com.coxandkings.travel.bookingengine.db.utils.SearchUtil;
import org.hibernate.Session;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.math.BigInteger;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Qualifier("ArrivalListSearchRepository")
@Repository
public class ArrivalListSearchRepositoryImpl extends SimpleJpaRepository<Booking, Serializable>
        implements ArrivalListSearchRepository {

    private EntityManager em;

    public ArrivalListSearchRepositoryImpl(EntityManager em) {
        super(Booking.class, em);
        this.em = em;
    }


    @Override
    public List<ArrivalListHotelInfo> searchHotelArrivalList(HotelArrivalListSearchCriteria hotelArrivalListSearchCriteria)
            throws SQLException, BookingEngineDBException {

        //the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);

        List<ArrivalListHotelInfo> arrivalListInfoListHotel = new ArrayList<>();

        String opsDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";
        SimpleDateFormat dateFormatter = new SimpleDateFormat(opsDateFormat);


        //Begin calling Stored Proc
        session.doWork((Connection connection) -> {

            try (final CallableStatement aCallableStatement = connection.prepareCall(
                    // Call Stored procedure - has 7 IN parameters, returns REFCursor as OUT param
                    "{ ? = call " + DBConfig.schemaName + ".searcharrivallisthotel( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) }")) {

                //First param to stored Proc for PostGres is always REFCursor
                aCallableStatement.registerOutParameter(1, Types.OTHER);
                aCallableStatement.setNull(2, Types.VARCHAR); //p_checkInDate;
                aCallableStatement.setNull(3, Types.VARCHAR); //p_bookingdatetime;
                aCallableStatement.setNull(4, Types.VARCHAR); //p_productsubcategory;
                aCallableStatement.setNull(5, Types.VARCHAR); //p_supplierid;
                aCallableStatement.setNull(6, Types.VARCHAR); //p_clienttype;
                aCallableStatement.setNull(7, Types.VARCHAR); //p_clientgroupid;
                aCallableStatement.setNull(8, Types.VARCHAR); //p_clientid;
                aCallableStatement.setNull(9, Types.VARCHAR); //p_continent;
                aCallableStatement.setNull(10, Types.VARCHAR);//p_country;
                aCallableStatement.setNull(11, Types.VARCHAR);//p_city;
                aCallableStatement.setNull(12, Types.VARCHAR);//p_productName;
                aCallableStatement.setNull(13, Types.VARCHAR);//p_chain;
                aCallableStatement.setNull(14, Types.VARCHAR);//p_isMsteryProduct;

                Boolean isCheckInDateTimeNull = false;
                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getCheckInDate()))) {
                    isCheckInDateTimeNull = true;
                    String checkInDate = hotelArrivalListSearchCriteria.getCheckInDate();
                    aCallableStatement.setObject(2, checkInDate, Types.VARCHAR); // p_checkInDate
                }

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getBookingDateTime()))) {
                    if(isCheckInDateTimeNull.equals(Boolean.TRUE)){
                        //TODO : throw exception here
                        try {
                            throw new BookingEngineDBException("Either Give Check In Date or Booking Date");
                        } catch (BookingEngineDBException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    String bookingDateTime = hotelArrivalListSearchCriteria.getBookingDateTime();
                    aCallableStatement.setObject(3, bookingDateTime, Types.VARCHAR); // p_checkOutDate
                }

                aCallableStatement.setObject(4, ProductSubCategory.HOTEL.getProductSubCategory(),
                        Types.VARCHAR);

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getSupplierId()))) {
                    String supplierId = hotelArrivalListSearchCriteria.getSupplierId();
                    aCallableStatement.setObject(5, supplierId, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getClientType()))) {
                    String clientType = hotelArrivalListSearchCriteria.getClientType();
                    aCallableStatement.setObject(6, clientType, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getClientGroupId()))) {
                    String clientGroupId = hotelArrivalListSearchCriteria.getClientGroupId();
                    aCallableStatement.setObject(7, clientGroupId, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getClientId()))) {
                    String clientId = hotelArrivalListSearchCriteria.getClientId();
                    aCallableStatement.setObject(8, clientId, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getContinent()))) {
                    String continent = hotelArrivalListSearchCriteria.getContinent();
                    aCallableStatement.setObject(9, continent, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getCountry()))) {
                    String country = hotelArrivalListSearchCriteria.getCountry();
                    aCallableStatement.setObject(10, country, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getCity()))) {
                    String city = hotelArrivalListSearchCriteria.getCity();
                    aCallableStatement.setObject(11, city, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getProductName()))) {
                    String productName = hotelArrivalListSearchCriteria.getProductName();
                    aCallableStatement.setObject(12, productName, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getChain()))) {
                    String chain = hotelArrivalListSearchCriteria.getChain();
                    aCallableStatement.setObject(13, chain, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(hotelArrivalListSearchCriteria.getIsMysteryProduct()))) {
                    String isMysteryProduct = hotelArrivalListSearchCriteria.getIsMysteryProduct();
                    aCallableStatement.setObject(14, isMysteryProduct, Types.VARCHAR);
                }

                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);

                JSONObject resJson = new JSONObject();
                while (results.next()) {

                    ArrivalListHotelInfo arrivalListHotelInfo = new ArrivalListHotelInfo();
                    String bookID = results.getString("bookid");
                    String supplierReferenceNumber = results.getString("supplierreferenceid");
                    String productCategory = results.getString("productcategory");
                    String productSubCategory = results.getString("productsubcategory");
                    String supplierId = results.getString("supplierid");
                    String checkInDate = results.getString("checkInDate");
                    String checkOutDate = results.getString("checkOutDate");
                    String roomCategory = results.getString("roomCategory");
                    String roomType = results.getString("roomType");
                    String paxCount = results.getString("paxcount");
                    String firstName = results.getString("firstName");
                    String lastName = results.getString("lastName");
                    String paxType = results.getString("paxtype");
                    String totalRoomsInOrder = results.getString("totalRoomsInOrder");

                    arrivalListHotelInfo.setBookID(bookID);
                    arrivalListHotelInfo.setSupplierReferenceNumber(supplierReferenceNumber);
                    arrivalListHotelInfo.setProductCategory(productCategory);
                    arrivalListHotelInfo.setProductSubCategory(productSubCategory);
                    arrivalListHotelInfo.setSupplierId(supplierId);
                    arrivalListHotelInfo.setCheckInDate(checkInDate);
                    arrivalListHotelInfo.setCheckOutDate(checkOutDate);
                    arrivalListHotelInfo.setRoomCategory(roomCategory);
                    arrivalListHotelInfo.setRoomType(roomType);
                    arrivalListHotelInfo.setPaxCount(paxCount);
                    arrivalListHotelInfo.setFirstName(firstName);
                    arrivalListHotelInfo.setLastName(lastName);
                    arrivalListHotelInfo.setPaxType(paxType);
                    arrivalListHotelInfo.setTotalRoomsInOrder(totalRoomsInOrder);

                    arrivalListInfoListHotel.add(arrivalListHotelInfo);
                }
                results.close();
                aCallableStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                // TODO How to close statement here? We have no reference of Statement object
                // Can Spring Aspect help here??
            } finally {
            }
        });

        return arrivalListInfoListHotel;
    }

    @Override
    public List<ArrivalListFlightInfo> searchFlightArrivalList(FlightArrivalListSearchCriteria
                                                                       flightArrivalListSearchCriteria) throws SQLException {

        //the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);

        List<ArrivalListFlightInfo> arrivalListFlightInfoList = new ArrayList<>();

        String opsDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";
        SimpleDateFormat dateFormatter = new SimpleDateFormat(opsDateFormat);


        session.doWork((Connection connection) -> {

            try (final CallableStatement aCallableStatement = connection.prepareCall(
                    // Call Stored procedure - has 7 IN parameters, returns REFCursor as OUT param
                    "{ ? = call " + DBConfig.schemaName + ".searcharrivallistflight( ?, ?, ?, ?, ?, ?, ?, ?, ?) }")) {



                aCallableStatement.registerOutParameter(1, Types.OTHER);
                aCallableStatement.setNull(2, Types.VARCHAR); //p_traveldatetime;
                aCallableStatement.setNull(3, Types.VARCHAR); //p_BookingDateTime;
                aCallableStatement.setNull(4, Types.VARCHAR); //p_supplierid;
                aCallableStatement.setNull(5, Types.VARCHAR); //p_clienttype;
                aCallableStatement.setNull(6, Types.VARCHAR); //p_clientgroupid;
                aCallableStatement.setNull(7, Types.VARCHAR); //p_clientid;
                aCallableStatement.setNull(8, Types.VARCHAR); //p_fromcity;
                aCallableStatement.setNull(9, Types.VARCHAR); //p_tocity;
                aCallableStatement.setNull(10, Types.VARCHAR);//p_journ qeytype;

                Boolean istravelDateTimeNull = false;
                if (!(StringUtils.isEmpty(flightArrivalListSearchCriteria.getTravelDateTime()))) {
                    istravelDateTimeNull = true;
                    String travelDateTime = flightArrivalListSearchCriteria.getTravelDateTime();
                    aCallableStatement.setObject(2, travelDateTime, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightArrivalListSearchCriteria.getBookingDateTime()))) {
                    if(istravelDateTimeNull.equals(Boolean.TRUE)){
                        try {
                            throw new BookingEngineDBException("Either Give Check In Date or Booking Date");
                        } catch (BookingEngineDBException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    String bookingDateTime = flightArrivalListSearchCriteria.getBookingDateTime();
                    aCallableStatement.setObject(3, bookingDateTime, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightArrivalListSearchCriteria.getSupplierId()))) {
                    String productSubCategory = flightArrivalListSearchCriteria.getSupplierId();
                    aCallableStatement.setObject(4, productSubCategory, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightArrivalListSearchCriteria.getClientType()))) {
                    String supplierId = flightArrivalListSearchCriteria.getClientType();
                    aCallableStatement.setObject(5, supplierId, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightArrivalListSearchCriteria.getClientGroupId()))) {
                    String clientGroupId = flightArrivalListSearchCriteria.getClientGroupId();
                    aCallableStatement.setObject(6, clientGroupId, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightArrivalListSearchCriteria.getClientId()))) {
                    String clientId = flightArrivalListSearchCriteria.getClientId();
                    aCallableStatement.setObject(7, clientId, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightArrivalListSearchCriteria.getFromCity()))) {
                    String fromCity = flightArrivalListSearchCriteria.getFromCity();
                    aCallableStatement.setObject(8, fromCity, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightArrivalListSearchCriteria.getToCity()))) {
                    String toCity = flightArrivalListSearchCriteria.getToCity();
                    aCallableStatement.setObject(9, toCity, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightArrivalListSearchCriteria.getJourneyType()))) {
                    String journeyType = flightArrivalListSearchCriteria.getJourneyType();
                    aCallableStatement.setObject(10, journeyType, Types.VARCHAR);
                }

                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);

                JSONObject resJson = new JSONObject();
                while (results.next()) {
                    ArrivalListFlightInfo arrivalListFlightInfo = new ArrivalListFlightInfo();

                    String bookID = results.getString("bookid");
                    String supplierReferenceNumber = results.getString("supplier_reference_number");//is AirlinePnr
                    String productCategory = results.getString("productcategory");
                    String productSubCategory = results.getString("productsubcategory");
                    String clientType = results.getString("clienttype");
                    String groupNameId = results.getString("groupnameid");
                    String supplierId = results.getString("supplierid");
                    String flightNumber = results.getString("flightnumber");
                    String airLineCode = results.getString("airlinecode");
                    String originLocation = results.getString("originLocation");
                    String destinationLocation = results.getString("destinationLocation");
                    String firstName = results.getString("firstName");
                    String lastName = results.getString("lastName");
                    String paxType =  results.getString("paxtype");
                    String cabinType = results.getString("cabinType");
                    String rph = results.getString("rph");
                    String paxCount = results.getString("paxcount");
                    String ticketingPcc = results.getString("ticketingpcc");

                    arrivalListFlightInfo.setBookID(bookID);
                    arrivalListFlightInfo.setSupplierReferenceNumber(supplierReferenceNumber);
                    arrivalListFlightInfo.setProductCategory(productCategory);
                    arrivalListFlightInfo.setProductSubCategory(productSubCategory);
                    arrivalListFlightInfo.setClientType(clientType);
                    arrivalListFlightInfo.setGroupNameId(groupNameId);
                    arrivalListFlightInfo.setSupplierId(supplierId);
                    arrivalListFlightInfo.setFlightNumber(flightNumber);
                    arrivalListFlightInfo.setAirLineCode(airLineCode);
                    arrivalListFlightInfo.setOriginLocation(originLocation);
                    arrivalListFlightInfo.setDestinationLocation(destinationLocation);
                    arrivalListFlightInfo.setFirstName(firstName);
                    arrivalListFlightInfo.setLastName(lastName);
                    arrivalListFlightInfo.setPaxType(paxType);
                    arrivalListFlightInfo.setClientType(cabinType);
                    arrivalListFlightInfo.setRph(rph);
                    arrivalListFlightInfo.setPaxCount(paxCount);
                    arrivalListFlightInfo.setTicketingPcc(ticketingPcc);

                    arrivalListFlightInfoList.add(arrivalListFlightInfo);
                }
                results.close();
                aCallableStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                // TODO How to close statement here? We have no reference of Statement object
                // Can Spring Aspect help here??
            } finally {
            }
        });
        return arrivalListFlightInfoList;
    }

    @Override
    public List<ArrivalListGeneralInfo> searchGeneralArrivalList(GeneralArrivalListSearchCriteria
                                                             generalArrivalListSearchCriteria) throws SQLException {

        //the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);

        List<ArrivalListGeneralInfo> arrivalListGeneralInfoList = new ArrayList<>();

        String opsDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";
        SimpleDateFormat dateFormatter = new SimpleDateFormat(opsDateFormat);


        session.doWork((Connection connection) -> {
            try (final CallableStatement aCallableStatement = connection.prepareCall(
                    // Call Stored procedure - has 7 IN parameters, returns REFCursor as OUT param
                    "{ ? = call " + DBConfig.schemaName + ".searcharrivallistgeneral( ?, ?, ?, ?, ?, ?, ?) }")) {

                //First param to stored Proc for PostGres is always REFCursor
                aCallableStatement.registerOutParameter(1, Types.OTHER);
                aCallableStatement.setNull(2, Types.VARCHAR); //p_traveldatetime;
                aCallableStatement.setNull(3, Types.VARCHAR); //p_BookingDateTime;
                aCallableStatement.setNull(4, Types.VARCHAR); //p_productsubcategory;
                aCallableStatement.setNull(5, Types.VARCHAR); //p_supplierid;
                aCallableStatement.setNull(6, Types.VARCHAR); //p_clienttype;
                aCallableStatement.setNull(7, Types.VARCHAR); //p_clientgroupid;
                aCallableStatement.setNull(8, Types.VARCHAR); //p_clientid;

                if (!(StringUtils.isEmpty(generalArrivalListSearchCriteria.getTravelDateTime()))) {
                    String travelDateTime = generalArrivalListSearchCriteria.getTravelDateTime();
                    aCallableStatement.setObject(2, travelDateTime, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(generalArrivalListSearchCriteria.getBookingDateTime()))) {
                    String bookingDateTime = generalArrivalListSearchCriteria.getBookingDateTime();
                    aCallableStatement.setObject(3, bookingDateTime, Types.VARCHAR);
                }


                if (!(StringUtils.isEmpty(generalArrivalListSearchCriteria.getProductSubCategory()))) {
                    String productSubCategory = generalArrivalListSearchCriteria.getProductSubCategory();
                    aCallableStatement.setObject(4, productSubCategory, Types.VARCHAR);
                }



                if (!(StringUtils.isEmpty(generalArrivalListSearchCriteria.getSupplierId()))) {
                    String productSubCategory = generalArrivalListSearchCriteria.getSupplierId();
                    aCallableStatement.setObject(5, productSubCategory, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(generalArrivalListSearchCriteria.getClientType()))) {
                    String supplierId = generalArrivalListSearchCriteria.getClientType();
                    aCallableStatement.setObject(6, supplierId, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(generalArrivalListSearchCriteria.getClientGroupId()))) {
                    String clientGroupId = generalArrivalListSearchCriteria.getClientGroupId();
                    aCallableStatement.setObject(7, clientGroupId, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(generalArrivalListSearchCriteria.getClientId()))) {
                    String clientId = generalArrivalListSearchCriteria.getClientId();
                    aCallableStatement.setObject(8, clientId, Types.VARCHAR);
                }

                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);

                JSONObject resJson = new JSONObject();
                while (results.next()) {
                    ArrivalListGeneralInfo arrivalListGeneralInfo = new ArrivalListGeneralInfo();

//                    TBL_BOOKING.bookid as bookid, TBL_AIRORDERS.airlinepnr as supplier_reference_number, 'Transportation' as productcategory,
//                    TBL_AIRORDERS.productsubcategory as productsubcategory, TBL_BOOKING.clienttype as clienttype,
//                    TBL_BOOKING.groupnameid as groupnameid, TBL_AIRORDERS.supplierid as supplierid
//

                    String bookID = results.getString("bookid");
                    String supplierReferenceNumber = results.getString("supplier_reference_number");
                    String productCategory = results.getString("productcategory");
                    String productSubCategory = results.getString("productsubcategory");
                    String clientType = results.getString("clienttype");
                    String groupNameId = results.getString("groupnameid");
                    String supplierId = results.getString("supplierid");
                    String clientId = results.getString("clientId");

                    arrivalListGeneralInfo.setBookID(bookID);
                    arrivalListGeneralInfo.setSupplierReferenceNumber(supplierReferenceNumber);
                    arrivalListGeneralInfo.setProductCategory(productCategory);
                    arrivalListGeneralInfo.setProductSubCategory(productSubCategory);
                    arrivalListGeneralInfo.setClientType(clientType);
                    arrivalListGeneralInfo.setGroupNameId(groupNameId);
                    arrivalListGeneralInfo.setSupplierId(supplierId);
                    arrivalListGeneralInfo.setClientId(clientId);

                    arrivalListGeneralInfoList.add(arrivalListGeneralInfo);
                }
                results.close();
                aCallableStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                // TODO How to close statement here? We have no reference of Statement object
                // Can Spring Aspect help here??
            } finally {
            }
        });

        return arrivalListGeneralInfoList;
    }
}


