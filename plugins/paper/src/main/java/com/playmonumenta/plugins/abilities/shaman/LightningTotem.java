package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DestructiveExpertise;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SupportExpertise;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Transformation;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class LightningTotem extends TotemAbility {

	private static final int COOLDOWN = 22 * 20;
	private static final int TOTEM_DURATION = 10 * 20;
	private static final int INTERVAL = 2 * 20;
	private static final int AOE_RANGE = 7;
	private static final int DAMAGE_1 = 14;
	private static final int DAMAGE_2 = 21;
	private static final double ROD_CHANCE = 0.75;
	private static final double ROD_DAMAGE_PERCENT = 0.35;
	private static final int ROD_DAMAGE_RADIUS = 2;
	public static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.25f);

	public static final String CHARM_DURATION = "Lightning Totem Duration";
	public static final String CHARM_RADIUS = "Lightning Totem Radius";
	public static final String CHARM_COOLDOWN = "Lightning Totem Cooldown";
	public static final String CHARM_DAMAGE = "Lightning Totem Damage";
	public static final String CHARM_ROD_CHANCE = "Lightning Totem Lightning Rod Chance";
	public static final String CHARM_ROD_DAMAGE = "Lightning Totem Lightning Rod Damage";
	public static final String CHARM_ROD_RADIUS = "Lightning Totem Lightning Rod Radius";
	public static final String CHARM_PULSE_DELAY = "Lightning Totem Pulse Delay";

	public static final AbilityInfo<LightningTotem> INFO =
		new AbilityInfo<>(LightningTotem.class, "Lightning Totem", LightningTotem::new)
			.linkedSpell(ClassAbility.LIGHTNING_TOTEM)
			.scoreboardId("LightningTotem")
			.shorthandName("LT")
			.descriptions(
				String.format("Press the drop key while holding a melee weapon and not sneaking to fire a projectile that summons a Lightning Totem. The totem will target a " +
					"mob within %s blocks with priority towards boss and elite mobs and deal %s magic damage every %s seconds. Charge up time: %ss. Duration: %ss. Cooldown: %ss.",
					AOE_RANGE,
					DAMAGE_1,
					StringUtils.ticksToSeconds(INTERVAL),
					StringUtils.ticksToSeconds(TotemAbility.PULSE_DELAY),
					StringUtils.ticksToSeconds(TOTEM_DURATION),
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("The totem deals %s magic damage per hit.",
					DAMAGE_2),
				String.format("If the totem kills a mob, there is a %s%% chance of it spawning a lightning rod " +
					"at the mob's location. The lightning rod will deal %s%% of the main magic damage " +
					"to all mobs within %s blocks of the rod every %s seconds.",
					StringUtils.multiplierToPercentage(ROD_CHANCE),
					StringUtils.multiplierToPercentage(ROD_DAMAGE_PERCENT),
					ROD_DAMAGE_RADIUS,
					StringUtils.ticksToSeconds(INTERVAL)
				)
			)
			.simpleDescription("Summon a totem which will strike a mob within range for high damage throughout its duration.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", LightningTotem::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.YELLOW_WOOL);

	private double mDamage;
	private final int mDuration;
	private final double mRadius;
	private @Nullable LivingEntity mTarget = null;
	public double mDecayedTotemBuff = 0;
	private final double mRodChance;
	private final double mRodDamagePercent;
	private final double mRodRadius;
	private final List<BlockDisplay> mAllDisplays = new ArrayList<>();
	private final int mInterval;

	public LightningTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Lightning Totem Projectile", "LightningTotem", "Lightning Totem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamage *= DestructiveExpertise.damageBuff(mPlayer);
		mDamage *= SupportExpertise.damageBuff(mPlayer);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, TOTEM_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
		mRodChance = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ROD_CHANCE, ROD_CHANCE);
		mRodDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ROD_DAMAGE, ROD_DAMAGE_PERCENT);
		mRodRadius = CharmManager.getRadius(mPlayer, CHARM_ROD_RADIUS, ROD_DAMAGE_RADIUS);
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, INTERVAL);
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		new PPCircle(Particle.CRIT, standLocation.clone().add(0, 0.3, 0), mRadius).countPerMeter(0.4).spawnAsPlayerActive(mPlayer);
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean bonusAction) {
		if (mTarget == null || mTarget.isDead() || !mTarget.isValid() || mTarget.getLocation().distance(standLocation) > mRadius) {
			mTarget = null;
			List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null);
			affectedMobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));
			affectedMobs.removeIf(mob -> DamageUtils.isImmuneToDamage(mob, DamageEvent.DamageType.MAGIC));
			if (!affectedMobs.isEmpty()) {
				Collections.shuffle(affectedMobs);
				for (LivingEntity mob : affectedMobs) {
					if (mTarget == null) {
						mTarget = mob;
					}
					if (EntityUtils.isBoss(mob) || EntityUtils.isElite(mob)) {
						mTarget = mob;
						break;
					}
				}
			}
		}
		double damageApplied = (mDamage + mDecayedTotemBuff)
			* (bonusAction ? ChainLightning.ENHANCE_NEGATIVE_EFFICIENCY : 1);
		if (mTarget != null) {
			DamageUtils.damage(mPlayer, mTarget, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC,
				mInfo.getLinkedSpell(), stats), damageApplied, true, false, false);
			PPLightning lightning = new PPLightning(Particle.END_ROD, mTarget.getLocation())
				.count(8).duration(3);
			mPlayer.getWorld().playSound(mTarget.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);
			lightning.init(4, 2.5, 0.3, 0.3);
			lightning.spawnAsPlayerActive(mPlayer);
		}
		if (isEnhanced()) {
			for (BlockDisplay display : mAllDisplays) {
				Location targetSpot = display.getLocation().clone().add(0.5, 0, 0.5);
				PPLightning lightning = new PPLightning(Particle.END_ROD, targetSpot)
					.count(8).duration(3);
				mPlayer.getWorld().playSound(display.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.25f);
				lightning.init(3, 2.5, 0.3, 0.3);
				lightning.spawnAsPlayerActive(mPlayer);
				for (LivingEntity entity : EntityUtils.getNearbyMobsInSphere(targetSpot, mRodRadius, mTarget)) {
					DamageUtils.damage(mPlayer, entity, new DamageEvent.Metadata(
						DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), stats),
						damageApplied * mRodDamagePercent, true, false, false);
				}
			}
		}
		dealSanctuaryImpacts(EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null), INTERVAL + 20);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (isEnhanced()
			&& mTarget != null
			&& event.getEntity().equals(mTarget)
			&& Math.random() < mRodChance) {
			BlockDisplay blockDisplay = mTarget.getWorld().spawn(mTarget.getLocation().clone().add(-0.5, -0.3, -0.5), BlockDisplay.class);
			blockDisplay.addScoreboardTag("REMOVE_ON_UNLOAD");
			blockDisplay.setBlock(Material.LIGHTNING_ROD.createBlockData());
			blockDisplay.setBrightness(new Display.Brightness(8, 8));
			blockDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f), new Quaternionf()));
			blockDisplay.setInterpolationDuration(2);
			mAllDisplays.add(blockDisplay);
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		new PartialParticle(Particle.FLASH, standLocation, 3, 0.3, 1.1, 0.3, 0.15).spawnAsPlayerActive(mPlayer);
		world.playSound(standLocation, Sound.ENTITY_BLAZE_DEATH, 0.7f, 0.5f);
		mTarget = null;
		mDecayedTotemBuff = 0;
		for (BlockDisplay display : mAllDisplays) {
			display.remove();
		}
		mAllDisplays.removeIf(display -> !display.isValid());
		if (mAllDisplays.size() > 0) {
			MMLog.warning("Failed to remove all lightning rod displays for player "
				+ mPlayer.getName() + " on totem expire, remaining: " + mAllDisplays.size());
		}
	}

	@Override
	public void onTotemHitEntity(Entity entity) {
		if (entity instanceof LivingEntity target
			&& EntityUtils.isHostileMob(target)) {
			mTarget = target;
		}
	}

}
