package com.beecollab.qa.hooks;

import com.beecollab.qa.config.Config;
import com.beecollab.qa.driver.DriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class Hooks {
    private static volatile boolean serverWarmedUp = false;

    private static synchronized void warmUpBackend() {
        if (serverWarmedUp) return;
        String backendUrl = Config.get("api.url", "https://beecollab-rwbj.onrender.com/health");
        System.out.println("[Hooks] Pre-warming backend to prevent Render free-tier cold start timeout: " + backendUrl);
        long deadline = System.currentTimeMillis() + 90_000;
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(backendUrl)).GET().build();

        while (System.currentTimeMillis() < deadline) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 && response.body().contains("\"db\":\"up\"")) {
                    System.out.println("[Hooks] Backend is awake and database is UP! Proceeding with UI suite.");
                    serverWarmedUp = true;
                    return;
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.err.println("[Hooks] Warning: Backend warmup deadline reached, continuing test execution...");
        serverWarmedUp = true;
    }

    @Before
    public void setUp(Scenario scenario) {
        warmUpBackend();
        DriverFactory.initDriver();
    }

    @After
    public void tearDown(Scenario scenario) {
        WebDriver driver = DriverFactory.getDriver();
        if (driver != null) {
            if (scenario.isFailed()) {
                try {
                    byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    scenario.attach(screenshot, "image/png", scenario.getName());
                } catch (Exception e) {
                    System.err.println("Failed to capture screenshot: " + e.getMessage());
                }
            }
            DriverFactory.quitDriver();
        }
    }
}
