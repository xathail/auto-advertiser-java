import java.util.Scanner;

public class OnlinerModule {
    private final Main main;
    private final Scanner scanner;

    // Initialise OnlinerModule with Main instance and shared Scanner.
    public OnlinerModule(Main main) {
        this.main = main;
        this.scanner = main.scanner;
    }
    
    // Displays onliner menu and routes user input.
    public void displayMenu() {
        while (true) {
            main.clearConsole();
            System.out.println("\u001B[31mOnliner:\n\u001B[33m" +
                "1. Start onliner\n2. Change status\n3. Change custom status\n4. Leave");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": 
                    // Start onliner thread.
                    new Thread(this::onlinerLoop).start(); 
                    break;
                case "2": 
                    changeStatus(); 
                    break;
                case "3": 
                    changeCustomStatus(); 
                    break;
                case "4": 
                    return;
                default: 
                    System.out.println("Invalid choice");
            }
        }
    }
    
    // Runs onliner loop, ensuring websocket is active.
    private void onlinerLoop() {
        System.out.println("\u001B[31mOnliner started! Press 'Enter' to return to menu (onliner will keep running)");
        while (main.running.get()) {
            try {
                if (main.webSocket == null) {
                    // Establish websocket connection if not connected.
                    main.webSocket = DiscordWebSocket.startWebSocket(main);
                }
                if (System.in.available() > 0) {
                    // Exit loop on user input.
                    scanner.nextLine();
                    return;
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    // Prompts user to change online status and updates configuration.
    private void changeStatus() {
        String oldStatus = (String) main.config.get("status");
        String status;
        do {
            System.out.print("Enter status (online, idle, dnd, invisible): ");
            status = scanner.nextLine().toLowerCase().trim();
        } while (!("online".equals(status) || "idle".equals(status) || "dnd".equals(status) || "invisible".equals(status)));
        main.config.put("status", status);
        main.configManager.saveConfig(main.config);
        if (!status.equals(oldStatus)) {
            updateStatus();
        }
    }
    
    // Prompts user to update custom status and applies the change.
    private void changeCustomStatus() {
        System.out.print("Enter your custom status: ");
        String customStatus = scanner.nextLine().trim();
        main.config.put("customStatus", customStatus);
        main.configManager.saveConfig(main.config);
        updateStatus();
    }
    
    // Sends an update status payload via websocket; reconnects if necessary.
    public void updateStatus() {
        try {
            String status = (String) main.config.get("status");
            String customStatus = (String) main.config.get("customStatus");
            
            org.json.JSONObject payload = new org.json.JSONObject()
                .put("op", 3)
                .put("d", new org.json.JSONObject()
                    .put("since", 0)
                    .put("activities", new org.json.JSONArray()
                        .put(new org.json.JSONObject()
                            .put("type", 4)
                            .put("state", customStatus)
                            .put("name", "Custom Status")))
                    .put("status", status)
                    .put("afk", false));

            if (main.webSocket != null) {
                try {
                    main.webSocket.sendText(payload.toString(), true);
                    System.out.println("Status updated successfully!");
                } catch (Exception e) {
                    System.out.println("WebSocket not connected, establishing new connection...");
                    main.webSocket = DiscordWebSocket.startWebSocket(main);
                    Thread.sleep(1000);
                    if (main.webSocket != null) {
                        main.webSocket.sendText(payload.toString(), true);
                        System.out.println("Status updated successfully!");
                    }
                }
            } else {
                System.out.println("Connecting to WebSocket...");
                main.webSocket = DiscordWebSocket.startWebSocket(main);
                Thread.sleep(1000);
                if (main.webSocket != null) {
                    main.webSocket.sendText(payload.toString(), true);
                    System.out.println("Status updated successfully!");
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to update status: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
