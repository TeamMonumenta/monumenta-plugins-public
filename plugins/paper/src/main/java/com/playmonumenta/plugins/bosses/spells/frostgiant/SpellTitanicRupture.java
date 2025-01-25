package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.listeners.StasisListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import com.playmonumenta.scriptedquests.growables.GrowableProgress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class SpellTitanicRupture extends Spell {
	private static final String SPELL_NAME = "Titanic Rupture";
	private static final int HEIGHT_ABOVE_FLOOR = 20;
	private static final int CHARGE_DURATION = (int) (Constants.TICKS_PER_SECOND * 2.5);
	private static final int CAST_DURATION = Constants.TICKS_PER_SECOND * 2;
	private static final int GRAZE_RADIUS = 4;
	private static final int INSTANT_KILL_RADIUS = 2;
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);

	private final Plugin mPlugin;
	private final FrostGiant mFrostGiant;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final List<Location> mTargetLocations = new ArrayList<>();
	private final ChargeUpManager mChargeManager;
	/** Stores references to growables that are in progress */
	private final List<GrowableProgress> mGrowables = new ArrayList<>();

	private boolean mCooldown = false;

	public SpellTitanicRupture(final Plugin plugin, final FrostGiant frostGiant) {
		mPlugin = plugin;
		mFrostGiant = frostGiant;
		mBoss = mFrostGiant.mBoss;
		mWorld = mBoss.getWorld();
		mChargeManager = new ChargeUpManager(mBoss, CHARGE_DURATION, Component.text("Charging ", NamedTextColor.DARK_AQUA)
			.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED)), BossBar.Color.RED, BossBar.Overlay.PROGRESS, FrostGiant.detectionRange);
	}

	@Override
	public void run() {
		mCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mCooldown = false, Constants.TICKS_PER_SECOND * 25);

		if (mFrostGiant.getArenaParticipants().isEmpty()) {
			return;
		}
		mFrostGiant.getArenaParticipants().forEach(player -> mFrostGiant.sendDialogue("CRUMBLE UNDER THE WEIGHT OF THE MOUNTAIN.", NamedTextColor.DARK_RED, true));
		mFrostGiant.freezeGolems();
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 5, 0.5f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 5, 1);

		final BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (!mChargeManager.nextTick()) {
					return;
				}

				/* Finished charging - set to casting, get current player locations, and spawn icicles */
				mChargeManager.reset();
				mChargeManager.setTitle(Component.text("Casting ", NamedTextColor.DARK_AQUA)
					.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED)));
				mChargeManager.setChargeTime(CAST_DURATION);
				mChargeManager.setTime(0);

				List<Player> players = (List<Player>) mFrostGiant.getArenaParticipants();
				if (players.isEmpty()) {
					this.cancel();
					return;
				}

				Collections.shuffle(players);
				if (players.size() >= 3) {
					players = players.subList(0, 2);
				}

				players.forEach(target -> mTargetLocations.add(target.getLocation()));
				createTitanicIcicles();
				this.cancel();
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void createTitanicIcicles() {
		mTargetLocations.forEach(location -> {
			location.setY(FrostGiant.ARENA_FLOOR_Y + 1);
			mWorld.playSound(location, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 3.0f, 0.5f);

			/* Call growable to create the Titanic Rupture Icicle 20 blocks above the location */
			try {
				final GrowableProgress newGrowable = GrowableAPI.grow("titanicruptureicicle",
					location.clone().add(0, HEIGHT_ABOVE_FLOOR, 0), 1, 10, true);
				// Add it to the list for now, and when it's done, have it remove itself from the list
				mGrowables.add(newGrowable);
				newGrowable.whenComplete(mGrowables::remove);
			} catch (final Exception e) {
				MMLog.warning("[FrostGiant] Failed to grow scripted quests structure 'titanicruptureicicle': " + e.getMessage());
			}
		});

		final int runnablePeriod = 1;
		final BukkitRunnable runnable = new BukkitRunnable() {
			float mPitch = 1;

			@Override
			public void run() {
				final Location particleLoc = new Location(mWorld, 0, 0, 0);
				final PPCircle redInstantKillCircle = new PPCircle(Particle.REDSTONE, particleLoc, INSTANT_KILL_RADIUS).countPerMeter(3).delta(0.03).data(RED_COLOR);
				final PPCircle redGrazeCircle = new PPCircle(Particle.REDSTONE, particleLoc, GRAZE_RADIUS).countPerMeter(3).delta(0.03).data(RED_COLOR);
				final PPCircle dragonBreathCircle = new PPCircle(Particle.DRAGON_BREATH, particleLoc, INSTANT_KILL_RADIUS).countPerMeter(3).delta(0.05).extra(0.05);

				if (mChargeManager.getTime() % Constants.HALF_TICKS_PER_SECOND == 0) {
					mTargetLocations.forEach(location -> {
						redInstantKillCircle.location(location).spawnAsEntityActive(mBoss);
						redGrazeCircle.location(location).spawnAsEntityActive(mBoss);

						dragonBreathCircle.radius(INSTANT_KILL_RADIUS).location(location.clone().add(0, 1, 0)).spawnAsEntityActive(mBoss);
						dragonBreathCircle.radius(GRAZE_RADIUS).spawnAsEntityActive(mBoss);

						redInstantKillCircle.location(location.clone().add(0, 2, 0)).spawnAsEntityActive(mBoss);
						redGrazeCircle.location(location.clone().add(0, 2, 0)).spawnAsEntityActive(mBoss);

						mWorld.playSound(location, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2.0f, mPitch);
					});
				}

				if (mChargeManager.getTime() % Constants.QUARTER_TICKS_PER_SECOND == 0) {
					mPitch += 0.05f;
				}

				if (!mChargeManager.nextTick(runnablePeriod)) {
					return;
				}

				mTargetLocations.forEach(location -> {
					mWorld.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 3, 0.5f);
					mWorld.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3, 0.5f);

					/* Turn all the icicles in the growable into falling blocks */
					final Location blockLoc = location.clone();
					for (int y = -15; y <= 0; y++) {
						for (int x = -6; x < 6; x++) {
							for (int z = -6; z < 6; z++) {
								blockLoc.set(location.getX() + x, location.getY() + y + HEIGHT_ABOVE_FLOOR, location.getZ() + z);
								final Block b = blockLoc.getBlock();
								if (b.getType() == Material.BLUE_ICE || b.getType() == Material.SNOW_BLOCK) {
									mWorld.spawn(blockLoc, FallingBlock.class, CreatureSpawnEvent.SpawnReason.CUSTOM,
										(final FallingBlock block) -> {
											block.setBlockData(Bukkit.createBlockData(b.getType()));
											block.setVelocity(new Vector(0, -1, 0));
											block.setDropItem(false);
										});
									b.setType(Material.AIR);
								}
							}
						}
					}
				});

				mTargetLocations.forEach(location -> {
					List<Player> hitPlayers = new Hitbox.UprightCylinderHitbox(location.clone().add(0, -5, 0), HEIGHT_ABOVE_FLOOR + 5, INSTANT_KILL_RADIUS).getHitPlayers(true);
					hitPlayers.forEach(player -> {
						/* I want that player DECEASED. OBLITERATED. IRREVOCABLY DESTROYED */
						DamageUtils.damage(mBoss, player, DamageType.TRUE, EntityUtils.getMaxHealth(player) * 10,
							null, true, false, SPELL_NAME);
						player.damage(1);
						/* Except if they're in stasis */
						if (StasisListener.isInStasis(player)) {
							AdvancementUtils.grantAdvancement(player, "monumenta:challenges/r2/fg/lonely_soloist");
						}
					});

					hitPlayers = new Hitbox.UprightCylinderHitbox(location.clone().add(0, -5, 0), HEIGHT_ABOVE_FLOOR + 5, GRAZE_RADIUS).getHitPlayers(true);
					hitPlayers.forEach(player -> DamageUtils.damage(mBoss, player, DamageType.BLAST, 60, null, false, true, SPELL_NAME));

					for (double y = FrostGiant.ARENA_FLOOR_Y; y < FrostGiant.ARENA_FLOOR_Y + 10; y++) {
						final Location tempLoc = new Location(mWorld, location.getX(), y, location.getZ());
						redGrazeCircle.location(tempLoc).spawnAsEntityActive(mBoss);
						dragonBreathCircle.location(tempLoc).radius(GRAZE_RADIUS + 1).spawnAsEntityActive(mBoss);
					}
				});

				mFrostGiant.unfreezeGolems();
				mTargetLocations.clear();
				mChargeManager.reset();
				mChargeManager.setChargeTime(CHARGE_DURATION);
				mChargeManager.setTitle(Component.text("Charging ", NamedTextColor.DARK_AQUA)
					.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED)));
				this.cancel();
			}
		};

		runnable.runTaskTimer(mPlugin, 0, runnablePeriod);
		mActiveRunnables.add(runnable);
	}

	@Override
	public void cancel() {
		super.cancel();
		mFrostGiant.unfreezeGolems();
		mChargeManager.reset();
		mChargeManager.setChargeTime(CHARGE_DURATION);
		mChargeManager.setTitle(Component.text("Charging ", NamedTextColor.DARK_AQUA)
			.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED)));

		/* Need to make a copy of the growables list because it also removes entries from the list when they are cancelled */
		new ArrayList<>(mGrowables).forEach(GrowableProgress::cancel);
		mGrowables.clear();

		for (final Location loc : mTargetLocations) {
			final Location l = loc.clone();
			//Deletes icicles in air once cancelled
			for (int y = -15; y <= 0; y++) {
				for (int x = -6; x < 6; x++) {
					for (int z = -6; z < 6; z++) {
						l.set(loc.getX() + x, loc.getY() + y + HEIGHT_ABOVE_FLOOR, loc.getZ() + z);
						final Block b = l.getBlock();
						if (b.getType() == Material.BLUE_ICE || b.getType() == Material.SNOW_BLOCK) {
							b.setType(Material.AIR);
						}
					}
				}
			}
		}
		mTargetLocations.clear();
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	@Override
	public int cooldownTicks() {
		return Constants.TICKS_PER_SECOND * 7;
	}
}
