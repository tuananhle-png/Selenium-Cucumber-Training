@auth
Feature: Authentication
  As a user of OrangeHRM
  I want to log in and out securely
  So that only authorized users can access the system

  Background:
    Given I am on the OrangeHRM login page

  @TC-AUTH-01 @mandatory @smoke
  Scenario: Login with valid credentials
    When I log in with username "Admin" and password "admin123"
    Then I should be redirected to the dashboard
    And the dashboard header should show "Dashboard"

  @TC-AUTH-02 @mandatory @regression
  Scenario Outline: Login with invalid credentials
    When I log in with username "<username>" and password "<password>"
    Then I should see the login error message "<errorMessage>"
    And I should remain on the login page

    Examples:
      | username | password   | errorMessage        |
      | Admin    | wrongPass1 | Invalid credentials |
      | WrongUsr | admin123   | Invalid credentials |

  @TC-AUTH-03 @mandatory @regression
  Scenario: Login with blank username and password
    When I submit the login form with blank username and blank password
    Then validation messages should appear on both the username and password fields

  @TC-AUTH-04 @bonus @regression
  Scenario Outline: Login with a single blank field
    When I log in with username "<username>" and password "<password>"
    Then a required validation message should appear on the "<field>" field

    Examples:
      | username | password | field    |
      | Admin    |          | password |

  @TC-AUTH-05 @mandatory @sanity
  Scenario: Logout from the application
    Given I have logged in with username "Admin" and password "admin123"
    When I log out
    Then I should be redirected to the login page
    And navigating back should not re-enter the application

  @TC-AUTH-06 @bonus @regression @known-issue
  Scenario: Forgot password with a registered email
    When I request a password reset for username "Admin"
    Then I should see the reset password confirmation message

  @TC-AUTH-07 @bonus @regression @known-issue
  Scenario: Forgot password with an unregistered email
    When I request a password reset for username "no_such_user_9999"
    Then I should see the reset password confirmation message
