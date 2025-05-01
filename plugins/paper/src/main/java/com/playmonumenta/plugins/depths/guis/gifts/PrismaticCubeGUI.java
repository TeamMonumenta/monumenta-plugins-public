package com.playmonumenta.plugins.depths.guis.gifts;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.gifts.PrismaticCube;
import java.util.Collections;
import java.util.List;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PrismaticCubeGUI extends AbstractDepthsSelectionGUI<DepthsAbilityInfo<?>> {
	public PrismaticCubeGUI(Player player) {
		super(player, "Prismatic Cube (Replace)", PrismaticCube.ABILITY_NAME,
			getEligibleAbilities(player), dai -> getAbilityItem(player, dai), true);
	}

	@Override
	protected void selected(DepthsAbilityInfo<?> selection) {
		if (mDepthsPlayer == null) {
			return;
		}
		DepthsManager dm = DepthsManager.getInstance();
		String randomPrismatic = dm.getRandomReplaceablePrismatic(selection.getDepthsTrigger());
		if (randomPrismatic != null) {
			dm.setPlayerLevelInAbility(selection.getDisplayName(), mPlayer, mDepthsPlayer, 0, true, true);
			dm.setPlayerLevelInAbility(randomPrismatic, mPlayer, mDepthsPlayer, 1, true, true);
			mPlayer.playSound(mPlayer, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		}
	}

	private static List<DepthsAbilityInfo<?>> getEligibleAbilities(Player player) {
		DepthsManager dm = DepthsManager.getInstance();
		DepthsPlayer dp = dm.getDepthsPlayer(player);
		if (dp == null) {
			return Collections.emptyList();
		}
		return DepthsManager.getAbilities().stream()
			.filter(ability -> dp.hasAbility(ability.getDisplayName()))
			.filter(ability -> ability.getDepthsTrigger().isActive() && dm.getRandomReplaceablePrismatic(ability.getDepthsTrigger()) != null)
			.toList();
	}

	private static ItemStack getAbilityItem(Player player, DepthsAbilityInfo<?> ability) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		int rarity;
		if (dp == null) {
			rarity = 1;
		} else {
			rarity = dp.getLevelInAbility(ability.getDisplayName());
		}
		return ability.createAbilityItem(rarity, 0, 0, player, false);
	}
}
