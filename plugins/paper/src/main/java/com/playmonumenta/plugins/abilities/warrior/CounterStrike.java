package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.CounterStrikeCS;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CounterStrike extends Ability {
	private static final int DURATION = 4 * 20;
	private static final double DAMAGE_1 = 0.1;
	private static final double DAMAGE_2 = 0.2;
	private static final double RESISTANCE = 0.1;
	private static final double ABSORPTION_RESISTANCE = 0.125;
	private static final double KBR = 0.5;
	private static final String KBR_EFFECT = "CounterStrikeKBREffect";
	private static final double BLEED_AMPLIFIER = 0.1;
	private static final int BLEED_DURATION = 8 * 20;
	private static final int RADIUS = 2;

	public static final String CHARM_DURATION = "Counter Strike Duration";
	public static final String CHARM_DAMAGE = "Counter Strike Damage";
	public static final String CHARM_DAMAGE_REDUCTION = "Counter Strike Damage Reduction";
	public static final String CHARM_KBR = "Counter Strike Knockback Resistance";
	public static final String CHARM_BLEED = "Counter Strike Bleed Amplifier";
	public static final String CHARM_BLEED_DURATION = "Counter Strike Bleed Duration";
	public static final String CHARM_RADIUS = "Counter Strike Radius";

	public static final AbilityInfo<CounterStrike> INFO =
		new AbilityInfo<>(CounterStrike.class, "Counter Strike", CounterStrike::new)
			.linkedSpell(ClassAbility.COUNTER_STRIKE)
			.scoreboardId("CounterStrike")
			.shorthandName("CS")
			.descriptions(
				String.format(
					"When a mob attacks you (even if the attack is blocked or negated), a Counter Strike is primed against that mob, which lasts up to %ss. Hitting a mob with a Counter Strike deals %s%% more damage and removes the Counter Strike.",
					StringUtils.ticksToSeconds(DURATION),
					StringUtils.multiplierToPercentage(DAMAGE_1)
				),
				String.format(
					"Damage bonus is increased to %s%%. Additionally, the first time you taunt a given mob using an ability, a Counter Strike is primed against that mob.",
					StringUtils.multiplierToPercentage(DAMAGE_2)
				),
				String.format(
					"Take %s%% less damage from a mob that has a Counter Strike primed against them. If the Counter Strike was primed while you had absorption active, take %s%% less damage instead, and gain %s%% Knockback Resistance while the Counter Strike is active. Also, when dealing a Counter Strike, apply %s%% Bleed for %ss to mobs in a %s block radius.",
					StringUtils.multiplierToPercentage(RESISTANCE),
					StringUtils.multiplierToPercentage(ABSORPTION_RESISTANCE),
					StringUtils.multiplierToPercentage(KBR),
					StringUtils.multiplierToPercentage(BLEED_AMPLIFIER),
					StringUtils.ticksToSeconds(BLEED_DURATION),
					RADIUS
				)
			)
			.simpleDescription("Deal extra damage to a mob after taking damage from them.")
			.displayItem(Material.CACTUS);

	private final Map<LivingEntity, Integer> mLastHurtTicks;
	private final List<LivingEntity> mTauntedMobs;
	private final List<LivingEntity> mAbsorptionMobs;
	private final int mDuration;
	private final double mDamage;
	private final double mKBR;

	private final CounterStrikeCS mCosmetic;

	public CounterStrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mLastHurtTicks = new HashMap<>();
		mTauntedMobs = new ArrayList<>();
		mAbsorptionMobs = new ArrayList<>();
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mDamage = (isLevelOne() ? DAMAGE_1 : DAMAGE_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mKBR = KBR + CharmManager.getLevel(mPlayer, CHARM_KBR) / 10;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CounterStrikeCS());
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source != null) {
			if (isEnhanced() && mLastHurtTicks.containsKey(source)) {
				double resistance = (mAbsorptionMobs.contains(source) ? ABSORPTION_RESISTANCE : RESISTANCE) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_REDUCTION);
				event.setFlatDamage(event.getDamage() * (1 - resistance));
			}

			prime(source);
			mCosmetic.onPrime(mPlayer, mPlayer.getLocation());
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MELEE && mLastHurtTicks.remove(enemy) != null) {
			if (isEnhanced()) {
				double bleed = BLEED_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BLEED);
				int duration = CharmManager.getDuration(mPlayer, CHARM_BLEED_DURATION, BLEED_DURATION);
				double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
				Hitbox hitbox = new Hitbox.SphereHitbox(enemy.getLocation().add(0, 0.75, 0), radius);
				for (LivingEntity entity : hitbox.getHitMobs()) {
					EntityUtils.applyBleed(mPlugin, duration, bleed, entity);
				}
			}

			event.updateDamageWithMultiplier(1 + mDamage);

			mLastHurtTicks.remove(enemy);
			boolean recalculationNeeded = mAbsorptionMobs.remove(enemy);
			if (recalculationNeeded) {
				recalculateKBR();
			}

			Location enemyLoc = enemy.getLocation();
			mCosmetic.onCounterStrike(mPlayer, enemy, enemyLoc);
		}

		return false;
	}

	private void prime(LivingEntity enemy) {
		mLastHurtTicks.put(enemy, Bukkit.getCurrentTick());
		if (isEnhanced() && mPlayer.getAbsorptionAmount() > 0) {
			mAbsorptionMobs.add(enemy);
			recalculateKBR();
		}
	}

	public void onTaunt(LivingEntity enemy) {
		mTauntedMobs.removeIf(e -> e.isDead() || !e.isValid());
		if (isLevelTwo() && !mTauntedMobs.contains(enemy)) {
			mTauntedMobs.add(enemy);
			prime(enemy);
		}
	}

	private void recalculateKBR() {
		mPlugin.mEffectManager.clearEffects(mPlayer, KBR_EFFECT);
		int currentTick = Bukkit.getCurrentTick();
		int duration = mAbsorptionMobs.stream().map(mLastHurtTicks::get).filter(Objects::nonNull).mapToInt(t -> t + mDuration - currentTick).max().orElse(0);
		if (duration > 0) {
			mPlugin.mEffectManager.addEffect(mPlayer, KBR_EFFECT, new PercentKnockbackResist(duration, mKBR, KBR_EFFECT));
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		mLastHurtTicks.keySet().removeIf(e -> e.isDead() || !e.isValid());
		int currentTick = Bukkit.getCurrentTick();
		mLastHurtTicks.values().removeIf(t -> currentTick - t > mDuration);
		boolean recalculationNeeded = mAbsorptionMobs.removeIf(e -> !mLastHurtTicks.containsKey(e));
		if (recalculationNeeded) {
			recalculateKBR();
		}

		mLastHurtTicks.keySet().forEach(e -> mCosmetic.onPrimedMobTick(mPlayer, e.getLocation()));
		mAbsorptionMobs.forEach(e -> mCosmetic.onAbsorptionMobTick(mPlayer, e.getLocation()));
	}
}
