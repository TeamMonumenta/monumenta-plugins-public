package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.LucidRendBoss;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellParasomnicMist extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final IntruderBoss.Dialogue mDialogue;
	private final Team mUnpushableTeam;
	@Nullable
	private SafeZone mSafeZone;

	private static final int CHANGE_DIRECTION_INTERVAL = 3 * 20;
	private static final double SPEED = 0.175;

	private static final int RANGE = 50;
	private static final int CHARGE_UP_TIME = 6 * 20;
	private static final int DURATION = 15 * 20;

	private double mSafeZoneRadius = 3;
	private final ChargeUpManager mChargeUpManager;
	private static final String SPELL_NAME = "Parasomnic Mist (☠)";

	public SpellParasomnicMist(Plugin plugin, LivingEntity boss, Location center, IntruderBoss.Dialogue dialogue) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mDialogue = dialogue;
		mChargeUpManager = new ChargeUpManager(mBoss, CHARGE_UP_TIME, Component.text("Charging ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, RANGE);
		mUnpushableTeam = ScoreboardUtils.getExistingTeamOrCreate(TrackingManager.UNPUSHABLE_TEAM);
	}

	@Override
	public void run() {
		mSafeZoneRadius = mBoss.getScoreboardTags().contains(IntruderBoss.STALKER_ACTIVE_TAG) ? 4 : 3;
		mDialogue.dialogue(2 * 20, List.of("THERE IS TOO MUCH. NOISE. YOU NEED. TO SINK.", "DEEPER. INTO. THE MIST."));
		mChargeUpManager.setTitle(Component.text("Preparing ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
		mChargeUpManager.setColor(BossBar.Color.YELLOW);
		mChargeUpManager.setChargeTime(CHARGE_UP_TIME);

		List<Player> players = IntruderBoss.playersInRange(mBoss.getLocation());

		List<Entity> lucidRends = new ArrayList<>(EntityUtils.getNearbyMobs(mBoss.getLocation(), IntruderBoss.DETECTION_RANGE, IntruderBoss.DETECTION_RANGE, IntruderBoss.DETECTION_RANGE,
			entity -> entity.getScoreboardTags().contains("LucidRend")));
		lucidRends.forEach(entity -> {
			entity.addScoreboardTag(LucidRendBoss.DISABLE_TAG);
			EffectManager.getInstance().addEffect(entity, "MistSlow", new PercentSpeed(CHARGE_UP_TIME + DURATION, -1.0, "MistSlow"));
			EntityUtils.teleportStack(entity, mCenter.clone().subtract(0, 10, 0));
		});

		players.forEach(mUnpushableTeam::addPlayer);

		mActiveTasks.add(new BukkitRunnable() {
			boolean mCasting = false;

			@Override
			public void run() {
				if (mCasting) {
					if (mChargeUpManager.previousTick(1)) {
						mChargeUpManager.remove();
						players.forEach(player -> {
							player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.4f, 0.1f);
							player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 5, 1, true, false));
							player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 5, 1, true, false));
						});
						lucidRends.forEach(entity -> {
							entity.removeScoreboardTag(LucidRendBoss.DISABLE_TAG);
							EffectManager.getInstance().clearEffects(entity, "MistSlow");
							EntityUtils.teleportStack(entity, mCenter.clone().add(0, 1, 0));
						});
						this.cancel();
					}
				} else {
					if (mChargeUpManager.nextTick(1)) {
						mChargeUpManager.setTitle(Component.text("Unleashing  ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
						mChargeUpManager.setColor(BossBar.Color.RED);
						mChargeUpManager.setTime(DURATION);
						mChargeUpManager.setChargeTime(DURATION);
						mCasting = true;
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));

		mBoss.setAI(false);
		mBoss.setInvulnerable(true);
		mBoss.setInvisible(true);
		replaceBlocks(mCenter.clone().subtract(0, 1, 0));


		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.DUST_COLOR_TRANSITION, mCenter)
					.count(400)
					.delta(RANGE / 2.0)
					.data(new Particle.DustTransition(Color.BLACK, Color.fromRGB(0x6b0000), 5.0f))
					.spawnAsBoss();

				players.forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.3f, 0.75f + (float) mTicks / CHARGE_UP_TIME));

				mTicks += 5;
				if (mTicks >= CHARGE_UP_TIME) {
					this.cancel();
					masterTask(mCenter);
					new PartialParticle(Particle.SQUID_INK, mBoss.getLocation())
						.count(30)
						.extra(0.7)
						.spawnAsBoss();

					mBoss.setInvisible(false);
					mBoss.teleport(mCenter.clone().subtract(0, 15, 0));
					players.forEach(player -> player.hideEntity(mPlugin, mBoss));
				}
			}
		}.runTaskTimer(mPlugin, 0, 5));

		mSafeZone = new SafeZone(LocationUtils.randomLocationInCircle(mCenter, 15));
	}

	private class SafeZone {
		private final Location mLocation;

		public SafeZone(Location location) {
			mLocation = location;
			safeZoneTask();
			movementRunnable();
			new PPPillar(Particle.END_ROD, location, 20)
				.count(90)
				.delta(0.1)
				.spawnAsBoss();
		}

		public void safeZoneTask() {
			mActiveTasks.add(new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks % 4 == 0) {
						if (mTicks <= CHARGE_UP_TIME) {
							new PPCircle(Particle.SOUL_FIRE_FLAME, mLocation, mSafeZoneRadius)
								.countPerMeter(2)
								.spawnAsBoss();

							new PPCircle(Particle.SOUL_FIRE_FLAME, mLocation, mSafeZoneRadius)
								.directionalMode(true)
								.delta(0, 1, 0)
								.countPerMeter(2)
								.extraRange(0.15, 0.25)
								.spawnAsBoss();
						} else {
							new PPCircle(Particle.SOUL_FIRE_FLAME, mLocation, mSafeZoneRadius)
								.countPerMeter(2)
								.directionalMode(true)
								.delta(0.0, -1, 0)
								.extra(0.05)
								.spawnAsBoss();
						}

						new PartialParticle(Particle.END_ROD, mLocation)
							.count(3)
							.delta(mSafeZoneRadius / 2.0)
							.spawnAsBoss();
					}

					mTicks++;
					if (mTicks >= CHARGE_UP_TIME + DURATION) {
						mLocation.getWorld().playSound(mLocation, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 1.5f, 1.6f);
						new PartialParticle(Particle.FLASH, mLocation).minimumCount(1).spawnAsBoss();
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1));
		}

		public void movementRunnable() {
			mActiveTasks.add(new BukkitRunnable() {
				int mTicks = 0;
				Vector mDirection = VectorUtils.randomHorizontalUnitVector();

				@Override
				public void run() {
					// If it should change directions
					if (mTicks % CHANGE_DIRECTION_INTERVAL == 0) {
						int mSafety = 0;
						do {
							mSafety++;
							mDirection = VectorUtils.randomHorizontalUnitVector();
							if (mSafety > 150) {
								mDirection = new Vector();
								break;
							}
						} while (willRunIntoWall(mDirection));
						mLocation.getWorld().playSound(mLocation, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 2.0f, 2.0f);
					}
					double velocity = Math.abs(FastUtils.sin(mTicks * Math.PI / CHANGE_DIRECTION_INTERVAL)) * SPEED;
					mLocation.add(mDirection.clone().multiply(velocity));
					mTicks++;
					if (mTicks > DURATION) {
						this.cancel();
					}
				}

				private boolean willRunIntoWall(Vector direction) {
					// By integration, the location of the circle after CHANGE_DIRECTION_INTERVAL
					Location nextLoc = mLocation.clone().add(direction.clone().multiply(2 * SPEED * CHANGE_DIRECTION_INTERVAL / Math.PI));
					return nextLoc.distance(mCenter) >= 25 - mSafeZoneRadius || nextLoc.getBlock().isSolid();
				}
			}.runTaskTimer(mPlugin, CHARGE_UP_TIME, 1));
		}
	}

	public void masterTask(Location mCenter) {
		List<Player> players = IntruderBoss.playersInRange(mBoss.getLocation());
		players.forEach(player -> {
			player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, DURATION, 1, true, false, false));
			EffectManager.getInstance().addEffect(player, "ParasomnicAnemia", new PercentHeal(DURATION, -1.0));
		});


		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.DUST_COLOR_TRANSITION, mCenter)
					.count(175)
					.delta(RANGE / 2.0)
					.data(new Particle.DustTransition(Color.RED, Color.fromRGB(0x6b0000), 5.0f))
					.spawnAsBoss();

				players.forEach(player -> {
					player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, 0.5f, 0.1f + (float) mTicks / DURATION);
				});
				players.stream()
					.filter(player -> mSafeZone == null || mSafeZone.mLocation.distance(player.getLocation()) > mSafeZoneRadius)
					.forEach(player -> {
						// Damage
						if (mTicks % 10 == 0) {
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, 4, null, true);
						}
					});

				if (mTicks > DURATION) {
					this.cancel();

					finishSpell();
				}
				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2));
	}

	private void finishSpell() {
		mBoss.setAI(true);
		mBoss.setInvulnerable(false);
		IntruderBoss.playersInRange(mBoss.getLocation()).forEach(player -> {
			mUnpushableTeam.removeEntity(player);
			player.showEntity(mPlugin, mBoss);
		});
		mBoss.teleport(mCenter);
	}

	@Override
	public void cancel() {
		if (isRunning()) {
			finishSpell();
		}
		super.cancel();
	}

	public void replaceBlocks(Location loc) {
		for (int i = 0; i < 360; i++) {
			final Vector mVec = VectorUtils.rotateYAxis(new Vector(1, 0, 0), i);
			new BukkitRunnable() {
				private final Location mLoc = loc.clone();

				@Override
				public void run() {
					mLoc.add(mVec);
					if (mLoc.getBlock().getType().isSolid()) {
						TemporaryBlockChangeManager.INSTANCE.changeBlock(mLoc.getBlock(), Material.CRIMSON_NYLIUM, CHARGE_UP_TIME + DURATION);
					} else {
						this.cancel();
						return;
					}

					if (mLoc.distance(loc) > RANGE) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public int cooldownTicks() {
		return CHARGE_UP_TIME + DURATION + 20;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
