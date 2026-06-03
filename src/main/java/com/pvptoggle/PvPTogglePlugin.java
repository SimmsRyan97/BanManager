package com.pvptoggle;

import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PvPTogglePlugin extends JavaPlugin {

    private PvPService pvpService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.pvpService = new PvPService(this);

        PluginCommand pvpCommand = getCommand("pvp");
        if (pvpCommand == null) {
            getLogger().severe("Command 'pvp' is missing from plugin.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PvPCommand executor = new PvPCommand(this, pvpService);
        pvpCommand.setExecutor(executor);
        pvpCommand.setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new PvPListener(this, pvpService), this);
    }
}
