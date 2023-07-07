package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

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

public class WhirlingBladeCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.WHIRLING_BLADE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_SWORD;
	}

	public void tick(Player player, Location bladeLoc, World world, Location loc) {
		new PartialParticle(Particle.SWEEP_ATTACK, bladeLoc, 3, 0.35, 0, 0.35, 1).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.4f, 0.9f);
		world.playSound(loc, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.4f, 1.2f);
		world.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 0.4f, 0.8f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.2f, 1.7f);
	}

	public void end(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.75f);
	}
}
