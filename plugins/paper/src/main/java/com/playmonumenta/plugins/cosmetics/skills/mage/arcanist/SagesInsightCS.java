package com.playmonumenta.plugins.cosmetics.skills.mage.arcanist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SagesInsightCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SAGES_INSIGHT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_STAR;
	}

	private static final float[] PITCHES = {1.6f, 1.8f, 1.6f, 1.8f, 2f};
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(222, 219, 36), 1.0f);

	public void insightTrigger(Plugin plugin, Player player, int resetSize) {
		World world = player.getWorld();
		Location loc = player.getLocation();
		new PartialParticle(Particle.REDSTONE, loc, 20, 1.4, 1.4, 1.4, COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 2.1, 0), 20, 0.5, 0.1, 0.5, 0.1).spawnAsPlayerActive(player);
		for (int i = 0; i < PITCHES.length; i++) {
			float pitch = PITCHES[i];
			new BukkitRunnable() {
				@Override
				public void run() {
					world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1, pitch);
				}
			}.runTaskLater(plugin, i);
		}
	}

	public void insightStackGain(Player player, DamageEvent event) {
		Location locD = event.getDamagee().getLocation().add(0, 1, 0);
		new PartialParticle(Particle.REDSTONE, locD, 15, 0.4, 0.4, 0.4, COLOR).spawnAsPlayerActive(player);
		new PartialParticle(Particle.EXPLOSION_NORMAL, locD, 15, 0, 0, 0, 0.2).spawnAsPlayerActive(player);
	}
}
