package com.playmonumenta.plugins.guis.classselection;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.overrides.YellowTesseractOverride;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ClassSelectionGui extends Gui {
	protected static final int COMMON_HEADER_ROW = 0;
	private static final int COMMON_REMAINING_SKILL_COLUMN = 8;
	private static final int COMMON_REMAINING_SPEC_COLUMN = 7;
	private static final int COMMON_REMAINING_ENHANCEMENTS_COLUMN = 6;

	private static final String UNLOCK_SPECS = "Quest103";
	private static final int UNLOCK_SPECS_MIN = 6;

	protected final boolean mFromYellowTess;
	protected final boolean mWasYellowTessOnCooldown;
	protected final boolean mGuiTextures;
	protected Page mPage;

	public ClassSelectionGui(Player player, boolean fromYellowTess) {
		super(player, 54, "Class Selection GUI");
		mFromYellowTess = fromYellowTess;
		mWasYellowTessOnCooldown = fromYellowTess && YellowTesseractOverride.getCooldown(player) > 0;
		mGuiTextures = GUIUtils.getGuiTextureObjective(player);
		mPage = new ClassPage(this);
	}

	@Override
	protected void setup() {
		mPage.setup();
		setRemainingCountIcons();
	}

	protected boolean isClassLocked(PlayerClass testClass) {
		if (
			testClass.mQuestReq != null
				&& !AbilityUtils.getEffectiveSpecs(mPlayer)
				&& ScoreboardUtils.getScoreboardValue(mPlayer, testClass.mQuestReq) < testClass.mQuestReqMin
		) {
			return true;
		}

		return isClassPermLocked(testClass);
	}

	protected boolean isClassPermLocked(PlayerClass testClass) {
		return testClass.mPermissionString != null
			&& !mPlayer.hasPermission(testClass.mPermissionString);
	}

	protected int remainingSkillPoints() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_SKILL);
	}

	protected boolean hasSpecsUnlocked() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, UNLOCK_SPECS) >= UNLOCK_SPECS_MIN;
	}

	protected boolean hasEffectiveSpecsUnlocked() {
		return hasSpecsUnlocked() || AbilityUtils.getEffectiveSpecs(mPlayer);
	}

	protected boolean hasSpecUnlocked(PlayerSpec spec) {
		return ScoreboardUtils.getScoreboardValue(mPlayer, spec.mSpecQuestScoreboard) >= 100;
	}

	protected int remainingSpecPoints() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_SPEC);
	}

	protected int remainingEnhanceCount() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE);
	}

	private void setRemainingCountIcons() {
		if (PlayerUtils.hasUnlockedRing(mPlayer)) {
			int currentEnhanceCount = remainingEnhanceCount();
			ItemStack summaryItem = GUIUtils.createBasicItem(
				currentEnhanceCount == 0 ? Material.BARRIER : Material.ENCHANTING_TABLE,
				"Enhancement Points",
				NamedTextColor.WHITE,
				false,
				"You have " + currentEnhanceCount
					+ " enhancement point" + (currentEnhanceCount == 1 ? "" : "s")
					+ " remaining.",
				NamedTextColor.LIGHT_PURPLE
			);
			GUIUtils.setGuiNbtTag(
				summaryItem,
				"texture",
				"cross_gui_total_en" + (currentEnhanceCount == 0 ? "_none" : ""),
				mGuiTextures
			);
			summaryItem.setAmount(currentEnhanceCount > 0 ? currentEnhanceCount : 1);
			setItem(COMMON_HEADER_ROW, COMMON_REMAINING_ENHANCEMENTS_COLUMN, summaryItem);
		}

		if (hasEffectiveSpecsUnlocked()) {
			int currentSpecCount = remainingSpecPoints();
			ItemStack summaryItem = GUIUtils.createBasicItem(
				currentSpecCount == 0 ? Material.BARRIER : Material.SAND,
				"Specialization Points",
				NamedTextColor.WHITE,
				false,
				"You have " + currentSpecCount
					+ " specialization point" + (currentSpecCount == 1 ? "" : "s")
					+ " remaining.",
				NamedTextColor.LIGHT_PURPLE
			);
			GUIUtils.setGuiNbtTag(
				summaryItem,
				"texture",
				"cross_gui_total_spec" + (currentSpecCount == 0 ? "_none" : ""),
				mGuiTextures
			);
			summaryItem.setAmount(currentSpecCount > 0 ? currentSpecCount : 1);
			setItem(COMMON_HEADER_ROW, COMMON_REMAINING_SPEC_COLUMN, summaryItem);
		}

		int currentSkillCount = remainingSkillPoints();
		ItemStack summaryItem = GUIUtils.createBasicItem(
			currentSkillCount == 0 ? Material.BARRIER : Material.GREEN_CONCRETE,
			"Skill Points",
			NamedTextColor.WHITE,
			false,
			"You have " + currentSkillCount + " skill point" + (currentSkillCount == 1 ? "" : "s") + " remaining.",
			NamedTextColor.LIGHT_PURPLE
		);
		GUIUtils.setGuiNbtTag(
			summaryItem,
			"texture",
			"cross_gui_total_sp" + (currentSkillCount == 0 ? "_none" : ""),
			mGuiTextures
		);
		summaryItem.setAmount(currentSkillCount > 0 ? currentSkillCount : 1);
		setItem(COMMON_HEADER_ROW, COMMON_REMAINING_SKILL_COLUMN, summaryItem);
	}

	protected void setAbilityIcon(
		int row,
		int column,
		PlayerClass displayedClass,
		@Nullable PlayerSpec displayedSpec, // Only set for spec abilities
		AbilityInfo<?> ability
	) {
		String desc = ability.getSimpleDescription();
		Component clickHere = Component.text(
			"Click here to remove your levels in this skill, and click the panes to the right to pick a level in this skill.",
			NamedTextColor.GRAY
		);
		Component lore = Component.text(desc == null ? "" : desc, NamedTextColor.WHITE);
		if (isClass(displayedClass, displayedSpec)) {
			if (desc != null) {
				lore = lore.append(Component.newline());
			}
			lore = lore.append(clickHere);
		}
		String quest216Message = ability.getQuest216Message();
		if (
			quest216Message != null
			&& mPlayer.getScoreboardTags().contains("Q216Distortion4Active")
		) {
			lore = lore
				.append(Component.newline())
				.append(Component.text(
					quest216Message,
					NamedTextColor.DARK_GRAY,
					TextDecoration.ITALIC,
					TextDecoration.OBFUSCATED
				));
		}
		Material item = ability.getDisplayItem();
		if (item == null) {
			item = displayedClass.mDisplayItem;
		}
		String name = ability.getDisplayName();
		if (name == null) {
			name = "";
		}
		ItemStack abilityIcon = GUIUtils.createBasicItem(item, 1, name,
			displayedClass.mClassColor, true, lore, 30, true);
		GUIUtils.setGuiNbtTag(abilityIcon, "texture", ability.getDisplayName(), mGuiTextures);
		setItem(row, column, abilityIcon).onClick(event -> {
			if (event.isShiftClick()) {
				return;
			}
			applyAbilityChosen(displayedClass, displayedSpec, ability, 0);
			update();
		});
	}

	protected void setLevelIcon(
		int row,
		int column,
		PlayerClass displayedClass,
		@Nullable PlayerSpec displayedSpec, // Only set for spec abilities
		AbilityInfo<?> ability,
		int level
	) {
		ItemStack levelItem;

		int currentLevel;
		String objective = ability.getScoreboard();
		if (objective == null) {
			currentLevel = 0;
		} else {
			currentLevel = ScoreboardUtils.getScoreboardValue(mPlayer, objective);
			if (currentLevel > 2) {
				currentLevel -= 2;
			}
		}

		Material newMat = currentLevel >= level ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
		levelItem = GUIUtils.createBasicItem(
			newMat,
			1,
			"Level " + level,
			displayedClass.mClassColor,
			true,
			ability.getDescription(level).color(NamedTextColor.WHITE),
			30,
			true
		);
		GUIUtils.setGuiNbtTag(
			levelItem,
			"texture",
			(displayedSpec == null ? "skill_select_sp_" : "spec_select_spec_")
				+ (currentLevel >= level ? "lit" : "unlit"),
			mGuiTextures
		);
		GUIUtils.setGuiNbtTag(levelItem, "skill", ability.getDisplayName(), mGuiTextures);
		GUIUtils.setGuiNbtTag(levelItem, "level", "" + level, mGuiTextures);

		setItem(row, column, levelItem).onClick(event -> {
			if (event.isShiftClick()) {
				return;
			}
			applyAbilityChosen(displayedClass, displayedSpec, ability, level);
			update();
		});
	}

	protected void setEnhanceIcon(
		int row,
		int column,
		PlayerClass displayedClass,
		AbilityInfo<?> ability
	) {
		ItemStack newItem;
		if (ability.getDescriptions().size() != 3) {
			newItem = GUIUtils.createBasicItem(Material.BARRIER, "No Option",
				displayedClass.mClassColor, true, "No Enhancement Created", NamedTextColor.WHITE);
			setItem(row, column, newItem);
			return;
		}

		boolean hasEnhancement;
		Material newMat;
		String scoreboard = ability.getScoreboard();
		switch (scoreboard == null ? 0 : ScoreboardUtils.getScoreboardValue(mPlayer, scoreboard)) {
			case 0 -> {
				newMat = Material.BARRIER;
				ItemStack disabledEn = GUIUtils.createBasicItem(newMat, 1,
					"Enhancement", displayedClass.mClassColor, true,
					Component.text("Cannot Select; Choose levels in the ability first. Description: ")
						.append(ability.getDescription(3)),
					30, true);
				GUIUtils.setGuiNbtTag(disabledEn, "texture", "skill_select_en_disabled", mGuiTextures);
				setItem(row, column, disabledEn);
				return;
			}
			case 1, 2 -> hasEnhancement = false;
			case 3, 4 -> hasEnhancement = true;
			default -> {
				newMat = Material.BARRIER;
				newItem = GUIUtils.createBasicItem(newMat, "Unknown Level",
					displayedClass.mClassColor, true, "Unknown level for ability.", NamedTextColor.WHITE);
				setItem(row, column, newItem);
				return;
			}
		}
		newMat = hasEnhancement ? Material.YELLOW_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE;
		newItem = GUIUtils.createBasicItem(newMat, 1,
			"Enhancement", displayedClass.mClassColor, true,
			ability.getDescription(3), 30, true);
		GUIUtils.setGuiNbtTag(
			newItem,
			"texture",
			hasEnhancement ? "skill_select_en_lit" : "skill_select_en_unlit",
			mGuiTextures
		);

		setItem(row, column, newItem)
			.onClick(event -> {
				if (event.isShiftClick()) {
					return;
				}
				applyEnhancementChosen(displayedClass, ability, !hasEnhancement);
				update();
			});
	}

	protected void applyAbilityChosen(
		PlayerClass displayedClass,
		@Nullable PlayerSpec displayedSpec,
		AbilityInfo<?> selectedAbility,
		int level
	) {
		if (!isClass(displayedClass, displayedSpec)) {
			return;
		}

		if (isClassLocked(displayedClass)) {
			level = 0;
		}

		String objective = selectedAbility.getScoreboard();
		if (objective == null) {
			return;
		}

		String remainingPointsObjective
			= displayedSpec == null ? AbilityUtils.REMAINING_SKILL : AbilityUtils.REMAINING_SPEC;

		int currentLevel = ScoreboardUtils.getScoreboardValue(mPlayer, objective);
		boolean hasEnhancement = currentLevel > 2;
		int enhancementOffset = hasEnhancement ? 2 : 0;
		// actualCurrentLevel is 0, 1, or 2 - the actual level of the ability before any changes
		int actualCurrentLevel = currentLevel - enhancementOffset;
		if (level == 0) {
			// Remove the ability
			if (hasEnhancement) {
				// Remove the enhancement
				int currentEnhancement = ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE);
				ScoreboardUtils.setScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE, currentEnhancement + 1);
			}
			ScoreboardUtils.setScoreboardValue(mPlayer, objective, 0);
			int currentCount = ScoreboardUtils.getScoreboardValue(mPlayer, remainingPointsObjective);
			ScoreboardUtils.setScoreboardValue(mPlayer, remainingPointsObjective, currentCount + (actualCurrentLevel - level));
		} else if (level < actualCurrentLevel) {
			// Level clicked is lower than level existing - remove levels down to clicked level
			ScoreboardUtils.setScoreboardValue(mPlayer, objective, enhancementOffset + level);
			int currentCount = ScoreboardUtils.getScoreboardValue(mPlayer, remainingPointsObjective);
			ScoreboardUtils.setScoreboardValue(mPlayer, remainingPointsObjective, currentCount + (actualCurrentLevel - level));
		} else if (level > actualCurrentLevel) {
			// Level clicked is higher than level existing - upgrade to clicked level if enough points
			int currentCount = ScoreboardUtils.getScoreboardValue(mPlayer, remainingPointsObjective);
			if (currentCount >= level - actualCurrentLevel) {
				// can upgrade
				ScoreboardUtils.setScoreboardValue(mPlayer, remainingPointsObjective, currentCount - (level - actualCurrentLevel));
				ScoreboardUtils.setScoreboardValue(mPlayer, objective, enhancementOffset + level);
			} else if (displayedSpec == null) {
				mPlayer.sendMessage("You don't have enough skill points to select this skill!");
				return;
			} else {
				mPlayer.sendMessage("You don't have enough specialization points to select this skill!");
				return;
			}
		}
		updatePlayerAbilities();
	}

	protected void applyEnhancementChosen(
		PlayerClass displayedClass,
		AbilityInfo<?> selectedAbility,
		boolean add
	) {
		if (!isClass(displayedClass, null)) {
			return;
		}

		if (isClassLocked(displayedClass)) {
			add = false;
		}

		String objective = selectedAbility.getScoreboard();
		if (objective == null) {
			return;
		}

		int currentLevel = ScoreboardUtils.getScoreboardValue(mPlayer, objective);
		if (add) {
			// Clear ability data
			int currentCount = ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE);
			// We don't want to assign the ability if we don't have points
			if (currentCount > 0) {
				ScoreboardUtils.setScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE, currentCount - 1);
				ScoreboardUtils.setScoreboardValue(mPlayer, objective, currentLevel + 2);
			} else {
				mPlayer.sendMessage("You don't have enough enhancement points to select this enhancement!");
				return;
			}
		} else {
			// Level clicked is lower than level existing
			ScoreboardUtils.setScoreboardValue(mPlayer, objective, currentLevel - 2);
			int currentCount = ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE);
			ScoreboardUtils.setScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE, currentCount + 1);
		}

		updatePlayerAbilities();
	}

	protected boolean hasClass() {
		return AbilityUtils.getClassNum(mPlayer) != 0;
	}

	protected boolean hasSpec() {
		return AbilityUtils.getSpecNum(mPlayer) != 0;
	}

	protected boolean isClass(PlayerClass displayedClass, @Nullable PlayerSpec displayedSpec) {
		boolean isThisClass = displayedClass.mClass == AbilityUtils.getClassNum(mPlayer);
		if (isThisClass && displayedSpec != null) {
			isThisClass = displayedSpec.mSpecialization == AbilityUtils.getSpecNum(mPlayer);
		}
		return isThisClass;
	}

	protected void updatePlayerAbilities() {
		if (AbilityManager.getManager() != null) {
			AbilityManager.getManager().updatePlayerAbilities(mPlayer, true);
			updateYellowTessCooldown();
		}
	}

	protected void updateYellowTessCooldown() {
		if (
			mFromYellowTess
			&& !ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.RESIST_5)
		) {
			YellowTesseractOverride.setCooldown(mPlayer, 5);
			if (mWasYellowTessOnCooldown) {
				Plugin.getInstance().mEffectManager
					.addEffect(mPlayer, "YellowTessSilence", new AbilitySilence(30 * 20));
			}
		}
	}
}
