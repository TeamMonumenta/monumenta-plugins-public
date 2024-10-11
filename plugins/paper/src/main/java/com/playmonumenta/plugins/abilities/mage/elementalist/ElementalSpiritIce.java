package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ElementalSpiritIce extends BaseElementalSpirit {
	public static final int DAMAGE_1 = 4;
	public static final int DAMAGE_2 = 6;
	public static final int SIZE = 3;
	public static final double BOW_MULTIPLIER_1 = 0.1;
	public static final double BOW_MULTIPLIER_2 = 0.15;
	public static final int PULSE_INTERVAL = Constants.TICKS_PER_SECOND;
	public static final int PULSES = 3;
	public static final int COOLDOWN_TICKS = ElementalSpiritFire.COOLDOWN_TICKS;
	public static final EnumSet<ClassAbility> ICE_ABILITIES = EnumSet.of(ClassAbility.ELEMENTAL_ARROWS_ICE, ClassAbility.BLIZZARD, ClassAbility.FROST_NOVA);

	public static final String CHARM_DAMAGE2 = "Ice Elemental Spirit Damage";
	public static final String CHARM_COOLDOWN2 = "Ice Elemental Spirit Cooldown";

	// Note that name and description are empty. This ability is ignored by Tesseract of Elements
	public static final AbilityInfo<ElementalSpiritIce> INFO =
		new AbilityInfo<>(ElementalSpiritIce.class, null, ElementalSpiritIce::new)
			.linkedSpell(ClassAbility.ELEMENTAL_SPIRIT_ICE)
			.scoreboardId("ElementalSpirit")
			.hotbarName("EsI")
			.cooldown(COOLDOWN_TICKS, ElementalSpiritFire.CHARM_COOLDOWN, CHARM_COOLDOWN2);


	private @Nullable BukkitTask mSpiritPulser;

	public ElementalSpiritIce(Plugin plugin, Player player) {
		super(plugin, player, INFO, ICE_ABILITIES, DAMAGE_1, DAMAGE_2, BOW_MULTIPLIER_1, BOW_MULTIPLIER_2);
	}

	@Override
	protected @Nullable LivingEntity getTargetEntity() {
		Location playerLocation = mPlayer.getLocation();
		@Nullable LivingEntity closestEnemy = null;
		double closestDistanceSquared = 7050;

		for (LivingEntity enemy : mEnemiesAffected) {
			if (enemy.isValid()) {
				double distanceSquared = playerLocation.distanceSquared(enemy.getLocation());
				if (distanceSquared < closestDistanceSquared) {
					closestDistanceSquared = distanceSquared;
					closestEnemy = enemy;
				}
			}
		}
		return closestEnemy;
	}

	@Override
	protected void activate(LivingEntity target, World world, double spellDamage, ItemStatManager.PlayerItemStats playerItemStats, boolean isElementalArrows) {
		Location centre = LocationUtils.getHalfHeightLocation(target);
		double size = CharmManager.getRadius(mPlayer, ElementalSpiritFire.CHARM_SIZE, SIZE);
		double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE2, spellDamage);
		mSpiritPulser = new BukkitRunnable() {
			int mPulses = 1; // The current pulse for this run

			@Override
			public void run() {
				// Damage actions
				for (LivingEntity mob : EntityUtils.getNearbyMobs(centre, size)) {
					damage(mob, damage, playerItemStats, isElementalArrows);
					mob.setVelocity(new Vector()); // Wipe velocity, extreme local climate
				}

				mCosmetic.iceSpiritPulse(mPlayer, world, centre, size);

				if (mPulses >= PULSES) {
					this.cancel();
				} else {
					mPulses++;
				}
			}
		}.runTaskTimer(mPlugin, 0, PULSE_INTERVAL);
	}

	@Override
	protected AbstractPartialParticle<?> getPeriodicParticle() {
		return mCosmetic.getIcePeriodicParticle(mPlayer);
	}

	@Override
	protected int getAngleMultiplier() {
		return -1;
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (mSpiritPulser != null) {
			mSpiritPulser.cancel();
		}
	}
}
