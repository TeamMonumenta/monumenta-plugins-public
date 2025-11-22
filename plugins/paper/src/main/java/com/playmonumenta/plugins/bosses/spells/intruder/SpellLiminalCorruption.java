package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellLiminalCorruption extends Spell {
	private final IntruderBoss.Dialogue mDialogue;

	public static final String SPELL_NAME = "Liminal Corruption";
	public static final Material MATERIAL = Material.GRAY_GLAZED_TERRACOTTA;
	private static final int TELEGRAPH_DURATION = 4 * 20;
	private static final int TELEGRAPH_INTERVAL = 20;
	public static final double RIFT_STEP = 0.9;
	private static final int DELAY = 15 * 20;
	private static final int MAX_RANGE = 32;
	private static final int HALF_ANGLE = 30;
	private static final int DAMAGE_DIRECT = 40;
	public static final int DAMAGE_OVER_TIME = 5;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final EntityTargets mTargets;
	private final int mDuration;
	private final List<Location> mDangerousBlocks = new ArrayList<>();

	private final SpellCooldownManager mSpellCooldownManager;

	public SpellLiminalCorruption(Plugin plugin, LivingEntity boss, IntruderBoss.Dialogue dialogue, boolean enhanced) {
		mPlugin = plugin;
		mBoss = boss;
		mDialogue = dialogue;
		mDuration = enhanced ? 9 * 20 : 20 * 20;

		mTargets = new EntityTargets(EntityTargets.TARGETS.PLAYER, 30, EntityTargets.Limit.DEFAULT_ONE);
		mSpellCooldownManager = new SpellCooldownManager(enhanced ? 10 * 20 : 40 * 20, boss::isValid, boss::hasAI);
	}

	@Override
	public void run() {
		if (!canRun()) {
			return;
		}

		mSpellCooldownManager.setOnCooldown();
		mDangerousBlocks.clear();
		// Copied
		Location startLoc = mBoss.getLocation();
		mBoss.getWorld().playSound(startLoc, Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 3.0f, 1.66f, 25);
		mBoss.getWorld().playSound(startLoc, Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.5f, 0.5f);

		List<? extends LivingEntity> players = PlayerUtils.playersInRange(startLoc, MAX_RANGE * 2, true);
		List<? extends LivingEntity> targets = mTargets.getTargetsList(mBoss);
		List<Location> locs = new ArrayList<>();
		if (targets.isEmpty()) {
			return;
		}
		Location flatLocation = targets.get(0).getLocation();
		flatLocation.setY(startLoc.getY());

		Vector dir = startLoc.getDirection();
		for (int angle = -HALF_ANGLE; angle < HALF_ANGLE; angle++) {
			locs.add(startLoc.clone().add(VectorUtils.rotateYAxis(dir, angle)));
		}

		EntityUtils.selfRoot(mBoss, TELEGRAPH_DURATION);
		int i = 0;

		for (Location l : locs) {
			createRift(l, players, i++ % 2 == 0);
		}

		mDialogue.dialogue("LOST. IN THE DARKNESS.");

		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PPCircle(Particle.SQUID_INK, startLoc.clone().add(0, 0.5, 0), 1)
					.count(30)
					.delta(0.4, 0, 0)
					.rotateDelta(true)
					.directionalMode(true)
					.extra(1)
					.spawnAsBoss();

				new PPCircle(Particle.SQUID_INK, startLoc.clone().add(0, 0.5, 0), 1)
					.count(30)
					.spawnAsBoss();

				mTicks += TELEGRAPH_INTERVAL;
				mBoss.getWorld().playSound(startLoc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, 2.0f, 0.75f);
				mBoss.getWorld().playSound(startLoc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.2f, 0.1f);

				if (mTicks >= TELEGRAPH_DURATION) {
					this.cancel();
					new PartialParticle(Particle.FLASH, startLoc).minimumCount(1).spawnAsBoss();

					mBoss.getWorld().playSound(startLoc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.1f);
					mBoss.getWorld().playSound(startLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.4f, 0.1f);
					mBoss.getWorld().playSound(startLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 3.0f, 0.5f);
					mBoss.getWorld().playSound(startLoc, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 3.0f, 0.8f);
				}
			}
		}.runTaskTimer(mPlugin, 0, TELEGRAPH_INTERVAL));

		mActiveTasks.add(new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT += 5;
				for (LivingEntity target : players) {
					for (Location loc : mDangerousBlocks) {
						BoundingBox box = BoundingBox.of(loc, 0.85, 1.2, 0.85);
						if (target.getBoundingBox().overlaps(box)) {
							DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, DAMAGE_OVER_TIME, null, true);
							target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 4 * 20, 9));
							target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
							EffectManager.getInstance().addEffect(target, "LiminalCorruptionHealing", new PercentHeal(40, -0.7));
							break;
						}
					}
				}


				if (!mBoss.isValid() || mT >= mDuration) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 5));
	}


	private void createRift(Location loc, List<? extends LivingEntity> players, boolean telegrapher) {
		BukkitRunnable runnable = new BukkitRunnable() {
			final Location mLoc = mBoss.getLocation().add(0, -1, 0);
			final Vector mDir = LocationUtils.getDirectionTo(loc, mLoc).setY(0).normalize();
			final BoundingBox mBox = BoundingBox.of(mLoc, 0.85, 1.2, 0.85);
			final Location mOgLoc = mLoc.clone();

			@Override
			public void run() {
				if (!Double.isFinite(mDir.getX())) {
					mDir.setX(1).setY(0).setZ(0);
				}
				mBox.shift(mDir.clone().multiply(RIFT_STEP));
				Location bLoc = mBox.getCenter().toLocation(mLoc.getWorld());
				// Encountered non-solid block on floor: End of arena
				if (!bLoc.getBlock().isSolid()) {
					this.cancel();
					return;
				}

				// Super cool ultra new telegraph
				BukkitRunnable telRunnable = new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						Location pLoc = bLoc.clone().add(0, 1, 0);
						if (telegrapher) {
							new PartialParticle(Particle.CRIT, pLoc.clone().add(0, 0.2, 0))
								.delta(0.3, 0, 0.3)
								.spawnAsBoss();
						}

						mTicks += TELEGRAPH_INTERVAL;
						if (mTicks >= TELEGRAPH_DURATION) {
							if (telegrapher) {
								new PartialParticle(Particle.CRIT_MAGIC, pLoc)
									.delta(0.3, 0, 0.3)
									.extra(0.1)
									.spawnAsBoss();
							}
							Iterator<? extends LivingEntity> playersIterator = players.iterator();
							while (playersIterator.hasNext()) {
								LivingEntity target = playersIterator.next();
								if (target.getBoundingBox().overlaps(mBox)) {
									DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, DAMAGE_DIRECT, null, false, true, SPELL_NAME);
									playersIterator.remove();
								}
							}
							this.cancel();
							BlockData blockData = MATERIAL.createBlockData();
							blockData.rotate(FastUtils.getRandomElement(Arrays.stream(StructureRotation.values()).toList()));
							TemporaryBlockChangeManager.INSTANCE.changeBlock(bLoc.getBlock(), blockData, mDuration);
							mDangerousBlocks.add(bLoc.clone().toBlockLocation().add(0.5, 1, 0.5));

							bLoc.add(0, 0.5, 0);
						}
					}
				};
				telRunnable.runTaskTimer(mPlugin, 0, TELEGRAPH_INTERVAL);
				mActiveRunnables.add(telRunnable);

				if (bLoc.distance(mOgLoc) >= MAX_RANGE) {
					this.cancel();
				}
			}

		};

		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	public void forceOnCooldown() {
		mSpellCooldownManager.setOnCooldown(DELAY);
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown() && mBoss.hasAI();
	}

	@Override
	public int cooldownTicks() {
		return TELEGRAPH_DURATION + 20;
	}
}
