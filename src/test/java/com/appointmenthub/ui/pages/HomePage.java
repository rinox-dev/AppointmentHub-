package com.appointmenthub.ui.pages;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object for the AppointmentHub single page interface (PDR section 15.3).
 *
 * <p>All locators use the dedicated {@code data-testid} attributes, so the tests
 * do not depend on styling or on the surrounding markup. Waits are explicit and
 * always wait for a user visible condition.</p>
 */
public final class HomePage {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    public HomePage(WebDriver driver, String baseUrl) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, TIMEOUT);
        this.baseUrl = baseUrl;
    }

    public HomePage open() {
        driver.get(baseUrl + "/");
        waitForServices();
        return this;
    }

    public void refresh() {
        driver.navigate().refresh();
        waitForServices();
    }

    // --- services -----------------------------------------------------------------

    public HomePage waitForServices() {
        wait.until(ExpectedConditions.presenceOfElementLocated(byTestId("service-item")));
        return this;
    }

    public List<String> serviceNames() {
        return attributesOf("[data-testid='service-item']", "data-service-name");
    }

    /**
     * Reads the offered start times.
     *
     * <p>The values are collected inside the browser in one step: reading them
     * element by element would fail whenever the list is re-rendered in between.</p>
     */
    public List<String> availableSlots() {
        return attributesOf("[data-testid='slot-item']", "data-start-time");
    }

    private List<String> attributesOf(String selector, String attribute) {
        Object result = ((JavascriptExecutor) driver).executeScript(
                "var attributeName = arguments[1];"
                        + "var nodes = Array.from(document.querySelectorAll(arguments[0]));"
                        + "return nodes.map(function (element) { return element.getAttribute(attributeName); });",
                selector, attribute);
        return ((List<?>) result).stream().map(String::valueOf).toList();
    }

    public HomePage selectService(String serviceName) {
        waitForServices();
        By selector = By.cssSelector("[data-service-name='" + serviceName + "']");
        clickWithRetry(selector);
        wait.until(ExpectedConditions.attributeContains(selector, "aria-pressed", "true"));
        return this;
    }

    public String selectedServiceText() {
        return driver.findElement(byTestId("service-selected")).getText();
    }

    // --- availability -------------------------------------------------------------

    public HomePage setDate(LocalDate date) {
        WebElement dateInput = driver.findElement(byTestId("date-input"));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];"
                        + "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                dateInput, date.toString());
        return this;
    }

    public HomePage clickCheckAvailability() {
        driver.findElement(byTestId("check-availability")).click();
        return waitForAvailabilityLoaded();
    }

    /**
     * Waits until a running availability request has finished.
     *
     * <p>The interface shows a loading hint while the request is in flight, so the
     * test can wait for a real condition instead of guessing how long the refresh
     * takes.</p>
     */
    public HomePage waitForAvailabilityLoaded() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(byTestId("slots-loading")));
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(byTestId("slot-item")),
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='slot-list'] .hint"))));
        return this;
    }

    public HomePage waitForSlots() {
        wait.until(ExpectedConditions.presenceOfElementLocated(byTestId("slot-item")));
        return this;
    }

    public HomePage selectSlot(String startTime) {
        waitForSlots();
        clickWithRetry(By.cssSelector("[data-testid='slot-item'][data-start-time='" + startTime + "']"));
        wait.until(ExpectedConditions.attributeContains(
                By.cssSelector("[data-testid='slot-item'][data-start-time='" + startTime + "']"),
                "aria-pressed", "true"));
        return this;
    }

    public String selectedSlotText() {
        return driver.findElement(byTestId("selected-slot")).getText();
    }

    // --- booking ------------------------------------------------------------------

    public HomePage fillCustomer(String name, String email) {
        WebElement nameInput = driver.findElement(byTestId("customer-name"));
        nameInput.clear();
        nameInput.sendKeys(name);
        WebElement emailInput = driver.findElement(byTestId("customer-email"));
        emailInput.clear();
        emailInput.sendKeys(email);
        return this;
    }

    public HomePage submitBooking() {
        driver.findElement(byTestId("book-button")).click();
        return this;
    }

    /** Success message text, or empty when no success message is displayed. */
    public Optional<String> successMessage() {
        return visibleText("message-success");
    }

    /** Error message text, or empty when no error message is displayed. */
    public Optional<String> errorMessage() {
        return visibleText("message-error");
    }

    public HomePage waitForSuccessMessage() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(byTestId("message-success")));
        return this;
    }

    public HomePage waitForErrorMessage() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(byTestId("message-error")));
        return this;
    }

    private Optional<String> visibleText(String testIdName) {
        List<WebElement> elements = driver.findElements(byTestId(testIdName));
        if (elements.isEmpty() || !elements.get(0).isDisplayed()) {
            return Optional.empty();
        }
        return Optional.of(elements.get(0).getText().trim());
    }

    // --- appointments -------------------------------------------------------------

    public HomePage clickRefreshAppointments() {
        driver.findElement(byTestId("refresh-appointments")).click();
        return this;
    }

    public List<WebElement> appointmentRows() {
        return driver.findElements(byTestId("appointment-row"));
    }

    public HomePage waitForAppointment(int appointmentId) {
        wait.until(ExpectedConditions.presenceOfElementLocated(rowSelector(appointmentId)));
        return this;
    }

    public Optional<WebElement> appointmentRow(int appointmentId) {
        return driver.findElements(rowSelector(appointmentId)).stream().findFirst();
    }

    public String appointmentStatus(int appointmentId) {
        return rowCell(appointmentId, "appointment-status");
    }

    public String appointmentCustomer(int appointmentId) {
        return rowCell(appointmentId, "appointment-customer");
    }

    public String appointmentDate(int appointmentId) {
        return rowCell(appointmentId, "appointment-date");
    }

    public String appointmentTime(int appointmentId) {
        return rowCell(appointmentId, "appointment-time");
    }

    private String rowCell(int appointmentId, String cellTestId) {
        return appointmentRow(appointmentId)
                .orElseThrow(() -> new IllegalStateException("No row for appointment " + appointmentId))
                .findElement(byTestId(cellTestId))
                .getText()
                .trim();
    }

    public HomePage cancelAppointment(int appointmentId) {
        clickWithRetry(By.cssSelector("[data-testid='appointment-row'][data-appointment-id='"
                + appointmentId + "'] [data-testid='cancel-button']"));
        return this;
    }

    /**
     * Waits until the status cell shows the expected value.
     *
     * <p>The table is re-rendered after every operation, so the element is looked
     * up again on every poll instead of being captured once: a captured element
     * would become stale and the wait would never see the new text.</p>
     */
    public HomePage waitForStatus(int appointmentId, String expectedStatus) {
        wait.until(driver -> appointmentRow(appointmentId)
                .map(row -> expectedStatus.equals(row.findElement(byTestId("appointment-status")).getText().trim()))
                .orElse(false));
        return this;
    }

    public HomePage waitForAppointmentCount(int expectedCount) {
        wait.until(driver -> appointmentRows().size() == expectedCount);
        return this;
    }

    /** Clicks an element, re-locating it when the page re-renders in between. */
    private void clickWithRetry(By locator) {
        StaleElementReferenceException lastFailure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
                return;
            } catch (StaleElementReferenceException e) {
                lastFailure = e;
            }
        }
        throw lastFailure;
    }

    public String pageTitle() {
        return driver.getTitle();
    }

    public String pageSource() {
        return driver.getPageSource();
    }

    private static By rowSelector(int appointmentId) {
        return By.cssSelector("[data-testid='appointment-row'][data-appointment-id='" + appointmentId + "']");
    }

    private static By byTestId(String testIdName) {
        return By.cssSelector("[data-testid='" + testIdName + "']");
    }
}
