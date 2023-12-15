package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ClassDisplayCustomInventory extends CustomInventory {
	public static final ArrayList<Integer> ABILITY_LOCS = new ArrayList<>(Arrays.asList(9, 10, 11, 12, 14, 15, 16, 17));
	public static final Integer SPEC_LOCATION = 38;

	private static final ArrayList<Integer> SPEC_ABILITY_LOCS = new ArrayList<>(Arrays.asList(40, 41, 42));

	private static final Material ABILITY_L1_MAT = Material.MANGROVE_SLAB;
	private static final Material ABILITY_L2_MAT = Material.MANGROVE_PLANKS;

	private static final Material SPEC_L1_MAT = Material.OXIDIZED_CUT_COPPER_SLAB;
	private static final Material SPEC_L2_MAT = Material.OXIDIZED_CUT_COPPER;
	private static final int BACK_LOC = 0;

	private static final MonumentaClasses mClasses = new MonumentaClasses();
	private final Player mTargetPlayer;
	private final Player mRequestingPlayer;
	private final boolean mFromPDGUI;

	public ClassDisplayCustomInventory(Player player) {
		this(player, player);
	}

	public ClassDisplayCustomInventory(Player requestingPlayer, Player targetPlayer) {
		this(requestingPlayer, targetPlayer, false);
	}

	public ClassDisplayCustomInventory(Player requestingPlayer, Player targetPlayer, boolean fromPDGUI) {
		super(requestingPlayer, 54, "Class Display GUI");
		mFromPDGUI = fromPDGUI;
		mTargetPlayer = targetPlayer;
		mRequestingPlayer = requestingPlayer;
		int currentClass = ScoreboardUtils.getScoreboardValue(targetPlayer, AbilityUtils.SCOREBOARD_CLASS_NAME);
		PlayerClass playerClass = null;
		PlayerSpec spec = null;
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (currentClass == oneClass.mClass) {
				playerClass = oneClass;
				if (ScoreboardUtils.getScoreboardValue(targetPlayer, AbilityUtils.SCOREBOARD_SPEC_NAME) == oneClass.mSpecOne.mSpecialization) {
					spec = oneClass.mSpecOne;
				} else if (ScoreboardUtils.getScoreboardValue(targetPlayer, AbilityUtils.SCOREBOARD_SPEC_NAME) == oneClass.mSpecTwo.mSpecialization) {
					spec = oneClass.mSpecTwo;
				}
				break;
			}
		}
		if (playerClass == null) {
			requestingPlayer.sendMessage("No class selected!");
			mInventory.close();
			return;
		}
		makePage(playerClass, spec, targetPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory ||
			    clickedItem == null) {
			return;
		}
		if (clickedItem.getType() == Material.BARRIER) {
			mInventory.close();
		}
		if (event.getSlot() == BACK_LOC) {
			mInventory.close();
			new PlayerDisplayCustomInventory(mRequestingPlayer, mTargetPlayer).openInventory(mRequestingPlayer, Plugin.getInstance());
		}
	}

	public void makePage(PlayerClass userClass, @Nullable PlayerSpec spec, Player player) {
		mInventory.clear();
		//make abilities
		int iterator = 0;
		for (AbilityInfo<?> ability : userClass.mAbilities) {
			ItemStack item = createAbilityItem(userClass, ability);
			mInventory.setItem(ABILITY_LOCS.get(iterator), item);
			ItemStack levelItem = createLevelItem(ability, player);
			GUIUtils.setGuiNbtTag(levelItem, "Skill", ability.getDisplayName());
			mInventory.setItem(ABILITY_LOCS.get(iterator) + 9, levelItem);
			ItemStack enhanceItem = createEnhanceItem(userClass, ability, player);
			GUIUtils.setGuiNbtTag(enhanceItem, "Skill", ability.getDisplayName());
			mInventory.setItem(ABILITY_LOCS.get(iterator++) + 18, enhanceItem);
		}

		//summary
		ItemStack summaryItem = GUIUtils.createBasicItem(userClass.mDisplayItem, userClass.mClassName, userClass.mClassColor, true,
			"", userClass.mClassColor);
		mInventory.setItem(4, summaryItem);

		if (mFromPDGUI) {
			ItemStack backItem = GUIUtils.createBasicItem(Material.ARROW, "Back to Player Display GUI", NamedTextColor.GRAY, true,
				"", NamedTextColor.WHITE);
			mInventory.setItem(0, backItem);
		}

		if (spec != null) {
			addSpecItems(userClass, spec, player);
		}

		//exit button
		ItemStack exitButton = GUIUtils.createBasicItem(Material.BARRIER, "Exit",
			NamedTextColor.GRAY, false, "", NamedTextColor.GRAY);
		mInventory.setItem(8, exitButton);

		fillEmptyAndSetPlainTags(userClass);
	}

	public void addSpecItems(PlayerClass playerClass, PlayerSpec spec, Player player) {
		ItemStack specItem = GUIUtils.createBasicItem(spec.mDisplayItem, spec.mSpecName, playerClass.mClassColor, true,
			"", NamedTextColor.GRAY);
		mInventory.setItem(SPEC_LOCATION, specItem);

		int iterator = 0;
		for (AbilityInfo<?> ability : spec.mAbilities) {
			ItemStack item = createAbilityItem(playerClass, ability);
			mInventory.setItem(SPEC_ABILITY_LOCS.get(iterator), item);
			ItemStack levelOne = createSpecLevelItem(ability, player);
			mInventory.setItem(SPEC_ABILITY_LOCS.get(iterator++) + 9, levelOne);
		}
	}

	public ItemStack createLevelItem(AbilityInfo<?> ability, Player player) {
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
		Material newMat = getScore == 2 ? ABILITY_L2_MAT :
			(getScore == 1 ? ABILITY_L1_MAT : Material.STRUCTURE_VOID);
		return GUIUtils.createBasicItem(newMat, 1,
			"Level " + getScore, NamedTextColor.GRAY, false, Component.empty(),
			30, true);
	}

	public ItemStack createSpecLevelItem(AbilityInfo<?> ability, Player player) {
		int getScore;
		String scoreboard = ability.getScoreboard();
		if (scoreboard == null) {
			getScore = 0;
		} else {
			getScore = ScoreboardUtils.getScoreboardValue(player, scoreboard);
		}
		Material newMat = getScore == 2 ? SPEC_L2_MAT :
			(getScore == 1 ? SPEC_L1_MAT : Material.STRUCTURE_VOID);
		return GUIUtils.createBasicItem(newMat, 1,
			"Level " + getScore, NamedTextColor.GRAY, false, Component.empty(),
			30, true);
	}

	public ItemStack createEnhanceItem(PlayerClass theClass, AbilityInfo<?> ability, Player player) {
		if (ability.getDescriptions().size() == 3) {
			Material newMat;
			TextComponent levelString;
			String scoreboard = ability.getScoreboard();
			switch (scoreboard == null ? 0 : ScoreboardUtils.getScoreboardValue(player, scoreboard)) {
				case 0, 1, 2 -> {
					levelString = Component.text("Not chosen", NamedTextColor.GRAY);
					newMat = Material.STRUCTURE_VOID;
				}
				case 3, 4 -> {
					levelString = Component.text("Chosen", NamedTextColor.GRAY);
					newMat = Material.GLOWSTONE;
				}
				default -> {
					newMat = Material.STRUCTURE_VOID;
					return GUIUtils.createBasicItem(newMat, "Unknown Level",
						theClass.mClassColor, true, "Unknown level for ability.", NamedTextColor.WHITE);
				}
			}
			return GUIUtils.createBasicItem(newMat, 1,
				"Enhancement", theClass.mClassColor, false,
				levelString, 30, true);
		}

		return GUIUtils.createBasicItem(Material.STRUCTURE_VOID, "No Option",
			theClass.mClassColor, true, "No Enhancement Created", NamedTextColor.WHITE);
	}

	public ItemStack createAbilityItem(PlayerClass theClass, AbilityInfo<?> ability) {
		TextComponent lore = ability.getSimpleDescription() != null ?
			Component.text(ability.getSimpleDescription(), NamedTextColor.GRAY) : Component.empty();
		Material item = ability.getDisplayItem();
		if (item == null) {
			item = theClass.mDisplayItem;
		}
		String name = ability.getDisplayName();
		if (name == null) {
			name = "";
		}
		return GUIUtils.createBasicItem(item, 1, name,
			theClass.mClassColor, false, lore, 30, true);
	}

	public void fillEmptyAndSetPlainTags(PlayerClass theClass) {
		for (ItemStack item : mInventory) {
			if (item != null) {
				ItemUtils.setPlainTag(item);
			}
		}
		GUIUtils.fillWithFiller(mInventory, theClass.mClassGlassFiller);
	}
}
