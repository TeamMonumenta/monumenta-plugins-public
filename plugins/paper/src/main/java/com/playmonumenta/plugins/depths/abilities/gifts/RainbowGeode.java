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
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class RainbowGeode extends DepthsAbility {
	public static final String ABILITY_NAME = "Rainbow Geode";

	public static final DepthsAbilityInfo<RainbowGeode> INFO =
		new DepthsAbilityInfo<>(RainbowGeode.class, ABILITY_NAME, RainbowGeode::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.AMETHYST_SHARD)
			.floors(floor -> floor == 2)
			.descriptions(RainbowGeode::getDescription);

	private final @Nullable DepthsPlayer mDepthsPlayer;

	public RainbowGeode(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDepthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
	}

	private static Description<RainbowGeode> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("You now have the choice to skip room rewards. Gain a ")
			.add(DepthsTree.PRISMATIC.getNameComponent())
			.add(" ability selection every time you skip three rewards.")
			.add((a, p) -> a != null && a.mDepthsPlayer != null
				? Component.text("\nGained rewards: " + (a.mDepthsPlayer.mRewardSkips % 3))
				: Component.empty());
	}
}
