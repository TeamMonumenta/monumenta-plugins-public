package com.playmonumenta.plugins.minigames.pzero;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.event.entity.EntityDismountEvent;

public class PzeroManager implements Listener {
	public static final String PZERO_PIG_TAG = "PZeroPig";
	public static final int COUNTDOWN_SECONDS = 20;
	public static final String ICE_DIAMOND_LOOT_TABLE = "epic:event/winter2018/ice_diamond";
	public static final String WINTER_ESSENCE_LOOT_TABLE = "epic:event/winter2019/essence_of_winter";
	public static final int WINTER_ESSENCE_COST = 1;
	public static final int BASE_REWARD_AMOUNT = 24;
	public static final double REWARD_RATIO = 1.25;
	public static final int MAX_PLAYER_COUNT = 8;
	public static final String FINISH_LEADERBOARD = "pzerofinishes";
	public static final int WIN_ADVANCEMENT_MIN_PLAYERS = 4;

	private final HashMap<UUID, PzeroPlayer> mTrackedPlayers = new HashMap<>();
	private final List<PzeroMap> mMaps = List.of(PzeroMap.values());

	public void update(boolean oneHertz, boolean twoHertz, boolean fourHertz) {
		// Send the action bar message regarding energy, and other info.
		if (twoHertz) {
			mTrackedPlayers.values().stream().filter(pzPlayer -> getMap(pzPlayer.getMapName()).isRunning())
				.forEach(pzPlayer -> {
						int position = getMap(pzPlayer.getMapName()).getCurrentPlayerPlacement(pzPlayer.getPlayer());
						String placementColor = PzeroPlayer.getPlacementColor(position).asHexString();
						String placementString = "<" + placementColor + ">" + (position == 0 ? "..." : StringUtils.intToOrdinal(position)) + "</" + placementColor + ">";
						pzPlayer.getPlayer().sendActionBar(
							MessagingUtils.fromMiniMessage("<bold>【[<gold>Lap " + (pzPlayer.getCurrentLap() + 1) + "/" + pzPlayer.getLapCount() + "<white>] [" + placementString + "]<gradient:#CF1533:#F94B18:#FAA426:#FFF03E:#63CB5F:#1B82AA:#0339C4> " + "|".repeat(pzPlayer.getCurrentEnergy()) + ".".repeat(PzeroPlayer.MAX_ENERGY - pzPlayer.getCurrentEnergy()) + " <white>[" + (pzPlayer.isBoosting() ? "<gold>" : "<dark_gray>") + "⚡<white>]】")
						);
					}
				);
		}

		// Anti Cheat: can't hold boost rod in offhand as it wouldn't count as an interactable,
		// and instead allows using the vanilla pig speed boost mechanic.
		mTrackedPlayers.values().forEach(pzPlayer -> {
			if (pzPlayer.moveRodFromOffhandToMainhand()) {
				pzPlayer.getPlayer().updateInventory();
			}
		});

		ArrayList<PzeroPlayerPlacement> placements = new ArrayList<>();
		mTrackedPlayers.values().stream().filter(pzPlayer -> getMap(pzPlayer.getMapName()).isRunning()).forEach(pzPlayer -> {
			pzPlayer.incrementTimer();
			pzPlayer.doPeriodicEffects();
			if (!pzPlayer.isInWinAnimation()) {
				placements.add(handleCheckpoints(pzPlayer));
			}
			handleCollisions(pzPlayer);
		});

		handleLeaderboard(placements);
	}

	private void handleLeaderboard(ArrayList<PzeroPlayerPlacement> placements) {
		// Leaderboard logic

		// Remove placements which already have a value set for placement
		// (those mark the placements of players who won and lost)
		placements.removeIf(placement -> placement.mPlacement != 0);

		mMaps.forEach(map -> {
			if (map.getName().equals(PzeroMap.MAP_NULL.getName()) || countPlayersInMap(map.getName()) == 0) {
				return;
			}

			// Remove the placements that are not for the currently examined map
			ArrayList<PzeroPlayerPlacement> placementCopy = new ArrayList<>(placements);
			ArrayList<PzeroPlayerPlacement> mapPlacements = map.getPlacements();
			placementCopy.removeIf(placement -> {
				PzeroPlayer pzPlayer = getPzeroPlayer(placement.mPlayer);
				if (pzPlayer == null) {
					return true;
				}

				if (!pzPlayer.getMapName().equals(map.getName())) {
					return true;
				}

				// Remove players who already have a static placement, but are still in game (case of during the losing animation)
				return mapPlacements.stream().anyMatch(mapPlacement -> mapPlacement.mPlayer.getUniqueId().equals(placement.mPlayer.getUniqueId()));
			});

			// Sort the remaining placements: those of the players still racing
			placementCopy.sort(null);

			int currentPlace = map.getNextAvailableTopPosition();
			for (PzeroPlayerPlacement placement : placementCopy) {
				placement.mPlacement = currentPlace;
				currentPlace++;
			}

			// Merge the ongoing placements with the solidified ones, and sort by the placement number
			ArrayList<PzeroPlayerPlacement> merged = new ArrayList<>();
			merged.addAll(placementCopy);
			merged.addAll(mapPlacements);
			merged.sort(Comparator.comparingInt(placement -> placement.mPlacement));

			// Display the placements on the scoreboard. The placement number is handled as a relative number,
			// thanks to the sort above, so that if players leave the game, their positions should remain/be updated.
			int scoreboardCurrentPlace = 1;
			for (PzeroPlayerPlacement placement : merged) {
				map.setCurrentPlayerPlacement(placement.mPlayer, scoreboardCurrentPlace);
				if (placement.mHasCrossedFinishLine) {
					awardEnergyForCrossingFinishLine(placement);
				}
				scoreboardCurrentPlace++;
			}
		});
	}

	private void awardEnergyForCrossingFinishLine(PzeroPlayerPlacement placement) {
		PzeroPlayer pzPlayer = getPzeroPlayer(placement.mPlayer);
		if (pzPlayer == null) {
			return;
		}
		pzPlayer.restoreEnergy((placement.mPlacement - 1) * 2, false);
	}

	private PzeroPlayerPlacement handleCheckpoints(PzeroPlayer pzPlayer) {
		// Checkpoints and leaderboard logic
		int currentCheckpoint = pzPlayer.getCurrentCheckpoint();
		PzeroCheckpoint nextCheckpoint = getMap(pzPlayer.getMapName()).getNextCheckpoint(currentCheckpoint);
		boolean hasCrossedFinishLine = false;

		if (nextCheckpoint.isPlayerInside(pzPlayer)) {
			if (nextCheckpoint.getId() == 0) {
				int nextLap = pzPlayer.getCurrentLap() + 1;
				pzPlayer.updateLap(nextLap);
				hasCrossedFinishLine = true;

				if (nextLap == pzPlayer.getLapCount() && !pzPlayer.isPorksploding()) {
					return win(pzPlayer);
				}
			}
			pzPlayer.setCurrentCheckpoint(nextCheckpoint.getId());
			pzPlayer.resetCheckpointTimer();
		}

		return new PzeroPlayerPlacement(pzPlayer.getPlayer(), pzPlayer.getCurrentLap(), nextCheckpoint.getId(),
			nextCheckpoint.distanceSquaredFromCenter(pzPlayer), hasCrossedFinishLine);
	}

	private void handleCollisions(PzeroPlayer pzPlayer) {
		// Collision physics with other pigs, and blocks
		Pig pig = pzPlayer.getPig();
		if (pig == null) {
			return;
		}

		// Collision with blocks
		double rayLength = pzPlayer.isBoosting() ? PzeroPlayer.PIG_BOOSTED_SPEED : PzeroPlayer.PIG_BASE_SPEED;
		LocationUtils.rayTraceToBlock(pig.getEyeLocation(), pig.getEyeLocation().getDirection().setY(0), rayLength, (loc) -> {
			pzPlayer.depleteEnergy(1);
			pig.setVelocity(LocationUtils.getDirectionTo(pig.getLocation(), loc));
		});

		// Collision with other players
		new Hitbox.AABBHitbox(pig.getWorld(), pig.getBoundingBox().expand(new Vector(0.25, 1, 0.25))).getHitPlayers(pzPlayer.getPlayer(), true)
			.forEach(hitPlayer -> {
				PzeroPlayer hitPzPlayer = mTrackedPlayers.get(hitPlayer.getUniqueId());
				if (hitPzPlayer == null) {
					return;
				}

				Pig hitPig = hitPzPlayer.getPig();
				// Also prevent collisions with launched players to not throw off the launch trajectory.
				if (hitPig == null || hitPzPlayer.isBeingLaunched() || hitPzPlayer.isInGracePeriod() || hitPzPlayer.isInWinAnimation()
					|| pzPlayer.isBeingLaunched() || pzPlayer.isInGracePeriod() || pzPlayer.isInWinAnimation()) {
					return;
				}

				Vector velocity = LocationUtils.getDirectionTo(pig.getLocation(), hitPig.getLocation()).setY(0);
				pig.setVelocity(velocity);
				hitPig.setVelocity(velocity.multiply(-1));
				hitPzPlayer.setLastHitBy(pzPlayer);
				if (pzPlayer.isBoosting()) {
					hitPzPlayer.depleteEnergy(1);
				}
			});
	}

	public void boost(Player player) {
		PzeroPlayer pzPlayer = getPzeroPlayer(player);
		if (pzPlayer == null) {
			return;
		}

		pzPlayer.tryBoost();
	}

	public void restoreEnergy(Player player, int amount, boolean fromPit) {
		PzeroPlayer pzPlayer = getPzeroPlayer(player);
		if (pzPlayer == null) {
			return;
		}

		pzPlayer.restoreEnergy(amount, fromPit);
	}

	public void giveBoostRod(Player player) {
		PzeroPlayer pzPlayer = getPzeroPlayer(player);
		if (pzPlayer == null) {
			return;
		}

		pzPlayer.giveBoostRod();
	}

	public void removeBoostRod(Player player) {
		PzeroPlayer pzPlayer = getPzeroPlayer(player);
		if (pzPlayer == null) {
			return;
		}

		pzPlayer.removeBoostRod();
	}

	public void launch(Player player, double x, double y, double z, int delay, int gracePeriodTicks) {
		PzeroPlayer pzPlayer = getPzeroPlayer(player);
		if (pzPlayer == null) {
			return;
		}

		pzPlayer.launch(x, y, z, delay, gracePeriodTicks);
	}

	private boolean checkAndRemoveWinterEssence(Player player) {
		ItemStack winterEssence = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(WINTER_ESSENCE_LOOT_TABLE));
		if (winterEssence == null) {
			return false;
		}

		winterEssence.setAmount(WINTER_ESSENCE_COST);
		if (player.getInventory().containsAtLeast(winterEssence, 1)) {
			player.getInventory().removeItem(winterEssence);
			return true;
		}
		return false;
	}

	public @Nullable String signUp(Player player, String mapName) {
		boolean alreadyTracked = mTrackedPlayers.values().stream().anyMatch(pzPlayer -> pzPlayer.getUniqueId().equals(player.getUniqueId()));
		if (alreadyTracked) {
			return null;
		}

		PzeroMap map = getMap(mapName);
		if (map.isRunning() || map.getName().equals(PzeroMap.MAP_NULL.getName())) {
			return "A race is already in progress.";
		}

		if (countPlayersInMap(map.getName()) >= MAX_PLAYER_COUNT) {
			return "This race has reached maximum player count.";
		}

		if (map.isWaiting() && !checkAndRemoveWinterEssence(player)) {
			return "You must have an Essence of Winter in your inventory to start the race.";
		}

		player.teleport(map.mSpawnPosition.toLocation(player.getWorld()));
		PzeroPlayer pzPlayer = new PzeroPlayer(player, mapName, map.mLapCount);
		mTrackedPlayers.put(player.getUniqueId(), pzPlayer);
		pzPlayer.removeBoostRod();
		pzPlayer.giveBoostRod();
		// Reset the map and start a countdown if the map was in the WAITING state.
		if (map.isWaiting()) {
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () ->
				NmsUtils.getVersionAdapter().runConsoleCommandSilently("function " + map.mResetFunctionName)
			);
			doMapCountdown(map, mapName);
			map.clearCurrentPlayerPlacements();
		}

		return null;
	}

	public void leave(Player player) {
		PzeroPlayer pzPlayer = mTrackedPlayers.remove(player.getUniqueId());
		if (pzPlayer == null) {
			return;
		}

		pzPlayer.removeBoostRod();
		if (countPlayersInMap(pzPlayer.getMapName()) == 0) {
			resetMap(getMap(pzPlayer.getMapName()), pzPlayer.getPlayer().getWorld());
		}

		@Nullable Pig pig = pzPlayer.getPig();
		if (pig != null && pig.isValid()) {
			pig.remove();
		}
	}

	public void returnPlayerToSpawn(Player player) {
		PzeroPlayer pzPlayer = mTrackedPlayers.get(player.getUniqueId());
		if (pzPlayer != null) {
			returnPlayerToSpawn(pzPlayer);
		}
	}

	public void returnPlayerToSpawn(PzeroPlayer pzPlayer) {
		pzPlayer.getPlayer().teleport(getMap(pzPlayer.getMapName()).mReturnPosition.toLocation(pzPlayer.getPlayer().getWorld()));
	}

	public PzeroPlayerPlacement win(PzeroPlayer pzPlayer) {
		PzeroMap map = getMap(pzPlayer.getMapName());
		PzeroPlayerPlacement placement = new PzeroPlayerPlacement(pzPlayer.getPlayer(), map.getNextAvailableTopPosition());
		placement.mHasFinished = true;
		placement.mFinalTimerTicks = pzPlayer.getTimer();
		map.addPlacement(placement);
		map.registerPlayerTime(pzPlayer);
		pzPlayer.showEndingInfo(placement, true, false);
		pzPlayer.doPorkscension(map);
		// Increment player's finish scoreboard score
		int currentFinishes = ScoreboardUtils.getScoreboardValue(pzPlayer.getPlayer(), FINISH_LEADERBOARD).orElse(0);
		ScoreboardUtils.setScoreboardValue(pzPlayer.getPlayer(), FINISH_LEADERBOARD, currentFinishes + 1);
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "leaderboard update " + pzPlayer.getPlayer().getName() + " " + FINISH_LEADERBOARD);
		// Win advancement 1st place and race had at least 4 players
		if (placement.mPlacement == 1 && countPlayersInMap(map.getName()) >= WIN_ADVANCEMENT_MIN_PLAYERS) {
			AdvancementUtils.grantAdvancement(pzPlayer.getPlayer(), "monumenta:trophies/events/2023/winter");
		}

		return placement;
	}

	public void lose(PzeroPlayer pzPlayer, boolean eliminated, boolean afkKicked) {
		// Leave is handled by the porksplosion function, and is called after the animation.
		// However, this is called as soon as the animation starts, to solidify the placement.
		PzeroMap map = getMap(pzPlayer.getMapName());
		int bottomPos = map.getNextAvailableBottomPosition();
		PzeroPlayerPlacement placement = new PzeroPlayerPlacement(pzPlayer.getPlayer(), bottomPos);
		map.addPlacement(placement);
		pzPlayer.showEndingInfo(placement, false, eliminated);
	}

	public void sendToSpectatorArea(PzeroPlayer pzPlayer, PzeroMap map) {
		// Called at the end of Crash Out / Finish animations, as well as when a player
		// leaves the game, if they hadn't Crashed Out or Finished yet.
		pzPlayer.getPlayer().teleport(map.mSpectatePosition.toLocation(pzPlayer.getPlayer().getWorld()));
		leave(pzPlayer.getPlayer());
	}

	private void resetMap(PzeroMap map, World world) {
		map.displayStandingsToNearbyPlayers(world, 200);
		giveRewards(map, map.getPlacements());
		map.setWaiting();
		map.clearPlacements();
		map.setPlayerCountAtStart(0);
	}

	private void giveRewards(PzeroMap map, List<PzeroPlayerPlacement> finalPlacements) {
		int nonAfkParticipants = 0;
		// Check that at least one player in the race has finished.
		// Otherwise, don't assign rewards to anyone.
		if (finalPlacements.stream().anyMatch(placement -> placement.mHasFinished)) {
			for (PzeroPlayerPlacement placement : finalPlacements) {
				if (!placement.mAfkKicked) {
					nonAfkParticipants++;
				}
			}
		}
		int finalNonAfkParticipants = nonAfkParticipants;
		int totalPrize = finalNonAfkParticipants == 0 ? 0 : BASE_REWARD_AMOUNT * finalNonAfkParticipants + (5 * Math.max(0, finalNonAfkParticipants - 1));

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			for (PzeroPlayerPlacement placement : finalPlacements) {
				// Teleport the players back to spawn first,
				placement.mPlayer.teleport(map.mReturnPosition.toLocation(placement.mPlayer.getWorld()));
				leave(placement.mPlayer);
				if (!placement.mAfkKicked) {
					// then give rewards after a delay, so that they can't drop them in the spectating area due to full inventory.
					int rewardPortion = totalPrize == 0 ? 0 : (int) (((Math.pow(REWARD_RATIO, finalNonAfkParticipants - placement.mPlacement) * (REWARD_RATIO - 1)) / (Math.pow(REWARD_RATIO, finalNonAfkParticipants) - 1)) * totalPrize);
					for (int i = 0; i < rewardPortion; i++) {
						InventoryUtils.giveItemFromLootTable(placement.mPlayer, NamespacedKeyUtils.fromString(ICE_DIAMOND_LOOT_TABLE), 1);
					}
				}
			}
		}, 20);
	}

	private int countPlayersInMap(String mapName) {
		return (int) mTrackedPlayers.values().stream().filter(pzPlayer -> pzPlayer.getMapName().equals(mapName)).count();
	}

	private void doMapCountdown(PzeroMap map, String mapName) {
		map.setStarting();

		new BukkitRunnable() {
			final String mMapName = mapName;

			int mRuns = COUNTDOWN_SECONDS;

			@Override
			public void run() {
				getPlayersInMap(mMapName)
					.forEach(pzPlayer -> MessagingUtils.sendActionBarMessage(pzPlayer.getPlayer(), "The race is starting in " + mRuns + "s!", NamedTextColor.YELLOW));

				// If all players leave before the countdown is done, set the state back to WAITING.
				if (countPlayersInMap(mMapName) == 0) {
					map.setWaiting();
					cancel();
					return;
				}
				if (mRuns > 0 && mRuns <= 3) {
					getPlayersInMap(mMapName)
						.forEach(pzPlayer -> {
							pzPlayer.getPlayer().showTitle(Title.title(
									Component.text("Ready" + ".".repeat(4 - mRuns), NamedTextColor.YELLOW, TextDecoration.BOLD),
									Component.empty(),
									Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)
								)
							);
							pzPlayer.getPlayer().playSound(pzPlayer.getPlayer(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 3, 1);
						});
				}

				if (mRuns <= 0) {
					cancel();
					getPlayersInMap(mMapName)
						.forEach(pzPlayer -> {
							Player player = pzPlayer.getPlayer();
							// GO!! Flashing Green and White, alternating
							player.showTitle(Title.title(
									Component.text("GO!!", NamedTextColor.GREEN, TextDecoration.BOLD),
									Component.empty(),
									Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
								)
							);
							Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
								player.showTitle(Title.title(
										Component.text("GO!!", NamedTextColor.WHITE, TextDecoration.BOLD),
										Component.empty(),
										Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
									)
								);
							}, 2);
							Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
								player.showTitle(Title.title(
										Component.text("GO!!", NamedTextColor.GREEN, TextDecoration.BOLD),
										Component.empty(),
										Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
									)
								);
							}, 4);
							Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
								player.showTitle(Title.title(
										Component.text("GO!!", NamedTextColor.WHITE, TextDecoration.BOLD),
										Component.empty(),
										Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
									)
								);
							}, 6);
							Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
								player.showTitle(Title.title(
										Component.text("GO!!", NamedTextColor.GREEN, TextDecoration.BOLD),
										Component.empty(),
										Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)
									)
								);
							}, 8);
							// Sounds
							pzPlayer.getPlayer().playSound(pzPlayer.getPlayer(), Sound.ENTITY_PIG_AMBIENT, SoundCategory.PLAYERS, 2, 1);
							Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> pzPlayer.getPlayer().playSound(pzPlayer.getPlayer(), Sound.ENTITY_PIG_AMBIENT, SoundCategory.PLAYERS, 2, 1.25f), 4);
							Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> pzPlayer.getPlayer().playSound(pzPlayer.getPlayer(), Sound.ENTITY_PIG_AMBIENT, SoundCategory.PLAYERS, 2, 1.5f), 8);
						});

					map.setRunning();
					map.setPlayerCountAtStart(countPlayersInMap(mMapName));
					NmsUtils.getVersionAdapter().runConsoleCommandSilently("function " + map.mStartFunctionName);
				}
				mRuns--;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 20);
	}

	public PzeroMap getMap(String mapName) {
		Optional<PzeroMap> map = mMaps.stream().filter(pzMap -> pzMap.mName.equals(mapName)).findFirst();

		return map.orElse(PzeroMap.MAP_NULL);
	}

	public List<PzeroPlayer> getPlayersInMap(String mapName) {
		return mTrackedPlayers.values().stream().filter(pzPlayer -> pzPlayer.getMapName().equals(mapName)).toList();
	}

	private @Nullable PzeroPlayer getPzeroPlayer(Player player) {
		List<PzeroPlayer> playerList = mTrackedPlayers.values().stream().filter(pzPlayer -> pzPlayer.getUniqueId().equals(player.getUniqueId())).toList();
		if (playerList.size() == 0) {
			return null;
		}

		return playerList.get(0);
	}

	// Prevent dismounting from the pig
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onEntityDismount(EntityDismountEvent event) {
		if (
			event.getEntity() instanceof Player player &&
			mTrackedPlayers.containsKey(player.getUniqueId()) &&
			event.getDismounted().getScoreboardTags().contains(PZERO_PIG_TAG)
		) {
			event.setCancelled(true);
			// Car honk sound!
			mTrackedPlayers.get(player.getUniqueId()).tryHonk();
		}
	}

	// Remove the player from tracked players if they log out
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		returnPlayerToSpawn(event.getPlayer());
		leave(event.getPlayer());
	}
}
