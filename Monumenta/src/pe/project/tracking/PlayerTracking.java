package pe.project.tracking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.player.PlayerData;
import pe.project.player.PlayerInventory;
import pe.project.point.Point;
import pe.project.utils.LocationUtils;
import pe.project.utils.LocationUtils.LocationType;
import pe.project.utils.NetworkUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.PlayerUtils;
import pe.project.utils.ScoreboardUtils;

public class PlayerTracking implements EntityTracking {
	Plugin mPlugin = null;
	private HashMap<Player, PlayerInventory> mPlayers = new HashMap<Player, PlayerInventory>();

	PlayerTracking(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public void addEntity(Entity entity) {
		Player player = (Player)entity;
		try {
			mPlugin.mPotionManager.clearAllEffects(player);
			mPlugin.getClass(player).setupClassPotionEffects(player);

			PlayerData.loadPlayerData(mPlugin, player);
			PlayerData.removePlayerDataFile(mPlugin, player);
			mPlugin.mPotionManager.refreshClassEffects(player);
		} catch (Exception e) {
			mPlugin.getLogger().severe("Failed to load playerdata for player '" + player.getName() + "'");
			e.printStackTrace();

			player.sendMessage(ChatColor.RED + "Something very bad happened while transferring your player data.");
			player.sendMessage(ChatColor.RED + "  As a precaution, the server has attempted to move you to Purgatory.");
			player.sendMessage(ChatColor.RED + "  If for some reason you aren't on purgatory, take a screenshot and log off.");
			player.sendMessage(ChatColor.RED + "  Please post in #moderator-help and tag @admin");
			player.sendMessage(ChatColor.RED + "  Include details about what you were doing");
			player.sendMessage(ChatColor.RED + "  such as joining or leaving a dungeon (and which one!)");

			try {
				NetworkUtils.sendPlayer(mPlugin, player, "purgatory");
			} catch (Exception ex) {
				mPlugin.getLogger().severe("CRITICAL: Failed to send failed player '" + player.getName() + "' to purgatory");
				ex.printStackTrace();
			}

		}

		// Remove the metadata that prevents player from interacting with things (if present)
		player.removeMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY, mPlugin);

		// Remove the tag that prevents the spawn box from applying functions to the player
		player.removeScoreboardTag(Constants.PLAYER_MID_TRANSFER_TAG);

		mPlayers.put(player, new PlayerInventory(mPlugin, player));
	}

	@Override
	public void removeEntity(Entity entity) {
		Player player = (Player)entity;

		// Add a scoreboard tag that prevents the spawn box from applying functions to the player
		player.addScoreboardTag(Constants.PLAYER_MID_TRANSFER_TAG);

		PlayerData.removePlayerDataFile(mPlugin, player);

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

	public double onAttack(Plugin plugin, World world, Player player, LivingEntity target, double damage, DamageCause cause) {
		PlayerInventory manager = mPlayers.get(player);
		if (manager != null) {
			damage = manager.onAttack(plugin, world, player, target, damage, cause);
		}

		return damage;
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

					PlayerUtils.awardStrike(player, "breaking rule #5, leaving the bounds of the map.");
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

			// Extra Effects.
			inventory.tick(mPlugin, world, player);
			_updatePatreonEffects(player, world);

			mPlugin.mPotionManager.updatePotionStatus(player, ticks);
		}
	}

	void _transitionToAdventure(Player player) {
		player.setGameMode(GameMode.ADVENTURE);

		Entity vehicle = player.getVehicle();
		if (vehicle != null) {
			if (vehicle instanceof Boat) {
				vehicle.remove();
			}
		}
	}

	void _transitionToSurvival(Player player) {
		player.setGameMode(GameMode.SURVIVAL);

		mPlugin.getClass(player).setupClassPotionEffects(player);
	}

	// TODO: We should move this out of being ticked and into an event based system as well as store all
	// Patrons in a list so we're not testing against every player 4 times a second.
	void _updatePatreonEffects(Player player, World world) {
		int patreon = ScoreboardUtils.getScoreboardValue(player, "Patreon");
		if (patreon > 0) {
			int shinyWhite = ScoreboardUtils.getScoreboardValue(player, "ShinyWhite");
			if (shinyWhite == 1 && patreon >= 5) {
				ParticleUtils.playParticlesInWorld(world, Particle.SPELL_INSTANT, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}

			int shinyPurple = ScoreboardUtils.getScoreboardValue(player, "ShinyPurple");
			if (shinyPurple == 1 && patreon >= 10) {
				ParticleUtils.playParticlesInWorld(world, Particle.DRAGON_BREATH, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}

			int shinyGreen = ScoreboardUtils.getScoreboardValue(player, "ShinyGreen");
			if (shinyGreen == 1 && patreon >= 20) {
				ParticleUtils.playParticlesInWorld(world, Particle.VILLAGER_HAPPY, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}

			int shinyRed = ScoreboardUtils.getScoreboardValue(player, "ShinyRed");
			if (shinyRed == 1 && patreon >= 30) {
				ParticleUtils.playParticlesInWorld(world, Particle.REDSTONE, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
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
			PlayerData.removePlayerDataFile(mPlugin, player);
		}

		mPlayers.clear();
	}
}
