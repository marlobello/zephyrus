package com.zephyrus.app.util

/**
 * Maps common US airport IATA codes to city names for geocoding.
 * Open-Meteo geocoding doesn't support airport codes natively.
 */
object AirportCodes {

    private val codes = mapOf(
        "ATL" to "Atlanta",
        "LAX" to "Los Angeles",
        "ORD" to "Chicago",
        "DFW" to "Dallas",
        "DEN" to "Denver",
        "JFK" to "New York",
        "SFO" to "San Francisco",
        "SEA" to "Seattle",
        "LAS" to "Las Vegas",
        "MCO" to "Orlando",
        "EWR" to "Newark",
        "MIA" to "Miami",
        "PHX" to "Phoenix",
        "IAH" to "Houston",
        "BOS" to "Boston",
        "MSP" to "Minneapolis",
        "DTW" to "Detroit",
        "FLL" to "Fort Lauderdale",
        "PHL" to "Philadelphia",
        "LGA" to "New York",
        "BWI" to "Baltimore",
        "SLC" to "Salt Lake City",
        "DCA" to "Washington",
        "IAD" to "Washington",
        "SAN" to "San Diego",
        "TPA" to "Tampa",
        "PDX" to "Portland",
        "HNL" to "Honolulu",
        "STL" to "St. Louis",
        "BNA" to "Nashville",
        "AUS" to "Austin",
        "MSY" to "New Orleans",
        "RDU" to "Raleigh",
        "CLT" to "Charlotte",
        "MCI" to "Kansas City",
        "SMF" to "Sacramento",
        "SJC" to "San Jose",
        "OAK" to "Oakland",
        "PIT" to "Pittsburgh",
        "CLE" to "Cleveland",
        "IND" to "Indianapolis",
        "CVG" to "Cincinnati",
        "CMH" to "Columbus",
        "MKE" to "Milwaukee",
        "JAX" to "Jacksonville",
        "RNO" to "Reno",
        "ABQ" to "Albuquerque",
        "OMA" to "Omaha",
        "BUF" to "Buffalo",
        "ANC" to "Anchorage",
    )

    /** Returns the city name for a given IATA code, or null if not found. */
    fun toCityName(code: String): String? = codes[code]
}
