package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PrismaticShieldCS implements CosmeticSkill {

	public static final ImmutableMap<String, PrismaticShieldCS> SKIN_LIST = ImmutableMap.<String, PrismaticShieldCS>builder()
		.put(SanguineAegisCS.NAME, new SanguineAegisCS())
		.build();

	@Override
	public Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.PRISMATIC_SHIELD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SHIELD;
	}

	@Override
	public String getName() {
		return null;
	}

	public void prismaEffect(World world, Player mPlayer, double radius) {
		world.playSound(mPlayer.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1, 1.35f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation().add(0, 1.15, 0), 150, 0.2, 0.35, 0.2, 0.5).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1.15, 0), 100, 0.2, 0.35, 0.2, 1).spawnAsPlayerActive(mPlayer);
	}

	public void prismaOnStun(LivingEntity mob, int stunTime, Player mPlayer) {
		//Nope!
	}

	public void prismaOnHeal(Player mPlayer) {
		//Nope!
	}
}
