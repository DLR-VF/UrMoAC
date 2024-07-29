#!/usr/bin/env python
# -*- coding: utf-8 -*-
from __future__ import print_function
# ===========================================================================
"""GTFS definitions built automatically using parse_reference.py"""
# ===========================================================================
__author__     = "Daniel Krajzewicz"
__copyright__  = "Copyright 2023-2024, Institute of Transport Research, German Aerospace Center (DLR)"
__credits__    = ["Daniel Krajzewicz"]
__license__    = "EPL 2.0"
__version__    = "0.8.0"
__maintainer__ = "Daniel Krajzewicz"
__email__      = "daniel.krajzewicz@dlr.de"
__status__     = "Production"
# ===========================================================================
# - https://github.com/DLR-VF/UrMoAC
# - https://www.dlr.de/vf
# ===========================================================================


# --- imports ---------------------------------------------------------------
from enum import IntEnum


# --- enum definitions ------------------------------------------------------
class Presence(IntEnum):
    """An enumeration of known presence conditions"""
    REQUIRED = 0
    OPTIONAL = 1
    CONDITIONALLY_REQUIRED = 2
    CONDITIONALLY_FORBIDDEN = 3


class FieldType(IntEnum):
    """An enumeration of known field types"""
    COLOR = 0
    CURRENCY_CODE = 1
    CURRENCY_AMOUNT = 2
    DATE = 3
    EMAIL = 4
    ENUM = 5
    ID = 6
    LANGUAGE_CODE = 7
    LATITUDE = 8
    LONGITUDE = 9
    FLOAT = 10
    INTEGER = 11
    PHONE_NUMBER = 12
    TIME = 13
    TEXT = 14
    TIMEZONE = 15
    URL = 16


# --- GTFS definitions ------------------------------------------------------
tableDefinitions = {
    "agency" : [
        [ "agency_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "agency_name", FieldType.TEXT, Presence.REQUIRED ],
        [ "agency_url", FieldType.URL, Presence.REQUIRED ],
        [ "agency_timezone", FieldType.TIMEZONE, Presence.REQUIRED ],
        [ "agency_lang", FieldType.LANGUAGE_CODE, Presence.OPTIONAL ],
        [ "agency_phone", FieldType.PHONE_NUMBER, Presence.OPTIONAL ],
        [ "agency_fare_url", FieldType.URL, Presence.OPTIONAL ],
        [ "agency_email", FieldType.EMAIL, Presence.OPTIONAL ]
    ],
    "stops" : [
        [ "stop_id", FieldType.ID, Presence.REQUIRED ],
        [ "stop_code", FieldType.TEXT, Presence.OPTIONAL ],
        [ "stop_name", FieldType.TEXT, Presence.CONDITIONALLY_REQUIRED ],
        [ "tts_stop_name", FieldType.TEXT, Presence.OPTIONAL ],
        [ "stop_desc", FieldType.TEXT, Presence.OPTIONAL ],
        [ "stop_lat", FieldType.LATITUDE, Presence.CONDITIONALLY_REQUIRED ],
        [ "stop_lon", FieldType.LONGITUDE, Presence.CONDITIONALLY_REQUIRED ],
        [ "zone_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "stop_url", FieldType.URL, Presence.OPTIONAL ],
        [ "location_type", FieldType.ENUM, Presence.OPTIONAL ],
        [ "parent_station", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "stop_timezone", FieldType.TIMEZONE, Presence.OPTIONAL ],
        [ "wheelchair_boarding", FieldType.ENUM, Presence.OPTIONAL ],
        [ "level_id", FieldType.ID, Presence.OPTIONAL ],
        [ "platform_code", FieldType.TEXT, Presence.OPTIONAL ]
    ],
    "routes" : [
        [ "route_id", FieldType.ID, Presence.REQUIRED ],
        [ "agency_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "route_short_name", FieldType.TEXT, Presence.CONDITIONALLY_REQUIRED ],
        [ "route_long_name", FieldType.TEXT, Presence.CONDITIONALLY_REQUIRED ],
        [ "route_desc", FieldType.TEXT, Presence.OPTIONAL ],
        [ "route_type", FieldType.ENUM, Presence.REQUIRED ],
        [ "route_url", FieldType.URL, Presence.OPTIONAL ],
        [ "route_color", FieldType.COLOR, Presence.OPTIONAL ],
        [ "route_text_color", FieldType.COLOR, Presence.OPTIONAL ],
        [ "route_sort_order", FieldType.INTEGER, Presence.OPTIONAL ],
        [ "continuous_pickup", FieldType.ENUM, Presence.OPTIONAL ],
        [ "continuous_drop_off", FieldType.ENUM, Presence.OPTIONAL ],
        [ "network_id", FieldType.ID, Presence.OPTIONAL ]
    ],
    "trips" : [
        [ "route_id", FieldType.ID, Presence.REQUIRED ],
        [ "service_id", FieldType.ID, Presence.REQUIRED ],
        [ "trip_id", FieldType.ID, Presence.REQUIRED ],
        [ "trip_headsign", FieldType.TEXT, Presence.OPTIONAL ],
        [ "trip_short_name", FieldType.TEXT, Presence.OPTIONAL ],
        [ "direction_id", FieldType.ENUM, Presence.OPTIONAL ],
        [ "block_id", FieldType.ID, Presence.OPTIONAL ],
        [ "shape_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "wheelchair_accessible", FieldType.ENUM, Presence.OPTIONAL ],
        [ "bikes_allowed", FieldType.ENUM, Presence.OPTIONAL ]
    ],
    "stop_times" : [
        [ "trip_id", FieldType.ID, Presence.REQUIRED ],
        [ "arrival_time", FieldType.TIME, Presence.CONDITIONALLY_REQUIRED ],
        [ "departure_time", FieldType.TIME, Presence.CONDITIONALLY_REQUIRED ],
        [ "stop_id", FieldType.ID, Presence.REQUIRED ],
        [ "stop_sequence", FieldType.INTEGER, Presence.REQUIRED ],
        [ "stop_headsign", FieldType.TEXT, Presence.OPTIONAL ],
        [ "pickup_type", FieldType.ENUM, Presence.OPTIONAL ],
        [ "drop_off_type", FieldType.ENUM, Presence.OPTIONAL ],
        [ "continuous_pickup", FieldType.ENUM, Presence.OPTIONAL ],
        [ "continuous_drop_off", FieldType.ENUM, Presence.OPTIONAL ],
        [ "shape_dist_traveled", FieldType.FLOAT, Presence.OPTIONAL ],
        [ "timepoint", FieldType.ENUM, Presence.OPTIONAL ]
    ],
    "calendar" : [
        [ "service_id", FieldType.ID, Presence.REQUIRED ],
        [ "monday", FieldType.ENUM, Presence.REQUIRED ],
        [ "tuesday", FieldType.ENUM, Presence.REQUIRED ],
        [ "wednesday", FieldType.ENUM, Presence.REQUIRED ],
        [ "thursday", FieldType.ENUM, Presence.REQUIRED ],
        [ "friday", FieldType.ENUM, Presence.REQUIRED ],
        [ "saturday", FieldType.ENUM, Presence.REQUIRED ],
        [ "sunday", FieldType.ENUM, Presence.REQUIRED ],
        [ "start_date", FieldType.DATE, Presence.REQUIRED ],
        [ "end_date", FieldType.DATE, Presence.REQUIRED ]
    ],
    "calendar_dates" : [
        [ "service_id", FieldType.ID, Presence.REQUIRED ],
        [ "date", FieldType.DATE, Presence.REQUIRED ],
        [ "exception_type", FieldType.ENUM, Presence.REQUIRED ]
    ],
    "fare_attributes" : [
        [ "fare_id", FieldType.ID, Presence.REQUIRED ],
        [ "price", FieldType.FLOAT, Presence.REQUIRED ],
        [ "currency_type", FieldType.CURRENCY_CODE, Presence.REQUIRED ],
        [ "payment_method", FieldType.ENUM, Presence.REQUIRED ],
        [ "transfers", FieldType.ENUM, Presence.REQUIRED ],
        [ "agency_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "transfer_duration", FieldType.INTEGER, Presence.OPTIONAL ]
    ],
    "fare_rules" : [
        [ "fare_id", FieldType.ID, Presence.REQUIRED ],
        [ "route_id", FieldType.ID, Presence.OPTIONAL ],
        [ "origin_id", FieldType.ID, Presence.OPTIONAL ],
        [ "destination_id", FieldType.ID, Presence.OPTIONAL ],
        [ "contains_id", FieldType.ID, Presence.OPTIONAL ]
    ],
    "fare_products" : [
        [ "fare_product_id", FieldType.ID, Presence.REQUIRED ],
        [ "fare_product_name", FieldType.TEXT, Presence.OPTIONAL ],
        [ "amount", FieldType.CURRENCY_AMOUNT, Presence.REQUIRED ],
        [ "currency", FieldType.CURRENCY_CODE, Presence.REQUIRED ]
    ],
    "fare_leg_rules" : [
        [ "leg_group_id", FieldType.ID, Presence.OPTIONAL ],
        [ "network_id", FieldType.ID, Presence.OPTIONAL ],
        [ "from_area_id", FieldType.ID, Presence.OPTIONAL ],
        [ "to_area_id", FieldType.ID, Presence.OPTIONAL ],
        [ "fare_product_id", FieldType.ID, Presence.REQUIRED ]
    ],
    "fare_transfer_rules" : [
        [ "from_leg_group_id", FieldType.ID, Presence.OPTIONAL ],
        [ "to_leg_group_id", FieldType.ID, Presence.OPTIONAL ],
        [ "transfer_count", FieldType.INTEGER, Presence.CONDITIONALLY_FORBIDDEN ],
        [ "duration_limit", FieldType.INTEGER, Presence.OPTIONAL ],
        [ "duration_limit_type", FieldType.ENUM, Presence.CONDITIONALLY_REQUIRED ],
        [ "fare_transfer_type", FieldType.ENUM, Presence.REQUIRED ],
        [ "fare_product_id", FieldType.ID, Presence.OPTIONAL ]
    ],
    "areas" : [
        [ "area_id", FieldType.ID, Presence.REQUIRED ],
        [ "area_name", FieldType.TEXT, Presence.OPTIONAL ]
    ],
    "stop_areas" : [
        [ "area_id", FieldType.ID, Presence.REQUIRED ],
        [ "stop_id", FieldType.ID, Presence.REQUIRED ]
    ],
    "shapes" : [
        [ "shape_id", FieldType.ID, Presence.REQUIRED ],
        [ "shape_pt_lat", FieldType.LATITUDE, Presence.REQUIRED ],
        [ "shape_pt_lon", FieldType.LONGITUDE, Presence.REQUIRED ],
        [ "shape_pt_sequence", FieldType.INTEGER, Presence.REQUIRED ],
        [ "shape_dist_traveled", FieldType.FLOAT, Presence.OPTIONAL ]
    ],
    "frequencies" : [
        [ "trip_id", FieldType.ID, Presence.REQUIRED ],
        [ "start_time", FieldType.TIME, Presence.REQUIRED ],
        [ "end_time", FieldType.TIME, Presence.REQUIRED ],
        [ "headway_secs", FieldType.INTEGER, Presence.REQUIRED ],
        [ "exact_times", FieldType.ENUM, Presence.OPTIONAL ]
    ],
    "transfers" : [
        [ "from_stop_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "to_stop_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "from_route_id", FieldType.ID, Presence.OPTIONAL ],
        [ "to_route_id", FieldType.ID, Presence.OPTIONAL ],
        [ "from_trip_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "to_trip_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "transfer_type", FieldType.ENUM, Presence.REQUIRED ],
        [ "min_transfer_time", FieldType.INTEGER, Presence.OPTIONAL ]
    ],
    "pathways" : [
        [ "pathway_id", FieldType.ID, Presence.REQUIRED ],
        [ "from_stop_id", FieldType.ID, Presence.REQUIRED ],
        [ "to_stop_id", FieldType.ID, Presence.REQUIRED ],
        [ "pathway_mode", FieldType.ENUM, Presence.REQUIRED ],
        [ "is_bidirectional", FieldType.ENUM, Presence.REQUIRED ],
        [ "length", FieldType.FLOAT, Presence.OPTIONAL ],
        [ "traversal_time", FieldType.INTEGER, Presence.OPTIONAL ],
        [ "stair_count", FieldType.INTEGER, Presence.OPTIONAL ],
        [ "max_slope", FieldType.FLOAT, Presence.OPTIONAL ],
        [ "min_width", FieldType.FLOAT, Presence.OPTIONAL ],
        [ "signposted_as", FieldType.TEXT, Presence.OPTIONAL ],
        [ "reversed_signposted_as", FieldType.TEXT, Presence.OPTIONAL ]
    ],
    "levels" : [
        [ "level_id", FieldType.ID, Presence.REQUIRED ],
        [ "level_index", FieldType.FLOAT, Presence.REQUIRED ],
        [ "level_name", FieldType.TEXT, Presence.OPTIONAL ]
    ],
    "translations" : [
        [ "table_name", FieldType.ENUM, Presence.REQUIRED ],
        [ "field_name", FieldType.TEXT, Presence.REQUIRED ],
        [ "language", FieldType.LANGUAGE_CODE, Presence.REQUIRED ],
        [ "translation", FieldType.TEXT, Presence.REQUIRED ],
        [ "record_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "record_sub_id", FieldType.ID, Presence.CONDITIONALLY_REQUIRED ],
        [ "field_value", FieldType.TEXT, Presence.CONDITIONALLY_REQUIRED ]
    ],
    "feed_info" : [
        [ "feed_publisher_name", FieldType.TEXT, Presence.REQUIRED ],
        [ "feed_publisher_url", FieldType.URL, Presence.REQUIRED ],
        [ "feed_lang", FieldType.LANGUAGE_CODE, Presence.REQUIRED ],
        [ "default_lang", FieldType.LANGUAGE_CODE, Presence.OPTIONAL ],
        [ "feed_start_date", FieldType.DATE, Presence.OPTIONAL ],
        [ "feed_end_date", FieldType.DATE, Presence.OPTIONAL ],
        [ "feed_version", FieldType.TEXT, Presence.OPTIONAL ],
        [ "feed_contact_email", FieldType.EMAIL, Presence.OPTIONAL ],
        [ "feed_contact_url", FieldType.URL, Presence.OPTIONAL ]
    ],
    "attributions" : [
        [ "attribution_id", FieldType.ID, Presence.OPTIONAL ],
        [ "agency_id", FieldType.ID, Presence.OPTIONAL ],
        [ "route_id", FieldType.ID, Presence.OPTIONAL ],
        [ "trip_id", FieldType.ID, Presence.OPTIONAL ],
        [ "organization_name", FieldType.TEXT, Presence.REQUIRED ],
        [ "is_producer", FieldType.ENUM, Presence.OPTIONAL ],
        [ "is_operator", FieldType.ENUM, Presence.OPTIONAL ],
        [ "is_authority", FieldType.ENUM, Presence.OPTIONAL ],
        [ "attribution_url", FieldType.URL, Presence.OPTIONAL ],
        [ "attribution_email", FieldType.EMAIL, Presence.OPTIONAL ],
        [ "attribution_phone", FieldType.PHONE_NUMBER, Presence.OPTIONAL ]
    ]
}


optionalTables = [ "calendar", "calendar_dates", "fare_attributes", "fare_rules", "fare_products", "fare_leg_rules", "fare_transfer_rules", "areas", "stop_areas", "shapes", "frequencies", "transfers", "pathways", "levels", "translations", "feed_info", "attributions" ]

