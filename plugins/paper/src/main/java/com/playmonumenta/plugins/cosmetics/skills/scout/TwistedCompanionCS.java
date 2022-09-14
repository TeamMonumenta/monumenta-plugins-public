package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class TwistedCompanionCS extends HuntingCompanionCS {
	//Twisted theme

	public static final String NAME = "Twisted Companion";

	private final String FOX_NAME = "TwistedCompanion";

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName());
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HUNTING_COMPANION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.WITHER_ROSE;
	}

	@Override
	public String getFoxName() {
		return FOX_NAME;
	}

	@Override
	public void foxOnSummon(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 0.7f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 0.85f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_FOX_SNIFF, 2.0f, 0.9f);
		world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 1.0f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.85f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.8f);
		world.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 0.25f, 1.5f);
	}

	@Override
	public void foxOnAggro(World world, Player mPlayer, LivingEntity summon) {
		new PartialParticle(Particle.SOUL, summon.getEyeLocation(), 15, 0.25, 0.25, 0.25, 0.005).spawnAsPlayerActive(mPlayer);
	}
}
