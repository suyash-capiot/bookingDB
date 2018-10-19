-- FUNCTION: be_db_services_schema.searcharrivallistgeneral(character varying, character varying, character varying, character varying, character varying, character varying, character varying)

-- DROP FUNCTION be_db_services_schema.searcharrivallistgeneral(character varying, character varying, character varying, character varying, character varying, character varying, character varying);

CREATE OR REPLACE FUNCTION be_db_services_schema.searcharrivallistgeneral(
	p_traveldatetime character varying,
	p_bookingdatetime character varying,
	p_productsubcategory character varying,
	p_supplierid character varying,
	p_clienttype character varying,
	p_clientgroupid character varying,
	p_clientid character varying)
    RETURNS refcursor AS $$


    DECLARE
		p_BookingTimeStamp timestamp:= null;
        p_TravelTimeStamp timestamp:= null;

        -- Declare Cursors
        	cur_Bookings refcursor;
        -- End Cursors definition
        BEGIN
        	-- START OF IF CLAUSE --
            IF p_BookingDateTime IS NOT NULL THEN
            	p_BookingTimeStamp = TO_TIMESTAMP( p_BookingDateTime, 'YYYY-MM-DD HH24:MI:SS');

                OPEN cur_Bookings FOR
                    (
                        select
                        TBL_BOOKING.bookid as bookid, TBL_AIRORDERS.airlinepnr as supplier_reference_number, 'Transportation' as productcategory,
                        TBL_AIRORDERS.productsubcategory as productsubcategory, TBL_BOOKING.clienttype as clienttype,
                        TBL_BOOKING.groupnameid as groupnameid, TBL_AIRORDERS.supplierid as supplierid
                        FROM
                        be_db_services_schema.Booking as TBL_BOOKING
                        LEFT JOIN be_db_services_schema.airorders as TBL_AIRORDERS ON TBL_AIRORDERS.bookid = TBL_BOOKING.bookid
                        where 1 = 1 and
                        (
                            (TBL_BOOKING.createdat = p_BookingTimeStamp )
                        )
                        and
                        (
                            ( TBL_AIRORDERS.productsubcategory LIKE '%Flight%' )
                        )
                        group by TBL_BOOKING.bookid, supplier_reference_number, productsubcategory,
                                TBL_BOOKING.clienttype, TBL_BOOKING.groupnameid, supplierid
                    )
                    UNION
                    (
                        select
                        TBL_BOOKING.bookid as bookid, TBL_ACCOORDERS.supplierreferenceid as supplier_reference_number, 'Accomodation' as productcategory,
                        TBL_ACCOORDERS.productsubcategory as productsubcategory, TBL_BOOKING.clienttype as clienttype,
                        TBL_BOOKING.groupnameid as groupnameid, TBL_ACCOORDERS.supplierid as supplierid
                        FROM
                        be_db_services_schema.Booking as TBL_BOOKING
                        LEFT JOIN  be_db_services_schema.accoorders as TBL_ACCOORDERS ON TBL_ACCOORDERS.bookid = TBL_BOOKING.bookid
                        where 1 = 1 and
                        (
                            ( TBL_BOOKING.createdat = p_BookingTimeStamp )
                        )
                        and
                        (
                            ( TBL_ACCOORDERS.productsubcategory LIKE '%Hotel%' )
                        )
                        group by TBL_BOOKING.bookid, supplier_reference_number, productsubcategory,
                        TBL_BOOKING.clienttype, TBL_BOOKING.groupnameid, supplierid
                   )
                   order by bookid, productsubcategory desc;

               RETURN cur_Bookings;
            END IF;
            -- END OF IF CLAUSE --

            -- BEGIN IF CHECK FOR TRAVEL DATE ---
			IF p_TravelDateTime IS NOT NULL THEN
            	p_TravelTimeStamp = TO_TIMESTAMP( p_TravelDateTime, 'YYYY-MM-DD HH24:MI:SS');

                OPEN cur_Bookings FOR
                    (
                        select
                        TBL_BOOKING.bookid as bookid, TBL_AIRORDERS.airlinepnr as supplier_reference_number, 'Transportation' as productcategory,
                        TBL_AIRORDERS.productsubcategory as productsubcategory, TBL_BOOKING.clienttype as clienttype,
                        TBL_BOOKING.groupnameid as groupnameid, TBL_AIRORDERS.supplierid as supplierid
                        FROM
                        be_db_services_schema.Booking as TBL_BOOKING
                        LEFT JOIN be_db_services_schema.airorders as TBL_AIRORDERS ON TBL_AIRORDERS.bookid = TBL_BOOKING.bookid
                        CROSS JOIN jsonb_array_elements( TBL_AIRORDERS.flightdetails ->'originDestinationOptions'->0->'flightSegment' ) each_flight_segment
                        where 1 = 1 and
                        (
                            ( TBL_AIRORDERS.productsubcategory LIKE '%Flight%' )
                            and
                            (
                                p_TravelTimeStamp = TO_TIMESTAMP( each_flight_segment ->>'departureDate', 'YYYY-MM-DD HH24:MI:SS' )
                            )
                        )
                        group by TBL_BOOKING.bookid, supplier_reference_number, productsubcategory,
                                TBL_BOOKING.clienttype, TBL_BOOKING.groupnameid, supplierid
                    )

                    UNION
                    (
                        select
                        TBL_BOOKING.bookid as bookid, TBL_ACCOORDERS.supplierreferenceid as supplier_reference_number, 'Accomodation' as productcategory,
                        TBL_ACCOORDERS.productsubcategory as productsubcategory, TBL_BOOKING.clienttype as clienttype,
                        TBL_BOOKING.groupnameid as groupnameid, TBL_ACCOORDERS.supplierid as supplierid
                        FROM
                        be_db_services_schema.Booking as TBL_BOOKING
                        LEFT JOIN  be_db_services_schema.accoorders as TBL_ACCOORDERS ON TBL_ACCOORDERS.bookid = TBL_BOOKING.bookid
                        LEFT JOIN be_db_services_schema.accoroomdetails as TBL_ACCOROOMDETAILS ON TBL_ACCOROOMDETAILS.acco_order_id = TBL_ACCOORDERS.id
                        where 1 = 1 and
                        (
                            ( TBL_ACCOORDERS.productsubcategory LIKE '%Hotel%' )
                        )
                        and
                        (
                            p_TravelTimeStamp = TO_TIMESTAMP( TBL_ACCOROOMDETAILS.checkindate, 'YYYY-MM-DD HH24:MI:SS')
                    	)
                        group by TBL_BOOKING.bookid, supplier_reference_number, productsubcategory,
                        TBL_BOOKING.clienttype, TBL_BOOKING.groupnameid, supplierid
                   )

                   order by bookid, productsubcategory desc;

               RETURN cur_Bookings;
            END IF;
            -- END IF CHECK FOR TRAVEL DATE ---

	END; -- END OF Function BEGIN tag
$$ LANGUAGE plpgsql;
COMMIT;

rollback
BEGIN;
   SELECT be_db_services_schema.SearchArrivalListGeneral( '2018-04-27', null, null, null, null, null, null )
   FETCH ALL IN "<unnamed portal 3>" ;
COMMIT;



