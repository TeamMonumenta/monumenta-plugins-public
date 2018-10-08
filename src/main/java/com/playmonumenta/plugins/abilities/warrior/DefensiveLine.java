package com.playmonumenta.plugins.abilities.warrior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class DefensiveLine extends Ability {
	
	private static final Integer DEFENSIVE_LINE_DURATION = 14 * 20;
	private static final float DEFENSIVE_LINE_RADIUS = 8.0f;
	private static final Integer DEFENSIVE_LINE_1_COOLDOWN = 50 * 20;
	private static final Integer DEFENSIVE_LINE_2_COOLDOWN = 30 * 20;
	
	@Override
	public boolean cast(Player player) {
		for (Player target : PlayerUtils.getNearbyPlayers(player, DEFENSIVE_LINE_RADIUS, true)) {
			Location loc = target.getLocation();

			target.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 0.4f, 1.0f);
			mPlugin.mPotionManager.addPotion(target, PotionID.APPLIED_POTION,
			                                 new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,
			                                                  DEFENSIVE_LINE_DURATION,
			                                                  1, true, true));
		}

		ParticleUtils.explodingSphereEffect(mPlugin, player, DEFENSIVE_LINE_RADIUS, Particle.FIREWORKS_SPARK, 1.0f, Particle.CRIT, 1.0f);
		putOnCooldown(player);
		return true;
	}
	
	@Override
	public AbilityInfo getInfo() {
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 1;
		info.specId = -1;
		info.linkedSpell = Spells.DEFENSIVE_LINE;
		info.scoreboardId = "DefensiveLine";
		int cd = ScoreboardUtils.getScoreboardValue(player, info.scoreboardId) == 1 ? DEFENSIVE_LINE_1_COOLDOWN : DEFENSIVE_LINE_2_COOLDOWN;
		info.cooldown = cd;
		info.trigger = AbilityTrigger.RIGHT_CLICK;
		return info;
	}
	
	@Override
	public boolean runCheck(Player player) {
		//  If we're sneaking and we block with a shield we can attempt to trigger the ability.
		if (player.isSneaking()) {
			ItemStack offHand = player.getInventory().getItemInOffHand();
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			if (offHand.getType() == Material.SHIELD || mainHand.getType() == Material.SHIELD)
				return true;
		}
		return false;
	}

}
