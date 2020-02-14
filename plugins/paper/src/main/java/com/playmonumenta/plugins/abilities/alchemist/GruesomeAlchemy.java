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

public class GruesomeAlchemy extends Ability {
	private static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;
	private static final int GRUESOME_ALCHEMY_VULN = 4; //25%
	private static final int GRUESOME_ALCHEMY_SLOW = 1;
	public static final String GRUESOME_ALCHEMY_SCOREBOARD = "GruesomeAlchemy";

	public GruesomeAlchemy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Gruesome Alchemy");
		mInfo.scoreboardId = GRUESOME_ALCHEMY_SCOREBOARD;
	}

	@Override
	public boolean playerSplashPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				for (LivingEntity entity : affectedEntities) {
					if (EntityUtils.isHostileMob(entity)) {
						apply(entity);
					}
				}
			}
		}
		return true;
	}

	public void apply(LivingEntity mob) {
		PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.SLOW, GRUESOME_ALCHEMY_DURATION, GRUESOME_ALCHEMY_SLOW, false, true));
		if (getAbilityScore() > 1) {
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.UNLUCK, GRUESOME_ALCHEMY_DURATION, GRUESOME_ALCHEMY_VULN, false, true));
		}
	}

}
