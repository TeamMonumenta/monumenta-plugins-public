package com.playmonumenta.plugins.cosmetics.skills.scout;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.*;
import org.bukkit.entity.Fox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class HuntingCompanionCS implements CosmeticSkill {

	public static final ImmutableMap<String, HuntingCompanionCS> SKIN_LIST = ImmutableMap.<String, HuntingCompanionCS>builder()
		.put(TwistedCompanionCS.NAME, new TwistedCompanionCS())
		.build();

	private final String FOX_NAME = "FoxCompanion";

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.HUNTING_COMPANION;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SWEET_BERRIES;
	}

	public String getFoxName() {
		return FOX_NAME;
	}

	public void foxOnSummon(World world, Location loc, Player mPlayer, Fox mFox) {
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.0f);
		world.playSound(loc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.2f);
		world.playSound(loc, Sound.ENTITY_FOX_SNIFF, 2.0f, 1.0f);
		world.playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_BREAK, 0.75f, 1.2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.0f);
	}

	public void foxOnAggro(World world, Player mPlayer, Fox mFox) {
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f);
		world.playSound(mFox.getLocation(), Sound.ENTITY_FOX_AGGRO, 1.5f, 1.0f);
		new PartialParticle(Particle.VILLAGER_ANGRY, mFox.getEyeLocation(), 25).spawnAsPlayerActive(mPlayer);
	}

	public void foxTick(Fox mFox, Player mPlayer, LivingEntity mTarget, int t) {

	}

	public void foxDespawn(Player mPlayer, Fox mFox) {
		World world = mFox.getLocation().getWorld();
		Location foxLoc = mFox.getLocation();
		world.playSound(foxLoc, Sound.ENTITY_FOX_SNIFF, 1.5f, 1.0f);
		world.playSound(foxLoc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 0.8f);
		world.playSound(foxLoc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.0f);
		world.playSound(foxLoc, Sound.ENTITY_FOX_AMBIENT, 1.5f, 1.2f);
		new PartialParticle(Particle.SMOKE_NORMAL, foxLoc, 20).spawnAsPlayerActive(mPlayer);
	}
}
