package com.playmonumenta.plugins.cosmetics.skills.mage;

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

public class ManaLanceCS implements CosmeticSkill {

	private static final Particle.DustOptions MANA_LANCE_COLOR = new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.MANA_LANCE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TRIDENT;
	}

	public void lanceHitBlock(Player player, Location loc, World world) {
		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1, 1.65f);
		new PartialParticle(Particle.CLOUD, loc, 30, 0, 0, 0, 0.125).spawnAsPlayerActive(player);
	}

	public void lanceParticle(Player player, Location startLoc, Location endLoc) {
		new PPLine(Particle.EXPLOSION_NORMAL, startLoc, endLoc).shiftStart(0.75).countPerMeter(2).minParticlesPerMeter(0).delta(0.05).extra(0.025).spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, startLoc, endLoc).shiftStart(0.75).countPerMeter(18).delta(0.35).data(MANA_LANCE_COLOR).spawnAsPlayerActive(player);
	}

	public void lanceSound(World world, Player player, Location loc) {
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1.8f, 2.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.4f, 0.8f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.9f, 2.0f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.9f, 2.0f);
	}

	public void lanceHit(Location loc, Player player) {

	}
}
