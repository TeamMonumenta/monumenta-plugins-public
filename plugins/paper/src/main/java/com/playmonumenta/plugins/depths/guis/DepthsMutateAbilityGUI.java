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

public class DepthsMutateAbilityGUI extends AbstractDepthsAbilityUtilityGUI {

	public DepthsMutateAbilityGUI(Player targetPlayer) {
		super(targetPlayer, "Mutate an Ability Trigger", false);
	}

	@Override
	public void onConfirm(Player player, DepthsPlayer dp, DepthsAbilityInfo<?> ability) {
		if (!dp.mUsedAbilityMutation) {
			dp.mUsedAbilityMutation = true;
			String removedAbility = ability.getDisplayName();
			DepthsManager.getInstance().setPlayerLevelInAbility(removedAbility, player, dp, 0, true, false);
			player.closeInventory();
			DepthsManager.getInstance().getMutatedAbility(player, dp, ability);
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1.0f, 1.0f);
			MessagingUtils.sendActionBarMessage(player, "Ability mutated!");

			DepthsManager.getInstance().validateOfferings(dp, removedAbility);

			//Trigger Generosity if applicable
			Generosity.abilityRemoved(dp, removedAbility);
		}
	}

	@Override
	protected ItemStack getDescriptionItem() {
		return createCustomItem(Material.PURPLE_STAINED_GLASS_PANE, "Click the ability to mutate", "Replace one ability trigger of your choosing.");
	}
}
