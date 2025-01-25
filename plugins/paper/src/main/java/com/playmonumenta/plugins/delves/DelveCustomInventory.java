package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.custominventories.BountyGui;
import com.playmonumenta.plugins.delves.abilities.Entropy;
import com.playmonumenta.plugins.delves.abilities.StatMultiplier;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DungeonUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

/**
 * GUI to display delve modifiers.
 */
public class DelveCustomInventory extends CustomInventory {

	private static final ItemStack SELECT_ALL_MOD_ITEM = GUIUtils.createBasicItem(
		Material.ENCHANTED_GOLDEN_APPLE,
		"Select all modifiers!",
		NamedTextColor.WHITE,
		true,
		""
	);
	private static final ItemStack REMOVE_ALL_MOD_ITEM = GUIUtils.createBasicItem(
		Material.BARRIER,
		"Remove all modifiers!",
		NamedTextColor.WHITE,
		true,
		""
	);
	private static final ItemStack BOUNTY_SELECTION_ITEM = GUIUtils.createBasicItem(
		Material.BARRIER,
		"Back to bounty selection!",
		NamedTextColor.WHITE,
		true,
		""
	);
	private static final ItemStack STARTING_ITEM = GUIUtils.createBasicItem(
		Material.OBSERVER,
		"Start delve!",
		NamedTextColor.WHITE,
		true,
		""
	);
	private static final ItemStack STARTING_ITEM_NOT_ENOUGH_POINTS = GUIUtils.createBasicItem(
		Material.OBSERVER,
		"Start delve!",
		NamedTextColor.WHITE,
		true,
		"- Requires " + DelvesUtils.MINIMUM_DEPTH_POINTS + " Delve Points to begin",
		NamedTextColor.RED
	);
	private static final ItemStack ROTATING_DELVE_MODIFIER_INFO = DelvesModifier.createIcon(
		Material.MAGENTA_GLAZED_TERRACOTTA,
		Component.text("Rotating Modifier", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
				 .decoration(TextDecoration.ITALIC, false),
		new String[] {
			"Some of these modifiers are randomly available each week.",
			"Selecting at least one will result in 25% increased XP."
		}
	);
	private static final ItemStack EXPERIMENTAL_DELVE_MODIFIER_INFO = DelvesModifier.createIcon(
		Material.CYAN_GLAZED_TERRACOTTA,
		Component.text("Experimental Modifier", NamedTextColor.AQUA, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false),
		new String[] {
			"This is a one-time event modifier that will not be coming back."
		}
	);
	private static final ItemStack LEFT_ARROW_ITEM = GUIUtils.createBasicItem(
		Material.ARROW,
		"Previous Page",
		NamedTextColor.WHITE,
		true,
		""
	);
	private static final ItemStack RIGHT_ARROW_ITEM = GUIUtils.createBasicItem(
		Material.ARROW,
		"Next Page",
		NamedTextColor.WHITE,
		true,
		""
	);

	private static final Map<String, String> DUNGEON_FUNCTION_MAPPINGS = new HashMap<>();
	private static final Map<String, String> CHALL_FUNCTION_MAPPINGS = new HashMap<>();

	static {
		DUNGEON_FUNCTION_MAPPINGS.put("white", "function monumenta:lobbies/d1/new");
		DUNGEON_FUNCTION_MAPPINGS.put("orange", "function monumenta:lobbies/d2/new");
		DUNGEON_FUNCTION_MAPPINGS.put("magenta", "function monumenta:lobbies/d3/new");
		DUNGEON_FUNCTION_MAPPINGS.put("lightblue", "function monumenta:lobbies/d4/new");
		DUNGEON_FUNCTION_MAPPINGS.put("yellow", "function monumenta:lobbies/d5/new");
		DUNGEON_FUNCTION_MAPPINGS.put("willows", "function monumenta:lobbies/db1/new");
		DUNGEON_FUNCTION_MAPPINGS.put("reverie", "function monumenta:lobbies/dc/new");
		DUNGEON_FUNCTION_MAPPINGS.put("lime", "function monumenta:lobbies/d6/new");
		DUNGEON_FUNCTION_MAPPINGS.put("pink", "function monumenta:lobbies/d7/new");
		DUNGEON_FUNCTION_MAPPINGS.put("gray", "function monumenta:lobbies/d8/new");
		DUNGEON_FUNCTION_MAPPINGS.put("lightgray", "function monumenta:lobbies/d9/new");
		DUNGEON_FUNCTION_MAPPINGS.put("cyan", "function monumenta:lobbies/d10/new");
		DUNGEON_FUNCTION_MAPPINGS.put("purple", "function monumenta:lobbies/d11/new");
		DUNGEON_FUNCTION_MAPPINGS.put("teal", "function monumenta:lobbies/dtl/new");
		DUNGEON_FUNCTION_MAPPINGS.put("forum", "function monumenta:lobbies/dff/new");
		DUNGEON_FUNCTION_MAPPINGS.put("shiftingcity", "function monumenta:lobbies/drl2/new");
		DUNGEON_FUNCTION_MAPPINGS.put("ruin", "function monumenta:lobbies/dmas/new");
		DUNGEON_FUNCTION_MAPPINGS.put("portal", "function monumenta:lobbies/dps/new");
		DUNGEON_FUNCTION_MAPPINGS.put("blue", "function monumenta:lobbies/d12/new");
		DUNGEON_FUNCTION_MAPPINGS.put("brown", "function monumenta:lobbies/d13/new");

		CHALL_FUNCTION_MAPPINGS.put("white", "function monumenta:lobbies/d1/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("orange", "function monumenta:lobbies/d2/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("magenta", "function monumenta:lobbies/d3/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("lightblue", "function monumenta:lobbies/d4/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("yellow", "function monumenta:lobbies/d5/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("willows", "function monumenta:lobbies/db1/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("reverie", "function monumenta:lobbies/dc/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("lime", "function monumenta:lobbies/d6/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("pink", "function monumenta:lobbies/d7/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("gray", "function monumenta:lobbies/d8/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("lightgray", "function monumenta:lobbies/d9/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("cyan", "function monumenta:lobbies/d10/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("purple", "function monumenta:lobbies/d11/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("teal", "function monumenta:lobbies/dtl/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("forum", "function monumenta:lobbies/dff/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("shiftingcity", "function monumenta:lobbies/drl2/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("blue", "function monumenta:lobbies/d12/new_challenge");
		CHALL_FUNCTION_MAPPINGS.put("brown", "function monumenta:lobbies/d13/new_challenge");
	}

	private static final int MODS_ROW = 5;

	private static final int TOTAL_POINT_SLOT = 0;
	private static final int START_SLOT = 8;
	private static final int PRESET_SLOT = 18;
	private static final int PAGE_LEFT_SLOT = 45;
	private static final int PAGE_RIGHT_SLOT = 53;
	private static final int GUI_IDENTIFIER_LOC_L = 36;
	private static final int GUI_IDENTIFIER_LOC_R = 44;

	private final Player mOwner;
	private final String mDungeonName;
	private final boolean mGuiTextures;
	private final Config mConfig;

	private final Map<DelvesModifier, Integer> mPointSelected;

	private int mPage;
	private int mTotalPoint;
	private int mAlreadyRolledEntropy;

	/**
	 * Configuration options for this DelveCustomInventory.
	 *
	 * @param editable    Whether the player should be allowed to change delve
	 *                    points shown in this menu.
	 * @param startable   Whether the player should be allowed to start
	 *                    a delve using this menu.
	 * @param preset      The delve preset shown in this menu, if any.
	 */
	public record Config(boolean editable, boolean startable, @Nullable DelvePreset preset) {
		/**
		 * Returns the default configuration.
		 * - editable: false
		 * - startable: false
		 * - allowBounty: false
		 * - preset: null
		 */
		public Config() {
			this(false, false, null);
		}

		/**
		 * Returns a new clone of this config with editable
		 * set to isEditable.
		 */
		public Config editable(boolean isEditable) {
			return new Config(isEditable, startable, preset);
		}

		/**
		 * Returns a new clone of this config with startable
		 * set to isStartable.
		 */
		public Config startable(boolean isStartable) {
			return new Config(editable, isStartable, preset);
		}

		/**
		 * Returns a new clone of this config with allowBounty
		 * set to isAllowBounty.
		 */
		public Config preset(DelvePreset newPreset) {
			return new Config(editable, startable, newPreset);
		}

	}

	/**
	 * Returns a mapping of the points initially selected when
	 * this menu is shown to the player.
	 *
	 * With no delve preset, the player will be shown
	 * the delve modifiers in their current dungeon.
	 *
	 * Precondition: mOwner, mDungeonName, mConfig are initialized.
	 */
	private Map<DelvesModifier, Integer> getInitialPoints() {
		if (mConfig.preset() != null) {
			return new HashMap<>(mConfig.preset().mModifiers);
		}
		return Arrays.stream(DelvesModifier.values())
					 .collect(Collectors.toMap(
						mod -> mod,
						mod -> DelvesUtils.getDelveModLevel(mOwner, mDungeonName, mod),
						(a, b) -> b,
						HashMap::new
					 ));
	}

	public DelveCustomInventory(Player owner, String dungeon, Config config) {
		super(owner, 54, "Delve Modifiers " + (config.editable() ? "Selection" : "Selected"));
		mOwner = owner;
		mDungeonName = dungeon;
		mConfig = config;
		mGuiTextures = GUIUtils.getGuiTextureObjective(owner);

		mPointSelected = getInitialPoints();

		// Pre-existing entropy should be accounted for.
		// Entropy contained in presets still needs to be rolled.
		mAlreadyRolledEntropy = mConfig.preset() == null ? mPointSelected.getOrDefault(DelvesModifier.ENTROPY, 0) : 0;

		mPage = 0;
		mTotalPoint = 0;

		GUIUtils.setGuiNbtTag(SELECT_ALL_MOD_ITEM, "texture", "all_delve_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(REMOVE_ALL_MOD_ITEM, "texture", "clear_delve_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(STARTING_ITEM, "texture", "start_delve_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(STARTING_ITEM_NOT_ENOUGH_POINTS, "texture", "start_delve_2", mGuiTextures);
		GUIUtils.setGuiNbtTag(ROTATING_DELVE_MODIFIER_INFO, "texture", "rotateinfo_delve_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(BOUNTY_SELECTION_ITEM, "texture", "backtobounty_delve_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(LEFT_ARROW_ITEM, "texture", "left_delve_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(RIGHT_ARROW_ITEM, "texture", "right_delve_1", mGuiTextures);

		loadInv();
	}

	/**
	 * There are 7 entries per page.
	 */
	private static int pageRelToAbsIndex(int page, int indexOnPage) {
		return page * 7 + indexOnPage;
	}

	/**
	 * Recalculates point total from mPointSelected, and stores it
	 * in mTotalPoint.
	 */
	private void updatePointTotal() {
		mTotalPoint = mPointSelected.entrySet()
									.stream()
									.mapToInt(entry -> entry.getValue() * entry.getKey().getPointsPerLevel())
									.sum();

		// Pre-existing entropy should only be single-counted!
		int entropyExtraPoints =
			Entropy.getDepthPointsAssigned(mPointSelected.getOrDefault(DelvesModifier.ENTROPY, 0)) -
			Entropy.getDepthPointsAssigned(mAlreadyRolledEntropy);
		int entropyMaxAssignable =
			DelvesModifier.entropyAssignable()
						  .stream()
						  .mapToInt(mod -> DelvesUtils.getMaxPointAssignable(mod, 1000) - mPointSelected.getOrDefault(mod, 0))
						  .sum();
		mTotalPoint += Math.min(entropyExtraPoints, entropyMaxAssignable);

		if (mTotalPoint > DelvesUtils.MAX_DEPTH_POINTS) {
			mTotalPoint = DelvesUtils.MAX_DEPTH_POINTS;
		}

		if (mTotalPoint < 0) {
			mTotalPoint = 0;
		}
	}

	/**
	 * Retrieves Bukkit numbered slot from row and column.
	 * @param row up to down
	 * @param col left to right
	 */
	private static int rowColToIndex(int row, int col) {
		return row * 9 + col;
	}

	/**
	 * Places items in the UI.
	 */
	private void loadInv() {
		mInventory.clear();

		mInventory.setItem(GUI_IDENTIFIER_LOC_L, GUIUtils.createGuiIdentifierItem("gui_delve_1_l", mGuiTextures));
		mInventory.setItem(GUI_IDENTIFIER_LOC_R, GUIUtils.createGuiIdentifierItem("gui_delve_1_r", mGuiTextures));

		updatePointTotal();

		mInventory.setItem(TOTAL_POINT_SLOT, getSummary());
		if (mConfig.startable()) {
			mInventory.setItem(START_SLOT, mTotalPoint < 5 ? STARTING_ITEM_NOT_ENOUGH_POINTS : STARTING_ITEM);
		}

		List<DelvesModifier> mods = getAvailableModifiers();

		for (int i = 0; i < 7; i++) {
			int absoluteIndex = pageRelToAbsIndex(mPage, i);
			if (absoluteIndex >= mods.size()) {
				break;
			}

			DelvesModifier mod = mods.get(absoluteIndex);
			if (mod == null) {
				continue;
			}

			mInventory.setItem(rowColToIndex(MODS_ROW, i + 1), mod.getIcon());

			if (DelvesModifier.rotatingDelveModifiers().contains(mod)) {
				mInventory.setItem(rowColToIndex(0, i + 1), ROTATING_DELVE_MODIFIER_INFO);
			}
			if (DelvesModifier.experimentalDelveModifiers().contains(mod)) {
				mInventory.setItem(rowColToIndex(0, i + 1), EXPERIMENTAL_DELVE_MODIFIER_INFO);
			}

			int level = mPointSelected.getOrDefault(mod, 0);

			// Can happen in challenge delves
			if (level > DelvesUtils.MODIFIER_RANK_CAPS.getOrDefault(mod, 0)) {
				ItemStack stack = DelvesUtils.getRankItem(mod, 1, level);
				if (stack == null) {
					continue;
				}
				GUIUtils.setGuiNbtTag(stack, "texture", "rank_delve_1", mGuiTextures);
				mInventory.setItem(rowColToIndex(MODS_ROW - 1, i + 1), stack);
				continue;
			}

			for (int j = 0; j < 5; j++) {
				ItemStack stack = DelvesUtils.getRankItem(mod, j + 1, level);
				if (stack == null) {
					continue;
				}

				GUIUtils.setGuiNbtTag(stack, "texture", j >= level ? "rank_delve_2" : "rank_delve_1", mGuiTextures);
				mInventory.setItem(rowColToIndex(MODS_ROW - (j + 1), i + 1), stack);
			}

		}

		if (mPage > 0) {
			mInventory.setItem(PAGE_LEFT_SLOT, LEFT_ARROW_ITEM);
		} else if (mConfig.editable()) {
			mInventory.setItem(PAGE_LEFT_SLOT, REMOVE_ALL_MOD_ITEM);
		} else if (mConfig.preset() != null && !mConfig.preset().isDungeonChallengePreset()) {
			mInventory.setItem(PAGE_LEFT_SLOT, BOUNTY_SELECTION_ITEM);
		}

		if (mods.size() > pageRelToAbsIndex(mPage + 1, 0)) {
			mInventory.setItem(PAGE_RIGHT_SLOT, RIGHT_ARROW_ITEM);
		} else if (mConfig.editable()) {
			mInventory.setItem(PAGE_RIGHT_SLOT, SELECT_ALL_MOD_ITEM);
		}

		// Remark: This relies on the current exact count of delve mods being 13, so
		// this fits in spot 7 of the second page! If a new one is added,
		// make sure to change the PRESET_SLOT and mPage condition.
		if (mPage == 1 && mDungeonName.equals("ring") && mConfig.editable()) {
			int presetId = ScoreboardUtils.getScoreboardValue(mOwner, DelvePreset.PRESET_SCOREBOARD)
										  .orElse(0);
			DelvePreset delvePreset = DelvePreset.getDelvePreset(presetId);
			if (delvePreset != null) {
				ItemStack presetItem = GUIUtils.createBasicItem(
					delvePreset.mDisplayItem,
					"Use Bounty Preset",
					NamedTextColor.WHITE,
					true,
					delvePreset.mName,
					NamedTextColor.AQUA
				);
				mInventory.setItem(PRESET_SLOT, presetItem);
			}
		}

		GUIUtils.fillWithFiller(mInventory);
	}

	/**
	 * Returns a list of available delve modifiers
	 * corresponding to the dungeon name this GUI
	 * was opened with.
	 */
	private List<DelvesModifier> getAvailableModifiers() {
		List<DelvesModifier> mods = DelvesModifier.valuesList();
		List<DelvesModifier> experimentalMods = DelvesUtils.getExperimentalDelveModifier();
		for (DelvesModifier experimental : DelvesModifier.experimentalDelveModifiers()) {
			if (!experimentalMods.contains(experimental)) {
				mods.remove(experimental);
			}
		}

		if (mDungeonName.startsWith("ring")) {
			mods.removeAll(DelvesModifier.rotatingDelveModifiers());
			mods.remove(DelvesModifier.ENTROPY);
			return mods;
		}

		// Developers
		if (mOwner.getGameMode() == GameMode.CREATIVE) {
			return mods;
		}

		List<DelvesModifier> weeklyMods = DelvesUtils.getWeeklyRotatingModifier();
		for (DelvesModifier rotating : DelvesModifier.rotatingDelveModifiers()) {
			boolean fromPreviousWeek = mPointSelected.getOrDefault(rotating, 0) != 0;
			if (!fromPreviousWeek && !weeklyMods.contains(rotating)) {
				mods.remove(rotating);
			}
		}

		if (mDungeonName.startsWith("portal") || mDungeonName.startsWith("ruin")) {
			mods.remove(DelvesModifier.FRAGILE);
		}

		if (mDungeonName.startsWith("depths")) {
			mods.removeAll(DelvesModifier.rotatingDelveModifiers());
			mods.removeAll(DelvesModifier.experimentalDelveModifiers());
			mods.remove(DelvesModifier.ENTROPY);
			mods.remove(DelvesModifier.TWISTED);
		}

		if (mDungeonName.startsWith("zenith")) {
			mods.removeAll(DelvesModifier.rotatingDelveModifiers());
			mods.removeAll(DelvesModifier.experimentalDelveModifiers());
			mods.remove(DelvesModifier.ENTROPY);
		}

		return mods;
	}

	/**
	 * Gets an ItemStack describing the statistics of this delve.
	 */
	private ItemStack getSummary() {
		int depthPoints = mTotalPoint;

		ItemStack item = new ItemStack(Material.SOUL_LANTERN, Math.max(1, depthPoints));
		GUIUtils.setGuiNbtTag(item, "texture", mTotalPoint > 0 ? "summary_delve_1" : "summary_delve_2", mGuiTextures);

		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Delve Summary", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

		List<Component> lore = new ArrayList<>();

		lore.add(Component.text(String.format("%d Delve Points Assigned", depthPoints), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));

		lore.add(Component.text(""));

		lore.add(Component.text("Stat Multipliers from Delve Points:", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		double damageMultiplier = StatMultiplier.getDamageMultiplier(depthPoints);
		double healthMultiplier = StatMultiplier.getHealthMultiplier(depthPoints);
		double speedMultiplier = StatMultiplier.getSpeedMultiplier(depthPoints);

		NamedTextColor damageMultiplierColor;
		if (damageMultiplier >= 1.75) {
			damageMultiplierColor = NamedTextColor.DARK_RED;
		} else if (damageMultiplier >= 1.45) {
			damageMultiplierColor = NamedTextColor.RED;
		} else {
			damageMultiplierColor = NamedTextColor.GRAY;
		}

		lore.add(Component.text(String.format("- Damage Multiplier: x%.3f", damageMultiplier), damageMultiplierColor).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Health Multiplier: x%.3f", healthMultiplier), damageMultiplierColor).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Speed Multiplier: x%.3f", speedMultiplier), damageMultiplierColor).decoration(TextDecoration.ITALIC, false));

		lore.add(Component.text(""));

		DungeonUtils.DungeonCommandMapping mapping = DungeonUtils.DungeonCommandMapping.getByShard(mDungeonName);
		boolean exalted = mapping != null && mapping.getTypeName() != null && ScoreboardUtils.getScoreboardValue(mOwner, mapping.getTypeName()).orElse(0) == 1;
		double dungeonMultiplier = StatMultiplier.getStatCompensation(mDungeonName, exalted);
		lore.add(Component.text("Stat Multipliers from Base Dungeon:", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Damage Multiplier: x%.3f", dungeonMultiplier), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Health Multiplier: x%.3f", dungeonMultiplier), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		lore.add(Component.text(""));

		if (!(mDungeonName.equals("depths") || mDungeonName.equals("corridors"))) {
			double baseAmount = DelveLootTableGroup.getDelveMaterialTableChance(DelvesUtils.MINIMUM_DEPTH_POINTS, 9001);
			if (!(mDungeonName.equals("portal") || mDungeonName.equals("ruin"))) {
				List<Double> multipliers = new ArrayList<>();
				for (int i = 1; i <= 4; i++) {
					multipliers.add(DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, i) / baseAmount);
				}

				lore.add(Component.text("Delve Material Multipliers (Not Counting Loot Scaling):", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));

				for (int i = 1; i <= 4; i++) {
					double multiplier = multipliers.get(i - 1);
					String s = i == 1 ? "" : "s";
					String plus = i == 4 ? "+" : "";
					// i Player(s): xX.XX
					String text = String.format("- %d%s Player%s: x%.2f", i, plus, s, multiplier);
					NamedTextColor color = NamedTextColor.GRAY;
					if (multiplier <= 0) {
						text = "  " + text;
						color = NamedTextColor.DARK_GRAY;
					} else if (multiplier == DelveLootTableGroup.getDelveMaterialTableChance(9001, i) / baseAmount) {
						text += " (Capped)";
						color = NamedTextColor.YELLOW;
					}
					lore.add(Component.text(text, color).decoration(TextDecoration.ITALIC, false));
				}
			} else {
				double multiplier = DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, 9001) / baseAmount;
				String text = String.format("x%.2f", multiplier);
				NamedTextColor color = NamedTextColor.GRAY;
				if (multiplier <= 0) {
					color = NamedTextColor.DARK_GRAY;
				} else if (multiplier == DelveLootTableGroup.getDelveMaterialTableChance(9001, 9001) / baseAmount) {
					text += " (Capped)";
					color = NamedTextColor.YELLOW;
				}
				lore.add(Component.text("Delve Material Multiplier: ", NamedTextColor.WHITE).append(Component.text(text, color)).decoration(TextDecoration.ITALIC, false));
			}
		}

		lore.add(Component.text(""));

		if (mapping != null && mapping.getDelvePreset() != null && mapping.getDelvePreset().isDungeonChallengePreset() && DelvePreset.validatePresetModifiers(mPointSelected, mapping.getDelvePreset(), false)) {
			lore.add(Component.text("- Challenge Mode active", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("- Challenge Mode not active", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		}
		if (depthPoints >= DelvesUtils.MAX_DEPTH_POINTS) {
			lore.add(Component.text("- All Delves Modifiers advancement granted upon completion", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("- All Delves Modifiers advancement not granted upon completion", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		}

		meta.lore(lore);

		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);

		return item;
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		if (!mInventory.equals(event.getClickedInventory())) {
			return;
		}

		Player playerWhoClicked = (Player) event.getWhoClicked();
		int slot = event.getSlot();

		List<DelvesModifier> mods = getAvailableModifiers();

		int column = slot % 9;
		int row = slot / 9;
		if (mConfig.editable() && 1 <= column && column <= 7) {

			int index = pageRelToAbsIndex(mPage, column - 1);
			if (index < mods.size()) {
				DelvesModifier mod = mods.get(index);

				if (row == MODS_ROW) {
					mPointSelected.put(mod, 0);
				} else {
					int finalPoint = DelvesUtils.getMaxPointAssignable(mod, 5 - row);
					mPointSelected.put(mod, finalPoint);
					playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.BLOCK_STONE_PLACE, SoundCategory.PLAYERS, 1f, 1.5f);
				}
				if (mPointSelected.get(DelvesModifier.COLOSSAL) != null) {
					if (mod == DelvesModifier.CHANCECUBES && mPointSelected.get(DelvesModifier.COLOSSAL) > 0) {
						mPointSelected.put(DelvesModifier.COLOSSAL, 0);
						playerWhoClicked.sendMessage(Component.text("This modifier is not compatible with Colossal! Colossal has been unselected.", NamedTextColor.RED));
					}
				}
				if (mPointSelected.get(DelvesModifier.CHANCECUBES) != null) {
					if (mod == DelvesModifier.COLOSSAL && mPointSelected.get(DelvesModifier.CHANCECUBES) > 0) {
						mPointSelected.put(DelvesModifier.CHANCECUBES, 0);
						playerWhoClicked.sendMessage(Component.text("This modifier is not compatible with Chance Cubes! Chance Cubes has been unselected.", NamedTextColor.RED));
					}
				}
				if (mPointSelected.get(DelvesModifier.ENTROPY) != null) {
					if (mod == DelvesModifier.CHANCECUBES && mPointSelected.get(DelvesModifier.ENTROPY) > 0) {
						mPointSelected.put(DelvesModifier.ENTROPY, 0);
						playerWhoClicked.sendMessage(Component.text("This modifier is not compatible with Entropy! Entropy has been unselected.", NamedTextColor.RED));
					}
				}
				if (mPointSelected.get(DelvesModifier.CHANCECUBES) != null) {
					if (mod == DelvesModifier.ENTROPY && mPointSelected.get(DelvesModifier.CHANCECUBES) > 0) {
						mPointSelected.put(DelvesModifier.CHANCECUBES, 0);
						playerWhoClicked.sendMessage(Component.text("This modifier is not compatible with Chance Cubes! Chance Cubes has been unselected.", NamedTextColor.RED));
					}
				}

				// Editing the entropy mod at all should mean
				// that existing already rolled mods are left
				// on purposefully and you want more random rolls
				if (mod == DelvesModifier.ENTROPY) {
					mAlreadyRolledEntropy = 0;
				}
			}
		}

		if (slot == PAGE_RIGHT_SLOT) {
			if (mods.size() > pageRelToAbsIndex(mPage + 1, 0)) {
				mPage++;
				playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.5f);
			} else if (mConfig.editable()) {
				if ((event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT)) {
					playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 1f, 0.5f);
					mAlreadyRolledEntropy = 0;
					List<DelvesModifier> experimentalModifiers = DelvesModifier.experimentalDelveModifiers();
					for (DelvesModifier mod : getAvailableModifiers()) {
						if (experimentalModifiers.contains(mod)) {
							mPointSelected.put(mod, 0);
						} else {
							mPointSelected.put(mod, DelvesUtils.getMaxPointAssignable(mod, 1000));
						}
					}
				} else {
					playerWhoClicked.sendMessage(Component.text("You must shift-click to enable all delve mods!", NamedTextColor.RED));
					event.setCancelled(true);
				}
			}
		}

		if (slot == PAGE_LEFT_SLOT) {
			if (mPage > 0) {
				mPage--;
				playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.5f);
			} else if (mConfig.editable()) {
				playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1f, 0.5f);
				mPointSelected.forEach((mod, value) -> mPointSelected.put(mod, 0));
				mAlreadyRolledEntropy = 0;
			} else if (mConfig.preset() != null && !mConfig.preset().isDungeonChallengePreset()) {
				this.close();
				try {
					new BountyGui(playerWhoClicked, 3, 0).open();
				} catch (Exception e) {
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);
				}
			}
		}

		if (slot == PRESET_SLOT && mPage == 1 && mDungeonName.equals("ring") && mConfig.editable()) {
			int presetId = ScoreboardUtils.getScoreboardValue(mOwner, DelvePreset.PRESET_SCOREBOARD)
										  .orElse(0);
			DelvePreset delvePreset = DelvePreset.getDelvePreset(presetId);
			if (delvePreset != null) {
				mAlreadyRolledEntropy = 0;
				mPointSelected.putAll(delvePreset.mModifiers);
			}
		}

		if (slot == START_SLOT && mConfig.startable()) {
			if (mTotalPoint >= 5) {
				if (mPointSelected.containsKey(DelvesModifier.ENTROPY)) {
					int entropyPoints =
						Entropy.getDepthPointsAssigned(mPointSelected.get(DelvesModifier.ENTROPY)) -
						Entropy.getDepthPointsAssigned(mAlreadyRolledEntropy);
					List<DelvesModifier> entropyableMods = DelvesModifier.entropyAssignable();

					while (entropyPoints > 0) {
						if (entropyableMods.isEmpty()) {
							break;
						}
						DelvesModifier mod = entropyableMods.get(FastUtils.RANDOM.nextInt(entropyableMods.size()));
						int oldValue = mPointSelected.getOrDefault(mod, 0);
						if (oldValue == DelvesUtils.getMaxPointAssignable(mod, oldValue + 1)) {
							entropyableMods.remove(mod);
							continue;
						}
						mPointSelected.put(mod, oldValue + 1);
						entropyPoints--;
					}
				}

				int presetId = 0;
				if (mConfig.preset() != null && DelvePreset.validatePresetModifiers(mPointSelected, mConfig.preset(), true)) {
					presetId = mConfig.preset().getId();
				}

				DelvesManager.savePlayerData(mOwner, mDungeonName, mPointSelected, presetId);
				mInventory.clear();
				playerWhoClicked.closeInventory();

				if (!ServerProperties.getShardName().equals(mDungeonName)) {
					String dungeonFunc = mConfig.preset() != null && mConfig.preset().isDungeonChallengePreset() ? CHALL_FUNCTION_MAPPINGS.get(mDungeonName) : DUNGEON_FUNCTION_MAPPINGS.get(mDungeonName);
					if (dungeonFunc == null) {
						playerWhoClicked.sendMessage(Component.text("Unable to find dungeon transfer function, please report to TM!", NamedTextColor.RED));
					} else {
						Bukkit.getScheduler()
							  .runTaskLater(
									Plugin.getInstance(),
									() -> NmsUtils.getVersionAdapter()
												  .runConsoleCommandSilently("execute as " + mOwner.getName() + " at @s run " + dungeonFunc),
									0
							  );
					}
				}
			} else {
				playerWhoClicked.sendMessage(Component.text("You need at least 5 delve points to start a delve!", NamedTextColor.RED));
			}
		}

		loadInv();
	}

	@Override
	public void inventoryClose(InventoryCloseEvent event) {
		if (mConfig.editable() && mConfig.preset() != null && !mConfig.preset().isDungeonChallengePreset()) {
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> new DelvePresetSelectionGui(mOwner, mDungeonName).open());
		}
	}
}
