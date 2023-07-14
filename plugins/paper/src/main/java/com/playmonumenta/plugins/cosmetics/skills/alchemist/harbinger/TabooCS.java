package com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TabooCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.TABOO;
	}

	@Override
	public Material getDisplayItem() {
		return Material.DRAGON_BREATH;
	}

	public void periodicEffects(Player player, World world, Location loc, boolean twoHertz, boolean oneSecond, int ticks, boolean inBurst) {
		if (oneSecond) {
			new PartialParticle(Particle.FALLING_DUST, loc.clone().add(0, player.getHeight() / 2, 0), 5, 0.25, 0.2, 0.25, 0).data(Material.WARPED_HYPHAE.createBlockData()).spawnAsPlayerBuff(player);
			new PartialParticle(Particle.FALLING_OBSIDIAN_TEAR, loc.clone().add(0, player.getHeight() / 2, 0), 10, 0.25, 0.2, 0.25, 0).spawnAsPlayerBuff(player);
			AbilityUtils.playPassiveAbilitySound(player, loc, Sound.BLOCK_CONDUIT_AMBIENT, 0.8f, 1);

			if (inBurst) {
				new PPCircle(Particle.FLAME, loc.clone().add(0, 0.1, 0), 2).count(50).ringMode(false).spawnAsPlayerBuff(player);
				world.playSound(loc, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
			}
		}
	}

	public void burstEffects(Player player, World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 0.7f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.8f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.PLAYERS, 0.3f, 0.1f);
		world.playSound(loc, Sound.ENTITY_BLAZE_DEATH, SoundCategory.PLAYERS, 0.4f, 0.1f);
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 2.0f, 1.0f);
		new PartialParticle(Particle.DAMAGE_INDICATOR, loc.clone().add(0, player.getHeight() / 2, 0), 20, 0.2, 0.2, 0.2, 0).spawnAsPlayerBuff(player);
		new PPCircle(Particle.FLAME, loc.clone().add(0, 0.1, 0), 2).count(50).ringMode(false).spawnAsPlayerBuff(player);
	}

	public void unburstEffects(Player player, World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 2.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 0.7f, 1.0f);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 2.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.2f, 1.2f);
	}

	public void toggle(World world, Location loc, boolean active) {
		if (active) {
			world.playSound(loc, Sound.ENTITY_WANDERING_TRADER_DRINK_POTION, SoundCategory.PLAYERS, 1, 0.9f);
		} else {
			world.playSound(loc, Sound.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.8f, 1.2f);
		}
	}

}
