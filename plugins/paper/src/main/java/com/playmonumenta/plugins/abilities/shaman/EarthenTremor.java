package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.EarthenTremorCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class EarthenTremor extends Ability {

	private static final int COOLDOWN = 10 * 20;
	private static final int SILENCE_DURATION = 30;
	private static final int RANGE = 7;
	private static final int DAMAGE_1 = 10;
	private static final int DAMAGE_2 = 13;
	private static final double KNOCKBACK = 0.8;
	private static final int SHOCKWAVES = 6;
	private static final int SHOCKWAVE_RADIUS = 3;
	private static final int SHOCKWAVE_DISTANCE = 5;
	private static final double DAMAGE_BONUS_ENHANCE = 2.5;

	public static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.25f);

	public static final String CHARM_COOLDOWN = "Earthen Tremor Cooldown";
	public static final String CHARM_DAMAGE = "Earthen Tremor Damage";
	public static final String CHARM_RADIUS = "Earthen Tremor Radius";
	public static final String CHARM_SILENCE_DURATION = "Earthen Tremor Silence Duration";
	public static final String CHARM_KNOCKBACK = "Earthen Tremor Knockback";
	public static final String CHARM_SHOCKWAVES = "Earthen Tremor Shockwaves";
	public static final String CHARM_SHOCKWAVE_RADIUS = "Earthen Tremor Shockwave Radius";
	public static final String CHARM_SHOCKWAVE_DISTANCE = "Earthen Tremor Shockwave Distance";

	public static final AbilityInfo<EarthenTremor> INFO =
		new AbilityInfo<>(EarthenTremor.class, "Earthen Tremor", EarthenTremor::new)
			.linkedSpell(ClassAbility.EARTHEN_TREMOR)
			.scoreboardId("EarthenTremor")
			.shorthandName("ET")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Summons a earthen tremor on your location, dealing damage and knocking mobs away.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EarthenTremor::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.DIRT);

	private final double mDamageBase;
	private final double mDamageEnhance;
	private final double mDamage;
	private final double mRadius;
	private final int mSilenceDuration;
	private final float mKnockback;
	private final int mShockwaves;
	private final double mShockwaveDistance;
	private final double mShockwaveRadius;
	private final List<LivingEntity> mHitEntities = new ArrayList<>();
	private final EarthenTremorCS mCosmetic;

	public EarthenTremor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageBase = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamageEnhance = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_BONUS_ENHANCE);
		mDamage = mDamageBase + (isEnhanced() ? mDamageEnhance : 0);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RANGE);
		mSilenceDuration = CharmManager.getDuration(mPlayer, CHARM_SILENCE_DURATION, SILENCE_DURATION);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		mShockwaves = SHOCKWAVES + (int) CharmManager.getLevel(mPlayer, CHARM_SHOCKWAVES);
		mShockwaveDistance = CharmManager.getRadius(mPlayer, CHARM_SHOCKWAVE_DISTANCE, SHOCKWAVE_DISTANCE);
		mShockwaveRadius = CharmManager.getRadius(mPlayer, CHARM_SHOCKWAVE_RADIUS, SHOCKWAVE_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EarthenTremorCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		for (LivingEntity mob : EntityUtils.getNearbyMobsInSphere(mPlayer.getLocation(), mRadius, null)) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, false);
			mHitEntities.add(mob);
			if (!EntityUtils.isCCImmuneMob(mob)) {
				MovementUtils.knockAway(mPlayer, mob, mKnockback);
				if (isLevelTwo()) {
					EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
				}
			}
		}
		mCosmetic.earthenTremorEffect(mPlayer, mRadius);

		if (isEnhanced()) {
			int angleBetween = 360 / mShockwaves;
			Vector forward = mPlayer.getLocation().getDirection().setY(0).normalize();
			for (int i = 0; i < 360; i += angleBetween) {
				Vector normDir = VectorUtils.rotateYAxis(forward, i).normalize();
				Location shockwaveLoc = mPlayer.getLocation().add(normDir.clone().multiply(mRadius));
				for (int j = 0; j < mShockwaveDistance; j++) {
					mCosmetic.earthenTremorEnhancement(mPlayer, shockwaveLoc, mShockwaveRadius, j, mShockwaveDistance);
					shockwaveLoc.add(normDir);
					List<LivingEntity> mShockwaveHits = EntityUtils.getNearbyMobsInSphere(shockwaveLoc, mShockwaveRadius, null);
					mShockwaveHits.removeAll(mHitEntities);
					for (LivingEntity mob : mShockwaveHits) {
						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, false);
						mHitEntities.add(mob);
						if (!EntityUtils.isCCImmuneMob(mob)) {
							MovementUtils.knockAway(mPlayer, mob, mKnockback);
							if (isLevelTwo()) {
								EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
							}
						}
					}
				}
			}
		}
		mHitEntities.clear();
		return true;
	}

	private static Description<EarthenTremor> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to summon an earthen tremor at your location that deals ")
			.add(a -> a.mDamageBase, DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage to mobs within ")
			.add(a -> a.mRadius, RANGE)
			.add(" blocks and pushes them away.")
			.addCooldown(COOLDOWN);
	}

	private static Description<EarthenTremor> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mDamageBase, DAMAGE_2, false, Ability::isLevelTwo)
			.add(". Now additionally silences mobs for ")
			.addDuration(a -> a.mSilenceDuration, SILENCE_DURATION)
			.add(" seconds.");
	}

	private static Description<EarthenTremor> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Upon triggering, now sends out ")
			.add(a -> a.mShockwaves, SHOCKWAVES)
			.add(" additional shockwaves from the edge of the radius outwards for an additional ")
			.add(a -> a.mShockwaveDistance, SHOCKWAVE_DISTANCE)
			.add(" blocks with a radius of ")
			.add(a -> a.mShockwaveRadius, SHOCKWAVE_RADIUS)
			.add(" blocks. Damage is increased by ")
			.add(a -> a.mDamageEnhance, DAMAGE_BONUS_ENHANCE)
			.add(".");
	}
}
