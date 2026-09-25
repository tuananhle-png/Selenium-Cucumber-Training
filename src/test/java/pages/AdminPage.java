package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class AdminPage extends BasePage {

    // User Management (the "Admin" sidebar item lands directly on the Users list)
    private static final By ADD_BUTTON = By.xpath("//button[normalize-space()='Add']");
    private static final By USER_ROLE_DROPDOWN = By.xpath("//label[text()='User Role']/../..//div[contains(@class,'oxd-select-text--active')]");
    private static final By EMPLOYEE_NAME_INPUT = By.xpath("//label[text()='Employee Name']/../..//input");
    private static final By EMPLOYEE_AUTOCOMPLETE_OPTIONS = By.xpath(
            "//div[contains(@class,'oxd-autocomplete-dropdown')]//div[contains(@class,'oxd-autocomplete-option')]");
    private static final By STATUS_DROPDOWN = By.xpath("//label[text()='Status']/../..//div[contains(@class,'oxd-select-text--active')]");
    private static final By USERNAME_INPUT = By.xpath("//label[text()='Username']/../..//input");
    private static final By PASSWORD_INPUT = By.xpath("(//label[text()='Password']/../..//input)[1]");
    private static final By CONFIRM_PASSWORD_INPUT = By.xpath("//label[text()='Confirm Password']/../..//input");
    private static final By SAVE_BUTTON = By.xpath("//button[normalize-space()='Save']");
    private static final By USERNAME_SEARCH_INPUT = By.xpath("//label[text()='Username']/../..//input");
    private static final By SEARCH_BUTTON = By.xpath("//button[@type='submit']");
    private static final By USER_ROWS = By.cssSelector(".oxd-table-body .oxd-table-card");
    private static final By TOAST_ERROR = By.cssSelector(".oxd-text--toast-message, .oxd-input-field-error-message");

    // Job Titles
    private static final By JOB_MENU = By.xpath("//span[normalize-space()='Job']");
    private static final By JOB_TITLES_SUBMENU = By.xpath("//a[normalize-space()='Job Titles']");
    private static final By JOB_TITLE_INPUT = By.name("jobTitle");
    private static final By JOB_ROWS = By.cssSelector(".oxd-table-body .oxd-table-card");
    private static final By ROW_DELETE_ICON = By.cssSelector(".bi-trash");
    private static final By DELETE_WARNING = By.cssSelector(".oxd-text--toast-message");

    // Organization
    private static final By ORG_MENU = By.xpath("//span[normalize-space()='Organization']");
    private static final By GENERAL_INFO_SUBMENU = By.xpath("//a[normalize-space()='General Information']");
    private static final By ORG_NAME_INPUT = By.name("name");
    private static final By STRUCTURE_SUBMENU = By.xpath("//a[normalize-space()='Structure']");
    private static final By ADD_UNIT_BUTTON = By.cssSelector(".bi-plus");
    private static final By UNIT_NAME_INPUT = By.name("name");
    private static final By HEADER_ORG_NAME = By.cssSelector(".oxd-topbar-header-breadcrumb h6");

    public AdminPage(WebDriver driver) {
        super(driver);
    }

    // ---- Users ----

    public AdminPage openUsersList() {
        // Not a no-op: this is called again after creating/editing a user,
        // by which point we've navigated away from the list (to the Add User
        // form, then wherever Save lands), so it must actually (re)navigate
        // rather than assume we're still on the list from the initial
        // "Admin" sidebar click.
        driver.get(config.ConfigReader.get("base.url") + "/web/index.php/admin/viewSystemUsers");
        return this;
    }

    public AdminPage clickAdd() {
        click(ADD_BUTTON);
        return this;
    }

    public void createUser(String role, String employeeName, String username, String password) {
        selectFromOxdDropdown(USER_ROLE_DROPDOWN, role);
        selectFromOxdAutocomplete(EMPLOYEE_NAME_INPUT, EMPLOYEE_AUTOCOMPLETE_OPTIONS, employeeName);
        selectFromOxdDropdown(STATUS_DROPDOWN, "Enabled");
        type(USERNAME_INPUT, username);
        // Typing fires an immediate "Should be at least 5 characters"
        // length check that only clears once the field's debounced
        // validation catches up to the real (valid) value a moment later.
        // The rest of this flow (Password, Confirm Password, Save) runs
        // fast enough that Save can otherwise get clicked while that stale
        // error is still flagged, which blocks the submit even though the
        // typed username is fine — wait for the transient error to clear.
        // This same field also live-validates uniqueness, though, and that
        // error is not transient: the duplicate-username negative scenario
        // deliberately types an already-taken username expecting exactly
        // this error to persist. Using the full explicit-wait budget here
        // would hang for its entire length on that case, waiting for an
        // error that will never clear by design — a short, bounded wait
        // covers the transient case's real (sub-second) settle time while
        // still letting Save proceed (and surface the duplicate error there
        // instead) when the error genuinely isn't going away.
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(5))
                    .until(d -> !d.findElement(USERNAME_INPUT).getAttribute("class").contains("oxd-input--error"));
        } catch (org.openqa.selenium.TimeoutException e) {
            // Genuinely persistent (e.g. duplicate username) — proceed and
            // let Save/the caller's own assertion handle it.
        }
        type(PASSWORD_INPUT, password);
        type(CONFIRM_PASSWORD_INPUT, password);
        click(SAVE_BUTTON);
        // Wait for feedback (success toast or a duplicate-username error)
        // before returning, so a caller that immediately searches for the
        // just-created user isn't racing the save itself.
        isVisible(TOAST_ERROR);
    }

    public AdminPage searchUser(String username) {
        type(USERNAME_SEARCH_INPUT, username);
        click(SEARCH_BUTTON);
        return this;
    }

    public boolean isUserListed(String username) {
        // A one-shot check can race the search results still settling after
        // a fresh creation; retry the search itself (not just the read)
        // rather than assume the first attempt reflects the final state.
        try {
            return waitUntil(d -> {
                searchUser(username);
                List<WebElement> rows = d.findElements(USER_ROWS);
                return !rows.isEmpty() && rows.stream().anyMatch(row -> row.getText().contains(username));
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    public String getToastOrValidationError() {
        return getText(TOAST_ERROR);
    }

    public AdminPage openUser(String username) {
        // The Username cell itself isn't a link — the row's edit (pencil)
        // icon in the Actions column is the actual navigation trigger.
        WebElement row = waitAllVisible(USER_ROWS).stream()
                .filter(r -> r.getText().contains(username))
                .findFirst()
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException("User row not found: " + username));
        row.findElement(By.cssSelector(".bi-pencil-fill")).click();
        return this;
    }

    public void disableUser() {
        selectFromOxdDropdown(STATUS_DROPDOWN, "Disabled");
        click(SAVE_BUTTON);
    }

    // ---- Job titles ----

    public AdminPage openJobTitlesList() {
        click(JOB_MENU);
        click(JOB_TITLES_SUBMENU);
        return this;
    }

    public void addJobTitle(String title) {
        click(ADD_BUTTON);
        type(JOB_TITLE_INPUT, title);
        click(SAVE_BUTTON);
    }

    public boolean isJobTitleListed(String title) {
        return waitAllVisible(JOB_ROWS).stream().anyMatch(row -> row.getText().contains(title));
    }

    public AdminPage attemptDeleteJobTitle(String title) {
        By row = By.xpath("//div[contains(@class,'oxd-table-body')]//div[contains(@class,'oxd-table-card') and contains(.,'" + title + "')]");
        driver.findElement(row).findElement(ROW_DELETE_ICON).click();
        return this;
    }

    public boolean isDeleteWarningShown() {
        return isVisible(DELETE_WARNING);
    }

    // ---- Organization ----

    public AdminPage openGeneralInformation() {
        click(ORG_MENU);
        click(GENERAL_INFO_SUBMENU);
        return this;
    }

    public void updateOrganizationName(String name) {
        type(ORG_NAME_INPUT, name);
        click(SAVE_BUTTON);
    }

    public AdminPage openStructure() {
        click(ORG_MENU);
        click(STRUCTURE_SUBMENU);
        return this;
    }

    public void addOrganizationUnit(String unitName) {
        click(ADD_UNIT_BUTTON);
        type(UNIT_NAME_INPUT, unitName);
        click(SAVE_BUTTON);
    }

    public String getHeaderBranding() {
        return getText(HEADER_ORG_NAME);
    }
}
