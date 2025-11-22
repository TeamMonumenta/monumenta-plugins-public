package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MuddyShriek extends Spell {
	private static final int TELEGRAPH_DURATION = 35;

	private static final int CONE_ANGLE = 50;
	private static final int CONE_RANGE = 12;
	private static final int CONE_DELTA_Y = 3;
	private static final int SPREAD_STEP_DELAY = 2;

	private static final int MUD_TIME = 20 * 20;

	private static final int WORM_SPAWNERS = 1;
	private static final int HIGH_PLAYER_WORM_SPAWNERS = 2;

	private static final int ATTACK_DAMAGE = 75;

	private static final String SLOWNESS_TAG = "ExperimentShriekSlowness";
	private static final int SLOWNESS_DURATION = 4 * 20;
	private static final double SLOWNESS_AMOUNT = 0.35;


	private final Plugin mPlugin;
	private final com.playmonumenta.plugins.Plugin mMonumentaPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final ExperimentSeventyOne mExperimentSeventyOne;

	private final boolean mPermanentMud;

	private final int mCooldownModifier;

	public MuddyShriek(Plugin plugin, LivingEntity boss, ExperimentSeventyOne experimentSeventyOne, boolean permanentMud, int cooldownModifier) {
		mPlugin = plugin;
		mMonumentaPlugin = com.playmonumenta.plugins.Plugin.getInstance();
		mBoss = boss;
		mWorld = boss.getWorld();
		mExperimentSeventyOne = experimentSeventyOne;
		mPermanentMud = permanentMud;
		mCooldownModifier = cooldownModifier;
	}

	@Override
	public boolean canRun() {
		return mExperimentSeventyOne.canRunSpell(this);
	}

	@Override
	public void run() {
		EntityUtils.selfRoot(mBoss, TELEGRAPH_DURATION + SPREAD_STEP_DELAY * CONE_RANGE);

		List<Player> possibleTargets = PlayerUtils.playersInRange(mBoss.getLocation(), 20, true);
		if (possibleTargets.isEmpty()) {
			return;
		}
		Vector target = possibleTargets.get(FastUtils.randomIntInRange(0, possibleTargets.size() - 1)).getLocation().toVector().subtract(mBoss.getLocation().toVector());
		target.setY(0).normalize();

		Vector minAngle = VectorUtils.rotateYAxis(target.clone(), -CONE_ANGLE).multiply(CONE_RANGE + 2);
		Vector maxAngle = VectorUtils.rotateYAxis(target.clone(), CONE_ANGLE).multiply(CONE_RANGE + 2);
		for (int i = 0; i < 2; i++) {
			Location center = mBoss.getLocation().clone().add(0, i, 0);
			new PPLine(Particle.SMOKE_NORMAL, center, center.clone().add(minAngle))
				.countPerMeter(15)
				.spawnAsBoss();
			new PPLine(Particle.WAX_ON, center, center.clone().add(minAngle))
				.countPerMeter(15)
				.spawnAsBoss();
			new PPLine(Particle.SMOKE_NORMAL, center, center.clone().add(maxAngle))
				.countPerMeter(15)
				.spawnAsBoss();
			new PPLine(Particle.WAX_ON, center, center.clone().add(maxAngle))
				.countPerMeter(15)
				.spawnAsBoss();
			new PPParametric(Particle.SMOKE_NORMAL, center, (parameter, builder) -> {
				Vector direction = VectorUtils.rotateYAxis(minAngle.clone().normalize(), parameter * CONE_ANGLE * 2).multiply(CONE_RANGE + 2);
				builder.location(center.clone().add(direction));
			})
				.count(CONE_ANGLE * 4)
				.spawnAsBoss();
			new PPParametric(Particle.WAX_ON, center, (parameter, builder) -> {
				Vector direction = VectorUtils.rotateYAxis(minAngle.clone().normalize(), parameter * CONE_ANGLE * 2).multiply(CONE_RANGE + 2);
				builder.location(center.clone().add(direction));
			})
				.count(CONE_ANGLE * 4)
				.spawnAsBoss();
		}

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 5f, 0.8f);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> attack(mBoss.getLocation().clone(), target), TELEGRAPH_DURATION);
	}

	private void attack(Location start, Vector target) {
		Vector[] angles = new Vector[21];
		for (int i = 0; i < angles.length; i++) {
			angles[i] = VectorUtils.rotateYAxis(target.clone(), -CONE_ANGLE + i * (CONE_ANGLE / (((double) angles.length - 1) / 2)));
		}

		mWorld.playSound(start, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 5f, 0.8f);

		List<Integer> allRanges = new ArrayList<>();
		for (int i = 1; i <= CONE_RANGE; i++) {
			allRanges.add(i);
		}
		Collections.shuffle(allRanges);
		int players = PlayerUtils.playersInRange(mExperimentSeventyOne.mSpawnLoc, ExperimentSeventyOne.OUTER_RADIUS, true).size();
		List<Integer> validRanges = allRanges.subList(0, players >= ExperimentSeventyOne.HIGH_PLAYER_CUTOFF ? HIGH_PLAYER_WORM_SPAWNERS : WORM_SPAWNERS);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mStep = 1;
			final List<Player> mHitPlayers = new ArrayList<>();

			@Override
			public void run() {
				if (mStep > CONE_RANGE || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				List<Block> totalLevelBlocks = new ArrayList<>();
				for (Vector angle : angles) {
					Location hit = start.clone().add(angle.clone().multiply(mStep));
					Block hitBlock = hit.getBlock();
					if (!totalLevelBlocks.contains(hitBlock)) {
						totalLevelBlocks.add(hitBlock);
					}
				}

				List<Block> mudBlocks = new ArrayList<>();
				for (Block levelBlock : totalLevelBlocks) {
					for (int i = 0; i < CONE_DELTA_Y * 2; i++) {
						Block block = levelBlock.getRelative(0, i - CONE_DELTA_Y, 0);
						if (BlockUtils.isExposed(block)) {
							if (block.isSolid()) {
								mudBlocks.add(block);
							} else {
								block.breakNaturally();
							}
						}
					}
				}

				if (validRanges.contains(mStep)) {
					Block spawner = FastUtils.getRandomElement(mudBlocks);
					mudBlocks.remove(spawner);
					mExperimentSeventyOne.placeWormSpawner(spawner);
				}
				if (mPermanentMud) {
					mudBlocks.forEach(mExperimentSeventyOne::placeMudBlock);
				} else {
					mudBlocks.forEach((block) -> mExperimentSeventyOne.placeMudBlock(block, MUD_TIME));
				}

				Hitbox hitbox = Hitbox.approximateCone(start.clone().setDirection(target).add(0, 1, 0), mStep, Math.toRadians(CONE_ANGLE));
				for (Player player : hitbox.getHitPlayers(true)) {
					if (!mHitPlayers.contains(player)) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.PROJECTILE, ATTACK_DAMAGE, null, false, true, "Muddy Shriek");
						mMonumentaPlugin.mEffectManager.addEffect(player, SLOWNESS_TAG, new PercentSpeed(SLOWNESS_DURATION, -SLOWNESS_AMOUNT, SLOWNESS_TAG));

						player.playSound(player.getLocation(), Sound.ENTITY_TURTLE_EGG_CRACK, SoundCategory.HOSTILE, 2f, 0.8f);
						mHitPlayers.add(player);
					}
				}

				mStep++;
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, SPREAD_STEP_DELAY);
	}

	@Override
	public int cooldownTicks() {
		return 30 + TELEGRAPH_DURATION + SPREAD_STEP_DELAY * CONE_RANGE + mCooldownModifier;
	}
}
