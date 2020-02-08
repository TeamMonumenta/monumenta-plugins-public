package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellWindWalk extends Spell {
	private Plugin mPlugin;
	private LivingEntity mCaster;

	public SpellWindWalk(Plugin plugin, LivingEntity caster) {
		mPlugin = plugin;
		mCaster = caster;
	}

	@Override
	public void run() {
		World world = mCaster.getWorld();
		mCaster.removePotionEffect(PotionEffectType.SLOW);
		mCaster.removePotionEffect(PotionEffectType.WEAKNESS);
		mCaster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 8, 0));
		world.playSound(mCaster.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2, 1.75f);
		world.playSound(mCaster.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 2, 1f);
		world.spawnParticle(Particle.SMOKE_NORMAL, mCaster.getLocation(), 90, 0.25, 0.45, 0.25, 0.1);
		world.spawnParticle(Particle.CLOUD, mCaster.getLocation(), 20, 0.25, 0.45, 0.25, 0.15);
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks++;
				world.spawnParticle(Particle.EXPLOSION_NORMAL, mCaster.getLocation(), 2, 0.3, 0, 0.3, 0);
				if (mTicks >= 20 * 8 || mCaster.isDead() || !mCaster.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int duration() {
		return 20 * 10;
	}

}
