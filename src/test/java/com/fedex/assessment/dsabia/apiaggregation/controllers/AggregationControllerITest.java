package com.fedex.assessment.dsabia.apiaggregation.controllers;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.net.InetAddress;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AggregationControllerITest {

    @Autowired
    private MockMvc mockMvc;

    public static MockWebServer mockBackEnd;


    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start(4000);
        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return switch (request.getPath()) {
                    case "/shipment-products?orderNumber=987654321" -> new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("[\"BOX\", \"BOX\", \"PALLET\"]");
                    case "/shipment-products?orderNumber=109347263" -> new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("[\"BOX\"]");
                    case "/track-status?orderNumber=123456789" -> new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("\"COLLECTING\"");
                    case "/track-status?orderNumber=109347263" -> new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("\"IN_TRANSIT\"");
                    case "/pricing?countryCode=NL" -> new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("14.242090605778");
                    case "/pricing?countryCode=CN" -> new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("20.503467806384");
                    default -> new MockResponse().setResponseCode(404);
                };
            }
        };
        mockBackEnd.setDispatcher(dispatcher);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }


    @Test
    public void getAggregationWithAssignmentExample() throws Exception {

        this.mockMvc.perform(MockMvcRequestBuilders
                        .get("/aggregation?shipmentsOrderNumbers=987654321,123456789&trackOrderNumbers=987654321,123456789&pricingCountryCodes=NL,CN"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipments").exists())
                .andExpect(jsonPath("$.shipments").isNotEmpty())
                .andExpect(jsonPath("$.shipments.987654321").isArray())
                .andExpect(jsonPath("$.shipments.987654321[0]").value("BOX"))
                .andExpect(jsonPath("$.shipments.987654321[1]").value("PALLET"))
                .andExpect(jsonPath("$.track").exists())
                .andExpect(jsonPath("$.track").isNotEmpty())
                .andExpect(jsonPath("$.track.123456789").isNotEmpty())
                .andExpect(jsonPath("$.track.123456789").value("COLLECTING"))
                .andExpect(jsonPath("$.pricing").exists())
                .andExpect(jsonPath("$.pricing").isNotEmpty())
                .andExpect(jsonPath("$.pricing.CN").isNotEmpty())
                .andExpect(jsonPath("$.pricing.CN").value(20.503467806384D))
                .andExpect(jsonPath("$.pricing.NL").isNotEmpty())
                .andExpect(jsonPath("$.pricing.NL").value(14.242090605778D));
    }

    @Test
    public void testGetAggregationWithPricingParameter() throws Exception {

        this.mockMvc.perform(MockMvcRequestBuilders
                        .get("/aggregation?pricingCountryCodes=NL"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipments").exists())
                .andExpect(jsonPath("$.shipments").isEmpty())
                .andExpect(jsonPath("$.track").exists())
                .andExpect(jsonPath("$.track").isEmpty())
                .andExpect(jsonPath("$.pricing").exists())
                .andExpect(jsonPath("$.pricing").isNotEmpty());
    }

    @Test
    public void testGetAggregationWithoutParameters() throws Exception {

        this.mockMvc.perform(MockMvcRequestBuilders
                        .get("/aggregation?pricingCountryCodes=NL"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipments").exists())
                .andExpect(jsonPath("$.shipments").isEmpty())
                .andExpect(jsonPath("$.track").exists())
                .andExpect(jsonPath("$.track").isEmpty())
                .andExpect(jsonPath("$.pricing").exists())
                .andExpect(jsonPath("$.pricing").isNotEmpty());
    }
}