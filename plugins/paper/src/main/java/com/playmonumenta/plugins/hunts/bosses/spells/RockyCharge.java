package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RockyCharge extends Spell {
	// the length of time between the telegraph and the boss charging
	private static final int WINDUP_TIME = 20;

	// the speed of the boss during the charge, in blocks per tick
	private static final double CHARGE_SPEED = 0.75;

	private static final double TARGET_DISTANCE = 10;

	// the longest time the boss can charge for, in ticks
	private static final int MAX_CHARGE_TIME = 20 * 5;

	private static final int ATTACK_DAMAGE = 75;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Uamiel mUamiel;
	private final World mWorld;

	private final int mCooldownModifier;

	public RockyCharge(Plugin plugin, LivingEntity boss, Uamiel uamiel, int cooldownModifier) {
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
		List<Player> validPlayerTargets = PlayerUtils.playersInRange(mBoss.getLocation(), TARGET_DISTANCE, false);
		if (validPlayerTargets.isEmpty()) {
			return;
		}

		Location targetLocation = validPlayerTargets.get(FastUtils.randomIntInRange(0, validPlayerTargets.size() - 1)).getLocation().clone();
		targetLocation.add(new Vector(0, 0.25, 0));
		charge(targetLocation);
	}

	public void charge(Location targetLocation) {
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_CAMEL_HURT, SoundCategory.HOSTILE, 3f, 0.65f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.HOSTILE, 3f, 0.5f);

		Vector direction = targetLocation.clone().subtract(mBoss.getLocation()).toVector().normalize();

		int chargeTime = Math.min((int) Math.ceil(targetLocation.distance(mBoss.getLocation()) / CHARGE_SPEED), MAX_CHARGE_TIME);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mBoss instanceof Creature creature) {
					creature.setTarget(null);
					creature.getPathfinder().stopPathfinding();
					creature.lookAt(targetLocation);
				}

				if (mTicks % 5 == 0) {
					new PPLine(Particle.SCRAPE, mBoss.getLocation().clone().add(0, 1.5, 0), targetLocation.clone().add(0, 1.5, 0)).countPerMeter(10).delta(0.5).extra(0.05).spawnAsBoss();
				}

				if (mTicks == WINDUP_TIME) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 5f, 0.8f);
				}

				if (mTicks > WINDUP_TIME) {
					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 4, true)) {
						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, ATTACK_DAMAGE, "Rocky Charge", mBoss.getLocation(), Uamiel.SHIELD_STUN_TIME);
						MovementUtils.knockAway(mBoss.getLocation(), player, 1f, 0.25f, false);

						mWorld.playSound(mBoss.getLocation(), Sound.ITEM_AXE_STRIP, SoundCategory.HOSTILE, 1.5f, 0.67f);
					}

					mBoss.teleport(mBoss.getLocation().clone().add(direction.clone().multiply(CHARGE_SPEED)));
				}

				mTicks++;
				if (mTicks >= WINDUP_TIME + chargeTime || mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				if (mBoss instanceof Creature creature) {
					creature.setTarget(EntityUtils.getNearestPlayer(mBoss.getLocation(), 15));
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return 100 + mCooldownModifier;
	}
}
