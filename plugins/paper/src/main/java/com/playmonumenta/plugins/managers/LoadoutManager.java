package com.playmonumenta.plugins.managers;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.Constants.Keybind;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.GearChanged;
import com.playmonumenta.plugins.guis.IchorSelectionGUI;
import com.playmonumenta.plugins.guis.LoadoutManagerGui;
import com.playmonumenta.plugins.inventories.ClickLimiter;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.overrides.YellowTesseractOverride;
import com.playmonumenta.plugins.parrots.ParrotManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class LoadoutManager implements Listener {

	private static final String KEY_PLUGIN_DATA = "MonumentaLoadouts";

	public static final String LOADOUT_MANAGER_PERMISSION = "monumenta.feature.loadoutmanager";
	public static final Material LOADOUT_MANAGER_MATERIAL = Material.SMITHING_TABLE;
	public static final String LOADOUT_MANAGER_NAME = "Mechanical Armory";
	public static final String STORAGE_SHULKER_NAME = "Equipment Case";

	public static final int MAX_SHULKERS_IN_STORAGE_SHULKER = 5;

	private static final int[] EQUIPMENT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 36, 37, 38, 39, 40};

	/**
	 * Set of locations that are to be considered "exalted" in r3, i.e. such items will only be selected by the armory if the {@link LoadoutItem#mIsExalted exalted} flag is set.
	 */
	private static final ImmutableSet<Location> EXALTED_LOCATIONS = ImmutableSet.of(
		// Region 1
		Location.LABS, Location.WHITE, Location.ORANGE, Location.MAGENTA,
		Location.LIGHTBLUE, Location.YELLOW, Location.REVERIE,
		Location.WILLOWS, Location.WILLOWSKIN, Location.EPHEMERAL, Location.EPHEMERAL_ENHANCEMENTS,
		Location.SANCTUM, Location.VERDANT, Location.VERDANTSKIN,
		Location.AZACOR, Location.KAUL, Location.DIVINE, Location.ROYAL,

		// Region 2
		Location.LIME, Location.PINK, Location.GRAY, Location.LIGHTGRAY,
		Location.CYAN, Location.PURPLE, Location.TEAL, Location.SHIFTING,
		Location.FORUM,
		Location.MIST, Location.HOARD, Location.GREEDSKIN,
		Location.REMORSE, Location.REMORSEFULSKIN, Location.VIGIL,
		Location.DEPTHS,
		Location.HORSEMAN, Location.HALLOWEENSKIN,
		Location.FROSTGIANT, Location.TITANICSKIN,
		Location.LICH, Location.ETERNITYSKIN,
		Location.RUSH, Location.TREASURE, Location.INTELLECT
	);

	public enum EquipmentCaseTag {
		R1("valley", "r1|valley", 0, (item) -> ItemStatUtils.getRegion(item) == Region.VALLEY),
		R2("isles", "r2|isles", 0, (item) -> ItemStatUtils.getRegion(item) == Region.ISLES),
		R3("ring", "r3|ring", 0, (item) -> ItemStatUtils.getRegion(item) == Region.RING),

		LEGENDARY("legendary", "legendar(?:ys?|ies)", 1, item -> ItemStatUtils.getTier(item) == Tier.LEGENDARY),
		EPIC("epic", "epics?", 1, item -> List.of(Tier.EPIC, Tier.EPIC_CHARM).contains(ItemStatUtils.getTier(item))),
		ARTIFACT("artifact", "artifacts?", 1, item -> ItemStatUtils.getTier(item) == Tier.ARTIFACT),
		RARE("rare", "rares?", 1, item -> List.of(Tier.RARE, Tier.RARE_CHARM).contains(ItemStatUtils.getTier(item))),
		UNIQUE("unique", "uniques?", 1, item -> ItemStatUtils.getTier(item) == Tier.UNIQUE),
		UNCOMMON("uncommon", "uncommons?", 1, item -> ItemStatUtils.getTier(item) == Tier.UNCOMMON),
		TIERED("tiered", "tiereds?", 1, item -> List.of(Tier.ZERO, Tier.I, Tier.II, Tier.III, Tier.IV, Tier.V, Tier.COMMON).contains(ItemStatUtils.getTier(item))),

		ARMOR("armor", "armou?rs?", 1, item -> ItemUtils.isArmorOrWearable(item) || ItemStatUtils.hasAttributeInSlot(item, Slot.OFFHAND)),

		SWORD("sword", "swords?", 2, item -> ItemUtils.isSword(item)),
		AXE("axe", "axes?", 2, item -> ItemUtils.isAxe(item)),
		PICKAXE("pickaxe", "pickaxes?", 2, item -> ItemUtils.isPickaxe(item)),
		SCYTHE("scythe", "hoes?|scythes?", 2, item -> ItemUtils.isHoe(item)),
		WAND("wand", "wands?", 2, item -> ItemUtils.isWand(item)),
		RANGED("ranged", "ranged|projectiles?|(?:cross|x)?bows?|tridents?|(?:snow)?balls?", 2, item -> ItemUtils.isProjectileWeapon(item)),
		WEAPON("weapon", "weapons?|mainhands?", 2, item -> ItemStatUtils.hasAttributeInSlot(item, Slot.MAINHAND) && !ItemStatUtils.hasAttributeInSlot(item, Slot.OFFHAND)),
		SHIELD("shield", "shields?", 2, item -> item.getType() == Material.SHIELD),

		OFFHAND("offhand", "offhands?", 2,
				item -> ItemStatUtils.hasAttributeInSlot(item, Slot.OFFHAND)
						// weightless items are offhands even without stats, except for the tesseract of light
						|| (ItemStatUtils.hasEnchantment(item, EnchantmentType.WEIGHTLESS) && item.getType() != Material.LIGHT_GRAY_STAINED_GLASS)),
		HELMET("helmet", "helmets?|hats?", 2, item -> ItemUtils.getEquipmentSlot(item) == EquipmentSlot.HEAD),
		CHEST("chestplate", "chest(?:plate)?s?", 2, item -> ItemUtils.getEquipmentSlot(item) == EquipmentSlot.CHEST),
		LEGS("legs", "leg(?:ging)?s?", 2, item -> ItemUtils.getEquipmentSlot(item) == EquipmentSlot.LEGS),
		BOOTS("boots", "boots?", 2, item -> ItemUtils.getEquipmentSlot(item) == EquipmentSlot.FEET),

		// higher priority than region + weapon
		TOOL("tool", "tools?|util(?:s?|it(?:ys?|ies))", 104,
			item -> (ItemUtils.isShovel(item)
				         || ItemUtils.isPickaxe(item)
				         // assume silk touch axes are considered tools, as are axes with no damage added
				         || (ItemUtils.isAxe(item) && (ItemStatUtils.hasEnchantment(item, EnchantmentType.SILK_TOUCH) || ItemStatUtils.getAttributeAmount(item, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) <= 1))
				         // similarly, non-offhand swords with no attack damage are tools
				         || (ItemUtils.isSword(item) && ItemStatUtils.getAttributeAmount(item, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) <= 1 && !ItemStatUtils.hasAttributeInSlot(item, Slot.OFFHAND))
				         || item.getType() == Material.SHEARS
				         || item.getType() == Material.COMPASS
				         || ItemUtils.isShulkerBox(item.getType())
				         || ItemStatUtils.hasEnchantment(item, EnchantmentType.MULTITOOL)
				         || ItemStatUtils.hasEnchantment(item, EnchantmentType.RECOIL)
				         || ItemStatUtils.hasEnchantment(item, EnchantmentType.RIPTIDE))
				        && !ItemStatUtils.isCharm(item)),

		// Consumables are (mostly) region-independent, and charms always R3, so their priority includes the priority a region tag would add
		// this for example makes 'consumables' higher priority than 'r2 weapons', thus sorting Fruit of Life into 'consumables' rather than 'weapons'
		CHARM("charm", "charms?", 104, item -> ItemStatUtils.isCharm(item)),
		ZENITH("zenith", "zenith", 105, item -> ItemStatUtils.isZenithCharm(item)),
		CONSUMABLE("consumable", "consumables?|foods?|potions?", 104,
			item -> ItemStatUtils.isConsumable(item)
				        || (item.getType().isEdible() && !ItemStatUtils.isCharm(item))
				        || (ItemUtils.isSomePotion(item) && !ItemUtils.isAlchemistItem(item))
				        || ShulkerEquipmentListener.isPotionInjectorItem(item)),
		;

		private final String mName;
		private final Pattern mTagPattern;
		private final int mPriority;
		private final Predicate<ItemStack> mPredicate;

		// groupings for case tag matching - for each group, either none of the tags must be on the case or at least one tag must apply to an item for it to go into that case
		private static final List<Set<EquipmentCaseTag>> TAG_GROUPS = new ArrayList<>(List.of(
			Set.of(R1, R2, R3),
			Set.of(LEGENDARY, EPIC, ARTIFACT, RARE, UNIQUE, UNCOMMON, TIERED)
			// everything else is added as a last group by the static {} below
		));

		static {
			Set<EquipmentCaseTag> miscGroup = new HashSet<>(List.of(values()));
			miscGroup.removeIf(tag -> TAG_GROUPS.stream().anyMatch(group -> group.contains(tag)));
			TAG_GROUPS.add(miscGroup);
		}

		EquipmentCaseTag(String name, String tagPattern, int priority, Predicate<ItemStack> predicate) {
			mName = name;
			mTagPattern = Pattern.compile("\\b(?:" + tagPattern + ")\\b", Pattern.CASE_INSENSITIVE);
			mPriority = priority;
			mPredicate = predicate;
		}

		public String getName() {
			return mName;
		}

		public static Set<EquipmentCaseTag> getTags(ItemStack item) {
			String caseName = ItemStatUtils.getPlayerCustomName(item);
			if (caseName == null) {
				return Set.of();
			}
			return Arrays.stream(values())
				       .filter(tag -> tag.mTagPattern.matcher(caseName).find())
				       .collect(Collectors.toSet());
		}
	}

	private final Map<UUID, LoadoutData> mData = new HashMap<>();

	public LoadoutData getData(Player player) {
		return mData.computeIfAbsent(player.getUniqueId(), key -> new LoadoutData());
	}

	private static boolean isLoadoutManager(ItemStack item) {
		if (item == null || !LOADOUT_MANAGER_MATERIAL.equals(item.getType())) {
			return false;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return false;
		}

		Component displayName = meta.displayName();
		if (displayName == null) {
			return false;
		}

		return LOADOUT_MANAGER_NAME.equals(MessagingUtils.plainText(displayName));
	}

	private static ItemUtils.ItemIdentifier parseItemIdentifier(JsonObject json, String typeProperty, String nameProperty) {
		JsonPrimitive name = json.getAsJsonPrimitive(nameProperty);
		return new ItemUtils.ItemIdentifier(Material.valueOf(json.getAsJsonPrimitive(typeProperty).getAsString()),
			name == null ? null : name.getAsString());
	}

	private static ItemUtils.ZenithCharmIdentifier parseItemIdentifier(JsonObject json, String typeProperty, String nameProperty, String uuidProperty) {
		JsonPrimitive name = json.getAsJsonPrimitive(nameProperty);
		return new ItemUtils.ZenithCharmIdentifier(Material.valueOf(json.getAsJsonPrimitive(typeProperty).getAsString()),
			name == null ? null : name.getAsString(), json.getAsJsonPrimitive(uuidProperty).getAsLong());
	}

	public static boolean isEquipmentStorageBox(@Nullable ItemStack item) {
		return item != null && ItemUtils.isShulkerBox(item.getType()) && STORAGE_SHULKER_NAME.equals(ItemUtils.getPlainNameIfExists(item));
	}

	private record ItemInventory(Inventory mInventory, Runnable mSaveAction, Set<EquipmentCaseTag> tags) {
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
						}, EquipmentCaseTag.getTags(item)));
					}
				}
			};
			findStorageShulkers.accept(player.getInventory());
			boolean hasEnderChest = ItemUtils.hasPortableEnderOrIsNearEnderChest(player);
			if (hasEnderChest) {
				findStorageShulkers.accept(player.getEnderChest());
				inventories.add(new ItemInventory(player.getEnderChest(), () -> {
				}, Set.of()));
			}
			inventories.add(new ItemInventory(player.getInventory(), () -> {
			}, Set.of()));

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
							// Item already equipped, nothing to do, unless they have an ichor infusion, in which case, check if it needs to be swapped
							InfusionType ichorInfusion = IchorListener.getIchorInfusion(playerItem);
							InfusionType targetIchorInfusion = loadoutItem.mIchorInfusionType;
							if (targetIchorInfusion != null && ichorInfusion != null && ichorInfusion != targetIchorInfusion) {
								IchorSelectionGUI.clearIchorInfusions(playerItem);
								ItemStatUtils.addInfusion(playerItem, targetIchorInfusion, 1, player.getUniqueId());
								swappedEquipment.set(true);
							}
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
									EffectManager.getInstance().addEffect(player, GearChanged.effectID, new GearChanged(GearChanged.DURATION));

									InfusionType ichorInfusion = IchorListener.getIchorInfusion(newItem);
									InfusionType targetIchorInfusion = loadoutItem.mIchorInfusionType;
									if (targetIchorInfusion != null && ichorInfusion != null) {
										IchorSelectionGUI.clearIchorInfusions(newItem);
										ItemStatUtils.addInfusion(newItem, targetIchorInfusion, 1, player.getUniqueId());
									}

									ItemStatUtils.cleanIfNecessary(newItem);

									ItemStack newItemClone = ItemUtils.clone(newItem);
									if (loadoutItem.mSlot > 9) { // armor/offhand: take only one
										newItemClone.setAmount(1);
										newItem.subtract();
									} else { // hotbar: take entire stack
										newItem.setAmount(0);
									}
									player.getInventory().setItem(loadoutItem.mSlot, newItemClone);

									if (!ItemUtils.isNullOrAir(playerItem)) {
										giveItem(player, inventories, playerItem, List.of(inventory), i);
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
									giveItem(player, inventories, playerItem, List.of(), 0);
									swappedEquipment.set(true);
								}
							}
						}
					}

					if (swappedEquipment.get()) {
						// Reset player's attack cooldown so that it cannot be exploited by
						// swapping from high to low cooldown items of the same type
						PlayerUtils.resetAttackCooldown(player);
					}

				}

				// Swap charms
				if (full && loadout.mIncludeCharms) {
					CharmManager.CharmType type;
					List<ItemUtils.ItemIdentifier> loadoutCharms;
					if (loadout.mUseZenithCharms) {
						type = CharmManager.CharmType.ZENITH;
						loadoutCharms = loadout.mZenithCharms;
					} else {
						type = CharmManager.CharmType.NORMAL;
						loadoutCharms = loadout.mCharms;
					}
					List<ItemUtils.ItemIdentifier> itemsNotFound = new ArrayList<>();
					List<ItemUtils.ItemIdentifier> failedItems = new ArrayList<>();
					List<ItemStack> activeCharms = Plugin.getInstance().mCharmManager.getCharms(player, type);
					Set<ItemStack> oldCharmsSet = new HashSet<>(activeCharms);
					List<ItemStack> oldCharms = new ArrayList<>(activeCharms);
					activeCharms.clear();
					List<ItemInventory> charmInventories = new ArrayList<>(); // Try to put extra charms in the same boxes where we got the charms from

					// Equip new charms, swapping with old charms as far as possible
					charmLoop:
					for (ItemUtils.ItemIdentifier charmIdentifier : loadoutCharms) {
						// First, check if equipped charms match and re-equip them if so
						for (Iterator<ItemStack> iterator = oldCharms.iterator(); iterator.hasNext(); ) {
							ItemStack oldCharm = iterator.next();
							if (charmIdentifier.isIdentifierFor(oldCharm, false) && Plugin.getInstance().mCharmManager.validateCharm(player, oldCharm, type)) {
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
									ItemStatUtils.cleanIfNecessary(newItem);
									ItemStack newItemClone = ItemUtils.clone(newItem);
									newItemClone.setAmount(1);
									if (!Plugin.getInstance().mCharmManager.validateCharm(player, newItemClone, type)) {
										failedItems.add(charmIdentifier);
										continue charmLoop;
									}
									newItem.subtract();
									activeCharms.add(newItemClone);
									if (!oldCharms.isEmpty()) {
										ItemStack oldCharm = oldCharms.remove(0);
										giveItem(player, inventories, oldCharm, List.of(inventory), invI);
									}
									if (!charmInventories.contains(inventory)) {
										charmInventories.add(inventory);
									}
									continue charmLoop;
								}
							}
						}
						itemsNotFound.add(charmIdentifier);
					}

					// Put any extra old charms into any free storage boxes, prioritising the boxes where charms were swapped from
					for (ItemStack charm : oldCharms) {
						giveItem(player, inventories, charm, charmInventories, -1);
					}

					swappedCharms = !oldCharmsSet.equals(new HashSet<>(activeCharms));

					if (swappedCharms) {
						Plugin.getInstance().mCharmManager.updateCharms(player, type, activeCharms);
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
							                   .append(Component.text(" (Swap CD: ", NamedTextColor.AQUA)).append(Component.text(yellowCooldown, NamedTextColor.YELLOW)).append(Component.text(" mins)", NamedTextColor.AQUA)));
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
			       && (!EXALTED_LOCATIONS.contains(ItemStatUtils.getLocation(item)) || loadoutItem.mIsExalted == (ItemStatUtils.getRegion(item) == Region.RING))
			       && (!matchInfusion || loadoutItem.mInfusionType == null || ItemStatUtils.hasInfusion(item, loadoutItem.mInfusionType))
			       && (!matchInfusion || loadoutItem.mDelveInfusionType == null || ItemStatUtils.hasInfusion(item, loadoutItem.mDelveInfusionType));
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

	private static void giveItem(Player player, List<ItemInventory> inventories, ItemStack item, List<ItemInventory> preferredInventories, int preferredSlot) {
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}

		// Find the best place to store an item following these priorities:
		// 1. The case with the most matching tags and no negative tag matches [+100 per tag]
		// 2. Any case with no tags (so that these work as a general/misc case) [+10]
		// 3. The equipment case & slot that the previous item(s) was/were taken from (preferredInventories & preferredSlot parameters) [+1 if any space in case]
		// 4. Any case [0]
		ItemInventory bestInventory = null;
		int bestPriority = -1;
		int bestSlot = -1;

		Set<EquipmentCaseTag> itemTags = Arrays.stream(EquipmentCaseTag.values())
			                                 .filter(tag -> tag.mPredicate.test(item))
			                                 .collect(Collectors.toSet());
		boolean isShulker = ItemUtils.isShulkerBox(item.getType());
		boolean isSwappableShulker = isShulker && ShulkerEquipmentListener.canSwapItem(item);

		for (ItemInventory inv : inventories) {
			int priority = 0;
			int slot = -1;
			if (inv.tags.isEmpty()) {
				priority += 10;
			} else if (EquipmentCaseTag.TAG_GROUPS.stream()
				           // groupings for case tag matching - for each group, either none of the tags must be on the case,
				           // or at least one tag must apply to an item for it to go into that case
				           .allMatch(tagGroup -> tagGroup.stream().noneMatch(inv.tags::contains) || itemTags.stream().anyMatch(t -> inv.tags.contains(t) && tagGroup.contains(t)))) {
				priority += inv.tags.stream().mapToInt(tag -> 1000 + 10 * tag.mPriority).sum();
			}
			if (preferredInventories.contains(inv)) {
				priority += 1;
				slot = preferredSlot;
			}
			// if best inventory so far, and there's space, set as new best inventory
			if (priority > bestPriority
				    && (isShulker
					        ? (InventoryUtils.numEmptySlots(inv.mInventory) > 0
						           && (inv.mInventory.equals(player.getEnderChest())
							               || inv.mInventory.equals(player.getInventory())
							               || (isSwappableShulker && canPutMoreShulkersIntoEquipmentBox(inv.mInventory))))
					        : InventoryUtils.canFitInInventory(item, inv.mInventory))) {
				bestInventory = inv;
				bestPriority = priority;
				bestSlot = slot;
			}
		}

		if (bestInventory != null) {
			if (bestSlot >= 0 && bestSlot < bestInventory.mInventory.getSize() && ItemUtils.isNullOrAir(bestInventory.mInventory.getItem(bestSlot))) {
				bestInventory.mInventory.setItem(bestSlot, item);
			} else {
				bestInventory.mInventory.addItem(item);
			}
		} else {
			// no good place found: give to player (most likely drops on the ground)
			InventoryUtils.giveItem(player, item);
		}
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
			CharmManager.CharmType type;
			List<ItemUtils.ItemIdentifier> loadoutCharms;
			if (loadout.mUseZenithCharms) {
				type = CharmManager.CharmType.ZENITH;
				loadoutCharms = loadout.mZenithCharms;
			} else {
				type = CharmManager.CharmType.NORMAL;
				loadoutCharms = loadout.mCharms;
			}
			List<ItemStack> charms = Plugin.getInstance().mCharmManager.getCharms(player, type);
			for (ItemUtils.ItemIdentifier charm : loadoutCharms) {
				if (charms.stream().noneMatch(item -> charm.isIdentifierFor(item, false))) {
					return false;
				}
			}
		}
		if (loadout.mIncludeClass) {
			if (loadout.mClass.mClassId != AbilityUtils.getClassNum(player)
				    || loadout.mClass.mSpecId != AbilityUtils.getSpecNum(player)) {
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

	public void openOwnGui(Player player) {
		if (!player.hasPermission(LOADOUT_MANAGER_PERMISSION)) {
			player.sendMessage(Component.text(
				LOADOUT_MANAGER_NAME + " is currently disabled.",
				NamedTextColor.RED
			));
			return;
		}

		if (ScoreboardUtils.getScoreboardValue(player, "Portal").orElse(0) <= 0) {
			player.sendMessage(Component.text(
				"You must have completed P.O.R.T.A.L. to use this item.",
				NamedTextColor.RED
			));
			return;
		}

		if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.RESIST_5)) {
			YellowTesseractOverride.setCooldown(player, 0);
		}

		int yellowCooldown = YellowTesseractOverride.getCooldown(player);
		if (yellowCooldown > 0) {
			player.sendMessage(Component.text("Warning: Swapping skills is on cooldown! You will be silenced if you perform any changes to your class or abilities in the next ", NamedTextColor.YELLOW)
				                   .append(Component.text("" + yellowCooldown, NamedTextColor.RED, TextDecoration.BOLD))
				                   .append(Component.text(yellowCooldown == 1 ? " minute." : " minutes.", NamedTextColor.YELLOW)));
		}

		new LoadoutManagerGui(player).open();
	}

	public void quickSwap(Player player) {
		quickSwap(player, Keybind.SWAP_OFFHAND);
	}

	public void quickSwap(Player player, Keybind keybind) {
		if (!player.hasPermission(LOADOUT_MANAGER_PERMISSION)) {
			player.sendMessage(Component.text(
				LOADOUT_MANAGER_NAME + " is currently disabled.",
				NamedTextColor.RED
			));
			return;
		}

		if (ScoreboardUtils.getScoreboardValue(player, "Portal").orElse(0) <= 0) {
			player.sendMessage(Component.text(
				"You must have completed P.O.R.T.A.L. to use this item.",
				NamedTextColor.RED
			));
			return;
		}

		if (ClickLimiter.isLocked(player)) {
			return;
		}

		if (!ShulkerEquipmentListener.checkAllowedToSwapEquipment(player)) {
			return;
		}

		LoadoutData data = getData(player);
		for (Loadout loadout : data.mLoadouts) {
			if (loadout.mQuickSwaps.contains(keybind)) {
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

		player.sendMessage(
			Component.text("You do not have a loadout set for the keybind ", NamedTextColor.GRAY)
				.append(Component.keybind(keybind))
				.append(Component.text(". Open the GUI to define one!"))
		);
	}

	public void retrieveUnusedItems(Player player) {
		try {
			if (InventoryUtils.numEmptySlots(player.getInventory()) == 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.1f);
				player.sendMessage(Component.text("Make some space in your inventory first!", NamedTextColor.GRAY));
				return;
			}
			int totalRetrieved = 0;
			boolean finished = true;
			LoadoutData data = getData(player);
			inventoryLoop:
			for (Inventory inventory : ItemUtils.hasPortableEnderOrIsNearEnderChest(player)
				                           ? new Inventory[] {player.getEnderChest(), player.getInventory()}
				                           : new Inventory[] {player.getInventory()}) {
				for (ItemStack shulkerItem : inventory.getContents()) {
					if (shulkerItem != null
						    && isEquipmentStorageBox(shulkerItem)
						    && shulkerItem.getItemMeta() instanceof BlockStateMeta blockStateMeta
						    && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
						boolean changed = false;
						for (ItemStack item : shulkerBox.getInventory()) {
							if (!ItemUtils.isNullOrAir(item)
								    && data.mLoadouts.stream()
									       .noneMatch(loadout -> loadout.mEquipment.stream().anyMatch(loadoutItem -> isLoadoutItem(loadoutItem, false, item))
										                             || loadout.mCharms.stream().anyMatch(charm -> charm.isIdentifierFor(item, false))
										                             || loadout.mZenithCharms.stream().anyMatch(charm -> charm.isIdentifierFor(item, false)))) {
								if (InventoryUtils.canFitInInventory(item, player.getInventory())) {
									player.getInventory().addItem(ItemUtils.clone(item));
									item.setAmount(0);
									changed = true;
									totalRetrieved++;
								} else {
									finished = false;
									break;
								}
							}
						}
						if (changed) {
							blockStateMeta.setBlockState(shulkerBox);
							shulkerItem.setItemMeta(blockStateMeta);
						}
						if (InventoryUtils.numEmptySlots(player.getInventory()) == 0) {
							finished = false;
							break inventoryLoop;
						}
					}
				}
			}
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.1f);
			if (totalRetrieved > 0) {
				player.sendMessage(Component.text("Retrieved " + totalRetrieved + " item" + (totalRetrieved == 1 ? "" : "s") + ".", NamedTextColor.GOLD));
				if (finished) {
					player.sendMessage(Component.text("There are no more items to retrieve.", NamedTextColor.GRAY));
				} else {
					player.sendMessage(Component.text("There may be more items to retrieve.", NamedTextColor.WHITE));
				}
			} else {
				player.sendMessage(Component.text("No items were retrieved.", NamedTextColor.WHITE));
			}
		} catch (Exception e) {
			String message = "Exception in Loadout Manager swap code, items have likely been duped or deleted. Affected player: " + player.getName();
			AuditListener.logSevere(message);
			MMLog.severe(message, e);
		}
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

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}

		if (!ItemUtils.isNullOrAir(event.getCursor())) {
			// Don't trigger when clicking while having an item on the cursor
			return;
		}

		if (!isLoadoutManager(event.getCurrentItem())) {
			return;
		}

		switch (event.getClick()) {
			case RIGHT -> openOwnGui(player);
			case SWAP_OFFHAND -> quickSwap(player, Keybind.SWAP_OFFHAND);
			case NUMBER_KEY -> quickSwap(player, Keybind.hotbar(event.getHotbarButton()));
			default -> {
				return;
			}
		}
		event.setCancelled(true);
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
		public ItemStack mDisplayItem = new ItemStack(Material.ARMOR_STAND);

		public boolean mIncludeEquipment = true;
		public boolean mIncludeVanity = true;
		public boolean mIncludeCharms = true;
		public boolean mIncludeClass = true;

		public boolean mUseZenithCharms = false;

		public List<LoadoutItem> mEquipment = new ArrayList<>();
		public Map<EquipmentSlot, ItemStack> mVanity = new EnumMap<>(EquipmentSlot.class);
		public int mLeftParrot;
		public int mRightParrot;
		public List<ItemUtils.ItemIdentifier> mCharms = new ArrayList<>();
		public List<ItemUtils.ItemIdentifier> mZenithCharms = new ArrayList<>();
		public LoadoutClass mClass = new LoadoutClass();

		public TreeSet<Keybind> mQuickSwaps = new TreeSet<>();

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
					mEquipment.add(new LoadoutItem(i, ItemUtils.getIdentifier(item, false),
						EXALTED_LOCATIONS.contains(ItemStatUtils.getLocation(item)) && ItemStatUtils.getRegion(item) == Region.RING,
						InfusionUtils.getCurrentInfusion(item).getInfusionType(),
						Objects.requireNonNullElse(DelveInfusionUtils.getCurrentInfusion(item), DelveInfusionUtils.DelveInfusionSelection.REFUND).getInfusionType(),
						IchorListener.getIchorInfusion(item)));
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
			CharmManager.CharmType type;
			List<ItemUtils.ItemIdentifier> loadoutCharms;
			if (mUseZenithCharms) {
				type = CharmManager.CharmType.ZENITH;
				loadoutCharms = mZenithCharms;
			} else {
				type = CharmManager.CharmType.NORMAL;
				loadoutCharms = mCharms;
			}
			List<ItemStack> charms = type.mPlayerCharms.get(player.getUniqueId());
			loadoutCharms.clear();
			if (charms != null) {
				for (ItemStack charm : charms) {
					loadoutCharms.add(ItemUtils.getIdentifier(charm, false));
				}
			}
		}

		public void setClassFromPlayer(Player player) {
			mClass.mAbilityScores.clear();
			mClass.mClassId = AbilityUtils.getClassNum(player);
			PlayerClass playerClass = new MonumentaClasses().getClassById(mClass.mClassId);
			if (playerClass == null) {
				mClass.mSpecId = 0;
				return;
			}
			mClass.mSpecId = AbilityUtils.getSpecNum(player);
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

		public JsonObject toJson() {
			JsonObject json = new JsonObject();

			json.addProperty("index", mIndex);
			json.addProperty("name", mName);

			json.addProperty("displayItem", ItemUtils.serializeItemStack(mDisplayItem));

			json.addProperty("includeEquipment", mIncludeEquipment);
			json.addProperty("includeVanity", mIncludeVanity);
			json.addProperty("includeCharms", mIncludeCharms);
			json.addProperty("includeClass", mIncludeClass);

			json.addProperty("useZenithCharms", mUseZenithCharms);

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

			JsonArray zenithCharms = new JsonArray();
			for (ItemUtils.ItemIdentifier charm : mZenithCharms) {
				if (!(charm instanceof ItemUtils.ZenithCharmIdentifier zenithCharm)) {
					continue;
				}
				JsonObject charmJson = new JsonObject();
				charmJson.addProperty("type", zenithCharm.mType.name());
				charmJson.addProperty("name", zenithCharm.mName);
				charmJson.addProperty("uuid", zenithCharm.mUUID);
				zenithCharms.add(charmJson);
			}
			json.add("zenithCharms", zenithCharms);

			json.add("class", mClass.toJson());

			JsonArray quickSwapsJson = new JsonArray();
			for (Keybind keybind : mQuickSwaps) {
				quickSwapsJson.add(keybind.asKeybind());
			}
			json.add("quickSwaps", quickSwapsJson);
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
			if (json.has("displayItemType")) { // old data format
				ItemUtils.ItemIdentifier itemIdentifier = parseItemIdentifier(json, "displayItemType", "displayItemName");
				loadout.mDisplayItem = new ItemStack(itemIdentifier.mType);
				if (itemIdentifier.mName != null) {
					loadout.mDisplayItem.editMeta(meta -> meta.displayName(itemIdentifier.getDisplayName()));
					ItemUtils.setPlainName(loadout.mDisplayItem, itemIdentifier.mName);
				}
			} else {
				ItemStack item = ItemUtils.parseItemStack(json.getAsJsonPrimitive("displayItem").getAsString());
				if (ItemStatUtils.isDirty(item)) {
					ItemUtils.setPlainTag(item);
					ItemUpdateHelper.generateItemStats(item);
					ItemStatUtils.removeDirty(item);
					item = VanityManager.cleanCopyForDisplay(item);
				}
				loadout.mDisplayItem = item;
			}

			loadout.mIncludeEquipment = json.getAsJsonPrimitive("includeEquipment").getAsBoolean();
			loadout.mIncludeVanity = json.getAsJsonPrimitive("includeVanity").getAsBoolean();
			loadout.mIncludeCharms = json.getAsJsonPrimitive("includeCharms").getAsBoolean();
			loadout.mIncludeClass = json.getAsJsonPrimitive("includeClass").getAsBoolean();

			loadout.mUseZenithCharms = false;
			JsonPrimitive useZenithCharmsPrimitive = json.getAsJsonPrimitive("useZenithCharms");
			if (useZenithCharmsPrimitive != null) {
				loadout.mUseZenithCharms = useZenithCharmsPrimitive.getAsBoolean();
			}

			for (JsonElement equipment : json.getAsJsonArray("equipment")) {
				loadout.mEquipment.add(LoadoutItem.fromJson(equipment.getAsJsonObject()));
			}

			for (JsonElement charm : json.getAsJsonArray("charms")) {
				loadout.mCharms.add(parseItemIdentifier(charm.getAsJsonObject(), "type", "name"));
			}

			JsonArray zenithCharms = json.getAsJsonArray("zenithCharms");
			if (zenithCharms != null) {
				for (JsonElement charm : json.getAsJsonArray("zenithCharms")) {
					loadout.mZenithCharms.add(parseItemIdentifier(charm.getAsJsonObject(), "type", "name", "uuid"));
				}
			}

			loadout.mClass = LoadoutClass.fromJson(json.getAsJsonObject("class"));

			JsonElement isQuickSwapJson = json.get("isQuickSwap");
			if (isQuickSwapJson instanceof JsonPrimitive isQuickSwapPrimitive && isQuickSwapPrimitive.isBoolean()) {
				if (isQuickSwapPrimitive.getAsBoolean()) {
					loadout.mQuickSwaps.add(Keybind.SWAP_OFFHAND);
				}
			} else {
				for (JsonElement quickSwapJson : json.getAsJsonArray("quickSwaps")) {
					if (quickSwapJson instanceof JsonPrimitive quickSwapPrimitive && quickSwapPrimitive.isString()) {
						Keybind keybind = Keybind.of(quickSwapPrimitive.getAsString());
						if (keybind != null) {
							loadout.mQuickSwaps.add(keybind);
						}
					}
				}
			}
			loadout.mClearEmpty = json.getAsJsonPrimitive("clearEmpty").getAsBoolean();

			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("vanity").entrySet()) {
				ItemStack item = ItemUtils.parseItemStack(entry.getValue().getAsString());
				if (ItemStatUtils.isDirty(item)) {
					ItemUtils.setPlainTag(item);
					ItemUpdateHelper.generateItemStats(item);
					ItemStatUtils.removeDirty(item);
					item = VanityManager.cleanCopyForDisplay(item);
				}
				loadout.mVanity.put(EquipmentSlot.valueOf(entry.getKey()), item);
			}

			loadout.mLeftParrot = json.getAsJsonPrimitive("leftParrot").getAsInt();
			loadout.mRightParrot = json.getAsJsonPrimitive("rightParrot").getAsInt();

			return loadout;
		}

		public @Nullable Component quickSwapsComponent() {
			if (mQuickSwaps.isEmpty()) {
				return null;
			}
			return Component.text("Quick-Swap Loadout for ", NamedTextColor.GOLD, TextDecoration.ITALIC)
				       .append(MessagingUtils.concatenateComponentsWithAnd(mQuickSwaps.stream()
					                                                           .map(Component::keybind)
					                                                           .collect(Collectors.toUnmodifiableList())));
		}
	}

	public static class LoadoutItem {
		public int mSlot;
		public ItemUtils.ItemIdentifier mIdentifier;
		public boolean mIsExalted;
		public @Nullable InfusionType mInfusionType;
		public @Nullable InfusionType mDelveInfusionType;
		public @Nullable InfusionType mIchorInfusionType;

		public LoadoutItem(int slot, ItemUtils.ItemIdentifier identifier, boolean isExalted, @Nullable InfusionType infusionType, @Nullable InfusionType delveInfusionType, @Nullable InfusionType ichorInfusionType) {
			mSlot = slot;
			mIdentifier = identifier;
			mIsExalted = isExalted;
			mInfusionType = infusionType;
			mDelveInfusionType = delveInfusionType;
			mIchorInfusionType = ichorInfusionType;
		}

		public JsonObject toJson() {
			JsonObject json = new JsonObject();
			json.addProperty("slot", mSlot);
			json.addProperty("type", mIdentifier.mType.name());
			json.addProperty("name", mIdentifier.mName);
			json.addProperty("exalted", mIsExalted);
			json.addProperty("infusion", mInfusionType == null ? null : mInfusionType.name());
			json.addProperty("delveInfusion", mDelveInfusionType == null ? null : mDelveInfusionType.name());
			json.addProperty("ichorInfusion", mIchorInfusionType == null ? null : mIchorInfusionType.name());
			return json;
		}

		public static LoadoutItem fromJson(JsonObject json) {
			int slot = json.getAsJsonPrimitive("slot").getAsInt();
			ItemUtils.ItemIdentifier identifier = parseItemIdentifier(json, "type", "name");
			boolean exalted = json.has("exalted") && json.getAsJsonPrimitive("exalted").getAsBoolean();
			InfusionType infusion;
			try {
				JsonPrimitive infusionElement = json.getAsJsonPrimitive("infusion");
				infusion = infusionElement == null ? null : InfusionType.valueOf(infusionElement.getAsString());
			} catch (IllegalArgumentException e) {
				infusion = null;
			}
			InfusionType delveInfusion;
			try {
				JsonPrimitive infusionElement = json.getAsJsonPrimitive("delveInfusion");
				delveInfusion = infusionElement == null ? null : InfusionType.valueOf(infusionElement.getAsString());
			} catch (IllegalArgumentException e) {
				delveInfusion = null;
			}
			InfusionType ichorInfusion;
			try {
				JsonPrimitive infusionElement = json.getAsJsonPrimitive("ichorInfusion");
				ichorInfusion = infusionElement == null ? null : InfusionType.valueOf(infusionElement.getAsString());
			} catch (IllegalArgumentException e) {
				ichorInfusion = null;
			}
			return new LoadoutItem(slot, identifier, exalted, infusion, delveInfusion, ichorInfusion);
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
