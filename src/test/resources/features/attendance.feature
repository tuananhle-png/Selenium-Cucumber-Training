# Punch In/Out is not a top-level sidebar module in this OrangeHRM version —
# it lives at Time > Attendance (a dropdown) > Punch In/Out, direct URL
# /web/index.php/attendance/punchIn. That single page toggles between an
# "In" button and an "Out" button based on current punch state.
@attendance
Feature: Attendance / Time Tracking

  Background:
    Given I have logged in with username "Admin" and password "admin123"

  @TC-ATT-01 @mandatory @smoke
  Scenario: Punch in without a prior punch out
    When I punch in
    Then the punch-in should be recorded with a timestamp

  @TC-ATT-02 @mandatory @smoke
  Scenario: Punch out after punching in
    Given I have punched in
    When I punch out
    Then the punch-out should be recorded
    And the punch duration should equal the time between punch-in and punch-out

  @TC-ATT-03 @mandatory @regression
  Scenario: Punch in when already punched in
    Given I have punched in
    When I attempt to punch in again
    Then I should see a duplicate punch warning

  @TC-ATT-04 @bonus @regression
  Scenario: View attendance records for the current month
    When I view my attendance records for the current month
    Then all punch-in and punch-out records for the month should be listed

  @TC-ATT-05 @bonus @regression
  Scenario: Admin edits an employee attendance record
    Given an attendance record exists
    When the admin edits the attendance record punch-in time to "09:00"
    Then the audit trail should be preserved

  @TC-ATT-06 @bonus @regression
  Scenario Outline: View attendance report by date range
    When I view the attendance report from "<fromDate>" to "<toDate>"
    Then the report should only show records within "<fromDate>" and "<toDate>"

    Examples:
      | fromDate   | toDate     |
      | 2026-09-01 | 2026-09-15 |
      | 2026-09-16 | 2026-09-23 |
