package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SizeDeathBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_size_death";

	public static class Parameters extends BossParameters {

		@BossParam(help = "If the size should increase or decrease")
		public boolean INCREASE = false;

		@BossParam(help = "Max size of the applicable entity before death")
		public int SIZE = 5;

		@BossParam(help = "The increase/decrease in size")
		public int CHANGER = 1;

		@BossParam(help = "How often (in ticks) the size changes")
		public int SPEED = 2;

		@BossParam(help = "Particles while the boss is changing size")
		public ParticlesList PARTICLES_SIZE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FLAME, 1, 0.0, 0.0, 0.0, 0.05))
			.build();

		@BossParam(help = "Particles when the boss is dead")
		public ParticlesList PARTICLES_DEATH = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FLAME, 1, 0.0, 0.0, 0.0, 0.05))
			.build();

		@BossParam(help = "Sound while the boss is changing size")
		public SoundsList SOUNDS_SIZE = SoundsList.EMPTY;

		@BossParam(help = "Sound when the boss is dead")
		public SoundsList SOUNDS_DEATH = SoundsList.EMPTY;

		public int DETECTION = 80;
	}

	private final SizeDeathBoss.Parameters mParams;

	public SizeDeathBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mParams = BossParameters.getParameters(mBoss, identityTag, new SizeDeathBoss.Parameters());
		mParams.SIZE = Math.max(0, mParams.SIZE);
		mParams.SPEED = Math.max(1, mParams.SPEED);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParams.DETECTION, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		int originalSize = EntityUtils.getSize(mBoss);
		if (mBoss instanceof Slime slime) {
			slime.setSize(mParams.SIZE);
			slime.remove();
		} else if (mBoss instanceof Phantom phantom) {
			phantom.setSize(mParams.SIZE);
			phantom.remove();
		} else {
			return;
		}
		LivingEntity clone = EntityUtils.copyMob(mBoss);
		EntityUtils.setSize(clone, originalSize);
		clone.setGravity(false);
		clone.setAI(false);
		clone.setSilent(true);
		clone.addScoreboardTag("SkillImmune");
		clone.setInvulnerable(true);
		clone.spawnAt(mBoss.getLocation());

		new BukkitRunnable() {
			int mCurrentSize = originalSize;

			@Override
			public void run() {
				if (clone.isDead() || !clone.isValid()) {
					this.cancel();
					return;
				}

				Location loc = LocationUtils.getHalfHeightLocation(clone);

				if (mParams.INCREASE ? mCurrentSize >= mParams.SIZE : mCurrentSize <= mParams.SIZE) {
					mParams.PARTICLES_DEATH.spawn(clone, loc);
					mParams.SOUNDS_DEATH.play(loc);
					clone.setHealth(0);
					return;
				}

				int sizeChange = mParams.INCREASE ? mParams.CHANGER : -mParams.CHANGER;
				mCurrentSize += sizeChange;

				if (mParams.INCREASE) {
					mCurrentSize = Math.min(mParams.SIZE, mCurrentSize);
				} else {
					mCurrentSize = Math.max(mParams.SIZE, mCurrentSize);
				}

				EntityUtils.setSize(clone, mCurrentSize);
				mParams.SOUNDS_SIZE.play(loc);
				mParams.PARTICLES_SIZE.spawn(clone, loc);
			}
		}.runTaskTimer(mPlugin, 0, mParams.SPEED);
	}
}
