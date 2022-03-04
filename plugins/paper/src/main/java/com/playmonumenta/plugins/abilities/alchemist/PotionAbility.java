package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class PotionAbility extends Ability {

	private double mDamage;
	private double mRadius;

	public PotionAbility(Plugin plugin, @Nullable Player player, String displayName, double damage1, double damage2) {
		this(plugin, player, displayName, damage1, damage2, 3);
	}

	public PotionAbility(Plugin plugin, @Nullable Player player,
	                     String displayName, double damage1, double damage2, double radius) {
		super(plugin, player, displayName);

		// getAbilityScore() doesn't work until we set the mInfo.mScoreboard in the child class
		new BukkitRunnable() {
			@Override
			public void run() {
				mDamage = getAbilityScore() == 1 ? damage1 : damage2;
				mRadius = radius;
			}
		}.runTaskLater(mPlugin, 1);
	}

	public void apply(LivingEntity mob, boolean isGruesome) {

	}

	public void applyToPlayer(Player player, ThrownPotion potion, boolean isGruesome) {

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
