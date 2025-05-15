package com.playmonumenta.plugins.bosses.spells.intruder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.intruder.IntruderBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellParasomnicMist extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private List<Player> mPlayers;
	private final IntruderBoss.Dialogue mDialogue;
	@Nullable
	private SafeZone mSafeZone;

	private static final int CHANGE_DIRECTION_INTERVAL = 5 * 20;
	private static final double SPEED = 3;

	private static final int RANGE = 50;
	private static final int CHARGE_UP_TIME = 4 * 20;
	private static final int DURATION = 15 * 20;
	private static final int KILL_DURATION = 2 * 20;

	private double mSafeZoneRadius = 3;
	private final ChargeUpManager mChargeUpManager;
	private static final String SPELL_NAME = "Parasomnic Mist (☠)";

	public SpellParasomnicMist(Plugin plugin, LivingEntity boss, Location center, List<Player> players, IntruderBoss.Dialogue dialogue) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mPlayers = List.copyOf(players);
		mDialogue = dialogue;
		mChargeUpManager = new ChargeUpManager(mBoss, CHARGE_UP_TIME, Component.text("Charging ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)), BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS, RANGE);

	}

	@Override
	public void run() {
		mSafeZoneRadius = mBoss.getScoreboardTags().contains(IntruderBoss.STALKER_ACTIVE_TAG) ? 4 : 3;
		mDialogue.dialogue(2 * 20, List.of("THERE IS TOO MUCH. NOISE. YOU NEED. TO SINK.", "DEEPER. INTO. THE MIST."));
		mPlayers = IntruderBoss.playersInRange(mBoss.getLocation());
		mChargeUpManager.setTitle(Component.text("Charging ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
		mChargeUpManager.setColor(BossBar.Color.YELLOW);
		mChargeUpManager.setChargeTime(CHARGE_UP_TIME);

		mActiveTasks.add(new BukkitRunnable() {
			int mState = 0;

			@Override
			public void run() {
				if (mState == 0) {
					if (mChargeUpManager.nextTick(1)) {
						mChargeUpManager.setTitle(Component.text("Channeling  ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
						mChargeUpManager.setColor(BossBar.Color.RED);
						mChargeUpManager.setTime(DURATION);
						mChargeUpManager.setChargeTime(DURATION);
						mState = 1;
					}
				} else if (mState == 1) {
					if (mChargeUpManager.previousTick(1)) {
						mChargeUpManager.setTime(KILL_DURATION);
						mChargeUpManager.setChargeTime(KILL_DURATION);
						mChargeUpManager.setTitle(Component.text("Casting  ", NamedTextColor.DARK_RED).append(Component.text(SPELL_NAME, NamedTextColor.RED)));
						mPlayers.forEach(player -> {
							player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.4f, 0.1f);
							player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, KILL_DURATION, 1, true, false));
							player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 5, 1, true, false));
						});

						mState = 2;
					}
				} else if (mState == 2) {
					if (mChargeUpManager.previousTick(1)) {
						mChargeUpManager.remove();
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

				mPlayers.forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.3f, 0.75f + (float) mTicks / CHARGE_UP_TIME));

				mTicks += 5;
				if (mTicks >= CHARGE_UP_TIME) {
					this.cancel();
					masterTask(mCenter);
					new PartialParticle(Particle.SQUID_INK, mBoss.getLocation())
						.count(30)
						.extra(0.7)
						.spawnAsBoss();

					mBoss.teleport(mCenter);
					mPlayers.forEach(player -> player.hideEntity(mPlugin, mBoss));
					mBoss.setInvisible(false);
				}
			}
		}.runTaskTimer(mPlugin, 0, 5));

		mSafeZone = new SafeZone(LocationUtils.randomLocationInCircle(mCenter, 15));
	}

	private class SafeZone {
		private final Location mLocation;
		private State mState = State.NONE;


		private enum State {
			NONE(new Particle.DustTransition(Color.fromRGB(0x6b0000), Color.BLACK, 1.4f)),
			ONE(new Particle.DustTransition(Color.fromRGB(0x006b00), Color.GREEN, 1.4f)),
			TOO_MANY(new Particle.DustTransition(Color.fromRGB(0x6b6b00), Color.YELLOW, 1.4f));

			private final Particle.DustTransition mDustTransition;

			State(Particle.DustTransition dustTransition) {
				mDustTransition = dustTransition;
			}
		}
		public SafeZone(Location location) {
			mLocation = location;
			safeZoneTask();
			movementRunnable();
			new PPPillar(Particle.END_ROD, location, 20)
				.count(40)
				.spawnAsBoss();
		}

		public void safeZoneTask() {
			mActiveTasks.add(new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks % 10 == 0) {
						if (mTicks <= CHARGE_UP_TIME) {
							new PPCircle(Particle.DUST_COLOR_TRANSITION, mLocation, mSafeZoneRadius)
								.countPerMeter(4)
								.data(mState.mDustTransition)
								.spawnAsBoss();
						} else {
							new PPCircle(Particle.SOUL_FIRE_FLAME, mLocation, mSafeZoneRadius)
								.countPerMeter(3)
								.rotateDelta(true).directionalMode(true)
								.delta(-0.06, 0, 0)
								.extra(1)
								.spawnAsBoss();
						}

						new PartialParticle(Particle.END_ROD, mLocation)
							.count(4)
							.delta(mSafeZoneRadius / 2.0)
							.spawnAsBoss();
					}

					List<Player> players = PlayerUtils.playersInRange(mLocation, mSafeZoneRadius, true, false);
					mState = switch (players.size()) {
						case 0 -> State.NONE;
						case 1 -> State.ONE;
						default -> State.TOO_MANY;
					};

					mTicks += 1;
					if (mTicks >= CHARGE_UP_TIME + DURATION + KILL_DURATION) {
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
					mTicks += 1;
					if (mTicks >= DURATION) {
						new PPCircle(Particle.SQUID_INK, mLocation.clone().add(new Vector(0, 0.15, 0)), 0.5)
							.count(30)
							.rotateDelta(true).directionalMode(true)
							.delta(1, 0, 0)
							.extra(0.5)
							.spawnAsBoss();
						this.cancel();
						return;
					}
					// If it should change directions
					if (mTicks % CHANGE_DIRECTION_INTERVAL == 0 || willRunIntoWall(mDirection.clone().multiply(2))) {
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
					double velocity = Math.abs(FastUtils.sin(mTicks * CHANGE_DIRECTION_INTERVAL / Math.PI)) * SPEED / 20;
					mLocation.add(mDirection.clone().multiply(velocity));
				}

				private boolean willRunIntoWall(Vector direction) {
					Location nextLoc = mLocation.clone().add(direction);
					return nextLoc.distance(mCenter) >= 25 - mSafeZoneRadius || nextLoc.getBlock().isSolid();
				}
			}.runTaskTimer(mPlugin, CHARGE_UP_TIME, 1));
		}
	}

	public void masterTask(Location mCenter) {
		mPlayers.forEach(player -> {
			player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, DURATION + KILL_DURATION, 1, true, false, false));
			EffectManager.getInstance().addEffect(player, "ParasomnicAnemia", new PercentHeal(DURATION + KILL_DURATION, -1.0));
		});


		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.DUST_COLOR_TRANSITION, mCenter)
					.count(300)
					.delta(RANGE / 2.0)
					.data(new Particle.DustTransition(Color.RED, Color.fromRGB(0x6b0000), 5.0f))
					.spawnAsBoss();

				List<Player> players = new ArrayList<>(mPlayers);
				mPlayers.forEach(player -> {
					player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, 0.5f, 0.1f + (float) mTicks / DURATION);
				});
				players.removeIf(player -> mSafeZone != null && mSafeZone.mLocation.distance(player.getLocation()) <= mSafeZoneRadius);

				// Damage
				if (mTicks % 4 == 0) {
					players.forEach(player -> {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, 2, null, true);
					});
				}
				if (mTicks >= DURATION) {
					players.forEach(player -> {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, 10, null, true);
						player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 1.9f, 0.5f);
					});
				}
				if (mTicks > DURATION + KILL_DURATION) {
					players.forEach(player -> {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE, 1000000, null, false);
					});
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
		mPlayers.forEach(player -> player.showEntity(mPlugin, mBoss));
		mBoss.teleport(mCenter);
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
						TemporaryBlockChangeManager.INSTANCE.changeBlock(mLoc.getBlock(), Material.CRIMSON_NYLIUM, CHARGE_UP_TIME + DURATION + KILL_DURATION);
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
		return CHARGE_UP_TIME + DURATION + KILL_DURATION + 20;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
