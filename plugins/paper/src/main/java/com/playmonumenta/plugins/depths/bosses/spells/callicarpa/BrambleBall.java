package com.playmonumenta.plugins.depths.bosses.spells.callicarpa;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BrambleBall extends Spell {
	private static final String SPELL_NAME = "Bramble Ball";
	private static final int COOLDOWN = 200;
	private static final int CAST_DELAY = 30;
	private static final int AIR_TIME = 200;
	private static final double DAMAGE = 50;
	private static final double BRAMBLE_SPEED = 0.175;
	private static final double BRAMBLE_SPEED_A8_INCREASE = 0.025;
	private static final double BRAMBLE_FALL_SPEED = 0.05;
	private static final String SLOWNESS_SRC = "BrambleBallSlowness";
	private static final int PLAYER_HIT_DEBUFFS_DURATION = 100;
	private static final int BRAMBLE_FIELD_DURATION = 400;
	private static final int BRAMBLE_FIELD_ACTIVATE_INTERVAL = 5;
	private static final double BRAMBLE_FIELD_RADIUS = 6;
	private static final Color[] BRAMBLE_COLORS = {Color.fromRGB(79, 59, 15), Color.fromRGB(47, 102, 17)};

	private final Particle.DustOptions[] mBrambleOptions = {
		new Particle.DustOptions(BRAMBLE_COLORS[0], 1.5f),
		new Particle.DustOptions(BRAMBLE_COLORS[0], 1.5f),
		new Particle.DustOptions(BRAMBLE_COLORS[0], 1.5f),
		new Particle.DustOptions(BRAMBLE_COLORS[1], 1.5f),
	};
	private final LivingEntity mBoss;
	private final int mFloorY;
	private final int mFinalCooldown;
	private final @Nullable DepthsParty mParty;

	public BrambleBall(LivingEntity boss, int floorY, @Nullable DepthsParty party) {
		mBoss = boss;
		mFloorY = floorY;
		mFinalCooldown = DepthsParty.getAscensionEightCooldown(COOLDOWN, party);
		mParty = party;
	}

	@Override
	public void run() {
		// Play sounds to indicate the attack is going to happen (don't want to use a boss bar for this one).
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITCH_DEATH, SoundCategory.HOSTILE, 10f, 0.7f);

		// Make the boss glow a different color for an extra indication.
		GlowingManager.startGlowing(mBoss, NamedTextColor.RED, CAST_DELAY, GlowingManager.BOSS_SPELL_PRIORITY);

		BukkitRunnable brambleRunnable = new BukkitRunnable() {
			final ChargeUpManager mChargeUp = new ChargeUpManager(mBoss, CAST_DELAY,
				Component.text("Charging ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME, NamedTextColor.DARK_GREEN, TextDecoration.BOLD)),
				BossBar.Color.GREEN, BossBar.Overlay.PROGRESS, 200);

			@Override
			public void run() {
				if (mBoss.isDead()) {
					this.cancel();
					return;
				}

				if (mChargeUp.nextTick()) {
					// Launch the projectile.
					launchBramble();
					this.cancel();
				}
			}
		};

		brambleRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return mFinalCooldown;
	}

	private void launchBramble() {
		List<Player> players = mBoss.getLocation().getNearbyPlayers(100).stream()
			.filter(p -> !p.getGameMode().equals(GameMode.SPECTATOR)).toList();

		if (players.isEmpty()) {
			return;
		}

		if (mParty != null && mParty.getAscension() >= 15) {
			players.forEach(this::launchBrambleInternal);
		} else {
			launchBrambleInternal(FastUtils.getRandomElement(players));
		}
	}

	private void launchBrambleInternal(Player target) {
		// Cast out the vine bramble.
		BukkitRunnable vineRunnable = new BukkitRunnable() {
			final Location mCurrLoc = mBoss.getLocation().clone().add(0, 1, 0);
			final Player mTarget = target;
			final double mBrambleSpeed = getBrambleSpeed(mParty);

			int mTicks = 0;

			@Override
			public void run() {
				if (mBoss.isDead()) {
					this.cancel();
					return;
				}

				// Update the location to move towards a player.
				Vector dir = LocationUtils.getDirectionTo(mTarget.getLocation().clone().add(0, 1, 0), mCurrLoc).multiply(mBrambleSpeed);
				mCurrLoc.add(dir);

				mBoss.getWorld().playSound(mCurrLoc, Sound.BLOCK_AZALEA_BREAK, SoundCategory.HOSTILE, 1f, 0.7f);
				new PartialParticle(Particle.ENCHANTMENT_TABLE, mCurrLoc, 1).extra(0).spawnAsBoss();
				new PartialParticle(Particle.REDSTONE, mCurrLoc, 5).delta(0.1).extra(0.05)
					.data(getRandomBrambleOptions()).spawnAsBoss();


				// If the bramble has collided with a Player or a Block, it stops there.
				if (doBrambleCollision()) {
					mBoss.getWorld().playSound(mCurrLoc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 1f, 2f);
					this.cancel();
					return;
				}

				// If the bramble has not collided with anything,
				if (mTicks >= AIR_TIME) {
					// Start the animation to drop the bramble to the ground.
					doBrambleFallAnimation(mCurrLoc, mTarget);
					this.cancel();
				}
				mTicks++;
			}

			private boolean doBrambleCollision() {
				// If the bramble hits a player, apply major slowness and jump negation.
				Hitbox hitbox = new Hitbox.SphereHitbox(mCurrLoc, 0.5);
				List<Player> hitPlayers = hitbox.getHitPlayers(false);
				if (hitPlayers.size() != 0) {
					Player hitPlayer = hitPlayers.get(0);
					// Apply major slowness and jump negation
					hitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, PLAYER_HIT_DEBUFFS_DURATION, -10, true, false, false));
					Plugin.getInstance().mEffectManager.addEffect(hitPlayer, SLOWNESS_SRC,
						new PercentSpeed(PLAYER_HIT_DEBUFFS_DURATION, -1.0, SLOWNESS_SRC));
					DamageUtils.damage(mBoss, hitPlayer, DamageEvent.DamageType.MAGIC, DAMAGE, null, true, true, SPELL_NAME);
					return true;
				}

				// If the bramble hits a block, it is stopped.
				Block currentBlock = mCurrLoc.getBlock();
				return currentBlock.isCollidable();
			}
		};

		vineRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void doBrambleFallAnimation(Location currLoc, Player target) {
		BukkitRunnable dropBrambleRunnable = new BukkitRunnable() {
			final Location mCurrLoc = currLoc;
			final Player mTarget = target;
			final double mBrambleSpeed = getBrambleSpeed(mParty);

			double mTicks = BRAMBLE_FALL_SPEED;

			@Override
			public void run() {
				if (mBoss.isDead()) {
					this.cancel();
					return;
				}

				// The bramble should both keep moving towards the player, and slowly fall to the ground.
				Vector dir = LocationUtils.getDirectionTo(mTarget.getLocation().clone().add(0, 1, 0), mCurrLoc).multiply(mBrambleSpeed);
				mCurrLoc.add(dir);
				// It has a bit of an upwards curve initially.
				mCurrLoc.subtract(0, mTicks - 0.5, 0);

				new PartialParticle(Particle.ENCHANTMENT_TABLE, mCurrLoc, 1).extra(0).spawnAsBoss();
				new PartialParticle(Particle.REDSTONE, mCurrLoc, 5).delta(0.1).extra(0.05)
					.data(getRandomBrambleOptions()).spawnAsBoss();

				if (mCurrLoc.getY() <= mFloorY + 1) {
					// The bramble has fallen to the ground.
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_TURTLE_EGG_HATCH, SoundCategory.HOSTILE, 1f, 1f);
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_TURTLE_EGG_HATCH, SoundCategory.HOSTILE, 1f, 1f);

					Location pulseLoc = mCurrLoc.clone();
					pulseLoc.setY(mFloorY + 1);

					doBrambleField(pulseLoc);

					this.cancel();
				}

				mTicks += BRAMBLE_FALL_SPEED;
			}
		};

		dropBrambleRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void doBrambleField(Location pulseLoc) {
		BukkitRunnable brambleFieldRunnable = new BukkitRunnable() {
			final Location mPulseLoc = pulseLoc;

			int mTicks = 0;

			@Override
			public void run() {
				if (mBoss.isDead()) {
					this.cancel();
					return;
				}

				mBoss.getWorld().playSound(mPulseLoc, Sound.BLOCK_GRASS_BREAK, SoundCategory.HOSTILE, 1f, 0.7f);
				// Have a specifically defined ring
				new PPCircle(Particle.REDSTONE, mPulseLoc, BRAMBLE_FIELD_RADIUS).countPerMeter(3).ringMode(true)
					.data(ParticleUtils.getTransition(mBrambleOptions[0], mBrambleOptions[3], Math.random())).spawnAsBoss();
				// and some particles in the middle, too.
				new PPCircle(Particle.REDSTONE, mPulseLoc, BRAMBLE_FIELD_RADIUS).countPerMeter(3).ringMode(false)
					.data(ParticleUtils.getTransition(mBrambleOptions[0], mBrambleOptions[3], Math.random())).spawnAsBoss();

				// Apply debuffs to players in radius
				mPulseLoc.getNearbyPlayers(BRAMBLE_FIELD_RADIUS).forEach(player -> {
					EntityUtils.applySlow(Plugin.getInstance(), BRAMBLE_FIELD_ACTIVATE_INTERVAL, 0.35, player);
					EntityUtils.applyVulnerability(Plugin.getInstance(), BRAMBLE_FIELD_ACTIVATE_INTERVAL, 0.5, player);
				});

				// Cancel when over the duration of the spell.
				if (mTicks >= BRAMBLE_FIELD_DURATION) {
					this.cancel();
				}
				mTicks += BRAMBLE_FIELD_ACTIVATE_INTERVAL;
			}
		};

		brambleFieldRunnable.runTaskTimer(Plugin.getInstance(), 0, BRAMBLE_FIELD_ACTIVATE_INTERVAL);
	}

	private Particle.DustOptions getRandomBrambleOptions() {
		return mBrambleOptions[FastUtils.randomIntInRange(0, mBrambleOptions.length - 1)];
	}

	private double getBrambleSpeed(@Nullable DepthsParty party) {
		double speed = BRAMBLE_SPEED;
		if (party != null && party.getAscension() >= 8) {
			speed += BRAMBLE_SPEED_A8_INCREASE;
		}
		return speed;
	}
}
