package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GaianRoots extends Spell {
	// the length of time before the rocks are fired, in ticks
	private static final int WINDUP_DURATION = (int) (2.5 * 20);

	// the starting time to display target locations at, in ticks
	private static final int DISPLAY_TARGET_TIME = 20;

	// the amount of rocks to be fired
	private static final int TOTAL_ROCKS = 6;

	// the minimum and maximum range for a target location, in blocks
	private static final double MINIMUM_TARGET_RANGE = 8;
	private static final double MAXIMUM_TARGET_RANGE = 16;

	// the travel time each rock takes between the boss and the target location, in ticks
	private static final int ROCK_TRAVEL_TIME = 12;

	// the radius of the target location
	private static final double TARGET_RADIUS = 4.8;

	// the attack damage of being hit by a block as its moving
	private static final int ROCK_ATTACK_DAMAGE = 50;

	// the attack damage of being hit by the impact of a block
	private static final int IMPACT_ATTACK_DAMAGE = 4 * 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Uamiel mUamiel;
	private final World mWorld;

	private final int mCooldownModifier;

	public GaianRoots(Plugin plugin, LivingEntity boss, Uamiel uamiel, int cooldownModifier) {
		mPlugin = plugin;
		mBoss = boss;
		mUamiel = uamiel;
		mWorld = boss.getWorld();
		mCooldownModifier = cooldownModifier;
	}

	@Override
	public boolean canRun() {
		return mUamiel.canRunSpell(this);
	}

	@Override
	public void run() {
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, Uamiel.MOVEMENT_SPEED / 4);

		List<BlockDisplay> blocks = new ArrayList<>();
		double rockRadius = (double) TOTAL_ROCKS / 2.3;
		for (int i = 0; i < TOTAL_ROCKS; i++) {
			double theta = (Math.PI * 2) * ((double) i / TOTAL_ROCKS);
			Location location = mBoss.getLocation().clone().add(FastUtils.cos(theta) * rockRadius, 3.5, FastUtils.sin(theta) * rockRadius);
			BlockDisplay block = mWorld.spawn(location, BlockDisplay.class);
			block.setBlock(Uamiel.DISPLAY_BLOCK_OPTIONS.get(FastUtils.randomIntInRange(0, Uamiel.DISPLAY_BLOCK_OPTIONS.size() - 1)).createBlockData());
			block.setTransformation(new Transformation(new Vector3f(-0.5f, -0.5f, -0.5f), new Quaternionf(), new Vector3f(1f, 1f, 1f), new Quaternionf()));
			block.setBrightness(new Display.Brightness(15, 15));
			EntityUtils.setRemoveEntityOnUnload(block);
			blocks.add(block);
		}

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 3f, 0.5f);
		mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_BASALT_BREAK, SoundCategory.HOSTILE, 2.5f, 0.6f);

		new PPParametric(Particle.SMOKE_LARGE, mBoss.getLocation().clone().add(0, 3.5, 0), (parameter, builder) -> {
			double r = FastUtils.randomDoubleInRange(0, Math.PI * 2);
			Vector d = new Vector(FastUtils.cos(r), 0, FastUtils.sin(r));
			builder.offset(d.getX(), d.getY(), d.getZ());
		})
			.directionalMode(true)
			.count(80)
			.delta(0.4)
			.extra(0.5)
			.spawnAsBoss();

		List<Location> targetLocations = new ArrayList<>();
		for (int i = 0; i < TOTAL_ROCKS; i++) {
			double theta = FastUtils.randomDoubleInRange((Math.PI * 2) * ((double) i / TOTAL_ROCKS), (Math.PI * 2) * ((double) (i + 1) / TOTAL_ROCKS));
			double distance = FastUtils.randomDoubleInRange(MINIMUM_TARGET_RANGE, MAXIMUM_TARGET_RANGE);
			targetLocations.add(mBoss.getLocation().clone().add(FastUtils.cos(theta) * distance, 0, FastUtils.sin(theta) * distance));
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			final Map<BlockDisplay, Location> mBlockStartLocations = new HashMap<>();

			@Override
			public void run() {
				// make the blocks spin
				if (mTicks <= WINDUP_DURATION) {
					for (BlockDisplay block : blocks) {
						double r = (Math.PI * 2) * (((double) blocks.indexOf(block) / TOTAL_ROCKS) + ((double) mTicks / WINDUP_DURATION));
						Location location = mBoss.getLocation().clone().add(FastUtils.cos(r) * rockRadius, 3.5, FastUtils.sin(r) * rockRadius);
						block.teleport(location);
					}
				}

				// move the blocks
				if (mTicks == WINDUP_DURATION) {
					for (BlockDisplay block : blocks) {
						mBlockStartLocations.put(block, block.getLocation());
					}
				}
				if (mTicks > WINDUP_DURATION) {
					for (BlockDisplay block : blocks) {
						if (mBlockStartLocations.get(block) != null) {
							Location start = mBlockStartLocations.get(block).clone();
							Location difference = targetLocations.get(blocks.indexOf(block)).clone().subtract(start);
							Location nextStep = block.getLocation().clone().add(difference.getX() / 20, difference.getY() / 20, difference.getZ() / 20);
							block.teleport(nextStep);
						}

						new PartialParticle(Particle.END_ROD, block.getLocation()).count(1).extra(0).delta(0).spawnAsBoss();
					}
				}

				// telegraph target locations and paths
				if (mTicks % 10 == 0 && mTicks > DISPLAY_TARGET_TIME) {
					for (Location location : targetLocations) {
						new PPCircle(Particle.SCRAPE, location.clone().add(0, 0.75, 0), TARGET_RADIUS)
							.ringMode(true)
							.count(45)
							.extra(0.01)
							.spawnAsBoss();
						new PPCircle(Particle.REDSTONE, location.clone().add(0, 0.1, 0), TARGET_RADIUS)
							.data(new Particle.DustOptions(Color.fromRGB(81, 158, 134), 2.0f))
							.ringMode(true)
							.count(45)
							.spawnAsBoss();
					}
				}
				if (mTicks % 4 == 0 && mTicks > WINDUP_DURATION) {
					for (int i = 0; i < TOTAL_ROCKS; i++) {
						new PPLine(Particle.CRIT_MAGIC, targetLocations.get(i), blocks.get(i).getLocation())
							.countPerMeter(5)
							.delta(0.05)
							.extra(0.02)
							.spawnAsBoss();
					}
				}
				if (mTicks == WINDUP_DURATION) {
					new PPParametric(Particle.SMOKE_LARGE, mBoss.getLocation().clone().add(0, 3.5, 0), (parameter, builder) -> {
						double r = FastUtils.randomDoubleInRange(0, Math.PI * 2);
						Vector d = new Vector(FastUtils.cos(r), 0, FastUtils.sin(r));
						builder.offset(d.getX(), d.getY(), d.getZ());
					})
						.directionalMode(true)
						.count(80)
						.delta(0.4)
						.extra(0.5)
						.spawnAsBoss();
				}

				// sounds
				if (mTicks == WINDUP_DURATION) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 3f, 0.8f);
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_BASALT_BREAK, SoundCategory.HOSTILE, 1.8f, 0.6f);
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_BASALT_BREAK, SoundCategory.HOSTILE, 1.8f, 0.9f);
					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 2.1f, 0.65f);
				}
				if (mTicks == WINDUP_DURATION + ROCK_TRAVEL_TIME) {
					for (Location location : targetLocations) {
						mWorld.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.2f, 0.85f);
					}
				}

				// explosion effect
				if (mTicks == WINDUP_DURATION + ROCK_TRAVEL_TIME) {
					for (Location location : targetLocations) {
						new PPCircle(Particle.CAMPFIRE_COSY_SMOKE, location.clone().add(0, 0.4, 0), TARGET_RADIUS - 0.3)
							.ringMode(true)
							.countPerMeter(12)
							.extra(0)
							.delta(0.2)
							.spawnAsBoss();
						new PartialParticle(Particle.FLASH, location)
							.minimumCount(1)
							.count(1)
							.spawnAsBoss();
						new PartialParticle(Particle.LAVA, location.clone().add(0, 1, 0))
							.count(60)
							.delta(0.2)
							.extra(0.5)
							.spawnAsBoss();
						new PPCircle(Particle.EXPLOSION_NORMAL, location.clone().add(0, 0.8, 0), TARGET_RADIUS / 3 * 2)
							.ringMode(true)
							.countPerMeter(5)
							.extra(0.1)
							.delta(0.2)
							.spawnAsBoss();
					}
				}

				// check hitboxes
				if (mTicks > WINDUP_DURATION) {
					for (BlockDisplay block : blocks) {
						Hitbox hitbox = new Hitbox.AABBHitbox(mWorld, BoundingBox.of(block.getLocation(), 0.5, 0.5, 0.5));
						for (Player player : hitbox.getHitPlayers(true)) {
							BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.PROJECTILE, ROCK_ATTACK_DAMAGE, "Gaian Rock", block.getLocation(), Uamiel.SHIELD_STUN_TIME);
							MovementUtils.knockAway(mBoss.getLocation(), player, 1f, 0.25f, false);

							mWorld.playSound(player.getLocation(), Sound.BLOCK_SCULK_CATALYST_BREAK, SoundCategory.HOSTILE, 1.2f, 0.63f);
						}
					}
				}
				if (mTicks == WINDUP_DURATION + ROCK_TRAVEL_TIME) {
					for (Location location : targetLocations) {
						Hitbox hitbox = new Hitbox.UprightCylinderHitbox(location, TARGET_RADIUS, TARGET_RADIUS);
						for (Player player : hitbox.getHitPlayers(true)) {
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, IMPACT_ATTACK_DAMAGE, null, false, true, "Gaian Roots");
							MovementUtils.knockAway(location, player, 0.05f, 0.75f, false);

							mWorld.playSound(player.getLocation(), Sound.ITEM_AXE_STRIP, SoundCategory.HOSTILE, 1.2f, 0.63f);
						}
					}
				}

				mTicks++;
				if (mTicks > WINDUP_DURATION + ROCK_TRAVEL_TIME || mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				for (BlockDisplay block : blocks) {
					if (block.isValid()) {
						block.remove();
					}
				}
				EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MOVEMENT_SPEED, Uamiel.MOVEMENT_SPEED);
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return 80 + mCooldownModifier;
	}
}
