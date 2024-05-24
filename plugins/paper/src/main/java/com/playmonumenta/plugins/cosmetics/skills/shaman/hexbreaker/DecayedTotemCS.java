package com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DecayedTotemCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DECAYED_TOTEM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WITHER_ROSE;
	}

	private static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.fromRGB(13, 13, 13), 1.0f);
	private static final Particle.DustOptions GREEN = new Particle.DustOptions(Color.fromRGB(5, 120, 5), 1.0f);

	public void decayedTotemSpawn(Player player, ArmorStand stand) {
		stand.getWorld().playSound(stand, Sound.BLOCK_CONDUIT_AMBIENT,
			SoundCategory.PLAYERS, 20.0f, 1.2f);
		stand.getWorld().playSound(stand, Sound.ENTITY_SKELETON_HURT,
			SoundCategory.PLAYERS, 0.6f, 0.3f);
		stand.getWorld().playSound(stand, Sound.ENTITY_PHANTOM_DEATH,
			SoundCategory.PLAYERS, 0.5f, 0.2f);
	}

	public void decayedTotemTick(Player player, ArmorStand stand) {

	}

	public void decayedTotemAnchor(Player player, ArmorStand stand, LivingEntity target) {
		new PPLine(Particle.REDSTONE, stand.getEyeLocation(), target.getLocation()).countPerMeter(8).delta(0.03).data(BLACK).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, stand.getEyeLocation(), target.getLocation()).countPerMeter(8).delta(0.03).data(GREEN).spawnAsPlayerActive(player);
	}

	public void decayedTotemExpire(Player player, World world, Location standLocation) {
		new PartialParticle(Particle.SQUID_INK, standLocation, 5, 0.2, 1.1, 0.2, 0.1).spawnAsPlayerActive(player);
		world.playSound(standLocation, Sound.BLOCK_WOOD_BREAK,
			SoundCategory.PLAYERS, 0.7f, 0.5f);
	}
}
