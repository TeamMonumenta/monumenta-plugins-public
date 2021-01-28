package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.AntiRangeBoss;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class Chivalrous extends DelveModifier {

	private static final double[] SPAWN_CHANCE = {
			0.15,
			0.3,
			0.45
	};

	private static final String[] MOUNTS = {
			"SlimeMount",
			"MagmaCubeMount"
	};

	public static final String DESCRIPTION = "Enemies become Knights of Slime.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance to be Chivalrous."
			}, {
				"Enemies have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance to be Chivalrous."
			}, {
				"Enemies have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance to be Chivalrous."
			}
	};

	private final double mSpawnChance;

	public Chivalrous(Plugin plugin, Player player) {
		super(plugin, player, Modifier.CHIVALROUS);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.CHIVALROUS);
		mSpawnChance = SPAWN_CHANCE[rank - 1];
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (!mob.isInsideVehicle() && !(mob instanceof Flying) && !EntityUtils.isBoss(mob) && !DelvesUtils.isDelveMob(mob)
				&& FastUtils.RANDOM.nextDouble() < mSpawnChance) {
			Entity mount = LibraryOfSoulsIntegration.summon(mob.getLocation(), MOUNTS[FastUtils.RANDOM.nextInt(MOUNTS.length)]);
			mount.addPassenger(mob);
			mob.addScoreboardTag(AntiRangeBoss.identityTag);
		}
	}

}
