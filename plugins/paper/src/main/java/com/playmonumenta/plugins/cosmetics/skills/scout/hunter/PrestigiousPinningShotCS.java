package com.playmonumenta.plugins.cosmetics.skills.scout.hunter;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PrestigiousPinningShotCS extends PinningShotCS implements PrestigeCS {

	public static final String NAME = "Prestigious Pinning Shot";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.1f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.0f);
	private static final Particle.DustOptions WARN_COLOR = new Particle.DustOptions(Color.fromRGB(240, 64, 0), 0.9f);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"PIN_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.PINNING_SHOT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_HORSE_ARMOR;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return player != null;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public int getPrice() {
		return 1;
	}

	@Override
	public void pinEffect1(World world, Player mPlayer, LivingEntity enemy) {
		Location eLoc = enemy.getEyeLocation().subtract(0, 0.25, 0);
		world.playSound(eLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.1f, 0.944f);
		world.playSound(eLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.2f, 1.189f);
		world.playSound(eLoc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1.3f, 1.414f);
		world.playSound(eLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.5f, 1.25f);

		Vector mFront = eLoc.toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
		ParticleUtils.drawRing(eLoc, 36, mFront, 1.6,
			(l, t) -> {
				new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, 0, WARN_COLOR).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
			}
		);
		ParticleUtils.drawCurve(eLoc, -9, 9, mFront,
			t -> 0,
			t -> 0.21 * t,
			t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, WARN_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(eLoc, -9, 9, mFront,
			t -> 0,
			t -> 0,
			t -> 0.21 * t,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, WARN_COLOR).spawnAsPlayerActive(mPlayer)
		);
	}

	@Override
	public void pinEffect2(World world, Player mPlayer, LivingEntity enemy) {
		Location eLoc = enemy.getEyeLocation().subtract(0, 0.25, 0);
		world.playSound(eLoc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 1.6f, 1.2f);
		world.playSound(eLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 0.2f, 0.6f);
		world.playSound(eLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.75f, 0.85f);

		new PartialParticle(Particle.FIREWORKS_SPARK, eLoc, 40, 0, 0, 0, 0.4).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, eLoc, 60, 0.75, 0.5, 0.75, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		Vector mFront = eLoc.toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
		ParticleUtils.drawRing(eLoc, 36, mFront, 1.6,
			(l, t) -> {
				new PartialParticle(Particle.FALLING_DUST, l, 1, 0.1, 0.1, 0.1, 0, Bukkit.createBlockData(Material.YELLOW_CONCRETE)).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FALLING_DUST, l, 1, 0.1, 0.1, 0.1, 0, Bukkit.createBlockData(Material.ORANGE_CONCRETE)).spawnAsPlayerActive(mPlayer);
			}
		);
	}
}
