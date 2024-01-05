package com.playmonumenta.plugins.bosses.spells.sirius.miniboss;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.sirius.PassiveStarBlightConversion;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellSwordCleave extends Spell {
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private PassiveStarBlightConversion mConverter;
	private boolean mOnCooldown;
	private static final float SWINGRADIUS = 5;
	private static final int COOLDOWN = 7 * 20;
	private static final int SWINGDELAY = 2 * 20;
	private static final int DAMAGE = 65;

	public SpellSwordCleave(Plugin plugin, PassiveStarBlightConversion converter, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mConverter = converter;
		mOnCooldown = false;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
		EntityUtils.selfRoot(mBoss, 26);
		new BukkitRunnable() {
			Vector mDirection = mBoss.getEyeLocation().getDirection().normalize();
			int mTicks = 0;
			float mAngle = (float) (-FastMath.atan2(mDirection.getX(), mDirection.getZ()) - FastMath.PI / 4.0f);

			@Override
			public void run() {
				if (mTicks == SWINGDELAY) {

					List<BoundingBox> boxes = new ArrayList<>();
					for (double theta = mAngle; theta < mAngle + FastMath.PI / 2.0; theta = (theta + (FastMath.PI / 2.0) / 10)) {
						for (int r = 0; r <= SWINGRADIUS; r++) {
							Location loc = mBoss.getLocation().add(-r * FastMath.sin(theta), 0.25, r * FastMath.cos(theta));
							mConverter.convertColumn(loc.getX(), loc.getZ());
							BoundingBox mHitBox = BoundingBox.of(loc, 0.5, 1.0, 0.5);
							boxes.add(mHitBox);
							new PPExplosion(Particle.SWEEP_ATTACK, loc).count(2).delta(0.1).spawnAsBoss();
						}
					}
					List<Player> mPlayers = PlayerUtils.playersInRange(mBoss.getLocation(), SWINGRADIUS + 1, true, true);
					for (BoundingBox box : boxes) {
						for (Player p : mPlayers) {
							if (p.getBoundingBox().overlaps(box)) {
								DamageUtils.damage(null, p, DamageEvent.DamageType.MELEE, DAMAGE, null, false, true, "Blighted Slash");
							}
						}
						new PPExplosion(Particle.SCRAPE, box.getCenter().toLocation(mBoss.getWorld())).count(2).delta(0.25).spawnAsBoss();
					}
					this.cancel();
				} else {
					for (double theta = mAngle; theta < mAngle + FastMath.PI / 2.0; theta = (theta + (FastMath.PI / 2.0) / 10)) {
						for (int r = 0; r <= SWINGRADIUS; r++) {
							new PartialParticle(Particle.SCRAPE, mBoss.getLocation().add(-r * FastMath.sin(theta), 0.25, r * FastMath.cos(theta))).count(1).spawnAsBoss();
						}
					}
				}
				if (mBoss.isDead()) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}


}
