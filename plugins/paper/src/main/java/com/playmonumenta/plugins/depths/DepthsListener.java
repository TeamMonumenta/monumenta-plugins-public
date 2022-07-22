package com.playmonumenta.plugins.depths;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Enlightenment;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Sundrops;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenWrath;
import com.playmonumenta.plugins.depths.abilities.steelsage.FireworkBlast;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.io.File;
import java.util.Map;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.projectiles.ProjectileSource;

public class DepthsListener implements Listener {
	Plugin mPlugin = null;

	public DepthsListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();

		if (player == null) {
			return;
		}

		//Spawner break message
		if (DepthsManager.getInstance().isInSystem(player) && event.getBlock().getType() == Material.SPAWNER) {
			DepthsManager.getInstance().playerBrokeSpawner(player, event.getBlock().getLocation());

			//Sundrops logic
			//Check if anyone in their party has sundrops levels
			DepthsParty party = DepthsManager.getInstance().getPartyFromId(DepthsManager.getInstance().mPlayers.get(player.getUniqueId()));
			int totalChance = 0;
			for (DepthsPlayer dp : party.mPlayersInParty) {
				if (dp == null || dp.mAbilities == null || dp.mAbilities.size() == 0) {
					continue;
				}
				Integer sundropsLevel = dp.mAbilities.get(Sundrops.ABILITY_NAME);
				if (sundropsLevel != null && sundropsLevel.intValue() > 0) {
					totalChance += Sundrops.DROP_CHANCE[sundropsLevel.intValue() - 1];
				}
			}
			if (totalChance >= 100) {
				Sundrops.summonSundrop(event.getBlock().getLocation().add(0.5, 0, 0.5));
			} else if (totalChance > 0) {
				//Do the random roll
				Random r = new Random();
				int roll = r.nextInt(100) + 1;
				if (roll < totalChance) {
					Sundrops.summonSundrop(event.getBlock().getLocation().add(0.5, 0, 0.5));
				}
			}


		} else if (DepthsManager.getInstance().isInSystem(player) && event.getBlock().getType() == Material.CHEST) {
			//Player's can't break chests themselves
			event.setCancelled(true);
		}
	}

	//Enlightenment ability logic
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerExpChangeEvent(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();
		if (DepthsManager.getInstance().isInSystem(player)) {
			//Check if anyone in their party has enlightenment levels
			DepthsParty party = DepthsManager.getInstance().getPartyFromId(DepthsManager.getInstance().mPlayers.get(player.getUniqueId()));
			int highestLevel = 0;

			for (DepthsPlayer dp : party.mPlayersInParty) {
				if (dp == null || dp.mAbilities == null || dp.mAbilities.size() == 0) {
					continue;
				}
				Integer enlightenmentLevel = dp.mAbilities.get(Enlightenment.ABILITY_NAME);
				if (enlightenmentLevel != null && enlightenmentLevel.intValue() > highestLevel) {
					highestLevel = enlightenmentLevel.intValue();
				}
			}
			if (highestLevel > 0) {
				event.setAmount((int) (event.getAmount() * Enlightenment.XP_MULTIPLIER[highestLevel - 1]));
			}
		}
	}

	//Logic to replace chest opening with a ability selection gui, if applicable
	@EventHandler(ignoreCancelled = true)
	public void onInventoryOpenEvent(InventoryOpenEvent e) {
	   Player p = (Player) e.getPlayer();
	   if (e.getInventory().getHolder() instanceof Chest || e.getInventory().getHolder() instanceof DoubleChest) {
		   Boolean guiLoaded = DepthsManager.getInstance().getRoomReward(p, e.getInventory().getLocation());
		   if (guiLoaded == false) {
			   return;
		   }
		   e.setCancelled(true);
	   }
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void damageEvent(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		LivingEntity source = event.getSource();

		if (damagee instanceof Player player) {
			DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());
			if (dp == null) {
				return;
			}
			DepthsParty party = DepthsManager.getInstance().getPartyFromId(dp);
			if (party == null) {
				return;
			}

			// Handle earthen wrath damage
			EarthenWrath.handleDamageEvent(event, player, party);

			// Extra damage taken at higher floors
			int floor = party.getFloor();
			if (floor > 15) {
				double multiplier = 1 + (0.1 * (((floor - 1) / 3) - 4));
			    event.setDamage(event.getDamage() * multiplier);
			}
			if (source != null && EntityUtils.isBoss(source) && floor > 3) {
				double multiplier = 1 + (0.05 * ((floor - 1) / 3));
			    event.setDamage(event.getDamage() * multiplier);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void projectileHitEvent(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		ProjectileSource shooter = proj.getShooter();
		Entity damagee = event.getHitEntity();
		if (proj instanceof Firework firework && FireworkBlast.isDamaging(firework) && damagee instanceof Player) {
			// Firework Blast fireworks go through players
			event.setCancelled(true);
		} else if (damagee instanceof Slime && damagee.getName().contains("Eye") && shooter instanceof Player player) {
			// Sound on shooting an eye
			player.playSound(player.getLocation(), Sound.ENTITY_BAT_HURT, 0.4f, 0.2f);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		//Set treasure score at death time, so they can't just wait around in death screen for party to get more rewards
		DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(event.getEntity().getUniqueId());

		if (dp != null) {
			DepthsParty party = DepthsManager.getInstance().getPartyFromId(dp);
			if (party != null) {
				dp.mFinalTreasureScore = party.mTreasureScore;
				dp.setDeathRoom(party.getRoomNumber());
				event.getEntity().sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You have died! Your final treasure score is " + dp.mFinalTreasureScore + "!");
				event.getEntity().sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You reached room " + party.mRoomNumber + "!");
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerPostRespawnEvent(PlayerPostRespawnEvent event) {
		//Tp player to loot room when they respawn
		DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(event.getPlayer().getUniqueId());
		DepthsUtils.storetoFile(dp, Plugin.getInstance().getDataFolder() + File.separator + "DepthsStats"); //Save the player's stats
		if (dp != null && DepthsManager.getInstance().getPartyFromId(dp) != null) {
			if (DepthsManager.getInstance().getPartyFromId(dp).mEndlessMode && DepthsManager.getInstance().getPartyFromId(dp).mRoomNumber > 30) {
				DepthsManager.getInstance().getPartyFromId(dp).populateLootRoom(event.getPlayer(), true);
			} else {
				DepthsManager.getInstance().getPartyFromId(dp).populateLootRoom(event.getPlayer(), false);
			}
		}
	}

	//Save player data on logout or shard crash, to be loaded on startup later
	@EventHandler(ignoreCancelled = true)
	public void playerSave(PlayerSaveEvent event) {

		//If they are in the system with an access score of zero, remove them from the system
		if (ScoreboardUtils.getScoreboardValue(event.getPlayer(), "DDAccess").orElse(0) == 0) {
			DepthsManager.getInstance().deletePlayer(event.getPlayer());
		}

		DepthsManager.getInstance().save(Plugin.getInstance().getDataFolder() + File.separator + "depths");
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		//Call util to check for ice barrier slow
		DepthsUtils.explodeEvent(event);
		EntityType entityType = event.getEntityType();
		if (!(entityType == EntityType.CREEPER || entityType == EntityType.PRIMED_TNT)) {
			return;
		}

		event.blockList().removeIf(block -> block.getType().equals(Material.CHEST));
		event.blockList().removeIf(block -> block.getType().equals(Material.STONE_BUTTON) && block.getLocation().add(1, 0, 0).getBlock().getType() == Material.OBSIDIAN);
		if (entityType == EntityType.PRIMED_TNT) {
			event.blockList().removeIf(block -> block.getType().equals(Material.SPAWNER));
		}

		for (Block b : event.blockList()) {
			Material mat = b.getType();
			Location loc = b.getLocation();

			Player p = EntityUtils.getNearestPlayer(loc, 100);
			if (p != null && DepthsManager.getInstance().isInSystem(p)) {
				if (mat == Material.SPAWNER) { // count spawners exploded by creepers
					if (p == null || !DepthsManager.getInstance().isInSystem(p)) {
						return;
					}
					DepthsManager.getInstance().playerBrokeSpawner(p, loc);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		DepthsManager manager = DepthsManager.getInstance();
		if (manager.isInSystem(event.getPlayer())) {
			DepthsPlayer dp = manager.mPlayers.get(event.getPlayer().getUniqueId());
			if (manager.getPartyFromId(dp).getRoomNumber() % 10 == 0) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		DepthsManager manager = DepthsManager.getInstance();
		if (manager.isInSystem(player)) {
			DepthsPlayer dp = manager.mPlayers.get(event.getPlayer().getUniqueId());
			DepthsParty party = manager.getPartyFromId(dp);
			if (party != null) {
				Map<DelvesModifier, Integer> delvePointsForParty = party.mDelveModifiers;
				for (DelvesModifier m : DelvesModifier.values()) {
					DelvesUtils.setDelvePoint(null, player, ServerProperties.getShardName(), m, delvePointsForParty.getOrDefault(m, 0));

				}
			}
			if (dp.mOfflineTeleportLoc != null) {
				player.teleport(dp.mOfflineTeleportLoc);
				dp.mOfflineTeleportLoc = null;
			}
		}
	}
}
