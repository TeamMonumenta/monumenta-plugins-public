package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.Collection;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Invigorating Odor: Alchemist potions give Speed 1 and
 * Regeneration 1 (0:10) on allies they hit, also your alch
 * pots deal +2/4 damage. At level 2, they also provide Resistance
 * 1 for 10 seconds.
 */
public class InvigoratingOdor extends Ability {

	private static final int INVIGORATING_1_DAMAGE = 2;
	private static final int INVIGORATING_2_DAMAGE = 4;
	private static final int INVIGORATING_DURATION = 20 * 10;

	public InvigoratingOdor(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "InvigoratingOdor";
	}

	@Override
	public boolean PlayerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				int invigoratingOdor = getAbilityScore();
				for (LivingEntity le : affectedEntities) {
					apply(mPlugin, mPlayer, le, invigoratingOdor);
				}
			}
		}
		return true;
	}

	public static void apply(Plugin plugin, Player damager, LivingEntity damagee, int score) {
		if (damagee instanceof Player) {
			Player player = (Player) damagee;
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, INVIGORATING_DURATION, 0, true, true));
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.REGENERATION, INVIGORATING_DURATION, 0, true, true));
			if (score > 1) {
				plugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 10, 0, true, true));
			}
		} else if (EntityUtils.isHostileMob(damagee)) {
			int damage = score == 1 ? INVIGORATING_1_DAMAGE : INVIGORATING_2_DAMAGE;
			EntityUtils.damageEntity(plugin, damagee, damage, damager);
		}
	}

}
