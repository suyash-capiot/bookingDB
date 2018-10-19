package com.coxandkings.travel.bookingengine.db.kafka;


import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.orchestrator.Constants;

@Service
public class KafkaBookConsumer implements Constants {

    @Autowired
    BookingListenerFactory bookingListenerFactory;

    @KafkaListener(topics = "bookingEngine")
    public void onReceiving(String payload) throws Exception {
       
    	JSONObject reqjson = new JSONObject(payload);
    	
    	JSONObject reqBody = reqjson.has(JSON_PROP_REQBODY) ? reqjson.getJSONObject(JSON_PROP_REQBODY) : reqjson.getJSONObject(JSON_PROP_RESBODY);
    	
    	if(!(reqBody.has("operation")) || reqBody.getString("operation").equalsIgnoreCase("book"))
    		bookingListenerFactory.processBooking(reqjson);
    	else
    		bookingListenerFactory.processCancelAmend(reqjson);
    	
    }


}
