package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class PimPage extends BasePage {

    // Employee List / search
    private static final By ADD_EMPLOYEE_BUTTON = By.xpath("//button[normalize-space()='Add']");
    private static final By EMPLOYEE_NAME_SEARCH_INPUT = By.xpath("//label[text()='Employee Name']/../..//input");
    private static final By SEARCH_BUTTON = By.xpath("//button[@type='submit']");
    private static final By TABLE_ROWS = By.cssSelector(".oxd-table-body .oxd-table-card");
    private static final By ROW_CELLS = By.cssSelector(".oxd-table-cell");
    private static final By ROW_CHECKBOX = By.cssSelector("input[type='checkbox']");
    private static final By NO_RECORDS_MESSAGE = By.xpath("//span[normalize-space()='No Records Found']");
    private static final By DELETE_SELECTED_BUTTON = By.xpath("//button[contains(., 'Delete Selected')]");
    private static final By CONFIRM_DELETE_BUTTON = By.xpath("//button[normalize-space()='Yes, Delete']");

    // Add Employee form
    private static final By FIRST_NAME_INPUT = By.name("firstName");
    private static final By MIDDLE_NAME_INPUT = By.name("middleName");
    private static final By LAST_NAME_INPUT = By.name("lastName");
    private static final By EMPLOYEE_ID_INPUT = By.xpath("//label[text()='Employee Id']/../..//input");
    private static final By SAVE_BUTTON = By.xpath("//button[normalize-space()='Save']");
    private static final By REQUIRED_ERROR = By.cssSelector(".oxd-input-group .oxd-input-field-error-message");

    // Report-to (supervisor assignment)
    private static final By ADD_SUPERVISOR_BUTTON = By.xpath("//*[normalize-space()='Assigned Supervisors']/following::button[contains(normalize-space(),'Add')][1]");
    private static final By SUPERVISOR_NAME_INPUT = By.xpath("//label[text()='Name']/../..//input");
    private static final By REPORTING_METHOD_DROPDOWN = By.xpath("//label[text()='Reporting Method']/../..//div[contains(@class,'oxd-select-text--active')]");
    private static final By AUTOCOMPLETE_OPTIONS = By.xpath(
            "//div[contains(@class,'oxd-autocomplete-dropdown')]//div[contains(@class,'oxd-autocomplete-option')]");

    // Toast feedback (shared across Add/Edit/Delete operations)
    private static final By TOAST_MESSAGE = By.cssSelector(".oxd-text--toast-message");

    // Employee Profile
    private static final By JOB_TAB = By.xpath("//a[normalize-space()='Job']");
    private static final By JOB_TITLE_DROPDOWN = By.xpath("//label[text()='Job Title']/../..//div[contains(@class,'oxd-select-text--active')]");
    private static final By JOB_TITLE_VALUE = By.xpath("//label[text()='Job Title']/../..//div[contains(@class,'oxd-select-text-input')]");

    // Photo upload
    private static final By PHOTO_FILE_INPUT = By.cssSelector("input.employee-image-input, input[type='file']");
    private static final By PHOTO_UPLOAD_ERROR = By.cssSelector(".oxd-input-field-error-message, .oxd-toast-content--error");

    // Emergency contacts
    private static final By EMERGENCY_CONTACTS_TAB = By.xpath("//a[normalize-space()='Emergency Contacts']");
    private static final By EC_ADD_BUTTON = By.xpath("//button[normalize-space()='Add']");
    private static final By EC_NAME_INPUT = By.name("name");
    private static final By EC_RELATIONSHIP_INPUT = By.name("relationship");
    private static final By EC_MOBILE_INPUT = By.name("mobile");
    private static final By EC_SAVE_BUTTON = By.xpath("//button[normalize-space()='Save']");
    private static final By EC_ROWS = By.cssSelector(".orangehrm-container .oxd-table-card");

    public PimPage(WebDriver driver) {
        super(driver);
    }

    // ---- Employee List ----

    public PimPage clickAddEmployee() {
        click(ADD_EMPLOYEE_BUTTON);
        return this;
    }

    public void fillNewEmployee(String firstName, String lastName) {
        type(FIRST_NAME_INPUT, firstName);
        type(LAST_NAME_INPUT, lastName);
    }

    public void saveEmployee() {
        click(SAVE_BUTTON);
    }

    /**
     * Waits for the success toast before returning, so a caller that
     * immediately searches/navigates to the just-created employee isn't
     * racing the save itself — useful wherever success is actually
     * expected (unlike the invalid-save path, which must not wait for a
     * toast that will never appear).
     */
    public void saveEmployeeAndConfirm() {
        click(SAVE_BUTTON);
        // The Employee Id field auto-populates with the app's own
        // next-sequential guess, which on this heavily-populated, never-reset
        // shared demo (many employees created by this suite's own repeated
        // runs, plus whoever else is using the same public instance) can
        // already be taken by the time Save actually runs — a transient
        // collision, not a real validation failure. A random id practically
        // never collides twice in a row, so retrying once with one is enough.
        if (hasEmployeeIdConflict()) {
            type(EMPLOYEE_ID_INPUT, String.valueOf(100000 + new java.util.Random().nextInt(900000)));
            click(SAVE_BUTTON);
        }
        isVisible(TOAST_MESSAGE);
    }

    private boolean hasEmployeeIdConflict() {
        try {
            return new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(d -> d.findElements(REQUIRED_ERROR).stream()
                            .anyMatch(e -> e.getText().toLowerCase().contains("already exists")));
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    public int getRequiredErrorCount() {
        return waitAllVisible(REQUIRED_ERROR).size();
    }

    /**
     * Saving redirects to the new employee's own Personal Details page
     * (.../pim/viewPersonalDetails/empNumber/&lt;id&gt;); pulling the id out of
     * that URL is the only reliable way to address this specific employee
     * afterward (e.g. to assign a supervisor), since there is no unique
     * search key otherwise on a shared demo where names can repeat.
     */
    public String getCurrentEmployeeNumber() {
        // This shared, never-reset public demo accumulates employees from
        // every run (including this suite's own repeated ones), and the
        // post-save redirect slows down as that list grows — the standard
        // explicit wait isn't always enough, so this gets its own longer
        // budget rather than raising the global timeout for everything.
        String url = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(90))
                .until(d -> {
                    String u = d.getCurrentUrl();
                    return u.contains("/pim/viewPersonalDetails/empNumber/") ? u : null;
                });
        return url.substring(url.lastIndexOf('/') + 1);
    }

    /**
     * Reads First/Middle/Last Name directly from the currently open
     * Personal Details page (e.g. after DashboardPage.goToMyInfo()).
     * Unlike the topbar's own display name — which is just first + last,
     * silently dropping any middle name — this is the exact, complete name
     * the app itself indexes for autocomplete searches like Report-to's
     * supervisor picker. That gap matters here: this shared, never-reset
     * demo accumulates many employees from other automated runs sharing
     * the same generic first name (e.g. "Demo"), so searching on a
     * first-name-only string that's missing its middle name falls through
     * to an ambiguous match and silently assigns the wrong employee as
     * supervisor instead of the one actually logged in.
     */
    public String getOwnFullName() {
        // The form is visible (and its inputs structurally present) well
        // before its async-fetched values actually populate them, so a
        // plain read right after navigating here can catch it still blank
        // — wait for the (always-required) First Name field specifically
        // to actually hold a value before reading any of the three.
        waitUntil(d -> {
            String value = d.findElement(FIRST_NAME_INPUT).getAttribute("value");
            return value != null && !value.isEmpty();
        });
        String first = driver.findElement(FIRST_NAME_INPUT).getAttribute("value").trim();
        String middle = driver.findElements(MIDDLE_NAME_INPUT).stream()
                .findFirst()
                .map(e -> e.getAttribute("value"))
                .orElse("")
                .trim();
        String last = driver.findElement(LAST_NAME_INPUT).getAttribute("value").trim();
        return java.util.stream.Stream.of(first, middle, last)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    /**
     * Assigns a supervisor via the employee's own Report-to page. Approval
     * authority over a leave request in this app is tied to being the
     * requester's supervisor, not simply having the Admin role — a
     * freshly created employee has no supervisor at all, so nothing (not
     * even an Admin) can approve their leave until one is assigned here.
     */
    public void assignSupervisor(String empNumber, String supervisorDisplayName) {
        driver.get(config.ConfigReader.get("base.url") + "/web/index.php/pim/viewReportToDetails/empNumber/" + empNumber);
        click(ADD_SUPERVISOR_BUTTON);
        selectFromOxdAutocomplete(SUPERVISOR_NAME_INPUT, AUTOCOMPLETE_OPTIONS, supervisorDisplayName);
        // Reporting Method is a second required field on this same form,
        // easy to miss since Name is the only one that looks essential —
        // left at its "-- Select --" default, Save is silently a no-op (no
        // toast, no error, no row added), which otherwise surfaces several
        // steps downstream as a confusing "leave request row not found"
        // once nothing (not even an Admin) turns out to have approval
        // authority over it.
        selectFromOxdDropdown(REPORTING_METHOD_DROPDOWN, "Direct");
        click(SAVE_BUTTON);
        // Save gives no other confirmation to wait on, and moving straight
        // to the approval flow's own search risks racing this assignment
        // before it's actually persisted server-side — wait for the
        // success toast before returning.
        isVisible(TOAST_MESSAGE);
    }

    public String getToastMessage() {
        return getText(TOAST_MESSAGE);
    }

    public PimPage searchByEmployeeName(String name) {
        // Add Employee's Save keeps the form on the same page rather than
        // navigating away, so a caller that just created a prerequisite
        // employee (the "an employee named X exists" step) is not
        // necessarily on the Employee List — force it explicitly rather
        // than assume the search filter is already on screen.
        driver.get(config.ConfigReader.get("base.url") + "/web/index.php/pim/viewEmployeeList");
        type(EMPLOYEE_NAME_SEARCH_INPUT, name);
        click(SEARCH_BUTTON);
        return this;
    }

    /**
     * The employee list table has separate "First (& Middle) Name" and "Last
     * Name" columns (cells index 2 and 3, after the checkbox at index 0 and
     * Id at index 1) rather than one combined name column.
     */
    public List<String> getSearchResultNames() {
        return waitAllVisible(TABLE_ROWS).stream()
                .map(this::rowFullName)
                .collect(Collectors.toList());
    }

    private String rowFullName(WebElement row) {
        List<WebElement> cells = row.findElements(ROW_CELLS);
        if (cells.size() < 4) {
            return row.getText();
        }
        return (cells.get(2).getText().trim() + " " + cells.get(3).getText().trim()).trim();
    }

    public boolean isNoRecordsMessageShown() {
        return isVisible(NO_RECORDS_MESSAGE);
    }

    public PimPage openEmployeeFromList(String fullName) {
        WebElement row = findRowByName(fullName);
        row.findElements(ROW_CELLS).get(2).click();
        return this;
    }

    // ---- Job title edit ----

    public PimPage openJobTab() {
        click(JOB_TAB);
        return this;
    }

    public void updateJobTitle(String jobTitle) {
        selectFromOxdDropdown(JOB_TITLE_DROPDOWN, jobTitle);
        click(SAVE_BUTTON);
    }

    public String getJobTitleValue() {
        return getText(JOB_TITLE_VALUE);
    }

    // ---- Delete ----

    public PimPage selectEmployeeCheckbox(String fullName) {
        WebElement row = findRowByName(fullName);
        WebElement checkbox = row.findElement(ROW_CHECKBOX);
        // The checkbox's own custom check-icon overlay sits on top of the
        // native input and intercepts a normal click; a JS click bypasses
        // that without needing to guess a different, more specific target.
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", checkbox);
        return this;
    }

    private WebElement findRowByName(String fullName) {
        String[] parts = fullName.split(" ", 2);
        String first = parts[0];
        String last = parts.length > 1 ? parts[1] : "";
        return waitAllVisible(TABLE_ROWS).stream()
                .filter(row -> {
                    List<WebElement> cells = row.findElements(ROW_CELLS);
                    return cells.size() >= 4
                            && cells.get(2).getText().trim().equalsIgnoreCase(first)
                            && cells.get(3).getText().trim().equalsIgnoreCase(last);
                })
                .findFirst()
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException("Employee row not found: " + fullName));
    }

    public PimPage deleteEmployee(String fullName) {
        selectEmployeeCheckbox(fullName);
        click(DELETE_SELECTED_BUTTON);
        return this;
    }

    public boolean isDeleteConfirmationShown() {
        return isVisible(CONFIRM_DELETE_BUTTON);
    }

    public PimPage confirmDelete() {
        click(CONFIRM_DELETE_BUTTON);
        return this;
    }

    // ---- Photo upload ----

    public PimPage uploadProfilePhoto(String absoluteFilePath) {
        driver.findElement(PHOTO_FILE_INPUT).sendKeys(absoluteFilePath);
        return this;
    }

    public boolean isPhotoUploadErrorShown() {
        return isVisible(PHOTO_UPLOAD_ERROR);
    }

    // ---- Emergency contacts ----

    public PimPage openEmergencyContactsTab() {
        click(EMERGENCY_CONTACTS_TAB);
        return this;
    }

    public PimPage clickAddEmergencyContact() {
        click(EC_ADD_BUTTON);
        return this;
    }

    public void fillEmergencyContact(String name, String relationship, String mobile) {
        type(EC_NAME_INPUT, name);
        type(EC_RELATIONSHIP_INPUT, relationship);
        type(EC_MOBILE_INPUT, mobile);
        click(EC_SAVE_BUTTON);
    }

    public boolean isEmergencyContactListed(String name) {
        return waitAllVisible(EC_ROWS).stream().anyMatch(row -> row.getText().contains(name));
    }
}
