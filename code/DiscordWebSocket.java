import org.json.JSONObject;
import org.json.JSONArray;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

public class DiscordWebSocket implements WebSocket.Listener {
    private final Main main;
    private StringBuilder messageBuffer;
    private boolean hasIdentified = false;

    // Sets up WebSocket listener instance for Main.
    public DiscordWebSocket(Main main) {
        this.main = main;
        this.messageBuffer = new StringBuilder();
    }
    
    // Called when WebSocket connection is established.
    @Override
    public void onOpen(WebSocket webSocket) {
        if (!hasIdentified) {
            System.out.println("websocket connected!");
            try {
                Thread.sleep(1000);
                JSONObject identify = new JSONObject()
                    .put("op", 2)
                    .put("d", new JSONObject()
                        .put("token", main.token)
                        .put("properties", new JSONObject()
                            .put("$os", "Windows")
                            .put("$browser", "Chrome")
                            .put("$device", ""))
                        .put("presence", new JSONObject()
                            .put("status", main.config.get("status"))
                            .put("afk", false)
                            .put("since", 0)
                            .put("activities", new JSONArray()
                                .put(new JSONObject()
                                    .put("type", 4)
                                    .put("state", main.config.get("customStatus"))
                                    .put("name", "Custom Status"))))
                        .put("intents", 513));
                // Send identification payload to Discord.
                webSocket.sendText(identify.toString(), true);
                hasIdentified = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        WebSocket.Listener.super.onOpen(webSocket);
    }
    
    // Handles incoming text messages and triggers heartbeat setup on receiving op code 10.
    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        // Append received data to buffer for complete message.
        messageBuffer.append(data);
        if (last) {
            String message = messageBuffer.toString();
            messageBuffer = new StringBuilder();
            try {
                JSONObject payload = new JSONObject(message);
                // If payload signals hello, start heartbeat.
                if (payload.getInt("op") == 10) {
                    int heartbeatInterval = payload.getJSONObject("d").getInt("heartbeat_interval");
                    startHeartbeat(webSocket, heartbeatInterval);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }
    
    // Initialises new thread to send periodic heartbeat messages.
    private void startHeartbeat(WebSocket webSocket, int interval) {
        new Thread(() -> {
            while (true) {
                try {
                    // Send heartbeat payload.
                    webSocket.sendText("{\"op\": 1,\"d\": null}", true);
                    Thread.sleep(interval);
                } catch (Exception e) {
                    break;
                }
            }
        }).start();
    }
    
    // Called when WebSocket connection is closed; resets identification flag.
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        System.out.println("websocket closed: " + reason);
        main.webSocket = null;
        hasIdentified = false;
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }
    
    // Handles errors encountered during WebSocket communication.
    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        System.out.println("websocket error: " + error.getMessage());
        WebSocket.Listener.super.onError(webSocket, error);
    }
    
    // Static helper method to initiate new WebSocket connection to Discord.
    public static WebSocket startWebSocket(Main main) {
        try {
            URI uri = URI.create("wss://gateway.discord.gg/?v=9&encoding=json");
            CompletableFuture<WebSocket> ws = main.httpClient.newWebSocketBuilder()
                .buildAsync(uri, new DiscordWebSocket(main));
            return ws.join();
        } catch (Exception e) {
            System.out.println("Failed to connect to WebSocket: " + e.getMessage());
            return null;
        }
    }
}
