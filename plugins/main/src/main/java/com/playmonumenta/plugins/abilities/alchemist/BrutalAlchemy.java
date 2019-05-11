package com.playmonumenta.plugins.abilities.alchemist;

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
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class BrutalAlchemy extends Ability {
	private static final int BRUTAL_ALCHEMY_DAMAGE_1 = 3;
	private static final int BRUTAL_ALCHEMY_DAMAGE_2 = 5;
	private static final int BRUTAL_ALCHEMY_WITHER_1_DURATION = 4 * 20 + 10;
	private static final int BRUTAL_ALCHEMY_WITHER_2_DURATION = 6 * 20 + 10;
	public static final String BRUTAL_ALCHEMY_SCOREBOARD = "BrutalAlchemy";

	public BrutalAlchemy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = BRUTAL_ALCHEMY_SCOREBOARD;
	}

	@Override
	public boolean PlayerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				int brutalAlchemy = getAbilityScore();
				for (LivingEntity entity : affectedEntities) {
					if (EntityUtils.isHostileMob(entity)) {
						int damage = (brutalAlchemy == 1) ? BRUTAL_ALCHEMY_DAMAGE_1 : BRUTAL_ALCHEMY_DAMAGE_2;
						int duration = (brutalAlchemy == 1) ? BRUTAL_ALCHEMY_WITHER_1_DURATION : BRUTAL_ALCHEMY_WITHER_2_DURATION;
						EntityUtils.damageEntity(mPlugin, entity, damage, mPlayer);
						PotionUtils.applyPotion(mPlayer, entity, new PotionEffect(PotionEffectType.WITHER, duration, 1, false, true));
					}
				}
			}
		}
		return true;
	}
}
