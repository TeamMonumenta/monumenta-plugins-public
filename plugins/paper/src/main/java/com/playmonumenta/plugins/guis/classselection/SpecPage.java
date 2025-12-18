package com.playmonumenta.plugins.guis.classselection;

import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.utils.DescriptionUtils.ACTION_SELECT;
import static com.playmonumenta.plugins.utils.DescriptionUtils.LIGHT_GREY;

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
		Component description = (mSpec == mClass.mSpecOne ? mClass.getSpecOneDescription(mGui.mPlayer) : mClass.getSpecTwoDescription(mGui.mPlayer));
		Component name = DescriptionUtils.centeredComponent(description, mSpec.mSpecName, mClass.mClassColor, true);

		setHeaderIcon(GUIUtils.createBasicItem(mSpec.mDisplayItem, 1, name, description, 99, true));

		// Back button
		Component backDescription = new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addAction("Click to go back.", ACTION_SELECT)
			.get();
		Component backName = DescriptionUtils.centeredComponent(backDescription, "Return", LIGHT_GREY, true);
		ItemStack backButton = GUIUtils.createBasicItem(Material.ARROW, 1, backName, backDescription, 99, true);
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
