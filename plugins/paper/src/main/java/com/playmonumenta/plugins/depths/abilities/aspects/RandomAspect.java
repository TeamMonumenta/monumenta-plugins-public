package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RandomAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Mystery Box";

	public static final DepthsAbilityInfo<RandomAspect> INFO =
		new DepthsAbilityInfo<>(RandomAspect.class, ABILITY_NAME, RandomAspect::new, null, DepthsTrigger.WEAPON_ASPECT)
			.displayItem(new ItemStack(Material.BARREL))
			.description("Obtain a random ability. Transforms into a random other aspect after defeating floor 3.");

	public RandomAspect(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

}

