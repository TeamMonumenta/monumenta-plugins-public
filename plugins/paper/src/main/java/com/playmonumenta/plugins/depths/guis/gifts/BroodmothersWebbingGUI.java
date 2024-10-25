package com.playmonumenta.plugins.depths.guis.gifts;

import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.abilities.gifts.BroodmothersWebbing;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BroodmothersWebbingGUI extends AbstractDepthsSelectionGUI<DepthsPlayer> {
	public BroodmothersWebbingGUI(Player player) {
		super(player, "Webbing (Select Player)", BroodmothersWebbing.ABILITY_NAME, getPlayers(player), BroodmothersWebbingGUI::getPlayerHead, true);
	}

	@Override
	protected void selected(DepthsPlayer selection) {
		if (mDepthsPlayer == null) {
			return;
		}
		selection.mBroodmothersWebbing = true;
		DepthsManager.getInstance().setPlayerLevelInAbility(BroodmothersWebbing.ABILITY_NAME, mPlayer, mDepthsPlayer, 0, false, false);
		DepthsParty party = DepthsManager.getInstance().getPartyFromId(mDepthsPlayer);
		if (party == null) {
			return;
		}
		String ownPlayerName = mPlayer.getName();
		party.sendMessage(ownPlayerName + " has given " + (ownPlayerName.equals(selection.mPlayerName) ? "themselves" : selection.mPlayerName) + " a protective webbing! They will now be protected the next time they die!");
	}

	public static List<DepthsPlayer> getPlayers(Player player) {
		DepthsParty party = DepthsManager.getInstance().getDepthsParty(player);
		if (party == null) {
			return Collections.emptyList();
		}
		return party.mPlayersInParty.stream()
			.filter(dp -> !dp.mBroodmothersWebbing)
			.toList();
	}

	private static ItemStack getPlayerHead(DepthsPlayer dp) {
		Player player = dp.getPlayer();
		ItemStack playerHead = GUIUtils.createBasicItem(Material.PLAYER_HEAD, dp.mPlayerName, NamedTextColor.YELLOW);
		if (player != null) {
			GUIUtils.setSkullOwner(playerHead, player);
		}
		return playerHead;
	}
}
