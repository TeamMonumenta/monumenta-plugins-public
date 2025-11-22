package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class SpellPsychicMiasma extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final SpellCooldownManager mSpellCooldownManager;

	private static final String SPELL_NAME = "Psychic Miasma";
	private static final int DELAY = 8 * 20;
	private static final int CAST_TIME = 9 * 20;
	private static final int SUMMON_COUNT = 6;
	private static final int CAST_INTERVAL = CAST_TIME / SUMMON_COUNT;
	private static final int DAMAGE = 35;
	private static final int SIZE = 5;
	private static final float KNOCKBACK = 1.1f;
	private static final double SPEED = 0.2;
	private static final double SPEED_HIT = 0.34;
	private static final List<Integer> OFFSETS;
	private static final List<Integer> DIRECTIONS;
	public static final String TAG = "PsychicMiasma";

	// Janky randomization method to prevent weird overlaps
	static {
		List<Integer> tempOffsets = new ArrayList<>();
		List<Integer> tempCounts = new ArrayList<>();
		for (int i = -25; i < 25; i += SIZE) {
			tempOffsets.add(i);
		}
		for (int i = 0; i < Math.ceil((double) SUMMON_COUNT / 4) * 4; i++) {
			tempCounts.add(i);
		}
		OFFSETS = List.copyOf(tempOffsets);
		DIRECTIONS = List.copyOf(tempCounts);
	}

	private int mCount = 0;
	private final List<Integer> mShuffledOffsets = new ArrayList<>(OFFSETS);
	private final List<Integer> mShuffledDirection = new ArrayList<>(DIRECTIONS);

	public SpellPsychicMiasma(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mSpellCooldownManager = new SpellCooldownManager(30 * 20, DELAY, mBoss::isValid, mBoss::hasAI);
	}

	@Override
	public void run() {
		if (!canRun()) {
			return;
		}
		cast();
	}

	private void cast() {
		mCount = 0;
		Collections.shuffle(mShuffledOffsets);
		Collections.shuffle(mShuffledDirection);
		mSpellCooldownManager.setOnCooldown();
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_ROAR, SoundCategory.HOSTILE, 1.2f, 0.25f);
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 2.0f, 0.1f, 20);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks > CAST_TIME) {
					this.cancel();
				} else if (mTicks % CAST_INTERVAL == 0) {

					Location spawnLoc = mCenter.clone();
					Vector direction = switch (mShuffledDirection.get(mCount++) % 4) {
						case 0 -> {
							spawnLoc.add(new Vector(35, 0, getRandomHorizontal(mCount)));
							yield new Vector(-1, 0, 0);
						}
						case 1 -> {
							spawnLoc.add(new Vector(-35, 0, -getRandomHorizontal(mCount)));
							yield new Vector(1, 0, 0);
						}
						case 2 -> {
							spawnLoc.add(new Vector(getRandomHorizontal(mCount), 0, 35));
							yield new Vector(0, 0, -1);
						}
						case 3 -> {
							spawnLoc.add(new Vector(-getRandomHorizontal(mCount), 0, -35));
							yield new Vector(0, 0, 1);
						}
						default -> throw new IllegalStateException("Unexpected value!");
					};
					spawnLoc.setYaw(0);
					spawnLoc.setPitch(0);

					BlockDisplay display = mBoss.getWorld().spawn(spawnLoc, BlockDisplay.class);
					display.setBlock(Bukkit.createBlockData(Material.PINK_STAINED_GLASS));
					display.setBrightness(new Display.Brightness(15, 15));
					display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(SIZE, SIZE, 0.5f), new AxisAngle4f()));
					display.setInterpolationDelay(-1);
					display.addScoreboardTag(TAG);
					EntityUtils.setRemoveEntityOnUnload(display);

					mBoss.getWorld().playSound(spawnLoc, Sound.ENTITY_BREEZE_DEATH, SoundCategory.HOSTILE, 2.2f, 0.25f);

					double[] yawPitch = VectorUtils.vectorToRotation(direction);
					display.setRotation((float) yawPitch[0], (float) yawPitch[1]);

					BukkitRunnable miasmaRunnable = new BukkitRunnable() {
						final List<Player> mPlayers = IntruderBoss.playersInRange(mBoss.getLocation());
						final Vector mCorner = VectorUtils.rotateYAxis(direction, -90).multiply(SIZE);
						double mBlocks = 0;
						double mSpeed = SPEED;

						@Override
						public void run() {
							mBlocks += mSpeed;
							display.teleport(display.getLocation().add(direction.clone().multiply(mSpeed)));

							Location wallLoc = display.getLocation();
							BoundingBox box = BoundingBox.of(wallLoc, wallLoc.clone().add(mCorner.clone().add(new Vector(0, SIZE, 0))));
							new PPLine(Particle.REDSTONE, wallLoc, wallLoc.clone().add(mCorner)).countPerMeter(1).delta(0.2).data(new Particle.DustOptions(Color.fromRGB(0xec1023), 0.9f)).spawnAsBoss();

							display.getWorld().playSound(wallLoc.clone().add(mCorner.clone().multiply(0.5)), Sound.ENTITY_WARDEN_ANGRY, SoundCategory.HOSTILE, 0.3f, 2.0f);

							mPlayers.stream().filter(player -> player.getBoundingBox().overlaps(box)).forEach(player -> {
								player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_HURT, SoundCategory.HOSTILE, 0.5f, 0.1f);
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false, SPELL_NAME);
								MovementUtils.knockAway(player.getLocation().subtract(direction), player, KNOCKBACK, false);
								mSpeed = SPEED_HIT;
							});
							if (mBlocks > 70) {
								this.cancel();
							}
						}

						@Override
						public synchronized void cancel() throws IllegalStateException {
							Bukkit.getScheduler().runTaskLater(mPlugin, display::remove, 1);
							super.cancel();
						}
					};
					miasmaRunnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(miasmaRunnable);
				}
				mTicks++;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private int getRandomHorizontal(int count) {
		return mShuffledOffsets.get(count % mShuffledOffsets.size());
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown() && mBoss.hasAI();
	}

	public void forceOnCooldown() {
		mSpellCooldownManager.setOnCooldown(DELAY);
	}

	public void forceCast() {
		cancel();
		cast();
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
