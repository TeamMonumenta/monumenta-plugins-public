package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class ConfirmationGUI extends Gui {
	private final String mConfirmCommand;
	private final String mDenyCommand;

	public ConfirmationGUI(Player player, String confirmCommand, String denyCommand) {
		super(player, 3 * 9, Component.text("Confirmation"));
		this.mConfirmCommand = confirmCommand;
		this.mDenyCommand = denyCommand;
	}

	@Override
	protected void setup() {
		ItemStack confirmItem = GUIUtils.createBasicItem(Material.GREEN_STAINED_GLASS_PANE, "Confirm", NamedTextColor.GREEN);
		ItemStack denyItem = GUIUtils.createBasicItem(Material.ORANGE_STAINED_GLASS_PANE, "Cancel", NamedTextColor.RED);
		setItem(1, 6, confirmItem).onClick((clickEvent) -> {
			serverCommand(mConfirmCommand, mPlayer);
			close();
		});
		setItem(1, 2, denyItem).onClick((clickEvent) -> {
			serverCommand(mDenyCommand, mPlayer);
			close();
		});
	}

	@Override
	protected void onClose(InventoryCloseEvent event) {
		if (event.getReason() == InventoryCloseEvent.Reason.PLAYER) {
			serverCommand(mDenyCommand, mPlayer);
		}
	}

	public void serverCommand(String command, Player player) {
		String pName = player.getName();
		String finalCommand = command.replace("@S", pName);
		NmsUtils.getVersionAdapter().runConsoleCommandSilently("execute at " + pName + " as " + pName + " run " + finalCommand);
	}
}
