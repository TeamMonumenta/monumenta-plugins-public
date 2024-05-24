package com.playmonumenta.plugins.cosmetics.skills.mage.elementalist;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class BlizzardCS implements CosmeticSkill {
	public static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(180, 230, 255), 1f);

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BLIZZARD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SNOWBALL;
	}

	public void onCast(Player player, World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 2.0f, 2.0f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 2.0f, 0.7f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 2.0f, 0.7f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 2.0f, 0.1f);
		AbilityUtils.playPassiveAbilitySound(player, loc, Sound.ITEM_ELYTRA_FLYING, 0.7f, 1.2f);
		AbilityUtils.playPassiveAbilitySound(player, loc, Sound.ITEM_ELYTRA_FLYING, 0.7f, 1.1f);
		AbilityUtils.playPassiveAbilitySound(player, loc, Sound.ITEM_ELYTRA_FLYING, 0.6f, 1.0f);
		AbilityUtils.playPassiveAbilitySound(player, loc, Sound.ITEM_ELYTRA_FLYING, 0.5f, 0.9f);
		AbilityUtils.playPassiveAbilitySound(player, loc, Sound.ITEM_ELYTRA_FLYING, 0.3f, 0.8f);
		AbilityUtils.playPassiveAbilitySound(player, loc, Sound.ITEM_ELYTRA_FLYING, 0.2f, 0.7f);
		AbilityUtils.playPassiveAbilitySound(player, loc, Sound.ITEM_ELYTRA_FLYING, 0.1f, 0.6f);
	}

	public void tick(Player player, Location loc, int ticks, double radius) {
		new PartialParticle(Particle.SNOWBALL, loc, 4).delta(radius / 2).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CLOUD, loc, 2).delta(radius / 2).extra(0.05).spawnAsPlayerActive(player);
		if (ticks % 5 == 0 || ticks % 5 == 1) {
			new PPCircle(Particle.SNOWFLAKE, loc.clone().add(0, FastUtils.randomDoubleInRange(2, 2.5), 0), radius * 0.9).extraRange(0.4, 0.7).countPerMeter(0.35).innerRadiusFactor(0.1)
				.directionalMode(true).delta(-0.35, 0.4, 1).rotateDelta(true).spawnAsPlayerActive(player);
			new PPCircle(Particle.SNOWFLAKE, loc.clone().add(0, FastUtils.randomDoubleInRange(-0.25, 0.25), 0), radius).extraRange(0.25, 0.55).countPerMeter(0.35).innerRadiusFactor(0.1)
				.directionalMode(true).delta(-0.25, 1, 1).rotateDelta(true).spawnAsPlayerActive(player);
		}
		double angle = ticks * 3 % 60;
		new PPCircle(Particle.REDSTONE, loc, radius).data(COLOR).count(6)
			.arcDegree(angle, angle + 360).spawnAsPlayerActive(player);
	}
}
