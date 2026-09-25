package base;

import config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

/**
 * Owns one WebDriver instance per test thread so parallel TestNG/Cucumber
 * execution never shares a browser session across threads.
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver getDriver() {
        if (DRIVER.get() == null) {
            DRIVER.set(createDriver());
        }
        return DRIVER.get();
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }

    private static WebDriver createDriver() {
        String browser = ConfigReader.get("browser").toLowerCase();
        boolean headless = ConfigReader.getBoolean("headless");
        WebDriver driver;

        switch (browser) {
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    // "-headless" alone keeps Firefox's default small headless
                    // viewport, which collapses OrangeHRM's sidebar into a
                    // hamburger menu. Force a real desktop viewport instead.
                    options.addArguments("-headless", "--width=1920", "--height=1080");
                }
                driver = new FirefoxDriver(options);
            }
            case "edge" -> {
                // WebDriverManager's auto-resolution can itself be
                // unreachable (its CDN host may be stale/retired); if a
                // driver path was already supplied via -Dwebdriver.edge.driver
                // (e.g. a manually downloaded matching driver), trust it
                // instead of forcing a network lookup that would just fail.
                if (System.getProperty("webdriver.edge.driver") == null) {
                    WebDriverManager.edgedriver().setup();
                }
                EdgeOptions options = new EdgeOptions();
                options.addArguments("--remote-allow-origins=*", "--window-size=1920,1080");
                if (headless) {
                    options.addArguments("--headless=new");
                }
                driver = new EdgeDriver(options);
            }
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver(chromeOptions(headless));
            }
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        }

        // window().maximize() is a no-op/unreliable in headless mode (there is
        // no real OS window to maximize), which can leave the browser at a
        // small default viewport. OrangeHRM's OXD layout collapses the
        // sidebar behind a hamburger icon below its responsive breakpoint, so
        // every headless run needs an explicit desktop-sized viewport set via
        // browser launch args (above) rather than relying on maximize() here.
        if (!headless) {
            driver.manage().window().maximize();
        }
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigReader.getInt("page.load.timeout.seconds")));
        return driver;
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*", "--window-size=1920,1080");
        if (headless) {
            options.addArguments("--headless=new");
        }
        return options;
    }
}
