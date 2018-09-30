package com.playmonumenta.plugins.abilities.rogue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class Smokescreen extends Ability {

	private static final int SMOKESCREEN_RANGE = 7;
	private static final int SMOKESCREEN_WEAKNESS_EFFECT_LEVEL_1 = 0;
	private static final int SMOKESCREEN_WEAKNESS_EFFECT_LEVEL_2 = 1;
	private static final int SMOKESCREEN_DURATION = 8 * 20;
	private static final int SMOKESCREEN_SLOWNESS_EFFECT_LEVEL_1 = 1;
	private static final int SMOKESCREEN_SLOWNESS_EFFECT_LEVEL_2 = 2;
	private static final int SMOKESCREEN_COOLDOWN = 20 * 20;
	
	@Override
	public boolean cast(Player player) { 
		int smokeScreen = getAbilityScore(player);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), SMOKESCREEN_RANGE)) {
			int weaknessLevel = smokeScreen == 1 ? SMOKESCREEN_WEAKNESS_EFFECT_LEVEL_1 :
			                    SMOKESCREEN_WEAKNESS_EFFECT_LEVEL_2;
			int slownessLevel = smokeScreen == 1 ? SMOKESCREEN_SLOWNESS_EFFECT_LEVEL_1 :
			                    SMOKESCREEN_SLOWNESS_EFFECT_LEVEL_2;

			mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, SMOKESCREEN_DURATION, weaknessLevel, false, true));
			mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, SMOKESCREEN_DURATION, slownessLevel, false, true));
			
			Location loc = player.getLocation();
			mWorld.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 300, 2.5, 0.8, 2.5, 0.05);
			mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 600, 2.5, 0.2, 2.5, 0.1);
			mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.35f);

			mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.SMOKESCREEN, SMOKESCREEN_COOLDOWN);
		}
		return true;
	}
	
	@Override
	public AbilityInfo getInfo() { 
		AbilityInfo info = new AbilityInfo(this);
		info.classId = 4;
		info.specId = -1;
		info.linkedSpell = Spells.SMOKESCREEN;
		info.scoreboardId = "SmokeScreen";
		info.cooldown = SMOKESCREEN_COOLDOWN;
		info.trigger = AbilityTrigger.LEFT_CLICK;
		return info; 
	}
	
	@Override
	public boolean runCheck(Player player) {
		if (player.isSneaking()) {
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			if (mainHand != null && mainHand.getType() != Material.BOW && InventoryUtils.isSwordItem(mainHand))
				return true;
		}
		return false;
	}
	
}
