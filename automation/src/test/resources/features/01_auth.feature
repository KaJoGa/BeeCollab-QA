Feature: Authentication & User Onboarding
  As a BeeCollab user
  I want to be able to sign up, sign in, and access the platform as a guest
  So that I can participate in video meetings

  @smoke @AUTH-01
  Scenario: Successfully register a new account
    Given the user is on the login page
    When the user registers a new account with name "QA Engineer" and password "QaPassw0rd!"
    Then the user should be on the home page
    And the user should see username "QA Engineer" displayed

  @smoke @AUTH-02
  Scenario: Continue as an anonymous guest
    Given the user is on the login page
    When the user continues as guest with name "Guest Explorer"
    Then the user should be on the home page
    And the user should see username "Guest Explorer" displayed

  @regression @AUTH-03
  Scenario: Attempt login with invalid credentials
    Given the user is on the login page
    When the user enters email "nonexistent_qa@beecollab.test" and password "WrongPassword123!"
    Then an authentication error message should be displayed
