package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BowAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Aspect of the Bow";
	public static final double COOLDOWN_REDUCTION = 0.25;
	public static final double PASSIVE_ARROW_SAVE = 0.5;

	public static final DepthsAbilityInfo<BowAspect> INFO =
		new DepthsAbilityInfo<>(BowAspect.class, ABILITY_NAME, BowAspect::new, null, DepthsTrigger.WEAPON_ASPECT)
			.displayItem(new ItemStack(Material.BOW))
			.description("Your sneak fire with bow ability has " + StringUtils.multiplierToPercentage(COOLDOWN_REDUCTION) + "% reduced cooldown, and you have a " +
				             StringUtils.multiplierToPercentage(PASSIVE_ARROW_SAVE) + "% chance for arrows to not be consumed when using a bow or crossbow.");

	public BowAspect(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean playerConsumeArrowEvent() {
		if (FastUtils.RANDOM.nextDouble() < PASSIVE_ARROW_SAVE) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.2f, 1.0f);
			return false;
		}
		return true;
	}

	public static double getCooldownReduction(Player player) {
		if (player != null && AbilityManager.getManager().getPlayerAbility(player, BowAspect.class) != null) {
			return 1 - COOLDOWN_REDUCTION;
		}
		return 1;
	}

}

