package com.karatly.config;

import com.karatly.service.rate.provider.GoldApiIoProvider;
import com.karatly.service.rate.provider.GoldPriceDevProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** One HTTP client per rate provider, each with its own address and timeout. */
@Configuration
public class RateClientConfig {

    @Bean
    GoldPriceDevProvider goldPriceDevProvider(RatesProperties properties) {
        RatesProperties.Provider settings = properties.goldpriceDev();
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(settings.baseUrl())
                .requestFactory(requestFactory(settings));
        if (settings.hasApiKey()) {
            builder.defaultHeader("Authorization", "Bearer " + settings.apiKey());
        }
        return new GoldPriceDevProvider(builder.build(), settings);
    }

    @Bean
    GoldApiIoProvider goldApiIoProvider(RatesProperties properties) {
        RatesProperties.Provider settings = properties.goldapiIo();
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(settings.baseUrl())
                .requestFactory(requestFactory(settings));
        if (settings.hasApiKey()) {
            builder.defaultHeader("x-access-token", settings.apiKey());
        }
        return new GoldApiIoProvider(builder.build(), settings);
    }

    private static SimpleClientHttpRequestFactory requestFactory(RatesProperties.Provider settings) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(settings.timeout());
        factory.setReadTimeout(settings.timeout());
        return factory;
    }
}
