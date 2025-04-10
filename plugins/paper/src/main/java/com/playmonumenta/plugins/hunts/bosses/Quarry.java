package com.playmonumenta.plugins.hunts.bosses;

import com.playmonumenta.libraryofsouls.bestiary.BestiaryManager;
import com.playmonumenta.libraryofsouls.commands.LibraryOfSoulsCommand;
import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.bosses.SerializedLocationBossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.MonumentaEvent;
import com.playmonumenta.plugins.hunts.HuntsManager;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.integrations.monumentanetworkrelay.BroadcastedEvents;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.commands.Leaderboard;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class Quarry extends SerializedLocationBossAbilityGroup {
	private static final int BANISH_DELAY = 20 * 20;
	private static final int BANISH_PERCENT = 75;

	public static final String BANISH_CHARACTER = "⚠";
	public static final String SPOIL_CHARACTER = "❌";

	private static final int TEMPORARY_BLOCK_DURATION = 10 * 20;

	private static final String GENERAL_WINS_SCOREBOARD = "HuntsWins";
	private static final String GENERAL_UNSPOILED_WINS_SCOREBOARD = "HuntsUnspoiledWins";

	private static final List<Vector> LODGE_LOCS = Arrays.asList(
		new Vector(-501.5, 80, -636.5),
		new Vector(-505.5, 80, -630.5),
		new Vector(-511.5, 80, -630.5),
		new Vector(-515.5, 80, -637.5),
		new Vector(-514.5, 80, -641.5)
	);

	protected final double mRadiusInner;
	protected final double mRadiusOuter;
	protected final HuntsManager.QuarryType mQuarryType;

	protected final List<UUID> mPlayers;
	protected final Set<UUID> mSpoiledPlayers;
	private final List<UUID> mWarnedLatePlayers;
	private final BukkitRunnable mRunnable;

	protected @Nullable Spell mBanishSpell = null;
	private boolean mHasBanished;

	public final Set<Block> mBreakableBlocks = new HashSet<>();
	public final Map<Block, Integer> mDecayingBlocks = new HashMap<>();
	private final Set<UUID> mBlockBreakMessagedPlayers = new HashSet<>();

	public Quarry(Plugin plugin, String identityTag, LivingEntity boss, Location spawnLoc, Location endLoc, double radiusInner, double radiusOuter, HuntsManager.QuarryType quarryType) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);
		mRadiusInner = radiusInner;
		mRadiusOuter = radiusOuter;
		mQuarryType = quarryType;

		mPlayers = PlayerUtils.playersInRange(mSpawnLoc, mRadiusInner, true).stream().map(Player::getUniqueId).collect(Collectors.toCollection(ArrayList::new));
		mSpoiledPlayers = new HashSet<>();
		mWarnedLatePlayers = new ArrayList<>();

		// If they ever get unloaded (probably because players die) remove them
		// The BroadcastedEvent is handled in the runnable below
		EntityUtils.setRemoveEntityOnUnload(mBoss);

		mRunnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					BroadcastedEvents.clearEvent(mQuarryType.name());
					this.cancel();
					return;
				}

				Location loc = mBoss.getLocation();
				// If there are no nearby players, or the boss has moved too far from its spawnpoint, the hunt has failed
				List<Player> players = getPlayers();
				if (players.isEmpty() || bossIsOutOfRange()) {
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						onDespawn();
						for (Player player : players) {
							// If players are empty, no one will be sent this message, so it is always accurate
							player.sendMessage(Component.text("The Quarry wandered too far away from its habitat and disappeared!", NamedTextColor.RED));
						}
						new PartialParticle(Particle.CLOUD, loc, 15, 0.3, 0.3, 0.3, 0.1).spawnAsBoss();
						mBoss.getWorld().playSound(loc, Sound.BLOCK_AZALEA_LEAVES_FALL, SoundCategory.HOSTILE, 1.0f, 1.0f);
						mBoss.remove();
						BroadcastedEvents.clearEvent(mQuarryType.name());
					}, 3 * 20);
					this.cancel();
					return;
				}

				// Remove players who are too far from the boss
				for (Player player : players) {
					if (playerIsOutOfRange(player)) {
						UUID uuid = player.getUniqueId();
						mPlayers.remove(uuid);
						mSpoiledPlayers.remove(uuid);
					}
				}

				// Warn players who just entered that they can't get rewards
				List<Player> newPlayers = PlayerUtils.playersInRange(mSpawnLoc, mRadiusOuter, true);
				newPlayers.removeAll(players);
				newPlayers.removeAll(getWarnedLatePlayers());
				for (Player player : newPlayers) {
					mWarnedLatePlayers.add(player.getUniqueId());
					player.sendMessage(Component.text("You have entered the range of a Hunt, but were too late to get any rewards and are at risk of shattering on death!", NamedTextColor.RED, TextDecoration.BOLD));
				}

				if (!mHasBanished && mT >= BANISH_DELAY) {
					runBanishSpell();
					BossBarManager bossBar = getBossBar();
					if (bossBar != null) {
						bossBar.removeHealthEvent(BANISH_PERCENT);
					}
				}

				mT += 5;
			}
		};
		mRunnable.runTaskTimer(mPlugin, 0, 5);
	}

	public boolean bossIsOutOfRange() {
		return mBoss.getLocation().distanceSquared(mSpawnLoc) > mRadiusOuter * mRadiusOuter;
	}

	public boolean playerIsOutOfRange(Player player) {
		Location loc = player.getLocation();
		// Need to be out of the range of the fight and not close to the boss (in case the boss is near the boundary)
		return loc.distanceSquared(mSpawnLoc) > mRadiusOuter * mRadiusOuter && loc.distanceSquared(mBoss.getLocation()) > mRadiusInner * mRadiusInner;
	}

	public boolean spoil(Player player) {
		UUID uuid = player.getUniqueId();
		if (!mPlayers.contains(uuid) || mSpoiledPlayers.contains(uuid)) {
			// Don't add a player to the spoiled list if they are not properly in the fight or if they are already spoiled
			return false;
		}

		MessagingUtils.sendTitle(player, Component.text(""), Component.text(SPOIL_CHARACTER, mQuarryType.getColor()));

		mSpoiledPlayers.add(uuid);
		MMLog.fine("[Hunts] Player " + player.getName() + " spoiled quarry " + mBoss.getName());
		return true;
	}

	public void banish(Player player) {
		UUID uuid = player.getUniqueId();
		mPlayers.remove(uuid);
		mSpoiledPlayers.remove(uuid);
		teleportToLodge(player);
		MessagingUtils.sendTitle(player, Component.text("You have been banished!", NamedTextColor.RED), Component.text("You find yourself safe back at the Lodge", NamedTextColor.GRAY));
		PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), player, new PotionEffect(PotionEffectType.BLINDNESS, 3 * 20, 0, true, false));
		PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), player, new PotionEffect(PotionEffectType.DARKNESS, 2 * 20, 0, true, false));
		EffectManager.getInstance().addEffect(player, "QuarryBanishStasis", new Stasis(2 * 20, false));
		MMLog.fine("[Hunts] Player " + player.getName() + " banished by quarry " + mBoss.getName());
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		boolean inFight = mPlayers.remove(uuid);
		mSpoiledPlayers.remove(uuid);
		if (inFight) {
			event.setKeepInventory(true);
			event.setKeepLevel(true);
			event.getDrops().clear();
			event.setDroppedExp(0);
			player.addScoreboardTag(RespawnStasis.TEMP_NO_SPECTATE_TAG);
		}
	}

	private void teleportToLodge(Player player) {
		player.teleport(FastUtils.getRandomElement(LODGE_LOCS).toLocation(player.getWorld()));
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		mRunnable.cancel();
		giveAllLoot();
		BroadcastedEvents.clearEvent(mQuarryType.name());
	}

	@Override
	public boolean hasNearbyBlockBreakTrigger() {
		return true;
	}

	@Override
	public void nearbyBlockBreak(BlockBreakEvent event) {
		super.nearbyBlockBreak(event);

		Block block = event.getBlock();
		if (!isBreakable(block)) {
			event.setCancelled(true);

			Player player = event.getPlayer();
			UUID uuid = player.getUniqueId();
			if (!mBlockBreakMessagedPlayers.contains(uuid)) {
				mBlockBreakMessagedPlayers.add(uuid);
				player.sendMessage(Component.text("It is too dangerous to break this while the beast is nearby!", NamedTextColor.RED));
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 1, 0.63f);
			}
		} else {
			mBreakableBlocks.remove(block);
			mDecayingBlocks.remove(block);
		}
	}

	protected boolean isBreakable(Block block) {
		return mBreakableBlocks.contains(block) || block.getType().getHardness() == 0;
	}

	@Override
	public boolean hasNearbyBlockPlaceTrigger() {
		return true;
	}

	@Override
	public void nearbyBlockPlace(BlockPlaceEvent event) {
		super.nearbyBlockPlace(event);

		Block block = event.getBlockPlaced();
		mBreakableBlocks.add(block);
		mDecayingBlocks.put(block, Bukkit.getCurrentTick());
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (Bukkit.getCurrentTick() - mDecayingBlocks.get(block) == TEMPORARY_BLOCK_DURATION / 2) {
					block.setType(Material.BLACK_STAINED_GLASS);
				}
				if (Bukkit.getCurrentTick() - mDecayingBlocks.get(block) == TEMPORARY_BLOCK_DURATION) {
					this.cancel();
				}

				mTicks++;
				if (!mBreakableBlocks.contains(block)) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				mBreakableBlocks.remove(block);
				mDecayingBlocks.remove(block);

				block.setType(Material.AIR);
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public abstract String getUnspoiledLootTable();

	public abstract String getSpoiledLootTable();

	public String getWinsScoreboard() {
		return getBestiaryLoS() + "Wins";
	}

	public String getUnspoiledWinsScoreboard() {
		return getWinsScoreboard() + "Unspoiled";
	}

	public abstract String getAdvancement();

	public String getAdvancementUnspoiled() {
		return getAdvancement() + "_unspoiled";
	}

	public abstract String getQuestTag();

	private void giveAllLoot() {
		List<Player> allPlayers = getPlayers();
		List<Player> spoiledPlayers = getSpoiledPlayers();

		List<Player> unspoiledPlayers = new ArrayList<>(allPlayers);
		unspoiledPlayers.removeAll(spoiledPlayers);

		for (Player player : allPlayers) {
			addLeaderboardScore(player, GENERAL_WINS_SCOREBOARD);
			int kills = addLeaderboardScore(player, getWinsScoreboard());
			try {
				BestiaryManager.setKillsForMob(player, LibraryOfSoulsCommand.getSoul(getBestiaryLoS()), kills);
			} catch (WrapperCommandSyntaxException e) {
				MMLog.severe("[Hunts] Caught exception finding SoulEntry for " + getBestiaryLoS() + ": " + e);
			}
			AdvancementUtils.grantAdvancement(player, getAdvancement());
		}

		ItemStack unspoiledChest = ChestUtils.giveChestWithLootTable(getUnspoiledLootTable(), mQuarryType.getName() + " Yield (Unspoiled)", null, List.of(Component.text("The complete harvest from the quarry.", NamedTextColor.DARK_GRAY)));
		ItemStack spoiledChest = ChestUtils.giveChestWithLootTable(getSpoiledLootTable(), mQuarryType.getName() + " Yield (Spoiled)", null, List.of(Component.text("Some loot from the quarry, but", NamedTextColor.DARK_GRAY), Component.text("perhaps more could be extracted.", NamedTextColor.DARK_GRAY)));
		for (Player player : unspoiledPlayers) {
			InventoryUtils.giveItemWithWarningAfterDelay(player, unspoiledChest.clone());
			player.sendMessage(Component.text(mQuarryType.getName() + " has been slain and you obtained the complete haul of the loot.", mQuarryType.getColor()));

			addLeaderboardScore(player, GENERAL_UNSPOILED_WINS_SCOREBOARD);
			addLeaderboardScore(player, getUnspoiledWinsScoreboard());
			Bukkit.getPluginManager().callEvent(new MonumentaEvent(player, "huntsunspoiled"));
			AdvancementUtils.grantAdvancement(player, getAdvancementUnspoiled());
			if (ScoreboardUtils.getScoreboardValue(player, getUnspoiledWinsScoreboard()).orElse(0) == 1) {
				player.addScoreboardTag(getQuestTag());
				MonumentaNetworkRelayIntegration.broadcastCommand("tellmini msg @a <italic><gold>" + player.getName() + "</gold> has defeated " + mQuarryType.getName() + " without spoiling for the first time!");
			}
		}

		for (Player player : spoiledPlayers) {
			InventoryUtils.giveItemWithWarningAfterDelay(player, spoiledChest.clone());
			player.sendMessage(Component.text(mQuarryType.getName() + " has been slain, but you were only able to gather a portion of the loot.", mQuarryType.getColor()));
			Bukkit.getPluginManager().callEvent(new MonumentaEvent(player, "huntsspoiled"));
		}

		List<Player> noRewardsPlayers = PlayerUtils.playersInRange(mSpawnLoc, mRadiusOuter, true);
		noRewardsPlayers.removeAll(allPlayers);
		for (Player player : noRewardsPlayers) {
			player.sendMessage(Component.text(mQuarryType.getName() + " has been slain, but you did not arrive soon enough to get rewards or credit!", NamedTextColor.RED));
		}
	}

	private int addLeaderboardScore(Player player, String objective) {
		int total = ScoreboardUtils.addScore(player, objective, 1);
		Leaderboard.leaderboardUpdate(player, objective);
		return total;
	}

	private String getBestiaryLoS() {
		if (mQuarryType == HuntsManager.QuarryType.THE_IMPENETRABLE) {
			return "TheImpenetrable";
		} else {
			return mQuarryType.getLos();
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		// Get all players even if they are not part of the hunt so that the bosses cannot be cheesed by people entering late
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRadiusOuter, true);
		double mult = BossUtils.healthScalingCoef(players.size(), 0.6, 0.3);
		event.setFlatDamage(event.getFlatDamage() / mult);

		if (event.getSource() instanceof Player player && (mSpoiledPlayers.contains(player.getUniqueId()) || !mPlayers.contains(player.getUniqueId()))) {
			event.setFlatDamage(event.getFlatDamage() * 0.8);
		}
		// keep quarry above 25% during banish
		if (mBanishSpell != null && mBanishSpell.isRunning() && event.getCause() != EntityDamageEvent.DamageCause.VOID) {
			double hpDiff = mBoss.getHealth() - (EntityUtils.getMaxHealth(mBoss) * 0.25);
			if (event.getDamage() > hpDiff) {
				event.setDamageCap(Math.max(0, hpDiff));
			}
		}
	}

	public Map<Integer, BossBarManager.BossHealthAction> getBaseHealthEvents() {
		Map<Integer, BossBarManager.BossHealthAction> events = new HashMap<>();
		events.put(BANISH_PERCENT, boss -> runBanishSpell());
		return events;
	}

	private void runBanishSpell() {
		mHasBanished = true;
		if (mBanishSpell != null) {
			mBanishSpell.run();
		}
	}

	public void onDespawn() {

	}

	public List<Player> getPlayers() {
		return mapToPlayer(mPlayers);
	}

	public List<Player> getSpoiledPlayers() {
		return mapToPlayer(mSpoiledPlayers);
	}

	private List<Player> getWarnedLatePlayers() {
		return mapToPlayer(mWarnedLatePlayers);
	}

	private List<Player> mapToPlayer(Collection<UUID> uuids) {
		return uuids.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
	}

	public boolean hasPlayer(Player player) {
		return mPlayers.contains(player.getUniqueId());
	}
}
