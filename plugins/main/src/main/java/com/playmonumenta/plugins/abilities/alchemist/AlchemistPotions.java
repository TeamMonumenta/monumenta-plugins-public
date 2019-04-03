package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * This is a utility "ability" that makes it possible to give stacked potions for
 * Brutal Alchemy and Gruesome Alchemy in just one place
 */
public class AlchemistPotions extends Ability {

	public AlchemistPotions(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	/*
	 * Override the default canUse check to only succeed if player has EITHER
	 * Brutal OR Gruesome alchemy
	 */
	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, BrutalAlchemy.BRUTAL_ALCHEMY_SCOREBOARD) > 0 ||
		       ScoreboardUtils.getScoreboardValue(player, GruesomeAlchemy.GRUESOME_ALCHEMY_SCOREBOARD) > 0;
	}

	@Override
	public boolean PlayerThrewSplashPotionEvent(SplashPotion potion) {
		if (InventoryUtils.testForItemWithName(potion.getItem(), "Alchemist's Potion")) {
			mPlugin.mProjectileEffectTimers.addEntity(potion, Particle.SPELL);
			potion.setMetadata("AlchemistPotion", new FixedMetadataValue(mPlugin, 0));
		}
		return true;
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		int newPot = 1;
		if (mRandom.nextDouble() < 0.50) {
			newPot++;
		}

		AbilityUtils.addAlchemistPotions(mPlayer, newPot);
	}
}
