package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.guis.lib.ReactiveValue;
import com.playmonumenta.plugins.listeners.ItemDropListener;
import com.playmonumenta.plugins.listeners.JunkItemListener;
import com.playmonumenta.plugins.utils.SignUtils;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;

final class PickupAndDisableDropPage extends PebPage {
	private static final String KEYBIND_STR = Constants.Keybind.SWAP_OFFHAND.asKeybind();

	PickupAndDisableDropPage(PebGui gui) {
		super(gui, Material.PRISMARINE_CRYSTALS, "Pickup and Disable Drop Settings",
			"Choose the appropriate level of pickup filter and drop filter below.");
	}

	@Override
	protected void render() {
		super.render();

		entry(
			Material.LEATHER,
			"Item Drop Settings",
			"Configure item drop blocking"
		).cycle(
			ReactiveValue.fromEnum(
				mGui,
				ItemDropListener.Mode.class,
				ItemDropListener::getPlayerMode,
				(mode, player) -> ItemDropListener.setPlayerMode(player, mode)
			),
			Arrays.stream(ItemDropListener.Mode.values()).map(x -> x.mDisplayName).toArray(String[]::new)
		).set(2, 3);

		final var threshold = JunkItemListener.PlayerSetting.get(getPlayer()).threshold();
		final var text = "[<white><key:" + KEYBIND_STR + "></white>] Threshold (currently <white>" + threshold + "</white>)";

		entry(
			Material.SCAFFOLDING,
			"Item Pickup Settings",
			"Configure item pickup filtering"
		).cycle(
			ReactiveValue.fromEnum(
				mGui,
				JunkItemListener.PlayerSetting.Mode.class,
				p -> JunkItemListener.PlayerSetting.get(p).mode(),
				(mode, player) -> JunkItemListener.PlayerSetting.get(player).mode(mode).set(player)
			),
			Arrays.stream(JunkItemListener.PlayerSetting.Mode.values()).map(x -> x.mDisplayName).toArray(String[]::new)
		).lore("").lore(text).onClick(event -> {
			if (event.getClick() == ClickType.SWAP_OFFHAND) {
				openPrompt();
			}
		}).set(2, 5);
	}

	private void openPrompt() {
		SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Input a number", "from 1-65 above."))
			.reopenIfFail(false)
			.response((player, strings) -> {
				int inputVal;
				try {
					inputVal = Integer.parseInt(strings[0]);
				} catch (Exception e) {
					player.sendMessage(Component.text("Input is not an integer.", NamedTextColor.RED));
					return false;
				}
				if (inputVal >= 1 && inputVal <= 65) {
					JunkItemListener.PlayerSetting.get(getPlayer()).threshold(inputVal).set(getPlayer());
					mGui.markDirty();
					return false;
				} else {
					player.sendMessage(Component.text("Input is not with the bounds of 1 - 65.", NamedTextColor.RED));
				}
				return true;
			})
			.open(getPlayer());
	}
}
