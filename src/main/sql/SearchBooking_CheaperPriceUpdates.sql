//********************************************************************//
-- Following line drops any existing Function/Procedure
DROP FUNCTION be_db_services_schema.SearchBookingsForProductUpdates(TEXT);

//********************************************************************//
-- Create a function which returns Booking info for Product updates
-- IN parameter is Product Sub Category
-- OUT parameter is RefCursor having data for Flights/Hotels/etc.

CREATE OR REPLACE FUNCTION be_db_services_schema.SearchBookingsForProductUpdates( productSubCategory TEXT) RETURNS refcursor AS $$
     DECLARE
        -- Declare Cursors
        cur_Bookings refcursor;
        -- End Cursors definition    
    BEGIN       
    --- Handle for Flights sub category ---
    IF productSubCategory = 'Flight' THEN
                OPEN cur_Bookings FOR 
                -- Select all Flight details for cheaper price updates --
                    select
                    TBL_BOOKINGS.bookid as "bookID", TBL_AIRORDERS.id as "orderID", TBL_AIRORDERS.supplierid as "supplierID",
                    TBL_AIRORDERS.supplierprice, TBL_AIRORDERS.supplierpricecurrencycode,
                    TBL_AIRORDERS.flightdetails as "flightDetailsJSON", TBL_AIRORDERS.triptype as "tripType", TBL_AIRORDERS.status as "status"
                    from
                    be_db_services_schema.booking as TBL_BOOKINGS, be_db_services_schema.airorders as TBL_AIRORDERS
                    where
                    TBL_BOOKINGS.bookid = TBL_AIRORDERS.bookid AND
                    UPPER(TBL_AIRORDERS.status) <> UPPER( 'Ticketed' ) -- Handle status
                    order by TBL_BOOKINGS.bookid, TBL_AIRORDERS.id;
                -- End of Select all Flight details for cheaper price updates --
           --- Handle for Accomodation sub category ---
                ELSIF ( productSubCategory = 'Hotel' ) THEN
                OPEN cur_Bookings FOR
                -- Select all Hotel details for cheaper price updates --
                    select
                    TBL_BOOKINGS.bookid, TBL_ACCOORDERS.id as orderID, TBL_ACCOORDERS.productsubcategory,
                    to_date(TBL_ROOMDETAILS.checkindate, 'YYYY MM DD') as checkindate, to_date(TBL_ROOMDETAILS.checkoutdate, 'YYYY MM DD') as checkoutdate,
                    TBL_ACCOORDERS.supplierid as supplierID, TBL_ACCOORDERS.supplierprice, TBL_ACCOORDERS.supplierpricecurrencycode,
                    TBL_ROOMDETAILS.roomcategoryid, TBL_ROOMDETAILS.roomcategoryname,
                    TBL_ROOMDETAILS.mealcode, TBL_ROOMDETAILS.mealname, TBL_ACCOORDERS.status
                    from
                    be_db_services_schema.booking as TBL_BOOKINGS, be_db_services_schema.accoorders as TBL_ACCOORDERS, be_db_services_schema.accoroomdetails AS TBL_ROOMDETAILS
                    where
                    TBL_BOOKINGS.bookid = TBL_ACCOORDERS.bookid AND
                    TBL_ACCOORDERS.id = TBL_ROOMDETAILS.acco_order_id;
                -- End of Select for cheaper price updates --
           END IF;
                    
      RETURN cur_Bookings;      
     END;
$$ LANGUAGE plpgsql;
COMMIT
/*************************************************************
--Following code is used to test the Stored Procedure
--rollback;
--BEGIN;
--   SELECT SearchBookingsForProductUpdates( 'Flight' );
--   SELECT SearchBookingsForProductUpdates(1);
--   FETCH ALL IN "<unnamed portal 6>";
--
--   SELECT SearchBookingsForProductUpdates( 'Hotel' );
--   FETCH ALL IN "<unnamed portal 3>";
--COMMIT;
************************************************************/