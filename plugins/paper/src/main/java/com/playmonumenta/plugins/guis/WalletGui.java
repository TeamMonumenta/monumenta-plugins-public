package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.inventories.BaseWallet;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.itemstats.enums.Region;
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

public class WalletGui extends Gui {
	private final boolean mOpenedAsModerator;
	private final BaseWallet mWallet;
	private final WalletManager.WalletSettings mSettings;
	private final String mPlainName;
	private int mPage;

	public WalletGui(Player player, BaseWallet wallet, WalletManager.WalletSettings settings, Component displayName, boolean openedAsModerator) {
		super(player, 6 * 9, displayName);
		mOpenedAsModerator = openedAsModerator;
		mWallet = wallet;
		mSettings = settings;
		mPlainName = MessagingUtils.plainText(displayName);
	}

	@Override
	// the loop is exited after the modification, so no CME can happen
	@SuppressWarnings("ModifyCollectionInEnhancedForLoop")
	protected void setup() {

		if (!mOpenedAsModerator && mWallet.canNotAccess(mPlayer)) {
			mPlayer.sendMessage(Component.text("You no longer have access to this wallet", NamedTextColor.RED));
			mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.closeInventory(), 1L);
			return;
		}

		List<BaseWallet.WalletItem> walletItemsCopy = new ArrayList<>(
			mWallet.mItems.stream()
				.map(item -> new BaseWallet.WalletItem(ItemUtils.clone(item.mItem), item.mAmount))
				.toList());

		// Add compressed currencies as fake items to be able to retrieve them
		ItemStack lastUsedBase = null;
		ciLoop:
		for (WalletManager.CompressionInfo ci : WalletManager.COMPRESSIBLE_CURRENCIES) {
			for (BaseWallet.WalletItem baseWalletItem : walletItemsCopy) {
				if (baseWalletItem.mItem.isSimilar(ci.mBase)) {
					long baseAmount = baseWalletItem.mAmount;
					if (baseAmount >= ci.mAmount || (lastUsedBase != null && lastUsedBase.isSimilar(ci.mBase))) {
						baseWalletItem.mAmount = baseAmount % ci.mAmount;
						lastUsedBase = ci.mBase;
						walletItemsCopy.add(new BaseWallet.WalletItem(ItemUtils.clone(ci.mCompressed), baseAmount / ci.mAmount));
						continue ciLoop;
					}
				}
			}
		}

		// Items grouped by region, and sorted within each region
		Map<Region, List<BaseWallet.WalletItem>> items =
			walletItemsCopy.stream()
				.sorted(
					// sort main currencies to the very front
					Comparator.comparing((BaseWallet.WalletItem item) -> {
							int index = WalletManager.MAIN_CURRENCIES.indexOf(item.mItem);
							return index < 0 ? Integer.MAX_VALUE : index;
						})
						// then sort by location
						.thenComparing((BaseWallet.WalletItem item) -> ItemStatUtils.getLocation(item.mItem))
						// then by manual sort order (and manually sorted items are the first in their location)
						.thenComparing((BaseWallet.WalletItem item) -> {
							int index = WalletManager.MANUAL_SORT_ORDER.indexOf(item.mItem);
							return index < 0 ? Integer.MAX_VALUE : index;
						})
						// sort compressible currencies from most valuable to least valuable
						.thenComparing((BaseWallet.WalletItem item) -> {
							WalletManager.CompressionInfo compressionInfo = WalletManager.getCompressionInfo(item.mItem);
							if (compressionInfo != null) {
								return -compressionInfo.mAmount;
							}
							return WalletManager.COMPRESSIBLE_CURRENCIES.stream().anyMatch(ci -> ci.mBase.isSimilar(item.mItem)) ? 0 : 1;
						})
						// finally, sort by name
						.thenComparing((BaseWallet.WalletItem item) -> ItemUtils.getPlainNameIfExists(item.mItem)))
				// group everything by region
				.collect(Collectors.groupingBy((BaseWallet.WalletItem item) -> WalletManager.MAIN_CURRENCIES.contains(item.mItem) ? Region.NONE : ItemStatUtils.getRegion(item.mItem)));

		// Fill GUI with items
		boolean showAmounts = mPlayer.getScoreboardTags().contains(CustomContainerItemManager.SHOW_AMOUNTS_TAG);
		boolean showAmountsAsStacks = mPlayer.getScoreboardTags().contains(CustomContainerItemManager.SHOW_AMOUNTS_AS_STACKS_TAG);
		int pos = 0;
		int itemsPerPage = 5 * 8; // top row and left column reserved
		for (Region region : Region.values()) {
			List<BaseWallet.WalletItem> regionItems = items.get(region);
			if (regionItems == null) {
				continue;
			}
			boolean firstOfRegion = true;
			pos = pos % 8 == 0 ? pos : pos + 8 - (pos % 8); // start new region on new line
			for (BaseWallet.WalletItem item : regionItems) {
				int posInPage = pos - itemsPerPage * mPage;
				if (posInPage < 0 || posInPage >= itemsPerPage) {
					pos++;
					firstOfRegion = true; // always place a region icon at the start of a new page
					continue;
				}
				if (firstOfRegion && WalletManager.REGION_ICONS.containsKey(region)) {
					setItem(9 + posInPage + posInPage / 8, WalletManager.REGION_ICONS.get(region));
				}
				ItemStack displayItem = ItemUtils.clone(item.mItem);
				ItemMeta itemMeta = displayItem.getItemMeta();
				if (itemMeta != null) {
					String amount;
					if (showAmountsAsStacks && item.mItem.getMaxStackSize() > 1 && item.mAmount >= item.mItem.getMaxStackSize()) {
						long stacks = item.mAmount / item.mItem.getMaxStackSize();
						long remaining = item.mAmount % item.mItem.getMaxStackSize();
						amount = item.mAmount + " (" + stacks + " stack" + (stacks == 1 ? "" : "s") + (remaining == 0 ? "" : " + " + remaining) + ")";
					} else {
						amount = "" + item.mAmount;
					}
					Component name = Component.text(amount + " ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
						.append(ItemUtils.getDisplayName(item.mItem));
					for (WalletManager.CompressionInfo compressionInfo : WalletManager.COMPRESSIBLE_CURRENCIES) {
						if (compressionInfo.mBase.isSimilar(item.mItem) || compressionInfo.mCompressed.isSimilar(item.mItem)) {
							long count = mWallet.count(item.mItem);
							if (count != item.mAmount) {
								name = name.append(Component.text(" (" + count + " total)", NamedTextColor.GOLD));
							}
							break;
						}
					}
					itemMeta.displayName(name);
					displayItem.setItemMeta(itemMeta);
				}
				if (showAmounts) {
					displayItem.setAmount((int) Math.max(1, Math.min(64, showAmountsAsStacks ? item.mAmount / item.mItem.getMaxStackSize() : item.mAmount)));
				}
				setItem(10 + posInPage + posInPage / 8, new GuiItem(displayItem, false))
					.onClick(event -> {
						if (!mOpenedAsModerator && mWallet.canNotAccess(mPlayer)) {
							mPlayer.sendMessage(Component.text("You no longer have access to this wallet", NamedTextColor.RED));
							mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
							mPlayer.closeInventory();
							return;
						}

						ItemStack movedItem = ItemUtils.clone(item.mItem);
						switch (event.getClick()) {
							case LEFT, SHIFT_LEFT -> {
								int maxFit = InventoryUtils.numCanFitInInventory(movedItem, mPlayer.getInventory());
								if (maxFit > 0) {
									movedItem.setAmount(event.getClick() == ClickType.LEFT ? Math.min(movedItem.getMaxStackSize(), maxFit) : maxFit);
									mWallet.remove(mPlayer, movedItem);
									mPlayer.getInventory().addItem(movedItem);
								}
							}
							case RIGHT, SHIFT_RIGHT -> {
								movedItem.setAmount(1);
								if (InventoryUtils.canFitInInventory(movedItem, mPlayer.getInventory())) {
									mWallet.remove(mPlayer, movedItem);
									mPlayer.getInventory().addItem(movedItem);
								}
							}
							case SWAP_OFFHAND -> {
								close();
								SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter how many", "items to retrieve"))
									.response((player, lines) -> {
										double retrievedAmount;
										try {
											retrievedAmount = lines[0].isEmpty() ? 0 : WalletManager.parseDoubleOrCalculation(lines[0]);
										} catch (NumberFormatException e) {
											player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
											return false;
										}
										if (retrievedAmount < 0) {
											player.sendMessage(Component.text("Please enter a positive number.", NamedTextColor.RED));
											return false;
										}
										WalletManager.CompressionInfo compressionInfo = WalletManager.getCompressionInfo(item.mItem);
										long countInWallet = mWallet.count(compressionInfo == null ? movedItem : compressionInfo.mBase);
										long desiredAmount = (long) Math.ceil(compressionInfo == null ? retrievedAmount : retrievedAmount * compressionInfo.mAmount);

										// Warn if not enough and exit (to not take out less than expected if not double-checked)
										if (desiredAmount > countInWallet) {
											player.sendMessage(Component.text("There are fewer than the requested amount of items in the bag.", NamedTextColor.RED));
											player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
											open();
											return true;
										}
										if (desiredAmount >= Integer.MAX_VALUE) { // no. even if you really have that many.
											return false;
										}

										ItemStack base = compressionInfo == null ? item.mItem : compressionInfo.mBase;
										// If retrieving items that can be compressed, take out the appropriate amount of compressed/uncompressed
										// Requires special handling for the inventory space calculation in case of partial stacks
										if (WalletManager.isBase(base)) {
											int leftToRemove = (int) desiredAmount;
											List<ItemStack> result = new ArrayList<>();
											for (WalletManager.CompressionInfo ci : WalletManager.COMPRESSIBLE_CURRENCIES) {
												if (ci.mBase.isSimilar(base)) {
													ItemStack res = ItemUtils.clone(ci.mCompressed);
													int compressed = leftToRemove / ci.mAmount;
													res.setAmount(compressed);
													result.add(res);
													leftToRemove -= compressed * ci.mAmount;
													if (leftToRemove == 0) {
														break;
													}
												}
											}
											if (leftToRemove > 0) {
												ItemStack res = ItemUtils.clone(base);
												res.setAmount(leftToRemove);
												result.add(res);
											}
											if (InventoryUtils.canFitInInventory(result, player.getInventory())) {
												ItemStack baseClone = ItemUtils.clone(base);
												baseClone.setAmount((int) desiredAmount);
												mWallet.remove(mPlayer, baseClone);
												result.forEach(mPlayer.getInventory()::addItem);
											} else {
												player.sendMessage(Component.text("Not enough space in inventory for all items. No items have been retrieved.", NamedTextColor.RED));
												player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
											}
										} else {
											// normal case
											if (desiredAmount <= InventoryUtils.numCanFitInInventory(movedItem, mPlayer.getInventory())) {
												movedItem.setAmount((int) desiredAmount);
												mWallet.remove(mPlayer, movedItem);
												mPlayer.getInventory().addItem(movedItem);
											} else {
												player.sendMessage(Component.text("Not enough space in inventory for all items. No items have been retrieved.", NamedTextColor.RED));
												player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
											}
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
				firstOfRegion = false;
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
		if (mOpenedAsModerator || !mWallet.canNotChangeOwner(mPlayer)) {
			ItemStack ownerIcon = mWallet.ownerIcon();
			ItemMeta meta = ownerIcon.getItemMeta();
			meta.lore(List.of(
				Component.text("Left click here to change the owner of this " + mPlainName + ".", NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false)
			));
			ownerIcon.setItemMeta(meta);
			setItem(0, 2, ownerIcon)
				.onLeftClick(() -> {
					if (!mOpenedAsModerator && mWallet.canNotChangeOwner(mPlayer)) {
						mPlayer.sendMessage(Component.text("You no longer have access to this wallet", NamedTextColor.RED));
						mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
						mPlayer.closeInventory();
						return;
					}
					Component changeOwnerTitle = Component.text("Set owner for " + mPlainName, NamedTextColor.DARK_GREEN);
					new WalletOwnerGui(this, mPlayer, mWallet, changeOwnerTitle, mOpenedAsModerator).open();
				});
		}
		{
			ItemStack infoIcon = new ItemStack(Material.DARK_OAK_SIGN);
			ItemMeta itemMeta = infoIcon.getItemMeta();
			itemMeta.displayName(Component.text(mPlainName + " Info", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			itemMeta.lore(List.of(
				Component.text("Left click here to toggle displaying item counts.", NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false),
				Component.text("Right click here to toggle showing counts in stacks.", NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false),
				Component.text("Click on currency items in your inventory to store them.", NamedTextColor.GRAY)
					.decoration(TextDecoration.ITALIC, false),
				Component.text("Shift click to store all of the same type.", NamedTextColor.GRAY)
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
				})
				.onRightClick(() -> {
					ScoreboardUtils.toggleTag(mPlayer, CustomContainerItemManager.SHOW_AMOUNTS_AS_STACKS_TAG);
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
		if (!mWallet.isLoaded()) {
			mPlayer.sendMessage(Component.text("Wallet is not loaded!", NamedTextColor.RED));
			mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			mPlayer.closeInventory();
			return false;
		}

		if (!mOpenedAsModerator && mWallet.canNotAccess(mPlayer)) {
			mPlayer.sendMessage(Component.text("You no longer have access to this wallet", NamedTextColor.RED));
			mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			mPlayer.closeInventory();
			return false;
		}

		return true;
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		if (!mWallet.isLoaded()) {
			mPlayer.sendMessage(Component.text("Wallet is not loaded!", NamedTextColor.RED));
			mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			mPlayer.closeInventory();
			return;
		}

		if (!mOpenedAsModerator && mWallet.canNotAccess(mPlayer)) {
			mPlayer.sendMessage(Component.text("You no longer have access to this wallet", NamedTextColor.RED));
			mPlayer.playSound(mPlayer, Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			mPlayer.closeInventory();
			return;
		}

		ItemStack currentItem = event.getCurrentItem();
		if (event.getClick() == ClickType.LEFT
			&& WalletManager.canPutIntoWallet(currentItem, mSettings)) {
			mWallet.add(mPlayer, currentItem);
		} else if (event.getClick() == ClickType.SHIFT_LEFT && WalletManager.canPutIntoWallet(currentItem, mSettings)) {
			ItemStack combinedItems = ItemUtils.clone(currentItem);
			currentItem.setAmount(0);
			for (ItemStack item : mPlayer.getInventory().getStorageContents()) {
				if (item != null && item.isSimilar(combinedItems)) {
					combinedItems.setAmount(combinedItems.getAmount() + item.getAmount());
					item.setAmount(0);
				}
			}
			mWallet.add(mPlayer, combinedItems);
		} else if ((event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) && WalletManager.canPutIntoWallet(currentItem, mSettings)) {
			ItemStack oneItem = ItemUtils.clone(currentItem);
			currentItem.setAmount(currentItem.getAmount() - 1);
			oneItem.setAmount(1);
			mWallet.add(mPlayer, oneItem);
		}
	}

	public void updateIfWalletMatches(BaseWallet wallet) {
		if (mWallet.equals(wallet)) {
			update();
		}
	}
}
