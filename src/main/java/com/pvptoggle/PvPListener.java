package com.pvptoggle;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class PvPListener implements Listener {

    private final PvPTogglePlugin plugin;
    private final PvPService pvpService;

    public PvPListener(PvPTogglePlugin plugin, PvPService pvpService) {
        this.plugin = plugin;
        this.pvpService = pvpService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        boolean attackerEnabled = pvpService.isPvpEnabled(attacker.getUniqueId());
        boolean victimEnabled = pvpService.isPvpEnabled(victim.getUniqueId());

        if (attackerEnabled && victimEnabled) {
            return;
        }

        event.setCancelled(true);

        if (!attackerEnabled) {
            MessageUtil.send(plugin, attacker, "attacker-disabled");
            return;
        }

        MessageUtil.send(plugin, attacker, "target-disabled-warning");
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }

        return null;
    }
}
