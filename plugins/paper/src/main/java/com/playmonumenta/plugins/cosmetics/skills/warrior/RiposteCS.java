package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class RiposteCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.RIPOSTE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SKELETON_SKULL;
	}

	public void onParry(Player player, World world, Location playerLoc, Entity entity) {
		world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(playerLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(playerLoc, Sound.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(playerLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(playerLoc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1.4f, 1.4f);
		Vector dir = LocationUtils.getDirectionTo(playerLoc.clone().add(0, 1, 0), entity.getLocation().add(0, entity.getHeight() / 2, 0));
		Location loc = playerLoc.clone().add(0, 1, 0).subtract(dir);
		new PartialParticle(Particle.SWEEP_ATTACK, loc, 8, 0.75, 0.5, 0.75, 0.001).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.75, 0.5, 0.75, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, loc, 75, 0.1, 0.1, 0.1, 0.6).spawnAsPlayerActive(player);
	}

	public void onAxeStun(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.3f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.2f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.2f, 1.7f);
	}

	public void onSwordAttack(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.3f, 2.0f);
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.5f, 0.1f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_SKELETON_HURT, SoundCategory.PLAYERS, 0.3f, 0.1f);
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_DIAMOND, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 1.0f, 0.6f);

	}

	public void onEnhancedParry(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_ANVIL_FALL, SoundCategory.PLAYERS, 0.5f, 0.5f);
	}
}
