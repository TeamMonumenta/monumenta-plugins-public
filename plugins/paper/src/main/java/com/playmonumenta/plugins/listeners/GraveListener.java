package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.Grave;
import com.playmonumenta.plugins.graves.GraveManager;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBTEntity;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraveListener implements Listener {
	Plugin mPlugin;

	private static final double KEPT_ITEM_DURABILITY_DAMAGE_PERCENT = 0.1;
	private static final List<Integer> KEEP_EQUIPPED_SLOTS = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 36, 37, 38, 39, 40);

	public GraveListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
		GraveManager.onAttemptPickupItem(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoin(PlayerJoinEvent event) {
		Grave.removeSummonListTag(event.getPlayer());
		GraveManager.onLogin(event.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void playerQuit(PlayerQuitEvent event) {
		GraveManager.onLogout(event.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void playerSave(PlayerSaveEvent event) {
		GraveManager.onSave(event);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerInteractEntity(PlayerInteractEntityEvent event) {
		if (GraveManager.isGrave(event.getRightClicked())) {
			GraveManager.onInteract(event.getPlayer(), event.getRightClicked());
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
		if (GraveManager.isGrave(event.getRightClicked())) {
			GraveManager.onInteract(event.getPlayer(), event.getRightClicked());
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerInteractAtEntity(PlayerInteractAtEntityEvent event) {
		if (GraveManager.isGrave(event.getRightClicked())) {
			GraveManager.onInteract(event.getPlayer(), event.getRightClicked());
			event.setCancelled(true);
		}
	}

	// handle cancelled events as we're only interested in the act of clicking/attacking the grave, and not whether the attack was successful
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	public void entityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player && event.getEntityType() == EntityType.ARMOR_STAND) {
			if (GraveManager.isGrave(event.getEntity())) {
				GraveManager.onInteract((Player) event.getDamager(), event.getEntity());
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void itemMerge(ItemMergeEvent event) {
		Item entity = event.getEntity();
		if (GraveManager.isGraveItem(entity) || GraveManager.isThrownItem(entity)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void chunkLoad(ChunkLoadEvent event) {
		GraveManager.onChunkLoad(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void chunkUnload(ChunkUnloadEvent event) {
		GraveManager.onChunkUnload(event);
	}

	// Fires whenever an item entity despawns due to time. Does not catch items that got killed in other ways.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void itemDespawnEvent(ItemDespawnEvent event) {
		GraveManager.onDestroyItem(event.getEntity());
	}

	// Fires any time any entity is deleted.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
		if (event.getEntity() instanceof Item) {
			// Check if an item entity was destroyed by the void.
			Item entity = (Item) event.getEntity();
			if (entity.getLocation().getY() <= -64) {
				GraveManager.onDestroyItem(entity);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityDamage(EntityDamageEvent event) {
		if (event.getEntityType() == EntityType.DROPPED_ITEM) {
			Item entity = (Item) event.getEntity();
			EntityDamageEvent.DamageCause cause = event.getCause();
			if (entity.isInvulnerable()
				|| (entity.getScoreboardTags().contains("ExplosionImmune")
				&& (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION))) {
				event.setCancelled(true);
				return;
			}
			GraveManager.onDestroyItem(entity);
		} else if (event.getEntityType() == EntityType.ARMOR_STAND) {
			if (GraveManager.isGrave(event.getEntity())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		Item entity = event.getItemDrop();
		ItemStack item = entity.getItemStack();
		if (item == null) {
			return;
		}

		ItemUtils.ItemDeathResult result = ItemUtils.getItemDeathResult(item);
		if (!player.getScoreboardTags().contains("DisableGraves")) {
			if (result == ItemUtils.ItemDeathResult.SHATTER || result == ItemUtils.ItemDeathResult.SHATTER_NOW
				|| result == ItemUtils.ItemDeathResult.SAFE || result == ItemUtils.ItemDeathResult.KEEP) {
				if (entity.getThrower().equals(player.getUniqueId())) {
					GraveManager.onDropItem(player, entity);
				} else {
					new BukkitRunnable() {
						@Override
						public void run() {
							if (entity.isValid()) {
								NBTEntity nbte = new NBTEntity(entity);
								if (nbte.getShort("Age") < 11999) {
									GraveManager.onDropItem(player, entity);
								}
							}
						}
					}.runTask(mPlugin);
				}
			}
		}
	}

	// An item on the player breaks.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerItemBreakEvent(PlayerItemBreakEvent event) {
		// If an item breaks, attempt to shatter it
		ItemStack item = event.getBrokenItem();
		if (ItemStatUtils.isShattered(item) || ItemStatUtils.shatter(item)) {
			// If the item shatters, drop it on the player with instant pickup, grave item if it couldn't be picked up.
			Player player = event.getPlayer();
			Location location = player.getLocation();
			Item droppedItem = player.getWorld().dropItemNaturally(location, item);
			droppedItem.setOwner(player.getUniqueId());
			droppedItem.setThrower(player.getUniqueId());
			droppedItem.setPickupDelay(0);
			// Allow other players to pick this up after 10s
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				if (droppedItem.isValid() && !droppedItem.isDead()) {
					droppedItem.setOwner(null);
				}
			}, 200);
			if (!player.getScoreboardTags().contains("DisableGraves")) {
				GraveManager.onDropItem(player, droppedItem);
			}
			player.sendMessage(ChatColor.RED + "An item shattered because it ran out of durability");
		}
	}

	// The player has died
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		PlayerInventory inv = player.getInventory();

		if (player.getHealth() > 0) {
			return;
		}

		if (event.getKeepInventory()) {
			//Remove Curse of Vanishing 2 Items even if Keep Inventory is on
			for (int slot = 0; slot <= 40; slot++) {
				ItemStack item = inv.getItem(slot);
				if (ItemUtils.isItemCurseOfVanishingII(item)) {
					inv.setItem(slot, null);
				}
			}
		}

		GraveManager.onDeath(player);

		if (!event.getKeepInventory()) {
			/* Monumenta-custom keep inventory
			 *
			 * Keep armor and hotbar items if they meet some conditions (_isKeptItemOnDeath)
			 *
			 * The player always gets keepinv set on them to prevent relog bugs - so items must
			 * be manually dropped here if they don't meet the conditions.
			 *
			 * Items dropped are invulnerable for a short while to prevent double-creepering
			 */

			// Effectively cancel vanilla item dropping to replace with our custom system
			event.setKeepInventory(true);
			event.setKeepLevel(false);
			event.getDrops().clear();

			Map<Integer, ItemStack> droppedItems = new HashMap<>();
			HashMap<EquipmentSlot, ItemStack> equipment = new HashMap<>();
			equipment.put(EquipmentSlot.HEAD, player.getInventory().getHelmet());
			equipment.put(EquipmentSlot.CHEST, player.getInventory().getChestplate());
			equipment.put(EquipmentSlot.LEGS, player.getInventory().getLeggings());
			equipment.put(EquipmentSlot.FEET, player.getInventory().getBoots());
			equipment.put(EquipmentSlot.HAND, player.getInventory().getItemInMainHand());
			equipment.put(EquipmentSlot.OFF_HAND, player.getInventory().getItemInOffHand());

			for (int slot = 0; slot <= 40; slot++) {
				ItemStack item = inv.getItem(slot);
				if (item == null || item.getType() == Material.AIR) {
					continue;
				}
				ItemUtils.ItemDeathResult result = ItemUtils.getItemDeathResult(item);
				if (result == ItemUtils.ItemDeathResult.DESTROY) {
					// Destroy item before it's even dropped
					inv.setItem(slot, null);
				} else if (result == ItemUtils.ItemDeathResult.KEEP_DAMAGED
					|| (result == ItemUtils.ItemDeathResult.KEEP_EQUIPPED && KEEP_EQUIPPED_SLOTS.contains(slot))) {
					// Item is kept, but damaged
					ItemUtils.damageItemPercent(item, KEPT_ITEM_DURABILITY_DAMAGE_PERCENT, false);
				} else if (result != ItemUtils.ItemDeathResult.KEEP
				           && result != ItemUtils.ItemDeathResult.KEEP_NOGRAVE) {
					droppedItems.put(slot, item);
					inv.setItem(slot, null);
				}
			}

			if (droppedItems.size() > 0) {
				if (player.getScoreboardTags().contains("DisableGraves")) {
					// Graves disabled, just drop items
					for (ItemStack item : droppedItems.values()) {
						player.getWorld().dropItemNaturally(player.getLocation(), item);
					}
				} else {
					// Generate a new grave
					GraveManager.onDeath(player, droppedItems, equipment);
				}
			}
		}
	}
}
