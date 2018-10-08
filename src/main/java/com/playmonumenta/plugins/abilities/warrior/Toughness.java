package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;

import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.World;

public class Toughness extends Ability {

	public Toughness(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 2;
		mInfo.specId = -1;
		mInfo.scoreboardId = "Toughness";
	}

	private void toughness(Player player) {
		int toughness = getAbilityScore(player);
		int healthBoost = toughness == 1 ? 0 : 1;
		mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.HEALTH_BOOST, 1000000, healthBoost, true, false));
		mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, 100, 4, true, false));
	}

	@Override
	public void setupClassPotionEffects(Player player) {
		toughness(player);
	}

	@Override
	public void PlayerRespawnEvent(Player player) {
		toughness(player);
	}
}
