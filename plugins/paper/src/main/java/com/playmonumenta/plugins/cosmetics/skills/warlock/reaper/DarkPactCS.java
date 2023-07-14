package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class DarkPactCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DARK_PACT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SOUL_SAND;
	}

	public void onCast(Player player, World world, Location loc) {
		new PartialParticle(Particle.SPELL_WITCH, loc, 50, 0.2, 0.1, 0.2, 1).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 1.3f, 1.0f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 1.3f, 1.0f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 1.3f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 0.7f, 0.2f);
		world.playSound(loc, Sound.ENTITY_STRAY_HURT, SoundCategory.PLAYERS, 0.8f, 0.1f);
	}

	public void tick(Player player) {
		new PartialParticle(Particle.SPELL_WITCH, player.getLocation(), 3, 0.2, 0.2, 0.2, 0.2).spawnAsPlayerActive(player);
	}

	public void loseEffect(Player player) {
		AbilityUtils.playPassiveAbilitySound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.35f, 0.75f);
	}
}
