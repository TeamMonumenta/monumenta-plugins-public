package com.playmonumenta.plugins.bosses.spells.sirius.miniboss;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlightConversion;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellRadiance extends Spell {
	private PassiveStarBlightConversion mConverter;
	private LivingEntity mBoss;
	private static final int RADIUS = 5;
	private static final int DAMAGE = 20;
	private static final int IRRADIATECOUNT = 3;

	public SpellRadiance(PassiveStarBlightConversion converter, LivingEntity entity) {
		mConverter = converter;
		mBoss = entity;
	}

	@Override
	public void run() {
		new PPCircle(Particle.REDSTONE, mBoss.getLocation(), RADIUS).ringMode(true).data(new Particle.DustOptions(Color.fromRGB(3, 135, 126), 1.0f)).count(40).spawnAsBoss();
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_AMBIENT, SoundCategory.HOSTILE, 0.8f, 0.7f);
		for (int i = 0; i < IRRADIATECOUNT; i++) {
			Location mIrradatiate = getNewLoc(0);
			mConverter.convertColumn(mIrradatiate.x(), mIrradatiate.z());
			for (int y = -RADIUS; y < RADIUS; y++) {
				new PPExplosion(Particle.SCRAPE,
					new Location(mBoss.getWorld(), mIrradatiate.getX(), mBoss.getLocation().add(0, y, 0).getY(), mIrradatiate.getZ())).count(10).delta(0.25).spawnAsBoss();
			}
		}
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 0.2f);
		for (Player p : PlayerUtils.playersInRange(mBoss.getLocation(), RADIUS, false, true)) {
			DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false, "Starblight Radiance");
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private Location getNewLoc(int attempts) {
		if (attempts > 20) {
			return mBoss.getLocation();
		}
		int x = FastUtils.randomIntInRange(-RADIUS, RADIUS);
		int z = (int) Math.sqrt(RADIUS * RADIUS - x * x);
		if (FastUtils.randomIntInRange(-1, 0) == 0) {
			z *= -1;
		}
		int mRealX = (int) Math.abs(mConverter.mCornerOne.getX() - mBoss.getLocation().getX() - x);
		int mRealZ = (int) Math.abs(mConverter.mCornerOne.getZ() - mBoss.getLocation().getZ());
		Location mIrradiate = mBoss.getLocation().add(x, 0, z);
		if (!mConverter.mBlighted[mRealX][mRealZ]) {
			attempts++;
			return getNewLoc(attempts);
		} else {
			mIrradiate.set(mIrradiate.getX(), mBoss.getLocation().getY(), mIrradiate.getZ());
			return mIrradiate;
		}
	}
}
