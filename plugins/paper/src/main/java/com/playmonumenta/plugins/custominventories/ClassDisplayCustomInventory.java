package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.PlayerSpec;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ClassDisplayCustomInventory extends Gui {
	public static final ArrayList<Integer> ABILITY_LOCS = new ArrayList<>(Arrays.asList(9, 10, 11, 12, 14, 15, 16, 17));
	public static final Integer SPEC_LOCATION = 38;

	private static final ArrayList<Integer> SPEC_ABILITY_LOCS = new ArrayList<>(Arrays.asList(40, 41, 42));

	private static final Material ABILITY_L1_MAT = Material.MANGROVE_SLAB;
	private static final Material ABILITY_L2_MAT = Material.MANGROVE_PLANKS;

	private static final Material SPEC_L1_MAT = Material.OXIDIZED_CUT_COPPER_SLAB;
	private static final Material SPEC_L2_MAT = Material.OXIDIZED_CUT_COPPER;

	private static final MonumentaClasses mClasses = new MonumentaClasses();
	private final Player mTargetPlayer;
	private final Player mRequestingPlayer;
	private PlayerClass mClass;
	private @Nullable PlayerSpec mSpec = null;
	private final boolean mFromPDGUI;
	private boolean mGuiTextures;

	public ClassDisplayCustomInventory(Player player) {
		this(player, player);
	}

	public ClassDisplayCustomInventory(Player requestingPlayer, Player targetPlayer) {
		this(requestingPlayer, targetPlayer, false);
	}

	public ClassDisplayCustomInventory(Player requestingPlayer, Player targetPlayer, boolean fromPDGUI) {
		super(requestingPlayer, 54, Component.text("Class Display GUI"));
		mFromPDGUI = fromPDGUI;
		mTargetPlayer = targetPlayer;
		mRequestingPlayer = requestingPlayer;
		int currentClass = AbilityUtils.getClassNum(targetPlayer);
		for (PlayerClass oneClass : mClasses.mClasses) {
			if (currentClass == oneClass.mClass) {
				mClass = oneClass;
				int currentSpec = AbilityUtils.getSpecNum(targetPlayer);
				if (currentSpec == oneClass.mSpecOne.mSpecialization) {
					mSpec = oneClass.mSpecOne;
				} else if (currentSpec == oneClass.mSpecTwo.mSpecialization) {
					mSpec = oneClass.mSpecTwo;
				}
				break;
			}
		}
		if (mClass == null) {
			requestingPlayer.sendMessage("No class selected!");
			close();
			return;
		}
		mFiller = GUIUtils.createFiller(mClass.mClassGlassFiller);
		mGuiTextures = GUIUtils.getGuiTextureObjective(requestingPlayer);
	}

	@Override
	public void setup() {
		int iterator = 0;
		for (AbilityInfo<?> ability : mClass.mAbilities) {
			GuiItem item = createAbilityItem(mClass, ability);
			setItem(ABILITY_LOCS.get(iterator), item);
			GuiItem levelItem = createLevelItem(ability);
			setItem(ABILITY_LOCS.get(iterator) + 9, levelItem);
			GuiItem enhanceItem = createEnhanceItem(mTargetPlayer, ability);
			setItem(ABILITY_LOCS.get(iterator++) + 18, enhanceItem);
		}

		ItemStack summaryItem = GUIUtils.createBasicItem(mClass.mDisplayItem, mClass.mClassName, mClass.mClassColor, true);
		setItem(4, summaryItem);

		if (mFromPDGUI) {
			ItemStack backItem = GUIUtils.createBasicItem(Material.ARROW, "Back to Player Display GUI", NamedTextColor.GRAY, true);
			setItem(0, new GuiItem(backItem).onClick(event -> {
				close();
				new PlayerDisplayCustomInventory(mRequestingPlayer, mTargetPlayer).openInventory(mRequestingPlayer, mPlugin);
			}));
		}

		addSpecItems();

		ItemStack exitButton = GUIUtils.createBasicItem(Material.BARRIER, "Exit", NamedTextColor.GRAY);
		setItem(8, new GuiItem(exitButton).onClick(event -> close()));
	}

	public void addSpecItems() {
		if (mSpec == null) {
			return;
		}

		ItemStack specItem = GUIUtils.createBasicItem(mSpec.mDisplayItem, mSpec.mSpecName, mClass.mClassColor, true);
		setItem(SPEC_LOCATION, specItem);

		int iterator = 0;
		for (AbilityInfo<?> ability : mSpec.mAbilities) {
			GuiItem item = createAbilityItem(mClass, ability);
			setItem(SPEC_ABILITY_LOCS.get(iterator), item);
			GuiItem levelOne = createSpecLevelItem(ability, mTargetPlayer);
			setItem(SPEC_ABILITY_LOCS.get(iterator++) + 9, levelOne);
		}
	}

	public GuiItem createLevelItem(AbilityInfo<?> ability) {
		int getScore;
		String scoreboard = ability.getScoreboard();
		if (scoreboard == null) {
			getScore = 0;
		} else {
			getScore = ScoreboardUtils.getScoreboardValue(mTargetPlayer, scoreboard);
			if (getScore > 2) {
				getScore -= 2;
			}
		}
		Material newMat = getScore == 2 ? ABILITY_L2_MAT : (getScore == 1 ? ABILITY_L1_MAT : Material.STRUCTURE_VOID);
		ItemStack item = GUIUtils.createBasicItem(newMat, "Level " + getScore, NamedTextColor.GRAY, false, "Click to view description.", NamedTextColor.DARK_GRAY);
		GUIUtils.setGuiNbtTag(item, "Skill", ability.getDisplayName(), mGuiTextures);
		return createClickableItem(item, ability);
	}

	public GuiItem createSpecLevelItem(AbilityInfo<?> ability, Player player) {
		int getScore;
		String scoreboard = ability.getScoreboard();
		if (scoreboard == null) {
			getScore = 0;
		} else {
			getScore = ScoreboardUtils.getScoreboardValue(player, scoreboard);
		}
		Material newMat = getScore == 2 ? SPEC_L2_MAT :
			(getScore == 1 ? SPEC_L1_MAT : Material.STRUCTURE_VOID);
		return createClickableItem(GUIUtils.createBasicItem(newMat, "Level " + getScore, NamedTextColor.GRAY, false, "Click to view description.", NamedTextColor.DARK_GRAY), ability);
	}

	public GuiItem createEnhanceItem(Player player, AbilityInfo<?> ability) {
		if (ability.getDescriptions().size() == 3) {
			Material newMat;
			String levelString;
			String scoreboard = ability.getScoreboard();
			switch (scoreboard == null ? 0 : ScoreboardUtils.getScoreboardValue(player, scoreboard)) {
				case 0, 1, 2 -> {
					levelString = "Not Enhanced";
					newMat = Material.STRUCTURE_VOID;
				}
				case 3, 4 -> {
					levelString = "Enhanced";
					newMat = Material.GLOWSTONE;
				}
				default -> {
					newMat = Material.STRUCTURE_VOID;
					return new GuiItem(GUIUtils.createBasicItem(newMat, "Unknown Level",
						mClass.mClassColor, true, "Unknown level for ability.", NamedTextColor.WHITE));
				}
			}

			ItemStack item = GUIUtils.createBasicItem(newMat, levelString, NamedTextColor.GRAY, false, "Click to view description.", NamedTextColor.DARK_GRAY);
			GUIUtils.setGuiNbtTag(item, "Skill", ability.getDisplayName(), mGuiTextures);
			return createClickableItem(item, ability);
		}

		return new GuiItem(GUIUtils.createBasicItem(Material.STRUCTURE_VOID, "No Option",
			mClass.mClassColor, true, "No Enhancement Created", NamedTextColor.WHITE));
	}

	public GuiItem createAbilityItem(PlayerClass theClass, AbilityInfo<?> ability) {
		String lore = ability.getSimpleDescription() != null ? ability.getSimpleDescription() : "";
		Material item = ability.getDisplayItem();
		if (item == null) {
			item = theClass.mDisplayItem;
		}
		String name = ability.getDisplayName();
		if (name == null) {
			name = "";
		}
		return createClickableItem(GUIUtils.createBasicItem(item, name, theClass.mClassColor, false, lore, NamedTextColor.GRAY), ability);
	}

	public void sendDescriptionMessage(AbilityInfo<?> info) {
		close();
		mRequestingPlayer.sendMessage(info.getFormattedDescriptions(mTargetPlayer));
	}

	public GuiItem createClickableItem(ItemStack item, AbilityInfo<?> info) {
		return new GuiItem(item).onClick(event -> sendDescriptionMessage(info));
	}
}
