package com.playmonumenta.plugins.fishing;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.delves.mobabilities.StatMultiplierBoss;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ExperienceUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class FishingCombatManager implements Listener {
	private static final String COMBAT_LIMIT = "DailyLimitRingFishingCombat";
	private static final String COMBAT_TOTAL = "FishCombatsCompleted";
	private static final int PORTAL_DURATION = 200;
	private static final int WAVE_COUNT = 3;
	private static final int[][] WAVE_MOB_TYPES = new int[][] {
		// Difficulty 0
		{ 0, 0, 1, 1, 0, 0 },
		{ 0, 1, 1, 1, 0, 0, 1, 0, 0 },
		{ 0, 1, 1, 2, 1, 0, 1, 1, 0, 0 },
		// Difficulty 1
		{ 0, 1, 1, 1, 1, 0, 1, 0 },
		{ 0, 1, 1, 2, 0, 0, 1, 1, 0, 1, 0},
		{ 0, 1, 1, 1, 0, 0, 2, 1, 1, 0, 0, 1, 1, 0}
	};
	private static final int[] MOB_SPAWN_DELAYS = new int[] { 25, 18 };
	private final LoSPool POOL_COMMON = new LoSPool.LibraryPool("~FishingCommonMobs");
	private final LoSPool POOL_UNCOMMON = new LoSPool.LibraryPool("~FishingUncommonMobs");
	private final LoSPool POOL_ELITE = new LoSPool.LibraryPool("~FishingEliteMobs");
	private final HashMap<Player, FishingArena> mPlayerArenaMap = new HashMap<>();

	@EventHandler(ignoreCancelled = false)
	public void entityDeathEvent(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player player && mPlayerArenaMap.containsKey(player)) {
			FishingArena arena = mPlayerArenaMap.get(player);
			if (player.equals(arena.mOwner)) {
				ejectMobs(arena, player.getWorld());
				ejectArena(arena, player.getWorld(), false);
			}
			mPlayerArenaMap.remove(entity);
		}
	}

	@EventHandler(ignoreCancelled = false)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (!mPlayerArenaMap.containsKey(player)) {
			return;
		}

		FishingArena arena = mPlayerArenaMap.get(player);
		if (player.equals(arena.mOwner)) {
			ejectMobs(arena, player.getWorld());
			ejectArena(arena, player.getWorld(), false);
		} else {
			player.teleport(Objects.requireNonNullElseGet(arena.mOrigin, () -> player.getWorld().getSpawnLocation()));
		}
		mPlayerArenaMap.remove(player);
	}

	public boolean initiate(Player player, Location portalLocation, int difficulty) {
		FishingArena arena = getFreeArena();
		if (arena == null) {
			return false;
		}

		mPlayerArenaMap.put(player, arena);
		arena.mOrigin = player.getLocation();
		arena.mOwner = player;
		arena.mOccupied = true;
		arena.mDifficulty = difficulty;

		drawWhirlpool(portalLocation);
		setupPortal(portalLocation, arena);
		launchPlayerSequence(player, portalLocation, arena);
		return true;
	}

	private void combatSuccess(Player player) {
		FishingArena arena = mPlayerArenaMap.get(player);
		if (arena == null) {
			return;
		}
		final int difficulty = arena.mDifficulty;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayerArenaMap.containsKey(player)) {
					player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1.5f);

					player.teleport(Objects.requireNonNullElseGet(arena.mOrigin, () -> player.getWorld().getSpawnLocation()));
					ejectArena(arena, player.getWorld(), true);
					mPlayerArenaMap.remove(player);

					ItemStack reward = new ItemStack(Material.CHEST);

					int combatLimit = ScoreboardUtils.getScoreboardValue(player, COMBAT_LIMIT).orElse(0);
					if (combatLimit > 0) {
						if (difficulty > 0) {
							reward.setItemMeta(FishingManager.getAbyssalChest(difficulty).getItemMeta());
						} else {
							reward.setItemMeta(FishingManager.getGreaterChest().getItemMeta());
						}
						ScoreboardUtils.setScoreboardValue(player, COMBAT_LIMIT, combatLimit - 1);
					} else {
						reward.setItemMeta(FishingManager.getLesserChest().getItemMeta());
					}

					int playerTotal = ScoreboardUtils.getScoreboardValue(player, COMBAT_TOTAL).orElse(0);
					ScoreboardUtils.setScoreboardValue(player, COMBAT_TOTAL, playerTotal + 1);
					Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + player.getName() + " " + COMBAT_TOTAL);

					InventoryUtils.giveItem(player, reward);
				}
			}
		}.runTaskLater(Plugin.getInstance(), 30);
	}

	private void ejectArena(FishingArena arena, World world, boolean awardParticipants) {
		int radius = arena.mRadius;
		arena.mActive = false;
		arena.mOccupied = false;

		Hitbox ejectionSpace = new Hitbox.SphereHitbox(new Location(world, arena.mCoordinates.getX(), arena.mCoordinates.getY(), arena.mCoordinates.getZ()), radius);
		for (Player player : ejectionSpace.getHitPlayers(true)) {
			player.teleport(Objects.requireNonNullElseGet(arena.mOrigin, () -> player.getWorld().getSpawnLocation()));
			mPlayerArenaMap.remove(player);
			if (awardParticipants) {
				player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1.5f);
				player.sendMessage(Component.text("You have been awarded some EXP for your participation.", NamedTextColor.GRAY));
				player.giveExp(ExperienceUtils.getTotalExperience(10));
			}
		}
	}

	private void ejectMobs(FishingArena arena, World world) {
		int radius = arena.mRadius;

		Hitbox ejectionSpace = new Hitbox.SphereHitbox(new Location(world, arena.mCoordinates.getX(), arena.mCoordinates.getY(), arena.mCoordinates.getZ()), radius);
		for (LivingEntity entity : ejectionSpace.getHitMobs()) {
			entity.remove();
		}
	}

	private void progressWave(Player player, FishingArena arena) {
		waveClearAesthetics(arena, player);
		arena.mWave++;

		if (arena.mWave > WAVE_COUNT) {
			combatSuccess(player);
			return;
		}
		spawnWave(arena.mWave, player, arena, arena.mDifficulty);
	}

	private @Nullable FishingArena getFreeArena() {
		List<FishingArena> arenaList = Arrays.asList(FishingArena.values());
		Collections.shuffle(arenaList);
		for (FishingArena arena : arenaList) {
			if (!arena.mOccupied) {
				return arena;
			}
		}
		return null;
	}

	private void launchPlayerSequence(Player player, Location toLocation, FishingArena arena) {
		launchPlayer(player, toLocation.clone().add(0, 2, 0), true);
		new BukkitRunnable() {
			int mLaunches = 1;
			@Override
			public void run() {
				if (mLaunches >= 5 || arena.mActive) {
					this.cancel();
					return;
				}
				launchPlayer(player, toLocation, false);
				mLaunches++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 4);
	}

	private void launchPlayer(Player player, Location toLocation, boolean applyY) {
		Vector velocity = toLocation.toVector().subtract(player.getLocation().toVector()).multiply(0.15);
		velocity.setY(applyY ? 0.1 : player.getVelocity().getY());
		player.setVelocity(velocity);
	}

	private void setupPortal(Location portalLocation, FishingArena arena) {
		Hitbox hitbox = new Hitbox.SphereHitbox(portalLocation, 2);
		new BukkitRunnable() {
			int mTicks;
			@Override
			public void run() {
				if (mTicks >= PORTAL_DURATION) {
					if (!arena.mActive) {
						arena.mOccupied = false;
						mPlayerArenaMap.remove(arena.mOwner);
					}
					this.cancel();
				}
				if (!arena.mOccupied) {
					return;
				}
				for (Player player : hitbox.getHitPlayers(false)) {
					if (!(mPlayerArenaMap.containsKey(player) || arena.mActive)) {
						continue;
					}
					player.teleport(arena.mCoordinates.toLocation(player.getWorld()));
					if (!arena.mActive) {
						arena.mActive = true;
						initiateCombat(player, arena);
					}
					if (!mPlayerArenaMap.containsKey(player)) {
						mPlayerArenaMap.put(player, arena);
					}
				}
				mTicks += 5;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 5);
	}

	private void initiateCombat(Player player, FishingArena arena) {
		ejectMobs(arena, player.getWorld());
		arena.mWave = 1;
		spawnWave(arena.mWave, player, arena, arena.mDifficulty);
		trackArena(player, arena);
	}

	private void spawnWave(int wave, Player player, FishingArena arena, int difficulty) {
		int[] mobTypes = WAVE_MOB_TYPES[wave - 1 + 3 * difficulty];
		int delayBetweenMobs = MOB_SPAWN_DELAYS[difficulty];
		new BukkitRunnable() {
			int mMobsSpawned = 0;
			@Override
			public void run() {
				if (mMobsSpawned > mobTypes.length - 1) {
					this.cancel();
					return;
				}
				Vector spawnCoordinates = arena.mSummonCoordinates.get(FastUtils.randomIntInRange(0, arena.mSummonCoordinates.size() - 1));
				Location spawnLocation = new Location(player.getWorld(), spawnCoordinates.getX(), spawnCoordinates.getY(), spawnCoordinates.getZ());
				spawnMob(mobTypes[mMobsSpawned], spawnLocation, player, difficulty);
				mMobsSpawned++;
			}
		}.runTaskTimer(Plugin.getInstance(), 25, delayBetweenMobs);
	}

	// Progress wave when all mobs are killed in the arena.
	private void trackArena(Player player, FishingArena arena) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!arena.mOccupied) {
					this.cancel();
					return;
				}

				int radius = arena.mRadius;
				Hitbox arenaSpace = new Hitbox.SphereHitbox(new Location(player.getWorld(), arena.mCoordinates.getX(), arena.mCoordinates.getY(), arena.mCoordinates.getZ()), radius);
				if (arenaSpace.getHitMobs().isEmpty()) {
					progressWave(player, arena);
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 80, 80);
	}

	private void spawnMob(int type, Location location, Player player, int difficulty) {
		Entity entity;
		switch (type) {
			default -> entity = POOL_COMMON.spawn(location);
			case 1 -> entity = POOL_UNCOMMON.spawn(location);
			case 2 -> entity = POOL_ELITE.spawn(location);
		}
		if (difficulty > 0 && entity instanceof LivingEntity livingEntity) {
			double damageMultiplier = 1 + difficulty * 0.3;
			double healthModifier = difficulty * 0.15;

			ArrayList<LivingEntity> stackedMobs = new ArrayList<>();
			EntityUtils.getStackedMobsAbove(livingEntity, stackedMobs);
			for (LivingEntity mob : stackedMobs) {
				scaleMob(mob, damageMultiplier, healthModifier);
			}
		}
		spawnMobAesthetics(location, player.getWorld());
	}

	private void scaleMob(LivingEntity entity, double damageMult, double healthMod) {
		EntityUtils.scaleMaxHealth(entity, healthMod, "FishingMobMultiplier");
		entity.addScoreboardTag(StatMultiplierBoss.identityTag);
		entity.addScoreboardTag(StatMultiplierBoss.identityTag + "[damagemult=" + damageMult + "]");
	}

	private void spawnMobAesthetics(Location location, World world) {
		ArrayList<Vector> helix = new ArrayList<>();
		for (double d = 0; d < 3 * Math.PI; d += 2 * Math.PI / 40) {
			helix.add(new Vector(1.8 * FastUtils.cos(d), -0.5 + 2 * d / (3 * Math.PI), 1.8 * FastUtils.sin(d)));
			helix.add(new Vector(-1.8 * FastUtils.cos(d), -0.5 + 2 * d / (3 * Math.PI), -1.8 * FastUtils.sin(d)));
		}

		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				for (int i = 0; i < 12; i++) {
					new PartialParticle(Particle.REDSTONE, location.clone().add(helix.get(i + mTicks * 6))).data(new Particle.DustOptions(Color.fromRGB(50, 100, 255), 0.75f)).count(2).minimumCount(0).spawnAsEnemy();
				}
				mTicks += 2;
				if (mTicks >= 20) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 2);

		world.playSound(location, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, SoundCategory.HOSTILE, 1f, 1.5f);
		world.playSound(location, Sound.ENTITY_DROWNED_AMBIENT_WATER, SoundCategory.HOSTILE, 0.8f, 0.5f);
	}

	private void waveClearAesthetics(FishingArena arena, Player player) {
		Location loc = player.getLocation();
		final double radius = 5 + arena.mWave * 0.8;
		new PartialParticle(Particle.CRIT_MAGIC, loc).count(48 + arena.mWave * 12).delta(radius, 0, radius).spawnAsEnemy();
		new PartialParticle(Particle.SNOWFLAKE, loc).count(24 + arena.mWave * 6).delta(radius, 0, radius).spawnAsEnemy();
		new PartialParticle(Particle.REDSTONE, loc).data(new Particle.DustOptions(Color.fromRGB(50, 100, 255), 0.75f)).count(24 + arena.mWave * 6).delta(radius, 0, radius).spawnAsEnemy();
		new PartialParticle(Particle.REDSTONE, loc).data(new Particle.DustOptions(Color.fromRGB(0, 70, 200), 0.75f)).count(24 + arena.mWave * 6).delta(radius, 0, radius).spawnAsEnemy();
		player.getWorld().playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, SoundCategory.HOSTILE, 1f, 0.6f + 0.1f * arena.mWave);
	}

	private void drawWhirlpool(Location location) {
		PPSpiral spiral = new PPSpiral(Particle.WATER_WAKE, location, 6).distanceFalloff(30);

		new BukkitRunnable() {
			final int mMaxRuns = PORTAL_DURATION / 30;
			int mTimesRun = 0;

			@Override
			public void run() {
				spiral.spawnFull();
				location.getWorld().playSound(location, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 0.2f, 0.8f);
				location.getWorld().playSound(location, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.HOSTILE, 0.2f, 0.8f);

				if (mTimesRun >= mMaxRuns) {
					cancel();
					return;
				}
				mTimesRun++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 30);
	}
}
