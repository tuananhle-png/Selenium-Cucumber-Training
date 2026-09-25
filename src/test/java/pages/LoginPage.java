package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private static final By USERNAME_INPUT = By.name("username");
    private static final By PASSWORD_INPUT = By.name("password");
    private static final By LOGIN_BUTTON = By.cssSelector("button[type='submit']");
    private static final By ERROR_ALERT = By.cssSelector(".oxd-alert-content-text");
    private static final By REQUIRED_FIELD_ERRORS = By.cssSelector(".oxd-input-group .oxd-input-field-error-message");
    private static final By FORGOT_PASSWORD_LINK = By.cssSelector(".orangehrm-login-forgot-header");
    private static final By RESET_USERNAME_INPUT = By.cssSelector("input[placeholder='Username']");
    private static final By RESET_SUBMIT_BUTTON = By.cssSelector("button[type='submit']");
    private static final By RESET_CONFIRMATION_TITLE = By.cssSelector(".orangehrm-forgot-password-title");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open(String baseUrl) {
        driver.get(baseUrl + "/web/index.php/auth/login");
        return this;
    }

    public DashboardPage loginAs(String username, String password) {
        submitLogin(username, password);
        return new DashboardPage(driver);
    }

    public void submitLogin(String username, String password) {
        type(USERNAME_INPUT, username);
        type(PASSWORD_INPUT, password);
        click(LOGIN_BUTTON);
    }

    public boolean isOnLoginPage() {
        return isVisible(USERNAME_INPUT);
    }

    public String getErrorMessage() {
        return getText(ERROR_ALERT);
    }

    public int getRequiredFieldErrorCount() {
        return waitAllVisible(REQUIRED_FIELD_ERRORS).size();
    }

    public LoginPage clickForgotPassword() {
        click(FORGOT_PASSWORD_LINK);
        return this;
    }

    public void submitForgotPassword(String username) {
        type(RESET_USERNAME_INPUT, username);
        click(RESET_SUBMIT_BUTTON);
    }

    public String getResetConfirmationTitle() {
        return getText(RESET_CONFIRMATION_TITLE);
    }
}
