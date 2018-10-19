package com.coxandkings.travel.bookingengine.db.utils;

import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.BookingDetailsFilter;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.ClientAndPassengerBasedFilter;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.CompanyDetailsFilter;
import com.coxandkings.travel.bookingengine.db.criteria.bookingsearchcriteria.ProductDetailsFilter;
import com.coxandkings.travel.bookingengine.db.criteria.managefailures.DuplicateAccommodationBookingsSearchCriteria;
import com.coxandkings.travel.bookingengine.db.criteria.managefailures.DuplicateFlightBookingsSearchCriteria;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Component
public class SearchUtil {

    //(123456,2018-08-03 00:00:00,,,,,)
    public String getBookingDetailsFilter( BookingDetailsFilter bookingDetails )    {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "(" );
        buffer.append( (bookingDetails.getBookingRefId() != null )? bookingDetails.getBookingRefId() + ",": "," );
        buffer.append( (bookingDetails.getBookingTypeId() != null )? bookingDetails.getBookingTypeId() + ",": "," );

        String fromDate = bookingDetails.getBookingFromDate();
        String toDate = bookingDetails.getBookingToDate();

        if( fromDate == null || fromDate.isEmpty() )    {
            buffer.append( "" );
            buffer.append( "," );
        }
        else {
            buffer.append( fromDate + "," );
        }

        if( toDate == null || toDate.isEmpty() )    {
            buffer.append( "" );
            buffer.append( "," );
        }
        else {
            buffer.append( toDate + "," );
        }

        buffer.append( (bookingDetails.getPriority() != null )? bookingDetails.getPriority() + ",": "," );
        buffer.append( (bookingDetails.getBookingStatusId() != null )? bookingDetails.getBookingStatusId() + ",": "," );
        buffer.append( (bookingDetails.getAssignment() != null) ? bookingDetails.getAssignment() + ",": "," );
        buffer.append( (bookingDetails.getUserId() != null )? bookingDetails.getUserId() + ",": "," );
        buffer.append( (bookingDetails.getFinancialControlId() != null )? bookingDetails.getFinancialControlId() : "" );
        buffer.append( ")" );
        return buffer.toString();
    }

    public String getFlightSearchingCriteria( ProductDetailsFilter prodDetails ) {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "(" );
        buffer.append( ( prodDetails.getProductCategorySubTypeId()!=null) ? prodDetails.getProductCategorySubTypeId() + "," : ",");
        buffer.append( ( prodDetails.getSupplierName() != null )? prodDetails.getSupplierName() + "," : "," );
        buffer.append( ( prodDetails.getAirlinePNR() != null )? prodDetails.getAirlinePNR() + ",": "," );
        buffer.append( ( prodDetails.getTravelFromDate() != null )? prodDetails.getTravelFromDate() + ",": "," ); // for from travel date //TODO
        buffer.append( ( prodDetails.getTravelToDate() != null )? prodDetails.getTravelToDate() + ",": "," ); // for to travel date //TODO
        buffer.append( ( prodDetails.getGsdPnr() != null )? prodDetails.getGsdPnr() + ",": "," ); // for GDS PNR //TODO
        buffer.append( ( prodDetails.getTicketNumber() != null )? prodDetails.getTicketNumber() + ",": "," ); // for Ticket PNR(Number) //TODO
        buffer.append( "" ); // for AirlineName //TODO
        buffer.append( ")" );
        return buffer.toString();
    }

    public String getHotelSearchingCriteria( ProductDetailsFilter prodDetails ) {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "(" );
        buffer.append( ( prodDetails.getProductCategorySubTypeId() != null )? prodDetails.getProductCategorySubTypeId() + "," : "," );
        buffer.append( ( prodDetails.getCountry() != null )? prodDetails.getCountry() + "," : "," ); //for Country //TODO
        buffer.append( ( prodDetails.getCity() != null )? prodDetails.getCity() + "," : "," ); //for City //TODO
        buffer.append( "," ); //for ProductName //TODO
        buffer.append( ( prodDetails.getSupplierReferenceNumber() != null )? prodDetails.getSupplierReferenceNumber() + "," : "," );
        buffer.append( ( prodDetails.getSupplierName() != null )? prodDetails.getSupplierName() : "" );
        buffer.append( ")" );
        return buffer.toString();
    }

    
    public String getActivitiesSearchCriteria( ProductDetailsFilter prodDetails ) {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "(" );
        buffer.append( ( prodDetails.getProductCategorySubTypeId()!=null) ? prodDetails.getProductCategorySubTypeId() + "," : ",");
        buffer.append( ( prodDetails.getCountry() != null )? prodDetails.getCountry() + "," : "," );
        buffer.append( ( prodDetails.getCity() != null )? prodDetails.getCity() + ",": "," );
        buffer.append( ( prodDetails.getProductName() != null )? prodDetails.getProductName() + ",": "," ); // for from travel date //TODO
        buffer.append( ( prodDetails.getSupplierReferenceNumber() != null )? prodDetails.getSupplierReferenceNumber() + ",": "," ); // for to travel date //TODO
        buffer.append( ( prodDetails.getSupplierName() != null )? prodDetails.getSupplierName() : "" ); // for GDS PNR //TODO
        buffer.append( "" ); // for AirlineName //TODO
        buffer.append( ")" );
        return buffer.toString();
    }
	
	public String getHolidaysSearchCriteria( ProductDetailsFilter prodDetails ) {
      StringBuilder buffer = new StringBuilder();
      buffer.append( "(" );
      buffer.append( ( prodDetails.getProductCategorySubTypeId()!=null) ? prodDetails.getProductCategorySubTypeId() + "," : ",");
      buffer.append( ( prodDetails.getDestination() != null )? prodDetails.getDestination() + "," : "," );
      buffer.append( ( prodDetails.getCountry() != null )? prodDetails.getCountry() + "," : "," );
      buffer.append( ( prodDetails.getCity() != null )? prodDetails.getCity() + ",": "," );
      buffer.append( ( prodDetails.getNoOfNights() != null )? prodDetails.getNoOfNights() + ",": "," );
      buffer.append( ( prodDetails.getProductFlavourName() != null )? prodDetails.getProductFlavourName() + "," : "," );
      buffer.append( ( prodDetails.getPackageType() != null )? prodDetails.getPackageType() + "," : "," );
      buffer.append( ( prodDetails.getBrand() != null )? prodDetails.getBrand() + ",": "," );
      buffer.append( ( prodDetails.getCompanyPackageName() != null )? prodDetails.getCompanyPackageName() + ",": "," );
      buffer.append( ( prodDetails.getProductName() != null )? prodDetails.getProductName() + ",": "," ); // for from travel date //TODO
      buffer.append( ( prodDetails.getSupplierReferenceNumber() != null )? prodDetails.getSupplierReferenceNumber() + ",": "," ); // for to travel date //TODO
      buffer.append( ( prodDetails.getSupplierName() != null )? prodDetails.getSupplierName() : "" ); // for GDS PNR //TODO
      buffer.append( "" ); //TODO
      buffer.append( ")" );
      return buffer.toString();
  }
    
    public String getCompaniesSearchingCriteria( CompanyDetailsFilter companyDetails ) {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "(" );
        buffer.append( ( companyDetails.getGroupOfCompaniesId() != null )? companyDetails.getGroupOfCompaniesId() + ",": "," );
        buffer.append( ( companyDetails.getGroupNameId() != null )? companyDetails.getGroupNameId() + ",": "," );
        buffer.append( ( companyDetails.getCompanyId() != null )? companyDetails.getCompanyId() + ",": "," );
        buffer.append( ( companyDetails.getCompanyMarketId() != null )? companyDetails.getCompanyMarketId() + ",": "," );
        buffer.append( ( companyDetails.getSbuId() != null )? companyDetails.getSbuId() + ",": "," );
        buffer.append( ( companyDetails.getBuId() != null )? companyDetails.getBuId() : "" );
        buffer.append( ")" );
        return buffer.toString();
    }

    public String getClientPaxSearchingCriteria( ClientAndPassengerBasedFilter clientPaxDetails ) {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "(" );
        buffer.append( ( clientPaxDetails.getClientType() != null )? clientPaxDetails.getClientType() + ",": "," );
        buffer.append( ( clientPaxDetails.getClientCategoryId() != null )? clientPaxDetails.getClientCategoryId() + ",": "," );
        buffer.append( ( clientPaxDetails.getClientSubCategoryId() != null )? clientPaxDetails.getClientSubCategoryId() + ",": "," );
        buffer.append( ( clientPaxDetails.getClientId() != null )? clientPaxDetails.getClientId() + ",": "," );
        buffer.append( ( clientPaxDetails.getPassengerName() != null )? clientPaxDetails.getPassengerName() + ",": "," );
        buffer.append( ( clientPaxDetails.getPhoneNumber() != null )? clientPaxDetails.getPhoneNumber() + ",": "," );
        buffer.append( ( clientPaxDetails.getEmailId() != null )? clientPaxDetails.getEmailId() : "" );
        buffer.append( ")" );
        return buffer.toString();
    }

    private String formatTimestampToSQL( String javaTimeStamp, boolean isEndDate ) {
        String outputTS = null;
        String opsDateFormat = "yyyy-MM-dd'T'HH:mm:ssXXX";
        String postgresFormat = "yyyy-MM-dd HH:mm:ss.SSS";
        SimpleDateFormat opsDateFormatter = new SimpleDateFormat( opsDateFormat );
        SimpleDateFormat postgresDateFormatter = new SimpleDateFormat( postgresFormat );
        try {
            Date javaDate =  opsDateFormatter.parse( javaTimeStamp );
            Calendar rightNow = Calendar.getInstance();
            rightNow.setTime( javaDate );

            if( isEndDate ) {
                rightNow.set(Calendar.HOUR_OF_DAY, 23);
                rightNow.set(Calendar.MINUTE, 59);
                rightNow.set(Calendar.SECOND, 59);
                rightNow.set(Calendar.MILLISECOND,999);
            }else{
                rightNow.set(Calendar.HOUR_OF_DAY, 0);
                rightNow.set(Calendar.MINUTE, 0);
                rightNow.set(Calendar.SECOND, 0);
                rightNow.set(Calendar.MILLISECOND,000);
            }

            outputTS = postgresDateFormatter.format( rightNow.getTime() );
        } catch (ParseException e) {
            e.printStackTrace();
            outputTS = "";
        }
        return outputTS;
    }

    //=================Search Duplicate Flight Bookings===================//

    public String getDuplicateFlightBookingsCriteria(DuplicateFlightBookingsSearchCriteria duplicateFlightBookingsSearchCriteria) {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "\"(" );
        buffer.append( ( duplicateFlightBookingsSearchCriteria.getAirLineName() != null )? duplicateFlightBookingsSearchCriteria.getAirLineName() + ",": "," );
        buffer.append( ( duplicateFlightBookingsSearchCriteria.getFlightNumber() != null )? duplicateFlightBookingsSearchCriteria.getFlightNumber() + ",": "," );
        buffer.append( ( duplicateFlightBookingsSearchCriteria.getFirstName() != null )? duplicateFlightBookingsSearchCriteria.getFirstName() + ",": "," );
        buffer.append( ( duplicateFlightBookingsSearchCriteria.getLastName() != null )? duplicateFlightBookingsSearchCriteria.getLastName() + ",": "," );
        buffer.append( ( duplicateFlightBookingsSearchCriteria.getFromSector() != null )? duplicateFlightBookingsSearchCriteria.getFromSector() + ",": "," );
        buffer.append( ( duplicateFlightBookingsSearchCriteria.getToSector() != null )? duplicateFlightBookingsSearchCriteria.getToSector() + ",": "," );
        buffer.append( ( duplicateFlightBookingsSearchCriteria.getTravelFromDate() != null )? duplicateFlightBookingsSearchCriteria.getTravelFromDate() + ",": ",");
        buffer.append( ( duplicateFlightBookingsSearchCriteria.getTravelToDate() != null )? duplicateFlightBookingsSearchCriteria.getTravelToDate() + ",": ",");
        buffer.append( ( duplicateFlightBookingsSearchCriteria.getCabinType() != null )? duplicateFlightBookingsSearchCriteria.getCabinType() : "" );
        buffer.append( ")\"," );
        return buffer.toString();
    }

    public String getDuplicateAccoBookingsCriteria(
            DuplicateAccommodationBookingsSearchCriteria duplicateAccoBookingsSearchCriteria) {

        StringBuilder buffer = new StringBuilder();
        buffer.append( "\"(" );
        buffer.append( ( duplicateAccoBookingsSearchCriteria.getFirstName() != null )? duplicateAccoBookingsSearchCriteria.getFirstName() + ",": "," );
        buffer.append( ( duplicateAccoBookingsSearchCriteria.getLastName() != null )? duplicateAccoBookingsSearchCriteria.getLastName() + ",": "," );
        buffer.append( ( duplicateAccoBookingsSearchCriteria.getCheckInDate() != null )? duplicateAccoBookingsSearchCriteria.getCheckInDate() + ",": "," );
        buffer.append( ( duplicateAccoBookingsSearchCriteria.getCheckOutDate() != null )? duplicateAccoBookingsSearchCriteria.getCheckOutDate() + ",": "," );
        buffer.append( ( duplicateAccoBookingsSearchCriteria.getHotelCode() != null )? duplicateAccoBookingsSearchCriteria.getHotelCode() : "" );
        buffer.append( ")\"," );
        return buffer.toString();

    }
}
