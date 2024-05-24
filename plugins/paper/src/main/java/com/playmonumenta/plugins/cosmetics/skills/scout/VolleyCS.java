package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class VolleyCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.VOLLEY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ARROW;
	}

	public void volleyEffect(Player player) {
		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 0.75f);
		world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1f);
		world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1.33f);
	}

	public void volleyHit(Player player, LivingEntity enemy) {

	}

	public void volleyBleed(Player player, LivingEntity enemy) {

	}

}
