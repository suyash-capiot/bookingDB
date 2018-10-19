package com.coxandkings.travel.bookingengine.db.kafka;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.orchestrator.BookingDataBaseServiceImpl;
import com.coxandkings.travel.bookingengine.db.orchestrator.BookingDatabaseService;
import com.coxandkings.travel.bookingengine.db.orchestrator.BookingServiceImpl;
import com.coxandkings.travel.bookingengine.db.orchestrator.DataBaseService;

@Service
public class BookingListenerFactory {

	@Autowired
	private List<DataBaseService> services;
	@Autowired
	@Qualifier("Kafka")
	private KafkaBookProducer kafkaService;

	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingDataService;

	private static final Logger logger = LogManager.getLogger(BookingListenerFactory.class);

	private DataBaseService serviceForProduct(String product) {
		for (DataBaseService service : services) {
			if (service.isResponsibleFor(product)) {
				return service;
			}
		}

		throw new UnsupportedOperationException("unsupported ProductType");
	}

	// TODO: check for the return types of these methods
	public void processBooking(JSONObject reqjson) throws JSONException, BookingEngineDBException {

		String res = null, productRes;
		if (reqjson.has("requestBody"))
			serviceForProduct(reqjson.getJSONObject("requestBody").getString("product")).processBookRequest(reqjson);

		else {
			productRes = serviceForProduct(reqjson.getJSONObject("responseBody").getString("product"))
					.processBookResponse(reqjson);

			if ("SUCCESS".equalsIgnoreCase(productRes))
				res = bookingDataService.processBookResponse(reqjson);

			if ("SUCCESS".equalsIgnoreCase(res)) {
				try {
					JSONObject kafkaJson = new JSONObject();
					kafkaJson.put("BookID",reqjson.getJSONObject("responseBody").getString("bookID"));
					kafkaJson.put("Operation", "book");
					kafkaJson.put("type", "NEW");
					kafkaJson.put("timestamp", ZonedDateTime.now( ZoneOffset.UTC ).toString());
					
					kafkaService.runProducer(1,kafkaJson);

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}

	}

	public void processCancelAmend(JSONObject reqJson) throws Exception {
		
		String res = null;
		String productRes = null;
		if (reqJson.has("requestBody"))
			serviceForProduct(reqJson.getJSONObject("requestBody").getString("product")).processAmClRequest(reqJson);
		else {
			res= "Failed";
		 productRes = serviceForProduct(reqJson.getJSONObject("responseBody").getString("product")).processAmClResponse(reqJson);
		
		 if (productRes.equalsIgnoreCase("SUCCESS"))
				res = bookingDataService.processAmClResponse(reqJson);
		 
		if("SUCCESS".equalsIgnoreCase(res)) {
			
			JSONObject kafkaJson = new JSONObject();
			kafkaJson.put("BookID",reqJson.getJSONObject("responseBody").optString("bookID"));
			
			kafkaJson.put("orderID",reqJson.getJSONObject("responseBody").getString("orderID"));
			kafkaJson.put("Operation",reqJson.getJSONObject("responseBody").getString("requestType"));
			kafkaJson.put("type", reqJson.getJSONObject("responseBody").getString("type"));
			kafkaJson.put("timestamp", ZonedDateTime.now( ZoneOffset.UTC ).toString());
			kafkaJson.put("errorCode",reqJson.getJSONObject("responseBody").optString("errorCode"));
			kafkaJson.put("errorMessage", reqJson.getJSONObject("responseBody").optString("errorMessage"));
			
			kafkaService.runProducer(1,kafkaJson);
			
		}
			
			
	}
}
	
	
}
