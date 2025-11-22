package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class BrokenClock extends DepthsAbility {
	public static final String ABILITY_NAME = "Broken Clock";

	public static final DepthsAbilityInfo<BrokenClock> INFO =
		new DepthsAbilityInfo<>(BrokenClock.class, ABILITY_NAME, BrokenClock::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.BLAZE_SPAWN_EGG)
			.floors(floor -> floor == 2)
			.descriptions(BrokenClock::getDescription);

	public BrokenClock(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void trigger(Player player, DepthsPlayer dp) {
		dp.addReward(DepthsRoomType.DepthsRewardType.GIFT);
		dp.addReward(DepthsRoomType.DepthsRewardType.GIFT);
		DepthsManager.getInstance().setPlayerLevelInAbility(ABILITY_NAME, player, dp, 0, false, false);
		playSound(player);
	}

	public static void playSound(Player player) {
		Plugin plugin = Plugin.getInstance();
		player.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			player.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1.0f, 0.75f);
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				player.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				player.playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0f, 0.8f);
			}, 20);
		}, 20);
	}

	private static Description<BrokenClock> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("Gain nothing now. If you defeat the next boss, gain three celestial gift selections instead of one.");
	}
}
