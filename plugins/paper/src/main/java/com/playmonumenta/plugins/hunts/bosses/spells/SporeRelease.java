package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SporousAmalgam;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.HashSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SporeRelease extends Spell {
	public static double SHOCKWAVE_DAMAGE = 80;
	public static float SHOCKWAVE_SPORES = 2.5f;
	public static int TELEGRAPH_DURATION = (int) (20 * 1.75);
	public static int TRAVEL_DURATION = (int) (20 * 2.5);
	public static int REBOUND_DELAY = (int) (20 * 1.5);
	public static int SHOCKWAVE_POINTS = 54;
	public static final Particle.DustTransition PARTICLE_DATA = new Particle.DustTransition(Color.WHITE, Color.GREEN, 1.5f);
	public static final double SHOCKWAVE_START_DISTANCE = 4;
	public static final int EXTRA_DELAY_TICKS = 12;
	public static int COOLDOWN = TELEGRAPH_DURATION + REBOUND_DELAY + TRAVEL_DURATION * 2 + EXTRA_DELAY_TICKS + 20 * 5;

	private final Plugin mPlugin;
	private final SporousAmalgam mSporousAmalgam;
	private final LivingEntity mBoss;
	private final World mWorld;

	private final HashSet<Player> mSporedPlayers;
	private final HashSet<Player> mHitPlayers;

	public SporeRelease(Plugin plugin, SporousAmalgam sporousAmalgam) {
		mPlugin = plugin;
		mSporousAmalgam = sporousAmalgam;
		mBoss = sporousAmalgam.mBoss;
		mWorld = sporousAmalgam.mBoss.getWorld();

		mSporedPlayers = new HashSet<>();
		mHitPlayers = new HashSet<>();
	}


	@Override
	public void run() {
		//Telegraph sound
		mWorld.playSound(mBoss, Sound.ENTITY_WARDEN_ANGRY, 2.5f, 1.6f);
		mWorld.playSound(mBoss, Sound.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED, 2.5f, 1.1f);
		mWorld.playSound(mBoss, Sound.ENTITY_GHAST_WARN, 2.5f, 0.6f);
		mWorld.playSound(mBoss, Sound.ENTITY_GHAST_WARN, 2.5f, 0.8f);


		new BukkitRunnable() {
			int mTicks = 0;
			final PPCircle mTelegraphCircle = new PPCircle(Particle.DUST_COLOR_TRANSITION, mBoss.getLocation().add(0, 0.2, 0), SHOCKWAVE_START_DISTANCE).count(10).data(PARTICLE_DATA);

			@Override
			public void run() {
				mTelegraphCircle.spawnAsBoss();

				if (!mBoss.isValid()) {
					this.cancel();
				}

				if (mTicks >= TELEGRAPH_DURATION) {
					//Shockwave start
					for (Player p : new Hitbox.SphereHitbox(mBoss.getLocation(), SHOCKWAVE_START_DISTANCE).getHitPlayers(true)) {
						DamageUtils.damage(mBoss, p, DamageEvent.DamageType.BLAST, SHOCKWAVE_DAMAGE, null, false, false, "Spore Release");
						if (mSporedPlayers.add(p)) {
							mSporousAmalgam.addSpores(p, SHOCKWAVE_SPORES);
						}
					}
					mWorld.playSound(mBoss, Sound.ENTITY_PUFFER_FISH_BLOW_UP, 1.6f, 0.4f);
					mWorld.playSound(mBoss, Sound.ENTITY_BREEZE_INHALE, 1.0f, 0.8f);
					mWorld.playSound(mBoss, Sound.ENTITY_GLOW_SQUID_SQUIRT, 0.8f, 0.8f);
					mWorld.playSound(mBoss, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 0.7f, 0.8f);
					mWorld.playSound(mBoss, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, 1f, 0.7f);

					double speed = (SporousAmalgam.SPELL_INNER_RADIUS - SHOCKWAVE_START_DISTANCE) / TRAVEL_DURATION;
					for (int i = 0; i < SHOCKWAVE_POINTS; i++) {
						double angle = 360.0 / SHOCKWAVE_POINTS * i;
						Location pointLocation = mBoss.getLocation().add(FastUtils.sinDeg(angle) * SHOCKWAVE_START_DISTANCE, 0, FastUtils.cosDeg(angle) * SHOCKWAVE_START_DISTANCE);

						new BukkitRunnable() {
							private final PartialParticle mParticle = new PartialParticle(Particle.DUST_COLOR_TRANSITION, pointLocation, 1).data(PARTICLE_DATA);
							private final Location mLocation = pointLocation;
							private final Vector mPointSpeed = LocationUtils.getDirectionTo(pointLocation, mBoss.getLocation()).multiply(speed);
							private final BoundingBox mBoundingBox = BoundingBox.of(pointLocation, 0.65, 0.1, 0.65);
							private boolean mRebound = false;
							private int mTicks = 0;

							@Override
							public void run() {
								if (mTicks <= TRAVEL_DURATION || mTicks > TRAVEL_DURATION + REBOUND_DELAY) {
									mBoundingBox.shift(mPointSpeed);
									mLocation.add(mPointSpeed);
									for (Player p : mSporousAmalgam.getPlayersInOutRange()) {
										if (p.getBoundingBox().overlaps(mBoundingBox)) {
											double damage = mHitPlayers.add(p) ? SHOCKWAVE_DAMAGE : SHOCKWAVE_DAMAGE / 2;
											DamageUtils.damage(mBoss, p, DamageEvent.DamageType.BLAST, damage, null, false, false, "Spore Release");
											if (mSporedPlayers.add(p)) {
												mSporousAmalgam.addSpores(p, SHOCKWAVE_SPORES);
											}
											MovementUtils.knockAway(mBoss.getLocation(), p, 0, 0.75f);
										}
									}
								} else if (!mRebound) {
									mRebound = true;
									mPointSpeed.multiply(-1);
								}

								mParticle.location(mLocation).spawnAsBoss();

								if (mTicks >= TRAVEL_DURATION * 2 + REBOUND_DELAY + EXTRA_DELAY_TICKS || !mBoss.isValid()) {
									this.cancel();
								}
								mTicks++;
							}
						}.runTaskTimer(mPlugin, 0, 1);
					}
					new BukkitRunnable() {
						private int mTicks = 0;
						@Override
						public void run() {
							if (mTicks <= TRAVEL_DURATION || mTicks > TRAVEL_DURATION + REBOUND_DELAY) {
								if (mTicks % 3 == 0) {
									mWorld.playSound(mBoss, Sound.BLOCK_CHORUS_FLOWER_GROW, 3f, 1f);
								}
							} else if (mTicks == TRAVEL_DURATION + REBOUND_DELAY - 1) {
								//Rebound sound
								mSporedPlayers.clear();
								mWorld.playSound(mBoss, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, 1.6f, 0.4f);
								mWorld.playSound(mBoss, Sound.ENTITY_BREEZE_SLIDE, 2f, 1f);
								mWorld.playSound(mBoss, Sound.ENTITY_GLOW_SQUID_SQUIRT, 0.8f, 0.4f);
								mWorld.playSound(mBoss, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 0.7f, 0.4f);
								mWorld.playSound(mBoss, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, 1f, 0.4f);
							}

							if (mTicks >= TRAVEL_DURATION * 2 + REBOUND_DELAY + EXTRA_DELAY_TICKS || !mBoss.isValid()) {
								mHitPlayers.clear();
								mSporedPlayers.clear();
								//end sound
								mWorld.playSound(mBoss, Sound.ENTITY_WARDEN_AGITATED, 1f, 1.6f);
								mWorld.playSound(mBoss, Sound.ENTITY_RAVAGER_STUNNED, 1f, 0.8f);
								mWorld.playSound(mBoss, Sound.ENTITY_GHAST_DEATH, 1f, 0.8f);
								this.cancel();
							}
							mTicks++;
						}
					}.runTaskTimer(mPlugin, 0, 1);
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return COOLDOWN;
	}

	@Override
	public boolean canRun() {
		if (!mSporousAmalgam.canRunUproot()) {
			return mSporousAmalgam.canRunSpell(this);
		}
		return false;
	}
}
