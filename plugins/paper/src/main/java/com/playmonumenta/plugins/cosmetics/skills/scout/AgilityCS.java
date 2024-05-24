package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class AgilityCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.AGILITY;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_PICKAXE;
	}

	public void agilityEffectLevelTwo(Player player, LivingEntity enemy) {

	}

	public void agilityEffectLevelOne(Player player, LivingEntity enemy) {

	}

	public void agilityEnhancementEffect(Player player) {
		Location loc = player.getLocation();
		player.playSound(loc, Sound.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.PLAYERS, 0.8f, 1.0f);
	}
}
