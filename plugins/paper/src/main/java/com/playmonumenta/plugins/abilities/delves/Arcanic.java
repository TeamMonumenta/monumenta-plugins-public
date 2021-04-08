package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.ChargerStrongBoss;
import com.playmonumenta.plugins.bosses.bosses.MagicArrowBoss;
import com.playmonumenta.plugins.bosses.bosses.RejuvenationBoss;
import com.playmonumenta.plugins.bosses.bosses.TpBehindTargetedBoss;
import com.playmonumenta.plugins.bosses.bosses.TrackingProjectileBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class Arcanic extends DelveModifier {

	private static final double[] CUSTOM_DAMAGE_TAKEN_MULTIPLIER = {
			1.1,
			1.2,
			1.3,
			1.4,
			1.5
	};

	private static final double[] ABILITY_CHANCE = {
			0.06,
			0.12,
			0.18,
			0.24,
			0.3
	};

	private static final String[] ABILITY_POOL = {
		RejuvenationBoss.identityTag,
		TrackingProjectileBoss.identityTag,
		TpBehindTargetedBoss.identityTag,
		ChargerStrongBoss.identityTag,
		MagicArrowBoss.identityTag
	};

	public static final String DESCRIPTION = "Enemies gain magical abilities.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(ABILITY_CHANCE[0] * 100) + "% chance to be Arcanic.",
				"Enemies deal +" + Math.round((CUSTOM_DAMAGE_TAKEN_MULTIPLIER[0] - 1) * 100) + "% Ability Damage."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[1] * 100) + "% chance to be Arcanic.",
				"Enemies deal +" + Math.round((CUSTOM_DAMAGE_TAKEN_MULTIPLIER[1] - 1) * 100) + "% Ability Damage."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[2] * 100) + "% chance to be Arcanic.",
				"Enemies deal +" + Math.round((CUSTOM_DAMAGE_TAKEN_MULTIPLIER[2] - 1) * 100) + "% Ability Damage."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[3] * 100) + "% chance to be Arcanic.",
				"Enemies deal +" + Math.round((CUSTOM_DAMAGE_TAKEN_MULTIPLIER[3] - 1) * 100) + "% Ability Damage."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[4] * 100) + "% chance to be Arcanic.",
				"Enemies deal +" + Math.round((CUSTOM_DAMAGE_TAKEN_MULTIPLIER[4] - 1) * 100) + "% Ability Damage."
			}
	};

	private final double mCustomDamageTakenMultiplier;
	private final double mAbilityChance;

	public Arcanic(Plugin plugin, Player player) {
		super(plugin, player, Modifier.ARCANIC);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.ARCANIC);
			mCustomDamageTakenMultiplier = CUSTOM_DAMAGE_TAKEN_MULTIPLIER[rank - 1];
			mAbilityChance = ABILITY_CHANCE[rank - 1];
		} else {
			mCustomDamageTakenMultiplier = 0;
			mAbilityChance = 0;
		}
	}

	@Override
	public boolean playerTookCustomDamageEvent(EntityDamageByEntityEvent event) {
		event.setDamage(EntityUtils.getDamageApproximation(event, mCustomDamageTakenMultiplier));
		return true;
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mAbilityChance) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			mob.addScoreboardTag(ABILITY_POOL[FastUtils.RANDOM.nextInt(ABILITY_POOL.length)]);
		}
	}

}
