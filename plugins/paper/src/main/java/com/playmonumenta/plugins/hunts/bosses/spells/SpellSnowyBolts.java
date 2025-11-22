package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.AlocAcoc;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellSnowyBolts extends Spell {
	public static final int COOLDOWN = 2 * 20;
	private static final int MAX_FLIGHT_TIME = 7 * 20;
	private static final float SPEED = 1.5f;
	private static final int DAMAGE = 65;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final AlocAcoc mAlocAcoc;
	private final PassivePolarAura mAura;

	public SpellSnowyBolts(Plugin plugin, LivingEntity boss, AlocAcoc alocAcoc, PassivePolarAura aura) {
		mPlugin = plugin;
		mBoss = boss;
		mAlocAcoc = alocAcoc;
		mAura = aura;
	}

	@Override
	public boolean canRun() {
		return mAlocAcoc.canRunSpell(this);
	}

	private void bullet(Player target) {
		Location mBulletLoc = mBoss.getLocation().clone();
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks == 0) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_DROWNED_SHOOT, 0.3f, 1);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_BREATHE, 2, 2);
				}
				Vector vec;
				if (mTicks < 10) {
					vec = new Vector(0, 1.5, 0);
				} else {
					vec = LocationUtils.getDirectionTo(target.getLocation(), mBulletLoc).multiply(SPEED);
					if (mTicks <= 20) {
						vec.add(new Vector(0, -0.15 * (mTicks - 9) + 1.5, 0));
					}
				}
				checkForCollision(mBulletLoc, vec, this);
				mBulletLoc.add(vec);
				new PartialParticle(Particle.CLOUD, mBulletLoc, 10).delta(0.2).spawnAsBoss();
				if (mBoss.isDead() || mTicks >= MAX_FLIGHT_TIME) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void run() {
		for (Player p : mAura.getPlayersInAura()) {
			bullet(p);
		}
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	private void checkForCollision(Location loc, Vector vec, BukkitRunnable runnable) {
		BoundingBox box = new BoundingBox().shift(loc).expand(1);
		Vector movement = vec.clone().multiply(0.5);
		for (int i = 0; i < 2; i++) {
			box.shift(movement);
			for (Player p : PlayerUtils.playersInRange(box.getCenter().toLocation(mBoss.getWorld()), 5, true)) {
				if (p.getBoundingBox().overlaps(box)) {
					DamageUtils.damage(mBoss, p, DamageEvent.DamageType.PROJECTILE, DAMAGE, null, false, true, "Snowy Bolts");
					mAura.addFrostbite(p, 0.175f);
					new PPExplosion(Particle.CLOUD, box.getCenter().toLocation(mBoss.getWorld())).spawnAsBoss();
					runnable.cancel();
					return;
				}
				if (loc.getBlock().getType().isSolid()) {
					new PPExplosion(Particle.CLOUD, box.getCenter().toLocation(mBoss.getWorld())).spawnAsBoss();
					runnable.cancel();
					return;
				}
			}
		}
	}
}
