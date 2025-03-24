package com.playmonumenta.plugins.hunts;

import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.hunts.bosses.AlocAcoc;
import com.playmonumenta.plugins.hunts.bosses.CoreElemental;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import com.playmonumenta.plugins.hunts.bosses.SteelWingHawk;
import com.playmonumenta.plugins.hunts.bosses.TheImpenetrable;
import com.playmonumenta.plugins.hunts.bosses.Uamiel;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.monumentanetworkrelay.BroadcastedEvents;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.RBoardAPI;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HuntsManager {
	private static final String HUNTS_SCOREHOLDER = "$Hunts";
	private static final String TIME_OBJECTIVE = "HuntsSpawnTime";
	private static final String BAITED_OBJECTIVE = "HuntsBaited";
	private static final String QUARRY_OBJECTIVE = "HuntsNextQuarry";
	private static final String INSTANCE_OBJECTIVE = "HuntsNextInstance";

	private static final String ANNOUNCEMENT_DISABLE_TAG = "HuntsAnnouncementDisable";

	private static final int MEAN_SECONDS = 120 * 60;
	private static final int SD_SECONDS = 15 * 60;

	private static final int WARNING_15 = 15 * 60;
	private static final int WARNING_5 = 5 * 60;

	public enum QuarryType {
		ALOC_ACOC(
			"Aloc Acoc", "AlocAcoc", AlocAcoc.identityTag,
			new Vector(247, 33, 1334), new Vector(210, 9, 1298), AlocAcoc.INNER_RADIUS,
			AlocAcoc.COLOR, Sound.ENTITY_POLAR_BEAR_WARNING, "A chill descends on the glacier beneath the crystal cliffs. Aloc Acoc is reawakening."
		),
		CORE_ELEMENTAL(
			"Core Elemental", "CoreElemental", CoreElemental.identityTag,
			new Vector(-351, 97, 141), new Vector(-382, 93, 112), CoreElemental.INNER_RADIUS,
			CoreElemental.COLOR, Sound.ENTITY_MAGMA_CUBE_JUMP, "Deep-down magma begins to rumble. Core Elemental materializes soon."
		),
		STEEL_WING_HAWK(
			"Steel Wing Hawk", "SteelWingHawk", SteelWingHawk.identityTag,
			new Vector(-264, 137, -521), new Vector(-311, 134, -562), SteelWingHawk.INNER_RADIUS,
			SteelWingHawk.COLOR, Sound.ENTITY_PHANTOM_SWOOP, "Screeches tear down through the boughs. Steel Wing Hawk begins to stir."
		),
		THE_IMPENETRABLE(
			"The Impenetrable", "TheImpenetrable", TheImpenetrable.identityTag,
			new Vector(-206, 40, -353), new Vector(-245, 33, -395), TheImpenetrable.INNER_RADIUS,
			TheImpenetrable.COLOR, Sound.ENTITY_SHULKER_SHOOT, "The caves chime as inexplicable phenomena occur. The Impenetrable is about to appear."
		),
		UAMIEL(
			"Uamiel", "Uamiel", Uamiel.identityTag, new Vector(-692, 38, -384),
			new Vector(-726, 32, -416), Uamiel.INNER_RADIUS,
			Uamiel.TEXT_COLOR, Sound.ENTITY_RAVAGER_ROAR, "Distant roars echo from the coastal hills. Uamiel is on the prowl."
		),
		EXPERIMENT_SEVENTY_ONE(
			"Experiment Seventy-One", "ExperimentSeventyOne", ExperimentSeventyOne.identityTag,
			new Vector(-351, 174, 271), new Vector(-382, 172, 241), ExperimentSeventyOne.INNER_RADIUS,
			ExperimentSeventyOne.TEXT_COLOR, Sound.ENTITY_HOGLIN_ANGRY, "Sloshing can be heard from the mud patch. Experiment Seventy-One is looking to make a mess."
		);

		private final String mName;
		private final String mLos;
		private final String mTag;
		private final Vector mSpawnLoc;
		private final Vector mRespawnArenaPos;
		private final double mInnerRadius;
		private final TextColor mColor;
		private final Sound mSound;
		private final String mWarning;

		QuarryType(String name, String los, String tag, Vector spawnLoc, Vector respawnArenaPos, double innerRadius, TextColor color, Sound sound, String warning) {
			mName = name;
			mLos = los;
			mTag = tag;
			mSpawnLoc = spawnLoc;
			mRespawnArenaPos = respawnArenaPos;
			mInnerRadius = innerRadius;
			mColor = color;
			mSound = sound;
			mWarning = warning;
		}

		public String getName() {
			return mName;
		}

		public String getLos() {
			return mLos;
		}

		public TextColor getColor() {
			return mColor;
		}

		public Location getLocation(World world) {
			return mSpawnLoc.toLocation(world);
		}

		public @Nullable LivingEntity summon(World world) {
			if (!Plugin.IS_PLAY_SERVER) {
				return null;
			}

			Location loc = getLocation(world);
			if (!loc.isChunkLoaded()) {
				BroadcastedEvents.clearEvent(name());
				MMLog.fine("[Hunts] Failed to summon quarry " + mName + " because the chunk was unloaded.");
				return null;
			}

			respawnArena(world);

			Entity entity = LibraryOfSoulsIntegration.summon(loc, mLos);
			if (entity instanceof LivingEntity quarry) {
				// Special case - TheImpenetrable los summons an armor stand that the boss rides on
				if (this == THE_IMPENETRABLE) {
					List<Entity> passengers = quarry.getPassengers();
					if (!passengers.isEmpty() && passengers.get(0) instanceof Shulker shulker) {
						quarry = shulker;
					}
				}

				try {
					BossManager.createBoss(null, quarry, mTag, loc);
					BroadcastedEvents.updateEvent(name(), 0);
					MMLog.fine("[Hunts] Successfully summoned quarry " + mName);
				} catch (Exception e) {
					BroadcastedEvents.clearEvent(name());
					MMLog.severe("[Hunts] Failed to initialize boss tag " + mTag);
					e.printStackTrace();
				}
				return quarry;
			} else {
				MMLog.severe("[Hunts] Failed to get quarry " + mLos + " from Library of Souls!");
				return null;
			}
		}

		public void respawnArena(World world) {
			StructuresAPI.loadAndPasteStructure(String.format("ring/hunts/%s", mName.replaceAll(" ", "")), mRespawnArenaPos.toLocation(world), false, false);
		}
	}

	private @Nullable BukkitRunnable mRunnable;

	// In practice these should never be null because the initialization will set them
	private @Nullable QuarryType mNextQuarry = null;
	private @Nullable String mNextInstance = null;
	private boolean mIsBaited;
	private long mSpawnTime;

	private boolean mTriggeredFifteen = false;
	private boolean mTriggeredFive = false;

	public final Plugin mPlugin;
	public final @Nullable World mWorld;

	public HuntsManager(Plugin plugin) {
		mPlugin = plugin;
		mWorld = Bukkit.getWorld("Project_Epic-ring");

		refresh().thenRun(() -> {
			// If there was a hunt that should have happened here but this shard was down, do it again later
			if (getRemainingTime() <= 0 && isCorrectInstance()) {
				setRandomTime().thenRun(this::refreshOthers);
			}
			MMLog.info("[Hunts] Restarted timer due it being negative when manager was initialized");
		});
		initializeTimer();
	}

	public void initializeTimer() {
		if (!Plugin.IS_PLAY_SERVER) {
			return;
		}
		if (mRunnable != null) {
			mRunnable.cancel();
		}
		mRunnable = new BukkitRunnable() {

			@Override
			public void run() {
				if (isCorrectInstance()) {
					long remainingTime = getRemainingTime();
					if (remainingTime <= WARNING_15) {
						if (remainingTime <= 0) {
							summonQuarry();
							//TODO spawn message
							MMLog.fine("[Hunts] Timer is up and this is the correct instance, summoning quarry");
							return;
						} else if (remainingTime <= WARNING_5) {
							if (!mTriggeredFive) {
								sendWarning();
								mTriggeredFive = true;
								MMLog.fine("[Hunts] Sent 5 minute warning");
							}
						} else {
							if (!mTriggeredFifteen) {
								sendWarning();
								mTriggeredFifteen = true;
								MMLog.fine("[Hunts] Sent 15 minute warning");
							}
						}

						if (mNextQuarry != null) {
							BroadcastedEvents.updateEvent(mNextQuarry.name(), (int) remainingTime);
							preSpawnParticles(mNextQuarry, remainingTime);
						}
					}

				}
			}
		};
		mRunnable.runTaskTimer(mPlugin, 5 * 20, 5 * 20);
	}

	private boolean isCorrectInstance() {
		try {
			return NetworkRelayAPI.getShardName().equals(mNextInstance);
		} catch (Exception e) {
			MMLog.severe("[Hunts] Exception caught when checking instance name.");
			e.printStackTrace();
		}
		return false;
	}

	public void summonQuarry() {
		if (mWorld != null && mNextQuarry != null && mNextInstance != null) {
			List<Player> players = PlayerUtils.playersInRange(mNextQuarry.mSpawnLoc.toLocation(mWorld), mNextQuarry.mInnerRadius, true);
			int count = players.size();
			int extraInstances = count / 10;
			if (extraInstances > 0) {
				Map<String, List<Player>> instanceMap = new HashMap<>();
				instanceMap.put(mNextInstance, new ArrayList<>());

				Set<String> ringShards = getRingShards();
				int prevNum;
				if (mNextInstance.equals("ring")) {
					prevNum = 1;
				} else {
					prevNum = Integer.parseInt(mNextInstance.split("-")[1]);
				}
				for (int i = 0; i < extraInstances; i++) {
					String test = "ring-" + (prevNum + 1);
					if (ringShards.contains(test)) {
						instanceMap.put(test, new ArrayList<>());
						prevNum = prevNum + 1;
					} else {
						instanceMap.put("ring", new ArrayList<>());
						prevNum = 1;
					}
				}

				Map<Group, List<Player>> guildMap = new HashMap<>();
				List<Player> noGuildPlayers = new ArrayList<>();
				for (Player player: players) {
					Group guildMembershipGroup = LuckPermsIntegration.getGuild(player);
					Group guildRoot = LuckPermsIntegration.getGuildRoot(guildMembershipGroup);
					if (guildRoot == null) {
						noGuildPlayers.add(player);
					} else {
						guildMap.computeIfAbsent(guildRoot, key -> new ArrayList<>()).add(player);
					}
				}

				int maxPlayersPer = (int) Math.ceil(((double) count) / (1 + extraInstances));

				while (!guildMap.isEmpty()) {
					Group guild = guildMap.keySet().stream().max(Comparator.comparingInt(g -> Objects.requireNonNull(guildMap.get(g)).size())).get(); // Not empty, must be some maximum
					List<Player> members = Objects.requireNonNull(guildMap.get(guild));
					List<String> availableShards = instanceMap.keySet().stream().filter(s -> Objects.requireNonNull(instanceMap.get(s)).size() + members.size() <= maxPlayersPer).toList();
					if (availableShards.isEmpty()) {
						// We can't fit everyone, need to split up
						String leastPopulatedShard = instanceMap.keySet().stream().min(Comparator.comparingInt(s -> Objects.requireNonNull(instanceMap.get(s)).size())).orElse(null);
						if (leastPopulatedShard == null) {
							// Should never happen, give up
							MMLog.warning("[Hunts] Error in shard sorting algorithm - no shards available for guild sorting!");
							break;
						}
						List<Player> shardPlayers = Objects.requireNonNull(instanceMap.get(leastPopulatedShard));
						Collections.shuffle(members);
						for (int i = 0; i < maxPlayersPer - shardPlayers.size(); i++) {
							shardPlayers.add(members.remove(i)); // members size decreases, making progress
						}
					} else {
						String shard = FastUtils.getRandomElement(availableShards);
						List<Player> shardPlayers = Objects.requireNonNull(instanceMap.get(shard));
						shardPlayers.addAll(members);
						guildMap.remove(guild);
					}
				}

				Collections.shuffle(noGuildPlayers);
				for (String shard : instanceMap.keySet()) {
					List<Player> shardPlayers = Objects.requireNonNull(instanceMap.get(shard));
					while (shardPlayers.size() < maxPlayersPer && !noGuildPlayers.isEmpty()) {
						shardPlayers.add(noGuildPlayers.remove(0));
					}
				}

				for (String shard : instanceMap.keySet()) {
					if (shard.equals(mNextInstance)) {
						// Exclude this instance, no need to transfer
						continue;
					}
					for (Player player : instanceMap.get(shard)) {
						try {
							MonumentaRedisSyncAPI.sendPlayer(player, shard);
						} catch (Exception e) {
							MMLog.severe("[Hunts] Failed to sort players to shard " + shard);
						}
					}
				}

				// Give players time to switch their instance before spawning
				QuarryType quarry = mNextQuarry;
				String thisInstance = mNextInstance;
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					quarry.summon(mWorld);
					// Don't duplicate instances if we happen to have way too many players
					for (String shard : instanceMap.keySet()) {
						if (shard.equals(thisInstance)) {
							continue;
						}
						MonumentaNetworkRelayIntegration.broadcastCommand("hunts forcesummon " + quarry.name() + " " + shard);
					}
				}, 15 * 20);
			} else {
				mNextQuarry.summon(mWorld);
			}
		}
		startRandomHunt();
	}

	public void forceSummon(QuarryType quarry) {
		if (mWorld != null) {
			quarry.summon(mWorld);
		}
	}

	public void startRandomHunt() {
		if (getRemainingTime() > 0 && mNextQuarry != null && mNextInstance != null) {
			BroadcastedEvents.clearEvent(mNextQuarry.name(), mNextInstance);
		}
		CompletableFuture.allOf(
			setRandomTime(),
			setBaited(false),
			setRandomQuarry(),
			setRandomInstance()
		)
			.thenRun(this::refreshOthers)
			.thenRun(() -> {
				mTriggeredFifteen = false;
				mTriggeredFive = false;
			});
		MMLog.fine("[Hunts] Starting random hunt");
	}

	public void refreshOthers() {
		MonumentaNetworkRelayIntegration.broadcastCommand("hunts refresh");
		MMLog.fine("[Hunts] Refreshing other shards");
	}

	public CompletableFuture<Void> refresh() {
		if (!Plugin.IS_PLAY_SERVER) {
			return CompletableFuture.allOf();
		}
		MMLog.fine("[Hunts] Refreshing information");
		return CompletableFuture.allOf(
			RBoardAPI.getAsLong(HUNTS_SCOREHOLDER, QUARRY_OBJECTIVE, 0).whenComplete((val, ex) -> {
				if (ex != null) {
					MMLog.warning("[Hunts] Encountered exception when refreshing next Quarry:");
					ex.printStackTrace();
				} else {
					int score = val.intValue();
					if (score >= QuarryType.values().length) {
						MMLog.warning("[Hunts] HuntsNextQuarry score was " + score + ", while the maximum is " + (QuarryType.values().length - 1) + ". Defaulted to 0.");
						score = 0;
					}
					mNextQuarry = QuarryType.values()[score];
					MMLog.finer("[Hunts] Next Quarry is " + mNextQuarry.getName());
				}
			}),

			RBoardAPI.getAsLong(HUNTS_SCOREHOLDER, INSTANCE_OBJECTIVE, 1).whenComplete((val, ex) -> {
				if (ex != null) {
					MMLog.warning("[Hunts] Encountered exception when refreshing next instance:");
					ex.printStackTrace();
				} else {
					String nextInstance = null;
					if (val > 1) {
						for (String shard : getRingShards()) {
							if (shard.endsWith("-" + val)) {
								nextInstance = shard;
								break;
							}
						}
					}
					mNextInstance = nextInstance == null ? "ring" : nextInstance;
					MMLog.finer("[Hunts] Next instance is " + mNextInstance);
				}
			}),

			RBoardAPI.getAsLong(HUNTS_SCOREHOLDER, BAITED_OBJECTIVE, 0).whenComplete((val, ex) -> {
				if (ex != null) {
					MMLog.warning("[Hunts] Encountered exception when refreshing baited status:");
					ex.printStackTrace();
				} else {
					mIsBaited = val > 0;
					MMLog.finer("[Hunts] Next baited status is " + mIsBaited);
				}
			}),

			RBoardAPI.getAsLong(HUNTS_SCOREHOLDER, TIME_OBJECTIVE, 0).whenComplete((val, ex) -> {
				if (ex != null) {
					MMLog.warning("[Hunts] Encountered exception when refreshing spawn time:");
					ex.printStackTrace();
				} else {
					mSpawnTime = val;
					MMLog.finer("[Hunts] Next spawn time is " + mSpawnTime);
				}
			})
		);
	}

	private Set<String> getRingShards() {
		try {
			Set<String> shards = NetworkRelayAPI.getOnlineShardNames();
			shards.removeIf(shard -> !shard.startsWith("ring"));
			return shards;
		} catch (Exception e) {
			MMLog.severe("[Hunts] Caught exception when finding online shards");
			e.printStackTrace();
		}
		return new HashSet<>();
	}

	private CompletableFuture<Long> setRandomTime() {
		long seconds = (long) FastUtils.RANDOM.nextGaussian(MEAN_SECONDS, SD_SECONDS);
		// Always within 2 standard deviations to avoid extreme edge cases
		seconds = Math.max(MEAN_SECONDS - 2 * SD_SECONDS, Math.min(MEAN_SECONDS + 2 * SD_SECONDS, seconds));
		return setTime(seconds);
	}

	public CompletableFuture<Long> setTime(long seconds) {
		MMLog.finer("[Hunts] Set spawn time to " + seconds + " seconds from now");
		return RBoardAPI.set(HUNTS_SCOREHOLDER, TIME_OBJECTIVE, seconds + DateUtils.getSecondsSinceEpoch());
	}

	private CompletableFuture<Long> setBaited(boolean isBaited) {
		MMLog.finer("[Hunts] Set baited to " + isBaited);
		return RBoardAPI.set(HUNTS_SCOREHOLDER, BAITED_OBJECTIVE, isBaited ? 1 : 0);
	}

	private CompletableFuture<Long> setRandomQuarry() {
		return setQuarry(FastUtils.RANDOM.nextInt(0, QuarryType.values().length));
	}

	@SuppressWarnings("EnumOrdinal")
	private CompletableFuture<Long> setQuarry(QuarryType quarry) {
		return setQuarry(quarry.ordinal());
	}

	private CompletableFuture<Long> setQuarry(int ordinal) {
		MMLog.finer("[Hunts] Set next Quarry to " + QuarryType.values()[ordinal].getName());
		return RBoardAPI.set(HUNTS_SCOREHOLDER, QUARRY_OBJECTIVE, ordinal);
	}

	private CompletableFuture<Long> setRandomInstance() {
		int instance = FastUtils.RANDOM.nextInt(1, getRingShards().size() + 1);
		MMLog.finer("[Hunts] Set next instance to " + instance);
		return RBoardAPI.set(HUNTS_SCOREHOLDER, INSTANCE_OBJECTIVE, instance);
	}

	public void bait(Player player, QuarryType quarry) {
		if (mIsBaited) {
			player.sendMessage(Component.text("A Quarry has already been baited! You must wait until the next Hunt to bait another one.", NamedTextColor.RED));
		} else if (getRemainingTime() <= WARNING_15) {
			player.sendMessage(Component.text("It is too soon to the next Hunt to bait a Quarry! Wait until the Hunt is over to try again."));
		} else {
			player.sendMessage(Component.text("You baited " + quarry.getName() + "! It will be the next Quarry to be hunted.", NamedTextColor.GOLD));
			MMLog.info("[Hunts] Player " + player.getName() + " baited " + quarry.getName());
			CompletableFuture.allOf(
				setQuarry(quarry),
				setBaited(true)
			).thenRun(this::refreshOthers);
		}
	}

	public void track(Player player) {
		if (mNextQuarry == null || mNextInstance == null) {
			player.sendMessage(Component.text("Encountered an error; please try again soon."));
			return;
		}

		long remainingTime = getRemainingTime();
		String timeDisplay = "";
		if (remainingTime < WARNING_15) {
			if (remainingTime <= 0) {
				timeDisplay = " very soon";
			} else {
				timeDisplay = " in " + StringUtils.longToOptionalHoursMinuteAndSeconds(remainingTime);
			}
		}
		Component message = Component.empty()
			.append(Component.text("The next Quarry to be hunted is ", NamedTextColor.GOLD))
			.append(Component.text(mNextQuarry.getName(), NamedTextColor.DARK_RED, TextDecoration.BOLD))
			.append(Component.text((mIsBaited ? ", which has been baited" : "") + ". It will appear on " + mNextInstance + timeDisplay + ".", NamedTextColor.GOLD));
		player.sendMessage(message);
	}

	private void sendWarning() {
		try {
			NetworkRelayAPI.sendBroadcastCommand("hunts warn");
		} catch (Exception e) {
			MMLog.warning("[Hunts] Caught exception sending warning message:");
			e.printStackTrace();
		}
	}

	public void warn(List<Player> players) {
		if (mNextQuarry == null || mNextInstance == null) {
			MMLog.severe("[Hunts] Could not warn players due to null mNextQuarry or mNextInstance");
			return;
		}

		players.removeIf(player -> !hasLodgeAnnouncementScore(player));
		players.removeIf(player -> ScoreboardUtils.checkTag(player, ANNOUNCEMENT_DISABLE_TAG));
		int mins = (int) Math.round(getRemainingTime() / 60.0);
		if (mins <= 1) {
			return;
		}

		Component message1 = Component.text(mNextQuarry.mWarning, mNextQuarry.mColor);
		Component message2 = Component.text(mNextQuarry.getName() + " will be hunted on " + mNextInstance + " in " + mins + " minutes!", NamedTextColor.GRAY, TextDecoration.ITALIC);
		for (Player player : players) {
			player.sendMessage(message1);
			player.sendMessage(message2);
			player.playSound(player, mNextQuarry.mSound, SoundCategory.HOSTILE, mins > 5 ? 2.0f : 1.5f, 1.0f);
		}
	}

	public boolean hasLodgeAnnouncementScore(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "HuntsLodge").orElse(0) >= 2;
	}

	// In seconds
	public long getRemainingTime() {
		return mSpawnTime - DateUtils.getSecondsSinceEpoch();
	}

	public @Nullable Location getNextLocation() {
		if (mNextQuarry == null || mWorld == null) {
			return null;
		}
		Location loc = mNextQuarry.getLocation(mWorld);
		if (mNextQuarry == QuarryType.UAMIEL) {
			loc.add(0, 3, 0);
		}
		return loc;
	}

	private void preSpawnParticles(QuarryType quarryType, long timeRemaining) {
		if (mWorld == null) {
			return;
		}

		Particle particle = switch (quarryType) {
			case UAMIEL, EXPERIMENT_SEVENTY_ONE -> Particle.BLOCK_CRACK;
			case THE_IMPENETRABLE -> Particle.DRAGON_BREATH;
			case ALOC_ACOC -> Particle.SNOWFLAKE;
			case CORE_ELEMENTAL -> Particle.FLAME;
			case STEEL_WING_HAWK -> Particle.FIREWORKS_SPARK;
		};

		Location center = quarryType.getLocation(mWorld);
		if (quarryType == QuarryType.UAMIEL) {
			center.add(0, 3, 0);
		}

		if (quarryType == QuarryType.UAMIEL || quarryType == QuarryType.EXPERIMENT_SEVENTY_ONE) {
			new PPCircle(particle, center, quarryType.mInnerRadius * 0.5)
				.data((quarryType == QuarryType.UAMIEL ? Material.TUFF : Material.MUDDY_MANGROVE_ROOTS).createBlockData())
				.countPerMeter(18 - 5 * ((double) timeRemaining) / WARNING_15)
				.delta(0.5, 1.5, 0.5)
				.randomizeAngle(true)
				.ringMode(false)
				.spawnAsBoss();
		} else {
			Location center2 = center.clone().add(0, quarryType.mInnerRadius * 0.15, 0);
			int count = quarryType == QuarryType.STEEL_WING_HAWK ? (int) (140 - 35 * ((double) timeRemaining) / WARNING_15) : (int) (180 - 45 * ((double) timeRemaining) / WARNING_15);
			new PartialParticle(particle, center2)
				.count(count)
				.delta(quarryType.mInnerRadius * 0.55, quarryType.mInnerRadius * 0.3, quarryType.mInnerRadius * 0.55)
				.spawnAsBoss();
		}

		Location center2 = center.clone().add(0, quarryType.mInnerRadius * 0.11, 0);
		new PartialParticle(Particle.END_ROD, center2)
			.count((int) (20 - 8 * ((double) timeRemaining) / WARNING_15))
			.delta(quarryType.mInnerRadius * 0.35, quarryType.mInnerRadius * 0.2, quarryType.mInnerRadius * 0.35)
			.spawnAsBoss();
	}
}
