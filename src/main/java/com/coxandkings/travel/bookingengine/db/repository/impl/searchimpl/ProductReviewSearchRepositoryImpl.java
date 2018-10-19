package com.coxandkings.travel.bookingengine.db.repository.impl.searchimpl;

import com.coxandkings.travel.bookingengine.db.config.DBConfig;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.repository.search.ArrivalListSearchRepository;
import com.coxandkings.travel.bookingengine.db.repository.search.ProductReviewSearchRepository;
import com.coxandkings.travel.bookingengine.db.resource.productreview.ProductReviewInfo;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Qualifier("ProductReviewSearchRepository")
@Repository
public class ProductReviewSearchRepositoryImpl extends SimpleJpaRepository<Booking, Serializable>
        implements ProductReviewSearchRepository {

    private EntityManager em;

    public ProductReviewSearchRepositoryImpl(EntityManager em) {
        super(Booking.class, em);
        this.em = em;
    }

    @Override
    public List<ProductReviewInfo> searchGeneralArrivalList(String productEndDate) throws SQLException {

        //the only way we are able execute a Stored Proc with Hibernate is below!!
        Session session = em.unwrap(Session.class);

        List<ProductReviewInfo> productReviewInfoList = new ArrayList<>();

        session.doWork((Connection connection) -> {
            try (final CallableStatement aCallableStatement = connection.prepareCall(
                    // Call Stored procedure - has 7 IN parameters, returns REFCursor as OUT param
                    "{ ? = call " + DBConfig.schemaName + ".searchprodutstoreview(?) }")) {

                //First param to stored Proc for PostGres is always REFCursor
                aCallableStatement.registerOutParameter(1, Types.OTHER);
                aCallableStatement.setNull(2, Types.VARCHAR); //p_productEndDate;


                if (!(StringUtils.isEmpty(productEndDate))) {
                    aCallableStatement.setObject(2, productEndDate, Types.VARCHAR);
                }

                aCallableStatement.execute();
                ResultSet results = (ResultSet) aCallableStatement.getObject(1);


                while (results.next()) {
                    ProductReviewInfo productReviewInfo = new ProductReviewInfo();

                    String bookID = results.getString("bookId");
                    String orderId = results.getString("orderId");
                    String productSubCategory = results.getString("productsubcategory");
                    String clientId = results.getString("clientId");
                    String clientType = results.getString("clienttype");
                    String groupNameId = results.getString("groupnameid");

                    productReviewInfo.setBookId(bookID);
                    productReviewInfo.setOrderId(orderId);
                    productReviewInfo.setProductSubCategory(productSubCategory);
                    productReviewInfo.setClientId(clientId);
                    productReviewInfo.setClientType(clientType);
                    productReviewInfo.setGroupNameId(groupNameId);

                    productReviewInfoList.add(productReviewInfo);
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

        return productReviewInfoList;
    }

}