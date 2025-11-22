package com.playmonumenta.plugins.bosses.spells;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.bosses.bosses.RamBoss;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;


public class SpellRam extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final RamBoss.Parameters mParameters;

	public SpellRam(Plugin plugin, LivingEntity boss, RamBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = boss;
		mParameters = parameters;
	}

	@Override
	public void run() {
		List<? extends LivingEntity> targets = mParameters.TARGETS.getTargetsList(mBoss);
		if (targets.isEmpty()) {
			return;
		}
		LivingEntity target = targets.get(0);
		Location endLoc = target.getLocation();
		if (mParameters.TELEGRAPH_DURATION > 0) {
			mParameters.SOUND_TEL.play(mBoss.getLocation());
		}
		BukkitRunnable runnable = new BukkitRunnable() {
			private boolean mHitPlayer = false;

			int mTicks = 0;
			private final List<Player> mHitPlayers = new ArrayList<>();

			@Override
			public void run() {
				if (EntityUtils.shouldCancelSpells(mBoss)) {
					this.cancel();
					return;
				}

				if (mBoss instanceof Mob c) {
					Pathfinder pathfinder = c.getPathfinder();

					pathfinder.stopPathfinding();
				}


				double[] yawPitch = VectorUtils.vectorToRotation(endLoc.clone().subtract(mBoss.getLocation()).toVector());
				mBoss.setRotation((float) yawPitch[0], (float) yawPitch[1]);

				if (mTicks % mParameters.SOUND__INTERVAL == 0) {
					mParameters.SOUND_RAM_TICK.play(mBoss.getLocation());
				}

				if (mTicks % mParameters.TELEGRAPH_INTERVAL == 0) {
					mParameters.PARTICLE_TEL.spawn(mBoss, particle ->
						new PPLine(particle, mBoss.getLocation(), endLoc).countPerMeter(3));
				}

				if (mTicks == mParameters.TELEGRAPH_DURATION) {
					mParameters.SOUND_LAUNCH.play(mBoss.getLocation());
					mParameters.PARTICLE_LAUNCH.spawn(mBoss, mBoss.getLocation());
				}

				if (mTicks > mParameters.TELEGRAPH_DURATION) {
					Location loc = mBoss.getLocation();

					BoundingBox hitbox = mBoss.getBoundingBox().expand(mParameters.HITBOX_EXPAND);
					List<Player> players = new ArrayList<>(PlayerUtils.playersInRange(loc, hitbox.getWidthX() * 4, true)
						.stream().filter(player -> player.getBoundingBox().overlaps(hitbox)).toList());
					players.removeAll(mHitPlayers);

					for (Player p : players) {
						mHitPlayers.add(p);
						if (mParameters.DAMAGE > 0) {
							DamageUtils.damage(mBoss, p, mParameters.DAMAGE_TYPE, mParameters.DAMAGE, null, false, false, mParameters.SPELL_NAME);
						}
						if (mParameters.DAMAGE_PERCENTAGE > 0) {
							DamageUtils.damagePercentHealth(mBoss, p, mParameters.DAMAGE_PERCENTAGE, false, false, mParameters.SPELL_NAME, true, mParameters.EFFECTS_HIT.mEffectList());
						}
						MovementUtils.knockAway(loc, p, mParameters.KB_XZ, mParameters.KB_Y, false);
						mParameters.EFFECTS_HIT.apply(p, mBoss);
						mHitPlayer = true;
					}

					Vector dir = endLoc.toVector().clone().subtract(loc.clone().toVector());
					dir.normalize();
					dir.multiply(mParameters.VELOCITY);
					mBoss.setVelocity(dir);
				}

				if (mTicks >= mParameters.RAM_DURATION + mParameters.TELEGRAPH_DURATION || (mHitPlayer && mParameters.CANCEL_ON_DAMAGE) || endLoc.distance(mBoss.getLocation()) < mParameters.END_DISTANCE_THRESHOLD) {
					if (mParameters.CHANGE_TARGET && mBoss instanceof Mob c) {
						c.setTarget(target);
					}
					mParameters.PARTICLE_END.spawn(mBoss, mBoss.getLocation());
					mParameters.SOUND_END.play(mBoss.getLocation());
					this.cancel();
					return;
				}
				mTicks++;
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return mParameters.COOLDOWN;
	}
}
