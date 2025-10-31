package com.playmonumenta.plugins.rush;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventListener;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class RushArena {

	private static final long TELEPORT_DELAY = Constants.TICKS_PER_SECOND * 3 / 2;

	private static final String POOL_COMMON_PREFIX = "~RushCommon";
	private static final String POOL_ELITE_PREFIX = "~RushElite";
	private static final String POOL_BOSS_PREFIX = "~RushBoss";
	private static final String STRUCTURE_PATH = "dungeon/rush/";

	private static final Component BOSS_WARNING = Component.text("Something stirs in the nexus...", NamedTextColor.RED).decorate(TextDecoration.ITALIC);
	private static final Component SCALING_WARNING = Component.text("The nexus has grown full. The denizens of dissonance grow stronger...", NamedTextColor.RED).decorate(TextDecoration.ITALIC);

	enum Season {
		SUMMER("Summer", Component.text("The atmosphere is buzzing with heat...", NamedTextColor.YELLOW),
			Sound.ENTITY_ENDERMAN_STARE, 2.0f,
			loc -> new PartialParticle(Particle.SMALL_FLAME, loc, RushArenaUtils.SEASON_PARTICLE_COUNT, 10, 6, 10)),

		FALL("Fall", Component.text("Strife echoes through the leaves...", NamedTextColor.GOLD),
			Sound.ENTITY_WITHER_DEATH, 1.25f,
			loc -> new PartialParticle(Particle.FALLING_DUST, loc, RushArenaUtils.SEASON_PARTICLE_COUNT, 10, 6, 10,
				Material.SPRUCE_WOOD.createBlockData())),

		WINTER("Winter", Component.text("A chilling presence lingers in the silence...", NamedTextColor.AQUA),
			Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, 1.0f,
			loc -> new PartialParticle(Particle.SNOWFLAKE, loc, RushArenaUtils.SEASON_PARTICLE_COUNT, 10, 6, 10)),

		SPRING("Spring", Component.text("Life begins to blossom beneath the soil...", NamedTextColor.GREEN),
			Sound.ENTITY_ALLAY_DEATH, 0.5f,
			loc -> new PartialParticle(Particle.SPORE_BLOSSOM_AIR, loc, RushArenaUtils.SEASON_PARTICLE_COUNT, 10, 6, 10)),

		DISSONANCE("Dissonance", Component.text("Dissonance stirs...", NamedTextColor.RED),
			Sound.ENTITY_ZOMBIE_HORSE_DEATH, 0.5f,
			loc -> new PartialParticle(Particle.REDSTONE, loc, RushArenaUtils.SEASON_PARTICLE_COUNT, 10, 6, 10,
				new Particle.DustOptions(Color.fromRGB(166, 40, 40), 1f)));

		final LoSPool commonPool;
		final LoSPool elitePool;
		final LoSPool boss;

		final Float pitch;
		final Sound sound;
		final TextComponent component;
		final Function<Location, PartialParticle> particleSupplier;

		Season(String season, TextComponent component, Sound sound, Float pitch, Function<Location, PartialParticle> particleSupplier) {
			commonPool = new LoSPool.LibraryPool(POOL_COMMON_PREFIX + season);
			elitePool = new LoSPool.LibraryPool(POOL_ELITE_PREFIX + season);
			boss = new LoSPool.LibraryPool(POOL_BOSS_PREFIX + season);

			this.pitch = pitch;
			this.sound = sound;
			this.component = component;
			this.particleSupplier = particleSupplier;
		}

		public PartialParticle doSeasonEffects(World world, Location loc, Set<Player> players) {
			Location center = loc.clone().add(0, 6, 0);

			world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 5.0f, 0.5f);
			world.playSound(center, sound, SoundCategory.AMBIENT, 5.0f, pitch);
			players.forEach(p -> p.sendMessage(component.decorate(TextDecoration.ITALIC)));
			return particleSupplier.apply(center);
		}
	}

	final Location mCenter;
	final Location mStructure;
	final ArmorStand mSpawnStand;
	final List<Location> mSpawnPoints = new ArrayList<>();
	final ArrayList<LoSPool> mMobPool = new ArrayList<>();
	final World mWorld;
	final Set<Player> mPlayers;

	int mPlayerCount;
	int mRound = 1;
	Season mSeason;
	double[] mMobCount = {};
	int mWave = 0;
	RushAntiCheese mCheese;

	/*
		Creates an instance of a Rush Combat Arena.
		The same arena can be used to run consecutive rounds.
		Otherwise, leaving it and returning from a checkpoint would create another one.
	 */
	public RushArena(List<Player> players) {
		Player player = players.get(0);

		mPlayers = new HashSet<>(players);
		mWorld = player.getWorld();

		// Retrieve every armor stand first
		List<ArmorStand> armorStands = RushManager.retrieveArmorStandList(player.getLocation());

		mSpawnPoints.addAll(
			armorStands.stream()
				.filter(armorStand -> armorStand.getScoreboardTags().contains(RushArenaUtils.RUSH_SPAWNER_TAG))
				.map(ArmorStand::getLocation)
				.toList()
		);
		if (mSpawnPoints.isEmpty()) {
			RushManager.printDebugMessage("Spawn points are not found!", player);
			throw new NoSuchElementException("Cannot find mSpawnPoints");
		}

		mSpawnStand = RushArenaUtils.getStandOrThrow(armorStands, RushArenaUtils.RUSH_SPAWN_TAG);
		mStructure = RushArenaUtils.getLocOrThrow(armorStands, RushArenaUtils.RUSH_STRUCTURE_TAG);
		mCenter = RushArenaUtils.getLocOrThrow(armorStands, RushArenaUtils.RUSH_CENTER_TAG);

		RushManager.addArenaToMap(players, this);
		mPlayers.addAll(players);

		// Process every armor stand
		mRound = RushArenaUtils.retrieveRound(mSpawnStand);
		mPlayerCount = RushArenaUtils.retrievePlayerCount(mSpawnStand, player);

		teleportPlayers(mCenter);
		setUpArena();
	}

	private void setUpArena() {
		mSeason = mRound == 1 ? Season.DISSONANCE : Season.values()[FastUtils.randomIntInRange(0, Season.values().length - 1)];
		mWave = 0;

		mMobPool.clear();
		mMobPool.add(mSeason.commonPool);
		mMobPool.add(mSeason.elitePool);
		mMobPool.add(mSeason.boss);

		mCheese = new RushAntiCheese(mPlayers, mCenter.getY());

		mMobCount = RushManager.calculateMobCount(mRound, false);
		RushManager.applyVulnerability(mRound, mPlayers);

		cleanUp(mStructure);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), this::startRound, TELEPORT_DELAY);
	}

	private void cleanUp(Location structure) {
		mWorld.getNearbyEntities(mCenter, RushArenaUtils.RUSH_RADIUS, RushArenaUtils.RUSH_RADIUS, RushArenaUtils.RUSH_RADIUS).stream()
			.filter(e -> e instanceof Projectile || (e instanceof LivingEntity && !(e instanceof Player) && !(e instanceof ArmorStand)))
			.forEach(Entity::remove);

		StructuresAPI.loadAndPasteStructure(STRUCTURE_PATH + mSeason.name().toLowerCase(Locale.ROOT),
			structure, false, false);
	}

	private void teleportPlayers(Location loc) {
		for (Player p : mPlayers) {
			p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (TELEPORT_DELAY * 2), 0, false, false, false));
			p.playSound(p, Sound.ENTITY_GUARDIAN_ATTACK, SoundCategory.HOSTILE, 1f, 1.5f);
			p.playSound(p, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 1f, 1.5f);
		}
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			for (Player p : mPlayers) {
				PlayerUtils.playerTeleport(p, loc);
				p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1f, 1.5f);
				p.playSound(p, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 1f, 2f);
				MovementUtils.knockAway(loc.clone().add(0, -1, 0), p, 0.55f, 0.3f, false);
			}
		}, TELEPORT_DELAY);
	}

	private void startRound() {
		mPlayers.forEach(p -> p.sendMessage(MessagingUtils.fromMiniMessage(String.format("<red>Starting round <yellow>%s</yellow>...</red>", mRound))));

		// Wave progression requires 25% of current wave to remain
		new BukkitRunnable() {
			final double mNextWaveReq = (mMobCount[0] + mMobCount[1] + mMobCount[2]) * 0.25;
			boolean mWaiting = false;

			@Override
			public void run() {
				if (mPlayers.isEmpty()) {
					this.cancel();
					return;
				}
				if (mWaiting) {
					List<LivingEntity> allArenaEnemies = EntityUtils.getNearbyMobs(mCenter, RushArenaUtils.RUSH_RADIUS);
					if (allArenaEnemies.size() <= mNextWaveReq) {
						mWaiting = false;
					}
				}
				if (!mWaiting) {
					if (++mWave == RushManager.WAVE_PER_ROUND) {
						finalWave();
						this.cancel();
						return;
					}
					spawnWave();
					mWaiting = true;
				}
			}
		}.runTaskTimer(Plugin.getInstance(), Constants.TICKS_PER_SECOND * 2, Constants.TICKS_PER_SECOND * 2);
	}

	boolean mCountMobs = false;

	private void finalWave() {
		mCountMobs = false;
		mMobCount = RushManager.calculateMobCount(mRound, true);
		final PartialParticle mParticles = mSeason.doSeasonEffects(mWorld, mCenter, mPlayers);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			spawnWave();
			mCountMobs = true;
		}, Constants.TICKS_PER_SECOND * 3);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mPlayers.isEmpty()) {
					this.cancel();
					return;
				}
				mParticles.spawnAsBoss();
				if (mCountMobs) {
					List<LivingEntity> allArenaEnemies = EntityUtils.getNearbyMobs(mCenter, RushArenaUtils.RUSH_RADIUS);
					if (allArenaEnemies.size() <= 5) {
						allArenaEnemies.forEach(e -> GlowingManager.startGlowing(e, NamedTextColor.RED, -1, 1));
					}
					// Final wave is conquered
					if (allArenaEnemies.isEmpty()) {
						finishRound();
						mCheese.disable();
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> promptBreakRequest(), Constants.TICKS_PER_SECOND);
						this.cancel();
					}
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, Constants.HALF_TICKS_PER_SECOND);
	}

	private void finishRound() {
		int prevRound = mRound;
		mRound++;

		for (Player p : mPlayers) {
			p.sendMessage(MessagingUtils.fromMiniMessage(String.format("<red>Round <yellow>%s</yellow> conquered!</red>", prevRound)));

			SeasonalEventListener.playerRushRound(p);
			RushManager.restorePlayer(p);

			boolean isScalingRound = mRound == RushManager.SCALING_ROUND + 1;
			boolean isBossRound = mRound >= RushManager.BOSS_ROUND && mRound % RushManager.BOSS_INCREMENT == 0;

			if (isScalingRound || isBossRound) {
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					p.playSound(p, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 5.0f, 0.5f);
					if (isBossRound) {
						p.sendMessage(BOSS_WARNING);
					}
					if (isScalingRound) {
						p.sendMessage(SCALING_WARNING);
					}
				}, Constants.TICKS_PER_SECOND * 2);
			}
		}
		mSpawnStand.getPersistentDataContainer().set(RushManager.RUSH_WAVE_KEY, PersistentDataType.INTEGER, mRound);
	}

	boolean mRequestWindow = false;

	private void promptBreakRequest() {
		mRequestWindow = true;

		for (Player p : mPlayers) {
			p.sendMessage(RushManager.BREAK_ASK);
			p.sendMessage(Component.text("[Yes]")
				.color(NamedTextColor.LIGHT_PURPLE)
				.clickEvent(ClickEvent.runCommand("/rushpause")));
		}

		new BukkitRunnable() {
			int mSecond = 0;

			@Override
			public void run() {
				if (!mRequestWindow) {
					wrapUpArena();
					this.cancel();
					return;
				}

				if (mSecond >= 5) {
					mRequestWindow = false;
					mPlayers.forEach(p -> p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Constants.TICKS_PER_SECOND * 4, 0, false, false, false)));
					mWorld.playSound(mCenter, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.AMBIENT, 5.0f, 0.5f);
					mWorld.playSound(mCenter, Sound.ENTITY_MINECART_RIDING, SoundCategory.AMBIENT, 5.0f, 0.6f);
					setUpArena();
					this.cancel();
					return;
				}
				mPlayers.forEach(p -> MessagingUtils.sendActionBarMessage(p, "Starting in... " + (5 - mSecond)));
				mSecond++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, Constants.TICKS_PER_SECOND);
	}

	private void wrapUpArena() {
		teleportPlayers(mSpawnStand.getLocation());

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			RushManager.clearArenaFromMap(mPlayers);
			mPlayers.clear();
		}, TELEPORT_DELAY);
	}

	private void spawnWave() {
		for (int i = 0; i < mMobCount.length; i++) {
			double count = mMobCount[i];
			while (count >= 1 || count > FastUtils.RANDOM.nextDouble()) {
				count--;
				Entity entity = mMobPool.get(i).spawn(getRandomLoc());
				RushManager.scaleMobHealthMultiplayer(entity, mPlayerCount);
				RushManager.scaleMobPastRound(entity, mRound);
			}
		}
		if (mSeason.equals(Season.FALL) && FastUtils.RANDOM.nextDouble() < 0.05) {
			Objects.requireNonNull(LibraryOfSoulsIntegration.summon(getRandomLoc(), "WanderingTuathan"));
		}
		mWorld.playSound(mCenter, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.BLOCKS, 5f, 1.5f);
		mWorld.playSound(mCenter, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 5f, 2f);
	}

	private Location getRandomLoc() {
		return LocationUtils.randomSafeLocationInCircle(
				mSpawnPoints.get(FastUtils.randomIntInRange(0, mSpawnPoints.size() - 1)),
				2,
				loc -> !loc.getBlock().isSolid())
			.subtract(0, RushDownMobBoss.HEIGHT - 0.1, 0);
	}

}
