package com.playmonumenta.plugins.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.overrides.YellowTesseractOverride;
import com.playmonumenta.plugins.parrots.ParrotManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Nullable;

public class LoadoutManager implements Listener {

	private static final String KEY_PLUGIN_DATA = "MonumentaLoadouts";

	public static final String LOADOUT_MANAGER_NAME = "Mechanical Armory";
	public static final String STORAGE_SHULKER_NAME = "Equipment Case";

	public static final int MAX_SHULKERS_IN_STORAGE_SHULKER = 5;

	private static final int[] EQUIPMENT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 36, 37, 38, 39, 40};

	private final Map<UUID, LoadoutData> mData = new HashMap<>();

	public LoadoutData getData(Player player) {
		return mData.computeIfAbsent(player.getUniqueId(), key -> new LoadoutData());
	}

	private static ItemUtils.ItemIdentifier parseItemIdentifier(JsonObject json, String typeProperty, String nameProperty) {
		JsonPrimitive name = json.getAsJsonPrimitive(nameProperty);
		return new ItemUtils.ItemIdentifier(Material.valueOf(json.getAsJsonPrimitive(typeProperty).getAsString()),
			name == null ? null : name.getAsString());
	}

	public static boolean isEquipmentStorageBox(@Nullable ItemStack item) {
		return item != null && ItemUtils.isShulkerBox(item.getType()) && STORAGE_SHULKER_NAME.equals(ItemUtils.getPlainNameIfExists(item));
	}

	@SuppressWarnings("unused") // no, ErrorProne, these are used ._.
	private record ItemInventory(Inventory mInventory, Runnable mSaveAction) {
	}

	public void swapTo(Player player, Loadout loadout, boolean full) {
		// If the player has a shulker open, e.g. an equipment case, close it and immediately save
		if (ShulkerInventoryManager.playerHasShulkerOpen(player)) {
			Plugin.getInstance().mShulkerInventoryManager.closeShulker(player, true);
			player.closeInventory();
		}

		AtomicBoolean swappedEquipment = new AtomicBoolean(false);
		boolean swappedVanity = false;
		boolean swappedCharms = false;
		boolean swappedClass = false;
		if (loadout.mIncludeEquipment || loadout.mIncludeCharms) {
			if (!ShulkerEquipmentListener.checkAllowedToSwapEquipment(player)) {
				return;
			}

			if (!Plugin.getInstance().mShulkerEquipmentListener.checkCooldown(player, LOADOUT_MANAGER_NAME)) {
				return;
			}

			StatTrackManager.getInstance().updateInventory(player);

			// first, grab all inventories (player, ender chest, all eligible shulkers)
			// then for each loadout item, iterate over all inventories to find it and swap it with the current item (keep current item equipped if item cannot be found or is empty in the loadout)
			// at the end, move any extra charms into any free shulker (prefer the shulkers where charms were taken from though)
			List<ItemInventory> inventories = new ArrayList<>();
			Consumer<Inventory> findStorageShulkers = inventory -> {
				for (ItemStack item : inventory.getContents()) {
					if (item != null
						    && isEquipmentStorageBox(item)
						    && item.getItemMeta() instanceof BlockStateMeta blockStateMeta
						    && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
						inventories.add(new ItemInventory(shulkerBox.getInventory(), () -> {
							blockStateMeta.setBlockState(shulkerBox);
							item.setItemMeta(blockStateMeta);
						}));
					}
				}
			};
			findStorageShulkers.accept(player.getInventory());
			boolean hasEnderChest = ItemUtils.hasPortableEnderOrIsNearEnderChest(player);
			if (hasEnderChest) {
				findStorageShulkers.accept(player.getEnderChest());
				inventories.add(new ItemInventory(player.getEnderChest(), () -> {
				}));
			}
			inventories.add(new ItemInventory(player.getInventory(), () -> {
			}));

			try {
				// Swap equipment
				if (loadout.mIncludeEquipment) {
					List<ItemUtils.ItemIdentifier> itemsNotFound = new ArrayList<>();
					Set<Integer> swappedSlots = new HashSet<>();
					BiPredicate<LoadoutItem, Boolean> findAndSwapItem = (loadoutItem, matchInfusion) -> {
						ItemStack playerItem = player.getInventory().getItem(loadoutItem.mSlot);
						if (isEquipmentStorageBox(playerItem)) {
							// Must not move around these boxes - their inventory is currently open
							player.sendMessage(Component.text("Please remove the " + STORAGE_SHULKER_NAME + " from your hotbar to swap loadouts properly!", NamedTextColor.RED));
							return true;
						}
						if (isLoadoutItem(loadoutItem, matchInfusion, playerItem)) {
							// Item already equipped, nothing to do
							return true;
						}
						for (ItemInventory inventory : inventories) {
							for (int i = 0; i < inventory.mInventory.getSize(); i++) {
								if (inventory.mInventory.equals(player.getInventory()) && swappedSlots.contains(i)) {
									// Don't take items from slots we already filled
									continue;
								}
								ItemStack newItem = inventory.mInventory.getItem(i);
								if (newItem != null && isLoadoutItem(loadoutItem, matchInfusion, newItem)) {
									swappedEquipment.set(true);

									ItemStack newItemClone = ItemUtils.clone(newItem);
									if (loadoutItem.mSlot > 9) { // armor/offhand: take only one
										newItemClone.setAmount(1);
										newItem.subtract();
									} else { // hotbar: take entire stack
										newItem.setAmount(0);
									}
									player.getInventory().setItem(loadoutItem.mSlot, newItemClone);

									if (!ItemUtils.isNullOrAir(playerItem)) {
										if (newItem.getAmount() == 0
											    && (!ItemUtils.isShulkerBox(playerItem.getType())
												        || inventory.mInventory.equals(player.getInventory())
												        || inventory.mInventory.equals(player.getEnderChest())
												        || (ShulkerEquipmentListener.canSwapItem(playerItem)
													            && canPutMoreShulkersIntoEquipmentBox(inventory.mInventory)))) {
											// Swap item if that is allowed: target slot is empty, and:
											// - the item swapped is not a shulker box
											// - or is swapping to inventory or ender chest
											// - or is an allowed shulker box and the targeted equipment case is below the shulker box limit
											inventory.mInventory.setItem(i, playerItem);
										} else {
											// Otherwise, put into any valid inventory
											giveItem(player, inventories, playerItem);
										}
									}
									return true;
								}
							}
						}
						return false;
					};
					for (LoadoutItem loadoutItem : loadout.mEquipment) {
						if (!findAndSwapItem.test(loadoutItem, true) && !findAndSwapItem.test(loadoutItem, false)) {
							itemsNotFound.add(loadoutItem.mIdentifier);
						} else {
							swappedSlots.add(loadoutItem.mSlot);
						}
					}
					if (!itemsNotFound.isEmpty()) {
						player.sendMessage(Component.text("Could not find all equipment items for this loadout! Hover this message to see missing items.", NamedTextColor.RED)
							                   .hoverEvent(itemListHover(itemsNotFound)));
					}

					if (loadout.mClearEmpty) {
						for (int slot : EQUIPMENT_SLOTS) {
							if (loadout.mEquipment.stream().noneMatch(i -> i.mSlot == slot)) {
								ItemStack playerItem = player.getInventory().getItem(slot);
								if (isEquipmentStorageBox(playerItem)) {
									// Must not move around these boxes - their inventory is currently open
									player.sendMessage(Component.text("Please remove the " + STORAGE_SHULKER_NAME + " from your hotbar to swap loadouts properly!", NamedTextColor.RED));
									continue;
								}
								if (playerItem != null && !playerItem.getType().isAir()) {
									player.getInventory().setItem(slot, null);
									giveItem(player, inventories, playerItem);
									swappedEquipment.set(true);
								}
							}
						}
					}

					if (swappedEquipment.get()) {
						// Reset player's attack cooldown so it cannot be exploited by swapping from high to low cooldown items of the same type
						PlayerUtils.resetAttackCooldown(player);
					}

				}

				// Swap charms
				if (full && loadout.mIncludeCharms) {
					List<ItemUtils.ItemIdentifier> itemsNotFound = new ArrayList<>();
					List<ItemUtils.ItemIdentifier> failedItems = new ArrayList<>();
					List<ItemStack> activeCharms = Plugin.getInstance().mCharmManager.getCharms(player);
					Set<ItemStack> oldCharmsSet = new HashSet<>(activeCharms);
					List<ItemStack> oldCharms = new ArrayList<>(activeCharms);
					activeCharms.clear();
					List<Inventory> charmInventories = new ArrayList<>(); // Try to put extra charms in the same boxes where we got the charms from

					// Equip new charms, swapping with old charms as far as possible
					charmLoop:
					for (ItemUtils.ItemIdentifier charmIdentifier : loadout.mCharms) {
						// First, check if equipped charms match and re-equip them if so
						for (Iterator<ItemStack> iterator = oldCharms.iterator(); iterator.hasNext(); ) {
							ItemStack oldCharm = iterator.next();
							if (charmIdentifier.isIdentifierFor(oldCharm, false) && Plugin.getInstance().mCharmManager.validateCharm(player, oldCharm)) {
								activeCharms.add(oldCharm);
								iterator.remove();
								continue charmLoop;
							}
						}
						// only after check for charms elsewhere
						for (ItemInventory inventory : inventories) {
							for (int invI = 0; invI < inventory.mInventory.getSize(); invI++) {
								ItemStack newItem = inventory.mInventory.getItem(invI);
								if (newItem != null && charmIdentifier.isIdentifierFor(newItem, false)) {
									ItemStack newItemClone = ItemUtils.clone(newItem);
									newItemClone.setAmount(1);
									if (!Plugin.getInstance().mCharmManager.validateCharm(player, newItemClone)) {
										failedItems.add(charmIdentifier);
										continue charmLoop;
									}
									newItem.subtract();
									activeCharms.add(newItemClone);
									if (!oldCharms.isEmpty()) {
										ItemStack oldCharm = oldCharms.remove(0);
										if (newItem.getAmount() == 0) {
											inventory.mInventory.setItem(invI, oldCharm);
										} else {
											giveItem(player, inventories, oldCharm);
										}
									}
									if (!charmInventories.contains(inventory.mInventory)) {
										charmInventories.add(inventory.mInventory);
									}
									continue charmLoop;
								}
							}
						}
						itemsNotFound.add(charmIdentifier);
					}

					// Put any extra old charms into any free storage boxes, prioritising the boxes where charms were swapped from
					charmLoop:
					for (ItemStack charm : oldCharms) {
						for (Inventory charmInventory : charmInventories) {
							if (InventoryUtils.canFitInInventory(charm, charmInventory)) {
								charmInventory.addItem(charm);
								continue charmLoop;
							}
						}
						for (ItemInventory inventory : inventories) {
							if (InventoryUtils.canFitInInventory(charm, inventory.mInventory)) {
								inventory.mInventory.addItem(charm);
								continue charmLoop;
							}
						}
						InventoryUtils.giveItem(player, charm);
					}

					swappedCharms = !oldCharmsSet.equals(new HashSet<>(activeCharms));

					if (swappedCharms) {
						Plugin.getInstance().mCharmManager.updateCharms(player, activeCharms);
					}
					if (!itemsNotFound.isEmpty()) {
						player.sendMessage(Component.text("Could not find all charms for this loadout! Hover this message to see missing charms.", NamedTextColor.RED)
							                   .hoverEvent(itemListHover(itemsNotFound)));
					}
					if (!failedItems.isEmpty()) {
						player.sendMessage(Component.text("Some charms could not be equipped, their charm power has likely changed since the loadout was created." +
							                                  " Hover this message to see affected charms.", NamedTextColor.RED)
							                   .hoverEvent(itemListHover(failedItems)));
					}

				}
			} catch (Exception e) {
				String message = "Exception in Loadout Manager swap code, items have likely been duped or deleted. Affected player: " + player.getName();
				AuditListener.logSevere(message);
				MMLog.severe(message, e);
			} finally {
				// Update shulker box items to new contents
				for (ItemInventory inventory : inventories) {
					inventory.mSaveAction.run();
				}
			}

			if (swappedEquipment.get() || swappedCharms) {
				Plugin.getInstance().mShulkerEquipmentListener.startCooldownIfApplicable(player, LOADOUT_MANAGER_NAME);
			}

		}

		if (loadout.mIncludeVanity) {
			VanityManager.VanityData vanityData = Plugin.getInstance().mVanityManager.getData(player);
			Map<EquipmentSlot, ItemStack> prevEquipped = new HashMap<>(vanityData.getEquipped());
			vanityData.setEquipped(loadout.mVanity, player);
			swappedVanity = !prevEquipped.equals(vanityData.getEquipped());

			if (ScoreboardUtils.getScoreboardValue(player, ParrotManager.SCOREBOARD_PARROT_LEFT).orElse(0) != loadout.mLeftParrot
				    || ScoreboardUtils.getScoreboardValue(player, ParrotManager.SCOREBOARD_PARROT_RIGHT).orElse(0) != loadout.mRightParrot) {
				swappedVanity = true;
				ScoreboardUtils.setScoreboardValue(player, ParrotManager.SCOREBOARD_PARROT_LEFT, loadout.mLeftParrot);
				ScoreboardUtils.setScoreboardValue(player, ParrotManager.SCOREBOARD_PARROT_RIGHT, loadout.mRightParrot);
				ParrotManager.updateParrots(player);
			}
		}

		// Swap class and abilities
		boolean safeZone = ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.RESIST_5);
		int yellowCooldown = YellowTesseractOverride.getCooldown(player);
		if (safeZone && yellowCooldown > 0) {
			YellowTesseractOverride.setCooldown(player, 0);
			yellowCooldown = 0;
		}
		if (full && loadout.mIncludeClass && !ServerProperties.getDepthsEnabled()) {
			Loadout testLoadout = new Loadout(-1, "test");
			testLoadout.setClassFromPlayer(player);
			if (testLoadout.mClass.mClassId != loadout.mClass.mClassId
				    || testLoadout.mClass.mSpecId != loadout.mClass.mSpecId
				    || !testLoadout.mClass.mAbilityScores.equals(loadout.mClass.mAbilityScores)) {
				boolean success = YellowTesseractOverride.loadClass(player, loadout.mClass.mClassId, loadout.mClass.mSpecId, loadout.mClass.mAbilityScores, false);
				swappedClass = true;
				if (success) {
					if (yellowCooldown != 0) {
						player.sendMessage(Component.text("Swapping skills is still on cooldown. You have been silenced for 30s.", NamedTextColor.RED)
							                   .append(Component.text(ChatColor.AQUA + " (Swap CD: " + ChatColor.YELLOW + "" + yellowCooldown + "" + ChatColor.AQUA + " mins)")));
						Plugin.getInstance().mEffectManager.addEffect(player, "YellowTessSilence", new AbilitySilence(30 * 20));
					} else if (!safeZone) {
						YellowTesseractOverride.setCooldown(player, 3);
					}
				} else {
					player.sendMessage(Component.text("Your class has been reset!", NamedTextColor.RED));
				}
			}
		}

		Plugin.getInstance().mItemStatManager.updateStats(player);
		InventoryUtils.scheduleDelayedEquipmentCheck(Plugin.getInstance(), player, null);

		List<String> swappedThings = new ArrayList<>();
		if (swappedEquipment.get()) {
			swappedThings.add("equipment");
		}
		if (swappedVanity) {
			swappedThings.add("vanity");
		}
		if (swappedCharms) {
			swappedThings.add("charms");
		}
		if (swappedClass) {
			swappedThings.add("class");
		}
		if (swappedThings.isEmpty()) {
			player.sendMessage(Component.text("Nothing swapped, you already match this loadout.", NamedTextColor.GRAY));
			return;
		}
		String message = "Swapped " + String.join(", ", swappedThings.subList(0, swappedThings.size() - 1)) + (swappedThings.size() == 1 ? "" : " and ") + swappedThings.get(swappedThings.size() - 1) + ".";
		player.sendMessage(Component.text(message, NamedTextColor.GOLD, TextDecoration.BOLD));
		player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.1f);

	}

	private static boolean isLoadoutItem(LoadoutItem loadoutItem, Boolean matchInfusion, ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}
		return loadoutItem.mIdentifier.isIdentifierFor(item, !matchInfusion)
			       && (!matchInfusion || loadoutItem.mInfusionType == null || ItemStatUtils.hasInfusion(item, loadoutItem.mInfusionType));
	}

	private static boolean canPutMoreShulkersIntoEquipmentBox(Inventory inventory) {
		return Arrays.stream(inventory.getContents()).filter(it -> it != null && ItemUtils.isShulkerBox(it.getType())).count() < MAX_SHULKERS_IN_STORAGE_SHULKER;
	}

	private static HoverEvent<?> itemListHover(Collection<ItemUtils.ItemIdentifier> items) {
		return HoverEvent.showText(items.stream()
			                           .map(ItemUtils.ItemIdentifier::getDisplayName)
			                           .reduce((c1, c2) -> c1.append(Component.newline()).append(c2))
			                           .orElse(Component.empty()));
	}

	private static void giveItem(Player player, List<ItemInventory> inventories, ItemStack item) {
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}
		if (ItemUtils.isShulkerBox(item.getType())) {
			// shulker box: allow in player inventory and ender chest, and allow specific shulkers in equipment cases up to a limit
			boolean canSwapItem = ShulkerEquipmentListener.canSwapItem(item);
			for (ItemInventory inv : inventories) {
				if (InventoryUtils.numEmptySlots(inv.mInventory) > 0
					    && (inv.mInventory.equals(player.getEnderChest())
						        || inv.mInventory.equals(player.getInventory())
						        || (canSwapItem && canPutMoreShulkersIntoEquipmentBox(inv.mInventory)))) {
					inv.mInventory.addItem(item);
					return;
				}
			}
		} else {
			// normal item: just place where it fits
			for (ItemInventory inv : inventories) {
				if (InventoryUtils.canFitInInventory(item, inv.mInventory)) {
					inv.mInventory.addItem(item);
					return;
				}
			}
		}
		// no good place found: give to player
		InventoryUtils.giveItem(player, item);
	}

	public boolean isEquipped(Player player, Loadout loadout) {
		if (loadout.mIncludeEquipment) {
			for (LoadoutItem loadoutItem : loadout.mEquipment) {
				if (!loadoutItem.mIdentifier.isIdentifierFor(player.getInventory().getItem(loadoutItem.mSlot), true)) {
					return false;
				}
			}
		}
		if (loadout.mIncludeCharms) {
			List<ItemStack> charms = Plugin.getInstance().mCharmManager.getCharms(player);
			for (ItemUtils.ItemIdentifier charm : loadout.mCharms) {
				if (charms.stream().noneMatch(item -> charm.isIdentifierFor(item, false))) {
					return false;
				}
			}
		}
		if (loadout.mIncludeClass) {
			if (loadout.mClass.mClassId != ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0)
				    || loadout.mClass.mSpecId != ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME).orElse(0)) {
				return false;
			}
			for (Map.Entry<String, Integer> entry : loadout.mClass.mAbilityScores.entrySet()) {
				if (ScoreboardUtils.getScoreboardValue(player, entry.getKey()).orElse(0) < entry.getValue()) {
					return false;
				}
			}
		}
		return true;
	}

	public void quickSwap(Player player) {
		if (!ShulkerEquipmentListener.checkAllowedToSwapEquipment(player)) {
			return;
		}

		LoadoutData data = getData(player);
		for (Loadout loadout : data.mLoadouts) {
			if (loadout.mIsQuickSwap) {
				if (data.mBackSwapLoadout != null && isEquipped(player, loadout)) {
					swapTo(player, data.mBackSwapLoadout, true);
				} else {
					Loadout backSwapLoadout = new Loadout(-1, "BackSwapLoadout");
					backSwapLoadout.setFromPlayer(player);
					backSwapLoadout.mClearEmpty = true;
					swapTo(player, loadout, true);
					data.mBackSwapLoadout = backSwapLoadout;
				}
				return;
			}
		}
		player.sendMessage(Component.text("No quickswap loadout defined. Open the GUI to define one!", NamedTextColor.RED));
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		JsonObject json = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), KEY_PLUGIN_DATA);
		if (json == null) {
			return;
		}
		mData.put(player.getUniqueId(), LoadoutData.fromJson(json));
	}

	@EventHandler(ignoreCancelled = true)
	public void playerSave(PlayerSaveEvent event) {
		LoadoutData loadoutData = mData.get(event.getPlayer().getUniqueId());
		if (loadoutData == null) {
			return;
		}
		event.setPluginData(KEY_PLUGIN_DATA, loadoutData.toJson());
	}

	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!player.isOnline()) {
				mData.remove(player.getUniqueId());
			}
		}, 10);
	}

	public static class LoadoutData {

		public List<Loadout> mLoadouts = new ArrayList<>();
		public @Nullable Loadout mBackSwapLoadout;

		public int mMaxLoadouts = 3;

		private JsonObject toJson() {
			JsonObject json = new JsonObject();
			JsonArray loadoutsJson = new JsonArray();
			for (Loadout loadout : mLoadouts) {
				loadoutsJson.add(loadout.toJson());
			}
			json.add("loadouts", loadoutsJson);
			json.add("backSwapLoadout", mBackSwapLoadout == null ? null : mBackSwapLoadout.toJson());
			json.addProperty("maxLoadouts", mMaxLoadouts);
			return json;
		}

		private static LoadoutData fromJson(JsonObject json) {
			LoadoutData data = new LoadoutData();
			for (JsonElement jsonElement : json.getAsJsonArray("loadouts")) {
				data.mLoadouts.add(Loadout.fromJson(jsonElement.getAsJsonObject()));
			}
			data.mBackSwapLoadout = json.get("backSwapLoadout") == null ? null : Loadout.fromJson(json.getAsJsonObject("backSwapLoadout"));
			data.mMaxLoadouts = json.getAsJsonPrimitive("maxLoadouts").getAsInt();
			return data;
		}

	}

	public static class Loadout {
		public int mIndex;

		public String mName;
		public ItemUtils.ItemIdentifier mDisplayItem = new ItemUtils.ItemIdentifier(Material.ARMOR_STAND, null);

		public boolean mIncludeEquipment = true;
		public boolean mIncludeVanity = true;
		public boolean mIncludeCharms = true;
		public boolean mIncludeClass = true;

		public List<LoadoutItem> mEquipment = new ArrayList<>();
		public Map<EquipmentSlot, ItemStack> mVanity = new EnumMap<>(EquipmentSlot.class);
		public int mLeftParrot;
		public int mRightParrot;
		public List<ItemUtils.ItemIdentifier> mCharms = new ArrayList<>();
		public LoadoutClass mClass = new LoadoutClass();

		public boolean mIsQuickSwap = false;

		public boolean mClearEmpty = false;

		public Loadout(int index, String name) {
			mIndex = index;
			mName = name;
		}

		public void setFromPlayer(Player player) {
			setEquipmentFromPlayer(player);
			setVanityFromPlayer(player);
			setCharmsFromPlayer(player);
			setClassFromPlayer(player);
		}

		public void setEquipmentFromPlayer(Player player) {
			@Nullable ItemStack[] contents = player.getInventory().getContents();
			mEquipment.clear();
			for (int i : EQUIPMENT_SLOTS) {
				ItemStack item = contents[i];
				if (item != null && !item.getType().isAir() && !isEquipmentStorageBox(item)) {
					mEquipment.add(new LoadoutItem(i, ItemUtils.getIdentifier(item, false), InfusionUtils.getCurrentInfusion(item).getInfusionType()));
				}
			}
		}

		public void setVanityFromPlayer(Player player) {
			VanityManager.VanityData vanityData = Plugin.getInstance().mVanityManager.getData(player);
			mVanity.clear();
			mVanity.putAll(vanityData.getEquipped());
			mLeftParrot = ScoreboardUtils.getScoreboardValue(player, ParrotManager.SCOREBOARD_PARROT_LEFT).orElse(0);
			mRightParrot = ScoreboardUtils.getScoreboardValue(player, ParrotManager.SCOREBOARD_PARROT_RIGHT).orElse(0);
		}

		public void setCharmsFromPlayer(Player player) {
			List<ItemStack> charms = Plugin.getInstance().mCharmManager.mPlayerCharms.get(player.getUniqueId());
			mCharms.clear();
			if (charms != null) {
				for (ItemStack charm : charms) {
					mCharms.add(ItemUtils.getIdentifier(charm, false));
				}
			}
		}

		public void setClassFromPlayer(Player player) {
			mClass.mAbilityScores.clear();
			mClass.mClassId = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0);
			PlayerClass playerClass = new MonumentaClasses().getClassById(mClass.mClassId);
			if (playerClass == null) {
				mClass.mSpecId = 0;
				return;
			}
			mClass.mSpecId = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME).orElse(0);
			PlayerSpec playerSpec = playerClass.getSpecById(mClass.mSpecId);
			for (AbilityInfo<?> ability : playerClass.mAbilities) {
				String scoreboard = ability.getScoreboard();
				if (scoreboard != null) {
					int score = ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0);
					if (score > 0) {
						mClass.mAbilityScores.put(scoreboard, score);
					}
				}
			}
			if (playerSpec != null) {
				for (AbilityInfo<?> ability : playerSpec.mAbilities) {
					String scoreboard = ability.getScoreboard();
					if (scoreboard != null) {
						int score = ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0);
						if (score > 0) {
							mClass.mAbilityScores.put(scoreboard, score);
						}
					}
				}
			}
		}

		public @Nullable LoadoutItem getEquipmentInSlot(int slot) {
			return mEquipment.stream().filter(i -> i.mSlot == slot).findFirst().orElse(null);
		}

		public JsonObject toJson() {
			JsonObject json = new JsonObject();

			json.addProperty("index", mIndex);
			json.addProperty("name", mName);

			json.addProperty("displayItemType", mDisplayItem.mType.name());
			json.addProperty("displayItemName", mDisplayItem.mName);

			json.addProperty("includeEquipment", mIncludeEquipment);
			json.addProperty("includeVanity", mIncludeVanity);
			json.addProperty("includeCharms", mIncludeCharms);
			json.addProperty("includeClass", mIncludeClass);

			JsonArray equipment = new JsonArray();
			for (LoadoutItem loadoutItem : mEquipment) {
				equipment.add(loadoutItem.toJson());
			}
			json.add("equipment", equipment);

			JsonArray charms = new JsonArray();
			for (ItemUtils.ItemIdentifier charm : mCharms) {
				JsonObject charmJson = new JsonObject();
				charmJson.addProperty("type", charm.mType.name());
				charmJson.addProperty("name", charm.mName);
				charms.add(charmJson);
			}
			json.add("charms", charms);

			json.add("class", mClass.toJson());

			json.addProperty("isQuickSwap", mIsQuickSwap);
			json.addProperty("clearEmpty", mClearEmpty);

			JsonObject vanity = new JsonObject();
			for (Map.Entry<EquipmentSlot, ItemStack> entry : mVanity.entrySet()) {
				vanity.addProperty(entry.getKey().name(), ItemUtils.serializeItemStack(entry.getValue()));
			}
			json.add("vanity", vanity);

			json.addProperty("leftParrot", mLeftParrot);
			json.addProperty("rightParrot", mRightParrot);

			return json;
		}

		private static Loadout fromJson(JsonObject json) {
			int index = json.getAsJsonPrimitive("index").getAsInt();
			String name = json.getAsJsonPrimitive("name").getAsString();
			Loadout loadout = new Loadout(index, name);
			loadout.mDisplayItem = parseItemIdentifier(json, "displayItemType", "displayItemName");

			loadout.mIncludeEquipment = json.getAsJsonPrimitive("includeEquipment").getAsBoolean();
			loadout.mIncludeVanity = json.getAsJsonPrimitive("includeVanity").getAsBoolean();
			loadout.mIncludeCharms = json.getAsJsonPrimitive("includeCharms").getAsBoolean();
			loadout.mIncludeClass = json.getAsJsonPrimitive("includeClass").getAsBoolean();

			for (JsonElement equipment : json.getAsJsonArray("equipment")) {
				loadout.mEquipment.add(LoadoutItem.fromJson(equipment.getAsJsonObject()));
			}

			for (JsonElement charm : json.getAsJsonArray("charms")) {
				loadout.mCharms.add(parseItemIdentifier(charm.getAsJsonObject(), "type", "name"));
			}

			loadout.mClass = LoadoutClass.fromJson(json.getAsJsonObject("class"));

			loadout.mIsQuickSwap = json.getAsJsonPrimitive("isQuickSwap").getAsBoolean();
			loadout.mClearEmpty = json.getAsJsonPrimitive("clearEmpty").getAsBoolean();

			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("vanity").entrySet()) {
				ItemStack item = ItemUtils.parseItemStack(entry.getValue().getAsString());
				if (!ItemStatUtils.isClean(item)) {
					ItemUtils.setPlainTag(item);
					ItemStatUtils.generateItemStats(item);
					ItemStatUtils.markClean(item);
					item = VanityManager.cleanCopyForDisplay(item);
				}
				loadout.mVanity.put(EquipmentSlot.valueOf(entry.getKey()), item);
			}

			loadout.mLeftParrot = json.getAsJsonPrimitive("leftParrot").getAsInt();
			loadout.mRightParrot = json.getAsJsonPrimitive("rightParrot").getAsInt();

			return loadout;
		}
	}

	public static class LoadoutItem {
		public int mSlot;
		public ItemUtils.ItemIdentifier mIdentifier;
		public @Nullable ItemStatUtils.InfusionType mInfusionType;

		public LoadoutItem(int slot, ItemUtils.ItemIdentifier identifier, @Nullable ItemStatUtils.InfusionType infusionType) {
			mSlot = slot;
			mIdentifier = identifier;
			mInfusionType = infusionType;
		}

		public JsonObject toJson() {
			JsonObject json = new JsonObject();
			json.addProperty("slot", mSlot);
			json.addProperty("type", mIdentifier.mType.name());
			json.addProperty("name", mIdentifier.mName);
			json.addProperty("infusion", mInfusionType == null ? null : mInfusionType.name());
			return json;
		}

		public static LoadoutItem fromJson(JsonObject json) {
			int slot = json.getAsJsonPrimitive("slot").getAsInt();
			ItemUtils.ItemIdentifier identifier = parseItemIdentifier(json, "type", "name");
			ItemStatUtils.InfusionType infusion;
			try {
				JsonPrimitive infusionElement = json.getAsJsonPrimitive("infusion");
				infusion = infusionElement == null ? null : ItemStatUtils.InfusionType.valueOf(infusionElement.getAsString());
			} catch (IllegalArgumentException e) {
				infusion = null;
			}
			return new LoadoutItem(slot, identifier, infusion);
		}
	}

	public static class LoadoutClass {
		public int mClassId;
		public int mSpecId;
		public final Map<String, Integer> mAbilityScores = new HashMap<>();

		public JsonObject toJson() {
			JsonObject json = new JsonObject();
			json.addProperty("classId", mClassId);
			json.addProperty("specId", mSpecId);
			JsonObject abilityScores = new JsonObject();
			for (Map.Entry<String, Integer> entry : mAbilityScores.entrySet()) {
				abilityScores.addProperty(entry.getKey(), entry.getValue());
			}
			json.add("abilityScores", abilityScores);
			return json;
		}

		public static LoadoutClass fromJson(JsonObject json) {
			LoadoutClass result = new LoadoutClass();
			result.mClassId = json.getAsJsonPrimitive("classId").getAsInt();
			result.mSpecId = json.getAsJsonPrimitive("specId").getAsInt();
			PlayerClass playerClass = new MonumentaClasses().getClassById(result.mClassId);
			PlayerSpec playerSpec = playerClass == null ? null : playerClass.getSpecById(result.mSpecId);
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("abilityScores").entrySet()) {
				// Only load ability score if the scoreboard name is still valid
				if (playerClass != null
					    && (playerClass.mAbilities.stream().anyMatch(info -> entry.getKey().equals(info.getScoreboard()))
						        || (playerSpec != null && playerSpec.mAbilities.stream().anyMatch(info -> entry.getKey().equals(info.getScoreboard()))))) {
					result.mAbilityScores.put(entry.getKey(), entry.getValue().getAsInt());
				}
			}
			return result;
		}

	}

}
