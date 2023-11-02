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
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FlameTotem extends TotemAbility {

	private static final int COOLDOWN = 22 * 20;
	private static final int INTERVAL = 20;
	private static final int DURATION_1 = 10 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final int AOE_RANGE_1 = 6;
	private static final int AOE_RANGE_2 = 7;
	private static final int FIRE_DURATION = 2 * 20;
	private static final int DAMAGE_1 = 4;
	private static final int DAMAGE_2 = 5;
	private static final double ENHANCE_RADIUS = 2.5;
	private static final double ENHANCE_INFERNO_SCALE = 0.5;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);

	public static final String CHARM_DURATION = "Flame Totem Duration";
	public static final String CHARM_RADIUS = "Flame Totem Radius";
	public static final String CHARM_COOLDOWN = "Flame Totem Cooldown";
	public static final String CHARM_DAMAGE = "Flame Totem Damage";
	public static final String CHARM_FIRE_DURATION = "Flame Totem Fire Duration";
	public static final String CHARM_ENHANCE_RADIUS = "Flame Totem Enhancement Radius";
	public static final String CHARM_ENHANCE_INFERNO_SCALE = "Flame Totem Enhancement Inferno Efficiency";
	public static final String CHARM_PULSE_DELAY = "Flame Totem Pulse Delay";

	private double mDamage;
	private final int mDuration;
	private final double mRadius;
	private final double mEnhanceRadius;
	private final int mFireDuration;
	private final double mEnhanceInfernoScale;
	private final int mInterval;

	public double mDecayedTotemBuff = 0;

	public static final AbilityInfo<FlameTotem> INFO =
		new AbilityInfo<>(FlameTotem.class, "Flame Totem", FlameTotem::new)
			.linkedSpell(ClassAbility.FLAME_TOTEM)
			.scoreboardId("FlameTotem")
			.shorthandName("FT")
			.descriptions(
				String.format("Right click while holding a melee weapon and sneaking to fire a projectile that summons a flame totem. Mobs within %s blocks of this totem are dealt %s magic damage and set on " +
					"fire, without inferno damage, for %s seconds every second. Charge up time: %ss. Duration: %ss. Cooldown: %ss.",
					AOE_RANGE_1,
					DAMAGE_1,
					StringUtils.ticksToSeconds(FIRE_DURATION),
					StringUtils.ticksToSeconds(TotemAbility.PULSE_DELAY),
					StringUtils.ticksToSeconds(DURATION_1),
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("The totem deals %s magic damage per hit, radius is increased to %s, " +
						"and duration is extended to %ss.",
					DAMAGE_2,
					AOE_RANGE_2,
					StringUtils.ticksToSeconds(DURATION_2)),
				String.format("The totem now throws explosive fireballs at a target " +
					"every second that deal %s magic damage in a %s block radius and set mobs on fire for %s seconds " +
					"instead of the base skill's approach. Applies inferno at %s%% efficiency.",
					DAMAGE_2,
					ENHANCE_RADIUS,
					StringUtils.ticksToSeconds(FIRE_DURATION),
					StringUtils.multiplierToPercentage(ENHANCE_INFERNO_SCALE))
			)
			.simpleDescription("Summon a totem that deals damage and sets mobs on fire within its range.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", FlameTotem::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.MAGMA_BLOCK);

	public FlameTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Flame Totem Projectile", "FlameTotem", "Flame Totem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamage *= DestructiveExpertise.damageBuff(mPlayer);
		mDamage *= SupportExpertise.damageBuff(mPlayer);

		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelTwo() ? AOE_RANGE_2 : AOE_RANGE_1);
		mFireDuration = CharmManager.getDuration(mPlayer, CHARM_FIRE_DURATION, FIRE_DURATION);
		mEnhanceRadius = CharmManager.getRadius(mPlayer, CHARM_ENHANCE_RADIUS, ENHANCE_RADIUS);
		mEnhanceInfernoScale = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_INFERNO_SCALE, ENHANCE_INFERNO_SCALE);
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, INTERVAL);
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (isEnhanced()) {
			PPCircle fireRing = new PPCircle(Particle.SOUL_FIRE_FLAME, standLocation, mRadius).ringMode(true).countPerMeter(0.2).delta(0);
			fireRing.spawnAsPlayerActive(mPlayer);
		} else {
			PPCircle fireRing = new PPCircle(Particle.FLAME, standLocation, mRadius).ringMode(true).countPerMeter(0.6).delta(0);
			fireRing.spawnAsPlayerActive(mPlayer);
		}
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
			dealSanctuaryImpacts(EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null), 40);
		}
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean bonusAction) {
		List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null);
		double damageApplied = (mDamage + mDecayedTotemBuff)
			* (bonusAction ? ChainLightning.ENHANCE_NEGATIVE_EFFICIENCY : 1);
		if (isEnhanced()) {
			Collections.shuffle(affectedMobs);
			if (!affectedMobs.isEmpty()) {
				LivingEntity target = affectedMobs.get(0);
				Location targetLocation = target.getLocation();
				Location standEyeLocation = standLocation.clone().add(0, 1.5, 0);
				new PPLine(Particle.FLAME, standEyeLocation, targetLocation)
					.countPerMeter(8).delta(0).spawnAsPlayerActive(mPlayer);
				DisplayEntityUtils.groundBlockQuake(targetLocation, mEnhanceRadius,
					List.of(Material.RED_CONCRETE_POWDER, Material.FIRE_CORAL_BLOCK, Material.ORANGE_CONCRETE),
					new Display.Brightness(10, 10));
				List<LivingEntity> newImpactedMobs = EntityUtils.getNearbyMobsInSphere(targetLocation, mEnhanceRadius, null);
				for (LivingEntity mob : newImpactedMobs) {
					DamageUtils.damage(mPlayer, mob,
						new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), stats),
						damageApplied, true, false, false);
					EntityUtils.applyFire(mPlugin, mFireDuration, mob, mPlayer,
						stats, mEnhanceInfernoScale);
				}
			}
		} else {
			for (LivingEntity mob : affectedMobs) {
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC,
					mInfo.getLinkedSpell(), stats), damageApplied,
					true, false, false);
				EntityUtils.applyFire(mPlugin, mFireDuration, mob, null);
			}

			PPCircle fireArea = new PPCircle(Particle.FLAME, standLocation, mRadius).ringMode(false).countPerMeter(1.3).delta(0.01).extra(0.05);
			fireArea.spawnAsPlayerActive(mPlayer);

			standLocation.getWorld().playSound(standLocation, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE,
				0.3f, 0.5f);
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		new PartialParticle(Particle.REDSTONE, standLocation, 45, 0.2, 1.1, 0.2, 0.1, COLOR).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, standLocation, 40, 0.3, 1.1, 0.3, 0.15).spawnAsPlayerActive(mPlayer);
		world.playSound(standLocation, Sound.ENTITY_BLAZE_DEATH, 0.7f, 0.5f);
		mDecayedTotemBuff = 0;
	}
}
