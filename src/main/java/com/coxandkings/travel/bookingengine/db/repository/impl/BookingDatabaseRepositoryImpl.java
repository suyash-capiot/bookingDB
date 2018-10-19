package com.coxandkings.travel.bookingengine.db.repository.impl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;
import javax.persistence.criteria.*;

import com.coxandkings.travel.bookingengine.db.config.DBConfig;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.BookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.ProductDetailsFilter;
import com.coxandkings.travel.bookingengine.db.enums.ProductCategory;
import com.coxandkings.travel.bookingengine.db.enums.ProductSubCategory;
import com.coxandkings.travel.bookingengine.db.resource.searchviewfilter.BookingSearchResponseItem;
import com.coxandkings.travel.bookingengine.db.utils.SearchUtil;
import org.hibernate.Session;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import org.springframework.util.StringUtils;

@Qualifier("Booking")
@Repository
public class BookingDatabaseRepositoryImpl extends SimpleJpaRepository<Booking, Serializable> implements BookingDatabaseRepository {

    public BookingDatabaseRepositoryImpl(EntityManager em) {
        super(Booking.class, em);
        this.em = em;
    }

    private EntityManager em;

    @Autowired
    private DBConfig dbConfig;

    public Booking saveOrder(Booking orderObj, String prevOrder) {
        return this.save(orderObj);

    }

    @Override
    public List<Booking> findByUserID(String userID) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Booking> criteria = builder.createQuery(Booking.class);
        Root<Booking> root = criteria.from(Booking.class);
        Predicate p1 = builder.and(builder.equal(root.get("userID"), userID));
        criteria.where(p1);
        return em.createQuery(criteria).getResultList();
    }

    @Override
    public List<Booking> findByStatus(String status) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Booking> criteria = builder.createQuery(Booking.class);
        Root<Booking> root = criteria.from(Booking.class);
        Predicate p1 = builder.and(builder.equal(root.get("status"), status));
        criteria.where(p1);
        return em.createQuery(criteria).getResultList();
    }

    @Override
    public List<Booking> findBySearchCriteria(BookingSearchCriteria bookingCriteria) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Booking> criteriaQuery = criteriaBuilder.createQuery(Booking.class);
        Root<Booking> root = criteriaQuery.from(Booking.class);
        criteriaQuery.select(root);

        List<Predicate> predicates = new ArrayList<>();
        if (!(StringUtils.isEmpty(bookingCriteria.getBookingBasedFilter().getBookingRefId()))) {
            predicates.add(criteriaBuilder.equal(root.get("bookID"), bookingCriteria.getBookingBasedFilter().getBookingRefId()));
        }

        if (!(StringUtils.isEmpty(bookingCriteria.getBookingBasedFilter().getBookingStatusId()))) {
            predicates.add(criteriaBuilder.equal(root.get("status"), bookingCriteria.getBookingBasedFilter().getBookingStatusId()));
        }

        if (!(StringUtils.isEmpty(bookingCriteria.getBookingBasedFilter().getUserId()))) {
            predicates.add(criteriaBuilder.equal(root.get("userID"), bookingCriteria.getBookingBasedFilter().getUserId()));
        }

        if (!(StringUtils.isEmpty(bookingCriteria.getClientPxBasedFilter().getClientId()))) {
            predicates.add(criteriaBuilder.equal(root.get("clientID"),
                    bookingCriteria.getClientPxBasedFilter().getClientId()));
        }

        if (!(StringUtils.isEmpty(bookingCriteria.getClientPxBasedFilter().getClientType()))) {
            predicates.add(criteriaBuilder.equal(root.get("clientType"),
                    bookingCriteria.getClientPxBasedFilter().getClientType()));
        }

        if (!(StringUtils.isEmpty(bookingCriteria.getBookingBasedFilter().getBookingFromDate()))) {
            String text = bookingCriteria.getBookingBasedFilter().getBookingFromDate().trim();
            ZonedDateTime a = ZonedDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            Timestamp ts = Timestamp.from(a.toInstant());
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.<Timestamp>get("createdAt"), ts));
        }

        if (!(StringUtils.isEmpty(bookingCriteria.getBookingBasedFilter().getBookingToDate()))) {
            String text = bookingCriteria.getBookingBasedFilter().getBookingToDate().trim();
            ZonedDateTime a = ZonedDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            Timestamp ts = Timestamp.from(a.toInstant());
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.<Timestamp>get("createdAt"), ts));
        }

        if (!predicates.isEmpty()) {
            criteriaQuery.where(predicates.toArray(new Predicate[0]));
        } else {
            em.createQuery(criteriaQuery).getResultList();
        }


        return em.createQuery(criteriaQuery).getResultList();
    }

    @Override
    public List<BookingSearchResponseItem> searchBookings(BookingSearchCriteria aBookingSearchCriteria) {
        // the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);
        SearchUtil filterUtil = new SearchUtil();

        List<BookingSearchResponseItem> bookingSearchResponseList = new ArrayList<>();

        String opsDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";
        SimpleDateFormat dateFormatter = new SimpleDateFormat(opsDateFormat);

        // Begin calling Stored Proc
        session.doWork((Connection connection) -> {

            try (
                    final CallableStatement aCallableStatement = connection.prepareCall(
                            // Call Stored procedure - has 7 IN parameters, returns REFCursor as OUT param
                            "{ ? = call " + DBConfig.schemaName + ".SearchBookings( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) }")) {

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
                aCallableStatement.setNull(3, Types.INTEGER); // limit index
                aCallableStatement.setNull(4, Types.VARCHAR); // sort field
                aCallableStatement.setObject(5, bookingDetailsInParam, Types.OTHER); // BookingDetailsFilter IN param
                aCallableStatement.setObject(6, companyDetailsInParam, Types.OTHER); // CompanyDetailsFilter IN param
                aCallableStatement.setObject(7, clientPaxInParam, Types.OTHER);  // ClientPaxFilter IN param
                aCallableStatement.setNull(8, Types.OTHER); // Set null initially for Flight, override in switch block below
                aCallableStatement.setNull(9, Types.OTHER); // Set null initially for Flight, override in switch block below
                aCallableStatement.setNull(10, Types.OTHER);// Set null initially for Activities, override in switch block below
                aCallableStatement.setNull(11, Types.OTHER);// Set null initially for Holidays, override in switch block below

                ProductDetailsFilter prodDetails = aBookingSearchCriteria.getProductBasedFilter();

                Integer size = aBookingSearchCriteria.getSize();
                Integer pageNumber = aBookingSearchCriteria.getPage() < 1 ? 1 : aBookingSearchCriteria.getPage();


                Integer startIndex = ((pageNumber - 1) * size);

                aCallableStatement.setInt(2, startIndex );
                aCallableStatement.setInt(3, size );

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
                            aCallableStatement.setObject(8, flightDetailsInParam, Types.OTHER); // FlightSearch filter IN param
                            // Add for Cars, Bus, etc. in future
                        }
                        break;

                        case PRODUCT_CATEGORY_ACCOMMODATION:    {
                            // Prepare filter for Hotel details filter
                            aBookingSearchCriteria.getProductBasedFilter().setProductCategorySubTypeId(ProductSubCategory.HOTEL.getProductSubCategory());
                            PGobject hotelDetailsInParam = new PGobject();
                            hotelDetailsInParam.setType("HotelSearchCriteria");
                            hotelDetailsInParam.setValue(filterUtil.getHotelSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(9, hotelDetailsInParam, Types.OTHER); // Hotel Search Filter IN param
                        }
                        break;

                        case PRODUCT_CATEGORY_ACTIVITIES:    {
                            // Prepare filter for Hotel details filter
                            aBookingSearchCriteria.getProductBasedFilter().setProductCategorySubTypeId(ProductSubCategory.EVENTS.getProductSubCategory());
                            PGobject activitiesDetailsInParam = new PGobject();
                            activitiesDetailsInParam.setType("ActivitiesSearchCriteria");
                            activitiesDetailsInParam.setValue(filterUtil.getActivitiesSearchCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(10, activitiesDetailsInParam, Types.OTHER); // Hotel Search Filter IN param
                        }
                        break;

                        case PRODUCT_CATEGORY_HOLIDAYS:    {
                            // Prepare filter for Holidays details filter
                            aBookingSearchCriteria.getProductBasedFilter().setProductCategorySubTypeId(ProductSubCategory.HOLIDAYS.getProductSubCategory());
                            PGobject holidaysDetailsInParam = new PGobject();
                            holidaysDetailsInParam.setType("HolidaysSearchCriteria");
                            holidaysDetailsInParam.setValue(filterUtil.getHolidaysSearchCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(11, holidaysDetailsInParam, Types.OTHER); // Holidays Search Filter in param
                        }
                        break;
                    }
                }

                else if (prodDetails != null && !(StringUtils.isEmpty(prodDetails.getProductCategorySubTypeId()) )) {
                    ProductSubCategory aProdSubCategory = ProductSubCategory.fromString(prodDetails.getProductCategorySubTypeId());
                    switch (aProdSubCategory) {
                        case AIR: {
                            // Prepare filter for Flight details filter
                            PGobject flightDetailsInParam = new PGobject();
                            flightDetailsInParam.setType("FlightSearchCriteria");
                            flightDetailsInParam.setValue(filterUtil.getFlightSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(8, flightDetailsInParam, Types.OTHER); // FlightSearch filter IN param
                        }
                        break;

                        case HOTEL: {
                            // Prepare filter for Hotel details filter
                            PGobject hotelDetailsInParam = new PGobject();
                            hotelDetailsInParam.setType("HotelSearchCriteria");
                            hotelDetailsInParam.setValue(filterUtil.getHotelSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(9, hotelDetailsInParam, Types.OTHER); // Hotel Search Filter IN param
                        }
                        break;

                        case EVENTS: {
                            // Prepare filter for Hotel details filter
                            PGobject activitiesDetailsInParam = new PGobject();
                            activitiesDetailsInParam.setType("ActivitiesSearchCriteria");
                            activitiesDetailsInParam.setValue(filterUtil.getActivitiesSearchCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(10, activitiesDetailsInParam, Types.OTHER); // Hotel Search Filter IN param
                        }
                        break;

                        case HOLIDAYS: {
                            // Prepare filter for Holidays details filter
                            PGobject holidaysDetailsInParam = new PGobject();
                            holidaysDetailsInParam.setType("HolidaysSearchCriteria");
                            holidaysDetailsInParam.setValue(filterUtil.getHolidaysSearchCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                            aCallableStatement.setObject(11, holidaysDetailsInParam, Types.OTHER); // Holidays Search Filter in param
                        }
                        break;
                    }
                }

                else if( (StringUtils.isEmpty(prodDetails.getProductCategorySubTypeId())) &&
                        (StringUtils.isEmpty(prodDetails.getProductCategorySubTypeId())) )  {

                    // Prepare filter for Flight details filter
                    aBookingSearchCriteria.getProductBasedFilter().setProductCategorySubTypeId(ProductSubCategory.AIR.getProductSubCategory());
                    PGobject flightDetailsInParam = new PGobject();
                    flightDetailsInParam.setType("FlightSearchCriteria");
                    flightDetailsInParam.setValue(filterUtil.getFlightSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                    aCallableStatement.setObject(8, flightDetailsInParam, Types.OTHER); // FlightSearch filter IN param

                    aBookingSearchCriteria.getProductBasedFilter().setProductCategorySubTypeId(ProductSubCategory.HOTEL.getProductSubCategory());
                    PGobject hotelDetailsInParam = new PGobject();
                    hotelDetailsInParam.setType("HotelSearchCriteria");
                    hotelDetailsInParam.setValue(filterUtil.getHotelSearchingCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                    aCallableStatement.setObject(9, hotelDetailsInParam, Types.OTHER); // Hotel Search Filter IN param

                    aBookingSearchCriteria.getProductBasedFilter().setProductCategorySubTypeId(ProductSubCategory.EVENTS.getProductSubCategory());
                    PGobject activitiesDetailsInParam = new PGobject();
                    activitiesDetailsInParam.setType("ActivitiesSearchCriteria");
                    activitiesDetailsInParam.setValue(filterUtil.getActivitiesSearchCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                    aCallableStatement.setObject(10, activitiesDetailsInParam, Types.OTHER); // Hotel Search Filter IN param

                    aBookingSearchCriteria.getProductBasedFilter().setProductCategorySubTypeId(ProductSubCategory.HOLIDAYS.getProductSubCategory());
                    PGobject holidaysDetailsInParam = new PGobject();
                    holidaysDetailsInParam.setType("HolidaysSearchCriteria");
                    holidaysDetailsInParam.setValue(filterUtil.getHolidaysSearchCriteria(aBookingSearchCriteria.getProductBasedFilter()));
                    aCallableStatement.setObject(11, holidaysDetailsInParam, Types.OTHER); // Holidays Search Filter IN param

                }


                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);

                while (results.next()) {
                    BookingSearchResponseItem bookingSearchResponseItem = new BookingSearchResponseItem();
                    String bookID = results.getString("bookid");
                    java.sql.Timestamp createdDate = results.getTimestamp("createdat");
                    String clientType = results.getString("clienttype");
                    String clientID = results.getString("clientid");
                    String company = results.getString("companyid");
                    String status = results.getString("status");
                    String flightSubCategory = results.getString("flightSubCategory");
                    String hotelSubCategory = results.getString("hotelSubCategory");
                    String activitiesSubCategory = results.getString("activitiessubcategory");
                    String holidaysSubCategory = results.getString("holidayssubcategory");
                    String paymentStatus = results.getString("paymentstatus");

                    String staffID = results.getString( "staffid" );
                    String pointOfSale = results.getString("pointofsale");

                    Long fullCount = results.getLong("fullcount");

                    Double pages = Double.valueOf(fullCount) / size;
                    Integer numberOfPages = (int) Math.ceil(pages);

                    bookingSearchResponseItem.setBookID(bookID);
                    bookingSearchResponseItem.setClientType(clientType);
                    bookingSearchResponseItem.setClientID(clientID);
                    bookingSearchResponseItem.setCompany(company);
                    bookingSearchResponseItem.setStatus(status);
                    bookingSearchResponseItem.setStaffID( staffID );
                    bookingSearchResponseItem.setPointOfSale(pointOfSale);
                    bookingSearchResponseItem.setNumberOfPages(numberOfPages);
                    bookingSearchResponseItem.setPaymentStatus(paymentStatus);

                    List<String> productSubCategories = new ArrayList<>();
                    if(!(StringUtils.isEmpty(flightSubCategory)) && !(productSubCategories.contains("Flight"))){
                        productSubCategories.add(flightSubCategory);
                    }

                    if(!(StringUtils.isEmpty(hotelSubCategory))  && !(productSubCategories.contains("Hotel"))){
                        productSubCategories.add(hotelSubCategory);
                    }


                    if(!(StringUtils.isEmpty(activitiesSubCategory))){
                        productSubCategories.add(activitiesSubCategory);
                    }

                    if(!(StringUtils.isEmpty(holidaysSubCategory))){
                        productSubCategories.add(holidaysSubCategory);
                    }

                    bookingSearchResponseItem.setProductSubCategories(productSubCategories);

                    if (createdDate != null) {
                        String createdDateStr = dateFormatter.format(createdDate);
                        bookingSearchResponseItem.setCreatedDate(createdDateStr);
                    }

                    bookingSearchResponseList.add(bookingSearchResponseItem);
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
        return bookingSearchResponseList;
    }
}
