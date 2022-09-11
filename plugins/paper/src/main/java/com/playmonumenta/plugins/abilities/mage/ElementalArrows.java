package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;


public class ElementalArrows extends Ability {
	public static final String NAME = "Elemental Arrows";
	public static final ClassAbility ABILITY = ClassAbility.ELEMENTAL_ARROWS;
	public static final ClassAbility ABILITY_FIRE = ClassAbility.ELEMENTAL_ARROWS_FIRE;
	public static final ClassAbility ABILITY_ICE = ClassAbility.ELEMENTAL_ARROWS_ICE;

	public static final double AOE_DAMAGE_MULTIPLIER = 0.2;
	public static final double ENHANCED_DAMAGE_MULTIPLIER = 0.5;
	public static final int ELEMENTAL_ARROWS_BONUS_DAMAGE = 8;
	public static final int ELEMENTAL_ARROWS_DURATION = 20 * 6;
	public static final double ELEMENTAL_ARROWS_RADIUS = 3.0;
	public static final double SLOW_AMPLIFIER = 0.2;
	public static final int ENHANCED_ARROW_COOLDOWN = 10 * Constants.TICKS_PER_SECOND;
	public static final int ENHANCED_ARROW_STUN_DURATION = 1 * Constants.TICKS_PER_SECOND;

	public static final String CHARM_DAMAGE = "Elemental Arrows Damage";
	public static final String CHARM_AREA_DAMAGE = "Elemental Arrows Area Damage";
	public static final String CHARM_DURATION = "Elemental Arrows Duration";
	public static final String CHARM_RANGE = "Elemental Arrows Range";
	public static final String CHARM_THUNDER_COOLDOWN = "Elemental Arrows Thunder Arrow Cooldown";

	private double mLastDamage = 0;

	public ElementalArrows(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "Elemental";
		mInfo.mShorthandName = "EA";
		mInfo.mDescriptions.add(
			String.format("Your fully drawn arrows and tridents are set on fire. If sneaking, shoot an ice arrow instead, afflicting the target with %s%% Slowness for %s seconds. Projectiles shot this way deal magical damage instead of projectile damage. Ice arrows deal %s extra damage to Blazes. Fire arrows deal %s extra damage to strays. This skill can not apply Spellshock.",
				(int)(SLOW_AMPLIFIER * 100),
				ELEMENTAL_ARROWS_DURATION / 20,
				ELEMENTAL_ARROWS_BONUS_DAMAGE,
				ELEMENTAL_ARROWS_BONUS_DAMAGE
				));
		mInfo.mDescriptions.add(
			String.format("Your fire arrows also set nearby enemies within a radius of %s blocks on fire when they hit a target. Your ice arrows also slow nearby enemies within a radius of %s blocks when they hit a target. Both area of effect effects do %s%% bow damage to all targets affected.",
				(int)ELEMENTAL_ARROWS_RADIUS,
				(int)ELEMENTAL_ARROWS_RADIUS,
				(int)(AOE_DAMAGE_MULTIPLIER * 100)
				));
		mInfo.mDescriptions.add(
			String.format("Your next elemental arrow every %ss stuns non elite enemies hit for %ss and deals an extra %s%% bow damage to affected enemies.",
				ENHANCED_ARROW_COOLDOWN / 20,
				ENHANCED_ARROW_STUN_DURATION / 20,
				(int)(ENHANCED_DAMAGE_MULTIPLIER * 100)
			));
		mDisplayItem = new ItemStack(Material.SPECTRAL_ARROW, 1);
		mInfo.mCooldown = CharmManager.getCooldown(mPlayer, CHARM_THUNDER_COOLDOWN, ENHANCED_ARROW_COOLDOWN);
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mPlayer == null || !(event.getDamager() instanceof AbstractArrow arrow)) {
			return false;
		}
		ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(arrow);

		double damage = event.getDamage();
		double targetDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
		double areaDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_AREA_DAMAGE, damage * AOE_DAMAGE_MULTIPLIER);

		double radius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, ELEMENTAL_ARROWS_RADIUS);
		int duration = ELEMENTAL_ARROWS_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION);

		if (arrow.hasMetadata("ElementalArrowsFireArrow")) {
			if (isLevelTwo()) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), radius, enemy)) {
					EntityUtils.applyFire(mPlugin, duration, mob, mPlayer, playerItemStats);
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, ABILITY_FIRE, playerItemStats), areaDamage, true, true, false);
				}
			}
			if (enemy instanceof Stray) {
				targetDamage += ELEMENTAL_ARROWS_BONUS_DAMAGE;
			}

			EntityUtils.applyFire(mPlugin, duration, enemy, mPlayer, playerItemStats);
			event.setDamage(0);
			DamageUtils.damage(mPlayer, enemy, new DamageEvent.Metadata(DamageType.MAGIC, ABILITY_FIRE, playerItemStats), targetDamage, true, true, false);
			mLastDamage = event.getDamage();
		} else if (arrow.hasMetadata("ElementalArrowsIceArrow")) {
			if (isLevelTwo()) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), radius, enemy)) {
					EntityUtils.applySlow(mPlugin, duration, SLOW_AMPLIFIER, mob);
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, ABILITY_ICE, playerItemStats), areaDamage, true, true, false);
				}
			}
			if (enemy instanceof Blaze) {
				targetDamage += ELEMENTAL_ARROWS_BONUS_DAMAGE;
			}

			EntityUtils.applySlow(mPlugin, duration, SLOW_AMPLIFIER, enemy);
			event.setDamage(0);
			DamageUtils.damage(mPlayer, enemy, new DamageEvent.Metadata(DamageType.MAGIC, ABILITY_ICE, playerItemStats), targetDamage, true, true, false);
			mLastDamage = event.getDamage();
		} else if (arrow.hasMetadata("ElementalArrowsThunderArrow")) {
			putOnCooldown();
			targetDamage *= 1 + ENHANCED_DAMAGE_MULTIPLIER;
			areaDamage *= 1 + ENHANCED_DAMAGE_MULTIPLIER;
			if (isLevelTwo()) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), radius, enemy)) {
					EntityUtils.applyStun(mPlugin, ENHANCED_ARROW_STUN_DURATION, mob);
					DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, ABILITY, playerItemStats), areaDamage, true, true, false);
				}
			}
			EntityUtils.applyStun(mPlugin, ENHANCED_ARROW_STUN_DURATION, enemy);
			event.setDamage(0);
			DamageUtils.damage(mPlayer, enemy, new DamageEvent.Metadata(DamageType.MAGIC, ABILITY, playerItemStats), targetDamage, true, true, false);
		}
		return true; // creates new damage instances
	}

	public double getLastDamage() {
		return mLastDamage;
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (mPlayer == null) {
			return true;
		}
		if ((projectile instanceof AbstractArrow arrow && arrow.isCritical()) || projectile instanceof Trident) {
			if (isEnhanced() && !isTimerActive()) {
				projectile.setMetadata("ElementalArrowsThunderArrow", new FixedMetadataValue(mPlugin, 0));
				projectile.setFireTicks(0);
				mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.END_ROD);
			} else if (mPlayer.isSneaking()) {
				projectile.setMetadata("ElementalArrowsIceArrow", new FixedMetadataValue(mPlugin, 0));
				projectile.setFireTicks(0);
				mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.SNOW_SHOVEL);
			} else {
				projectile.setMetadata("ElementalArrowsFireArrow", new FixedMetadataValue(mPlugin, 0));
				projectile.setFireTicks(ELEMENTAL_ARROWS_DURATION);
				mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.FLAME);
			}
		}
		return true;
	}
}
