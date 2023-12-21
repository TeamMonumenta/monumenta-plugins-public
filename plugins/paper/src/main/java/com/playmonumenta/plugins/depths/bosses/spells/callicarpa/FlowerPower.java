package com.playmonumenta.plugins.depths.bosses.spells.callicarpa;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Callicarpa;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class FlowerPower extends Spell {
	public static final String SPELL_NAME = "Flower Power";
	public static final int DURATION = 140;
	public static final int DURATION_A15_DECREASE = 30;
	public static final double LASER_TRAVEL_SPEED = 2.7;
	public static final int LASER_MOVEMENT_STEPS = 8;
	public static final int LASER_MAX_LIFETIME = 200;
	public static final double LASER_DAMAGE = 20;
	public static final int LASER_CAST_DELAY = 5;
	public static final int LASER_MAX_CAST_TIME = 40;
	public static final int LASERS_PER_FLOWER = 3;
	public static final double FLOWER_RESISTANCE_PER_ASCENSION_FROM_A8 = 0.02;
	public static final String FLOWER_RESISTANCE_EFFECT_SOURCE = "FlowerPowerResistanceEffect";
	public static final Color ENERGY_COLOR = Color.fromRGB(252, 223, 78);
	public static final Color[] ACCUMULATION_COLORS = {Color.fromRGB(27, 140, 44), Color.fromRGB(230, 115, 197), Color.fromRGB(147, 201, 38)};
	public static final int INTERNAL_COOLDOWN = 350;

	// For natural block break purposes.
	private final Particle.DustOptions mEnergyOptions = new Particle.DustOptions(ENERGY_COLOR, 2);
	private final Particle.DustOptions mLaserOptions = new Particle.DustOptions(ENERGY_COLOR, 1.5f);
	private final Particle.DustOptions[] mAccumulationOptions = {
		new Particle.DustOptions(ACCUMULATION_COLORS[0], 2),
		new Particle.DustOptions(ACCUMULATION_COLORS[0], 2),
		new Particle.DustOptions(ACCUMULATION_COLORS[1], 2),
		new Particle.DustOptions(ACCUMULATION_COLORS[2], 2),
		new Particle.DustOptions(ACCUMULATION_COLORS[2], 2),
	};
	private final LivingEntity mBoss;
	private final int mFloorY;
	private final PassiveGardenTwo mGarden;
	private final int mFinalCooldown;
	private final @Nullable DepthsParty mParty;
	private final int mFinalDuration;

	private boolean mOnCooldown = false;

	public FlowerPower(LivingEntity boss, int floorY, @Nullable DepthsParty party, PassiveGardenTwo garden) {
		mBoss = boss;
		mFloorY = floorY;
		mGarden = garden;
		mFinalDuration = getDuration(party);
		mFinalCooldown = DepthsParty.getAscensionEightCooldown((int) (mFinalDuration * 1.5), party);
		mParty = party;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mOnCooldown = false, INTERNAL_COOLDOWN);

		// Spawn one additional flower.
		mGarden.spawnFlowers(1);

		// After the flowers have spawned, start the spell.
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), this::doRunSpell, PassiveGardenTwo.SPAWN_DELAY);
	}

	@Override
	public int cooldownTicks() {
		return mFinalCooldown;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown;
	}

	private void doRunSpell() {
		// For each flower, animate a particle energy ball moving towards the accumulation point
		List<Entity> activeFlowers = mBoss.getNearbyEntities(200, 30, 200)
			.stream().filter(e -> e.getScoreboardTags().contains(Callicarpa.FLOWER_TAG)).toList();

		// The cast location is Hedera's location, snapped to the floor level + 1, since it's below the floor.
		Location activeCastLoc = mBoss.getLocation().clone();
		activeCastLoc.setY(mFloorY + 1);

		Location accumulationPoint = activeCastLoc.clone().add(0, 9, 0);
		activeFlowers.forEach(flower -> animateEnergyParticle(flower, accumulationPoint.clone()));

		// Start the cast by storing the cast location and starting the telegraph
		BukkitRunnable flowerRunnable = ParticleUtils.drawFlowerPattern(accumulationPoint.clone(), 2, 6, mFinalDuration,
			0, Math.PI / 90, 0.2f, Particle.FLAME, mBoss);

		// If ascension 8+, give resistance to the active flowers
		if (mParty != null && mParty.getAscension() >= 8) {
			activeFlowers.forEach(flower ->
				Plugin.getInstance().mEffectManager.addEffect(
					flower,
					FLOWER_RESISTANCE_EFFECT_SOURCE,
					new PercentDamageReceived(mFinalDuration, -FLOWER_RESISTANCE_PER_ASCENSION_FROM_A8 * (mParty.getAscension() - 7))
				)
			);
		}

		BukkitRunnable spellRunnable = new BukkitRunnable() {
			final Location mActiveCastLoc = activeCastLoc;
			final Location mAccumulationPoint = accumulationPoint;
			final ChargeUpManager mChargeUp = new ChargeUpManager(mBoss, mFinalDuration, Component.text("Charging ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME, NamedTextColor.GOLD, TextDecoration.BOLD)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, 200);
			final List<Entity> mActiveFlowers = activeFlowers; // Will be useful at the end of the animation to determine how many flowers are still alive

			int mTicks = 0;

			@Override
			public void run() {
				// Accumulation Sphere particles
				new PartialParticle(Particle.REDSTONE, mAccumulationPoint, 5).data(getRandomAccumulationOptions())
					.extra(0).delta(0.2 + (double) mTicks / (double) mFinalDuration).spawnAsBoss();
				// Background noise for charging
				mBoss.getWorld().playSound(mAccumulationPoint, Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 10f, 0f);

				// Count how many flowers are still alive.
				int flowerCount = (int) mActiveFlowers.stream().filter(Entity::isValid).count();

				// If at any point there are no casting flowers left, cancel the attack.
				if (flowerCount == 0) {
					mBoss.getWorld().playSound(mAccumulationPoint, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 10f, 1.5f);
					mBoss.getWorld().playSound(mAccumulationPoint, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 10f, 1.5f);
					mBoss.getWorld().playSound(mAccumulationPoint, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 10f, 0.65f);
					new PartialParticle(Particle.FLAME, mAccumulationPoint, 350).extra(0.35).spawnAsBoss();
					new PartialParticle(Particle.EXPLOSION_HUGE, mAccumulationPoint, 2).extra(0.35).spawnAsBoss();
					flowerRunnable.cancel();
					this.cancel();
					return;
				}

				if (mChargeUp.nextTick()) {
					// Charge finished. Proceed with the rest of the attack.
					int evolvedFlowerCount = (int) mActiveFlowers.stream().filter(Entity::isValid)
						.filter(flower -> flower.getScoreboardTags().contains(Callicarpa.FLOWER_EVOLVED_TAG)).count();
					int laserCount = flowerCount * LASERS_PER_FLOWER + evolvedFlowerCount * LASERS_PER_FLOWER;

					// Signify charge finished with a sound.
					mBoss.getWorld().playSound(mAccumulationPoint, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 10f, 2f);
					mBoss.getWorld().playSound(mAccumulationPoint, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.HOSTILE, 10f, 2f);

					// Launch the lasers!
					BukkitRunnable laserRunnable = new BukkitRunnable() {
						final int mLaserCount = laserCount;

						int mLasersShot = 0;

						@Override
						public void run() {
							// Launch a laser.
							List<Player> targets = PlayerUtils.playersInRange(mActiveCastLoc, 50, false);
							targets.forEach(target -> launchEnergyLaser(target, mAccumulationPoint, mBoss, mLaserOptions, mFloorY, mActiveRunnables, false));
							// Keep the accumulated energy ball showing.
							new PartialParticle(Particle.REDSTONE, mAccumulationPoint, 30).data(mEnergyOptions)
									.extra(0).delta(1.2 - (double) mLasersShot / Math.max(laserCount, 1)).spawnAsBoss();
							mBoss.getWorld().playSound(mAccumulationPoint, Sound.BLOCK_CHAIN_HIT, SoundCategory.HOSTILE, 10f, 0.9f);
							mBoss.getWorld().playSound(mAccumulationPoint, Sound.BLOCK_CHAIN_HIT, SoundCategory.HOSTILE, 10f, 0.9f);
							mBoss.getWorld().playSound(mAccumulationPoint, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.HOSTILE, 10f, 0.2f);
							mBoss.getWorld().playSound(mAccumulationPoint, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.HOSTILE, 10f, 0.2f);
							mBoss.getWorld().playSound(mAccumulationPoint, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10f, 0.7f);
							mBoss.getWorld().playSound(mAccumulationPoint, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10f, 0.7f);
							mBoss.getWorld().playSound(mAccumulationPoint, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 10f, 0.7f);

							// Stop after all the lasers have been shot. Remove additional cast.
							if (mLasersShot >= mLaserCount) {
								this.cancel();
							}
							mLasersShot++;
						}
					};
					// Make sure it stops if the boss is killed.
					mActiveRunnables.add(laserRunnable);
					laserRunnable.runTaskTimer(Plugin.getInstance(), 0, Math.max(1, Math.min(LASER_CAST_DELAY, LASER_MAX_CAST_TIME / Math.max(laserCount, 1))));

					this.cancel();
				}

				mTicks++;
			}
		};

		spellRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public static void launchEnergyLaser(Player target, Location accumulationPoint, LivingEntity boss, Particle.DustOptions laserOptions,
			 int floorY, @Nullable Set<BukkitRunnable> activeRunnables, boolean onlyShowToThatPlayer) {

		BukkitRunnable energyLaserRunnable = new BukkitRunnable() {
			final Location mCurrentLaserLoc = accumulationPoint.clone();
			final Vector mDirection = target.getLocation().clone().add(0, 1, 0).toVector().subtract(accumulationPoint.toVector()).normalize();
			final Vector mLaserStep = mDirection.multiply(LASER_TRAVEL_SPEED).multiply(1.0 / (double) LASER_MOVEMENT_STEPS);

			int mTicks = 0;

			@Override
			public void run() {
				// Do the movement in two halves, checking collision with blocks/players at both steps.
				for (int i = 0; i < LASER_MOVEMENT_STEPS; i++) {
					if (doMovementStep()) {
						// The laser has collided during this step.
						this.cancel();
						return;
					}
				}

				// If the laser exceeds max lifetime, end it.
				if (mTicks >= LASER_MAX_LIFETIME) {
					this.cancel();
				}
				mTicks++;
			}

			private boolean doMovementStep() {
				mCurrentLaserLoc.add(mLaserStep);
				// Spawn a particle for the laser
				PartialParticle laserParticle = new PartialParticle(Particle.REDSTONE, mCurrentLaserLoc, 1).data(laserOptions);
				if (onlyShowToThatPlayer) {
					laserParticle.spawnForPlayer(ParticleCategory.BOSS, target);
				} else {
					laserParticle.spawnAsBoss();
				}

				if (doLaserCollision()) {
					PartialParticle hitParticle = new PartialParticle(Particle.FLAME, mCurrentLaserLoc, 15).extra(0.1);
					if (onlyShowToThatPlayer) {
						hitParticle.spawnForPlayer(ParticleCategory.BOSS, target);
						boss.getWorld().playSound(target, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 2f);
					} else {
						hitParticle.spawnAsBoss();
						boss.getWorld().playSound(mCurrentLaserLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 2f);
					}
					return true;
				}
				return false;
			}

			private boolean doLaserCollision() {
				// If the laser hits a player, damage it
				Hitbox hitbox = new Hitbox.SphereHitbox(mCurrentLaserLoc, 0.5);
				List<Player> hitPlayers = hitbox.getHitPlayers(false);
				if (onlyShowToThatPlayer) {
					// Only allow hitting that player, to avoid being hit by invisible projectiles
					hitPlayers = hitPlayers.stream().filter(player -> player.getUniqueId().equals(target.getUniqueId())).toList();
				}
				if (hitPlayers.size() != 0) {
					Player hitPlayer = hitPlayers.get(0);
					DamageUtils.damage(boss, hitPlayer, DamageEvent.DamageType.MAGIC, LASER_DAMAGE, null, true, true, SPELL_NAME);
					return true;
				}

				// If the laser hits a block, destroy it.
				// Also try to delete all the blocks in a 3D + shape around the block hit.
				Block currentBlock = mCurrentLaserLoc.getBlock();
				if (currentBlock.isCollidable()) {
					tryDeleteBlock(currentBlock);
					tryDeleteBlock(mCurrentLaserLoc.clone().add(0, 1, 0).getBlock());
					tryDeleteBlock(mCurrentLaserLoc.clone().add(0, -1, 0).getBlock());
					tryDeleteBlock(mCurrentLaserLoc.clone().add(1, 0, 0).getBlock());
					tryDeleteBlock(mCurrentLaserLoc.clone().add(-1, 0, 0).getBlock());
					tryDeleteBlock(mCurrentLaserLoc.clone().add(0, 0, 1).getBlock());
					tryDeleteBlock(mCurrentLaserLoc.clone().add(0, 0, -1).getBlock());
					return true;
				}
				return false;
			}

			private void tryDeleteBlock(Block block) {
				if (block.getY() > floorY && BlockUtils.canBeBroken(block)) {
					block.setType(Material.AIR);
				}
			}
		};

		if (activeRunnables != null) {
			activeRunnables.add(energyLaserRunnable);
		}
		energyLaserRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void animateEnergyParticle(Entity flower, Location accumulationPoint) {
		Team yellowTeam = ScoreboardUtils.getExistingTeamOrCreate("yellow", NamedTextColor.YELLOW);
		yellowTeam.addEntity(flower);

		BukkitRunnable energyAnimationRunnable = new BukkitRunnable() {
			final Entity mFlower = flower;
			final Location mCastLoc = mFlower.getLocation().clone().add(0, 1, 0);
			final Location mCurrEnergyLoc = mCastLoc.clone();
			final Location mAccumulationPoint = accumulationPoint;
			final Vector mDirection = mAccumulationPoint.toVector().subtract(mCastLoc.toVector()).normalize();
			final Vector mEnergyStep = mDirection.multiply(mCastLoc.distance(mAccumulationPoint) / mFinalDuration);

			int mTicks = 0;

			@Override
			public void run() {
				// Connect the flower to the accumulation point
				if (mTicks % 40 == 0) {
					new PPLine(Particle.ENCHANTMENT_TABLE, mCastLoc, mAccumulationPoint).extra(0).countPerMeter(3)
						.spawnAsEntityActive(mFlower);
				}

				// Check if the flower has been killed.
				// If so, explode the energy ball and add some more effects.
				if (!mFlower.isValid()) {
					new PartialParticle(Particle.FLAME, mCurrEnergyLoc, 100).extra(0.5).spawnAsEntityActive(mFlower);
					new PartialParticle(Particle.SMOKE_NORMAL, mCurrEnergyLoc, 50).extra(0.5).spawnAsEntityActive(mFlower);
					mFlower.getWorld().playSound(mCurrEnergyLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3f, 1f);
					this.cancel();
					return;
				}

				// Move the energy ball towards the accumulation point.
				mCurrEnergyLoc.add(mEnergyStep);
				// Spawn a particle for it
				new PartialParticle(Particle.REDSTONE, mCurrEnergyLoc, 1).data(mEnergyOptions)
					.extra(0).spawnAsEntityActive(mFlower);

				// Check if spell has finished casting.
				if (mTicks >= mFinalDuration) {
					yellowTeam.removeEntity(mFlower);
					this.cancel();
				}
				mTicks++;
			}
		};

		mActiveRunnables.add(energyAnimationRunnable);
		energyAnimationRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private Particle.DustOptions getRandomAccumulationOptions() {
		return mAccumulationOptions[FastUtils.randomIntInRange(0, mAccumulationOptions.length - 1)];
	}

	private int getDuration(@Nullable DepthsParty party) {
		int duration = DURATION;
		if (party != null && party.getAscension() >= 15) {
			duration -= DURATION_A15_DECREASE;
		}
		return duration;
	}
}
