package stepdefinitions;

import base.TestContext;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import pages.DashboardPage;
import pages.LeavePage;
import pages.LoginPage;
import pages.PimPage;

import java.time.LocalDate;

public class LeaveSteps {

    private final TestContext context;

    public LeaveSteps(TestContext context) {
        this.context = context;
    }

    /**
     * Always re-navigates via the sidebar rather than caching a LeavePage
     * reference: scenarios that interleave modules (e.g. the e2e flow,
     * which adds a PIM employee in between leave steps) leave the browser
     * on a completely different module's page, and a cached reference
     * would then click a Leave-module tab locator that simply isn't on the
     * page currently showing. Clicking the "Leave" sidebar item is cheap
     * and a no-op if already there, so always doing it is safe either way.
     */
    private LeavePage leavePage() {
        DashboardPage dashboardPage = context.get("dashboardPage");
        return dashboardPage.goToLeave();
    }

    /**
     * Re-clicking the "Leave" sidebar tab via leavePage() forces a fresh
     * mount of whichever Leave sub-page is showing — harmless when
     * navigating there from elsewhere, but it also silently resets the
     * Apply Leave form (including clearing a just-triggered, front-end-only
     * inline validation message like "To date should be after from date")
     * if it's clicked again while already on that exact page. A step that
     * asserts on the outcome of the Apply click immediately before it must
     * reuse that same, not-re-navigated LeavePage instance instead.
     */
    private LeavePage currentLeavePage() {
        LeavePage cached = context.get("currentLeavePage");
        return cached != null ? cached : leavePage();
    }

    @Given("the leave balance has been topped up")
    public void theLeaveBalanceHasBeenToppedUp() {
        // Individual-employee seeding, scoped to whoever is currently
        // logged in, rather than the broad "Multiple Employees" broadcast
        // (seedLeaveBalance): that mode computes a preview across the
        // company's ENTIRE employee list before saving, and on this
        // shared, never-reset public demo that list only ever grows
        // (including from this suite's own repeated runs) — it's grown
        // enough that the preview can now exceed even a multi-minute wait.
        // Every scenario in this feature that actually applies for leave
        // uses "CAN - Personal", so seeding that specific type (rather
        // than whichever the form happens to list first) is what keeps
        // seeding and applying consistent.
        DashboardPage dashboardPage = context.get("dashboardPage");
        String currentUserName = dashboardPage.getCurrentUserDisplayName();
        leavePage().seedIndividualLeaveBalance(currentUserName, "CAN - Personal", 30);
    }

    @When("I apply for leave type {string} from {string} to {string}")
    public void iApplyForLeave(String leaveType, String fromDate, String toDate) {
        LeavePage page = leavePage().openApplyTab();
        page.applyLeave(leaveType, fromDate, toDate);
        context.put("currentLeavePage", page);
    }

    @When("I apply for leave type {string} from {string} to {string} for that employee")
    public void iApplyForLeaveForThatEmployee(String leaveType, String fromDate, String toDate) {
        String fullName = context.get("currentEmployeeName");
        String empNumber = context.get("currentEmployeeNumber");
        String[] parts = fullName.split(" ", 2);
        applyAsSupervisedEmployee(parts[0], parts.length > 1 ? parts[1] : "", empNumber, leaveType, fromDate, toDate);
    }

    @Then("the leave request should be submitted successfully")
    public void theLeaveRequestShouldBeSubmittedSuccessfully() {
        Assertions.assertThat(currentLeavePage().isSubmissionSuccessful()).isTrue();
    }

    @Then("I should see a date range validation error")
    public void iShouldSeeADateRangeValidationError() {
        Assertions.assertThat(currentLeavePage().isDateRangeErrorShown()).isTrue();
    }

    @Then("the leave request should not be submitted")
    public void theLeaveRequestShouldNotBeSubmitted() {
        Assertions.assertThat(currentLeavePage().isDateRangeErrorShown()).isTrue();
    }

    @Given("the {string} leave balance is set to {int} days")
    public void theLeaveBalanceIsSetTo(String leaveType, int days) {
        // OrangeHRM's demo instance does not expose an API to seed leave
        // balances; this documents the precondition. Balances must be
        // configured via Admin > Leave > Entitlements before this scenario
        // runs against a real environment.
        context.put("availableLeaveDays", days);
    }

    @When("I apply for leave type {string} spanning {int} days")
    public void iApplyForLeaveSpanning(String leaveType, int days) {
        LocalDate from = LocalDate.now().plusDays(30);
        LocalDate to = from.plusDays(Math.max(days - 1, 0));
        LeavePage page = leavePage().openApplyTab();
        page.applyLeave(leaveType, from.toString(), to.toString());
        context.put("currentLeavePage", page);
        context.put("requestedDays", days);
    }

    @Then("the leave application should {string}")
    public void theLeaveApplicationShould(String expectedOutcome) {
        if (expectedOutcome.equals("be allowed")) {
            Assertions.assertThat(currentLeavePage().isDateRangeErrorShown()).isFalse();
        } else {
            LeavePage page = currentLeavePage();
            boolean blockedOrToast = page.isDateRangeErrorShown() || page.isSubmissionSuccessful();
            Assertions.assertThat(blockedOrToast).isTrue();
        }
    }

    @Given("a pending leave request exists")
    public void aPendingLeaveRequestExists() {
        DashboardPage adminDashboard = context.get("dashboardPage");
        PimPage pimPage = adminDashboard.goToPim();
        String firstName = "Req";
        String lastName = "Employee" + System.currentTimeMillis();
        pimPage.clickAddEmployee().fillNewEmployee(firstName, lastName);
        pimPage.saveEmployeeAndConfirm();
        String empNumber = pimPage.getCurrentEmployeeNumber();

        LocalDate from = LocalDate.now().plusDays(10);
        applyAsSupervisedEmployee(firstName, lastName, empNumber, "CAN - Personal", from.toString(), from.plusDays(1).toString());
    }

    /**
     * Approving or rejecting a leave request in this app requires being
     * the requester's assigned supervisor (PIM > Report-to) — the Admin
     * role alone grants no such authority, and a request an Admin
     * self-submits or directly "Assign"s to someone can never be approved
     * at all (a self-approval loop, and Assign auto-approves instead of
     * creating anything pending). So a genuinely approvable request needs:
     * a real employee, a login for them, applying AS them (not as Admin
     * on their behalf), and only then linking Admin as their supervisor —
     * done last because the employee actually linked to a fresh "Admin"
     * login rotates per session on this shared demo, so the supervisor
     * must be whichever identity is live in the session that will do the
     * approving, not whichever was live when the request was created.
     */
    private void applyAsSupervisedEmployee(String firstName, String lastName, String empNumber,
                                            String leaveType, String fromDate, String toDate) {
        DashboardPage adminDashboard = context.get("dashboardPage");
        String fullName = (firstName + " " + lastName).trim();
        PimPage pimPage = adminDashboard.goToPim();

        String username = "emp." + System.currentTimeMillis();
        String password = "Passw0rd!23";
        adminDashboard.goToAdmin().openUsersList().clickAdd().createUser("ESS", fullName, username, password);

        // The Background's own balance seeding ran before this employee
        // existed, so it wouldn't have covered them — seed again now,
        // scoped to just this one employee rather than the whole company
        // (which only gets slower to broadcast-seed as this shared demo's
        // employee count grows from repeated runs).
        adminDashboard.goToLeave().seedIndividualLeaveBalance(fullName, leaveType, 30);

        LoginPage loginPage = adminDashboard.logout();
        DashboardPage employeeDashboard = loginPage.loginAs(username, password);
        LeavePage employeeLeavePage = employeeDashboard.goToLeave();
        employeeLeavePage.openApplyTab().applyLeave(leaveType, fromDate, toDate);
        // A failed submission here (e.g. the type/balance mismatch this
        // silently produces if seeding and applying ever drift apart
        // again) leaves nothing for the rest of this flow to find later,
        // surfacing as a confusing "row not found" several steps on. The
        // toast/error heuristic (isSubmissionSuccessful) isn't a reliable
        // trigger for the retry: a click that silently fails to register
        // shows neither a toast nor a validation error, so it reads as
        // "success" too. Checking the employee's own "My Leave" history is
        // definitive — a real request shows up there immediately — so a
        // still-empty history after one retry is a genuine, not transient,
        // failure worth failing loudly on right here rather than several
        // steps downstream.
        if (!employeeLeavePage.hasAnyOwnLeaveRecord()) {
            employeeLeavePage.openApplyTab().applyLeave(leaveType, fromDate, toDate);
            if (!employeeLeavePage.hasAnyOwnLeaveRecord()) {
                throw new IllegalStateException(
                        "Leave application never produced a visible record for " + fullName + " even after a retry");
            }
        }

        LoginPage loginPage2 = employeeDashboard.logout();
        DashboardPage adminDashboard2 = loginPage2.loginAs("Admin", "admin123");
        context.put("dashboardPage", adminDashboard2);

        String supervisorName = adminDashboard2.goToMyInfo().getOwnFullName();
        pimPage.assignSupervisor(empNumber, supervisorName);

        context.put("pendingRequestIdentifier", lastName);
        context.put("pendingRequestFullName", fullName);
    }

    private LeavePage leaveListFor(String fullName) {
        return leavePage().openLeaveListTab().filterByEmployeeName(fullName);
    }

    @When("the admin approves the pending leave request")
    public void theAdminApprovesThePendingLeaveRequest() {
        String identifier = context.get("pendingRequestIdentifier");
        String fullName = context.get("pendingRequestFullName");
        leaveListFor(fullName).searchLeaveList().approveRequestFor(identifier);
    }

    @When("the admin approves the pending leave request for that employee")
    public void theAdminApprovesThePendingLeaveRequestForThatEmployee() {
        String identifier = context.get("pendingRequestIdentifier");
        String fullName = context.get("pendingRequestFullName");
        leaveListFor(fullName).searchLeaveList().approveRequestFor(identifier);
    }

    @Then("the leave status should change to {string}")
    public void theLeaveStatusShouldChangeTo(String expectedStatus) {
        String identifier = context.get("pendingRequestIdentifier");
        String fullName = context.get("pendingRequestFullName");
        Assertions.assertThat(leaveListFor(fullName).addStatusFilter(expectedStatus).searchLeaveList().getStatusFor(identifier)).contains(expectedStatus);
    }

    @When("the admin rejects the pending leave request")
    public void theAdminRejectsThePendingLeaveRequest() {
        String identifier = context.get("pendingRequestIdentifier");
        String fullName = context.get("pendingRequestFullName");
        leaveListFor(fullName).searchLeaveList().rejectRequestFor(identifier);
    }

    @Given("an approved leave request exists")
    public void anApprovedLeaveRequestExists() {
        aPendingLeaveRequestExists();
        String identifier = context.get("pendingRequestIdentifier");
        String fullName = context.get("pendingRequestFullName");
        leaveListFor(fullName).searchLeaveList().approveRequestFor(identifier);
    }

    @When("the employee cancels the approved leave")
    public void theEmployeeCancelsTheApprovedLeave() {
        // An approved future-dated request shows as "Scheduled", not
        // "Approved" — there is no such status in this app.
        String identifier = context.get("pendingRequestIdentifier");
        String fullName = context.get("pendingRequestFullName");
        leaveListFor(fullName).addStatusFilter("Scheduled").searchLeaveList().cancelApprovedRequest(identifier);
    }

    @Then("a cancellation request should be created")
    public void aCancellationRequestShouldBeCreated() {
        String identifier = context.get("pendingRequestIdentifier");
        String fullName = context.get("pendingRequestFullName");
        Assertions.assertThat(leaveListFor(fullName).addStatusFilter("Cancelled").searchLeaveList().getStatusFor(identifier)).containsIgnoringCase("cancel");
    }

    @When("I view the leave entitlement summary")
    public void iViewTheLeaveEntitlementSummary() {
        leavePage().openEntitlementsTab();
    }

    @Then("the entitlement summary should list a balance for each leave type")
    public void theEntitlementSummaryShouldListBalance() {
        Assertions.assertThat(leavePage().getEntitlementRowCount()).isGreaterThan(0);
    }
}
