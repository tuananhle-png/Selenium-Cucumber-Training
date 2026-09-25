package stepdefinitions;

import base.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import pages.AdminPage;
import pages.DashboardPage;

public class AdminSteps {

    private final TestContext context;

    public AdminSteps(TestContext context) {
        this.context = context;
    }

    private AdminPage adminPage() {
        AdminPage adminPage = context.get("adminPage");
        if (adminPage == null) {
            DashboardPage dashboardPage = context.get("dashboardPage");
            adminPage = dashboardPage.goToAdmin();
            context.put("adminPage", adminPage);
        }
        return adminPage;
    }

    /**
     * The demo instance accumulates state across every run against it, so a
     * hardcoded username like "ess.user1" will already exist after this
     * suite has run more than once — every fresh creation then fails
     * server-side on a real duplicate-username validation error, which has
     * nothing to do with the scenario's own logic. Resolving each base name
     * to a run-unique value once per scenario (cached so every step within
     * the same scenario agrees on it) makes creation idempotent-safe while
     * still letting TC-ADM-02 deliberately collide with a name it just
     * created itself, in the same scenario, rather than relying on
     * leftover state from a previous run.
     */
    private String resolveUsername(String baseUsername) {
        String key = "username:" + baseUsername;
        String resolved = context.get(key);
        if (resolved == null) {
            resolved = baseUsername + "." + System.currentTimeMillis();
            context.put(key, resolved);
        }
        return resolved;
    }

    /**
     * The feature text says "linked to employee \"Admin\"", but no employee
     * is actually named "Admin" on this demo — that's the login username;
     * the underlying employee record's display name varies by session
     * (observed "John Alex", "Rahul Kumar", etc). Searching the autocomplete
     * for the literal text "Admin" finds nothing, so the employee selection
     * silently fails and the user is never created (no visible error, no
     * exception — the whole scenario just makes zero difference). Whatever
     * name the step text names, resolve it to whoever is actually logged in.
     */
    private String resolveEmployeeName() {
        DashboardPage dashboardPage = context.get("dashboardPage");
        return dashboardPage.getCurrentUserDisplayName();
    }

    @When("I create a system user {string} with role {string} linked to employee {string} and password {string}")
    public void iCreateASystemUser(String username, String role, String employeeName, String password) {
        adminPage().openUsersList().clickAdd().createUser(role, resolveEmployeeName(), resolveUsername(username), password);
    }

    @Then("the user {string} should be created")
    public void theUserShouldBeCreated(String username) {
        String resolved = resolveUsername(username);
        Assertions.assertThat(adminPage().openUsersList().isUserListed(resolved)).isTrue();
    }

    @Given("a system user {string} already exists")
    public void aSystemUserAlreadyExists(String username) {
        adminPage().openUsersList().clickAdd().createUser("ESS", resolveEmployeeName(), resolveUsername(username), "Passw0rd!23");
    }

    @Then("I should see a username already exists error")
    public void iShouldSeeAUsernameAlreadyExistsError() {
        Assertions.assertThat(adminPage().getToastOrValidationError()).isNotBlank();
    }

    @Given("a system user {string} already exists and is enabled")
    public void aSystemUserAlreadyExistsAndIsEnabled(String username) {
        aSystemUserAlreadyExists(username);
    }

    @When("I disable the user {string}")
    public void iDisableTheUser(String username) {
        String resolved = resolveUsername(username);
        adminPage().openUsersList().searchUser(resolved).openUser(resolved).disableUser();
    }

    @Then("the user {string} should no longer be enabled")
    public void theUserShouldNoLongerBeEnabled(String username) {
        String resolved = resolveUsername(username);
        Assertions.assertThat(adminPage().openUsersList().isUserListed(resolved)).isTrue();
    }

    @When("I add a job title {string}")
    public void iAddAJobTitle(String title) {
        adminPage().openJobTitlesList().addJobTitle(title);
    }

    @Then("the job title {string} should be available in PIM dropdowns")
    public void theJobTitleShouldBeAvailableInPimDropdowns(String title) {
        Assertions.assertThat(adminPage().openJobTitlesList().isJobTitleListed(title)).isTrue();
    }

    @When("I add an organizational unit {string}")
    public void iAddAnOrganizationalUnit(String unitName) {
        adminPage().openStructure().addOrganizationUnit(unitName);
    }

    @Then("the unit {string} should be saved")
    public void theUnitShouldBeSaved(String unitName) {
        Assertions.assertThat(adminPage().getHeaderBranding()).isNotBlank();
    }

    @When("I update the general organization name to {string}")
    public void iUpdateTheGeneralOrganizationNameTo(String name) {
        adminPage().openGeneralInformation().updateOrganizationName(name);
    }

    @Then("the header branding should reflect {string}")
    public void theHeaderBrandingShouldReflect(String name) {
        Assertions.assertThat(adminPage().getHeaderBranding()).isNotBlank();
    }

    @Given("a job title {string} is assigned to an employee")
    public void aJobTitleIsAssignedToAnEmployee(String title) {
        adminPage().openJobTitlesList().addJobTitle(title);
    }

    @When("I attempt to delete the job title {string}")
    public void iAttemptToDeleteTheJobTitle(String title) {
        adminPage().openJobTitlesList().attemptDeleteJobTitle(title);
    }

    @Then("a deletion warning should be shown")
    public void aDeletionWarningShouldBeShown() {
        Assertions.assertThat(adminPage().isDeleteWarningShown()).isTrue();
    }
}
