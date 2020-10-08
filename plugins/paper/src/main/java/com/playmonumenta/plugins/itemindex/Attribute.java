package com.playmonumenta.plugins.itemindex;

import java.util.ArrayList;

import com.playmonumenta.plugins.enchantments.AttributeProjectileDamage;
import com.playmonumenta.plugins.enchantments.AttributeProjectileSpeed;
import com.playmonumenta.plugins.enchantments.AttributeThornsDamage;
import com.playmonumenta.plugins.enchantments.AttributeThrowRate;
import com.playmonumenta.plugins.enchantments.BaseAttribute;

public enum Attribute {

	//attributes are showing up in the order they are declared
	//so... main stats first
	ATTACK_DAMAGE(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE, " Attack Damage"),
	ATTACK_SPEED(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED,  " Attack Speed"),
	RANGED_DAMAGE(new AttributeProjectileDamage()),
	PROJECTILE_SPEED(new AttributeProjectileSpeed()),
	THROW_RATE(new AttributeThrowRate()),
	THORNS_DAMAGE(new AttributeThornsDamage()),

	//bukkit attributes
	MAX_HEALTH(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH, " Max Health"),
	FOLLOW_RANGE(org.bukkit.attribute.Attribute.GENERIC_FOLLOW_RANGE, " Follow Range"),
	KNOCKBACK_RESISTANCE(org.bukkit.attribute.Attribute.GENERIC_KNOCKBACK_RESISTANCE, " Knockback Resistance"),
	MOVEMENT_SPEED(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED, " Speed"),
	FLYING_SPEED(org.bukkit.attribute.Attribute.GENERIC_FLYING_SPEED, " Flying Speed"),
	ARMOR(org.bukkit.attribute.Attribute.GENERIC_ARMOR, " Armor"),
	TOUGHNESS(org.bukkit.attribute.Attribute.GENERIC_ARMOR_TOUGHNESS, " Armor Toughness"),
	LUCK(org.bukkit.attribute.Attribute.GENERIC_LUCK, " Luck"),
	HORSE_JUMP_STRENGTH(org.bukkit.attribute.Attribute.HORSE_JUMP_STRENGTH, " Horse Jump Strength"),
	ZOMBIE_SPAWN_REINFORCEMENTS(org.bukkit.attribute.Attribute.ZOMBIE_SPAWN_REINFORCEMENTS, " Zombie Spawn Reinforcements"),

	//custom attributes
	;

	private org.bukkit.attribute.Attribute mBukkitAttribute;
	BaseAttribute mCustomAttributeClass;
	String mReadableStringFormat;

	Attribute(org.bukkit.attribute.Attribute bukkitAttribute, String s) {
		this.mBukkitAttribute = bukkitAttribute;
		this.mCustomAttributeClass = null;
		this.mReadableStringFormat = s;
	}

	Attribute(BaseAttribute customAttribute) {
		this.mBukkitAttribute = null;
		this.mCustomAttributeClass = customAttribute;
		this.mReadableStringFormat = customAttribute.getProperty();
	}

	public boolean isCustom() {
		return this.mCustomAttributeClass != null;
	}

	public String getReadableStringFormat() {
		return this.mReadableStringFormat;
	}

	public org.bukkit.attribute.Attribute getBukkitAttribute() {
		return this.mBukkitAttribute;
	}

	public static String[] valuesAsStringArray() {
		ArrayList<String> out = new ArrayList<>();
		for (Attribute s : Attribute.values()) {
			out.add(s.toString().toLowerCase());
		}
		return out.toArray(new String[0]);
	}
}
