# Every Leave scenario tops up the account's leave balance first via
# Admin > Leave > Entitlements. This shared public demo has no API to reset
# state between runs, and applying for leave consumes finite balance, so a
# long-running suite (or repeated CI runs) would otherwise eventually see
# "No Leave Types with Leave Balance" instead of a usable Apply form.
@leave
Feature: Leave Management

  Background:
    Given I have logged in with username "Admin" and password "admin123"
    And the leave balance has been topped up

  @TC-LV-01 @mandatory @smoke
  Scenario: Apply for leave with a valid date range
    When I apply for leave type "CAN - Personal" from "2026-10-01" to "2026-10-02"
    Then the leave request should be submitted successfully

  @TC-LV-02 @mandatory @regression
  Scenario: Apply for leave with end date before start date
    When I apply for leave type "CAN - Personal" from "2026-10-05" to "2026-10-01"
    Then I should see a date range validation error
    And the leave request should not be submitted

  @TC-LV-03 @bonus @regression
  Scenario Outline: Apply for leave exceeding available balance (Boundary Value Analysis)
    Given the "CAN - Personal" leave balance is set to <availableDays> days
    When I apply for leave type "CAN - Personal" spanning <requestedDays> days
    Then the leave application should "<expectedOutcome>"

    Examples:
      | availableDays | requestedDays | expectedOutcome           |
      | 5              | 4              | be allowed                |
      | 5              | 5              | be allowed                |
      | 5              | 6              | be blocked or flagged      |

  @TC-LV-04 @mandatory @regression
  Scenario: Admin approves a pending leave request
    Given a pending leave request exists
    When the admin approves the pending leave request
    Then the leave status should change to "Scheduled"

  @TC-LV-05 @mandatory @regression
  Scenario: Admin rejects a pending leave request
    Given a pending leave request exists
    When the admin rejects the pending leave request
    Then the leave status should change to "Rejected"

  @TC-LV-06 @bonus @regression
  Scenario: Employee cancels an approved leave
    Given an approved leave request exists
    When the employee cancels the approved leave
    Then a cancellation request should be created

  @TC-LV-07 @bonus @regression
  Scenario: View leave entitlement summary
    When I view the leave entitlement summary
    Then the entitlement summary should list a balance for each leave type
