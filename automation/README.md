# BeeCollab UI Automation Framework

Enterprise UI End-to-End Test Automation Suite built with **Java 21 LTS + Selenium WebDriver 4 + Cucumber BDD 7 + JUnit 5 + Maven**.

Target Application: **BeeCollab Web Platform** (`https://beecollab.vercel.app`)

---

## 1. Project Structure

```text
automation/
├── pom.xml                               # Maven build & dependencies (Java 21, Selenium 4, Cucumber 7, REST Assured 5.5, JUnit 5)
├── src/test/
│   ├── java/com/beecollab/qa/
│   │   ├── api/                          # REST Assured API Test Suite
│   │   │   ├── ApiBaseTest.java          # Base API configuration, authentication & token extraction
│   │   │   ├── RestSmokeTest.java        # L1 API Smoke Suite (Health, Auth, Meeting CRUD, ICE servers)
│   │   │   └── ApiSecurityDefectTest.java # Security & Defect Suite (Verifies BUG-001 IDOR, BUG-002, BUG-003, SEC-01/02)
│   │   ├── config/
│   │   │   └── Config.java               # Config loader with System properties and Env overrides
│   │   ├── driver/
│   │   │   └── DriverFactory.java        # Thread-safe WebDriver factory with WebRTC fake device flags
│   │   ├── pages/                        # Page Object Model (POM)
│   │   │   ├── BasePage.java             # Explicit waits, locator helpers, zero fixed sleep
│   │   │   ├── LoginPage.java            # Sign In / Register / Guest access
│   │   │   ├── HomePage.java             # Dashboard, new meeting modal, join code
│   │   │   └── MeetingRoomPage.java      # WebRTC in-call controls, mic, camera, hand, chat, overlays
│   │   ├── steps/                        # Cucumber Step Definitions
│   │   │   ├── AuthSteps.java            # Authentication & onboarding bindings
│   │   │   └── MeetingSteps.java         # Meeting creation, room controls, in-call actions
│   │   ├── hooks/
│   │   │   └── Hooks.java                # Scenario lifecycle hooks, screenshot capture on failure
│   │   └── runners/
│   │       └── CucumberTestRunner.java   # JUnit 5 Suite runner with Cucumber engine
│   └── resources/
│       ├── config/
│       │   └── local.properties          # Base URL, API URL, timeouts, headless flag, viewport dimensions
│       └── features/
│           ├── 01_auth.feature           # @AUTH-* scenarios (Register, Guest, Invalid login)
│           ├── 02_meeting.feature        # @MEET-* scenarios (Meeting creation, Mic toggle, Hand raise, Chat, Agenda, Poll)
│           └── 03_mobile_web.feature     # @MOB-* scenarios (Mobile viewport Pixel 7 emulation)
```

---

## 2. Key Architecture Principles

1. **Zero TypeScript UI Tests**: Layer 4 UI automation strictly uses Java 21 and Selenium WebDriver.
2. **Zero Fixed Sleep**: All element synchronization relies strictly on `WebDriverWait` and `ExpectedConditions` (no `Thread.sleep()`).
3. **WebRTC Fake Media Simulation**: Chrome runs with `--use-fake-device-for-media-stream`, `--use-fake-ui-for-media-stream`, and auto-granted camera/microphone permissions.
4. **Resilient Overlay Handling**: Page objects explicitly wait for the `joining-screen` loading overlay to detach before interacting with meeting room controls.

---

## 3. How to Run the Tests

Execute from inside the `automation/` directory:

```bash
cd automation

# Run All API Tests (REST Assured Smoke + Security Defects):
mvn test "-Dtest=RestSmokeTest,ApiSecurityDefectTest"

# Run Specific Defect Verification Test:
mvn test -Dtest=ApiSecurityDefectTest

# Run Smoke Suite (@smoke):
mvn test -Dcucumber.filter.tags="@smoke"

# Run Full Combined Suite (UI BDD + API REST Assured):
mvn test

# Run Specific Spec / Tag:
mvn test -Dcucumber.filter.tags="@MEET-01"

# Run in Headed Mode (see browser UI):
mvn test -Dheadless=false
```

---

## 4. Test Reports

* **HTML Report**: `automation/target/cucumber-reports/cucumber.html`
* **JSON Report**: `automation/target/cucumber-reports/cucumber.json`
* **Surefire Summary**: `automation/target/surefire-reports/com.beecollab.qa.runners.CucumberTestRunner.txt`
