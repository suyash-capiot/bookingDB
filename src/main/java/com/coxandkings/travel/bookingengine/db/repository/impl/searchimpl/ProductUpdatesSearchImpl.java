package com.coxandkings.travel.bookingengine.db.repository.impl.searchimpl;

import com.coxandkings.travel.bookingengine.db.config.DBConfig;
import com.coxandkings.travel.bookingengine.db.criteria.manageproductupdates.FlightUpdatesSearchCriteria;
import com.coxandkings.travel.bookingengine.db.enums.ProductSubCategory;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.search.ProductUpdatesSearchRepository;
import com.coxandkings.travel.bookingengine.db.resource.arrivallist.ArrivalListFlightInfo;
import com.coxandkings.travel.bookingengine.db.resource.managecheaperprices.CheaperPriceBookingInfo;
import com.coxandkings.travel.bookingengine.db.resource.managecheaperprices.CheaperPriceUpdatesFlightInfo;
import com.coxandkings.travel.bookingengine.db.resource.managecheaperprices.CheaperPriceUpdateFlightSegmentInfo;
import com.coxandkings.travel.bookingengine.db.resource.managecheaperprices.CheaperPriceUpdatesHotelInfo;
import com.coxandkings.travel.bookingengine.db.resource.manageproductupdates.ProductUpdateFlightInfo;
import com.coxandkings.travel.bookingengine.db.resource.manageproductupdates.ProductUpdateFlightResponse;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Qualifier("ProductUpdatesSearchRepository")
@Repository
public class ProductUpdatesSearchImpl extends SimpleJpaRepository<Booking, Serializable> implements ProductUpdatesSearchRepository  {

    private EntityManager em;

    public ProductUpdatesSearchImpl(EntityManager em) {
        super(Booking.class, em);
        this.em = em;
    }

    @Override
    public List<? extends CheaperPriceBookingInfo> searchCheaperPriceBookings( String newProductSubCategory ) throws SQLException {

        ArrayList<? extends CheaperPriceBookingInfo> resultsList = new ArrayList<>();
        ProductSubCategory productSubCategory = ProductSubCategory.fromString( newProductSubCategory );

        switch( productSubCategory )   {
            case AIR:   {
                resultsList = getFlightBookings( newProductSubCategory );
            }
            break;

            case HOTEL:   {
                resultsList = getHotelBookings( newProductSubCategory );
            }
            break;
        }

        return resultsList;
    }

    private ArrayList<CheaperPriceUpdatesFlightInfo> getFlightBookings(String newProductSubCategory ) throws SQLException {
        ArrayList<CheaperPriceUpdatesFlightInfo> flightBookingsList = new ArrayList<>();

        // the only way we can execute a Stored Proc with Hibernate is below!
        Session session = em.unwrap(Session.class);

        session.doWork((Connection connection) -> {
            try ( final CallableStatement aCallableStatement = connection.prepareCall(
                    "{ ? = call "+ DBConfig.schemaName + ".SearchBookingsForProductUpdates(?) }")) {

                aCallableStatement.registerOutParameter(1, Types.OTHER);
                aCallableStatement.setString(2, newProductSubCategory);
                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);

                while (results.next()) {
                    CheaperPriceUpdatesFlightInfo aFlightInfo = new CheaperPriceUpdatesFlightInfo();
                    aFlightInfo.setBookID(results.getString("bookID"));
                    aFlightInfo.setOrderID(results.getString("orderID"));
                    aFlightInfo.setSupplierID(results.getString("supplierID"));

                    String supplierPriceStr = results.getString( "supplierprice" );
                    aFlightInfo.setSupplierPrice( new Double( supplierPriceStr ) );
                    aFlightInfo.setSupplierCurrency( results.getString( "supplierpricecurrencycode" ));

                    JSONObject flightDetailsJSON = new JSONObject(results.getObject("flightDetailsJSON"));
                    int odoSize = 0;
                    if (!(flightDetailsJSON.isNull("value"))) {

                        JSONObject obj = new JSONObject(flightDetailsJSON.getString("value"));
                        JSONArray odoList = obj.getJSONArray("originDestinationOptions");
                        odoSize = odoList.length();

                        for (int index = 0; index < odoSize; index++) {
                            JSONObject aODO = odoList.getJSONObject(index);
                            JSONArray aFlightSegmentArray = ((JSONArray) aODO.get("flightSegment"));
                            int flightSegmentCount = aFlightSegmentArray.length();
                            ArrayList<CheaperPriceUpdateFlightSegmentInfo> flightSegmentInfoList = new ArrayList<>(flightSegmentCount);

                            for (int count = 0; count < flightSegmentCount; count++) {
                                CheaperPriceUpdateFlightSegmentInfo flightSegmentInfo = new CheaperPriceUpdateFlightSegmentInfo();
                                flightSegmentInfo.setDepartureDate(aFlightSegmentArray.getJSONObject(count).getString("departureDate"));
                                flightSegmentInfo.setOriginLocation(aFlightSegmentArray.getJSONObject(count).getString("originLocation"));
                                flightSegmentInfo.setCabintype(aFlightSegmentArray.getJSONObject(count).getString("cabinType"));
                                flightSegmentInfo.setDestinationLocation(aFlightSegmentArray.getJSONObject(count).getString("destinationLocation"));
                                flightSegmentInfoList.add(flightSegmentInfo);
                            }
                            aFlightInfo.setFlightSegments(flightSegmentInfoList);
                        }
                    }

                    aFlightInfo.setTripType(results.getString("tripType"));
                    aFlightInfo.setStatus(results.getString("status"));
                    flightBookingsList.add(aFlightInfo);
                }
                results.close();
                aCallableStatement.close();
            }
            catch( SQLException e ) {
                e.printStackTrace();
                //TODO How to close statement here? We have no reference of Statement object
                // Can Spring Aspect help here??
            }
            finally {

            }

        });

        return flightBookingsList;
    }

    private ArrayList<CheaperPriceUpdatesHotelInfo> getHotelBookings(String newProductSubCategory ) throws SQLException {
        ArrayList<CheaperPriceUpdatesHotelInfo> hotelBookingsList = new ArrayList<>();

        // the only way we can execute a Stored Proc with Hibernate is below!
        Session session = em.unwrap(Session.class);
        session.doWork((Connection connection) -> {
            try (CallableStatement aCallableStatement = connection.prepareCall(
                    "{ ? = call SearchBookingsForProductUpdates(?) }")) {
                aCallableStatement.registerOutParameter(1, Types.OTHER);
                aCallableStatement.setString(2, newProductSubCategory);
                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);

                while (results.next()) {
                    CheaperPriceUpdatesHotelInfo aHotelInfo = new CheaperPriceUpdatesHotelInfo();
                    aHotelInfo.setBookID(results.getString("bookID"));
                    aHotelInfo.setOrderID(results.getString("orderID"));
                    aHotelInfo.setSupplierID(results.getString("supplierID"));
                    aHotelInfo.setProductSubCategory( results.getString( "productsubcategory" ));

                    String supplierPriceStr = results.getString( "supplierprice" );
                    aHotelInfo.setSupplierPrice( new Double( supplierPriceStr ) );
                    aHotelInfo.setSupplierCurrency( results.getString( "supplierpricecurrencycode" ));

                    java.sql.Date checkInDate = results.getDate( "checkindate" );
                    LocalDate localCheckinDate = checkInDate.toLocalDate();
                    ZonedDateTime checkInDT = ZonedDateTime.ofInstant( localCheckinDate.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault() );
                    String checkInDateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").format( checkInDT );
                    aHotelInfo.setCheckInDate( checkInDateStr );

                    java.sql.Date checkOutDate = results.getDate( "checkoutdate" );
                    LocalDate localCheckoutDate = checkInDate.toLocalDate();
                    ZonedDateTime checkOutDT = ZonedDateTime.ofInstant( localCheckoutDate.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault() );
                    String checkoutDateStr = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").format( checkOutDT );
                    aHotelInfo.setCheckoutDate( checkoutDateStr );

                    aHotelInfo.setRoomCategoryID( results.getString( "roomcategoryid"));
                    aHotelInfo.setRoomCategoryName( results.getString( "roomcategoryname" ));
                    aHotelInfo.setMealPlan( results.getString( "mealname"));
                    aHotelInfo.setMealCode( results.getString( "mealcode" ));
                    aHotelInfo.setStatus( results.getString( "status" ));

                    hotelBookingsList.add( aHotelInfo );
                }
                results.close();
                aCallableStatement.close();
            }

        });
        return hotelBookingsList;
    }


    @Override
    public ProductUpdateFlightResponse
        searchFlightsForProductUpdates(FlightUpdatesSearchCriteria flightUpdateSearchCriteria) throws SQLException {

        //the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);

        List<ProductUpdateFlightInfo> productUpdateFlightInfoList = new ArrayList<>();

        String opsDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";
        SimpleDateFormat dateFormatter = new SimpleDateFormat(opsDateFormat);

        ProductUpdateFlightResponse productUpdateFlightResponse =  new ProductUpdateFlightResponse();

        session.doWork((Connection connection) -> {

            try (final CallableStatement aCallableStatement = connection.prepareCall(
                    // Call Stored procedure - has 7 IN parameters, returns REFCursor as OUT param
                    "{ ? = call " + DBConfig.schemaName + ".searchproductupdates( ?, ?,?, ?, ?, ?, ?, ?, ?) }")) {

                aCallableStatement.registerOutParameter(1, Types.OTHER);
                aCallableStatement.setNull(2, Types.INTEGER); // start index
                aCallableStatement.setNull(3, Types.INTEGER); // limit index
                aCallableStatement.setNull(4, Types.VARCHAR); //p_airlineName;
                aCallableStatement.setNull(5, Types.VARCHAR); //p_fromSector;
                aCallableStatement.setNull(6, Types.VARCHAR); //p_toSector;
                aCallableStatement.setNull(7, Types.VARCHAR); //p_flightNumber;
                aCallableStatement.setNull(8, Types.VARCHAR); //p_flightTimingFrom;
                aCallableStatement.setNull(9, Types.VARCHAR); //p_flightTimingTo;
                aCallableStatement.setNull(10, Types.VARCHAR); //p_supplierId;

                Integer size = flightUpdateSearchCriteria.getSize();
                Integer pageNumber = flightUpdateSearchCriteria.getPageNumber() < 1 ? 1 : flightUpdateSearchCriteria.getPageNumber();


                Integer startIndex = ((pageNumber - 1) * size);

                aCallableStatement.setInt(2, startIndex );
                aCallableStatement.setInt(3, size );


                //TODO : Airline name not yet matched
                if (!(StringUtils.isEmpty(flightUpdateSearchCriteria.getAirlineName()))) {
                    String airlineName = flightUpdateSearchCriteria.getAirlineName();
                    aCallableStatement.setObject(4, airlineName, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightUpdateSearchCriteria.getFromSector()))) {
                    String fromSector = flightUpdateSearchCriteria.getFromSector();
                    aCallableStatement.setObject(5, fromSector, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightUpdateSearchCriteria.getToSector()))) {
                    String toSector = flightUpdateSearchCriteria.getToSector();
                    aCallableStatement.setObject(6, toSector, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightUpdateSearchCriteria.getFlightNumber()))) {
                    String flightNumber  = flightUpdateSearchCriteria.getFlightNumber();
                    aCallableStatement.setObject(7, flightNumber, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightUpdateSearchCriteria.getFlightTimingFrom()))) {
                    String flightTimingFrom = flightUpdateSearchCriteria.getFlightTimingFrom();
                    aCallableStatement.setObject(8, flightTimingFrom, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightUpdateSearchCriteria.getFlightTimingTo()))) {
                    String flightTimingTo = flightUpdateSearchCriteria.getFlightTimingTo();
                    aCallableStatement.setObject(9, flightTimingTo, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(flightUpdateSearchCriteria.getSupplierId()))) {
                    String supplierId = flightUpdateSearchCriteria.getSupplierId();
                    aCallableStatement.setObject(10, supplierId, Types.VARCHAR);
                }

                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);


                Long fullCount = 0L;
                while (results.next()) {
                    ProductUpdateFlightInfo productUpdateFlightInfo = new ProductUpdateFlightInfo();
                    String bookingID = results.getString("bookid");
                    String orderId = results.getString("orderId");//is AirlinePnr
                    String supplierId = results.getString("supplierId");
                    String firstName = results.getString("firstName");
                    String lastName = results.getString("lastName");
                    String fromSector =  results.getString("fromSector");
                    String toSector = results.getString("toSector");
                    String flightNumber = results.getString("flightnumber");
                    String departureDate = results.getString("departureDate");
                    String departureTime = results.getString("departureTime");
                    String arrivalDate = results.getString("arrivalDate");
                    String arrivalTime = results.getString("arrivalTime");
                    fullCount = results.getLong("fullcount");;


                    productUpdateFlightInfo.setBookingID(bookingID);
                    productUpdateFlightInfo.setOrderID(orderId);
                    productUpdateFlightInfo.setSupplierID(supplierId);
                    productUpdateFlightInfo.setFlightNumber(flightNumber);
                    productUpdateFlightInfo.setLeadPaxName(firstName + " " + lastName);
                    productUpdateFlightInfo.setFromSector(fromSector);
                    productUpdateFlightInfo.setToSector(toSector);
                    productUpdateFlightInfo.setFlightNumber(flightNumber);
                    productUpdateFlightInfo.setDepartureDate(departureDate);
                    productUpdateFlightInfo.setDepartureTime(departureTime);
                    productUpdateFlightInfo.setArrivalDate(arrivalDate);
                    productUpdateFlightInfo.setArrivalTime(arrivalTime);
                    productUpdateFlightInfoList.add(productUpdateFlightInfo);
                }

                Double pages = Double.valueOf(fullCount) / size;
                Integer numberOfPages = (int) Math.ceil(pages);

                productUpdateFlightResponse.setNumberOfPages(numberOfPages);
                productUpdateFlightResponse.setProductUpdateFlightInfo(productUpdateFlightInfoList);
                results.close();
                aCallableStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                // TODO How to close statement here? We have no reference of Statement object
                // Can Spring Aspect help here??
            } finally {
            }
        });
        return productUpdateFlightResponse;
    }
}