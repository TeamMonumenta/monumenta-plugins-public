package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collections;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

public class RebornBoss extends BossAbilityGroup {
	private boolean mActivated = false;
	public static final String identityTag = "boss_reborn";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new RebornBoss(plugin, boss);
	}

	public RebornBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (!mActivated && mBoss.getHealth() - event.getFinalDamage(true) <= 0) {
			mActivated = true;
			World world = mBoss.getWorld();
			event.setCancelled(true);
			event.setDamage(0);
			world.playSound(mBoss.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1);
			mBoss.setHealth(EntityUtils.getMaxHealth(mBoss) / 2);
			mBoss.setFireTicks(-1);
			for (PotionEffect effect : mBoss.getActivePotionEffects()) {
				mBoss.removePotionEffect(effect.getType());
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
					world.spawnParticle(Particle.TOTEM, mBoss.getEyeLocation(), 4, 0, 0, 0, 0.35);
					if (mT > 20 * 2) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}
}

