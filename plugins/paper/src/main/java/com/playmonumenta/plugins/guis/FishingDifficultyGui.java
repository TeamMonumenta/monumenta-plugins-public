package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FishingDifficultyGui extends Gui {

	private static final Component TITLE = Component.text("Fish Shrine");
	private static final String DIFFICULTY_UNLOCKED_SCOREBOARD = "FishUnlockedDifficulties";
	private static final String DIFFICULTY_SELECTED_SCOREBOARD = "FishCombatDifficulty";
	private static final String SAND_DOLLAR_LOOTTABLE = "epic:r3/items/fishing/sand_dollar";
	private static final String[] DIFFICULTY_NAMES = {
		"Clear Skies",
		"Rainstorm",
		"Monsoon",
		"Typhoon"
	};
	private static final Component[] DIFFICULTY_TITLES = {
		Component.text("Pray for ").decoration(TextDecoration.ITALIC, false)
			.append(Component.text(DIFFICULTY_NAMES[0]).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)),
		Component.text("Pray for a ").decoration(TextDecoration.ITALIC, false)
			.append(Component.text(DIFFICULTY_NAMES[1]).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true).color(TextColor.fromHexString("#ed8e2f"))),
		Component.text("Pray for a ").decoration(TextDecoration.ITALIC, false)
			.append(Component.text(DIFFICULTY_NAMES[2]).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true).color(TextColor.fromHexString("#ed2f2f"))),
		Component.text("???").decoration(TextDecoration.ITALIC, true).decoration(TextDecoration.BOLD, true).color(NamedTextColor.DARK_GRAY)
	};
	private static final List<List<Component>> DIFFICULTY_DESCRIPTIONS = List.of(
		new ArrayList<>(List.of(
			Component.text("The waters seem calm...").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.DARK_GRAY)
		)),
		new ArrayList<>(Arrays.asList(
			Component.text("Mobs from fishing combat events become").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.DARK_GRAY),
			Component.text("empowered, are greater in numbers, and").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.DARK_GRAY),
			Component.text("will spawn faster.").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.DARK_GRAY)
		)),
		new ArrayList<>(Arrays.asList(
			Component.text("Mobs from fishing combat events become").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.DARK_GRAY),
			Component.text("greatly empowered, are far greater in").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.DARK_GRAY),
			Component.text("numbers, and will spawn faster.").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.DARK_GRAY)
		)),
		new ArrayList<>(List.of(Component.text("Not yet available.").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.DARK_GRAY)))
	);
	private static final Material[] DIFFICULTY_MATERIALS = {
		Material.WHITE_CONCRETE_POWDER,
		Material.ORANGE_CONCRETE_POWDER,
		Material.RED_CONCRETE_POWDER,
		Material.GRAY_CONCRETE_POWDER
	};
	private static final int[] DIFFICULTY_SAND_DOLLAR_COSTS = {0, 12, 24, 36};

	public FishingDifficultyGui(Player player) {
		super(player, 3 * 9, TITLE);
	}

	@Override
	protected void setup() {
		// Deep clone of the description lists.
		List<List<Component>> baseDescriptions = new ArrayList<>();
		for (List<Component> list : DIFFICULTY_DESCRIPTIONS) {
			baseDescriptions.add(new ArrayList<>(list));
		}

		setItem(1, 1, GUIUtils.createBasicItem(DIFFICULTY_MATERIALS[0], 1, DIFFICULTY_TITLES[0], baseDescriptions.get(0), true))
			.onClick((clickEvent) -> {
				mPlayer.playSound(mPlayer, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.PLAYERS, 1f, 2f);
				mPlayer.sendMessage(Component.text("Clear Skies selected.").color(NamedTextColor.GRAY));
				selectDifficulty(0);
				close();
			});

		tryAppendPurchaseInformation(baseDescriptions.get(1), 1);
		setItem(1, 3, GUIUtils.createBasicItem(DIFFICULTY_MATERIALS[1], 1, DIFFICULTY_TITLES[1], baseDescriptions.get(1), true))
			.onLeftClick(() -> {
				if (isDifficultyUnlocked(1)) {
					// Select the difficulty
					mPlayer.sendMessage(Component.text("Rainstorm selected.").color(NamedTextColor.GRAY));
					mPlayer.playSound(mPlayer, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 0.5f);
					selectDifficulty(1);
				} else {
					// Send error message
					mPlayer.sendMessage(Component.text("You do not have this difficulty unlocked.").color(NamedTextColor.RED));
					mPlayer.playSound(mPlayer, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1f, 1.5f);
				}
				close();
			})
			.onRightClick(() -> {
				if (isDifficultyUnlocked(1)) {
					// Select the difficulty
					mPlayer.sendMessage(Component.text("Rainstorm selected.").color(NamedTextColor.GRAY));
					mPlayer.playSound(mPlayer, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 0.5f);
					selectDifficulty(1);
					close();
				} else {
					if (tryUnlockDifficulty(1)) {
						update();
					}
				}
			});

		tryAppendPurchaseInformation(baseDescriptions.get(2), 2);
		setItem(1, 5, GUIUtils.createBasicItem(DIFFICULTY_MATERIALS[2], 1, DIFFICULTY_TITLES[2], baseDescriptions.get(2), true))
			.onLeftClick(() -> {
				if (isDifficultyUnlocked(2)) {
					// Select the difficulty
					mPlayer.sendMessage(Component.text("Monsoon selected.").color(NamedTextColor.GRAY));
					mPlayer.playSound(mPlayer, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 0.5f);
				} else {
					// Send error message
					mPlayer.sendMessage(Component.text("You do not have this difficulty unlocked.").color(NamedTextColor.RED));
					mPlayer.playSound(mPlayer, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1f, 1.5f);
				}
				close();
			})
			.onRightClick(() -> {
				if (isDifficultyUnlocked(2)) {
					// Select the difficulty
					mPlayer.sendMessage(Component.text("Monsoon selected.").color(NamedTextColor.GRAY));
					mPlayer.playSound(mPlayer, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 0.5f);
					close();
				} else {
					if (tryUnlockDifficulty(2)) {
						update();
					}
				}
			});

		tryAppendPurchaseInformation(baseDescriptions.get(3), 3);
		setItem(1, 7, GUIUtils.createBasicItem(DIFFICULTY_MATERIALS[3], 1, DIFFICULTY_TITLES[3], baseDescriptions.get(3), true))
			.onLeftClick(() -> {
				if (isDifficultyUnlocked(3)) {
					// Select the difficulty
					mPlayer.sendMessage(Component.text("Typhoon selected.").color(NamedTextColor.GRAY));
					mPlayer.playSound(mPlayer, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 0.5f);
				} else {
					// Send error message
					mPlayer.sendMessage(Component.text("You do not have this difficulty unlocked.").color(NamedTextColor.RED));
					mPlayer.playSound(mPlayer, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1f, 1.5f);
				}
				close();
			})
			.onRightClick(() -> {
				if (isDifficultyUnlocked(3)) {
					// Select the difficulty
					mPlayer.sendMessage(Component.text("Typhoon selected.").color(NamedTextColor.GRAY));
					mPlayer.playSound(mPlayer, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1f, 0.5f);
					close();
				} else {
					if (tryUnlockDifficulty(3)) {
						update();
					}
				}
			});
	}

	private int getUnlockedDifficulty() {
		return ScoreboardUtils.getScoreboardValue(mPlayer, DIFFICULTY_UNLOCKED_SCOREBOARD).orElse(0);
	}

	private void setUnlockedDifficulty(int difficulty) {
		ScoreboardUtils.setScoreboardValue(mPlayer, DIFFICULTY_UNLOCKED_SCOREBOARD, difficulty);
	}

	private void selectDifficulty(int difficulty) {
		ScoreboardUtils.setScoreboardValue(mPlayer, DIFFICULTY_SELECTED_SCOREBOARD, difficulty);
	}

	private boolean isDifficultyUnlocked(int difficulty) {
		return getUnlockedDifficulty() >= difficulty;
	}

	private boolean isDifficultyUnlockable(int difficulty) {
		return difficulty == (getUnlockedDifficulty() + 1);
	}

	private @Nullable ItemStack getSandDollarItem() {
		return InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKeyUtils.fromString(SAND_DOLLAR_LOOTTABLE));
	}

	private boolean paySandDollars(int amount) {
		ItemStack sandDollars = getSandDollarItem();

		if (sandDollars == null) {
			return false;
		}

		sandDollars.setAmount(amount);
		mPlayer.getInventory().removeItem(sandDollars);
		return true;
	}

	private boolean hasEnoughMaterialsToUnlockDifficulty(int difficulty) {
		ItemStack sandDollars = getSandDollarItem();

		if (sandDollars == null) {
			return false;
		}

		return mPlayer.getInventory().containsAtLeast(sandDollars, DIFFICULTY_SAND_DOLLAR_COSTS[difficulty]);
	}

	private void tryAppendPurchaseInformation(List<Component> lore, int difficulty) {
		if (isDifficultyUnlocked(difficulty) || difficulty == 0 || difficulty == 3) {
			return;
		}

		lore.add(Component.text(""));
		lore.add(Component.text("Right Click to purchase for %s Sand Dollars.".formatted(DIFFICULTY_SAND_DOLLAR_COSTS[difficulty]))
			.decoration(TextDecoration.ITALIC, false).color(NamedTextColor.GREEN));
	}

	private boolean tryUnlockDifficulty(int difficulty) {
		if (difficulty > 2) {
			mPlayer.playSound(mPlayer, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1f, 1.5f);
			mPlayer.sendMessage(Component.text("This difficulty is not yet purchasable.").color(NamedTextColor.RED));
			return false;
		}

		if (!isDifficultyUnlockable(difficulty)) {
			mPlayer.playSound(mPlayer, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1f, 1.5f);
			mPlayer.sendMessage(Component.text("Unlock the previous difficulty to be able to unlock this one!").color(NamedTextColor.RED));
			return false;
		}

		if (!hasEnoughMaterialsToUnlockDifficulty(difficulty)) {
			mPlayer.playSound(mPlayer, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1f, 1.5f);
			mPlayer.sendMessage(Component.text("You do not have enough materials to unlock this difficulty.").color(NamedTextColor.RED));
			return false;
		}

		if (!paySandDollars(DIFFICULTY_SAND_DOLLAR_COSTS[difficulty])) {
			return false;
		}

		setUnlockedDifficulty(difficulty);
		mPlayer.sendMessage(Component.text("You have now unlocked %s".formatted(DIFFICULTY_NAMES[difficulty])).decoration(TextDecoration.BOLD, true).color(NamedTextColor.GOLD));
		mPlayer.playSound(mPlayer, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1f, 1.5f);
		return true;
	}
}
