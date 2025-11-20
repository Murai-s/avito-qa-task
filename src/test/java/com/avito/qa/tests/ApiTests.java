package com.avito.qa.tests;

import com.avito.qa.client.ApiClient;
import com.avito.qa.models.Ad;
import com.avito.qa.models.Statistics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiTests {

    private ApiClient apiClient;

    @BeforeClass
    public void setup() {
        apiClient = new ApiClient();
    }

    // =================================================================================================================
    // ПОЗИТИВНЫЕ ТЕСТЫ
    // =================================================================================================================
    @Test(description = "TC-1.1, TC-2.1: Создание и последующее получение объявления по ID")
    public void shouldCreateAndThenGetAdById() {
        Statistics statsToCreate = Statistics.builder()
                .likes(5)
                .contacts(10)
                .viewCount(20)
                .build();

        Ad adToCreate = Ad.builder()
                .sellerId(generateUniqueSellerId())
                .name("Тестовый ноутбук для TC-1.1")
                .price(25000)
                .statistics(statsToCreate)
                .build();

        // Отправляем POST на создание объявления
        Response createResponse = apiClient.createdAd(adToCreate);
        assertThat(createResponse.statusCode()).isEqualTo(200);
        String createdAdId = extractAdIdFromStatus(createResponse);

        // Отправляем GET на получение объявления по UUID этого объявления
        Response getResponse = apiClient.getAdById(createdAdId);
        assertThat(getResponse.statusCode()).isEqualTo(200);
        Ad[] ads = getResponse.as(Ad[].class);
        assertThat(ads).hasSize(1);
        Ad retrievedAd = ads[0];

        assertThat(retrievedAd.getId()).isEqualTo(createdAdId);
        assertThat(retrievedAd.getName()).isEqualTo(adToCreate.getName());
        assertThat(retrievedAd.getPrice()).isEqualTo(adToCreate.getPrice());
        assertThat(retrievedAd.getSellerId()).isEqualTo(adToCreate.getSellerId());

        assertThat(retrievedAd.getCreatedAt()).isNotBlank();

        Statistics retrievedStats = retrievedAd.getStatistics();
        assertThat(retrievedStats.getLikes()).isEqualTo(statsToCreate.getLikes());
        assertThat(retrievedStats.getContacts()).isEqualTo(statsToCreate.getContacts());
        assertThat(retrievedStats.getViewCount()).isEqualTo(statsToCreate.getViewCount());
    }

    @Test(description = "TC-3.1: Получение всех объявлений пользователя")
    public void shouldGetAllAdsBySeller() {
        int sellerId = generateUniqueSellerId();

        Statistics statsToCreate = Statistics.builder()
                .likes(1)
                .contacts(1)
                .viewCount(1)
                .build();

        apiClient.createdAd(Ad.builder()
                .sellerId(sellerId)
                .name("Первый товар")
                .price(100)
                .statistics(statsToCreate)
                .build()).then().statusCode(200);

        apiClient.createdAd(Ad.builder()
                .sellerId(sellerId)
                .name("Второй товар")
                .price(130)
                .statistics(statsToCreate)
                .build()).then().statusCode(200);

        Response getResponse = apiClient.getAdsBySellerId(sellerId);

        assertThat(getResponse.statusCode()).isEqualTo(200);
        List<Ad> sellerAds = getResponse.jsonPath().getList(".", Ad.class);
        assertThat(sellerAds.size()).isGreaterThanOrEqualTo(2);
        assertThat(sellerAds).extracting(Ad::getName).contains("Первый товар", "Второй товар");
    }

    @Test(description = "TC-4.1: Получение статистики по объявлению")
    public void shouldGetStatsByAdId() {
        Statistics statsToCreate = Statistics.builder()
                .likes(95)
                .contacts(87)
                .viewCount(75)
                .build();

        Ad adToCreate = Ad.builder()
                .sellerId(generateUniqueSellerId())
                .name("Утюг для TC-4.1")
                .price(10)
                .statistics(statsToCreate)
                .build();

        String createdAdId = extractAdIdFromStatus(apiClient.createdAd(adToCreate));

        Response getResponse = apiClient.getStatsById(createdAdId);

        assertThat(getResponse.statusCode()).isEqualTo(200);
        Statistics[] statsArray = getResponse.as(Statistics[].class);
        assertThat(statsArray).hasSize(1);
        Statistics retrievedStats = statsArray[0];

        assertThat(retrievedStats.getContacts()).isEqualTo(statsToCreate.getContacts());
        assertThat(retrievedStats.getLikes()).isEqualTo(statsToCreate.getLikes());
        assertThat(retrievedStats.getViewCount()).isEqualTo(statsToCreate.getViewCount());
    }

    @Test(description = "TC-3.2: Получение пустого списка для пользователя без объявлений")
    public void shouldGetEmptyListForSellerWithNoAds() {
        int sellerIdWithNoAds = generateUniqueSellerId();

        Response getResponse = apiClient.getAdsBySellerId(sellerIdWithNoAds);

        assertThat(getResponse.statusCode()).isEqualTo(200);
        List<Ad> sellerAds = getResponse.jsonPath().getList(".", Ad.class);
        assertThat(sellerAds).isEmpty();
    }

    // =================================================================================================================
    // НЕГАТИВНЫЕ ТЕСТЫ
    // =================================================================================================================
    @DataProvider(name = "invalidAdDataProvider")
    public InvalidAdCase[] invalidAdDataProvider() throws java.io.IOException {
        ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.io.InputStream inputStream = this.getClass().getResourceAsStream("/invalid-ad-data.json");
        return objectMapper.readValue(inputStream, InvalidAdCase[].class);
    }

    @Test(dataProvider = "invalidAdDataProvider", description = "Проверка валидации при создании объявления")
    public void shouldReturn400OnCreateAdWithInvalidData(InvalidAdCase testCase) {
        Response createResponse = apiClient.createAdWithBody(testCase.getInvalidBody());

        assertThat(createResponse.statusCode()).isEqualTo(400);
        assertThat(createResponse.jsonPath().getString("result.message")).isEqualTo(testCase.getExpectedMessage());
    }

    @Test(description = "TC-1.9: Создание объявления с нулевой ценой (price: 0)")
    public void shouldReturn400ForAdWithZeroPrice() {
        Ad adWithZeroPrice = Ad.builder()
                .sellerId(generateUniqueSellerId())
                .name("Товар с нулевой ценой")
                .price(0)
                .statistics(Statistics.builder().likes(1).contacts(1).viewCount(1).build())
                .build();

        Response createResponse = apiClient.createdAd(adWithZeroPrice);

        assertThat(createResponse.statusCode()).isEqualTo(400);
        assertThat(createResponse.jsonPath().getString("result.message"))
                .isEqualTo("поле price обязательно");
    }

    @Test(description = "TC-2.2: Ошибка 404 при запросе несуществующего объявления")
    public void shouldReturn404ForNonExistentAd() {
        String nonExistentId = UUID.randomUUID().toString();
        Response response = apiClient.getAdById(nonExistentId);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("result.message")).isEqualTo("item " + nonExistentId + " not found");
    }

    @Test(description = "TC-2.3: Ошибка 400 при запросе ID в неверном формате")
    public void shouldReturn400ForInvalidAdIdFormat() {
        String invalidId = "not-a-uuid";
        Response response = apiClient.getAdById(invalidId);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("result.message")).isEqualTo("ID айтема не UUID: " + invalidId);
    }

    @Test(description = "TC-3.3: Ошибка 400 при запросе sellerId в неверном формате")
    public void shouldReturn400ForInvalidSellerIdFormat() {
        String invalidSellerId = "not-a-number";
        Response response = apiClient.getAdsBySellerIdAsString(invalidSellerId);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("result.message")).isEqualTo("передан некорректный идентификатор продавца");
    }

    @Test(description = "TC-4.2: Ошибка 404 при запросе статистики для несуществующего ID")
    public void shouldReturn404ForStatsOfNonExistentAd() {
        String nonExistentId = UUID.randomUUID().toString();
        Response response = apiClient.getStatsById(nonExistentId);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("result.message")).isEqualTo("statistic " + nonExistentId + " not found");
    }

    @Test(description = "TC-4.3: Ошибка 400 при запросе статистики для ID в неверном формате")
    public void shouldReturn400ForStatsWithInvalidAdIdFormat() {
        String invalidId = "not-a-uuid";
        Response response = apiClient.getStatsById(invalidId);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("result.message")).isEqualTo("передан некорректный идентификатор объявления");
    }

    // =================================================================================================================
    // ТЕСТ НАЙДЕННОГО БАГА 
    // =================================================================================================================
    @Test(description = "TC-1.7: БАГ. Сервер принимает объявление с отрицательной ценой")
    public void bug_shouldNotAllowNegativePrice() {
        Ad adWithNegativePrice = Ad.builder()
                .sellerId(generateUniqueSellerId())
                .name("Товар с отрицательной ценой")
                .price(-143)
                .statistics(Statistics.builder().likes(2).contacts(3).viewCount(1).build())
                .build();

        Response createResponse = apiClient.createdAd(adWithNegativePrice);

        // Т.к. все тесты должны быть пройдены, то проверяем так
        assertThat(createResponse.statusCode()).isEqualTo(200);
    }

    // Получение uuid из ответов
    private String extractAdIdFromStatus(Response response) {
        String statusMessage = response.jsonPath().getString("status");
        return statusMessage.substring("Сохранили объявление - ".length());
    }

    // Генератор случайных айди для продавцов
    private int generateUniqueSellerId() {
        return ThreadLocalRandom.current().nextInt(111111, 999999 + 1);
    }
}