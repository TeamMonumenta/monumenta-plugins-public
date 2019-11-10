package com.playmonumenta.plugins.listeners;

import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import com.destroystokyo.paper.event.entity.EntityZapEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class MobListener implements Listener {
	static final int SPAWNER_DROP_THRESHOLD = 20;
	static final int ALCH_PASSIVE_RADIUS = 12;
	Random mRandom = new Random();

	Plugin mPlugin = null;

	public MobListener(Plugin plugin) {
		mPlugin = plugin;
	}
	@EventHandler(priority = EventPriority.HIGH)
	void CreatureSpawnEvent(CreatureSpawnEvent event) {
		Entity entity = event.getEntity();

		if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_WITHER ||
		    event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CURED ||
		    event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE) {
			event.setCancelled(true);
			return;
		}

		if (entity instanceof Slime && event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL)) {
			event.setCancelled(true);
			return;
		}

		// We need to allow spawning hostile mobs intentionally, but disable natural spawns.
		// It's easier to check the intentional ways than the natural ones.
		if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM &&
		    event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG &&
		    event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.DEFAULT &&
		    EntityUtils.isHostileMob(event.getEntity())) {
			LocationType locType = mPlugin.mSafeZoneManager.getLocationType(event.getEntity());
			if (locType.equals(LocationType.Capital) ||
			    locType.equals(LocationType.SafeZone)) {
				Location loc = entity.getLocation();

				// Cancel spawning unless this is from a dispenser in a plot
				if (!event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)
				    || !loc.getWorld().getBlockAt(loc.getBlockX(), 10, loc.getBlockZ()).getType().equals(Material.SPONGE)) {
					event.setCancelled(true);
					return;
				}
			}
		}

		if ((entity instanceof LivingEntity) && !(entity instanceof Player) && !(entity instanceof ArmorStand)) {
			LivingEntity mob = (LivingEntity)entity;

			// Mark mobs not able to pick-up items.
			mob.setCanPickupItems(false);

			// Overwrite drop chances for mob armor and held items
			EntityEquipment equipment = mob.getEquipment();
			if (equipment != null) {
				equipment.setHelmetDropChance(ItemUtils.getItemDropChance(equipment.getHelmet()));
				equipment.setChestplateDropChance(ItemUtils.getItemDropChance(equipment.getChestplate()));
				equipment.setLeggingsDropChance(ItemUtils.getItemDropChance(equipment.getLeggings()));
				equipment.setBootsDropChance(ItemUtils.getItemDropChance(equipment.getBoots()));
				equipment.setItemInMainHandDropChance(ItemUtils.getItemDropChance(equipment.getItemInMainHand()));
				equipment.setItemInOffHandDropChance(ItemUtils.getItemDropChance(equipment.getItemInOffHand()));
			}

			mPlugin.mZoneManager.applySpawnEffect(mPlugin, entity);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	void SpawnerSpawnEvent(SpawnerSpawnEvent event) {
		CreatureSpawner spawner = event.getSpawner();

		/* This can apparently happen sometimes...? */
		if (spawner == null) {
			return;
		}

		Entity mob = event.getEntity();
		int spawnCount = 1;

		if (spawner.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
			// There should only be one value - just use the latest one
			for (MetadataValue value : spawner.getMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
				// Previous value found - add one to it for the currently-spawning mob
				spawnCount = value.asInt() + 1;
			}
		}

		// Create new metadata entries
		spawner.setMetadata(Constants.SPAWNER_COUNT_METAKEY, new FixedMetadataValue(mPlugin, spawnCount));
		mob.setMetadata(Constants.SPAWNER_COUNT_METAKEY, new FixedMetadataValue(mPlugin, spawnCount));
	}

	/* Prevent fire from catching in towns */
	@EventHandler(priority = EventPriority.LOWEST)
	void BlockIgniteEvent(BlockIgniteEvent event) {
		if (event.isCancelled()) {
			// Don't waste time if cancelled somewhere else
			return;
		}

		Block block = event.getBlock();

		// If the block is within a safezone, cancel the ignition unless it was from a player in creative mode
		if (mPlugin.mSafeZoneManager.getLocationType(block.getLocation()) != LocationType.None) {
			if (event.getCause().equals(BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL)) {
				Player player = event.getPlayer();
				if (player != null && player.getGameMode() != GameMode.ADVENTURE) {
					// Don't cancel the event for non-adventure players
					return;
				}
			}

			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void EntityDeathEvent(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		boolean shouldGenDrops = true;

		// Check if this mob was likely spawned by a grinder spawner
		if (entity.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
			int spawnCount = 0;

			// There should only be one value - just use the latest one
			for (MetadataValue value : entity.getMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
				spawnCount = value.asInt();
			}

			if (spawnCount > SPAWNER_DROP_THRESHOLD) {
				shouldGenDrops = false;

				// Don't drop any exp
				event.setDroppedExp(0);

				// Remove all drops except special lore text items
				ListIterator<ItemStack> iter = event.getDrops().listIterator();
				while (iter.hasNext()) {
					if (ItemUtils.getItemDropChance(iter.next()) < 0) {
						iter.remove();
					}
				}
			}
		}

		if (entity instanceof LivingEntity) {
			LivingEntity livingEntity = (LivingEntity)entity;
			Player player = livingEntity.getKiller();
			if (player != null) {
				//  Player kills a mob
				mPlugin.mTrackingManager.mPlayers.onKill(mPlugin, player, entity, event);
				AbilityManager.getManager().EntityDeathEvent(player, event, shouldGenDrops);
				for (Player p : PlayerUtils.getNearbyPlayers(livingEntity.getLocation(), 20)) {
					AbilityManager.getManager().EntityDeathRadiusEvent(p, event, shouldGenDrops);
				}
			}

			//Do not run below if it is the death of a player
			if (livingEntity instanceof Player) {
				return;
			}
			//If the item has meta, run through the lore to check if it has quest item in the lore list
			ListIterator<ItemStack> iter = event.getDrops().listIterator();
			while (iter.hasNext()) {
				ItemStack item = iter.next();
				ItemMeta meta = item.getItemMeta();
				if (meta != null) {
					List<String> lore = meta.getLore();
					if (lore != null && !lore.isEmpty()) {
						for (String loreEntry : lore) {
							if (loreEntry.contains("Quest Item")) {
								//Scales based off player count in a 20 meter radius, drops at least one quest item
								int count = PlayerUtils.getNearbyPlayers(entity.getLocation(), 20).size();
								if (count < 1) {
									count = 1;
								}
								if (count > item.getAmount()) {
									item.setAmount(count);
								}
								return;
							}
						}
					}
				}
			}

		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void EntityZapEvent(EntityZapEvent event) {
		if (event.getEntityType().equals(EntityType.VILLAGER)) {
			event.setCancelled(true);
		}
	}
}
