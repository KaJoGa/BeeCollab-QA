package com.beecollab.qa.pages;

import com.beecollab.qa.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {
    private final By title = testId("auth-title");
    private final By emailInput = testId("auth-email-input");
    private final By passwordInput = testId("auth-password-input");
    private final By nameInput = testId("auth-name-input");
    private final By submitBtn = testId("auth-submit");
    private final By switchToRegisterBtn = testId("auth-switch-to-register");
    private final By switchToLoginBtn = testId("auth-switch-to-login");
    private final By errorMsg = testId("auth-error");
    private final By guestNameInput = testId("auth-guest-name-input");
    private final By guestSubmitBtn = testId("auth-guest-submit");
    private final By guestErrorMsg = testId("auth-guest-error");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void open() {
        driver.get(Config.getBaseUrl() + "/login");
        waitForVisibility(title);
    }

    public String getTitleText() {
        return getText(title);
    }

    public void switchToRegister() {
        if (!isDisplayed(nameInput)) {
            click(switchToRegisterBtn);
            waitForVisibility(nameInput);
        }
    }

    public void switchToLogin() {
        if (!isDisplayed(emailInput)) {
            click(switchToLoginBtn);
            waitForVisibility(emailInput);
        }
    }

    public void login(String email, String password) {
        switchToLogin();
        type(emailInput, email);
        type(passwordInput, password);
        click(submitBtn);
    }

    public void register(String name, String email, String password) {
        switchToRegister();
        type(nameInput, name);
        type(emailInput, email);
        type(passwordInput, password);
        click(submitBtn);
    }

    public void loginAsGuest(String guestName) {
        type(guestNameInput, guestName);
        click(guestSubmitBtn);
    }

    public boolean isErrorMessageDisplayed() {
        try {
            return waitForVisibility(errorMsg).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getErrorMessage() {
        return getText(errorMsg);
    }
}
