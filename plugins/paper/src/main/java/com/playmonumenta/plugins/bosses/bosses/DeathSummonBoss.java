package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.SPAWNER_COUNT_METAKEY;

public class DeathSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_death_summon";

	public static class Parameters extends BossParameters {

		@BossParam(help = "Pool of mobs to summon (use this OR PARTY, not both)")
		public LoSPool POOL = LoSPool.LibraryPool.EMPTY;

		@BossParam(help = "LoS Party to summon (use this OR POOL, not both)")
		public LoSPool PARTY = LoSPool.LibraryPool.EMPTY;

		@BossParam(help = "Particles summon when the mob spawm")
		public ParticlesList PARTICLES = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SOUL_FIRE_FLAME, 20, 0.7, 0.7, 0.7, 0.2))
			.build();

		@BossParam(help = "Sounds summon when the mob spawm")
		public SoundsList SOUNDS = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_SOUL_SAND_FALL, 2.0f, 0.5f))
			.build();

		@BossParam(help = "Delay for the mob spawned to get AI activated")
		public int MOB_AI_DELAY = 10;

		@BossParam(help = "if the mob spawned will have the same agro as the mob dead")
		public boolean AUTO_AGGRO = true;

		@BossParam(help = "Number of mobs summoned if POOL is used, number of parties spawn if PARTY is used")
		public int MOB_COUNT = 1;

		@BossParam(help = "Minimum horizontal spawn velocity")
		public double H_MIN_VELOCITY = 0;

		@BossParam(help = "Maximum horizontal spawn velocity")
		public double H_MAX_VELOCITY = 0;

		@BossParam(help = "Minimum vertical spawn velocity")
		public double V_MIN_VELOCITY = 0;

		@BossParam(help = "Maximum vertical spawn velocity")
		public double V_MAX_VELOCITY = 0;

	}

	private final Parameters mParam;

	public DeathSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = BossParameters.getParameters(boss, identityTag, new Parameters());

		boolean poolSet = !mParam.POOL.toString().isEmpty();
		boolean partySet = !mParam.PARTY.toString().isEmpty();
		
		if (!poolSet && !partySet) {
			MMLog.severe("[DeathSummonBoss] Boss '" + boss.getName() + "' has neither POOL nor PARTY set! At least one must be configured.");
		} else if (poolSet && partySet) {
			MMLog.severe("[DeathSummonBoss] Boss '" + boss.getName() + "' has both POOL and PARTY set! Only one should be configured.");
		}

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		Location loc = mBoss.getLocation();
		mParam.PARTICLES.spawn(mBoss, loc.clone().add(0, 0.5, 0));
		mParam.SOUNDS.play(loc);
		Random r = new Random();
		
		boolean isParty = !mParam.PARTY.toString().isEmpty();
		
		for (int i = 0; i < mParam.MOB_COUNT; i++) {
			if (isParty) {
				// For parties, spawn all mobs from the party
				Map<Soul, Integer> partyMobs = LibraryOfSoulsIntegration.getPool(mParam.PARTY.toString());
				if (partyMobs != null) {
					for (Map.Entry<Soul, Integer> mobEntry : partyMobs.entrySet()) {
						Soul soul = mobEntry.getKey();
						int count = mobEntry.getValue();
						for (int j = 0; j < count; j++) {
							Entity entity = soul.summon(loc);
							applyEntitySettings(entity, r);
						}
					}
				}
			} else {
				// For pools, spawn one random mob from the pool
				Entity entity = mParam.POOL.spawn(loc);
				if (entity != null) {
					applyEntitySettings(entity, r);
				}
			}
		}
	}
	
	private void applyEntitySettings(Entity entity, Random r) {
		// Include the original mob's metadata for spawner counting to prevent mob farming
		if (entity != null && mBoss.hasMetadata(SPAWNER_COUNT_METAKEY)) {
			entity.setMetadata(SPAWNER_COUNT_METAKEY, mBoss.getMetadata(SPAWNER_COUNT_METAKEY).get(0));
		}

		//Applies a random velocity inside the given constraints to each entity when spawning
		//The velocity will be applied after the AI is returned as mobs with noAI are unaffected by velocity
		if (entity != null && (mParam.H_MAX_VELOCITY != 0 || mParam.H_MIN_VELOCITY != 0 || mParam.V_MIN_VELOCITY != 0 || mParam.V_MAX_VELOCITY != 0)) {
			entity.setVelocity(new Vector(
				r.nextDouble() * (mParam.H_MAX_VELOCITY - mParam.H_MIN_VELOCITY) + mParam.H_MIN_VELOCITY,
				r.nextDouble() * (mParam.V_MAX_VELOCITY - mParam.V_MIN_VELOCITY) + mParam.V_MIN_VELOCITY,
				r.nextDouble() * (mParam.H_MAX_VELOCITY - mParam.H_MIN_VELOCITY) + mParam.H_MIN_VELOCITY
			));
		}
		if (entity instanceof LivingEntity livingEntity) {
			livingEntity.setRotation(mBoss.getLocation().getYaw(), mBoss.getLocation().getPitch());
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
