package com.playmonumenta.plugins.abilities.alchemist;

import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class BasiliskPoison extends Ability {

	private static final double BASILISK_POISON_1_PERCENT_DAMAGE = 0.05;
	private static final double BASILISK_POISON_2_PERCENT_DAMAGE = 0.08;
	private static final int BASILISK_POISON_DURATION = 6 * 20;

	private final double mPercent;

	public BasiliskPoison(Plugin plugin, Player player) {
		super(plugin, player, "Basilisk Poison");
		mInfo.mScoreboardId = "BasiliskPoison";
		mInfo.mShorthandName = "BP";
		mInfo.mDescriptions.add("Equips your arrows with a damage over time that deals 5% of your bow shot every 1s for 6s.");
		mInfo.mDescriptions.add("Damage over time is improved to 8%");
		mPercent = getAbilityScore() == 1 ? BASILISK_POISON_1_PERCENT_DAMAGE : BASILISK_POISON_2_PERCENT_DAMAGE;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (proj instanceof Arrow || proj instanceof SpectralArrow) {
			applyPoison(damagee, event);
			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.TOTEM, damagee.getLocation().add(0, 1.6, 0), 12, 0.4, 0.4, 0.4, 0.1);
			world.playSound(damagee.getLocation(), Sound.ENTITY_CREEPER_HURT, 1, 1.6f);
		}

		return true;
	}

	public void applyPoison(LivingEntity entity, EntityDamageByEntityEvent event) {
		new BukkitRunnable() {
			private int mTimer = 0;
			World mWorld = mPlayer.getWorld();

			@Override
			public void run() {
				if (mTimer >= BASILISK_POISON_DURATION || entity.isDead()) {
					this.cancel();
				}

				mTimer += 1;
				if (mTimer % 20 == 0) {
					EntityUtils.damageEntity(mPlugin, entity, event.getDamage() * mPercent, mPlayer);
					mWorld.spawnParticle(Particle.TOTEM, event.getEntity().getLocation().add(0, 1.6, 0), 7, 0.4, 0.4, 0.4, 0.1);
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.TOTEM);
		return true;
	}
}
