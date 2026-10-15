package com.appointmenthub.ui.support;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * Creates the WebDriver used by the UI tests (PDR section 15).
 *
 * <p>Settings, all optional, can be passed with {@code -D} on the command line:</p>
 * <ul>
 *   <li>{@code -Dbrowser=chrome} (only Chrome is configured in the first release)</li>
 *   <li>{@code -Dheadless=true|false} (default {@code true}, so the suite runs on CI)</li>
 *   <li>{@code -Dchrome.binary=/path/to/chrome} to use a specific browser build</li>
 *   <li>{@code -Dwebdriver.chrome.driver=/path/to/chromedriver} to use a specific driver;
 *       when omitted, Selenium Manager resolves and downloads a matching driver</li>
 * </ul>
 */
public final class WebDriverFactory {

    private WebDriverFactory() {
    }

    public static WebDriver create() {
        String browser = System.getProperty("browser", "chrome").trim().toLowerCase();
        if (!"chrome".equals(browser)) {
            throw new IllegalArgumentException("Only 'chrome' is configured for UI automation, received: " + browser);
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--window-size=1280,1024");
        if (isHeadless()) {
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
        }
        String binary = System.getProperty("chrome.binary", "").trim();
        if (!binary.isEmpty()) {
            options.setBinary(binary);
        }
        return new ChromeDriver(options);
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(System.getProperty("headless", "true"));
    }
}
