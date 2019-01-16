package com.playmonumenta.plugins.tracking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.player.PlayerInventory;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.LocationUtils.LocationType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class PlayerTracking implements EntityTracking {
	Plugin mPlugin = null;
	private HashMap<Player, PlayerInventory> mPlayers = new HashMap<Player, PlayerInventory>();

	PlayerTracking(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public void addEntity(Entity entity) {
		Player player = (Player)entity;

		// Initialize the player, either by loading data from disk or from the player
		PlayerData.initializePlayer(mPlugin, player);

		// Remove the metadata that prevents player from interacting with things (if present)
		player.removeMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY, mPlugin);

		// Remove the tag that prevents the spawn box from applying functions to the player
		player.removeScoreboardTag(Constants.PLAYER_MID_TRANSFER_TAG);

		// Load the players inventory / custom enchantments and apply them
		mPlayers.put(player, new PlayerInventory(mPlugin, player));
	}

	@Override
	public void removeEntity(Entity entity) {
		Player player = (Player)entity;

		// Add a scoreboard tag that prevents the spawn box from applying functions to the player
		player.addScoreboardTag(Constants.PLAYER_MID_TRANSFER_TAG);

		mPlayers.remove(player);
	}

	public Set<Player> getPlayers() {
		return mPlayers.keySet();
	}

	public void updateEquipmentProperties(Player player) {
		PlayerInventory manager = mPlayers.get(player);
		if (manager != null) {
			manager.updateEquipmentProperties(mPlugin, player);
		}
	}

	public void onAttack(Plugin plugin, Player player, LivingEntity target, EntityDamageByEntityEvent event) {
		PlayerInventory manager = mPlayers.get(player);
		if (manager != null) {
			manager.onAttack(plugin, player, target, event);
		}
	}

	public void onShootAttack(Plugin plugin, Player player, LivingEntity target, EntityDamageByEntityEvent event) {
		PlayerInventory manager = mPlayers.get(player);
		if (manager != null) {
			manager.onShootAttack(plugin, player, target, event);
		}
	}

	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent event) {
		PlayerInventory manager = mPlayers.get(player);
		if (manager != null) {
			manager.onExpChange(plugin, player, event);
		}
	}

	@Override
	public void update(World world, int ticks) {
		Iterator<Entry<Player, PlayerInventory>> playerIter = mPlayers.entrySet().iterator();
		while (playerIter.hasNext()) {
			Entry<Player, PlayerInventory> entry = playerIter.next();
			Player player = entry.getKey();
			PlayerInventory inventory = entry.getValue();

			boolean inSafeZone = false;
			boolean inCapital = false;
			boolean applyEffects = true;
			GameMode mode = player.getGameMode();

			if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
				Location location = player.getLocation();
				Point loc = new Point(location);

				// First we'll check if the player is too high, if so they shouldn't be here.
				if (loc.mY >= 255 && player.isOnGround()) {
					// Double check to make sure their on the ground as it can trigger a false positive.
					Block below = world.getBlockAt(location.subtract(0, 1, 0));
					if (below != null && below.getType() == Material.AIR) {
						continue;
					}

					PlayerUtils.awardStrike(mPlugin, player, "breaking rule #5, leaving the bounds of the map.");
				} else {
					LocationType zone = LocationUtils.getLocationType(mPlugin, player);
					inSafeZone = (zone != LocationType.None);
					inCapital = (zone == LocationType.Capital);
					applyEffects = (zone == LocationType.Capital || zone == LocationType.SafeZone);

					if (inSafeZone) {
						if (zone == LocationType.Capital) {
							Material mat = world.getBlockAt(location.getBlockX(), 10, location.getBlockZ()).getType();
							boolean neededMat = mat == Material.SPONGE || mat == Material.OBSIDIAN;

							if (mode == GameMode.SURVIVAL && !neededMat) {
								_transitionToAdventure(player);

							} else if (mode == GameMode.ADVENTURE && neededMat
							           && loc.mY > mPlugin.mServerProperties.getPlotSurvivalMinHeight()
							           && ScoreboardUtils.getScoreboardValue(player, "Apartment") == 0) {
								_transitionToSurvival(player);
							}
						} else {
							if (mode == GameMode.SURVIVAL) {
								_transitionToAdventure(player);
							}
						}
					}
				}

				// Give potion effects to those in a City;
				if (inSafeZone) {
					if (applyEffects) {
						if (inCapital) {
							mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CAPITAL_SPEED_EFFECT);
						} else {
							mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CITY_SPEED_MASK_EFFECT);
						}

						mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CITY_RESISTANCE_EFFECT);
						mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CITY_JUMP_MASK_EFFECT);
						mPlugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, Constants.CITY_SATURATION_EFFECT);
					}
				} else {
					if (mode == GameMode.ADVENTURE) {
						_transitionToSurvival(player);
					}
				}
			}

			// Extra Effects
			try {
				inventory.tick(mPlugin, world, player);
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				_updatePatreonEffects(player, world);
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

	void _transitionToAdventure(Player player) {
		player.setGameMode(GameMode.ADVENTURE);
	}

	void _transitionToSurvival(Player player) {
		player.setGameMode(GameMode.SURVIVAL);
	}

	private static final Particle.DustOptions RED_PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);

	// TODO: We should move this out of being ticked and into an event based system as well as store all
	// Patrons in a list so we're not testing against every player 4 times a second.
	// New Ability system would be great for this
	void _updatePatreonEffects(Player player, World world) {
		// Players in spectator do not have patreon particles
		if (player.getGameMode().equals(GameMode.SPECTATOR)) {
			return;
		}

		int patreon = ScoreboardUtils.getScoreboardValue(player, "Patreon");
		if (patreon > 0) {
			int shinyWhite = ScoreboardUtils.getScoreboardValue(player, "ShinyWhite");
			if (shinyWhite == 1 && patreon >= 5) {
				world.spawnParticle(Particle.SPELL_INSTANT, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}

			int shinyPurple = ScoreboardUtils.getScoreboardValue(player, "ShinyPurple");
			if (shinyPurple == 1 && patreon >= 10) {
				world.spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}

			int shinyGreen = ScoreboardUtils.getScoreboardValue(player, "ShinyGreen");
			if (shinyGreen == 1 && patreon >= 20) {
				world.spawnParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}

			int shinyRed = ScoreboardUtils.getScoreboardValue(player, "ShinyRed");
			if (shinyRed == 1 && patreon >= 30) {
				world.spawnParticle(Particle.REDSTONE, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, RED_PARTICLE_COLOR);
				return;
			}
		}
	}

	@Override
	public void unloadTrackedEntities() {
		Iterator<Entry<Player, PlayerInventory>> iter = mPlayers.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Player, PlayerInventory> entry = iter.next();
			Player player = entry.getKey();
			entry.getValue().removeProperties(mPlugin, player);
		}

		mPlayers.clear();
	}
}
