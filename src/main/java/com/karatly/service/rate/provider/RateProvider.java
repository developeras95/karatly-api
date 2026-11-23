package com.karatly.service.rate.provider;

import java.util.List;

import com.karatly.entity.rate.RateSource;

/**
 * An outside source of metal prices. Each provider covers some metals; RateService decides when to call
 * it (only when a cached rate is past the TTL, within the monthly budget) and stores what it returns.
 */
public interface RateProvider {

    /** Also the name stored with each rate and each logged call. */
    RateSource source();

    /** Metal codes this provider prices, e.g. XAU. */
    List<String> metals();

    /** False when switched off or missing a required API key; {@link #unavailableReason()} says why. */
    boolean isAvailable();

    String unavailableReason();

    /** HTTP requests one {@link #fetch} uses, counted against {@link #monthlyBudget()}. */
    int requestsPerFetch(String currency);

    int monthlyBudget();

    /** Spot prices per gram of pure metal in the currency, one per metal. Throws RateFetchException. */
    List<FetchedRate> fetch(String currency);
}
