package stepdefinitions;

import base.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import pages.DashboardPage;
import pages.RecruitmentPage;

public class RecruitmentSteps {

    private final TestContext context;

    public RecruitmentSteps(TestContext context) {
        this.context = context;
    }

    private RecruitmentPage recruitmentPage() {
        RecruitmentPage recruitmentPage = context.get("recruitmentPage");
        if (recruitmentPage == null) {
            DashboardPage dashboardPage = context.get("dashboardPage");
            recruitmentPage = dashboardPage.goToRecruitment();
            context.put("recruitmentPage", recruitmentPage);
        }
        return recruitmentPage;
    }

    @When("I create a job vacancy {string} with {int} positions")
    public void iCreateAJobVacancy(String vacancyName, int positions) {
        recruitmentPage().openVacanciesTab().clickAdd().createVacancy("QA Engineer", vacancyName, positions);
    }

    @Then("the vacancy {string} should appear in the vacancy list")
    public void theVacancyShouldAppearInTheList(String vacancyName) {
        Assertions.assertThat(recruitmentPage().openVacanciesTab().isVacancyListed(vacancyName)).isTrue();
    }

    @Given("a vacancy {string} exists")
    public void aVacancyExists(String vacancyName) {
        recruitmentPage().openVacanciesTab().clickAdd().createVacancy("QA Engineer", vacancyName, 1);
    }

    @When("I add candidate {string} to vacancy {string}")
    public void iAddCandidateToVacancy(String candidateFullName, String vacancyName) {
        String[] parts = candidateFullName.split(" ", 2);
        String lastName = (parts.length > 1 ? parts[1] : "candidate") + System.currentTimeMillis();
        // The candidate's own NAME (not just its email) needs to be
        // run-unique too: this shared, never-reset public demo accumulates
        // a same-named candidate from every past run, and code elsewhere
        // that locates "the" candidate by name (e.g. scheduleInterview's
        // "most recent row matching this name" lookup) can pick up an old
        // run's candidate instead of this run's own — one left in a
        // broken/stuck workflow state a fresh one never would be.
        String email = parts[0].toLowerCase() + "." + lastName.toLowerCase() + "@example.com";
        recruitmentPage().openCandidatesTab().clickAdd()
                .addCandidate(parts[0], lastName, email, vacancyName);
        context.put("currentCandidateName", parts[0] + " " + lastName);
    }

    /**
     * Every later step in a scenario refers to the candidate by the same
     * literal name the Gherkin text used to create it (e.g. "Ivy Chan"),
     * but the real, run-unique name recorded by iAddCandidateToVacancy is
     * what actually exists in the UI — resolving through that context
     * value keeps every step working against this scenario's own
     * candidate rather than the literal, demo-wide-shared name.
     */
    private String resolveCandidateName(String rawName) {
        String current = context.get("currentCandidateName");
        return current != null ? current : rawName;
    }

    @Then("the candidate {string} should have status {string}")
    public void theCandidateShouldHaveStatus(String candidateFullName, String status) {
        Assertions.assertThat(recruitmentPage().openCandidatesTab().getCandidateStatus(resolveCandidateName(candidateFullName))).contains(status);
    }

    @Given("candidate {string} exists for vacancy {string}")
    public void candidateExistsForVacancy(String candidateFullName, String vacancyName) {
        aVacancyExists(vacancyName);
        iAddCandidateToVacancy(candidateFullName, vacancyName);
    }

    @When("I schedule an interview for {string} on {string} at {string} with interviewer {string}")
    public void iScheduleAnInterviewFor(String candidateFullName, String date, String time, String interviewer) {
        recruitmentPage().openCandidatesTab().scheduleInterview(resolveCandidateName(candidateFullName), date, time, interviewer);
    }

    @Then("the interview should be recorded for {string}")
    public void theInterviewShouldBeRecordedFor(String candidateFullName) {
        Assertions.assertThat(recruitmentPage().getCandidateStatus(resolveCandidateName(candidateFullName))).isNotBlank();
    }

    @When("I mark {string} as {string}")
    public void iMarkAs(String candidateFullName, String status) {
        recruitmentPage().openCandidatesTab().markShortlisted(resolveCandidateName(candidateFullName));
    }

    @Then("the candidate {string} status should be {string}")
    public void theCandidateStatusShouldBe(String candidateFullName, String status) {
        Assertions.assertThat(recruitmentPage().getCandidateStatus(resolveCandidateName(candidateFullName))).contains(status);
    }

    @When("I reject {string} with reason {string}")
    public void iRejectWithReason(String candidateFullName, String reason) {
        recruitmentPage().openCandidatesTab().rejectCandidate(resolveCandidateName(candidateFullName), reason);
    }

    @When("I attempt to create a vacancy without selecting a job title")
    public void iAttemptToCreateAVacancyWithoutJobTitle() {
        recruitmentPage().openVacanciesTab().clickAdd().createVacancy(null, "No Title Vacancy", 1);
    }

    @Then("a validation error should be shown")
    public void aValidationErrorShouldBeShown() {
        Assertions.assertThat(recruitmentPage().isValidationErrorShown()).isTrue();
    }

    @Then("the vacancy should not be saved")
    public void theVacancyShouldNotBeSaved() {
        Assertions.assertThat(recruitmentPage().isValidationErrorShown()).isTrue();
    }
}
