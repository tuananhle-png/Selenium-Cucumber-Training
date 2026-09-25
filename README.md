# OrangeHRM Automation Framework — Selenium + Cucumber (BDD) + POM

Java/Maven test automation framework for the OrangeHRM demo application
(`https://opensource-demo.orangehrmlive.com`), built with **Selenium
WebDriver + Cucumber (Gherkin/BDD) + TestNG**, following the **Page Object
Model**. Test data for data-driven cases is expressed with **Scenario
Outline + Examples tables**. Scope and test case IDs come from
[`mock-project-requirement.md`](mock-project-requirement.md) — 43 test
cases across 6 modules (Authentication, PIM, Leave, Attendance,
Recruitment, Admin) plus one End-to-End capstone flow.

## Stack

| Layer | Choice |
|---|---|
| Language / build | Java 17, Maven |
| Browser automation | Selenium WebDriver 4.x |
| Driver management | WebDriverManager (no manual driver binaries) |
| BDD runner | Cucumber-JVM 7.x on TestNG (`AbstractTestNGCucumberTests`) |
| Assertions | AssertJ |
| Reporting | Allure (Cucumber adapter) |
| CI | Jenkins or GitHub Actions (`mvn clean test`) |

## Project layout

```
src/test/java/
├── config/ConfigReader.java        Loads config.properties, overridable via -D system properties
├── base/
│   ├── DriverManager.java          ThreadLocal<WebDriver>; browser choice via -Dbrowser
│   └── TestContext.java            Scenario-scoped state, shared across step classes via PicoContainer DI
├── hooks/Hooks.java                @Before opens the login page, @After attaches a screenshot on failure and quits the driver
├── pages/                          Page Objects — locators + actions only, zero assertions
│   ├── BasePage.java               FluentWait helpers shared by every page
│   ├── LoginPage.java / DashboardPage.java
│   └── PimPage.java / LeavePage.java / AttendancePage.java / RecruitmentPage.java / AdminPage.java
├── stepdefinitions/                Glue code: Gherkin steps → page object calls + AssertJ assertions
└── runners/TestRunner.java         Cucumber-TestNG entry point (tags come from the command line, not hardcoded)

src/test/resources/
├── config.properties               base.url, credentials, timeouts, thresholds — no hardcoded values in code
├── testdata/                       Sample files for the upload scenarios (TC-PIM-08/09)
└── features/                       One .feature file per module + e2e.feature
    ├── authentication.feature      TC-AUTH-01..07
    ├── pim.feature                 TC-PIM-01..10
    ├── leave.feature               TC-LV-01..07 (TC-LV-03 is a boundary-value Scenario Outline)
    ├── attendance.feature          TC-ATT-01..06
    ├── recruitment.feature         TC-RC-01..06
    ├── admin.feature                TC-ADM-01..07
    └── e2e.feature                 Capstone: add employee → apply leave → approve → punch in/out

testng.xml                          Wires TestRunner into the Surefire execution
pom.xml
```

Every scenario carries three kinds of tags:
- **Traceability**: `@TC-AUTH-01`, `@TC-PIM-03`, … — one per test case ID
- **Tier**: `@mandatory` / `@bonus`
- **Suite**: `@smoke`, `@sanity`, `@regression`

## Running

```bash
# All 43 TCs + E2E, Chrome, headed
mvn clean test

# Only the Mandatory tier (22 TCs)
mvn clean test -Dcucumber.filter.tags="@mandatory"

# Only Smoke suite
mvn clean test -Dcucumber.filter.tags="@smoke"

# Only Regression suite
mvn clean test -Dcucumber.filter.tags="@regression"

# A single test case
mvn clean test -Dcucumber.filter.tags="@TC-LV-03"

# Different browser (chrome | firefox | edge)
mvn clean test -Dbrowser=firefox

# Headless
mvn clean test -Dheadless=true

# Generate and open the Allure report
mvn allure:serve
```

No Maven installed locally? Use the bundled wrapper instead of `mvn`:
`./mvnw clean test` (or `mvnw.cmd` on Windows cmd/PowerShell).

## Configuration

All environment-specific values live in
[`src/test/resources/config.properties`](src/test/resources/config.properties)
(`base.url`, `admin.username`/`admin.password`, `browser`, `headless`,
wait timeouts, `leave.balance.limit.days`). Every key can be overridden
per-run with a matching `-D` system property without touching the file,
e.g. `-Dbase.url=https://staging.example.com`.

## Design notes

- **Zero `Thread.sleep`** — all waits go through `BasePage`'s `FluentWait`
  (explicit waits only).
- **Locator strategy** — `By.name` / `By.cssSelector` preferred; a small
  number of dynamic OXD components (dropdowns, table rows matched by
  visible text) require `By.xpath` since OrangeHRM's component library
  doesn't expose stable IDs there.
- **`TC-LV-03`** demonstrates boundary value analysis with a
  `Scenario Outline` + `Examples` table (at, above, below the leave
  balance threshold) — the closest OrangeHRM analog to the original
  coupon/tax BVA examples, per the requirement doc's re-mapping.
- **`TC-AUTH-06`/`TC-AUTH-07`** are tagged `@known-issue`: the requirement
  doc marks these FAILED because the OrangeHRM demo's forgot-password flow
  returns the same confirmation regardless of whether the email is
  registered, so the negative case can't be distinguished from the
  positive one on this instance.

## Known limitations / next steps

- **Attendance/Punch In-Out is not on the sidebar** — it lives at
  Time → Attendance (a dropdown) → Punch In/Out, direct URL
  `/web/index.php/attendance/punchIn`. That single page toggles between an
  "In" button and an "Out" button based on current punch state rather than
  exposing separate pages; `AttendancePage` navigates there directly rather
  than clicking through the dropdown each time.
- **Locators have been verified against the live demo repeatedly** during
  development (not just authored from convention), including several
  rounds of fixing real, confirmed mismatches: an exact-class XPath match
  that should have used `contains()`, a dropdown-trigger locator that
  matched multiple sibling elements by class substring, a form field with
  no `name` attribute at all, and page-navigation assumptions that didn't
  hold once verified against the actual DOM. Some deeper flows (Recruitment
  candidate/interview scheduling, Admin's employee autocomplete) are still
  best-effort and worth re-verifying if OrangeHRM's demo build changes.
- **The shared public demo instance is not always fast or stable.** Under
  sustained automated load (many sequential scenarios/runs) it has shown
  page-load timeouts unrelated to any locator. Point this framework at a
  dedicated/private OrangeHRM instance for reliable CI.
- **Test data is static** (e.g. `"ess.user1"`, `"Dana Scott"`). Since the
  demo instance is a shared public environment, repeated runs will
  accumulate records rather than fail outright, but usernames in
  `admin.feature` (`TC-ADM-01/02/03`) will collide on a second run.
  Recommended follow-up: suffix generated names/usernames with a
  timestamp or `UUID` in the step definitions once running against a
  real environment, to make reruns idempotent.
- **`TC-LV-03`'s balance precondition** (`the "CAN - Personal" leave balance
  is set to <n> days`) documents the precondition rather than seeding it —
  OrangeHRM's demo has no API to set entitlements, so this must be
  configured once via Admin → Leave → Entitlements before the scenario
  runs unattended.
- Cross-browser (Chrome/Firefox/Edge) and CI wiring (Jenkins or GitHub
  Actions) are supported by `DriverManager` and the Surefire
  configuration, but no CI pipeline file is included yet — add a
  `Jenkinsfile` or `.github/workflows/tests.yml` invoking the `mvn
  clean test` commands above per the project's CI/CD choice.
