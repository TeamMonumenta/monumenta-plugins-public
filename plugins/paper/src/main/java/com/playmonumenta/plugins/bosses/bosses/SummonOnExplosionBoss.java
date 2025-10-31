package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import java.util.Collections;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.SPAWNER_COUNT_METAKEY;

public class SummonOnExplosionBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_summon_on_explode";

	public static class Parameters extends BossParameters {

		@BossParam(help = "Pool of mobs to summon")
		public LoSPool POOL = LoSPool.LibraryPool.EMPTY;

		@BossParam(help = "Particles summon when the mob spawm")
		public ParticlesList PARTICLES = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SOUL_FIRE_FLAME, 20, 0.7, 0.7, 0.7, 0.2))
			.build();

		@BossParam(help = "Sounds summon when the mob spawm")
		public SoundsList SOUNDS = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_SOUL_SAND_FALL, 2.0f, 0.5f))
			.build();

		@BossParam(help = "Delay for the mob spawned to get AI activated")
		public int MOB_AI_DELAY = 0;

		@BossParam(help = "if the mob spawned will have the same agro as the mob dead")
		public boolean AUTO_AGGRO = true;

		@BossParam(help = "Number of mobs summoned")
		public int MOB_COUNT = 1;

		@BossParam(help = "Damage % transferred to spawn")
		public double TRANSFER = 0;

	}

	private final Parameters mParam;

	public SummonOnExplosionBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);

		if (!(boss instanceof Creeper)) {
			throw new Exception(identityTag + " only works on creepers! Entity name='" + boss.getName() + "', tags=[" + String.join(",", boss.getScoreboardTags()) + "]");
		}
		mParam = BossParameters.getParameters(boss, identityTag, new Parameters());

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event == null) {
			//it exploded
			mParam.PARTICLES.spawn(mBoss, mBoss.getLocation().clone().add(0, 0.5, 0));
			mParam.SOUNDS.play(mBoss.getLocation());
			double health = mBoss.getHealth();
			double maxHealth = Objects.requireNonNull(mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();
			double hpPercent = health / maxHealth;
			for (int i = 0; i < mParam.MOB_COUNT; i++) {
				Entity entity = mParam.POOL.spawn(mBoss.getLocation());

				// Include the original mob's metadata for spawner counting to prevent mob farming
				if (entity != null && mBoss.hasMetadata(SPAWNER_COUNT_METAKEY)) {
					entity.setMetadata(SPAWNER_COUNT_METAKEY, mBoss.getMetadata(SPAWNER_COUNT_METAKEY).get(0));
				}

				if (entity instanceof LivingEntity livingEntity) {
					//Decrease its percent hp by the percent health the spawner is missing times the transfer amount.
					livingEntity.setHealth(
						(1 - mParam.TRANSFER) * (livingEntity.getHealth() - hpPercent * livingEntity.getHealth())
							+ hpPercent * livingEntity.getHealth());
					livingEntity.setAI(false);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						livingEntity.setAI(true);
						if (mParam.AUTO_AGGRO && entity instanceof Mob newMob && mBoss instanceof Mob oldMob) {
							newMob.setTarget(oldMob.getTarget());
						}
					}, mParam.MOB_AI_DELAY);
				}
			}
		}
	}
}
