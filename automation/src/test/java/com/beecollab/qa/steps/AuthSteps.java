package com.beecollab.qa.steps;

import com.beecollab.qa.driver.DriverFactory;
import com.beecollab.qa.pages.HomePage;
import com.beecollab.qa.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;

public class AuthSteps {
    private LoginPage loginPage;
    private HomePage homePage;

    private WebDriver getDriver() {
        return DriverFactory.getDriver();
    }

    @Given("the user is on the login page")
    public void userIsOnLoginPage() {
        loginPage = new LoginPage(getDriver());
        loginPage.open();
    }

    @Given("the user is on the home page")
    public void userIsOnHomePage() {
        homePage = new HomePage(getDriver());
        homePage.open();
    }

    @When("the user registers a new account with name {string} and password {string}")
    public void userRegistersNewAccount(String name, String password) {
        String uniqueEmail = String.format("qa-%d-%d@qa.beecollab.test", System.currentTimeMillis(), (int)(Math.random() * 10000));
        loginPage.register(name, uniqueEmail, password);
    }

    @When("the user enters email {string} and password {string}")
    public void userLogsIn(String email, String password) {
        loginPage.login(email, password);
    }

    @When("the user continues as guest with name {string}")
    public void userLogsInAsGuest(String guestName) {
        loginPage.loginAsGuest(guestName);
    }

    @Then("the user should be on the home page")
    public void userShouldBeOnHomePage() {
        homePage = new HomePage(getDriver());
        homePage.waitForHomePage();
        Assertions.assertTrue(homePage.isLoggedIn(), "Expected user to be logged in on the home page");
    }

    @Then("the user should see username {string} displayed")
    public void userShouldSeeUsername(String expectedName) {
        homePage = new HomePage(getDriver());
        Assertions.assertTrue(homePage.getUserNameText().contains(expectedName),
                "Expected user name to contain: " + expectedName + ", but was: " + homePage.getUserNameText());
    }

    @Then("an authentication error message should be displayed")
    public void authErrorShouldBeDisplayed() {
        Assertions.assertTrue(loginPage.isErrorMessageDisplayed(), "Expected authentication error message to be displayed");
    }

    @When("the user clicks logout")
    public void userClicksLogout() {
        homePage.logout();
    }
}
