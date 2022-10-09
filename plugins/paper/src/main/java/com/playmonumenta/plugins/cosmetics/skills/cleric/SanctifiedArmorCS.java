package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SanctifiedArmorCS implements CosmeticSkill {

	public static final ImmutableMap<String, SanctifiedArmorCS> SKIN_LIST = ImmutableMap.<String, SanctifiedArmorCS>builder()
		.put(BloodyRetaliationCS.NAME, new BloodyRetaliationCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.SANCTIFIED_ARMOR;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_CHESTPLATE;
	}

	@Override
	public String getName() {
		return null;
	}

	public void sanctOnTrigger1(World world, Player mPlayer, Location loc, LivingEntity source) {
		new PartialParticle(Particle.FIREWORKS_SPARK, loc.add(0, source.getHeight() / 2, 0), 7, 0.35, 0.35, 0.35, 0.125).spawnAsPlayerPassive(mPlayer);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.7f, 1.2f);
	}

	public void sanctOnTrigger2(World world, Player mPlayer, Location loc, LivingEntity source) {
		sanctOnTrigger1(world, mPlayer, loc, source);
	}

	public void sanctOnHeal(Player mPlayer, LivingEntity enemy) {
		mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.65f, 1.25f);
	}
}
