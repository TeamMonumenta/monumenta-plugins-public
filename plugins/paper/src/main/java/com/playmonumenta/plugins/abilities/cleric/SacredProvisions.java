package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
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

	public static final AbilityInfo<SacredProvisions> INFO =
		new AbilityInfo<>(SacredProvisions.class, "Sacred Provisions", SacredProvisions::new)
			.scoreboardId("SacredProvisions")
			.shorthandName("SP")
			.descriptions(
				"Players within 30 blocks of a cleric have a 20% chance to not consume food, potions, arrows, or durability when the respective item is used. Does not stack with multiple clerics.",
				"The chance is increased to 40%.",
				String.format(
					"Each player in the radius gains +%s%% healing bonus when they are at %s%% hunger or higher.",
					(int) (100 * ENHANCEMENT_HEALING_BONUS), (int) (100 * ENHANCEMENT_HEALING_HUNGER_REQUIREMENT / 20.0)))
			.displayItem(new ItemStack(Material.COOKED_BEEF, 1));

	private final double mChance;

	public SacredProvisions(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mChance = (isLevelOne() ? PROVISIONS_1_CHANCE : PROVISIONS_2_CHANCE) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CHANCE);
	}

	public boolean isInRange(Player player) {
		return CharmManager.getRadius(mPlayer, CHARM_RANGE, PROVISIONS_RANGE) >= mPlayer.getLocation().distance(player.getLocation());
	}

	public double getChance() {
		return mChance;
	}
}
