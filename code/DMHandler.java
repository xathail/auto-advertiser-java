import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DMHandler {
    private final Main main;

    public DMHandler(Main main) {
        this.main = main;
    }
    
    // Fetches DM channels from Discord and processes each one for auto response.
    public void checkAndRespondToDMs() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/users/@me/channels"))
                .header("Authorization", main.token)
                .GET()
                .build();
            HttpResponse<String> response = main.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray channels = new JSONArray(response.body());
            for (int i = 0; i < channels.length(); i++) {
                JSONObject channel = channels.getJSONObject(i);
                if (channel.getInt("type") == 1) {
                    processChannel(channel);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Deletes DM channels from Discord using the API.
    public void deleteDMs() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/users/@me/channels"))
                .header("Authorization", main.token)
                .GET()
                .build();
            HttpResponse<String> response = main.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray channels = new JSONArray(response.body());

            for (int i = 0; i < channels.length(); i++) {
                JSONObject channel = channels.getJSONObject(i);
                if (channel.getInt("type") == 1) {
                    HttpRequest deleteRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://discord.com/api/v10/channels/" + channel.getString("id")))
                        .header("Authorization", main.token)
                        .DELETE()
                        .build();
                    main.httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Processes DM channel by fetching messages and sending response if needed.
    private void processChannel(JSONObject channel) {
        try {
            String channelId = channel.getString("id");
            HttpRequest messagesRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/channels/" + channelId + "/messages"))
                .header("Authorization", main.token)
                .GET()
                .build();
            HttpResponse<String> messagesResponse = main.httpClient.send(messagesRequest, HttpResponse.BodyHandlers.ofString());
            JSONArray messages = new JSONArray(messagesResponse.body());
            if (messages.length() > 0) {
                JSONObject lastMessage = messages.getJSONObject(0);
                if (!lastMessage.getJSONObject("author").getString("id").equals(getUserId())) {
                    sendDMResponse(channelId, lastMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Retrieves current user's Discord ID by querying Discord API.
    private String getUserId() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/users/@me"))
                .header("Authorization", main.token)
                .GET()
                .build();
            HttpResponse<String> response = main.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject user = new JSONObject(response.body());
            return user.getString("id");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    // Sends automated DM response and triggers webhook notification if configured.
    private void sendDMResponse(String channelId, JSONObject message) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/v10/channels/" + channelId + "/messages"))
                .header("Authorization", main.token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    new JSONObject().put("content", main.config.get("dmResponse")).toString()))
                .build();
            main.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (main.config.get("webhook") != null && !((String) main.config.get("webhook")).isEmpty()) {
                JSONObject author = message.getJSONObject("author");
                String notificationMessage = String.format("%s Auto DM Replied to %s#%s (%s)",
                    main.config.get("webhookPing"),
                    author.getString("username"),
                    author.getString("discriminator"),
                    author.getString("id"));
                main.getAdvertiserModule().sendWebhookMessage(notificationMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}