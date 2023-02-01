#!/usr/bin/env python
# =========================================================
# osmmodes.py
# 
# @author Daniel Krajzewicz, Simon Nieland
# @date 01.04.2016
# @copyright Institut fuer Verkehrsforschung, 
#            Deutsches Zentrum fuer Luft- und Raumfahrt
# @brief Defines modes of transport for the OSM importer
#
# This file is part of the "UrMoAC" accessibility tool
# https://github.com/DLR-VF/UrMoAC
# Licensed under the Eclipse Public License 2.0
#
# Copyright (c) 2016-2023 DLR Institute of Transport Research
# All rights reserved.
# =========================================================
  
FOOT = 1
BICYCLE = 2
PASSENGER = 4
MOTORCYCLE = 8
DELIVERY = 16
HGV = 32
PSV = 64
BUS = 128

HORSE = 256
MOPED = 512
EMERGENCY = 1024
RAIL = 2048
TRAM = 4096
TAXI = 8192
SKI = 16384
INLINE_SKATES = 32768

ICE_SKATES = 65536
CARRIAGE = 131072
TRAILER = 262144
CARAVAN = 524288
MOFA = 1048576
MOTORHOME = 2097152
TOURIST_BUS = 4194304
GOODS = 8388608

AGRICULTURAL = 16777216
ATV = 33554432
SNOWMOBILE = 67108864
HOV = 134217728
CAR_SHARING = 268435456
HAZMAT = 536870912
DISABLED = 1073741824

CLOSED = 2147483648

#PRIVATE = 8192
#DESTINATION = 16384
#PERMISSIVE = 32768

PRIVATE_MOTORISED = PASSENGER|MOTORCYCLE|HGV|DELIVERY|BUS|MOPED|TAXI
PUBLIC_MOTORISED = PSV|TRAM|EMERGENCY
MOTORISED = PRIVATE_MOTORISED|PUBLIC_MOTORISED|EMERGENCY
SOFT = FOOT|BICYCLE|HORSE
ALL = MOTORISED|SOFT
VEHICLE = ALL&~FOOT


modes1 = {
  PASSENGER:      {"mml":"passenger",     "vmax":200/3.6 },
  MOTORCYCLE:     {"mml":"motorcycle",    "vmax":120/3.6 },
  DELIVERY:       {"mml":"delivery",      "vmax":160/3.6 },
  HGV:            {"mml":"hgv",           "vmax":100/3.6 },
  PSV:            {"mml":"psv",           "vmax":80/3.6 },
  BUS:            {"mml":"bus",           "vmax":120/3.6 },

  FOOT:           {"mml":"foot",          "vmax":5/3.6 },
  BICYCLE:        {"mml":"bicycle",       "vmax":15/3.6 },
  HORSE:          {"mml":"horse",         "vmax":200/3.6 },
  MOPED:          {"mml":"moped",         "vmax":200/3.6 },

  EMERGENCY:      {"mml":"emergency",     "vmax":200/3.6 },
  RAIL:           {"mml":"rail",          "vmax":200/3.6 },
  TRAM:           {"mml":"tram",          "vmax":200/3.6 },
  TAXI:           {"mml":"taxi",          "vmax":200/3.6 },

  SKI:            {"mml":"ski",           "vmax":200/3.6 },
  INLINE_SKATES:  {"mml":"inline_skates", "vmax":200/3.6 },
  ICE_SKATES:     {"mml":"ice_skates",    "vmax":200/3.6 },
  CARRIAGE:       {"mml":"carriage",      "vmax":200/3.6 },
  TRAILER:        {"mml":"trailer",       "vmax":200/3.6 },
  CARAVAN:        {"mml":"caravan",       "vmax":200/3.6 },
  MOFA:           {"mml":"mofa",          "vmax":200/3.6 },
  MOTORHOME:      {"mml":"motorhome",     "vmax":200/3.6 },
  TOURIST_BUS:    {"mml":"tourist_bus",   "vmax":200/3.6 },
  GOODS:          {"mml":"goods",         "vmax":200/3.6 },
  AGRICULTURAL:   {"mml":"agricultural",  "vmax":200/3.6 },
  ATV:            {"mml":"atv",           "vmax":200/3.6 },
  SNOWMOBILE:     {"mml":"snowmobile",    "vmax":200/3.6 },
  HOV:            {"mml":"hov",           "vmax":200/3.6 },
  CAR_SHARING:    {"mml":"car_sharing",   "vmax":200/3.6 },
  HAZMAT:         {"mml":"hazmat",        "vmax":200/3.6 },
  DISABLED:       {"mml":"disabled",      "vmax":200/3.6 },

  CLOSED:         {"mml":"-closed-",      "vmax":1/3.6 },

  PRIVATE_MOTORISED:  {"mml":"-private_motorised-", "vmax":200/3.6 },
  PUBLIC_MOTORISED:   {"mml":"-public_motorised-",  "vmax":200/3.6 },
  MOTORISED:      {"mml":"-motorised-",   "vmax":200/3.6 },
  SOFT:           {"mml":"-soft-",        "vmax":200/3.6 },
  ALL:            {"mml":"-all-",         "vmax":200/3.6 },
  VEHICLE:        {"mml":"-vehicle-",     "vmax":200/3.6 }
}