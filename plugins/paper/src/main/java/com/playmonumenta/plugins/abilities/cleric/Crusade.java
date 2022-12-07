package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class Crusade extends Ability {
	public static final String NAME = "Crusade";

	public static final double DAMAGE_MULTIPLIER = 1d / 3;
	public static final double ENHANCEMENT_RADIUS = 8;
	public static final int ENHANCEMENT_MAX_MOBS = 6;
	public static final double ENHANCEMENT_BONUS_DAMAGE = 0.05;
	public static final int ENHANCEMENT_DURATION = 10 * 20;

	public static final AbilityInfo<Crusade> INFO =
		new AbilityInfo<>(Crusade.class, NAME, Crusade::new)
			.scoreboardId(NAME)
			.shorthandName("Crs")
			.descriptions(
				String.format(
					"Your abilities passively deal %s%% more combined damage to undead enemies.",
					StringUtils.multiplierToPercentage(DAMAGE_MULTIPLIER)
				),
				"Your abilities that work against undead enemies now also work against human-like enemies - illagers, vexes, witches, piglins, piglin brutes, golems and giants.",
				String.format("Gain %s%% ability damage for every mob affected by this ability within %s blocks, capping at %s mobs. " +
					              "Additionally, after damaged or debuffed by an active Cleric ability, monster-like mobs (affected by Slayer) will count as undead for the next 10s.",
					(int) (ENHANCEMENT_BONUS_DAMAGE * 100), ENHANCEMENT_RADIUS, ENHANCEMENT_MAX_MOBS)
			)
			.displayItem(new ItemStack(Material.ZOMBIE_HEAD, 1));

	private final boolean mCountsHumanlikes;

	public Crusade(Plugin plugin, @Nullable Player player) {
		super(plugin, player, INFO);

		mCountsHumanlikes = isLevelTwo();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null || event.getAbility().isFake()) {
			return false;
		}

		if (isEnhanced()) {
			long numMobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), ENHANCEMENT_RADIUS).stream()
				.filter(this::enemyTriggersAbilities)
				.filter(mob -> mob.getLocation().distanceSquared(mPlayer.getLocation()) <= ENHANCEMENT_RADIUS * ENHANCEMENT_RADIUS)
				.limit(ENHANCEMENT_MAX_MOBS)
				.count();
			event.setDamage(event.getDamage() * (1 + ENHANCEMENT_BONUS_DAMAGE * numMobs));
		}

		if (enemyTriggersAbilities(enemy)) {
			double originalDamage = event.getDamage();
			event.setDamage(
				originalDamage + (originalDamage * DAMAGE_MULTIPLIER)
			);

			double doubleWidthDelta = PartialParticle.getWidthDelta(enemy) * 2;
			new PartialParticle(
				Particle.CRIT_MAGIC,
				enemy.getEyeLocation(),
				10,
				doubleWidthDelta,
				PartialParticle.getHeightDelta(enemy),
				doubleWidthDelta,
				0
			).spawnAsPlayerActive(mPlayer);
		}
		return false; // only increases event damage
	}

	private boolean enemyTriggersAbilities(LivingEntity enemy) {
		return enemyTriggersAbilities(enemy, this);
	}

	public static boolean enemyTriggersAbilities(LivingEntity enemy, @Nullable Crusade crusade) {
		return EntityUtils.isUndead(enemy)
			       || (crusade != null && crusade.mCountsHumanlikes && EntityUtils.isHumanlike(enemy))
			       || Plugin.getInstance().mEffectManager.hasEffect(enemy, "CrusadeSlayerTag");
	}

	public static boolean applyCrusadeToSlayer(LivingEntity enemy, @Nullable Crusade crusade) {
		return crusade != null && crusade.isEnhanced() && EntityUtils.isBeast(enemy);
	}

	public static int getEnhancementDuration() {
		return ENHANCEMENT_DURATION;
	}
}
