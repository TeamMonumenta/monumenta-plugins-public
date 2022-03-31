package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class SummonOnExplosionBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_summon_on_explode";

	public static class Parameters extends BossParameters {

		@BossParam(help = "Pool of mobs to summon")
		public LoSPool POOL = LoSPool.EMPTY;

		@BossParam(help = "Particles summon when the mob spawm")
		public ParticlesList PARTICLES = ParticlesList.fromString("[(SOUL_FIRE_FLAME,20,0.7,0.7,0.7,0.2)]");

		@BossParam(help = "Sounds summon when the mob spawm")
		public SoundsList SOUNDS = SoundsList.fromString("[(BLOCK_SOUL_SAND_FALL,2,0.5)]");

		@BossParam(help = "Delay for the mob spawned to get AI activated")
		public int MOB_AI_DELAY = 0;

		@BossParam(help = "if the mob spawned will have the same agro as the mob dead")
		public boolean AUTO_AGRO = true;

	}

	private final Parameters mParam;

	public SummonOnExplosionBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);

		if (!(boss instanceof Creeper creeper)) {
			throw new Exception("This boss ability can only be used on Creeper!");
		}
		mParam = BossParameters.getParameters(boss, identityTag, new Parameters());

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SummonOnExplosionBoss(plugin, boss);
	}

	@Override
	public void death(EntityDeathEvent event) {
		if (event == null) {
			//it exploded
			Entity entity = mParam.POOL.spawn(mBoss.getLocation());
			if (entity != null) {
				mParam.PARTICLES.spawn(mBoss.getLocation().clone().add(0, 0.5, 0));
				mParam.SOUNDS.play(mBoss.getLocation());
			}

			if (entity instanceof LivingEntity livingEntity) {
				livingEntity.setAI(false);
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					livingEntity.setAI(true);
					if (mParam.AUTO_AGRO && entity instanceof Mob newMob && mBoss instanceof Mob oldMob) {
						newMob.setTarget(oldMob.getTarget());
					}
				}, mParam.MOB_AI_DELAY);
			}
		}

	}
}
