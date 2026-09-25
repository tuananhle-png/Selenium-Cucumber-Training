package base;

import java.util.HashMap;
import java.util.Map;

/**
 * Scenario-scoped bag shared across step-definition classes via Cucumber's
 * PicoContainer dependency injection. Holds data created in one step (e.g. a
 * generated employee name) that a later step in the same scenario needs.
 */
public class TestContext {

    private final Map<String, Object> data = new HashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }
}
