package com.appointmenthub.ui;

import com.appointmenthub.support.ApiClient;
import com.appointmenthub.support.TestApp;
import com.appointmenthub.support.TestData;
import com.appointmenthub.ui.pages.HomePage;
import com.appointmenthub.ui.support.WebDriverFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Base class for Selenium UI tests (PDR section 15).
 *
 * <p>The application under test is started in-process on a free port with a clean
 * store, so the UI suite is self contained. When {@code -Dapp.base.url} is
 * provided, the tests run against that already running instance instead (for
 * example a local environment started with {@code mvn exec:java}).</p>
 *
 * <p>A screenshot is captured into {@code target/screenshots} whenever a UI test
 * fails, as required by the evidence rules of the PDR.</p>
 */
@Tag("ui")
public abstract class BaseUiTest {

    protected static TestApp app;
    protected static ApiClient api;
    protected static WebDriver driver;
    protected static HomePage home;

    @RegisterExtension
    static final TestWatcher SCREENSHOT_ON_FAILURE = new TestWatcher() {
        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            captureScreenshot(context.getDisplayName());
        }
    };

    @BeforeAll
    static void startApplicationAndBrowser() {
        String externalUrl = System.getProperty("app.base.url", "").trim();
        String baseUrl;
        if (externalUrl.isEmpty()) {
            app = TestApp.start();
            baseUrl = app.baseUrl();
        } else {
            baseUrl = externalUrl;
        }
        api = new ApiClient(baseUrl);
        driver = WebDriverFactory.create();
        home = new HomePage(driver, baseUrl);
    }

    @AfterAll
    static void stopBrowserAndApplication() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
        if (app != null) {
            app.close();
            app = null;
        }
    }

    /** Books an appointment directly through the API; used to prepare UI scenarios. */
    protected static int bookViaApi(LocalDate date, String startTime) {
        ApiClient.Response response = api.post("/api/appointments",
                TestData.bookingBody(TestData.SECOND_NAME, TestData.SECOND_EMAIL, TestData.ACTIVE_SERVICE_ID,
                        date, startTime));
        if (response.status() != 201) {
            throw new IllegalStateException("Unable to prepare the test appointment: " + response);
        }
        return response.number("id");
    }

    /** A far future date that no other test is expected to use. */
    protected static LocalDate freeDate() {
        return TestData.freeDate();
    }

    private static void captureScreenshot(String name) {
        if (driver == null) {
            return;
        }
        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path directory = Path.of("target", "screenshots");
            Files.createDirectories(directory);
            Path file = directory.resolve(name.replaceAll("[^A-Za-z0-9]+", "-") + ".png");
            Files.write(file, screenshot);
            System.out.println("Screenshot captured for the failure: " + file.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Unable to capture a screenshot: " + e.getMessage());
        }
    }
}
