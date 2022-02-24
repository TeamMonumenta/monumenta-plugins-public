package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import javax.annotation.Nullable;


public class ElementalArrows extends Ability {
	public static final String NAME = "Elemental Arrows";
	public static final ClassAbility ABILITY = ClassAbility.ELEMENTAL_ARROWS;
	public static final ClassAbility ABILITY_FIRE = ClassAbility.ELEMENTAL_ARROWS_FIRE;
	public static final ClassAbility ABILITY_ICE = ClassAbility.ELEMENTAL_ARROWS_ICE;

	public static final double DAMAGE_MULTIPLIER_1 = 0.1;
	public static final double DAMAGE_MULTIPLIER_2 = 0.2;
	public static final int ELEMENTAL_ARROWS_BONUS_DAMAGE = 8;
	public static final int ELEMENTAL_ARROWS_DURATION = 20 * 6;
	public static final double ELEMENTAL_ARROWS_RADIUS = 3.0;
	public static final double SLOW_AMPLIFIER = 0.2;

	private double mLastDamage = 0;
	private double mDamageMultiplier;

	public ElementalArrows(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "Elemental";
		mInfo.mShorthandName = "EA";
		mInfo.mDescriptions.add("Your fully drawn arrows and tridents are set on fire. If sneaking, shoot an ice arrow instead, afflicting the target with 20% Slowness for 6 seconds. Fire and Ice arrows deal 10% extra damage. Ice arrows deal 8 extra damage to Blazes. Fire arrows deal 8 extra damage to strays. This skill can not apply Spellshock.");
		mInfo.mDescriptions.add("Your fire arrows also set nearby enemies within a radius of 3 blocks on fire when they hit a target. Your ice arrows also slow nearby enemies within a radius of 3 blocks when they hit a target. Both area of effect effects do 20% bow damage to all targets affected.");
		mDisplayItem = new ItemStack(Material.SPECTRAL_ARROW, 1);

		mDamageMultiplier = isLevelOne() ? DAMAGE_MULTIPLIER_1 : DAMAGE_MULTIPLIER_2;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer == null || !(event.getDamager() instanceof AbstractArrow)) {
			return false;
		}
		AbstractArrow arrow = (AbstractArrow) event.getDamager();

		double damage = mDamageMultiplier * event.getDamage();
		if (arrow.hasMetadata("ElementalArrowsFireArrow")) {
			if (isLevelTwo()) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), ELEMENTAL_ARROWS_RADIUS, enemy)) {
					EntityUtils.applyFire(mPlugin, ELEMENTAL_ARROWS_DURATION, mob, mPlayer);
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, ABILITY_FIRE, true);
				}
			}
			if (enemy instanceof Stray) {
				damage += ELEMENTAL_ARROWS_BONUS_DAMAGE;
			}

			EntityUtils.applyFire(mPlugin, ELEMENTAL_ARROWS_DURATION, enemy, mPlayer);
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, damage, ABILITY_FIRE);
			mLastDamage = event.getDamage();
		} else if (arrow.hasMetadata("ElementalArrowsIceArrow")) {
			if (isLevelTwo()) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), ELEMENTAL_ARROWS_RADIUS, enemy)) {
					EntityUtils.applySlow(mPlugin, ELEMENTAL_ARROWS_DURATION, SLOW_AMPLIFIER, mob);
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, ABILITY_ICE, true);
				}
			}
			if (enemy instanceof Blaze) {
				damage += ELEMENTAL_ARROWS_BONUS_DAMAGE;
			}

			EntityUtils.applySlow(mPlugin, ELEMENTAL_ARROWS_DURATION, SLOW_AMPLIFIER, enemy);
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, damage, ABILITY_ICE);
			mLastDamage = event.getDamage();
		}
		return true; // creates new damage instances
	}

	public double getLastDamage() {
		return mLastDamage;
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlayer == null) {
			return true;
		}
		if (arrow.isCritical() || arrow instanceof Trident) {
			if (mPlayer.isSneaking()) {
				arrow.setMetadata("ElementalArrowsIceArrow", new FixedMetadataValue(mPlugin, 0));
				arrow.setFireTicks(0);
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.SNOW_SHOVEL);
			} else {
				arrow.setMetadata("ElementalArrowsFireArrow", new FixedMetadataValue(mPlugin, 0));
				arrow.setFireTicks(ELEMENTAL_ARROWS_DURATION);
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.FLAME);
			}
		}
		return true;
	}
}
