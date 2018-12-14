package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.World;

public class DefensiveLine extends Ability {

	private static final Integer DEFENSIVE_LINE_DURATION = 14 * 20;
	private static final float DEFENSIVE_LINE_RADIUS = 8.0f;
	private static final Integer DEFENSIVE_LINE_1_COOLDOWN = 50 * 20;
	private static final Integer DEFENSIVE_LINE_2_COOLDOWN = 30 * 20;

	public DefensiveLine(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 2;
		mInfo.specId = -1;
		mInfo.linkedSpell = Spells.DEFENSIVE_LINE;
		mInfo.scoreboardId = "DefensiveLine";
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.cooldown = getAbilityScore() == 1 ? DEFENSIVE_LINE_1_COOLDOWN : DEFENSIVE_LINE_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public boolean cast() {
		for (Player target : PlayerUtils.getNearbyPlayers(mPlayer, DEFENSIVE_LINE_RADIUS, true)) {
			// Don't buff players that have their class disabled
			if (target.getScoreboardTags().contains("disable_class")) {
				continue;
			}

			Location loc = target.getLocation();

			target.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 0.4f, 1.0f);
			mPlugin.mPotionManager.addPotion(target, PotionID.APPLIED_POTION,
			                                 new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,
			                                                  DEFENSIVE_LINE_DURATION,
			                                                  1, true, true));
		}

		ParticleUtils.explodingSphereEffect(mPlugin, mPlayer, DEFENSIVE_LINE_RADIUS, Particle.FIREWORKS_SPARK, 1.0f, Particle.CRIT, 1.0f);
		putOnCooldown();
		return true;
	}

	@Override
	public boolean runCheck() {
		//  If we're sneaking and we block with a shield we can attempt to trigger the ability.
		if (mPlayer.isSneaking()) {
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (offHand.getType() == Material.SHIELD || mainHand.getType() == Material.SHIELD) {
				return true;
			}
		}
		return false;
	}

}
