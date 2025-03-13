package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FrostNovaCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.FROST_NOVA;
	}

	@Override
	public Material getDisplayItem() {
		return Material.ICE;
	}

	public void onCast(Plugin plugin, Player player, World world, double size) {
		new BukkitRunnable() {
			double mRadius = 0;
			final Location mLoc = player.getLocation().add(0, 0.15, 0);

			@Override
			public void run() {
				mRadius += 1.25;

				new PPCircle(Particle.SNOWFLAKE, mLoc, mRadius).count(40).extra(0.1).spawnAsPlayerActive(player);
				new PPCircle(Particle.FALLING_DUST, mLoc.clone().add(0, 0.5, 0), mRadius)
					.count(40).extra(0.65).delta(0.25)
					.data(Material.LIGHT_BLUE_WOOL.createBlockData())
					.spawnAsPlayerActive(player);
				new PPCircle(Particle.CRIT_MAGIC, mLoc.clone().add(0, 0.5, 0), mRadius)
					.count(20).extra(1).delta(0.25)
					.spawnAsPlayerActive(player);

				if (mRadius >= size + 1) {
					this.cancel();
				}
			}

		}.runTaskTimer(plugin, 0, 1);
		Location loc = player.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.CLOUD, loc, 25, 0, 0, 0, 0.35).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SPIT, loc, 35, 0, 0, 0, 0.45).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 0.7f);
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.1f, 0.7f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.2f, 2.0f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.4f, 2.0f);
	}

	public void enemyEffect(Plugin plugin, Player player, LivingEntity enemy) {
		//none here!
	}
}
