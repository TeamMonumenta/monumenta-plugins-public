package com.playmonumenta.plugins.abilities.rogue.swordsage;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class WindWalk extends Ability {

	/*
	 * Wind Walk: Sprinting and left-clicking grants you jump boost IV / V and
	 * Speed I / II for 6 seconds. Cooldown: 14 s
	 */

	public WindWalk(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 4;
		mInfo.specId = 1;
		mInfo.linkedSpell = Spells.WIND_WALK;
		mInfo.scoreboardId = "WindWalk";
		mInfo.cooldown = 20 * 14;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public boolean cast() {
		int windWalk = getAbilityScore();
		int jumpAmp = windWalk == 1 ? 3 : 4;
		int spdAmp = windWalk == 1 ? 0 : 1;
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP,
		                                 20 * 6, jumpAmp, true, false));
		mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED,
		                                 20 * 6, spdAmp, true, false));

		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1.75f);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 1f);

		mWorld.spawnParticle(Particle.SMOKE_NORMAL, mPlayer.getLocation(), 90, 0.25, 0.45, 0.25, 0.1);
		mWorld.spawnParticle(Particle.CLOUD, mPlayer.getLocation(), 20, 0.25, 0.45, 0.25, 0.15);
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSprinting()) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (mainHand != null && mainHand.getType() != Material.BOW && InventoryUtils.isSwordItem(mainHand)) {
				return true;
			}
		}
		return false;
	}

}
