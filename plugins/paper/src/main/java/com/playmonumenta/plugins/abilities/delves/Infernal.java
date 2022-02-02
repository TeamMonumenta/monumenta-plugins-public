package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.FireBombTossBoss;
import com.playmonumenta.plugins.bosses.bosses.FlameNovaBoss;
import com.playmonumenta.plugins.bosses.bosses.FlameTrailBoss;
import com.playmonumenta.plugins.bosses.bosses.SeekingProjectileBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;

import java.util.EnumSet;

public class Infernal extends DelveModifier {

	private static final EnumSet<DamageType> ENVIRONMENTAL_DAMAGE_CAUSES = EnumSet.of(
			DamageType.AILMENT,
			DamageType.FALL
	);

	private static final double[] ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER = {
			1.1,
			1.2,
			1.3,
			1.4,
			1.5,
			1.6,
			1.7
	};

	private static final double[] BURNING_DAMAGE_TAKEN_MULTIPLIER = {
			1.2,
			1.4,
			1.6,
			1.8,
			2,
			2.2,
			2.4
	};

	private static final double[] ABILITY_CHANCE = {
			0.06,
			0.12,
			0.18,
			0.24,
			0.3,
			0.36,
			0.42
	};

	private static final String[] ABILITY_POOL = {
		SeekingProjectileBoss.identityTag,
		FlameNovaBoss.identityTag,
		FlameTrailBoss.identityTag,
		FireBombTossBoss.identityTag
	};

	public static final String DESCRIPTION = "Enemies gain fiery abilities.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(ABILITY_CHANCE[0] * 100) + "% chance to be Infernal.",
				"Players take +" + Math.round((BURNING_DAMAGE_TAKEN_MULTIPLIER[0] - 1) * 100) + "% Burning Damage",
				"and +" + Math.round((ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER[0] - 1) * 100) + "% Environmental Damage."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[1] * 100) + "% chance to be Infernal.",
				"Players take +" + Math.round((BURNING_DAMAGE_TAKEN_MULTIPLIER[1] - 1) * 100) + "% Burning Damage",
				"and +" + Math.round((ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER[1] - 1) * 100) + "% Environmental Damage."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[2] * 100) + "% chance to be Infernal.",
				"Players take +" + Math.round((BURNING_DAMAGE_TAKEN_MULTIPLIER[2] - 1) * 100) + "% Burning Damage",
				"and +" + Math.round((ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER[2] - 1) * 100) + "% Environmental Damage."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[3] * 100) + "% chance to be Infernal.",
				"Players take +" + Math.round((BURNING_DAMAGE_TAKEN_MULTIPLIER[3] - 1) * 100) + "% Burning Damage",
				"and +" + Math.round((ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER[3] - 1) * 100) + "% Environmental Damage."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[4] * 100) + "% chance to be Infernal.",
				"Players take +" + Math.round((BURNING_DAMAGE_TAKEN_MULTIPLIER[4] - 1) * 100) + "% Burning Damage",
				"and +" + Math.round((ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER[4] - 1) * 100) + "% Environmental Damage."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[5] * 100) + "% chance to be Infernal.",
				"Players take +" + Math.round((BURNING_DAMAGE_TAKEN_MULTIPLIER[5] - 1) * 100) + "% Burning Damage",
				"and +" + Math.round((ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER[5] - 1) * 100) + "% Environmental Damage."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[6] * 100) + "% chance to be Infernal.",
				"Players take +" + Math.round((BURNING_DAMAGE_TAKEN_MULTIPLIER[6] - 1) * 100) + "% Burning Damage",
				"and +" + Math.round((ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER[6] - 1) * 100) + "% Environmental Damage."
			}
	};

	private final double mEnvironmentalDamageTakenMultiplier;
	private final double mBurningDamageTakenMultiplier;
	private final double mAbilityChance;

	public Infernal(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.INFERNAL);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.INFERNAL);
			mEnvironmentalDamageTakenMultiplier = ENVIRONMENTAL_DAMAGE_TAKEN_MULTIPLIER[rank - 1];
			mBurningDamageTakenMultiplier = BURNING_DAMAGE_TAKEN_MULTIPLIER[rank - 1];
			mAbilityChance = ABILITY_CHANCE[rank - 1];
		} else {
			mEnvironmentalDamageTakenMultiplier = 0;
			mBurningDamageTakenMultiplier = 0;
			mAbilityChance = 0;
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageType.FIRE) {
			event.setDamage(event.getDamage() * mBurningDamageTakenMultiplier);
		} else if (ENVIRONMENTAL_DAMAGE_CAUSES.contains(event.getType())) {
			event.setDamage(event.getDamage() * mEnvironmentalDamageTakenMultiplier);
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mAbilityChance) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			mob.addScoreboardTag(ABILITY_POOL[FastUtils.RANDOM.nextInt(ABILITY_POOL.length)]);
		}
	}

}
