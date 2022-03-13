package com.playmonumenta.plugins.tracking;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.player.PlayerInventoryManager;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerDropItemEvent;

public class PlayerTracking implements EntityTracking {
	private static @Nullable PlayerTracking INSTANCE = null;

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
			if (player.getGameMode().equals(GameMode.SURVIVAL)
				&& ZoneUtils.hasZoneProperty(player, ZoneProperty.ADVENTURE_MODE)
				&& !ZoneUtils.isInPlot(player)) {
				player.setGameMode(GameMode.ADVENTURE);
			} else if (player.getGameMode().equals(GameMode.ADVENTURE)
				&& (!ZoneUtils.hasZoneProperty(player, ZoneProperty.ADVENTURE_MODE)
					|| ZoneUtils.isInPlot(player))) {
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
		Iterator<Entry<Player, PlayerInventoryManager>> playerIter = mPlayers.entrySet().iterator();
		while (playerIter.hasNext()) {
			Entry<Player, PlayerInventoryManager> entry = playerIter.next();
			Player player = entry.getKey();
			PlayerInventoryManager inventory = entry.getValue();

			GameMode mode = player.getGameMode();

			if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
				Location location = player.getLocation();
				Point loc = new Point(location);

				// First we'll check if the player is too high, if so they shouldn't be here.
				if (loc.mY >= 255 && (player.isOnGround() || player.isInsideVehicle())) {
					// Double check to make sure they're on the ground as it can trigger a false positive.
					Block below = player.getWorld().getBlockAt(location.subtract(0, 1, 0));
					if (below != null && below.getType() == Material.AIR) {
						continue;
					}

					PlayerUtils.awardStrike(mPlugin, player, "breaking rule #5, leaving the bounds of the map.");
				} else {
					if (ZoneUtils.hasZoneProperty(player, ZoneProperty.PLOTS_POSSIBLE)) {
						boolean isInPlot = ZoneUtils.inPlot(location, ServerProperties.getIsTownWorld());

						if (mode == GameMode.SURVIVAL && !isInPlot) {
							player.setGameMode(GameMode.ADVENTURE);
						} else if (mode == GameMode.ADVENTURE
							           && isInPlot
							           && loc.mY > ServerProperties.getPlotSurvivalMinHeight()
							           && ScoreboardUtils.getScoreboardValue(player, "TotalLevel").orElse(0) >= 5) {
							player.setGameMode(GameMode.SURVIVAL);
						}
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
			}

			try {
				mPlugin.mPotionManager.updatePotionStatus(player, ticks);
			} catch (Exception e) {
				e.printStackTrace();
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
