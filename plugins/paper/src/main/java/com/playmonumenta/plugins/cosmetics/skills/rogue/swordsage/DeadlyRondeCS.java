package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class DeadlyRondeCS implements CosmeticSkill {

	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DEADLY_RONDE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_ROD;
	}

	public void rondeHitEffect(World world, Player player, Entity enemy, double radius, double rondeBaseRadius, boolean lv2) {
		double distance = enemy.getLocation().distance(player.getLocation());
		Location particleLoc = player.getEyeLocation().add(player.getEyeLocation().getDirection().multiply(distance));
		double multiplier = radius / rondeBaseRadius;
		new PartialParticle(Particle.SWEEP_ATTACK, particleLoc, (int) (2 * radius), 1, 0.5, 1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT, particleLoc, (int) (40 * multiplier), 1, 0.5, 1, 0.2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, particleLoc, (int) (20 * multiplier), 1, 0.5, 1, 0.3).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, particleLoc, (int) (35 * multiplier), 1, 0.5, 1, SWORDSAGE_COLOR).spawnAsPlayerActive(player);

		world.playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(particleLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.4f);
		world.playSound(particleLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 2.0f, 0.7f);
		world.playSound(particleLoc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.5f, 2.0f);
		world.playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(particleLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.4f, 2.0f);
		world.playSound(particleLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(particleLoc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.5f, 1.6f);
		world.playSound(particleLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.7f);
		world.playSound(particleLoc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 0.5f, 2.0f);
	}

	public void rondeGainStackEffect(Player player, Location loc) {
		player.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 0.7f, 0.8f);
		player.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.3f, 1.6f);
	}

	public void rondeTickEffect(Player player, int charges, int mTicks) {
		new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 3, 0.25, 0.45, 0.25, SWORDSAGE_COLOR).spawnAsPlayerBuff(player);
	}
}
