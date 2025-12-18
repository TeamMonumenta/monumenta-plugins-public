package com.playmonumenta.plugins.guis.classselection;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.overrides.YellowTesseractOverride;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.scoreboard;

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

	public static final Style SKILL_POINT_COLOR = Style.style(TextColor.color(0xD99F00));
	public static final Style SPEC_POINT_COLOR = Style.style(TextColor.color(0x4CC8D4));
	public static final Style ENHANCEMENT_POINT_COLOR = Style.style(TextColor.color(0xD934A2));

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
		return testClass.mQuestReq != null
			&& !AbilityUtils.getEffectiveSpecs(mPlayer)
			&& ScoreboardUtils.getScoreboardValue(mPlayer, testClass.mQuestReq).orElse(0) < testClass.mQuestReqMin;
	}

	protected int remainingSkillPoints() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_SKILL).orElse(0);
	}

	protected boolean hasSpecsUnlocked() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, UNLOCK_SPECS).orElse(0) >= UNLOCK_SPECS_MIN;
	}

	protected boolean hasEffectiveSpecsUnlocked() {
		return hasSpecsUnlocked() || AbilityUtils.getEffectiveSpecs(mPlayer);
	}

	protected boolean hasSpecUnlocked(PlayerSpec spec) {
		return ScoreboardUtils.getScoreboardValue(mPlayer, spec.mSpecQuestScoreboard).orElse(0) >= 100;
	}

	protected int remainingSpecPoints() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_SPEC).orElse(0);
	}

	protected int remainingEnhanceCount() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE).orElse(0);
	}

	private void setRemainingCountIcons() {
		if (PlayerUtils.hasUnlockedRing(mPlayer)) {
			int currentEnhanceCount = remainingEnhanceCount();

			Material material = currentEnhanceCount == 0 ? Material.BARRIER : Material.ENCHANTING_TABLE;

			Component description = new FormattedDescriptionBuilder<>()
				.addDashedLine()
				.addLine("You have %d out of %d *Enhancement*").styles(ENHANCEMENT_POINT_COLOR)
					.statValues(scoreboard(AbilityUtils.REMAINING_ENHANCE), scoreboard(AbilityUtils.TOTAL_ENHANCE))
				.addLine("*Points* remaining.").styles(ENHANCEMENT_POINT_COLOR)
				.addLine()
				.addLine("*Enhancement Points* boost your").styles(ENHANCEMENT_POINT_COLOR)
				.addLine("existing abilities even further.")
				.addLine()
				.addIfElse((a, p) -> ServerProperties.getAbilityEnhancementsEnabled(p),
					desc -> desc.addLine("Enhancements are currently *enabled*").styles(DescriptionUtils.GREEN),
					desc -> desc.addLine("Enhancements are currently *disabled*").styles(DescriptionUtils.RED))
				.addLine("in this region.")
				.addDashedLine()
				.get(mPlayer);

			Component name = DescriptionUtils.centeredComponent(description, "Enhancement Points", ENHANCEMENT_POINT_COLOR, true);

			ItemStack summaryItem = GUIUtils.createBasicItem(material, 1, name, description, 99, true);
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
			Material material = currentSpecCount == 0 ? Material.BARRIER : Material.SAND;

			Component description = new FormattedDescriptionBuilder<>()
				.addDashedLine()
				.addLine("You have %d out of %d *Specialization*").styles(SPEC_POINT_COLOR)
				.statValues(scoreboard(AbilityUtils.REMAINING_SPEC), scoreboard(AbilityUtils.TOTAL_SPEC))
				.addLine("*Points* remaining.").styles(SPEC_POINT_COLOR)
				.addLine()
				.addLine("*Specialization Points* are used for").styles(SPEC_POINT_COLOR)
				.addLine("your class specialization's abilities.")
				.addLine()
				.addIfElse((a, p) -> ServerProperties.getClassSpecializationsEnabled(p),
					desc -> desc.addLine("Specializations are currently *enabled*").styles(DescriptionUtils.GREEN),
					desc -> desc.addLine("Specializations are currently *disabled*").styles(DescriptionUtils.RED))
				.addLine("in this region.")
				.addDashedLine()
				.get(mPlayer);

			Component name = DescriptionUtils.centeredComponent(description, "Specialization Points", SPEC_POINT_COLOR, true);

			ItemStack summaryItem = GUIUtils.createBasicItem(material, 1, name, description, 99, true);
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

		Material material = currentSkillCount == 0 ? Material.BARRIER : Material.GREEN_CONCRETE;

		Component description = new FormattedDescriptionBuilder<>()
			.addDashedLine()
			.addLine("You have %d out of %d *Skill Points*").styles(SKILL_POINT_COLOR)
			.statValues(scoreboard(AbilityUtils.REMAINING_SKILL), scoreboard(AbilityUtils.TOTAL_LEVEL))
			.addLine("remaining.").styles(SKILL_POINT_COLOR)
			.addLine()
			.addLine("*Skill Points* are used to unlock new").styles(SKILL_POINT_COLOR)
			.addLine("abilities or upgrade existing ones.")
			.addDashedLine()
			.get(mPlayer);

		Component name = DescriptionUtils.centeredComponent(description, "Skill Points", SKILL_POINT_COLOR, true);

		ItemStack summaryItem = GUIUtils.createBasicItem(material, 1, name, description, 99, true);
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
		int currentLevel;
		if (ability.getScoreboard() != null) {
			currentLevel = ScoreboardUtils.getScoreboardValue(mPlayer, ability.getScoreboard()).orElse(0);
		} else {
			currentLevel = 0;
		}

		Component description = new FormattedDescriptionBuilder<>(() -> ability)
			.addDashedLine()
			.add((a, p) -> {
				var result = new FormattedDescriptionBuilder<>(() -> ability);

				if (displayedSpec != null && !ServerProperties.getClassSpecializationsEnabled(p)) {
					result.addStat("Current Level: *Disabled*").styles(DescriptionUtils.RED);
				} else {
					switch (currentLevel) {
						case 0 -> result.addStat("Current Level: *Not Selected*").styles(DescriptionUtils.DARK_GREY);
						case 1 -> result.addStat("Current Level: Level 1");
						case 2 -> result.addStat("Current Level: Level 2");
						case 3 -> result.addStat("Current Level: Level 1 *[Enhanced]*")
							.styles(ServerProperties.getAbilityEnhancementsEnabled(p) ? ENHANCEMENT_POINT_COLOR : DescriptionUtils.DISABLED);
						case 4 -> result.addStat("Current Level: Level 2 *[Enhanced]*")
							.styles(ServerProperties.getAbilityEnhancementsEnabled(p) ? ENHANCEMENT_POINT_COLOR : DescriptionUtils.DISABLED);
					}
				}
				return result.get(p);
			})
			.addCharmEffects()
			.addDashedLine()
			.addIf((a, p) -> currentLevel != 0, desc -> desc.addAction("Click to remove this ability.", DescriptionUtils.ACTION_SELECT))
			.get(mPlayer);

		String quest216Message = ability.getQuest216Message();
		Component quest216Component;
		if (quest216Message != null && mPlayer.getScoreboardTags().contains("Q216Distortion4Active")) {
			quest216Component = Component.text(quest216Message,
				NamedTextColor.DARK_GRAY,
				TextDecoration.ITALIC,
				TextDecoration.OBFUSCATED
			);
			description = description.appendNewline().appendSpace().append(quest216Component);
		} else {
			quest216Component = null;
		}
		Material item = ability.getDisplayItem();
		if (item == null) {
			item = displayedClass.mDisplayItem;
		}
		Component name = Component.empty();
		if (ability.getDisplayName() != null) {
			name = DescriptionUtils.centeredComponent(description, ability.getDisplayName(), displayedClass.mClassColor, true);
		}
		ItemStack abilityIcon = GUIUtils.createBasicItem(item, 1, name, description, 99, true);
		GUIUtils.setGuiNbtTag(abilityIcon, "texture", ability.getDisplayName(), mGuiTextures);
		setItem(row, column, abilityIcon).onClick(event -> {
			if (event.isShiftClick()) {
				return;
			}
			if (quest216Component != null) {
				mPlayer.sendMessage(quest216Component);
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
			currentLevel = ScoreboardUtils.getScoreboardValue(mPlayer, objective).orElse(0);
			if (currentLevel > 2) {
				currentLevel -= 2;
			}
		}

		boolean canSelect;
		if (displayedSpec == null) {
			int remainingSkill = ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_SKILL).orElse(0);
			canSelect = level - currentLevel <= remainingSkill;
		} else {
			int remainingSpec = ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_SPEC).orElse(0);
			canSelect = level - currentLevel <= remainingSpec;
		}

		Component instruction;
		if (!isClass(displayedClass, displayedSpec)) {
			instruction = Component.empty();
		} else if (!canSelect) {
			instruction = DescriptionUtils.actionLine("Cannot select ability!", DescriptionUtils.ACTION_DENIED).appendNewline().appendSpace()
				.append(DescriptionUtils.actionLine("Not enough points!", DescriptionUtils.ACTION_DENIED));
		} else if (currentLevel < level) {
			instruction = DescriptionUtils.actionLine("Click to select!", DescriptionUtils.ACTION_SELECT);
		} else {
			instruction = DescriptionUtils.actionLine("Ability already selected.", DescriptionUtils.ACTION_COMPLETED);
		}

		Component lore = ability.getDescription(level, mPlayer, true);
		lore = lore.appendNewline().append(instruction);

		Component name = Component.text(Objects.requireNonNull(ability.getDisplayName()), displayedClass.mClassColor)
			.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)
			.append(Component.text(" [Lv. %s]".formatted(level), DescriptionUtils.GOLD).decoration(TextDecoration.BOLD, false));

		Material newMat = currentLevel >= level ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
		levelItem = GUIUtils.createBasicItem(newMat, 1, name, lore, 99, true);

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
			Bukkit.getScheduler().runTask(mPlugin, this::update);
		});
	}

	protected void setEnhanceIcon(
		int row,
		int column,
		PlayerClass displayedClass,
		AbilityInfo<?> ability
	) {
		ItemStack newItem;

		boolean isDisabled;
		boolean hasEnhancement;
		Material newMat;
		String guiTexture;
		String scoreboard = ability.getScoreboard();
		switch (scoreboard == null ? 0 : ScoreboardUtils.getScoreboardValue(mPlayer, scoreboard).orElse(0)) {
			case 0 -> {
				isDisabled = true;
				hasEnhancement = false;
				newMat = Material.BARRIER;
				guiTexture = "skill_select_en_disabled";
			}
			case 1, 2 -> {
				isDisabled = false;
				hasEnhancement = false;
				newMat = Material.ORANGE_STAINED_GLASS_PANE;
				guiTexture = "skill_select_en_unlit";
			}
			case 3, 4 -> {
				isDisabled = false;
				hasEnhancement = true;
				newMat = Material.YELLOW_STAINED_GLASS_PANE;
				guiTexture = "skill_select_en_lit";
			}
			default -> {
				newMat = Material.BARRIER;
				newItem = GUIUtils.createBasicItem(newMat, "Unknown Level",
					displayedClass.mClassColor, true, "Unknown level for ability.", NamedTextColor.WHITE);
				setItem(row, column, newItem);
				return;
			}
		}

		Component instruction;
		if (!isClass(displayedClass, null)) {
			instruction = Component.empty();
		} else if (hasEnhancement) {
			instruction = DescriptionUtils.actionLine("Enhancement already selected.", DescriptionUtils.ACTION_COMPLETED).appendNewline().appendSpace()
				.append(DescriptionUtils.actionLine("Click to deselect!", DescriptionUtils.ACTION_SELECT));
		} else if (ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE).orElse(0) == 0) {
			instruction = DescriptionUtils.actionLine("Cannot select enhancement!", DescriptionUtils.ACTION_DENIED).appendNewline().appendSpace()
				.append(DescriptionUtils.actionLine("Not enough points!", DescriptionUtils.ACTION_DENIED));
		} else if (isDisabled) {
			instruction = DescriptionUtils.actionLine("Cannot select enhancement!", DescriptionUtils.ACTION_DENIED).appendNewline().appendSpace()
				.append(DescriptionUtils.actionLine("Ability must be selected first!", DescriptionUtils.ACTION_DENIED));
		} else {
			instruction = DescriptionUtils.actionLine("Click to select!", DescriptionUtils.ACTION_SELECT);
		}

		Component description = ability.getDescription(3, mPlayer, true).appendNewline().appendSpace();
		description = description.append(instruction);

		Component name = Component.text(Objects.requireNonNull(ability.getDisplayName()), displayedClass.mClassColor)
			.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)
			.append(Component.text(" [Enhancement]", DescriptionUtils.GOLD).decoration(TextDecoration.BOLD, false));

		newItem = GUIUtils.createBasicItem(newMat, 1, name, description, 99, true);
		GUIUtils.setGuiNbtTag(
			newItem,
			"texture",
			guiTexture,
			mGuiTextures
		);


		setItem(row, column, newItem)
			.onClick(event -> {
				if (event.isShiftClick()) {
					return;
				}
				if (isDisabled) {
					mPlayer.playSound(mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
					mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 0.8f, 0.75f);
					return;
				}
				applyEnhancementChosen(displayedClass, ability, !hasEnhancement);
				Bukkit.getScheduler().runTask(mPlugin, this::update);
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

		int currentLevel = ScoreboardUtils.getScoreboardValue(mPlayer, objective).orElse(0);
		boolean hasEnhancement = currentLevel > 2;
		int enhancementOffset = hasEnhancement ? 2 : 0;
		// actualCurrentLevel is 0, 1, or 2 - the actual level of the ability before any changes
		int actualCurrentLevel = currentLevel - enhancementOffset;
		if (level == 0) {
			// Remove the ability
			if (hasEnhancement) {
				// Remove the enhancement
				int currentEnhancement = ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE).orElse(0);
				ScoreboardUtils.setScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE, currentEnhancement + 1);
			}
			ScoreboardUtils.setScoreboardValue(mPlayer, objective, 0);
			int currentCount = ScoreboardUtils.getScoreboardValue(mPlayer, remainingPointsObjective).orElse(0);
			ScoreboardUtils.setScoreboardValue(mPlayer, remainingPointsObjective, currentCount + (actualCurrentLevel - level));

			if (actualCurrentLevel != 0) {
				playSelectionSound(mPlayer, displayedSpec == null ? 0 : null, displayedSpec == null ? null : 0, null);
			}
		} else if (level < actualCurrentLevel) {
			// Level clicked is lower than level existing - remove levels down to clicked level
			ScoreboardUtils.setScoreboardValue(mPlayer, objective, enhancementOffset + level);
			int currentCount = ScoreboardUtils.getScoreboardValue(mPlayer, remainingPointsObjective).orElse(0);
			ScoreboardUtils.setScoreboardValue(mPlayer, remainingPointsObjective, currentCount + (actualCurrentLevel - level));

			playSelectionSound(mPlayer, displayedSpec == null ? level : null, displayedSpec == null ? null : level, null);
		} else if (level > actualCurrentLevel) {
			// Level clicked is higher than level existing - upgrade to clicked level if enough points
			int currentCount = ScoreboardUtils.getScoreboardValue(mPlayer, remainingPointsObjective).orElse(0);
			if (currentCount >= level - actualCurrentLevel) {
				// can upgrade
				ScoreboardUtils.setScoreboardValue(mPlayer, remainingPointsObjective, currentCount - (level - actualCurrentLevel));
				ScoreboardUtils.setScoreboardValue(mPlayer, objective, enhancementOffset + level);

				playSelectionSound(mPlayer, displayedSpec == null ? level : null, displayedSpec == null ? null : level, null);
			} else {
				mPlayer.playSound(mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
				mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 0.8f, 0.75f);
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

		int currentLevel = ScoreboardUtils.getScoreboardValue(mPlayer, objective).orElse(0);
		if (add) {
			// Clear ability data
			int currentCount = ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE).orElse(0);
			// We don't want to assign the ability if we don't have points
			if (currentCount > 0) {
				ScoreboardUtils.setScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE, currentCount - 1);
				ScoreboardUtils.setScoreboardValue(mPlayer, objective, currentLevel + 2);

				playSelectionSound(mPlayer, null, null, 1);
			} else {
				mPlayer.playSound(mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
				mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 0.8f, 0.75f);
				return;
			}
		} else {
			// Level clicked is lower than level existing
			ScoreboardUtils.setScoreboardValue(mPlayer, objective, currentLevel - 2);
			int currentCount = ScoreboardUtils.getScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE).orElse(0);
			ScoreboardUtils.setScoreboardValue(mPlayer, AbilityUtils.REMAINING_ENHANCE, currentCount + 1);

			playSelectionSound(mPlayer, null, null, 0);
		}

		updatePlayerAbilities();
	}

	protected void playSelectionSound(Player player, @Nullable Integer skillLevel, @Nullable Integer specLevel, @Nullable Integer enhanceLevel) {
		if (skillLevel != null) {
			switch (skillLevel) {
				case 0: {
					player.playSound(player, Sound.BLOCK_TRIAL_SPAWNER_PLACE, SoundCategory.PLAYERS, 0.7f, 0.9f);
					player.playSound(player, Sound.ENTITY_BREEZE_LAND, SoundCategory.PLAYERS, 0.8f, 0.9f);
					break;
				}
				case 1: {
					player.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1f, 1.35f);
					break;
				}
				case 2: {
					player.playSound(player, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 1f, 1.7f);
					player.playSound(player, Sound.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.PLAYERS, 1f, 1.5f);
					break;
				}
				default: {
				}
			}
		}

		if (specLevel != null) {
			switch (specLevel) {
				case 0: {
					player.playSound(player, Sound.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, SoundCategory.PLAYERS, 0.75f, 0.75f);
					player.playSound(player, Sound.BLOCK_TRIAL_SPAWNER_PLACE, SoundCategory.PLAYERS, 0.75f, 0.6f);
					break;
				}
				case 1: {
					player.playSound(player, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.PLAYERS, 1f, 1.5f);
					player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1f);
					break;
				}
				case 2: {
					player.playSound(player, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.PLAYERS, 1f, 2f);
					player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f, 1.28f);
					break;
				}
				default: {
				}
			}
		}

		if (enhanceLevel != null) {
			switch (enhanceLevel) {
				case 0: {
					player.playSound(player, Sound.BLOCK_TRIAL_SPAWNER_CLOSE_SHUTTER, SoundCategory.PLAYERS, 0.75f, 0.75f);
					player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1f, 1.5f);
					break;
				}
				case 1: {
					player.playSound(player, Sound.BLOCK_TRIAL_SPAWNER_OPEN_SHUTTER, SoundCategory.PLAYERS, 1f, 1.55f);
					player.playSound(player, Sound.BLOCK_CHORUS_FLOWER_GROW, SoundCategory.PLAYERS, 1f, 1.5f);
					player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1f, 0.8f);
					break;
				}
				default: {
				}
			}
		}
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
