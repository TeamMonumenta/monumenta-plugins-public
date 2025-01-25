package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class SpellSpinDown extends Spell {
	private static final String SPELL_NAME = FastUtils.RANDOM.nextInt(10000) == 0 ? "Frosted Beyblade" : "Frost Whirlwind";
	private static final int CHARGE_DURATION = Constants.TICKS_PER_SECOND * 3;

	private final Plugin mPlugin;
	private final FrostGiant mFrostGiant;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Location mStartLoc;
	private final List<Block> mChangedBlocks = new ArrayList<>();
	private final ChargeUpManager mChargeManager;
	private final ChargeUpManager mCastManager;

	private boolean mCooldown = false;

	public SpellSpinDown(final Plugin plugin, final FrostGiant frostGiant, final Location loc) {
		mPlugin = plugin;
		mFrostGiant = frostGiant;
		mBoss = mFrostGiant.mBoss;
		mWorld = mBoss.getWorld();
		mStartLoc = loc;
		mChargeManager = FrostGiant.defaultChargeUp(mBoss, CHARGE_DURATION, "Charging " + SPELL_NAME + "...");
		mCastManager = FrostGiant.defaultChargeUp(mBoss, CHARGE_DURATION, "Casting " + SPELL_NAME + "...");
	}

	@Override
	public void run() {
		mCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCooldown = false, Constants.TICKS_PER_SECOND * 30);

		mFrostGiant.teleport(mStartLoc.clone().add(0, 1, 0));
		mFrostGiant.freezeGolems();
		mFrostGiant.mPreventTargetting = true;
		mChangedBlocks.clear();

		final World world = mBoss.getWorld();
		final Location loc = mBoss.getLocation();
		final Mob mMob = (Mob) mBoss;
		final Pathfinder mPathfinder = mMob.getPathfinder();
		final BukkitRunnable runnable = new BukkitRunnable() {
			float mPitch = 0.3f;

			@Override
			public void run() {
				mMob.setTarget(null);
				mPathfinder.stopPathfinding();
				final Location speenLoc = mBoss.getLocation();
				speenLoc.setYaw(speenLoc.getYaw() + 45);
				mBoss.teleport(speenLoc);

				if (mChargeManager.getTime() % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.HOSTILE, 4, mPitch);
					mPitch += 0.01f;
					new PartialParticle(Particle.BLOCK_CRACK, loc.clone().add(0, 2, 0), 5, 1,
						0.35, 1, 0.25, FrostGiant.ICE_TYPE.createBlockData()).spawnAsEntityActive(mBoss);
				}

				if (!mChargeManager.nextTick()) {
					return;
				}

				mChargeManager.reset();
				mCastManager.setTime(CHARGE_DURATION);

				new BukkitRunnable() {
					@Override
					public void run() {
						mMob.setTarget(null);
						mPathfinder.stopPathfinding();
						final Location speenLoc = mBoss.getLocation();
						speenLoc.setYaw(speenLoc.getYaw() + 45);
						mBoss.teleport(speenLoc);

						//Shoots out ice blocks every third tick after 3 seconds
						if (mCastManager.getTime() % 3 == 0) {
							createNewIce();
						}

						if (mCastManager.previousTick()) {
							mCastManager.reset();
							mFrostGiant.mPreventTargetting = false;
							mFrostGiant.unfreezeGolems();
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 1);

				this.cancel();
			}
		};

		runnable.runTaskTimer(mPlugin, 1, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public void cancel() {
		super.cancel();

		mChargeManager.reset();
		mCastManager.reset();
		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, FrostGiant.ICE_TYPE);
		mChangedBlocks.clear();
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 7;
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	private void createNewIce() {
		final Location bossLoc = mBoss.getLocation();
		final FallingBlock block = mWorld.spawn(bossLoc, FallingBlock.class, CreatureSpawnEvent.SpawnReason.CUSTOM,
			(final FallingBlock ice) -> {
				ice.setBlockData(Bukkit.createBlockData(Material.ICE));
				ice.setVelocity(new Vector(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(0.1, 0.75), FastUtils.randomDoubleInRange(-1, 1)));
				ice.setDropItem(false);
				EntityUtils.disableBlockPlacement(ice);
			});

		mWorld.playSound(bossLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 4, 0.5f);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				//Once the ice touches the ground or after 5 seconds, create a 4*4 square of damaging frosted ice (cracked)
				if (block.isOnGround() || mTicks >= Constants.TICKS_PER_SECOND * 5) {
					final Location bLoc = block.getLocation();
					final Material groundMat = bLoc.getBlock().getRelative(BlockFace.DOWN).getType();

					if (groundMat == Material.BEDROCK || groundMat == Material.AIR || groundMat == Material.BARRIER) {
						block.remove();
						this.cancel();
						return;
					}

					for (int r = 0; r <= 2; r++) {
						for (int x = -r; x < r; x++) {
							for (int z = -r; z < r; z++) {
								//Have to clone location because of use in HashMap
								final Block b = bLoc.clone().add(x, -1, z).getBlock();

								if (b.getType() != SpellFrostRift.RIFT_BLOCK_TYPE
									&& TemporaryBlockChangeManager.INSTANCE.changeBlock(b, FrostGiant.ICE_TYPE, 20 * FrostGiant.frostedIceDuration + FastUtils.randomIntInRange(0, 10))) {
									final Ageable age = (Ageable) b.getBlockData();
									age.setAge(1 + FastUtils.RANDOM.nextInt(3));
									b.setBlockData(age);
								}
							}
						}
					}
					block.remove();
					this.cancel();
				}
				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}
}
