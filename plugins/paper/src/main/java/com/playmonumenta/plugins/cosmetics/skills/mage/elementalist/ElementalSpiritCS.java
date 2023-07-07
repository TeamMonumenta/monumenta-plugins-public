package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.AbstractPartialParticle;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ElementalSpiritCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ELEMENTAL_SPIRIT_FIRE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SUNFLOWER;
	}

	public void fireSpiritActivate(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1, 0.5f);
	}

	public void fireSpiritTravel(Player player, Location loc, double hitbox) {
		PartialParticle partialParticle = new PartialParticle(Particle.FLAME, loc)
			.count(4)
			.delta(PartialParticle.getWidthDelta(hitbox))
			.extra(0.05)
			.spawnAsPlayerActive(player);
		partialParticle
			.particle(Particle.SMOKE_LARGE)
			.spawnAsPlayerActive(player);
	}

	public void iceSpiritPulse(Player player, World world, Location loc, double size) {
		PartialParticle partialParticle = new PartialParticle(Particle.SNOWBALL, loc)
			.count(150)
			.delta(PartialParticle.getWidthDelta(size))
			.extra(0.1)
			.spawnAsPlayerActive(player);
		partialParticle.particle(Particle.FIREWORKS_SPARK)
			.count(30)
			.spawnAsPlayerActive(player);

		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.7f, 0.7f);
		world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.3f, 2.0f);
		world.playSound(loc, Sound.ENTITY_TURTLE_HURT_BABY, SoundCategory.PLAYERS, 1.0f, 0.4f);
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_DIAMOND, SoundCategory.PLAYERS, 1.4f, 0.1f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.0f, 1.4f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.7f, 2.0f);
	}

	public AbstractPartialParticle<?> getFirePeriodicParticle(Player player) {
		return new PPPeriodic(Particle.FLAME, player.getLocation()).extra(0.01);
	}

	public AbstractPartialParticle<?> getIcePeriodicParticle(Player player) {
		return new PPPeriodic(Particle.SNOWBALL, player.getLocation()).count(3);
	}
}
