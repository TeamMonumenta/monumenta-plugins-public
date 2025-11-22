package com.playmonumenta.plugins.cosmetics.skills.cleric.seraph;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class EtherealAscensionCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ETHEREAL_ASCENSION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.TOTEM_OF_UNDYING;
	}

	public void onLaunch(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_BREEZE_SLIDE, SoundCategory.PLAYERS, 1.6f, 1.1f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.8f, 1.2f);
	}

	public void tickEffect(Player player, Location loc, double hoverHeight) {
		// Previous effect didn't work, will design something proper later
	}

	public void orbShoot(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.0f, 1.5f);
		world.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.4f);
	}

	public void orbTrail(Player player, Location loc, Location startLoc) {
		new PartialParticle(Particle.ELECTRIC_SPARK, loc, 1, 0.05, 0.05, 0.05, 0.2).spawnAsPlayerActive(player);
	}

	public void orbImpact(Player player, World world, Location loc, double radius) {
		new PartialParticle(Particle.GUST, loc, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.END_ROD, loc, 8, 0.05, 0.05, 0.05, 0.2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SMALL_FLAME, loc, 10, 0.05, 0.05, 0.05, 0.2).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.5f, 1.4f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 1.1f);
	}

	public void orbBuff(Player player, World world, Location loc, List<Player> hitPlayers, double radius) {
		world.playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 1f, 2f);
	}

	public void dash(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_BREEZE_SLIDE, 1f, 1.8f);
	}

	public void forceEndAscension(Player player, World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 2.0f);
	}

	public void ascensionEnd(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.6f, 1.0f);
	}
}
