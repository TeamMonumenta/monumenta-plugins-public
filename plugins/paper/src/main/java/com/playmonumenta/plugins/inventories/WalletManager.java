package com.playmonumenta.plugins.inventories;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.MonumentaTrigger;
import com.playmonumenta.plugins.commands.WalletCommand;
import com.playmonumenta.plugins.guis.WalletGui;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class WalletManager implements Listener {

	private static final String KEY_PLUGIN_DATA = "Wallet";

	public static final ImmutableMap<Region, ItemStack> REGION_ICONS = ImmutableMap.of(
		Region.VALLEY, ItemUtils.parseItemStack("{id:\"minecraft:cyan_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"sc\",Color:3},{Pattern:\"mc\",Color:11},{Pattern:\"flo\",Color:15},{Pattern:\"bts\",Color:11},{Pattern:\"tts\",Color:11}]},HideFlags:63,display:{Name:'{\"text\":\"King\\'s Valley\",\"italic\":false,\"bold\":true,\"color\":\"aqua\"}'}}}"),
		Region.ISLES, ItemUtils.parseItemStack("{id:\"minecraft:green_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"gru\",Color:5},{Pattern:\"bo\",Color:13},{Pattern:\"mr\",Color:13},{Pattern:\"mc\",Color:5}]},HideFlags:63,display:{Name:'{\"text\":\"Celsian Isles\",\"italic\":false,\"bold\":true,\"color\":\"green\"}'}}}"),
		Region.RING, ItemUtils.parseItemStack("{id:\"minecraft:white_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"ss\",Color:12},{Pattern:\"bts\",Color:13},{Pattern:\"tts\",Color:13},{Pattern:\"gra\",Color:8},{Pattern:\"ms\",Color:13},{Pattern:\"gru\",Color:7},{Pattern:\"flo\",Color:15},{Pattern:\"mc\",Color:0}]},HideFlags:63,display:{Name:'{\"bold\":true,\"italic\":false,\"underlined\":false,\"color\":\"white\",\"text\":\"Architect\\\\u0027s Ring\"}'}}}")
	);

	public static @MonotonicNonNull ImmutableList<ItemStack> MANUAL_SORT_ORDER;

	public static @MonotonicNonNull ImmutableList<ItemStack> MAIN_CURRENCIES;

	/**
	 * Compressible currency items.
	 * If an item has multiple compression levels, they must be ordered from most compressed to least compressed.
	 * NB: some code depends on the assumption that only the base currencies have more than 2 tiers of compression.
	 */
	public static @MonotonicNonNull ImmutableList<CompressionInfo> COMPRESSIBLE_CURRENCIES;

	public static void initialize(Location loc) {

		MAIN_CURRENCIES = Stream.of(
				// r1
				"epic:r1/items/currency/hyper_experience",
				"epic:r1/items/currency/concentrated_experience",
				"epic:r1/items/currency/experience",

				// r2
				"epic:r2/items/currency/hyper_crystalline_shard",
				"epic:r2/items/currency/compressed_crystalline_shard",
				"epic:r2/items/currency/crystalline_shard",

				// r3
				"epic:r3/items/currency/hyperchromatic_archos_ring",
				"epic:r3/items/currency/archos_ring"
			).map(path -> InventoryUtils.getItemFromLootTableOrWarn(loc, NamespacedKeyUtils.fromString(path)))
			                  .filter(Objects::nonNull) // for build shard
			                  .collect(ImmutableList.toImmutableList());

		MANUAL_SORT_ORDER = Stream.of(
				// r1
				"epic:r1/items/currency/pulsating_gold_bar",
				"epic:r1/items/currency/pulsating_gold",
				"epic:r1/fragments/royal_dust",
				"epic:r1/transmog/rare_frag",
				"epic:r1/items/currency/pulsating_dust",
				"epic:r1/items/currency/pulsating_dust_frag",

				// r2
				"epic:r2/items/currency/pulsating_emerald_block",
				"epic:r2/items/currency/pulsating_emerald",
				"epic:r2/fragments/gleaming_dust",
				"epic:r2/transmog/rare_frag",
				"epic:r2/transmog/pulsating_powder",
				"epic:r2/transmog/t5_frag",

				// r3
				"epic:r3/items/currency/pulsating_diamond",
				"epic:r3/items/currency/silver_dust",
				"epic:r3/transmog/melted_candle",
				"epic:r3/items/currency/alacrity_augment",
				"epic:r3/items/currency/fortitude_augment",
				"epic:r3/items/currency/potency_augment",
				"epic:r3/items/currency/pulsating_shard",
				"epic:r3/transmog/pulsating_shard_fragment"
			).map(path -> InventoryUtils.getItemFromLootTableOrWarn(loc, NamespacedKeyUtils.fromString(path)))
			                    .filter(Objects::nonNull) // for build shard
			                    .collect(ImmutableList.toImmutableList());

		COMPRESSIBLE_CURRENCIES = Stream.of(
			// r1
			CompressionInfo.create("epic:r1/items/currency/hyper_experience",
				"epic:r1/items/currency/experience", 64 * 8, loc),
			CompressionInfo.create("epic:r1/items/currency/concentrated_experience",
				"epic:r1/items/currency/experience", 8, loc),

			CompressionInfo.create("epic:r1/items/currency/pulsating_gold_bar",
				"epic:r1/fragments/royal_dust", 2 * 8, loc),
			CompressionInfo.create("epic:r1/items/currency/pulsating_gold",
				"epic:r1/fragments/royal_dust", 2, loc),

			// r2
			CompressionInfo.create("epic:r2/items/currency/hyper_crystalline_shard",
				"epic:r2/items/currency/crystalline_shard", 64 * 8, loc),
			CompressionInfo.create("epic:r2/items/currency/compressed_crystalline_shard",
				"epic:r2/items/currency/crystalline_shard", 8, loc),

			CompressionInfo.create("epic:r2/items/currency/pulsating_emerald_block",
				"epic:r2/fragments/gleaming_dust", 2 * 8, loc),
			CompressionInfo.create("epic:r2/items/currency/pulsating_emerald",
				"epic:r2/fragments/gleaming_dust", 2, loc),

			// r3
			CompressionInfo.create("epic:r3/items/currency/hyperchromatic_archos_ring",
				"epic:r3/items/currency/archos_ring", 64, loc),

			CompressionInfo.create("epic:r3/items/currency/pulsating_diamond",
				"epic:r3/items/currency/silver_dust", 2, loc),

			// misc
			CompressionInfo.create("epic:soul/twisted_soul_thread",
				"epic:soul/soul_thread", 8, loc),
			CompressionInfo.create("epic:r2/blackmist/spectral_maradevi",
				"epic:r2/blackmist/spectral_doubloon", 8, loc),
			CompressionInfo.create("epic:r2/items/currency/carnival_ticket_wheel",
				"epic:r2/items/currency/carnival_ticket_bundle", 10, loc),
			CompressionInfo.create("epic:r2/dungeons/rushdown/a_dis_energy",
				"epic:r2/dungeons/rushdown/dis_energy", 100, loc)
		).filter(Objects::nonNull).collect(ImmutableList.toImmutableList());
	}

	public record WalletSettings(Region mMaxRegion, boolean allCurrencies) {
	}

	public static final WalletSettings MAX_SETTINGS = new WalletSettings(Region.RING, true);
	private static final ImmutableMap<ItemUtils.ItemIdentifier, WalletSettings> WALLET_SETTINGS = ImmutableMap.of(
		new ItemUtils.ItemIdentifier(Material.GLASS_BOTTLE, "Experience Flask"), new WalletSettings(Region.VALLEY, false),
		new ItemUtils.ItemIdentifier(Material.CAULDRON, "Experience Bucket"), new WalletSettings(Region.VALLEY, true),
		new ItemUtils.ItemIdentifier(Material.AMETHYST_CLUSTER, "Crystal Cluster"), new WalletSettings(Region.ISLES, false),
		new ItemUtils.ItemIdentifier(Material.AMETHYST_BLOCK, "Crystal Collector"), new WalletSettings(Region.ISLES, true),
		new ItemUtils.ItemIdentifier(Material.PORKCHOP, "Piggy Bank"), new WalletSettings(Region.RING, false),
		new ItemUtils.ItemIdentifier(Material.FLOWER_POT, "Bag of Hoarding"), MAX_SETTINGS
	);

	private static final Map<UUID, Wallet> mWallets = new HashMap<>();

	public static class CompressionInfo {
		public final ItemStack mCompressed;
		public final ItemStack mBase;
		public final int mAmount;

		CompressionInfo(ItemStack compressed, ItemStack base, int amount) {
			mCompressed = compressed;
			mBase = base;
			mAmount = amount;
		}

		static @Nullable CompressionInfo create(String compressed, String base, int amount, Location loc) {
			ItemStack compressedItem = InventoryUtils.getItemFromLootTableOrWarn(loc, NamespacedKeyUtils.fromString(compressed));
			if (compressedItem == null) {
				return null;
			}
			ItemStack baseItem = InventoryUtils.getItemFromLootTableOrWarn(loc, NamespacedKeyUtils.fromString(base));
			if (baseItem == null) {
				return null;
			}
			return new CompressionInfo(compressedItem, baseItem, amount);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		JsonObject walletData = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), KEY_PLUGIN_DATA);
		if (walletData != null) {
			mWallets.put(player.getUniqueId(), Wallet.deserialize(player, walletData));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onSave(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		Wallet wallet = mWallets.get(player.getUniqueId());
		if (wallet != null) {
			event.setPluginData(KEY_PLUGIN_DATA, wallet.serialize());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!p.isOnline()) {
				mWallets.remove(p.getUniqueId());
			}
		}, 100);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		WalletSettings settings = getSettings(event.getCurrentItem());
		if (settings != null
			    && event.getCurrentItem().getAmount() == 1
			    && event.getWhoClicked() instanceof Player player
			    && player.hasPermission("monumenta.usewallet")) {

			ItemStack walletItem = event.getCurrentItem();

			Wallet wallet = getWallet(player);

			if (ItemUtils.isNullOrAir(event.getCursor())) {
				if (event.getClick() == ClickType.RIGHT) {
					// open wallet
					event.setCancelled(true);
					if (checkNotSoulbound(player, walletItem)) {
						return;
					}
					player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
					new WalletGui(player, wallet, settings, walletItem.getItemMeta().displayName()).open();
				} else if (event.getClick() == ClickType.SWAP_OFFHAND) {
					// quick-fill wallet
					event.setCancelled(true);
					GUIUtils.refreshOffhand(event);
					if (checkNotSoulbound(player, walletItem)) {
						return;
					}
					int deposited = 0;
					Map<String, Integer> depositedItems = new TreeMap<>();
					ItemStack[] inventoryItems = player.getInventory().getStorageContents();
					for (int i = 9; i < inventoryItems.length; i++) {
						ItemStack item = inventoryItems[i];
						if (canPutIntoWallet(item, settings)) {
							deposited += item.getAmount();
							depositedItems.merge(ItemUtils.getPlainName(item), item.getAmount(), Integer::sum);
							wallet.add(player, item);
						}
					}
					if (deposited > 0) {
						String undoCommand = "/wallet withdraw " + depositedItems.entrySet().stream().map(e -> e.getValue() + " " + e.getKey()).collect(Collectors.joining(", "));
						Component undoTooltip = Component.text("Click to retrieve these items again", NamedTextColor.GRAY);
						if (undoCommand.length() >= 255) {
							undoCommand = MonumentaTrigger.makeTrigger(player, false, p -> {
								try {
									WalletCommand.withdrawFromWallet(p, depositedItems.entrySet().stream().map(e -> new WalletCommand.WalletCommandItem(e.getKey(), e.getValue())).toList());
								} catch (WrapperCommandSyntaxException e) {
									p.sendMessage(Component.text(e.getRawMessage().getString(), NamedTextColor.RED));
								}
							});
							undoTooltip = undoTooltip.append(Component.newline()).append(Component.text("This button only works once due to", TextColor.color(255, 127, 0)))
								              .append(Component.newline()).append(Component.text("the large number of items deposited!", TextColor.color(255, 127, 0)));
						}
						String depositedHoverString = depositedItems.entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
							                              .collect(Collectors.joining("\n"));
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
						player.sendMessage(Component.text(deposited + " item" + (deposited == 1 ? "" : "s") + " deposited into your " + ItemUtils.getPlainName(walletItem), NamedTextColor.GOLD)
							                   .hoverEvent(HoverEvent.showText(Component.text(depositedHoverString, NamedTextColor.GRAY)))
							                   .append(Component.text(" "))
							                   .append(Component.text("[undo]", NamedTextColor.GRAY)
								                           .hoverEvent(HoverEvent.showText(undoTooltip))
								                           .clickEvent(ClickEvent.runCommand(undoCommand))));
					}
				}
			} else {
				if (event.getClick() == ClickType.RIGHT) {
					event.setCancelled(true);
					if (checkNotSoulbound(player, walletItem)) {
						return;
					}
					ItemStack cursor = event.getCursor();
					if (canPutIntoWallet(cursor, settings)) {
						wallet.add(player, cursor);
						event.getView().setCursor(cursor);
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
						player.sendMessage(Component.text("Item deposited into your " + ItemUtils.getPlainName(walletItem), NamedTextColor.GOLD));
					} else {
						player.sendMessage(Component.text("This item cannot be put into the " + ItemUtils.getPlainName(walletItem), NamedTextColor.RED));
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		if (isWallet(event.getItem())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (isWallet(event.getItem())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof ItemFrame)
			    && !(event.getRightClicked() instanceof ArmorStand)
			    && isWallet(event.getPlayer().getEquipment().getItem(event.getHand()))) {
			event.setCancelled(true);
		}
	}

	public static Wallet getWallet(Player player) {
		return mWallets.computeIfAbsent(player.getUniqueId(), Wallet::new);
	}

	private static boolean checkNotSoulbound(Player player, ItemStack item) {
		if (ItemStatUtils.getInfusionLevel(item, InfusionType.SOULBOUND) > 0
			    && !player.getUniqueId().equals(ItemStatUtils.getInfuser(item, InfusionType.SOULBOUND))) {
			player.sendMessage(Component.text("This " + ItemUtils.getPlainName(item) + " does not belong to you!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return true;
		}
		return false;
	}

	public static boolean canPutIntoWallet(ItemStack item, WalletSettings settings) {
		return item != null
			       && item.getAmount() > 0
			       && isCurrency(item)
			       && ItemStatUtils.getPlayerModified(new NBTItem(item)) == null
			       && ItemStatUtils.getRegion(item).compareTo(settings.mMaxRegion) <= 0
			       && (settings.allCurrencies || ItemStatUtils.getRegion(item).compareTo(settings.mMaxRegion) < 0 || MAIN_CURRENCIES.stream().anyMatch(c -> c.isSimilar(item)));
	}

	public static boolean isCurrency(ItemStack item) {
		return !ItemUtils.isNullOrAir(item)
			       && (ItemStatUtils.getTier(item) == Tier.CURRENCY
				           || ItemStatUtils.getTier(item) == Tier.EVENT_CURRENCY
				           || InventoryUtils.testForItemWithLore(item, "Can be put into a wallet."));
	}

	public static boolean isBase(ItemStack item) {
		for (CompressionInfo compressionInfo : COMPRESSIBLE_CURRENCIES) {
			if (compressionInfo.mBase.isSimilar(item)) {
				return true;
			}
		}
		return false;
	}

	public static List<CompressionInfo> getAllCompressionInfoMatchingBase(ItemStack item) {
		List<CompressionInfo> out = new ArrayList<>();
		for (CompressionInfo compressionInfo : COMPRESSIBLE_CURRENCIES) {
			if (compressionInfo.mBase.isSimilar(item)) {
				out.add(compressionInfo);
			}
		}
		out.sort(Comparator.comparingInt(o -> o.mAmount));
		return out;
	}

	public static @Nullable CompressionInfo getCompressionInfo(ItemStack item) {
		for (CompressionInfo compressionInfo : COMPRESSIBLE_CURRENCIES) {
			if (compressionInfo.mCompressed.isSimilar(item)) {
				return compressionInfo;
			}
		}
		return null;
	}

	public static ItemStack decompress(ItemStack item) {
		for (CompressionInfo compressionInfo : COMPRESSIBLE_CURRENCIES) {
			if (compressionInfo.mCompressed.isSimilar(item)) {
				ItemStack compressed = ItemUtils.clone(compressionInfo.mBase);
				compressed.setAmount(item.getAmount() * compressionInfo.mAmount);
				return compressed;
			}
		}
		return item;
	}

	public static ItemStack compress(ItemStack item) {
		for (CompressionInfo compressionInfo : COMPRESSIBLE_CURRENCIES) {
			if (compressionInfo.mBase.isSimilar(item) && item.getAmount() % compressionInfo.mAmount == 0) {
				ItemStack compressed = ItemUtils.clone(compressionInfo.mCompressed);
				compressed.setAmount(item.getAmount() / compressionInfo.mAmount);
				return compressed;
			}
		}
		return item;
	}

	@Contract("null -> null")
	public static @Nullable WalletManager.WalletSettings getSettings(@Nullable ItemStack item) {
		return item == null ? null : WALLET_SETTINGS.get(ItemUtils.getIdentifier(item, false));
	}

	public static boolean isWallet(@Nullable ItemStack itemStack) {
		return getSettings(itemStack) != null;
	}

	private static final Pattern OPERATION_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([-+*/])\\s*(\\d+(?:\\.\\d+)?)");

	public static double parseDoubleOrCalculation(String line) throws NumberFormatException {
		try {
			return Double.parseDouble(line);
		} catch (NumberFormatException e) {
			Matcher matcher = OPERATION_PATTERN.matcher(line);
			if (matcher.matches()) {
				double n1 = Double.parseDouble(matcher.group(1));
				double n2 = Double.parseDouble(matcher.group(3));
				return switch (matcher.group(2)) {
					case "-" -> n1 - n2;
					case "+" -> n1 + n2;
					case "*" -> n1 * n2;
					case "/" -> n1 / n2;
					default -> 0;
				};
			} else {
				throw e;
			}
		}
	}

	/**
	 * Give a player a currency ItemStack, with an option to notify them.
	 * If the player has a wallet, add it to the wallet. If not, add it to the inventory.
	 * If the inventory is full, we drop it on the ground.
	 * If <code>notify</code> is <code>true</code>, the player will receive a message informing them of the addition.
	 * @see InventoryUtils#playerHasWalletItem(Player)
	 * @see InventoryUtils#giveItemWithStacksizeCheck(Player, ItemStack)
	 *
	 * @param player The player to give the currency to.
	 * @param item The currency item.
	 * @param notify Whether to send the player a message about it aswell.
	 */
	public static void giveCurrencyToPlayer(final Player player, ItemStack item, boolean notify) {
		if (!isCurrency(item)) {
			MMLog.warning("Attempted to give non-currency item " + item + " to player " + player.getName());
			return;
		}
		int amount = item.getAmount();
		if (notify) {
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
		}
		ItemStack walletItem = playerGetWalletItem(player);
		if (walletItem != null) {
			WalletSettings settings = getSettings(walletItem);
			if (settings != null && canPutIntoWallet(item, settings)) {
				if (notify) {
					player.sendMessage(Component.text(item.getAmount() + " item" + (amount == 1 ? "" : "s") + " deposited into your wallet.", NamedTextColor.GOLD)
						.hoverEvent(HoverEvent.showText(Component.text(amount + " " + ItemUtils.getPlainNameOrDefault(item), NamedTextColor.GRAY))));
				}
				getWallet(player).add(player, item); // this method sets the count of the item to 0, so we need to do it after notifying.
				return;
			}
		}
		InventoryUtils.giveItemWithStacksizeCheck(player, item);
		if (notify) {
			player.sendMessage(Component.text("Given ", NamedTextColor.GREEN).append(Component.text(amount + " " + ItemUtils.getPlainNameOrDefault(item) + ".", NamedTextColor.WHITE)));
		}

	}

	@Nullable
	public static ItemStack playerGetWalletItem(final Player player) {
		for (ItemStack item : player.getInventory().getContents()) {
			if (isWallet(item)) {
				return item;
			}
		}
		return null;
	}

}
