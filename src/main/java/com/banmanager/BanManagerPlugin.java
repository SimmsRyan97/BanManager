package com.banmanager;

import com.banmanager.command.BanCommandExecutor;
import com.banmanager.listener.CommandOverrideListener;
import com.banmanager.listener.ConnectionListener;
import com.banmanager.service.PlayerRepository;
import com.banmanager.service.PunishmentService;
import com.banmanager.service.RuleService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BanManagerPlugin extends JavaPlugin {

    private PlayerRepository playerRepository;
    private RuleService ruleService;
    private PunishmentService punishmentService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.playerRepository = new PlayerRepository(this);
        this.playerRepository.load();

        this.ruleService = new RuleService(this);
        this.ruleService.reload();

        this.punishmentService = new PunishmentService(this, playerRepository, ruleService);

        registerCommands();
        getServer().getPluginManager().registerEvents(new CommandOverrideListener(), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(punishmentService, playerRepository),
                this);

        getLogger().info("BanManager enabled.");
    }

    @Override
    public void onDisable() {
        if (playerRepository != null) {
            playerRepository.save();
        }
    }

    public void reloadAll() {
        reloadConfig();
        ruleService.reload();
        playerRepository.save();
    }

    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }

    public RuleService getRuleService() {
        return ruleService;
    }

    public PunishmentService getPunishmentService() {
        return punishmentService;
    }

    private void registerCommands() {
        BanCommandExecutor executor = new BanCommandExecutor(this, punishmentService, playerRepository, ruleService);
        bind("warn", executor);
        bind("kick", executor);
        bind("tempban", executor);
        bind("ban", executor);
        bind("ipban", executor);
        bind("unban", executor);
        bind("bmhistory", executor);
        bind("bmreload", executor);
    }

    private void bind(String commandName, BanCommandExecutor executor) {
        PluginCommand command = getCommand(commandName);
        if (command != null) {
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("Command missing from plugin.yml: " + commandName);
        }
    }
}
