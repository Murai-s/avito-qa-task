package com.avito.qa.client;

import com.avito.qa.models.Ad;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class ApiClient {

    private final RequestSpecification requestSpec;

    public ApiClient() {
        this.requestSpec = RestAssured.given()
                .baseUri("https://qa-internship.avito.com")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    // Создание объявления
    public Response createdAd(Ad ad) {
        return getRequestSpec()
                .body(ad)
                .when()
                .post("/api/1/item");
    }

    // Получение объявления по айди
    public Response getAdById(String adId) {
        return getRequestSpec()
                .pathParam("id", adId)
                .when()
                .get("/api/1/item/{id}");
    }

    // Получение всех объявлений продавца по его айди
    public Response getAdsBySellerId(int sellerId) {
        return getRequestSpec()
                .pathParam("sellerID", sellerId)
                .when()
                .get("/api/1/{sellerID}/item");
    }

    // Получение статистики объявления по его айди
    public Response getStatsById(String adId) {
        return getRequestSpec()
                .pathParam("id", adId)
                .when()
                .get("/api/1/statistic/{id}");
    }

    private RequestSpecification getRequestSpec() {
        return RestAssured.given(requestSpec);
    }

    public Response createAdWithBody(Object body) {
        return getRequestSpec()
                .body(body)
                .when()
                .post("/api/1/item");
    }

    public Response getAdsBySellerIdAsString(String sellerId) {
        return getRequestSpec()
                .pathParam("sellerID", sellerId)
                .when()
                .get("/api/1/{sellerID}/item");
    }
}