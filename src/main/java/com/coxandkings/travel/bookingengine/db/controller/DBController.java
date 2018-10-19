package com.coxandkings.travel.bookingengine.db.controller;

import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaBootstrapConfiguration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.coxandkings.travel.bookingengine.db.kafka.KafkaBookProducer;
import com.coxandkings.travel.bookingengine.db.orchestrator.BookingDataBaseServiceImpl;
import com.coxandkings.travel.bookingengine.db.orchestrator.BookingDatabaseService;
import com.coxandkings.travel.bookingengine.db.orchestrator.BookingServiceImpl;
import com.coxandkings.travel.bookingengine.db.orchestrator.DataBaseService;
import com.coxandkings.travel.bookingengine.db.utils.TrackingContext;
import com.coxandkings.travel.bookingengine.db.utils.ServletContext;

@RestController
@RequestMapping("/DBService")
public class DBController {

	@Autowired
	private List<DataBaseService> services;
	
	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingDataService;
	

	@Autowired
	@Qualifier("Kafka")
	private KafkaBookProducer kafkaService;

	private DataBaseService serviceForProduct(String product) {
		for (DataBaseService service : services) {
			if (service.isResponsibleFor(product)) {
				return service;
			}
		}

		throw new UnsupportedOperationException("unsupported ProductType");
	}

	@PostMapping(value = "/dbUpdate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> bookInsert(InputStream req) throws JSONException, Exception {

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
		HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getResponse();
		ServletContext.setServletContext(request, response);
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		TrackingContext.setTrackingContext(reqJson);
		String res, productRes;
		if (reqJson.has("requestBody")) {
			res = serviceForProduct(reqJson.getJSONObject("requestBody").getString("product"))
					.processBookRequest(reqJson);
			return new ResponseEntity<String>(res, HttpStatus.CREATED);
		} else {
			res = "FAILED";
			productRes = serviceForProduct(reqJson.getJSONObject("responseBody").getString("product"))
					.processBookResponse(reqJson);
			if (productRes.equalsIgnoreCase("SUCCESS"))
				res = bookingDataService.processBookResponse(reqJson);

			if ("SUCCESS".equals(res)) {
				JSONObject kafkaJson = new JSONObject();
				kafkaJson.put("BookID", reqJson.getJSONObject("responseBody").getString("bookID"));
				kafkaJson.put("Operation", "book");
				kafkaJson.put("type", "NEW");
				kafkaJson.put("timestamp", ZonedDateTime.now(ZoneOffset.UTC).toString());
				
				kafkaService.runProducer(1, kafkaJson);
			}

			return new ResponseEntity<String>(res, HttpStatus.OK);

		}
	}

	@PostMapping(value = "/amendDBUpdate", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> updateCancelAmend(InputStream req) throws JSONException, Exception {

		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getRequest();
		HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
				.getResponse();
		ServletContext.setServletContext(request, response);
		JSONTokener jsonTok = new JSONTokener(req);
		JSONObject reqJson = new JSONObject(jsonTok);
		TrackingContext.setTrackingContext(reqJson);
		String res, productRes;
		if (reqJson.has("requestBody")) {
			res = serviceForProduct(reqJson.getJSONObject("requestBody").getString("product"))
					.processAmClRequest(reqJson);
			return new ResponseEntity<String>(res, HttpStatus.CREATED);
		} else {

			res= "Failed";
			productRes = serviceForProduct(reqJson.getJSONObject("responseBody").getString("product"))
					.processAmClResponse(reqJson);

			if (productRes.equalsIgnoreCase("SUCCESS"))
				res = bookingDataService.processAmClResponse(reqJson);
			
			JSONObject kafkaJson = new JSONObject();
			kafkaJson.put("BookID",reqJson.getJSONObject("responseBody").optString("bookID"));
			
			kafkaJson.put("orderID",reqJson.getJSONObject("responseBody").getString("orderID"));
			kafkaJson.put("Operation",reqJson.getJSONObject("responseBody").getString("requestType"));
			kafkaJson.put("type", reqJson.getJSONObject("responseBody").getString("type"));
			kafkaJson.put("timestamp", ZonedDateTime.now( ZoneOffset.UTC ).toString());
			kafkaJson.put("errorCode",reqJson.getJSONObject("responseBody").optString("errorCode"));
			kafkaJson.put("errorMessage", reqJson.getJSONObject("responseBody").optString("errorMessage"));
			
			kafkaService.runProducer(1, kafkaJson);

			return new ResponseEntity<String>(res, HttpStatus.OK);

		}

	}

}
