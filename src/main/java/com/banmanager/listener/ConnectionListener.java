package com.banmanager.listener;

import com.banmanager.service.PlayerRepository;
import com.banmanager.service.PunishmentService;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class ConnectionListener implements Listener {

    private final PunishmentService punishmentService;
    private final PlayerRepository repository;

    public ConnectionListener(PunishmentService punishmentService, PlayerRepository repository) {
        this.punishmentService = punishmentService;
        this.repository = repository;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        InetAddress address = event.getAddress();
        String ip = address == null ? "" : address.getHostAddress();

        Optional<PlayerRepository.ActiveBan> activeBan = punishmentService.getActiveBanForLogin(uuid, ip);
        if (activeBan.isPresent()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    punishmentService.formatBanKickMessage(activeBan.get()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        InetSocketAddress address = event.getAddress() == null ? null : new InetSocketAddress(event.getAddress(), 0);
        punishmentService.updateIdentity(event.getPlayer(), address);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        String ip = "";
        if (event.getPlayer().getAddress() != null && event.getPlayer().getAddress().getAddress() != null) {
            ip = event.getPlayer().getAddress().getAddress().getHostAddress();
        }
        repository.updateIdentity(event.getPlayer().getUniqueId(), event.getPlayer().getName(), ip);
    }
}
