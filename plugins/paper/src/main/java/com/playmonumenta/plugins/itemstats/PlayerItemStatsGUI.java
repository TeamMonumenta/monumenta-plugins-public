package com.playmonumenta.plugins.itemstats;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import javax.annotation.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Operation;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.scriptedquests.utils.CustomInventory;

import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class PlayerItemStatsGUI extends CustomInventory {

	public enum HealthStat {
		HEALING(13, Material.APPLE, "Effective Healing Rate");

		private final int mSlot;
		private final Material mIcon;
		private final String mName;

		HealthStat(int slot, Material icon, String name) {
			mSlot = slot;
			mIcon = icon;
			mName = name;
		}

		public int getSlot() {
			return mSlot;
		}

		public Material getIcon() {
			return mIcon;
		}

		public Component getDisplay(double value) {
			return Component.text(String.format("%s: %s", mName, PERCENT_FORMATTER.format(value)), NamedTextColor.WHITE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		}

		public Component getDisplay(double value, double valueOther) {
			NamedTextColor color;
			if (valueOther > value) {
				color = NamedTextColor.GREEN;
			} else if (valueOther < value) {
				color = NamedTextColor.RED;
			} else {
				color = NamedTextColor.WHITE;
			}

			return getDisplay(value).append(Component.text(String.format(" -> %s", PERCENT_FORMATTER.format(valueOther)), color)).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		}
	}

	public enum DefenseStat {
		MELEE(21, Material.IRON_INGOT, "Melee"),
		PROJECTILE(22, Material.FLINT, "Projectile"),
		MAGIC(23, Material.PRISMARINE_CRYSTALS, "Magic"),
		BLAST(30, Material.GUNPOWDER, "Blast"),
		FIRE(31, Material.BLAZE_POWDER, "Fire"),
		FALL(32, Material.FEATHER, "Fall");

		private final int mSlot;
		private final Material mIcon;
		private final String mName;

		DefenseStat(int slot, Material icon, String name) {
			mSlot = slot;
			mIcon = icon;
			mName = name;
		}

		public int getSlot() {
			return mSlot;
		}

		public Material getIcon() {
			return mIcon;
		}

		public Component getDisplay(double value, double health, Metric metric) {
			return Component.text(String.format("%s %s: %s", mName, metric.getAbbreviation(), getDisplayString(value, health, metric)), NamedTextColor.WHITE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		}

		public Component getDisplay(double value, double health, double valueOther, double healthOther, Metric metric) {
			double percent = metric == Metric.DAMAGE_REDUCTION ? (value / valueOther - 1) : (value / valueOther * healthOther / health - 1);
			Component relative = Component.text(String.format(" (%s)", PERCENT_CHANGE_FORMATTER.format(percent)), NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);

			NamedTextColor color;
			if (percent > 0) {
				color = NamedTextColor.GREEN;
			} else if (percent < 0) {
				color = NamedTextColor.RED;
			} else {
				color = NamedTextColor.WHITE;
			}

			return getDisplay(value, health, metric).append(Component.text(String.format(" -> %s", getDisplayString(valueOther, healthOther, metric)), color)).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false).append(relative);
		}

		private String getDisplayString(double value, double health, Metric metric) {
			if (metric == Metric.NORMALIZED_DAMAGE_REDUCTION) {
				return PERCENT_FORMATTER.format(1 - 20 / health * value);
			} else if (metric == Metric.DAMAGE_REDUCTION) {
				return PERCENT_FORMATTER.format(1 - value);
			} else {
				return NUMBER_FORMATTER.format(health / value);
			}
		}
	}

	public enum SecondaryStat {
		SHIELDING(0, Material.NAUTILUS_SHELL, EnchantmentType.SHIELDING, true),
		POISE(1, Material.LILY_OF_THE_VALLEY, EnchantmentType.POISE, true),
		INURE(2, Material.NETHERITE_SCRAP, EnchantmentType.INURE, true),
		STEADFAST(3, Material.LEAD, EnchantmentType.STEADFAST, true),
		ETHEREAL(5, Material.PHANTOM_MEMBRANE, EnchantmentType.ETHEREAL, false),
		REFLEXES(6, Material.ENDER_EYE, EnchantmentType.REFLEXES, false),
		EVASION(7, Material.ELYTRA, EnchantmentType.EVASION, false),
		TEMPO(8, Material.CLOCK, EnchantmentType.TEMPO, false);

		private final int mSlot;
		private final Material mIcon;
		private final String mName;
		private final boolean mIsArmorModifier;
		private final EnchantmentType mEnchantmentType;

		SecondaryStat(int slot, Material icon, EnchantmentType enchantmentType, boolean isArmorModifier) {
			mSlot = slot;
			mIcon = icon;
			mName = enchantmentType.getName();
			mEnchantmentType = enchantmentType;
			mIsArmorModifier = isArmorModifier;
		}

		public int getSlot() {
			return mSlot;
		}

		public Material getIcon() {
			return mIcon;
		}

		public String getName() {
			return mName;
		}

		public EnchantmentType getEnchantmentType() {
			return mEnchantmentType;
		}

		public boolean isArmorModifier() {
			return mIsArmorModifier;
		}

		public Component getDisplay(boolean enabled) {
			return Component.text(String.format("Calculate Bonus from %s%s", mName, enabled ? " - Enabled" : " - Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		}
	}

	public enum Metric {
		NORMALIZED_DAMAGE_REDUCTION(48, Material.GLOWSTONE_DUST, "Health Normalized DR", "HNDR"),
		DAMAGE_REDUCTION(49, Material.SUGAR, "Damage Reduction", "DR"),
		EFFECTIVE_HEALTH(50, Material.REDSTONE, "Effective Health", "EH");

		private final int mSlot;
		private final Material mIcon;
		private final String mName;
		private final String mAbbreviation;

		Metric(int slot, Material icon, String name, String abbreviation) {
			mSlot = slot;
			mIcon = icon;
			mName = name;
			mAbbreviation = abbreviation;
		}

		public int getSlot() {
			return mSlot;
		}

		public Material getIcon() {
			return mIcon;
		}

		public String getName() {
			return mName;
		}

		public String getAbbreviation() {
			return mAbbreviation;
		}

		public Component getDisplay(boolean selected) {
			return Component.text(String.format("Display %s (%s) %s", mName, mAbbreviation, selected ? " - Selected" : ""), selected ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		}
	}

	public enum CurrentEquipment {
		MAINHAND(46),
		OFFHAND(19),
		HEAD(18),
		CHEST(27),
		LEGS(36),
		FEET(45);

		private final int mSlot;

		CurrentEquipment(int slot) {
			mSlot = slot;
		}

		public int getSlot() {
			return mSlot;
		}
	}

	public enum OtherEquipment {
		MAINHAND(52, "Main Hand"),
		OFFHAND(25, "Off Hand"),
		HEAD(26, "Head"),
		CHEST(35, "Chest"),
		LEGS(44, "Legs"),
		FEET(53, "Feet");

		private final int mSlot;
		private final String mName;
		private final List<Component> mLore = List.of(Component.text("Click here, then click an item to compare builds.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

		OtherEquipment(int slot, String name) {
			mSlot = slot;
			mName = name;
		}

		public int getSlot() {
			return mSlot;
		}

		public Material getIcon() {
			return Material.ITEM_FRAME;
		}

		public Component getDisplay(boolean selected) {
			return Component.text(String.format("%s Slot%s", mName, selected ? " (Selected)" : ""), selected ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		}

		public List<Component> getLore() {
			return mLore;
		}
	}

	private static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("0.##");
	private static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("0.##%");
	private static final DecimalFormat PERCENT_CHANGE_FORMATTER = new DecimalFormat("+0.##%;-0.##%");

	private final Player mPlayer;
	private final PlayerItemStats mCurrentStats;
	private final PlayerItemStats mOtherStats;

	private Metric mMetric = Metric.DAMAGE_REDUCTION;
	private boolean[] mSecondaryStatEnabled = new boolean[54];
	private @Nullable OtherEquipment mSelected = null;
	private EnumMap<OtherEquipment, ItemStack> mOtherEquipment = new EnumMap<>(OtherEquipment.class);

	public PlayerItemStatsGUI(Player player) {
		super(player, 54);
		mPlayer = player;
		mCurrentStats = new PlayerItemStats(player);
		mOtherStats = new PlayerItemStats(player);
		generateInventory();
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);

		int slot = event.getSlot();
		Inventory inv = event.getClickedInventory();
		if (inv == null) {
			return;
		}

		if (inv.equals(mInventory)) {
			for (SecondaryStat stat : SecondaryStat.values()) {
				if (slot == stat.getSlot()) {
					mSecondaryStatEnabled[slot] ^= true;
					generateInventory();
					return;
				}
			}

			for (Metric metric : Metric.values()) {
				if (slot == metric.getSlot()) {
					mMetric = metric;
					generateInventory();
					return;
				}
			}

			for (OtherEquipment equipment : OtherEquipment.values()) {
				if (slot == equipment.getSlot()) {
					if (mSelected == equipment) {
						mSelected = null;
					} else if (mOtherEquipment.get(equipment) == null) {
						mSelected = equipment;
					} else {
						mOtherEquipment.remove(equipment);
					}

					generateInventory();
					return;
				}
			}
		} else if (mSelected != null) {
			ItemStack clickedItem = inv.getItem(slot);
			if (clickedItem != null) {
				ItemStack item = new ItemStack(event.getClickedInventory().getItem(slot));
				for (OtherEquipment equipment : OtherEquipment.values()) {
					if (mSelected == equipment && isValid(equipment, item.getType())) {
						mOtherEquipment.put(equipment, item);
						mSelected = null;
						generateInventory();
						return;
					}
				}
			}
		}
	}

	private boolean isValid(OtherEquipment equipment, Material material) {
		if (equipment == OtherEquipment.HEAD) {
			switch (material) {
			case LEATHER_HELMET:
			case GOLDEN_HELMET:
			case CHAINMAIL_HELMET:
			case IRON_HELMET:
			case DIAMOND_HELMET:
			case NETHERITE_HELMET:
			case TURTLE_HELMET:
			case SKELETON_SKULL:
			case WITHER_SKELETON_SKULL:
			case PLAYER_HEAD:
			case ZOMBIE_HEAD:
			case CREEPER_HEAD:
			case DRAGON_HEAD:
				return true;
			default:
				return false;
			}
		} else if (equipment == OtherEquipment.CHEST) {
			switch (material) {
			case LEATHER_CHESTPLATE:
			case GOLDEN_CHESTPLATE:
			case CHAINMAIL_CHESTPLATE:
			case IRON_CHESTPLATE:
			case DIAMOND_CHESTPLATE:
			case NETHERITE_CHESTPLATE:
			case ELYTRA:
				return true;
			default:
				return false;
			}
		} else if (equipment == OtherEquipment.LEGS) {
			switch (material) {
			case LEATHER_LEGGINGS:
			case GOLDEN_LEGGINGS:
			case CHAINMAIL_LEGGINGS:
			case IRON_LEGGINGS:
			case DIAMOND_LEGGINGS:
			case NETHERITE_LEGGINGS:
				return true;
			default:
				return false;
			}
		} else if (equipment == OtherEquipment.FEET) {
			switch (material) {
			case LEATHER_BOOTS:
			case GOLDEN_BOOTS:
			case CHAINMAIL_BOOTS:
			case IRON_BOOTS:
			case DIAMOND_BOOTS:
			case NETHERITE_BOOTS:
				return true;
			default:
				return false;
			}
		}

		return true;
	}

	private double getAttributeAmount(NBTCompoundList mainhandA, NBTCompoundList offhandA, NBTCompoundList headA, NBTCompoundList chestA, NBTCompoundList legsA, NBTCompoundList feetA, AttributeType type, Operation operation) {
		return ItemStatUtils.getAttributeAmount(mainhandA, type, operation, Slot.MAINHAND) + ItemStatUtils.getAttributeAmount(offhandA, type, operation, Slot.OFFHAND)
				+ ItemStatUtils.getAttributeAmount(headA, type, operation, Slot.HEAD) + ItemStatUtils.getAttributeAmount(chestA, type, operation, Slot.CHEST)
				+ ItemStatUtils.getAttributeAmount(legsA, type, operation, Slot.LEGS) + ItemStatUtils.getAttributeAmount(feetA, type, operation, Slot.FEET);
	}

	private ItemStack getCleanItem(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);
		return item;
	}

	private double getHealth(NBTCompoundList ma, NBTCompoundList oa, NBTCompoundList ha, NBTCompoundList ca, NBTCompoundList la, NBTCompoundList fa) {
		return (20 + getAttributeAmount(ma, oa, ha, ca, la, fa, AttributeType.MAX_HEALTH, Operation.ADD))
				* (1 + getAttributeAmount(ma, oa, ha, ca, la, fa, AttributeType.MAX_HEALTH, Operation.MULTIPLY));
	}

	private double getHealing(PlayerItemStats stats, double health) {
		return 20 / health * (1 + 0.1 * (stats.getItemStats().get(EnchantmentType.SUSTENANCE.getItemStat()) - stats.getItemStats().get(EnchantmentType.CURSE_OF_ANEMIA.getItemStat())));
	}

	private EnumMap<DefenseStat, Double> getDefense(PlayerItemStats stats) {
		int armorBonus = 0;
		int agilityBonus = 0;
		for (SecondaryStat stat : SecondaryStat.values()) {
			if (mSecondaryStatEnabled[stat.getSlot()]) {
				if (stat.isArmorModifier()) {
					armorBonus += stats.getItemStats().get(stat.getEnchantmentType().getItemStat());
				} else {
					agilityBonus += stats.getItemStats().get(stat.getEnchantmentType().getItemStat());
				}
			}
		}

		// TODO: remove the magic number 0.2 (preferably link it to some constant declared in the enchantment classes themselves)
		double armor = stats.getItemStats().get(AttributeType.ARMOR.getItemStat()) * (1 + 0.2 * armorBonus);
		double agility = stats.getItemStats().get(AttributeType.AGILITY.getItemStat()) * (1 + 0.2 * agilityBonus);

		double melee = stats.getItemStats().get(EnchantmentType.MELEE_PROTECTION.getItemStat());
		double projectile = stats.getItemStats().get(EnchantmentType.PROJECTILE_PROTECTION.getItemStat());
		double magic = stats.getItemStats().get(EnchantmentType.MAGIC_PROTECTION.getItemStat());
		double blast = stats.getItemStats().get(EnchantmentType.BLAST_PROTECTION.getItemStat());
		double fire = stats.getItemStats().get(EnchantmentType.FIRE_PROTECTION.getItemStat());
		double feather = stats.getItemStats().get(EnchantmentType.FEATHER_FALLING.getItemStat());

		EnumMap<DefenseStat, Double> defense = new EnumMap<>(DefenseStat.class);
		defense.put(DefenseStat.MELEE, DamageUtils.getDamageMultiplier(armor, agility, melee * 2, false));
		defense.put(DefenseStat.PROJECTILE, DamageUtils.getDamageMultiplier(armor, agility, projectile * 2, false));
		defense.put(DefenseStat.MAGIC, DamageUtils.getDamageMultiplier(armor, agility, magic * 2, false));
		defense.put(DefenseStat.BLAST, DamageUtils.getDamageMultiplier(armor, agility, blast * 2, true));
		defense.put(DefenseStat.FIRE, DamageUtils.getDamageMultiplier(armor, agility, fire * 2, true));
		defense.put(DefenseStat.FALL, DamageUtils.getDamageMultiplier(armor, agility, feather * 3, true));

		return defense;
	}

	private void generateInventory() {
		PlayerInventory inventory = mPlayer.getInventory();

		ItemStack currentMainhand = inventory.getItemInMainHand();
		NBTCompoundList ma = null;
		if (currentMainhand != null && currentMainhand.getType() != Material.AIR) {
			ma = ItemStatUtils.getAttributes(new NBTItem(currentMainhand));
		}

		ItemStack currentOffhand = inventory.getItemInOffHand();
		NBTCompoundList oa = null;
		if (currentOffhand != null && currentOffhand.getType() != Material.AIR) {
			oa = ItemStatUtils.getAttributes(new NBTItem(currentOffhand));
		}

		ItemStack currentHead = inventory.getHelmet();
		NBTCompoundList ha = null;
		if (currentHead != null && currentHead.getType() != Material.AIR) {
			ha = ItemStatUtils.getAttributes(new NBTItem(currentHead));
		}

		ItemStack currentChest = inventory.getChestplate();
		NBTCompoundList ca = null;
		if (currentChest != null && currentChest.getType() != Material.AIR) {
			ca = ItemStatUtils.getAttributes(new NBTItem(currentChest));
		}

		ItemStack currentLegs = inventory.getLeggings();
		NBTCompoundList la = null;
		if (currentLegs != null && currentLegs.getType() != Material.AIR) {
			la = ItemStatUtils.getAttributes(new NBTItem(currentLegs));
		}

		ItemStack currentFeet = inventory.getBoots();
		NBTCompoundList fa = null;
		if (currentFeet != null && currentFeet.getType() != Material.AIR) {
			fa = ItemStatUtils.getAttributes(new NBTItem(currentFeet));
		}

		EnumMap<CurrentEquipment, ItemStack> currentItems = new EnumMap<>(CurrentEquipment.class);
		currentItems.put(CurrentEquipment.MAINHAND, currentMainhand);
		currentItems.put(CurrentEquipment.OFFHAND, currentOffhand);
		currentItems.put(CurrentEquipment.HEAD, currentHead);
		currentItems.put(CurrentEquipment.CHEST, currentChest);
		currentItems.put(CurrentEquipment.LEGS, currentLegs);
		currentItems.put(CurrentEquipment.FEET, currentFeet);

		double currentHealth = getHealth(ma, oa, ha, ca, la, fa);
		double currentHealing = getHealing(mCurrentStats, currentHealth);
		EnumMap<DefenseStat, Double> currentDefense = getDefense(mCurrentStats);

		for (int i = 0; i < 54; i++) {
			mInventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
		}

		if (mOtherEquipment.isEmpty()) {
			for (HealthStat stat : HealthStat.values()) {
				ItemStack item = getCleanItem(new ItemStack(stat.getIcon(), 1));
				ItemMeta meta = item.getItemMeta();
				meta.displayName(stat.getDisplay(currentHealing));
				item.setItemMeta(meta);

				mInventory.setItem(stat.getSlot(), item);
			}

			for (DefenseStat stat : DefenseStat.values()) {
				ItemStack item = getCleanItem(new ItemStack(stat.getIcon(), 1));
				ItemMeta meta = item.getItemMeta();
				meta.displayName(stat.getDisplay(currentDefense.get(stat), currentHealth, mMetric));
				item.setItemMeta(meta);

				mInventory.setItem(stat.getSlot(), item);
			}
		} else {
			ItemStack otherMainhand = mOtherEquipment.get(OtherEquipment.MAINHAND);
			if (otherMainhand != null && otherMainhand.getType() != Material.AIR) {
				ma = ItemStatUtils.getAttributes(new NBTItem(otherMainhand));
			} else {
				otherMainhand = currentMainhand;
			}

			ItemStack otherOffhand = mOtherEquipment.get(OtherEquipment.OFFHAND);
			if (otherOffhand != null && otherOffhand.getType() != Material.AIR) {
				oa = ItemStatUtils.getAttributes(new NBTItem(otherOffhand));
			} else {
				otherOffhand = currentOffhand;
			}

			ItemStack otherHead = mOtherEquipment.get(OtherEquipment.HEAD);
			if (otherHead != null && otherHead.getType() != Material.AIR) {
				ha = ItemStatUtils.getAttributes(new NBTItem(otherHead));
			} else {
				otherHead = currentHead;
			}

			ItemStack otherChest = mOtherEquipment.get(OtherEquipment.CHEST);
			if (otherChest != null && otherChest.getType() != Material.AIR) {
				ca = ItemStatUtils.getAttributes(new NBTItem(otherChest));
			} else {
				otherChest = currentChest;
			}

			ItemStack otherLegs = mOtherEquipment.get(OtherEquipment.LEGS);
			if (otherLegs != null && otherLegs.getType() != Material.AIR) {
				la = ItemStatUtils.getAttributes(new NBTItem(otherLegs));
			} else {
				otherLegs = currentLegs;
			}

			ItemStack otherFeet = mOtherEquipment.get(OtherEquipment.FEET);
			if (otherFeet != null && otherFeet.getType() != Material.AIR) {
				fa = ItemStatUtils.getAttributes(new NBTItem(otherFeet));
			} else {
				otherFeet = currentFeet;
			}

			mOtherStats.updateStats(otherMainhand, otherOffhand, otherHead, otherChest, otherLegs, otherFeet, true);

			double otherHealth = getHealth(ma, oa, ha, ca, la, fa);
			double otherHealing = getHealing(mOtherStats, otherHealth);
			EnumMap<DefenseStat, Double> otherDefense = getDefense(mOtherStats);

			for (HealthStat stat : HealthStat.values()) {
				ItemStack item = getCleanItem(new ItemStack(stat.getIcon(), 1));
				ItemMeta meta = item.getItemMeta();
				meta.displayName(stat.getDisplay(currentHealing, otherHealing));
				item.setItemMeta(meta);

				mInventory.setItem(stat.getSlot(), item);
			}

			for (DefenseStat stat : DefenseStat.values()) {
				ItemStack item = getCleanItem(new ItemStack(stat.getIcon(), 1));
				ItemMeta meta = item.getItemMeta();
				meta.displayName(stat.getDisplay(currentDefense.get(stat), currentHealth, otherDefense.get(stat), otherHealth, mMetric));
				item.setItemMeta(meta);

				mInventory.setItem(stat.getSlot(), item);
			}
		}

		for (SecondaryStat stat : SecondaryStat.values()) {
			ItemStack item = getCleanItem(new ItemStack(stat.getIcon(), 1));
			ItemMeta meta = item.getItemMeta();
			if (mSecondaryStatEnabled[stat.getSlot()]) {
				meta.displayName(stat.getDisplay(true));
				meta.addEnchant(org.bukkit.enchantments.Enchantment.ARROW_DAMAGE, 1, true);
			} else {
				meta.displayName(stat.getDisplay(false));
			}

			item.setItemMeta(meta);

			mInventory.setItem(stat.getSlot(), item);
		}

		for (Metric metric : Metric.values()) {
			ItemStack item = getCleanItem(new ItemStack(metric.getIcon(), 1));
			ItemMeta meta = item.getItemMeta();
			meta.displayName(metric.getDisplay(metric == mMetric));
			if (metric == mMetric) {
				meta.addEnchant(org.bukkit.enchantments.Enchantment.ARROW_DAMAGE, 1, true);
			}

			item.setItemMeta(meta);

			mInventory.setItem(metric.getSlot(), item);
		}

		for (CurrentEquipment equipment : CurrentEquipment.values()) {
			ItemStack item = currentItems.get(equipment);
			if (item != null) {
				mInventory.setItem(equipment.getSlot(), new ItemStack(currentItems.get(equipment)));
			} else {
				mInventory.setItem(equipment.getSlot(), null);
			}
		}

		for (OtherEquipment equipment : OtherEquipment.values()) {
			ItemStack item = mOtherEquipment.get(equipment);
			if (item != null) {
				mInventory.setItem(equipment.getSlot(), new ItemStack(item));
			} else {
				item = getCleanItem(new ItemStack(equipment.getIcon(), 1));
				ItemMeta meta = item.getItemMeta();
				meta.displayName(equipment.getDisplay(equipment == mSelected));
				if (equipment == mSelected) {
					meta.addEnchant(org.bukkit.enchantments.Enchantment.ARROW_DAMAGE, 1, true);
				}

				item.setItemMeta(meta);
				item.lore(equipment.getLore());

				mInventory.setItem(equipment.getSlot(), new ItemStack(item));
			}
		}
	}

	public static void registerPlayerItemStatsGUICommand(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.playerstats");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new PlayerArgument("opener"));
		arguments.add(new PlayerArgument("openee"));

		new CommandAPICommand("playerstats").withPermission(perms).withArguments(arguments).executes((sender, args) -> {
			Player opener = (Player) args[0];
			Player openee = (Player) args[1];
			new PlayerItemStatsGUI(openee).openInventory(opener, plugin);
		}).register();

		new CommandAPICommand("playerstats").withPermission(perms).executes((sender, args) -> {
			if (sender instanceof Player player) {
				new PlayerItemStatsGUI(player).openInventory(player, plugin);
			}
		}).register();
	}
}
