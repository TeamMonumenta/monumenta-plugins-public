package pe.project.listeners;

import java.util.ListIterator;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.utils.ItemUtils;
import pe.project.utils.LocationUtils;
import pe.project.utils.LocationUtils.LocationType;

public class MobListener implements Listener {
	static final int SPAWNER_DROP_THRESHOLD = 20;

	Plugin mPlugin = null;

	public MobListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH)
	void CreatureSpawnEvent(CreatureSpawnEvent event) {
		Entity entity = event.getEntity();
		if ((entity instanceof LivingEntity) && !(entity instanceof Player) && !(entity instanceof ArmorStand)) {
			LivingEntity mob = (LivingEntity)entity;

			//	Mark mobs not able to pick-up items.
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
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	void SpawnerSpawnEvent(SpawnerSpawnEvent event) {
		CreatureSpawner spawner = event.getSpawner();
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

	@EventHandler(priority = EventPriority.LOWEST)
	void BlockBreakEvent(BlockBreakEvent event) {
		if (!mPlugin.mItemOverrides.blockBreakInteraction(mPlugin, event.getPlayer(), event.getBlock())) {
			event.setCancelled(true);
		}
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
		if (LocationUtils.getLocationType(mPlugin, block.getLocation()) != LocationType.None) {
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
				//	Player kills a mob
				mPlugin.getClass(player).EntityDeathEvent(player, livingEntity,
														  entity.getLastDamageCause().getCause(),
														  shouldGenDrops);
			}
		}
	}
}
