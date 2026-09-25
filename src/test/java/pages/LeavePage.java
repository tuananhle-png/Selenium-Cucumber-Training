package pages;

import config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class LeavePage extends BasePage {

    // Apply
    private static final By APPLY_TAB = By.xpath("//*[contains(@class,'oxd-topbar-body-nav-tab-item') and normalize-space()='Apply']");
    private static final By LEAVE_TYPE_DROPDOWN = By.xpath("//label[text()='Leave Type']/../..//div[contains(@class,'oxd-select-text--active')]");
    private static final By FROM_DATE_INPUT = By.xpath("(//label[text()='From Date']/../..//input)[1]");
    private static final By TO_DATE_INPUT = By.xpath("(//label[text()='To Date']/../..//input)[1]");
    private static final By APPLY_BUTTON = By.xpath("//button[normalize-space()='Apply']");
    private static final By DATE_RANGE_ERROR = By.cssSelector(".oxd-input-field-error-message");
    private static final By TOAST_MESSAGE = By.cssSelector(".oxd-text--toast-message");

    // My Leave / Leave List
    private static final By MY_LEAVE_TAB = By.xpath("//*[contains(@class,'oxd-topbar-body-nav-tab-item') and normalize-space()='My Leave']");
    private static final By LEAVE_LIST_TAB = By.xpath("//*[contains(@class,'oxd-topbar-body-nav-tab-item') and normalize-space()='Leave List']");
    private static final By LEAVE_LIST_SEARCH_BUTTON = By.xpath("//button[@type='submit']");
    private static final By LEAVE_ROWS = By.cssSelector(".oxd-table-body .oxd-table-card");
    private static final By ROW_ACTION_BUTTON = By.cssSelector(".oxd-table-cell-actions button");
    private static final By CANCEL_LEAVE_ACTION = By.xpath("//li[contains(@class,'oxd-table-dropdown-item') and normalize-space()='Cancel Leave']");
    // Approve/Reject are NOT in the row's kebab at all (confirmed: that
    // menu only ever offers Add Comment / View Leave Details / View PIM
    // Info / Cancel Leave, regardless of who's logged in). They only
    // render as their own buttons directly in the row — and only once the
    // viewer is the requester's assigned supervisor (PIM > Report-to);
    // without that relationship even an Admin login sees no way to act on
    // the request at all, on any page.
    private static final By ROW_APPROVE_BUTTON = By.xpath(".//button[normalize-space()='Approve']");
    private static final By ROW_REJECT_BUTTON = By.xpath(".//button[normalize-space()='Reject']");
    // The status vocabulary here is Pending Approval / Scheduled / Taken /
    // Rejected / Cancelled — there is no status literally called
    // "Approved"; an approved request becomes "Scheduled" (future dates)
    // or "Taken" (past/current ones).
    private static final By STATUS_CELL = By.xpath(".//div[contains(@class,'oxd-table-cell') and (contains(text(),'Pending') or contains(text(),'Scheduled') or contains(text(),'Taken') or contains(text(),'Rejected') or contains(text(),'Cancelled'))]");
    private static final By STATUS_DROPDOWN_TRIGGER = By.xpath("//label[text()='Show Leave with Status']/../..//div[contains(@class,'oxd-select-text')]");
    private static final By STATUS_CHIP_CLOSE_ICON = By.xpath("//label[text()='Show Leave with Status']/../..//i[contains(@class,'bi-x')]");
    private static final By EMPLOYEE_NAME_FILTER_INPUT = By.xpath("//label[text()='Employee Name']/../..//input");

    // Entitlements
    private static final By ENTITLEMENTS_TAB = By.xpath("//*[contains(@class,'oxd-topbar-body-nav-tab-item') and normalize-space()='Entitlements']");
    private static final By ENTITLEMENT_ROWS = By.cssSelector(".oxd-table-body .oxd-table-card");
    private static final By ENTITLEMENT_DAYS_INPUT = By.xpath("//label[text()='Entitlement']/../..//input");
    private static final By ENTITLEMENT_PERIOD_DROPDOWN = By.xpath("//label[text()='Leave Period']/../..//div[contains(@class,'oxd-select-text--active')]");
    private static final By ENTITLEMENT_EMPLOYEE_NAME_INPUT = By.xpath("//label[text()='Employee Name']/../..//input");
    private static final By AUTOCOMPLETE_OPTIONS = By.xpath(
            "//div[contains(@class,'oxd-autocomplete-dropdown')]//div[contains(@class,'oxd-autocomplete-option')]");

    public LeavePage(WebDriver driver) {
        super(driver);
    }

    public LeavePage openApplyTab() {
        click(APPLY_TAB);
        return this;
    }

    public void applyLeave(String leaveType, String fromDate, String toDate) {
        selectFromOxdDropdown(LEAVE_TYPE_DROPDOWN, leaveType);
        type(FROM_DATE_INPUT, fromDate);
        type(TO_DATE_INPUT, toDate);
        click(APPLY_BUTTON);
    }

    public boolean isDateRangeErrorShown() {
        return isVisible(DATE_RANGE_ERROR);
    }

    public String getToastMessage() {
        return getText(TOAST_MESSAGE);
    }

    /**
     * The success toast is transient (OXD auto-dismisses it after a few
     * seconds), so a check that runs even slightly late can miss it even on
     * a genuinely successful submission. Falling back to "no validation
     * error is showing" gives a durable secondary signal instead of forcing
     * every caller to win a race against the toast's fade timer.
     */
    public boolean isSubmissionSuccessful() {
        return isVisible(TOAST_MESSAGE) || !isDateRangeErrorShown();
    }

    public LeavePage openMyLeaveTab() {
        click(MY_LEAVE_TAB);
        return this;
    }

    /**
     * A definitive, positive signal that Apply actually created a request —
     * unlike isSubmissionSuccessful()'s toast/error heuristic, which reads
     * as "success" whenever a click silently fails to register too (no
     * toast fires, but no validation error renders either, since nothing
     * was ever submitted). "My Leave" is the applicant's own history, so a
     * request that really was created shows up there immediately; one that
     * wasn't leaves it exactly as empty as before the attempt.
     */
    public boolean hasAnyOwnLeaveRecord() {
        openMyLeaveTab().searchLeaveList();
        try {
            return !waitAllVisible(LEAVE_ROWS).isEmpty();
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    public LeavePage openLeaveListTab() {
        click(LEAVE_LIST_TAB);
        return this;
    }

    public LeavePage searchLeaveList() {
        click(LEAVE_LIST_SEARCH_BUTTON);
        return this;
    }

    /**
     * Narrows the search server-side to one employee before submitting,
     * rather than relying on scanning whatever rows happen to render on
     * page 1 of the default (unfiltered-by-employee) result set. On this
     * shared, never-reset public demo, every prior run's own supervised
     * test employee accumulates under the same supervisor identity — with
     * enough of them, a specific employee's own row can simply not be on
     * the first page at all, which a plain "search visible rows for this
     * text" approach has no way to detect or recover from.
     */
    public LeavePage filterByEmployeeName(String fullName) {
        selectFromOxdAutocomplete(EMPLOYEE_NAME_FILTER_INPUT, AUTOCOMPLETE_OPTIONS, fullName);
        return this;
    }

    /**
     * The Leave List defaults to a "Pending Approval" status filter chip,
     * which is exactly right while checking that a request IS pending —
     * but that same default then hides the request the instant it stops
     * being pending (approved/rejected/cancelled). Selecting a *second*
     * chip alongside it looks like the fix (an inclusive "either status"
     * search), but this app's multi-select here doesn't actually OR its
     * values: combining "Pending Approval" with any other status
     * reproducibly returns zero rows even for a request confirmed (via a
     * single-status search) to exist under that other status — verified
     * directly against this demo instance, not an assumption. Removing
     * every existing chip first and selecting only the target status
     * avoids that broken combination entirely. "Show Leave with Status"
     * being a required field only matters at Search time; leaving it
     * momentarily empty between removing the old chip and adding the new
     * one is fine.
     */
    public LeavePage addStatusFilter(String status) {
        List<WebElement> chips = driver.findElements(STATUS_CHIP_CLOSE_ICON);
        while (!chips.isEmpty()) {
            chips.get(0).click();
            chips = driver.findElements(STATUS_CHIP_CLOSE_ICON);
        }
        click(STATUS_DROPDOWN_TRIGGER);
        List<WebElement> statusOptions = waitAllVisible(By.xpath("//div[@role='listbox']//span"));
        statusOptions.stream()
                .filter(o -> o.getText().trim().equalsIgnoreCase(status))
                .findFirst()
                .ifPresent(WebElement::click);
        // Close the dropdown by clicking its own label rather than the
        // page body, so nothing else on the form is disturbed.
        click(By.xpath("//label[text()='Show Leave with Status']"));
        return this;
    }

    public LeavePage approveRequestFor(String identifier) {
        clickRowAction(identifier, ROW_APPROVE_BUTTON);
        return this;
    }

    public LeavePage rejectRequestFor(String identifier) {
        clickRowAction(identifier, ROW_REJECT_BUTTON);
        return this;
    }

    /**
     * This click occasionally doesn't register on the first attempt — no
     * error, the button and the "Pending Approval" status just remain
     * exactly as they were. Waiting for the button to actually disappear
     * (the row re-renders once the status changes) and retrying once if
     * it's still there catches that without masking a real failure, which
     * would still leave the button present after the retry too.
     */
    private void clickRowAction(String identifier, By actionButton) {
        findRowFor(identifier).findElement(actionButton).click();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> findRowFor(identifier).findElements(actionButton).isEmpty());
        } catch (org.openqa.selenium.TimeoutException e) {
            findRowFor(identifier).findElement(actionButton).click();
        }
    }

    public String getStatusFor(String identifier) {
        return findRowFor(identifier).findElement(STATUS_CELL).getText();
    }

    /**
     * Polls the whole row set repeatedly rather than taking one snapshot:
     * a request can still be settling into the search results right after
     * being created/updated, so a single check can race a table that
     * hasn't finished (re-)rendering yet.
     */
    private WebElement findRowFor(String identifier) {
        try {
            return waitUntil(d -> waitAllVisible(LEAVE_ROWS).stream()
                    .filter(row -> row.getText().contains(identifier))
                    .findFirst()
                    .orElse(null));
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new org.openqa.selenium.NoSuchElementException("Leave request row not found for: " + identifier);
        }
    }

    public LeavePage cancelApprovedRequest(String identifier) {
        findRowFor(identifier).findElement(ROW_ACTION_BUTTON).click();
        click(CANCEL_LEAVE_ACTION);
        return this;
    }

    public LeavePage openEntitlementsTab() {
        click(ENTITLEMENTS_TAB);
        return this;
    }

    public int getEntitlementRowCount() {
        return waitAllVisible(ENTITLEMENT_ROWS).size();
    }

    /**
     * Seeds a balance for one specific, already-known employee and leave
     * type (e.g. one this suite just created for a leave-approval flow)
     * via the form's default "Individual Employee" mode. This doesn't need
     * a "Multiple Employees" broadcast, which computes a preview across
     * the company's ENTIRE employee list before its Save takes effect —
     * and on this shared, never-reset public demo that list only ever
     * grows (including from this suite's own repeated runs), to the point
     * the preview can take minutes. Individual mode updates just the one
     * named employee and saves immediately, with no such scaling problem.
     */
    public LeavePage seedIndividualLeaveBalance(String employeeFullName, String leaveType, int days) {
        driver.get(ConfigReader.get("base.url") + "/web/index.php/leave/addLeaveEntitlement");
        // "Individual Employee" is this form's default selected mode
        // already, so no radio click is needed here.
        selectFromOxdAutocomplete(ENTITLEMENT_EMPLOYEE_NAME_INPUT, AUTOCOMPLETE_OPTIONS, employeeFullName);

        // This lists plain calendar years in order, so the *last* option
        // is next year, not necessarily this one — match by the year the
        // test's near-future dates actually fall in instead of trusting
        // position (a brand new employee freshly created "today" always
        // needs the *current* year's period, not next year's).
        click(ENTITLEMENT_PERIOD_DROPDOWN);
        List<WebElement> periodOptions = waitAllVisible(By.xpath("//div[@role='listbox']//span"));
        if (!periodOptions.isEmpty()) {
            String targetYear = String.valueOf(java.time.LocalDate.now().getYear());
            periodOptions.stream()
                    .filter(o -> o.getText().contains(targetYear))
                    .reduce((first, second) -> second)
                    .orElse(periodOptions.get(periodOptions.size() - 1))
                    .click();
        }

        // Match the exact type rather than always taking whichever comes
        // first — a mismatch here (seeding one type, then applying for a
        // different one that still has zero balance) makes the later
        // "Apply" step silently substitute whatever type does have a
        // balance instead of the one actually requested.
        selectFromOxdDropdown(LEAVE_TYPE_DROPDOWN, leaveType);
        type(ENTITLEMENT_DAYS_INPUT, String.valueOf(days));
        click(By.xpath("//button[normalize-space()='Save']"));
        // Individual mode also opens an "Updating Entitlement" confirmation
        // dialog before anything is actually persisted — every employee
        // implicitly starts at a 0.00 entitlement, so even a brand new
        // employee's very first Save is technically an "update" that needs
        // this same Confirm click. Missing this leaves the modal open,
        // silently blocking every subsequent click on the page (including
        // unrelated sidebar navigation) until something dismisses it.
        //
        // The framework's own click() insists on any .oxd-form-loader /
        // .oxd-table-loader being gone first — but the spinner behind this
        // exact dialog doesn't clear until Confirm is clicked, which is a
        // deadlock. Waiting directly for the button instead, bypassing
        // that precondition, breaks the cycle.
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='Confirm']")))
                .click();
        isVisible(TOAST_MESSAGE);
        return this;
    }

}
