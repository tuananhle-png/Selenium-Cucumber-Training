package pages;

import config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Attendance/Punch is not a top-level sidebar module in this OrangeHRM
 * version — it lives under Time > Attendance (a dropdown) > Punch In/Out,
 * at the direct URL /web/index.php/attendance/punchIn. That single page
 * toggles between showing an "In" button or an "Out" button depending on
 * whether the employee currently has an open punch, rather than exposing
 * separate punch-in/punch-out pages.
 */
public class AttendancePage extends BasePage {

    private static final By ATTENDANCE_MENU = By.xpath("//span[normalize-space()='Attendance']");
    private static final By PUNCH_IN_BUTTON = By.xpath("//button[normalize-space()='In']");
    private static final By PUNCH_OUT_BUTTON = By.xpath("//button[normalize-space()='Out']");
    private static final By TOAST_MESSAGE = By.cssSelector(".oxd-text--toast-message");

    private static final By MY_RECORDS_LINK = By.xpath("//a[normalize-space()='My Records']");
    private static final By EMPLOYEE_RECORDS_LINK = By.xpath("//a[normalize-space()='Employee Records']");
    private static final By REPORTS_LINK = By.xpath("//a[normalize-space()='Reports']");
    private static final By RECORDS_TABLE_ROWS = By.cssSelector(".oxd-table-body .oxd-table-card");
    private static final By ROW_EDIT_ICON = By.cssSelector(".bi-pencil-fill");
    private static final By SAVE_BUTTON = By.xpath("//button[normalize-space()='Save']");
    private static final By FROM_DATE_INPUT = By.xpath("(//label[text()='From Date']/../..//input)[1]");
    private static final By TO_DATE_INPUT = By.xpath("(//label[text()='To Date']/../..//input)[1]");
    private static final By GENERATE_BUTTON = By.xpath("//button[normalize-space()='View']");

    public AttendancePage(WebDriver driver) {
        super(driver);
    }

    public AttendancePage openPunchPage() {
        driver.get(ConfigReader.get("base.url") + "/web/index.php/attendance/punchIn");
        return this;
    }

    public boolean isPunchInAvailable() {
        return isVisible(PUNCH_IN_BUTTON);
    }

    public boolean isPunchOutAvailable() {
        return isVisible(PUNCH_OUT_BUTTON);
    }

    /**
     * Idempotent precondition helper: punch state belongs to the Admin
     * account itself and persists across scenarios (each scenario gets a
     * fresh browser via Hooks, but not a fresh account), so a prior
     * scenario that punched in without punching out leaves the account
     * already in that state. If "In" isn't offered, the account is already
     * punched in — the intent of "have punched in" is already satisfied.
     */
    public AttendancePage punchIn() {
        openPunchPage();
        if (isPunchInAvailable()) {
            click(PUNCH_IN_BUTTON);
            // click() only waits for the button itself to be clickable, not
            // for the server to persist the punch — a caller that
            // immediately reloads the page (e.g. to assert the duplicate
            // punch is blocked) can otherwise race the submission and still
            // see the stale "In" state.
            waitVisible(PUNCH_OUT_BUTTON);
        }
        return this;
    }

    /**
     * Forces a clean "not punched in" state first, then performs an actual
     * punch-in — for scenarios that test the punch-in action itself (as
     * opposed to using it merely as a precondition), where leftover state
     * from an earlier scenario would otherwise make the action a no-op.
     */
    public AttendancePage punchInFresh() {
        openPunchPage();
        if (isPunchOutAvailable()) {
            click(PUNCH_OUT_BUTTON);
            waitVisible(PUNCH_IN_BUTTON);
            openPunchPage();
        }
        click(PUNCH_IN_BUTTON);
        waitVisible(PUNCH_OUT_BUTTON);
        return this;
    }

    public AttendancePage punchOut() {
        openPunchPage();
        if (isPunchOutAvailable()) {
            click(PUNCH_OUT_BUTTON);
            waitVisible(PUNCH_IN_BUTTON);
        }
        return this;
    }

    public String getToastMessage() {
        return getText(TOAST_MESSAGE);
    }

    /**
     * The success toast is transient and can be missed by a check that
     * runs even slightly late (same class of issue as Leave's toast).
     * Falling back to "the button offered flipped to the other action"
     * gives a durable secondary signal that the punch actually took.
     */
    public boolean isPunchInSuccessful() {
        return isVisible(TOAST_MESSAGE) || isPunchOutAvailable();
    }

    public boolean isPunchOutSuccessful() {
        return isVisible(TOAST_MESSAGE) || isPunchInAvailable();
    }

    private void openAttendanceSubmenuLink(By link) {
        driver.get(ConfigReader.get("base.url") + "/web/index.php/time/viewEmployeeTimesheet");
        click(ATTENDANCE_MENU);
        click(link);
    }

    public AttendancePage openMyRecords() {
        openAttendanceSubmenuLink(MY_RECORDS_LINK);
        return this;
    }

    public int getRecordCount() {
        return waitAllVisible(RECORDS_TABLE_ROWS).size();
    }

    public AttendancePage openEmployeeRecords() {
        openAttendanceSubmenuLink(EMPLOYEE_RECORDS_LINK);
        return this;
    }

    public AttendancePage editFirstRecordPunchInTime(String time) {
        click(ROW_EDIT_ICON);
        By punchInTimeInput = By.xpath("//label[contains(text(),'Time')]/../..//input");
        type(punchInTimeInput, time);
        click(SAVE_BUTTON);
        return this;
    }

    public boolean isRecordsTableShown() {
        return isVisible(RECORDS_TABLE_ROWS);
    }

    public AttendancePage openReports() {
        // Unlike My Records/Employee Records, "Reports" is a sibling
        // top-level tab next to "Timesheets"/"Attendance" on the Time
        // module's own topbar — not nested inside the Attendance dropdown.
        driver.get(ConfigReader.get("base.url") + "/web/index.php/time/viewEmployeeTimesheet");
        click(REPORTS_LINK);
        return this;
    }

    public AttendancePage generateReport(String fromDate, String toDate) {
        type(FROM_DATE_INPUT, fromDate);
        type(TO_DATE_INPUT, toDate);
        click(GENERATE_BUTTON);
        return this;
    }

    public int getReportRowCount() {
        return waitAllVisible(RECORDS_TABLE_ROWS).size();
    }
}
