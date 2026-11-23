package com.karatly.entity.rate;

/** Where a rate came from. MANUAL rates are typed in and win over fetched ones for the same day. */
public enum RateSource {
    GOLDPRICE_DEV,
    GOLDAPI_IO,
    MANUAL
}
