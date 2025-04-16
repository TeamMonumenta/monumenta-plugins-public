package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.DepthsWinded;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Whirlwind extends DepthsAbility {
	public static final String ABILITY_NAME = "Whirlwind";
	private static final double[] COOLDOWN_REDUCTION = {0.05, 0.075, 0.1, 0.125, 0.15, 0.2};
	private static final double[] SPEED = {0.1, 0.125, 0.15, 0.175, 0.2, 0.3};
	private static final int COOLDOWN = 15 * 20;
	private static final int SPEED_DURATION = 6 * 20;
	private static final int AIRBORNE_THRESHOLD = 15;
	private static final String SPEED_EFFECT_NAME = "WhirlwindSpeedEffect";
	public static final String CHARM_COOLDOWN = "Whirlwind Cooldown";

	public static final DepthsAbilityInfo<Whirlwind> INFO =
		new DepthsAbilityInfo<>(Whirlwind.class, ABILITY_NAME, Whirlwind::new, DepthsTree.WINDWALKER, DepthsTrigger.WILDCARD)
			.linkedSpell(ClassAbility.WHIRLWIND)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.IRON_PICKAXE)
			.descriptions(Whirlwind::getDescription);

	private final double mCooldownReduction;
	private final double mSpeed;
	private final int mDuration;
	private final BukkitTask mAirborneTask;

	private int mAirborneTime;

	public Whirlwind(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCooldownReduction = COOLDOWN_REDUCTION[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.WHIRLWIND_COOLDOWN_REDUCTION.mEffectName);
		mSpeed = SPEED[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.WHIRLWIND_SPEED_AMPLIFIER.mEffectName);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.WHIRLWIND_SPEED_DURATION.mEffectName, SPEED_DURATION);
		mAirborneTask = new BukkitRunnable() {
			@Override
			public void run() {
				checkAirborne();
			}
		}.runTaskTimer(mPlugin, 0, 1);
		cancelOnDeath(mAirborneTask);
		mAirborneTime = 0;
	}

	@Override
	public void invalidate() {
		mAirborneTask.cancel();
	}

	private void checkAirborne() {
		if (isOnCooldown() || mPlugin.mEffectManager.hasEffect(mPlayer, DepthsWinded.class)) {
			return;
		}
		if (PlayerUtils.isOnGround(mPlayer)) {
			mAirborneTime = 0;
		} else {
			mAirborneTime++;
			if (mAirborneTime == AIRBORNE_THRESHOLD) {
				mPlugin.mEffectManager.addEffect(mPlayer, DepthsWinded.effectID, new DepthsWinded().deleteOnLogout(true).displaysTime(false));
				mAirborneTime = 0;

				mPlayer.playSound(mPlayer, Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.2f, 0.5f);
				new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 30, 1, 1, 1, 0.8).spawnAsPlayerActive(mPlayer);
			}
		}
	}

	public void trigger(Ability ability) {
		putOnCooldown();
		mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT_NAME, new PercentSpeed(mDuration, mSpeed, SPEED_EFFECT_NAME));
		mPlayer.playSound(mPlayer, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 0.5f, 1f);
		mPlayer.playSound(mPlayer, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1f, 1.7f);
		mPlayer.playSound(mPlayer, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 0.6f);

		// ability cdr
		AbilityInfo<?> ai = ability.getInfo();
		ClassAbility spell = ai.getLinkedSpell();
		if (!ai.hasCooldown() || spell == null) {
			return;
		}
		int cooldown = ability.getModifiedCooldown();
		mPlugin.mTimers.updateCooldown(mPlayer, spell, (int) (cooldown * mCooldownReduction));
	}

	private static Description<Whirlwind> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Being airborne for ")
			.add(StringUtils.ticksToSeconds(AIRBORNE_THRESHOLD))
			.add(" seconds will reduce your next casted ability's cooldown by ")
			.addPercent(a -> a.mCooldownReduction, COOLDOWN_REDUCTION[rarity - 1], false, true)
			.add(". Additionally, you receive ")
			.addPercent(a -> a.mSpeed, SPEED[rarity - 1], false, true)
			.add(" speed for ")
			.addDuration(a -> a.mDuration, SPEED_DURATION)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}
}

