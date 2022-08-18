package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class BlueAirBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_blueair";
	public static final int detectionRange = 20;

	public static final double[] SPAWN_CHANCE = {0, 0.1, 0.15, 0.2};

	private int mBlueTimeOfDay = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BlueAirBoss(plugin, boss);
	}

	public BlueAirBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Player nearestPlayer = EntityUtils.getNearestPlayer(boss.getLocation(), 30);
		mBlueTimeOfDay = ScoreboardUtils.getScoreboardValue(nearestPlayer, "BlueTimeOfDay").orElse(0);
		mBlueTimeOfDay = Math.min(3, Math.max(0, mBlueTimeOfDay));

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null, 100, 20);
	}

	@Override public void death(EntityDeathEvent event) {
		double chance = FastUtils.RANDOM.nextDouble();

		if (chance < SPAWN_CHANCE[mBlueTimeOfDay]) {
			Location loc = mBoss.getLocation();
			LibraryOfSoulsIntegration.summon(loc, "SpectreCloud");

			loc.add(0, 1, 0);
			new PartialParticle(Particle.CLOUD, loc, 50, 0.5, 0.5, 0.5, 0.5).spawnAsEnemy();
		}
	}
}
