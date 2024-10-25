package com.playmonumenta.plugins.depths.guis;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.prismatic.Generosity;
import com.playmonumenta.plugins.utils.MessagingUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DepthsRemoveAbilityGUI extends AbstractDepthsAbilityUtilityGUI {

	public DepthsRemoveAbilityGUI(Player targetPlayer, boolean showPassives) {
		super(targetPlayer, "Remove an Ability", showPassives);
	}

	@Override
	public void onConfirm(Player player, DepthsPlayer dp, DepthsAbilityInfo<?> ability) {
		if (!dp.mUsedAbilityDeletion) {
			dp.mUsedAbilityDeletion = true;
			String removedAbility = ability.getDisplayName();
			DepthsManager.getInstance().setPlayerLevelInAbility(removedAbility, player, dp, 0, true, false);
			player.closeInventory();
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1.0f, 1.0f);
			MessagingUtils.sendActionBarMessage(player, "Ability removed!");

			DepthsManager.getInstance().validateOfferings(dp, removedAbility);

			//Trigger Generosity if applicable
			Generosity.abilityRemoved(dp, removedAbility);
		}
	}

	@Override
	protected ItemStack getDescriptionItem() {
		return createCustomItem(Material.PURPLE_STAINED_GLASS_PANE, "Click the ability to remove", "Remove 1 ability of your choosing at no cost.");
	}
}
