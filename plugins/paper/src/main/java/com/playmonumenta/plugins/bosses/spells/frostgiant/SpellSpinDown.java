package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.bukkit.entity.Creature;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellSpinDown extends Spell {

	private static final Material ICE_TYPE = Material.FROSTED_ICE;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mStartLoc;

	private boolean mCooldown = false;
	private final List<Block> mChangedBlocks = new ArrayList<>();

	public SpellSpinDown(Plugin plugin, LivingEntity boss, Location loc) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = loc;
	}

	@Override
	public void run() {
		FrostGiant.freezeGolems(mBoss);
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 30);

		//Same as teleport method from the Frost Giant class, teleport with particle effect/sounds
		Location bossLoc = mBoss.getLocation();
		Location secondLoc = bossLoc.clone().add(0, 1, 0);
		World world = mStartLoc.getWorld();
		world.playSound(bossLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, secondLoc, 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.CLOUD, secondLoc, 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, bossLoc, 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
		mBoss.teleport(mStartLoc.clone().add(0, 1, 0));
		world.playSound(bossLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, secondLoc, 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, secondLoc, 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, bossLoc, 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);

		FrostGiant.delayHailstormDamage();

		if (FrostGiant.mInstance != null) {
			FrostGiant.mInstance.mPreventTargetting = true;
		}

		mChangedBlocks.clear();

		final float vel = 45f;
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 0;

			@Override
			public void run() {
				World world = mBoss.getWorld();

				Creature c = (Creature) mBoss;
				Pathfinder pathfinder = c.getPathfinder();

				c.setTarget(null);
				pathfinder.stopPathfinding();

				Location loc = mBoss.getLocation();
				mBoss.setRotation(loc.getYaw() + vel, loc.getPitch());

				if (mTicks % 2 == 0 && mTicks <= 20 * 3) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.HOSTILE, 4, mPitch);
					mPitch += 0.01f;

					new PartialParticle(Particle.BLOCK_DUST, mBoss.getLocation().add(0, 2, 0), 5, 1, 0.35, 1, 0.25, ICE_TYPE.createBlockData()).spawnAsEntityActive(mBoss);
				}

				//Shoots out ice blocks every third tick after 3 seconds
				if (mTicks >= 20 * 3 && mTicks % 3 == 0) {
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 4, 0.5f);

					//Velocity randomized of the frosted ice as a falling block
					FallingBlock block = world.spawnFallingBlock(loc, Bukkit.createBlockData(ICE_TYPE));
					block.setDropItem(false);
					EntityUtils.disableBlockPlacement(block);
					block.setVelocity(new Vector(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(0.1, 0.75), FastUtils.randomDoubleInRange(-1, 1)));

					new BukkitRunnable() {
						int mTicks = 0;

						@Override
						public void run() {
							//Once the ice touches the ground or after 5 seconds, create a 4*4 square of damaging frosted ice (cracked)
							if (mTicks >= 20 * 5 || block.isOnGround()) {
								Location bLoc = block.getLocation();
								Material groundMat = bLoc.getBlock().getRelative(BlockFace.DOWN).getType();
								if (groundMat != Material.BEDROCK && groundMat != Material.AIR && groundMat != Material.BARRIER) {
									for (int r = 0; r <= 2; r++) {
										for (int x = -r; x < r; x++) {
											for (int z = -r; z < r; z++) {
												//Have to clone location because of use in HashMap
												Block b = bLoc.clone().add(x, -1, z).getBlock();

												if (b.getType() != SpellFrostRift.RIFT_BLOCK_TYPE
													    && TemporaryBlockChangeManager.INSTANCE.changeBlock(b, ICE_TYPE, 20 * FrostGiant.frostedIceDuration + FastUtils.randomIntInRange(0, 10))) {
													Ageable age = (Ageable) b.getBlockData();
													age.setAge(1 + FastUtils.RANDOM.nextInt(3));
													b.setBlockData(age);
												}
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


				if (mTicks >= 20 * 6) {
					if (FrostGiant.mInstance != null) {
						FrostGiant.mInstance.mPreventTargetting = false;
					}
					FrostGiant.unfreezeGolems(mBoss);
					this.cancel();
				}

				mTicks += 1;
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);

		// Damage players standing on frosted ice for the duration
		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {

				//Stop running after 20 seconds
				if (mTicks >= 20 * FrostGiant.frostedIceDuration || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 40, true)) {
					if ((player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR || player.getLocation().getBlock().getType() != Material.AIR)
						    && (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == ICE_TYPE || player.getLocation().getBlock().getType() == ICE_TYPE)) {
						Vector vel = player.getVelocity();
						DamageUtils.damage(mBoss, player, DamageType.MAGIC, 18, null, false, true, "Frosted Ice");
						player.setVelocity(vel);
					}
				}
				mTicks += 10;
			}
		}.runTaskTimer(mPlugin, 0, 10)); //Every 0.5 seconds, check if player is on cone area damage
	}

	@Override
	public void cancel() {
		super.cancel();

		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, ICE_TYPE);
		mChangedBlocks.clear();
	}

	@Override
	public int cooldownTicks() {
		return 7 * 20;
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}
}
