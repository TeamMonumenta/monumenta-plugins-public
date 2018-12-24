package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;

public class PowerInjection extends Ability {
	private static final int POWER_INJECTION_RANGE = 16;
	private static final int POWER_INJECTION_1_STRENGTH_EFFECT_LVL = 1;
	private static final int POWER_INJECTION_2_STRENGTH_EFFECT_LVL = 2;
	private static final int POWER_INJECTION_SPEED_EFFECT_LVL = 0;
	private static final int POWER_INJECTION_DURATION = 20 * 20;
	private static final int POWER_INJECTION_COOLDOWN = 30 * 20;

	public PowerInjection(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.POWER_INJECTION;
		mInfo.scoreboardId = "PowerInjection";
		mInfo.cooldown = POWER_INJECTION_COOLDOWN;
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (arrow.isCritical() && (mPlayer.isSneaking())) {
			int powerInjection = getAbilityScore();
			LivingEntity targetEntity = EntityUtils.GetEntityAtCursor(mPlayer, POWER_INJECTION_RANGE, true, true, true);
			if (targetEntity != null && targetEntity instanceof Player) {
				Player targetPlayer = (Player) targetEntity;
				if (targetPlayer.getGameMode() != GameMode.SPECTATOR) {
					mWorld.spawnParticle(Particle.FLAME, targetPlayer.getLocation().add(0, 1, 0), 30, 1.0, 1.0, 1.0, 0.001);
					mWorld.playSound(targetPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.2f, 1.0f);
					mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.2f, 1.0f);

					int effectLvl = powerInjection == 1 ? POWER_INJECTION_1_STRENGTH_EFFECT_LVL : POWER_INJECTION_2_STRENGTH_EFFECT_LVL;

					mPlugin.mPotionManager.addPotion(targetPlayer, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, POWER_INJECTION_DURATION, effectLvl, false, true));
					if (powerInjection > 1) {
						mPlugin.mPotionManager.addPotion(targetPlayer, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, POWER_INJECTION_DURATION, POWER_INJECTION_SPEED_EFFECT_LVL, false, true));
					}

					putOnCooldown();

					arrow.remove();

					// In case this was particle spamming from basilisk arrows
					mPlugin.mProjectileEffectTimers.removeEntity(arrow);
				}
			}
		}
		return true;
	}
}
