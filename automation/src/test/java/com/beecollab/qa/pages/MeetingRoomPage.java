package com.beecollab.qa.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class MeetingRoomPage extends BasePage {
    private final By joiningScreen = testId("joining-screen");
    private final By micBtn = testId("ctrl-mic");
    private final By cameraBtn = testId("ctrl-camera");
    private final By handBtn = testId("ctrl-hand");
    private final By infoBtn = testId("ctrl-info");
    private final By infoModal = testId("info-modal");
    private final By infoRoomCode = testId("info-room-code");
    private final By infoCloseBtn = testId("info-close");
    private final By chatTabBtn = testId("ctrl-tab-chat");
    private final By peopleTabBtn = testId("ctrl-tab-people");
    private final By agendaTabBtn = testId("ctrl-tab-agenda");
    private final By pollsTabBtn = testId("ctrl-tab-polls");
    private final By participantCountBadge = testId("participant-count");
    private final By sidebar = testId("sidebar");
    private final By sidebarTitle = testId("sidebar-title");
    private final By sidebarCloseBtn = testId("sidebar-close");
    private final By chatInput = testId("chat-input");
    private final By chatSendBtn = testId("chat-send");
    private final By chatMessages = testId("chat-messages");
    private final By leaveBtn = testId("ctrl-leave");
    private final By endMeetingBtn = testId("leave-end-meeting");
    private final By leaveMeetingBtn = testId("leave-leave-meeting");

    // Agenda locators
    private final By agendaTitleInput = testId("agenda-title-input");
    private final By agendaDurationInput = testId("agenda-duration-input");
    private final By agendaSaveBtn = testId("agenda-save");
    private final By agendaItem = testId("agenda-item");
    private final By agendaStartBtn = testId("agenda-start");

    // Poll locators
    private final By pollQuestionInput = testId("poll-question-input");
    private final By pollOptionInput = testId("poll-option-input");
    private final By pollLaunchBtn = testId("poll-launch");
    private final By pollCard = testId("poll-card");

    // Mobile locators
    private final By mobileInfoBtn = testId("mobile-info-btn");
    private final By mobileLeaveBtn = testId("mobile-leave-btn");
    private final By mobileChatBtn = testId("ctrl-mobile-chat");
    private final By mobilePeopleBtn = testId("ctrl-mobile-people");
    private final By mobileAgendaBtn = testId("ctrl-mobile-agenda");
    private final By mobilePollsBtn = testId("ctrl-mobile-polls");
    private final By mobileParticipantCount = testId("mobile-participant-count");

    public MeetingRoomPage(WebDriver driver) {
        super(driver);
    }

    public void waitForRoomReady() {
        waitForUrlContains("/meeting/");
        // Critical gotcha: wait for joining screen overlay to disappear completely
        waitForInvisibility(joiningScreen);
        waitForVisibility(micBtn);
    }

    public boolean isRoomReady() {
        try {
            waitForRoomReady();
            return isDisplayed(micBtn);
        } catch (Exception e) {
            return false;
        }
    }

    public void toggleMic() {
        click(micBtn);
    }

    public boolean isMicEnabled() {
        return "true".equalsIgnoreCase(getAttribute(micBtn, "data-enabled"));
    }

    public void toggleCamera() {
        click(cameraBtn);
    }

    public boolean isCameraEnabled() {
        return "true".equalsIgnoreCase(getAttribute(cameraBtn, "data-enabled"));
    }

    public void toggleHand() {
        click(handBtn);
    }

    public boolean isHandRaised() {
        return "true".equalsIgnoreCase(getAttribute(handBtn, "data-active"));
    }

    public String extractRoomCodeFromInfo() {
        click(infoBtn);
        waitForVisibility(infoModal);
        String code = getText(infoRoomCode);
        click(infoCloseBtn);
        waitForInvisibility(infoModal);
        return code;
    }

    public void openTab(String tab) {
        switch (tab.toLowerCase()) {
            case "chat" -> click(chatTabBtn);
            case "people" -> click(peopleTabBtn);
            case "agenda" -> click(agendaTabBtn);
            case "polls" -> click(pollsTabBtn);
            default -> throw new IllegalArgumentException("Unknown tab: " + tab);
        }
        waitForVisibility(sidebarTitle);
    }

    public String getSidebarTitleText() {
        return getText(sidebarTitle);
    }

    public void closeSidebar() {
        click(sidebarCloseBtn);
        waitForInvisibility(sidebar);
    }

    public String getParticipantCountText() {
        return getText(participantCountBadge);
    }

    public void openChat() {
        openTab("chat");
        waitForVisibility(chatInput);
    }

    public void sendChatMessage(String message) {
        openChat();
        type(chatInput, message);
        click(chatSendBtn);
    }

    public boolean hasChatMessage(String message) {
        By msgLocator = By.xpath(String.format("//*[@data-testid='chat-message-text'][contains(normalize-space(.), '%s')]", message));
        try {
            waitForVisibility(msgLocator);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getChatMessageSender(String messageText) {
        List<WebElement> msgElements = driver.findElements(testId("chat-message"));
        for (WebElement el : msgElements) {
            if (el.getText().contains(messageText)) {
                return el.getAttribute("data-sender");
            }
        }
        return null;
    }

    public boolean isOwnChatMessageMarkedAsYou(String messageText) {
        List<WebElement> msgElements = driver.findElements(testId("chat-message"));
        for (WebElement el : msgElements) {
            if (el.getText().contains(messageText)) {
                String isMe = el.getAttribute("data-me");
                String text = el.getText();
                return "true".equalsIgnoreCase(isMe) || text.contains("You");
            }
        }
        return false;
    }

    public void createAgendaItem(String title, int duration) {
        openTab("agenda");
        type(agendaTitleInput, title);
        type(agendaDurationInput, String.valueOf(duration));
        click(agendaSaveBtn);
        waitForVisibility(agendaItem);
    }

    public boolean hasAgendaItem(String title) {
        return getText(sidebar).contains(title);
    }

    public void createPoll(String question, String opt1, String opt2) {
        openTab("polls");
        type(pollQuestionInput, question);
        List<WebElement> options = driver.findElements(pollOptionInput);
        if (options.size() >= 2) {
            options.get(0).clear();
            options.get(0).sendKeys(opt1);
            options.get(1).clear();
            options.get(1).sendKeys(opt2);
        }
        click(pollLaunchBtn);
        waitForVisibility(pollCard);
    }

    public boolean hasPollCard(String question) {
        return getText(sidebar).contains(question);
    }

    public void leaveMeeting() {
        click(leaveBtn);
        if (isDisplayed(leaveMeetingBtn)) {
            click(leaveMeetingBtn);
        }
    }

    public void endMeetingAsHost() {
        click(leaveBtn);
        if (isDisplayed(endMeetingBtn)) {
            click(endMeetingBtn);
        }
    }

    public void setMobileViewport() {
        driver.manage().window().setSize(new org.openqa.selenium.Dimension(412, 915));
    }

    public boolean isMobileHeaderDisplayed() {
        return isDisplayed(mobileInfoBtn) || isDisplayed(mobileLeaveBtn);
    }

    public void openMobileTab(String tab) {
        switch (tab.toLowerCase()) {
            case "chat" -> click(mobileChatBtn);
            case "people" -> click(mobilePeopleBtn);
            case "agenda" -> click(mobileAgendaBtn);
            case "polls" -> click(mobilePollsBtn);
            default -> throw new IllegalArgumentException("Unknown mobile tab: " + tab);
        }
        waitForVisibility(sidebarTitle);
    }

    public String getMobileParticipantCountText() {
        return getText(mobileParticipantCount);
    }
}
