package hooks;

import base.DriverManager;
import config.ConfigReader;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import pages.LoginPage;

public class Hooks {

    @Before
    public void beforeScenario() {
        WebDriver driver = DriverManager.getDriver();
        new LoginPage(driver).open(ConfigReader.get("base.url"));
    }

    @After
    public void afterScenario(Scenario scenario) {
        try {
            if (scenario.isFailed()) {
                WebDriver driver = DriverManager.getDriver();
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                scenario.attach(screenshot, "image/png", scenario.getName());
            }
        } finally {
            DriverManager.quitDriver();
        }
    }
}
