package com.coxandkings.travel.bookingengine.db.orchestrator;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coxandkings.travel.bookingengine.db.enums.OrderStatus;
import com.coxandkings.travel.bookingengine.db.exception.BookingEngineDBException;
import com.coxandkings.travel.bookingengine.db.model.AccoOrders;
import com.coxandkings.travel.bookingengine.db.model.AccoRoomDetails;
import com.coxandkings.travel.bookingengine.db.model.ActivitiesOrders;
import com.coxandkings.travel.bookingengine.db.model.Booking;
import com.coxandkings.travel.bookingengine.db.model.ClientCommercial;
import com.coxandkings.travel.bookingengine.db.model.HolidaysExtensionDetails;
import com.coxandkings.travel.bookingengine.db.model.HolidaysExtrasDetails;
import com.coxandkings.travel.bookingengine.db.model.HolidaysOrders;
import com.coxandkings.travel.bookingengine.db.model.InsuranceOrders;
import com.coxandkings.travel.bookingengine.db.model.PassengerDetails;
import com.coxandkings.travel.bookingengine.db.model.PaymentInfo;
import com.coxandkings.travel.bookingengine.db.model.ProductOrder;
import com.coxandkings.travel.bookingengine.db.model.SupplierCommercial;
import com.coxandkings.travel.bookingengine.db.model.TransfersOrders;
import com.coxandkings.travel.bookingengine.db.repository.BookingDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.HolidaysDatabaseRepository;
import com.coxandkings.travel.bookingengine.db.repository.PassengerRepository;
import com.coxandkings.travel.bookingengine.db.utils.CopyUtils;
import com.coxandkings.travel.bookingengine.db.utils.LoggerUtil;

@Service
@Qualifier("Holidays")
@Transactional(readOnly = false)
public class HolidaysDatabaseServiceImpl implements DataBaseService,Constants,ErrorConstants{

	@Autowired
	@Qualifier("Holidays")
	private HolidaysDatabaseRepository holidaysRepository;

	@Autowired
	@Qualifier("Booking")
	private BookingDatabaseRepository bookingRepository;
	
	@Autowired
	@Qualifier("BookingService")
	private BookingDatabaseService bookingService;
	
	@Autowired
    @Qualifier("Passenger")
    private PassengerRepository passengerRepository;
	
	Logger myLogger = LoggerUtil.getLoggerInstance(this.getClass());
    
    JSONObject response=new JSONObject(); 
	
	@Override
    public boolean isResponsibleFor(String product) {
		return "Holidays".equals(product);
	}


	@Override
	public String processBookRequest(JSONObject bookRequestJson) throws BookingEngineDBException {
	    
	    JSONObject bookRequestHeader = bookRequestJson.getJSONObject(JSON_PROP_REQHEADER);
		
		Booking booking = bookingRepository.findOne(bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID));
		
		if(booking==null)
		{
		  booking = bookingService.processBookRequest(bookRequestJson, true);
		}
		else
		{
		  booking.setHolidayBooking(true);
		}
		
		
		for (Object orderJson : bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getJSONArray("dynamicPackage")) {
			
		    Map<String, PassengerDetails> paxIndexMap = new HashMap<String, PassengerDetails>();
		  
			HolidaysOrders holidaysOrder = populateHolidaysData((JSONObject) orderJson, bookRequestHeader,booking,paxIndexMap);
		      
			JSONArray paxIDs = new JSONArray();
		      for (Map.Entry<String, PassengerDetails> entry : paxIndexMap.entrySet())
		      {
		        JSONObject paxJson = new JSONObject();
		        paxJson.put("paxId", entry.getValue().getPassanger_id());
		        paxIDs.put(paxJson);
		        System.out.println(entry.getKey() + "/" + entry.getValue().getPassanger_id());
		      }
		    holidaysOrder.setPaxDetails(paxIDs.toString());
			
			HolidaysOrders holidaysOrdersResponse = saveHolidaysOrder(holidaysOrder, "");
			
		}
		myLogger.info(String.format("Holidays Booking Request populated successfully for req with bookID %s = %s",bookRequestJson.getJSONObject(JSON_PROP_REQBODY).getString(JSON_PROP_BOOKID), bookRequestJson.toString()));
		return "success";
	}

	private HolidaysOrders populateHolidaysData(JSONObject holidayOrderJson, JSONObject bookRequestHeader, Booking booking,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {
		
	  try
	  {
		HolidaysOrders holidaysOrder = new HolidaysOrders();
		JSONObject suppInfoJson= holidayOrderJson.getJSONObject(JSON_PROP_HOLIDAYS_SUPPINFO);
		
		/*// Creating a random UUID (Universally unique identifier).
        UUID uuid = UUID.randomUUID();
        long now = System.currentTimeMillis();
        String id = String.format("%s-%d", uuid.toString(), now);

		holidaysOrder.setId(id);*/
		
		readPassengerDetails(holidayOrderJson, paxIndexMap);
		
		holidaysOrder.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
		holidaysOrder.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));

		holidaysOrder.setBooking(booking);
		holidaysOrder.setLastModifiedBy(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		holidaysOrder.setStatus("OnRequest");
		/*HolidaysOrder.setClientIATANumber(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTIATANUMBER));
		HolidaysOrder.setClientCurrency(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTCURRENCY));
		HolidaysOrder.setClientID(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
		HolidaysOrder.setClientType(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTTYPE));*/
		
		holidaysOrder.setSupplierID(holidayOrderJson.getString(JSON_PROP_SUPPLIERID));
		holidaysOrder.setProductSubCategory(Constants.JSON_PROP_HOLIDAYS_SUBCATEGORY);
		holidaysOrder.setOperationType("insert");
		
		holidaysOrder.setRoe(suppInfoJson.get("roe").toString());
		holidaysOrder.setSupplierName(holidayOrderJson.getString("supplierID"));
		
		//TODO:determine from where to set these fields
		holidaysOrder.setDestination("destination");
		holidaysOrder.setCountry("country");
		holidaysOrder.setCity("city");
		holidaysOrder.setNoOfNights("noOfNights");
		holidaysOrder.setSupplierReferenceNumber("supplierReferenceNumber");
		holidaysOrder.setProductName("Holidays");
		holidaysOrder.setProductFlavourName("productFlavourName");
		holidaysOrder.setPackageType("packageType");
		holidaysOrder.setCompanyPackageName("companyPackageName");
		
		//TODO: confirm from where to set this field
		holidaysOrder.setMultiCurrencyBooking("false");
		
		//TODO: for now populating all suppliers as online. Later have  this logic after operations is concrete.
		holidaysOrder.setSupplierType("online");
		
		//Total Packages Price
		holidaysOrder.setTotalPriceBeforeTax(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_PKGTOTFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
		holidaysOrder.setTotalPriceAfterTax(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_PKGTOTFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
		holidaysOrder.setTotalPriceCurrencyCode(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_PKGTOTFARE).getString(JSON_PROP_CURRENCYCODE));
		holidaysOrder.setTotalTaxAmount(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_PKGTOTFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
		holidaysOrder.setTotalPriceTaxes(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_PKGTOTFARE).getJSONObject(JSON_PROP_TAXES).toString());
		holidaysOrder.setReceivables(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_PKGTOTFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
		holidaysOrder.setCompanyTaxes(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_PKGTOTFARE).getJSONObject(JSON_PROP_COMPANYTAXES).toString());
        
        //Supplier Package Price
		holidaysOrder.setSupplierPriceBeforeTax(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
		holidaysOrder.setSupplierPriceAfterTax(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
		holidaysOrder.setSupplierPriceCurrencyCode(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
		holidaysOrder.setSupplierTaxAmount(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
		holidaysOrder.setSuppPriceTaxes(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());

		//Packages Field
        holidaysOrder.setBrandName(holidayOrderJson.getString(JSON_PROP_PKGS_BRANDNAME));
        holidaysOrder.setTourCode(holidayOrderJson.getString(JSON_PROP_PKGS_TOURCODE));
        holidaysOrder.setSubTourCode(holidayOrderJson.getString(JSON_PROP_PKGS_SUBTOURCODE));
        
        //Remove this condition once SI passes tourDetails in reprice
        if(suppInfoJson.optJSONObject(JSON_PROP_HOLIDAYS_TOURDETAILS) != null && (suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_TOURDETAILS).length() != 0)) {
        holidaysOrder.setTourName(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_TOURDETAILS).optString(JSON_PROP_PKGS_TOURNAME));
        holidaysOrder.setTourStartCity(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_TOURDETAILS).optString(JSON_PROP_PKGS_TOURSTARTCITY));
        holidaysOrder.setTourEndCity(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_TOURDETAILS).optString(JSON_PROP_PKGS_TOURENDCITY));}
        else {
        holidaysOrder.setTourName("tourName");
        holidaysOrder.setTourStartCity("tourStartCity");
        holidaysOrder.setTourEndCity("tourEndCity");}
        
        holidaysOrder.setTourStart(suppInfoJson.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART));
        holidaysOrder.setTourEnd(suppInfoJson.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND));
        holidaysOrder.setTravelStartDate(suppInfoJson.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANTRAVELSTART));
        holidaysOrder.setTravelEndDate(suppInfoJson.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANTRAVELEND));
		
		Set<SupplierCommercial> setSupplierCommercials = new HashSet<SupplierCommercial>();
		setSupplierCommercials = readSupplierCommercials(suppInfoJson.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO), holidaysOrder);
		holidaysOrder.setSuppcommercial(setSupplierCommercials);
        
        Set<ClientCommercial> setClientCommercials = new HashSet<ClientCommercial>();
        setClientCommercials = readClientCommercials(suppInfoJson.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS), holidaysOrder);
        holidaysOrder.setClientCommercial(setClientCommercials);
   
        
        //Accommodation in Packages
        AccoOrders accoOrders = new AccoOrders();
    	accoOrders = populateAccoData(holidayOrderJson, bookRequestHeader,paxIndexMap, holidaysOrder);
        holidaysOrder.setAccoOrders(accoOrders);
        
        
        //Activities in Packages
        JSONArray activitiesConfigArray = holidayOrderJson.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).optJSONArray(JSON_PROP_HOLIDAYS_ACTIVITYCOMPONENT);
        if(activitiesConfigArray != null && activitiesConfigArray.length()>0)
        {
          Set<ActivitiesOrders> activitiesOrders = new HashSet<ActivitiesOrders>();
          activitiesOrders = readActivityDetails(holidayOrderJson, bookRequestHeader,holidaysOrder,paxIndexMap);     
          holidaysOrder.setActivitiesOrders(activitiesOrders);
        }
        
        //Transfers in Packages
        JSONArray transferConfigArray = holidayOrderJson.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).optJSONArray(JSON_PROP_HOLIDAYS_TRANSFERCOMONENT);
        if(transferConfigArray != null && transferConfigArray.length()>0)
        {
          Set<TransfersOrders> transferOrders = new HashSet<TransfersOrders>();
          transferOrders = readTransferDetails(holidayOrderJson, bookRequestHeader,holidaysOrder,paxIndexMap);     
          holidaysOrder.setTransfersOrders(transferOrders);
        }
        
        //Insurance in Packages
        JSONArray insuranceConfigArray = holidayOrderJson.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).optJSONArray(JSON_PROP_HOLIDAYS_INSURANCECOMPONENT);
        if(insuranceConfigArray != null && insuranceConfigArray.length()>0)
        {
          Set<InsuranceOrders> insuranceOrders = new HashSet<InsuranceOrders>();
          insuranceOrders = readInsuranceDetails(holidayOrderJson, bookRequestHeader,holidaysOrder,paxIndexMap);     
          holidaysOrder.setInsuranceOrders(insuranceOrders);
        }
        
        //ExtensionNights in Packages
        JSONObject preNightComponent = holidayOrderJson.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).optJSONObject(JSON_PROP_HOLIDAYS_PRENIGHT);
        JSONObject postNightComponent = holidayOrderJson.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).optJSONObject(JSON_PROP_HOLIDAYS_POSTNIGHT);
        Set<HolidaysExtensionDetails> holidaysExtensionDetails = new HashSet<HolidaysExtensionDetails>();
        if(preNightComponent != null && preNightComponent.length()>0)
        {
          holidaysExtensionDetails.add(readExtensionNightDetails(preNightComponent, bookRequestHeader,holidaysOrder,paxIndexMap));
        }
        if(postNightComponent != null && postNightComponent.length()>0)
        {
          holidaysExtensionDetails.add(readExtensionNightDetails(postNightComponent, bookRequestHeader,holidaysOrder,paxIndexMap));
        }
        holidaysOrder.setHolidaysExtensionDetails(holidaysExtensionDetails);
        
        //Extras in Packages
        JSONArray extrasComponentArray = holidayOrderJson.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).optJSONArray(JSON_PROP_HOLIDAYS_EXTRACOMPONENT);
        if(extrasComponentArray != null && extrasComponentArray.length()>0)
        {
          Set<HolidaysExtrasDetails> holidaysExtrasDetails = new HashSet<HolidaysExtrasDetails>();
          holidaysExtrasDetails = readExtrasDetails(extrasComponentArray, bookRequestHeader,holidaysOrder,paxIndexMap);     
          holidaysOrder.setHolidaysExtrasDetails(holidaysExtrasDetails);
        }
        
        
		return holidaysOrder;
      }
      catch(Exception e)
      {
          myLogger.fatal("Failed to populate Holidays Data "+ e);
          e.printStackTrace();
          throw new BookingEngineDBException("Failed to populate Holidays Data");
      }
		
	}
	
	public AccoOrders populateAccoData(JSONObject requestBodyObject, JSONObject bookRequestHeader, Map<String, PassengerDetails> paxIndexMap, HolidaysOrders holidaysOrder) throws BookingEngineDBException {

      try {
    	  JSONObject hotelComponent = requestBodyObject.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).optJSONObject(JSON_PROP_HOLIDAYS_HOTELCOMPONENT);
          JSONObject cruiseComponent = requestBodyObject.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).optJSONObject(JSON_PROP_HOLIDAYS_CRUISECOMPONENT);
          JSONObject accommodationConfig = null;
          
          if(hotelComponent!= null && hotelComponent.length() != 0)
        	  accommodationConfig = hotelComponent;
          else if(cruiseComponent!= null && cruiseComponent.length() != 0)
        	  accommodationConfig = cruiseComponent;
          else 
      		  throw new Exception("Hotel/Cruise Component is missing");
  
      AccoOrders order = new AccoOrders();
      
      order.setHolidaysOrders(holidaysOrder);
      
      order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
      
      order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
      order.setLastModifiedBy(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
      order.setStatus(OrderStatus.RQ.getProductStatus());
            
      order.setSupplierID(requestBodyObject.getString(JSON_PROP_SUPPLIERID));
      order.setOperationType("insert");
      
      order.setSupplierPriceBeforeTax(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
      order.setSupplierPriceAfterTax(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
      order.setSupplierPriceCurrencyCode(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
      order.setSupplierTaxAmount(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
      order.setSuppPriceTaxes(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
      
      order.setTotalPriceBeforeTax(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
      order.setTotalPriceAfterTax(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
      order.setTotalPriceCurrencyCode(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getString(JSON_PROP_CURRENCYCODE));
      order.setTotalTaxAmount(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
      order.setTotalPriceTaxes(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getJSONObject(JSON_PROP_TAXES).toString());
      order.setReceivables(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());

      order.setSupplierCommercials(accommodationConfig.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS).toString());
      order.setClientCommercials(accommodationConfig.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS).toString());
      
      //TODO: check if we need to put taxes as well here
      Set<AccoRoomDetails> setRoomDetails = new HashSet<AccoRoomDetails>();
      if(hotelComponent!= null && hotelComponent.length() != 0) {
    	  holidaysOrder.setTourType(JSON_PROP_HOLIDAYS_LAND);
    	  order.setAccomodationType("Hotel");
    	  setRoomDetails = readHotelDetails(accommodationConfig, order,paxIndexMap);
      }      
      else {
    	  holidaysOrder.setTourType(JSON_PROP_HOLIDAYS_CRUISE);
    	  order.setAccomodationType("Cruise");
    	  order.setName(accommodationConfig.optString(JSON_PROP_PKGS_NAME));
          order.setTourCruID(accommodationConfig.optString(JSON_PROP_HOLIDAYS_ID));
    	  setRoomDetails = readCruiseDetails(accommodationConfig, order,paxIndexMap);
      }
      order.setRoomDetails(setRoomDetails);

      return order;
      }
      catch(Exception e)
      {
          
          myLogger.fatal("Failed to populate Acco Data "+ e);
          e.printStackTrace();
          throw new BookingEngineDBException("Failed to populate Acco Data");
      }
  }

	private Set<AccoRoomDetails> readCruiseDetails(JSONObject accommodationConfig, AccoOrders accoOrders, Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {
		
		  Set<AccoRoomDetails> holidaysAccoDetailsSet = new HashSet<AccoRoomDetails>();
		  JSONArray categoryOptionsArray = accommodationConfig.getJSONArray(JSON_PROP_HOLIDAYS_CATGOPTIONS);
		  
		  for(int j=0;j<categoryOptionsArray.length();j++) {
			  
			  JSONArray accommodationConfigArray = categoryOptionsArray.getJSONObject(j).getJSONArray(JSON_PROP_HOLIDAYS_CATGOPTION);
			  
		  for(int i=0;i<accommodationConfigArray.length();i++)
		  {
		    AccoRoomDetails cruiseAccoDetails = new AccoRoomDetails();
		    
		    JSONObject currentAccommodationObj = accommodationConfigArray.getJSONObject(i);
		    
		    cruiseAccoDetails.setAccoOrders(accoOrders);
		    
		    cruiseAccoDetails.setAccomodationType(JSON_PROP_HOLIDAYS_CRUISE);
		    
		    cruiseAccoDetails.setSupplierPriceBeforeTax(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
		    cruiseAccoDetails.setSupplierPriceAfterTax(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
		    cruiseAccoDetails.setSupplierPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
		    cruiseAccoDetails.setSupplierTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
		    cruiseAccoDetails.setSupplierTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
		    cruiseAccoDetails.setSuppPaxTypeFares(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
		    
		    cruiseAccoDetails.setTotalPriceBeforeTax(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
		    cruiseAccoDetails.setTotalPriceAfterTax(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
	        cruiseAccoDetails.setTotalPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
	        cruiseAccoDetails.setTotalTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
	        cruiseAccoDetails.setTotalTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
	        cruiseAccoDetails.setReceivables(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
	        cruiseAccoDetails.setTotalPaxTypeFares(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
	        
	        //cruiseAccoDetails.setReceivables(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_RECEIVABLES).toString());
	        
	        cruiseAccoDetails.setSuppCommercials(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS).toString());
	        cruiseAccoDetails.setClientCommercials(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS).toString());
	        
	        cruiseAccoDetails.setAvailabilityStatus(currentAccommodationObj.getString(JSON_PROP_PKGS_AVAILABILITYSTATUS));
	        if(currentAccommodationObj.optJSONArray("occupancyInfo") != null)
	        cruiseAccoDetails.setOccupancyInfo(currentAccommodationObj.optJSONArray("occupancyInfo").toString());
	        
	        //cruiseAccoDetails.setHotelInfo(currentAccommodationObj.getJSONArray(JSON_PROP_PKGS_ACCOHOTELINFO).toString());
	        /*holidaysAccoDetails.setHotelCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString(JSON_PROP_ACCO_HOTELCODE));
	        holidaysAccoDetails.setHotelName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString(JSON_PROP_ACCO_HOTELNAME));
	        holidaysAccoDetails.setHotelRef(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString("hotelRef"));
	        holidaysAccoDetails.setHotelSegmentCategoryCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString("hotelSegmentCategoryCode"));
	        */
	        
	        //cruiseAccoDetails.setAddress(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
	        
	        cruiseAccoDetails.setRoomType(currentAccommodationObj.getString(JSON_PROP_HOLIDAYS_CABINTYPE));
	        cruiseAccoDetails.setRoomCategory(currentAccommodationObj.getString(JSON_PROP_HOLIDAYS_CABINCATEGORY));
	        cruiseAccoDetails.setRoomName(currentAccommodationObj.optString(JSON_PROP_PKGS_DESCRIPTION));
	        cruiseAccoDetails.setCabinNumber(currentAccommodationObj.getJSONArray(JSON_PROP_HOLIDAYS_CABINOPTION).getJSONObject(0).getString(JSON_PROP_PKGS_ACCOCABINNUMBER));
	        //cruiseAccoDetails.setInvBlockCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOROOMTYPEINFO).getString(JSON_PROP_PKGS_ACCOBLOCKCODE));
	        
	        
	        //cruiseAccoDetails.setRatePlanName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_RATEPLANNAME));
	        //cruiseAccoDetails.setRatePlanCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_RATEPLANCODE));
	        //cruiseAccoDetails.setBookingRef(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_BOOKINGREF));
	        
	        cruiseAccoDetails.setStart(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART));
	        cruiseAccoDetails.setDuration(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANDURATION));
	        cruiseAccoDetails.setEnd(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND));
	        
	        cruiseAccoDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
	        cruiseAccoDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
	        
	        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
	        setGuestDetails = readPassengerDetails(currentAccommodationObj, paxIndexMap);
	        
	        JSONArray paxIDs = new JSONArray();
	        for(PassengerDetails paxID : setGuestDetails ) {
	            JSONObject paxJson = new JSONObject();
	            paxJson.put("paxId", paxID.getPassanger_id());
	            paxIDs.put(paxJson);
	        }
	        cruiseAccoDetails.setPaxDetails(paxIDs.toString());
	        
	        holidaysAccoDetailsSet.add(cruiseAccoDetails);
		  }}
		  
			return holidaysAccoDetailsSet;
		}


	public Map<String, Object> getPaxIndexMap(JSONObject componentConfigJson, Map<String, Object> paxIndexMap) {

	//MultiValueMap<String, String> paxComponentWise = new LinkedMultiValueMap<String, String>();
    
    JSONArray paxInfoArray = componentConfigJson.getJSONArray(JSON_PROP_PAXINFO);
      
    //List<String> resGuestList = new ArrayList<String>();
      
    for (int j = 0; j < paxInfoArray.length(); j++) 
    {
      JSONObject currentPax = paxInfoArray.getJSONObject(j);
        
      String resGuestRPH = currentPax.getString(JSON_PROP_PKGS_RESGUESTRPH); 

      if(paxIndexMap.containsKey(resGuestRPH))
      {
        continue;
      }
      else
      {
        paxIndexMap.put(resGuestRPH, currentPax);
      }
       
    }

    return paxIndexMap;
}
	
	private Set<PassengerDetails> readPassengerDetails(JSONObject componentConfigJson,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {
		
		JSONArray paxInfoArray = null;
		JSONArray resGuests = componentConfigJson.optJSONArray(JSON_PROP_HOLIDAYS_RESGUESTS);
		if(!(resGuests==null) && resGuests.length()!=0)
			paxInfoArray = resGuests;
		else
			paxInfoArray = componentConfigJson.getJSONArray(JSON_PROP_HOLIDAYS_GUESTCOUNT);
		
      Set<PassengerDetails> HolidaysPassengerDetailsSet = new HashSet<PassengerDetails>();
      
      for (int i = 0; i < paxInfoArray.length(); i++) {
          
        JSONObject currentPaxDetails = paxInfoArray.getJSONObject(i);
        
        String resGuestRPH = currentPaxDetails.get(JSON_PROP_PKGS_RESGUESTRPH).toString(); 
        
        PassengerDetails passengerDetailsResponse = new PassengerDetails();

        if(paxIndexMap.containsKey(resGuestRPH))
        {
          passengerDetailsResponse = paxIndexMap.get(resGuestRPH);
        }
        else
        {
          PassengerDetails resGuestDetails = new PassengerDetails();
          
          resGuestDetails.setTitle(currentPaxDetails.optString(JSON_PROP_TITLE));
          resGuestDetails.setFirstName(currentPaxDetails.optString(JSON_PROP_FIRSTNAME));
          resGuestDetails.setMiddleName(currentPaxDetails.optString(JSON_PROP_MIDDLENAME));
          resGuestDetails.setLastName(currentPaxDetails.optString(JSON_PROP_LASTNAME));
          resGuestDetails.setBirthDate(currentPaxDetails.optString(JSON_PROP_DOB));
          resGuestDetails.setIsLeadPax(currentPaxDetails.optBoolean(JSON_PROP_HOLIDAYS_PRIMARYIND));
          resGuestDetails.setGender(currentPaxDetails.optString(JSON_PROP_GENDER));
          resGuestDetails.setAge(currentPaxDetails.optString(JSON_PROP_HOLIDAYS_AGE));
          resGuestDetails.setPaxType(currentPaxDetails.optString(JSON_PROP_PAX_TYPE));
          
          resGuestDetails.setContactDetails(currentPaxDetails.getJSONArray(JSON_PROP_CONTACTDETAILS).toString());
          resGuestDetails.setAddressDetails(currentPaxDetails.getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
          
          resGuestDetails.setDocumentDetails(currentPaxDetails.getJSONArray(JSON_PROP_HOLIDAYS_DOCINFO).toString());
          resGuestDetails.setSpecialRequests(currentPaxDetails.getJSONObject(JSON_PROP_SPECIALREQUESTS).getJSONArray(JSON_PROP_SPECIALREQUESTINFO).toString());
          
          resGuestDetails.setLastModifiedBy("");
          resGuestDetails.setCreatedAt(ZonedDateTime.now(ZoneOffset.UTC));
          resGuestDetails.setLastModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
          
          resGuestDetails.setRph(currentPaxDetails.getString(JSON_PROP_PKGS_RESGUESTRPH));
          resGuestDetails.setIsHolidayPassenger(true);
          
          passengerDetailsResponse = savePaxDetails(resGuestDetails,"");
          
          paxIndexMap.put(resGuestRPH, passengerDetailsResponse);
 
        }
          
        HolidaysPassengerDetailsSet.add(passengerDetailsResponse);

      }
      return HolidaysPassengerDetailsSet;
	}

	private Set<ActivitiesOrders> readActivityDetails(JSONObject requestBody, JSONObject bookRequestHeader, HolidaysOrders holidaysOrders,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {

	  JSONArray activitiesConfigArray = requestBody.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).getJSONArray(JSON_PROP_HOLIDAYS_ACTIVITYCOMPONENT);
      
      Set<ActivitiesOrders> holidaysActivitiesDetailsSet = new HashSet<ActivitiesOrders>();
      
      for(int i=0;i<activitiesConfigArray.length();i++)
      {
        ActivitiesOrders holidaysActivitiesDetails = new ActivitiesOrders();
        
        JSONObject currentActivityObj = activitiesConfigArray.getJSONObject(i);
        
        holidaysActivitiesDetails.setHolidaysOrders(holidaysOrders);
        
        holidaysActivitiesDetails.setConfigType(currentActivityObj.optString(JSON_PROP_PKGS_CONFIGTYPE));
        holidaysActivitiesDetails.setActivityType(currentActivityObj.getString(JSON_PROP_PKGS_ACTIVITYTYPE));
        
        holidaysActivitiesDetails.setSupplierPriceBeforeTax(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
        holidaysActivitiesDetails.setSupplierPriceAfterTax(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
        holidaysActivitiesDetails.setSupplierPriceCurrencyCode(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
        holidaysActivitiesDetails.setSupplierTaxAmount(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysActivitiesDetails.setSupplierTaxBreakup(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
        holidaysActivitiesDetails.setSuppPaxTypeFares(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
        
        holidaysActivitiesDetails.setTotalPriceBeforeTax(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
        holidaysActivitiesDetails.setTotalPriceAfterTax(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
        holidaysActivitiesDetails.setTotalPriceCurrencyCode(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
        holidaysActivitiesDetails.setTotalTaxAmount(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysActivitiesDetails.setTotalTaxBreakup(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
        holidaysActivitiesDetails.setTotalPaxTypeFares(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
        holidaysActivitiesDetails.setTotalPriceReceivables(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
        
        holidaysActivitiesDetails.setSupplierCommercials(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS).toString());
        holidaysActivitiesDetails.setClientCommercials(currentActivityObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS).toString());
                
        holidaysActivitiesDetails.setAvailabilityStatus(currentActivityObj.getString(JSON_PROP_PKGS_AVAILABILITYSTATUS));
        
        holidaysActivitiesDetails.setName(currentActivityObj.optJSONObject(JSON_PROP_PKGS_ACTIVITYDETAILS).getString(JSON_PROP_PKGS_NAME));
        holidaysActivitiesDetails.setActivityCode(currentActivityObj.optJSONObject(JSON_PROP_PKGS_ACTIVITYDETAILS).getString(JSON_PROP_PKGS_CODE));
        holidaysActivitiesDetails.setQuantity(currentActivityObj.optJSONObject(JSON_PROP_PKGS_ACTIVITYDETAILS).getString(JSON_PROP_PKGS_QUANTITY));
        holidaysActivitiesDetails.setDescription(currentActivityObj.optJSONObject(JSON_PROP_PKGS_ACTIVITYDETAILS).getString(JSON_PROP_PKGS_DESCRIPTION));
        holidaysActivitiesDetails.setActivityType(currentActivityObj.optJSONObject(JSON_PROP_PKGS_ACTIVITYDETAILS).getString(JSON_PROP_PKGS_TYPE));
        
        if(currentActivityObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART)!=null && currentActivityObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART).length() > 0)
        holidaysActivitiesDetails.setStartDate(readStartDateEndDate(currentActivityObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART)));
        else
        	holidaysActivitiesDetails.setStartDate(readStartDateEndDate("0000-00-00"));
        holidaysActivitiesDetails.setDuration(currentActivityObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANDURATION));
        
        if(currentActivityObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND)!=null && currentActivityObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND).length() > 0)
        holidaysActivitiesDetails.setEndDate(readStartDateEndDate(currentActivityObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND)));
        else
        	holidaysActivitiesDetails.setEndDate(readStartDateEndDate("0000-00-00"));
        
        holidaysActivitiesDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        holidaysActivitiesDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
        setGuestDetails = readPassengerDetails(currentActivityObj, paxIndexMap);
        
        JSONArray paxIDs = new JSONArray();
        for(PassengerDetails paxID : setGuestDetails ) {
            JSONObject paxJson = new JSONObject();
            paxJson.put("paxId", paxID.getPassanger_id());
            paxIDs.put(paxJson);
        }
        holidaysActivitiesDetails.setPaxDetails(paxIDs.toString());
        
        holidaysActivitiesDetailsSet.add(holidaysActivitiesDetails);
      }
      
        return holidaysActivitiesDetailsSet;
	}

	private Set<InsuranceOrders> readInsuranceDetails(JSONObject requestBody, JSONObject bookRequestHeader, HolidaysOrders holidaysOrders,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException  {
	   
      JSONArray insuranceConfigArray = requestBody.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).getJSONArray(JSON_PROP_HOLIDAYS_INSURANCECOMPONENT);
      
      Set<InsuranceOrders> holidaysInsuranceDetailsSet = new HashSet<InsuranceOrders>();
      
      for(int i=0;i<insuranceConfigArray.length();i++)
      {
        InsuranceOrders holidaysInsuranceDetails = new InsuranceOrders();
        
        JSONObject currentInsuranceObj = insuranceConfigArray.getJSONObject(i);
        
        holidaysInsuranceDetails.setHolidaysOrders(holidaysOrders);
        
        holidaysInsuranceDetails.setConfigType(currentInsuranceObj.optString(JSON_PROP_PKGS_CONFIGTYPE));
        holidaysInsuranceDetails.setInsuranceType(currentInsuranceObj.getJSONObject(JSON_PROP_PKGS_INSURANCEINFO).optString(JSON_PROP_PKGS_NAME));
        
        holidaysInsuranceDetails.setSupplierPriceBeforeTax(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
        holidaysInsuranceDetails.setSupplierPriceAfterTax(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
        holidaysInsuranceDetails.setSupplierPriceCurrencyCode(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
        holidaysInsuranceDetails.setSupplierTaxAmount(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysInsuranceDetails.setSupplierTaxBreakup(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
        holidaysInsuranceDetails.setSuppPaxTypeFares(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
	    
        holidaysInsuranceDetails.setTotalPriceBeforeTax(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
        holidaysInsuranceDetails.setTotalPriceAfterTax(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
        holidaysInsuranceDetails.setTotalPriceCurrencyCode(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
        holidaysInsuranceDetails.setTotalTaxAmount(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysInsuranceDetails.setTotalTaxBreakup(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
        holidaysInsuranceDetails.setTotalPaxTypeFares(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
        holidaysInsuranceDetails.setReceivables(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
        
        holidaysInsuranceDetails.setSupplierCommercials(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS).toString());
        holidaysInsuranceDetails.setClientCommercials(currentInsuranceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS).toString());
       
        holidaysInsuranceDetails.setInsDescription(currentInsuranceObj.getJSONObject(JSON_PROP_PKGS_INSURANCEINFO).optString(JSON_PROP_PKGS_NAME));
        holidaysInsuranceDetails.setInsId(currentInsuranceObj.getJSONObject(JSON_PROP_PKGS_INSURANCEINFO).optString(JSON_PROP_PKGS_DESCRIPTION));
        holidaysInsuranceDetails.setInsName(currentInsuranceObj.getJSONObject(JSON_PROP_PKGS_INSURANCEINFO).optString(JSON_PROP_HOLIDAYS_ID));
        
        holidaysInsuranceDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        holidaysInsuranceDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
        setGuestDetails = readPassengerDetails(currentInsuranceObj, paxIndexMap);
        
        JSONArray paxIDs = new JSONArray();
        for(PassengerDetails paxID : setGuestDetails ) {
            JSONObject paxJson = new JSONObject();
            paxJson.put("paxId", paxID.getPassanger_id());
            paxIDs.put(paxJson);
        }
        holidaysInsuranceDetails.setPaxDetails(paxIDs.toString());
        
        holidaysInsuranceDetailsSet.add(holidaysInsuranceDetails);
      }
      
        return holidaysInsuranceDetailsSet;
	}

	private Set<TransfersOrders> readTransferDetails(JSONObject requestBody, JSONObject bookRequestHeader, HolidaysOrders holidaysOrders,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {
	   
	  JSONArray transferConfigArray = requestBody.getJSONObject(JSON_PROP_HOLIDAYS_COMPONENTS).getJSONArray(JSON_PROP_HOLIDAYS_TRANSFERCOMONENT);
      
      Set<TransfersOrders> holidaysTransferDetailsSet = new HashSet<TransfersOrders>();
      
      for(int i=0;i<transferConfigArray.length();i++)
      {
        TransfersOrders holidaysTransferDetails = new TransfersOrders();
        
        JSONObject currentTransferObj = transferConfigArray.getJSONObject(i);
       
        JSONArray groundService = currentTransferObj.getJSONArray("groundService");
        
        for(int j =0; j <groundService.length(); j++) {
        	
        	JSONObject currentGroundServiceObj = groundService.getJSONObject(j);
        	
	        holidaysTransferDetails.setHolidaysOrders(holidaysOrders);
	        
	        holidaysTransferDetails.setConfigType(currentTransferObj.optString("dynamicPkgStatus"));
	        holidaysTransferDetails.setTransferType(currentTransferObj.getString("dynamicPkgAction"));
	        holidaysTransferDetails.setAvailabilityStatus(currentTransferObj.optString(JSON_PROP_PKGS_AVAILABILITYSTATUS));
	        
	        holidaysTransferDetails.setSupplierPriceBeforeTax(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
	        holidaysTransferDetails.setSupplierPriceAfterTax(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
	        holidaysTransferDetails.setSupplierPriceCurrencyCode(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
	        holidaysTransferDetails.setSupplierTaxAmount(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
	        holidaysTransferDetails.setSupplierTaxBreakup(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
	        holidaysTransferDetails.setSuppPaxTypeFares(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
		    
	        holidaysTransferDetails.setTotalPriceBeforeTax(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
	        holidaysTransferDetails.setTotalPriceAfterTax(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
	        holidaysTransferDetails.setTotalPriceCurrencyCode(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
	        holidaysTransferDetails.setTotalTaxAmount(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
	        holidaysTransferDetails.setTotalTaxBreakup(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
	        holidaysTransferDetails.setTotalPaxTypeFares(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
	        holidaysTransferDetails.setReceivables(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
	        
	        holidaysTransferDetails.setSupplierCommercials(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS).toString());
	        holidaysTransferDetails.setClientCommercialss(currentGroundServiceObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS).toString());
	        
	        holidaysTransferDetails.setPickUpLocation(currentGroundServiceObj.getJSONObject(JSON_PROP_PKGS_TRANSFERLOCATION).getString(JSON_PROP_PKGS_TRANSFERPICKUPLOCATION));
	        holidaysTransferDetails.setAirportName(currentGroundServiceObj.getJSONObject(JSON_PROP_PKGS_TRANSFERLOCATION).getString(JSON_PROP_PKGS_TRANSFERAIRPORTNAME));
	        
	        holidaysTransferDetails.setTransferName(currentGroundServiceObj.getString(JSON_PROP_PKGS_NAME));
	        holidaysTransferDetails.setTransferDescription(currentGroundServiceObj.getString(JSON_PROP_PKGS_DESCRIPTION));
	        holidaysTransferDetails.setDepartureCity(currentGroundServiceObj.getString(JSON_PROP_PKGS_TRANSFERDEPARTURECITY));
	        holidaysTransferDetails.setArrivalCity(currentGroundServiceObj.getString(JSON_PROP_PKGS_TRANSFERARRIVALCITY));
	        holidaysTransferDetails.setDepartureDate(currentGroundServiceObj.getString(JSON_PROP_PKGS_TRANSFERDEPARTUREDATE));
	        holidaysTransferDetails.setArrivalDate(currentGroundServiceObj.getString(JSON_PROP_PKGS_TRANSFERARRIVALDATE));
	        
	        holidaysTransferDetails.setStart(currentGroundServiceObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANSTART));
	        holidaysTransferDetails.setDuration(currentGroundServiceObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANDURATION));
	        holidaysTransferDetails.setEnd(currentGroundServiceObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).getString(JSON_PROP_PKGS_TIMESPANEND));
	        
	        holidaysTransferDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
	        holidaysTransferDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
	        
	        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
	        setGuestDetails = readPassengerDetails(currentGroundServiceObj, paxIndexMap);
	        
	        JSONArray paxIDs = new JSONArray();
	        for(PassengerDetails paxID : setGuestDetails ) {
	            JSONObject paxJson = new JSONObject();
	            paxJson.put("paxId", paxID.getPassanger_id());
	            paxIDs.put(paxJson);
	        }
	        holidaysTransferDetails.setPaxDetails(paxIDs.toString());
	        
	        holidaysTransferDetailsSet.add(holidaysTransferDetails);
        	}
        }
      
        return holidaysTransferDetailsSet;
	}


	private Set<HolidaysExtrasDetails> readExtrasDetails(JSONArray extrasConfigArray, JSONObject bookRequestHeader, HolidaysOrders holidaysOrders,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {

      Set<HolidaysExtrasDetails> holidaysExtrasDetailsSet = new HashSet<HolidaysExtrasDetails>();
      
      for(int i=0;i<extrasConfigArray.length();i++)
      {
        HolidaysExtrasDetails holidaysExtrasDetails = new HolidaysExtrasDetails();
        
        JSONObject currentExtraObj = extrasConfigArray.getJSONObject(i);
        
        holidaysExtrasDetails.setHolidaysOrders(holidaysOrders);
        
        //holidaysExtrasDetails.setConfigType(currentExtraObj.optString(JSON_PROP_PKGS_CONFIGTYPE));
        holidaysExtrasDetails.setExtraType(currentExtraObj.optString(JSON_PROP_PKGS_TYPE));
        
        holidaysExtrasDetails.setSupplierPriceBeforeTax(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
        holidaysExtrasDetails.setSupplierPriceAfterTax(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
        holidaysExtrasDetails.setSupplierPriceCurrencyCode(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
        holidaysExtrasDetails.setSupplierTaxAmount(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysExtrasDetails.setSupplierTaxBreakup(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
        holidaysExtrasDetails.setSupplierPaxTypeFares(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
        
        holidaysExtrasDetails.setTotalPriceBeforeTax(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
        holidaysExtrasDetails.setTotalPriceAfterTax(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
        holidaysExtrasDetails.setTotalPriceCurrencyCode(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
        holidaysExtrasDetails.setTotalTaxAmount(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysExtrasDetails.setTotalTaxBreakup(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
        holidaysExtrasDetails.setTotalPaxTypeFares(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
        if(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).optJSONObject(JSON_PROP_RECEIVABLES) != null)
        holidaysExtrasDetails.setReceivables(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
        
        holidaysExtrasDetails.setSupplierCommercials(currentExtraObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS).toString());
        holidaysExtrasDetails.setClientCommercials(currentExtraObj.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS).toString());
        
        holidaysExtrasDetails.setAvailabilityStatus(currentExtraObj.optString(JSON_PROP_PKGS_AVAILABILITYSTATUS));
        
        holidaysExtrasDetails.setExtraName(currentExtraObj.getString("feeName"));
        holidaysExtrasDetails.setExtraCode(currentExtraObj.getString("feeCode"));
        holidaysExtrasDetails.setExtraQuantity(currentExtraObj.optString(JSON_PROP_PKGS_QUANTITY));
        holidaysExtrasDetails.setExtraDescription(currentExtraObj.getString("text"));
       
        holidaysExtrasDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        holidaysExtrasDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
        setGuestDetails = readPassengerDetails(currentExtraObj, paxIndexMap);
        
        JSONArray paxIDs = new JSONArray();
        for(PassengerDetails paxID : setGuestDetails ) {
            JSONObject paxJson = new JSONObject();
            paxJson.put("paxId", paxID.getPassanger_id());
            paxIDs.put(paxJson);
        }
        holidaysExtrasDetails.setPaxDetails(paxIDs.toString());
        
        holidaysExtrasDetailsSet.add(holidaysExtrasDetails);
      }
      
        return holidaysExtrasDetailsSet;
	}


	private HolidaysExtensionDetails readExtensionNightDetails(JSONObject extensionNight, JSONObject bookRequestHeader, HolidaysOrders holidaysOrders,Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException{
		try {
			
	      HolidaysExtensionDetails order = new HolidaysExtensionDetails();
	      
	      order.setHolidaysOrders(holidaysOrders);
	        
	      //order.setConfigType(extensionNight.getString(JSON_PROP_PKGS_CONFIGTYPE));
	      String dynamicPkgAction = extensionNight.getString("dynamicPkgAction");
	      if(dynamicPkgAction.toLowerCase().contains("prenight"))
	    	  order.setExtensionType(JSON_PROP_HOLIDAYS_PRENIGHT);
	      else
	    	  order.setExtensionType(JSON_PROP_HOLIDAYS_POSTNIGHT);
	      
	      order.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
	      
	      order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
	      order.setLastModifiedBy(bookRequestHeader.getJSONObject(JSON_PROP_CLIENTCONTEXT).getString(JSON_PROP_CLIENTID));
	      order.setStatus("OnRequest");
	      
	      order.setOperationType("insert");
	      
	      order.setSupplierPriceBeforeTax(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
	      order.setSupplierPriceAfterTax(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
	      order.setSupplierPriceCurrencyCode(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
	      order.setSupplierTaxAmount(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
	      order.setSupplierTaxBreakup(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
	      
	      order.setTotalPriceBeforeTax(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
	      order.setTotalPriceAfterTax(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
	      order.setTotalPriceCurrencyCode(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getString(JSON_PROP_CURRENCYCODE));
	      order.setTotalTaxAmount(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
	      order.setTotalTaxBreakup(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getJSONObject(JSON_PROP_TAXES).toString());
	      order.setReceivables(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_COMPTOTFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
	      
	      order.setSupplierCommercials(extensionNight.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS).toString());
	      order.setClientCommercials(extensionNight.getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS).toString());
	      
	      //TODO: check if we need to put taxes as well here
	      Set<AccoRoomDetails> setRoomDetails = new HashSet<AccoRoomDetails>();
	      setRoomDetails = readExtensionRoomDetails(extensionNight, order,paxIndexMap);
	      order.setRoomDetails(setRoomDetails);
	      
	      return order;
	      }
	      catch(Exception e)
	      {
	          
	          myLogger.fatal("Failed to populate Acco Data "+ e);
	          throw new BookingEngineDBException("Failed to populate Acco Data");
	      }
	      }

	private Set<AccoRoomDetails> readExtensionRoomDetails(JSONObject extensionNight, HolidaysExtensionDetails order,
			Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {
		
		  JSONArray roomStayArray = extensionNight.getJSONObject("roomStays").getJSONArray("roomStay");
		  
		  Set<AccoRoomDetails> holidaysAccoDetailsSet = new HashSet<AccoRoomDetails>();
		  
		  for(int i=0;i<roomStayArray.length();i++)
		  {
		    AccoRoomDetails holidaysAccoDetails = new AccoRoomDetails();
		    
		    JSONObject roomStayObj = roomStayArray.getJSONObject(i);
		    
		    holidaysAccoDetails.setExtensionOrders(order);
		    
		    holidaysAccoDetails.setSupplierPriceBeforeTax(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
		    holidaysAccoDetails.setSupplierPriceAfterTax(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
		    holidaysAccoDetails.setSupplierPriceCurrencyCode(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
		    holidaysAccoDetails.setSupplierTaxAmount(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
		    holidaysAccoDetails.setSupplierTaxBreakup(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
		    holidaysAccoDetails.setSuppPaxTypeFares(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
		    
		    holidaysAccoDetails.setTotalPriceBeforeTax(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
		    holidaysAccoDetails.setTotalPriceAfterTax(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
	        holidaysAccoDetails.setTotalPriceCurrencyCode(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
	        holidaysAccoDetails.setTotalTaxAmount(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
	        holidaysAccoDetails.setTotalTaxBreakup(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
	        holidaysAccoDetails.setTotalPaxTypeFares(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
		    holidaysAccoDetails.setReceivables(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
	        
	        holidaysAccoDetails.setSuppCommercials(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS).toString());
	        holidaysAccoDetails.setClientCommercials(roomStayObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS).toString());
	        
	        holidaysAccoDetails.setAvailabilityStatus(roomStayObj.optString("roomStayStatus"));
	        
	        holidaysAccoDetails.setHotelInfo(roomStayObj.getJSONArray("basicPropertyInfo").toString());

	        //TODO Confirm from where will we get this?
	        //holidaysAccoDetails.setAddress(roomStayObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
	        
	        if(roomStayObj.optJSONArray("occupancyInfo") != null)
	        holidaysAccoDetails.setOccupancyInfo(roomStayObj.getJSONArray("occupancyInfo").toString());
	        
	        //TODO Confirm from where will we get this?
	        holidaysAccoDetails.setRoomType(roomStayObj.optString("roomType"));
	        holidaysAccoDetails.setRoomCategory(roomStayObj.getString("roomCategory"));
	        //holidaysAccoDetails.setRoomName(roomStayObj.optString("roomType"));
	        
	        //TODO Confirm from where will we get this?
	        //holidaysAccoDetails.setRatePlanName(roomStayObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_RATEPLANNAME));
	        holidaysAccoDetails.setRatePlanCode(roomStayObj.optString("ratePlanCategory"));
	        //holidaysAccoDetails.setBookingRef(roomStayObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_BOOKINGREF));
	        
	        holidaysAccoDetails.setStart(roomStayObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).optString(JSON_PROP_PKGS_TIMESPANSTART));
	        holidaysAccoDetails.setDuration(roomStayObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).optString(JSON_PROP_PKGS_TIMESPANDURATION));
	        holidaysAccoDetails.setEnd(roomStayObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).optString(JSON_PROP_PKGS_TIMESPANEND));
	        
	        holidaysAccoDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
	        holidaysAccoDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
	        
	        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
	        setGuestDetails = readPassengerDetails(roomStayObj, paxIndexMap);
	        
	        JSONArray paxIDs = new JSONArray();
	        for(PassengerDetails paxID : setGuestDetails ) {
	            JSONObject paxJson = new JSONObject();
	            paxJson.put("paxId", paxID.getPassanger_id());
	            paxIDs.put(paxJson);
	        }
	        holidaysAccoDetails.setPaxDetails(paxIDs.toString());
	        
	        holidaysAccoDetailsSet.add(holidaysAccoDetails);
		  }
		  
			return holidaysAccoDetailsSet;
		}


	private Set<AccoRoomDetails> readHotelDetails(JSONObject accommodationConfig, AccoOrders accoOrders, Map<String, PassengerDetails> paxIndexMap) throws BookingEngineDBException {
		
	  JSONArray accommodationConfigArray = accommodationConfig.getJSONObject("roomStays").getJSONArray("roomStay");
	  
	  Set<AccoRoomDetails> holidaysAccoDetailsSet = new HashSet<AccoRoomDetails>();
	  
	  for(int i=0;i<accommodationConfigArray.length();i++)
	  {
	    AccoRoomDetails holidaysAccoDetails = new AccoRoomDetails();
	    
	    JSONObject currentAccommodationObj = accommodationConfigArray.getJSONObject(i);
	    
	    holidaysAccoDetails.setAccoOrders(accoOrders);
	    
	    holidaysAccoDetails.setAccomodationType("hotel");
	    
	    holidaysAccoDetails.setSupplierPriceBeforeTax(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
	    holidaysAccoDetails.setSupplierPriceAfterTax(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
	    holidaysAccoDetails.setSupplierPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
	    holidaysAccoDetails.setSupplierTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
	    holidaysAccoDetails.setSupplierTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
	    holidaysAccoDetails.setSuppPaxTypeFares(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
	    
	    holidaysAccoDetails.setTotalPriceBeforeTax(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTBEFORETAX).toString());
	    holidaysAccoDetails.setTotalPriceAfterTax(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getBigDecimal(JSON_PROP_HOLIDAYS_AMOUNTAFTERTAX).toString());
        holidaysAccoDetails.setTotalPriceCurrencyCode(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getString(JSON_PROP_CURRENCYCODE));
        holidaysAccoDetails.setTotalTaxAmount(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).getBigDecimal(JSON_PROP_AMOUNT).toString());
        holidaysAccoDetails.setTotalTaxBreakup(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_TAXES).toString());
        holidaysAccoDetails.setReceivables(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONObject(JSON_PROP_TOTALFARE).getJSONObject(JSON_PROP_RECEIVABLES).toString());
        holidaysAccoDetails.setTotalPaxTypeFares(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_HOLIDAYS_PAXTYPEFARES).toString());
                
        holidaysAccoDetails.setSuppCommercials(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_SUPPLIERPRICINGINFO).getJSONArray(JSON_PROP_SUPPCOMMTOTALS).toString());
        holidaysAccoDetails.setClientCommercials(currentAccommodationObj.getJSONObject(JSON_PROP_HOLIDAYS_TOTALPRICINGINFO).getJSONArray(JSON_PROP_CLIENTENTITYTOTALCOMMERCIALS).toString());
        
        holidaysAccoDetails.setAvailabilityStatus(currentAccommodationObj.optString("roomStayStatus"));
        
        holidaysAccoDetails.setHotelInfo(currentAccommodationObj.getJSONArray("basicPropertyInfo").toString());
        /*holidaysAccoDetails.setHotelCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString(JSON_PROP_ACCO_HOTELCODE));
        holidaysAccoDetails.setHotelName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString(JSON_PROP_ACCO_HOTELNAME));
        holidaysAccoDetails.setHotelRef(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString("hotelRef"));
        holidaysAccoDetails.setHotelSegmentCategoryCode(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCOHOTELINFO).getString("hotelSegmentCategoryCode"));
        */
        if(currentAccommodationObj.optJSONArray("occupancyInfo") != null)
        holidaysAccoDetails.setOccupancyInfo(currentAccommodationObj.optJSONArray("occupancyInfo").toString());
        //TODO Confirm from where will we get this?
        //holidaysAccoDetails.setAddress(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_ADDRESSDETAILS).toString());
        
        //TODO Confirm from where will we get this?
        holidaysAccoDetails.setRoomType(currentAccommodationObj.optString("roomType"));
        holidaysAccoDetails.setRoomCategory(currentAccommodationObj.getString("roomCategory"));
        //holidaysAccoDetails.setRoomName(currentAccommodationObj.optString("roomType"));
        
        //TODO Confirm from where will we get this?
        //holidaysAccoDetails.setRatePlanName(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_RATEPLANNAME));
        holidaysAccoDetails.setRatePlanCode(currentAccommodationObj.optString("ratePlanCategory"));
        //holidaysAccoDetails.setBookingRef(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_ACCOROOMINFO).getJSONObject(JSON_PROP_PKGS_ACCORATEPLANINFO).getString(JSON_PROP_ACCO_BOOKINGREF));
        
        if(currentAccommodationObj.has(JSON_PROP_PKGS_TIMESPAN)) {
        holidaysAccoDetails.setStart(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).optString(JSON_PROP_PKGS_TIMESPANSTART));
        holidaysAccoDetails.setDuration(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).optString(JSON_PROP_PKGS_TIMESPANDURATION));
        holidaysAccoDetails.setEnd(currentAccommodationObj.getJSONObject(JSON_PROP_PKGS_TIMESPAN).optString(JSON_PROP_PKGS_TIMESPANEND));
        }
        
        holidaysAccoDetails.setCreatedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        holidaysAccoDetails.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        
        Set<PassengerDetails> setGuestDetails = new HashSet<PassengerDetails>();
        setGuestDetails = readPassengerDetails(currentAccommodationObj, paxIndexMap);
        
        JSONArray paxIDs = new JSONArray();
        for(PassengerDetails paxID : setGuestDetails ) {
            JSONObject paxJson = new JSONObject();
            paxJson.put("paxId", paxID.getPassanger_id());
            paxIDs.put(paxJson);
        }
        holidaysAccoDetails.setPaxDetails(paxIDs.toString());
        
        holidaysAccoDetailsSet.add(holidaysAccoDetails);
	  }
	  
		return holidaysAccoDetailsSet;
	}
	
	public Set<SupplierCommercial> readSupplierCommercials(JSONObject supplierPricingInfo, Object order1) {
			ProductOrder order = (ProductOrder)order1;
		
	  JSONArray suppCommsJsonArray = supplierPricingInfo.getJSONArray(JSON_PROP_SUPPCOMMTOTALS);
      Set<SupplierCommercial> suppCommercialsSet = new HashSet<SupplierCommercial>();
      SupplierCommercial suppCommercials;
      for (int i = 0; i < suppCommsJsonArray.length(); i++) {
          JSONObject suppComm = suppCommsJsonArray.getJSONObject(i);

          suppCommercials = new SupplierCommercial();
          suppCommercials.setCommercialName(suppComm.getString(JSON_PROP_COMMERCIALNAME));
          suppCommercials.setCommercialType(suppComm.getString(JSON_PROP_COMMERCIALTYPE));
          suppCommercials.setCommercialAmount(suppComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
          suppCommercials.setCommercialCurrency(suppComm.getString(JSON_PROP_COMMERCIALCURRENCY));
          
          suppCommercials.setProduct(JSON_PROP_PRODUCTHOLIDAYS);
          suppCommercials.setOrder(order);
          suppCommercialsSet.add(suppCommercials);

      }
      return suppCommercialsSet;
  }

  Set<ClientCommercial> readClientCommercials(JSONArray clientCommsJsonArray, Object order1) {
	  ProductOrder order = (ProductOrder)order1; 
	  
    Set<ClientCommercial> clientCommercialsSet =new HashSet<ClientCommercial>();
    ClientCommercial clientCommercials;
    
    for(int i=0;i<clientCommsJsonArray.length();i++)    {
        
        JSONObject totalClientComm = clientCommsJsonArray.getJSONObject(i);
        
         String clientID = totalClientComm.getString(JSON_PROP_CLIENTID);
         String parentClientID = totalClientComm.getString(JSON_PROP_PARENTCLIENTID);;      
         String commercialEntityType = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYTYPE);;      
         String commercialEntityID = totalClientComm.getString(JSON_PROP_COMMERCIALENTITYID);;
        
         boolean companyFlag = (i==0)?true:false;
        
    
    JSONArray clientComms = totalClientComm.getJSONArray("clientCommercials");
    
    for(int j=0;j<clientComms.length();j++) {
    
    JSONObject clientComm = clientComms.getJSONObject(j);
    
    clientCommercials =new ClientCommercial();
    clientCommercials.setCommercialName(clientComm.getString(JSON_PROP_COMMERCIALNAME));
    clientCommercials.setCommercialType(clientComm.getString(JSON_PROP_COMMERCIALTYPE));
    clientCommercials.setCommercialAmount(clientComm.getBigDecimal(JSON_PROP_COMMAMOUNT).toString());
    clientCommercials.setCommercialCurrency(clientComm.getString(JSON_PROP_COMMERCIALCURRENCY));
    clientCommercials.setClientID(clientID);
    clientCommercials.setParentClientID(parentClientID);
    clientCommercials.setCommercialEntityType(commercialEntityType);
    clientCommercials.setCommercialEntityID(commercialEntityID);
    clientCommercials.setCompanyFlag(companyFlag);

    clientCommercials.setProduct(JSON_PROP_PRODUCTHOLIDAYS);
    clientCommercials.setOrder(order);
    clientCommercialsSet.add(clientCommercials);
    }
    }
    return clientCommercialsSet;
} 
  
  public String processBookResponse(JSONObject bookResponseJson) throws BookingEngineDBException {

	//TODO: We need to put logic to update status for booking based on the statuses of individual products.
		
	  Booking booking = bookingRepository.findOne(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString(JSON_PROP_BOOKID));
	    if(booking==null)
	    {
	        myLogger.warn(String.format("Holiday Booking Response could not be populated since no bookings found for req with bookID %s", bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString("bookID")));
	        response.put("ErrorCode","BE_ERR_HOLIDAYS_004");
	        response.put("ErrorMsg", BE_ERR_HOLIDAYS_004);
	        return response.toString();
	    }
	    else
			{
			JSONArray dynPkgArray = bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray("dynamicPackage");
			for(int k=0;k<dynPkgArray.length();k++){
			JSONObject currentDynPkgObj = dynPkgArray.getJSONObject(k);
			List<HolidaysOrders> orders = holidaysRepository.findByBooking(booking);
			if(orders.size()==0){
		        myLogger.warn(String.format("Holiday Booking Response could not be populated since no holiday orders found for req with bookID %s", bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getString("bookID")));
		        response.put("ErrorCode", "BE_ERR_HOLIDAYS_005");
		        response.put("ErrorMsg", BE_ERR_HOLIDAYS_005);
		        return response.toString();
		    	}
		    else{
			int count =0;
			for(int l=0;l<orders.size();l++) {
				if(l==k) {
		        HolidaysOrders order=orders.get(l);	
				String prevOrder = order.toString();
				//List<AccoRoomDetails> list = null;
				order.setStatus(OrderStatus.OK.getProductStatus());
				order.setSuppierReservationId(currentDynPkgObj.getJSONObject(JSON_PROP_SUPPLIERBOOKREFERENCES).optString("bookingID"));
				//order.setSupplierReferenceId(currentDynPkgObj.optString(JSON_PROP_SUPPLIERREFERENCEID));
				//order.setClientReferenceId(currentDynPkgObj.optString(JSON_PROP_CLIENTREFERENCEID));
				//order.setSupplierCancellationId(currentDynPkgObj.optString(JSON_PROP_SUPPLIERCANCELLATIONID));
				count++;
				order.setLastModifiedAt(ZonedDateTime.now( ZoneOffset.UTC ));
        //order.setSupp_booking_reference(bookResponseJson.getJSONObject(JSON_PROP_RESBODY).getJSONArray(JSON_PROP_SUPPLIERBOOKREFERENCES).getJSONObject(count).getString(JSON_PROP_BOOKREFID));
        
        //For Accommodation Orders
        for(AccoRoomDetails room: order.getAccoOrders().getRoomDetails()) {
            
            room.setStatus("Confirmed");
        }
        
        //For Activities Orders
        for(ActivitiesOrders activitiesOrder: order.getActivitiesOrders()) {
          
          activitiesOrder.setStatus("Confirmed");
        }
        
        //For Transfers Orders
        for(TransfersOrders transfersOrder: order.getTransfersOrders()) {
          
          transfersOrder.setStatus("Confirmed");
        }
        
        //For Insurance Orders
        for(InsuranceOrders insuranceOrder: order.getInsuranceOrders()) {
          
          insuranceOrder.setStatus("Confirmed");
        }
        
        //For Extension Nights
        for(HolidaysExtensionDetails holidaysExtensionDetail: order.getHolidaysExtensionDetails()) {
          
          holidaysExtensionDetail.setStatus("Confirmed");
          
          for(AccoRoomDetails room: holidaysExtensionDetail.getRoomDetails()) {
        	  room.setStatus("Confirmed");        	  
          }
        }
        
        //For Extras
        for(HolidaysExtrasDetails holidaysExtrasDetail: order.getHolidaysExtrasDetails()) {
          
          holidaysExtrasDetail.setStatus("Confirmed");
        }
        
        saveHolidaysOrder(order, prevOrder);
				}
			}
	  }
	}
			myLogger.info(String.format("Holidays Booking Response populated successfully for req with bookID %s = %s", bookResponseJson.getJSONObject("responseBody").getString("bookID"),bookResponseJson.toString()));
		    return "SUCCESS";
  }
}
  
    //Have To make Method for update and cancellation - 
    //Make methods - processAmClRequest, processAmClResponse, fullCancel, updateRoom, updatePaxDetails, saveAccoAmCl

	public Booking saveBookingOrder(Booking order,String prevOrder) {
		Booking orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, Booking.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return bookingRepository.saveOrder(orderObj,prevOrder);
	}

	
	private HolidaysOrders saveHolidaysOrder(HolidaysOrders order, String prevOrder) {
		HolidaysOrders orderObj = null;
		try {
			orderObj = CopyUtils.copy(order, HolidaysOrders.class);

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		}
		return holidaysRepository.saveOrder(orderObj, prevOrder);
		
	}
	
	private PassengerDetails savePaxDetails(PassengerDetails pax, String prevOrder) throws BookingEngineDBException 
    {
        PassengerDetails orderObj = null;
        try {
            orderObj = CopyUtils.copy(pax, PassengerDetails.class);

        }
        catch (InvocationTargetException | IllegalAccessException e) {
             myLogger.fatal("Error while saving Holidays Passenger order object : " + e);
             //myLogger.error("Error while saving order object: " + e);
            throw new BookingEngineDBException("Failed to save order object");
        }
        return passengerRepository.saveOrder(orderObj,prevOrder);
    }

	private ZonedDateTime readStartDateEndDate(String stringInDate) {
      try {
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
          Instant instant = sdf.parse(stringInDate).toInstant();

          // TODO: done
          ZonedDateTime zonedDateTime = instant.atZone(ZoneOffset.UTC );
//        String stringOutDate = zonedDateTime.toString();
          return zonedDateTime;
      } catch (ParseException e) {
          e.printStackTrace();
      }
      return null;
  }
	
	@Override
	public String processAmClRequest(JSONObject reqJson) throws BookingEngineDBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String processAmClResponse(JSONObject resJson) throws BookingEngineDBException {
		// TODO Auto-generated method stub
		return null;
	}
}
