package com.beecollab.qa.api;

import com.beecollab.qa.config.Config;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;

import java.util.UUID;

import static io.restassured.RestAssured.given;

public abstract class ApiBaseTest {

    protected static final String TEST_PASSWORD = "QaPassw0rd!";

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.baseURI = Config.getApiUrl();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 5) + "@qa.beecollab.test";
    }

    protected String extractToken(Response response) {
        String token = response.jsonPath().getString("data.access_token");
        if (token == null) {
            token = response.jsonPath().getString("access_token");
        }
        return token;
    }

    protected Response registerUser(String name, String email, String password) {
        JSONObject body = new JSONObject();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);

        return given()
                .contentType(ContentType.JSON)
                .body(body.toString())
                .when()
                .post("/auth/register");
    }

    protected Response loginUser(String email, String password) {
        JSONObject body = new JSONObject();
        body.put("email", email);
        body.put("password", password);

        return given()
                .contentType(ContentType.JSON)
                .body(body.toString())
                .when()
                .post("/auth/login");
    }

    protected Response registerGuest(String name) {
        JSONObject body = new JSONObject();
        body.put("name", name);

        return given()
                .contentType(ContentType.JSON)
                .body(body.toString())
                .when()
                .post("/auth/guest");
    }

    protected Response createMeeting(String token, String title, int duration, int maxParticipants) {
        JSONObject body = new JSONObject();
        body.put("title", title);
        body.put("duration", duration);
        body.put("maxParticipants", maxParticipants);

        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(body.toString())
                .when()
                .post("/meetings");
    }
}
