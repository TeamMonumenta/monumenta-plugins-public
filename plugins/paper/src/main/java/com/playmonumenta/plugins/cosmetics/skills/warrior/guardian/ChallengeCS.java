package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ChallengeCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.CHALLENGE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_AXE;
	}

	public void onCast(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.4f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.6f, 0.5f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.8f, 0.9f);
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.7f, 0.1f);
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_IRON, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 0.1f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.4f, 0.1f);
		new PartialParticle(Particle.FLAME, loc, 25, 0.4, 1, 0.4, 0.7f).spawnAsPlayerActive(player);
		loc.add(0, 1.25, 0);
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 250, 0, 0, 0, 0.425).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, loc, 300, 0, 0, 0, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 300, 0, 0, 0, 1).spawnAsPlayerActive(player);
	}

	public void onCastEffect(Player player, World world, Location loc) {

	}

	public void killMob(Player player, World world, Location loc) {

	}

	public void maxMobs(Player player, World world, Location loc) {
		player.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
	}
}
