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
import com.playmonumenta.plugins.effects.CursedEarth;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class EarthenTremor extends Ability {
	private static final int COOLDOWN_1 = 12 * 20;
	private static final int COOLDOWN_2 = 10 * 20;
	private static final double DAMAGE_1 = 8;
	private static final double DAMAGE_2 = 11;
	private static final double RADIUS = 7;
	private static final double TOTEM_THROW_DAMAGE_1 = 5;
	private static final double TOTEM_THROW_DAMAGE_2 = 8;
	private static final int ENHANCE_EFFECT_DURATION = 5 * 20;
	private static final double ENHANCE_MELEE_SCALING = 0.5;
	private static final double ENHANCE_PROJ_SCALING = 0.35;
	private static final double ENHANCE_RADIUS = 4;
	private static final double KNOCKUP = 0.8;
	private static final String ENHANCE_EFFECT_SOURCE = "EarthenTremorCursedEarth";
	private static final int TOTEM_THROW_RANGE = 4;
	private static final int ROOT_DURATION = 2 * 20;

	public static final String CHARM_COOLDOWN = "Earthen Tremor Cooldown";
	public static final String CHARM_DAMAGE = "Earthen Tremor Damage";
	public static final String CHARM_KNOCKUP = "Earthen Tremor Knockup";
	public static final String CHARM_RADIUS = "Earthen Tremor Radius";
	public static final String CHARM_TOTEM_THROW_DAMAGE = "Earthen Tremor Totem Landing Damage";
	public static final String CHARM_TOTEM_THROW_RADIUS = "Earthen Tremor Totem Landing Range";
	public static final String CHARM_ENHANCE_DURATION = "Earthen Tremor Cursed Earth Duration";
	public static final String CHARM_ENHANCE_MELEE = "Earthen Tremor Cursed Earth Melee Scaling";
	public static final String CHARM_ENHANCE_PROJ = "Earthen Tremor Cursed Earth Projectile Scaling";
	public static final String CHARM_ENHANCE_RADIUS = "Earthen Tremor Cursed Earth Radius";
	public static final String CHARM_ROOT_DURATION = "Earthen Tremor Root Duration";

	public static final AbilityInfo<EarthenTremor> INFO =
		new AbilityInfo<>(EarthenTremor.class, "Earthen Tremor", EarthenTremor::new)
			.linkedSpell(ClassAbility.EARTHEN_TREMOR)
			.scoreboardId("EarthenTremor")
			.shorthandName("ET")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Summons a earthen tremor on your location, dealing damage and knocking mobs up.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EarthenTremor::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.addAltPresetTrigger(new AbilityTriggerInfo<>("cast", "cast", EarthenTremor::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false).lookDirections(AbilityTrigger.LookDirection.DOWN).onGround(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.DIRT);

	private final double mDamage;
	private final double mRadius;
	private final double mTotemLandingDamage;
	private final double mTotemLandingRadius;
	private final int mCursedEarthDuration;
	private final double mCursedEarthScalingM;
	private final double mCursedEarthScalingP;
	private final double mCursedEarthRadius;
	private final double mKnockup;
	private final int mRootDuration;
	private final EarthenTremorCS mCosmetic;

	public EarthenTremor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mTotemLandingDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_TOTEM_THROW_DAMAGE, isLevelOne() ? TOTEM_THROW_DAMAGE_1 : TOTEM_THROW_DAMAGE_2);
		mTotemLandingRadius = CharmManager.getRadius(mPlayer, CHARM_TOTEM_THROW_RADIUS, TOTEM_THROW_RANGE);
		mCursedEarthDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCE_DURATION, ENHANCE_EFFECT_DURATION);
		mCursedEarthScalingM = ENHANCE_MELEE_SCALING + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_MELEE);
		mCursedEarthScalingP = ENHANCE_PROJ_SCALING + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ENHANCE_PROJ);
		mCursedEarthRadius = CharmManager.getRadius(mPlayer, CHARM_ENHANCE_RADIUS, ENHANCE_RADIUS);
		mKnockup = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKUP, KNOCKUP);
		mRootDuration = CharmManager.getDuration(mPlayer, CHARM_ROOT_DURATION, ROOT_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EarthenTremorCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getLocation();
		mCosmetic.earthenTremorEffect(mPlayer, loc, mRadius);

		List<LivingEntity> nearbyMobs = new Hitbox.SphereHitbox(loc, mRadius).getHitMobs();
		for (LivingEntity mob : nearbyMobs) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true, false);
			if (!EntityUtils.isFlyingMob(mob) && !EntityUtils.isCCImmuneMob(mob) && mob.getPassengers().isEmpty()) {
				MovementUtils.knockUp(mob, (float) mKnockup, false);
				EntityUtils.applySlow(mPlugin, mRootDuration, 1, mob);
			}
			if (isEnhanced()) {
				mPlugin.mEffectManager.addEffect(mob, ENHANCE_EFFECT_SOURCE, new CursedEarth(
					ENHANCE_EFFECT_SOURCE, mCursedEarthDuration, mPlayer, mCosmetic, mCursedEarthScalingM, mCursedEarthScalingP, mCursedEarthRadius
				));
			}
		}

		List<LivingEntity> totems = ShamanPassiveManager.getTotemList(mPlayer);
		List<ArmorStand> nearbyTotems = new Hitbox.SphereHitbox(loc, mRadius).getHitEntitiesByClass(ArmorStand.class);
		nearbyTotems.removeIf(totem -> !totems.contains(totem));

		for (LivingEntity totem : nearbyTotems) {
			MovementUtils.knockUp(totem, (float) mKnockup * 1.2f, false);

			new BukkitRunnable() {
				int mT = 0;
				@Override
				public void run() {
					mT++;
					if (mT >= 4 * 20 || (totem.isOnGround() && totem.getVelocity().getY() < 0.5)) {
						Location totemLoc = totem.getLocation();
						mCosmetic.totemLandingEffect(mPlayer, totemLoc, mTotemLandingRadius);
						List<LivingEntity> totemHits = new Hitbox.SphereHitbox(totemLoc, mTotemLandingRadius).getHitMobs();
						for (LivingEntity mob : totemHits) {
							DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mTotemLandingDamage, mInfo.getLinkedSpell(), true, false);
						}
						this.cancel();
					}

					if (totem.isDead() || !totem.isValid()) {
						this.cancel();
					}
					mT++;
				}
			}.runTaskTimer(mPlugin, 0, 5);
		}

		return true;
	}

	private static Description<EarthenTremor> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to summon an earthen tremor at your location that deals ")
			.add(a -> a.mDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage to mobs in a radius of ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks. Totems and mobs within the tremor are launched, totems dealing ")
			.add(a -> a.mTotemLandingDamage, TOTEM_THROW_DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage in a ")
			.add(a -> a.mTotemLandingRadius, TOTEM_THROW_RANGE)
			.add(" block radius of where they land. Launched mobs are also rooted in place for ")
			.addDuration(a -> a.mRootDuration, ROOT_DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN_1, Ability::isLevelOne);
	}

	private static Description<EarthenTremor> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" and totem slam damage is increased to ")
			.add(a -> a.mTotemLandingDamage, TOTEM_THROW_DAMAGE_2, false, Ability::isLevelTwo)
			.add(".")
			.addCooldown(COOLDOWN_2, Ability::isLevelTwo);
	}

	private static Description<EarthenTremor> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Mobs damaged by the intial hit of Earthen Tremor are inflicted with Cursed Earth for ")
			.addDuration(a -> a.mCursedEarthDuration, ENHANCE_EFFECT_DURATION)
			.add(" seconds. Damaging them with a [Melee / Projectile] attack deals [")
			.addPercent(a -> a.mCursedEarthScalingM, ENHANCE_MELEE_SCALING)
			.add(" / ")
			.addPercent(a -> a.mCursedEarthScalingP, ENHANCE_PROJ_SCALING)
			.add("] of the base weapon damage as magic damage to all mobs within a ")
			.add(a -> a.mCursedEarthRadius, ENHANCE_RADIUS)
			.add(" block radius. The effect is then cleared.");
	}
}
