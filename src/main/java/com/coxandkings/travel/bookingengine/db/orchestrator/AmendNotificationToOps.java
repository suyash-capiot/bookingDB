package com.coxandkings.travel.bookingengine.db.orchestrator;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.coxandkings.travel.bookingengine.db.kafka.KafkaBookProducer;

public class AmendNotificationToOps implements Constants{
	
	@Autowired
	@Qualifier("Kafka")
	private static KafkaBookProducer kafkaService;

	 public static void sendAmendNotificationToOps(JSONObject reqJson) throws Exception {
	 JSONObject reqToOps = new JSONObject();
	 String bookid=reqJson.getJSONObject(JSON_PROP_RESBODY).getString("bookID");
	 String orderId=reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_ORDERID);
	 String prodName=reqJson.getJSONObject(JSON_PROP_RESBODY).getString("product");
	 String op=reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_REQUESTTYPE);
	 String operationType=reqJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_TYPE);
	 reqToOps.put("BookID", bookid);
	 reqToOps.put("orderID", orderId);
	 reqToOps.put("Product", prodName);
	 reqToOps.put("Operation", op);
	 reqToOps.put("type", operationType);
	 kafkaService.runProducer(1,reqToOps);
	  }
}
