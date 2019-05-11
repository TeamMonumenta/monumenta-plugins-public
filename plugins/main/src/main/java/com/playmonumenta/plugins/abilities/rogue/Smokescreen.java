package com.playmonumenta.plugins.abilities.rogue;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.rogue.assassin.Preparation;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Smokescreen extends Ability {

	private static final int SMOKESCREEN_RANGE = 7;
	private static final int SMOKESCREEN_WEAKNESS_EFFECT_LEVEL_1 = 0;
	private static final int SMOKESCREEN_WEAKNESS_EFFECT_LEVEL_2 = 1;
	private static final int SMOKESCREEN_DURATION = 8 * 20;
	private static final int SMOKESCREEN_SLOWNESS_EFFECT_LEVEL_1 = 1;
	private static final int SMOKESCREEN_SLOWNESS_EFFECT_LEVEL_2 = 2;
	private static final int SMOKESCREEN_COOLDOWN = 20 * 20;

	public Smokescreen(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.SMOKESCREEN;
		mInfo.scoreboardId = "SmokeScreen";
		mInfo.cooldown = SMOKESCREEN_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
	}

	@Override
	public void cast() {
		int smokeScreen = getAbilityScore();
		Location loc = mPlayer.getLocation();
		mWorld.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 300, 2.5, 0.8, 2.5, 0.05);
		mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 600, 2.5, 0.2, 2.5, 0.1);
		mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.35f);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), SMOKESCREEN_RANGE, mPlayer)) {
			int weaknessLevel = smokeScreen == 1 ? SMOKESCREEN_WEAKNESS_EFFECT_LEVEL_1 :
			                    SMOKESCREEN_WEAKNESS_EFFECT_LEVEL_2;
			int slownessLevel = smokeScreen == 1 ? SMOKESCREEN_SLOWNESS_EFFECT_LEVEL_1 :
			                    SMOKESCREEN_SLOWNESS_EFFECT_LEVEL_2;

			Preparation pp = (Preparation) AbilityManager.getManager().getPlayerAbility(mPlayer, Preparation.class);
			int ppDuration = 0;
			if (pp != null) {
				ppDuration = pp.getBonus(mInfo.linkedSpell);
			}
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, SMOKESCREEN_DURATION + ppDuration, weaknessLevel, false, true));
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, SMOKESCREEN_DURATION + ppDuration, slownessLevel, false, true));


			mPlugin.mTimers.AddCooldown(mPlayer.getUniqueId(), Spells.SMOKESCREEN, SMOKESCREEN_COOLDOWN);
		}
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking()) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			if (mainHand != null && mainHand.getType() != Material.BOW && InventoryUtils.isSwordItem(mainHand)) {
				return true;
			}
		}
		return false;
	}

}
