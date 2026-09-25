@recruitment
Feature: Recruitment

  Background:
    Given I have logged in with username "Admin" and password "admin123"

  @TC-RC-01 @mandatory @smoke
  Scenario: Create a new job vacancy
    When I create a job vacancy "QA Engineer Opening" with 2 positions
    Then the vacancy "QA Engineer Opening" should appear in the vacancy list

  @TC-RC-02 @mandatory @regression
  Scenario: Add a candidate to a vacancy
    Given a vacancy "QA Engineer Opening" exists
    When I add candidate "Ivy Chan" to vacancy "QA Engineer Opening"
    Then the candidate "Ivy Chan" should have status "Application Initiated"

  @TC-RC-03 @mandatory @regression
  Scenario: Schedule an interview for a candidate
    Given candidate "Ivy Chan" exists for vacancy "QA Engineer Opening"
    When I schedule an interview for "Ivy Chan" on "2026-10-10" at "10:00" with interviewer "Admin"
    Then the interview should be recorded for "Ivy Chan"

  @TC-RC-04 @bonus @regression
  Scenario: Mark candidate as Shortlisted
    Given candidate "Ivy Chan" exists for vacancy "QA Engineer Opening"
    When I mark "Ivy Chan" as "Shortlisted"
    Then the candidate "Ivy Chan" status should be "Shortlisted"

  @TC-RC-05 @bonus @regression
  Scenario: Reject a candidate with a reason
    Given candidate "Ivy Chan" exists for vacancy "QA Engineer Opening"
    When I reject "Ivy Chan" with reason "Not a fit for the role"
    Then the candidate "Ivy Chan" status should be "Rejected"

  @TC-RC-06 @bonus @regression
  Scenario: Create a vacancy with no job title selected
    When I attempt to create a vacancy without selecting a job title
    Then a validation error should be shown
    And the vacancy should not be saved
