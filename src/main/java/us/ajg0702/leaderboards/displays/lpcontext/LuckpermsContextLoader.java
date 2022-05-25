package us.ajg0702.leaderboards.displays.lpcontext;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import us.ajg0702.leaderboards.LeaderboardPlugin;

public class LuckpermsContextLoader {
    public static void load(LeaderboardPlugin plugin) {
        if(!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return;

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            plugin.getLogger().warning("LuckPerms is enabled on the server, but cannot load it's api provider! Position context cannot be registered!");
            return;
        }

        LuckPerms api = provider.getProvider();

        api.getContextManager().registerCalculator(new PositionContext(plugin));

        plugin.getLogger().info("LuckPerms position context calculator registered!");
    }
}
