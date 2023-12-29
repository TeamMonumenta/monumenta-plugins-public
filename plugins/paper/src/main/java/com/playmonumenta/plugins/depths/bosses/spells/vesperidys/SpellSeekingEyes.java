package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class SpellSeekingEyes extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Vesperidys mVesperidys;

	private static final int INITIAL_DELAY = 1 * 20;
	private static final int EYE_DURATION_A0 = 15 * 20;
	private static final int EYE_DURATION_A8 = 10 * 20;
	private static final int PLAYERS_COUNT = 2;
	private static final int EYES_COUNT = 2;
	private static final int EYES_COUNT_ASCENSION_8 = 3;
	private static final int EYES_COUNT_ASCENSION_15 = 4;
	private static final double DAMAGE = 50;
	private static final String SPELL_NAME = "Seeking Eyes";

	private boolean mOnCooldown = false;
	private final int mEyeCount;
	private final int mEyeDuration;

	public SpellSeekingEyes(Plugin plugin, LivingEntity boss, Vesperidys vesperidys) {
		mPlugin = plugin;
		mBoss = boss;
		mVesperidys = vesperidys;

		if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 15) {
			mEyeCount = EYES_COUNT_ASCENSION_15;
			mEyeDuration = EYE_DURATION_A8;
		} else if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 8) {
			mEyeCount = EYES_COUNT_ASCENSION_8;
			mEyeDuration = EYE_DURATION_A8;
		} else {
			mEyeCount = EYES_COUNT;
			mEyeDuration = EYE_DURATION_A0;
		}
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mOnCooldown = false;
		}, cooldownTicks() + 30 * 20);

		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 3, 0.5f);

		int playerCount = 0;

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), Vesperidys.detectionRange, true);
		Collections.shuffle(players);

		for (Player player : players) {
			if (playerCount > PLAYERS_COUNT) {
				break;
			}
			playerCount++;

			for (int num = 0; num < mEyeCount; num++) {
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					if (player.isDead() || mVesperidys.mInvincible || player.getLocation().distance(mBoss.getLocation()) > 100) {
						this.cancel();
						return;
					}
					Location spawnLoc = player.getLocation().clone().add(FastUtils.randomDoubleInRange(-10, 10), 0, FastUtils.randomDoubleInRange(-10, 10));
					spawnLoc.setY(mVesperidys.mSpawnLoc.getY() + FastUtils.randomDoubleInRange(6, 10));
					summonEyes(spawnLoc, player);
				}, INITIAL_DELAY + num * 20);
			}
		}
	}

	private void summonEyes(Location spawnLocation, Player player) {
		mBoss.getWorld().playSound(spawnLocation, Sound.BLOCK_END_PORTAL_FRAME_FILL, 3, 1f);

		LivingEntity magmaCube = (LivingEntity) LibraryOfSoulsIntegration.summon(spawnLocation, "VesperidysSeekers");

		if (magmaCube == null) {
			return;
		}

		new PartialParticle(Particle.FLASH, spawnLocation)
			.spawnAsBoss();
		new PartialParticle(Particle.REDSTONE, spawnLocation, 20, 0.25, 0.25, 0.25)
			.data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1f))
			.spawnAsBoss();

		BukkitRunnable eyesRunnable = new BukkitRunnable() {
			int mEyesTicks = 0;

			@Override
			public synchronized void cancel() {
				super.cancel();
				magmaCube.remove();
			}

			@Override
			public void run() {
				if (magmaCube.isDead() || player.isDead() || player.getLocation().distance(magmaCube.getLocation()) > 100) {
					this.cancel();
					return;
				}

				// Laser Explode
				if (mEyesTicks > mEyeDuration) {
					mBoss.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 2);
					mBoss.getWorld().playSound(magmaCube.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 2, 1);
					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, null, true, true, SPELL_NAME);
					new PPExplosion(Particle.FLAME, player.getLocation())
						.count(20)
						.spawnAsBoss();

					// Draw tether line from Player to Dark Hole.
					Location playerLoc = LocationUtils.getEntityCenter(player);
					Vector dir = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(magmaCube), playerLoc).normalize().multiply(0.5);
					Location pLoc = playerLoc.clone();
					for (int i = 0; i < 40; i++) {
						pLoc.add(dir);

						new PartialParticle(Particle.SMOKE_LARGE, pLoc, 1, 0, 1, 0)
							.extra(0.5)
							.directionalMode(true)
							.spawnForPlayer(ParticleCategory.BOSS, player);


						if (pLoc.distance(LocationUtils.getEntityCenter(magmaCube)) < 0.5) {
							break;
						}
					}

					this.cancel();
					return;
				}

				// IF too far, teleport magmacube closer.
				double magmaX = magmaCube.getLocation().getX();
				double magmaZ = magmaCube.getLocation().getZ();
				double playerX = player.getLocation().getX();
				double playerZ = player.getLocation().getZ();
				if (Math.abs(magmaX - playerX) > 10 || Math.abs(magmaZ - playerZ) > 10) {
					Location playerLoc = player.getLocation();
					Vector dirXZ = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(magmaCube), playerLoc).setY(0).normalize().multiply(-0.2);
					magmaCube.teleport(magmaCube.getLocation().add(dirXZ));
				}

				// Draw tether line from Player to Dark Hole.
				Location playerLoc = LocationUtils.getEntityCenter(player);
				Vector dir = LocationUtils.getVectorTo(LocationUtils.getEntityCenter(magmaCube), playerLoc).normalize().multiply(0.5);
				Location pLoc = playerLoc.clone();
				for (int i = 0; i < 40; i++) {
					pLoc.add(dir);

					if (mEyesTicks < 2 * mEyeDuration / 3) {
						new PartialParticle(Particle.FLAME, pLoc, 1, 0, 0, 0)
							.extra(9999999)
							.spawnForPlayer(ParticleCategory.BOSS, player);
					} else {
						new PartialParticle(Particle.SOUL_FIRE_FLAME, pLoc, 1, 0, 0, 0)
							.extra(9999999)
							.spawnForPlayer(ParticleCategory.BOSS, player);
					}

					if (pLoc.distance(LocationUtils.getEntityCenter(magmaCube)) < 0.5) {
						break;
					}
				}

				if (mEyesTicks % 5 == 0) {
					mBoss.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_IN, 1, 2);
				}

				new PartialParticle(Particle.REDSTONE, magmaCube.getLocation(), 5, 0.25, 0.25, 0.25)
					.data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.5f))
					.spawnAsBoss();

				mEyesTicks++;
			}
		};

		eyesRunnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(eyesRunnable);
	}

	@Override public boolean canRun() {
		return !mOnCooldown && !mVesperidys.mTeleportSpell.mTeleporting;
	}

	@Override
	public int cooldownTicks() {
		return mVesperidys.mSpellCooldowns;
	}
}
