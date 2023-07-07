package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

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

public class BlizzardCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BLIZZARD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SNOWBALL;
	}

	public void onCast(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 2.0f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 2.0f, 0.7f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 2.0f, 0.7f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.7f, 1.2f);
		world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.7f, 1.1f);
		world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.6f, 1.0f);
		world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.5f, 0.9f);
		world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.3f, 0.8f);
		world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.2f, 0.7f);
		world.playSound(loc, Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.1f, 0.6f);
	}

	public void tick(Player player, Location loc) {
		new PartialParticle(Particle.SNOWBALL, loc, 6, 2, 2, 2, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, loc, 4, 2, 2, 2, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.15).spawnAsPlayerActive(player);
	}
}
