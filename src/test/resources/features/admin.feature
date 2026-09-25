@admin
Feature: Admin - User & Configuration Management

  Background:
    Given I have logged in with username "Admin" and password "admin123"

  @TC-ADM-01 @mandatory @smoke
  Scenario: Create a new user with ESS role
    When I create a system user "ess.user1" with role "ESS" linked to employee "Admin" and password "Passw0rd!23"
    Then the user "ess.user1" should be created

  @TC-ADM-02 @mandatory @regression
  Scenario: Create user with a duplicate username
    Given a system user "ess.user1" already exists
    When I create a system user "ess.user1" with role "ESS" linked to employee "Admin" and password "Passw0rd!23"
    Then I should see a username already exists error

  @TC-ADM-03 @mandatory @regression
  Scenario: Disable an active user account
    Given a system user "ess.user1" already exists and is enabled
    When I disable the user "ess.user1"
    Then the user "ess.user1" should no longer be enabled

  @TC-ADM-04 @bonus @regression
  Scenario: Add a new job title
    When I add a job title "Automation Engineer"
    Then the job title "Automation Engineer" should be available in PIM dropdowns

  @TC-ADM-05 @bonus @regression
  Scenario: Add a new organizational unit
    When I add an organizational unit "Quality Engineering"
    Then the unit "Quality Engineering" should be saved

  @TC-ADM-06 @bonus @regression
  Scenario: Update general organization name
    When I update the general organization name to "Railinc Mock Org"
    Then the header branding should reflect "Railinc Mock Org"

  @TC-ADM-07 @bonus @regression
  Scenario: Delete a job title in use by an employee
    Given a job title "QA Engineer" is assigned to an employee
    When I attempt to delete the job title "QA Engineer"
    Then a deletion warning should be shown
