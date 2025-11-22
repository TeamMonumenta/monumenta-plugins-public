package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class RebornBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_reborn";

	public static class Parameters extends BossParameters {
		public int DETECTION = 40;

		@BossParam(help = "How Many times this mob can reborn")
		public int REBORN_TIMES = 1;

		@BossParam(help = "% of the MaxHealth that the mob will have when reborn")
		public double REBORN_PERCENT_HEALTH = 0.5;

		@BossParam(help = "Whether the mob becomes invulnerable when reborn")
		public boolean IS_INVULNERABLE = false;

		@BossParam(help = "how long the reborn invulnerability lasts for, if applicable")
		public int INVULN_DURATION = 60;

		@BossParam(help = "sound played on reborn")
		public SoundsList SOUND_REBORN = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ITEM_TOTEM_USE, 1.0f, 1.0f))
			.build();

		@BossParam(help = "particles displayed on reborn")
		public ParticlesList PARTICLE_REBORN = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.TOTEM, 4, 0.0, 0.0, 0.0, 0.35))
			.build();
	}

	private final Parameters mParams;
	private int mTimesReborn = 0;
	public static final int detectionRange = 40;

	public RebornBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mParams.REBORN_TIMES > mTimesReborn && mBoss.getHealth() - event.getFinalDamage(true) <= 0) {
			mTimesReborn++;
			event.setFlatDamage(0.001);
			if (mParams.IS_INVULNERABLE) {
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, "REBORN_INVULN", new PercentDamageReceived(mParams.INVULN_DURATION, -4));
			}
			mParams.SOUND_REBORN.play(mBoss.getLocation());
			mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) * mParams.REBORN_PERCENT_HEALTH);
			mBoss.setFireTicks(-1);

			for (PotionEffect effect : mBoss.getActivePotionEffects()) {
				//INVISIBILITY effects should not get removed
				if (!effect.getType().equals(PotionEffectType.INVISIBILITY)) {
					mBoss.removePotionEffect(effect.getType());
				}
			}
			if (EntityUtils.isSlowed(com.playmonumenta.plugins.Plugin.getInstance(), mBoss)) {
				EntityUtils.setSlowTicks(com.playmonumenta.plugins.Plugin.getInstance(), mBoss, 0);
			}
			if (EntityUtils.isParalyzed(com.playmonumenta.plugins.Plugin.getInstance(), mBoss)) {
				EntityUtils.removeParalysis(com.playmonumenta.plugins.Plugin.getInstance(), mBoss);
			}
			if (EntityUtils.isStunned(mBoss)) {
				EntityUtils.removeStun(mBoss);
			}
			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					mT++;
					mParams.PARTICLE_REBORN.spawn(mBoss, mBoss.getEyeLocation());
					if (mT > 20 * 2) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}
}
