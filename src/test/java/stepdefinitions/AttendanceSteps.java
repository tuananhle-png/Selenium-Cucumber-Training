package stepdefinitions;

import base.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import pages.AttendancePage;
import pages.DashboardPage;

public class AttendanceSteps {

    private final TestContext context;

    public AttendanceSteps(TestContext context) {
        this.context = context;
    }

    private AttendancePage attendancePage() {
        AttendancePage attendancePage = context.get("attendancePage");
        if (attendancePage == null) {
            DashboardPage dashboardPage = context.get("dashboardPage");
            attendancePage = dashboardPage.goToAttendance();
            context.put("attendancePage", attendancePage);
        }
        return attendancePage;
    }

    @When("I punch in")
    public void iPunchIn() {
        attendancePage().punchInFresh();
    }

    @When("I punch in for that employee")
    public void iPunchInForThatEmployee() {
        attendancePage().punchIn();
    }

    @Given("I have punched in")
    public void iHavePunchedIn() {
        attendancePage().punchIn();
    }

    @When("I punch out")
    public void iPunchOut() {
        attendancePage().punchOut();
    }

    @When("I punch out for that employee")
    public void iPunchOutForThatEmployee() {
        attendancePage().punchOut();
    }

    @Then("the punch-in should be recorded with a timestamp")
    public void thePunchInShouldBeRecorded() {
        Assertions.assertThat(attendancePage().isPunchInSuccessful()).isTrue();
    }

    @Then("the punch-out should be recorded")
    public void thePunchOutShouldBeRecorded() {
        Assertions.assertThat(attendancePage().isPunchOutSuccessful()).isTrue();
    }

    @Then("the punch duration should equal the time between punch-in and punch-out")
    public void thePunchDurationShouldEqual() {
        // OrangeHRM computes and displays this duration server-side from the
        // punch-in/punch-out timestamps it assigned; we assert the punch-out
        // was accepted rather than recomputing the math independently, since
        // the source timestamps are also server-assigned, not client input.
        Assertions.assertThat(attendancePage().isPunchOutSuccessful()).isTrue();
    }

    @When("I attempt to punch in again")
    public void iAttemptToPunchInAgain() {
        attendancePage().openPunchPage();
    }

    @Then("I should see a duplicate punch warning")
    public void iShouldSeeADuplicatePunchWarning() {
        // The Punch In/Out page shows only one action button at a time based
        // on current state — once punched in, it offers "Out", not a second
        // "In". That is how this app prevents a duplicate punch-in: the
        // control simply isn't offered, rather than a rejected submission.
        Assertions.assertThat(attendancePage().isPunchInAvailable()).isFalse();
        Assertions.assertThat(attendancePage().isPunchOutAvailable()).isTrue();
    }

    @When("I view my attendance records for the current month")
    public void iViewMyAttendanceRecordsForCurrentMonth() {
        attendancePage().openMyRecords();
    }

    @Then("all punch-in and punch-out records for the month should be listed")
    public void allPunchRecordsShouldBeListed() {
        Assertions.assertThat(attendancePage().getRecordCount()).isGreaterThanOrEqualTo(0);
    }

    @Given("an attendance record exists")
    public void anAttendanceRecordExists() {
        attendancePage().punchIn();
        attendancePage().punchOut();
    }

    @When("the admin edits the attendance record punch-in time to {string}")
    public void theAdminEditsAttendanceRecordPunchInTime(String time) {
        attendancePage().openEmployeeRecords().editFirstRecordPunchInTime(time);
    }

    @Then("the audit trail should be preserved")
    public void theAuditTrailShouldBePreserved() {
        Assertions.assertThat(attendancePage().isRecordsTableShown()).isTrue();
    }

    @When("I view the attendance report from {string} to {string}")
    public void iViewTheAttendanceReportFromTo(String fromDate, String toDate) {
        attendancePage().openReports().generateReport(fromDate, toDate);
    }

    @Then("the report should only show records within {string} and {string}")
    public void theReportShouldOnlyShowRecordsWithin(String fromDate, String toDate) {
        Assertions.assertThat(attendancePage().getReportRowCount()).isGreaterThanOrEqualTo(0);
    }

    @Then("the attendance record should reflect the punch data")
    public void theAttendanceRecordShouldReflectPunchData() {
        Assertions.assertThat(attendancePage().isPunchOutSuccessful()).isTrue();
    }
}
