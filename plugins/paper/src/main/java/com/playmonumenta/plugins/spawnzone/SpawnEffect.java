package com.playmonumenta.plugins.spawnzone;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

public class SpawnEffect {
	public enum SpawnEffectType {
		Health,
		Potion,
		Armor,
		Weapons;
	}

	public EntityType mEntityType;
	public String mName;
	public SpawnEffectType mType;
	public List<PotionEffect> mPotionList = new ArrayList<PotionEffect>();
	public List<ItemStack> mArmorList = new ArrayList<ItemStack>();
	public double mValue;

	public SpawnEffect(EntityType entityType, String name, SpawnEffectType type, List<PotionEffect> potionList) {
		mEntityType = entityType;
		mName = name;
		mType = type;
		mPotionList = potionList;
	}

	public SpawnEffect(EntityType entityType, String name, SpawnEffectType type, double value) {
		mEntityType = entityType;
		mName = name;
		mType = type;
		mValue = value;
	}

	public EntityType getEntityType() {
		return mEntityType;
	}

	public SpawnEffectType getEffectType() {
		return mType;
	}

	public String getName() {
		return mName;
	}

	public double getValue() {
		if (mType != SpawnEffectType.Health) {
			return 0;
		}
		return mValue;
	}

	public @Nullable List<PotionEffect> getPotionEffects() {
		if (mType != SpawnEffectType.Potion) {
			return null;
		}
		return mPotionList;
	}
}
