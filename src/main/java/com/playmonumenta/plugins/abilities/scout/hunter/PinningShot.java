package com.playmonumenta.plugins.abilities.scout.hunter;

import java.util.Random;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

/*
 * Pinning Shot: When you shoot an enemy with a bow
 * for the first time, you automatically inflict Slowness
 * VII for 5/10 seconds. This can only affect each enemy once
 */
public class PinningShot extends Ability {

	public PinningShot(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "PinningShot";
	}

	private static final String PINNING_SHOT_METAKEY = "PinningShotMetakey";

	@Override
	public boolean LivingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (!damagee.hasMetadata(PINNING_SHOT_METAKEY)) {
			damagee.setMetadata(PINNING_SHOT_METAKEY, new FixedMetadataValue(mPlugin, null));
			mWorld.playSound(damagee.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
			mWorld.spawnParticle(Particle.FIREWORKS_SPARK, arrow.getLocation(), 25, 0, 0, 0, 0.2);
			mWorld.spawnParticle(Particle.SNOWBALL, arrow.getLocation(), 35, 0, 0, 0, 0.25);

			int duration = getAbilityScore() == 1 ? 20 * 5 : 20 * 10;
			damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration, 6));
		}
		return true;
	}

}
