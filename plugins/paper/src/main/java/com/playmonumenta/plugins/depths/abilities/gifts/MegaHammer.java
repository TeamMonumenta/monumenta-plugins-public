package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsRarity;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class MegaHammer extends DepthsAbility {
	public static final String ABILITY_NAME = "Mega Hammer";

	public static final DepthsAbilityInfo<MegaHammer> INFO =
		new DepthsAbilityInfo<>(MegaHammer.class, ABILITY_NAME, MegaHammer::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.IRON_AXE)
			.floors(floor -> floor == 3)
			.gain(MegaHammer::gain)
			.descriptions(MegaHammer::getDescription);

	public MegaHammer(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsManager dm = DepthsManager.getInstance();
		DepthsPlayer dp = dm.getDepthsPlayer(player);
		DepthsParty party = dm.getDepthsParty(player);
		if (dp == null || party == null) {
			return;
		}
		dp.mAbilities.forEach((ability, level) -> {
			if (level == 1 || level == 2) {
				DepthsAbilityInfo<?> info = dm.getAbility(ability);
				if (info != null && info.getHasLevels()) {
					dm.setPlayerLevelInAbility(ability, player, 4, false);
				}
			}
		});
		party.sendMessage(player.getName() + " used a MEGA HAMMER and upgraded their common and uncommon abilities to epic level!");
		player.playSound(player, Sound.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0f, 0.6f);
		DepthsManager.getInstance().setPlayerLevelInAbility(ABILITY_NAME, player, 0, false);
	}

	private static Description<MegaHammer> getDescription() {
		return new DescriptionBuilder<MegaHammer>().add("Any ")
			.add(DepthsRarity.COMMON.getDisplay())
			.add(" or ")
			.add(DepthsRarity.UNCOMMON.getDisplay())
			.add(" abilities you have are upgraded to ")
			.add(DepthsRarity.EPIC.getDisplay())
			.add(" level.");
	}
}
