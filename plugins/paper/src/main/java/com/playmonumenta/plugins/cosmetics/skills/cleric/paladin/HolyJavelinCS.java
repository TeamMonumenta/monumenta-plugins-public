package com.playmonumenta.plugins.cosmetics.skills.cleric.paladin;

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
import org.bukkit.entity.Player;

public class HolyJavelinCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.HOLY_JAVELIN;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TRIDENT;
	}

	public void javelinHitBlock(Player player, Location loc, World world) {
		new PartialParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125f).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 2.0f, 1.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 1.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.PLAYERS, 2.0f, 0.1f);
	}

	public void javelinParticle(Player player, Location startLoc, Location endLoc, double size) {
		Particle.DustOptions color = new Particle.DustOptions(Color.fromRGB(255, 255, 50), (float) size);
		new PartialParticle(Particle.EXPLOSION_NORMAL, startLoc.clone().add(startLoc.getDirection()), 10, 0, 0, 0, 0.125f).spawnAsPlayerActive(player);
		new PPLine(Particle.EXPLOSION_NORMAL, startLoc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0).extra(0.025).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).shiftStart(0.75).countPerMeter(22).delta(0.25 * size).data(color).spawnAsPlayerActive(player);
	}

	public void javelinSound(World world, Location loc) {
		world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.4f, 0.7f);
		world.playSound(loc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.3f, 1.4f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.7f, 2.0f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.6f, 2.0f);
	}
}
