package us.ajg0702.leaderboards.displays.lpcontext;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import us.ajg0702.leaderboards.LeaderboardPlugin;

public class WithLPCtx extends LuckpermsContextLoader {

    RegisteredServiceProvider<LuckPerms> provider;
    LuckPerms api;

    PositionContext context;

    public WithLPCtx(LeaderboardPlugin leaderboardPlugin) {
        super(leaderboardPlugin);
        provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        api = provider == null ? null : provider.getProvider();
    }

    @Override
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

    @Override
    public void checkReload(boolean register) {
        if(loaded && !register && context != null) {
            api.getContextManager().unregisterCalculator(context);
            context = null;
        }
        if(!loaded && register) {
            load();
        }

        if(context != null) {
            context.reloadConfig();
        }
    }

    @Override
    public void calculatePotentialContexts() {
        if(!loaded || context == null) return;
        context.calculatePotentialContexts();
    }
}
