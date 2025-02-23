import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Scanner;

public class ConfigManager {
    // File representing config file path.
    private final File configFile = new File("config.json");

    // Loads config from file; creates file with defaults if missing.
    public HashMap<String, Object> loadConfig() {
        HashMap<String, Object> defaultConfig = getDefaultConfig();
        if (!configFile.exists()) {
            try {
                saveConfig(defaultConfig);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return defaultConfig;
        }
        try {
            String content = new String(Files.readAllBytes(configFile.toPath()));
            JSONObject json = new JSONObject(content);
            return jsonToMap(json);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultConfig;
        }
    }

    // Saves given config map to config file.
    public void saveConfig(HashMap<String, Object> config) {
        try {
            JSONObject json = new JSONObject(config);
            Files.write(Paths.get("config.json"), json.toString(4).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Provides default config values.
    private HashMap<String, Object> getDefaultConfig() {
        HashMap<String, Object> defaultConfig = new HashMap<>();
        defaultConfig.put("token", "");
        defaultConfig.put("message", "");
        defaultConfig.put("dmResponse", "");
        defaultConfig.put("delay", "10");
        defaultConfig.put("channels", new ArrayList<String>());
        defaultConfig.put("status", "online");
        defaultConfig.put("webhook", "");
        defaultConfig.put("webhookPing", "");
        defaultConfig.put("customStatus", "");
        defaultConfig.put("repeatBypass", "n");
        return defaultConfig;
    }

    // Converts JSONObject to HashMap.
    private HashMap<String, Object> jsonToMap(JSONObject json) {
        HashMap<String, Object> map = new HashMap<>();
        for (String key : json.keySet()) {
            Object value = json.get(key);
            if (value instanceof JSONArray) {
                map.put(key, new ArrayList<>(((JSONArray) value).toList()));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    // Prompts user to fill any missing config values.
    public void promptForMissingValues(HashMap<String, Object> config, Scanner scanner) {
        config.put("token", getOrPrompt(config, "token", "Enter your token: ", scanner));
        config.put("message", getOrPrompt(config, "message", "Enter message: ", scanner).replace("\\n", "\n"));
        config.put("dmResponse", getOrPrompt(config, "dmResponse", "Enter DM Autoresponse: ", scanner).replace("\\n", "\n"));
        config.put("delay", getOrPrompt(config, "delay", "Enter your delay (in seconds): ", scanner));
        config.put("status", getOrPrompt(config, "status", "Enter status (online, idle, dnd, invisible): ", scanner).toLowerCase());
        config.put("webhook", getOrPrompt(config, "webhook", "Enter webhook URL (leave blank if not needed): ", scanner));
        if (!((String)config.get("webhook")).isEmpty()) {
            config.put("webhookPing", getOrPrompt(config, "webhookPing", "Enter role to ping (leave blank if not needed): ", scanner));
        }
        config.put("customStatus", getOrPrompt(config, "customStatus", "Enter your custom status: ", scanner));
        config.put("repeatBypass", getOrPrompt(config, "repeatBypass", "Enable message repeat bypass? (y/n): ", scanner).toLowerCase());
        saveConfig(config);
    }

    // Retrieves configuration value or prompts user if missing.
    public String getOrPrompt(HashMap<String, Object> config, String key, String prompt, Scanner scanner) {
        String value = (String) config.get(key);
        if (value == null || value.isEmpty()) {
            System.out.print(prompt);
            return scanner.nextLine().replace("\"", "").replace("'", "");
        }
        return value;
    }
}
