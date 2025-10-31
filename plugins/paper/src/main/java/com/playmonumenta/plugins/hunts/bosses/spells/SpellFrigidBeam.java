package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.AlocAcoc;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellFrigidBeam extends Spell {
	private static final double BEAM_RANGE = 30;

	private static final int TELEGRAPH_DURATION = 25;

	private static final int EXTRA_CAST_DELAY = 20;

	private static final double HITBOX_SIZE = 0.6;
	private static final int DAMAGE = 70;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final AlocAcoc mAlocAcoc;

	private final int mCasts;

	public SpellFrigidBeam(Plugin plugin, LivingEntity boss, AlocAcoc alocAcoc, int casts) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mAlocAcoc = alocAcoc;
		mCasts = casts;
	}

	@Override
	public boolean canRun() {
		return mAlocAcoc.canRunSpell(this);
	}

	@Override
	public void run() {
		EntityUtils.selfRoot(mBoss, TELEGRAPH_DURATION);

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), BEAM_RANGE, true);
		Collections.shuffle(players);

		for (int c = 0; c < mCasts; c++) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				for (int i = 0; i < beamCount(players.size()); i++) {
					Player player = players.get(i);
					Location bossLoc = LocationUtils.getHalfHeightLocation(mBoss);
					Location targetLoc;
					if (player.getLocation().getY() >= bossLoc.getY()) {
						targetLoc = player.getLocation();
					} else if (player.getLocation().getY() + player.getHeight() <= bossLoc.getY()) {
						targetLoc = player.getLocation().add(0, player.getHeight(), 0);
					} else {
						targetLoc = player.getLocation();
						targetLoc.setY(bossLoc.getY());
					}
					castBeam(targetLoc.subtract(bossLoc).toVector().normalize());
				}
			}, (long) c * EXTRA_CAST_DELAY);
		}
	}

	private void castBeam(Vector direction) {
		if (mBoss.isDead()) {
			return;
		}

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_SNOW_GOLEM_HURT, SoundCategory.HOSTILE, 3f, 0.5f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 3f, 1.6f);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks < TELEGRAPH_DURATION) {
					if (mTicks % 4 == 0) {
						Location start = LocationUtils.getHalfHeightLocation(mBoss);
						Location end = start.clone().add(direction.clone().multiply(BEAM_RANGE));
						new PPLine(Particle.SOUL_FIRE_FLAME, start, end)
							.directionalMode(true)
							.delta(direction.getX(), direction.getY(), direction.getZ())
							.extra(0.1)
							.countPerMeter(0.5)
							.spawnAsBoss();
						new PPLine(Particle.CRIT_MAGIC, start, end)
							.delta(0.2)
							.countPerMeter(8)
							.spawnAsBoss();
					}
				}

				if (mTicks == TELEGRAPH_DURATION) {
					Location center = LocationUtils.getHalfHeightLocation(mBoss);

					for (double i = 0; i < BEAM_RANGE; i += HITBOX_SIZE * 2) {
						Location hitboxPosition = center.clone().add(direction.clone().multiply(i));
						Hitbox hitbox = new Hitbox.AABBHitbox(mWorld, BoundingBox.of(hitboxPosition, HITBOX_SIZE, HITBOX_SIZE, HITBOX_SIZE));
						for (Player player : hitbox.getHitPlayers(true)) {
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.PROJECTILE, DAMAGE, null, false, false, "Frigid Beam");
							mAlocAcoc.mAura.addFrostbite(player, 0.2125f);
							player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.75f);
						}
					}

					Vector axis1 = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.3);
					Vector axis2 = direction.clone().crossProduct(axis1).normalize().multiply(0.3);
					new PPParametric(Particle.DUST_COLOR_TRANSITION, center, (parameter, builder) -> {
						double distance = parameter * BEAM_RANGE;
						double theta = (distance < 7 ? (1.9 * Math.pow(0.2 * distance, 3)) : 1.6 * distance - 6) % (Math.PI * 2);
						Location point = center.clone().add(direction.clone().multiply(distance))
							.add(axis1.clone().multiply(FastUtils.cos(theta) * 1.85))
							.add(axis2.clone().multiply(FastUtils.sin(theta) * 1.85));
						builder.location(point);
					})
						.data(new Particle.DustTransition(Color.fromRGB(0, 180, 240), Color.fromRGB(0, 40, 240), 1.35f))
						.count((int) (8 * BEAM_RANGE))
						.extra(0)
						.spawnAsBoss();
					new PPLine(Particle.SNOWFLAKE, center, center.clone().add(direction.clone().multiply(BEAM_RANGE)))
						.countPerMeter(2)
						.delta(0.8)
						.spawnAsBoss();
					new PPLine(Particle.SOUL_FIRE_FLAME, center, center.clone().add(direction.clone().multiply(BEAM_RANGE)))
						.countPerMeter(3)
						.delta(0.1)
						.spawnAsBoss();

					new PartialParticle(Particle.FLASH, center)
						.count(1)
						.spawnAsBoss();

					Location circleCenter = center.clone().add(direction.clone().multiply(2));
					new PPParametric(Particle.SNOWFLAKE, circleCenter, (parameter, builder) -> {
						double theta = parameter * Math.PI * 2;
						builder.location(circleCenter.clone()
							.add(axis1.clone().multiply(FastUtils.cos(theta) * 1.5))
							.add(axis2.clone().multiply(FastUtils.sin(theta) * 1.5)));
						Vector offset = axis1.clone().multiply(FastUtils.cos(theta + Math.PI / 2))
							.add(axis2.clone().multiply(FastUtils.sin(theta + Math.PI / 2)));
						builder.offset(offset.getX(), offset.getY(), offset.getZ());
					})
						.directionalMode(true)
						.count(25)
						.extra(0.5)
						.spawnAsBoss();

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.HOSTILE, 3f, 0.8f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 3f, 1.7f);
				}

				mTicks++;
				if (mTicks > TELEGRAPH_DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private int beamCount(int playerCount) {
		return (int) ((double) playerCount / 3.5 + 1);
	}

	@Override
	public int cooldownTicks() {
		return TELEGRAPH_DURATION + (mCasts - 1) * EXTRA_CAST_DELAY + 20;
	}
}
