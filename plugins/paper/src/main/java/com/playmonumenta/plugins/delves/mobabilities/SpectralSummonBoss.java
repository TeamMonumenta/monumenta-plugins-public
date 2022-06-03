package com.playmonumenta.plugins.delves.mobabilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class SpectralSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "Spectral";

	private static final String[] SPECTERS = {
		"SpecterofFury",
		"SpecterofSilence",
		"SpecterofIgnorance"
	};

	private static final String[] SPECTERS_WATER = {
		"LeviathanofSilence",
		"LeviathanofFury"
	};


	public static class Parameters extends BossParameters {
		public double SPAWN_CHANCE = 0;
	}

	final Parameters mParam;

	public SpectralSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		if (!EntityUtils.isElite(mBoss) && !DelvesUtils.isDelveMob(mBoss) && EntityUtils.isHostileMob(mBoss)) {
			double chance = FastUtils.RANDOM.nextDouble();
			if (chance < mParam.SPAWN_CHANCE) {
				Location loc = mBoss.getLocation();
				if (loc.getBlock().getType() == Material.WATER) {
					LibraryOfSoulsIntegration.summon(loc, SPECTERS_WATER[FastUtils.RANDOM.nextInt(SPECTERS_WATER.length)]);
				} else {
					LibraryOfSoulsIntegration.summon(loc, SPECTERS[FastUtils.RANDOM.nextInt(SPECTERS.length)]);
				}

				loc.add(0, 1, 0);
				new PartialParticle(Particle.SPELL_WITCH, loc, 50, 0, 0, 0, 0.5).spawnAsEnemy();
				new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0).spawnAsEnemy();
			}
		}
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SpectralSummonBoss(plugin, boss);
	}
}
