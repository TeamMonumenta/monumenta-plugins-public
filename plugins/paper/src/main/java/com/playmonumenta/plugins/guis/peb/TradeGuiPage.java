package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.guis.CustomTradeGui;
import com.playmonumenta.plugins.guis.lib.ReactiveValue;
import org.bukkit.Material;

final class TradeGuiPage extends PebPage {
	TradeGuiPage(PebGui gui) {
		super(gui, Material.DEEPSLATE_EMERALD_ORE, "Trading GUI Options", "Options for the custom trade GUI");
	}

	@Override
	protected void render() {
		super.render();

		entry(
			Material.BIRCH_SIGN,
			"General",
			"General trade options"
		).set(1, 2);

		entry(
			Material.LOOM,
			"Custom Trade GUI",
			"Toggles between vanilla UI and custom GUI."
		).toggle(
			"Trade GUI: ",
			ReactiveValue.binaryScoreboard(mGui, CustomTradeGui.MAIN, true),
			"<white>custom",
			"<white>vanilla"
		).set(2, 1);

		entry(
			Material.CARTOGRAPHY_TABLE,
			"Theme",
			"Toggles between classic and sleek theme."
		).toggle(
			"Theme: ",
			ReactiveValue.binaryScoreboard(mGui, CustomTradeGui.THEME, false),
			"<white>classic",
			"<white>sleek"
		).set(2, 2);

		entry(
			Material.BIRCH_SIGN,
			"Trade Preview Page",
			"Preview trade options."
		).set(1, 6);

		entry(
			Material.TRIPWIRE_HOOK,
			"Trade Spacing",
			"Toggles the spacing options for a trade page."
		).cycle(CustomTradeGui.SPACING, "Auto", "16 per page", "28 per page").set(2, 5);

		entry(
			Material.EMERALD,
			"Display Price on Preview Items",
			"Toggles displaying the price of each item on the preview page."
		).invertedToggle(
			"Display price: ",
			ReactiveValue.binaryScoreboard(mGui, CustomTradeGui.PREVIEWDISPLAY, false)
		).set(2, 6);

		entry(
			Material.BOOKSHELF,
			"Trade Organization",
			"Toggles sorting trades by category or displaying them together."
		).toggle(
			"Trade organization: ",
			ReactiveValue.binaryScoreboard(mGui, CustomTradeGui.TRADEORG, false),
			"<white>split by type", "<white>display together"
		).set(2, 7);

		entry(
			Material.BIRCH_SIGN,
			"Trade Logistics",
			"Trade logistics options."
		).set(3, 2);

		entry(
			Material.ANVIL,
			"Confirm Page on Left-Click",
			"Toggles whether to show a confirm page when left-clicking on a trade preview."
		).invertedToggle(
			"Confirm page: ",
			ReactiveValue.binaryScoreboard(mGui, CustomTradeGui.CONFIRM, false)
		).set(4, 1);

		entry(
			Material.LIGHTNING_ROD,
			"Quick-Buy on Shift-Click",
			"Toggles whether to instantly buy 1 when shift-clicking on a trade preview."
		).invertedToggle(
			"Quick-buy: ",
			ReactiveValue.binaryScoreboard(mGui, CustomTradeGui.QUICKBUY, false)
		).set(4, 2);

		entry(
			Material.GLOW_ITEM_FRAME,
			"Upon Successful Trade",
			"Set what to do upon a successful trade."
		).cycle(
			ReactiveValue.scoreboard(mGui, CustomTradeGui.SUCCESS, 2),
			"return to preview",
			"close GUI",
			"do nothing"
		).set(4, 3);

		entry(
			Material.BIRCH_SIGN,
			"Misc",
			"Miscellaneous trade options."
		).set(3, 6);

		entry(
			Material.FIREWORK_ROCKET,
			"Trade Particle Effects",
			"Toggles particle effects upon successful trade."
		).invertedToggle(
			"Trade particles: ",
			ReactiveValue.binaryScoreboard(mGui, CustomTradeGui.PARTICLES, false)
		).set(4, 5);

		entry(
			Material.BELL,
			"Trade Sound Effects",
			"Toggles sound effects upon successful trade."
		).invertedToggle(
			"Trade sounds: ",
			ReactiveValue.binaryScoreboard(mGui, CustomTradeGui.SOUNDS, false)
		).set(4, 6);

		entry(
			Material.FLOWER_POT,
			"Wallet Integration",
			"Toggles whether to take currency directly from your wallet."
		).cycle(CustomTradeGui.WALLET, "Prioritize inventory", "Disabled", "Prioritize wallet").set(4, 7);
	}
}
