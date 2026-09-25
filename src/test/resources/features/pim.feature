@pim
Feature: PIM - Personal Information Management

  Background:
    Given I have logged in with username "Admin" and password "admin123"

  @TC-PIM-01 @mandatory @smoke
  Scenario: Add a new employee with all required fields
    When I add a new employee with first name "John" and last name "Doe123"
    Then the employee "John Doe123" should be created
    And the employee should appear in the employee list

  @TC-PIM-02 @mandatory @regression
  Scenario: Add employee without first name
    When I attempt to add a new employee with first name "" and last name "Doe123"
    Then the employee should not be saved
    And a required validation message should appear on the employee form

  @TC-PIM-03 @mandatory @regression
  Scenario: Search employee by exact name
    Given an employee named "Alice Walters" exists
    When I search for an employee by name "Alice Walters"
    Then the results should include employee "Alice Walters"

  @TC-PIM-04 @bonus @regression
  Scenario Outline: Search employee by partial name
    Given an employee named "<fullName>" exists
    When I search for an employee by name "<partialName>"
    Then the results should include employee "<fullName>"

    Examples:
      | fullName      | partialName |
      | Brenda Walker | Bren        |
      | Brenda Walker | Walker      |

  @TC-PIM-05 @bonus @regression
  Scenario: Search employee with non-existent name
    When I search for an employee by name "NoSuchEmployeeXYZ"
    Then I should see the "No Records Found" message

  @TC-PIM-06 @mandatory @regression
  Scenario: Edit employee job title and save
    Given an employee named "Carlos Reyes" exists
    When I update the job title of "Carlos Reyes" to "QA Engineer"
    Then the job title for "Carlos Reyes" should be "QA Engineer"

  @TC-PIM-07 @mandatory @regression
  Scenario: Delete an employee
    Given an employee named "Dana Scott" exists
    When I delete the employee "Dana Scott"
    Then I should be asked to confirm the deletion
    And after confirming, "Dana Scott" should no longer appear in the employee list

  @TC-PIM-08 @bonus @regression
  Scenario: Upload a valid profile photo
    Given an employee named "Ella Fox" exists
    When I upload the profile photo "valid-photo.jpg" for "Ella Fox"
    Then the profile photo should be updated for "Ella Fox"

  @TC-PIM-09 @bonus @regression
  Scenario: Upload an unsupported file type as profile photo
    Given an employee named "Frank Ito" exists
    When I upload the profile photo "unsupported-file.txt" for "Frank Ito"
    Then I should see a photo upload error message
    And the profile photo should not change for "Frank Ito"

  @TC-PIM-10 @bonus @regression
  Scenario: Add emergency contact for employee
    Given an employee named "Grace Lin" exists
    When I add an emergency contact named "Henry Lin" with relationship "Spouse" and mobile "5551234567" for "Grace Lin"
    Then the emergency contact "Henry Lin" should be saved under the Emergency Contacts tab for "Grace Lin"
