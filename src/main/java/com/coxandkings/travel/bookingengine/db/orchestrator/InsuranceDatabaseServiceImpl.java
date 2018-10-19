package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.enums.BookingStatus;
import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.CruiseOrders;
import com.coxandkings.travel.bookingengine.db.model.InsuranceOrders;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.InsuranceDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Transactional(readOnly=false)
public class InsuranceDatabaseServiceImpl implements DataBaseService,Constants,ErrorConstants {

	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
	
	@Autowired
	@Qualifier("Insurance")
	private InsuranceDatabaseRepository insuranceRepository;
	
	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingService;
	
	@Override
	public boolean isResponsibleFor(String product) {
		// TODO Auto-generated method stub
		return "insurance".equalsIgnoreCase(product);
	}

	@Override
	public String processBookRequest(JSONObject bookRequestJson) throws BookingEngineDBException {
		// TODO Auto-generated method stub
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		if(booking==null)
			booking = bookingService.processBookRequest(bookRequestJson,false);
		
		JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("insuranceDetails")) {
			
			JSONObject orderJsonObj = new JSONObject(orderJson.toString());
			
			InsuranceOrders order = populateInsuranceData(orderJsonObj,booking,bookRequestHeader);
			order.setProductSubCategory(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString("product"));
			saveOrder(order,"");
			
		}
		
		myLogger.info(String.format("Insurance Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));
		return "success";
	}
	
	public InsuranceOrders populateInsuranceData(JSONObject orderJsonObj, Booking booking,JSONObject bookRequestHeader) throws BookingEngineDBException {
		
		InsuranceOrders order = new InsuranceOrders();
		try {
			
			order.setBooking(booking);
			
			order.setLastModifiedBy(bookRequestHeader.getString(JSON_PROP_USERID));
			order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
			order.setStatus(OrderStatus.RQ.getProductStatus());
			
			order.setSupplierType("online");
			
			order.setSupplierID(orderJsonObj.getString(JSON_PROP_SUPPREF));
			order.setRoe(orderJsonObj.optString(JSON_PROP_ROE));
			
			order.setPaxDetails(orderJsonObj.optJSONArray("coveredTravelers").toString());
				
			order.setCoveredTrips(orderJsonObj.getJSONObject("insCoverageDetail").getJSONArray("coveredTrips").toString());
			
			order.setInsuCustDtls(orderJsonObj.getJSONObject("insuranceCustomer").toString());
			
			order.setPfbPlanID(orderJsonObj.getJSONObject("planForBook").getString("planID"));
			order.setPfbTypeID(orderJsonObj.getJSONObject("planForBook").getString("typeID"));
			
//			order.setSupplierPrice(orderJsonObj.getJSONObject("planForBook").getJSONObject("planCost").toString());
		} catch (Exception e) {
			// TODO: handle exception
			myLogger.fatal("Failed to populate Insurance Data "+ e);
			throw new BookingEngineDBException("Failed to populate Insurance Data");
		}
		
		return order;
	}

	public InsuranceOrders saveOrder(InsuranceOrders order, String prevOrder) throws BookingEngineDBException {
		InsuranceOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, InsuranceOrders.class);

		} catch (InvocationTargetException | IllegalAccessException e) {
			 myLogger.fatal("Error while saving Air order object : " + e);
			throw new BookingEngineDBException("Failed to save air order object");
		}
		return insuranceRepository.saveOrder(orderObj,prevOrder);
	}

	JSONObject response=new JSONObject();
	@Override
	public String processBookResponse(JSONObject resJson) throws BookingEngineDBException {
		Booking booking = bookingRepository.findOne(resJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null){
			myLogger.warn(String.format("Insurance Booking Response could not be populated since no bookings found for req with bookID %s","" ));
			response.put("ErrorCode","BE_ERR_001");
			response.put("ErrorMsg", BE_ERR_001);
			return response.toString();
		}
		else
		{
			List<InsuranceOrders> orders = insuranceRepository.findByBooking(booking);
			
			JSONArray suppBookRefArray = resJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("insuranceDetails");
			
			for(int i=0;i<suppBookRefArray.length();i++)
			{
				JSONObject suppBookObj = new JSONObject();
				suppBookObj=suppBookRefArray.getJSONObject(i);
			
				for(int j=0;j<orders.size();j++) {
					InsuranceOrders order = orders.get(j);
					if(j==i)
					{
						order.setPolicyNumber(suppBookObj.getJSONObject("planForBook").getJSONObject("policyDetail").getJSONObject("policyNumber").getString("id"));
						order.setPolicyNumberType(suppBookObj.getJSONObject("planForBook").getJSONObject("policyDetail").getJSONObject("policyNumber").getString("type"));
						
						order.setRefNumber(suppBookObj.getJSONObject("planForBook").getJSONObject("policyDetail").getJSONObject("refNumber").getString("id"));
						order.setRefNumberType(suppBookObj.getJSONObject("planForBook").getJSONObject("policyDetail").getJSONObject("refNumber").getString("type"));
						
						order.setPlanRestrictionCode(suppBookObj.getJSONObject("planForBook").getJSONObject("policyDetail").getJSONArray("planRestrictions").toString());
						order.setPolicyDetailURL(suppBookObj.getJSONObject("planForBook").getJSONObject("policyDetail").getString("poilicyDetailURL"));
						
						order.setStatus(OrderStatus.OK.getProductStatus());
						insuranceRepository.save(order);
					}
					
				}
			}
//			myLogger.info(String.format("Insurance Booking Response populated successfully for req with bookID %s = %s", resJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID),resJson.toString()));
			return "SUCCESS";
		}
	}

	@Override
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String processAmClResponse(JSONObject resJson) throws BookingEngineDBException, Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
