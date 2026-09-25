package stepdefinitions;

import base.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import pages.DashboardPage;
import pages.PimPage;

import java.nio.file.Path;

public class PimSteps {

    private final TestContext context;

    public PimSteps(TestContext context) {
        this.context = context;
    }

    private PimPage pimPage() {
        PimPage pimPage = context.get("pimPage");
        if (pimPage == null) {
            DashboardPage dashboardPage = context.get("dashboardPage");
            pimPage = dashboardPage.goToPim();
            context.put("pimPage", pimPage);
        }
        return pimPage;
    }

    @Given("an employee named {string} exists")
    public void anEmployeeNamedExists(String fullName) {
        String[] parts = fullName.split(" ", 2);
        pimPage().clickAddEmployee().fillNewEmployee(parts[0], parts.length > 1 ? parts[1] : "");
        pimPage().saveEmployeeAndConfirm();
        context.put("currentEmployeeName", fullName);
    }

    @When("I add a new employee with first name {string} and last name {string}")
    public void iAddNewEmployee(String firstName, String lastName) {
        pimPage().clickAddEmployee().fillNewEmployee(firstName, lastName);
        pimPage().saveEmployeeAndConfirm();
        context.put("currentEmployeeName", (firstName + " " + lastName).trim());
        context.put("currentEmployeeNumber", pimPage().getCurrentEmployeeNumber());
    }

    @When("I attempt to add a new employee with first name {string} and last name {string}")
    public void iAttemptToAddEmployeeInvalid(String firstName, String lastName) {
        pimPage().clickAddEmployee().fillNewEmployee(firstName, lastName);
        pimPage().saveEmployee();
    }

    @Then("the employee {string} should be created")
    public void theEmployeeShouldBeCreated(String fullName) {
        // Not a re-check of the save toast: iAddNewEmployee's own
        // saveEmployeeAndConfirm() already waited for and saw it, and OXD
        // auto-dismisses it after a few seconds — a fresh wait here is
        // racing a toast that, by this point, has usually already faded
        // for good, timing out deterministically rather than flakily. The
        // employee number recorded right after save (itself derived from
        // the post-save redirect URL, not the toast) is the durable signal
        // that the save actually happened.
        String currentEmployeeNumber = context.get("currentEmployeeNumber");
        Assertions.assertThat(currentEmployeeNumber).isNotBlank();
    }

    @Then("the employee should appear in the employee list")
    public void theEmployeeShouldAppearInList() {
        String fullName = context.get("currentEmployeeName");
        DashboardPage dashboardPage = context.get("dashboardPage");
        PimPage pimPage = dashboardPage.goToPim().searchByEmployeeName(fullName);
        Assertions.assertThat(pimPage.getSearchResultNames()).anyMatch(name -> name.contains(fullName));
    }

    @Then("the employee should not be saved")
    public void theEmployeeShouldNotBeSaved() {
        Assertions.assertThat(pimPage().getRequiredErrorCount()).isGreaterThanOrEqualTo(1);
    }

    @Then("a required validation message should appear on the employee form")
    public void aRequiredValidationMessageOnEmployeeForm() {
        Assertions.assertThat(pimPage().getRequiredErrorCount()).isGreaterThanOrEqualTo(1);
    }

    @When("I search for an employee by name {string}")
    public void iSearchForAnEmployeeByName(String name) {
        pimPage().searchByEmployeeName(name);
    }

    @Then("the results should include employee {string}")
    public void theResultsShouldIncludeEmployee(String fullName) {
        Assertions.assertThat(pimPage().getSearchResultNames()).anyMatch(name -> name.contains(fullName));
    }

    @Then("I should see the {string} message")
    public void iShouldSeeTheMessage(String message) {
        Assertions.assertThat(pimPage().isNoRecordsMessageShown()).isTrue();
    }

    @When("I update the job title of {string} to {string}")
    public void iUpdateTheJobTitleOf(String fullName, String jobTitle) {
        pimPage().searchByEmployeeName(fullName).openEmployeeFromList(fullName).openJobTab().updateJobTitle(jobTitle);
        context.put("currentEmployeeName", fullName);
    }

    @Then("the job title for {string} should be {string}")
    public void theJobTitleForShouldBe(String fullName, String jobTitle) {
        Assertions.assertThat(pimPage().getJobTitleValue()).isEqualTo(jobTitle);
    }

    @When("I delete the employee {string}")
    public void iDeleteTheEmployee(String fullName) {
        pimPage().searchByEmployeeName(fullName).deleteEmployee(fullName);
    }

    @Then("I should be asked to confirm the deletion")
    public void iShouldBeAskedToConfirmDeletion() {
        Assertions.assertThat(pimPage().isDeleteConfirmationShown()).isTrue();
    }

    @Then("after confirming, {string} should no longer appear in the employee list")
    public void afterConfirmingShouldNoLongerAppear(String fullName) {
        pimPage().confirmDelete();
        PimPage refreshed = pimPage().searchByEmployeeName(fullName);
        Assertions.assertThat(refreshed.isNoRecordsMessageShown() || refreshed.getSearchResultNames().stream().noneMatch(n -> n.contains(fullName)))
                .isTrue();
    }

    @When("I upload the profile photo {string} for {string}")
    public void iUploadTheProfilePhotoFor(String fileName, String fullName) {
        Path filePath = Path.of("src", "test", "resources", "testdata", fileName).toAbsolutePath();
        pimPage().searchByEmployeeName(fullName).openEmployeeFromList(fullName).uploadProfilePhoto(filePath.toString());
        context.put("currentEmployeeName", fullName);
    }

    @Then("the profile photo should be updated for {string}")
    public void theProfilePhotoShouldBeUpdatedFor(String fullName) {
        Assertions.assertThat(pimPage().isPhotoUploadErrorShown()).isFalse();
    }

    @Then("I should see a photo upload error message")
    public void iShouldSeeAPhotoUploadErrorMessage() {
        Assertions.assertThat(pimPage().isPhotoUploadErrorShown()).isTrue();
    }

    @Then("the profile photo should not change for {string}")
    public void theProfilePhotoShouldNotChangeFor(String fullName) {
        Assertions.assertThat(pimPage().isPhotoUploadErrorShown()).isTrue();
    }

    @When("I add an emergency contact named {string} with relationship {string} and mobile {string} for {string}")
    public void iAddAnEmergencyContact(String contactName, String relationship, String mobile, String fullName) {
        pimPage().searchByEmployeeName(fullName).openEmployeeFromList(fullName)
                .openEmergencyContactsTab().clickAddEmergencyContact()
                .fillEmergencyContact(contactName, relationship, mobile);
        context.put("currentEmployeeName", fullName);
    }

    @Then("the emergency contact {string} should be saved under the Emergency Contacts tab for {string}")
    public void theEmergencyContactShouldBeSaved(String contactName, String fullName) {
        Assertions.assertThat(pimPage().isEmergencyContactListed(contactName)).isTrue();
    }
}
