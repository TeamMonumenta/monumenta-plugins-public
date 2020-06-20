package com.playmonumenta.plugins.abilities.alchemist;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

public abstract class PotionAbility extends Ability {

	private final double mDamage;
	private final double mRadius;

	public PotionAbility(Plugin plugin, World world, Player player,
			String displayName, double damage1, double damage2) {
		this(plugin, world, player, displayName, damage1, damage2, 3);
	}

	public PotionAbility(Plugin plugin, World world, Player player,
			String displayName, double damage1, double damage2, double radius) {
		super(plugin, world, player, displayName);
		mDamage = getAbilityScore() == 1 ? damage1 : damage2;
		mRadius = radius;
	}

	public void apply(LivingEntity mob) {

	}

	public void createAura(Location loc, double radius) {

	}

	// This should not be overridden
	public final void createAura(Location loc) {
		createAura(loc, mRadius);
	}

	public double getDamage() {
		return mDamage;
	}

}
