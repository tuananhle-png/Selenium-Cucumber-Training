# Automation Mock Project — Requirements & Test Case Package
### AUT: OrangeHRM Demo · https://opensource-demo.orangehrmlive.com · Java + Selenium + TestNG
**Target Audience:** Beginner / Intermediate Automation Students
**Credentials:** `Admin` / `admin123`

---

## 1. Project Overview

| Item | Detail                                                       |
|------|--------------------------------------------------------------|
| **Application Under Test** | https://opensource-demo.orangehrmlive.com                    |
| **Framework** | Java + Selenium WebDriver + TestNG; Playwright + typescript  |
| **Build Tool** | Maven                                                        |
| **Reporting** | Allure Report                                                |
| **CI/CD** | Jenkins                                                      |
| **Architecture** | Page Object Model (POM)                                      |
| **Duration** | 5-day sprint (flexible; see Phase plan)                      |
| **Total Test Cases** | 43 (see note below)                                          |

> **Note on test case count:** the source test case bank (`orangehrm_test_cases.md`) lists 43 individual rows across 6 modules (7+10+7+6+6+7), though its own summary table states 40. This package uses the actual 43 rows and tiers them below — trainers may trim 2–3 Bonus-tier cases per class size without affecting the Mandatory core.

---

## 2. Tiered Test Case Scope (per Rubric adjustment)

* **Mandatory (Beginner Core — 22 TCs):** Smoke paths, straightforward Regression, and the End-to-End Capstone. Builds solid POM, locator, and assertion fundamentals.
* **Bonus (Intermediate Stretch — 21 TCs):** Boundary Value Analysis (leave-balance threshold, `TC-LV-03`), file-upload handling, multi-step admin/recruitment workflows, and audit-trail verification.
* Because OrangeHRM has no shopping-cart coupon or tax logic, the original plan's BVA/math-assertion examples are re-mapped:
  - **Coupon threshold BVA → `TC-LV-03`** (leave request exceeding available balance)
  - **Tax/price math assertion → `TC-ATT-02`** (punch-out duration calculated from punch-in/out timestamps)
  - **Advanced sorting → `TC-ATT-06` / `TC-PIM-04`** (date-range report filtering / partial-name search) as the closest "advanced filtering" analogs

---

## 3. Full Test Case Registry

### Module: Authentication (7 TCs — 4 Mandatory / 3 Bonus)

| ID                  | Test Case | Expected Result | Priority | Type | Tier | Suite |
|---------------------|---|---|---|---|---|---|
| TC-AUTH-01          | Login with valid credentials | Dashboard loads; welcome message shown | High | Positive | Mandatory | Smoke |
| TC-AUTH-02          | Login with invalid password | Error "Invalid credentials" shown; no redirect | High | Negative | Mandatory | Regression |
| TC-AUTH-03          | Login with blank username and password | Validation messages appear on both fields | High | Negative | Mandatory | Regression |
| TC-AUTH-04          | Login with username only (password blank) | "Required" validation on password field | Medium | Boundary | Bonus | Regression |
| TC-AUTH-05          | Logout from the application | Session ends; login page shown; back-button does not re-enter | High | Functional | Mandatory | Sanity |
| TC-AUTH-06 - FAILED | Forgot password with registered email | Password reset email sent; success message shown | Medium | Positive | Bonus | Regression |
| TC-AUTH-07 - FAILED | Forgot password with unregistered email | Error or "email not found" message shown | Medium | Negative | Bonus | Regression |

### Module: PIM — Personal Information Management (10 TCs — 5 Mandatory / 5 Bonus)

| ID | Test Case | Expected Result | Priority | Type | Tier | Suite |
|---|---|---|---|---|---|---|
| TC-PIM-01 | Add a new employee with all required fields | Employee created; appears in employee list | High | Positive | Mandatory | Smoke |
| TC-PIM-02 | Add employee without first name | "Required" validation prevents save | High | Negative | Mandatory | Regression |
| TC-PIM-03 | Search employee by exact name | Matching employee shown in results | High | Positive | Mandatory | Regression |
| TC-PIM-04 | Search employee by partial name | All matching employees shown | Medium | Functional | Bonus | Regression |
| TC-PIM-05 | Search employee with non-existent name | "No records found" message shown | Medium | Negative | Bonus | Regression |
| TC-PIM-06 | Edit employee job title and save | Updated job title saved; reflected on profile | High | Positive | Mandatory | Regression |
| TC-PIM-07 | Delete an employee | Confirmation prompt; employee removed from list | High | Functional | Mandatory | Regression |
| TC-PIM-08 | Upload profile photo (valid JPG, under 1 MB) | Photo saved; visible on employee profile | Medium | Positive | Bonus | Regression |
| TC-PIM-09 | Upload unsupported file type as profile photo | Error message; photo not changed | Medium | Negative | Bonus | Regression |
| TC-PIM-10 | Add emergency contact for employee | Contact saved under Emergency Contacts tab | Medium | Positive | Bonus | Regression |

### Module: Leave (7 TCs — 4 Mandatory / 3 Bonus)

| ID | Test Case | Expected Result | Priority | Type | Tier | Suite |
|---|---|---|---|---|---|---|
| TC-LV-01 | Apply for leave with valid date range | Leave request submitted; status "Pending" | High | Positive | Mandatory | Smoke |
| TC-LV-02 | Apply for leave with end date before start date | Validation error; request not submitted | High | Negative | Mandatory | Regression |
| TC-LV-03 | Apply for leave exceeding available balance | Warning or error; request blocked or flagged | High | **Boundary (BVA)** | **Bonus** | Regression |
| TC-LV-04 | Admin approves a pending leave request | Status changes to "Approved"; employee notified | High | Functional | Mandatory | Regression |
| TC-LV-05 | Admin rejects a pending leave request | Status changes to "Rejected"; balance not deducted | High | Functional | Mandatory | Regression |
| TC-LV-06 | Employee cancels an approved leave | Cancellation request created; balance restored on approval | Medium | Functional | Bonus | Regression |
| TC-LV-07 | View leave entitlement summary | Correct balance shown per leave type | Medium | Positive | Bonus | Regression |

### Module: Attendance (6 TCs — 3 Mandatory / 3 Bonus)

| ID | Test Case | Expected Result | Priority | Type | Tier | Suite |
|---|---|---|---|---|---|---|
| TC-ATT-01 | Punch in without prior punch out | Punch-in recorded; timestamp shown | High | Positive | Mandatory | Smoke |
| TC-ATT-02 | Punch out after punching in | Punch-out recorded; duration calculated | High | Positive | **Mandatory (Math Assertion)** | Smoke |
| TC-ATT-03 | Punch in when already punched in | Error or warning; duplicate punch blocked | High | Negative | Mandatory | Regression |
| TC-ATT-04 | View attendance records for current month | All punch-in/out records listed correctly | Medium | Functional | Bonus | Regression |
| TC-ATT-05 | Admin edits an employee attendance record | Record updated; audit trail preserved | Medium | Functional | Bonus | Regression |
| TC-ATT-06 | View attendance report by date range | Report shows records within selected range only | Medium | Positive | **Bonus (Filtering)** | Regression |

### Module: Recruitment (6 TCs — 3 Mandatory / 3 Bonus)

| ID | Test Case | Expected Result | Priority | Type | Tier | Suite |
|---|---|---|---|---|---|---|
| TC-RC-01 | Create a new job vacancy | Vacancy created; visible in vacancy list | High | Positive | Mandatory | Smoke |
| TC-RC-02 | Add a candidate to a vacancy | Candidate record linked to vacancy; status "Application Initiated" | High | Positive | Mandatory | Regression |
| TC-RC-03 | Schedule an interview for a candidate | Interview record created with date, time, interviewer | High | Functional | Mandatory | Regression |
| TC-RC-04 | Mark candidate as "Shortlisted" | Status updates; candidate moves to shortlist stage | Medium | Functional | Bonus | Regression |
| TC-RC-05 | Reject a candidate with a reason | Status "Rejected"; reason stored; candidate notified (if configured) | Medium | Functional | Bonus | Regression |
| TC-RC-06 | Create a vacancy with no job title selected | Validation error; vacancy not saved | Medium | Negative | Bonus | Regression |

### Module: Admin (7 TCs — 3 Mandatory / 4 Bonus)

| ID | Test Case | Expected Result | Priority | Type | Tier | Suite |
|---|---|---|---|---|---|---|
| TC-ADM-01 | Create a new user with ESS role | User created; can log in with given credentials | High | Positive | Mandatory | Smoke |
| TC-ADM-02 | Create user with duplicate username | Error "Username already taken"; user not created | High | Negative | Mandatory | Regression |
| TC-ADM-03 | Disable an active user account | User cannot log in; "Account disabled" message shown | High | Functional | Mandatory | Regression |
| TC-ADM-04 | Add a new job title | Job title saved; available in PIM dropdowns | Medium | Positive | Bonus | Regression |
| TC-ADM-05 | Add a new organizational unit/department | Unit saved; available in employee structure | Medium | Positive | Bonus | Regression |
| TC-ADM-06 | Update general organization name | New name reflected in header/branding on save | Low | Functional | Bonus | Regression |
| TC-ADM-07 | Delete a job title in use by an employee | Error or warning; deletion blocked to preserve data integrity | Medium | Negative | Bonus | Regression |

---

## 4. End-to-End Capstone Flow

Implemented as `E2ETest.java`, ties multiple modules together in one flow:

```
Login (Admin) → Navigate to PIM → Add new employee →
Navigate to Leave → Apply leave for that employee →
Admin approves leave → Navigate to Attendance →
Punch in / punch out for employee → Assert attendance record reflects punch data
```

---

## 5. Tier & Suite Summary

| Module | Total | Mandatory | Bonus |
|---|---|---|---|
| Authentication | 7 | 4 | 3 |
| PIM | 10 | 5 | 5 |
| Leave | 7 | 4 | 3 |
| Attendance | 6 | 3 | 3 |
| Recruitment | 6 | 3 | 3 |
| Admin | 7 | 3 | 4 |
| **Total** | **43** | **22** | **21** |

| Suite | Approx. Count | Notes |
|---|---|---|
| **Smoke** | 7 + E2E | One core happy-path per module |
| **Sanity** | 1–2 | Login/logout session integrity |
| **Regression** | 34 | All remaining negative, boundary, and functional TCs |

---

## 6. Simplified CI/CD Integration

Setup jenkins server and create a jenkins job to execute test daily (include daily new tests)
---

## 7. Suggested Framework Structure

```
src/
├── main/java/
│   ├── pages/
│   │   ├── LoginPage.java
│   │   ├── PIMPage.java
│   │   ├── LeavePage.java
│   │   ├── AttendancePage.java
│   │   ├── RecruitmentPage.java
│   │   └── AdminPage.java
│   ├── base/
│   │   ├── BasePage.java
│   │   ├── BaseTest.java
│   │   └── DriverManager.java   ← ThreadLocal<WebDriver>
│   └── utils/
│       ├── ConfigReader.java
│       └── ScreenshotUtil.java
└── test/
    ├── java/
    │   ├── tests/
    │   │   ├── AuthTest.java         ← TC-AUTH-01 to 07
    │   │   ├── PimTest.java          ← TC-PIM-01 to 10
    │   │   ├── LeaveTest.java        ← TC-LV-01 to 07
    │   │   ├── AttendanceTest.java   ← TC-ATT-01 to 06
    │   │   ├── RecruitmentTest.java  ← TC-RC-01 to 06
    │   │   ├── AdminTest.java        ← TC-ADM-01 to 07
    │   │   └── E2ETest.java          ← capstone flow
    │   └── listeners/
    │       └── AllureListener.java
    └── resources/
        ├── config.properties
        └── testng.xml
```

`config.properties` should externalize `base.url`, credentials, and any thresholds (e.g. leave balance limit) — no hardcoded values anywhere in test or page classes.

---

## 9. Scoring Rubric (100 Points Total)

### 9.1 Framework Architecture & Design (25 pts)
* **[10]** Page Object Model — locators/actions only, zero assertions in page classes
* **[10]** Base Components — correct `BasePage`/`BaseTest`, `ThreadLocal` driver management
* **[5]** Configuration Management — no hardcoded URLs/credentials/thresholds

### 9.2 Test Implementation & Synchronization (30 pts)
* **[15]** Wait Strategy — zero `Thread.sleep()`; explicit/`FluentWait` for dynamic elements
* **[10]** Locator Strategy — `By.cssSelector`/`By.id`/`By.name` preferred over fragile XPath
* **[5]** Assertions — effective Hard/Soft assertions with clear validation messages

### 9.3 Test Execution & Reliability (20 pts)
* **[10]** Pass Rate — full core suite passes via `mvn clean test` unattended
* **[5]** Stability — no flakiness across repeated runs
* **[5]** Cross-Browser Support — Chrome, Firefox, Edge

### 9.4 Reporting & CI/CD (15 pts)
* **[10]** Reporting — Allure generates with failure screenshots, step logging, categorization
* **[5]** Pipeline Execution — suite runs successfully in Jenkins or GitHub Actions

### 9.5 Clean Code & Version Control (10 pts)
* **[5]** Naming Conventions — `PascalCase` classes, `camelCase` methods, `UPPER_SNAKE_CASE` constants
* **[5]** Git Best Practices — clean commit history, `develop`/feature branches, documented `README.md`

---

## 10. Suggested 10-Day Phase Plan

| Phase | Days | Focus |
|---|---|---|
| **Phase 0 — Setup** | Day 0 | Java/Maven/Git ready; WebDriverManager configured; Jenkins or GitHub Actions chosen; test accounts confirmed |
| **Phase 1 — Test Design** | Days 1–2 | Document Mandatory-tier TCs first (steps, expected results, data); Bonus TCs documented if time allows |
| **Phase 2 — Framework Architecture** | Days 3–4 | POM skeleton, `BasePage`/`BaseTest`, `config.properties`, empty TestNG run green in CI |
| **Phase 3 — Implementation** | Days 5–8 | Automate Mandatory tier first (22 TCs); Bonus tier (21 TCs) as stretch once Mandatory is stable |
| **Phase 4 — QA & Stabilization** | Days 9–10 | 3× consecutive stable runs, Allure trends, cross-browser smoke, PR review, tag `v1.0.0` |

### Entry & Exit Criteria (Phase 3)
| | Criteria |
|--|---|
| **Entry** | Framework skeleton merged; Mandatory-tier TCs documented |
| **Exit** | 22/22 Mandatory TCs passing locally; Bonus TCs attempted per student pace; PR peer-reviewed |

---

## 11. Definition of Done (Mandatory Tier)

- [ ] All 22 Mandatory TCs automated and passing locally (`mvn clean test`)
- [ ] Zero `Thread.sleep` — confirmed by `grep -r "Thread.sleep" src/`
- [ ] Zero hardcoded values — externalized in `config.properties`
- [ ] Smoke group passes on at least 2 browsers
- [ ] Allure report generated with screenshots on failure
- [ ] CI pipeline (Jenkins or GitHub Actions) triggers and runs green
- [ ] `README.md` documents setup, local run commands, and CI usage
- [ ] PR peer-reviewed and merged to `main`

**Stretch Definition of Done (Bonus Tier):** all 21 Bonus TCs (including `TC-LV-03` BVA) automated; `@DataProvider` migrated to external JSON/Excel for at least one module; cross-browser on 3 browsers.

---

## 12. Quick Reference

```bash
# Run all Mandatory + Bonus TCs on Chrome
mvn clean test

# Run only Smoke suite
mvn clean test -Dgroups=smoke

# Run only Regression suite
mvn clean test -Dgroups=regression

# Run on Firefox
mvn clean test -Dbrowser=firefox

# Run a specific module
mvn clean test -Dtest=LeaveTest

# Generate and open Allure report
mvn allure:serve
```
