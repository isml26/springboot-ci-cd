package com.example.OrderService.controller;

import com.example.OrderService.OrderServiceConfig;
import com.example.OrderService.entity.Order;
import com.example.OrderService.model.OrderRequest;
import com.example.OrderService.model.OrderResponse;
import com.example.OrderService.model.PaymentMode;
import com.example.OrderService.model.ProductRequest;
import com.example.OrderService.repository.OrderRepository;
import com.example.OrderService.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.util.StreamUtils.copyToString;


@SpringBootTest({"server.port=0"})
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

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @RegisterExtension
    static WireMockExtension wireMockServer
            = WireMockExtension.newInstance()
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
//        addProduct();
        getProductDetailsResponse();
        doPayment();
        getPaymentDetails();
        reduceQuantity();
    }

    private void reduceQuantity() {
        circuitBreakerRegistry.circuitBreaker("external").reset();
        wireMockServer.stubFor(put(urlMatching("/products/reduceQuantity/.*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
    }


//    private void addProduct() throws JsonProcessingException {
//        ProductRequest productRequest = new ProductRequest();
//        productRequest.setProductName("SamsungS20");
//        productRequest.setPrice(1500);
//        productRequest.setQuantity(200);
//
//        circuitBreakerRegistry.circuitBreaker("external").reset();
//        wireMockServer.stubFor(post(urlMatching("/products"))
//                .withRequestBody(equalToJson(objectMapper.writeValueAsString(productRequest))) // Check the JSON equality
//                .willReturn(aResponse()
//                        .withStatus(HttpStatus.OK.value())
//                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE))
//        );
//    }



    private void getPaymentDetails() throws IOException {
        circuitBreakerRegistry.circuitBreaker("external").reset();
        wireMockServer.stubFor(get(urlMatching("/payment/.*"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                                copyToString(
                                        OrderControllerTest.class
                                                .getClassLoader()
                                                .getResourceAsStream("mock/GetPayment.json"),
                                        defaultCharset()
                                )
                        )));
    }

    private void doPayment() {
        wireMockServer.stubFor(post(urlEqualTo("/payment"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)));
    }

    private void getProductDetailsResponse() throws IOException {
        // GET /products/1
        wireMockServer.stubFor(WireMock.get("/products/1")
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(copyToString(
                                OrderController.class.getClassLoader()
                                        .getResourceAsStream("mock/GetProduct.json"),
                                Charset.defaultCharset()
                        ))));
    }

    private OrderRequest getMockOrderRequest() {
        return OrderRequest.builder()
                .productId(1)
                .paymentMode(PaymentMode.CASH)
                .quantity(10)
                .totalAmount(200)
                .build();
    }


    @Test
    public void test_WhenPlaceOrder_DoPayment_Success() throws Exception {
        // Place order

        // Get order by OrderId from DB

        // Check output

        OrderRequest orderRequest = getMockOrderRequest();

        MvcResult mvcResult =  mockMvc.perform(MockMvcRequestBuilders.post("/orders/placeOrder")
                .with(jwt().authorities(new SimpleGrantedAuthority("Customer")))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(orderRequest))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String orderId = mvcResult.getResponse().getContentAsString();

        Optional<Order> order = orderRepository.findById(Long.valueOf(orderId));
        Assertions.assertTrue(order.isPresent());

        Order getOrder = order.get();

        assertEquals(Long.parseLong(orderId),getOrder.getOrderId());
        assertEquals(orderRequest.getTotalAmount(),getOrder.getOrderAmount());
        assertEquals(orderRequest.getQuantity(),getOrder.getQuantity());

    }

    @Test
    public void test_WhenPlaceOrderWithWringAccess_thenThrow403() throws Exception {

        OrderRequest orderRequest = getMockOrderRequest();

        MvcResult mvcResult =  mockMvc.perform(MockMvcRequestBuilders.post("/orders/placeOrder")
                .with(jwt().authorities(new SimpleGrantedAuthority("Admin")))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(orderRequest))
        ).andExpect(MockMvcResultMatchers.status().isForbidden()).andReturn();
    }

    // To get correct result of that test we should run all test from the beginning of the class
    // Run class OrderControllerTest
    @Test
    public void test_WhenGetOrder_Success() throws Exception {
        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.get("/orders/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Admin")))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        Order order = orderRepository.findById(1l).get();
        String expectedResponse = getOrderResponse(order);

        assertEquals(expectedResponse,actualResponse);
    }

    @Test
    public void test_When_GetOrder_Order_Not_Found() throws Exception {
        MvcResult mvcResult
                = mockMvc.perform(MockMvcRequestBuilders.get("/orders/2")
                        .with(jwt().authorities(new SimpleGrantedAuthority("Admin")))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andReturn();

    }


    private String getOrderResponse(Order order) throws IOException {
        OrderResponse.PaymentDetails paymentDetails
                = objectMapper.readValue(
                copyToString(
                        OrderControllerTest.class.getClassLoader()
                                .getResourceAsStream("mock/GetPayment.json"
                                ),
                        defaultCharset()
                ), OrderResponse.PaymentDetails.class
        );
        paymentDetails.setPaymentStatus("SUCCESS");

        OrderResponse.ProductDetails productDetails
                = objectMapper.readValue(
                copyToString(
                        OrderControllerTest.class.getClassLoader()
                                .getResourceAsStream("mock/GetProduct.json"),
                        defaultCharset()
                ), OrderResponse.ProductDetails.class
        );

        OrderResponse orderResponse
                = OrderResponse.builder()
                .paymentDetails(paymentDetails)
                .productDetails(productDetails)
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getOrderDate())
                .amount(order.getOrderAmount())
                .orderId(order.getOrderId())
                .build();
        return objectMapper.writeValueAsString(orderResponse);
    }

}