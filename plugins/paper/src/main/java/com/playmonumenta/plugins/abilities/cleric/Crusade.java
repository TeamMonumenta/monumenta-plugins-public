package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import javax.annotation.Nullable;



public class Crusade extends Ability {
	public static final String NAME = "Crusade";

	public static final double DAMAGE_MULTIPLIER = 1d / 3;

	private final boolean mCountsHumanlikes;

	public Crusade(
		Plugin plugin,
		@Nullable Player player
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
			"Your abilities that work against undead enemies now also work against human-like enemies - illagers, vexes, witches, piglins, piglin brutes, golems and giants."
		); // List of human-likes hardcoded
		mDisplayItem = new ItemStack(Material.ZOMBIE_HEAD, 1);

		mCountsHumanlikes = getAbilityScore() == 2;
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null) {
			return;
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
			).spawnAsPlayer(mPlayer);
		}
	}

	private boolean enemyTriggersAbilities(
		LivingEntity enemy
	) {
		return enemyTriggersAbilities(enemy, this);
	}

	public static boolean enemyTriggersAbilities(
		LivingEntity enemy,
		@Nullable Crusade crusade
	) {
		return (
			EntityUtils.isUndead(enemy)
			|| (
				crusade != null
				&& crusade.mCountsHumanlikes
				&& EntityUtils.isHumanlike(enemy)
			)
		);
	}
}
