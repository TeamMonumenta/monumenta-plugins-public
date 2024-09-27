package com.playmonumenta.plugins.guis.classselection;

import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.guis.AbilityTriggersGui;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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

		ItemStack summaryItem = GUIUtils.createBasicItem(
			Material.SCUTE,
			"Main Menu",
			NamedTextColor.WHITE,
			false,
			"Pick a class to view abilities within that class. You can reset your class at any time, with no consequences.",
			NamedTextColor.LIGHT_PURPLE
		);
		GUIUtils.setGuiNbtTag(summaryItem, "texture", "class_select_main_menu", mGui.mGuiTextures);
		setHeaderIcon(summaryItem);

		if (mGui.hasClass()) {
			ItemStack resetItem = GUIUtils.createBasicItem(
				Material.CYAN_BED,
				"Reset Your Class",
				NamedTextColor.WHITE,
				false,
				"Click here to reset your class, allowing access to other choices.",
				NamedTextColor.LIGHT_PURPLE
			);
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
				ItemStack specItem = GUIUtils.createBasicItem(
					Material.RED_BANNER,
					"Reset Your Specialization",
					NamedTextColor.WHITE,
					false,
					"Click here to reset your specialization, allowing access to choose either specialization.",
					NamedTextColor.LIGHT_PURPLE
				);
				GUIUtils.setGuiNbtTag(specItem, "texture", "cross_gui_reset_spec", mGui.mGuiTextures);
				if (playerClass != null) {
					GUIUtils.setGuiNbtTag(
						specItem,
						"Spec",
						(spec == playerClass.mSpecOne.mSpecialization ? playerClass.mSpecOne : playerClass.mSpecTwo).mSpecName,
						mGui.mGuiTextures
					);
				}
				mGui.setItem(5, 4, specItem)
					.onClick(event -> {
						if (event.isShiftClick()) {
							return;
						}
						AbilityUtils.resetSpec(mGui.mPlayer);
						mGui.updateYellowTessCooldown();
						mGui.update();
					});
			}

			ItemStack triggersItem = GUIUtils.createBasicItem(
				Material.JIGSAW,
				"Change Ability Triggers",
				NamedTextColor.WHITE,
				false,
				"Click here to change which key combinations are used to cast abilities.",
				NamedTextColor.LIGHT_PURPLE
			);
			GUIUtils.setGuiNbtTag(triggersItem, "texture", "class_select_trigger", mGui.mGuiTextures);
			mGui.setItem(5, 6, triggersItem)
				.onClick(event -> {
					if (event.isShiftClick()) {
						return;
					}
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
		if (mGui.isClassPermLocked(classToItemize)) {
			mGui.setItem(
				row,
				column,
				GUIUtils.createBasicItem(
					Material.BARRIER,
					classToItemize.mClassName,
					classToItemize.mClassColor,
					true,
					"This class is currently unavailable to you.",
					NamedTextColor.RED
				)
			);
			return;
		}

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

		if (otherChosen) {
			mGui.setItem(
				row,
				column,
				GUIUtils.createBasicItem(
					Material.BARRIER,
					classToItemize.mClassName,
					classToItemize.mClassColor,
					true,
					"Click to view skills.",
					NamedTextColor.GRAY
				)
			)
				.onClick(event -> {
					if (event.isShiftClick()) {
						return;
					}
					mGui.mPage = new SkillPage(mGui, classToItemize);
					mGui.update();
				});
			return;
		}

		ItemStack classItem = GUIUtils.createBasicItem(
			classToItemize.mDisplayItem,
			classToItemize.mClassName,
			classToItemize.mClassColor,
			true,
			chosen ? "Click to view your skills." : "Click to choose this class!",
			NamedTextColor.GRAY
		);

		if (classToItemize.mClassDescription != null) {
			ItemMeta newMeta = classItem.getItemMeta();
			GUIUtils.splitLoreLine(
				newMeta,
				"Description: " + classToItemize.mClassDescription,
				NamedTextColor.YELLOW,
				30,
				false
			);
			classItem.setItemMeta(newMeta);
		}

		if (classToItemize.mClassPassiveDescription != null && classToItemize.mClassPassiveName != null) {
			ItemMeta newMeta = classItem.getItemMeta();
			GUIUtils.splitLoreLine(
				newMeta,
				classToItemize.mClassPassiveName + " (Passive): " + classToItemize.mClassPassiveDescription,
				NamedTextColor.GREEN,
				30,
				false
			);
			classItem.setItemMeta(newMeta);
		}

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
				}
				mGui.mPage = new SkillPage(mGui, classToItemize);
				mGui.update();
			});
	}
}
