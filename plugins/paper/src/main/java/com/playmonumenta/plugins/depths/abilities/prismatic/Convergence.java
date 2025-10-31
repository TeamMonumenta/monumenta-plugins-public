package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Convergence extends DepthsAbility {
	public static final String ABILITY_NAME = "Convergence";
	public static final int[] WILDCARD_CAP = {2, 3, 4, 5, 6, 7};

	public static final DepthsAbilityInfo<Convergence> INFO =
		new DepthsAbilityInfo<>(Convergence.class, ABILITY_NAME, Convergence::new, DepthsTree.PRISMATIC, DepthsTrigger.WILDCARD)
			.displayItem(Material.RECOVERY_COMPASS)
			.descriptions(Convergence::getDescription);

	public Convergence(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static List<DepthsAbilityInfo<?>> getWildcardAbilities(Player player) {
		DepthsManager dm = DepthsManager.getInstance();
		return dm.getPlayerAbilities(player).stream()
			.filter(a -> a.getDepthsTrigger() == DepthsTrigger.WILDCARD)
			.filter(a -> !ABILITY_NAME.equals(a.getDisplayName()))
			.sorted(Comparator.comparing(a -> dm.getPlayerLevelInAbility(a.getDisplayName(), player), Comparator.reverseOrder()))
			.toList();
	}

	public static boolean canGainWildcards(Player player) {
		int level = DepthsManager.getInstance().getPlayerLevelInAbility(ABILITY_NAME, player);
		int numWildcards = getWildcardAbilities(player).size();
		if (level == 0) {
			return numWildcards == 0;
		}
		return numWildcards < WILDCARD_CAP[level - 1];
	}

	public static void onLevelChange(Player player, int level) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		int keepCount = 1;
		if (level > 0) {
			keepCount = WILDCARD_CAP[level - 1];
		}
		for (DepthsAbilityInfo<?> ability : getWildcardAbilities(player)) {
			if (keepCount == 0) {
				DepthsManager.getInstance().setPlayerLevelInAbility(ability.getDisplayName(), player, dp, 0, true, true);
			} else {
				keepCount--;
			}
		}
	}

	private static Description<Convergence> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("You can now hold up to ")
			.add(a -> WILDCARD_CAP[rarity - 1], WILDCARD_CAP[rarity - 1], false, null, true)
			.add(" wildcard abilities.")
			.add((a, p) -> {
				if (p == null) {
					return Component.empty();
				}
				List<DepthsAbilityInfo<?>> wildcardAbilities = getWildcardAbilities(p);
				if (wildcardAbilities.isEmpty()) {
					return Component.empty();
				}
				List<Component> names = wildcardAbilities.stream()
					.map(DepthsAbilityInfo::getColoredName)
					.filter(Objects::nonNull)
					.toList();
				Component namesComp = MessagingUtils.concatenateComponents(names, Component.text(", "));
				return Component.text("\n\nCurrent abilities: ").append(namesComp);
			});
	}
}
