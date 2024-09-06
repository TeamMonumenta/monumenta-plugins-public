package com.playmonumenta.plugins.hexfall;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.hexfall.CreepingDeath;
import com.playmonumenta.plugins.effects.hexfall.DeathImmunity;
import com.playmonumenta.plugins.effects.hexfall.DeathVulnerability;
import com.playmonumenta.plugins.effects.hexfall.InfusedLife;
import com.playmonumenta.plugins.effects.hexfall.LifeImmunity;
import com.playmonumenta.plugins.effects.hexfall.LifeVulnerability;
import com.playmonumenta.plugins.effects.hexfall.VoodooBindings;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class HexfallListener implements Listener {

	public HexfallListener() {
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Creeper creeper && creeper.getScoreboardTags().contains("boss_ruten")) {
			if (event.getPlayer().getEquipment().getItemInMainHand().getType() == Material.FLINT_AND_STEEL ||
				event.getPlayer().getEquipment().getItemInOffHand().getType() == Material.FLINT_AND_STEEL) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void blockPlaceEventMonitor(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();

		if (HexfallUtils.playerInBoss(player)) {
			decayBlock(block);
		}
	}


	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void structureGrowEventMonitor(StructureGrowEvent event) {
		Player player = event.getPlayer();

		if (player != null && HexfallUtils.playerInBoss(player)) {
			for (Block block : event.getBlocks().stream().map(BlockState::getBlock).toList()) {
				decayBlock(block);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityChangeBlockEvent(EntityChangeBlockEvent event) {
		Block block = event.getBlock();

		for (Player player : PlayerUtils.playersInRange(block.getLocation(), 100, true)) {
			if (HexfallUtils.playerInBoss(player)) {
				decayBlock(block);
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getPlayer();

		player.getWorld().getEntities().stream().filter(entity -> entity.getScoreboardTags().contains("DHFFlawless")).forEach(entity -> entity.removeScoreboardTag("DHFFlawless"));

		if (!event.isCancelled()) {
			Plugin plugin = Plugin.getInstance();
			plugin.mEffectManager.clearEffects(player, CreepingDeath.GENERIC_NAME);
			plugin.mEffectManager.clearEffects(player, InfusedLife.GENERIC_NAME);
			plugin.mEffectManager.clearEffects(player, VoodooBindings.GENERIC_NAME);
			plugin.mEffectManager.clearEffects(player, LifeVulnerability.GENERIC_NAME);
			plugin.mEffectManager.clearEffects(player, DeathVulnerability.GENERIC_NAME);
			plugin.mEffectManager.clearEffects(player, LifeImmunity.GENERIC_NAME);
			plugin.mEffectManager.clearEffects(player, DeathImmunity.GENERIC_NAME);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		event.getPlayer().getWorld().getEntities().stream().filter(entity -> entity.getScoreboardTags().contains("DHFFlawless")).forEach(entity -> entity.removeScoreboardTag("DHFFlawless"));
	}

	@EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityResurrectEvent(EntityResurrectEvent event) {
		if (event.getEntity() instanceof Player player) {
			event.setCancelled(true);
			player.sendMessage(Component.text("Hycenea prevents your own resurrection magic...", NamedTextColor.GRAY));
		}
	}

	private void decayBlock(Block block) {
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> block.setBlockData(Material.PURPLE_STAINED_GLASS.createBlockData()), 5 * 20);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> block.setBlockData(Material.AIR.createBlockData()), 10 * 20);
	}
}
