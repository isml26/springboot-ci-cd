package com.example.OrderService.controller;

import com.example.OrderService.OrderServiceConfig;
import com.example.OrderService.repository.OrderRepository;
import com.example.OrderService.service.OrderService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest("{server.port=0}")
@EnableConfigurationProperties
@AutoConfigureMockMvc
@ContextConfiguration(classes = {OrderServiceConfig.class})
class OrderControllerTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MockMvc mockMvc;

    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(WireMockConfiguration
                    .wireMockConfig()
                    .port(8080))
            .build();

    private ObjectMapper objectMapper
                    = new ObjectMapper()
                    .findAndRegisterModules()
                    .configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS,false)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);


    @BeforeEach
    void setup() throws IOException {
        getProductDetailsResponse();
        doPayment();
        getPaymentDetails();
        reduceQuantity();
    }

    private void reduceQuantity() {
        wireMockServer.stubFor(put(urlMatching("/products/reduceQuantity/.*"))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(MediaType.APPLICATION_JSON_VALUE))
        );
    }

    private void getPaymentDetails() throws IOException {
        wireMockServer.stubFor(WireMock.get(urlMatching("/payment/.*"))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(MediaType.APPLICATION_JSON_VALUE)
                        .withBody(StreamUtils.copyToString(
                                OrderController.class.getClassLoader()
                                        .getResourceAsStream("mock/GetPayment.json"),
                                Charset.defaultCharset())))
        );
    }

    private void doPayment() {
        wireMockServer.stubFor(WireMock.post(("/payment"))
                .willReturn(aResponse().withStatus(HttpStatus.OK.value())
                        .withHeader(MediaType.APPLICATION_JSON_VALUE)));
    }

    private void getProductDetailsResponse() throws IOException {
        // GET /products/1
        wireMockServer.stubFor(WireMock.get("/products/1")
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(StreamUtils.copyToString(
                                OrderController.class.getClassLoader()
                                        .getResourceAsStream("mock/GetProduct.json"),
                                Charset.defaultCharset()
                        ))));
    }


    @Test
    public void test_WhenPlaceOrder_DoPayment_Success(){
        // Place order


        // Get order by OrderId from DB

        // Check output

    }

}