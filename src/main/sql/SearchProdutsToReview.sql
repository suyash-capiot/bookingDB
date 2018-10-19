CREATE OR REPLACE FUNCTION be_db_services_schema.searchprodutstoreview(
	p_productEndDate character varying)
    RETURNS refcursor AS $$

    DECLARE
        -- Declare Cursors
        	cur_Bookings refcursor;
        -- End Cursors definition
        BEGIN
        	    OPEN cur_Bookings FOR
                    (
                        select
                        TBL_BOOKING.bookid as bookId, TBL_AIRORDERS.id as orderId, TBL_AIRORDERS.productsubcategory as productsubcategory,
                        TBL_BOOKING.clientid as clientid,  TBL_BOOKING.clienttype as clienttype, TBL_BOOKING.groupnameid as clientGroup
                        FROM
                        be_db_services_schema.Booking as TBL_BOOKING
                        LEFT JOIN be_db_services_schema.airorders as TBL_AIRORDERS ON TBL_AIRORDERS.bookid = TBL_BOOKING.bookid
                        CROSS JOIN jsonb_array_elements( TBL_AIRORDERS.flightdetails ->'originDestinationOptions'-> 0 ->'flightSegment' ) each_flight_segment
                        where 1 = 1 and
                        (
                            (p_productEndDate IS NULL OR date(p_productEndDate)
                                                                          = date( each_flight_segment ->>'departureDate'))
                        )
                        and
                        (
                            ( TBL_AIRORDERS.productsubcategory LIKE '%Flight%' )
                        )
                        group by TBL_BOOKING.bookid, orderId, productsubcategory,
                                clientid, clienttype, clientGroup
                    )
                    UNION
                    (
                        select
                        TBL_BOOKING.bookid as bookId, TBL_ACCOORDERS.id as orderId, TBL_ACCOORDERS.productsubcategory as productsubcategory,
                        TBL_BOOKING.clientid as clientid, TBL_BOOKING.clienttype as clienttype,TBL_BOOKING.groupnameid as clientGroup
                        FROM
                        be_db_services_schema.Booking as TBL_BOOKING
                        LEFT JOIN  be_db_services_schema.accoorders as TBL_ACCOORDERS ON TBL_ACCOORDERS.bookid = TBL_BOOKING.bookid
                        LEFT JOIN be_db_services_schema.accoroomdetails as TBL_ACCOROOMDETAILS ON TBL_ACCOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id
                        where 1 = 1 and
                        (
                            (p_productEndDate is NULL or date(TBL_ACCOROOMDETAILS.checkOutDate) = date(p_productEndDate))
                        )
                        and
                        (
                            ( TBL_ACCOORDERS.productsubcategory LIKE '%Hotel%' )
                        )
                        group by TBL_BOOKING.bookid, orderId, productsubcategory,
                        clientid, clienttype, clientGroup
                   )
                   order by bookid, productsubcategory desc;

               RETURN cur_Bookings;

            -- END IF CHECK FOR TRAVEL DATE ---

	END; -- END OF Function BEGIN tag
$$ LANGUAGE plpgsql;
COMMIT;

rollback
BEGIN;
   SELECT be_db_services_schema.searchprodutstoreview( '2018-07-30')
   FETCH ALL IN "<unnamed portal 4>" ;
COMMIT;




