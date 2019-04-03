package com.playmonumenta.plugins.abilities.cleric;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class Rejuvenation extends Ability {

	private static final int REJUVENATION_RADIUS = 12;
	private static final String REJUVENATION_METADATA_KEY = "RejuvenationMetadataKey";

	public Rejuvenation(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "Rejuvenation";
	}

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		//  Don't trigger this if dead!
		if (!mPlayer.isDead()) {
			if (ticks % 60 == 0) {
				int rejuvenation = getAbilityScore();
				for (Player p : PlayerUtils.getNearbyPlayers(mPlayer, REJUVENATION_RADIUS, true)) {
					// Don't buff players that have their class disabled or who have PvP enabled
					if (p.getScoreboardTags().contains("disable_class") || AbilityManager.getManager().isPvPEnabled(p)) {
						continue;
					}

					if (MetadataUtils.checkOnceThisTick(mPlugin, p, REJUVENATION_METADATA_KEY)) {
						//  If this is us or we're allowing anyone to get it.
						if (p == mPlayer || rejuvenation > 1) {
							double oldHealth = p.getHealth();
							PlayerUtils.healPlayer(p, p.getMaxHealth() * 0.05);
							if (p.getHealth() > oldHealth) {
								mWorld.spawnParticle(Particle.HEART, (p.getLocation()).add(0, 2, 0), 1, 0.07, 0.07, 0.07, 0.001);
							}
						}
					}
				}
			}
		}
	}

}
