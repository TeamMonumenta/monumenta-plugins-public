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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CallicarpasPointedHat extends DepthsAbility {
	public static final String ABILITY_NAME = "Callicarpa's Pointed Hat";

	public static final DepthsAbilityInfo<CallicarpasPointedHat> INFO =
		new DepthsAbilityInfo<>(CallicarpasPointedHat.class, ABILITY_NAME, CallicarpasPointedHat::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.DRAGON_EGG)
			.floors(floor -> floor == 2)
			.gain(CallicarpasPointedHat::gain)
			.descriptions(CallicarpasPointedHat::getDescription);

	private final @Nullable DepthsPlayer mDepthsPlayer;

	public CallicarpasPointedHat(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDepthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
	}

	private static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mEarnedRewards.add(DepthsRoomType.DepthsRewardType.POINTED);
	}

	private static Description<CallicarpasPointedHat> getDescription() {
		return new DescriptionBuilder<CallicarpasPointedHat>().add("Pick an active tree. For the next three ability selections, you will only find abilities from that tree.")
			.add((a, p) -> a != null && a.mDepthsPlayer != null
				? Component.text("\nSelected tree: ")
				.append(a.mDepthsPlayer.mPointedHatTree == null
					? Component.text("None")
					: a.mDepthsPlayer.mPointedHatTree.getNameComponent()
					.append(Component.text("\nRemaining rewards: " + a.mDepthsPlayer.mPointedHatStacks, NamedTextColor.WHITE)))
				: Component.empty());
	}
}
