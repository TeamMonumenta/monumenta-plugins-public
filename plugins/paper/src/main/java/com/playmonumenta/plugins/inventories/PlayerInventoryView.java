package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.custominventories.PlayerDisplayCustomInventory;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.InventoryUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInventoryView implements Listener {
	private static final String PERMISSION = "monumenta.peb.inventoryview";

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void playerAnimationEvent(PlayerAnimationEvent event) {
		checkAndTriggerOpenEvent(event.getPlayer());
	}

	private void checkAndTriggerOpenEvent(Player requestingPlayer) {
		ItemStack mainHand = requestingPlayer.getInventory().getItemInMainHand();
		if (mainHand.getType().equals(Material.WRITTEN_BOOK)
			&& InventoryUtils.testForItemWithName(mainHand, "Personal Enchanted Book", true)
			&& InventoryUtils.testForItemWithLore(mainHand, "* Skin :")
			&& requestingPlayer.hasPermission(PERMISSION)) {

			Hitbox hitbox = Hitbox.approximateCone(requestingPlayer.getLocation(), 7, Math.toRadians(30));

			List<Player> nearbyPlayers = hitbox.getHitPlayers(requestingPlayer, true);
			if (nearbyPlayers.isEmpty()) {
				return;
			}
			Player clickedPlayer = nearbyPlayers.get(0);
			if (clickedPlayer != null && clickedPlayer.getScoreboardTags().contains("inventoryPrivacy")
				&& !requestingPlayer.hasPermission("group.devops")) {
				requestingPlayer.sendMessage(Component.text("This player has opted out of inventory viewing.",
					NamedTextColor.RED));
				return;
			}
			if (clickedPlayer != null && PremiumVanishIntegration.canSee(requestingPlayer, clickedPlayer)) {
				new PlayerDisplayCustomInventory(requestingPlayer, clickedPlayer).openInventory(requestingPlayer, Plugin.getInstance());
			}
		}
	}
}
