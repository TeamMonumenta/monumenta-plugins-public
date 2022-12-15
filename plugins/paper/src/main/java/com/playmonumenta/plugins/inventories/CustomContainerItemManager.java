package com.playmonumenta.plugins.inventories;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.scriptedquests.trades.TradeWindowOpenEvent;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class CustomContainerItemManager implements Listener {

	/**
	 * Key in Monumenta.PlayerModified that stores how many of that item are actually in the container.
	 * Tag type is Long.
	 */
	public static final String AMOUNT_KEY = "AmountInContainer";

	public static final String SHOW_AMOUNTS_TAG = "ShowContainerAmounts";

	public static final String SHOW_AMOUNTS_AS_STACKS_TAG = "ShowContainerAmountsAsStacks";

	public static final ImmutableMap<ItemStatUtils.Region, ItemStack> REGION_ICONS = ImmutableMap.of(
		ItemStatUtils.Region.VALLEY, ItemUtils.parseItemStack("{id:\"minecraft:cyan_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"sc\",Color:3},{Pattern:\"mc\",Color:11},{Pattern:\"flo\",Color:15},{Pattern:\"bts\",Color:11},{Pattern:\"tts\",Color:11}]},HideFlags:63,display:{Name:'{\"text\":\"King\\'s Valley\",\"italic\":false,\"bold\":true,\"color\":\"aqua\"}'}}}"),
		ItemStatUtils.Region.ISLES, ItemUtils.parseItemStack("{id:\"minecraft:green_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"gru\",Color:5},{Pattern:\"bo\",Color:13},{Pattern:\"mr\",Color:13},{Pattern:\"mc\",Color:5}]},HideFlags:63,display:{Name:'{\"text\":\"Celsian Isles\",\"italic\":false,\"bold\":true,\"color\":\"green\"}'}}}"),
		ItemStatUtils.Region.RING, ItemUtils.parseItemStack("{id:\"minecraft:white_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"ss\",Color:12},{Pattern:\"bts\",Color:13},{Pattern:\"tts\",Color:13},{Pattern:\"gra\",Color:8},{Pattern:\"ms\",Color:13},{Pattern:\"gru\",Color:7},{Pattern:\"flo\",Color:15},{Pattern:\"mc\",Color:0}]},HideFlags:63,display:{Name:'{\"bold\":true,\"italic\":false,\"underlined\":false,\"color\":\"white\",\"text\":\"Architect\\\\u0027s Ring\"}'}}}")
	);

	private static class CustomContainerItemGui extends Gui {
		private final ItemStack mContainer;
		private int mPage;

		public CustomContainerItemGui(Player player, ItemStack container) {
			super(player, 6 * 9, container.getItemMeta().displayName());
			this.mContainer = container;
			setFiller(Material.BLACK_STAINED_GLASS_PANE);
		}

		@Override
		protected void setup() {
			if (!validateContainerItem(mPlayer, mContainer)) {
				return;
			}

			List<ItemStack> itemsList = new ArrayList<>(
				new NBTItem(mContainer).addCompound(ItemStatUtils.MONUMENTA_KEY)
					.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
					.getCompoundList(ItemStatUtils.ITEMS_KEY).stream()
					.map(NBTItem::convertNBTtoItem)
					.toList());

			// Items grouped by region, and sorted by location, then name within a region
			Map<ItemStatUtils.Region, List<ItemStack>> items =
				itemsList.stream()
					.sorted(Comparator.comparing(ItemStatUtils::getLocation)
						        .thenComparing(ItemUtils::getPlainNameIfExists))
					.collect(Collectors.groupingBy(ItemStatUtils::getRegion));

			// Fill GUI with items
			boolean showAmounts = mPlayer.getScoreboardTags().contains(SHOW_AMOUNTS_TAG);
			int pos = 0;
			int itemsPerPage = 5 * 8; // top row and left column reserved
			for (ItemStatUtils.Region region : ItemStatUtils.Region.values()) {
				List<ItemStack> regionItems = items.get(region);
				if (regionItems == null) {
					continue;
				}
				boolean firstOfRegion = true;
				pos = pos % 8 == 0 ? pos : pos + 8 - (pos % 8); // start new region on new line
				for (ItemStack item : regionItems) {
					int posInPage = pos - itemsPerPage * mPage;
					if (posInPage < 0 || posInPage >= itemsPerPage) {
						pos++;
						firstOfRegion = true; // always place a region icon at the start of a new page
						continue;
					}
					if (firstOfRegion && REGION_ICONS.containsKey(region)) {
						setItem(9 + posInPage + posInPage / 8, REGION_ICONS.get(region));
					}
					ItemStack displayItem = ItemUtils.clone(item);
					ItemMeta itemMeta = displayItem.getItemMeta();
					long amount = ItemStatUtils.addPlayerModified(new NBTItem(item)).getLong(AMOUNT_KEY);
					itemMeta.displayName(
						Component.text(amount + " ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
							.append(itemMeta.displayName()));
					displayItem.setItemMeta(itemMeta);
					if (showAmounts) {
						displayItem.setAmount((int) Math.max(1, Math.min(64, amount)));
					}
					setItem(10 + posInPage + posInPage / 8, new GuiItem(displayItem, false))
						.onClick(event -> {
							ItemStack movedItem = ItemUtils.clone(item);
							new NBTItem(movedItem, true)
								.addCompound(ItemStatUtils.MONUMENTA_KEY)
								.removeKey(ItemStatUtils.PLAYER_MODIFIED_KEY);
							switch (event.getClick()) {
								case LEFT, SHIFT_LEFT -> {
									int maxFit = InventoryUtils.numCanFitInInventory(movedItem, mPlayer.getInventory());
									if (maxFit > 0) {
										movedItem.setAmount(event.getClick() == ClickType.LEFT ? Math.min(movedItem.getMaxStackSize(), maxFit) : maxFit);
										removeFromContainer(mPlayer, mContainer, movedItem);
										mPlayer.getInventory().addItem(movedItem);
									}
									update();
								}
								case RIGHT, SHIFT_RIGHT -> {
									movedItem.setAmount(1);
									if (InventoryUtils.canFitInInventory(movedItem, mPlayer.getInventory())) {
										removeFromContainer(mPlayer, mContainer, movedItem);
										mPlayer.getInventory().addItem(movedItem);
										update();
									}
								}
								case SWAP_OFFHAND -> {
									close();
									SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter how many", "items to retrieve"))
										.response((player, lines) -> {
											int retrievedAmount;
											try {
												retrievedAmount = lines[0].isEmpty() ? 0 : Integer.parseInt(lines[0]);
											} catch (NumberFormatException e) {
												player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
												return false;
											}
											if (retrievedAmount < 0) {
												player.sendMessage(Component.text("Please enter a positive number.", NamedTextColor.RED));
												return false;
											}
											long countInContainer = countInContainer(mPlayer, mContainer, movedItem);

											// Warn if not enough and exit (to not take out less than expected if not double-checked)
											if (retrievedAmount > countInContainer) {
												player.sendMessage(Component.text("There are fewer than the requested amount of items in the bag.", NamedTextColor.RED));
												player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
												open();
												return true;
											}

											if (retrievedAmount <= InventoryUtils.numCanFitInInventory(movedItem, mPlayer.getInventory())) {
												movedItem.setAmount(retrievedAmount);
												removeFromContainer(mPlayer, mContainer, movedItem);
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
			{
				ItemStack infoIcon = new ItemStack(Material.DARK_OAK_SIGN);
				ItemMeta itemMeta = infoIcon.getItemMeta();
				itemMeta.displayName(Component.text(mContainer.getItemMeta().getDisplayName() + " Info", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
				itemMeta.lore(List.of(
					Component.text("Click here to toggle displaying item counts.", NamedTextColor.WHITE)
						.decoration(TextDecoration.ITALIC, false),
					Component.text("Click on currency items in your inventory to store them.", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false),
					Component.text("Shift click to store all of the same type.", NamedTextColor.GRAY)
						.decoration(TextDecoration.ITALIC, false),
					Component.text("Click on items in the " + mContainer.getItemMeta().getDisplayName() + " to retrieve them:", NamedTextColor.GRAY)
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
						.append(Component.text(" to retrieve a custom amount", NamedTextColor.GRAY))
				));
				infoIcon.setItemMeta(itemMeta);
				setItem(4, infoIcon)
					.onLeftClick(() -> {
						ScoreboardUtils.toggleTag(mPlayer, SHOW_AMOUNTS_TAG);
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
			return validateContainerItem(mPlayer, mContainer);
		}

		@Override
		protected void onPlayerInventoryClick(InventoryClickEvent event) {
			if (!validateContainerItem(mPlayer, mContainer)) {
				return;
			}
			ItemStack currentItem = event.getCurrentItem();
			if (event.getClick() == ClickType.LEFT
				    && canPutIntoContainer(currentItem)) {
				addToContainer(mPlayer, mContainer, currentItem, true);
				update();
			} else if (event.getClick() == ClickType.SHIFT_LEFT && canPutIntoContainer(currentItem)) {
				ItemStack combinedItems = ItemUtils.clone(currentItem);
				currentItem.setAmount(0);
				for (ItemStack item : mPlayer.getInventory().getStorageContents()) {
					if (item != null && item.isSimilar(combinedItems)) {
						combinedItems.setAmount(combinedItems.getAmount() + item.getAmount());
						item.setAmount(0);
					}
				}
				addToContainer(mPlayer, mContainer, combinedItems, true);
				update();
			} else if ((event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) && canPutIntoContainer(currentItem)) {
				ItemStack oneItem = ItemUtils.clone(currentItem);
				currentItem.setAmount(currentItem.getAmount() - 1);
				oneItem.setAmount(1);
				addToContainer(mPlayer, mContainer, oneItem, true);
				update();
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.getClickedInventory() instanceof PlayerInventory // to prevent issues, only allow using from player inventory
			    && isCustomContainerItem(event.getCurrentItem())
			    && event.getCurrentItem().getAmount() == 1
			    && event.getWhoClicked() instanceof Player player) {

			ItemStack container = event.getCurrentItem();

			if (ItemUtils.isNullOrAir(event.getCursor())) {
				if (event.getClick() == ClickType.RIGHT) {
					// open container
					event.setCancelled(true);
					if (!checkSoulbound(player, container)) {
						return;
					}
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
					new CustomContainerItemGui(player, container).open();
				} else if (event.getClick() == ClickType.SWAP_OFFHAND) {
					// quick-fill container
					event.setCancelled(true);
					if (!checkSoulbound(player, container)) {
						return;
					}
					int deposited = 0;
					PlayerInventory inventory = player.getInventory();
					for (int i = 0; i < inventory.getSize(); i++) {
						ItemStack item = inventory.getItem(i);
						if (canPutIntoContainer(item)) {
							deposited += item.getAmount();
							addToContainer(player, container, item, false);
						}
					}
					if (deposited > 0) {
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
						player.sendMessage(Component.text(deposited + " item" + (deposited == 1 ? "" : "s") + " deposited into " + container.getItemMeta().getDisplayName(), NamedTextColor.GOLD));
						ItemStatUtils.generateItemStats(container);
					}
				}
			} else {
				if (event.getClick() == ClickType.RIGHT) {
					event.setCancelled(true);
					if (!checkSoulbound(player, container)) {
						return;
					}
					ItemStack cursor = event.getCursor();
					if (canPutIntoContainer(cursor)) {
						addToContainer(player, container, cursor, true);
						event.getView().setCursor(cursor);
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
						player.sendMessage(Component.text("Item deposited into " + container.getItemMeta().getDisplayName(), NamedTextColor.GOLD));
					} else {
						player.sendMessage(Component.text("Only plain currency can be put into the " + container.getItemMeta().getDisplayName(), NamedTextColor.RED));
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					}
				}
			}
		}
	}

	private static boolean checkSoulbound(Player player, ItemStack item) {
		if (ItemStatUtils.getInfusionLevel(item, ItemStatUtils.InfusionType.SOULBOUND) > 0
			    && !player.getUniqueId().equals(ItemStatUtils.getInfuser(item, ItemStatUtils.InfusionType.SOULBOUND))) {
			player.sendMessage(Component.text("This " + item.getItemMeta().getDisplayName() + " does not belong to you!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return false;
		}
		return true;
	}

	public static boolean canPutIntoContainer(ItemStack item) {
		return item != null
			       && ItemStatUtils.getTier(item) == ItemStatUtils.Tier.CURRENCY
			       && ItemStatUtils.getPlayerModified(new NBTItem(item)) == null;
	}

	public static boolean isCustomContainerItem(@Nullable ItemStack itemStack) {
		return false; // should put item type + name checks here once such an item is added
	}

	// Generates the container's lore text based on contents
	public static void generateDescription(NBTCompound monumenta, Consumer<Component> addLore) {
		NBTCompoundList items = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
			                        .getCompoundList(ItemStatUtils.ITEMS_KEY);
		long amount = 0;
		for (NBTListCompound compound : items) {
			amount += compound.addCompound("tag")
				          .addCompound(ItemStatUtils.MONUMENTA_KEY)
				          .addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
				          .getLong(CustomContainerItemManager.AMOUNT_KEY);
		}
		addLore.accept(Component.text("Contains ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
			               .append(Component.text(amount, NamedTextColor.WHITE))
			               .append(Component.text(" items", NamedTextColor.GRAY)));
	}

	private static boolean validateContainerItem(Player player, ItemStack container) {
		if (container.getAmount() != 1) {
			player.closeInventory();
			player.sendMessage(Component.text("Trying to be sneaky, are you?", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return false;
		}
		for (ItemStack item : player.getInventory().getContents()) {
			if (NmsUtils.getVersionAdapter().isSameItem(item, container)) {
				return true;
			}
		}
		player.closeInventory();
		player.sendMessage(Component.text("Your " + container.getItemMeta().getDisplayName() + " has disappeared from your inventory.", NamedTextColor.RED));
		player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
		return false;
	}

	private static void addToContainer(Player player, ItemStack container, ItemStack currency, boolean generateItemStats) {
		if (!validateContainerItem(player, container)) {
			throw new IllegalStateException("Container not valid in addToContainer");
		}

		NBTCompoundList itemsList = new NBTItem(container, true)
			                            .addCompound(ItemStatUtils.MONUMENTA_KEY)
			                            .addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
			                            .getCompoundList(ItemStatUtils.ITEMS_KEY);

		for (NBTListCompound compound : itemsList) {
			ItemStack containedItem = NBTItem.convertNBTtoItem(compound);
			new NBTItem(containedItem, true)
				.addCompound(ItemStatUtils.MONUMENTA_KEY)
				.removeKey(ItemStatUtils.PLAYER_MODIFIED_KEY);
			if (containedItem.isSimilar(currency)) {
				NBTCompound playerModified = compound.addCompound("tag")
					                             .addCompound(ItemStatUtils.MONUMENTA_KEY)
					                             .addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
				playerModified.setLong(AMOUNT_KEY, playerModified.getLong(AMOUNT_KEY) + currency.getAmount());
				currency.setAmount(0);
				if (generateItemStats) {
					ItemStatUtils.generateItemStats(container);
				}
				return;
			}
		}
		NBTCompound addedItem = NBTItem.convertItemtoNBT(currency);
		addedItem.addCompound("tag")
			.addCompound(ItemStatUtils.MONUMENTA_KEY)
			.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
			.setLong(AMOUNT_KEY, (long) currency.getAmount());
		addedItem.setByte("Count", (byte) 1);
		itemsList.addCompound(addedItem);
		currency.setAmount(0);
		if (generateItemStats) {
			ItemStatUtils.generateItemStats(container);
		}
	}

	private static long countInContainer(Player player, ItemStack container, ItemStack currency) {
		if (!validateContainerItem(player, container)) {
			throw new IllegalStateException("Container not valid in countInContainer");
		}

		NBTCompoundList itemsList = new NBTItem(container, true)
			                            .addCompound(ItemStatUtils.MONUMENTA_KEY)
			                            .addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
			                            .getCompoundList(ItemStatUtils.ITEMS_KEY);

		for (NBTListCompound compound : itemsList) {
			ItemStack containedItem = NBTItem.convertNBTtoItem(compound);
			new NBTItem(containedItem, true)
				.addCompound(ItemStatUtils.MONUMENTA_KEY)
				.removeKey(ItemStatUtils.PLAYER_MODIFIED_KEY);
			if (containedItem.isSimilar(currency)) {
				return compound.addCompound("tag")
					       .addCompound(ItemStatUtils.MONUMENTA_KEY)
					       .addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
					       .getLong(AMOUNT_KEY);
			}
		}
		return 0;
	}

	private static void removeFromContainer(Player player, ItemStack container, ItemStack currency) {
		if (!validateContainerItem(player, container)) {
			throw new IllegalStateException("Container not valid in removeFromContainer");
		}

		NBTCompoundList itemsList = new NBTItem(container, true)
			                            .addCompound(ItemStatUtils.MONUMENTA_KEY)
			                            .addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY)
			                            .getCompoundList(ItemStatUtils.ITEMS_KEY);

		for (int i = 0; i < itemsList.size(); i++) {
			NBTListCompound compound = itemsList.get(i);
			ItemStack containedItem = NBTItem.convertNBTtoItem(compound);
			new NBTItem(containedItem, true)
				.addCompound(ItemStatUtils.MONUMENTA_KEY)
				.removeKey(ItemStatUtils.PLAYER_MODIFIED_KEY);
			if (containedItem.isSimilar(currency)) {
				NBTCompound playerModified = compound.addCompound("tag")
					                             .addCompound(ItemStatUtils.MONUMENTA_KEY)
					                             .addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
				long containedAmount = playerModified.getLong(AMOUNT_KEY);
				if (currency.getAmount() >= containedAmount) {
					currency.setAmount((int) containedAmount);
					itemsList.remove(i);
				} else {
					playerModified.setLong(AMOUNT_KEY, containedAmount - currency.getAmount());
				}
				ItemStatUtils.generateItemStats(container);
				return;
			}
		}

		currency.setAmount(0);
	}

	// When buying a container, it is soulbound to the player that buys it
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void tradeWindowOpenEvent(TradeWindowOpenEvent event) {
		List<TradeWindowOpenEvent.Trade> trades = event.getTrades();
		for (int i = 0; i < trades.size(); i++) {
			TradeWindowOpenEvent.Trade trade = trades.get(i);
			MerchantRecipe recipe = trade.getRecipe();
			ItemStack result = recipe.getResult();
			if (isCustomContainerItem(result)) {
				result = ItemUtils.clone(result);
				ItemStatUtils.addInfusion(result, ItemStatUtils.InfusionType.SOULBOUND, 1, event.getPlayer().getUniqueId());
				MerchantRecipe newRecipe = new MerchantRecipe(result, recipe.getUses(), recipe.getMaxUses(), recipe.hasExperienceReward(), recipe.getVillagerExperience(),
					recipe.getPriceMultiplier(), recipe.getDemand(), recipe.getSpecialPrice(), recipe.shouldIgnoreDiscounts());
				newRecipe.setIngredients(recipe.getIngredients().stream().map(ItemUtils::clone).toList());
				trades.set(i, new TradeWindowOpenEvent.Trade(newRecipe, trade.getActions()));
			}
		}
	}

}
