package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class SpellGlacialPrison extends Spell {
	private static final String SPELL_NAME = "Glacial Prison";
	private static final int CHARGE_DURATION = Constants.TICKS_PER_SECOND * 2;
	private static final int PRISON_DURATION = Constants.TICKS_PER_SECOND * 8;

	private final Plugin mPlugin;
	private final FrostGiant mFrostGiant;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final List<Player> mTargets = new ArrayList<>();
	private final ChargeUpManager mChargeManager;
	private final ChargeUpManager mEscapeManager;

	private boolean mCanRun = true;

	public SpellGlacialPrison(final Plugin plugin, final FrostGiant frostGiant) {
		mPlugin = plugin;
		mFrostGiant = frostGiant;
		mBoss = mFrostGiant.mBoss;
		mWorld = mBoss.getWorld();
		mChargeManager = FrostGiant.defaultChargeUp(mBoss, CHARGE_DURATION, "Charging " + SPELL_NAME + "...");
		mEscapeManager = new ChargeUpManager(mBoss, PRISON_DURATION,
			Component.text("Escape " + SPELL_NAME, NamedTextColor.DARK_AQUA), BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10, FrostGiant.detectionRange);
	}

	@Override
	public void run() {
		mCanRun = false;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCanRun = true, Constants.TICKS_PER_SECOND * 60);

		List<Player> players = (List<Player>) mFrostGiant.getArenaParticipants();
		if (players.isEmpty()) {
			return;
		}

		Collections.shuffle(players);
		if (players.size() > 1) {
			players = players.subList(0, players.size() / 2);
		}

		mTargets.addAll(players);
		mFrostGiant.freezeGolems();
		mFrostGiant.delayHailstormDamage(CHARGE_DURATION + PRISON_DURATION);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3, 0.5f);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					mTargets.clear();
					this.cancel();
					return;
				}

				mTargets.forEach(player -> new PartialParticle(Particle.FIREWORKS_SPARK,
					player.getLocation().add(0, 1, 0), 2, 0.4, 0.4, 0.4, 0.05).spawnAsEntityActive(mBoss));

				if (mChargeManager.nextTick()) {
					mChargeManager.reset();
					mEscapeManager.setTime(PRISON_DURATION);
					createPrison();
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 10;
	}

	@Override
	public boolean canRun() {
		return mCanRun;
	}

	private void createPrison() {
		final List<Location> prisonerLocs = new ArrayList<>();
		final List<Block> changedBlocks = new ArrayList<>();

		mTargets.forEach(target -> {
			target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, PRISON_DURATION - Constants.TICKS_PER_SECOND * 2, 1));
			final Location playerLoc = target.getLocation();
			new PartialParticle(Particle.FIREWORKS_SPARK, playerLoc.clone().add(0, 1, 0), 30, 0.4, 0.4, 0.4, 0.2).spawnAsEntityActive(mBoss);
			new PartialParticle(Particle.EXPLOSION_NORMAL, playerLoc.clone().add(0, 1, 0), 35, 0.4, 0.4, 0.4, 0.25).spawnAsEntityActive(mBoss);
			mWorld.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 0.5f);
			mWorld.playSound(playerLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 0.75f);

			//Center the player first
			final Vector dir = playerLoc.getDirection();
			final Location l = playerLoc.getBlock().getLocation().add(0.5, 0.1, 0.5).setDirection(dir);
			while (l.getY() - FrostGiant.ARENA_FLOOR_Y >= 3) {
				l.add(0, -1, 0);
			}
			target.teleport(l);
			prisonerLocs.add(target.getLocation());

			final Location center = target.getLocation();
			final Location[] prisonBlockLocs = new Location[] {
				//First Layer
				center.clone().add(1, 0, 0),
				center.clone().add(-1, 0, 0),
				center.clone().add(0, 0, 1),
				center.clone().add(0, 0, -1),

				//Second Layer
				center.clone().add(1, 1, 0),
				center.clone().add(-1, 1, 0),
				center.clone().add(0, 1, 1),
				center.clone().add(0, 1, -1),

				//Top & Bottom
				center.clone().add(0, 2, 0),
				center.clone().add(0, -1, 0)
			};

			for (final Location loc : prisonBlockLocs) {
				Material mat = Material.BLUE_ICE;
				if (Math.abs(loc.getY() - (center.getY() + 1)) < 0.1) {
					// Ice at eye level to see through
					mat = Material.ICE;
				} else if (loc.getY() < center.getY()) {
					// A sea lantern below the player's feet
					mat = Material.SEA_LANTERN;
				}
				if (TemporaryBlockChangeManager.INSTANCE.changeBlock(loc.getBlock(), mat, PRISON_DURATION - 1)) {
					changedBlocks.add(loc.getBlock());
				}
			}
		});

		final BukkitRunnable prisonRunnable = new BukkitRunnable() {
			float mPitch = 0.5f;

			@Override
			public void run() {
				mPitch += 0.02f;

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				prisonerLocs.forEach(location -> {
					new PartialParticle(Particle.FIREWORKS_SPARK, location, 3, 1, 1, 1, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.CLOUD, location, 2, 1, 1, 1, 0).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.DAMAGE_INDICATOR, location, 1, 0.5, -0.25, 0.5, 0.005).spawnAsEntityActive(mBoss);

					if (mEscapeManager.getTime() % Constants.TICKS_PER_SECOND == 0) {
						mWorld.playSound(location, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.HOSTILE, 1, mPitch);
					}
				});

				if (mEscapeManager.previousTick(2)) {
					prisonerLocs.forEach(location -> {
						final List<Player> hitPlayers = new Hitbox.AABBHitbox(mWorld, BoundingBox.of(location, 0.5, 1, 0.5)).getHitPlayers(true);
						hitPlayers.forEach(player -> DamageUtils.damage(mBoss, player, DamageEvent.DamageType.TRUE,
							EntityUtils.getMaxHealth(player) * 0.8, null, true, false, SPELL_NAME));

						new PartialParticle(Particle.FIREWORKS_SPARK, location, 50, 1, 1, 1, 0.35).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.CLOUD, location, 75, 1, 1, 1, 0.25).spawnAsEntityActive(mBoss);
						mWorld.playSound(location, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 0.75f);
						mWorld.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0.75f);
					});

					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				mTargets.clear();
				mFrostGiant.unfreezeGolems();
				mEscapeManager.reset();
				TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(changedBlocks, Material.BLUE_ICE);
				TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(changedBlocks, Material.ICE);
				TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(changedBlocks, Material.SEA_LANTERN);
			}
		};

		prisonRunnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(prisonRunnable);
	}
}
