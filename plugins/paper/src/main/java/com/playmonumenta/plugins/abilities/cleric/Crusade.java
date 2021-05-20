package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;

import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class Crusade extends Ability {
	public static final String NAME = "Crusade";

	public static final double DAMAGE_MULTIPLIER = 1d / 3;

	private final boolean mCountsHumanoids;

	public Crusade(
		@NotNull Plugin plugin,
		@NotNull Player player
	) {
		super(plugin, player, NAME);

		mInfo.mScoreboardId = NAME;
		mInfo.mShorthandName = "Crs";
		mInfo.mDescriptions.add(
			String.format(
				"Your abilities passively deal %s%% more combined damage to undead enemies.",
				StringUtils.multiplierToPercentage(DAMAGE_MULTIPLIER)
			)
		);
		mInfo.mDescriptions.add(
			"Your abilities that work against undead enemies now also work against humanoid enemies (illagers, vexes, golems, witches and giants)."
		); // List of humanoids hardcoded

		mCountsHumanoids = getAbilityScore() == 2;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent customDamageEvent) {
		//TODO pass in casted entities for events like these
		LivingEntity enemy = (LivingEntity)customDamageEvent.getDamaged();

		if (enemyTriggersAbilities(enemy)) {
			double originalDamage = customDamageEvent.getDamage();
			customDamageEvent.setDamage(
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
			).spawnAsPlayer(mPlayer);
		}
	}

	private boolean enemyTriggersAbilities(
		@NotNull LivingEntity enemy
	) {
		return enemyTriggersAbilities(enemy, this);
	}

	public static boolean enemyTriggersAbilities(
		@NotNull LivingEntity enemy,
		@Nullable Crusade crusade
	) {
		return (
			EntityUtils.isUndead(enemy)
			|| (
				crusade != null
				&& crusade.mCountsHumanoids
				&& EntityUtils.isHumanoid(enemy)
			)
		);
	}
}