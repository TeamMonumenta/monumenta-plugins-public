package com.playmonumenta.plugins.tracking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.BaseEnchantment;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.events.EvasionEvent;
import com.playmonumenta.plugins.player.PlayerInventoryManager;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class PlayerTracking implements EntityTracking {
	private static PlayerTracking INSTANCE = null;

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
		Player player = (Player)entity;

		/* Make sure the player is in the correct mode for where they logged in */
		boolean isTownWorld = ServerProperties.getIsTownWorld();
		if (player.getGameMode().equals(GameMode.SURVIVAL)
			&& ZoneUtils.hasZoneProperty(player, ZoneProperty.ADVENTURE_MODE)
			&& !ZoneUtils.inPlot(player, isTownWorld)) {
			player.setGameMode(GameMode.ADVENTURE);
		} else if (player.getGameMode().equals(GameMode.ADVENTURE)
			&& (!ZoneUtils.hasZoneProperty(player, ZoneProperty.ADVENTURE_MODE)
				|| ZoneUtils.inPlot(player, isTownWorld))) {
			player.setGameMode(GameMode.SURVIVAL);
		}

		// Load the players inventory / custom enchantments and apply them
		mPlayers.put(player, new PlayerInventoryManager(mPlugin, player));
	}

	@Override
	public void removeEntity(Entity entity) {
		Player player = (Player)entity;

		mPlayers.remove(player);
	}

	public Set<Player> getPlayers() {
		return mPlayers.keySet();
	}

	public JsonObject getAsJsonObject(Player player) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			return manager.getAsJsonObject();
		}
		return new JsonObject();
	}

	public int getPlayerCustomEnchantLevel(Player player, Class<? extends BaseEnchantment> cls) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			return manager.getEnchantmentLevel(mPlugin, cls);
		}
		return 0;
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

	public void onKill(Plugin plugin, Player player, Entity target, EntityDeathEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onKill(plugin, player, target, event);
		}
	}

	public void onAttack(Plugin plugin, Player player, LivingEntity target, EntityDamageByEntityEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onAttack(plugin, player, target, event);
		}
	}

	public void onDamage(Plugin plugin, Player player, LivingEntity target, EntityDamageByEntityEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onDamage(plugin, player, target, event);
		}
	}

	public void onAbility(Plugin plugin, Player player, LivingEntity target, CustomDamageEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onAbility(plugin, player, target, event);
		}
	}

	public void onLaunchProjectile(Plugin plugin, Player player, Projectile proj, ProjectileLaunchEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onLaunchProjectile(plugin, player, proj, event);
		}
	}

	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event, ItemStack item) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onBlockBreak(plugin, player, event, item);
		}
	}

	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onPlayerInteract(plugin, player, event);
		}
	}

	public void onDeath(Plugin plugin, Player player, PlayerDeathEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onDeath(plugin, player, event);
		}
	}

	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onExpChange(plugin, player, event);
		}
	}

	public void onHurt(Plugin plugin, Player player, EntityDamageEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onHurt(plugin, player, event);
		}
	}

	public void onHurtByEntity(Plugin plugin, Player player, EntityDamageByEntityEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onHurtByEntity(plugin, player, event);
		}
	}

	public void onFatalHurt(Plugin plugin, Player player, EntityDamageEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onFatalHurt(plugin, player, event);
		}
	}

	public void onEvade(Plugin plugin, Player player, EvasionEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onEvade(plugin, player, event);
		}
	}

	public void onConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {

			manager.onConsume(plugin, player, event);
		}
	}

	public void onItemDamage(Plugin plugin, Player player, PlayerItemDamageEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {

			manager.onItemDamage(plugin, player, event);
		}
	}

	public void onRegain(Plugin plugin, Player player, EntityRegainHealthEvent event) {
		PlayerInventoryManager manager = mPlayers.get(player);
		if (manager != null) {
			manager.onRegain(plugin, player, event);
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
						} else if (mode == GameMode.ADVENTURE && isInPlot
									&& loc.mY > ServerProperties.getPlotSurvivalMinHeight()
									&& ScoreboardUtils.getScoreboardValue(player, "Prestige").orElse(0) >= 3) {
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

			// Extra Effects
			try {
				inventory.tick(mPlugin, player);
			} catch (Exception e) {
				e.printStackTrace();
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

	@Override
	public void unloadTrackedEntities() {
		Iterator<Entry<Player, PlayerInventoryManager>> iter = mPlayers.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Player, PlayerInventoryManager> entry = iter.next();
			Player player = entry.getKey();
			entry.getValue().removeProperties(mPlugin, player);
		}

		mPlayers.clear();
	}
}
