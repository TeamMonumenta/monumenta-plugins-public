package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import static com.playmonumenta.plugins.Constants.HALF_TICKS_PER_SECOND;
import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public final class SpellSoulShackle extends Spell {
	private static final String SPELL_NAME = "Soul Shackle";
	private static final int SHACKLE_RADIUS = 3;
	private static final int SHACKLE_HEIGHT = 7;
	private static final int SHACKLE_DURATION = TICKS_PER_SECOND * 5;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Location mCenter;
	private final double mRange;
	private final ChargeUpManager mChargeUp;
	private final List<UUID> mGotHit = new ArrayList<>();
	private final PartialParticle mPortal;
	private final PartialParticle mRod;
	private final PartialParticle mSpark;

	public SpellSoulShackle(final Plugin plugin, final LivingEntity boss, final Location loc, final double r) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = mBoss.getWorld();
		mCenter = loc;
		mRange = r;
		mChargeUp = Lich.defaultChargeUp(mBoss, TICKS_PER_SECOND, "Charging " + SPELL_NAME + "...");
		mPortal = new PartialParticle(Particle.PORTAL, mBoss.getLocation()).count(100).delta(0.1).extra(1.5);
		mRod = new PartialParticle(Particle.END_ROD, mBoss.getLocation()).count(40).delta(1);
		mSpark = new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation()).count(1);
	}

	@Override
	public void run() {
		final List<Player> players = Lich.playersInRange(mCenter, mRange, true);
		players.removeIf(p -> SpellDimensionDoor.getShadowed().contains(p));
		if (players.isEmpty()) {
			return;
		}

		mGotHit.clear();
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 6.0f, 1.0f);
		mPortal.location(mBoss.getLocation().add(0, 5, 0)).spawnAsEntityActive(mBoss);

		final BukkitRunnable spawnBullets = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.reset();
					mWorld.playSound(mBoss.getLocation().add(0, 5, 0), Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.HOSTILE, 3, 1);

					players.forEach(player -> {
						final Location loc = mBoss.getLocation().add(FastUtils.RANDOM.nextInt(3), 5, FastUtils.RANDOM.nextInt(3));
						mWorld.spawn(loc, ShulkerBullet.class, shulkerBullet -> {
							shulkerBullet.setTarget(player);
							shulkerBullet.setShooter(mBoss);
						});
					});
				}
			}
		};
		spawnBullets.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(spawnBullets);
	}

	@Override
	public void bossProjectileHit(final ProjectileHitEvent event) {
		if (!(event.getEntity() instanceof ShulkerBullet)) {
			return;
		}
		event.setCancelled(true);
		event.getEntity().remove();
		if (!(event.getHitEntity() instanceof final Player p) || mGotHit.contains(p.getUniqueId())) {
			return;
		}
		p.removePotionEffect(PotionEffectType.LEVITATION);
		mGotHit.add(p.getUniqueId());

		final Location centerLoc = p.getLocation().add(0, 1.5, 0);
		final PPCircle indicator = new PPCircle(Particle.END_ROD, centerLoc, SHACKLE_RADIUS).count(36);

		p.sendMessage(Component.text("You got chained by Hekawt! Don't move outside of the ring!", NamedTextColor.DARK_AQUA));
		DamageUtils.damage(mBoss, p, DamageType.MAGIC, 27, null, false, true, SPELL_NAME);
		AbilityUtils.silencePlayer(p, SHACKLE_DURATION);
		mRod.location(centerLoc).spawnAsEntityActive(mBoss);
		mWorld.playSound(centerLoc, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 0.7f, 0.5f);

		final ChargeUpManager shackleTimer = new ChargeUpManager(mBoss, SHACKLE_DURATION,
			Component.text(SPELL_NAME + " Duration", NamedTextColor.RED), BossBar.Color.RED,
			BossBar.Overlay.PROGRESS, Lich.detectionRange);
		shackleTimer.setTime(SHACKLE_DURATION);

		final BukkitRunnable hitDetection = new BukkitRunnable() {
			@Override
			public void run() {
				if (SpellDimensionDoor.getShadowed().contains(p)) {
					this.cancel();
					return;
				}

				if (shackleTimer.getTime() % HALF_TICKS_PER_SECOND == 0) {
					new PPLine(Particle.END_ROD, centerLoc, p.getLocation()).countPerMeter(10).delta(0.1).spawnAsEntityActive(mBoss);
					indicator.location(centerLoc).spawnAsEntityActive(mBoss);
					for (double n = -1; n < 2; n += 1) {
						mSpark.location(centerLoc.clone().add(0, n, 0)).spawnAsEntityActive(mBoss);
					}

					final List<Player> hitCheck = new Hitbox.UprightCylinderHitbox(centerLoc.clone()
						.add(0, -2.5, 0), SHACKLE_HEIGHT, SHACKLE_RADIUS).getHitPlayers(true);
					if (!hitCheck.contains(p)) {
						final Location shackleCenterGround = centerLoc.clone();
						shackleCenterGround.setY(mCenter.getY());

						DamageUtils.damage(mBoss, p, new DamageEvent.Metadata(DamageType.TRUE, null,
								null, SPELL_NAME), 0.15 * EntityUtils.getMaxHealth(p),
							false, false, true);
						MovementUtils.knockAway(shackleCenterGround, p, -0.75f, false);
						p.sendMessage(Component.text("I shouldn't leave this ring.", NamedTextColor.DARK_AQUA));
						mWorld.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 2, 1);
					}
				}

				if (shackleTimer.previousTick() || p.isDead() || Lich.phase3over() || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				shackleTimer.reset();
			}
		};
		hitDetection.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(hitDetection);
	}

	@Override
	public int cooldownTicks() {
		return TICKS_PER_SECOND * 8;
	}

	@Override
	public void cancel() {
		super.cancel();
		mGotHit.clear();
	}
}
