package com.playmonumenta.plugins.bosses.spells.lich;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class SpellFinalHeatMech extends Spell {

	private Plugin mPlugin;
	private double mT = 20 * 7;
	private int mSoloCooldown = 20 * 29;
	private double mCooldown;
	private double mMaxFactor = 1.7;
	private Location mCenter;
	private double mRange;
	private LivingEntity mBoss;
	private boolean mTrigger = false;
	private boolean mDamage;
	private List<Player> mPlayers = new ArrayList<Player>();

	public SpellFinalHeatMech(Plugin plugin, LivingEntity boss, Location loc, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = range;
	}

	@Override
	public void run() {
		//update player count every 5 seconds
		if (!mTrigger) {
			mPlayers = Lich.playersInRange(mCenter, mRange, true);
			mTrigger = true;
			new BukkitRunnable() {

				@Override
				public void run() {
					mTrigger = false;
				}

			}.runTaskLater(mPlugin, 20 * 5);
		}
		//cooldown
		double cooldownFactor = Math.min(mMaxFactor, Math.sqrt(mPlayers.size()) / 5 + 0.8);
		mCooldown = mSoloCooldown / cooldownFactor;
		mT -= 5;
		if (mT <= 0) {
			mT += mCooldown;
			mDamage = false;
			World world = mBoss.getWorld();
			BukkitRunnable runA = new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 5.0f, 0.8f);
					if (mT >= 20) {
						this.cancel();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 5.0f, 1.0f);
						toss();
					}
					if (Lich.bossDead()) {
						this.cancel();
					}
					mT += 10;
				}

			};
			runA.runTaskTimer(mPlugin, 0, 10);
			mActiveRunnables.add(runA);
		}
	}

	private void toss() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 4, 1);
		List<Vector> vecs = new ArrayList<Vector>();
		vecs.add(new Vector(0.22, 1.0, 0.22));
		vecs.add(new Vector(0.27, 0.95, 0.55));
		vecs.add(new Vector(0.55, 0.95, 0.27));

		for (int i = 0; i < 4; i++) {
			List<Vector> vec = new ArrayList<Vector>();
			vec.addAll(vecs);

			//rotate vectors
			for (int j = 0; j < vec.size(); j++) {
				Vector v = vec.get(j);
				Vector v2 = VectorUtils.rotateYAxis(v, i * 90);
				vec.set(j, v2);
			}

			for (Vector v : vec) {
				VectorUtils.rotateYAxis(v, 90 * i);
				FallingBlock fallingBlock = world.spawnFallingBlock(mBoss.getLocation().add(0, 5, 0), Material.MAGMA_BLOCK.createBlockData());
				fallingBlock.setDropItem(false);
				fallingBlock.setVelocity(v);

				BukkitRunnable runB = new BukkitRunnable() {

					@Override
					public void run() {
						Location l = fallingBlock.getLocation();
						new PartialParticle(Particle.FLAME, l, 3, 0.25, .25, .25, 0.025).spawnAsBoss();
						new PartialParticle(Particle.DAMAGE_INDICATOR, mCenter.clone().add(0, 5.5, 0), 8, 10, 1, 10, 0).spawnAsBoss();
						if (fallingBlock.isOnGround() || !fallingBlock.isValid()) {
							this.cancel();
							fallingBlock.remove();
							fallingBlock.getLocation().getBlock().setType(Material.AIR);
							world.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 1);
							damage();
						}
						if (Lich.bossDead()) {
							this.cancel();
							fallingBlock.remove();
							fallingBlock.getLocation().getBlock().setType(Material.AIR);
						}
					}

				};
				runB.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(runB);
			}
		}
	}

	private void damage() {
		//only damage once for all 12 falling blocks
		if (!mDamage) {
			mDamage = true;
			Location loc = mCenter.clone().add(0, 5.5, 0);
			BoundingBox box = BoundingBox.of(loc, 22, 2, 22);
			List<Player> players = Lich.playersInRange(mCenter, mRange, true);
			for (Player p : players) {
				if (p.getBoundingBox().overlaps(box)) {
					BossUtils.bossDamage(mBoss, p, 75, null, "Malakut's Dynamo");
					MovementUtils.knockAway(mBoss, p, 0.5f);
					p.setFireTicks(100);
				}
			}
			new PartialParticle(Particle.EXPLOSION_LARGE, loc, 400, 10, 1, 10, 0).spawnAsBoss();
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
