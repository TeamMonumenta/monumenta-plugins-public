package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class SpellTotemPlatformLaunchAds extends Spell {
	private final LivingEntity mBoss;
	private final int mRadius;
	private final ArmorStand mCenterStand;
	private final int mCooldown;
	private final boolean mLifeOrDeath;

	public SpellTotemPlatformLaunchAds(LivingEntity boss, int radius, ArmorStand centerStand, int cooldown, boolean lifeOrDeath) {
		mBoss = boss;
		mRadius = radius;
		mCenterStand = centerStand;
		mCooldown = cooldown;
		mLifeOrDeath = lifeOrDeath;
	}

	@Override
	public void run() {
		if (mCenterStand.getScoreboardTags().contains("Hycenea_Totem_NoThrow")) {
			return;
		}

		List<LivingEntity> nearby = EntityUtils.getNearbyMobs(mBoss.getLocation(), mRadius, mBoss).stream()
			.filter((mob) -> !mob.getScoreboardTags().contains("Boss") && mob.hasAI())
			.toList();

		if (!nearby.isEmpty()) {
			new PPExplosion(Particle.REDSTONE, mBoss.getLocation())
				.count(100)
				.data(new Particle.DustOptions(mLifeOrDeath ? Color.fromRGB(0, 204, 0) : Color.fromRGB(153, 76, 37), 1.65f))
				.delta(3)
				.spawnAsBoss();

			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 1f, 1f);

			for (Entity mob : nearby) {
				mob.setVelocity(LocationUtils.getDirectionTo(mCenterStand.getLocation(), mob.getLocation()).normalize().multiply(3).add(new Vector(0, 0.6, 0)));
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
