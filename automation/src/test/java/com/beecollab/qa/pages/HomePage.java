package com.beecollab.qa.pages;

import com.beecollab.qa.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class HomePage extends BasePage {
    private final By signInBtn = testId("home-signin-btn");
    private final By userName = testId("home-user-name");
    private final By logoutBtn = testId("home-logout-btn");
    private final By logoutConfirmBtn = testId("logout-confirm");
    private final By newMeetingBtn = testId("new-meeting-btn");
    private final By newMeetingTitleInput = testId("new-meeting-title");
    private final By newMeetingDurationInput = testId("new-meeting-duration");
    private final By newMeetingMaxPartInput = testId("new-meeting-max-participants");
    private final By newMeetingSubmitBtn = testId("new-meeting-submit");
    private final By newMeetingError = testId("new-meeting-error");
    private final By joinCodeInput = testId("join-code-input");
    private final By joinBtn = testId("join-btn");
    private final By guestJoinOpenBtn = testId("guest-join-open");
    private final By guestNameInput = testId("guest-name-input");
    private final By guestCodeInput = testId("guest-code-input");
    private final By guestJoinSubmitBtn = testId("guest-join-submit");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get(Config.getBaseUrl() + "/");
        waitForPageLoad();
    }

    public void waitForHomePage() {
        waitForVisibility(userName);
    }

    public boolean isLoggedIn() {
        return isDisplayed(userName);
    }

    public String getUserNameText() {
        return getText(userName);
    }

    public void clickSignIn() {
        click(signInBtn);
    }

    public void logout() {
        if (isDisplayed(logoutBtn)) {
            click(logoutBtn);
            if (isDisplayed(logoutConfirmBtn)) {
                click(logoutConfirmBtn);
            }
            waitForVisibility(signInBtn);
        }
    }

    public void openNewMeetingModal() {
        waitForVisibility(newMeetingBtn);
        click(newMeetingBtn);
        waitForVisibility(newMeetingTitleInput);
    }

    public void createMeeting(String title, int duration, int maxParticipants) {
        openNewMeetingModal();
        type(newMeetingTitleInput, title);
        type(newMeetingDurationInput, String.valueOf(duration));
        type(newMeetingMaxPartInput, String.valueOf(maxParticipants));
        click(newMeetingSubmitBtn);
    }

    public void joinByCode(String code) {
        type(joinCodeInput, code);
        click(joinBtn);
    }

    public void joinAsGuest(String name, String code) {
        click(guestJoinOpenBtn);
        waitForVisibility(guestNameInput);
        type(guestNameInput, name);
        type(guestCodeInput, code);
        click(guestJoinSubmitBtn);
    }
}
