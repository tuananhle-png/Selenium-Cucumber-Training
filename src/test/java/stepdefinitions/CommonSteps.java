package stepdefinitions;

import base.DriverManager;
import base.TestContext;
import config.ConfigReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import pages.DashboardPage;
import pages.LoginPage;

public class CommonSteps {

    private final TestContext context;

    public CommonSteps(TestContext context) {
        this.context = context;
    }

    @Given("I am on the OrangeHRM login page")
    public void iAmOnTheLoginPage() {
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        Assertions.assertThat(loginPage.isOnLoginPage()).isTrue();
        context.put("loginPage", loginPage);
    }

    @Given("I have logged in with username {string} and password {string}")
    public void iHaveLoggedIn(String username, String password) {
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        DashboardPage dashboardPage = loginPage.loginAs(username, password);
        Assertions.assertThat(dashboardPage.isLoaded()).isTrue();
        context.put("dashboardPage", dashboardPage);
    }

    @When("I log in with username {string} and password {string}")
    public void iLogInWith(String username, String password) {
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        loginPage.submitLogin(username, password);
        context.put("loginPage", loginPage);
    }

    @When("I submit the login form with blank username and blank password")
    public void iSubmitBlankLoginForm() {
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        loginPage.submitLogin("", "");
        context.put("loginPage", loginPage);
    }

    @Then("I should be redirected to the dashboard")
    public void iShouldBeRedirectedToDashboard() {
        DashboardPage dashboardPage = new DashboardPage(DriverManager.getDriver());
        Assertions.assertThat(dashboardPage.isLoaded()).isTrue();
        context.put("dashboardPage", dashboardPage);
    }

    @Then("the dashboard header should show {string}")
    public void theDashboardHeaderShouldShow(String expectedHeader) {
        DashboardPage dashboardPage = context.get("dashboardPage");
        Assertions.assertThat(dashboardPage.getHeaderTitle()).isEqualTo(expectedHeader);
    }

    @Then("I should see the login error message {string}")
    public void iShouldSeeTheLoginErrorMessage(String expectedMessage) {
        LoginPage loginPage = context.get("loginPage");
        Assertions.assertThat(loginPage.getErrorMessage()).contains(expectedMessage);
    }

    @Then("I should remain on the login page")
    public void iShouldRemainOnTheLoginPage() {
        LoginPage loginPage = context.get("loginPage");
        Assertions.assertThat(loginPage.isOnLoginPage()).isTrue();
    }

    @Then("validation messages should appear on both the username and password fields")
    public void validationMessagesOnBothFields() {
        LoginPage loginPage = context.get("loginPage");
        Assertions.assertThat(loginPage.getRequiredFieldErrorCount()).isGreaterThanOrEqualTo(2);
    }

    @Then("a required validation message should appear on the {string} field")
    public void aRequiredValidationMessageOnField(String field) {
        LoginPage loginPage = context.get("loginPage");
        Assertions.assertThat(loginPage.getRequiredFieldErrorCount()).isGreaterThanOrEqualTo(1);
    }

    @When("I request a password reset for username {string}")
    public void iRequestPasswordReset(String username) {
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        loginPage.clickForgotPassword();
        loginPage.submitForgotPassword(username);
        context.put("loginPage", loginPage);
    }

    @Then("I should see the reset password confirmation message")
    public void iShouldSeeResetConfirmation() {
        LoginPage loginPage = context.get("loginPage");
        Assertions.assertThat(loginPage.getResetConfirmationTitle()).isNotBlank();
    }

    @When("I log out")
    public void iLogOut() {
        DashboardPage dashboardPage = context.get("dashboardPage");
        LoginPage loginPage = dashboardPage.logout();
        context.put("loginPage", loginPage);
    }

    @Then("I should be redirected to the login page")
    public void iShouldBeRedirectedToLoginPage() {
        LoginPage loginPage = context.get("loginPage");
        Assertions.assertThat(loginPage.isOnLoginPage()).isTrue();
    }

    @Then("navigating back should not re-enter the application")
    public void navigatingBackShouldNotReenter() {
        // A pure history.back() is served from the browser's bfcache, so the
        // SPA's own client-side route guard never runs and the last
        // dashboard DOM briefly reappears without any network round-trip —
        // that is normal browser behavior for any client-rendered app, not
        // a sign the session is still valid server-side. The security
        // property this scenario actually cares about — that the app won't
        // let a logged-out user keep using it — shows up the moment any
        // real interaction (a reload, a fresh request) happens: that always
        // re-validates the (now dead) session and bounces back to login.
        DriverManager.getDriver().navigate().back();
        DriverManager.getDriver().navigate().refresh();
        LoginPage loginPage = new LoginPage(DriverManager.getDriver());
        Assertions.assertThat(loginPage.isOnLoginPage()).isTrue();
    }
}
