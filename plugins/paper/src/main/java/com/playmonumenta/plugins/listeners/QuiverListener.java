package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.ArrowConsumeEvent;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.inventories.CustomContainerItemGui;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
import com.playmonumenta.plugins.itemstats.enchantments.Multiload;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

/**
 * Handles quivers, which are shulker boxes for arrows.
 * Arrows are taken from them to shoot, and arrows being picked up are put in there before the inventory.
 */
public class QuiverListener implements Listener {

	public enum ArrowTransformMode {
		NONE("disabled", null),
		NORMAL("Normal Arrows", new ItemStack(Material.ARROW)),
		SPECTRAL("Spectral Arrows", new ItemStack(Material.SPECTRAL_ARROW)),
		// NB: don't change these enum constants' names, they are stored in quiver NBT and would break existing items when changed
		WEAKNESS("epic:items/arrows/ateoq_arrow"),
		SLOWNESS("epic:items/arrows/axcanyotl_arrow"),
		POISON("epic:items/arrows/tencualac_arrow"),
		;

		private final String mArrowName;

		private final @Nullable ItemStack mItemStack;

		ArrowTransformMode(String arrowName, @Nullable ItemStack itemStack) {
			mArrowName = arrowName;
			mItemStack = itemStack;
		}

		ArrowTransformMode(String lootTable) {
			ItemStack item = InventoryUtils.getItemFromLootTable(Bukkit.getWorlds().get(0).getSpawnLocation(), NamespacedKeyUtils.fromString(lootTable));
			mArrowName = ItemUtils.getPlainName(item);
			mItemStack = item;
		}

		// Using enum ordinal is the best way of handling this, and doesn't have any issues with persistence here.
		@SuppressWarnings("EnumOrdinal")
		ArrowTransformMode next(boolean reverse) {
			return values()[Math.floorMod(ordinal() + (reverse ? -1 : 1), values().length)];
		}

		public String getArrowName() {
			return mArrowName;
		}
	}

	public static CustomContainerItemManager.CustomContainerItemConfiguration getQuiverConfig(ItemStack quiver) {
		return new CustomContainerItemManager.CustomContainerItemConfiguration(quiver) {
			@Override
			public boolean canPutIntoContainer(ItemStack item) {
				return ItemUtils.isArrow(item)
					&& !ItemStatUtils.hasPlayerModified(item)
					&& !ItemStatUtils.isQuiver(item)
					&& !InventoryUtils.containsSpecialLore(item)
					&& !ItemUtils.isQuestItem(item);
			}

			@Override
			public boolean checkCanUse(Player player) {
				if (ItemStatUtils.isArrowTransformingQuiver(quiver) && !canUseArrowTransformingQuiver(player)) {
					player.sendMessage(Component.text("Ekah rejects your attempt to use the Shaman's Quiver!", NamedTextColor.RED));
					player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1, 1);
					return false;
				}
				return true;
			}

			@Override
			public void generateDescription(ReadableNBT monumenta, Consumer<Component> addLore) {
				ReadableNBT playerMod = monumenta.getCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
				if (playerMod == null) {
					return;
				}
				ReadableNBTList<ReadWriteNBT> items = playerMod.getCompoundList(ItemStatUtils.ITEMS_KEY);
				if (items == null) {
					return;
				}
				long amount = 0;
				for (ReadWriteNBT compound : items) {
					ReadWriteNBT tag = compound.getCompound("tag");
					if (tag == null) {
						continue;
					}
					ReadWriteNBT playerModified = ItemStatUtils.getPlayerModified(tag);
					if (playerModified == null) {
						continue;
					}
					amount += playerModified.getLong(CustomContainerItemManager.AMOUNT_KEY);
				}
				addLore.accept(Component.text("Contains ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
					.append(Component.text(amount + (mTotalItemsLimit > 0 ? "/" + mTotalItemsLimit : ""), NamedTextColor.WHITE))
					.append(Component.text(" arrows", NamedTextColor.GRAY)));
			}

			@Override
			public void createAdditionalGuiItems(ItemStack quiver, Gui gui) {
				if (ItemStatUtils.isArrowTransformingQuiver(quiver)) {
					ArrowTransformMode mode = ItemStatUtils.getArrowTransformMode(quiver);
					ItemStack icon = mode.mItemStack == null ? new ItemStack(Material.ARROW) : ItemUtils.clone(mode.mItemStack);
					ItemUtils.modifyMeta(icon, meta -> {
						meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
						if (mode == ArrowTransformMode.NONE) {
							meta.displayName(Component.text("Arrow transformation disabled", NamedTextColor.WHITE)
								.decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
						} else {
							meta.displayName(Component.text("Transform arrows to ", NamedTextColor.WHITE)
								.decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)
								.append(Component.text(mode.getArrowName(), NamedTextColor.GOLD)));
						}
						meta.lore(List.of(
							Component.text("Click to cycle through arrow transform modes.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
							Component.text("When enabled, picked up arrows", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
							Component.text("of transformable types with be transformed", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
							Component.text("into the selected variant.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
							Component.text("Press ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
								.append(Component.keybind("key.swapOffhand", NamedTextColor.WHITE))
								.append(Component.text(" to instantly transform", NamedTextColor.GRAY)),
							Component.text("all applicable arrows in the quiver.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
						));
					});
					gui.setItem(0, 5, icon)
						.onClick((event) -> {
							if (!CustomContainerItemManager.validateContainerItem(gui.mPlayer, quiver)) {
								return;
							}
							if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
								// Left or right click: cycle through transformation modes
								ArrowTransformMode newMode = mode.next(event.getClick() == ClickType.RIGHT);
								ItemStatUtils.setArrowTransformMode(quiver, newMode);
								ItemUpdateHelper.generateItemStats(quiver);
								gui.mPlayer.playSound(gui.mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 1, 1);
								gui.update();
							} else if (event.getClick() == ClickType.SWAP_OFFHAND && mode.mItemStack != null) {
								// Swap: transform all applicable arrows in the quiver
								NBT.modify(quiver, nbt -> {
									ReadWriteNBTCompoundList itemsList = ItemStatUtils.getItemList(nbt);
									ReadWriteNBT firstArrowPlayerModified = null;
									for (Iterator<ReadWriteNBT> it = itemsList.iterator(); it.hasNext(); ) {
										ReadWriteNBT compound = it.next();
										ItemStack containedItem = NBT.itemStackFromNBT(compound);
										if (containedItem == null) {
											continue;
										}
										NBT.modify(containedItem, inbt -> {
											ItemStatUtils.removePlayerModified(inbt);
										});
										if (Arrays.stream(ArrowTransformMode.values()).anyMatch(m -> containedItem.isSimilar(m.mItemStack))) {
											if (firstArrowPlayerModified == null) {
												// Modify the item in the quiver by modifying the transformed item copy, then overwriting the whole NBT
												ItemStack transformed = ItemUtils.clone(mode.mItemStack);
												NBT.modify(transformed, inbt -> {
													ItemStatUtils.addPlayerModified(inbt).mergeCompound(ItemStatUtils.addPlayerModified(compound.getOrCreateCompound("tag")));
												});
												compound.removeKey("tag");
												ReadWriteNBT newCompound = NBT.itemStackToNBT(transformed);
												if (newCompound == null) {
													continue;
												}
												compound.mergeCompound(newCompound);
												firstArrowPlayerModified = ItemStatUtils.addPlayerModified(compound.getOrCreateCompound("tag"));
											} else {
												// An arrow was transformed already: remove this item and increase the count of the transformed item
												it.remove();
												firstArrowPlayerModified.setLong(CustomContainerItemManager.AMOUNT_KEY,
													firstArrowPlayerModified.getLong(CustomContainerItemManager.AMOUNT_KEY)
														+ ItemStatUtils.addPlayerModified(compound.getOrCreateCompound("tag")).getLong(CustomContainerItemManager.AMOUNT_KEY));
											}
										}
									}
									// why is this needed
								});
								gui.mPlayer.playSound(gui.mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 1, 1);
								gui.update();
							}
						});
				}
			}
		};
	}

	private boolean mCallProjectileLaunchEvent = false;

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityShootBowEvent(EntityShootBowEvent event) {
		mCallProjectileLaunchEvent = false;
		if (event.getEntity() instanceof Player player
			&& ItemStatUtils.isQuiver(event.getConsumable())) {
			ItemStack quiver = event.getConsumable();
			boolean hasInfinity = ItemStatUtils.hasEnchantment(event.getBow(), EnchantmentType.INFINITY);
			Pair<ItemStack, Boolean> projectile = takeFromQuiver(player, quiver, 1, item -> {
				// infinity with normal arrows: don't consume
				if (hasInfinity && item.getType() == Material.ARROW) {
					return false;
				}
				ArrowConsumeEvent arrowConsumeEvent = new ArrowConsumeEvent(player, item);
				Bukkit.getPluginManager().callEvent(arrowConsumeEvent);
				return !arrowConsumeEvent.isCancelled();
			}, event.getBow());
			if (projectile == null) {
				event.setCancelled(true);
				return;
			}
			ItemStack projectileItem = projectile.getLeft();
			event.setConsumeItem(false);
			if (event.getProjectile() instanceof Arrow oldArrow) {
				// Create a new arrow entity, as the entity type may be different (i.e. when shooting a spectral arrow)
				Class<? extends AbstractArrow> arrowClass = projectileItem.getType() == Material.SPECTRAL_ARROW ? SpectralArrow.class : Arrow.class;
				AbstractArrow newArrow = oldArrow.getWorld().spawnArrow(oldArrow.getLocation(), oldArrow.getVelocity().normalize(),
					(float) oldArrow.getVelocity().length(), 0, arrowClass);
				event.setProjectile(newArrow);
				newArrow.setCritical(oldArrow.isCritical());
				newArrow.setDamage(oldArrow.getDamage());
				newArrow.setShooter(oldArrow.getShooter());
				newArrow.setShotFromCrossbow(oldArrow.isShotFromCrossbow());
				newArrow.setKnockbackStrength(oldArrow.getKnockbackStrength());
				newArrow.setFireTicks(oldArrow.getFireTicks());
				newArrow.setPickupStatus(projectile.getRight() ? AbstractArrow.PickupStatus.ALLOWED : AbstractArrow.PickupStatus.CREATIVE_ONLY);
				newArrow.setPierceLevel(oldArrow.getPierceLevel());

				if (newArrow instanceof Arrow newPotionArrow
					&& projectileItem.getItemMeta() instanceof PotionMeta itemMeta) {
					newPotionArrow.setBasePotionType(itemMeta.getBasePotionType());
					newPotionArrow.setColor(itemMeta.getColor());
					for (PotionEffect customEffect : itemMeta.getCustomEffects()) {
						newPotionArrow.addCustomEffect(customEffect, true);
					}
				}

				arrowSetItemWrapper(newArrow, projectileItem);

				// We need to call a new ProjectileLaunchEvent as we changed the projectile entity (which causes the event to no longer get fired automatically)
				// Delay to MONITOR to see if we actually shoot the projectile, and also allow the crossbow listener to add fire aspect and similar to the arrow
				mCallProjectileLaunchEvent = true;
			} else {
				event.setCancelled(true);
			}
			player.updateInventory();
		}
	}

	@SuppressWarnings("deprecation")
	public AbstractArrow arrowSetItemWrapper(AbstractArrow item, ItemStack projectileItem) {
		item.setItem(projectileItem);
		return item;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityShootBowEventMonitor(EntityShootBowEvent event) {
		if (mCallProjectileLaunchEvent) {
			ProjectileLaunchEvent newEvent = new ProjectileLaunchEvent(event.getProjectile());
			Bukkit.getPluginManager().callEvent(newEvent);
			if (newEvent.isCancelled()) {
				newEvent.getEntity().remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityLoadCrossbowEvent(EntityLoadCrossbowEvent event) {
		ItemStack crossbow = event.getCrossbow();
		if (event.getEntity() instanceof Player player && crossbow != null) {
			ItemStack quiver = NmsUtils.getVersionAdapter().getUsedProjectile(player, crossbow);
			if (ItemStatUtils.isQuiver(quiver)) {
				// Cancel the event as there's no way to change which projectile will be loaded
				event.setCancelled(true);
				int numProjectiles = 1 + ItemStatUtils.getEnchantmentLevel(crossbow, EnchantmentType.MULTILOAD);

				// Crossbows refund arrows when being shot instead of not consuming arrows when being loaded
				Pair<ItemStack, Boolean> projectile = takeFromQuiver(player, quiver, numProjectiles, is -> true, crossbow);
				if (projectile == null) {
					return;
				}
				ItemStack projectileItem = projectile.getLeft();
				int amount = projectileItem.getAmount();
				projectileItem.setAmount(1);
				if (numProjectiles > 1) {
					// multi-loading handles adding charged projectile
					Multiload.loadCrossbow(player, crossbow, projectileItem, numProjectiles, amount);
				} else {
					if (crossbow.getItemMeta() instanceof CrossbowMeta crossbowMeta) {
						crossbowMeta.addChargedProjectile(projectileItem);
						if (ItemStatUtils.hasEnchantment(crossbow, EnchantmentType.MULTISHOT)) {
							crossbowMeta.addChargedProjectile(ItemUtils.clone(projectileItem));
							crossbowMeta.addChargedProjectile(ItemUtils.clone(projectileItem));
						}
						crossbow.setItemMeta(crossbowMeta);
					}
				}
				// Sound copied from vanilla (won't play due to cancelled event)
				player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS,
					1.0F, 1.0F / (FastUtils.randomFloatInRange(0, 1) * 0.5F + 1.0F) + 0.2F);
			}
		}
	}

	private @Nullable Pair<ItemStack, Boolean> takeFromQuiver(Player player, ItemStack quiver, int numProjectiles, Predicate<ItemStack> consumePredicate, @Nullable ItemStack weapon) {
		if (quiver.getAmount() != 1) {
			player.sendMessage(Component.text("Cannot use stacked quivers!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1, 1);
			return null;
		}
		if (!getQuiverConfig(quiver).checkCanUse(player)) {
			return null;
		}
		String preferredArrowName = getPreferredArrowNameFromWeapon(weapon);
		if (preferredArrowName != null) {
			boolean requireFullAmount = numProjectiles > 1;
			if (!requireFullAmount || countInQuiver(quiver, preferredArrowName) >= numProjectiles) {
				Pair<ItemStack, Boolean> preferredResult = CustomContainerItemManager.removeFirstFromContainer(quiver, numProjectiles,
					item -> ItemUtils.isArrow(item) && ItemUtils.getPlainNameOrDefault(item).equals(preferredArrowName), consumePredicate);
				if (preferredResult != null) {
					return preferredResult;
				}
			}
		}
		Pair<ItemStack, Boolean> result = CustomContainerItemManager.removeFirstFromContainer(quiver, numProjectiles, ItemUtils::isArrow, consumePredicate);
		if (result == null) {
			player.sendMessage(Component.text("Your quiver is empty!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1, 1);
			return null;
		}
		return result;
	}

	private @Nullable String getPreferredArrowNameFromWeapon(@Nullable ItemStack weapon) {
		if (ItemUtils.isNullOrAir(weapon) || !ItemStatUtils.hasInfusion(weapon, InfusionType.AMMUNITION)) {
			return null;
		}
		String preferredArrowName = ItemStatUtils.getQuiverArrowPreferenceName(weapon);
		if (preferredArrowName == null || preferredArrowName.isBlank()) {
			return null;
		}
		return preferredArrowName;
	}

	private long countInQuiver(ItemStack quiver, String preferredArrowName) {
		return NBT.get(quiver, nbt -> {
			ReadableNBTList<ReadWriteNBT> itemsList = ItemStatUtils.getItemList(nbt);
			if (itemsList == null) {
				return 0L;
			}
			for (ReadWriteNBT compound : itemsList) {
				ItemStack containedItem = NBT.itemStackFromNBT(compound);
				if (containedItem == null) {
					continue;
				}
				NBT.modify(containedItem, ItemStatUtils::removePlayerModified);
				if (!ItemUtils.isArrow(containedItem) || !ItemUtils.getPlainNameOrDefault(containedItem).equals(preferredArrowName)) {
					continue;
				}
				ReadableNBT playerModified = ItemStatUtils.getPlayerModified(compound.getCompound("tag"));
				if (playerModified == null) {
					return 0L;
				}
				return playerModified.getLong(CustomContainerItemManager.AMOUNT_KEY);
			}
			return 0L;
		});
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPreDispenseEvent(BlockPreDispenseEvent event) {
		if (event.getBlock().getType() == Material.DISPENSER && ItemStatUtils.isQuiver(event.getItemStack())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		if (!(event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SWAP_OFFHAND)) {
			return;
		}
		if (!(event.getClickedInventory() instanceof PlayerInventory)) {
			return;
		}
		ItemStack item = event.getCurrentItem();
		if (ItemUtils.isNullOrAir(item) || item.getAmount() != 1) {
			return;
		}
		if (!isProjectileWeapon(item) || !ItemStatUtils.hasInfusion(item, InfusionType.AMMUNITION)) {
			return;
		}
		if (event.getClick() == ClickType.SWAP_OFFHAND) {
			ItemStatUtils.setQuiverArrowPreference(item, null);
			ItemUpdateHelper.generateItemStats(item);
			player.sendMessage(Component.text("Preferred arrow cleared.", NamedTextColor.GOLD));
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.8f, 0.9f);
			event.setCancelled(true);
			return;
		}
		ItemStack cursor = event.getCursor();
		if (!ItemUtils.isArrow(cursor) || ItemStatUtils.isQuiver(cursor)) {
			return;
		}

		ItemStatUtils.setQuiverArrowPreference(item, cursor);
		ItemUpdateHelper.generateItemStats(item);
		player.sendMessage(Component.text("Preferred arrow set to " + ItemUtils.getPlainNameOrDefault(cursor) + ".", NamedTextColor.GOLD));
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.8f, 1.2f);
		event.setCancelled(true);
	}

	private boolean isProjectileWeapon(ItemStack item) {
		return item.getType() == Material.BOW || item.getType() == Material.CROSSBOW;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerAttemptPickupItemEvent(PlayerAttemptPickupItemEvent event) {
		handlePickupEvent(event, event.getItem(), event.getPlayer());
	}

	// If an arrow is picked up, put it into a quiver if space is available
	private void handlePickupEvent(Cancellable event, Item item, Player player) {
		if (!item.isValid()) {
			return;
		}

		ItemStack itemStack = item.getItemStack();
		// PlayerAttemptPickupItemEvent runs 20 times a tick for one item entity if PickupDelay is set to 0/-1
		if (!ItemUtils.isArrow(itemStack) || !MetadataUtils.checkOnceInRecentTicks(Plugin.getInstance(), item, "QuiverPickupDelay" + player.getUniqueId(), 20)) {
			return;
		}

		if (!attemptPickup(player, itemStack)) {
			return;
		}

		if (itemStack.getAmount() == 0) {
			event.setCancelled(true);
			player.playPickupItemAnimation(item);
			item.remove();
		} else {
			item.setItemStack(itemStack);
		}
	}

	// If an arrow is picked up, put it into a quiver if space is available
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void playerPickupArrowEvent(PlayerPickupArrowEvent event) {
		Player player = event.getPlayer();
		AbstractArrow arrow = event.getArrow();
		ItemStack itemStack = arrow.getItemStack();
		// PlayerAttemptPickupItemEvent runs 20 times a tick for one item entity if PickupDelay is set to 0/-1
		if (!ItemUtils.isArrow(itemStack) || !MetadataUtils.checkOnceInRecentTicks(Plugin.getInstance(), arrow, "QuiverPickupDelay" + player.getUniqueId(), 20)) {
			return;
		}

		if (!attemptPickup(player, itemStack)) {
			return;
		}

		if (itemStack.getAmount() == 0) {
			event.setCancelled(true);
			event.setFlyAtPlayer(true);
			arrow.remove();
		} else {
			arrow.setItem(itemStack);
		}
	}

	public static boolean attemptPickup(Player player, ItemStack itemStack) {
		if (player.getGameMode() == GameMode.CREATIVE
			|| !ItemUtils.isArrow(itemStack)
			|| ItemStatUtils.getEnchantmentLevel(itemStack, EnchantmentType.THROWING_KNIFE) > 0) {
			return false;
		}
		for (ItemStack quiver : player.getInventory()) {
			if (!ItemStatUtils.isQuiver(quiver) || quiver.getAmount() != 1
				|| (ItemStatUtils.isArrowTransformingQuiver(quiver) && !canUseArrowTransformingQuiver(player))) {
				continue;
			}
			CustomContainerItemManager.CustomContainerItemConfiguration config = getQuiverConfig(quiver);
			if (!config.canPutIntoContainer(itemStack)) {
				continue;
			}

			ItemStack transformedItemStack = getTransformedArrowStack(quiver, itemStack);
			CustomContainerItemManager.addToContainer(player, quiver, config, transformedItemStack, true, true);
			itemStack.setAmount(transformedItemStack.getAmount());

			if (Gui.getOpenGui(player) instanceof CustomContainerItemGui gui && NmsUtils.getVersionAdapter().isSameItem(gui.getContainer(), quiver)) {
				// Update quiver GUI if it is open
				gui.update();
			}

			if (transformedItemStack.getAmount() == 0) {
				break;
			}
		}
		return true;
	}

	public static boolean canUseArrowTransformingQuiver(Player player) {
		return player.getScoreboardTags().contains("EkahKeyPaid");
	}

	public static ItemStack getTransformedArrowStack(ItemStack quiver, ItemStack arrows) {
		if (Arrays.stream(ArrowTransformMode.values()).noneMatch(m -> arrows.isSimilar(m.mItemStack))) {
			return arrows;
		}
		ArrowTransformMode mode = ItemStatUtils.getArrowTransformMode(quiver);
		if (mode.mItemStack == null) { // i.e. not transformed
			return arrows;
		}
		ItemStack transformed = ItemUtils.clone(mode.mItemStack);
		transformed.setAmount(arrows.getAmount());
		return transformed;
	}
}
