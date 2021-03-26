package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class Spectral extends DelveModifier {

	private static final double[] SPAWN_CHANCE = {
			0.07,
			0.14,
			0.21
	};

	private static final String[] SPECTERS = {
		"SpecterofFury",
		"SpecterofSilence"
	};

	public static final String DESCRIPTION = "Dying enemies transform into new enemies.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Dying Enemies have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance",
				"to spawn Specters."
			}, {
				"Dying Enemies have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance",
				"to spawn Specters."
			}, {
				"Dying Enemies have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance",
				"to spawn Specters."
			}
	};

	private final double mSpawnChance;

	public Spectral(Plugin plugin, Player player) {
		super(plugin, player, Modifier.SPECTRAL);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.SPECTRAL);
			mSpawnChance = SPAWN_CHANCE[rank - 1];
		} else {
			mSpawnChance = 0;
		}
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity mob = event.getEntity();

		if (!EntityUtils.isElite(mob) && !DelvesUtils.isDelveMob(mob)) {
			if (FastUtils.RANDOM.nextDouble() < mSpawnChance) {
				Location loc = mob.getLocation();
				World world = loc.getWorld();
				LibraryOfSoulsIntegration.summon(loc, SPECTERS[FastUtils.RANDOM.nextInt(SPECTERS.length)]);

				loc.add(0, 1, 0);
				world.spawnParticle(Particle.SPELL_WITCH, loc, 50, 0, 0, 0, 0.5);
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0);
			}
		}
	}

}
