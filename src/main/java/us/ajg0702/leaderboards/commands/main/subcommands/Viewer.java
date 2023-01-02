package us.ajg0702.leaderboards.commands.main.subcommands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.ajg0702.commands.CommandSender;
import us.ajg0702.commands.SubCommand;
import us.ajg0702.leaderboards.LeaderboardPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class Viewer extends SubCommand {
    private final LeaderboardPlugin plugin;
    public Viewer(LeaderboardPlugin plugin) {
        super("viewer", Collections.singletonList("webviewer"), null, "Export all board data to be viewed in the Web Viewer");
        this.plugin = plugin;
    }

    @Override
    public List<String> autoComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args, String label) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            JsonObject obj = plugin.getExporter().export(sender);
            if(obj == null) {
                sender.sendMessage(plugin.getMessages().getComponent("commands.export.fail"));
                return;
            }

            String data = obj.toString();

            sender.sendMessage(plugin.getMessages().getComponent("commands.viewer.uploading"));

            URL url;
            try {
                url = new URL("https://paste.ajg0702.us/post");
            } catch (MalformedURLException e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred while exporting to viewer:", e);
                sender.sendMessage(plugin.getMessages().getComponent("commands.export.fail"));
                return;
            }

            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", "AJLB-exporter/" + plugin.getDescription().getVersion());
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = data.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                try(BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    JsonObject responseJson = new Gson().fromJson(response.toString(), JsonObject.class);

                    sender.sendMessage(plugin.getMessages().getComponent(
                            "commands.viewer.success",
                            "URL:https://ajlb-viewer.ajg0702.us/#" + responseJson.get("key").getAsString()
                    ));
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "An error occurred while exporting to viewer:", e);
                sender.sendMessage(plugin.getMessages().getComponent("commands.export.fail"));
                return;
            }

        });
    }
}
