package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class BodyguardCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BODYGUARD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_CHESTPLATE;
	}

	public void onBodyguard(Player player, World world, Location loc) {
		generalBodyguardSounds(world, loc);
		new PartialParticle(Particle.FLAME, loc.clone().add(0, 0.15, 0), 25, 0.2, 0, 0.2, 0.1).spawnAsPlayerActive(player);
	}

	public void onBodyguardOther(Player player, Player target, World world) {
		new PPLine(Particle.FLAME, player.getEyeLocation(), target.getEyeLocation())
			.countPerMeter(12)
			.delta(0.25)
			.spawnAsPlayerActive(player);

		Location targetLoc = target.getLocation();

		new PPExplosion(Particle.FLAME, targetLoc.clone().add(0, 0.15, 0))
			.flat(true)
			.speed(1)
			.count(120)
			.extraRange(0.1, 0.4)
			.spawnAsPlayerActive(player);

		new PPExplosion(Particle.EXPLOSION_NORMAL, targetLoc.clone().add(0, 0.15, 0))
			.flat(true)
			.speed(1)
			.count(60)
			.extraRange(0.15, 0.5)
			.spawnAsPlayerActive(player);

		world.playSound(targetLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 2.0f, 0.8f);
		world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 2.0f, 0.8f);
		world.playSound(targetLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.7f, 0.1f);
		world.playSound(targetLoc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1.0f, 0.1f);

		generalBodyguardSounds(world, targetLoc);
	}

	private void generalBodyguardSounds(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 2.0f, 0.1f);
	}
}
