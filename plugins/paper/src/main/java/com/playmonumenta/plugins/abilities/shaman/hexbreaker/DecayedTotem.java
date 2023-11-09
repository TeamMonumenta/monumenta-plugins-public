package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.ChainLightning;
import com.playmonumenta.plugins.abilities.shaman.FlameTotem;
import com.playmonumenta.plugins.abilities.shaman.LightningTotem;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DecayedTotem extends TotemAbility {

	private static final int COOLDOWN = 26 * 20;
	private static final int AOE_RANGE = 8;
	private static final int INTERVAL_1 = 20;
	private static final int INTERVAL_2 = 10;
	private static final int DURATION_1 = 8 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final int DAMAGE = 4;
	private static final double SLOWNESS_PERCENT = 0.3;
	private static final int TARGETS = 3;
	private static final int FLAME_TOTEM_DAMAGE_BUFF = 1;
	private static final int LIGHTNING_TOTEM_DAMAGE_BUFF = 5;

	private static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);
	private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(5, 120, 5), 1.0f);

	public static final String CHARM_DURATION = "Decayed Totem Duration";
	public static final String CHARM_RADIUS = "Decayed Totem Radius";
	public static final String CHARM_COOLDOWN = "Decayed Totem Cooldown";
	public static final String CHARM_TARGETS = "Decayed Totem Targets";
	public static final String CHARM_DAMAGE = "Decayed Totem Damage";
	public static final String CHARM_SLOWNESS = "Decayed Totem Slowness Amplifier";
	public static final String CHARM_FLAME_TOTEM_DAMAGE_BUFF = "Decayed Totem Flame Totem Damage";
	public static final String CHARM_LIGHTNING_TOTEM_DAMAGE_BUFF = "Decayed Totem Lightning Totem Damage";
	public static final String CHARM_PULSE_DELAY = "Decayed Totem Pulse Delay";

	public static final AbilityInfo<DecayedTotem> INFO =
		new AbilityInfo<>(DecayedTotem.class, "Decayed Totem", DecayedTotem::new)
			.linkedSpell(ClassAbility.DECAYED_TOTEM)
			.scoreboardId("DecayedTotem")
			.shorthandName("DT")
			.descriptions(
				String.format("Press the swap key while holding a melee weapon and not sneaking to fire a projectile that summons a Decayed Totem. The totem anchors up to %s targets within %s blocks of the totem, " +
					"inflicting %s magic damage per second and %s%% Slowness. Additionally grants %s damage to your flame totems and %s damage to your lightning " +
					" totems that exist during this totem's duration. Charge up time: %ss. Duration: %ss. Cooldown: %ss.",
					TARGETS,
					AOE_RANGE,
					DAMAGE,
					StringUtils.multiplierToPercentage(SLOWNESS_PERCENT),
					FLAME_TOTEM_DAMAGE_BUFF,
					LIGHTNING_TOTEM_DAMAGE_BUFF,
					StringUtils.ticksToSeconds(TotemAbility.PULSE_DELAY),
					StringUtils.ticksToSeconds(DURATION_1),
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("Damage now ticks every half second, and duration is increased to %ss.",
					StringUtils.ticksToSeconds(DURATION_2))
			)
			.simpleDescription("Summons a totem, dealing damage and heavily slowing 3 mobs within range.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DecayedTotem::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.WITHER_ROSE);

	private final double mDamage;
	private final int mDuration;
	private final double mRadius;
	private final double mSlowness;
	private final int mTargetCount;
	private final List<LivingEntity> mTargets = new ArrayList<>();
	private final double mFlameTotemBuff;
	private final double mLightningTotemBuff;
	private final int mInterval;

	public DecayedTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Decayed Totem Projectile", "DecayedTotem", "Decayed Totem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE) * DestructiveExpertise.damageBuff(mPlayer);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, isLevelTwo() ? INTERVAL_2 : INTERVAL_1);
		mSlowness = SLOWNESS_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		mTargetCount = TARGETS + (int) CharmManager.getLevel(mPlayer, CHARM_TARGETS);
		mFlameTotemBuff = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_FLAME_TOTEM_DAMAGE_BUFF, FLAME_TOTEM_DAMAGE_BUFF);
		mLightningTotemBuff = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_LIGHTNING_TOTEM_DAMAGE_BUFF, LIGHTNING_TOTEM_DAMAGE_BUFF);
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks == 0) {
			applyDecayedDamageBoost();
			stand.getWorld().playSound(stand, Sound.BLOCK_CONDUIT_AMBIENT, 20.0f, 1.2f);
			stand.getWorld().playSound(stand, Sound.ENTITY_SKELETON_HURT, 0.6f, 0.3f);
			stand.getWorld().playSound(stand, Sound.ENTITY_PHANTOM_DEATH, 0.5f, 0.2f);
		}
		mTargets.removeIf(mob -> standLocation.distance(mob.getLocation()) >= mRadius || mob.isDead());
		if (mTargets.size() < mTargetCount) {
			List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null);
			Collections.shuffle(affectedMobs);

			for (LivingEntity mob : affectedMobs) {
				if (mTargets.contains(mob) || standLocation.distance(mob.getLocation()) >= mRadius) {
					continue;
				}
				impactMob(mob, mInterval + 5, false, false);
				mTargets.add(mob);
				if (mTargets.size() >= mTargetCount) {
					break;
				}
			}
		}

		if (ticks % 5 == 0) {
			for (LivingEntity target : mTargets) {
				new PPLine(Particle.REDSTONE, stand.getEyeLocation(), target.getLocation()).countPerMeter(8).delta(0.03).data(BLACK).spawnAsPlayerActive(mPlayer);
				new PPLine(Particle.REDSTONE, stand.getEyeLocation(), target.getLocation()).countPerMeter(8).delta(0.03).data(GREEN).spawnAsPlayerActive(mPlayer);
			}
		}
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}
	}

	private void impactMob(LivingEntity target, int duration, boolean dealDamage, boolean bonusAction) {
		if (dealDamage) {
			DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.MAGIC,
				mDamage * (bonusAction ? ChainLightning.ENHANCE_NEGATIVE_EFFICIENCY : 1),
				ClassAbility.DECAYED_TOTEM, true);
		}
		EntityUtils.applySlow(mPlugin, duration, mSlowness, target);
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		new PartialParticle(Particle.SQUID_INK, standLocation, 5, 0.2, 1.1, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(standLocation, Sound.BLOCK_WOOD_BREAK, 0.7f, 0.5f);
		mTargets.clear();
		applyDecayedDamageBoost();
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean bonusAction) {
		applyDecayedDamageBoost();
		for (LivingEntity target : mTargets) {
			impactMob(target, mInterval + 20, true, bonusAction);
		}
		dealSanctuaryImpacts(EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null), mInterval + 20);
	}

	public void applyDecayedDamageBoost() {
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			if (abil instanceof TotemAbility totemAbility && totemAbility.getRemainingAbilityDuration() > 0 && !(abil instanceof DecayedTotem)) {
				if (totemAbility instanceof FlameTotem totem) {
					totem.mDecayedTotemBuff = mFlameTotemBuff;
					totemAbility.mDecayedBuffed = true;
				} else if (totemAbility instanceof LightningTotem totem) {
					totem.mDecayedTotemBuff = mLightningTotemBuff;
					totemAbility.mDecayedBuffed = true;
				}
			}
		}
	}
}
