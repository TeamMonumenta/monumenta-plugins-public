package com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public class WhirlwindTotemCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.WHIRLWIND_TOTEM;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WHITE_STAINED_GLASS;
	}

	public void whirlwindTotemSpawn(World world, Player player, Location standLocation, ArmorStand stand) {
		world.playSound(standLocation, Sound.ENTITY_ENDER_EYE_LAUNCH, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(standLocation, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 2.0f, 0.7f);
		world.playSound(standLocation, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 2.0f, 1.3f);
	}

	public void whirlwindTotemPulse(Player player, Location standLocation, double radius) {
		PPSpiral windSpiral = new PPSpiral(Particle.SPELL_INSTANT, standLocation, radius).curveAngle(360).count(300).ticks(5).delta(0);
		windSpiral.spawnAsPlayerActive(player);
		standLocation.getWorld().playSound(standLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.3f, 0.5f);
	}

	public void whirlwindTotemExpire(Player player, World world, Location standLocation, double radius) {
		new PartialParticle(Particle.HEART, standLocation, 45, 0.2, 1.1, 0.2, 0.1).spawnAsPlayerActive(player);
		world.playSound(standLocation, Sound.BLOCK_WOOD_BREAK,
			SoundCategory.PLAYERS, 0.7f, 0.5f);
	}
}
