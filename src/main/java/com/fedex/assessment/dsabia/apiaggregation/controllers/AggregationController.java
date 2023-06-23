package com.fedex.assessment.dsabia.apiaggregation.controllers;

import com.fedex.assessment.dsabia.apiaggregation.dto.Aggregation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestController
@PropertySource("classpath:application.properties")
public class AggregationController {

    @Autowired
    @Value("${backend-services.url}")
    private String backendServicesUrl;

    private int BACKEND_SERVICE_TIMEOUT = 5000;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Example:
     * http://127.0.0.1:8080/aggregation?shipmentsOrderNumbers=987654321,123456789&trackOrderNumbers=987654321,123456789&pricingCountryCodes=NL,CN
     */
    @GetMapping(value = "/aggregation")
    public Aggregation getAggregation(
            @RequestParam @Nullable List<String> shipmentsOrderNumbers,
            @RequestParam @Nullable List<String> trackOrderNumbers,
            @RequestParam @Nullable List<String> pricingCountryCodes
    ) {
        final Map<String, Collection<String>> shipments = new HashMap<>();
        final Map<String, String> track = new HashMap<>();
        final Map<String, BigDecimal> pricing = new HashMap<>();

        final CompletableFuture<Void> shipmentsFuture = CompletableFuture.runAsync(() -> getShipmentsStream(shipmentsOrderNumbers)
                .forEach(entry -> shipments.put(entry.getKey(), entry.getValue())));
        final CompletableFuture<Void> tracksFuture = CompletableFuture.runAsync(() -> getTracksStream(trackOrderNumbers)
                .forEach(entry -> track.put(entry.getKey(), entry.getValue())));
        final CompletableFuture<Void> pricingFuture = CompletableFuture.runAsync(() -> getPricingStream(pricingCountryCodes)
                .forEach(entry -> pricing.put(entry.getKey(), entry.getValue())));

        Stream.of(shipmentsFuture, tracksFuture, pricingFuture).forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        return Aggregation
                .builder()
                .shipments(shipments)
                .track(track)
                .pricing(pricing)
                .build();
    }

    @SuppressWarnings("rawtypes")
    private Stream<Map.Entry<String, Collection<String>>> getShipmentsStream(List<String> shipmentsOrderNumbers) {
        return Stream.ofNullable(shipmentsOrderNumbers)
                .flatMap(Collection::stream)
                .map(orderNumber -> Optional.ofNullable(orderNumber)
                        .map(_1 -> backendServicesUrl + "/shipment-products?orderNumber=" + orderNumber)
                        .map(uri -> CompletableFuture.supplyAsync(
                                () -> restTemplate.exchange(uri, HttpMethod.GET, null, Collection.class)))
                        .map(future -> {
                            try {
                                return future.get(BACKEND_SERVICE_TIMEOUT, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                                return null;
                            }
                        })
                        .map(HttpEntity::getBody)
                        .map(trackStatus -> (Collection<String>) trackStatus
                                .stream()
                                .distinct()
                                .collect(Collectors.toList())
                        )
                        .map(trackStatus -> Map.entry(orderNumber, trackStatus))
                )
                .flatMap(Optional::stream);
    }

    private Stream<Map.Entry<String, String>> getTracksStream(List<String> trackOrderNumbers) {
        return Stream.ofNullable(trackOrderNumbers)
                .flatMap(Collection::stream)
                .map(orderNumber -> Optional.ofNullable(orderNumber)
                        .map(_1 -> backendServicesUrl + "/track-status?orderNumber=" + orderNumber)
                        .map(uri -> CompletableFuture.supplyAsync(
                                        () -> restTemplate.exchange(uri, HttpMethod.GET, null, String.class)
                                )
                        )
                        .map(future -> {
                            try {
                                return future.get(BACKEND_SERVICE_TIMEOUT, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                                return null;
                            }
                        })
                        .map(HttpEntity::getBody)
                        .map(this::unquote)
                        .map(trackStatus -> Map.entry(orderNumber, trackStatus))
                )
                .flatMap(Optional::stream);
    }

    private String unquote(@NonNull String input) {
        return input.replaceAll("^\"|\"$", "");
    }

    private Stream<Map.Entry<String, BigDecimal>> getPricingStream(List<String> pricingCountryCodes) {
        return Stream.ofNullable(pricingCountryCodes)
                .flatMap(Collection::stream)
                .map(countryCode -> Optional.ofNullable(countryCode)
                        .map(_1 -> backendServicesUrl + "/pricing?countryCode=" + countryCode)
                        .map(uri -> CompletableFuture.supplyAsync(
                                () -> restTemplate.exchange(uri, HttpMethod.GET, null, BigDecimal.class)))
                        .map(future -> {
                            try {
                                return future.get(BACKEND_SERVICE_TIMEOUT, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                                log.warn("response exception {}", e.getMessage());
                                return null;
                            }
                        })
                        .map(HttpEntity::getBody)
                        .map(price -> Map.entry(countryCode, price))
                )
                .flatMap(Optional::stream);
    }
}
