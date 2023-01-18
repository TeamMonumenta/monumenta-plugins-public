package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.events.ArrowConsumeEvent;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
import com.playmonumenta.plugins.itemstats.enchantments.Multiload;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

/**
 * Handles quivers, which are shulker boxes for arrows.
 * Arrows are taken from them to shoot, and arrows being picked up are put in there before the inventory.
 */
public class QuiverListener implements Listener {

	// Refill if the used arrow stack has less than this many arrows left.
	private static final int REFILL_LOWER_THAN = 16;

	// Refill arrows up to this amount. This is less than max stack size to prevent using an infinity crossbow (or multiple in a row) starting a new stack.
	private static final int REFILL_UP_TO = 48;

	public enum ArrowTransformMode {
		NONE("disabled", null),
		NORMAL("Normal Arrows", new ItemStack(Material.ARROW)),
		SPECTRAL("Spectral Arrows", new ItemStack(Material.SPECTRAL_ARROW)),
		WEAKNESS("Arrows of Weakness", makeTippedArrowStack(PotionType.WEAKNESS)),
		SLOWNESS("Arrows of Slowness", makeTippedArrowStack(PotionType.SLOWNESS)),
		POISON("Arrows of Poison", makeTippedArrowStack(PotionType.POISON)),
		;

		private final String mArrowName;

		private final @Nullable ItemStack mItemStack;

		ArrowTransformMode(String arrowName, @Nullable ItemStack itemStack) {
			mArrowName = arrowName;
			mItemStack = itemStack;
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
					       && ItemStatUtils.getPlayerModified(new NBTItem(item)) == null
					       && !ItemStatUtils.isQuiver(item)
					       && !InventoryUtils.containsSpecialLore(item);
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
			public void generateDescription(NBTCompound monumenta, Consumer<Component> addLore) {
				NBTCompoundList items = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
					                        .getCompoundList(ItemStatUtils.ITEMS_KEY);
				long amount = 0;
				for (NBTListCompound compound : items) {
					amount += ItemStatUtils.addPlayerModified(compound.addCompound("tag")).getLong(CustomContainerItemManager.AMOUNT_KEY);
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
						meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
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
								ArrowTransformMode[] allModes = ArrowTransformMode.values();
								ArrowTransformMode newMode = allModes[(mode.ordinal() + (event.getClick() == ClickType.LEFT ? 1 : -1) + allModes.length) % allModes.length];
								ItemStatUtils.setArrowTransformMode(quiver, newMode);
								ItemStatUtils.generateItemStats(quiver);
								gui.mPlayer.playSound(gui.mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 1, 1);
								gui.update();
							} else if (event.getClick() == ClickType.SWAP_OFFHAND && mode.mItemStack != null) {
								// Swap: transform all applicable arrows in the quiver
								NBTCompoundList itemsList = ItemStatUtils.addPlayerModified(new NBTItem(quiver, true)).getCompoundList(ItemStatUtils.ITEMS_KEY);
								NBTCompound firstArrowPlayerModified = null;
								for (Iterator<NBTListCompound> iterator = itemsList.iterator(); iterator.hasNext(); ) {
									NBTListCompound compound = iterator.next();
									ItemStack containedItem = NBTItem.convertNBTtoItem(compound);
									ItemStatUtils.removePlayerModified(new NBTItem(containedItem, true));
									if (Arrays.stream(ArrowTransformMode.values()).anyMatch(m -> containedItem.isSimilar(m.mItemStack))) {
										if (firstArrowPlayerModified == null) {
											// Modify the item in the quiver by modifying the transformed item copy, then overwriting the whole NBT
											ItemStack transformed = ItemUtils.clone(mode.mItemStack);
											ItemStatUtils.addPlayerModified(new NBTItem(transformed, true))
												.mergeCompound(ItemStatUtils.addPlayerModified(compound.addCompound("tag")));
											compound.removeKey("tag");
											compound.mergeCompound(NBTItem.convertItemtoNBT(transformed));
											firstArrowPlayerModified = ItemStatUtils.addPlayerModified(compound.addCompound("tag"));
										} else {
											// An arrow was transformed already: remove this item and increase the count of the transformed item
											iterator.remove();
											firstArrowPlayerModified.setLong(CustomContainerItemManager.AMOUNT_KEY,
												firstArrowPlayerModified.getLong(CustomContainerItemManager.AMOUNT_KEY)
													+ ItemStatUtils.addPlayerModified(compound.addCompound("tag")).getLong(CustomContainerItemManager.AMOUNT_KEY));
										}
									}
								}
								gui.mPlayer.playSound(gui.mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.BLOCKS, 1, 1);
								gui.update();
							}
						});
				}
			}
		};
	}

	private static ItemStack makeTippedArrowStack(PotionType potionType) {
		ItemStack result = new ItemStack(Material.TIPPED_ARROW);
		PotionMeta meta = ((PotionMeta) result.getItemMeta());
		meta.setBasePotionData(new PotionData(potionType));
		result.setItemMeta(meta);
		return result;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityShootBowEvent(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Player player
			    && ItemStatUtils.isQuiver(event.getConsumable())) {
			ItemStack quiver = event.getConsumable();
			Pair<ItemStack, Boolean> projectile = takeFromQuiver(player, quiver, 1, item -> {
				// infinity with normal arrows: don't consume
				if (item.getType() == Material.ARROW && ItemStatUtils.hasEnchantment(event.getBow(), ItemStatUtils.EnchantmentType.INFINITY)) {
					return false;
				}
				ArrowConsumeEvent arrowConsumeEvent = new ArrowConsumeEvent(player, item);
				Bukkit.getPluginManager().callEvent(arrowConsumeEvent);
				return !arrowConsumeEvent.isCancelled();
			});
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
					newPotionArrow.setBasePotionData(itemMeta.getBasePotionData());
					Color color = itemMeta.getColor();
					newPotionArrow.setColor(Color.fromRGB(-1).equals(color) ? null : color);
					for (PotionEffect customEffect : itemMeta.getCustomEffects()) {
						newPotionArrow.addCustomEffect(customEffect, true);
					}
				}
			} else {
				event.setCancelled(true);
			}
			player.updateInventory();
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
				if (crossbow.getItemMeta() instanceof CrossbowMeta crossbowMeta) {
					int numProjectiles = 1 + ItemStatUtils.getEnchantmentLevel(crossbow, ItemStatUtils.EnchantmentType.MULTILOAD);

					// Crossbows refund arrows when being shot instead of not consuming arrows when being loaded
					Pair<ItemStack, Boolean> projectile = takeFromQuiver(player, quiver, numProjectiles, is -> true);
					if (projectile == null) {
						return;
					}
					ItemStack projectileItem = projectile.getLeft();
					if (numProjectiles > 1) {
						Multiload.afterLoad(player, crossbow, numProjectiles, projectileItem.getAmount());
						projectileItem.setAmount(1);
					}
					crossbowMeta.addChargedProjectile(projectileItem);
					crossbow.setItemMeta(crossbowMeta);

					// Sound copied from vanilla (won't play due to cancelled event)
					player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS,
						1.0F, 1.0F / (FastUtils.randomFloatInRange(0, 1) * 0.5F + 1.0F) + 0.2F);
				}
			}
		}
	}

	private @Nullable Pair<ItemStack, Boolean> takeFromQuiver(Player player, ItemStack quiver, int numProjectiles, Predicate<ItemStack> consumePredicate) {
		if (quiver.getAmount() != 1) {
			player.sendMessage(Component.text("Cannot use stacked quivers!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1, 1);
			return null;
		}
		if (!getQuiverConfig(quiver).checkCanUse(player)) {
			return null;
		}
		Pair<ItemStack, Boolean> result = CustomContainerItemManager.removeFirstFromContainer(quiver, numProjectiles, ItemUtils::isArrow, consumePredicate);
		if (result == null) {
			player.sendMessage(Component.text("Your quiver is empty!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1, 1);
			return null;
		}
		return result;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPreDispenseEvent(BlockPreDispenseEvent event) {
		if (event.getBlock().getType() == Material.DISPENSER && ItemStatUtils.isQuiver(event.getItemStack())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerAttemptPickupItemEvent(PlayerAttemptPickupItemEvent event) {
		handlePickupEvent(event, event.getItem(), event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerPickupArrowEvent(PlayerPickupArrowEvent event) {
		handlePickupEvent(event, event.getItem(), event.getPlayer());
	}

	// If an arrow is picked up, put it into a quiver if space is available
	private void handlePickupEvent(Cancellable event, Item item, Player player) {
		if (!item.isValid()) {
			return;
		}

		ItemStack itemStack = item.getItemStack();
		attemptPickup(player, itemStack);

		if (itemStack.getAmount() == 0) {
			event.setCancelled(true);
			player.playPickupItemAnimation(item);
			item.remove();
		} else {
			item.setItemStack(itemStack);
		}
	}

	public static void attemptPickup(Player player, ItemStack itemStack) {
		if (player.getGameMode() == GameMode.CREATIVE
			    || !ItemUtils.isArrow(itemStack)
			    || ItemStatUtils.getEnchantmentLevel(itemStack, ItemStatUtils.EnchantmentType.THROWING_KNIFE) > 0) {
			return;
		}
		for (ItemStack quiver : player.getInventory()) {
			if (quiver == null || !ItemStatUtils.isQuiver(quiver) || quiver.getAmount() != 1
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
			if (transformedItemStack.getAmount() == 0) {
				return;
			}
		}
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
