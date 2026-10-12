package com.beecollab.qa.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ApiSecurityDefectTest extends ApiBaseTest {

    private final List<String> createdMeetingIds = new ArrayList<>();
    private String hostTokenForCleanup;

    @AfterEach
    void tearDownMeetings() {
        if (hostTokenForCleanup != null) {
            for (String id : createdMeetingIds) {
                try {
                    given()
                            .header("Authorization", "Bearer " + hostTokenForCleanup)
                            .when()
                            .delete("/meetings/" + id);
                } catch (Exception ignored) {
                }
            }
        }
        createdMeetingIds.clear();
    }

    @Test
    @DisplayName("BUG-001 Verification: IDOR allows unauthorized Member B to access private meeting metadata")
    void testBug001_IdorMeetingMetadataLeak() {
        // 1. Create Host A and private meeting
        String hostEmail = uniqueEmail("host-a");
        Response hostResp = registerUser("Host Alice", hostEmail, TEST_PASSWORD);
        String hostToken = extractToken(hostResp);
        assertNotNull(hostToken, "Host token must not be null");
        hostTokenForCleanup = hostToken;

        Response meetingResp = createMeeting(hostToken, "Confidential Strategy Meeting", 60, 10);
        String meetingId = meetingResp.jsonPath().getString("data.id");
        String roomCode = meetingResp.jsonPath().getString("data.roomCode");
        assertNotNull(meetingId);
        assertNotNull(roomCode);
        createdMeetingIds.add(meetingId);

        // 2. Create unrelated Member B
        String memberEmail = uniqueEmail("member-b");
        Response memberResp = registerUser("Member Bob", memberEmail, TEST_PASSWORD);
        String memberToken = extractToken(memberResp);
        assertNotNull(memberToken, "Member token must not be null");

        // 3. Member B queries Meeting A metadata via IDOR: GET /meetings/:id
        // VULNERABILITY: Endpoint returns 200 OK and leaks roomCode and host info instead of 403
        Response idorResp = given()
                .header("Authorization", "Bearer " + memberToken)
                .when()
                .get("/meetings/" + meetingId);

        // Assert live defect behavior (BUG-001):
        idorResp.then()
                .statusCode(200)
                .body("data.id", equalTo(meetingId))
                .body("data.title", equalTo("Confidential Strategy Meeting"))
                .body("data.roomCode", equalTo(roomCode));

        System.out.println("[VERIFIED BUG-001] IDOR vulnerability confirmed: Member B leaked private roomCode " + roomCode);
    }

    @Test
    @DisplayName("BUG-002 Verification: Case-sensitive room code lookup fails with 404 for lowercase characters")
    void testBug002_CaseSensitiveRoomCodeLookup() {
        // 1. Create host and meeting
        String hostEmail = uniqueEmail("host-case");
        Response hostResp = registerUser("Case Tester", hostEmail, TEST_PASSWORD);
        String hostToken = extractToken(hostResp);
        assertNotNull(hostToken);
        hostTokenForCleanup = hostToken;

        Response meetingResp = createMeeting(hostToken, "Case Sensitivity Test Meeting", 30, 5);
        String meetingId = meetingResp.jsonPath().getString("data.id");
        String uppercaseCode = meetingResp.jsonPath().getString("data.roomCode");
        assertNotNull(meetingId);
        assertNotNull(uppercaseCode);
        createdMeetingIds.add(meetingId);

        assertEquals(8, uppercaseCode.length());

        // 2. Lookup with uppercase room code -> 200 OK
        given()
                .header("Authorization", "Bearer " + hostToken)
                .when()
                .get("/meetings/code/" + uppercaseCode)
                .then()
                .statusCode(200)
                .body("data.roomCode", equalTo(uppercaseCode));

        // 3. Lookup with lowercase room code -> 404 Not Found (BUG-002)
        String lowercaseCode = uppercaseCode.toLowerCase();
        given()
                .header("Authorization", "Bearer " + hostToken)
                .when()
                .get("/meetings/code/" + lowercaseCode)
                .then()
                .statusCode(404);

        System.out.println("[VERIFIED BUG-002] Case-sensitivity bug confirmed: uppercase " + uppercaseCode + " returned 200, lowercase " + lowercaseCode + " returned 404");
    }

    @Test
    @DisplayName("BUG-003 Verification: Guest token calling GET /users/me returns 404 User not found")
    void testBug003_GuestTokenUserMe404() {
        // 1. Issue guest token
        Response guestResp = registerGuest("Guest Explorer");
        guestResp.then().statusCode(201);
        String guestToken = extractToken(guestResp);
        assertNotNull(guestToken);

        // 2. Guest requests profile: GET /users/me
        // DEFECT: Instead of returning guest profile or 403, returns unhandled 404 User not found
        given()
                .header("Authorization", "Bearer " + guestToken)
                .when()
                .get("/users/me")
                .then()
                .statusCode(404)
                .body("error", equalTo("User not found"));

        System.out.println("[VERIFIED BUG-003] Guest token profile defect confirmed: returns 404 User not found");
    }

    @Test
    @DisplayName("SEC-01: Tampered JWT token signature is rejected with 401 Unauthorized")
    void testJwtTamperingRejection() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvZSJ9.invalidSignatureHere";

        given()
                .header("Authorization", "Bearer " + invalidToken)
                .when()
                .get("/users/me")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("SEC-02: Non-host participant cannot delete another user's meeting")
    void testPrivilegeEscalationMeetingDeletionBlocked() {
        // 1. Host creates meeting
        String hostEmail = uniqueEmail("host-sec");
        Response hostResp = registerUser("Host Secure", hostEmail, TEST_PASSWORD);
        String hostToken = extractToken(hostResp);
        assertNotNull(hostToken);
        hostTokenForCleanup = hostToken;

        Response meetingResp = createMeeting(hostToken, "Protected Meeting", 30, 5);
        String meetingId = meetingResp.jsonPath().getString("data.id");
        assertNotNull(meetingId);
        createdMeetingIds.add(meetingId);

        // 2. Guest attempts to delete -> 403
        Response guestResp = registerGuest("Attacker Guest");
        String guestToken = extractToken(guestResp);
        assertNotNull(guestToken);

        given()
                .header("Authorization", "Bearer " + guestToken)
                .when()
                .delete("/meetings/" + meetingId)
                .then()
                .statusCode(403);

        // 3. Unrelated member attempts to delete -> 403
        String memberEmail = uniqueEmail("member-sec");
        Response memberResp = registerUser("Member Secure", memberEmail, TEST_PASSWORD);
        String memberToken = extractToken(memberResp);
        assertNotNull(memberToken);

        given()
                .header("Authorization", "Bearer " + memberToken)
                .when()
                .delete("/meetings/" + meetingId)
                .then()
                .statusCode(403);
    }
}
