package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SacredProvisions extends Ability {

	public static final int PROVISIONS_RANGE = 30;
	public static final float PROVISIONS_1_CHANCE = 0.2f;
	public static final float PROVISIONS_2_CHANCE = 0.4f;
	public static double ENHANCEMENT_HEALING_BONUS = 0.1;
	public static int ENHANCEMENT_HEALING_HUNGER_REQUIREMENT = 19;
	public static final String CHARM_CHANCE = "Sacred Provisions Chance";
	public static final String CHARM_RANGE = "Sacred Provisions Range";

	private double mChance;

	public SacredProvisions(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Sacred Provisions");
		mInfo.mScoreboardId = "SacredProvisions";
		mInfo.mShorthandName = "SP";
		mInfo.mDescriptions.add("Players within 30 blocks of a cleric have a 20% chance to not consume food, potions, arrows, or durability when the respective item is used. Does not stack with multiple clerics.");
		mInfo.mDescriptions.add("The chance is increased to 40%.");
		mInfo.mDescriptions.add(String.format(
			"Each player in the radius gains +%s%% healing bonus when they are at %s%% hunger or higher.",
			(int) (100 * ENHANCEMENT_HEALING_BONUS), (int) (100 * ENHANCEMENT_HEALING_HUNGER_REQUIREMENT / 20.0)));
		mDisplayItem = new ItemStack(Material.COOKED_BEEF, 1);

		mChance = (isLevelOne() ? PROVISIONS_1_CHANCE : PROVISIONS_2_CHANCE) * CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CHANCE);
	}

	public boolean isInRange(Player player) {
		return mPlayer != null && CharmManager.getRadius(mPlayer, SacredProvisions.CHARM_RANGE, PROVISIONS_RANGE) >= mPlayer.getLocation().distance(player.getLocation());
	}

	public double getChance() {
		return mChance;
	}
}
