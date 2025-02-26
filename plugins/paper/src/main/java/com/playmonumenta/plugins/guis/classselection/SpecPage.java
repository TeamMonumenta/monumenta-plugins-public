package com.playmonumenta.plugins.guis.classselection;

import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.utils.GUIUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.inventory.ItemStack;

public class SpecPage extends Page {
	protected final PlayerClass mClass;
	protected final PlayerSpec mSpec;

	public SpecPage(ClassSelectionGui gui, PlayerClass displayedClass, PlayerSpec displayedSpec) {
		super(gui);
		mClass = displayedClass;
		mSpec = displayedSpec;
	}

	@Override
	protected void setup() {
		// Make abilities
		int skillIndex = 0;
		for (AbilityInfo<?> ability : mSpec.mAbilities) {
			int row = skillIndex + 2;
			int column = skillIndex + 2;

			mGui.setAbilityIcon(row, column, mClass, mSpec, ability);
			mGui.setLevelIcon(row, column + 1, mClass, mSpec, ability, 1);
			mGui.setLevelIcon(row, column + 2, mClass, mSpec, ability, 2);

			skillIndex++;
		}

		// Summary
		setHeaderIcon(GUIUtils.createBasicItem(
			mSpec.mDisplayItem, mSpec.mSpecName + " Specialization Skills", NamedTextColor.WHITE, false,
			"Pick your specialization skills.", NamedTextColor.LIGHT_PURPLE
		));

		// Back button
		ItemStack backButton = GUIUtils.createBasicItem(Material.ARROW, "Back",
			NamedTextColor.GRAY, false, "Return to the skill selection page.", NamedTextColor.GRAY);
		GUIUtils.setGuiNbtTag(backButton, "texture", "spec_select_back", mGui.mGuiTextures);
		setBackIcon(backButton).onClick(event -> {
			if (event.isShiftClick()) {
				return;
			}
			mGui.mPage = new SkillPage(mGui, mClass);
			mGui.update();

			mGui.mPlayer.playSound(mGui.mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
		});

		// Set gui identifier
		setGuiIdentifier("gui_class_3");
	}
}
