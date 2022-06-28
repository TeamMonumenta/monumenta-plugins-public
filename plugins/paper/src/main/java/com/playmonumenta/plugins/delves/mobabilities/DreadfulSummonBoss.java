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
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

public class DreadfulSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "Dreadful";

	private static final String[] DREADNAUGHTS = {
		"DreadnaughtofDoom",
		"DreadnaughtofSorrow",
		"DreadnaughtofSubjugation"
	};

	private static final String DREADNAUGHT_WATER = "LeviathanofDoom";

	public static class Parameters extends BossParameters {
		public double SPAWN_CHANCE = 0;
	}

	final Parameters mParam;


	public DreadfulSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		if (event != null && EntityUtils.isElite(mBoss) && !DelvesUtils.isDelveMob(mBoss)) {
			if (FastUtils.RANDOM.nextDouble() < mParam.SPAWN_CHANCE) {
				Location loc = mBoss.getLocation();
				boolean isWaterLoc = LocationUtils.containsWater(loc.getBlock());
				if (isWaterLoc) {
					LibraryOfSoulsIntegration.summon(loc, DREADNAUGHT_WATER);
				} else {
					LibraryOfSoulsIntegration.summon(loc, DREADNAUGHTS[FastUtils.RANDOM.nextInt(DREADNAUGHTS.length)]);
				}

				loc.add(0, 1, 0);
				new PartialParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.1).spawnAsEnemy();
				new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0).spawnAsEnemy();
			}
		}
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DreadfulSummonBoss(plugin, boss);
	}
}
