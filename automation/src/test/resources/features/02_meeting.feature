Feature: Meeting Room & In-Call Collaboration
  As an authenticated user
  I want to create video meetings and use real-time in-call controls
  So that I can effectively collaborate with team members

  @smoke @MEET-01
  Scenario: Host creates a meeting and enters the room successfully
    Given the user is on the login page
    When the user registers a new account with name "Host Runner" and password "QaPassw0rd!"
    And the user creates a new meeting with title "Smoke Room" duration 30 and max participants 5
    Then the user should be connected to the meeting room

  @regression @MEET-02
  Scenario: Host toggles microphone and raises hand
    Given the user is on the login page
    When the user registers a new account with name "Control Host" and password "QaPassw0rd!"
    And the user creates a new meeting with title "Control Room" duration 30 and max participants 5
    Then the user should be connected to the meeting room
    When the user toggles the microphone
    Then the microphone should be toggled
    When the user toggles hand raise
    Then the hand raise status should be active

  @regression @MEET-03
  Scenario: Send in-call chat message and verify sender attribution bug R-24
    Given the user is on the login page
    When the user registers a new account with name "Chat Host" and password "QaPassw0rd!"
    And the user creates a new meeting with title "Chat Room" duration 30 and max participants 5
    Then the user should be connected to the meeting room
    When the user sends a chat message "Hello from Chat Host"
    Then the message "Hello from Chat Host" should appear in the chat panel
    And own chat message "Hello from Chat Host" exhibits sender attribution bug R-24

  @regression @MEET-04
  Scenario: Host creates an agenda item and views it in the sidebar
    Given the user is on the login page
    When the user registers a new account with name "Agenda Host" and password "QaPassw0rd!"
    And the user creates a new meeting with title "Agenda Room" duration 30 and max participants 5
    Then the user should be connected to the meeting room
    When the user adds an agenda item titled "Sprint Review" with duration 15
    Then the agenda item "Sprint Review" should be visible in the sidebar

  @regression @MEET-05
  Scenario: Host creates a poll and launches it in the sidebar
    Given the user is on the login page
    When the user registers a new account with name "Poll Host" and password "QaPassw0rd!"
    And the user creates a new meeting with title "Poll Room" duration 30 and max participants 5
    Then the user should be connected to the meeting room
    When the user creates a poll with question "Which framework do you prefer?" and options "Selenium" and "Playwright"
    Then the poll "Which framework do you prefer?" should appear in the sidebar

  @regression @MEET-06
  Scenario: Verify lowercase room code joining failure R-17
    Given the user is on the login page
    When the user registers a new account with name "Joiner User" and password "QaPassw0rd!"
    Then the user should be on the home page
    When the user attempts to join with lowercase code "8baddb42"
    Then the joining flow fails to connect due to case sensitivity bug R-17
