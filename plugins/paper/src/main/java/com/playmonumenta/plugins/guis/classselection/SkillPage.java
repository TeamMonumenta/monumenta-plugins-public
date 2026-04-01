package com.playmonumenta.plugins.guis.classselection;

import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.inventory.ItemStack;

import static com.playmonumenta.plugins.utils.DescriptionUtils.ACTION_COMPLETED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.ACTION_SELECT;
import static com.playmonumenta.plugins.utils.DescriptionUtils.LIGHT_GREY;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

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

		if (hasRingAccess) {
			mGui.setAbilityIcon(0, 1, mClass, null, mClass.mUltimate);
			mGui.setUltimateIcon(0, 2, mClass, mClass.mUltimate);
		}

		// Summary
		Component description = mClass.getDescription(mGui.mPlayer).appendNewline().appendSpace();

		Component name = DescriptionUtils.centeredComponent(description, mClass.mClassName, mClass.mClassColor, true);

		setHeaderIcon(GUIUtils.createBasicItem(mClass.mDisplayItem, 1, name, description, 99, true));

		// Back button
		Component backDescription = new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addAction("Click to go back.", ACTION_SELECT)
			.get();
		Component backName = DescriptionUtils.centeredComponent(backDescription, "Return", LIGHT_GREY, true);
		ItemStack backButton = GUIUtils.createBasicItem(Material.ARROW, 1, backName, backDescription, 99, true);
		GUIUtils.setGuiNbtTag(backButton, "texture", "skill_select_back", mGui.mGuiTextures);
		setBackIcon(backButton).onClick(event -> {
			if (event.isShiftClick()) {
				return;
			}
			mGui.mPage = new ClassPage(mGui);
			mGui.update();

			mGui.mPlayer.playSound(mGui.mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
		});

		// Possibly create reset spec item
		boolean isSpecOne = mGui.isClass(mClass, mClass.mSpecOne);
		boolean isSpecTwo = mGui.isClass(mClass, mClass.mSpecTwo);
		if (isSpecOne || isSpecTwo) {
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
			GUIUtils.setGuiNbtTag(
				specResetItem,
				"Spec",
				(isSpecOne ? mClass.mSpecOne : mClass.mSpecTwo).mSpecName,
				mGui.mGuiTextures
			);
			mGui.setItem(BOTTOM, 4, specResetItem)
				.onClick(event -> {
					if (event.isShiftClick()) {
						return;
					}
					AbilityUtils.resetSpec(mGui.mPlayer);
					mGui.updateYellowTessCooldown();
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

		boolean isClass = mGui.isClass(mClass, null);
		boolean hasSpec = mGui.hasSpec();
		boolean isSpec = mGui.isClass(mClass, spec);
		boolean otherSpec = hasSpec && !isSpec;

		Component description = (spec == mClass.mSpecOne ? mClass.getSpecOneDescription(mGui.mPlayer) : mClass.getSpecTwoDescription(mGui.mPlayer))
				.appendNewline().appendSpace();

		// Unlocked and possibly using this spec
		Component instruction;
		if (isSpec) {
			instruction = DescriptionUtils.centeredComponent(description, "Click to view your skills!", ACTION_COMPLETED);
		} else if (isClass && !hasSpec) {
			instruction = DescriptionUtils.centeredComponent(description, "Click to choose %s!".formatted(spec.mSpecName), ACTION_SELECT);
		} else {
			instruction = DescriptionUtils.centeredComponent(description, "Click to view %s's skills!".formatted(spec.mSpecName), ACTION_SELECT);
		}
		description = description.append(instruction);

		Component name = DescriptionUtils.centeredComponent(description, spec.mSpecName, mClass.mClassColor, true);

		ItemStack specItem = GUIUtils.createBasicItem(
			otherSpec ? Material.BARRIER : spec.mDisplayItem, 1,
			name, description, 99, true
		);

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

					mGui.mPlayer.playSound(mGui.mPlayer, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.7f, 1.4f);
					mGui.mPlayer.playSound(mGui.mPlayer, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1f, 1f);
				} else {
					mGui.mPlayer.playSound(mGui.mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
				}
				mGui.mPage = new SpecPage(mGui, mClass, spec);
				mGui.update();
			});
	}

}
