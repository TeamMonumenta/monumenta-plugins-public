package com.playmonumenta.plugins.depths;

import java.io.File;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Enlightenment;
import com.playmonumenta.plugins.depths.abilities.dawnbringer.Sundrops;
import com.playmonumenta.plugins.depths.abilities.earthbound.EarthenWrath;
import com.playmonumenta.plugins.depths.abilities.flamecaller.Pyromania;
import com.playmonumenta.plugins.depths.abilities.steelsage.FireworkBlast;
import com.playmonumenta.plugins.depths.abilities.steelsage.Metalmancy;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.event.PlayerSaveEvent;

public class DepthsListener implements Listener {
	Plugin mPlugin = null;

	public DepthsListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler
	public void blockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return;
		}
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
	@EventHandler(priority = EventPriority.LOW)
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
	@EventHandler
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

	@EventHandler(priority = EventPriority.LOW)
	public void entityDamageEvent(EntityDamageEvent event) {
		//Pyromania implementation handler
		if (!(event.getEntity() instanceof Player) && event.getCause() == DamageCause.FIRE_TICK && event.getEntity() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) event.getEntity();
			List<Player> playersToCheck = PlayerUtils.playersInRange(event.getEntity().getLocation(), Pyromania.RADIUS, true);
			double addedDamage = 0;
			for (Player p : playersToCheck) {
				DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(p.getUniqueId());

				if (dp != null) {
					Integer pyroLevel = dp.mAbilities.get(Pyromania.ABILITY_NAME);
					if (pyroLevel != null && pyroLevel.intValue() > 0) {
						addedDamage += Pyromania.FIRE_BONUS_DAMAGE[pyroLevel.intValue() - 1];
					}
				}
			}
			if (addedDamage > 0) {
				event.setDamage(event.getDamage() + addedDamage);
				entity.setNoDamageTicks(0);
			}
		}

		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			DepthsPlayer dp1 = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());
			if (dp1 == null || DepthsManager.getInstance().getPartyFromId(dp1) == null) {
				return;
			}
			DepthsParty party = DepthsManager.getInstance().getPartyFromId(dp1);
			// Extra damage taken at higher floors
			int floor = party.getFloor();
			if (floor > 15) {
				double multiplier = 1 + (0.1 * (((floor - 1) / 3) - 4));
			    event.setDamage(event.getDamage() * multiplier);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity entity = event.getEntity();
		Entity damager = event.getDamager();

		//Scaling boss damage- 5% extra per 3 floors
		if (EntityUtils.isBoss(damager) && entity instanceof Player && DepthsManager.getInstance().isInSystem((Player) entity)) {
			DepthsPlayer dp1 = DepthsManager.getInstance().mPlayers.get(entity.getUniqueId());
			if (DepthsManager.getInstance().getPartyFromId(dp1).getFloor() > 3) {
				double multiplier = 1 + (0.05 * (((DepthsManager.getInstance().getPartyFromId(dp1).getFloor() - 1) / 3)));
				event.setDamage(event.getDamage() * multiplier);
			}
		}

		//EarthenWrath implementation handler
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			DepthsPlayer dp1 = DepthsManager.getInstance().mPlayers.get(player.getUniqueId());
			if (dp1 == null || DepthsManager.getInstance().getPartyFromId(dp1) == null) {
				return;
			}
			try {
				if (dp1.mAbilities != null && dp1.mAbilities.get(EarthenWrath.ABILITY_NAME) != null && dp1.mAbilities.get(EarthenWrath.ABILITY_NAME).intValue() > 0) {
					AbilityManager.getManager().getPlayerAbility(player, EarthenWrath.class).damagedEntity(player, event);
				}
			} catch (Exception e) {
				Plugin.getInstance().getLogger().info("Exception for depths on entity damage- earthen wrath");
			}

			DepthsParty party = DepthsManager.getInstance().getPartyFromId(dp1);
			for (DepthsPlayer dp : party.mPlayersInParty) {
				if (dp == null || dp.mAbilities == null || dp.mAbilities.size() == 0) {
					continue;
				}

				Player p = Bukkit.getPlayer(dp.mPlayerId);
				try {
					if (dp != null && dp.mAbilities != null && dp.mAbilities.get(EarthenWrath.ABILITY_NAME) != null && dp.mAbilities.get(EarthenWrath.ABILITY_NAME).intValue() > 0 && p != null && p.isOnline() && !p.equals(player)) {
						AbilityManager.getManager().getPlayerAbility(p, EarthenWrath.class).damagedEntity(player, event);
						break;
					}
				} catch (Exception e) {
					Plugin.getInstance().getLogger().info("Exception for depths on entity damage- earthen wrath");
				}
			}
		}

		//Prevent plants from getting hit up
		if (entity.getName().contains("Dionaea")) {
			new BukkitRunnable() {
				@Override
				public void run() {
					entity.setVelocity(new Vector(0, 0, 0));
				}
			}.runTaskLater(mPlugin, 1);
		}

		//Metalmancy golem taunt
		if (entity instanceof Mob && damager instanceof IronGolem && damager.getScoreboardTags().contains(Metalmancy.GOLEM_TAG) && !EntityUtils.isBoss(entity)) {
			((Mob) entity).setTarget((IronGolem) damager);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void projectileHitEvent(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		ProjectileSource shooter = proj.getShooter();
		Entity damagee = event.getHitEntity();
		if (proj instanceof Firework && FireworkBlast.isDamaging((Firework) proj) && damagee instanceof Player) {
			// Firework Blast fireworks go through players
			event.setCancelled(true);
		} else if (damagee instanceof Slime && damagee.getName().contains("Eye") && shooter instanceof Player) {
			// Sound on shooting an eye
			((Player) shooter).playSound(((Player)shooter).getLocation(), Sound.ENTITY_BAT_HURT, 0.4f, 0.2f);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerDeathEvent(PlayerDeathEvent event) {
		//Set treasure score at death time, so they can't just wait around in death screen for party to get more rewards
		DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(event.getEntity().getUniqueId());

		if (dp != null && DepthsManager.getInstance().getPartyFromId(dp) != null) {
			dp.mFinalTreasureScore = DepthsManager.getInstance().getPartyFromId(dp).mTreasureScore;
			event.getEntity().sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You have died! Your final treasure score is " + dp.mFinalTreasureScore + "!");
			event.getEntity().sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You reached room " + DepthsManager.getInstance().getPartyFromId(dp).mRoomNumber + "!");
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerPostRespawnEvent(PlayerPostRespawnEvent event) {
		//Tp player to loot room when they respawn
		DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(event.getPlayer().getUniqueId());

		if (dp != null && DepthsManager.getInstance().getPartyFromId(dp) != null) {
			if (DepthsManager.getInstance().getPartyFromId(dp).mEndlessMode && DepthsManager.getInstance().getPartyFromId(dp).mRoomNumber > 30) {
				DepthsManager.getInstance().getPartyFromId(dp).populateLootRoom(event.getPlayer(), true);
			} else {
				DepthsManager.getInstance().getPartyFromId(dp).populateLootRoom(event.getPlayer(), false);
			}
		}
	}

	//Save player data on logout or shard crash, to be loaded on startup later
	@EventHandler
	public void playerSave(PlayerSaveEvent event) {

		//If they are in the system with an access score of zero, remove them from the system
		if (ScoreboardUtils.getScoreboardValue(event.getPlayer(), "DDAccess") == 0) {
			DepthsManager.getInstance().deletePlayer(event.getPlayer());
		}

		DepthsManager.getInstance().save(Plugin.getInstance().getDataFolder() + File.separator + "depths");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void entityExplodeEvent(EntityExplodeEvent event) {
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

	@EventHandler(priority = EventPriority.LOW)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		DepthsManager manager = DepthsManager.getInstance();
		if (manager.isInSystem(event.getPlayer())) {
			DepthsPlayer dp = manager.mPlayers.get(event.getPlayer().getUniqueId());
			if (manager.getPartyFromId(dp).getRoomNumber() % 10 == 0) {
				event.setCancelled(true);
			}
		}
	}
}
