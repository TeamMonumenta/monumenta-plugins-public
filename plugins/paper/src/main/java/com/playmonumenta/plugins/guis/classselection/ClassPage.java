package com.playmonumenta.plugins.guis.classselection;

import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.guis.AbilityTriggersGui;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.utils.DescriptionUtils.ACTION_COMPLETED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.ACTION_SELECT;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class ClassPage extends Page {
	public ClassPage(ClassSelectionGui gui) {
		super(gui);
	}

	@Override
	protected void setup() {
		// Get the current class, if they have one.
		int currentClass = AbilityUtils.getClassNum(mGui.mPlayer);
		PlayerClass playerClass = null;
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (currentClass == oneClass.mClass) {
				playerClass = oneClass;
				break;
			}
		}

		int classIndex = 0;
		// If classless, show all as the items. otherwise, barrier the choices but keep names
		for (PlayerClass oneClass : mClasses.mClasses) {
			setClassIcon(
				2 + Math.floorDiv(classIndex, 4),
				1 + Math.floorMod(classIndex, 4) * 2,
				oneClass,
				playerClass != null && playerClass != oneClass,
				playerClass == oneClass
			);

			classIndex++;
		}

		Component mainMenuDescription = new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("Choose a class and assign your")
			.addLine("*Skill Points* here.").styles(ClassSelectionGui.SKILL_POINT_COLOR)
			.addLine()
			.addLine("You can reset your class and")
			.addLine("reassign your points at any time,")
			.addLine("at no cost.")
			.addDashedLine()
			.get();
		Component mainMenuName = DescriptionUtils.centeredComponent(mainMenuDescription, "Main Menu", WHITE, true);
		ItemStack mainMenuItem = GUIUtils.createBasicItem(Material.SCUTE, 1, mainMenuName, mainMenuDescription, 99, true);

		GUIUtils.setGuiNbtTag(mainMenuItem, "texture", "class_select_main_menu", mGui.mGuiTextures);
		setHeaderIcon(mainMenuItem);

		if (mGui.hasClass()) {
			Component resetDescription = new FormattedDescriptionBuilder<>()
				.addDashedLine()
				.addLine("Reset your class and all assigned")
				.addLine("points, allowing you to pick a")
				.addLine("different class.")
				.addDashedLine()
				.addAction("Click to reset your class.", ACTION_SELECT)
				.get();
			Component resetName = DescriptionUtils.centeredComponent(resetDescription, "Reset Class", WHITE, true);
			ItemStack resetItem = GUIUtils.createBasicItem(Material.CYAN_BED, 1, resetName, resetDescription, 99, true);
			GUIUtils.setGuiNbtTag(
				resetItem,
				"texture",
				"class_select_reset_class",
				mGui.mGuiTextures
			);
			if (playerClass != null) {
				GUIUtils.setGuiNbtTag(resetItem, "Class", playerClass.mClassName, mGui.mGuiTextures);
			}
			mGui.setItem(5, 2, resetItem)
				.onClick(event -> {
					if (event.isShiftClick()) {
						return;
					}
					AbilityUtils.resetClass(mGui.mPlayer);
					mGui.updateYellowTessCooldown();
					mGui.update();
				});

			// Possibly create reset spec item
			int spec = AbilityUtils.getSpecNum(mGui.mPlayer);
			if (mGui.hasSpec()) {
				Component specResetDescription = new FormattedDescriptionBuilder<>()
					.addDashedLine()
					.addLine("Reset your class specialization, allowing")
					.addLine("you to pick a different one.")
					.addDashedLine()
					.addAction("Click to reset your specialization.", ACTION_SELECT)
					.get();
				Component specResetName = DescriptionUtils.centeredComponent(specResetDescription, "Reset Specialization", WHITE, true);
				ItemStack specResetItem = GUIUtils.createBasicItem(Material.RED_BANNER, 1, specResetName, specResetDescription, 99, true);
				GUIUtils.setGuiNbtTag(specResetItem, "texture", "cross_gui_reset_spec", mGui.mGuiTextures);
				if (playerClass != null) {
					GUIUtils.setGuiNbtTag(
						specResetItem,
						"Spec",
						(spec == playerClass.mSpecOne.mSpecialization ? playerClass.mSpecOne : playerClass.mSpecTwo).mSpecName,
						mGui.mGuiTextures
					);
				}
				mGui.setItem(5, 4, specResetItem)
					.onClick(event -> {
						if (event.isShiftClick()) {
							return;
						}
						AbilityUtils.resetSpec(mGui.mPlayer);
						mGui.updateYellowTessCooldown();
						mGui.update();
					});
			}

			Component triggersDescription = new FormattedDescriptionBuilder<>()
				.addDashedLine()
				.addLine("View and change which keybinds")
				.addLine("are used to cast abilities.")
				.addDashedLine()
				.addAction("Click to view ability triggers.", ACTION_SELECT)
				.get();
			Component triggersName = DescriptionUtils.centeredComponent(triggersDescription, "Change Ability Triggers", WHITE, true);
			ItemStack triggersItem = GUIUtils.createBasicItem(Material.JIGSAW, 1, triggersName, triggersDescription, 99, true);
			GUIUtils.setGuiNbtTag(triggersItem, "texture", "class_select_trigger", mGui.mGuiTextures);
			mGui.setItem(5, 6, triggersItem)
				.onClick(event -> {
					if (event.isShiftClick()) {
						return;
					}

					mGui.mPlayer.playSound(mGui.mPlayer, Sound.BLOCK_TRIAL_SPAWNER_EJECT_ITEM, SoundCategory.PLAYERS, 1f, 1f);
					mGui.mPlayer.playSound(mGui.mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);

					new AbilityTriggersGui(mGui.mPlayer, true).open();
				});
		}
		// Set gui identifier
		setGuiIdentifier("gui_class_1");
	}

	private void setClassIcon(
		int row,
		int column,
		PlayerClass classToItemize,
		boolean otherChosen,
		boolean chosen
	) {
		if (mGui.isClassLocked(classToItemize)) {
			mGui.setItem(
				row,
				column,
				GUIUtils.createBasicItem(
					Material.BARRIER,
					classToItemize.mClassName,
					classToItemize.mClassColor,
					true,
					"You don't have the requirements to choose this class.",
					NamedTextColor.RED
				)
			);
			return;
		}

		Component description = classToItemize.getDescription(mGui.mPlayer).appendNewline().appendSpace();

		Component instruction;
		if (chosen) {
			instruction = DescriptionUtils.centeredComponent(description, "Click to view your skills!", ACTION_COMPLETED);
		} else if (otherChosen) {
			instruction = DescriptionUtils.centeredComponent(description, "Click to view %s's skills!".formatted(classToItemize.mClassName), ACTION_SELECT);
		} else {
			instruction = DescriptionUtils.centeredComponent(description, "Click to choose %s!".formatted(classToItemize.mClassName), ACTION_SELECT);
		}
		description = description.append(instruction);

		Component name = DescriptionUtils.centeredComponent(description, classToItemize.mClassName, classToItemize.mClassColor, true);

		ItemStack classItem = GUIUtils.createBasicItem(
			otherChosen ? Material.BARRIER : classToItemize.mDisplayItem, 1,
			name, description, 99, true);

		mGui.setItem(
				row,
				column,
				classItem
			)
			.onClick(event -> {
				if (event.isShiftClick()) {
					return;
				}
				if (mGui.isClassLocked(classToItemize)) {
					mGui.update();
					return;
				}
				if (!mGui.hasClass()) {
					ScoreboardUtils.setScoreboardValue(
						mGui.mPlayer,
						AbilityUtils.SCOREBOARD_CLASS_NAME,
						classToItemize.mClass
					);
					mGui.updatePlayerAbilities();
					MonumentaNetworkRelayIntegration.refreshPlayer(mGui.mPlayer);

					mGui.mPlayer.playSound(mGui.mPlayer, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 0.5f, 1.5f);
					mGui.mPlayer.playSound(mGui.mPlayer, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1f, 0.5f);
				} else {
					mGui.mPlayer.playSound(mGui.mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
				}
				mGui.mPage = new SkillPage(mGui, classToItemize);
				mGui.update();
			});
	}
}
