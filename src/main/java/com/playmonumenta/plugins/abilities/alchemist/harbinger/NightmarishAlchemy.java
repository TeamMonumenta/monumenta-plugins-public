package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import java.util.Collection;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * Your Alchemist Potions deal +3/6 damage and inflict Shatter for 5/10 seconds.
 *
 * TODO: Shatter effect still needs implementing.
 */

public class NightmarishAlchemy extends Ability {
	private static final int NIGHTMARISH_ALCHEMY_1_DAMAGE = 3;
	private static final int NIGHTMARISH_ALCHEMY_2_DAMAGE = 6;
	private static final int NIGHTMARISH_ALCHEMY_1_SHATTER_DURATION = 5 * 20;
	private static final int NIGHTMARISH_ALCHEMY_2_SHATTER_DURATION = 10 * 20;

	public NightmarishAlchemy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "NightmarishAlchemy";
	}

	@Override
	public boolean PlayerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				int brutalAlchemy = getAbilityScore();
				int damage = getAbilityScore() == 1 ? NIGHTMARISH_ALCHEMY_1_DAMAGE : NIGHTMARISH_ALCHEMY_2_DAMAGE;
				int duration = getAbilityScore() == 1 ? NIGHTMARISH_ALCHEMY_1_SHATTER_DURATION : NIGHTMARISH_ALCHEMY_2_SHATTER_DURATION;
				for (LivingEntity entity : affectedEntities) {
					if (EntityUtils.isHostileMob(entity)) {
						EntityUtils.damageEntity(mPlugin, entity, damage, mPlayer);
						//SHATTER EFFECT HERE
					}
				}
			}
		}

		return true;
	}
}
