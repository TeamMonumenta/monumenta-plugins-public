package com.playmonumenta.plugins.guis.classselection;

import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.GUIUtils;
import org.bukkit.inventory.ItemStack;

public abstract class Page {
	protected static final int BOTTOM = 5;
	protected static final int LEFT = 0;
	protected static final int RIGHT = 8;
	protected static final int COMMON_BACK_COLUMN = 0;
	protected static final int COMMON_SUMMARY_COLUMN = 4;
	protected static final MonumentaClasses mClasses = new MonumentaClasses();

	protected final ClassSelectionGui mGui;

	public Page(ClassSelectionGui gui) {
		mGui = gui;
	}

	protected abstract void setup();

	protected void setHeaderIcon(ItemStack summaryItem) {
		mGui.setItem(ClassSelectionGui.COMMON_HEADER_ROW, COMMON_SUMMARY_COLUMN, summaryItem);
	}

	protected GuiItem setBackIcon(ItemStack backIcon) {
		return mGui.setItem(ClassSelectionGui.COMMON_HEADER_ROW, COMMON_BACK_COLUMN, backIcon);
	}

	protected void setGuiIdentifier(String guiIdentifier) {
		mGui.setItem(
			BOTTOM,
			LEFT,
			GUIUtils.createGuiIdentifierItem(guiIdentifier + "_l", mGui.mGuiTextures)
		);
		mGui.setItem(
			BOTTOM,
			RIGHT,
			GUIUtils.createGuiIdentifierItem(guiIdentifier + "_r", mGui.mGuiTextures)
		);
	}
}
