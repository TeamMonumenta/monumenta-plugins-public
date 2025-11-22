package com.playmonumenta.plugins.tracking;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.player.PlayerInventoryManager;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.HashMap;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class PlayerTracking implements EntityTracking {
	private static @MonotonicNonNull PlayerTracking INSTANCE = null;

	private final Plugin mPlugin;
	private final HashMap<Player, PlayerInventoryManager> mPlayers = new HashMap<>();

	PlayerTracking(Plugin plugin) {
		mPlugin = plugin;
		INSTANCE = this;
	}

	public static PlayerTracking getInstance() {
		return INSTANCE;
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity instanceof Player player) {
			/* Make sure the player is in the correct mode for where they logged in */
			GameMode currentGameMode = player.getGameMode();
			GameMode expectedGameMode = ZoneUtils.expectedGameMode(player);
			if (
				GameMode.SURVIVAL.equals(currentGameMode)
					&& GameMode.ADVENTURE.equals(expectedGameMode)
			) {
				player.setGameMode(GameMode.ADVENTURE);
			} else if (
				GameMode.ADVENTURE.equals(currentGameMode)
					&& GameMode.SURVIVAL.equals(expectedGameMode)
			) {
				player.setGameMode(GameMode.SURVIVAL);
			}

			// Load the players inventory / custom enchantments and apply them
			mPlayers.put(player, new PlayerInventoryManager(mPlugin, player));
		}
	}

	@Override
	public void removeEntity(Entity entity) {
		if (entity instanceof Player player) {
			mPlayers.remove(player);
		}
	}

	public Set<Player> getPlayers() {
		return mPlayers.keySet();
	}

	public void updateEquipmentProperties(Player player, @Nullable Event event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.updateEquipmentProperties(mPlugin, player, event);
		}
	}

	public void updateItemSlotProperties(Player player, int slot) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.updateItemSlotProperties(mPlugin, player, slot);
		}
	}

	@Override
	public void update(int ticks) {
		for (Player player : mPlayers.keySet()) {
			Location location = player.getLocation();
			updateLocation(player, location, ticks);
		}
	}

	public void updateLocation(Player player, Location location, int ticks) {
		GameMode mode = player.getGameMode();

		if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
			Point loc = new Point(location);

			// First we'll check if the player is too high, if so they shouldn't be here.
			boolean isOnGround = PlayerUtils.isOnGround(player);
			if (loc.mY >= location.getWorld().getMaxHeight() - 1 && (isOnGround || player.isInsideVehicle())) {
				// Double check to make sure they're on the ground as it can trigger a false positive.
				Block below = player.getWorld().getBlockAt(location.subtract(0, 1, 0));
				if (below.getType() != Material.AIR) {
					// Teleports the player to spawn, triggering a teleport event and re-running this method
					PlayerUtils.awardStrike(mPlugin, player, "breaking rule #5, leaving the bounds of the map.");
					return;
				}
			}

			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.SHOPS_POSSIBLE)) {
				GameMode expectedGameMode = ZoneUtils.expectedGameMode(player);

				if (mode == GameMode.SURVIVAL && expectedGameMode == GameMode.ADVENTURE) {
					player.setGameMode(GameMode.ADVENTURE);
				} else if (mode == GameMode.ADVENTURE && expectedGameMode == GameMode.SURVIVAL) {
					player.setGameMode(GameMode.SURVIVAL);
				}
			}

			// Give potion effects to those in a City;
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.SPEED_2)) {
				mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CAPITAL_SPEED_EFFECT);
			}
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.MASK_SPEED)) {
				mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CITY_SPEED_MASK_EFFECT);
			}
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.RESIST_5)) {
				mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CITY_RESISTANCE_EFFECT);
			}
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.SATURATION_2)) {
				mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CITY_SATURATION_EFFECT);
			}
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.MASK_JUMP_BOOST)) {
				mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CITY_JUMP_MASK_EFFECT);
			}

			// Give Conduit Power to those with their head in water in a Conduit Power Water zone
			if (ZoneUtils.hasZoneProperty(player, ZoneProperty.CONDUIT_POWER_WATER) && player.getEyeLocation().getBlock().getType() == Material.WATER) {
				EffectType.applyEffect(EffectType.VANILLA_CONDUIT, player, 5, 3, "Conduit Power Water Zone", false);
			}
		}

		try {
			mPlugin.mPotionManager.updatePotionStatus(player, ticks);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Calculate and apply anti speed if the player is in (enters) an anti speed zone.
		// This allows canceling speed gear, and is also recalculated in ItemStatManager when the player changes gear.
		// This here mostly handles applying/removing the penalty when entering/leaving the zone.
		if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.ANTI_SPEED)) {
			if (!EntityUtils.hasAttributesContaining(player, Attribute.GENERIC_MOVEMENT_SPEED, Constants.ANTI_SPEED_MODIFIER)) {
				PlayerUtils.cancelGearSpeed(player);
			}
		} else {
			// Remove anti speed, if the player has it.
			if (EntityUtils.hasAttributesContaining(player, Attribute.GENERIC_MOVEMENT_SPEED, Constants.ANTI_SPEED_MODIFIER)) {
				EntityUtils.removeAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, Constants.ANTI_SPEED_MODIFIER);
			}
		}
	}

	public boolean hasSlotChanged(Player player, int slot) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager == null) {
			return false;
		}
		return manager.hasSlotChanged(player, slot);
	}

	// Returns the first similar slot's number where there is a difference in item count, or -1 if not found
	public int getDroppedSlotId(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager == null) {
			return -1;
		}
		return manager.getDroppedSlotId(event);
	}

}
