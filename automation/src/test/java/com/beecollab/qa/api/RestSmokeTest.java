package com.beecollab.qa.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RestSmokeTest extends ApiBaseTest {

    @Test
    @DisplayName("L1-API-01: Health check endpoint reports database status UP")
    void testHealthEndpoint() {
        given()
                .when()
                .get("/health")
                .then()
                .statusCode(200)
                .body("data.db", equalTo("up"))
                .body("data.status", equalTo("ok"));
    }

    @Test
    @DisplayName("L1-API-02: Register validation rejects invalid payloads with 400 Bad Request")
    void testRegisterValidation() {
        // Invalid email format
        registerUser("Valid Name", "invalid-email-format", TEST_PASSWORD)
                .then()
                .statusCode(400);

        // Short password (< 6 chars)
        registerUser("Valid Name", uniqueEmail("shortpwd"), "123")
                .then()
                .statusCode(400);

        // Short name (< 2 chars)
        registerUser("A", uniqueEmail("shortname"), TEST_PASSWORD)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("L1-API-03: Register creates user with JWT token and rejects duplicate emails with 409")
    void testRegisterSuccessAndDuplicate() {
        String email = uniqueEmail("regtest");

        // First registration succeeds
        Response resp = registerUser("Smoke User", email, TEST_PASSWORD);
        resp.then()
                .statusCode(201)
                .body("data.access_token", notNullValue());

        // Duplicate registration must return 409 Conflict
        registerUser("Duplicate User", email, TEST_PASSWORD)
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("L1-API-04: Login handles authentication credentials and issues valid token")
    void testLoginFlow() {
        String email = uniqueEmail("logintest");
        registerUser("Login Tester", email, TEST_PASSWORD).then().statusCode(201);

        // Wrong password -> 401
        loginUser(email, "WrongPassword123!")
                .then()
                .statusCode(401);

        // Non-existent email -> 401
        loginUser("nonexistent@qa.beecollab.test", TEST_PASSWORD)
                .then()
                .statusCode(401);

        // Valid login -> 201 with access_token
        loginUser(email, TEST_PASSWORD)
                .then()
                .statusCode(201)
                .body("data.access_token", notNullValue());
    }

    @Test
    @DisplayName("L1-API-05: User profile endpoint /users/me protects against unauthenticated access")
    void testUsersMeProfile() {
        // Unauthenticated -> 401
        given()
                .when()
                .get("/users/me")
                .then()
                .statusCode(401);

        // Authenticated -> 200 with profile and no leaked passwordHash
        String email = uniqueEmail("metest");
        Response regResp = registerUser("Me Tester", email, TEST_PASSWORD);
        String token = extractToken(regResp);
        assertNotNull(token, "Access token must not be null");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/users/me")
                .then()
                .statusCode(200)
                .body("data.email", equalTo(email))
                .body("data.name", equalTo("Me Tester"))
                .body("data.passwordHash", nullValue())
                .body("data.password", nullValue());
    }

    @Test
    @DisplayName("L1-API-06: Guest registration creates anonymous session with restricted privileges")
    void testGuestRegistration() {
        // Short name -> 400
        registerGuest("A")
                .then()
                .statusCode(400);

        // Valid guest -> 201 with access_token
        Response guestResp = registerGuest("Guest Walker");
        guestResp.then()
                .statusCode(201)
                .body("data.access_token", notNullValue());

        String guestToken = extractToken(guestResp);
        assertNotNull(guestToken);

        // Guest attempting to create meeting -> 403 Forbidden
        createMeeting(guestToken, "Illegal Guest Meeting", 30, 10)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("L1-API-07: Meeting CRUD lifecycle handles creation, lookup, and host deletion")
    void testMeetingCrudLifecycle() {
        String email = uniqueEmail("hostcrud");
        Response hostResp = registerUser("Meeting Host", email, TEST_PASSWORD);
        String hostToken = extractToken(hostResp);
        assertNotNull(hostToken, "Host token must not be null");

        // 1. Create meeting
        Response createResp = createMeeting(hostToken, "Java REST Assured Meeting", 45, 20);
        createResp.then()
                .statusCode(201)
                .body("data.id", notNullValue())
                .body("data.roomCode", matchesPattern("^[0-9A-F]{8}$"))
                .body("data.status", equalTo("SCHEDULED"));

        String meetingId = createResp.jsonPath().getString("data.id");
        String roomCode = createResp.jsonPath().getString("data.roomCode");

        // 2. Lookup meeting by room code
        given()
                .header("Authorization", "Bearer " + hostToken)
                .when()
                .get("/meetings/code/" + roomCode)
                .then()
                .statusCode(200)
                .body("data.id", equalTo(meetingId))
                .body("data.roomCode", equalTo(roomCode));

        // 3. Delete meeting as host -> 200
        given()
                .header("Authorization", "Bearer " + hostToken)
                .when()
                .delete("/meetings/" + meetingId)
                .then()
                .statusCode(200);

        // 4. Query deleted meeting -> 404
        given()
                .header("Authorization", "Bearer " + hostToken)
                .when()
                .get("/meetings/" + meetingId)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("L1-API-08: WebRTC ICE servers configuration endpoint delivers STUN servers")
    void testWebRtcIceServers() {
        Response guestResp = registerGuest("ICE Checker");
        String guestToken = extractToken(guestResp);
        assertNotNull(guestToken);

        given()
                .header("Authorization", "Bearer " + guestToken)
                .when()
                .get("/webrtc/ice-servers")
                .then()
                .statusCode(200)
                .body("data.iceServers", notNullValue());
    }
}
