@e2e
Feature: End-to-End Capstone Flow

  @mandatory @smoke @e2e
  Scenario: New employee onboarding through leave approval and attendance
    Given I have logged in with username "Admin" and password "admin123"
    And the leave balance has been topped up
    When I add a new employee with first name "Capstone" and last name "Employee01"
    And I apply for leave type "CAN - Personal" from "2026-10-01" to "2026-10-02" for that employee
    And the admin approves the pending leave request for that employee
    And I punch in for that employee
    And I punch out for that employee
    Then the attendance record should reflect the punch data
