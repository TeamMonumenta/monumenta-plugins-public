package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.SporousAmalgam;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PassiveSpores extends Spell {
	private static final int SPORE_DECAY_TIME = 20 * 5;
	private static final float SPORE_DECAY_AMOUNT = (float) (0.25 / 20.0 * 2);
	private static final float SPORE_SPOIL_THRESHOLD = 10;

	private final SporousAmalgam mSporeBeast;

	private final Map<Player, Float> mSporeValues = new HashMap<>();
	private final Map<Player, BossBar> mSporeBossBars = new HashMap<>();
	private final Map<Player, Integer> mLastSporeAddTimes = new HashMap<>();
	private final Set<Player> mAllViewingPlayers;

	private static final Component NORMAL_BOSS_BAR = Component.text("Spores", TextColor.color(SporousAmalgam.TEXT_COLOR));

	private boolean mActive;
	private int mTicks;

	public PassiveSpores(Plugin plugin, SporousAmalgam sporeBeast) {
		mSporeBeast = sporeBeast;
		mActive = true;
		mTicks = 0;
		mAllViewingPlayers = new HashSet<>();
		Bukkit.getScheduler().runTaskLater(plugin, () -> mSporeBeast.getPlayersInOutRange().forEach(this::createSporeBar), 0);
	}

	@Override
	public void run() {
		if (!mActive) {
			return;
		}
		List<Player> players = mSporeBeast.getPlayersInOutRange();
		updateMaps(players);
		for (Player p : mSporeValues.keySet()) {
			if (!mLastSporeAddTimes.containsKey(p)) {
				continue;
			}
			if (mTicks - mLastSporeAddTimes.get(p) >= SPORE_DECAY_TIME) {
				addSpores(p, -SPORE_DECAY_AMOUNT);
			}
		}
		mTicks++;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private void createSporeBar(Player player) {
		if (!mActive) {
			return;
		}

		if (mSporeBossBars.containsKey(player)) {
			return;
		}

		mSporeValues.put(player, 0f);

		BossBar bar = BossBar.bossBar(NORMAL_BOSS_BAR, 0, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
		player.showBossBar(bar);
		mSporeBossBars.put(player, bar);
		mAllViewingPlayers.add(player);
	}

	public void addSpores(Player player, float amount) {
		if (!mSporeValues.containsKey(player) || !mSporeBossBars.containsKey(player)) {
			return;
		}

		if (mSporeValues.get(player) >= SPORE_SPOIL_THRESHOLD) {
			return;
		}

		changeMapValues(player, amount);
		if (amount > 0) {
			mLastSporeAddTimes.put(player, mTicks);
		}
	}

	public void updateMaps(List<Player> players) {
		if (!mActive) {
			return;
		}

		for (Player player : players) {
			createSporeBar(player);
		}
		for (Player player : new HashSet<>(mSporeBossBars.keySet())) {
			if (!players.contains(player)) {
				removePlayer(player);
			}
		}
	}

	private void changeMapValues(Player player, float amount) {
		if (!mSporeValues.containsKey(player) || !mSporeBossBars.containsKey(player)) {
			return;
		}
		float newSpore = Math.min(SPORE_SPOIL_THRESHOLD, mSporeValues.get(player) + amount);
		mSporeValues.put(player, Math.max(0f, newSpore));
		mSporeBossBars.get(player).progress(Math.min(1f, Math.max(0f, newSpore / SPORE_SPOIL_THRESHOLD)));
		if (newSpore == SPORE_SPOIL_THRESHOLD) {
			if (mSporeBeast.spoil(player)) {
				player.sendMessage(Component.text("The spores have clung to you and won't go dormant in your presence, spoiling your loot", SporousAmalgam.TEXT_COLOR));
			}
		}
	}

	public void clearSpores() {
		for (Player p : mAllViewingPlayers) {
			if (mSporeBossBars.get(p) != null) {
				p.hideBossBar(mSporeBossBars.get(p));
				mSporeBossBars.get(p).removeViewer(p);
			}
		}

		mSporeValues.clear();
		mSporeBossBars.clear();
		mAllViewingPlayers.clear();
		mActive = false;
	}

	public void removePlayer(Player player) {
		for (BossBar bar : mSporeBossBars.values()) {
			player.hideBossBar(bar);
		}
		mSporeBossBars.remove(player);
		mSporeValues.remove(player);
	}
}
