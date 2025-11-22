package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPBezier;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
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
import org.bukkit.util.Vector;

public class ShellRupture extends Spell {
	// cooldown times between attacks
	private static final int RANDOM_ATTACK_MIN = 35;
	private static final int RANDOM_ATTACK_MAX = 45;

	private static final int TELEGRAPH_DURATION = 40;

	// distance that the boss can target nearby players
	private static final double TARGET_RANGE = 5.5;

	// half of the total angle the attack takes up
	private static final double ATTACK_ANGLE = 35;
	private static final double ATTACK_RANGE = 6;

	private static final double ATTACK_DAMAGE = 80;

	// information about the weakness effect
	private static final String WEAKNESS_TAG = "ImpenetrableMeleeWeakness";
	private static final int WEAKNESS_DURATION = 5 * 20;
	private static final double WEAKNESS_AMOUNT = 0.35;

	private final Plugin mPlugin;
	private final com.playmonumenta.plugins.Plugin mMonumentaPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;

	private int mTicks = 0;
	private int mAttackDelay;

	public ShellRupture(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mMonumentaPlugin = com.playmonumenta.plugins.Plugin.getInstance();
		mBoss = boss;
		mWorld = boss.getWorld();

		setAttackDelay();
	}

	@Override
	public void run() {
		if (mTicks == mAttackDelay) {
			mTicks = 0;
			setAttackDelay();

			runAttack();
		} else {
			mTicks++;
		}
	}

	private void runAttack() {
		List<Player> targets = PlayerUtils.playersInRange(mBoss.getLocation(), TARGET_RANGE, false);
		if (targets.isEmpty()) {
			return;
		}

		Player target = targets.get(FastUtils.randomIntInRange(0, targets.size() - 1));

		Location baseLocation = mBoss.getLocation().clone().add(0, 0.25, 0);
		Vector attackDirection = target.getLocation().clone().subtract(mBoss.getLocation().clone()).toVector()
			.setY(0)
			.normalize()
			.multiply(ATTACK_RANGE);

		Location end1 = baseLocation.clone().add(VectorUtils.rotateYAxis(attackDirection, ATTACK_ANGLE));
		Location end2 = baseLocation.clone().add(VectorUtils.rotateYAxis(attackDirection, -ATTACK_ANGLE));

		mWorld.playSound(baseLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 2f, 0.85f);
		mWorld.playSound(baseLocation, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.HOSTILE, 2f, 0.7f);
		mWorld.playSound(baseLocation, Sound.ENTITY_TURTLE_EGG_CRACK, SoundCategory.HOSTILE, 2f, 0.9f);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks % 5 == 0) {
					Color color = mTicks < TELEGRAPH_DURATION - 20 ? Color.fromRGB(201, 88, 209) : Color.fromRGB(224, 50, 7);

					new PPLine(Particle.REDSTONE, baseLocation, end1)
						.countPerMeter(10)
						.data(new Particle.DustOptions(color, 0.7f))
						.spawnAsBoss();
					new PPLine(Particle.REDSTONE, baseLocation, end2)
						.countPerMeter(10)
						.data(new Particle.DustOptions(color, 0.7f))
						.spawnAsBoss();
					new PPBezier(Particle.REDSTONE, List.of(end1, baseLocation.clone().add(attackDirection), end2))
						.count((int) (Math.toRadians(ATTACK_ANGLE) * ATTACK_RANGE * 7))
						.data(new Particle.DustOptions(color, 0.7f))
						.spawnAsBoss();
				}

				if (mTicks == TELEGRAPH_DURATION - 18) {
					mWorld.playSound(baseLocation, Sound.BLOCK_ANVIL_USE, SoundCategory.HOSTILE, 2f, 0.9f);
				}

				mTicks++;
				if (mTicks == TELEGRAPH_DURATION) {
					mWorld.playSound(baseLocation, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.HOSTILE, 2f, 0.9f);
					mWorld.playSound(baseLocation, Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.HOSTILE, 2f, 1.4f);
					mWorld.playSound(baseLocation, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 2f, 1.1f);

					new PartialParticle(Particle.SWEEP_ATTACK, baseLocation.clone().add(attackDirection.clone().normalize()))
						.count(4)
						.delta(0.05)
						.spawnAsBoss();

					new PPParametric(Particle.WAX_OFF, baseLocation, (parameter, builder) -> {
						double dist = FastUtils.randomDoubleInRange(0, 1);
						Location location = baseLocation.clone().add(VectorUtils.rotateYAxis(attackDirection.clone().multiply(dist), FastUtils.randomDoubleInRange(-ATTACK_ANGLE, ATTACK_ANGLE)));
						Vector offset = attackDirection.clone().normalize().multiply(dist * 0.5 + 1.5).add(new Vector(0, dist + 0.3, 0));
						builder.location(location);
						builder.offset(offset.getX(), offset.getY(), offset.getZ());
					})
						.count(100)
						.extra(5)
						.directionalMode(true)
						.spawnAsBoss();

					new PPParametric(Particle.CLOUD, baseLocation, (parameter, builder) -> {
						double dist = FastUtils.randomDoubleInRange(0, 1);
						Location location = baseLocation.clone().add(VectorUtils.rotateYAxis(attackDirection.clone().multiply(dist), FastUtils.randomDoubleInRange(-ATTACK_ANGLE, ATTACK_ANGLE)));
						builder.location(location);
					})
						.count(50)
						.spawnAsBoss();

					Hitbox hitbox = Hitbox.approximateCone(baseLocation.clone().setDirection(attackDirection), ATTACK_RANGE, Math.toRadians(ATTACK_ANGLE));
					for (Player player : hitbox.getHitPlayers(true)) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MELEE, ATTACK_DAMAGE, null, false, true, "Rupturing Shell");
						mMonumentaPlugin.mEffectManager.addEffect(player, WEAKNESS_TAG, new PercentDamageDealt(WEAKNESS_DURATION, -WEAKNESS_AMOUNT));
					}

					this.cancel();
				}
				if (mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void setAttackDelay() {
		mAttackDelay = TELEGRAPH_DURATION + FastUtils.randomIntInRange(RANDOM_ATTACK_MIN, RANDOM_ATTACK_MAX);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
