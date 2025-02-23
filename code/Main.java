import java.net.*;
import java.util.*;
import java.net.http.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Scanner;

public class Main {
    protected HashMap<String, Object> config;
    protected String token;
    protected WebSocket webSocket;
    public final HttpClient httpClient;
    public final AtomicBoolean running;
    public final Scanner scanner;
    public ConfigManager configManager;
    private AdvertiserModule advertiserModule;
    private OnlinerModule onlinerModule;
    private DMHandler dmHandler;

    // Initialise Main instance and required dependencies.
    public Main() {
        this.scanner = new Scanner(System.in);
        this.configManager = new ConfigManager();
        this.config = configManager.loadConfig();
        configManager.promptForMissingValues(config, scanner);
        this.token = (String) config.get("token");
        validateToken();
        this.httpClient = HttpClient.newHttpClient();
        this.running = new AtomicBoolean(true);
        this.advertiserModule = new AdvertiserModule(this);
        this.onlinerModule = new OnlinerModule(this);
        this.dmHandler = new DMHandler(this);
    }

    // Validate token using API call; repeatedly prompt until valid token provided.
    private void validateToken() {
        while (!isValidToken()) {
            token = getOrPromptForToken("Invalid token, enter your token: ");
            config.put("token", token);
        }
        configManager.saveConfig(config);
    }

    // Retrieve token from config or prompt user if missing.
    private String getOrPromptForToken(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().replace("\"", "").replace("'", "");
    }

    // Verify token validity by sending API request to Discord's endpoint.
    private boolean isValidToken() {
        try {
            URI uri = URI.create("https://discord.com/api/v10/users/@me");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", token);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // Present main menu options and handle user input for navigation.
    private void mainMenu() {
        while (true) {
            clearConsole();
            dmHandler.deleteDMs();
            System.out.println("\u001B[31mHome:\n\u001B[33m" +
                "1. Advertiser\n2. Onliner\n3. Leave");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    advertiserModule.displayMenu();
                    break;
                case "2":
                    onlinerModule.displayMenu();
                    break;
                case "3":
                    System.exit(0);
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    // Clear terminal display by resetting console output.
    public void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // Entry point: launch application and display main menu.
    public static void main(String[] args) {
        new Main().mainMenu();
    }

    // Process incoming direct messages and send appropriate responses.
    public void checkAndRespondToDMs() {
        dmHandler.checkAndRespondToDMs();
    }

    // Return instance of AdvertiserModule.
    public AdvertiserModule getAdvertiserModule() {
        return advertiserModule;
    }

    // Close open resources (i.e scanner) before garbage collection.
    @Override
    protected void finalize() {
        if (scanner != null) {
            scanner.close();
        }
    }
}
