package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;

/*
 * When you kill an enemy they give off a noxious cloud, dealing 4/8 damage and
 * 6/12s of Weakness to all targets within 3 blocks. Mobs dying from this cloud
 * can also trigger Purple Haze.
 *
 * TODO: Particle effects need flair, weakness not implemented
 */

public class PurpleHaze extends Ability {
	private static final int PURPLE_HAZE_1_DAMAGE = 4;
	private static final int PURPLE_HAZE_2_DAMAGE = 8;
	private static final int PURPLE_HAZE_1_WEAKNESS_DURATION = 6 * 20;
	private static final int PURPLE_HAZE_2_WEAKNESS_DURATION = 12 * 20;
	private static final double PURPLE_HAZE_RADIUS = 3;

	public PurpleHaze(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "PurpleHaze";
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mWorld.spawnParticle(Particle.SPELL_INSTANT, event.getEntity().getLocation(), 200, PURPLE_HAZE_RADIUS, PURPLE_HAZE_RADIUS, PURPLE_HAZE_RADIUS, 0.5f); //Rudimentary effects
		int damage = getAbilityScore() == 1 ? PURPLE_HAZE_1_DAMAGE : PURPLE_HAZE_2_DAMAGE;
		for (Mob mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), PURPLE_HAZE_RADIUS)) {
			EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
		}
	}
}
