package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.GraveCommand;
import com.playmonumenta.plugins.graves.GraveManager;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import com.playmonumenta.scriptedquests.managers.TranslationsManager;
import de.tr7zw.nbtapi.NBTEntity;
import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.Nullable;

public class GraveListener implements Listener {

	private static final String INTERACT_METAKEY = "MonumentaGraveInteract";

	private final Plugin mPlugin;

	public GraveListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
		GraveManager.onAttemptPickupItem(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoin(PlayerJoinEvent event) {
		GraveCommand.removeSummonListTag(event.getPlayer());
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

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	public void playerInteractEntity(PlayerInteractEntityEvent event) {
		if (GraveManager.isGrave(event.getRightClicked())) {
			event.setCancelled(true);
			if (MetadataUtils.checkOnceThisTick(mPlugin, event.getPlayer(), INTERACT_METAKEY)) {
				GraveManager.onInteract(event.getPlayer(), event.getRightClicked());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	public void playerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
		if (GraveManager.isGrave(event.getRightClicked())) {
			event.setCancelled(true);
			if (MetadataUtils.checkOnceThisTick(mPlugin, event.getPlayer(), INTERACT_METAKEY)) {
				GraveManager.onInteract(event.getPlayer(), event.getRightClicked());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	public void playerInteractAtEntity(PlayerInteractAtEntityEvent event) {
		if (GraveManager.isGrave(event.getRightClicked())) {
			event.setCancelled(true);
			if (MetadataUtils.checkOnceThisTick(mPlugin, event.getPlayer(), INTERACT_METAKEY)) {
				GraveManager.onInteract(event.getPlayer(), event.getRightClicked());
			}
		}
	}

	// handle cancelled events as we're only interested in the act of clicking/attacking the grave, and not whether the attack was successful
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void entityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player player
			    && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
			    && GraveManager.isGrave(event.getEntity())
			    && MetadataUtils.checkOnceThisTick(mPlugin, player, INTERACT_METAKEY)) {
			event.setCancelled(true);
			GraveManager.onInteract(player, event.getEntity());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void itemMerge(ItemMergeEvent event) {
		Item entity = event.getEntity();
		if (GraveManager.isThrownItem(entity)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void chunkLoad(ChunkLoadEvent event) {
		GraveManager.onChunkLoad(event);
	}

	@EventHandler(ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		GraveManager.onEntityAddToWorld(event);
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
		if (event.getEntity() instanceof Item entity) {
			// Check if an item entity was destroyed by the void.
			if (entity.getLocation().getY() <= entity.getWorld().getMinHeight() - 64) {
				GraveManager.onDestroyItem(entity);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Item entity) {
			EntityDamageEvent.DamageCause cause = event.getCause();
			if ((entity.getScoreboardTags().contains("ExplosionImmune")
				     && (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION))) {
				event.setCancelled(true);
				return;
			}
			GraveManager.onDestroyItem(entity);
			if (entity.isValid()
				    && event.getCause() != EntityDamageEvent.DamageCause.VOID
				    && ItemStatUtils.getInfusionLevel(entity.getItemStack(), ItemStatUtils.InfusionType.HOPE) > 0) {
				// If a hoped item isn't put into a grave (because graves are disabled), cancel all non-void damage.
				event.setCancelled(true);
				entity.setInvulnerable(true); // also make the item invulnerable to prevent this event from being spammed
			}
		} else if (GraveManager.isGrave(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerDropItem(PlayerDropItemEvent event) {
		itemDropped(event.getPlayer(), event.getItemDrop());
	}

	public static void itemDropped(Player player, Item entity) {
		ItemStack item = entity.getItemStack();
		if (gravesEnabled(player)
			    && doesGrave(item)) {
			@Nullable UUID thrower = entity.getThrower();
			if (thrower != null && thrower.equals(player.getUniqueId())) {
				GraveManager.onDropItem(player, entity);
			} else {
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					if (entity.isValid()) {
						NBTEntity nbte = new NBTEntity(entity);
						if (nbte.getShort("Age") < 11999) {
							GraveManager.onDropItem(player, entity);
						}
					}
				});
			}
		}
	}

	/**
	 * Returns whether an items will grave if dropped and destroyed.
	 */
	public static boolean doesGrave(ItemStack item) {
		return switch (ItemStatUtils.getTier(item)) {
			case NONE, ZERO, I, II, III, KEYTIER, QUEST_COMPASS -> false;
			default -> true;
		};
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		// If a tiered item breaks, shatter it instead of destroying it
		ItemStack item = event.getItem();
		if (item.getItemMeta() instanceof Damageable meta
			    && event.getDamage() + meta.getDamage() >= item.getType().getMaxDurability()
			    && ItemStatUtils.getTier(item) != ItemStatUtils.Tier.NONE) {
			event.setCancelled(true);
			meta.setDamage(item.getType().getMaxDurability());
			item.setItemMeta(meta);
			if (Shattered.shatter(item, Shattered.DURABILITY_SHATTER)) {
				Component itemName = ItemUtils.getDisplayName(item).decoration(TextDecoration.UNDERLINED, false);
				String translatedMessage = TranslationsManager.translate(event.getPlayer(), "Your %s shattered because it ran out of durability!");
				Component message = Component.text(translatedMessage).color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
					                    .replaceText(TextReplacementConfig.builder().matchLiteral("%s").replacement(itemName).build());
				event.getPlayer().sendMessage(message);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerItemBreakEvent(PlayerItemBreakEvent event) {
		ItemStack item = event.getBrokenItem();
		if (ItemStatUtils.getTier(item) != ItemStatUtils.Tier.NONE) {
			MMLog.warning("Reached PlayerItemBreakEvent for tiered item " + ItemUtils.getPlainName(item) + "!");
			item.setAmount(item.getAmount() + 1);
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
				if (ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.CURSE_OF_VANISHING) >= 2) {
					inv.setItem(slot, null);
				}
			}
		}

		if (!event.getKeepInventory()) {
			/* Monumenta-custom keep inventory
			 *
			 * Keep inventory (but not levels), and equipped items get one level of Shatter.
			 */

			event.setKeepInventory(true);
			event.setKeepLevel(false);
			event.getDrops().clear();

			// Handle curse of vanishing
			ItemStack[] items = inv.getContents();
			for (int i = 0; i < items.length; i++) {
				ItemStack item = items[i];
				if (item == null) {
					continue;
				}
				int vanishing = ItemStatUtils.getEnchantmentLevel(item, ItemStatUtils.EnchantmentType.CURSE_OF_VANISHING);
				if (vanishing >= 2) {
					inv.setItem(i, null);
				} else if (vanishing == 1 || item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
					Shattered.shatter(item, Shattered.CURSE_OF_VANISHING_SHATTER);
				}
			}

			// Equipment for the grave. This must be after curse of vanishing, but before death shattering to have proper shattered levels on the items.
			HashMap<EquipmentSlot, ItemStack> equipment = new HashMap<>();
			equipment.put(EquipmentSlot.HEAD, ItemUtils.clone(player.getInventory().getHelmet()));
			equipment.put(EquipmentSlot.CHEST, ItemUtils.clone(player.getInventory().getChestplate()));
			equipment.put(EquipmentSlot.LEGS, ItemUtils.clone(player.getInventory().getLeggings()));
			equipment.put(EquipmentSlot.FEET, ItemUtils.clone(player.getInventory().getBoots()));
			equipment.put(EquipmentSlot.HAND, ItemUtils.clone(player.getInventory().getItemInMainHand()));
			equipment.put(EquipmentSlot.OFF_HAND, ItemUtils.clone(player.getInventory().getItemInOffHand()));

			// Shatter equipment
			EntityDamageEvent.DamageCause lastDamageCause = player.getLastDamageCause() != null ? player.getLastDamageCause().getCause() : null;
			int shatterLevels = player.getLocation().getY() < player.getWorld().getMinHeight() || lastDamageCause == EntityDamageEvent.DamageCause.VOID
				                    ? Shattered.DEATH_VOID_SHATTER
				                    : EntityUtils.touchesLava(player) || lastDamageCause == EntityDamageEvent.DamageCause.LAVA
					                      ? Shattered.DEATH_LAVA_SHATTER
					                      : Shattered.DEATH_SHATTER;
			for (int i = 36; i <= 40; i++) {
				ItemStack item = items[i];
				if (item == null || ItemStatUtils.getTier(item) == ItemStatUtils.Tier.NONE) {
					continue;
				}
				Shattered.shatter(item, ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.HOPE) > 0 ? Math.max(1, shatterLevels - 1) : shatterLevels);
			}

			if (gravesEnabled(player)) {
				// Generate a new grave if necessary
				GraveManager.onDeath(player, equipment);
			}

		}
	}

	public static boolean gravesEnabled(Player player) {
		return !player.getScoreboardTags().contains("DisableGraves")
			       && !ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.DISABLE_GRAVES);
	}
}
