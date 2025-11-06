package com.playmonumenta.plugins.hunts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.networkrelay.RemotePlayerData;
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
import com.playmonumenta.plugins.integrations.monumentanetworkrelay.BroadcastedEvents;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.RBoardAPI;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

public class HuntsManager implements Listener {
	private static final String HUNTS_SCOREHOLDER = "$Hunts";
	private static final String TIME_OBJECTIVE = "HuntsSpawnTime";
	private static final String BAITED_OBJECTIVE = "HuntsBaited";
	private static final String QUARRY_OBJECTIVE = "HuntsNextQuarry";
	private static final String IN_RANGE_CHANNEL = "com.playmonumenta.plugins.hunts.HuntsManager.playersInRange";
	private static final String TRANSFER_REQUEST_CHANNEL = "com.playmonumenta.plugins.hunts.HuntsManager.transferRequest";

	private static final String ANNOUNCEMENT_DISABLE_TAG = "HuntsAnnouncementDisable";

	private static final String CLASS_DISABLE_TAG = "HuntsDisableClass";

	private static final int MEAN_SECONDS = 120 * 60;
	private static final int SD_SECONDS = 10 * 60;

	private static final int WARNING_30 = 30 * 60;
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
			"The Impenetrable", "TheImpenetrableVehicle", TheImpenetrable.identityTag,
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

			for (Player player : world.getPlayers()) {
				if (player.getScoreboardTags().contains(CLASS_DISABLE_TAG)) {
					player.removeScoreboardTag(CLASS_DISABLE_TAG);
					player.removeScoreboardTag("disable_class");
					AbilityUtils.refreshClass(player);

					player.sendMessage(Component.text("The Hunt has begun and your class has been reenabled!", NamedTextColor.GRAY, TextDecoration.ITALIC));
				}
			}

			Location loc = getLocation(world);
			if (!loc.isChunkLoaded()) {
				BroadcastedEvents.clearEvent(name(), "ring");
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
					MMLog.fine("[Hunts] Successfully summoned quarry " + mName);
				} catch (Exception e) {
					MMLog.severe("[Hunts] Failed to initialize boss tag " + mTag, e);
				}
				BroadcastedEvents.clearEvent(name(), "ring");
				return quarry;
			} else {
				MMLog.severe("[Hunts] Failed to get quarry " + mLos + " from Library of Souls!");
				BroadcastedEvents.clearEvent(name(), "ring");
				return null;
			}
		}

		public void respawnArena(World world) {
			StructuresAPI.loadAndPasteStructure(String.format("ring/hunts/%s", mName.replaceAll(" ", "")), mRespawnArenaPos.toLocation(world), false, false)
				.thenAccept((v) -> {
					for (Player player : PlayerUtils.playersInRange(getLocation(world), mInnerRadius, true)) {
						if (player.getEyeLocation().getBlock().isSolid()) {
							player.teleport(LocationUtils.mapToGround(player.getLocation().clone().add(0, 5, 0), 5));
						}
					}
				});
		}
	}

	private @Nullable BukkitRunnable mRunnable;

	// In practice these should never be null because the initialization will set them
	private @Nullable QuarryType mNextQuarry = null;
	private boolean mIsBaited;
	private long mSpawnTime;

	private boolean mTriggeredThirty = false;
	private boolean mTriggeredFifteen = false;
	private boolean mTriggeredFive = false;
	private final Map<String, Set<UUID>> mPlayersInRange = new TreeMap<>();
	private boolean mSpawningBoss = false; // True if this shard is in the 15-second waiting time before spawning a boss

	public final Plugin mPlugin;
	public final @Nullable World mWorld;

	public HuntsManager(Plugin plugin) {
		mPlugin = plugin;
		mWorld = Bukkit.getWorld("Project_Epic-ring");

		boolean isRingShard;
		boolean isOverseerShard;
		String shardName = NetworkRelayAPI.getShardName();
		if (shardName.startsWith("ring")) {
			isRingShard = true;
			isOverseerShard = shardName.equals("ring");
		} else {
			isRingShard = false;
			isOverseerShard = false;
		}

		refresh().thenRun(() -> {
			// If there was a hunt that should have happened here but this shard was down, do it again later
			if (isOverseerShard && getRemainingTime() <= 0) {
				setTime(WARNING_15).thenRun(this::refreshOthers);
			}
			MMLog.info("[Hunts] Restarted timer due it being negative when manager was initialized");
		});

		if (isRingShard) {
			initializeTimer(isOverseerShard);
		}
	}

	public void initializeTimer(boolean overseer) {
		if (!Plugin.IS_PLAY_SERVER) {
			return;
		}
		if (mRunnable != null) {
			mRunnable.cancel();
		}
		mRunnable = new BukkitRunnable() {

			@Override
			public void run() {
				long remainingTime = getRemainingTime();
				if (remainingTime <= WARNING_30) {
					if (remainingTime <= 0) {
						if (overseer) {
							sendTransferRequest();
							MMLog.fine("[Hunts] Timer is up, sending transfer request");
						}
						return;
					} else if (remainingTime <= WARNING_5) {
						// Start broadcasting which players are in range at the 5 minutes mark
						sendLocalPlayersInRange();
						if (overseer && !mTriggeredFive) {
							sendWarning();
							mTriggeredFive = true;
							MMLog.fine("[Hunts] Sent 5 minute warning");
						}
					} else if (remainingTime <= WARNING_15) {
						if (!mTriggeredFifteen) {
							if (overseer) {
								sendWarning();
								mTriggeredFifteen = true;
								MMLog.fine("[Hunts] Sent 15 minute warning");
							}

							if (mNextQuarry != null && mWorld != null) {
								mNextQuarry.respawnArena(mWorld);
							}
						}
					} else {
						if (overseer && !mTriggeredThirty) {
							sendWarning();
							mTriggeredThirty = true;
							MMLog.fine("[Hunts] Sent 30 minute warning");
						}
					}

					if (getRemainingTime() < WARNING_15) {
						if (mNextQuarry != null) {
							if (overseer) {
								BroadcastedEvents.updateEvent(mNextQuarry.name(), (int) remainingTime);
							}
							preSpawnParticles(mNextQuarry, remainingTime);
						}

						if (mWorld != null) {
							for (Player player : mWorld.getPlayers()) {
								updatePlayerWaiting(player);
							}
						}
					}
				}
			}
		};
		mRunnable.runTaskTimer(mPlugin, 5 * 20, 5 * 20);
	}

	public void forceSummon(QuarryType quarry) {
		if (mWorld != null) {
			quarry.summon(mWorld);
		}
	}

	public void startRandomHunt() {
		if (getRemainingTime() > 0 && mNextQuarry != null) {
			BroadcastedEvents.clearEvent(mNextQuarry.name(), "ring");
		}
		CompletableFuture.allOf(
				setRandomTime(),
				setBaited(false),
				setRandomQuarry()
			)
			.thenRun(this::refreshOthers)
			.thenRun(() -> {
				mTriggeredThirty = false;
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
					MMLog.warning("[Hunts] Encountered exception when refreshing next Quarry:", ex);
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

			RBoardAPI.getAsLong(HUNTS_SCOREHOLDER, BAITED_OBJECTIVE, 0).whenComplete((val, ex) -> {
				if (ex != null) {
					MMLog.warning("[Hunts] Encountered exception when refreshing baited status:", ex);
				} else {
					mIsBaited = val > 0;
					MMLog.finer("[Hunts] Next baited status is " + mIsBaited);
				}
			}),

			RBoardAPI.getAsLong(HUNTS_SCOREHOLDER, TIME_OBJECTIVE, 0).whenComplete((val, ex) -> {
				if (ex != null) {
					MMLog.warning("[Hunts] Encountered exception when refreshing spawn time:", ex);
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
			MMLog.severe("[Hunts] Caught exception when finding online shards", e);
		}
		return new HashSet<>();
	}

	private CompletableFuture<Long> setRandomTime() {
		long seconds = (long) FastUtils.RANDOM.nextGaussian(MEAN_SECONDS, SD_SECONDS);
		// Always within 2 standard deviations to avoid extreme edge cases
		seconds = Math.max(MEAN_SECONDS - 3 * SD_SECONDS, Math.min(MEAN_SECONDS + 3 * SD_SECONDS, seconds));
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
		List<QuarryType> quarries = Arrays.stream(QuarryType.values()).filter(q -> q != mNextQuarry).toList();
		return setQuarry(FastUtils.getRandomElement(quarries));
	}

	@SuppressWarnings("EnumOrdinal")
	private CompletableFuture<Long> setQuarry(QuarryType quarry) {
		return setQuarry(quarry.ordinal());
	}

	private CompletableFuture<Long> setQuarry(int ordinal) {
		MMLog.finer("[Hunts] Set next Quarry to " + QuarryType.values()[ordinal].getName());
		return RBoardAPI.set(HUNTS_SCOREHOLDER, QUARRY_OBJECTIVE, ordinal);
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
		if (mNextQuarry == null) {
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
			.append(Component.text((mIsBaited ? ", which has been baited" : "") + ". It will appear " + timeDisplay + ".", NamedTextColor.GOLD));
		player.sendMessage(message);
	}

	private void sendWarning() {
		try {
			NetworkRelayAPI.sendBroadcastCommand("hunts warn");
		} catch (Exception e) {
			MMLog.warning("[Hunts] Caught exception sending warning message:", e);
		}
	}

	public void warn(List<Player> players) {
		if (mNextQuarry == null) {
			MMLog.severe("[Hunts] Could not warn players due to null mNextQuarry");
			return;
		}

		players.removeIf(player -> !hasLodgeAnnouncementScore(player));
		players.removeIf(player -> ScoreboardUtils.checkTag(player, ANNOUNCEMENT_DISABLE_TAG));
		int minutes = (int) Math.round(getRemainingTime() / 60.0);
		if (minutes <= 1) {
			return;
		}

		if (minutes > 15) {
			Component message1 = Component.text("The creatures of the Ring are unsettled... A great beast begins to stir.", com.playmonumenta.plugins.itemstats.enums.Location.HUNTS.getColor());
			Component message2 = Component.text("A quarry will emerge in " + minutes + " minutes!", NamedTextColor.GRAY, TextDecoration.ITALIC);
			for (Player player : players) {
				player.sendMessage(message1);
				player.sendMessage(message2);
				player.playSound(player, Sound.ENTITY_PARROT_FLY, SoundCategory.HOSTILE, 5f, 0.5f);
				player.playSound(player, Sound.ENTITY_PARROT_FLY, SoundCategory.HOSTILE, 5f, 1f);
				player.playSound(player, Sound.ENTITY_PARROT_FLY, SoundCategory.HOSTILE, 5f, 2f);
			}
		} else {
			Component message1 = Component.text(mNextQuarry.mWarning, mNextQuarry.mColor);
			Component message2 = Component.text(mNextQuarry.getName() + " will be hunted in " + minutes + " minutes!", NamedTextColor.GRAY, TextDecoration.ITALIC);
			for (Player player : players) {
				player.sendMessage(message1);
				player.sendMessage(message2);
				player.playSound(player, mNextQuarry.mSound, SoundCategory.HOSTILE, minutes > 5 ? 2.0f : 1.5f, 1.0f);
			}
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

	private void playerEnterWaitingArea(Player player) {
		player.addScoreboardTag("disable_class");
		AbilityUtils.refreshClass(player);

		if (mNextQuarry != null) {
			player.sendMessage(Component.text("You have entered the range of " + mNextQuarry.getName() + ".", mNextQuarry.mColor));
			player.sendMessage(Component.text("Your class has been disabled before the fight.", NamedTextColor.GRAY, TextDecoration.ITALIC));
		}
	}

	private void playerExitWaitingArea(Player player) {
		player.removeScoreboardTag("disable_class");
		AbilityUtils.refreshClass(player);

		if (mNextQuarry != null) {
			player.sendMessage(Component.text("You have exited the range of " + mNextQuarry.getName() + ".", mNextQuarry.mColor));
		}
		player.sendMessage(Component.text("Your class has been reenabled.", NamedTextColor.GRAY, TextDecoration.ITALIC));
	}

	private void updatePlayerWaiting(Player player) {
		if (mNextQuarry != null) {
			if (player.getLocation().toVector().distanceSquared(mNextQuarry.mSpawnLoc) < mNextQuarry.mInnerRadius * mNextQuarry.mInnerRadius) {
				if (!player.getScoreboardTags().contains(CLASS_DISABLE_TAG)) {
					player.addScoreboardTag(CLASS_DISABLE_TAG);
					playerEnterWaitingArea(player);
				}
			} else {
				if (player.getScoreboardTags().contains(CLASS_DISABLE_TAG)) {
					player.removeScoreboardTag(CLASS_DISABLE_TAG);
					playerExitWaitingArea(player);
				}
			}
		}
	}

	/**
	 * Checks whether a player trying to break or place a block should be cancelled due to the boss spawning soon
	 *
	 * @param player the player placing or breaking the block
	 * @param block  the block that is being modified
	 * @return true if the modification should be prevented, false if it should be allowed
	 */
	public boolean checkPreSpawnProtection(Player player, Block block) {
		if (getRemainingTime() < WARNING_15 && mNextQuarry != null
			&& block.getLocation().toVector().distanceSquared(mNextQuarry.mSpawnLoc) < mNextQuarry.mInnerRadius * mNextQuarry.mInnerRadius
			&& mWorld == player.getWorld()) {
			player.sendMessage(Component.text("The quarry will appear soon, it is best to leave the ground undisturbed.", NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 1, 0.63f);
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (checkPreSpawnProtection(event.getPlayer(), event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (checkPreSpawnProtection(event.getPlayer(), event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (getRemainingTime() < WARNING_15 && mWorld == player.getWorld()) {
			updatePlayerWaiting(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (player.getScoreboardTags().contains(CLASS_DISABLE_TAG)) {
			player.removeScoreboardTag(CLASS_DISABLE_TAG);
			playerExitWaitingArea(player);
		}
	}

	private void sendLocalPlayersInRange() {
		if (mWorld != null && mNextQuarry != null) {
			JsonArray playerIdsJson = new JsonArray();
			for (Player player : PlayerUtils.playersInRange(mNextQuarry.mSpawnLoc.toLocation(mWorld), mNextQuarry.mInnerRadius, true)) {
				playerIdsJson.add(player.getUniqueId().toString());
			}
			JsonObject data = new JsonObject();
			data.add("players", playerIdsJson);
			try {
				NetworkRelayAPI.sendBroadcastMessage(IN_RANGE_CHANNEL, data);
			} catch (Exception e) {
				MMLog.warning("[Hunts] Failed to broadcast local players in range");
			}
		}
	}

	private void receiveRemotePlayersInRange(String sourceShard, JsonObject data) {
		Set<UUID> playersInRange = new HashSet<>();
		if (data.get("players") instanceof JsonArray playersJson) {
			for (JsonElement playerElement : playersJson) {
				if (playerElement instanceof JsonPrimitive playerPrimitive && playerPrimitive.isString()) {
					playersInRange.add(UUID.fromString(playerPrimitive.getAsString()));
				}
			}
		}
		mPlayersInRange.put(sourceShard, playersInRange);
	}

	private static class PlayerGuildInfo {
		private final UUID mUUID;
		private final @Nullable String mGuild;

		private PlayerGuildInfo(UUID uuid) {
			mUUID = uuid;
			RemotePlayerData remoteData = MonumentaNetworkRelayIntegration.getRemotePlayer(uuid);
			if (remoteData != null) {
				mGuild = MonumentaNetworkRelayIntegration.remotePlayerGuild(remoteData);
			} else {
				// Should never happen; remoteData should not be null
				mGuild = null;
			}
		}
	}

	private void sendTransferRequest() {
		JsonObject transferData = new JsonObject();

		// We will modify this map to determine where to send people
		// By starting with where people are, we can minimize the total number of transfers
		int count = 0;
		Map<String, List<PlayerGuildInfo>> instanceMap = new HashMap<>();
		for (Map.Entry<String, Set<UUID>> entry : mPlayersInRange.entrySet()) {
			String shard = entry.getKey();
			for (UUID uuid : entry.getValue()) {
				instanceMap.computeIfAbsent(shard, s -> new ArrayList<>()).add(new PlayerGuildInfo(uuid));
				count++;
			}
		}

		int totalInstances = 1 + count / 10;
		int maxPlayersPer = (int) Math.ceil(((double) count) / totalInstances);

		// Players who don't fit into any existing shards
		List<PlayerGuildInfo> pool = new ArrayList<>();

		// Move players from overpopulated shards into the pool
		for (List<PlayerGuildInfo> shardPlayers : instanceMap.values()) {
			if (shardPlayers.size() >= maxPlayersPer) {
				// This shard has too many players!
				// Find the largest guild that can't fit and put them in the pool
				int lockedPlayers = 0;
				Map<String, List<PlayerGuildInfo>> guildMap = sortGuildList(shardPlayers);

				// Biggest guilds first
				List<Map.Entry<String, List<PlayerGuildInfo>>> sortedGuilds = guildMap.entrySet().stream()
					.sorted(Comparator.comparingInt((Map.Entry<String, List<PlayerGuildInfo>> entry) -> entry.getKey().isEmpty() ? -1 : entry.getValue().size()).reversed())
					.toList();

				for (Map.Entry<String, List<PlayerGuildInfo>> entry : sortedGuilds) {
					List<PlayerGuildInfo> guildPlayers = entry.getValue();
					if (lockedPlayers + guildPlayers.size() > maxPlayersPer) {
						if (entry.getKey().isEmpty()) {
							Collections.shuffle(guildPlayers);
							// Lock in individual players
							while (lockedPlayers <= maxPlayersPer) {
								if (guildPlayers.isEmpty()) {
									break;
								}
								guildPlayers.remove(0);
								lockedPlayers++;
							}
						}

						// This guild (or remaining individual players) can't fit; put them in the pool
						shardPlayers.removeAll(guildPlayers);
						pool.addAll(guildPlayers);
					} else {
						lockedPlayers += guildPlayers.size();
					}
				}
			}
		}

		// Move players from underpopulated shards into the pool
		while (instanceMap.size() > totalInstances) {
			Map.Entry<String, List<PlayerGuildInfo>> lowestShard = instanceMap.entrySet().stream()
				.min(Comparator.comparingInt((Map.Entry<String, List<PlayerGuildInfo>> entry) -> entry.getValue().size()))
				.orElse(null);
			if (lowestShard == null) {
				// Should never happen
				break;
			}
			// Forget about this shard and put them in the pool
			List<PlayerGuildInfo> removed = instanceMap.remove(lowestShard.getKey());
			pool.addAll(removed);
		}

		// If we don't have enough instances, add the healthiest ones
		if (instanceMap.size() < totalInstances) {
			Set<String> possibleShards = getRingShards();
			possibleShards.removeAll(instanceMap.keySet());
			Comparator<String> comparator = Comparator.comparingDouble(shard -> MonumentaNetworkRelayIntegration.remoteShardHealth(shard).healthScore());
			List<String> sortedShards = possibleShards.stream()
				.sorted(comparator.reversed())
				.toList();
			for (int i = 0; i < totalInstances - instanceMap.size(); i++) {
				instanceMap.put(sortedShards.get(i), new ArrayList<>());
			}
		}

		//Now we can finally add people from the pool back in
		Map<String, List<PlayerGuildInfo>> guildMap = sortGuildList(pool);
		List<PlayerGuildInfo> noGuildPlayers = guildMap.remove("");
		if (noGuildPlayers == null) {
			noGuildPlayers = new ArrayList<>();
		}

		while (!guildMap.isEmpty()) {
			Map.Entry<String, List<PlayerGuildInfo>> guildEntry = guildMap.entrySet().stream()
				.max(Comparator.comparingInt(entry -> entry.getValue().size()))
				.get(); // Not empty, must be some maximum

			String guild = guildEntry.getKey();
			List<PlayerGuildInfo> members = guildEntry.getValue();

			List<Map.Entry<String, List<PlayerGuildInfo>>> availableShardEntries = instanceMap.entrySet().stream()
				.filter(entry -> entry.getValue().size() + members.size() <= maxPlayersPer)
				.toList();

			if (availableShardEntries.isEmpty()) {
				// We can't fit everyone, need to split up
				Map.Entry<String, List<PlayerGuildInfo>> leastPopulatedShardEntry = instanceMap.entrySet().stream()
					.min(Comparator.comparingInt(entry -> entry.getValue().size()))
					.orElse(null);
				if (leastPopulatedShardEntry == null) {
					// Should never happen, give up
					MMLog.warning("[Hunts] Error in shard sorting algorithm - no shards available for guild sorting!");
					break;
				}
				List<PlayerGuildInfo> shardPlayers = leastPopulatedShardEntry.getValue();
				Collections.shuffle(members);
				for (int i = 0; i < maxPlayersPer - shardPlayers.size(); i++) {
					shardPlayers.add(members.remove(i)); // members size decreases, making progress
				}
			} else {
				Map.Entry<String, List<PlayerGuildInfo>> shardEntry = FastUtils.getRandomElement(availableShardEntries);
				List<PlayerGuildInfo> shardPlayers = shardEntry.getValue();
				shardPlayers.addAll(members);
				guildMap.remove(guild);
			}
		}

		Collections.shuffle(noGuildPlayers);
		for (PlayerGuildInfo playerId : noGuildPlayers) {
			Map.Entry<String, List<PlayerGuildInfo>> leastPopulatedShardEntry = instanceMap.entrySet().stream()
				.min(Comparator.comparingInt(entry -> entry.getValue().size()))
				.orElse(null);
			if (leastPopulatedShardEntry == null) {
				// Should never happen, give up
				MMLog.warning("[Hunts] Error in shard sorting algorithm - no shards available for guildless player!");
				break;
			}

			leastPopulatedShardEntry.getValue().add(playerId);
		}

		for (Map.Entry<String, List<PlayerGuildInfo>> shardPlayerEntries : instanceMap.entrySet()) {
			String targetShard = shardPlayerEntries.getKey();
			for (PlayerGuildInfo playerId : shardPlayerEntries.getValue()) {
				transferData.addProperty(playerId.mUUID.toString(), targetShard);
			}
		}

		try {
			NetworkRelayAPI.sendBroadcastMessage(TRANSFER_REQUEST_CHANNEL, transferData);
		} catch (Exception e) {
			MMLog.warning("[Hunts] Failed to broadcast transfer request");
		}
		startRandomHunt();
	}

	// Puts guildless players into the empty string
	private Map<String, List<PlayerGuildInfo>> sortGuildList(List<PlayerGuildInfo> guildList) {
		Map<String, List<PlayerGuildInfo>> map = new HashMap<>();
		for (PlayerGuildInfo player : guildList) {
			map.computeIfAbsent(player.mGuild == null ? "" : player.mGuild, g -> new ArrayList<>()).add(player);
		}
		return map;
	}

	private void receiveTransferRequest(JsonObject data) {
		String thisShard = NetworkRelayAPI.getShardName();
		for (Player player : Bukkit.getOnlinePlayers()) {
			String playerId = player.getUniqueId().toString();
			if (data.get(playerId) instanceof JsonPrimitive targetPrimitive && targetPrimitive.isString()) {
				String targetShard = targetPrimitive.getAsString();

				if (thisShard.equals(targetShard)) {
					// The player is already here
					continue;
				}

				try {
					MonumentaRedisSyncAPI.sendPlayer(player, targetShard);
				} catch (Exception e) {
					MMLog.severe("[Hunts] Failed to sort players to shard " + targetShard);
				}
			}
		}

		if (mSpawningBoss) {
			return;
		}

		QuarryType quarry = mNextQuarry;
		if (quarry == null) {
			return;
		}

		// If this shard is one of the target shards, summon the boss locally after a delay
		if (data.asMap().values().stream().anyMatch(e -> {
			if (!(e instanceof JsonPrimitive targetPrimitive && targetPrimitive.isString())) {
				return false;
			}
			return thisShard.equals(targetPrimitive.getAsString());
		})) {
			// Found a match, summon the boss; just make sure to
			// give players time to switch their instance before spawning
			if (mWorld == null) {
				// No quarry to spawn?
				return;
			}
			mSpawningBoss = true;
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				quarry.summon(mWorld);
				mSpawningBoss = false;
			}, 15 * 20);
		} else if (thisShard.equals("ring")) {
			// Not a match - but this is the shard responsible for updating the tab list!
			BroadcastedEvents.clearEvent(quarry.name());
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void networkRelayMessageEvent(@NotNull NetworkRelayMessageEvent event) {
		JsonObject data = event.getData();
		switch (event.getChannel()) {
			case IN_RANGE_CHANNEL -> {
				String source = event.getSource();
				Plugin.getInstance().mHuntsManager.receiveRemotePlayersInRange(source, data);
			}
			case TRANSFER_REQUEST_CHANNEL -> Plugin.getInstance().mHuntsManager.receiveTransferRequest(data);
			default -> {
			}
		}
	}
}
