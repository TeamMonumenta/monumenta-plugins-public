package com.playmonumenta.plugins.itemstats;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.itemstats.enchantments.Aptitude;
import com.playmonumenta.plugins.itemstats.enchantments.BlastProtection;
import com.playmonumenta.plugins.itemstats.enchantments.FeatherFalling;
import com.playmonumenta.plugins.itemstats.enchantments.FireProtection;
import com.playmonumenta.plugins.itemstats.enchantments.Ineptitude;
import com.playmonumenta.plugins.itemstats.enchantments.LifeDrain;
import com.playmonumenta.plugins.itemstats.enchantments.MagicProtection;
import com.playmonumenta.plugins.itemstats.enchantments.MeleeProtection;
import com.playmonumenta.plugins.itemstats.enchantments.ProjectileProtection;
import com.playmonumenta.plugins.itemstats.enchantments.Protection;
import com.playmonumenta.plugins.itemstats.enchantments.Regeneration;
import com.playmonumenta.plugins.itemstats.enchantments.Sustenance;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Operation;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

public class PlayerItemStatsGUI extends CustomInventory {

	private static class Stats {
		private final PlayerItemStats mPlayerItemStats = new PlayerItemStats();
		private final EnumSet<SecondaryStat> mSecondaryStatEnabled = EnumSet.noneOf(SecondaryStat.class);
		private final EnumMap<Equipment, ItemStack> mEquipment = new EnumMap<>(Equipment.class);
		private final EnumMap<Stat, Double> mStatCache = new EnumMap<>(Stat.class);
		private final @Nullable Stats mMainStats;

		private Stats(@Nullable Stats mainStats) {
			mMainStats = mainStats;
		}

		private double get(AttributeType attr) {
			return mPlayerItemStats.getItemStats().get(attr.getItemStat());
		}

		private double get(AttributeType attr, double defaultValue) {
			return mPlayerItemStats.getItemStats().get(attr.getItemStat(), defaultValue);
		}

		private double get(EnchantmentType ench) {
			return mPlayerItemStats.getItemStats().get(ench.getItemStat());
		}

		private ItemStack getItem(Equipment slot) {
			ItemStack result = mEquipment.get(slot);
			if ((result == null || result.getType() == Material.AIR) && mMainStats != null) {
				return mMainStats.mEquipment.get(slot);
			}
			return result;
		}

		private double getMainhandAttributeAmount(AttributeType type, Operation operation) {
			ItemStack mainhand = getItem(Equipment.MAINHAND);
			return mainhand == null ? 0 : ItemStatUtils.getAttributeAmount(mainhand, type, operation, Slot.MAINHAND);
		}

		private double getAttributeAmount(AttributeType type, Operation operation) {
			double result = 0;
			for (Equipment slot : Equipment.values()) {
				ItemStack item = getItem(slot);
				result += ItemStatUtils.getAttributeAmount(item, type, operation, slot.mSlot);
			}
			return result;
		}

		private double getAttributeAmount(AttributeType type, double base) {
			return (base + getAttributeAmount(type, Operation.ADD)) * (1 + getAttributeAmount(type, Operation.MULTIPLY));
		}

	}

	static double calculateDamageTaken(Stats stats, Protection protection) {
		double armorBonus = 0;
		double agilityBonus = 0;
		for (SecondaryStat stat : SecondaryStat.values()) {
			if (stats.mSecondaryStatEnabled.contains(stat)) {
				if (stat.isArmorModifier()) {
					armorBonus += stats.get(stat.getEnchantmentType());
				} else {
					agilityBonus += stats.get(stat.getEnchantmentType());
				}
			}
		}

		double armor = stats.get(AttributeType.ARMOR);
		double agility = stats.get(AttributeType.AGILITY);
		// TODO: remove the magic number 0.2 (preferably link it to some constant declared in the enchantment classes themselves)
		if (stats.get(EnchantmentType.ADAPTABILITY) > 0) {
			if (armor > agility) {
				armor *= 1 + 0.2 * (armorBonus + agilityBonus);
			} else {
				agility *= 1 + 0.2 * (armorBonus + agilityBonus);
			}
		} else {
			armor *= 1 + 0.2 * armorBonus;
			agility *= 1 + 0.2 * agilityBonus;
		}

		return DamageUtils.getDamageMultiplier(armor, agility, protection.getEPF() * stats.get(protection.getEnchantmentType()), protection.getType().isEnvironmental());
	}

	private static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("0.##");
	private static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("0.##%");
	private static final DecimalFormat PERCENT_CHANGE_FORMATTER = new DecimalFormat("+0.##%;-0.##%");

	private static final DoubleFunction<String> PERCENT = PERCENT_FORMATTER::format;
	private static final DoubleFunction<String> ONE_MINUS_PERCENT = d -> PERCENT_FORMATTER.format(1 - d);
	private static final DoubleFunction<String> NUMBER = NUMBER_FORMATTER::format;
	private static final DoubleFunction<String> DR_CHANGE_FORMAT = d -> PERCENT_CHANGE_FORMATTER.format(d) + " damage taken";

	private enum Stat {

		// health and healing
		HEALTH("Max Health", NUMBER, stats -> stats.getAttributeAmount(AttributeType.MAX_HEALTH, 20)),
		HEALING_RATE("Healing Rate", PERCENT, stats -> Sustenance.getHealingMultiplier(stats.get(EnchantmentType.SUSTENANCE), stats.get(EnchantmentType.CURSE_OF_ANEMIA))),
		EFFECTIVE_HEALING_RATE("Effective Healing Rate", PERCENT, stats -> HEALING_RATE.get(stats) * 20.0 / HEALTH.get(stats)),
		REGENERATION("Regeneration per second", NUMBER, stats -> 4 * Regeneration.healPer5Ticks(stats.get(EnchantmentType.REGENERATION)) * HEALING_RATE.get(stats)),
		EFFECTIVE_REGENERATION("Regeneration in %HP/s", PERCENT, stats -> REGENERATION.get(stats) / HEALTH.get(stats)),
		LIFE_DRAIN("Life Drain on crit", NUMBER, stats -> LifeDrain.LIFE_DRAIN_CRIT_HEAL * Math.sqrt(stats.get(EnchantmentType.LIFE_DRAIN)) * HEALING_RATE.get(stats)),
		EFFECTIVE_LIFE_DRAIN("Life Drain on crit in %HP", PERCENT, stats -> LIFE_DRAIN.get(stats) / HEALTH.get(stats)),

		// These stats are damage taken, but get displayed as damage reduction
		MELEE_DAMAGE_TAKEN("Melee", ONE_MINUS_PERCENT, stats -> calculateDamageTaken(stats, new MeleeProtection()), false, DR_CHANGE_FORMAT),
		PROJECTILE_DAMAGE_TAKEN("Projectile", ONE_MINUS_PERCENT, stats -> calculateDamageTaken(stats, new ProjectileProtection()), false, DR_CHANGE_FORMAT),
		MAGIC_DAMAGE_TAKEN("Magic", ONE_MINUS_PERCENT, stats -> calculateDamageTaken(stats, new MagicProtection()), false, DR_CHANGE_FORMAT),
		BLAST_DAMAGE_TAKEN("Blast", ONE_MINUS_PERCENT, stats -> calculateDamageTaken(stats, new BlastProtection()), false, DR_CHANGE_FORMAT),
		FIRE_DAMAGE_TAKEN("Fire", ONE_MINUS_PERCENT, stats -> calculateDamageTaken(stats, new FireProtection()), false, DR_CHANGE_FORMAT),
		FALL_DAMAGE_TAKEN("Fall", ONE_MINUS_PERCENT, stats -> calculateDamageTaken(stats, new FeatherFalling()), false, DR_CHANGE_FORMAT),

		// These stats are effective damage taken, but get displayed as effective damage reduction
		EFFECTIVE_MELEE_DAMAGE_TAKEN("Melee", ONE_MINUS_PERCENT, stats -> MELEE_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_PROJECTILE_DAMAGE_TAKEN("Projectile", ONE_MINUS_PERCENT, stats -> PROJECTILE_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_MAGIC_DAMAGE_TAKEN("Magic", ONE_MINUS_PERCENT, stats -> MAGIC_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_BLAST_DAMAGE_TAKEN("Blast", ONE_MINUS_PERCENT, stats -> BLAST_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_FIRE_DAMAGE_TAKEN("Fire", ONE_MINUS_PERCENT, stats -> FIRE_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),
		EFFECTIVE_FALL_DAMAGE_TAKEN("Fall", ONE_MINUS_PERCENT, stats -> FALL_DAMAGE_TAKEN.get(stats) * 20.0 / HEALTH.get(stats), false, DR_CHANGE_FORMAT),

		// effective health
		MELEE_EHP("Melee", NUMBER, stats -> HEALTH.get(stats) / MELEE_DAMAGE_TAKEN.get(stats)),
		PROJECTILE_EHP("Projectile", NUMBER, stats -> HEALTH.get(stats) / PROJECTILE_DAMAGE_TAKEN.get(stats)),
		MAGIC_EHP("Magic", NUMBER, stats -> HEALTH.get(stats) / MAGIC_DAMAGE_TAKEN.get(stats)),
		BLAST_EHP("Blast", NUMBER, stats -> HEALTH.get(stats) / BLAST_DAMAGE_TAKEN.get(stats)),
		FIRE_EHP("Fire", NUMBER, stats -> HEALTH.get(stats) / FIRE_DAMAGE_TAKEN.get(stats)),
		FALL_EHP("Fall", NUMBER, stats -> HEALTH.get(stats) / FALL_DAMAGE_TAKEN.get(stats)),

		// effective health multipliers
		// TODO this was suggested, but would need to fit into the GUI somehow
		// Maybe make it so clicking one of the other stats (e.g. damage reduction) switches its display to these?
		MELEE_EHM("Melee", NUMBER, stats -> 1.0 / MELEE_DAMAGE_TAKEN.get(stats)),
		PROJECTILE_EHM("Projectile", NUMBER, stats -> 1.0 / PROJECTILE_DAMAGE_TAKEN.get(stats)),
		MAGIC_EHM("Magic", NUMBER, stats -> 1.0 / MAGIC_DAMAGE_TAKEN.get(stats)),
		BLAST_EHM("Blast", NUMBER, stats -> 1.0 / BLAST_DAMAGE_TAKEN.get(stats)),
		FIRE_EHM("Fire", NUMBER, stats -> 1.0 / FIRE_DAMAGE_TAKEN.get(stats)),
		FALL_EHM("Fall", NUMBER, stats -> 1.0 / FALL_DAMAGE_TAKEN.get(stats)),

		// melee
		ATTACK_DAMAGE("Attack Damage", NUMBER, stats -> (1 + stats.get(AttributeType.ATTACK_DAMAGE_ADD)) * stats.get(AttributeType.ATTACK_DAMAGE_MULTIPLY, 1)),
		ATTACK_DAMAGE_ADD("+flat Attack Damage", NUMBER, stats -> stats.get(AttributeType.ATTACK_DAMAGE_ADD) - stats.getMainhandAttributeAmount(AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD)),
		ATTACK_DAMAGE_MULTIPLY("+% Attack Damage", PERCENT, stats -> stats.get(AttributeType.ATTACK_DAMAGE_MULTIPLY, 1) - 1),
		ATTACK_SPEED("Attack Speed", NUMBER, stats -> stats.getAttributeAmount(AttributeType.ATTACK_SPEED, 4)),

		// projectile
		PROJECTILE_DAMAGE("Projectile Damage", NUMBER, stats -> stats.get(AttributeType.PROJECTILE_DAMAGE_ADD) * stats.get(AttributeType.PROJECTILE_DAMAGE_MULTIPLY, 1)),
		PROJECTILE_DAMAGE_ADD("+flat Projectile Damage", NUMBER, stats -> stats.get(AttributeType.PROJECTILE_DAMAGE_ADD) - stats.getMainhandAttributeAmount(AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD)),
		PROJECTILE_MULTIPLY("+% Projectile Damage", PERCENT, stats -> stats.get(AttributeType.PROJECTILE_DAMAGE_MULTIPLY, 1) - 1),
		PROJECTILE_SPEED("Projectile Speed", NUMBER, stats -> {
			ItemStack mainhand = stats.getItem(Equipment.MAINHAND);
			if (ItemUtils.isBowOrTrident(mainhand)) {
				// bows, crossbows, and tridents have the final projectile speed lowered by one in ProjectileSpeed.onLaunchProjectile
				return stats.get(AttributeType.PROJECTILE_SPEED, 1) - 1;
			} else {
				return stats.get(AttributeType.PROJECTILE_SPEED, 0);
			}
		}),
		PROJECTILE_RATE("Shoot/Throw Rate", NUMBER, stats -> {
			double throwRate = stats.get(AttributeType.THROW_RATE, 0);
			if (throwRate != 0) {
				return throwRate;
			}
			ItemStack mainhand = stats.getItem(Equipment.MAINHAND);
			if (mainhand == null) {
				return 0;
			}
			if (mainhand.getType() == Material.BOW) {
				return 1;
			} else if (mainhand.getType() == Material.CROSSBOW) {
				return Math.max(0, 1.25 - mainhand.getEnchantmentLevel(Enchantment.QUICK_CHARGE) * 0.25);
			}
			return 0;
		}),

		// magic
		SPELL_POWER("Spell Power", PERCENT, stats -> stats.get(AttributeType.SPELL_DAMAGE)),
		MAGIC_DAMAGE_ADD("+flat Magic Damage", NUMBER, stats -> stats.get(AttributeType.MAGIC_DAMAGE_ADD)),
		MAGIC_DAMAGE_MULTIPLY("+% Magic Damage", PERCENT, stats -> stats.get(AttributeType.MAGIC_DAMAGE_MULTIPLY, 1) - 1),
		COOLDOWN_MULTIPLIER("Cooldown Multiplier", PERCENT, stats -> (1 + Aptitude.getCooldownPercentage(stats.get(EnchantmentType.APTITUDE)))
			                                                             * (1 + Ineptitude.getCooldownPercentage(stats.get(EnchantmentType.INEPTITUDE))), false),
		// misc
		ARMOR("Total Armor", NUMBER, stats -> stats.get(AttributeType.ARMOR)),
		AGILITY("Total Agility", NUMBER, stats -> stats.get(AttributeType.AGILITY)),
		MOVEMENT_SPEED("Movement Speed", PERCENT, stats -> stats.getAttributeAmount(AttributeType.SPEED, 0.1) / 0.1),
		KNOCKBACK_RESISTANCE("Knockback Resistance", PERCENT, stats -> stats.getAttributeAmount(AttributeType.KNOCKBACK_RESISTANCE, 0)),
		THORNS_DAMAGE("Thorns Damage", NUMBER, stats -> stats.get(AttributeType.THORNS)),

		;

		private final String mName;
		private final DoubleFunction<String> mFormat;
		private final ToDoubleFunction<Stats> mStatFunc;
		private final boolean mLargerIsBetter;
		private final DoubleFunction<String> mChangeFormat;

		Stat(String name, DoubleFunction<String> format, ToDoubleFunction<Stats> statFunc) {
			this(name, format, statFunc, true);
		}

		Stat(String name, DoubleFunction<String> format, ToDoubleFunction<Stats> statFunc, boolean largerIsBetter) {
			this(name, format, statFunc, largerIsBetter, PERCENT_CHANGE_FORMATTER::format);
		}

		Stat(String name, DoubleFunction<String> format, ToDoubleFunction<Stats> statFunc, boolean largerIsBetter, DoubleFunction<String> changeFormat) {
			mName = name;
			mFormat = format;
			mStatFunc = statFunc;
			mLargerIsBetter = largerIsBetter;
			mChangeFormat = changeFormat;
		}

		public double get(Stats stats) {
			return stats.mStatCache.computeIfAbsent(this, k -> mStatFunc.applyAsDouble(stats));
		}

		public Component getDisplay(Stats stats, @Nullable Stats otherStats) {
			double value = get(stats);
			Component comp = Component.text(mName + ": ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
				.append(Component.text(mFormat.apply(value), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			if (otherStats != null) {
				double otherValue = get(otherStats);
				NamedTextColor color;
				if (otherValue > value) {
					color = mLargerIsBetter ? NamedTextColor.GREEN : NamedTextColor.RED;
				} else if (otherValue < value) {
					color = mLargerIsBetter ? NamedTextColor.RED : NamedTextColor.GREEN;
				} else {
					color = NamedTextColor.WHITE;
				}
				comp = comp.append(Component.text(String.format(" \u2192 %s", mFormat.apply(otherValue)), color).decoration(TextDecoration.ITALIC, false));
				double relativeValue = value == otherValue ? 0 : otherValue / value - 1;
				if (!Double.isInfinite(relativeValue)) {
					comp = comp.append(Component.text(String.format(" (%s)", mChangeFormat.apply(relativeValue)), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
				}
			}
			return comp;
		}
	}

	private static class StatItem {
		private final int mSlot;
		private final Material mIcon;
		private final Component mName;
		private final List<Stat> mDisplayedStats;

		private StatItem(int mSlot, Material mIcon, Component mName, Stat... displayedStats) {
			this.mSlot = mSlot;
			this.mIcon = mIcon;
			this.mName = mName;
			this.mDisplayedStats = List.of(displayedStats);
		}

		public ItemStack getDisplay(Stats stats, @Nullable Stats otherStats) {
			ItemStack icon = getCleanItem(new ItemStack(mIcon, 1));
			ItemMeta meta = icon.getItemMeta();
			meta.displayName(mName);
			meta.lore(mDisplayedStats.stream().map(stat -> stat.getDisplay(stats, otherStats)).toList());
			icon.setItemMeta(meta);
			return icon;
		}
	}

	private static final StatItem HEALTH_INFO = new StatItem(13, Material.APPLE,
		Component.text("Health and Healing", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.HEALTH, Stat.HEALING_RATE, Stat.EFFECTIVE_HEALING_RATE, Stat.REGENERATION, Stat.EFFECTIVE_REGENERATION, Stat.LIFE_DRAIN, Stat.EFFECTIVE_LIFE_DRAIN);

	private static final StatItem HEALTH_NORMALIZED_DAMAGE_RESISTANCE = new StatItem(21, Material.GLOWSTONE_DUST,
		Component.text("Health Normalized Damage Reduction", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.EFFECTIVE_MELEE_DAMAGE_TAKEN, Stat.EFFECTIVE_PROJECTILE_DAMAGE_TAKEN, Stat.EFFECTIVE_MAGIC_DAMAGE_TAKEN, Stat.EFFECTIVE_BLAST_DAMAGE_TAKEN, Stat.EFFECTIVE_FIRE_DAMAGE_TAKEN, Stat.EFFECTIVE_FALL_DAMAGE_TAKEN);

	private static final StatItem DAMAGE_RESISTANCE = new StatItem(22, Material.SUGAR,
		Component.text("Damage Reduction", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.MELEE_DAMAGE_TAKEN, Stat.PROJECTILE_DAMAGE_TAKEN, Stat.MAGIC_DAMAGE_TAKEN, Stat.BLAST_DAMAGE_TAKEN, Stat.FIRE_DAMAGE_TAKEN, Stat.FALL_DAMAGE_TAKEN);

	private static final StatItem EFFECTIVE_HEALTH = new StatItem(23, Material.REDSTONE,
		Component.text("Effective Health", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.MELEE_EHP, Stat.PROJECTILE_EHP, Stat.MAGIC_EHP, Stat.BLAST_EHP, Stat.FIRE_EHP, Stat.FALL_EHP);

	private static final StatItem MELEE_INFO = new StatItem(30, Material.IRON_SWORD,
		Component.text("Melee", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.ATTACK_DAMAGE, Stat.ATTACK_DAMAGE_ADD, Stat.ATTACK_DAMAGE_MULTIPLY, Stat.ATTACK_SPEED);

	private static final StatItem PROJECTILE_INFO = new StatItem(31, Material.BOW,
		Component.text("Projectile", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.PROJECTILE_DAMAGE, Stat.PROJECTILE_DAMAGE_ADD, Stat.PROJECTILE_MULTIPLY, Stat.PROJECTILE_SPEED, Stat.PROJECTILE_RATE);

	private static final StatItem MAGIC_INFO = new StatItem(32, Material.BLAZE_ROD,
		Component.text("Magic", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.SPELL_POWER, Stat.MAGIC_DAMAGE_ADD, Stat.MAGIC_DAMAGE_MULTIPLY, Stat.COOLDOWN_MULTIPLIER);

	private static final StatItem MISC_INFO = new StatItem(40, Material.LEATHER_BOOTS,
		Component.text("Misc", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false),
		Stat.ARMOR, Stat.AGILITY, Stat.MOVEMENT_SPEED, Stat.KNOCKBACK_RESISTANCE, Stat.THORNS_DAMAGE);

	private static final StatItem[] STAT_ITEMS = {
		HEALTH_INFO,
		DAMAGE_RESISTANCE, HEALTH_NORMALIZED_DAMAGE_RESISTANCE, EFFECTIVE_HEALTH,
		MELEE_INFO, PROJECTILE_INFO, MAGIC_INFO,
		MISC_INFO
	};

	private enum SecondaryStat {
		SHIELDING(0, Material.NAUTILUS_SHELL, EnchantmentType.SHIELDING, true),
		POISE(1, Material.LILY_OF_THE_VALLEY, EnchantmentType.POISE, true),
		INURE(2, Material.NETHERITE_SCRAP, EnchantmentType.INURE, true),
		STEADFAST(3, Material.LEAD, EnchantmentType.STEADFAST, true),
		ETHEREAL(5, Material.PHANTOM_MEMBRANE, EnchantmentType.ETHEREAL, false),
		REFLEXES(6, Material.ENDER_EYE, EnchantmentType.REFLEXES, false),
		EVASION(7, Material.ELYTRA, EnchantmentType.EVASION, false),
		TEMPO(8, Material.CLOCK, EnchantmentType.TEMPO, false);
		// TODO second wind?

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

	private enum Equipment {
		MAINHAND(46, 52, 0, "Main Hand", EquipmentSlot.HAND, Slot.MAINHAND),
		OFFHAND(19, 25, 40, "Off Hand", EquipmentSlot.OFF_HAND, Slot.OFFHAND),
		HEAD(18, 26, 39, "Head", EquipmentSlot.HEAD, Slot.HEAD),
		CHEST(27, 35, 38, "Chest", EquipmentSlot.CHEST, Slot.CHEST),
		LEGS(36, 44, 37, "Legs", EquipmentSlot.LEGS, Slot.LEGS),
		FEET(45, 53, 36, "Feet", EquipmentSlot.FEET, Slot.FEET);

		private final int mLeftSlot;
		private final int mRightSlot;
		private final int mPlayerInventorySlot;
		private final String mName;
		private final EquipmentSlot mEquipmentSlot;
		private final Slot mSlot;
		private final ImmutableList<Component> mLore = ImmutableList.of(
			Component.text("Click here, then click an item to compare builds.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
			Component.text("Right click to restore the initial item.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

		Equipment(int leftSlot, int rightSlot, int playerInventorySlot, String name, EquipmentSlot equipmentSlot, Slot slot) {
			mLeftSlot = leftSlot;
			mRightSlot = rightSlot;
			mPlayerInventorySlot = playerInventorySlot;
			mName = name;
			mEquipmentSlot = equipmentSlot;
			mSlot = slot;
		}

		public Material getIcon() {
			return Material.ITEM_FRAME;
		}

		public Component getDisplay(boolean selected) {
			return Component.text(String.format("%s Slot%s", mName, selected ? " (Selected)" : ""), selected ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		}

		public ImmutableList<Component> getLore() {
			return mLore;
		}
	}

	private static final int SWAP_EQUIPMENT_SET_SLOT = 49;

	private final Stats mLeftStats = new Stats(null);
	private final Stats mRightStats = new Stats(mLeftStats);

	private final EnumMap<Equipment, ItemStack> mOriginalEquipmentLeft = new EnumMap<>(Equipment.class);
	private final EnumMap<Equipment, ItemStack> mOriginalEquipmentRight = new EnumMap<>(Equipment.class);

	private boolean mSelectedRightEquipmentSet;
	private @Nullable Equipment mSelectedEquipmentsSlot = null;

	public PlayerItemStatsGUI(Player player) {
		super(player, 54, Component.text("Player Stats Calculator", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		setEquipmentFromPlayer(false, player);
		generateInventory();
	}

	public PlayerItemStatsGUI(Player player, Player otherPlayer) {
		super(player, 54, Component.text("Player Stats Calculator", NamedTextColor.GOLD).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		setEquipmentFromPlayer(false, player);
		setEquipmentFromPlayer(true, otherPlayer);
		generateInventory();
	}

	private void setEquipmentFromPlayer(boolean right, Player player) {
		PlayerInventory inventory = player.getInventory();
		Stats stats = right ? mRightStats : mLeftStats;
		EnumMap<Equipment, ItemStack> originalEquipment = right ? mOriginalEquipmentRight : mOriginalEquipmentLeft;

		stats.mEquipment.put(Equipment.MAINHAND, inventory.getItemInMainHand());
		stats.mEquipment.put(Equipment.OFFHAND, inventory.getItemInOffHand());
		stats.mEquipment.put(Equipment.HEAD, inventory.getHelmet());
		stats.mEquipment.put(Equipment.CHEST, inventory.getChestplate());
		stats.mEquipment.put(Equipment.LEGS, inventory.getLeggings());
		stats.mEquipment.put(Equipment.FEET, inventory.getBoots());
		stats.mEquipment.values().removeIf(item -> item == null || item.getType() == Material.AIR);

		originalEquipment.clear();
		originalEquipment.putAll(stats.mEquipment);
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
			if (slot == SWAP_EQUIPMENT_SET_SLOT) {
				EnumMap<Equipment, ItemStack> leftEquipment = new EnumMap<>(mLeftStats.mEquipment);
				mLeftStats.mEquipment.clear();
				mLeftStats.mEquipment.putAll(mRightStats.mEquipment);
				mRightStats.mEquipment.clear();
				mRightStats.mEquipment.putAll(leftEquipment);
				generateInventory();
				return;
			}

			for (SecondaryStat stat : SecondaryStat.values()) {
				if (slot == stat.getSlot()) {
					if (!mLeftStats.mSecondaryStatEnabled.remove(stat)) {
						mLeftStats.mSecondaryStatEnabled.add(stat);
					}
					mRightStats.mSecondaryStatEnabled.clear();
					mRightStats.mSecondaryStatEnabled.addAll(mLeftStats.mSecondaryStatEnabled);
					generateInventory();
					return;
				}
			}

			for (Equipment equipment : Equipment.values()) {
				if (slot == equipment.mRightSlot) {
					if (event.isRightClick()) {
						mRightStats.mEquipment.put(equipment, mOriginalEquipmentRight.get(equipment));
					} else {
						if (mSelectedEquipmentsSlot == equipment && mSelectedRightEquipmentSet) {
							mSelectedEquipmentsSlot = null;
						} else if (mRightStats.mEquipment.get(equipment) == null) {
							mSelectedEquipmentsSlot = equipment;
							mSelectedRightEquipmentSet = true;
						} else {
							mRightStats.mEquipment.remove(equipment);
						}
					}

					generateInventory();
					return;
				}
				if (slot == equipment.mLeftSlot) {
					if (event.isRightClick()) {
						mLeftStats.mEquipment.put(equipment, mOriginalEquipmentLeft.get(equipment));
					} else {
						if (mSelectedEquipmentsSlot == equipment && !mSelectedRightEquipmentSet) {
							mSelectedEquipmentsSlot = null;
						} else if (mLeftStats.mEquipment.get(equipment) == null) {
							mSelectedEquipmentsSlot = equipment;
							mSelectedRightEquipmentSet = false;
						} else {
							mLeftStats.mEquipment.remove(equipment);
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
					Stats stats;
					if (mSelectedEquipmentsSlot != null) {
						stats = mSelectedRightEquipmentSet ? mRightStats : mLeftStats;
					} else if (event.getClick().isShiftClick()) {
						stats = mRightStats;
					} else {
						return;
					}
					mSelectedEquipmentsSlot = null;

					for (Equipment equipment : Equipment.values()) {
						Integer shulkerSlot = ShulkerEquipmentListener.getShulkerSlot(equipment.mPlayerInventorySlot);
						if (shulkerSlot == null) {
							continue;
						}
						ItemStack shulkerItem = shulker.getInventory().getItem(shulkerSlot);
						if (shulkerItem != null && shulkerItem.getType() != Material.AIR) {
							stats.mEquipment.put(equipment, shulkerItem);
						}
					}
					generateInventory();
					return;
				}
				Equipment targetSlot = null;
				boolean targetRightSet = false;
				if (mSelectedEquipmentsSlot != null) {
					if (isValid(mSelectedEquipmentsSlot, item.getType())) {
						targetSlot = mSelectedEquipmentsSlot;
						targetRightSet = mSelectedRightEquipmentSet;
					}
				} else if (event.getClick().isShiftClick()) {
					targetRightSet = true;
					for (Equipment equipment : new Equipment[] {Equipment.HEAD, Equipment.CHEST, Equipment.LEGS, Equipment.FEET}) {
						if (isValid(equipment, item.getType())) {
							targetSlot = equipment;
							break;
						}
					}
					if (targetSlot == null) {
						if (ItemStatUtils.hasAttributeInSlot(item, Slot.OFFHAND)) {
							targetSlot = Equipment.OFFHAND;
						} else {
							targetSlot = Equipment.MAINHAND;
						}
					}
				}
				if (targetSlot != null) {
					(targetRightSet ? mRightStats : mLeftStats).mEquipment.put(targetSlot, item);
					mSelectedEquipmentsSlot = null;
					generateInventory();
				}
			}
		}
	}

	private boolean isValid(Equipment equipment, Material material) {
		return equipment == Equipment.MAINHAND
			|| equipment == Equipment.OFFHAND
			|| equipment.mEquipmentSlot == material.getEquipmentSlot();
	}

	private static ItemStack getCleanItem(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		item.setItemMeta(meta);
		return item;
	}

	private static ItemStack makePlaceholderItem(Equipment equipment, boolean selected) {
		ItemStack empty = getCleanItem(new ItemStack(equipment.getIcon(), 1));
		ItemMeta meta = empty.getItemMeta();
		meta.displayName(equipment.getDisplay(selected));
		if (selected) {
			meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
		}
		empty.setItemMeta(meta);
		empty.lore(equipment.getLore());
		return empty;
	}

	private static @Nullable ItemStack getWarningIcon(Stats stats) {
		List<Component> warnings = new ArrayList<>();
		if (stats.get(EnchantmentType.CURSE_OF_CORRUPTION) > 1) {
			warnings.add(Component.text("Build has more than one Curse of Corruption item.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		if (stats.get(EnchantmentType.TWO_HANDED) > 0
			&& stats.getItem(Equipment.OFFHAND) != null
			&& ItemStatUtils.getEnchantmentLevel(stats.getItem(Equipment.OFFHAND), EnchantmentType.WEIGHTLESS) == 0) {
			warnings.add(Component.text("Build has a Two Handed item, but not a Weightless offhand.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		if (warnings.isEmpty()) {
			return null;
		}
		ItemStack item = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Warning!", NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		meta.lore(warnings);
		item.setItemMeta(meta);
		return item;
	}

	private void generateInventory() {

		for (Stats stats : new Stats[] {mLeftStats, mRightStats}) {
			stats.mStatCache.clear();
			stats.mPlayerItemStats.updateStats(stats.getItem(Equipment.MAINHAND), stats.getItem(Equipment.OFFHAND),
				stats.getItem(Equipment.HEAD), stats.getItem(Equipment.CHEST), stats.getItem(Equipment.LEGS), stats.getItem(Equipment.FEET), true);
		}

		for (int i = 0; i < 54; i++) {
			mInventory.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1));
		}

		ItemStack swapItem = new ItemStack(Material.ARMOR_STAND, 1);
		ItemMeta swapItemMeta = swapItem.getItemMeta();
		swapItemMeta.displayName(Component.text("Swap Equipment Sets", NamedTextColor.WHITE).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		swapItem.setItemMeta(swapItemMeta);
		mInventory.setItem(SWAP_EQUIPMENT_SET_SLOT, swapItem);

		for (StatItem statItem : STAT_ITEMS) {
			mInventory.setItem(statItem.mSlot, statItem.getDisplay(mLeftStats, mRightStats.mEquipment.isEmpty() ? null : mRightStats));
		}

		for (SecondaryStat stat : SecondaryStat.values()) {
			ItemStack item = getCleanItem(new ItemStack(stat.getIcon(), 1));
			ItemMeta meta = item.getItemMeta();
			if (mLeftStats.mSecondaryStatEnabled.contains(stat)) {
				meta.displayName(stat.getDisplay(true));
				meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
			} else {
				meta.displayName(stat.getDisplay(false));
			}

			item.setItemMeta(meta);

			mInventory.setItem(stat.getSlot(), item);
		}

		for (Equipment equipment : Equipment.values()) {
			ItemStack leftItem = mLeftStats.mEquipment.get(equipment);
			mInventory.setItem(equipment.mLeftSlot, leftItem != null ? new ItemStack(leftItem) : makePlaceholderItem(equipment, equipment == mSelectedEquipmentsSlot && !mSelectedRightEquipmentSet));

			ItemStack rightItem = mRightStats.mEquipment.get(equipment);
			mInventory.setItem(equipment.mRightSlot, rightItem != null ? new ItemStack(rightItem) : makePlaceholderItem(equipment, equipment == mSelectedEquipmentsSlot && mSelectedRightEquipmentSet));
		}

		ItemStack leftWarningIcon = getWarningIcon(mLeftStats);
		if (leftWarningIcon != null) {
			mInventory.setItem(28, leftWarningIcon);
		}
		ItemStack rightWarningIcon = getWarningIcon(mRightStats);
		if (rightWarningIcon != null) {
			mInventory.setItem(34, rightWarningIcon);
		}

		for (ItemStack item : mInventory.getContents()) {
			if (item != null) {
				ItemUtils.setPlainName(item);
			}
		}
	}
}
