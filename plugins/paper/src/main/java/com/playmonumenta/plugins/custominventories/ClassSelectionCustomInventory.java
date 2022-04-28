package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ClassSelectionCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private static final String UNLOCK_SPECS = "Quest103";
	private static final int UNLOCK_SPECS_MIN = 6;
	private static final int COMMON_BACK_LOC = 0;
	private static final int COMMON_SUMMARY_LOC = 4;
	private static final int COMMON_REMAINING_SKILL_LOC = 8;
	private static final int COMMON_REMAINING_SPEC_LOC = 7;
	private static final int COMMON_REMAINING_ENHANCEMENTS_LOC = 6;
	private static final int P1_CLASS_START_LOC = 19;
	private static final int P1_RESET_CLASS_LOC = 38;
	private static final int P1_RESET_SPEC_LOC = 42;
	public static final ArrayList<Integer> P2_ABILITY_LOCS = new ArrayList<>(Arrays.asList(10, 14, 19, 23, 28, 32, 37, 41));
	public static final ArrayList<Integer> SKILL_PAGE_SPEC_LOCS = new ArrayList<>(Arrays.asList(47, 51));
	private static final int SKILL_PAGE_RESET_SPEC_LOC = 49;

	private static final ArrayList<Integer> P3_ABILITY_LOCS = new ArrayList<>(Arrays.asList(9, 14, 18, 23, 27, 32, 36, 41));

	public static final ArrayList<Integer> P4_SPEC_LOCS = new ArrayList<>(Arrays.asList(20, 30, 40));
	private static final MonumentaClasses mClasses = new MonumentaClasses(Plugin.getInstance(), null);
	private static final String ABILITY_SKILLCOUNT = "Skill";
	private static final String SPEC_SKILLCOUNT = "SkillSpec";

	private static final String R3_UNLOCK_SCOREBOARD = "AccessEnhance";
	private static final int R3_UNLOCK_SCORE = 1;
	private static final String R3_ENHANCE_CURRENT = "Enhancements";

	private int mCurrentPage = 1;

	/*
	Page 1: Class Select
	Page 2: R1 Skill Select
	Page 3: R3 Skill Select
	Page 4: R2 Spec Select
	 */


	public ClassSelectionCustomInventory(Player player) {
		super(player, 54, "Class Selection GUI");
		makeClassSelectPage(player);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
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
			if (chosenSlot >= P1_CLASS_START_LOC && chosenSlot <= P1_CLASS_START_LOC + 6) {
				for (PlayerClass oneClass : mClasses.mClasses) {
					if (oneClass.mDisplayItem != null && clickedItem.getType() == oneClass.mDisplayItem.getType()) {
						ScoreboardUtils.setScoreboardValue(player, "Class", oneClass.mClass);
						makeSkillSelectPage(oneClass, player);
						break;
					}
				}
			} else if (chosenSlot == P1_RESET_CLASS_LOC) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
						"execute as " + player.getUniqueId() + " run function monumenta:class_selection/reset");
				makeClassSelectPage(player);
			} else if (chosenSlot == P1_RESET_SPEC_LOC) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
						"execute as " + player.getUniqueId() + " run function monumenta:class_selection/reset_spec");
				makeClassSelectPage(player);
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
					if (ScoreboardUtils.getScoreboardValue(player, "Class") == oneClass.mClass) {
						int specIndex = SKILL_PAGE_SPEC_LOCS.indexOf(chosenSlot);
						PlayerSpec chosenSpec = (specIndex == 0) ? oneClass.mSpecOne : oneClass.mSpecTwo;
						ScoreboardUtils.setScoreboardValue(player, "Specialization", chosenSpec.mSpecialization);
						makeSpecPage(oneClass, chosenSpec, player);
					}
				}
			} else if (chosenSlot == SKILL_PAGE_RESET_SPEC_LOC) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
						"execute as " + player.getUniqueId() + " run function monumenta:class_selection/reset_spec");
				for (PlayerClass oneClass : mClasses.mClasses) {
					if (ScoreboardUtils.getScoreboardValue(player, "Class") == oneClass.mClass) {
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
					if (ScoreboardUtils.getScoreboardValue(player, "Class") == oneClass.mClass) {
						int specIndex = SKILL_PAGE_SPEC_LOCS.indexOf(chosenSlot);
						PlayerSpec chosenSpec = (specIndex == 0) ? oneClass.mSpecOne : oneClass.mSpecTwo;
						ScoreboardUtils.setScoreboardValue(player, "Specialization", chosenSpec.mSpecialization);
						makeSpecPage(oneClass, chosenSpec, player);
					}
				}
			} else if (chosenSlot == SKILL_PAGE_RESET_SPEC_LOC) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
					"execute as " + player.getUniqueId() + " run function monumenta:class_selection/reset_spec");
				for (PlayerClass oneClass : mClasses.mClasses) {
					if (ScoreboardUtils.getScoreboardValue(player, "Class") == oneClass.mClass) {
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
					if (ScoreboardUtils.getScoreboardValue(player, "Class") == oneClass.mClass) {
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
		int currentClass = ScoreboardUtils.getScoreboardValue(player, "Class");
		PlayerClass playerClass = null;
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (currentClass == oneClass.mClass) {
				playerClass = oneClass;
				break;
			}
		}
		int currentSlot = P1_CLASS_START_LOC;
		//if classless, show all as the items. otherwise, barrier the choices but keep names
		for (PlayerClass oneClass : mClasses.mClasses) {
			boolean lockedClass = false;
			if (oneClass.mQuestReq != null) {
				lockedClass = ScoreboardUtils.getScoreboardValue(player, oneClass.mQuestReq) < oneClass.mQuestReqMin;
			}
			ItemStack createItem = createClassItem(oneClass, (playerClass != null && playerClass != oneClass), lockedClass);
			mInventory.setItem(currentSlot++, createItem);
		}

		ItemStack summaryItem = createBasicItem(Material.SCUTE, "Main Menu", NamedTextColor.WHITE, false,
				"Pick a class to view abilities within that class. You can reset your class at any time, with no consequences.", ChatColor.LIGHT_PURPLE);
		mInventory.setItem(COMMON_SUMMARY_LOC, summaryItem);

		if (ScoreboardUtils.getScoreboardValue(player, "Class") != 0) {
			ItemStack resetItem = createBasicItem(Material.CYAN_BED, "Reset Your Class", NamedTextColor.WHITE, false,
					"Click here to reset your class, allowing access to other choices.", ChatColor.LIGHT_PURPLE);
			mInventory.setItem(P1_RESET_CLASS_LOC, resetItem);
		}

		//possibly create reset spec item
		if (ScoreboardUtils.getScoreboardValue(player, "Specialization") != 0) {
			ItemStack specItem = createBasicItem(Material.RED_BANNER, "Reset Your Specialization", NamedTextColor.WHITE, false,
					"Click here to reset your specialization, allowing access to choose either specialization.", ChatColor.LIGHT_PURPLE);
			mInventory.setItem(P1_RESET_SPEC_LOC, specItem);
		}
		makeRemainingCountItems(player);
		fillEmpty();
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
		for (Ability ability : userClass.mAbilities) {
			ItemStack item = createAbilityItem(userClass, ability);
			mInventory.setItem(P2_ABILITY_LOCS.get(iterator), item);
			ItemStack levelOne = createLevelItem(userClass, ability, 1, player);
			mInventory.setItem(P2_ABILITY_LOCS.get(iterator) + 1, levelOne);
			ItemStack levelTwo = createLevelItem(userClass, ability, 2, player);
			mInventory.setItem(P2_ABILITY_LOCS.get(iterator++) + 2, levelTwo);
		}
		//specs
		if (ScoreboardUtils.getScoreboardValue(player, UNLOCK_SPECS) >= UNLOCK_SPECS_MIN) {
			addSpecItem(userClass.mSpecOne, userClass.mSpecTwo, 1, player);
			addSpecItem(userClass.mSpecTwo, userClass.mSpecOne, 2, player);
		}

		//summary
		if (userClass.mDisplayItem != null) {
			ItemStack summaryItem = createBasicItem(userClass.mDisplayItem.getType(), "Class Skills", NamedTextColor.WHITE, false,
				"Pick your skills and, if unlocked, your specialization.", ChatColor.LIGHT_PURPLE);
			mInventory.setItem(COMMON_SUMMARY_LOC, summaryItem);
		}

		//back button
		ItemStack backButton = createBasicItem(Material.ARROW, "Back",
				NamedTextColor.GRAY, false, "Return to the class selection page.", ChatColor.GRAY);
		mInventory.setItem(COMMON_BACK_LOC, backButton);

		//possibly create reset spec item
		if (ScoreboardUtils.getScoreboardValue(player, "Specialization") != 0) {
			ItemStack specItem = createBasicItem(Material.RED_BANNER, "Reset Your Specialization", NamedTextColor.WHITE, false,
					"Click here to reset your specialization to select a new one.", ChatColor.LIGHT_PURPLE);
			mInventory.setItem(SKILL_PAGE_RESET_SPEC_LOC, specItem);
		}

		makeRemainingCountItems(player);
		fillEmpty();
	}

	public void makeRegionThreeSkillPage(PlayerClass userClass, Player player) {
		mInventory.clear();
		//make abilities
		mCurrentPage = 3;
		int iterator = 0;
		for (Ability ability : userClass.mAbilities) {
			ItemStack item = createAbilityItem(userClass, ability);
			mInventory.setItem(P3_ABILITY_LOCS.get(iterator), item);
			ItemStack levelOne = createLevelItem(userClass, ability, 1, player);
			mInventory.setItem(P3_ABILITY_LOCS.get(iterator) + 1, levelOne);
			ItemStack levelTwo = createLevelItem(userClass, ability, 2, player);
			mInventory.setItem(P3_ABILITY_LOCS.get(iterator) + 2, levelTwo);
			ItemStack enhanceItem = createEnhanceItem(userClass, ability, player);
			mInventory.setItem(P3_ABILITY_LOCS.get(iterator++) + 3, enhanceItem);
		}

		//specs
		if (ScoreboardUtils.getScoreboardValue(player, UNLOCK_SPECS) >= UNLOCK_SPECS_MIN) {
			addSpecItem(userClass.mSpecOne, userClass.mSpecTwo, 1, player);
			addSpecItem(userClass.mSpecTwo, userClass.mSpecOne, 2, player);
		}

		//summary
		if (userClass.mDisplayItem != null) {
			ItemStack summaryItem = createBasicItem(userClass.mDisplayItem.getType(), "Class Skills", NamedTextColor.WHITE, false,
				"Pick your skills and, if unlocked, your specialization.", ChatColor.LIGHT_PURPLE);
			mInventory.setItem(COMMON_SUMMARY_LOC, summaryItem);
		}

		//back button
		ItemStack backButton = createBasicItem(Material.ARROW, "Back",
			NamedTextColor.GRAY, false, "Return to the class selection page.", ChatColor.GRAY);
		mInventory.setItem(COMMON_BACK_LOC, backButton);

		//possibly create reset spec item
		if (ScoreboardUtils.getScoreboardValue(player, "Specialization") != 0) {
			ItemStack specItem = createBasicItem(Material.RED_BANNER, "Reset Your Specialization", NamedTextColor.WHITE, false,
				"Click here to reset your specialization to select a new one.", ChatColor.LIGHT_PURPLE);
			mInventory.setItem(SKILL_PAGE_RESET_SPEC_LOC, specItem);
		}

		makeRemainingCountItems(player);
		fillEmpty();
	}

	public void makeSpecPage(PlayerClass userClass, PlayerSpec spec, Player player) {
		mInventory.clear();
		mCurrentPage = 4;
		int iterator = 0;
		for (Ability ability : spec.mAbilities) {
			ItemStack item = createAbilityItem(userClass, ability);
			mInventory.setItem(P4_SPEC_LOCS.get(iterator), item);
			ItemStack levelOne = createLevelItem(userClass, ability, 1, player);
			mInventory.setItem(P4_SPEC_LOCS.get(iterator) + 1, levelOne);
			ItemStack levelTwo = createLevelItem(userClass, ability, 2, player);
			mInventory.setItem(P4_SPEC_LOCS.get(iterator++) + 2, levelTwo);
		}

		//summary
		if (spec.mDisplayItem != null) {
			ItemStack summaryItem = createBasicItem(spec.mDisplayItem.getType(), "Specialization Skills", NamedTextColor.WHITE, false,
				"Pick your specialization skills.", ChatColor.LIGHT_PURPLE);
			mInventory.setItem(COMMON_SUMMARY_LOC, summaryItem);
		}

		//back button
		ItemStack backButton = createBasicItem(Material.ARROW, "Back",
				NamedTextColor.GRAY, false, "Return to the skill selection page.", ChatColor.GRAY);
		mInventory.setItem(COMMON_BACK_LOC, backButton);
		makeRemainingCountItems(player);
		fillEmpty();
	}

	public void applyAbilityChosen(int chosenSlot, Player player, int level) {
		ArrayList<Integer> currentLocations;
		int skillOffset;
		if (ScoreboardUtils.getScoreboardValue(player, R3_UNLOCK_SCOREBOARD) >= R3_UNLOCK_SCORE) {
			currentLocations = P3_ABILITY_LOCS;
		} else {
			currentLocations = P2_ABILITY_LOCS;
		}
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (ScoreboardUtils.getScoreboardValue(player, "Class") == oneClass.mClass) {
				//found class, find ability
				int abilityIndex = currentLocations.indexOf(chosenSlot);
				Ability selectedAbility = oneClass.mAbilities.get(abilityIndex);
				int currentLevel = ScoreboardUtils.getScoreboardValue(player, selectedAbility.mInfo.mScoreboardId);
				skillOffset = currentLevel > 2 ? 2 : 0;
				if (level == 0) {
					//clear ability data
					if (currentLevel > 2) {
						int currentEnhancement = ScoreboardUtils.getScoreboardValue(player, R3_ENHANCE_CURRENT);
						ScoreboardUtils.setScoreboardValue(player, R3_ENHANCE_CURRENT, currentEnhancement + 1);
					}
					ScoreboardUtils.setScoreboardValue(player, selectedAbility.mInfo.mScoreboardId, 0);
					int currentCount = ScoreboardUtils.getScoreboardValue(player, ABILITY_SKILLCOUNT);
					ScoreboardUtils.setScoreboardValue(player, ABILITY_SKILLCOUNT, currentCount + (currentLevel - skillOffset - level));
				} else if (level - currentLevel < 0) {
					//level clicked is lower than level existing
					ScoreboardUtils.setScoreboardValue(player, selectedAbility.mInfo.mScoreboardId, skillOffset + level);
					int currentCount = ScoreboardUtils.getScoreboardValue(player, ABILITY_SKILLCOUNT);
					ScoreboardUtils.setScoreboardValue(player, ABILITY_SKILLCOUNT, currentCount + (currentLevel - skillOffset - level));
				} else if (level - currentLevel > 0) {
					//level clicked is higher than level existing
					int currentCount = ScoreboardUtils.getScoreboardValue(player, ABILITY_SKILLCOUNT);
					if (currentCount >= level - currentLevel) {
						ScoreboardUtils.setScoreboardValue(player, ABILITY_SKILLCOUNT, currentCount - (level - currentLevel));
						ScoreboardUtils.setScoreboardValue(player, selectedAbility.mInfo.mScoreboardId, skillOffset + level);
					} else {
						player.sendMessage("You don't have enough skill points to select this skill!");
					}
				}
				makeSkillSelectPage(oneClass, player);
				if (AbilityManager.getManager() != null) {
					AbilityManager.getManager().updatePlayerAbilities(player, true);
				}
				return;
			}
		}
	}

	public void applySpecAbilityChosen(int chosenSlot, Player player, int level) {
		PlayerClass theClass = null;
		PlayerSpec spec = null;
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (ScoreboardUtils.getScoreboardValue(player, "Class") == oneClass.mClass) {
				theClass = oneClass;
				if (ScoreboardUtils.getScoreboardValue(player, "Specialization") == oneClass.mSpecOne.mSpecialization) {
					spec = oneClass.mSpecOne;
				} else if (ScoreboardUtils.getScoreboardValue(player, "Specialization") == oneClass.mSpecTwo.mSpecialization) {
					spec = oneClass.mSpecTwo;
				}
				break;
			}
		}
		if (spec != null) {
			//found class, find ability
			int abilityIndex = P4_SPEC_LOCS.indexOf(chosenSlot);
			Ability selectedAbility = spec.mAbilities.get(abilityIndex);
			int currentLevel = ScoreboardUtils.getScoreboardValue(player, selectedAbility.mInfo.mScoreboardId);
			if (level - currentLevel < 0) {
				//level clicked is lower than level existing
				ScoreboardUtils.setScoreboardValue(player, selectedAbility.mInfo.mScoreboardId, level);
				int currentCount = ScoreboardUtils.getScoreboardValue(player, SPEC_SKILLCOUNT);
				ScoreboardUtils.setScoreboardValue(player, SPEC_SKILLCOUNT, currentCount + (currentLevel - level));
			} else if (level - currentLevel > 0) {
				//level clicked is higher than level existing
				int currentCount = ScoreboardUtils.getScoreboardValue(player, SPEC_SKILLCOUNT);
				if (currentCount >= level - currentLevel) {
					ScoreboardUtils.setScoreboardValue(player, SPEC_SKILLCOUNT, currentCount - (level - currentLevel));
					ScoreboardUtils.setScoreboardValue(player, selectedAbility.mInfo.mScoreboardId, level);
				} else {
					player.sendMessage("You don't have enough specialization points to select this skill!");
				}
			}
			makeSpecPage(theClass, spec, player);
		}
		if (AbilityManager.getManager() != null) {
			AbilityManager.getManager().updatePlayerAbilities(player, true);
		}
	}

	public void applyEnhancementChosen(int chosenSlot, Player player, boolean add) {
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (ScoreboardUtils.getScoreboardValue(player, "Class") == oneClass.mClass) {
				//found class, find ability
				int abilityIndex = P3_ABILITY_LOCS.indexOf(chosenSlot);
				Ability selectedAbility = oneClass.mAbilities.get(abilityIndex);
				int currentLevel = ScoreboardUtils.getScoreboardValue(player, selectedAbility.mInfo.mScoreboardId);
				if (add) {
					//clear ability data
					ScoreboardUtils.setScoreboardValue(player, selectedAbility.mInfo.mScoreboardId, currentLevel + 2);
					int currentCount = ScoreboardUtils.getScoreboardValue(player, R3_ENHANCE_CURRENT);
					ScoreboardUtils.setScoreboardValue(player, R3_ENHANCE_CURRENT, currentCount - 1);
				} else {
					//level clicked is lower than level existing
					ScoreboardUtils.setScoreboardValue(player, selectedAbility.mInfo.mScoreboardId, currentLevel - 2);
					int currentCount = ScoreboardUtils.getScoreboardValue(player, R3_ENHANCE_CURRENT);
					ScoreboardUtils.setScoreboardValue(player, R3_ENHANCE_CURRENT, currentCount + 1);
				}
				makeSkillSelectPage(oneClass, player);
				if (AbilityManager.getManager() != null) {
					AbilityManager.getManager().updatePlayerAbilities(player, true);
				}
				return;
			}
		}
	}

	public void addSpecItem(PlayerSpec spec, PlayerSpec otherSpec, int specNumber, Player player) {
		if (spec.mDisplayItem == null) {
			return;
		}
		if (ScoreboardUtils.getScoreboardValue(player, spec.mSpecQuestScoreboard) < 100) {
			//not unlocked
			ItemStack specItem = createBasicItem(Material.BARRIER, "Unknown", NamedTextColor.RED, false,
					"You haven't unlocked this specialization yet.", ChatColor.WHITE);
			mInventory.setItem(SKILL_PAGE_SPEC_LOCS.get(specNumber - 1), specItem);
		} else if (ScoreboardUtils.getScoreboardValue(player, "Specialization") == otherSpec.mSpecialization) {
			//unlocked, but using other spec
			ItemStack specItem = createBasicItem(Material.BARRIER, spec.mSpecName, NamedTextColor.RED, false,
					"Reset your specialization to select a new one.", ChatColor.WHITE);
			mInventory.setItem(SKILL_PAGE_SPEC_LOCS.get(specNumber - 1), specItem);
		} else if (ScoreboardUtils.getScoreboardValue(player, "Specialization") == spec.mSpecialization) {
			//unlocked and already using this spec
			ItemStack specItem = createBasicItem(spec.mDisplayItem.getType(), spec.mSpecName, NamedTextColor.RED, false,
					"Click to view your specialization skills.", ChatColor.WHITE);
			mInventory.setItem(SKILL_PAGE_SPEC_LOCS.get(specNumber - 1), specItem);
		} else if (ScoreboardUtils.getScoreboardValue(player, "Specialization") == 0) {
			//unlocked and no spec selected
			ItemStack specItem = createBasicItem(spec.mDisplayItem.getType(), spec.mSpecName, NamedTextColor.RED, false,
					"Click to choose this specialization!", ChatColor.GRAY);
			if (spec.mDescription != null) {
				ItemMeta newMeta = specItem.getItemMeta();
				GUIUtils.splitLoreLine(newMeta, "Description: " + spec.mDescription, 30, ChatColor.YELLOW, false);
				specItem.setItemMeta(newMeta);
			}
			mInventory.setItem(SKILL_PAGE_SPEC_LOCS.get(specNumber - 1), specItem);
		}
	}

	public ItemStack createLevelItem(PlayerClass theClass, Ability ability, int level, Player player) {
		int getScore = ScoreboardUtils.getScoreboardValue(player, ability.getScoreboard());
		if (getScore > 2) {
			getScore -= 2;
		}
		Material newMat = getScore >= level ?
				Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
		return createBasicItem(newMat, "Level " + level,
				theClass.mClassColor, true, ability.mInfo.mDescriptions.get(level - 1), ChatColor.WHITE);
	}

	public ItemStack createEnhanceItem(PlayerClass theClass, Ability ability, Player player) {
		if (ability.mInfo.mDescriptions.size() == 3) {
			Material newMat;
			switch (ScoreboardUtils.getScoreboardValue(player, ability.getScoreboard())) {
				case 0 -> {
					newMat = Material.BARRIER;
					return createBasicItem(newMat, "Enhancement",
						theClass.mClassColor, true,
						"Cannot Select; Choose levels in the ability first. Description: " + ability.mInfo.mDescriptions.get(2),
						ChatColor.WHITE);
				}
				case 1, 2 -> newMat = Material.ORANGE_STAINED_GLASS_PANE;
				case 3, 4 -> newMat = Material.YELLOW_STAINED_GLASS_PANE;
				default -> {
					newMat = Material.BARRIER;
					return createBasicItem(newMat, "Unknown Level",
						theClass.mClassColor, true, "Unknown level for ability.", ChatColor.WHITE);
				}
			}
			return createBasicItem(newMat, "Enhancement",
				theClass.mClassColor, true, ability.mInfo.mDescriptions.get(2), ChatColor.WHITE);
		}

		return createBasicItem(Material.BARRIER, "No Option",
			theClass.mClassColor, true, "No Enhancement Created", ChatColor.WHITE);
	}

	public ItemStack createAbilityItem(PlayerClass theClass, Ability ability) {
		return createBasicItem(ability.mDisplayItem.getType(), ability.getDisplayName(),
				theClass.mClassColor, true, "Click here to remove your levels in this skill, and click the panes to the right to pick a level in this skill.", ChatColor.WHITE);
		//in case someone makes ability descriptions not the length of a short essay:
		//also need to getlore from the function call, then add to it if these lines are used
		//GUIUtils.splitLoreLine(meta, "Level 1: " + ability.mInfo.mDescriptions.get(0), 30, ChatColor.WHITE, true);
		//GUIUtils.splitLoreLine(meta, "Level 2: " + ability.mInfo.mDescriptions.get(1), 30, ChatColor.WHITE, false);
	}

	public ItemStack createClassItem(PlayerClass classToItemize, boolean otherChosen, boolean locked) {
		if (locked) {
			return createBasicItem(Material.BARRIER, classToItemize.mClassName,
					classToItemize.mClassColor, true, "You don't have the requirements to choose this class.", ChatColor.RED);
		}
		if (otherChosen) {
			return createBasicItem(Material.BARRIER, classToItemize.mClassName,
					classToItemize.mClassColor, true, "Reset your class to choose this one!", ChatColor.RED);
		}
		ItemStack newItem = createBasicItem(classToItemize.mDisplayItem.getType(), classToItemize.mClassName,
				classToItemize.mClassColor, true, "Click to choose this class!", ChatColor.GRAY);
		if (classToItemize.mClassDescription != null) {
			ItemMeta newMeta = newItem.getItemMeta();
			GUIUtils.splitLoreLine(newMeta, "Description: " + classToItemize.mClassDescription, 30, ChatColor.YELLOW, false);
			newItem.setItemMeta(newMeta);
		}
		if (classToItemize.mClassPassiveDescription != null) {
			ItemMeta newMeta = newItem.getItemMeta();
			GUIUtils.splitLoreLine(newMeta, "Passive: " + classToItemize.mClassPassiveDescription, 30, ChatColor.GREEN, false);
			newItem.setItemMeta(newMeta);
		}

		return newItem;
	}

	public ItemStack createBasicItem(Material mat, String name, NamedTextColor nameColor, boolean nameBold, String desc, ChatColor loreColor) {
		ItemStack item = new ItemStack(mat, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(name, nameColor)
				.decoration(TextDecoration.ITALIC, false)
				.decoration(TextDecoration.BOLD, nameBold));
		GUIUtils.splitLoreLine(meta, desc, 30, loreColor, true);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
		return item;
	}

	public void makeRemainingCountItems(Player player) {
		if (ScoreboardUtils.getScoreboardValue(player, R3_UNLOCK_SCOREBOARD) >= R3_UNLOCK_SCORE) {
			int currentEnhanceCount = ScoreboardUtils.getScoreboardValue(player, R3_ENHANCE_CURRENT);
			ItemStack summaryItem = createBasicItem(currentEnhanceCount == 0 ? Material.BARRIER : Material.ENCHANTING_TABLE, "Enhancement Points", NamedTextColor.WHITE, false,
				"You have " + currentEnhanceCount + " enhancement points remaining.", ChatColor.LIGHT_PURPLE);
			summaryItem.setAmount(currentEnhanceCount > 0 ? currentEnhanceCount : 1);
			mInventory.setItem(COMMON_REMAINING_ENHANCEMENTS_LOC, summaryItem);
		}
		if (ScoreboardUtils.getScoreboardValue(player, UNLOCK_SPECS) >= UNLOCK_SPECS_MIN) {
			int currentSpecCount = ScoreboardUtils.getScoreboardValue(player, SPEC_SKILLCOUNT);
			ItemStack summaryItem = createBasicItem(currentSpecCount == 0 ? Material.BARRIER : Material.SAND, "Specialization Points", NamedTextColor.WHITE, false,
					"You have " + currentSpecCount + " specialization points remaining.", ChatColor.LIGHT_PURPLE);
			summaryItem.setAmount(currentSpecCount > 0 ? currentSpecCount : 1);
			mInventory.setItem(COMMON_REMAINING_SPEC_LOC, summaryItem);
		}
		int currentSkillCount = ScoreboardUtils.getScoreboardValue(player, ABILITY_SKILLCOUNT);
		ItemStack summaryItem = createBasicItem(currentSkillCount == 0 ? Material.BARRIER : Material.GRASS_BLOCK, "Skill Points", NamedTextColor.WHITE, false,
				"You have " + currentSkillCount + " skill points remaining.", ChatColor.LIGHT_PURPLE);
		summaryItem.setAmount(currentSkillCount > 0 ? currentSkillCount : 1);
		mInventory.setItem(COMMON_REMAINING_SKILL_LOC, summaryItem);
	}

	public void fillEmpty() {
		for (int i = 0; i < 54; i++) {
			if (mInventory.getItem(i) == null) {
				mInventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
	}
}
