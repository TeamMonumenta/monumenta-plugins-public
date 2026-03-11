package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SporousAmalgam;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Uproot extends Spell {
	private static final int ROOTS_DURATION = (int) (20 * 1.25);
	private static final int COOLDOWN = 20 * 8 + ROOTS_DURATION;
	private static final int ROOT_AMOUNT = 17;
	private static final int MAX_BRANCHES_PER_ROOT = 7;
	private static final double NO_HIT_LENIENCY = 0.33;

	private static final double DAMAGE_AMOUNT = 50;
	private static final float SPORE_AMOUNT = 1.5f;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final SporousAmalgam mSporeBeast;
	private final Location mStartLocation;
	private final PPCircle mParticleCircle;

	private int mNoHitSpells;

	private static final Material mTargetMaterial = Material.MUSHROOM_STEM;
	private static final Material mMidwayMaterial = Material.BROWN_MUSHROOM_BLOCK;

	public Uproot(Plugin plugin, SporousAmalgam sporeBeast) {
		mPlugin = plugin;
		mSporeBeast = sporeBeast;
		mBoss = sporeBeast.mBoss;
		mStartLocation = mBoss.getLocation().add(0, -1, 0);
		mParticleCircle = new PPCircle(Particle.BLOCK_CRACK, mBoss.getLocation(), SporousAmalgam.SPELL_INNER_RADIUS).ringMode(false).count(100).data(Material.MUSHROOM_STEM.createBlockData());
	}

	@Override
	public void run() {
		mNoHitSpells = 0;
		mSporeBeast.resetKilledSummons();
		new BukkitRunnable() {
			int mTimesUsed = 0;

			@Override
			public void run() {
				if (mNoHitSpells >= 2) {
					mSporeBeast.uproot();
					this.cancel();
				} else if (mTimesUsed >= 5 || mBoss.isDead()) {
					this.cancel();
				}
				if (!this.isCancelled()) {
					double angleOffset = (mTimesUsed % 2 == 0) ? 0 : 360.0 / ROOT_AMOUNT / 2;
					double anglePerRoot = 360.0 / ROOT_AMOUNT;
					runUprootSpell(anglePerRoot, angleOffset);
					mTimesUsed++;
				}
			}
		}.runTaskTimer(mPlugin, 0, ROOTS_DURATION + 10);
	}

	private void runUprootSpell(double anglePerRoot, double angleOffset) {
		ArrayList<Block> changedBlocks = new ArrayList<>();
		ArrayList<BranchSeed> branchSeeds = new ArrayList<>();

		List<Player> mTargets = mSporeBeast.getPlayersInOutRange();
		TemporaryBlockChangeManager mBlockManager = TemporaryBlockChangeManager.INSTANCE;

		for (int i = 0; i < ROOT_AMOUNT; i++) {
			int branches = 0;
			int blocksSinceLastBranch = 0;

			double currentAngle = anglePerRoot * i + angleOffset;
			Location spellLocation = mStartLocation.clone();
			Vector direction = new Vector(FastUtils.cosDeg(currentAngle), 0, FastUtils.sinDeg(currentAngle));

			while (spellLocation.distance(mStartLocation) < SporousAmalgam.SPELL_INNER_RADIUS) {

				int random = (branches >= MAX_BRANCHES_PER_ROOT || spellLocation.distance(mStartLocation) <= 3 || blocksSinceLastBranch < 1) ? 0 : FastUtils.randomIntInRange(1, 10);
				blocksSinceLastBranch++;

				if (mBlockManager.changeBlock(spellLocation.getBlock(), mTargetMaterial, ROOTS_DURATION)) {
					changedBlocks.add(spellLocation.getBlock());
				}
				spellLocation.add(direction);

				if (random >= 1 && random <= 7) {
					branchSeeds.add(new BranchSeed(spellLocation.clone(), currentAngle));
					branches++;
					blocksSinceLastBranch = 0;
				}
			}
		}
		createBranches(branchSeeds, mBlockManager, changedBlocks);

		new BukkitRunnable() {
			int mTicks = 0;
			int mTimesUsed = 0;

			@Override
			public void run() {
				if (mTicks == ROOTS_DURATION) {
					dealDamage(changedBlocks);
					for (Player p : mTargets) {
						for (int i = 0; i < 10; i++) {
							p.playSound(p, Sound.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.HOSTILE, 2.0f, 1f - 0.1f * i);
						}
					}
					for (Block b : changedBlocks) {
						mBlockManager.changeBlock(b, mMidwayMaterial, 5);
					}
					this.cancel();
				} else if (mTicks % 10 == 0) {
					for (int i = 0; i < mTimesUsed + 1; i++) {
						for (Player p : mTargets) {
							p.playSound(p, Sound.BLOCK_AZALEA_LEAVES_PLACE, SoundCategory.HOSTILE, 2.0f, 1f);
							mParticleCircle.spawnAsBoss();
						}
					}
				}
				if (mBoss.isDead()) {
					this.cancel();
				}
				mTicks += 5;
				mTimesUsed++;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	private void dealDamage(List<Block> changedBlocks) {
		ArrayList<BoundingBox> hitboxes = new ArrayList<>();
		int allowedHits = (int) (mSporeBeast.getPlayersInInRange().size() * NO_HIT_LENIENCY);
		int hits = 0;

		for (Block b : changedBlocks) {
			BoundingBox boundingBox = b.getBoundingBox();
			boundingBox.shift(0, 1, 0);
			boundingBox.expand(0, 0, 0, 0, 10, 0);
			hitboxes.add(boundingBox);

		}
		Hitbox hitbox = Hitbox.unionOfAABB(hitboxes, mBoss.getWorld());
		List<Player> players = hitbox.getHitPlayers(true);
		for (Player p : players) {
			DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MELEE, DAMAGE_AMOUNT, null, false, true, "Uproot");
			mSporeBeast.addSpores(p, SPORE_AMOUNT);
			hits++;
		}
		if (hits <= allowedHits) {
			mNoHitSpells++;
		}
	}

	private void createBranches(List<BranchSeed> branchSeeds, TemporaryBlockChangeManager blockChangeManager, List<Block> changedBlocks) {
		for (BranchSeed s : branchSeeds) {

			int randomSeed = FastUtils.randomIntInRange(1, 5);
			int angleOffset;
			switch (randomSeed) {
				case 1 -> angleOffset = 60;
				case 2 -> angleOffset = 75;
				case 3 -> angleOffset = -60;
				case 4 -> angleOffset = -75;
				case 5 -> angleOffset = 30;
				default -> angleOffset = 0;
			}

			double branchAngle = s.mBranchStartingAngle + angleOffset;
			Vector direction = calculateDirection(branchAngle);
			Block tempBlock = s.mBranchStartLocation.getBlock();
			s.mBranchStartLocation.add(direction);

			while (s.mBranchStartLocation.distance(mStartLocation) < SporousAmalgam.SPELL_INNER_RADIUS) {
				if (blockChangeManager.changeBlock(s.mBranchStartLocation.getBlock(), mTargetMaterial, ROOTS_DURATION)) {
					changedBlocks.add(s.mBranchStartLocation.getBlock());
					tempBlock = s.mBranchStartLocation.getBlock();
				} else if (!s.mBranchStartLocation.getBlock().equals(tempBlock)) {
					break;
				}
				s.mBranchStartLocation.add(direction);
			}
		}
	}

	private Vector calculateDirection(double angle) {
		return new Vector(FastUtils.cosDeg(angle), 0, FastUtils.sinDeg(angle));
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		return mSporeBeast.canRunUproot() && !mSporeBeast.hasRunningSpell();
	}

	private record BranchSeed(Location mBranchStartLocation, double mBranchStartingAngle) {
	}
}
