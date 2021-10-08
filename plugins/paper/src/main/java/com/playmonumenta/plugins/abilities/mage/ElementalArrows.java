package com.playmonumenta.plugins.abilities.mage;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Stray;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;



public class ElementalArrows extends Ability {
	public static final String NAME = "Elemental Arrows";
	public static final ClassAbility ABILITY = ClassAbility.ELEMENTAL_ARROWS;

	public static final double DAMAGE_MULTIPLIER_1 = 0.1;
	public static final double DAMAGE_MULTIPLIER_2 = 0.2;
	public static final int ELEMENTAL_ARROWS_BONUS_DAMAGE = 8;
	public static final int ELEMENTAL_ARROWS_DURATION = 20 * 6;
	public static final double ELEMENTAL_ARROWS_RADIUS = 3.0;
	public static final double SLOW_AMPLIFIER = 0.2;

	private double mLastDamage = 0;

	public ElementalArrows(Plugin plugin, Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "Elemental";
		mInfo.mShorthandName = "EA";
		mInfo.mDescriptions.add("Your fully drawn arrows are set on fire. If sneaking, shoot an ice arrow instead, afflicting the target with 20% Slowness for 6 seconds. Fire and Ice arrows deal 10% extra damage. Ice arrows deal 8 extra damage to Blazes. Fire arrows deal 8 extra damage to strays. This skill can not apply Spellshock.");
		mInfo.mDescriptions.add("Your fire arrows also set nearby enemies within a radius of 3 blocks on fire when they hit a target. Your ice arrows also slow nearby enemies within a radius of 3 blocks when they hit a target. Both area of effect effects do 20% bow damage to all targets affected.");
		mDisplayItem = new ItemStack(Material.SPECTRAL_ARROW, 1);
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (!EntityUtils.isSomeArrow(proj)) {
			return true;
		}
		AbstractArrow arrow = (AbstractArrow) proj;

		int elementalArrows = getAbilityScore();
		double damage = elementalArrows == 1 ? DAMAGE_MULTIPLIER_1 * event.getDamage() : DAMAGE_MULTIPLIER_2 * event.getDamage();
		if (arrow.hasMetadata("ElementalArrowsFireArrow")) {
			if (elementalArrows > 1) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ELEMENTAL_ARROWS_RADIUS, damagee)) {
					EntityUtils.applyFire(mPlugin, ELEMENTAL_ARROWS_DURATION, mob, mPlayer);
					EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell, false, true);
				}
			}
			if (damagee instanceof Stray) {
				damage += ELEMENTAL_ARROWS_BONUS_DAMAGE;
			}

			EntityUtils.applyFire(mPlugin, ELEMENTAL_ARROWS_DURATION, damagee, mPlayer);
			EntityUtils.damageEntity(mPlugin, damagee, damage, mPlayer, MagicType.FIRE, true, mInfo.mLinkedSpell, false, true);
			mLastDamage = event.getDamage();
		} else if (arrow.hasMetadata("ElementalArrowsIceArrow")) {
			if (elementalArrows > 1) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(damagee.getLocation(), ELEMENTAL_ARROWS_RADIUS, damagee)) {
					EntityUtils.applySlow(mPlugin, ELEMENTAL_ARROWS_DURATION, SLOW_AMPLIFIER, mob);
					EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.ICE, true, mInfo.mLinkedSpell, false, true);
				}
			}
			if (damagee instanceof Blaze) {
				damage += ELEMENTAL_ARROWS_BONUS_DAMAGE;
			}

			EntityUtils.applySlow(mPlugin, ELEMENTAL_ARROWS_DURATION, SLOW_AMPLIFIER, damagee);
			EntityUtils.damageEntity(mPlugin, damagee, damage, mPlayer, MagicType.ICE, true, mInfo.mLinkedSpell, false, true);
			mLastDamage = event.getDamage();
		}

		return true;
	}

	public double getLastDamage() {
		return mLastDamage;
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (arrow.isCritical()) {
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
