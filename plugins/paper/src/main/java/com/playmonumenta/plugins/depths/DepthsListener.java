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
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.io.File;
import java.util.Map;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

	public DepthsListener() {
	}

	@EventHandler(ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		DepthsManager dm = DepthsManager.getInstance();

		if (!dm.isInSystem(player)) {
			return;
		}

		Block block = event.getBlock();
		Material type = block.getType();

		//Spawner break message
		if (type == Material.SPAWNER) {
			dm.playerBrokeSpawner(player, block.getLocation());

			//Sundrops logic
			//Check if anyone in their party has sundrops levels
			DepthsParty party = dm.getDepthsParty(player);
			if (party == null) {
				return;
			}
			int totalChance = 0;
			for (DepthsPlayer dp : party.mPlayersInParty) {
				if (dp == null || dp.mAbilities == null || dp.mAbilities.isEmpty()) {
					continue;
				}
				Integer sundropsLevel = dp.mAbilities.get(Sundrops.ABILITY_NAME);
				if (sundropsLevel != null && sundropsLevel > 0) {
					totalChance += Sundrops.DROP_CHANCE[sundropsLevel - 1];
				}
			}
			if (totalChance >= 100) {
				Sundrops.summonSundrop(block);
			} else if (totalChance > 0) {
				//Do the random roll
				Random r = new Random();
				int roll = r.nextInt(100) + 1;
				if (roll < totalChance) {
					Sundrops.summonSundrop(block);
				}
			}

		} else if (type == Material.CHEST) {
			//Player's can't break chests themselves
			event.setCancelled(true);
		}
	}

	//Enlightenment ability logic
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerExpChangeEvent(PlayerExpChangeEvent event) {
		Player player = event.getPlayer();
		DepthsManager dm = DepthsManager.getInstance();
		if (dm.isInSystem(player)) {
			double xpFactor = 1;

			//Check if anyone in their party has enlightenment levels
			DepthsParty party = dm.getDepthsParty(player);
			if (party == null) {
				return;
			}
			int highestLevel = 0;

			for (DepthsPlayer dp : party.mPlayersInParty) {
				if (dp == null || dp.mAbilities == null || dp.mAbilities.isEmpty()) {
					continue;
				}
				Integer enlightenmentLevel = dp.mAbilities.get(Enlightenment.ABILITY_NAME);
				if (enlightenmentLevel != null && enlightenmentLevel > highestLevel) {
					highestLevel = enlightenmentLevel;
				}
			}
			if (highestLevel > 0) {
				xpFactor *= Enlightenment.XP_MULTIPLIER[highestLevel - 1];
			}

			if (party.mEndlessMode) {
				xpFactor *= 0.5;
			}

			if (xpFactor != 1) {
				event.setAmount((int) Math.round(event.getAmount() * xpFactor));
			}
		}
	}

	//Logic to replace chest opening with a ability selection gui, if applicable
	@EventHandler(ignoreCancelled = true)
	public void onInventoryOpenEvent(InventoryOpenEvent e) {
		Player p = (Player) e.getPlayer();
		if (e.getInventory().getHolder() instanceof Chest || e.getInventory().getHolder() instanceof DoubleChest) {
			if (DepthsManager.getInstance().getRoomReward(p, e.getInventory().getLocation())) {
				e.setCancelled(true);
			}
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
		DepthsManager dm = DepthsManager.getInstance();
		DepthsPlayer dp = dm.getDepthsPlayer(event.getPlayer());

		if (dp != null) {
			DepthsParty party = dm.getPartyFromId(dp);
			if (party != null) {
				dp.mFinalTreasureScore = party.mTreasureScore;
				dp.setDeathRoom(party.getRoomNumber());
				event.getEntity().sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You have died! Your final treasure score is " + dp.mFinalTreasureScore + "!");
				event.getEntity().sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "You reached room " + party.mRoomNumber + "!");

				if (!party.mEndlessMode) {
					event.setKeepLevel(false);
					event.setDroppedExp(0);
					int keptXp = (int) (0.5 * ExperienceUtils.getTotalExperience(event.getPlayer()));
					int keptLevel = ExperienceUtils.getLevel(keptXp);
					event.setNewLevel(keptLevel);
					event.setNewExp(keptXp - ExperienceUtils.getTotalExperience(keptLevel));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerPostRespawnEvent(PlayerPostRespawnEvent event) {
		//Tp player to loot room when they respawn
		DepthsManager dm = DepthsManager.getInstance();
		Player player = event.getPlayer();
		DepthsPlayer dp = dm.getDepthsPlayer(player);
		if (dp != null) {
			DepthsUtils.storetoFile(dp, Plugin.getInstance().getDataFolder() + File.separator + "DepthsStats"); //Save the player's stats
			DepthsParty party = dm.getPartyFromId(dp);
			if (party != null) {
				party.populateLootRoom(player, party.mEndlessMode && party.mRoomNumber > 30);
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
		event.blockList().removeIf(block -> block.getType().equals(Material.STONE_BUTTON) && block.getRelative(BlockFace.EAST).getType() == Material.OBSIDIAN);
		if (entityType == EntityType.PRIMED_TNT) {
			event.blockList().removeIf(block -> block.getType().equals(Material.SPAWNER));
		}

		for (Block b : event.blockList()) {
			Material mat = b.getType();
			Location loc = b.getLocation();

			DepthsManager dm = DepthsManager.getInstance();
			Player p = EntityUtils.getNearestPlayer(loc, 100);
			if (p != null && dm.isInSystem(p)) {
				if (mat == Material.SPAWNER) { // count spawners exploded by creepers
					dm.playerBrokeSpawner(p, loc);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		DepthsParty party = DepthsManager.getInstance().getDepthsParty(event.getPlayer());
		if (party != null && party.getRoomNumber() % 10 == 0) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		DepthsManager manager = DepthsManager.getInstance();
		DepthsPlayer dp = manager.getDepthsPlayer(player);
		if (dp != null) {
			DepthsParty party = manager.getPartyFromId(dp);
			if (party != null) {
				Map<DelvesModifier, Integer> delvePointsForParty = party.mDelveModifiers;
				for (DelvesModifier m : DelvesModifier.values()) {
					DelvesUtils.setDelvePoint(null, player, ServerProperties.getShardName(), m, delvePointsForParty.getOrDefault(m, 0));

				}
			}
			dp.doOfflineTeleport();
		}
	}
}
