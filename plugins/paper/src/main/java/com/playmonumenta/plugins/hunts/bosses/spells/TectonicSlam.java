package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TectonicSlam extends Spell {
	// the time before the boss jumps, in ticks
	private static final int WINDUP_TIME = 15;

	private static final int COOLDOWN = 5 * 20;

	// the radius of the hitbox of the slam
	private static final int SLAM_RADIUS = 5;

	// the damage of the attack
	private static final int ATTACK_DAMAGE = 70;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Uamiel mUamiel;

	private final int mCooldownModifier;

	public TectonicSlam(Plugin plugin, LivingEntity boss, Uamiel uamiel, int cooldownModifier) {
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
		new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, mBoss.getLocation().clone().add(0, 0.2, 0))
			.count(20)
			.delta(1, 0.1, 1)
			.extra(0.01)
			.spawnAsBoss();
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.HOSTILE, 3f, 0.5f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_SKELETON_HORSE_GALLOP_WATER, SoundCategory.HOSTILE, 3f, 0.5f);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks == WINDUP_TIME) {
					Vector launchVelocity = mBoss.getVelocity().clone().setY(1);
					mBoss.setVelocity(launchVelocity);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 5f, 0.8f);
				}

				if (mTicks >= WINDUP_TIME + 5 && mBoss.isOnGround()) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3f, 0.7f);

					slamEffect(mBoss.getLocation().clone());

					Hitbox hitbox = new Hitbox.UprightCylinderHitbox(mBoss.getLocation(), 5, SLAM_RADIUS);
					for (Player player : hitbox.getHitPlayers(true)) {
						//DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, ATTACK_DAMAGE, null, false, true, "Tectonic Slam");
						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, ATTACK_DAMAGE, "Tectonic Slam", mBoss.getLocation(), Uamiel.SHIELD_STUN_TIME);
						MovementUtils.knockAway(mBoss.getLocation(), player, 0.5f, 0.25f, false);

						player.playSound(mBoss.getLocation(), Sound.ITEM_AXE_STRIP, SoundCategory.HOSTILE, 4f, 0.5f);
						player.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.HOSTILE, 2f, 0.8f);
					}

					this.cancel();
				}

				mTicks++;
				if (mBoss.isDead() || mTicks > 1000) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	// Visual effect for the slam
	private void slamEffect(Location location) {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			final Map<Integer, ArrayList<Location>> mLocationDelays = new HashMap<>();

			@Override
			public void run() {
				for (int i = 1; i <= SLAM_RADIUS; i++) {
					for (int c = 0; c < i * 2 * Math.PI * 0.35; c++) {
						int delay = FastUtils.randomIntInRange(0, 3) * 2 + (2 * i);
						double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);
						double r = i + FastUtils.randomDoubleInRange(-0.5, 0.5);
						Location l = location.clone().add(FastUtils.cos(theta) * r, -0.99, FastUtils.sin(theta) * r);
						List<Location> k = mLocationDelays.computeIfAbsent(delay, key -> new ArrayList<>());
						if (!k.contains(l)) {
							k.add(l);
						}
					}
				}

				if (mLocationDelays.containsKey(mTicks)) {
					mLocationDelays.get(mTicks).forEach(l -> {
						if (l.getBlock().getType() == Material.AIR && l.clone().add(0, -1, 0).getBlock().getType() == Material.AIR) {
							return;
						}
						Location nL = l.getBlock().getType() == Material.AIR ? l.clone().add(0, -1, 0) : l;
						FallingBlock b = location.getWorld().spawn(nL, FallingBlock.class, fb -> fb.setBlockData(Uamiel.DISPLAY_BLOCK_OPTIONS.get(FastUtils.randomIntInRange(0, Uamiel.DISPLAY_BLOCK_OPTIONS.size() - 1)).createBlockData()));
						Vector v = l.toVector().clone().subtract(location.toVector());
						v.normalize();
						v.setY(FastUtils.randomDoubleInRange(0.1, 0.25));
						b.setVelocity(v);
						b.setDropItem(false);
						EntityUtils.disableBlockPlacement(b);

						Location initialLocation = new Location(b.getWorld(), Math.round(b.getLocation().getX()), b.getLocation().getY(), Math.round(b.getLocation().getZ()));

						new BukkitRunnable() {
							int mBlockTicks = 0;

							@Override
							public void run() {
								// Remove the block if it goes too far
								if (b.getLocation().getX() > initialLocation.getX() + 0.6 || b.getLocation().getX() < initialLocation.getX() - 0.6 || b.getLocation().getZ() > initialLocation.getZ() + 0.6 || b.getLocation().getZ() < initialLocation.getZ() - 0.6) {
									b.remove();
								}

								mBlockTicks++;
								if (mBlockTicks > 10) {
									b.remove();
								}
							}
						}.runTaskTimer(mPlugin, 0, 1);
					});
				}

				if (mTicks < SLAM_RADIUS * 3 && mTicks % 3 == 0) {
					mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_NETHER_GOLD_ORE_BREAK, SoundCategory.HOSTILE, 4f, 0.9f - (mTicks * 0.015f));
				}

				mTicks++;
				if (mTicks > (SLAM_RADIUS + 1) * 2 + 8 || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN + mCooldownModifier;
	}
}
