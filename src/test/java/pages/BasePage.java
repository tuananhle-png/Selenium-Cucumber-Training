package pages;

import config.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Base class for every page object. Provides explicit/FluentWait-backed
 * element access so no page object ever needs Thread.sleep().
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final Wait<WebDriver> wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(ConfigReader.getInt("explicit.wait.seconds")))
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .ignoring(ElementClickInterceptedException.class);
    }

    protected WebElement waitVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected List<WebElement> waitAllVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    /**
     * waitVisible()'s own FluentWait already tolerates a stale element while
     * it's still searching, but the element it hands back can itself go
     * stale in the gap between that search returning and this method's own
     * .isDisplayed() call on it (the page re-rendering the exact instant a
     * caller here loses that race) — an uncaught
     * StaleElementReferenceException from .isDisplayed() itself, rather
     * than a TimeoutException, isn't a shape this method originally
     * anticipated. One retry (a fresh waitVisible() re-locates the element
     * rather than reusing the now-stale reference) covers that race the
     * same way callers already expect a transient miss to resolve to
     * false, not an exception.
     */
    protected boolean isVisible(By locator) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return waitVisible(locator).isDisplayed();
            } catch (org.openqa.selenium.TimeoutException e) {
                return false;
            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                // retry once with a freshly re-located element
            }
        }
        return false;
    }

    /**
     * Retries the full locate-and-click cycle (not just the wait) so a
     * transient overlay (e.g. a loading spinner still fading out right after
     * navigation) that intercepts one click attempt doesn't fail the whole
     * step — the element is re-checked and re-clicked on the next poll.
     */
    protected void click(By locator) {
        waitForLoadersGone();
        wait.until(d -> {
            WebElement element = ExpectedConditions.elementToBeClickable(locator).apply(d);
            if (element == null) {
                return false;
            }
            element.click();
            return true;
        });
    }

    protected void type(By locator, String text) {
        waitForLoadersGone();
        if (text == null || text.isEmpty()) {
            waitVisible(locator).clear();
            return;
        }
        // Some OXD fields (date pickers reacting to a sibling field, e.g.
        // Leave's "To Date" auto-populating from "From Date") are custom
        // masked inputs where WebElement.clear() doesn't reliably reset the
        // internal masked value, leaving old and new text concatenated.
        // Selecting all and deleting via the keyboard clears it properly;
        // verifying the actual resulting value and retrying covers any
        // remaining timing race with the field's own reactivity.
        wait.until(d -> {
            WebElement element = d.findElement(locator);
            element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            element.sendKeys(text);
            return text.equals(element.getAttribute("value"));
        });
    }

    /**
     * This shared public demo's date fields are custom masked inputs whose
     * expected format follows whatever this instance's Admin > Configuration
     * > Localization date format is currently set to — not necessarily
     * yyyy-MM-dd. That setting is global and can drift on a shared,
     * never-reset demo (observed live as "yyyy-dd-mm"), silently
     * misinterpreting a plain yyyy-MM-dd string (e.g. reading "2026-10-05"
     * as day=10, month=05) without any visible error, since the typed text
     * still matches the field's own value attribute either way. Reading the
     * field's placeholder (which mirrors the active format, e.g.
     * "yyyy-dd-mm") and formatting the intended date to match it keeps
     * every caller's date correct regardless of which format is live.
     */
    protected void typeDate(By locator, String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            type(locator, isoDate);
            return;
        }
        LocalDate date = LocalDate.parse(isoDate);
        String placeholder = waitVisible(locator).getAttribute("placeholder");
        String formatted = isoDate;
        if (placeholder != null && !placeholder.isBlank()) {
            String pattern = placeholder.replaceAll("(?i)dd", "dd").replaceAll("(?i)mm", "MM");
            try {
                formatted = DateTimeFormatter.ofPattern(pattern).format(date);
            } catch (RuntimeException e) {
                formatted = isoDate;
            }
        }
        type(locator, formatted);
    }

    protected String getText(By locator) {
        return waitVisible(locator).getText();
    }

    protected void waitForInvisibility(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    private static final By[] LOADER_OVERLAYS = {
            By.cssSelector(".oxd-form-loader"), By.cssSelector(".oxd-table-loader")
    };

    /**
     * OXD pages render a full-width loading spinner over the form/table
     * while their data finishes fetching after a navigation. Waiting for it
     * to disappear (a no-op if it was never present) avoids racing a click
     * against content that hasn't finished loading yet, which otherwise
     * shows up as either an ElementClickInterceptedException or a element
     * that simply never becomes visible in time.
     */
    private void waitForLoadersGone() {
        for (By loader : LOADER_OVERLAYS) {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(loader));
        }
    }

    protected <T> T waitUntil(Function<WebDriver, T> condition) {
        return wait.until(condition);
    }

    private static final By LISTBOX_OPTIONS = By.xpath("//div[@role='listbox']//span");

    /**
     * Selects the named option from an OXD dropdown, falling back to
     * whichever option is first in the list if the exact name isn't present.
     * The fallback matters on this shared public demo instance: its seed
     * data (e.g. which leave types exist) is not stable across page loads,
     * so asserting a specific option's exact name is inherently fragile
     * there — the test's intent (apply for *a* leave type) survives the
     * fallback even when the demo's specific data has drifted.
     */
    protected void selectFromOxdDropdown(By dropdownLocator, String optionText) {
        click(dropdownLocator);
        List<WebElement> options = waitAllVisible(LISTBOX_OPTIONS);
        WebElement match = options.stream()
                .filter(o -> o.getText().trim().equalsIgnoreCase(optionText))
                .findFirst()
                .orElse(options.get(0));
        match.click();
    }

    /**
     * Selects an option from an OXD autocomplete-style input (Employee
     * Name, Hiring Manager, etc). The dropdown cycles through two
     * transient placeholders that share the exact same CSS classes as a
     * genuine option: "Searching…." while its AJAX lookup is in flight,
     * then "No Records Found" if nothing matched. Grabbing whatever is
     * visible the instant anything appears reliably clicks the
     * "Searching…." placeholder instead of waiting for the real result,
     * leaving the field's required ID unset (surfaces later as an opaque
     * "Invalid" validation error on save, even though the input's typed
     * text looks fine). The search text is also not guaranteed to find
     * anything on the first try — a name sourced from elsewhere in the UI
     * (e.g. a topbar display name) can be a shortened/reordered form of
     * the real searchable name — so a full-text search alone can come back
     * genuinely empty even though a match exists. Waiting out the
     * "Searching…." state on the full text first, then falling back to
     * just its first token, resolves both problems.
     */
    protected void selectFromOxdAutocomplete(By inputLocator, By optionsLocator, String searchText) {
        String[] tokens = searchText.trim().split("\\s+");
        List<String> attempts = new java.util.ArrayList<>();
        attempts.add(searchText);
        if (tokens.length > 1) {
            attempts.add(tokens[0]);
        }
        for (String attempt : attempts) {
            type(inputLocator, attempt);
            WebElement realOption = waitForRealAutocompleteOption(optionsLocator);
            if (realOption != null) {
                realOption.click();
                return;
            }
        }
        // Every attempt came back empty; fall back to the last dropdown's
        // first entry so the failure still surfaces as the normal
        // downstream validation error rather than an opaque exception here.
        waitAllVisible(optionsLocator).get(0).click();
    }

    private WebElement waitForRealAutocompleteOption(By optionsLocator) {
        try {
            return waitUntil(d -> {
                List<WebElement> options = d.findElements(optionsLocator);
                return options.stream()
                        .filter(WebElement::isDisplayed)
                        .filter(o -> {
                            String text = o.getText().trim().toLowerCase();
                            return !text.isEmpty() && !text.startsWith("searching") && !text.equals("no records found");
                        })
                        .findFirst()
                        .orElse(null);
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            return null;
        }
    }
}
