package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.CounterStrikeCS;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class CounterStrike extends Ability {
	private static final int DURATION = 4 * 20;
	private static final double DAMAGE_1 = 0.1;
	private static final double DAMAGE_2 = 0.2;
	private static final double RESISTANCE = 0.1;
	private static final double ABSORPTION_RESISTANCE = 0.125;
	private static final double KBR = 0.5;
	private static final String KBR_EFFECT = "CounterStrikeKBREffect";

	public static final String CHARM_DURATION = "Counter Strike Duration";
	public static final String CHARM_DAMAGE = "Counter Strike Damage";
	public static final String CHARM_DAMAGE_REDUCTION = "Counter Strike Damage Reduction";
	public static final String CHARM_ABSORPTION_DAMAGE_REDUCTION = "Counter Strike Absorption Damage Reduction";
	public static final String CHARM_KBR = "Counter Strike Knockback Resistance";
	public static final String CHARM_RADIUS = "Counter Strike Radius";

	private static final Style COUNTER_COLOR = Style.style(TextColor.color(0xF0A000));

	public static final AbilityInfo<CounterStrike> INFO =
		new AbilityInfo<>(CounterStrike.class, "Counter Strike", CounterStrike::new)
			.linkedSpell(ClassAbility.COUNTER_STRIKE)
			.scoreboardId("CounterStrike")
			.shorthandName("CS")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Deal extra damage to a mob after taking damage from them.")
			.displayItem(Material.CACTUS);

	private final Map<LivingEntity, Integer> mLastHurtTicks;
	private final List<LivingEntity> mTauntedMobs;
	private final List<LivingEntity> mAbsorptionMobs;
	private final int mDuration;
	private final double mDamage;
	private final double mKBR;
	private final double mResistance;
	private final double mAbsorptionResistance;

	private final CounterStrikeCS mCosmetic;

	public CounterStrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mLastHurtTicks = new HashMap<>();
		mTauntedMobs = new ArrayList<>();
		mAbsorptionMobs = new ArrayList<>();
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mDamage = (isLevelOne() ? DAMAGE_1 : DAMAGE_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mKBR = KBR + CharmManager.getLevel(mPlayer, CHARM_KBR) / 10;
		mResistance = RESISTANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_REDUCTION);
		mAbsorptionResistance = ABSORPTION_RESISTANCE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_REDUCTION) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ABSORPTION_DAMAGE_REDUCTION);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CounterStrikeCS());
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source != null) {
			if (isEnhanced() && mLastHurtTicks.containsKey(source)) {
				double resistance = (mAbsorptionMobs.contains(source) ? mAbsorptionResistance : mResistance);
				event.setFlatDamage(event.getDamage() * (1 - resistance));
			}


			prime(source);
			mCosmetic.onPrime(mPlayer, mPlayer.getLocation());
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MELEE && mLastHurtTicks.remove(enemy) != null) {

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
			mPlugin.mEffectManager.addEffect(mPlayer, KBR_EFFECT,
				new PercentKnockbackResist(duration, mKBR, KBR_EFFECT).deleteOnAbilityUpdate(true));
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

	private static Description<CounterStrike> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("When a mob attacks you, prepare a")
			.addLine("*Counter Strike* against them for %t.").styles(COUNTER_COLOR)
				.statValues(stat(a -> a.mDuration, DURATION))
			.addLine()
			.addLine("Attacking a mob with a *Counter Strike* will").styles(COUNTER_COLOR)
			.addLine("deal increased damage and remove it.")
			.addLine()
			.addStat("Damage Boost: +%p1 (m)")
				.statValues(stat(a -> a.mDamage, DAMAGE_1))
			.addDashedLine();
	}

	private static Description<CounterStrike> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Counter Strike*'s damage boost.").styles(UNDERLINED)
			.addLine()
			.addLine("Taunting a mob now primes a *Counter Strike*").styles(COUNTER_COLOR)
			.addLine("against them. (Max once per mob)")
			.addLine()
			.addStatComparison("Damage Boost: +%p1 -> +%p2 (m)")
				.statValues(stat(DAMAGE_1), stat(a -> a.mDamage, DAMAGE_2))
			.addDashedLine();
	}

	private static Description<CounterStrike> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("Take less damage from mobs that have a")
			.addLine("*Counter Strike* primed against them.").styles(COUNTER_COLOR)
			.addLine()
			.addStat("Effect: +%p Resistance")
				.statValues(stat(a -> a.mResistance, RESISTANCE))
			.addLine()
			.addLine("If that *Counter Strike* was primed").styles(COUNTER_COLOR)
			.addLine("while you had absorption, take even less")
			.addLine("damage and gain knockback resistance")
			.addLine("while it remains on the mob.")
			.addLine()
			.addStat("Effect: +%p Resistance")
				.statValues(stat(a -> a.mAbsorptionResistance, ABSORPTION_RESISTANCE))
			.addStat("Effect: +%p Knockback Resistance")
				.statValues(stat(a -> a.mKBR, KBR))
			.addDashedLine();
	}
}
