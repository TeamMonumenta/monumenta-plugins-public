package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.inventories.CharmBag;
import com.playmonumenta.plugins.inventories.CharmBagManager;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CharmBagGui extends Gui {
	private final CharmBag mCharmBag;
	private final CharmBagManager.CharmBagSettings mSettings;
	private final String mPlainName;
	private int mPage;

	public CharmBagGui(Player player, CharmBag charmBag, CharmBagManager.CharmBagSettings settings, Component displayName) {
		super(player, 6 * 9, displayName);
		mCharmBag = charmBag;
		mSettings = settings;
		mPlainName = MessagingUtils.plainText(displayName);
	}

	@Override
	// the loop is exited after the modification, so no CME can happen
	protected void setup() {
		List<CharmBag.CharmBagItem> charmBagItemsCopy = new ArrayList<>(
			mCharmBag.mItems.stream()
				.map(item -> new CharmBag.CharmBagItem(ItemUtils.clone(item.mItem), item.mAmount))
				.toList());

		// Items grouped by class, and sorted within each region
		Map<String, List<CharmBag.CharmBagItem>> items =
			charmBagItemsCopy.stream()
				.sorted(
					// sort by location first (generally does not matter for normal charms)
					Comparator.comparing((CharmBag.CharmBagItem item) -> ItemStatUtils.getLocation(item.mItem))
						// then by charm power, greatest to least
						.thenComparing((CharmBag.CharmBagItem item) -> -ItemStatUtils.getCharmPower(item.mItem))
						// then by name
						.thenComparing((CharmBag.CharmBagItem item) -> ItemUtils.getPlainNameIfExists(item.mItem)))
				// then group everything by class
				.collect(Collectors.groupingBy((CharmBag.CharmBagItem item) -> {
					PlayerClass charmClass = ItemStatUtils.getCharmClass(item.mItem);
					if (charmClass == null || charmClass.mClassName == null) { // only would happen if we have a "Generalist" charm
						return "Generalist";
					} else {
						return charmClass.mClassName;
					}
				}));
		// Fill GUI with charms
		boolean showAmounts = mPlayer.getScoreboardTags().contains(CustomContainerItemManager.SHOW_AMOUNTS_TAG);
		int pos = 0;
		int itemsPerPage = 5 * 8; // top row and left column reserved

		String playerClass = AbilityUtils.getClass(mPlayer);
		List<String> classOrder = new ArrayList<>();
		if (!playerClass.equals("No Class")) { // player has a class, put their class first
			classOrder.add(playerClass);
			for (String thisClass : CharmBagManager.classListString) {
				if (!thisClass.equals(playerClass)) {
					classOrder.add(thisClass);
				}
			}
		} else { // No class; use default order
			classOrder = CharmBagManager.classListString;
		}

		for (String charmClass : classOrder) {
			List<CharmBag.CharmBagItem> classItems = items.get(charmClass);
			if (classItems == null) {
				continue;
			}
			boolean firstOfClass = true;
			pos = pos % 8 == 0 ? pos : pos + 8 - (pos % 8); // start new class on new line
			for (CharmBag.CharmBagItem item : classItems) {
				int posInPage = pos - itemsPerPage * mPage;
				if (posInPage < 0 || posInPage >= itemsPerPage) {
					pos++;
					firstOfClass = true; // always place a class icon at the start of a new page
					continue;
				}
				if (firstOfClass && CharmBagManager.CLASS_ICONS.containsKey(charmClass)) {
					setItem(9 + posInPage + posInPage / 8, CharmBagManager.CLASS_ICONS.get(charmClass));
				}
				ItemStack displayItem = ItemUtils.clone(item.mItem);
				ItemMeta itemMeta = displayItem.getItemMeta();
				if (itemMeta != null) {
					String amount = "" + item.mAmount;
					Component name = Component.text(amount + " ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
						.append(ItemUtils.getDisplayName(item.mItem));
					itemMeta.displayName(name);
					displayItem.setItemMeta(itemMeta);
				}
				if (showAmounts) {
					displayItem.setAmount((int) Math.min(64, item.mAmount));
				}
				setItem(10 + posInPage + posInPage / 8, new GuiItem(displayItem, false))
					.onClick(event -> {
						ItemStack movedItem = ItemUtils.clone(item.mItem);
						switch (event.getClick()) {
							case LEFT, SHIFT_LEFT -> {
								int maxFit = InventoryUtils.numCanFitInInventory(movedItem, mPlayer.getInventory());
								if (maxFit > 0) {
									movedItem.setAmount(event.getClick() == ClickType.LEFT ? Math.min(movedItem.getMaxStackSize(), maxFit) : maxFit);
									mCharmBag.remove(mPlayer, movedItem);
									mPlayer.getInventory().addItem(movedItem);
								}
								update();
							}
							case RIGHT, SHIFT_RIGHT -> {
								movedItem.setAmount(1);
								if (InventoryUtils.canFitInInventory(movedItem, mPlayer.getInventory())) {
									mCharmBag.remove(mPlayer, movedItem);
									mPlayer.getInventory().addItem(movedItem);
									update();
								}
							}
							case SWAP_OFFHAND -> {
								close();
								SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter how many", "items to retrieve"))
									.response((player, lines) -> {
										double retrievedAmount;
										try {
											retrievedAmount = lines[0].isEmpty() ? 0 : CharmBagManager.parseDoubleOrCalculation(lines[0]);
										} catch (NumberFormatException e) {
											player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
											return false;
										}
										if (retrievedAmount < 0) {
											player.sendMessage(Component.text("Please enter a positive number.", NamedTextColor.RED));
											return false;
										}

										long countInCharmBag = mCharmBag.count(item.mItem);
										long desiredAmount = (long) retrievedAmount;

										// Warn if not enough and exit (to not take out less than expected if not double-checked)
										if (desiredAmount > countInCharmBag) {
											player.sendMessage(Component.text("There are fewer than the requested amount of charms in the bag.", NamedTextColor.RED));
											player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
											open();
											return true;
										}
										if (desiredAmount >= Integer.MAX_VALUE) { // no. even if you really have that many.
											return false;
										}

										// normal case
										if (desiredAmount <= InventoryUtils.numCanFitInInventory(movedItem, mPlayer.getInventory())) {
											movedItem.setAmount((int) desiredAmount);
											mCharmBag.remove(mPlayer, movedItem);
											mPlayer.getInventory().addItem(movedItem);
										} else {
											player.sendMessage(Component.text("Not enough space in inventory for all items. No items have been retrieved.", NamedTextColor.RED));
											player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
										}
										open();
										return true;
									})
									.reopenIfFail(true)
									.open(mPlayer);
							}
							default -> {
								// Are you happy now, PMD?
							}
						}
					});
				pos++;
				firstOfClass = false;
			}
		}

		// page arrows and info item
		if (mPage > 0) {
			ItemStack previousPageIcon = new ItemStack(Material.ARROW);
			ItemMeta itemMeta = previousPageIcon.getItemMeta();
			itemMeta.displayName(Component.text("Previous Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			previousPageIcon.setItemMeta(itemMeta);
			setItem(0, previousPageIcon)
				.onLeftClick(() -> {
					mPage--;
					update();
				});
		}
		{
			ItemStack infoIcon = new ItemStack(Material.MANGROVE_HANGING_SIGN);
			ItemMeta itemMeta = infoIcon.getItemMeta();
			itemMeta.displayName(Component.text(mPlainName + " Info", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			itemMeta.lore(List.of(
				Component.text("Left click here to toggle displaying charm counts.", NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false),
				Component.text("Click on charms in your inventory to store them.", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false),
				Component.text("Shift click to store all charms of the same type.", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false),
				Component.text("Click on items in the " + mPlainName + " to retrieve them:", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false),
				Component.text(" - ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
					.append(Component.text("Left Click", NamedTextColor.WHITE))
					.append(Component.text(" to retrieve up to a stack", NamedTextColor.GRAY)),
				Component.text(" - ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
					.append(Component.text("Right Click", NamedTextColor.WHITE))
					.append(Component.text(" to retrieve one item only", NamedTextColor.GRAY)),
				Component.text(" - ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
					.append(Component.text("Shift + Left Click", NamedTextColor.WHITE))
					.append(Component.text(" to retrieve everything", NamedTextColor.GRAY)),
				Component.text(" - ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
					.append(Component.keybind("key.swapOffhand", NamedTextColor.WHITE))
					.append(Component.text(" to retrieve a custom amount", NamedTextColor.GRAY)),
				Component.text("   (may be a fraction or simple calculation)", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
			));
			infoIcon.setItemMeta(itemMeta);
			setItem(4, infoIcon)
				.onLeftClick(() -> {
					ScoreboardUtils.toggleTag(mPlayer, CustomContainerItemManager.SHOW_AMOUNTS_TAG);
					update();
				});
		}
		if (pos > itemsPerPage * (mPage + 1)) {
			ItemStack nextPageIcon = new ItemStack(Material.ARROW);
			ItemMeta itemMeta = nextPageIcon.getItemMeta();
			itemMeta.displayName(Component.text("Next Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			nextPageIcon.setItemMeta(itemMeta);
			setItem(8, nextPageIcon)
				.onLeftClick(() -> {
					mPage++;
					update();
				});
		}
	}

	@Override
	protected boolean onGuiClick(InventoryClickEvent event) {
		if (Bukkit.getPlayer(mCharmBag.mOwner) == null) {
			mPlayer.sendMessage(Component.text("Player has logged off!", NamedTextColor.RED));
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			mPlayer.closeInventory();
			return false;
		}
		return true;
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		if (Bukkit.getPlayer(mCharmBag.mOwner) == null) {
			mPlayer.sendMessage(Component.text("Player has logged off!", NamedTextColor.RED));
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			mPlayer.closeInventory();
			return;
		}
		ItemStack currentItem = event.getCurrentItem();
		if (event.getClick() == ClickType.LEFT
			&& CharmBagManager.canPutIntoCharmBag(currentItem, mSettings)) {
			mCharmBag.add(mPlayer, currentItem);
			update();
		} else if (event.getClick() == ClickType.SHIFT_LEFT && CharmBagManager.canPutIntoCharmBag(currentItem, mSettings)) {
			ItemStack combinedItems = ItemUtils.clone(currentItem);
			currentItem.setAmount(0);
			for (ItemStack item : mPlayer.getInventory().getStorageContents()) {
				if (item != null && item.isSimilar(combinedItems)) {
					combinedItems.setAmount(combinedItems.getAmount() + item.getAmount());
					item.setAmount(0);
				}
			}
			mCharmBag.add(mPlayer, combinedItems);
			update();
		} else if ((event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) && CharmBagManager.canPutIntoCharmBag(currentItem, mSettings)) {
			ItemStack oneItem = ItemUtils.clone(currentItem);
			currentItem.setAmount(currentItem.getAmount() - 1);
			oneItem.setAmount(1);
			mCharmBag.add(mPlayer, oneItem);
			update();
		} else if (!CharmBagManager.isCharm(currentItem) && (ItemStatUtils.isNormalCharm(currentItem) || ItemStatUtils.isZenithCharm(currentItem))) {
			mPlayer.sendMessage(Component.text("This type of charm cannot be put into the " + mPlainName, NamedTextColor.RED));
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_CLOSE, SoundCategory.PLAYERS, 1.0f, 0.7f);
		}
	}
}
