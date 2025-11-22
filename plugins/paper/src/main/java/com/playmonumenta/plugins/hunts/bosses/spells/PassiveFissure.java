package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.CoreElemental;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PassiveFissure extends Spell {
	@FunctionalInterface
	public interface StartAction {
		void run(Location location);
	}

	@FunctionalInterface
	public interface BlockChangeAction {
		void run(Location location);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mOrigin;
	private final int mStartAngle;
	private final Vector[][] mFissureVector;
	private final List<Player> mAffectedPlayers = new ArrayList<>();
	private final List<BukkitRunnable> mRunnables = new ArrayList<>();
	private final Material mPrimaryMaterial;
	private final Material mSecondaryMaterial;
	private final String mSpellName;
	private final int mDamage;
	private final BlockChangeAction mBlockChangeAction;
	private final StartAction mStartAction;
	private final CoreElemental mCoreElemental;
	private int mPhase = 0;
	private int mStayDuration = 0;
	private boolean mIsCast;

	public PassiveFissure(Plugin plugin, LivingEntity boss, Location origin, int numberOfFissure, int numberOfSegment, int[] lengthOfSegment, Material primaryMaterial, Material secondaryMaterial, int damage, String spellName, BlockChangeAction blockChangeAction, StartAction startAction, CoreElemental coreElemental) {
		mPlugin = plugin;
		mBoss = boss;
		mOrigin = origin;
		mStartAngle = FastUtils.randomIntInRange(0, 359);
		mFissureVector = generateFissure(numberOfFissure, numberOfSegment, lengthOfSegment);
		mPrimaryMaterial = primaryMaterial;
		mSecondaryMaterial = secondaryMaterial;
		mSpellName = spellName;
		mDamage = damage;
		mBlockChangeAction = blockChangeAction;
		mStartAction = startAction;
		mCoreElemental = coreElemental;
	}

	public void trigger(int phase) {
		trigger(phase, 0);
	}

	public void trigger(int phase, int duration) {
		if (mIsCast) {
			return;
		}
		mAffectedPlayers.clear();
		mPhase = phase;
		mStayDuration = duration;
		mIsCast = true;
		mStartAction.run(mOrigin);
		for (int i = 0; i < mFissureVector.length; i++) {
			generateSegment(i, 0, 0, null);
		}
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mIsCast = false, 20 * 3);
	}

	// Generate a 2D array of vectors
	public Vector[][] generateFissure(int numberOfFissure, int numberOfSegment, int[] lengthOfSegment) {
		Vector[][] result = new Vector[numberOfFissure][numberOfSegment];
		for (int i = 0; i < numberOfFissure; i++) {
			for (int j = 0; j < numberOfSegment; j++) {
				// Generate crack vectors and store them into 2D array
				Vector vector = generateSegmentVector(j == 0 ? mStartAngle + i * 360 / numberOfFissure :
					(int) VectorUtils.vectorToRotation(result[i][j - 1])[0], lengthOfSegment[j], j);
				result[i][j] = vector;
			}
		}
		return result;
	}

	// Generate an individual vector
	private Vector generateSegmentVector(int angle, double length, int segmentNumber) {
		int range = segmentNumber == 0 ? 10 : 30;
		return VectorUtils.rotationToVector(angle + FastUtils.randomIntInRange(-range, range), 0)
			.multiply(length * FastUtils.randomDoubleInRange(0.75, 1));
	}

	// Returns the starting point of a vector
	private Location getSegmentLocation(int fissureNumber, int segmentNumber) {
		Location finalLocation = mOrigin.clone();
		for (int i = 0; i < segmentNumber; i++) {
			finalLocation.add(mFissureVector[fissureNumber][i]);
		}
		return finalLocation;
	}

	private Vector getSegmentVector(int fissureNumber, int segmentNumber) {
		return mFissureVector[fissureNumber][segmentNumber].clone();
	}

	private int getFissureWidth(int segmentNumber, double step) {
		return (int) Math.max(
			Math.round((((double) mPhase) / 4d + 1)
				* Math.pow(0.7, (double) segmentNumber + step)), 1);
	}

	// Actually generates part of the fissure (i.e. replacing blocks on ground)
	private void generateSegment(int fissureNumber, int segmentNumber, double step, @Nullable Block lastBlock) {
		Vector finalVector = getSegmentVector(fissureNumber, segmentNumber);
		Location finalLocation = getSegmentLocation(fissureNumber, segmentNumber)
			.add(finalVector.multiply(step));
		Block block = getSurfaceBlock(finalLocation);
		// If the target block is the same is the last block, continue next step without delay
		if (lastBlock != null && block.getLocation().equals(lastBlock.getLocation())) {
			if (step < 1) {
				generateSegment(fissureNumber, segmentNumber, step + 0.1, block);
			} else {
				if (segmentNumber < mPhase) {
					generateSegment(fissureNumber, segmentNumber + 1, 0, block);
				}
			}
			return;
		}

		if (segmentNumber < mPhase) {
			// Chance to replace centre line to lava
			replaceBlock(block, true);
			// Widen current fissures
			BlockFace wideningDirection = getWideningDirection(finalVector);
			int width = getFissureWidth(segmentNumber, step);
			Block blockAdjacent = block;
			Block blockOpposite = block;
			for (int i = 0; i < width; i++) {
				blockAdjacent = getSurfaceBlock(blockAdjacent.getRelative(wideningDirection).getLocation().clone());
				blockOpposite = getSurfaceBlock(blockOpposite.getRelative(wideningDirection.getOppositeFace()).getLocation().clone());
				replaceBlock(blockAdjacent, false);
				replaceBlock(blockOpposite, false);
			}
		} else {
			// Extend fissures
			replaceBlock(block, false);
		}
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (step < 1) {
				generateSegment(fissureNumber, segmentNumber, step + 0.1, block);
			} else {
				if (segmentNumber < mPhase) {
					generateSegment(fissureNumber, segmentNumber + 1, 0, block);
				}
			}
		}, 1);
	}

	// Gets the block with its top face exposed to air
	private Block getSurfaceBlock(Location location) {
		Location finalLocation = LocationUtils.fallToGround(location.clone().add(0, 10, 0), mOrigin.getBlockY() - 5);
		return finalLocation.add(0, -1, 0).getBlock();
	}

	private void replaceBlock(Block block, boolean secondaryPossible) {
		Material finalMaterial = mPrimaryMaterial;

		// Do nothing if the block is already lava/ it is air or water/ is not mechanical/ valuable
		if (block.getType() == mSecondaryMaterial
			|| !block.isSolid()
			|| BlockUtils.isMechanicalBlock(block.getType())
			|| BlockUtils.isValuableBlock(block.getType())) {
			return;
		}

		if (secondaryPossible) {
			// Else, replace block with fissure block
			boolean lavaCandidate = block.getRelative(BlockFace.EAST).isSolid() &&
				block.getRelative(BlockFace.WEST).isSolid() &&
				block.getRelative(BlockFace.NORTH).isSolid() &&
				block.getRelative(BlockFace.SOUTH).isSolid() &&
				block.getRelative(BlockFace.DOWN).isSolid() &&
				block.getRelative(BlockFace.UP).getType() == Material.AIR;

			if (lavaCandidate && FastUtils.randomDoubleInRange(0, 1) <= 0.8) {
				finalMaterial = mSecondaryMaterial;
			}
		}

		int duration = mStayDuration == 0 ? 72000 : mStayDuration;
		if (TemporaryBlockChangeManager.INSTANCE.changeBlock(block, finalMaterial, duration)) {
			mCoreElemental.addChangedBlock(block);
		}

		mBlockChangeAction.run(block.getLocation());

		// Damage players
		damageNearby(block.getLocation(), mDamage);
	}

	// Damage nearby players
	private void damageNearby(Location location, double damage) {
		List<Player> mPlayers = PlayerUtils.playersInRange(location, 1.5, true, false);
		for (Player player : mPlayers) {
			if (!mAffectedPlayers.contains(player)) {
				BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, damage, mSpellName, mBoss.getLocation());
				player.setVelocity(player.getVelocity().setY(1.1));
				mAffectedPlayers.add(player);
			}
		}
	}

	private BlockFace getWideningDirection(Vector vector) {
		if (vector.getX() > vector.getZ()) {
			return BlockFace.NORTH;
		} else {
			return BlockFace.EAST;
		}
	}

	public void displayTelegraph(int phase) {
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mIsCast) {
					this.cancel();
					mRunnables.remove(this);
					return;
				}
				// Particles
				for (Vector[] vectors : mFissureVector) {
					Location startLoc = mOrigin.clone().add(0, 5, 0);
					for (int j = 0; j < phase + 1; j++) {
						Vector fissureVector = vectors[j].clone();
						double length = fissureVector.length();
						new PPParametric(Particle.REDSTONE, startLoc, (t, builder) -> builder.location(LocationUtils.fallToGround(startLoc.clone().add(fissureVector.clone().multiply(t)), mOrigin.getY() - 5)))
							.count(15 * (int) length)
							.delta(0.25, 0, 0.25)
							.extra(3)
							.data(new Particle.DustOptions(Color.fromRGB(252, 94, 3), 1))
							.spawnAsEntityActive(mBoss);

						startLoc.add(vectors[j]);
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 5);
		mRunnables.add(runnable);
	}

	@Override
	public void run() {

	}

	@Override
	public void cancel() {
		mRunnables.forEach(BukkitRunnable::cancel);
		mRunnables.clear();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
