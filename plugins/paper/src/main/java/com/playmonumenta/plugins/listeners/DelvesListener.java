package com.playmonumenta.plugins.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
import org.bukkit.util.BoundingBox;

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.delves.DelveModifier;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.DelveModifierSelectionGUI;

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
		DUNGEONS.add("shiftingcity");
		DUNGEONS.add("dev1");
		DUNGEONS.add("dev2");
		DUNGEONS.add("mobs");
	}

	private static final String HAS_DELVE_MODIFIER_TAG = "DelveModifiersApplied";

	@EventHandler(priority = EventPriority.LOW)
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

		if (entity instanceof LivingEntity) {
			Player player = getPlayerInDungeon(entity.getLocation());

			if (player != null) {
				entity.addScoreboardTag(HAS_DELVE_MODIFIER_TAG);

				for (Class<? extends DelveModifier> cls : DelvesUtils.getDelveInfo(player).getActiveModifiers()) {
					/*
					 * If this ever grabs an ability the player doesn't have, it'll
					 * throw a NullPointerException, but if that's the case, there's
					 * a bigger problem anyways and it'll be better if the plugin
					 * stops doing things like setting loot tables and all.
					 */
					AbilityManager.getManager().getPlayerAbility(player, cls).applyOnSpawnModifiers((LivingEntity) entity, event);
				}
			}
		}
	}

	/*
	 * Gets a player in the same dungeon as the specified location.
	 * Code may change if we change how dungeons are instanced.
	 */
	private Player getPlayerInDungeon(Location loc) {
		// Get a bounding box for the dungeon
		int xRegionMin = (((int)loc.getX()) >> 9) << 9;
		int zRegionMin = (((int)loc.getZ()) >> 9) << 9;
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



	private static final Map<UUID, DelveModifierSelectionGUI> GUI_MAPPINGS = new HashMap<>();

	public static void openGUI(Player player, DelveModifierSelectionGUI gui) {
		GUI_MAPPINGS.put(player.getUniqueId(), gui);
		gui.openGUI();
	}

	public static void closeGUI(Player player) {
		GUI_MAPPINGS.remove(player.getUniqueId());
		player.closeInventory();
	}

	private DelveModifierSelectionGUI getGUI(InventoryInteractEvent event) {
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

	@EventHandler
	public void inventoryClickEvent(InventoryClickEvent event) {
		DelveModifierSelectionGUI gui = getGUI(event);
		if (gui != null) {
			gui.registerClick(event);
			event.setCancelled(true);
		}
    }

	@EventHandler
	public void inventoryInteractEvent(InventoryInteractEvent event) {
		if (getGUI(event) != null) {
			event.setCancelled(true);
		}
	}
}
