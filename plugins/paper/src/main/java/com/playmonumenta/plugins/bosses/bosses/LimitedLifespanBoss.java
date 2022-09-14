package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class LimitedLifespanBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_limited_lifespan";

	public static class Parameters extends BossParameters {

		@BossParam(help = "Lifetime of the boss, in seconds")
		public int LIFETIME = 60;

		@BossParam(help = "Particles summoned when the boss dies due to running out of time")
		public ParticlesList PARTICLES = ParticlesList.fromString("[]");

		@BossParam(help = "Sounds played when the boss dies due to running out of time")
		public SoundsList SOUNDS = SoundsList.fromString("[]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new LimitedLifespanBoss(plugin, boss);
	}

	private BukkitTask mTask = null;

	public LimitedLifespanBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		mTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (boss.isValid()) {
				p.PARTICLES.spawn(boss, boss.getLocation());
				p.SOUNDS.play(boss.getLocation());
				boss.setHealth(0);
			}
		}, Math.max(0, p.LIFETIME * 20L - boss.getTicksLived()));
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), 0, null);
	}

	@Override
	public void unload() {
		super.unload();
		if (mTask != null) {
			mTask.cancel();
			mTask = null;
		}
	}
}
