package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CurseOfGluttony extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Gluttony";

	public static final DepthsAbilityInfo<CurseOfGluttony> INFO =
		new DepthsAbilityInfo<>(CurseOfGluttony.class, ABILITY_NAME, CurseOfGluttony::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.FERMENTED_SPIDER_EYE)
			.floors(floor -> floor != 1)
			.gain(CurseOfGluttony::gain)
			.descriptions(CurseOfGluttony::getDescription);

	public CurseOfGluttony(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		Map<Integer, List<String>> sortedAbilities = new HashMap<>();
		dp.mAbilities.forEach((ability, rarity) -> {
			if (rarity >= 6) {
				return;
			}
			List<String> rarityAbilities = sortedAbilities.computeIfAbsent(rarity, i -> new ArrayList<>());
			rarityAbilities.add(ability);
		});

		int removed = 0;
		int highestRarity = 5;
		List<String> abilities = sortedAbilities.get(highestRarity);
		outer: while (removed < 2) {
			while (abilities == null || abilities.isEmpty()) {
				highestRarity--;
				if (highestRarity <= 0) {
					break outer;
				}
				abilities = sortedAbilities.get(highestRarity);
			}
			String ability = FastUtils.getRandomElement(abilities);
			abilities.remove(ability);
			DepthsAbilityInfo<?> info = DepthsManager.getInstance().getAbility(ability);
			if (info == null || !info.getHasLevels()) {
				continue;
			}
			DepthsManager.getInstance().setPlayerLevelInAbility(ability, player, dp, 0, true, true);
			removed++;
		}
	}

	private static Description<CurseOfGluttony> getDescription() {
		return new DescriptionBuilder<CurseOfGluttony>()
			.add("Lose your 2 highest rarity abilities.");
	}
}
