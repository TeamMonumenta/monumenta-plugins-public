package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.bosses.TpBehindBoss;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellTpBehindPlayer extends Spell {

	private static final int TP_STUN_CREEPER_INCREASED = 20; // Increased time for creepers
	private static final int VERTICAL_DISTANCE_TO_ENTITY = 3;

	private final Plugin mPlugin;
	protected final LivingEntity mLauncher;
	private final TpBehindBoss.Parameters mParameters;

	public SpellTpBehindPlayer(Plugin plugin, LivingEntity launcher, TpBehindBoss.Parameters parameters) {
		mPlugin = plugin;
		mLauncher = launcher;
		mParameters = parameters;
	}

	// ExaltedCAxtal, MimicQueen, CAxtal, and SpellConditionalTpBehindPlayer
	public SpellTpBehindPlayer(Plugin plugin, LivingEntity launcher, int cooldown, int range, int delay, int stun, boolean random) {
		mParameters = new TpBehindBoss.Parameters();
		mPlugin = plugin;
		mLauncher = launcher;
		mParameters.COOLDOWN = cooldown;
		mParameters.DELAY = delay;
		mParameters.STUN = stun;
		mParameters.PREFER_TARGET = !random;
		mParameters.TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, range, new EntityTargets.Limit(EntityTargets.Limit.LIMITSENUM.ALL, EntityTargets.Limit.SORTING.RANDOM), List.of(EntityTargets.PLAYERFILTER.NOT_STEALTHED));
	}

	@Override
	public void run() {
		if (mLauncher instanceof Mob) {
			LivingEntity targetEntity = null;
			if (mParameters.PREFER_TARGET) {
				LivingEntity target = ((Mob) mLauncher).getTarget();
				if (target != null
					&& target.getLocation().distance(mLauncher.getLocation()) < mParameters.TARGETS.getRange()
					&& !ZoneUtils.hasZoneProperty(target, ZoneProperty.RESIST_5)) {
					targetEntity = target;
				}
			} // If the targetEntity is still null after prefer_target, have EntityTarget fallback
			if (targetEntity == null) {
				List<? extends LivingEntity> targets = mParameters.TARGETS.getTargetsList(mLauncher);
				targets.removeIf(target
					-> ZoneUtils.hasZoneProperty(target, ZoneProperty.RESIST_5)
					|| ZoneUtils.hasZoneProperty(target, ZoneProperty.LOOTROOM));
				targetEntity = targets.get(0);
			}
			// If that didn't work, don't tp at all
			if (targetEntity != null) {
				launch(targetEntity, mParameters.TARGETS.getRange());
				animation();
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mParameters.COOLDOWN;
	}

	protected void launch(LivingEntity target, double maxRange) {
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				Location loc = target.getLocation();
				// We need a second check when the teleport is actually about to occur
				if (!loc.getWorld().equals(mLauncher.getWorld()) || loc.distance(mLauncher.getLocation()) > maxRange || ZoneUtils.hasZoneProperty(target, ZoneProperty.RESIST_5)) {
					return;
				}

				loc.setY(loc.getY() + 0.1f);
				Vector shift = loc.getDirection();
				shift.setY(0).normalize().multiply(-0.5);

				final int DISTANCE_TO_ENTITY = 2 * mParameters.DISTANCE; // How many half-blocks behind the player maximum

				// Check from farthest horizontally to closest, lowest vertically to highest
				for (int horizontalShift = DISTANCE_TO_ENTITY; horizontalShift > 0; horizontalShift--) {
					for (int verticalShift = 0; verticalShift <= VERTICAL_DISTANCE_TO_ENTITY; verticalShift++) {
						Location locTest = loc.clone().add(shift.clone().multiply(horizontalShift));
						locTest.setY(locTest.getY() + verticalShift);
						if (canTeleport(locTest)) {
							loc.add(0, mLauncher.getHeight() / 2, 0);
							mParameters.PARTICLE_TP.spawn(mLauncher, mLauncher.getLocation());

							EntityUtils.teleportStack(mLauncher, locTest);
							// Don't aggro to a mob, or a player in stealth
							if (mLauncher instanceof Mob mob && !(target instanceof Player && AbilityUtils.isStealthed((Player) target))) {
								mob.setTarget(target);
								// For some reason just setting the target doesn't seem to be enough, so try again a tick later
								Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
									mob.setTarget(target);
								}, 1);
							}
							onTeleport(target);

							locTest.add(0, mLauncher.getHeight() / 2, 0);
							mParameters.PARTICLE_TP.spawn(mLauncher, mLauncher.getLocation());
							mParameters.SOUND_TP.play(mLauncher.getLocation());

							// The mPlugin here is of the incorrect type for some reason
							if (mLauncher instanceof Creeper) {
								EntityUtils.applyCooling(com.playmonumenta.plugins.Plugin.getInstance(), mParameters.STUN + TP_STUN_CREEPER_INCREASED, mLauncher);
							} else {
								EntityUtils.applyCooling(com.playmonumenta.plugins.Plugin.getInstance(), mParameters.STUN, mLauncher);
							}

							// Janky way to break out of nested loop
							horizontalShift = 0;
							break;
						}
					}
				}
			}
		};

		runnable.runTaskLater(mPlugin, mParameters.DELAY);
		mActiveRunnables.add(runnable);
	}

	protected void onTeleport(LivingEntity entity) {
		// Does nothing, can be overridden by extending spells
	}

	protected void animation() {
		mParameters.SOUND_TEL.play(mLauncher.getLocation());

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				mParameters.PARTICLE_TEL.spawn(mLauncher, mLauncher.getLocation());

				if (mTicks > mParameters.DELAY) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private boolean canTeleport(Location loc) {
		World world = loc.getWorld();

		// Move a bounding box to the target location
		BoundingBox box = mLauncher.getBoundingBox();
		box.shift(loc.clone().subtract(mLauncher.getLocation()));

		// Check the 8 corners of the bounding box and the location itself for blocks
		return !isObstructed(loc, box)
			&& !isObstructed(new Location(world, box.getMinX(), box.getMinY(), box.getMinZ()), box)
			&& !isObstructed(new Location(world, box.getMinX(), box.getMinY(), box.getMaxZ()), box)
			&& !isObstructed(new Location(world, box.getMinX(), box.getMaxY(), box.getMinZ()), box)
			&& !isObstructed(new Location(world, box.getMinX(), box.getMaxY(), box.getMaxZ()), box)
			&& !isObstructed(new Location(world, box.getMaxX(), box.getMinY(), box.getMinZ()), box)
			&& !isObstructed(new Location(world, box.getMaxX(), box.getMinY(), box.getMaxZ()), box)
			&& !isObstructed(new Location(world, box.getMaxX(), box.getMaxY(), box.getMinZ()), box)
			&& !isObstructed(new Location(world, box.getMaxX(), box.getMaxY(), box.getMaxZ()), box);
	}

	private boolean isObstructed(Location loc, BoundingBox box) {
		Block block = loc.getBlock();
		return block.getBoundingBox().overlaps(box) && !block.isLiquid();
	}

}
