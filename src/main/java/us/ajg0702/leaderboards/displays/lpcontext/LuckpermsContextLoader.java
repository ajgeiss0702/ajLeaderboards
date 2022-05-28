package us.ajg0702.leaderboards.displays.lpcontext;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import us.ajg0702.leaderboards.LeaderboardPlugin;

public class LuckpermsContextLoader {
    private final LeaderboardPlugin plugin;
    private boolean loaded = false;

    RegisteredServiceProvider<LuckPerms> provider;
    LuckPerms api;

    private PositionContext context;

    public LuckpermsContextLoader(LeaderboardPlugin leaderboardPlugin) {
        plugin = leaderboardPlugin;
        provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        api = provider == null ? null : provider.getProvider();
    }

    public void load() {
        if(!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            loaded = false;
            return;
        }

        if (provider == null || api == null) {
            plugin.getLogger().warning("LuckPerms is enabled on the server, but cannot load it's api provider! Position context cannot be registered!");
            loaded = false;
            return;
        }

        context = new PositionContext(plugin);
        api.getContextManager().registerCalculator(context);
        loaded = true;

        plugin.getLogger().info("LuckPerms position context calculator registered!");
    }

    public void checkReload() {
        boolean register = plugin.getAConfig().getBoolean("register-lp-contexts");
        if(loaded && !register) {
            api.getContextManager().unregisterCalculator(context);
            context = null;
        }
        if(!loaded && register) {
            load();
        }
    }

    public boolean isLoaded() {
        return loaded;
    }
}
