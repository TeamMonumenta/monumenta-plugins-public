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
import com.playmonumenta.plugins.depths.guis.gifts.BroodmothersWebbingGUI;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BroodmothersWebbing extends DepthsAbility {
	public static final String ABILITY_NAME = "Broodmother's Webbing";

	public static final DepthsAbilityInfo<BroodmothersWebbing> INFO =
		new DepthsAbilityInfo<>(BroodmothersWebbing.class, ABILITY_NAME, BroodmothersWebbing::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.COBWEB)
			.floors(floor -> floor == 3)
			.offerable(p -> !BroodmothersWebbingGUI.getPlayers(p).isEmpty())
			.gain(BroodmothersWebbing::gain)
			.descriptions(BroodmothersWebbing::getDescription);

	public BroodmothersWebbing(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mEarnedRewards.add(DepthsRoomType.DepthsRewardType.WEBBING);
	}

	private static Description<BroodmothersWebbing> getDescription() {
		return new DescriptionBuilder<BroodmothersWebbing>().add("Select another player to receive a protective webbing. The next time they die, their negative effects are cleansed and they are healed to full health.");
	}
}
