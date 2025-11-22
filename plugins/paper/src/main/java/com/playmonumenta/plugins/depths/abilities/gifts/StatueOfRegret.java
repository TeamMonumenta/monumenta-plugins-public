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
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class StatueOfRegret extends DepthsAbility {
	public static final String ABILITY_NAME = "Statue of Regret";

	public static final DepthsAbilityInfo<StatueOfRegret> INFO =
		new DepthsAbilityInfo<>(StatueOfRegret.class, ABILITY_NAME, StatueOfRegret::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.TOTEM_OF_UNDYING)
			.offerable(p -> DepthsManager.getInstance().hasAbilityOfTree(p, DepthsTree.CURSE))
			.gain(StatueOfRegret::gain)
			.descriptions(StatueOfRegret::getDescription);

	public StatueOfRegret(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.addReward(DepthsRoomType.DepthsRewardType.STATUE);
	}

	private static Description<StatueOfRegret> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("Choose one of your current curses to replace with a selection of five other curses.");
	}
}
