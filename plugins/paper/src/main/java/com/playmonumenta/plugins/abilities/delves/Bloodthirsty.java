package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CoordinatedAttackBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;

public class Bloodthirsty extends DelveModifier {

	private static final double[] BLOODTHIRSTY_CHANCE = {
			0.07,
			0.14,
			0.21,
			0.28,
			0.35
	};

	public static final String DESCRIPTION = "Enemies can coordinate attacks on players.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(BLOODTHIRSTY_CHANCE[0] * 100) + "% chance to be Bloodthirsty."
			}, {
				"Enemies have a " + Math.round(BLOODTHIRSTY_CHANCE[1] * 100) + "% chance to be Bloodthirsty."
			}, {
				"Enemies have a " + Math.round(BLOODTHIRSTY_CHANCE[2] * 100) + "% chance to be Bloodthirsty."
			}, {
				"Enemies have a " + Math.round(BLOODTHIRSTY_CHANCE[3] * 100) + "% chance to be Bloodthirsty."
			}, {
				"Enemies have a " + Math.round(BLOODTHIRSTY_CHANCE[4] * 100) + "% chance to be Bloodthirsty."
			}
	};

	private final double mBloodthirstyChance;

	public Bloodthirsty(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.BLOODTHIRSTY);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.BLOODTHIRSTY);
			mBloodthirstyChance = BLOODTHIRSTY_CHANCE[rank - 1];
		} else {
			mBloodthirstyChance = 0;
		}
	}

	@Override
	protected void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mBloodthirstyChance) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			mob.addScoreboardTag(CoordinatedAttackBoss.identityTag);
		}
	}

}
