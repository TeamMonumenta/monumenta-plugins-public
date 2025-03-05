package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.guis.lib.PagedGui;
import com.playmonumenta.plugins.utils.GUIUtils;
import org.bukkit.entity.Player;

public class PebGui extends PagedGui {
	public static final PagedGui.PageType MAIN_PAGE = new PagedGui.PageType("Main Page", 6 * 9);
	public static final PagedGui.PageType PLAYER_INFO_PAGE = new PagedGui.PageType("Player Information", 6 * 9);
	public static final PagedGui.PageType GAMEPLAY_OPTIONS_PAGE = new PagedGui.PageType("Gameplay Options", 6 * 9);
	public static final PagedGui.PageType TECHNICAL_OPTIONS_PAGE = new PagedGui.PageType("Technical Options", 6 * 9);
	public static final PagedGui.PageType TRADE_GUI_PAGE = new PagedGui.PageType("Trade Gui Settings", 6 * 9);
	public static final PagedGui.PageType INTERACTABLE_OPTIONS_PAGE = new PagedGui.PageType("Interactable Options", 6 * 9);
	public static final PagedGui.PageType SERVER_INFO_PAGE = new PagedGui.PageType("Server Information", 6 * 9);
	public static final PagedGui.PageType BOOK_SKINS_PAGE = new PagedGui.PageType("Book Skins", 6 * 9);
	public static final PagedGui.PageType PICKUP_AND_DISABLE_DROP_PAGE = new PagedGui.PageType("Pickup/Disable Drops", 6 * 9);
	public static final PagedGui.PageType GLOWING_PAGE = new PagedGui.PageType("Glowing Settings", 6 * 9);
	public static final PagedGui.PageType PARTIAL_PARTICLES_PAGE = new PagedGui.PageType("Particle Settings", 6 * 9);
	public static final PagedGui.PageType SOUND_CONTROLS_PAGE = new PagedGui.PageType("Sound Options", 6 * 9);
	public static final PagedGui.PageType SOUND_CATEGORIES_PAGE = new PagedGui.PageType("Sound Categories", 6 * 9);
	public static final PagedGui.PageType SOUND_OVERWORLD_PLOTS_PAGE = new PagedGui.PageType("Sound: Overworld/Plots", 6 * 9);

	public PebGui(Player player, PageType page) {
		super(player, GUIUtils.FILLER, page);
		registerPage(MAIN_PAGE, () -> new MainPage(this));
		registerPage(PLAYER_INFO_PAGE, () -> new PlayerInfoPage(this));
		registerPage(GAMEPLAY_OPTIONS_PAGE, () -> new GameplayOptionsPage(this));
		registerPage(TECHNICAL_OPTIONS_PAGE, () -> new TechnicalOptionsPage(this));
		registerPage(TRADE_GUI_PAGE, () -> new TradeGuiPage(this));
		registerPage(INTERACTABLE_OPTIONS_PAGE, () -> new InteractableOptionsPage(this));
		registerPage(SERVER_INFO_PAGE, () -> new ServerInfoPage(this));
		registerPage(BOOK_SKINS_PAGE, () -> new BookSkinsPage(this));
		registerPage(PICKUP_AND_DISABLE_DROP_PAGE, () -> new PickupAndDisableDropPage(this));
		registerPage(GLOWING_PAGE, () -> new GlowingOptionsPage(this));
		registerPage(PARTIAL_PARTICLES_PAGE, () -> new PartialParticlesPage(this));
		registerPage(SOUND_CONTROLS_PAGE, () -> new SoundOptionsPage(this));
		registerPage(SOUND_CATEGORIES_PAGE, () -> new SoundCategoriesPage(this));
		registerPage(SOUND_OVERWORLD_PLOTS_PAGE, () -> new SoundOverworldPlotsPage(this));
	}

	public PebGui(Player player) {
		this(player, MAIN_PAGE);
	}
}
