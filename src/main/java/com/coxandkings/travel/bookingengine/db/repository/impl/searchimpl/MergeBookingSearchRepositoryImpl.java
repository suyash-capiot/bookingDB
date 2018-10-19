package com.coxandkings.travel.bookingengine.db.repository.impl.searchimpl;

import com.coxandkings.travel.bookingengine.db.config.DBConfig;
import com.coxandkings.travel.bookingengine.db.criteria.mergebooking.MergeBookingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.search.MergeBookingSearchRepository;
import com.coxandkings.travel.bookingengine.db.resource.mergebooking.MergeBookingInfo;
import com.coxandkings.travel.bookingengine.db.utils.SearchUtil;
import org.apache.kafka.common.protocol.types.Field;
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
import java.util.ArrayList;
import java.util.List;

@Qualifier("MergeBookingSearchRepository")
@Repository
public class MergeBookingSearchRepositoryImpl extends SimpleJpaRepository<Booking, Serializable>
        implements MergeBookingSearchRepository {

    private EntityManager em;

    public MergeBookingSearchRepositoryImpl(EntityManager em) {
        super(Booking.class, em);
        this.em = em;
    }

    @Override
    public String searchMergeBookings(MergeBookingSearchCriteria mergeBookingSearchCriteria) throws SQLException {

        //the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);

        StringBuilder sb = new StringBuilder();

        JSONArray mergeProductInfo = new JSONArray();

        String opsDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";
        SimpleDateFormat dateFormatter = new SimpleDateFormat(opsDateFormat);

        session.doWork((Connection connection) -> {
            try (final CallableStatement aCallableStatement = connection.prepareCall(
                    // Call Stored procedure - has 7 IN parameters, returns REFCursor as OUT param
                    "{ ? = call " + DBConfig.schemaName + ".searchmergebookings( ?, ?, ?, ?, ?) }")) {

                //First param to stored Proc for PostGres is always REFCursor
                aCallableStatement.registerOutParameter(1, Types.OTHER);
                aCallableStatement.setNull(2, Types.VARCHAR); //p_checkInDate;
                aCallableStatement.setNull(3, Types.VARCHAR); //p_checkOutDate;
                aCallableStatement.setNull(4, Types.VARCHAR); //p_roomCategory;
                aCallableStatement.setNull(5, Types.VARCHAR); //p_roomType;
                aCallableStatement.setNull(6, Types.VARCHAR); //p_hotelCode;

                if (!(StringUtils.isEmpty(mergeBookingSearchCriteria.getCheckInDate()))) {
                    String checkInDate = mergeBookingSearchCriteria.getCheckInDate();
                    aCallableStatement.setObject(2, checkInDate, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(mergeBookingSearchCriteria.getCheckOutDate()))) {
                    String checkOutDate = mergeBookingSearchCriteria.getCheckOutDate();
                    aCallableStatement.setObject(3, checkOutDate, Types.VARCHAR);
                }


                if (!(StringUtils.isEmpty(mergeBookingSearchCriteria.getRoomCategory()))) {
                    String roomCategory = mergeBookingSearchCriteria.getRoomCategory();
                    aCallableStatement.setObject(4, roomCategory, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(mergeBookingSearchCriteria.getRoomType()))) {
                    String roomType = mergeBookingSearchCriteria.getRoomType();
                    aCallableStatement.setObject(5, roomType, Types.VARCHAR);
                }

                if (!(StringUtils.isEmpty(mergeBookingSearchCriteria.getHotelCode()))) {
                    String hotelCode = mergeBookingSearchCriteria.getHotelCode();
                    aCallableStatement.setObject(6, hotelCode, Types.VARCHAR);
                }

                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);


                while (results.next()) {
                    MergeBookingInfo mergeBookingInfo = new MergeBookingInfo();

                    String bookID = results.getString("bookId");
                    String productCategory = results.getString("productCategory");
                    String productSubCategory = results.getString("productSubCategory");
                    String orderId = results.getString("orderId");
                    String cityCode = results.getString("cityCode");
                    String countryCode = results.getString("countryCode");

                    String hotelCode = results.getString("hotelCode");
                    String hotelName = results.getString("hotelName");
                    String roomId = results.getString("roomId");
                    String checkInDate = results.getString("checkInDate");
                    String checkOutDate = results.getString("checkOutDate");
                    String paxID = results.getString("paxID");
                    String isLeadPax = results.getString("isLeadPax");
                    String roomTypeCode = results.getString("roomTypeCode");
                    String roomCategoryId = results.getString("roomCategoryId");

                    String mealCode = results.getString("mealCode");
                    String mealName = results.getString("mealName");
                    String totalPrice = results.getString("totalPrice");
                    JSONObject totalTaxBreakup = new JSONObject(results.getString("totalTaxBreakup"));
                    String totalPriceCurrencyCode = results.getString("totalPriceCurrencyCode");
                    JSONObject supplierTaxBreakup =  new JSONObject(results.getString("supplierTaxBreakup"));

                    String supplierPrice = results.getString("supplierPrice");
                    String supplierPriceCurrencyCode = results.getString("supplierPriceCurrencyCode");

                    mergeBookingInfo.setBookId(bookID);
                    mergeBookingInfo.setProductCategory(productCategory);
                    mergeBookingInfo.setProductSubCategory(productSubCategory);
                    mergeBookingInfo.setOrderId(orderId);
                    mergeBookingInfo.setCityCode(cityCode);
                    mergeBookingInfo.setCountryCode(countryCode);
                    mergeBookingInfo.setHotelCode(hotelCode);
                    mergeBookingInfo.setHotelName(hotelName);
                    mergeBookingInfo.setRoomId(roomId);
                    mergeBookingInfo.setCheckInDate(checkInDate);
                    mergeBookingInfo.setCheckOutDate(checkOutDate);
                    mergeBookingInfo.setPaxID(paxID);
                    mergeBookingInfo.setIsLeadPax(isLeadPax);
                    mergeBookingInfo.setRoomTypeCode(roomTypeCode);
                    mergeBookingInfo.setRoomCategoryId(roomCategoryId);
                    mergeBookingInfo.setMealCode(mealCode);
                    mergeBookingInfo.setMealName(mealName);
                    mergeBookingInfo.setTotalPrice(totalPrice);
                    mergeBookingInfo.setTotalTaxBreakup(totalTaxBreakup);
                    mergeBookingInfo.setTotalPriceCurrencyCode(totalPriceCurrencyCode);
                    mergeBookingInfo.setSupplierTaxBreakup(supplierTaxBreakup);
                    mergeBookingInfo.setSupplierPrice(supplierPrice);
                    mergeBookingInfo.setSupplierPriceCurrencyCode(supplierPriceCurrencyCode);

                    JSONObject resJson = new JSONObject(mergeBookingInfo);
                    mergeProductInfo.put(resJson);
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

        return mergeProductInfo.toString();
    }

	@Override
	public String getMergeBookings() throws SQLException {
		  //the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);
       JSONArray mergeProductInfo = new JSONArray();

       session.doWork((Connection connection) -> {
            try (final CallableStatement aCallableStatement = connection.prepareCall(
                    // Call Stored procedure - has 7 IN parameters, returns REFCursor as OUT param
                    "{ ? = call " + DBConfig.schemaName + ".getmergebookings() }")) {

                //First param to stored Proc for PostGres is always REFCursor
                aCallableStatement.registerOutParameter(1, Types.OTHER);
                
                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);


                while (results.next()) {
                    MergeBookingInfo mergeBookingInfo = new MergeBookingInfo();

                    String bookID = results.getString("Book_order_room_id");
                    String hotelName = results.getString("hotelName");
                    String checkInDate = results.getString("checkInDate");
                    String checkOutDate = results.getString("checkOutDate");
                    String roomTypeCode = results.getString("roomTypeName");
                    String roomCategoryId = results.getString("roomCategoryName");

                    
                    mergeBookingInfo.setBookId(bookID);
                    mergeBookingInfo.setHotelName(hotelName);
                    mergeBookingInfo.setCheckInDate(checkInDate);
                    mergeBookingInfo.setCheckOutDate(checkOutDate);
                    mergeBookingInfo.setRoomTypeCode(roomTypeCode);
                    mergeBookingInfo.setRoomCategoryId(roomCategoryId);

                    JSONObject resJson = new JSONObject(mergeBookingInfo);
                    mergeProductInfo.put(resJson);
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

        return mergeProductInfo.toString();

	}

}
