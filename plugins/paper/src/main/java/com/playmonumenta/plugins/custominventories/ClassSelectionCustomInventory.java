package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.guis.AbilityTriggersGui;
import com.playmonumenta.plugins.overrides.YellowTesseractOverride;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ClassSelectionCustomInventory extends CustomInventory {
	private static final Material FILLER = GUIUtils.FILLER_MATERIAL;
	private static final String UNLOCK_SPECS = "Quest103";
	private static final int UNLOCK_SPECS_MIN = 6;
	private static final int COMMON_BACK_LOC = 0;
	private static final int COMMON_SUMMARY_LOC = 4;
	private static final int COMMON_REMAINING_SKILL_LOC = 8;
	private static final int COMMON_REMAINING_SPEC_LOC = 7;
	private static final int COMMON_REMAINING_ENHANCEMENTS_LOC = 6;
	public static final ArrayList<Integer> P1_CLASS_LOCS = new ArrayList<>(Arrays.asList(19, 21, 23, 25, 28, 30, 32, 34));
	private static final int P1_RESET_CLASS_LOC = 47;
	private static final int P1_RESET_SPEC_LOC = 49;
	private static final int P1_CHANGE_TRIGGERS_LOC = 51;
	public static final ArrayList<Integer> P2_ABILITY_LOCS = new ArrayList<>(Arrays.asList(9, 14, 18, 23, 27, 32, 36, 41));
	public static final ArrayList<Integer> SKILL_PAGE_SPEC_LOCS = new ArrayList<>(Arrays.asList(47, 51));
	private static final int SKILL_PAGE_RESET_SPEC_LOC = 49;

	private static final ArrayList<Integer> P3_ABILITY_LOCS = new ArrayList<>(Arrays.asList(9, 14, 18, 23, 27, 32, 36, 41));

	public static final ArrayList<Integer> P4_SPEC_LOCS = new ArrayList<>(Arrays.asList(20, 30, 40));
	private static final MonumentaClasses mClasses = new MonumentaClasses();
	public static final String R3_UNLOCK_SCOREBOARD = "R3Access";
	private static final int R3_UNLOCK_SCORE = 1;

	private final boolean mFromYellowTess;
	private final boolean mWasYellowTessOnCooldown;

	private int mCurrentPage = 1;

	/*
	Page 1: Class Select
	Page 2: R1 Skill Select
	Page 3: R3 Skill Select
	Page 4: R2 Spec Select
	 */


	public ClassSelectionCustomInventory(Player player) {
		this(player, false);
	}

	public ClassSelectionCustomInventory(Player player, boolean fromYellowTess) {
		super(player, 54, "Class Selection GUI");
		mFromYellowTess = fromYellowTess;
		mWasYellowTessOnCooldown = fromYellowTess && YellowTesseractOverride.getCooldown(player) > 0;
		makeClassSelectPage(player);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		Player player = (Player) event.getWhoClicked();
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory ||
			    clickedItem == null || clickedItem.getType() == FILLER ||
			    event.isShiftClick()) {
			return;
		}
		if (clickedItem.getType() == Material.BARRIER) {
			//no barrier item should do anything
			return;
		}

		int chosenSlot = event.getSlot();
		//process per page
		switch (mCurrentPage) {
			case 1: //pick a class page
				//clicked a class location
				if (P1_CLASS_LOCS.contains(chosenSlot)) {
					for (PlayerClass oneClass : mClasses.mClasses) {
						if (clickedItem.getType() == oneClass.mDisplayItem) {
							ScoreboardUtils.setScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME, oneClass.mClass);
							makeSkillSelectPage(oneClass, player);
							break;
						}
					}
				} else if (chosenSlot == P1_RESET_CLASS_LOC) {
					AbilityUtils.resetClass(player);
					updateYellowTessCooldown(player);
					makeClassSelectPage(player);
				} else if (chosenSlot == P1_RESET_SPEC_LOC) {
					AbilityUtils.resetSpec(player);
					updateYellowTessCooldown(player);
					makeClassSelectPage(player);
				} else if (chosenSlot == P1_CHANGE_TRIGGERS_LOC) {
					new AbilityTriggersGui(player, true).open();
					return;
				}
				break;
			case 2:
				if (P2_ABILITY_LOCS.contains(chosenSlot)) {
					//clicked ability item, set to 0
					applyAbilityChosen(chosenSlot, player, 0);
					return;
				} else if (P2_ABILITY_LOCS.contains(chosenSlot - 1)) {
					//clicked level 1 item
					applyAbilityChosen(chosenSlot - 1, player, 1);
				} else if (P2_ABILITY_LOCS.contains(chosenSlot - 2)) {
					//clicked level 2 item
					applyAbilityChosen(chosenSlot - 2, player, 2);
				} else if (chosenSlot == COMMON_BACK_LOC) {
					//back to page one
					makeClassSelectPage(player);
				} else if (SKILL_PAGE_SPEC_LOCS.contains(chosenSlot)) {
					//assign spec, make pg 3
					for (PlayerClass oneClass : mClasses.mClasses) {
						if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME) == oneClass.mClass) {
							int specIndex = SKILL_PAGE_SPEC_LOCS.indexOf(chosenSlot);
							PlayerSpec chosenSpec = (specIndex == 0) ? oneClass.mSpecOne : oneClass.mSpecTwo;
							ScoreboardUtils.setScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME, chosenSpec.mSpecialization);
							makeSpecPage(oneClass, chosenSpec, player);
						}
					}
				} else if (chosenSlot == SKILL_PAGE_RESET_SPEC_LOC) {
					AbilityUtils.resetSpec(player);
					updateYellowTessCooldown(player);
					for (PlayerClass oneClass : mClasses.mClasses) {
						if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME) == oneClass.mClass) {
							makeSkillSelectPage(oneClass, player);
						}
					}
				}
				break;
			case 3:
				if (P3_ABILITY_LOCS.contains(chosenSlot)) {
					//clicked ability item, set to 0
					applyAbilityChosen(chosenSlot, player, 0);
					return;
				} else if (P3_ABILITY_LOCS.contains(chosenSlot - 1)) {
					//clicked level 1 item
					applyAbilityChosen(chosenSlot - 1, player, 1);
				} else if (P3_ABILITY_LOCS.contains(chosenSlot - 2)) {
					//clicked level 2 item
					applyAbilityChosen(chosenSlot - 2, player, 2);
				} else if (P3_ABILITY_LOCS.contains(chosenSlot - 3)) {
					//clicked enhanced item
					applyEnhancementChosen(chosenSlot - 3, player,
						clickedItem.getType().equals(Material.ORANGE_STAINED_GLASS_PANE));
				} else if (chosenSlot == COMMON_BACK_LOC) {
					//back to page one
					makeClassSelectPage(player);
				} else if (SKILL_PAGE_SPEC_LOCS.contains(chosenSlot)) {
					//assign spec, make pg 3
					for (PlayerClass oneClass : mClasses.mClasses) {
						if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME) == oneClass.mClass) {
							int specIndex = SKILL_PAGE_SPEC_LOCS.indexOf(chosenSlot);
							PlayerSpec chosenSpec = (specIndex == 0) ? oneClass.mSpecOne : oneClass.mSpecTwo;
							ScoreboardUtils.setScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME, chosenSpec.mSpecialization);
							makeSpecPage(oneClass, chosenSpec, player);
						}
					}
				} else if (chosenSlot == SKILL_PAGE_RESET_SPEC_LOC) {
					AbilityUtils.resetSpec(player);
					for (PlayerClass oneClass : mClasses.mClasses) {
						if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME) == oneClass.mClass) {
							makeSkillSelectPage(oneClass, player);
						}
					}
				}
				break;
			case 4:
				if (P4_SPEC_LOCS.contains(chosenSlot)) {
					//clicked spec ability item, no action
					applySpecAbilityChosen(chosenSlot, player, 0);
					return;
				} else if (P4_SPEC_LOCS.contains(chosenSlot - 1)) {
					//clicked level 1 item
					applySpecAbilityChosen(chosenSlot - 1, player, 1);
				} else if (P4_SPEC_LOCS.contains(chosenSlot - 2)) {
					//clicked level 2 item
					applySpecAbilityChosen(chosenSlot - 2, player, 2);
				} else if (chosenSlot == COMMON_BACK_LOC) {
					//back to page two
					for (PlayerClass oneClass : mClasses.mClasses) {
						if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME) == oneClass.mClass) {
							makeSkillSelectPage(oneClass, player);
							break;
						}
					}
				}
				break;
			case 0:
			default:
				player.sendMessage("interaction: page was 0 or not defined in switch");
				makeClassSelectPage(player);
		}
	}

	public void makeClassSelectPage(Player player) {
		mInventory.clear();
		mCurrentPage = 1;
		//Get the current class, if they have one.
		int currentClass = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME);
		PlayerClass playerClass = null;
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (currentClass == oneClass.mClass) {
				playerClass = oneClass;
				break;
			}
		}
		int currentIndex = 0;
		//if classless, show all as the items. otherwise, barrier the choices but keep names
		for (PlayerClass oneClass : mClasses.mClasses) {
			boolean lockedClass = false;
			boolean permLock = false;
			//get effective specs does the same thing as if i made a function to check if they had r3 access and are in an ability enhancement area.
			if (oneClass.mQuestReq != null && !AbilityUtils.getEffectiveSpecs(player)) {
				lockedClass = (ScoreboardUtils.getScoreboardValue(player, oneClass.mQuestReq) < oneClass.mQuestReqMin);
			}
			if (oneClass.mPermissionString != null) {
				lockedClass = !player.hasPermission(oneClass.mPermissionString);
				permLock = !player.hasPermission(oneClass.mPermissionString);
			}
			ItemStack createItem = createClassItem(oneClass, (playerClass != null && playerClass != oneClass), lockedClass, permLock);
			mInventory.setItem(P1_CLASS_LOCS.get(currentIndex++), createItem);
		}

		ItemStack summaryItem = GUIUtils.createBasicItem(Material.SCUTE, "Main Menu", NamedTextColor.WHITE, false,
			"Pick a class to view abilities within that class. You can reset your class at any time, with no consequences.", NamedTextColor.LIGHT_PURPLE);
		GUIUtils.setGuiNbtTag(summaryItem, "Gui", "class_select_main_menu");
		mInventory.setItem(COMMON_SUMMARY_LOC, summaryItem);

		if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME) != 0) {
			ItemStack resetItem = GUIUtils.createBasicItem(Material.CYAN_BED, "Reset Your Class", NamedTextColor.WHITE, false,
				"Click here to reset your class, allowing access to other choices.", NamedTextColor.LIGHT_PURPLE);
			GUIUtils.setGuiNbtTag(resetItem, "Gui", "class_select_reset_class");
			if (playerClass != null) {
				GUIUtils.setGuiNbtTag(resetItem, "Class", playerClass.mClassName);
			}
			mInventory.setItem(P1_RESET_CLASS_LOC, resetItem);
		}

		//possibly create reset spec item
		int spec = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME);
		if (spec != 0) {
			ItemStack specItem = GUIUtils.createBasicItem(Material.RED_BANNER, "Reset Your Specialization", NamedTextColor.WHITE, false,
				"Click here to reset your specialization, allowing access to choose either specialization.", NamedTextColor.LIGHT_PURPLE);
			GUIUtils.setGuiNbtTag(specItem, "Gui", "cross_gui_reset_spec");
			if (playerClass != null) {
				GUIUtils.setGuiNbtTag(specItem, "Spec",
					(spec == playerClass.mSpecOne.mSpecialization ? playerClass.mSpecOne : playerClass.mSpecTwo).mSpecName);
			}
			mInventory.setItem(P1_RESET_SPEC_LOC, specItem);
		}

		ItemStack triggersItem = GUIUtils.createBasicItem(Material.JIGSAW, "Change Ability Triggers", NamedTextColor.WHITE, false,
			"Click here to change which key combinations are used to cast abilities.", NamedTextColor.LIGHT_PURPLE);
		GUIUtils.setGuiNbtTag(triggersItem, "Gui", "class_select_trigger");
		mInventory.setItem(P1_CHANGE_TRIGGERS_LOC, triggersItem);

		makeRemainingCountItems(player);
		fillEmptyAndSetPlainTags();
	}

	public void makeSkillSelectPage(PlayerClass userClass, Player player) {
		if (ScoreboardUtils.getScoreboardValue(player, R3_UNLOCK_SCOREBOARD) >= R3_UNLOCK_SCORE) {
			makeRegionThreeSkillPage(userClass, player);
		} else {
			makeRegionOneSkillPage(userClass, player);
		}
	}

	public void makeRegionOneSkillPage(PlayerClass userClass, Player player) {
		mInventory.clear();
		//make abilities
		mCurrentPage = 2;
		int iterator = 0;
		for (AbilityInfo<?> ability : userClass.mAbilities) {
			ItemStack item = createAbilityItem(userClass, ability);
			mInventory.setItem(P2_ABILITY_LOCS.get(iterator), item);
			ItemStack levelOne = createLevelItem(userClass, ability, 1, player);
			mInventory.setItem(P2_ABILITY_LOCS.get(iterator) + 1, levelOne);
			ItemStack levelTwo = createLevelItem(userClass, ability, 2, player);
			mInventory.setItem(P2_ABILITY_LOCS.get(iterator++) + 2, levelTwo);
		}
		//specs
		if (ScoreboardUtils.getScoreboardValue(player, UNLOCK_SPECS) >= UNLOCK_SPECS_MIN) {
			addSpecItems(userClass, player);
		}

		//summary
		ItemStack summaryItem = GUIUtils.createBasicItem(userClass.mDisplayItem, userClass.mClassName + " Class Skills", NamedTextColor.WHITE, false,
			"Pick your skills and, if unlocked, your specialization.", NamedTextColor.LIGHT_PURPLE);
		mInventory.setItem(COMMON_SUMMARY_LOC, summaryItem);

		//back button
		ItemStack backButton = GUIUtils.createBasicItem(Material.ARROW, "Back",
			NamedTextColor.GRAY, false, "Return to the class selection page.", NamedTextColor.GRAY);
		mInventory.setItem(COMMON_BACK_LOC, backButton);

		//possibly create reset spec item
		int spec = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME);
		if (spec != 0) {
			ItemStack specItem = GUIUtils.createBasicItem(Material.RED_BANNER, "Reset Your Specialization", NamedTextColor.WHITE, false,
				"Click here to reset your specialization to select a new one.", NamedTextColor.LIGHT_PURPLE);
			GUIUtils.setGuiNbtTag(specItem, "Spec",
				(spec == userClass.mSpecOne.mSpecialization ? userClass.mSpecOne : userClass.mSpecTwo).mSpecName);
			mInventory.setItem(SKILL_PAGE_RESET_SPEC_LOC, specItem);
		}

		makeRemainingCountItems(player);
		fillEmptyAndSetPlainTags();
	}

	public void makeRegionThreeSkillPage(PlayerClass userClass, Player player) {
		mInventory.clear();
		//make abilities
		mCurrentPage = 3;
		int iterator = 0;
		for (AbilityInfo<?> ability : userClass.mAbilities) {
			ItemStack item = createAbilityItem(userClass, ability);
			mInventory.setItem(P3_ABILITY_LOCS.get(iterator), item);
			ItemStack levelOne = createLevelItem(userClass, ability, 1, player);
			GUIUtils.setGuiNbtTag(levelOne, "Skill", ability.getDisplayName());
			mInventory.setItem(P3_ABILITY_LOCS.get(iterator) + 1, levelOne);
			ItemStack levelTwo = createLevelItem(userClass, ability, 2, player);
			GUIUtils.setGuiNbtTag(levelTwo, "Skill", ability.getDisplayName());
			mInventory.setItem(P3_ABILITY_LOCS.get(iterator) + 2, levelTwo);
			ItemStack enhanceItem = createEnhanceItem(userClass, ability, player);
			GUIUtils.setGuiNbtTag(enhanceItem, "Skill", ability.getDisplayName());
			mInventory.setItem(P3_ABILITY_LOCS.get(iterator++) + 3, enhanceItem);
		}

		//specs
		if (ScoreboardUtils.getScoreboardValue(player, UNLOCK_SPECS) >= UNLOCK_SPECS_MIN || AbilityUtils.getEffectiveSpecs(player)) {
			addSpecItems(userClass, player);
		}

		//summary
		ItemStack summaryItem = GUIUtils.createBasicItem(userClass.mDisplayItem, userClass.mClassName + " Class Skills", NamedTextColor.WHITE, false,
			"Pick your skills and, if unlocked, your specialization.", NamedTextColor.LIGHT_PURPLE);
		mInventory.setItem(COMMON_SUMMARY_LOC, summaryItem);

		//back button
		ItemStack backButton = GUIUtils.createBasicItem(Material.ARROW, "Back",
			NamedTextColor.GRAY, false, "Return to the class selection page.", NamedTextColor.GRAY);
		mInventory.setItem(COMMON_BACK_LOC, backButton);

		//possibly create reset spec item
		if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME) != 0) {
			ItemStack specItem = GUIUtils.createBasicItem(Material.RED_BANNER, "Reset Your Specialization", NamedTextColor.WHITE, false,
				"Click here to reset your specialization to select a new one.", NamedTextColor.LIGHT_PURPLE);
			GUIUtils.setGuiNbtTag(specItem, "Spec",
				(ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME) == userClass.mSpecOne.mSpecialization ? userClass.mSpecOne : userClass.mSpecTwo).mSpecName);
			mInventory.setItem(SKILL_PAGE_RESET_SPEC_LOC, specItem);
		}

		makeRemainingCountItems(player);
		fillEmptyAndSetPlainTags();
	}

	public void makeSpecPage(PlayerClass userClass, PlayerSpec spec, Player player) {
		mInventory.clear();
		mCurrentPage = 4;
		int iterator = 0;
		for (AbilityInfo<?> ability : spec.mAbilities) {
			ItemStack item = createAbilityItem(userClass, ability);
			mInventory.setItem(P4_SPEC_LOCS.get(iterator), item);
			ItemStack levelOne = createLevelItem(userClass, ability, 1, player);
			mInventory.setItem(P4_SPEC_LOCS.get(iterator) + 1, levelOne);
			ItemStack levelTwo = createLevelItem(userClass, ability, 2, player);
			mInventory.setItem(P4_SPEC_LOCS.get(iterator++) + 2, levelTwo);
		}

		//summary
		ItemStack summaryItem = GUIUtils.createBasicItem(spec.mDisplayItem, spec.mSpecName + " Specialization Skills", NamedTextColor.WHITE, false,
			"Pick your specialization skills.", NamedTextColor.LIGHT_PURPLE);
		mInventory.setItem(COMMON_SUMMARY_LOC, summaryItem);

		//back button
		ItemStack backButton = GUIUtils.createBasicItem(Material.ARROW, "Back",
			NamedTextColor.GRAY, false, "Return to the skill selection page.", NamedTextColor.GRAY);
		mInventory.setItem(COMMON_BACK_LOC, backButton);
		makeRemainingCountItems(player);
		fillEmptyAndSetPlainTags();
	}

	public void applyAbilityChosen(int chosenSlot, Player player, int level) {
		ArrayList<Integer> currentLocations;
		if (ScoreboardUtils.getScoreboardValue(player, R3_UNLOCK_SCOREBOARD) >= R3_UNLOCK_SCORE) {
			currentLocations = P3_ABILITY_LOCS;
		} else {
			currentLocations = P2_ABILITY_LOCS;
		}
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME) == oneClass.mClass) {
				//found class, find ability
				int abilityIndex = currentLocations.indexOf(chosenSlot);
				AbilityInfo<?> selectedAbility = oneClass.mAbilities.get(abilityIndex);
				String scoreboard = selectedAbility.getScoreboard();
				if (scoreboard == null) {
					return;
				}
				int currentLevel = ScoreboardUtils.getScoreboardValue(player, scoreboard);
				int skillOffset = currentLevel > 2 ? 2 : 0;
				// actualCurrentLevel is 0, 1, or 2 - the actual level of the ability before any changes
				int actualCurrentLevel = currentLevel - skillOffset;
				if (level == 0) {
					// remove the ability
					if (currentLevel > 2) {
						// remove the enhancement
						int currentEnhancement = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_ENHANCE);
						ScoreboardUtils.setScoreboardValue(player, AbilityUtils.REMAINING_ENHANCE, currentEnhancement + 1);
					}
					ScoreboardUtils.setScoreboardValue(player, scoreboard, 0);
					int currentCount = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_SKILL);
					ScoreboardUtils.setScoreboardValue(player, AbilityUtils.REMAINING_SKILL, currentCount + (actualCurrentLevel - level));
				} else if (level < actualCurrentLevel) {
					//level clicked is lower than level existing - remove levels down to clicked level
					ScoreboardUtils.setScoreboardValue(player, scoreboard, skillOffset + level);
					int currentCount = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_SKILL);
					ScoreboardUtils.setScoreboardValue(player, AbilityUtils.REMAINING_SKILL, currentCount + (actualCurrentLevel - level));
				} else if (level > actualCurrentLevel) {
					//level clicked is higher than level existing - upgrade to clicked level if enough points
					int currentCount = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_SKILL);
					if (currentCount >= level - actualCurrentLevel) {
						// can upgrade
						ScoreboardUtils.setScoreboardValue(player, AbilityUtils.REMAINING_SKILL, currentCount - (level - actualCurrentLevel));
						ScoreboardUtils.setScoreboardValue(player, scoreboard, skillOffset + level);
					} else {
						player.sendMessage("You don't have enough skill points to select this skill!");
					}
				}
				makeSkillSelectPage(oneClass, player);
				updatePlayerAbilities(player);
				return;
			}
		}
	}

	public void applySpecAbilityChosen(int chosenSlot, Player player, int level) {
		PlayerClass theClass = null;
		PlayerSpec spec = null;
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME) == oneClass.mClass) {
				theClass = oneClass;
				if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME) == oneClass.mSpecOne.mSpecialization) {
					spec = oneClass.mSpecOne;
				} else if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME) == oneClass.mSpecTwo.mSpecialization) {
					spec = oneClass.mSpecTwo;
				}
				break;
			}
		}
		if (spec != null) {
			//found class, find ability
			int abilityIndex = P4_SPEC_LOCS.indexOf(chosenSlot);
			AbilityInfo<?> selectedAbility = spec.mAbilities.get(abilityIndex);
			String scoreboard = selectedAbility.getScoreboard();
			if (scoreboard == null) {
				return;
			}
			int currentLevel = ScoreboardUtils.getScoreboardValue(player, scoreboard);
			if (level - currentLevel < 0) {
				//level clicked is lower than level existing
				ScoreboardUtils.setScoreboardValue(player, scoreboard, level);
				int currentCount = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_SPEC);
				ScoreboardUtils.setScoreboardValue(player, AbilityUtils.REMAINING_SPEC, currentCount + (currentLevel - level));
			} else if (level - currentLevel > 0) {
				//level clicked is higher than level existing
				int currentCount = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_SPEC);
				if (currentCount >= level - currentLevel) {
					ScoreboardUtils.setScoreboardValue(player, AbilityUtils.REMAINING_SPEC, currentCount - (level - currentLevel));
					ScoreboardUtils.setScoreboardValue(player, scoreboard, level);
				} else {
					player.sendMessage("You don't have enough specialization points to select this skill!");
				}
			}
			makeSpecPage(Objects.requireNonNull(theClass), spec, player);
		}
		updatePlayerAbilities(player);
	}

	public void applyEnhancementChosen(int chosenSlot, Player player, boolean add) {
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME) == oneClass.mClass) {
				//found class, find ability
				int abilityIndex = P3_ABILITY_LOCS.indexOf(chosenSlot);
				AbilityInfo<?> selectedAbility = oneClass.mAbilities.get(abilityIndex);
				String scoreboard = selectedAbility.getScoreboard();
				if (scoreboard == null) {
					return;
				}
				int currentLevel = ScoreboardUtils.getScoreboardValue(player, scoreboard);
				if (add) {
					//clear ability data
					int currentCount = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_ENHANCE);
					// don't want to assign ability if we don't have points
					if (currentCount > 0) {
						ScoreboardUtils.setScoreboardValue(player, AbilityUtils.REMAINING_ENHANCE, currentCount - 1);
						ScoreboardUtils.setScoreboardValue(player, scoreboard, currentLevel + 2);
					} else {
						player.sendMessage("You don't have enough enhancement points to select this enhancement!");
					}
				} else {
					//level clicked is lower than level existing
					ScoreboardUtils.setScoreboardValue(player, scoreboard, currentLevel - 2);
					int currentCount = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_ENHANCE);
					ScoreboardUtils.setScoreboardValue(player, AbilityUtils.REMAINING_ENHANCE, currentCount + 1);
				}
				makeSkillSelectPage(oneClass, player);
				updatePlayerAbilities(player);
				return;
			}
		}
	}

	private void updatePlayerAbilities(Player player) {
		if (AbilityManager.getManager() != null) {
			AbilityManager.getManager().updatePlayerAbilities(player, true);
			updateYellowTessCooldown(player);
		}
	}

	private void updateYellowTessCooldown(Player player) {
		if (mFromYellowTess
			    && !ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.RESIST_5)) {
			YellowTesseractOverride.setCooldown(player, 5);
			if (mWasYellowTessOnCooldown) {
				Plugin.getInstance().mEffectManager.addEffect(player, "YellowTessSilence", new AbilitySilence(30 * 20));
			}
		}
	}

	public void addSpecItems(PlayerClass playerClass, Player player) {
		addSpecItem(playerClass, playerClass.mSpecOne, playerClass.mSpecTwo, 1, player);
		addSpecItem(playerClass, playerClass.mSpecTwo, playerClass.mSpecOne, 2, player);
	}

	public void addSpecItem(PlayerClass playerClass, PlayerSpec spec, PlayerSpec otherSpec, int specNumber, Player player) {
		if (!AbilityUtils.getEffectiveSpecs(player) && ScoreboardUtils.getScoreboardValue(player, spec.mSpecQuestScoreboard) < 100) {
			//not unlocked
			ItemStack specItem = GUIUtils.createBasicItem(Material.BARRIER, "Unknown", playerClass.mClassColor, false,
				"You haven't unlocked this specialization yet.", NamedTextColor.WHITE);
			mInventory.setItem(SKILL_PAGE_SPEC_LOCS.get(specNumber - 1), specItem);
		} else if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME) == otherSpec.mSpecialization) {
			//unlocked, but using other spec
			ItemStack specItem = GUIUtils.createBasicItem(Material.BARRIER, spec.mSpecName, playerClass.mClassColor, false,
				"Reset your specialization to select a new one.", NamedTextColor.WHITE);
			mInventory.setItem(SKILL_PAGE_SPEC_LOCS.get(specNumber - 1), specItem);
		} else if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME) == spec.mSpecialization) {
			//unlocked and already using this spec
			ItemStack specItem = GUIUtils.createBasicItem(spec.mDisplayItem, spec.mSpecName, playerClass.mClassColor, false,
				"Click to view your specialization skills.", NamedTextColor.WHITE);
			ItemMeta newMeta = specItem.getItemMeta();
			if (spec.mPassiveName != null) {
				GUIUtils.splitLoreLine(newMeta, spec.mPassiveName + " (Passive): " + spec.mPassiveDescription, NamedTextColor.GREEN, 30, false);
			}
			specItem.setItemMeta(newMeta);
			mInventory.setItem(SKILL_PAGE_SPEC_LOCS.get(specNumber - 1), specItem);
		} else if (ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME) == 0) {
			//unlocked and no spec selected
			ItemStack specItem = GUIUtils.createBasicItem(spec.mDisplayItem, spec.mSpecName, playerClass.mClassColor, false,
				"Click to choose this specialization!", NamedTextColor.GRAY);
			ItemMeta newMeta = specItem.getItemMeta();
			GUIUtils.splitLoreLine(newMeta, "Description: " + spec.mDescription, NamedTextColor.YELLOW, 30, false);
			if (spec.mPassiveName != null) {
				GUIUtils.splitLoreLine(newMeta, spec.mPassiveName + " (Passive): " + spec.mPassiveDescription, NamedTextColor.GREEN, 30, false);
			}
			specItem.setItemMeta(newMeta);
			mInventory.setItem(SKILL_PAGE_SPEC_LOCS.get(specNumber - 1), specItem);
		}
	}

	public ItemStack createLevelItem(PlayerClass theClass, AbilityInfo<?> ability, int level, Player player) {
		int getScore;
		String scoreboard = ability.getScoreboard();
		if (scoreboard == null) {
			getScore = 0;
		} else {
			getScore = ScoreboardUtils.getScoreboardValue(player, scoreboard);
			if (getScore > 2) {
				getScore -= 2;
			}
		}
		Material newMat = getScore >= level ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
		return GUIUtils.createBasicItem(newMat, 1,
			"Level " + level, theClass.mClassColor, true,
			ability.getDescription(level).color(NamedTextColor.WHITE), 30, true);
	}

	public ItemStack createEnhanceItem(PlayerClass theClass, AbilityInfo<?> ability, Player player) {
		if (ability.getDescriptions().size() == 3) {
			Material newMat;
			String scoreboard = ability.getScoreboard();
			switch (scoreboard == null ? 0 : ScoreboardUtils.getScoreboardValue(player, scoreboard)) {
				case 0 -> {
					newMat = Material.BARRIER;
					return GUIUtils.createBasicItem(newMat, 1,
						"Enhancement", theClass.mClassColor, true,
						Component.text("Cannot Select; Choose levels in the ability first. Description: ").append(ability.getDescription(3)),
						30, true);
				}
				case 1, 2 -> newMat = Material.ORANGE_STAINED_GLASS_PANE;
				case 3, 4 -> newMat = Material.YELLOW_STAINED_GLASS_PANE;
				default -> {
					newMat = Material.BARRIER;
					return GUIUtils.createBasicItem(newMat, "Unknown Level",
						theClass.mClassColor, true, "Unknown level for ability.", NamedTextColor.WHITE);
				}
			}
			return GUIUtils.createBasicItem(newMat, 1,
				"Enhancement", theClass.mClassColor, true,
				ability.getDescription(3), 30, true);
		}

		return GUIUtils.createBasicItem(Material.BARRIER, "No Option",
			theClass.mClassColor, true, "No Enhancement Created", NamedTextColor.WHITE);
	}

	public ItemStack createAbilityItem(PlayerClass theClass, AbilityInfo<?> ability) {
		String desc = ability.getSimpleDescription();
		TextComponent clickHere = Component.text("Click here to remove your levels in this skill, and click the panes to the right to pick a level in this skill.", NamedTextColor.GRAY);
		TextComponent lore = (desc == null ? clickHere : Component.text(desc + "\n", NamedTextColor.WHITE).append(clickHere));
		Material item = ability.getDisplayItem();
		if (item == null) {
			item = theClass.mDisplayItem;
		}
		String name = ability.getDisplayName();
		if (name == null) {
			name = "";
		}
		return GUIUtils.createBasicItem(item, 1, name,
			theClass.mClassColor, true, lore, 30, true);
	}

	public ItemStack createClassItem(PlayerClass classToItemize, boolean otherChosen, boolean locked, boolean permissionLock) {
		if (locked) {
			if (permissionLock) {
				return GUIUtils.createBasicItem(Material.BARRIER, classToItemize.mClassName,
					classToItemize.mClassColor, true, "This class is currently unavailable to you.", NamedTextColor.RED);
			}
			return GUIUtils.createBasicItem(Material.BARRIER, classToItemize.mClassName,
				classToItemize.mClassColor, true, "You don't have the requirements to choose this class.", NamedTextColor.RED);
		}
		if (otherChosen) {
			return GUIUtils.createBasicItem(Material.BARRIER, classToItemize.mClassName,
				classToItemize.mClassColor, true, "Reset your class to choose this one!", NamedTextColor.RED);
		}
		ItemStack newItem = GUIUtils.createBasicItem(classToItemize.mDisplayItem, classToItemize.mClassName,
			classToItemize.mClassColor, true, "Click to choose this class!", NamedTextColor.GRAY);
		if (classToItemize.mClassDescription != null) {
			ItemMeta newMeta = newItem.getItemMeta();
			GUIUtils.splitLoreLine(newMeta, "Description: " + classToItemize.mClassDescription, NamedTextColor.YELLOW, 30, false);
			newItem.setItemMeta(newMeta);
		}
		if (classToItemize.mClassPassiveDescription != null && classToItemize.mClassPassiveName != null) {
			ItemMeta newMeta = newItem.getItemMeta();
			GUIUtils.splitLoreLine(newMeta, classToItemize.mClassPassiveName + " (Passive): " + classToItemize.mClassPassiveDescription, NamedTextColor.GREEN, 30, false);
			newItem.setItemMeta(newMeta);
		}

		return newItem;
	}

	public void makeRemainingCountItems(Player player) {
		if (ScoreboardUtils.getScoreboardValue(player, R3_UNLOCK_SCOREBOARD) >= R3_UNLOCK_SCORE) {
			int currentEnhanceCount = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_ENHANCE);
			ItemStack summaryItem = GUIUtils.createBasicItem(currentEnhanceCount == 0 ? Material.BARRIER : Material.ENCHANTING_TABLE, "Enhancement Points", NamedTextColor.WHITE, false,
				"You have " + currentEnhanceCount + " enhancement point" + (currentEnhanceCount == 1 ? "" : "s") + " remaining.", NamedTextColor.LIGHT_PURPLE);
			summaryItem.setAmount(currentEnhanceCount > 0 ? currentEnhanceCount : 1);
			mInventory.setItem(COMMON_REMAINING_ENHANCEMENTS_LOC, summaryItem);
		}
		if (ScoreboardUtils.getScoreboardValue(player, UNLOCK_SPECS) >= UNLOCK_SPECS_MIN || AbilityUtils.getEffectiveSpecs(player)) {
			int currentSpecCount = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_SPEC);
			ItemStack summaryItem = GUIUtils.createBasicItem(currentSpecCount == 0 ? Material.BARRIER : Material.SAND, "Specialization Points", NamedTextColor.WHITE, false,
				"You have " + currentSpecCount + " specialization point" + (currentSpecCount == 1 ? "" : "s") + " remaining.", NamedTextColor.LIGHT_PURPLE);
			summaryItem.setAmount(currentSpecCount > 0 ? currentSpecCount : 1);
			mInventory.setItem(COMMON_REMAINING_SPEC_LOC, summaryItem);
		}
		int currentSkillCount = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.REMAINING_SKILL);
		ItemStack summaryItem = GUIUtils.createBasicItem(currentSkillCount == 0 ? Material.BARRIER : Material.GRASS_BLOCK, "Skill Points", NamedTextColor.WHITE, false,
			"You have " + currentSkillCount + " skill point" + (currentSkillCount == 1 ? "" : "s") + " remaining.", NamedTextColor.LIGHT_PURPLE);
		summaryItem.setAmount(currentSkillCount > 0 ? currentSkillCount : 1);
		mInventory.setItem(COMMON_REMAINING_SKILL_LOC, summaryItem);
	}

	public void fillEmptyAndSetPlainTags() {
		for (ItemStack item : mInventory) {
			if (item != null) {
				ItemUtils.setPlainTag(item);
			}
		}
		GUIUtils.fillWithFiller(mInventory);
	}
}
