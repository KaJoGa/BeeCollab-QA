Feature: Mobile Web Responsiveness & Touch Shell
  As a mobile user
  I want the meeting room UI to adapt to narrow screens
  So that I can access meeting controls seamlessly on mobile devices

  @mobile @regression @MOB-01
  Scenario: Meeting room adapts to mobile viewport with mobile header and inline controls
    Given the user is on the login page
    When the user registers a new account with name "Mobile Host" and password "QaPassw0rd!"
    And the user creates a new meeting with title "Mobile Call" duration 30 and max participants 5
    Then the user should be connected to the meeting room
    When the user resizes viewport to mobile device resolution
    Then the mobile responsive header controls should be displayed
    When the user opens mobile "chat" tab
    Then the sidebar title should be "Chat"
