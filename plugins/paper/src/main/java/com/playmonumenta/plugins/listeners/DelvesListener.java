package com.playmonumenta.plugins.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.BoundingBox;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.delves.DelveModifier;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.DelveModifierSelectionGUI;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.delves.DelveModifier;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.DelveModifierSelectionGUI;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class DelvesListener implements Listener {

	private static final Set<String> DUNGEONS = new HashSet<>();

	static {
		DUNGEONS.add("white");
		DUNGEONS.add("orange");
		DUNGEONS.add("magenta");
		DUNGEONS.add("lightblue");
		DUNGEONS.add("yellow");
		DUNGEONS.add("willows");
		DUNGEONS.add("reverie");
		DUNGEONS.add("lime");
		DUNGEONS.add("pink");
		DUNGEONS.add("gray");
		DUNGEONS.add("lightgray");
		DUNGEONS.add("cyan");
		DUNGEONS.add("purple");
		DUNGEONS.add("teal");
		DUNGEONS.add("forum");
		DUNGEONS.add("shiftingcity");
		DUNGEONS.add("dev1");
		DUNGEONS.add("dev2");
		DUNGEONS.add("mobs");
		DUNGEONS.add("depths");
	}

	private static final String HAS_DELVE_MODIFIER_TAG = "DelveModifiersApplied";

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entitySpawnEvent(EntitySpawnEvent event) {
		if (!DUNGEONS.contains(ServerProperties.getShardName())) {
			return;
		}

		Entity entity = event.getEntity();

		/*
		 * Since this intercepts the CreatureSpawnEvent and SpawnerSpawnEvent,
		 * it seems that some modifiers get applied twice, which is annoying.
		 *
		 * Making sure that this only runs once per entity just future proofs
		 * it in case more events get added along the line that extend the
		 * EntitySpawnEvent.
		 */
		Set<String> tags = entity.getScoreboardTags();
		if (tags != null && tags.contains(HAS_DELVE_MODIFIER_TAG)) {
			return;
		}

		if (EntityUtils.isHostileMob(entity)) {
			Player player = getPlayerInDungeon(entity.getLocation());

			if (player != null) {
				entity.addScoreboardTag(HAS_DELVE_MODIFIER_TAG);

				// Check that all the delve modifiers are present on the player in the form of abilities before applying them
				boolean mismatch = false;
				for (Class<? extends DelveModifier> cls : DelvesUtils.getDelveInfo(player).getActiveModifiers()) {
					if (AbilityManager.getManager().getPlayerAbility(player, cls) == null) {
						mismatch = true;
						break;
					}
				}

				// Try refreshing class and rerunning the check if we found a mismatch
				if (mismatch) {
					AbilityManager.getManager().updatePlayerAbilities(player);

					for (Class<? extends DelveModifier> cls : DelvesUtils.getDelveInfo(player).getActiveModifiers()) {
						if (AbilityManager.getManager().getPlayerAbility(player, cls) == null) {
							// Reset delve points to prevent cases like broken modifiers but loot still being boosted
							DelvesUtils.setDelveScore(player, ServerProperties.getShardName(), 0);
							AbilityManager.getManager().updatePlayerAbilities(player);

							Plugin.getInstance().getLogger().log(Level.SEVERE, "Plugin thinks that " + player.getName() + " has delve modifier " + cls.getName() + " but not the corresponding ability.");
							MessagingUtils.sendRawMessage(player, "Something went wrong and your delve modifiers have been reset. Please ask a moderator for help reselecting your delve modifiers.");
							return;
						}
					}
				}

				// Actually apply modifiers now that we know they're all present
				for (Class<? extends DelveModifier> cls : DelvesUtils.getDelveInfo(player).getActiveModifiers()) {
					AbilityManager.getManager().getPlayerAbility(player, cls).applyOnSpawnModifiers((LivingEntity) entity, event);
				}
			}
		}
	}

	/*
	 * Gets a player in the same dungeon as the specified location.
	 * Code may change if we change how dungeons are instanced.
	 */
	private @Nullable Player getPlayerInDungeon(Location loc) {
		// Get a bounding box for the dungeon
		int xRegionMin = (((int) loc.getX()) >> 9) << 9;
		int zRegionMin = (((int) loc.getZ()) >> 9) << 9;
		BoundingBox box = new BoundingBox(xRegionMin, 0, zRegionMin, xRegionMin + 512, 256, zRegionMin + 512);

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.getGameMode().equals(GameMode.SPECTATOR)) {
				if (box.contains(player.getLocation().toVector())) {
					return player;
				}
			}
		}

		return null;
	}


	@EventHandler(ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		DelvesUtils.removeDelveInfo(player);
		GUI_MAPPINGS.remove(player.getUniqueId());
	}



	private static final Map<UUID, DelveModifierSelectionGUI> GUI_MAPPINGS = new HashMap<>();

	public static void openGUI(Player player, DelveModifierSelectionGUI gui) {
		GUI_MAPPINGS.put(player.getUniqueId(), gui);
		gui.openGUI();
	}

	public static void closeGUI(Player player) {
		GUI_MAPPINGS.remove(player.getUniqueId());
		player.closeInventory();
	}

	private @Nullable DelveModifierSelectionGUI getGUI(InventoryInteractEvent event) {
		HumanEntity entity = event.getWhoClicked();
		if (entity instanceof Player) {
			Player player = (Player) entity;

			DelveModifierSelectionGUI gui = GUI_MAPPINGS.get(player.getUniqueId());
			if (gui != null && gui.contains(event.getInventory())) {
				return gui;
			}
		}

		return null;
	}

	@EventHandler(ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		DelveModifierSelectionGUI gui = getGUI(event);
		if (gui != null) {
			gui.registerClick(event);
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void inventoryInteractEvent(InventoryInteractEvent event) {
		if (getGUI(event) != null) {
			event.setCancelled(true);
		}
	}
}
