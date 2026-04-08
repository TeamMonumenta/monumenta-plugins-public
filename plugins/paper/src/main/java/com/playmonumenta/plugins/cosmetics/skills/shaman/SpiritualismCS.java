package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class SpiritualismCS implements CosmeticSkill {
	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SPIRITUALISM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_CHESTPLATE;
	}

	public void onCooldownRefresh(Player player) {
		Location loc = player.getLocation();
		player.playSound(loc, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.PLAYERS, 0.75f, 1.4f);
	}
}
