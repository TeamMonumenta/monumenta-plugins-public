package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellInspire;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class CommanderBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_commander";

	public static class Parameters extends BossParameters {
		public int DETECTION = 24;
		public int RANGE = 8;
	}

	private final Parameters mParams;
	boolean mSummonedReinforcements = false;

	public CommanderBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		List<Spell> passiveSpells = List.of(
			new SpellInspire(com.playmonumenta.plugins.Plugin.getInstance(), boss, mParams.RANGE)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, mParams.DETECTION, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (!mSummonedReinforcements && mBoss.getHealth() < EntityUtils.getMaxHealth(mBoss) / 2) {
			mSummonedReinforcements = true;

			World world = mBoss.getWorld();
			Location loc = mBoss.getLocation();
			world.playSound(loc, Sound.ENTITY_HORSE_ANGRY, SoundCategory.HOSTILE, 1f, 2f);
			world.playSound(loc, Sound.ENTITY_HORSE_DEATH, SoundCategory.HOSTILE, 1f, 0.5f);
			world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1f, 0.75f);
			world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 1f, 1f);
			new BukkitRunnable() {

				double mRadius = 0;
				final Location mL = loc.clone().add(0, 0.25, 0);
				final double RADIUS = 5;

				@Override
				public void run() {

					for (int i = 0; i < 2; i++) {
						mRadius += 0.33;
						for (int degree = 0; degree < 360; degree += 5) {
							double radian = FastMath.toRadians(degree);
							Vector vec = new Vector(FastUtils.cos(radian) * mRadius, 0,
								FastUtils.sin(radian) * mRadius);
							Location loc = mL.clone().add(vec);
							new PartialParticle(Particle.DUST_COLOR_TRANSITION, loc, 1, 0, 0, 0, 0,
								new Particle.DustTransition(
									ParticleUtils.getTransition(Color.fromRGB(226, 255, 156), Color.fromRGB(255, 156, 240), mRadius / RADIUS),
									ParticleUtils.getTransition(Color.fromRGB(37, 246, 245), Color.fromRGB(37, 246, 245), mRadius / RADIUS),
									1f
								)).spawnAsEntityActive(mBoss);
						}
					}

					if (mRadius >= RADIUS) {
						this.cancel();
					}
				}

			}.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 0, 1);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(mBoss.getLocation(), mParams.RANGE, mBoss)) {
				if (!EntityUtils.isBoss(mob)) {
					DelvesUtils.duplicateLibraryOfSoulsMob(mob);
				}
			}
		}
	}

}
