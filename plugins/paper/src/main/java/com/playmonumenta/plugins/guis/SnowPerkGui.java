package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.snowperks.CarbonCapture;
import com.playmonumenta.plugins.abilities.snowperks.CoalInsurance;
import com.playmonumenta.plugins.abilities.snowperks.CoalLauncher;
import com.playmonumenta.plugins.abilities.snowperks.CreeperMistletoe;
import com.playmonumenta.plugins.abilities.snowperks.FestiveSweater;
import com.playmonumenta.plugins.abilities.snowperks.IcicleBurst;
import com.playmonumenta.plugins.abilities.snowperks.Nutcracker;
import com.playmonumenta.plugins.abilities.snowperks.ShatterProofOrnament;
import com.playmonumenta.plugins.abilities.snowperks.ShinyWrappingPaper;
import com.playmonumenta.plugins.abilities.snowperks.SierhavenSnowglobe;
import com.playmonumenta.plugins.abilities.snowperks.SnowLeopardClaw;
import com.playmonumenta.plugins.abilities.snowperks.SnowyOwlFeather;
import com.playmonumenta.plugins.abilities.snowperks.StringLightHook;
import com.playmonumenta.plugins.abilities.snowperks.ToughCookie;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.guis.lib.Gui;
import com.playmonumenta.plugins.guis.lib.GuiItem;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.scoreboard;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class SnowPerkGui extends Gui {
	public static final Style SNOW_POINT_COLOR = Style.style(TextColor.color(0x74D2D2));
	public static final Style SNOW_ARROW_COLOR = Style.style(TextColor.color(0x236D85));
	public static final Style COAL_COLOR = Style.style(TextColor.color(0x736B63));
	public static final Style COALRUPTED_COLOR = Style.style(Location.KOAL.getColor());
	public static final Style ACHIEVEMENT_COLOR = Style.style(TextColor.color(0x0D5723)).decorate(TextDecoration.UNDERLINED);
	public static final Style ACHIEVEMENT_ARROW_COLOR = Style.style(TextColor.color(0x33D14));
	public static final String REMAINING_POINTS = "SnowPoints";
	public static final String TOTAL_POINTS = "TotalSnowPoints";
	public static final String COAL_UNTIL_POINTS = "CoalUntilPoints";
	public static final String COAL_COLLECTED = "LifetimeCoalCollected";
	public static final List<AbilityInfo<?>> PERKS = List.of(
		SierhavenSnowglobe.INFO,
		ShinyWrappingPaper.INFO,
		Nutcracker.INFO,
		ToughCookie.INFO,
		CoalInsurance.INFO,
		FestiveSweater.INFO,
		CoalLauncher.INFO,
		CarbonCapture.INFO,
		SnowLeopardClaw.INFO,
		SnowyOwlFeather.INFO,
		CreeperMistletoe.INFO,
		ShatterProofOrnament.INFO,
		IcicleBurst.INFO,
		StringLightHook.INFO
	);
	public static final List<AbilityInfo<?>> ACHIEVEMENT_PERKS = List.of(
		CreeperMistletoe.INFO,
		StringLightHook.INFO
	);
	private static final int[] PERK_POSITIONS = {10, 11, 12, 13, 14, 15, 16, 28, 29, 30, 31, 32, 33, 34};
	private static final List<TextColor> LIGHT_COLORS = List.of(TextColor.color(0xE6556D), TextColor.color(0xE6AF50), TextColor.color(0x51AB3F), TextColor.color(0x7E7EE6), TextColor.color(0xE65C95));

	private final BukkitRunnable mDescriptionRunnable; // animated holiday light borders!
	private final boolean mIsMaxPoints;
	private boolean mLightParity = true;
	private int mRainbowFrame = 0;

	public static class SnowPerkInfo<T extends Ability> extends AbilityInfo<T> {
		private int mSnowPointCost = 0;
		private @Nullable String mAdvancementReq = null;

		public SnowPerkInfo(Class<T> abilityClass, @Nullable String displayName, BiFunction<Plugin, Player, T> constructor) {
			super(abilityClass, displayName, constructor);
			canUse(player -> ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.COALRUPTED_SNOW_PERKS) && getLevelScore(player) > 0);
		}

		public SnowPerkInfo<T> snowPointCost(int cost) {
			mSnowPointCost = cost;
			return this;
		}

		public int snowPointCost() {
			return mSnowPointCost;
		}

		public SnowPerkInfo<T> advancementReq(String key) {
			mAdvancementReq = key;
			return this;
		}

		public @Nullable String advancementReq() {
			return mAdvancementReq;
		}

		@Override
		public SnowPerkInfo<T> scoreboardId(String scoreboardId) {
			super.scoreboardId(scoreboardId);
			return this;
		}

		@Override
		public SnowPerkInfo<T> linkedSpell(ClassAbility linkedSpell) {
			super.linkedSpell(linkedSpell);
			return this;
		}

		@Override
		public SnowPerkInfo<T> displayItem(Material displayItem) {
			super.displayItem(displayItem);
			return this;
		}

		@Override
		public SnowPerkInfo<T> description(Description<T> description) {
			super.description(description);
			return this;
		}
	}

	public SnowPerkGui(Player player) {
		super(player, GUIUtils.FILLER, Component.text("Snow Perk Selection"), 6 * 9);

		// GUI updating can be laggy, so only have the higher interval if the player is maxed and needs rainbow text
		mIsMaxPoints = ScoreboardUtils.getScoreboardValue(mPlayer, COAL_UNTIL_POINTS).orElse(0) == -1;
		if (mIsMaxPoints) {
			mDescriptionRunnable = new BukkitRunnable() {
				int mTimer = 0;
				@Override
				public void run() {
					// runs every 2 ticks, so we need to change lights every 15 runs
					mTimer = (mTimer + 1) % 15;
					if (mTimer == 0) {
						mLightParity = !mLightParity;
					}

					mRainbowFrame = (mRainbowFrame + 1) % 16;

					markDirty();
					update();
				}
			};
		} else {
			mDescriptionRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					mLightParity = !mLightParity;
					markDirty();
					update();
				}
			};
		}

		mDescriptionRunnable.runTaskTimer(Plugin.getInstance(), 0, mIsMaxPoints ? 2 : 30);
	}

	@Override
	protected void onClose(InventoryCloseEvent event) {
		mDescriptionRunnable.cancel();
	}

	@Override
	protected void render() {
		Component mainMenuDescription = new FormattedDescriptionBuilder<>().arrowColor(SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("You have %d out of %d *Snow Points* remaining.").styles(SNOW_POINT_COLOR)
				.statValues(scoreboard(REMAINING_POINTS), scoreboard(TOTAL_POINTS))
			.addLine()
			.addLine("*Snow Points* are used to unlock perks that").styles(SNOW_POINT_COLOR)
			.addLine("activate while inside *Coalrupted Sierhaven*!").styles(COALRUPTED_COLOR)
			.addLine()
			.addIfElse((a, p) -> mIsMaxPoints,
				d -> {
					List<Style> styles = new ArrayList<>();
					for (int i = 0; i < 16; i++) {
						styles.add(Style.style(TextColor.color(HSVLike.hsvLike(i / 16f, 0.6f, 0.9f))));
					}
					Collections.rotate(styles, mRainbowFrame);
					styles.add(SNOW_POINT_COLOR);
					return d.addLine("*C**o**n**g**r**a**t**u**l**a**t**i**o**n**s**!* You've maxed out your *Snow Points*!").styles(styles);
				},
				d -> d.addLine("Collect %d more *Coal* to get +%d *Snow Points*!").styles(COAL_COLOR, SNOW_POINT_COLOR)
					.statValues(scoreboard(COAL_UNTIL_POINTS), stat(2)))
			.addLine()
			.addStat("Total Coal Collected: %d")
				.statValues(scoreboard(COAL_COLLECTED))
			.addDashedLine()
			.get(mPlayer);

		mainMenuDescription = makeLinesJolly(mainMenuDescription, true, mLightParity);

		int remainingPoints = ScoreboardUtils.getScoreboardValue(mPlayer, REMAINING_POINTS).orElse(0);
		Component mainMenuName = DescriptionUtils.centeredComponent(mainMenuDescription, "Snow Points", SNOW_POINT_COLOR, true);
		GuiItem.builder().maxLoreLength(99)
			.name(mainMenuName)
			.lore(mainMenuDescription)
			.count(remainingPoints > 0 ? remainingPoints : 1)
			.material(remainingPoints > 0 ? Material.SNOW_BLOCK : Material.COAL_BLOCK)
			.set(this, 0, 4);

		Component resetDescription = new FormattedDescriptionBuilder<>().arrowColor(SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Reset your perks and all assigned")
			.addLine("points, allowing you to pick new")
			.addLine("*Snow Perks*.").styles(SNOW_POINT_COLOR)
			.addDashedLine()
			.addAction("Click to reset your perks.", DescriptionUtils.ACTION_SELECT)
			.get();
		resetDescription = makeLinesJolly(resetDescription, true, mLightParity);
		Component resetName = DescriptionUtils.centeredComponent(resetDescription, "Reset Snow Perks", SNOW_POINT_COLOR, true);
		GuiItem.builder(Material.POWDER_SNOW_BUCKET).maxLoreLength(99)
			.name(resetName)
			.lore(resetDescription)
			.onMouseClick(this::resetSnowPerks)
			.set(this, 5, 4);

		renderPerkItems();
	}

	private void renderPerkItems() {
		for (int i = 0; i < PERKS.size(); i++) {
			SnowPerkInfo<?> perk = (SnowPerkInfo<?>) PERKS.get(i);

			if (perk.getDisplayName() == null || perk.getDisplayItem() == null || perk.getScoreboard() == null) {
				continue;
			}

			String scoreboard = perk.getScoreboard();
			int pointCost = perk.snowPointCost();
			int currentPoints = ScoreboardUtils.getScoreboardValue(mPlayer, REMAINING_POINTS).orElse(0);
			boolean alreadySelected = ScoreboardUtils.getScoreboardValue(mPlayer, scoreboard).orElse(0) > 0;
			boolean noAchievement = perk.advancementReq() != null && !AdvancementUtils.checkAdvancement(mPlayer, perk.advancementReq());
			boolean enoughPoints = pointCost <= currentPoints;
			boolean isAchievementPerk = ACHIEVEMENT_PERKS.contains(perk);

			Component name = Component.text(perk.getDisplayName(), isAchievementPerk ? ACHIEVEMENT_COLOR : SNOW_POINT_COLOR).decorate(TextDecoration.BOLD)
				.append(Component.text(StringUtils.smallCaps(alreadySelected ? " [Active]" : " [Inactive]"), alreadySelected ? DescriptionUtils.GOLD : DescriptionUtils.DARK_GREY)
					.decoration(TextDecoration.BOLD, false).decoration(TextDecoration.UNDERLINED, false));

			Component instruction;
			Material paneColor;
			if (alreadySelected) {
				instruction = DescriptionUtils.actionLine("Perk already selected.", DescriptionUtils.ACTION_COMPLETED).appendNewline()
					.append(DescriptionUtils.actionLine("Click to deselect!", DescriptionUtils.ACTION_SELECT));
				paneColor = Material.CYAN_STAINED_GLASS_PANE;
			} else if (noAchievement) {
				instruction = DescriptionUtils.actionLine("Perk not unlocked!", DescriptionUtils.ACTION_DENIED);
				paneColor = Material.RED_STAINED_GLASS_PANE;
			} else if (enoughPoints) {
				instruction = DescriptionUtils.actionLine("Click to select this perk!", DescriptionUtils.ACTION_SELECT);
				paneColor = Material.WHITE_STAINED_GLASS_PANE;
			} else {
				instruction = DescriptionUtils.actionLine("Not enough points!", DescriptionUtils.ACTION_DENIED);
				paneColor = Material.BLACK_STAINED_GLASS_PANE;
			}

			Component description = perk.getDescription(1, mPlayer, false);
			description = description.appendNewline().append(instruction);
			description = makeLinesJolly(description, alreadySelected, mLightParity);

			Runnable togglePerk = () -> {
				if (alreadySelected) {
					// Already selected; run deselect perk logic
					ScoreboardUtils.setScoreboardValue(mPlayer, scoreboard, 0);
					ScoreboardUtils.setScoreboardValue(mPlayer, REMAINING_POINTS, currentPoints + pointCost);

					mPlayer.playSound(mPlayer, Sound.BLOCK_TRIAL_SPAWNER_PLACE, SoundCategory.PLAYERS, 0.8f, 0.9f);
				} else if (!enoughPoints || noAchievement) {
					// Action blocked due to something; error sound and return
					mPlayer.playSound(mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
					mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 0.8f, 0.75f);
				} else {
					// Allow selection to happen
					ScoreboardUtils.setScoreboardValue(mPlayer, scoreboard, 1);
					ScoreboardUtils.setScoreboardValue(mPlayer, REMAINING_POINTS, currentPoints - pointCost);

					if (isAchievementPerk) {
						mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 1f);
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 5/4f), 2);
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 3/2f), 4);
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 5/3f), 6);
					} else {
						mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 1f);
					}
					mPlayer.playSound(mPlayer, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 0.8f, 1.35f);
				}
				markDirty();
			};

			int position = PERK_POSITIONS[i];
			GuiItem.Builder perkItem = GuiItem.builder().maxLoreLength(99)
				.name(name)
				.lore(description)
				.onMouseClick(togglePerk);
			perkItem.material(perk.getDisplayItem()).set(this, position);
			perkItem.material(paneColor).set(this, position + 9);
		}
	}

	/**
	 * Replaces regular FormattedDescriptionBuilder dashed lines with dotted lines with animated lights.
	 * @param component the description to replace dashed lines with animated light lines
	 * @param lightEnabled whether or not the lights should be turned on or not.
	 * @param lightParity which frame of the light animation to use (2-frame animation)
	 * @return a component with animated light lines
	 */
	private Component makeLinesJolly(Component component, boolean lightEnabled, boolean lightParity) {
		for (int i = 0; i < 2; i++) {
			boolean lineParity = i % 2 == 0;
			component = component.replaceText(builder -> {
				builder.match("–+");
				builder.once();
				builder.replacement((matchResult, b) -> {
					Component line = Component.text("·", DescriptionUtils.BLACK.decorate(TextDecoration.BOLD));

					int width = matchResult.group().length() * 8;
					int dashPairsNeeded = (int) Math.ceil((width - 3) / 7d);
					for (int j = 0; j < dashPairsNeeded; j++) {
						boolean lightOn = lightEnabled && (j + (lightParity ^ lineParity ? 1 : 0)) % 2 == 0;
						TextColor color = LIGHT_COLORS.get(j % 5);

						line = line.append(Component.text("•", lightOn ? Style.style(color) : DescriptionUtils.BLACK))
							.append(Component.text("·", DescriptionUtils.BLACK));
					}
					return line;
				});
			});
		}

		return component;
	}

	private void resetSnowPerks() {
		ScoreboardUtils.setScoreboardValue(mPlayer, REMAINING_POINTS, ScoreboardUtils.getScoreboardValue(mPlayer, TOTAL_POINTS).orElse(6));
		for (AbilityInfo<?> perk : PERKS) {
			SnowPerkInfo<?> snowPerk = (SnowPerkInfo<?>) perk;
			if (snowPerk.getScoreboard() != null) {
				ScoreboardUtils.setScoreboardValue(mPlayer, snowPerk.getScoreboard(), 0);
			}
		}

		mPlayer.playSound(mPlayer, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1f, 1f);
		mPlayer.playSound(mPlayer, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 0.8f, 1f);
		mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 16/15f);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 1f), 3);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 4/5f), 6);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 2/3f), 9);

		markDirty();
	}
}
