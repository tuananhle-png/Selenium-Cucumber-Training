package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DashboardPage extends BasePage {

    private static final By HEADER_TITLE = By.cssSelector(".oxd-topbar-header-breadcrumb h6");
    private static final By USER_DROPDOWN = By.cssSelector(".oxd-userdropdown-tab");
    private static final By USER_DROPDOWN_NAME = By.cssSelector(".oxd-userdropdown-name");
    private static final By LOGOUT_LINK = By.linkText("Logout");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return isVisible(HEADER_TITLE);
    }

    public String getHeaderTitle() {
        return getText(HEADER_TITLE);
    }

    /**
     * The employee linked to the "Admin" login isn't stable across
     * sessions on this shared public demo (observed as "Demo Source",
     * "Rahul Kumar", "John Alex" on different runs) — always read the
     * currently displayed name rather than assuming a fixed one, e.g. when
     * seeding leave entitlements for "whichever employee is logged in".
     */
    public String getCurrentUserDisplayName() {
        return getText(USER_DROPDOWN_NAME);
    }

    public LoginPage logout() {
        click(USER_DROPDOWN);
        click(LOGOUT_LINK);
        return new LoginPage(driver);
    }

    private void openModule(String moduleName) {
        By menuItem = By.xpath("//span[contains(@class,'oxd-main-menu-item--name') and normalize-space()='" + moduleName + "']");
        click(menuItem);
    }

    public PimPage goToPim() {
        openModule("PIM");
        return new PimPage(driver);
    }

    /**
     * Navigates directly by URL rather than clicking the sidebar's "Leave"
     * item like the other modules do: on this shared public demo, that
     * item has been observed to simply not render in the sidebar at all
     * (confirmed independently of role/permissions — the same "Admin"
     * account's User Management entry still shows the full Admin role, and
     * the route itself loads correctly when reached directly) for
     * unknown reasons outside this suite's control, most likely
     * interference from another concurrent user of the same
     * never-reset, unauthenticated instance. Reaching the module directly
     * sidesteps depending on that sidebar item existing at all.
     */
    public LeavePage goToLeave() {
        driver.get(config.ConfigReader.get("base.url") + "/web/index.php/leave/applyLeave");
        return new LeavePage(driver);
    }

    public AttendancePage goToAttendance() {
        openModule("Time");
        return new AttendancePage(driver);
    }

    public RecruitmentPage goToRecruitment() {
        openModule("Recruitment");
        return new RecruitmentPage(driver);
    }

    public AdminPage goToAdmin() {
        openModule("Admin");
        return new AdminPage(driver);
    }

    public PimPage goToMyInfo() {
        openModule("My Info");
        return new PimPage(driver);
    }
}
