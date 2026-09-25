package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class RecruitmentPage extends BasePage {

    // Vacancies
    private static final By VACANCIES_TAB = By.xpath("//*[contains(@class,'oxd-topbar-body-nav-tab-item') and normalize-space()='Vacancies']");
    private static final By ADD_BUTTON = By.xpath("//button[normalize-space()='Add']");
    private static final By VACANCY_JOB_TITLE_DROPDOWN = By.xpath("//label[text()='Job Title']/../..//div[contains(@class,'oxd-select-text--active')]");
    private static final By VACANCY_NAME_INPUT = By.xpath("//label[text()='Vacancy Name']/../..//input");
    private static final By HIRING_MANAGER_INPUT = By.xpath("//label[text()='Hiring Manager']/../..//input");
    private static final By AUTOCOMPLETE_OPTIONS = By.xpath(
            "//div[contains(@class,'oxd-autocomplete-dropdown')]//div[contains(@class,'oxd-autocomplete-option')]");
    private static final By NO_OF_POSITIONS_INPUT = By.xpath("//label[text()='Number of Positions']/../..//input");
    private static final By SAVE_BUTTON = By.xpath("//button[normalize-space()='Save']");
    private static final By REQUIRED_ERROR = By.cssSelector(".oxd-input-field-error-message, .oxd-select-text-error-message");
    private static final By VACANCY_ROWS = By.cssSelector(".oxd-table-body .oxd-table-card");
    private static final By TOAST_MESSAGE = By.cssSelector(".oxd-text--toast-message");

    // Candidates
    private static final By CANDIDATES_TAB = By.xpath("//*[contains(@class,'oxd-topbar-body-nav-tab-item') and normalize-space()='Candidates']");
    private static final By FIRST_NAME_INPUT = By.name("firstName");
    private static final By LAST_NAME_INPUT = By.name("lastName");
    private static final By EMAIL_INPUT = By.xpath("//label[text()='Email']/../..//input");
    private static final By CANDIDATE_VACANCY_DROPDOWN = By.xpath("//label[text()='Vacancy']/../..//div[contains(@class,'oxd-select-text--active')]");
    private static final By CANDIDATE_STATUS_CELL = By.cssSelector(".oxd-table-cell");

    // Interview
    private static final By SCHEDULE_INTERVIEW_BUTTON = By.xpath("//button[contains(.,'Schedule Interview')]");
    private static final By INTERVIEW_NAME_INPUT = By.name("interviewName");
    private static final By INTERVIEW_DATE_INPUT = By.xpath("(//label[text()='Date']/../..//input)[1]");
    private static final By INTERVIEW_TIME_INPUT = By.xpath("(//label[text()='Time']/../..//input)[1]");
    private static final By INTERVIEWER_DROPDOWN = By.xpath("//label[text()='Interviewer(s)']/../..//div[contains(@class,'oxd-select-text--active')]");

    // Status change / reject
    private static final By ACTIONS_DROPDOWN = By.cssSelector(".oxd-table-cell-actions button");
    private static final By SHORTLIST_ACTION = By.xpath("//button[contains(.,'Shortlist')]");
    private static final By REJECT_ACTION = By.xpath("//button[contains(.,'Reject')]");
    private static final By REJECT_REASON_INPUT = By.cssSelector("textarea");
    private static final By CONFIRM_BUTTON = By.xpath("//button[normalize-space()='Save' or normalize-space()='Yes']");

    public RecruitmentPage(WebDriver driver) {
        super(driver);
    }

    public RecruitmentPage openVacanciesTab() {
        click(VACANCIES_TAB);
        return this;
    }

    public RecruitmentPage clickAdd() {
        click(ADD_BUTTON);
        return this;
    }

    public void createVacancy(String jobTitle, String vacancyName, int positions) {
        if (jobTitle != null && !jobTitle.isBlank()) {
            selectFromOxdDropdown(VACANCY_JOB_TITLE_DROPDOWN, jobTitle);
        }
        type(VACANCY_NAME_INPUT, vacancyName);
        // Hiring Manager is a required autocomplete field; the scenario has
        // no opinion on who it is, so a single common letter reliably
        // surfaces at least one real employee to pick regardless of which
        // identities happen to exist on this shared demo instance.
        selectFromOxdAutocomplete(HIRING_MANAGER_INPUT, AUTOCOMPLETE_OPTIONS, "a");
        type(NO_OF_POSITIONS_INPUT, String.valueOf(positions));
        click(SAVE_BUTTON);
        // The Save click occasionally doesn't register a submit on the
        // first attempt even though the button was confirmed clickable and
        // enabled (no error, no toast, page just stays on the add form) —
        // a headless-browser click-delivery flake rather than a validation
        // failure, which would show a toast/error instead. One retry,
        // confirmed by either navigating away from the add form or a toast
        // appearing, absorbs that without masking a real failure.
        if (!waitForSubmitConfirmation("/addJobVacancy")) {
            click(SAVE_BUTTON);
        }
    }

    private boolean waitForSubmitConfirmation(String addFormUrlFragment) {
        try {
            return waitUntil(d -> !d.getCurrentUrl().contains(addFormUrlFragment) || !d.findElements(TOAST_MESSAGE).isEmpty());
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    public boolean isValidationErrorShown() {
        return isVisible(REQUIRED_ERROR);
    }

    public boolean isVacancyListed(String vacancyName) {
        // Retries the whole row-scan rather than a single snapshot: the list
        // can still be re-fetching right after the tab navigation that
        // follows a save, so a one-shot check can race a stale/empty render.
        try {
            return waitUntil(d -> waitAllVisible(VACANCY_ROWS).stream().anyMatch(row -> row.getText().contains(vacancyName)));
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    public RecruitmentPage openCandidatesTab() {
        click(CANDIDATES_TAB);
        return this;
    }

    public void addCandidate(String firstName, String lastName, String email, String vacancyName) {
        type(FIRST_NAME_INPUT, firstName);
        type(LAST_NAME_INPUT, lastName);
        type(EMAIL_INPUT, email);
        selectFromOxdDropdown(CANDIDATE_VACANCY_DROPDOWN, vacancyName);
        click(SAVE_BUTTON);
        if (!waitForSubmitConfirmation("/addCandidate")) {
            click(SAVE_BUTTON);
        }
    }

    public String getCandidateStatus(String candidateFullName) {
        // Matches openCandidateProfile()'s "most recent" convention so this
        // checks the same candidate a prior step just acted on, not an
        // unrelated older one that happens to share the same name.
        return waitAllVisible(VACANCY_ROWS).stream()
                .filter(row -> row.getText().contains(candidateFullName))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException("Candidate not found: " + candidateFullName))
                .getText();
    }

    public RecruitmentPage scheduleInterview(String candidateFullName, String date, String time, String interviewer) {
        // "Schedule Interview" only exists on a specific candidate's own
        // profile page, not on the Candidates list — clicking straight for
        // it from the list view finds nothing and times out.
        openCandidateProfile(candidateFullName);
        // A freshly created candidate starts at "Application Initiated",
        // where the profile only offers Reject/Shortlist — Schedule
        // Interview only appears once they've been shortlisted. Clicking
        // Shortlist opens its own "Shortlist Candidate" confirmation form
        // that still needs its own Save before the status actually changes.
        // The JS-dispatched Save click below has occasionally fired before
        // Vue finishes attaching the confirmation form's own handlers (a
        // visible-but-not-yet-interactive element), silently doing nothing
        // — the modal closes on its own eventually but the shortlist never
        // actually persists, leaving Schedule Interview permanently absent
        // afterward. Retrying the whole shortlist attempt once, verified by
        // Schedule Interview actually being present afterward rather than
        // just assuming the click worked, catches that without masking a
        // genuine failure (which would still show Reject/Shortlist, not
        // Schedule Interview, after the retry too).
        for (int attempt = 0; attempt < 2; attempt++) {
            // openCandidateProfile() only waits for the row's own action
            // icon to be clickable, not for the resulting profile page to
            // finish rendering — a raw, unwaited findElements() check right
            // after it can land mid-navigation (URL changed, Vue hasn't
            // rendered either button yet) and misread "not loaded yet" as
            // "already past Shortlist", skipping straight to a Schedule
            // Interview click that isn't there either and hanging for the
            // full explicit-wait budget. Waiting for either action to
            // actually be present first removes that race.
            waitUntil(d -> !d.findElements(SHORTLIST_ACTION).isEmpty() || !d.findElements(SCHEDULE_INTERVIEW_BUTTON).isEmpty());
            if (driver.findElements(SHORTLIST_ACTION).isEmpty()) {
                break; // already shortlisted (or further along)
            }
            click(SHORTLIST_ACTION);
            // The old profile page's own Save (for its inline-editable
            // Candidate Profile section) can still be in the DOM the
            // instant Shortlist is clicked, and shares the exact same
            // generic locator — waiting for the confirmation form's own
            // heading first guarantees the click below lands on ITS Save,
            // not a leftover one from the page being replaced.
            waitVisible(By.xpath("//*[normalize-space()='Shortlist Candidate']"));
            // A native WebDriver click on this particular Save has proven
            // unreliable in headless runs (no error, no effect) even once
            // scoped to the right form. Calling the element's own .click()
            // via JS (the fix originally tried here) turned out to be just
            // as unreliable — verified directly: two full, properly-waited
            // attempts both left the candidate's status unchanged. A raw
            // .click() call doesn't construct a real MouseEvent, and this
            // particular Vue component's handler appears to need one;
            // dispatching an actual bubbling, cancelable MouseEvent (what a
            // genuine user click produces) is a closer simulation and is
            // what actually gets picked up.
            WebElement shortlistSave = waitVisible(SAVE_BUTTON);
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));",
                    shortlistSave);
            // "Shortlist Candidate" is actually its own full navigated page
            // (not an overlay/modal, despite its name and the heading
            // staying on screen) — waiting for that heading to disappear
            // assumes Save navigates away from it, which it may not do even
            // on a genuine success (e.g. staying put and just toasting).
            // The toast is the one signal this codebase already treats as
            // authoritative for a save elsewhere; it's a non-blocking check
            // (not thrown if it never appears) so a genuine failure here
            // still surfaces normally via the loop's own re-verification.
            isVisible(TOAST_MESSAGE);
            // The profile page's in-place view of the status can lag
            // behind the server-side change that Save just made — reopen
            // it fresh from the list so "Schedule Interview" renders
            // against the now-current status rather than a stale one.
            openCandidatesTab();
            openCandidateProfile(candidateFullName);
        }
        click(SCHEDULE_INTERVIEW_BUTTON);
        type(INTERVIEW_NAME_INPUT, candidateFullName + " Interview");
        typeDate(INTERVIEW_DATE_INPUT, date);
        type(INTERVIEW_TIME_INPUT, time);
        selectFromOxdDropdown(INTERVIEWER_DROPDOWN, interviewer);
        click(SAVE_BUTTON);
        return this;
    }

    private void openCandidateProfile(String candidateFullName) {
        // The row itself isn't a navigation target — only its "view" (eye)
        // icon in the Actions column is, so clicking anywhere else on the
        // row is a silent no-op that leaves the list page showing.
        //
        // This shared public demo accumulates candidates of the same name
        // across every past run (there's no reset), and some of that old
        // data is in a state the server can't process any more (a deleted
        // hiring manager reliably makes a shortlist attempt fail server-side
        // with "Unexpected Error Occurred"). The most recently created
        // match is the one this scenario itself just added, and rows for a
        // repeated name appear in creation order, so taking the last match
        // rather than the first reliably lands on the live, workable one.
        WebElement row = waitAllVisible(VACANCY_ROWS).stream()
                .filter(r -> r.getText().contains(candidateFullName))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException("Candidate not found: " + candidateFullName));
        row.findElement(By.cssSelector(".oxd-table-cell-actions button")).click();
    }

    public RecruitmentPage markShortlisted(String candidateFullName) {
        click(ACTIONS_DROPDOWN);
        click(SHORTLIST_ACTION);
        return this;
    }

    public RecruitmentPage rejectCandidate(String candidateFullName, String reason) {
        click(ACTIONS_DROPDOWN);
        click(REJECT_ACTION);
        type(REJECT_REASON_INPUT, reason);
        click(CONFIRM_BUTTON);
        return this;
    }
}
