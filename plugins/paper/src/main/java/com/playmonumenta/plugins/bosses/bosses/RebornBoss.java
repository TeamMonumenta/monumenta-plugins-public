package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
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
	}

	private final Parameters mParams;
	private int mTimesReborn = 0;
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new RebornBoss(plugin, boss);
	}

	public RebornBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mParams.REBORN_TIMES > mTimesReborn && mBoss.getHealth() - event.getFinalDamage(true) <= 0) {
			mTimesReborn++;
			World world = mBoss.getWorld();
			event.setCancelled(true);
			event.setDamage(0);
			world.playSound(mBoss.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 1, 1);
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
					new PartialParticle(Particle.TOTEM, mBoss.getEyeLocation(), 4, 0, 0, 0, 0.35).spawnAsEntityActive(mBoss);
					if (mT > 20 * 2) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}
}

