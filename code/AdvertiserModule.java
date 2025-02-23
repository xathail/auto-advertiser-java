import java.util.ArrayList;
import java.util.Scanner;
import java.net.http.*;
import java.net.URI;
import org.json.JSONObject;
import java.time.Duration;
import java.util.concurrent.*;

public class AdvertiserModule {
    private final Main main;
    private final HttpClient httpClient;
    private final Scanner scanner;

    public AdvertiserModule(Main main) {
        // Initialise with Main dependencies.
        this.main = main;
        this.httpClient = main.httpClient;
        this.scanner = main.scanner;
    }

    // Display advertiser menu and route user actions.
    public void displayMenu() {
        while (true) {
            main.clearConsole();
            System.out.println("\u001B[31mAdvertiser:\n\u001B[33m" +
                "1. Start advertiser\n2. Add channel\n3. Remove channel\n" +
                "4. Change message\n5. Change delay\n6. Change DM Response\n" +
                "7. Auto DM Response\n8. Leave");
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": 
                    sendMessage(); 
                    break;
                case "2": 
                    modifyChannels("add"); 
                    break;
                case "3": 
                    modifyChannels("remove"); 
                    break;
                case "4": 
                    changeMessage(); 
                    break;
                case "5": 
                    changeDelay(); 
                    break;
                case "6": 
                    changeDMResponse(); 
                    break;
                case "7": 
                    new Thread(this::autoDMResponseLoop).start(); 
                    break;
                case "8": 
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    // Retrieves list of channels from config.
    private ArrayList<String> getChannels() {
        Object channelsObj = main.config.get("channels");
        if (!(channelsObj instanceof java.util.List<?>)) {
            ArrayList<String> list = new ArrayList<>();
            main.config.put("channels", list);
            return list;
        }
        java.util.List<?> raw = (java.util.List<?>) channelsObj;
        ArrayList<String> channels = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof String) {
                channels.add((String) o);
            }
        }
        return channels;
    }

    // Starts advertising process and listens for user interruption.
    private void sendMessage() {
        long startTime = System.currentTimeMillis();
        System.out.println("\u001B[31mPress 'Enter' to stop advertising!");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                while (!Thread.interrupted()) {
                    ArrayList<String> channels = getChannels();
                    if (!channels.isEmpty()) {
                        for (String channel : channels) {
                            if (channel != null && !channel.isEmpty()) {
                                postMessage(channel);
                                logMessage(channel, startTime);
                                Thread.sleep(500);
                            }
                        }
                    }
                    long delayMs = Long.parseLong((String) main.config.get("delay")) * 1000L;
                    Thread.sleep(delayMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        scanner.nextLine();
        future.cancel(true);
        executor.shutdown();
    }

    // Sends message to specific Discord channel.
    private void postMessage(String channel) {
        try {
            String messageContent = (String) main.config.get("message");
            // Append random bypass if repeat bypass is enabled.
            if ("y".equals(main.config.get("repeatBypass"))) {
                long bypass = (long) (Math.random() * 1e30);
                messageContent += "\n\n" + bypass;
            }
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/channels/" + channel + "/messages"))
                .header("Authorization", main.token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(new JSONObject().put("content", messageContent).toString()))
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.out.println("message sending interrupted");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Logs details of sent message and optionally triggers webhook notification.
    private void logMessage(String channel, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        Duration duration = Duration.ofMillis(elapsed);
        String elapsedStr = String.format("[%02d:%02d:%02ds]",
            duration.toHours(),
            duration.toMinutesPart(),
            duration.toSecondsPart());
        if (main.config.get("webhook") != null && !((String)main.config.get("webhook")).isEmpty()) {
            sendWebhookMessage(elapsedStr + " Sent message to channel <#" + channel + ">");
        }
    }

    // Sends webhook message based on provided content.
    public void sendWebhookMessage(String content) {
        try {
            String webhook = (String) main.config.get("webhook");
            String ping = (String) main.config.get("webhookPing");
            if (!ping.isEmpty()) {
                content = ping + " " + content;
            }
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhook))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new JSONObject().put("content", content).toString()))
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Adds or removes channel based on operation.
    private void modifyChannels(String operation) {
        System.out.print("Enter channel ID: ");
        String channel = scanner.nextLine().trim();
        ArrayList<String> channels = getChannels();
        if ("add".equals(operation)) {
            if (!channels.contains(channel)) {
                channels.add(channel);
                System.out.println("Channel " + channel + " added.");
            } else {
                System.out.println("Channel " + channel + " already exists.");
            }
        } else if ("remove".equals(operation)) {
            if (channels.remove(channel)) {
                System.out.println("Channel " + channel + " removed.");
            } else {
                System.out.println("Channel " + channel + " not found.");
            }
        }
        main.configManager.saveConfig(main.config);
    }

    // Prompts user to input new message and updates configuration.
    private void changeMessage() {
        System.out.print("Enter your message: ");
        String newMessage = scanner.nextLine().replace("\\n", "\n");
        main.config.put("message", newMessage);
        main.configManager.saveConfig(main.config);
        System.out.println("Message updated!");
    }

    // Prompts user to set new delay value (in seconds) for advertising.
    private void changeDelay() {
        System.out.print("Enter your delay (in seconds): ");
        String newDelay = scanner.nextLine().trim();
        try {
            if (Integer.parseInt(newDelay) > 0) {
                main.config.put("delay", newDelay);
                main.configManager.saveConfig(main.config);
                System.out.println("Delay updated!");
            } else {
                System.out.println("Delay must be positive!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
    }

    // Updates DM response configuration.
    private void changeDMResponse() {
        main.config.put("dmResponse", main.configManager.getOrPrompt(main.config, "dmResponse", "Enter your DM Autoresponse: ", scanner));
        main.configManager.saveConfig(main.config);
    }

    // Continuously checks for DMs and sends automatic responses.
    private void autoDMResponseLoop() {
        while (main.running.get()) {
            try {
                main.checkAndRespondToDMs();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
