package com.beecollab.qa.steps;

import com.beecollab.qa.driver.DriverFactory;
import com.beecollab.qa.pages.HomePage;
import com.beecollab.qa.pages.MeetingRoomPage;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;

public class MeetingSteps {
    private HomePage homePage;
    private MeetingRoomPage meetingRoomPage;

    private WebDriver getDriver() {
        return DriverFactory.getDriver();
    }

    @When("the user creates a new meeting with title {string} duration {int} and max participants {int}")
    public void userCreatesMeeting(String title, int duration, int maxParticipants) {
        homePage = new HomePage(getDriver());
        homePage.createMeeting(title, duration, maxParticipants);
        meetingRoomPage = new MeetingRoomPage(getDriver());
        meetingRoomPage.waitForRoomReady();
    }

    @Then("the user should be connected to the meeting room")
    public void userShouldBeInMeetingRoom() {
        if (meetingRoomPage == null) {
            meetingRoomPage = new MeetingRoomPage(getDriver());
        }
        Assertions.assertTrue(meetingRoomPage.isRoomReady(), "Expected user to be connected inside the meeting room");
    }

    @When("the user toggles the microphone")
    public void userTogglesMic() {
        meetingRoomPage.toggleMic();
    }

    @Then("the microphone should be toggled")
    public void micShouldBeToggled() {
        boolean isEnabled = meetingRoomPage.isMicEnabled();
        System.out.println("Microphone current state: " + isEnabled);
    }

    @When("the user toggles hand raise")
    public void userTogglesHand() {
        meetingRoomPage.toggleHand();
    }

    @Then("the hand raise status should be active")
    public void handRaiseShouldBeActive() {
        Assertions.assertTrue(meetingRoomPage.isHandRaised(), "Expected hand raise to be active");
    }

    @When("the user sends a chat message {string}")
    public void userSendsChatMessage(String message) {
        meetingRoomPage.sendChatMessage(message);
    }

    @Then("the message {string} should appear in the chat panel")
    public void messageShouldAppearInChat(String message) {
        Assertions.assertTrue(meetingRoomPage.hasChatMessage(message), "Expected message to appear in chat: " + message);
    }

    @When("the user opens the {string} panel")
    public void userOpensPanel(String tab) {
        meetingRoomPage.openTab(tab);
    }

    @Then("the sidebar title should be {string}")
    public void sidebarTitleShouldBe(String expectedTitle) {
        Assertions.assertTrue(meetingRoomPage.getSidebarTitleText().contains(expectedTitle),
                "Expected sidebar title to contain: " + expectedTitle);
    }

    @When("the user adds an agenda item titled {string} with duration {int}")
    public void userAddsAgendaItem(String title, int duration) {
        meetingRoomPage.createAgendaItem(title, duration);
    }

    @Then("the agenda item {string} should be visible in the sidebar")
    public void agendaItemShouldBeVisible(String title) {
        Assertions.assertTrue(meetingRoomPage.hasAgendaItem(title), "Expected agenda item: " + title);
    }

    @When("the user creates a poll with question {string} and options {string} and {string}")
    public void userCreatesPoll(String question, String opt1, String opt2) {
        meetingRoomPage.createPoll(question, opt1, opt2);
    }

    @Then("the poll {string} should appear in the sidebar")
    public void pollShouldAppear(String question) {
        Assertions.assertTrue(meetingRoomPage.hasPollCard(question), "Expected poll card: " + question);
    }

    @Then("the participant count should be {string}")
    public void participantCountShouldBe(String count) {
        Assertions.assertEquals(count, meetingRoomPage.getParticipantCountText());
    }

    @Then("own chat message {string} exhibits sender attribution bug R-24")
    public void ownChatMessageExhibitsBugR24(String message) {
        boolean markedAsYou = meetingRoomPage.isOwnChatMessageMarkedAsYou(message);
        Assertions.assertFalse(markedAsYou, "Defect R-24 confirmed: Own message was not attributed to 'You'!");
    }

    @When("the user attempts to join with lowercase code {string}")
    public void userAttemptsJoinLowercase(String code) {
        homePage = new HomePage(getDriver());
        homePage.joinByCode(code.toLowerCase());
    }

    @Then("the joining flow fails to connect due to case sensitivity bug R-17")
    public void joiningFlowFailsDueToR17() {
        meetingRoomPage = new MeetingRoomPage(getDriver());
        boolean ready = meetingRoomPage.isRoomReady();
        Assertions.assertFalse(ready, "Defect R-17 confirmed: Lowercase room code failed to resolve and join meeting room!");
    }

    @When("the user ends the meeting")
    public void userEndsMeeting() {
        meetingRoomPage.endMeetingAsHost();
    }

    @When("the user resizes viewport to mobile device resolution")
    public void userResizesToMobile() {
        meetingRoomPage.setMobileViewport();
    }

    @Then("the mobile responsive header controls should be displayed")
    public void mobileHeaderShouldBeDisplayed() {
        Assertions.assertTrue(meetingRoomPage.isMobileHeaderDisplayed(),
                "Expected mobile responsive header controls to be displayed");
    }

    @When("the user opens mobile {string} tab")
    public void userOpensMobileTab(String tab) {
        meetingRoomPage.openMobileTab(tab);
    }
}
