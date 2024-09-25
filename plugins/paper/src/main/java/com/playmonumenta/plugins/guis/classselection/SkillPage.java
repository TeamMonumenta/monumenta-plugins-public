package com.playmonumenta.plugins.guis.classselection;

import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SkillPage extends Page {
	protected final PlayerClass mClass;

	public SkillPage(ClassSelectionGui gui, PlayerClass displayedClass) {
		super(gui);
		mClass = displayedClass;
	}

	@Override
	protected void setup() {
		boolean hasRingAccess = PlayerUtils.hasUnlockedRing(mGui.mPlayer);

		// Make abilities
		int skillIndex = 0;
		for (AbilityInfo<?> ability : mClass.mAbilities) {
			int row = Math.floorDiv(skillIndex, 2) + 1;
			int column;
			if (Math.floorMod(skillIndex, 2) == 0) {
				column = hasRingAccess ? 0 : 1;
			} else {
				column = 5;
			}

			mGui.setAbilityIcon(row, column, mClass, null, ability);
			mGui.setLevelIcon(row, column + 1, mClass, null, ability, 1);
			mGui.setLevelIcon(row, column + 2, mClass, null, ability, 2);
			if (hasRingAccess) {
				mGui.setEnhanceIcon(row, column + 3, mClass, ability);
			}

			skillIndex++;
		}

		// Specs
		if (mGui.hasEffectiveSpecsUnlocked()) {
			addSpecItem(2, mClass.mSpecOne);
			addSpecItem(6, mClass.mSpecTwo);
		}

		// Summary
		setHeaderIcon(GUIUtils.createBasicItem(
			mClass.mDisplayItem, mClass.mClassName + " Class Skills", NamedTextColor.WHITE, false,
			"Pick your skills and, if unlocked, your specialization.", NamedTextColor.LIGHT_PURPLE
		));

		// Back button
		ItemStack backButton = GUIUtils.createBasicItem(Material.ARROW, "Back",
			NamedTextColor.GRAY, false, "Return to the class selection page.", NamedTextColor.GRAY);
		GUIUtils.setGuiNbtTag(backButton, "texture", "skill_select_back", mGui.mGuiTextures);
		setBackIcon(backButton).onClick(event -> {
			if (event.isShiftClick()) {
				return;
			}
			mGui.mPage = new ClassPage(mGui);
			mGui.update();
		});

		// Possibly create reset spec item
		boolean isSpecOne = mGui.isClass(mClass, mClass.mSpecOne);
		boolean isSpecTwo = mGui.isClass(mClass, mClass.mSpecTwo);
		if (isSpecOne || isSpecTwo) {
			ItemStack specItem = GUIUtils.createBasicItem(
				Material.RED_BANNER, "Reset Your Specialization", NamedTextColor.WHITE, false,
				"Click here to reset your specialization to select a new one.", NamedTextColor.LIGHT_PURPLE
			);
			GUIUtils.setGuiNbtTag(specItem, "texture", "cross_gui_reset_spec", mGui.mGuiTextures);
			GUIUtils.setGuiNbtTag(
				specItem,
				"Spec",
				(isSpecOne ? mClass.mSpecOne : mClass.mSpecTwo).mSpecName,
				mGui.mGuiTextures
			);
			mGui.setItem(BOTTOM, 4, specItem)
				.onClick(event -> {
					if (event.isShiftClick()) {
						return;
					}
					AbilityUtils.resetSpec(mGui.mPlayer);
					mGui.updateYellowTessCooldown();
					mGui.mPage = new ClassPage(mGui);
					mGui.update();
				});
		}

		// Set gui identifier
		setGuiIdentifier(hasRingAccess ? "gui_class_2_2" : "gui_class_2_1");
	}

	protected void addSpecItem(
		int column,
		PlayerSpec spec
	) {
		if (!mGui.hasEffectiveSpecsUnlocked() && !mGui.hasSpecUnlocked(spec)) {
			// Not unlocked
			ItemStack specItem = GUIUtils.createBasicItem(
				Material.BARRIER,
				"Unknown",
				mClass.mClassColor,
				false,
				"You haven't unlocked this specialization yet.",
				NamedTextColor.WHITE
			);
			mGui.setItem(BOTTOM, column, specItem);
			return;
		}

		boolean hasSpec = mGui.hasSpec();
		boolean isSpec = mGui.isClass(mClass, spec);

		if (mGui.isClass(mClass, null) && hasSpec && !isSpec) {
			// Unlocked, but using other spec
			ItemStack specItem = GUIUtils.createBasicItem(
				Material.BARRIER,
				spec.mSpecName,
				mClass.mClassColor,
				false,
				"Click to view this specialization.",
				NamedTextColor.WHITE
			);
			mGui.setItem(BOTTOM, column, specItem)
				.onClick(event -> {
					if (event.isShiftClick()) {
						return;
					}
					mGui.mPage = new SpecPage(mGui, mClass, spec);
					mGui.update();
				});
			return;
		}

		// Unlocked and possibly using this spec
		String lore;
		if (isSpec) {
			lore = "Click to view your specialization skills.";
		} else if (hasSpec) {
			lore = "Click to choose this specialization!";
		} else {
			lore = "Click to view this specialization.";
		}
		ItemStack specItem = GUIUtils.createBasicItem(
			spec.mDisplayItem,
			spec.mSpecName,
			mClass.mClassColor,
			false,
			lore,
			NamedTextColor.WHITE
		);
		ItemMeta newMeta = specItem.getItemMeta();
		GUIUtils.splitLoreLine(
			newMeta,
			"Description: " + spec.mDescription,
			NamedTextColor.YELLOW,
			30,
			false
		);
		if (spec.mPassiveName != null) {
			GUIUtils.splitLoreLine(
				newMeta,
				spec.mPassiveName + " (Passive): " + spec.mPassiveDescription,
				NamedTextColor.GREEN,
				30,
				false
			);
		}
		specItem.setItemMeta(newMeta);
		mGui.setItem(BOTTOM, column, specItem)
			.onClick(event -> {
				if (event.isShiftClick()) {
					return;
				}
				if (mGui.isClassLocked(mClass)) {
					mGui.update();
					return;
				}
				if (mGui.isClass(mClass, null) && !mGui.hasSpec()) {
					ScoreboardUtils.setScoreboardValue(
						mGui.mPlayer,
						AbilityUtils.SCOREBOARD_SPEC_NAME,
						spec.mSpecialization
					);
					mGui.updatePlayerAbilities();
					MonumentaNetworkRelayIntegration.refreshPlayer(mGui.mPlayer);
				}
				mGui.mPage = new SpecPage(mGui, mClass, spec);
				mGui.update();
			});
	}
}
