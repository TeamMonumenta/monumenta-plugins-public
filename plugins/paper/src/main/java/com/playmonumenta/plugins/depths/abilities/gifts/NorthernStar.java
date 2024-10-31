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

public class NorthernStar extends DepthsAbility {
	public static final String ABILITY_NAME = "Northern Star";
	private static final int STACKS = 4;

	public static final DepthsAbilityInfo<NorthernStar> INFO =
		new DepthsAbilityInfo<>(NorthernStar.class, ABILITY_NAME, NorthernStar::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.END_CRYSTAL)
			.gain(NorthernStar::gain)
			.descriptions(NorthernStar::getDescription);

	private final @Nullable DepthsPlayer mDepthsPlayer;

	public NorthernStar(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDepthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
	}

	private static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mNorthernStarStacks = STACKS;
	}

	private static Description<NorthernStar> getDescription() {
		return new DescriptionBuilder<NorthernStar>().add("The next " + STACKS + " elite rooms offer an additional reward.")
			.add((a, p) -> a != null && a.mDepthsPlayer != null
				? Component.text("\nRemaining rewards: " + a.mDepthsPlayer.mNorthernStarStacks)
				: Component.empty());
	}
}
