package com.playmonumenta.plugins.itemstats.enums;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.attributes.Agility;
import com.playmonumenta.plugins.itemstats.attributes.Armor;
import com.playmonumenta.plugins.itemstats.attributes.AttackDamageAdd;
import com.playmonumenta.plugins.itemstats.attributes.AttackDamageMultiply;
import com.playmonumenta.plugins.itemstats.attributes.MagicDamageAdd;
import com.playmonumenta.plugins.itemstats.attributes.MagicDamageMultiply;
import com.playmonumenta.plugins.itemstats.attributes.PotionDamage;
import com.playmonumenta.plugins.itemstats.attributes.PotionRadius;
import com.playmonumenta.plugins.itemstats.attributes.ProjectileDamageAdd;
import com.playmonumenta.plugins.itemstats.attributes.ProjectileDamageMultiply;
import com.playmonumenta.plugins.itemstats.attributes.ProjectileSpeed;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.itemstats.attributes.ThornsDamage;
import com.playmonumenta.plugins.itemstats.attributes.ThrowRate;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.Nullable;

public enum AttributeType {
	ARMOR(new Armor(), false, true),
	AGILITY(new Agility(), false, true),
	MAX_HEALTH(Attribute.GENERIC_MAX_HEALTH, "Max Health", false, false),
	ATTACK_DAMAGE_ADD(new AttackDamageAdd(), true, false),
	ATTACK_DAMAGE_MULTIPLY(new AttackDamageMultiply(), true, false),
	ATTACK_SPEED(Attribute.GENERIC_ATTACK_SPEED, "Attack Speed", false, false),
	PROJECTILE_DAMAGE_ADD(new ProjectileDamageAdd(), true, false),
	PROJECTILE_DAMAGE_MULTIPLY(new ProjectileDamageMultiply(), true, false),
	PROJECTILE_SPEED(new ProjectileSpeed(), true, false),
	THROW_RATE(new ThrowRate(), false, false),
	SPELL_DAMAGE(new SpellPower(), false, false),
	MAGIC_DAMAGE_ADD(new MagicDamageAdd(), true, false),
	MAGIC_DAMAGE_MULTIPLY(new MagicDamageMultiply(), true, false),
	SPEED(Attribute.GENERIC_MOVEMENT_SPEED, "Speed", false, false),
	KNOCKBACK_RESISTANCE(Attribute.GENERIC_KNOCKBACK_RESISTANCE, "Knockback Resistance", false, false),
	POTION_DAMAGE(new PotionDamage(), true, false),
	POTION_RADIUS(new PotionRadius(), true, false),
	THORNS(new ThornsDamage(), true, true);

	static final Map<String, AttributeType> REVERSE_MAPPINGS = Arrays.stream(AttributeType.values())
		.collect(Collectors.toUnmodifiableMap(AttributeType::getCodeName, type -> type));

	public static final ImmutableList<AttributeType> MAINHAND_ATTRIBUTE_TYPES = ImmutableList.of(
		ATTACK_DAMAGE_ADD,
		ATTACK_SPEED,
		PROJECTILE_DAMAGE_ADD,
		PROJECTILE_SPEED,
		THROW_RATE,
		POTION_DAMAGE,
		POTION_RADIUS
	);

	public static final ImmutableList<AttributeType> PROJECTILE_ATTRIBUTE_TYPES = ImmutableList.of(
		PROJECTILE_DAMAGE_MULTIPLY,
		PROJECTILE_SPEED
	);

	public static final String KEY = "Attributes";

	final @Nullable Attribute mAttribute;
	final @Nullable ItemStat mItemStat;
	final String mName;
	final String mCodeName;
	final boolean mIsRegionScaled;
	final boolean mIsMainhandRegionScaled;

	// custom attribute
	AttributeType(ItemStat itemStat, boolean isRegionScaled, boolean isMainhandRegionScaled) {
		this(itemStat, null, itemStat.getName(), isRegionScaled, isMainhandRegionScaled);
	}

	// vanilla attribute
	AttributeType(Attribute attribute, String name, boolean isRegionScaled, boolean isMainhandRegionScaled) {
		this(null, attribute, name, isRegionScaled, isMainhandRegionScaled);
	}

	AttributeType(@Nullable ItemStat itemStat, @Nullable Attribute attribute, String name, boolean isRegionScaled, boolean isMainhandRegionScaled) {
		mAttribute = attribute;
		mItemStat = itemStat;
		mName = name;
		mCodeName = name.replace(" ", "");
		mIsRegionScaled = isRegionScaled;
		mIsMainhandRegionScaled = isMainhandRegionScaled;
	}

	public @Nullable Attribute getAttribute() {
		return mAttribute;
	}

	public @Nullable ItemStat getItemStat() {
		return mItemStat;
	}

	public String getName() {
		return mName;
	}

	public String getCodeName() {
		return mCodeName;
	}

	public boolean isRegionScaled() {
		return mIsRegionScaled;
	}

	public boolean isMainhandRegionScaled() {
		return mIsMainhandRegionScaled;
	}

	public static Component getDisplay(AttributeType attribute, double amount, Slot slot, Operation operation) {
		String name = attribute.getName();
		if (slot == Slot.MAINHAND && operation == Operation.ADD) {
			if (attribute == ATTACK_DAMAGE_ADD) {
				return Component.text(String.format(" %s %s", ItemStatUtils.NUMBER_FORMATTER.format(amount + 1), name.replace(" Add", "")), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
			} else if (attribute == ATTACK_SPEED) {
				return Component.text(String.format(" %s %s", ItemStatUtils.NUMBER_FORMATTER.format(amount + 4), name), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
			} else if (attribute == PROJECTILE_SPEED || attribute == THROW_RATE) {
				return Component.text(String.format(" %s %s", ItemStatUtils.NUMBER_FORMATTER.format(amount), name), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
			} else if (PROJECTILE_DAMAGE_ADD.getName().equals(name)) {
				return Component.text(String.format(" %s %s", ItemStatUtils.NUMBER_FORMATTER.format(amount), name.replace(" Add", "")), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
			} else if (attribute == POTION_DAMAGE || attribute == POTION_RADIUS) {
				return Component.text(String.format(" %s %s", ItemStatUtils.NUMBER_FORMATTER.format(amount), name), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
			}
		} else if (slot == Slot.MAINHAND && attribute == PROJECTILE_SPEED) {
			// Hack for mainhand items using projectile speed multiply instead of add
			return Component.text(String.format(" %s %s", ItemStatUtils.NUMBER_FORMATTER.format(amount), name), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
		}

		if (attribute == ARMOR || attribute == AGILITY) {
			if (operation == Operation.ADD) {
				return Component.text(String.format("%s %s", ItemStatUtils.NUMBER_CHANGE_FORMATTER.format(amount), name), amount > 0 ? TextColor.fromHexString("#33CCFF") : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
			} else {
				return Component.text(String.format("%s %s", ItemStatUtils.PERCENT_CHANGE_FORMATTER.format(amount), name), amount > 0 ? TextColor.fromHexString("#33CCFF") : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
			}
		} else {
			if (operation == Operation.ADD && attribute == KNOCKBACK_RESISTANCE) {
				return Component.text(String.format("%s %s", ItemStatUtils.NUMBER_CHANGE_FORMATTER.format(amount * 10), name.replace(" Add", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
			} else if (operation == Operation.ADD) {
				return Component.text(String.format("%s %s", ItemStatUtils.NUMBER_CHANGE_FORMATTER.format(amount), name.replace(" Add", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
			} else if (attribute == PROJECTILE_SPEED) {
				return Component.text(String.format("%s %s", ItemStatUtils.PERCENT_CHANGE_FORMATTER.format(amount), name.replace(" Multiply", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
			} else {
				return Component.text(String.format("%s %s", ItemStatUtils.PERCENT_CHANGE_FORMATTER.format(amount), name.replace(" Multiply", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
			}
		}
	}

	public static @Nullable AttributeType getAttributeType(String name) {
		return REVERSE_MAPPINGS.get(name.replace(" ", ""));
	}
}
