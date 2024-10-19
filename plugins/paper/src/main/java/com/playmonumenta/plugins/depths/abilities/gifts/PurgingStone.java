package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PurgingStone extends DepthsAbility {
	public static final String ABILITY_NAME = "Purging Stone";

	public static final DepthsAbilityInfo<PurgingStone> INFO =
		new DepthsAbilityInfo<>(PurgingStone.class, ABILITY_NAME, PurgingStone::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.CLAY)
			.floors(floor -> floor == 3)
			.offerable(p -> DepthsManager.getInstance().hasAbilityOfTree(p, DepthsTree.CURSE))
			.gain(PurgingStone::gain)
			.descriptions(PurgingStone::getDescription);

	public PurgingStone(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsManager dm = DepthsManager.getInstance();
		DepthsPlayer dp = dm.getDepthsPlayer(player);
		DepthsParty party = dm.getPartyFromId(dp);
		if (dp == null || party == null) {
			return;
		}
		List<String> curses = dm.getPlayerAbilities(dp).stream()
			.filter(dai -> dai.getDepthsTree() == DepthsTree.CURSE)
			.map(AbilityInfo::getDisplayName)
			.toList();
		if (!curses.isEmpty()) {
			dp.mAbilities.forEach((ability, level) -> {
				if (level > 1 && level < 6) {
					DepthsAbilityInfo<?> info = dm.getAbility(ability);
					if (info != null && info.getHasLevels()) {
						dm.setPlayerLevelInAbility(ability, player, Math.max(1, level - 1), false);
					}
				}
			});
			party.sendMessage(player.getName() + " downgraded all their abilities by a level!");
			dm.setPlayerLevelInAbility(FastUtils.getRandomElement(curses), player, 0, true, true);
		}
		dm.setPlayerLevelInAbility(ABILITY_NAME, player, 0, false);
	}

	private static Description<PurgingStone> getDescription() {
		return new DescriptionBuilder<PurgingStone>().add("Downgrade all abilities by one level and remove a random curse.");
	}
}
