package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.OmenBoss;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellOmen extends Spell {
	protected final OmenBoss.Parameters mP;
	protected final LivingEntity mBoss;
	protected final Plugin mPlugin;

	public SpellOmen(Plugin plugin, LivingEntity boss, OmenBoss.Parameters p) {
		mP = p;
		mBoss = boss;
		mPlugin = plugin;
	}

	@Override
	public void run() {
		mP.SOUND_WARN.play(mBoss.getLocation());
		List<? extends LivingEntity> targets = mP.TARGETS.getTargetsList(mBoss);
		if (!targets.isEmpty()) {
			Location targetLoc = targets.get(0).getLocation();
			targetLoc.setY(mBoss.getLocation().getY());
			if (mP.DO_TARGETING) {
				Vector targetDirection = targetLoc.toVector().setY(mBoss.getLocation().getY()).subtract(mBoss.getLocation().toVector());
				double[] targetYawPitch = VectorUtils.vectorToRotation(targetDirection);
				launchOmen(targetYawPitch[0]);
			} else {
				launchOmen(0);
			}


		}
	}

	public void launchOmen(double yaw) {
		new BukkitRunnable() {
			double mT = 0.0;
			final List<Vector> mBasevec = new ArrayList<>();

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				if ((!mP.INSTANT && mT == 0) || (mP.INSTANT && mT % mP.TEL_INTERVAL == 0 && mT < mP.TEL_DURATION)) {
					mBasevec.add(new Vector(0, 0, 1 + mP.BLADE_HEAD_OFFSET));
					mBasevec.add(new Vector(-4, 0, -2 - mP.BLADE_TAIL_OFFSET).normalize());
					mBasevec.add(new Vector(4, 0, -2 - mP.BLADE_TAIL_OFFSET).normalize());
					launchBlade(mBasevec, true, yaw);
				}
				mT++;
				//4 points swirl into center
				if (mT < mP.TEL_DURATION) {
					Vector dir = new Vector(4, 0, 0);
					mP.SOUND_TEL.play(mBoss.getLocation());
					for (int i = 0; i < mP.SPLITS; i++) {
						Vector shape = VectorUtils.rotateYAxis(dir.clone(), (i * (double) 360 / mP.SPLITS) + mP.DEGREE_OFFSET);
						Vector shapeLength = shape.multiply(1 - mT / mP.TEL_DURATION);
						Vector shapeDir = VectorUtils.rotateYAxis(shapeLength.clone(), ((double) 360 / mP.SPLITS) / (mT / mP.TEL_DURATION));
						Location l = mBoss.getLocation().add(shapeDir).add(0, 0.5, 0);
						mP.PARTICLE_TEL_SWIRL.spawn(mBoss, l);
					}
				}

				//blade function
				if (mT >= mP.TEL_DURATION) {
					this.cancel();
					mP.SOUND_LAUNCH.play(mBoss.getLocation());
					launchBlade(mBasevec, false, yaw);

				}
				if (!mP.CAN_MOVE) {
					EntityUtils.selfRoot(mBoss, mP.TEL_DURATION);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	public void launchBlade(List<Vector> basevec, boolean warning, double yaw) {
		Set<Player> hitPlayers = new HashSet<>();
		//loop for each direction, starting +Z, clockwise
		for (int i = 0; i < mP.SPLITS; i++) {
			List<Vector> vec = new ArrayList<>(basevec);

			//rotate vectors
			for (int j = 0; j < vec.size(); j++) {
				Vector v = vec.get(j);
				Vector v2 = VectorUtils.rotateYAxis(v, (i * mP.SPLIT_ANGLE) + mP.DEGREE_OFFSET + yaw);
				vec.set(j, v2);
			}

			//spawn loc shift up by y offset
			Vector dir = vec.get(0);

			Location startLoc = mBoss.getLocation().add(0, mP.HEIGHT_OFFSET, 0).add(dir.clone().multiply(mP.SAFESPOT_SIZE));
			//launch blade tip
			if (mP.INSTANT) {
				int mT = 0;
				moveBlade(mT, startLoc, dir, warning, vec, hitPlayers);
			} else {
				new BukkitRunnable() {
					int mT = 0;

					@Override
					public void run() {
						mT++;
						Location anchor;
						//iterate twice for higher accuracy
						for (int x = 0; x < 2; x++) {
							anchor = startLoc.clone().add(dir.clone().multiply(mP.VELOCITY / 20 * (mT + 0.5 * x)));
							if (anchor.distance(startLoc) > mP.MAX_RANGE) {
								this.cancel();
							}
							//play sounds
							if (!warning && mT % 4 == 0) {
								mP.SOUND_WAVE.play(anchor);
							}
							//construct blade
							createBlade(anchor, startLoc, vec, warning, hitPlayers);
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);
			}

		}
	}

	public void moveBlade(int mT, Location startLoc, Vector dir, boolean warning, List<Vector> vec, Collection<Player> hitPlayers) {
		Location anchor = startLoc.clone();
		while (anchor.distance(startLoc) < mP.MAX_RANGE) {
			mT++;
			//iterate twice for higher accuracy
			for (int x = 0; x < 2; x++) {
				anchor = startLoc.clone().add(dir.clone().multiply(mP.VELOCITY / 20 * (mT + 0.5 * x)));
				//play sounds
				if (mT % 4 == 0) {
					mP.SOUND_WAVE.play(anchor);
				}
				//construct blade
				createBlade(anchor, startLoc, vec, warning, hitPlayers);
			}
		}
	}

	public void createBlade(Location startLoc, Location origin, List<Vector> vec, boolean warning, Collection<Player> hitPlayers) {
		List<Location> locAll = new ArrayList<>();
		//construct blade
		for (int j = 1; j <= 2; j++) {
			Vector v = vec.get(j);
			//for 1 side, 5 locations
			for (int k = 0; k < mP.WIDTH; k++) {
				if (k % mP.PARTICLE_GAP == 0 && !warning) {
					Location l = startLoc.clone();
					l.add(v.clone().multiply(0.25 * k));
					if (!locAll.contains(l)) {
						locAll.add(l);
					}
				}
				if (k % mP.PARTICLE_GAP_TEL == 0 && warning) {
					Location l = startLoc.clone();
					l.add(v.clone().multiply(0.25 * k));
					if (!locAll.contains(l)) {
						locAll.add(l);
					}
				}
			}
		}
		//spawn particle + check loc
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mP.MAX_RANGE, true);
		List<Player> damage = new ArrayList<>();
		for (Location l : locAll) {
			if (warning) {
				mP.PARTICLE_TEL.spawn(mBoss, l);
			} else {
				mP.PARTICLE_OMEN.spawn(mBoss, l);
				BoundingBox box = BoundingBox.of(l, 0.3, mP.HITBOX_HEIGHT, 0.3);
				for (Player p : players) {
					if (p.getBoundingBox().overlaps(box) && !damage.contains(p)) {
						damage.add(p);
					}
				}
			}
		}
		//damage
		if (!warning) {
			for (Player player : damage) {
				mP.EFFECTS.apply(player, mBoss);
				if (mP.DAMAGE > 0 || mP.DAMAGE_PERCENTAGE > 0) {
					mP.SOUND_HIT.play(player.getLocation());
				}
				if (mP.RESPECT_IFRAMES && !hitPlayers.contains(player)) {
					if (mP.DAMAGE > 0) {
						BossUtils.blockableDamage(mBoss, player, mP.DAMAGE_TYPE, mP.DAMAGE, mP.SPELL_NAME, mBoss.getLocation(), mP.EFFECTS.mEffectList());
						hitPlayers.add(player);
					}
					if (mP.DAMAGE_PERCENTAGE > 0) {
						DamageUtils.damagePercentHealth(mBoss, player, mP.DAMAGE_PERCENTAGE, false,
							true, mP.SPELL_NAME, true, mP.EFFECTS.mEffectList());
						hitPlayers.add(player);
					}
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> hitPlayers.remove(player), 10);

					MovementUtils.knockAway(origin, player, mP.KB_X, mP.KB_Y);
				} else if (!mP.RESPECT_IFRAMES) {
					if (mP.DAMAGE_PERCENTAGE > 0) {
						DamageUtils.damagePercentHealth(mBoss, player, mP.DAMAGE_PERCENTAGE, false,
							true, mP.SPELL_NAME, true, mP.EFFECTS.mEffectList());
					}
					if (mP.DAMAGE > 0) {
						BossUtils.blockableDamage(mBoss, player, mP.DAMAGE_TYPE, mP.DAMAGE, mP.SPELL_NAME, mBoss.getLocation(), mP.EFFECTS.mEffectList());
					}
					MovementUtils.knockAway(origin, player, mP.KB_X, mP.KB_Y);
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mP.COOLDOWN;
	}
}
