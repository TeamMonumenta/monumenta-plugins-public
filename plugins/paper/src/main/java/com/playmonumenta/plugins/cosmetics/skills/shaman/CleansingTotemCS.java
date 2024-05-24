package com.playmonumenta.plugins.cosmetics.skills.shaman;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CleansingTotemCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CLEANSING_TOTEM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLUE_STAINED_GLASS;
	}

	public static final Particle.DustOptions DUST_CLEANSING_RING = new Particle.DustOptions(Color.fromRGB(0, 87, 255), 1.25f);

	public void cleansingTotemSpawn(World world, Location standLocation) {
		world.playSound(standLocation, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 2.0f, 1.3f);
		world.playSound(standLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8f, 2.0f);
	}

	public void cleansingTotemHeal(Player player) {

	}

	public void cleansingTotemCleanse(Player player, Location standLocation, double radius) {
		new PPCircle(Particle.END_ROD, standLocation, radius).ringMode(false).countPerMeter(0.8).spawnAsPlayerActive(player);
	}

	public void cleansingTotemPulse(Player player, Location standLocation, double radius) {
		PPCircle cleansingRing = new PPCircle(Particle.REDSTONE, standLocation, radius)
			.countPerMeter(1.05).delta(0).extra(0.05).data(DUST_CLEANSING_RING);
		PPSpiral cleansingSpiral = new PPSpiral(Particle.REDSTONE, standLocation, radius)
			.distancePerParticle(0.075).ticks(5).count(1).delta(0).extra(0.05).data(DUST_CLEANSING_RING);
		cleansingRing.spawnAsPlayerActive(player);
		cleansingSpiral.spawnAsPlayerActive(player);
	}

	public void cleansingTotemExpire(World world, Location standLocation, Player player) {
		new PartialParticle(Particle.HEART, standLocation, 45, 0.2, 1.1, 0.2, 0.1).spawnAsPlayerActive(player);
		world.playSound(standLocation, Sound.BLOCK_WOOD_BREAK, SoundCategory.PLAYERS, 0.7f, 0.5f);
	}
}
