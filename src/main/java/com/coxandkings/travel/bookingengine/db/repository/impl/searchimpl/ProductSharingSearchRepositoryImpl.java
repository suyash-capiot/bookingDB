package com.coxandkings.travel.bookingengine.db.repository.impl.searchimpl;


import com.coxandkings.travel.bookingengine.db.config.DBConfig;
import com.coxandkings.travel.bookingengine.db.criteria.productsharing.ProductSharingSearchCriteria;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.search.ProductSharingSearchRepository;
import com.coxandkings.travel.bookingengine.db.resource.productsharing.ProductSharingInfo;
import com.coxandkings.travel.bookingengine.db.utils.SearchUtil;
import org.hibernate.Session;
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

@Qualifier("ProductSharingSearchRepository")
@Repository
public class ProductSharingSearchRepositoryImpl extends SimpleJpaRepository<Booking, Serializable>
        implements ProductSharingSearchRepository {

    private EntityManager em;

    public ProductSharingSearchRepositoryImpl(EntityManager em) {
        super(Booking.class, em);
        this.em = em;
    }


    @Override
    public List<ProductSharingInfo> searchProductSharingBookings(ProductSharingSearchCriteria productSharingSearchCriteria)
            throws SQLException {

        //the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);
        SearchUtil filterUtil = new SearchUtil();

        List<ProductSharingInfo>  productSharingInfoList = new ArrayList<>();

        String opsDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";
        SimpleDateFormat dateFormatter = new SimpleDateFormat(opsDateFormat);

        //Begin calling Stored Proc
        session.doWork((Connection connection) -> {

            try (final CallableStatement aCallableStatement = connection.prepareCall(
                            // Call Stored procedure - has 7 IN parameters, returns REFCursor as OUT param
                            "{ ? = call " + DBConfig.schemaName + ".sharedProductSearch( ?, ?, ?, ?, ?, ?, ?) }")) {

                // First param to stored Proc for PostGres is always REFCursor
                aCallableStatement.registerOutParameter(1, Types.OTHER);

                aCallableStatement.setNull(2, Types.VARCHAR); // p_productname_subtype_flavorname
                aCallableStatement.setNull(3, Types.VARCHAR); // p_traveldatefrom
                aCallableStatement.setNull(4, Types.VARCHAR); // p_traveldateto
                aCallableStatement.setNull(5, Types.VARCHAR); // p_roomcategory
                aCallableStatement.setNull(6, Types.VARCHAR); // p_roomtype
                aCallableStatement.setNull(7, Types.VARCHAR); // p_gender
                aCallableStatement.setNull(8, Types.VARCHAR); // p_issharable

                if(!(StringUtils.isEmpty(productSharingSearchCriteria.getProductNameSubTypeFlavour()))) {
                    String productSharingName = productSharingSearchCriteria.getProductNameSubTypeFlavour();
                    aCallableStatement.setObject(2,productSharingName ,Types.VARCHAR); // p_productname_subtype_flavorname

                }

                if(!(StringUtils.isEmpty(productSharingSearchCriteria.getCheckInDate()))) {
                    String checkInDate = productSharingSearchCriteria.getCheckInDate();
                    aCallableStatement.setObject(3,checkInDate ,Types.VARCHAR); // p_traveldatefrom
                }

                if(!(StringUtils.isEmpty(productSharingSearchCriteria.getCheckOutDate()))) {
                    String checkOutDate = productSharingSearchCriteria.getCheckOutDate();
                    aCallableStatement.setObject(4,checkOutDate ,Types.VARCHAR); // p_traveldateto
                }

                if(!(StringUtils.isEmpty(productSharingSearchCriteria.getRoomCategory()))) {
                    String roomCategory = productSharingSearchCriteria.getRoomCategory();
                    aCallableStatement.setObject(5, roomCategory,Types.VARCHAR); // p_roomtype
                }

                if(!(StringUtils.isEmpty(productSharingSearchCriteria.getRoomType()))) {
                    String roomType = productSharingSearchCriteria.getRoomType();
                    aCallableStatement.setObject(6, roomType ,Types.VARCHAR);  // p_gender
                }

                if(!(StringUtils.isEmpty(productSharingSearchCriteria.getGender()))) {
                    String gender = productSharingSearchCriteria.getGender();
                    aCallableStatement.setObject(7,gender ,Types.VARCHAR); // p_roomcategory
                }

                if(!(StringUtils.isEmpty( productSharingSearchCriteria.getIsSharable()))) {
                    String isSharable = productSharingSearchCriteria.getIsSharable();
                    aCallableStatement.setObject(8,isSharable ,Types.VARCHAR);  // p_issharable
                }

                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);

                JSONObject resJson = new JSONObject();
                while (results.next()) {
                    ProductSharingInfo productSharingInfo = new ProductSharingInfo();
                    String bookID = results.getString("bookId");
                    String orderId = results.getString("orderId");
                    String hotelName = results.getString("hotelName");
                    String cityCode = results.getString("cityCode");
                    String countryCode = results.getString("countryCode");
                    String checkInDate = results.getString("checkInDate");
                    String checkOutDate = results.getString("checkOutDate");
                    String roomId = results.getString("roomId");
                    String numberOfNights = results.getString("numberOfNights");
                    String numberOfDays = results.getString("numberOfDays");
                    String paxId = results.getString("paxID");
                    String firstName = results.getString("firstName");
                    String lastName = results.getString("lastName");
                    JSONObject contactInfo = new JSONObject(results.getString("contactInfo"));
                    String contactInfoCountryCode = contactInfo.getString("countryCode");
                    String mobileNo =  contactInfo.getString("mobileNo");
                    String email = contactInfo.getString("email");

                    productSharingInfo.setBookingRefNo(bookID);
                    productSharingInfo.setCheckInDate(checkInDate);
                    productSharingInfo.setCheckOutDate(checkOutDate);
                    productSharingInfo.setNumberOfDays(numberOfNights);
                    productSharingInfo.setNumberOfNights(numberOfDays);
                    productSharingInfo.setCity(cityCode);
                    productSharingInfo.setCountry(countryCode);
                    productSharingInfo.setHotelName(hotelName);
                    productSharingInfo.setOrderId(orderId);
                    productSharingInfo.setRoomId(roomId);
                    productSharingInfo.setPaxId(paxId);
                    productSharingInfo.setFirstName(firstName);
                    productSharingInfo.setLastName(lastName);
                    productSharingInfo.setMobileNumber(mobileNo);
                    productSharingInfo.setEmail(email);
                    productSharingInfo.setContactInfoCountryCode(contactInfoCountryCode);

                    productSharingInfoList.add(productSharingInfo);
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

        return productSharingInfoList;
    }

}

