package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.google.common.collect.ImmutableMap;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class DeadlyRondeCS implements CosmeticSkill {

	public static final ImmutableMap<String, DeadlyRondeCS> SKIN_LIST = ImmutableMap.<String, DeadlyRondeCS>builder()
		.put(PrestigiousRondeCS.NAME, new PrestigiousRondeCS())
		.build();

	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.0f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return null;
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.DEADLY_RONDE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_ROD;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	public void rondeHitEffect(World world, Player mPlayer, double radius, double rondeBaseRadius, boolean lv2) {
		Location particleLoc = mPlayer.getEyeLocation().add(mPlayer.getEyeLocation().getDirection().multiply(3));
		double multiplier = radius / rondeBaseRadius;
		double delta = 1.5 * multiplier;
		new PartialParticle(Particle.SWEEP_ATTACK, particleLoc, (int) (10 * multiplier), delta, 0.5, delta).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT, particleLoc, (int) (50 * multiplier), delta, 0.5, delta, 0.2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CLOUD, particleLoc, (int) (20 * multiplier), delta, 0.5, delta, 0.3).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, particleLoc, (int) (45 * multiplier), delta, 0.5, delta, SWORDSAGE_COLOR).spawnAsPlayerActive(mPlayer);

		world.playSound(particleLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1, 1.25f);
		world.playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.8f, 0.75f);
		world.playSound(particleLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1, 0.75f);
		world.playSound(particleLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 0.75f);
	}

	public void rondeGainStackEffect(Player mPlayer) {
		mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 1, 1f);
		mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 0.7f, 1.5f);
	}

	public void rondeTickEffect(Player mPlayer, int charges, int mTicks) {
		new PartialParticle(Particle.REDSTONE, mPlayer.getLocation().add(0, 1, 0), 3, 0.25, 0.45, 0.25, SWORDSAGE_COLOR).spawnAsPlayerBuff(mPlayer);
	}
}
