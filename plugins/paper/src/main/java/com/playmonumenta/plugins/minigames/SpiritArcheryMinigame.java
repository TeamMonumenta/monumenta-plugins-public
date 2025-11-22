package com.playmonumenta.plugins.minigames;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.SpiritArcheryTargetBoss;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.commands.Leaderboard;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpiritArcheryMinigame extends Minigame {
	public static final String ID = "SpiritArchery";
	public static final String metadataTag = "spiritarcheryscore";
	public static final String scoreboard = "SpiritMinigame001";
	public static final String scoreboardHighScore = "SpiritMinigame001MaxScore";
	public static final int[] scorePerTicketByDifficulty = {4, 2, 2, 2};
	private static final String targetName = "SpiritArcheryTargetDrowned";
	private static final NamespacedKey TICKET_KEY = NamespacedKeyUtils.fromString("epic:r1/items/currency/circus_ticket");
	private static final NamespacedKey BOW_KEY = NamespacedKeyUtils.fromString("epic:r1/items/keys/freedom");
	private static final List<Map<Integer, Integer>> MOB_COUNT_BY_SCORE = List.of(
		Map.of(
			1, 3
		), // Difficulty 0 is Easy
		Map.of(
			1, 3,
			-1, 3
		), // Difficulty 1 is Medium
		Map.of(
			1, 2,
			3, 1,
			-1, 6
		), // Difficulty 2 is Hard
		Map.of(
			1, 1,
			3, 2,
			-1, 3,
			-5, 5
		) // Difficulty 3 is Expert
	);
	private static final int MIN_SPACING = 2;
	private static final int[] scoreToUnlockBowUpgrade = {20, 35, 50, 75};

	private final Location mLoc1;
	private final Location mLoc2;
	private final Plugin mPlugin = Plugin.getInstance();
	private final Map<Integer, Integer> mScoreToCount;
	private final int mTicksBetweenRefreshes;
	private final int mDifficulty;
	private final int mTimeLimit;
	private final int mMobsToKillPerRound;
	private final BossBar mBossBar;

	public int score;
	private long mTicks = 0;
	private int mMobsToKill;
	@Nullable
	private Player mPlayer;
	private long mLastRefreshedTick;

	public SpiritArcheryMinigame(Location loc1, Arguments arguments) {
		this(loc1, loc1.clone().add(getOffset(arguments)), arguments);
	}

	public SpiritArcheryMinigame(Location loc1, Location loc2, Arguments arguments) {
		super(ID, loc1, loc2);
		mLoc1 = loc1;
		mLoc2 = loc2;

		mDifficulty = arguments.getInt("difficulty", 0);
		mScoreToCount = MOB_COUNT_BY_SCORE.get(mDifficulty);
		mTimeLimit = arguments.getInt("time", 60) * Constants.TICKS_PER_SECOND;
		mMobsToKillPerRound = arguments.getInt("mobs_to_kill", () -> mScoreToCount.entrySet().stream()
			.filter(entry -> entry.getKey() > 0)
			.map(Map.Entry::getValue)
			.reduce(0, Integer::sum, Integer::sum));
		mTicksBetweenRefreshes = arguments.getInt("ticks_between_refreshes", mTimeLimit / 6);
		mBossBar = BossBar.bossBar(Component.text("Windstorm of Souls").color(NamedTextColor.WHITE), 1, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
	}

	private static Vector getOffset(Arguments arguments) {
		return new Vector(
			arguments.get("x", 9),
			arguments.get("y", 9),
			arguments.get("z", 9)
		);
	}

	private static char convertToChar(int inputScore) {
		int absScore = Math.abs(inputScore);
		if (absScore > 26) {
			return (char) 65;
			// Returns "A"
		}
		if (inputScore != 0) {
			return (char) (absScore + 64);
			// Returns letters "A" through "Z"
		}
		return 'Z';
	}

	private static Location getSpawnLoc(Location loc1, Location loc2, List<Location> targetLocs) {
		World world = loc1.getWorld();
		Location spawnLoc = loc1;
		// if you want larger targets, change these bounds.
		Location loc2Adjusted = loc2.clone().subtract(1, 2, 1);

		for (int safety = 0; safety < 100; safety++) {
			double xOffset = (loc2Adjusted.getX() - loc1.getX()) * FastUtils.randomDoubleInRange(0, 1);
			double yOffset = (loc2Adjusted.getY() - loc1.getY()) * FastUtils.randomDoubleInRange(0, 1);
			double zOffset = (loc2Adjusted.getZ() - loc1.getZ()) * FastUtils.randomDoubleInRange(0, 1);
			Predicate<Location> predicate = loc -> world.getBlockAt(loc).isPassable() &&
				loc.getX() < loc2Adjusted.getX() && loc.getX() > loc1.getX() &&
				loc.getY() < loc2Adjusted.getY() && loc.getY() > loc1.getY() &&
				loc.getZ() < loc2Adjusted.getZ() && loc.getZ() > loc1.getZ() &&
				targetLocs.stream().allMatch(otherLoc -> otherLoc.distanceSquared(loc) >= MIN_SPACING * MIN_SPACING);

			spawnLoc = LocationUtils.getNearbySafeLocation(loc1.clone().add(new Vector(xOffset, yOffset, zOffset)), predicate);
			if (predicate.test(spawnLoc)) {
				return spawnLoc;
			}
		}
		return spawnLoc;
	}

	@Override
	public void startMinigame(@Nullable Player player) {
		// Target the player
		mPlayer = player;
		if (mPlayer == null) {
			return;
		}
		// Initialise
		mMobsToKill = 0;
		mTicks = 0;
		mHitbox = new Hitbox.AABBHitbox(mLoc1.getWorld(), BoundingBox.of(mLoc1, mLoc2));

		PlayerUtils.playerTeleport(mPlayer, new Location(mPlayer.getWorld(), 268.5, 133.0, -143.5));
		InventoryUtils.giveItemFromLootTable(mPlayer, BOW_KEY, 1);

		mPlayer.showBossBar(mBossBar);
		mPlayer.sendMessage(Component.text("The souls of the damned burst forth...").color(NamedTextColor.GRAY));
	}

	@Override
	void tick(long tick) {
		mTicks = tick;
		if (tick >= mTimeLimit || mPlayer == null || !mPlayer.isOnline()) {
			minigameEnd();
			return;
		}
		// Functionality
		if (((tick - mLastRefreshedTick) + 20) % mTicksBetweenRefreshes == 0) {
			mPlayer.sendMessage(Component.text("The souls waver, and prepare to shift...").color(NamedTextColor.GRAY));
			MessagingUtils.sendTitle(mPlayer, Component.text(""), Component.text("Rearranging...").color(NamedTextColor.GRAY), 0, 15, 5);
		}
		// Check if sufficient targets have been hit / sufficient time has passed.
		if (mMobsToKill == 0 || (tick - mLastRefreshedTick) % mTicksBetweenRefreshes == 0) {
			mMobsToKill = mMobsToKillPerRound;
			spawnTargets(mLoc1, mLoc2, mScoreToCount);
		}
		if (tick % Constants.TICKS_PER_SECOND == 0) {
			mBossBar.progress(1 - (float) tick / mTimeLimit);
			if ((mTimeLimit - tick) <= (5 * Constants.TICKS_PER_SECOND)) {
				MessagingUtils.sendTitle(mPlayer, Component.text(""), Component.text((int) Math.ceil((mTimeLimit - tick) / 20.0) + "s left!").color(NamedTextColor.GOLD), 0, 10, 10);
			}
		}
	}

	private void spawnTargets(Location loc1, Location loc2, Map<Integer, Integer> scoreCountMap) {
		clearTargets();
		List<Location> targetLocs = new ArrayList<>();
		mLastRefreshedTick = mTicks;
		for (Map.Entry<Integer, Integer> entry : scoreCountMap.entrySet()) {
			int scorePerMob = entry.getKey();
			int mobCount = entry.getValue();
			for (int i = 0; i < mobCount; i++) {
				Location spawnLoc = getSpawnLoc(loc1, loc2, targetLocs);

				String soulName = targetName + ((scorePerMob >= 0) ? "Plus" : "Minus") + convertToChar(scorePerMob);
				@Nullable
				Entity targetEntity = LibraryOfSoulsIntegration.summon(spawnLoc, soulName);
				if (targetEntity == null) {
					MMLog.warning(String.format("Spirit Archery Minigame failed to summon %s at %s!", soulName, spawnLoc));
					continue;
				}

				targetEntity.setMetadata(metadataTag, new FixedMetadataValue(mPlugin, scorePerMob));
				targetEntity.setInvisible(false);
				targetLocs.add(targetEntity.getLocation());
			}
		}
	}

	@Override
	public void onEntityDeath(EntityDeathEvent event) {
		if (mPlayer == null) {
			return;
		}
		LivingEntity entity = event.getEntity();

		if (mHitbox.intersects(entity.getBoundingBox())) {
			// Check if the mob has the bosstag
			if (entity.getScoreboardTags().contains(SpiritArcheryTargetBoss.identityTag)) {
				List<MetadataValue> list = entity.getMetadata(metadataTag);
				if (list.size() != 1) {
					MMLog.warning(String.format("Warning: Mob %s had multiple scores: %s.", entity.getName(), list));
				} else {
					int scoreGain = list.get(0).asInt();
					// Mobs with negative scores aren't required to be killed
					if (scoreGain > 0) {
						mMobsToKill--;
					}
					score += scoreGain;
					MessagingUtils.sendActionBarMessage(mPlayer, String.format("Score: %d (%s%d)", score, scoreGain > 0 ? "+" : "", scoreGain));
				}
			} else {
				MMLog.finer("Mob other than target was killed in the firing range.");
			}
		}
	}

	@Override
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (event.getPlayer() == mPlayer && !event.isCancelled()) {
			minigameEnd();
		}
	}

	@Override
	public void onEndMinigame() {
		// Kill off any remaining targets
		clearTargets();
		// Reward function
		if (mPlayer == null) {
			return;
		}
		awardLoot(mPlayer, score, mDifficulty);
		mPlayer.sendMessage("Cleared Spirit Archery with " + score + " score!");
		attemptAdvanceScore(mPlayer, score, mDifficulty);
		// Clear lingering stuff
		mPlayer.hideBossBar(mBossBar);
		InventoryUtils.removeSpecialItems(mPlayer, true, true, false);
		score = 0;
		// This sucks. I don't want to be force-teleporting the player in this file, I think it should be done some other way, but I cannot figure out a better way. It's very very janky that the teleport-in is performed in the GUI but the teleport-out has to be done here.
		PlayerUtils.playerTeleport(mPlayer, new Location(mPlayer.getWorld(), 268.5, 133.0, -145.5));
	}

	private void clearTargets() {
		for (LivingEntity livingEntity : mHitbox.getHitMobs()) {
			if (livingEntity.getScoreboardTags().contains(SpiritArcheryTargetBoss.identityTag)) {
				livingEntity.getPassengers().forEach(Entity::remove);
				livingEntity.remove();
			}
		}
	}

	private void awardLoot(@NotNull Player player, int score, int difficulty) {
		Location lootLoc = player.getLocation();

		LootContext context = new LootContext.Builder(lootLoc).build();

		@Nullable
		LootTable ticketTable = Bukkit.getLootTable(TICKET_KEY);
		if (ticketTable == null) {
			MMLog.severe(String.format("Spirit Archery loot table %s not found!", TICKET_KEY));
			return;
		}

		Random r = new Random();
		float randomFloat = r.nextFloat();
		Collection<ItemStack> ticketLoot = ticketTable.populateLoot(FastUtils.RANDOM, context);

		final int scorePerTicket = scorePerTicketByDifficulty[difficulty];
		if (!ticketLoot.isEmpty()) {
			ItemStack materials = ticketLoot.iterator().next();
			materials.setAmount(score / scorePerTicket +
				((randomFloat * scorePerTicket > score % scorePerTicket) ? 0 : -1)
			);
			InventoryUtils.giveItemWithWarningAfterDelay(player, materials);
		}
	}

	private void attemptAdvanceScore(@NotNull Player player, int score, int difficulty) {
		if (score >= scoreToUnlockBowUpgrade[difficulty]
			&& ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0) == difficulty
		) {
			ScoreboardUtils.setScoreboardValue(player, scoreboard, difficulty + 1);
			player.sendMessage(Component.text("The spirits sigh in relief, and dissipate. In their wake, a mournful tune rings out...").color(NamedTextColor.GRAY));
			player.sendMessage(Component.text("The Spiritsinger may have something more for you.").color(NamedTextColor.GRAY));
		}
		if (ScoreboardUtils.getScoreboardValue(player, scoreboardHighScore).orElse(0) < score) {
			ScoreboardUtils.setScoreboardValue(player, scoreboardHighScore, score);
			Leaderboard.leaderboardUpdate(player, scoreboardHighScore);
			player.sendMessage(Component.text("New high score!"));
		}
	}
}
