package com.playmonumenta.plugins.itemstats.gui;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.VanityManager;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class PlayerItemStatsGUI extends CustomInventory {

	static class Settings {
		final EnumSet<PSGUISecondaryStat> mSecondaryStatEnabled = EnumSet.noneOf(PSGUISecondaryStat.class);
	}

	enum InfusionSetting {
		DISABLED("Ignore all infusions in calculations", null),
		ENABLED("Respect existing infusions on items", null),
		ENABLED_FULL("Existing infusions + delve infusions fully active", null),
		VITALITY("20 Vitality + 4 Tenacity", InfusionType.VITALITY),
		TENACITY("24 Tenacity", InfusionType.TENACITY),
		VIGOR("24 Vigor", InfusionType.VIGOR),
		FOCUS("24 Focus", InfusionType.FOCUS),
		PERSPICACITY("24 Perspicacity", InfusionType.PERSPICACITY),
		;

		/**
		 * Set of infusions that are only considered active with the setting {@link #ENABLED_FULL}
		 */
		static final ImmutableSet<InfusionType> CONDITIONAL_DELVE_INFUSIONS = ImmutableSet.of(
			InfusionType.ARDOR,
			InfusionType.CARAPACE,
			InfusionType.CHOLER,
			InfusionType.DECAPITATION,
			InfusionType.EXECUTION,
			InfusionType.EXPEDITE,
			InfusionType.FUELED,
			InfusionType.MITOSIS,
			InfusionType.VENGEFUL
		);

		private final String mDescription;
		final @Nullable InfusionType mInfusionType;

		InfusionSetting(String description, @Nullable InfusionType infusionType) {
			this.mDescription = description;
			this.mInfusionType = infusionType;
		}
	}

	private static class StatItem {
		private final int mSlot;
		private final Material mIcon;
		private final Component mName;
		private final List<PSGUIStat> mDisplayedStats;

		private StatItem(int mSlot, Material mIcon, Component mName, PSGUIStat... displayedStats) {
			this.mSlot = mSlot;
			this.mIcon = mIcon;
			this.mName = mName;
			this.mDisplayedStats = List.of(displayedStats);
		}

		public ItemStack getDisplay(PSGUIStats stats, @Nullable PSGUIStats otherStats) {
			ItemStack icon = getCleanItem(new ItemStack(mIcon, 1));
			ItemMeta meta = icon.getItemMeta();
			meta.displayName(mName);
			meta.lore(mDisplayedStats.stream().map(stat -> stat.getDisplay(stats, otherStats)).toList());
			icon.setItemMeta(meta);
			ItemUtils.setPlainName(icon);
			return icon;
		}
	}

	private static final StatItem HEALTH_INFO = new StatItem(13, Material.APPLE,
		Component.text("Health and Healing", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		PSGUIStat.HEALTH, PSGUIStat.HEALING_RATE, PSGUIStat.EFFECTIVE_HEALING_RATE, PSGUIStat.REGENERATION, PSGUIStat.EFFECTIVE_REGENERATION, PSGUIStat.LIFE_DRAIN, PSGUIStat.EFFECTIVE_LIFE_DRAIN);

	private static final StatItem HEALTH_NORMALIZED_DAMAGE_RESISTANCE = new StatItem(21, Material.GLOWSTONE_DUST,
		Component.text("Health Normalized Damage Reduction", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		PSGUIStat.EFFECTIVE_MELEE_DAMAGE_TAKEN, PSGUIStat.EFFECTIVE_PROJECTILE_DAMAGE_TAKEN, PSGUIStat.EFFECTIVE_MAGIC_DAMAGE_TAKEN,
		PSGUIStat.EFFECTIVE_BLAST_DAMAGE_TAKEN, PSGUIStat.EFFECTIVE_FIRE_DAMAGE_TAKEN, PSGUIStat.EFFECTIVE_FALL_DAMAGE_TAKEN, PSGUIStat.EFFECTIVE_AILMENT_DAMAGE_TAKEN);

	private static final StatItem DAMAGE_RESISTANCE = new StatItem(22, Material.SUGAR,
		Component.text("Damage Reduction", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		PSGUIStat.MELEE_DAMAGE_TAKEN, PSGUIStat.PROJECTILE_DAMAGE_TAKEN, PSGUIStat.MAGIC_DAMAGE_TAKEN, PSGUIStat.BLAST_DAMAGE_TAKEN, PSGUIStat.FIRE_DAMAGE_TAKEN, PSGUIStat.FALL_DAMAGE_TAKEN, PSGUIStat.AILMENT_DAMAGE_TAKEN);

	private static final StatItem EFFECTIVE_HEALTH = new StatItem(23, Material.REDSTONE,
		Component.text("Effective Health", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		PSGUIStat.MELEE_EHP, PSGUIStat.PROJECTILE_EHP, PSGUIStat.MAGIC_EHP, PSGUIStat.BLAST_EHP, PSGUIStat.FIRE_EHP, PSGUIStat.FALL_EHP, PSGUIStat.AILMENT_EHP);

	private static final StatItem MELEE_INFO = new StatItem(30, Material.IRON_SWORD,
		Component.text("Melee", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		/*Stat.ATTACK_DAMAGE_ADD,*/ PSGUIStat.ATTACK_DAMAGE_MULTIPLY, PSGUIStat.TOTAL_ATTACK_DAMAGE, PSGUIStat.ATTACK_SPEED);

	private static final StatItem PROJECTILE_INFO = new StatItem(31, Material.BOW,
		Component.text("Projectile", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		/*Stat.PROJECTILE_DAMAGE_ADD,*/ PSGUIStat.PROJECTILE_DAMAGE_MULTIPLY, PSGUIStat.TOTAL_PROJECTILE_DAMAGE, PSGUIStat.PROJECTILE_SPEED, PSGUIStat.PROJECTILE_RATE);

	private static final StatItem MAGIC_INFO = new StatItem(32, Material.BLAZE_ROD,
		Component.text("Magic", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		PSGUIStat.SPELL_POWER, /*Stat.MAGIC_DAMAGE_ADD,*/ PSGUIStat.MAGIC_DAMAGE_MULTIPLY, PSGUIStat.TOTAL_SPELL_DAMAGE, PSGUIStat.COOLDOWN_MULTIPLIER);

	private static final StatItem MISC_INFO = new StatItem(40, Material.LEATHER_BOOTS,
		Component.text("Misc", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		PSGUIStat.ARMOR, PSGUIStat.AGILITY, PSGUIStat.MOVEMENT_SPEED, PSGUIStat.KNOCKBACK_RESISTANCE, PSGUIStat.THORNS_DAMAGE, PSGUIStat.MINING_SPEED);

	private static final StatItem[] STAT_ITEMS = {
		HEALTH_INFO,
		DAMAGE_RESISTANCE, HEALTH_NORMALIZED_DAMAGE_RESISTANCE, EFFECTIVE_HEALTH,
		MELEE_INFO, PROJECTILE_INFO, MAGIC_INFO,
		MISC_INFO
	};

	private static final int REGION_SETTING_SLOT = 4;
	public static final ImmutableMap<ItemStatUtils.Region, ItemStack> REGION_ICONS = ImmutableMap.of(
		ItemStatUtils.Region.VALLEY, ItemUtils.parseItemStack("{id:\"minecraft:cyan_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"sc\",Color:3},{Pattern:\"mc\",Color:11},{Pattern:\"flo\",Color:15},{Pattern:\"bts\",Color:11},{Pattern:\"tts\",Color:11}]},HideFlags:63,display:{Name:'{\"text\":\"Calculation Region: King\\'s Valley\",\"italic\":false,\"bold\":true,\"color\":\"aqua\"}'}}}"),
		ItemStatUtils.Region.ISLES, ItemUtils.parseItemStack("{id:\"minecraft:green_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"gru\",Color:5},{Pattern:\"bo\",Color:13},{Pattern:\"mr\",Color:13},{Pattern:\"mc\",Color:5}]},HideFlags:63,display:{Name:'{\"text\":\"Calculation Region: Celsian Isles\",\"italic\":false,\"bold\":true,\"color\":\"green\"}'}}}"),
		ItemStatUtils.Region.RING, ItemUtils.parseItemStack("{id:\"minecraft:white_banner\",Count:1b,tag:{BlockEntityTag:{Patterns:[{Pattern:\"ss\",Color:12},{Pattern:\"bts\",Color:13},{Pattern:\"tts\",Color:13},{Pattern:\"gra\",Color:8},{Pattern:\"ms\",Color:13},{Pattern:\"gru\",Color:7},{Pattern:\"flo\",Color:15},{Pattern:\"mc\",Color:0}]},HideFlags:63,display:{Name:'{\"bold\":true,\"italic\":false,\"underlined\":false,\"color\":\"white\",\"text\":\"Calculation Region: Architect\\\\u0027s Ring\"}'}}}")
	);

	private static final int SWAP_EQUIPMENT_SET_SLOT = 49;
	private static final int INFUSION_SETTINGS_LEFT_SLOT = 12;
	private static final int INFUSION_SETTINGS_RIGHT_SLOT = 14;

	private final Settings mSettings = new Settings();
	private final PSGUIStats mLeftStats;
	private final PSGUIStats mRightStats;

	private final boolean mShowVanity;

	private boolean mSelectedRightEquipmentSet;
	private @Nullable PSGUIEquipment mSelectedEquipmentsSlot = null;

	public PlayerItemStatsGUI(Player player) {
		this(player, null);
	}

	public PlayerItemStatsGUI(Player player, @Nullable Player otherPlayer) {
		super(player, 54, Component.text("Player Stats Calculator", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		mShowVanity = Plugin.getInstance().mVanityManager.getData(player).mGuiVanityEnabled;
		mLeftStats = new PSGUIStats(player, null, mSettings);
		mRightStats = new PSGUIStats(player, mLeftStats, mSettings);

		setEquipmentFromPlayer(false, player);
		if (otherPlayer != null) {
			setEquipmentFromPlayer(true, otherPlayer);
		}
		ItemStatUtils.Region region = Stream.of(mLeftStats.getMaximumRegion(false, ServerProperties.getRegion(player)), mRightStats.getMaximumRegion(false, ServerProperties.getRegion(player)))
			                              .max(Comparator.naturalOrder())
			                              .orElse(ServerProperties.getRegion(player));
		mLeftStats.mPlayerItemStats.setRegion(region);
		mRightStats.mPlayerItemStats.setRegion(region);
		generateInventory();
	}

	private void setEquipmentFromPlayer(boolean right, Player player) {
		PSGUIStats stats = right ? mRightStats : mLeftStats;

		stats.mEquipment.clear();
		stats.mDisplayedEquipment.clear();
		for (PSGUIEquipment slot : PSGUIEquipment.values()) {
			setEquipmentFromPlayer(stats, player, slot);
		}

		stats.mOriginalEquipment.clear();
		stats.mOriginalEquipment.putAll(stats.mEquipment);
		stats.mOriginalDisplayedEquipment.clear();
		stats.mOriginalDisplayedEquipment.putAll(stats.mDisplayedEquipment);
	}

	private void setEquipmentFromPlayer(PSGUIStats stats, Player player, PSGUIEquipment slot) {
		ItemStack item = player.getInventory().getItem(slot.mEquipmentSlot);
		setEquipment(stats, slot, item);
		stats.mDisplayedEquipment.put(slot, getPlayerItemWithVanity(player, slot.mEquipmentSlot));
	}

	private ItemStack getPlayerItemWithVanity(Player player, EquipmentSlot slot) {
		return getPlayerItemWithVanity(player, slot, mShowVanity);
	}

	public static ItemStack getPlayerItemWithVanity(Player player, EquipmentSlot slot, boolean withVanity) {
		ItemStack item = player.getInventory().getItem(slot);
		if (item.getType() != Material.AIR && withVanity) {
			VanityManager.VanityData vanityData = Plugin.getInstance().mVanityManager.getData(player);
			if (vanityData.getEquipped(slot) != null) {
				ItemStack vanityItem = ItemUtils.clone(item);
				if (VanityManager.isInvisibleVanityItem(vanityData.getEquipped(slot))) {
					ItemMeta meta = vanityItem.getItemMeta();
					if (meta != null) {
						List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
						lore.add(0, Component.text("Invisibility vanity skin applied", NamedTextColor.GOLD));
						meta.lore(lore);
						vanityItem.setItemMeta(meta);
					}
				} else {
					VanityManager.applyVanity(vanityItem, vanityData, slot, false);
				}
				return vanityItem;
			}
		}
		return item;
	}

	private void setEquipment(PSGUIStats stats, PSGUIEquipment slot, ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		item = ItemUtils.clone(item);
		stats.mEquipment.put(slot, item);
		stats.mDisplayedEquipment.put(slot, item);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClick() == ClickType.DOUBLE_CLICK) {
			return;
		}

		int slot = event.getSlot();
		Inventory inv = event.getClickedInventory();
		if (inv == null) {
			return;
		}

		if (inv.equals(mInventory)) {
			if (slot == SWAP_EQUIPMENT_SET_SLOT) {
				EnumMap<PSGUIEquipment, ItemStack> leftEquipment = new EnumMap<>(mLeftStats.mEquipment);
				mLeftStats.mEquipment.clear();
				mLeftStats.mEquipment.putAll(mRightStats.mEquipment);
				mRightStats.mEquipment.clear();
				mRightStats.mEquipment.putAll(leftEquipment);
				EnumMap<PSGUIEquipment, ItemStack> leftDisplayEquipment = new EnumMap<>(mLeftStats.mDisplayedEquipment);
				mLeftStats.mDisplayedEquipment.clear();
				mLeftStats.mDisplayedEquipment.putAll(mRightStats.mDisplayedEquipment);
				mRightStats.mDisplayedEquipment.clear();
				mRightStats.mDisplayedEquipment.putAll(leftDisplayEquipment);
				InfusionSetting leftInfusionSetting = mLeftStats.mInfusionSetting;
				mLeftStats.mInfusionSetting = mRightStats.mInfusionSetting;
				mRightStats.mInfusionSetting = leftInfusionSetting;
				generateInventory();
				return;
			}

			if (slot == REGION_SETTING_SLOT) {
				ItemStatUtils.Region region = mLeftStats.mPlayerItemStats.getRegion() == ItemStatUtils.Region.VALLEY ? ItemStatUtils.Region.ISLES
					                              : mLeftStats.mPlayerItemStats.getRegion() == ItemStatUtils.Region.ISLES ? ItemStatUtils.Region.RING
						                                : ItemStatUtils.Region.VALLEY;
				mLeftStats.mPlayerItemStats.setRegion(region);
				mRightStats.mPlayerItemStats.setRegion(region);
				generateInventory();
				return;
			}

			for (PSGUISecondaryStat stat : PSGUISecondaryStat.values()) {
				if (slot == stat.getSlot()) {
					if (!mSettings.mSecondaryStatEnabled.remove(stat)) {
						mSettings.mSecondaryStatEnabled.add(stat);
					}
					generateInventory();
					return;
				}
			}

			if (slot == INFUSION_SETTINGS_LEFT_SLOT || slot == INFUSION_SETTINGS_RIGHT_SLOT) {
				PSGUIStats stats = (slot == INFUSION_SETTINGS_LEFT_SLOT ? mLeftStats : mRightStats);
				stats.mInfusionSetting = InfusionSetting.values()[(stats.mInfusionSetting.ordinal() + (event.getClick().isLeftClick() ? 1 : InfusionSetting.values().length - 1)) % InfusionSetting.values().length];
				generateInventory();
				return;
			}

			for (PSGUIEquipment equipment : PSGUIEquipment.values()) {
				if (slot == equipment.mRightSlot) {
					if (event.isRightClick()) {
						mRightStats.mEquipment.put(equipment, mRightStats.mOriginalEquipment.get(equipment));
						mRightStats.mDisplayedEquipment.put(equipment, mRightStats.mOriginalDisplayedEquipment.get(equipment));
					} else {
						if (mSelectedEquipmentsSlot == equipment && mSelectedRightEquipmentSet) {
							mSelectedEquipmentsSlot = null;
						} else if (mRightStats.mEquipment.get(equipment) == null) {
							mSelectedEquipmentsSlot = equipment;
							mSelectedRightEquipmentSet = true;
						} else {
							mRightStats.mEquipment.remove(equipment);
							mRightStats.mDisplayedEquipment.remove(equipment);
						}
					}

					generateInventory();
					return;
				}
				if (slot == equipment.mLeftSlot) {
					if (event.isRightClick()) {
						mLeftStats.mEquipment.put(equipment, mLeftStats.mOriginalEquipment.get(equipment));
						mLeftStats.mDisplayedEquipment.put(equipment, mLeftStats.mOriginalDisplayedEquipment.get(equipment));
					} else {
						if (mSelectedEquipmentsSlot == equipment && !mSelectedRightEquipmentSet) {
							mSelectedEquipmentsSlot = null;
						} else if (mLeftStats.mEquipment.get(equipment) == null) {
							mSelectedEquipmentsSlot = equipment;
							mSelectedRightEquipmentSet = false;
						} else {
							mLeftStats.mEquipment.remove(equipment);
							mLeftStats.mDisplayedEquipment.remove(equipment);
						}
					}

					generateInventory();
					return;
				}
			}
		} else {
			ItemStack clickedItem = inv.getItem(slot);
			if (clickedItem != null && clickedItem.getType() != Material.AIR) {
				ItemStack item = new ItemStack(clickedItem);
				if (ShulkerEquipmentListener.isEquipmentBox(item)
					&& item.getItemMeta() instanceof BlockStateMeta meta
					&& meta.getBlockState() instanceof ShulkerBox shulker) {
					PSGUIStats stats;
					if (event.getClick().isShiftClick()) {
						stats = event.getClick().isLeftClick() ? mRightStats : mLeftStats;
					} else if (mSelectedEquipmentsSlot != null) {
						stats = mSelectedRightEquipmentSet ? mRightStats : mLeftStats;
					} else {
						return;
					}
					mSelectedEquipmentsSlot = null;

					for (PSGUIEquipment equipment : PSGUIEquipment.values()) {
						Integer shulkerSlot = ShulkerEquipmentListener.getShulkerSlot(equipment.mPlayerInventorySlot);
						if (shulkerSlot == null) {
							continue;
						}
						ItemStack shulkerItem = shulker.getInventory().getItem(shulkerSlot);
						if (shulkerItem != null && shulkerItem.getType() != Material.AIR) {
							setEquipment(stats, equipment, shulkerItem);
						}
					}
					generateInventory();
					return;
				}
				PSGUIEquipment targetSlot = null;
				boolean targetRightSet = false;
				if (event.getClick().isShiftClick()) {
					targetRightSet = event.getClick().isLeftClick();
					for (PSGUIEquipment equipment : new PSGUIEquipment[] {PSGUIEquipment.HEAD, PSGUIEquipment.CHEST, PSGUIEquipment.LEGS, PSGUIEquipment.FEET}) {
						if (isValid(equipment, item.getType())) {
							targetSlot = equipment;
							break;
						}
					}
					if (targetSlot == null) {
						if (ItemStatUtils.hasAttributeInSlot(item, Slot.OFFHAND)) {
							targetSlot = PSGUIEquipment.OFFHAND;
						} else {
							targetSlot = PSGUIEquipment.MAINHAND;
						}
					}
				} else if (mSelectedEquipmentsSlot != null) {
					if (isValid(mSelectedEquipmentsSlot, item.getType())) {
						targetSlot = mSelectedEquipmentsSlot;
						targetRightSet = mSelectedRightEquipmentSet;
					}
				}
				if (targetSlot != null) {
					setEquipment(targetRightSet ? mRightStats : mLeftStats, targetSlot, item);
					mSelectedEquipmentsSlot = null;
					generateInventory();
				}
			}
		}
	}

	private boolean isValid(PSGUIEquipment equipment, Material material) {
		return equipment == PSGUIEquipment.MAINHAND
			       || equipment == PSGUIEquipment.OFFHAND
			       || equipment.mEquipmentSlot == material.getEquipmentSlot();
	}

	private static ItemStack getCleanItem(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);
		return item;
	}

	private static ItemStack makePlaceholderItem(PSGUIEquipment equipment, boolean selected) {
		ItemStack empty = getCleanItem(new ItemStack(equipment.getIcon(), 1));
		ItemMeta meta = empty.getItemMeta();
		meta.displayName(equipment.getDisplay(selected));
		if (selected) {
			meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
		}
		empty.setItemMeta(meta);
		empty.lore(equipment.getLore());
		ItemUtils.setPlainTag(empty);
		return empty;
	}

	private static @Nullable ItemStack getWarningIcon(PSGUIStats stats) {
		List<Component> warnings = new ArrayList<>();
		if (stats.getRegionScaling(stats.mPlayer, false) > 0) {
			warnings.add(Component.text("Build has equipment of a later region.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		if (stats.getRegionScaling(stats.mPlayer, true) > 0) {
			warnings.add(Component.text("Build has mainhand of a later region.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		if (stats.get(EnchantmentType.CURSE_OF_CORRUPTION) > 1) {
			warnings.add(Component.text("Build has more than one Curse of Corruption item.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		if (stats.get(EnchantmentType.TWO_HANDED) > 0
			    && stats.getItem(PSGUIEquipment.OFFHAND) != null
			    && ItemStatUtils.getEnchantmentLevel(stats.getItem(PSGUIEquipment.OFFHAND), EnchantmentType.WEIGHTLESS) == 0) {
			warnings.add(Component.text("Build has a Two Handed item, but a non-Weightless offhand.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		if (warnings.isEmpty()) {
			return null;
		}
		ItemStack item = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Warning!", NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		meta.lore(warnings);
		item.setItemMeta(meta);
		ItemUtils.setPlainName(item);
		return item;
	}

	private void generateInventory() {

		for (PSGUIStats stats : new PSGUIStats[] {mLeftStats, mRightStats}) {
			stats.mStatCache.clear();
			stats.mPlayerItemStats.updateStats(stats.getItem(PSGUIEquipment.MAINHAND), stats.getItem(PSGUIEquipment.OFFHAND),
				stats.getItem(PSGUIEquipment.HEAD), stats.getItem(PSGUIEquipment.CHEST), stats.getItem(PSGUIEquipment.LEGS), stats.getItem(PSGUIEquipment.FEET), (Player) mInventory.getHolder(), true);
		}

		ItemStack swapItem = new ItemStack(Material.ARMOR_STAND, 1);
		ItemMeta swapItemMeta = swapItem.getItemMeta();
		swapItemMeta.displayName(Component.text("Swap Equipment Sets", NamedTextColor.WHITE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		swapItem.setItemMeta(swapItemMeta);
		mInventory.setItem(SWAP_EQUIPMENT_SET_SLOT, swapItem);

		mInventory.setItem(REGION_SETTING_SLOT, REGION_ICONS.get(mLeftStats.mPlayerItemStats.getRegion()));

		for (StatItem statItem : STAT_ITEMS) {
			mInventory.setItem(statItem.mSlot, statItem.getDisplay(mLeftStats, mRightStats.mEquipment.isEmpty() && mRightStats.mInfusionSetting == InfusionSetting.DISABLED ? null : mRightStats));
		}

		for (PSGUISecondaryStat stat : PSGUISecondaryStat.values()) {
			ItemStack item = getCleanItem(new ItemStack(stat.getIcon(), 1));
			ItemMeta meta = item.getItemMeta();
			if (mSettings.mSecondaryStatEnabled.contains(stat)) {
				meta.displayName(stat.getDisplay(true));
				meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
			} else {
				meta.displayName(stat.getDisplay(false));
			}
			meta.lore(stat.getDisplayLore());
			item.setItemMeta(meta);
			mInventory.setItem(stat.getSlot(), item);
		}

		BiFunction<Boolean, InfusionSetting, ItemStack> makeInfusionSettingsItem = (left, selectedSetting) -> {
			ItemStack item = new ItemStack(Material.ANVIL);
			ItemMeta meta = item.getItemMeta();
			meta.displayName(Component.text("Infusion Settings", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("Click to select how infusion are", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("treated by this calculator.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Selected option:", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			for (InfusionSetting setting : InfusionSetting.values()) {
				String line;
				if (setting == InfusionSetting.DISABLED && !left) {
					line = "Use option chosen on the left build";
				} else {
					line = setting.mDescription;
				}
				boolean selected = setting == selectedSetting;
				lore.add(Component.text((selected ? "+ " : "- ") + line, selected ? NamedTextColor.GREEN : NamedTextColor.GRAY)
					         .decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, selected));
			}
			meta.lore(lore);
			item.setItemMeta(meta);
			return item;
		};
		mInventory.setItem(INFUSION_SETTINGS_LEFT_SLOT, makeInfusionSettingsItem.apply(true, mLeftStats.mInfusionSetting));
		mInventory.setItem(INFUSION_SETTINGS_RIGHT_SLOT, makeInfusionSettingsItem.apply(false, mRightStats.mInfusionSetting));

		mInventory.setItem(28, getWarningIcon(mLeftStats));
		mInventory.setItem(34, getWarningIcon(mRightStats));

		// Set plain name/lore tags. This must be before equipment items are added to not break vanity.
		for (ItemStack item : mInventory) {
			if (item != null) {
				ItemUtils.setPlainTag(item);
			}
		}

		for (PSGUIEquipment equipment : PSGUIEquipment.values()) {
			ItemStack leftItem = mLeftStats.mDisplayedEquipment.get(equipment);
			mInventory.setItem(equipment.mLeftSlot, leftItem != null && leftItem.getType() != Material.AIR ? leftItem
				                                        : makePlaceholderItem(equipment, equipment == mSelectedEquipmentsSlot && !mSelectedRightEquipmentSet));

			ItemStack rightItem = mRightStats.mDisplayedEquipment.get(equipment);
			mInventory.setItem(equipment.mRightSlot, rightItem != null && rightItem.getType() != Material.AIR ? rightItem
				                                         : makePlaceholderItem(equipment, equipment == mSelectedEquipmentsSlot && mSelectedRightEquipmentSet));
		}

		GUIUtils.fillWithFiller(mInventory, Material.BLACK_STAINED_GLASS_PANE);

	}
}
