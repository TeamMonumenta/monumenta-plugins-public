package com.playmonumenta.plugins.bosses.spells.sirius.miniboss;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellStarblightLeap extends Spell {
	private LivingEntity mBoss;
	private boolean mOnCooldown;
	private Plugin mPlugin;
	private static final int COOLDOWN = 5 * 20;
	private static final float VELOCITYMULTIPLIER = 1.2f;

	public SpellStarblightLeap(LivingEntity boss, Plugin plugin) {
		mBoss = boss;
		mOnCooldown = false;
		mPlugin = plugin;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN + 20);
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation();
		List<Player> pList = PlayerUtils.playersInRange(mBoss.getLocation(), 20, true, true);
		if (!pList.isEmpty()) {
			Player targetPlayer = FastUtils.getRandomElement(pList);
			Location locTarget = targetPlayer.getLocation();
			((Mob) mBoss).getPathfinder().moveTo(locTarget);
			Vector velocity = locTarget.toVector().normalize().multiply(VELOCITYMULTIPLIER);
			velocity.setY(1.1);
			final Vector finalVelocity = velocity;
			new BukkitRunnable() {
				final Location mLeapLocation = loc;
				boolean mLeaping = false;
				boolean mHasBeenOneTick = false;

				@Override
				public void run() {
					if (!mLeaping) {
						// start leaping
						if (mBoss.getLocation().distance(mLeapLocation) < 1) {
							world.playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
							new PartialParticle(Particle.SCRAPE, mBoss.getLocation(), 15, 1, 0f, 1, 0).spawnAsEntityActive(mBoss);
							((Mob) mBoss).getPathfinder().stopPathfinding();
							mBoss.setVelocity(finalVelocity);
							mLeaping = true;
						}
					} else {
						new PartialParticle(Particle.SCRAPE, mBoss.getLocation(), 4, 0.5, 0.5, 0.5, 1).spawnAsEntityActive(mBoss);
						mBoss.setFallDistance(0);
						if (mBoss.isOnGround() && mHasBeenOneTick) {
							this.cancel();
							return;
						}

						BoundingBox hitbox = mBoss.getBoundingBox();
						for (Player player : mBoss.getWorld().getPlayers()) {
							if (player.getBoundingBox().overlaps(hitbox) && mHasBeenOneTick) {
								((Mob) mBoss).setTarget(player);
								this.cancel();
								return;
							}
						}

						// Give the caller a chance to run extra effects or manipulate the boss's leap velocity
						if (targetPlayer.isOnline() && targetPlayer.getWorld().equals(mBoss.getWorld())) {
							Vector towardsPlayer = targetPlayer.getLocation().subtract(mBoss.getLocation()).toVector().setY(0).normalize();
							Vector originalVelocity = mBoss.getVelocity();
							double scale = 0.9;
							Vector newVelocity = new Vector();
							newVelocity.setX((originalVelocity.getX() * 20 + towardsPlayer.getX() * scale) / 20);
							// Use the original mob's vertical velocity, so it doesn't somehow fall faster than gravity
							newVelocity.setY(originalVelocity.getY());
							newVelocity.setZ((originalVelocity.getZ() * 20 + towardsPlayer.getZ() * scale) / 20);
							mBoss.setVelocity(newVelocity);
						}

						// At least one tick has passed to avoid insta smacking a nearby player
						mHasBeenOneTick = true;
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}
}
