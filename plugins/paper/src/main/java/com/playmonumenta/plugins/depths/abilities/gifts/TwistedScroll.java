package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsRarity;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class TwistedScroll extends DepthsAbility {
	public static final String ABILITY_NAME = "Twisted Scroll";

	public static final DepthsAbilityInfo<TwistedScroll> INFO =
		new DepthsAbilityInfo<>(TwistedScroll.class, ABILITY_NAME, TwistedScroll::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.PAPER)
			.gain(TwistedScroll::gain)
			.descriptions(TwistedScroll::getDescription);

	public TwistedScroll(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mEarnedRewards.add(DepthsRoomType.DepthsRewardType.TWISTED);
		DepthsManager.getInstance().setPlayerLevelInAbility(ABILITY_NAME, player, 0, false);
	}

	private static Description<TwistedScroll> getDescription() {
		return new DescriptionBuilder<TwistedScroll>()
			.add("Select an ability to upgrade to ")
			.add(DepthsRarity.TWISTED.getDisplay())
			.add(" level.");
	}
}
