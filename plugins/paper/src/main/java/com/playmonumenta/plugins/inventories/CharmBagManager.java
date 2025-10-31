package com.playmonumenta.plugins.inventories;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.commands.CharmBagCommand;
import com.playmonumenta.plugins.commands.MonumentaTrigger;
import com.playmonumenta.plugins.guis.CharmBagGui;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

public class CharmBagManager implements Listener {
	private static final String KEY_PLUGIN_DATA = "CharmBag";
	private static final String SKR_COMBAT_ROOM_SCORE = "SKRCombatRooms";
	private static final String SKR_PUZZLE_ROOM_SCORE = "SKRPuzzleRooms";

	public static final List<PlayerClass> classList = new MonumentaClasses().getClasses();
	public static final List<String> classListString = new ArrayList<>();

	public static @MonotonicNonNull ImmutableMap<String, ItemStack> CLASS_ICONS;

	public static void initialize(Location loc) {
		// Build a map that contains all the classes with their icons:
		ImmutableMap.Builder<String, ItemStack> mapBuilder = ImmutableMap.builder();
		for (PlayerClass pClass : classList) {
			mapBuilder.put(pClass.mClassName, GUIUtils.createBasicItem(pClass.mDisplayItem, 1, pClass.mClassName,
				pClass.mClassColor, true, Component.empty(), 30, true));
			classListString.add(pClass.mClassName);
		}
		// Include Generalist charms:
		mapBuilder.put("Generalist", GUIUtils.createBasicItem(Material.ANVIL, 1, "Generalist",
			NamedTextColor.GRAY, true, Component.empty(), 30, true));
		classListString.add("Generalist");
		CLASS_ICONS = mapBuilder.build();
	}

	public record CharmBagSettings(Tier mMaxTier, boolean allCharms) {
	}

	// Charm bag settings currently not really used, but keeping functionality there in case it becomes relevant
	public static final CharmBagSettings MAX_SETTINGS = new CharmBagSettings(Tier.CHARM, true);
	private static final ImmutableMap<ItemUtils.ItemIdentifier, CharmBagSettings> CHARM_BAG_SETTINGS = ImmutableMap.of(
		new ItemUtils.ItemIdentifier(Material.DECORATED_POT, "Portable Gatecharm"), MAX_SETTINGS
	);

	private static final Map<UUID, CharmBag> mCharmBags = new HashMap<>();

	@EventHandler(ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		JsonObject charmBagData = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), KEY_PLUGIN_DATA);
		if (charmBagData != null) {
			mCharmBags.put(player.getUniqueId(), CharmBag.deserialize(player, charmBagData));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onSave(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		CharmBag charmBag = mCharmBags.get(player.getUniqueId());
		if (charmBag != null) {
			event.setPluginData(KEY_PLUGIN_DATA, charmBag.serialize());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!player.isOnline()) {
				mCharmBags.remove(player.getUniqueId());
			}
		}, 100);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		CharmBagSettings settings = getSettings(event.getCurrentItem());
		if (settings != null
			&& event.getCurrentItem().getAmount() == 1
			&& event.getWhoClicked() instanceof Player player
			&& player.hasPermission("monumenta.usecharmbag")) {

			ItemStack charmBagItem = event.getCurrentItem();
			CharmBag charmBag = getCharmBag(player);

			if (ItemUtils.isNullOrAir(event.getCursor())) {
				if (event.getClick() == ClickType.RIGHT) {
					// Open Charm Bag
					event.setCancelled(true);
					if (checkNotSoulbound(player, charmBagItem) || checkInvalidCompletionScore(player, charmBagItem)) {
						return;
					}
					player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1.0f, 0.3f);
					new CharmBagGui(player, charmBag, settings, charmBagItem.getItemMeta().displayName()).open();
				} else if (event.getClick() == ClickType.SWAP_OFFHAND) {
					// Quick-Fill Charm Bag
					event.setCancelled(true);
					GUIUtils.refreshOffhand(event);
					if (checkNotSoulbound(player, charmBagItem) || checkInvalidCompletionScore(player, charmBagItem)) {
						return;
					}
					int deposited = 0;
					Map<String, Integer> depositedItems = new TreeMap<>();
					ItemStack[] inventoryItems = player.getInventory().getStorageContents();
					for (int i = 9; i < inventoryItems.length; i++) {
						ItemStack item = inventoryItems[i];
						if (canPutIntoCharmBag(item, settings)) {
							deposited += item.getAmount();
							depositedItems.merge(ItemUtils.getPlainName(item), item.getAmount(), Integer::sum);
							charmBag.add(player, item);
						}
					}
					if (deposited > 0) {
						String undoCommand = "/charmbag withdraw " + depositedItems.entrySet().stream().map(e -> e.getValue() + " " + e.getKey()).collect(Collectors.joining(", "));
						Component undoTooltip = Component.text("Click to retrieve these items again", NamedTextColor.GRAY);
						if (undoCommand.length() >= 255) {
							undoCommand = MonumentaTrigger.makeTrigger(player, false, p -> {
								try {
									CharmBagCommand.withdrawFromCharmBag(p, depositedItems.entrySet().stream().map(e -> new CharmBagCommand.CharmBagCommandItem(e.getKey(), e.getValue())).toList());
								} catch (WrapperCommandSyntaxException e) {
									p.sendMessage(Component.text(e.getRawMessage().getString(), NamedTextColor.RED));
								}
							});
							undoTooltip = undoTooltip.append(Component.newline()).append(Component.text("This button only works once due to", TextColor.color(255, 127, 0)))
								.append(Component.newline()).append(Component.text("the large number of items deposited!", TextColor.color(255, 127, 0)));
						}
						String depositedHoverString = depositedItems.entrySet().stream().map(e -> e.getValue() + " " + e.getKey())
							.collect(Collectors.joining("\n"));
						player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1.0f, 0.3f);
						player.sendMessage(Component.text(deposited + " item" + (deposited == 1 ? "" : "s") + " deposited into your " + ItemUtils.getPlainName(charmBagItem), NamedTextColor.GOLD)
							.hoverEvent(HoverEvent.showText(Component.text(depositedHoverString, NamedTextColor.GRAY)))
							.append(Component.text(" "))
							.append(Component.text("[undo]", NamedTextColor.GRAY)
								.hoverEvent(HoverEvent.showText(undoTooltip))
								.clickEvent(ClickEvent.runCommand(undoCommand)))
						);
					}
				}
			} else {
				if (event.getClick() == ClickType.RIGHT) {
					event.setCancelled(true);
					if (checkNotSoulbound(player, charmBagItem) || checkInvalidCompletionScore(player, charmBagItem)) {
						return;
					}
					ItemStack cursor = event.getCursor();
					if (canPutIntoCharmBag(cursor, settings)) {
						charmBag.add(player, cursor);
						event.getView().setCursor(cursor);
						player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1.0f, 0.3f);
						player.sendMessage(Component.text("Item deposited into your " + ItemUtils.getPlainName(charmBagItem), NamedTextColor.GOLD));
					} else {
						// Special error message if the item is a disallowed charm (non-regular tier)
						if (!isCharm(cursor) && (ItemStatUtils.isNormalCharm(cursor) || ItemStatUtils.isZenithCharm(cursor))) {
							player.sendMessage(Component.text("This type of charm cannot be put into the " + ItemUtils.getPlainName(charmBagItem), NamedTextColor.RED));
						} else {
							player.sendMessage(Component.text("This item cannot be put into the " + ItemUtils.getPlainName(charmBagItem), NamedTextColor.RED));
						}
						player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
					}
				}
			}
		}
	}

	// Might not need if the bags aren't edible
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		if (isCharmBag(event.getItem())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (isCharmBag(event.getItem())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof ItemFrame)
			&& !(event.getRightClicked() instanceof ArmorStand)
			&& isCharmBag(event.getPlayer().getEquipment().getItem(event.getHand()))) {
			event.setCancelled(true);
		}
	}

	public static CharmBag getCharmBag(Player player) {
		return mCharmBags.computeIfAbsent(player.getUniqueId(), CharmBag::new);
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

	private static boolean checkInvalidCompletionScore(Player player, ItemStack charmBagItem) {
		// SKR Room Completion Requirement (2 rooms or more)
		if (ScoreboardUtils.getScoreboardValue(player, SKR_COMBAT_ROOM_SCORE).orElse(0) +
			ScoreboardUtils.getScoreboardValue(player, SKR_PUZZLE_ROOM_SCORE).orElse(0) < 2) {
			player.sendMessage(Component.text("You must complete 2 Silver Knight's Remnants rooms before using the " + ItemUtils.getPlainName(charmBagItem) + "!", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return true;
		}
		return false;
	}

	public static boolean canPutIntoCharmBag(ItemStack item, CharmBagSettings settings) {
		return item != null
			&& item.getAmount() > 0
			&& isCharm(item)
			&& ItemStatUtils.getPlayerModified(new NBTItem(item)) == null;
		//&& ItemStatUtils.getTier(item).compareTo(settings.mMaxTier) <= 0
		//&& (settings.allCharms || ItemStatUtils.getTier(item).compareTo(settings.mMaxTier) < 0);
	}

	public static boolean isCharm(ItemStack item) {
		return !ItemUtils.isNullOrAir(item)
			&& (ItemStatUtils.getTier(item) == Tier.CHARM);
	}

	@Contract("null -> null")
	public static @Nullable CharmBagSettings getSettings(@Nullable ItemStack item) {
		return item == null ? null : CHARM_BAG_SETTINGS.get(ItemUtils.getIdentifier(item, false));
	}

	public static boolean isCharmBag(@Nullable ItemStack itemStack) {
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

	@Nullable
	public static ItemStack playerGetCharmBagItem(final Player player) {
		for (ItemStack item : player.getInventory().getContents()) {
			if (isCharmBag(item)) {
				return item;
			}
		}
		return null;
	}
}
