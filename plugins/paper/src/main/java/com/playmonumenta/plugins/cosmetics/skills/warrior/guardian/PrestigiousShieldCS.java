package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PrestigiousShieldCS extends ShieldWallCS implements PrestigeCS {

	public static final String NAME = "Prestigious Shield";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(192, 168, 32), 1.5f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.25f);

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"SHIELD_DESC"
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.SHIELD_WALL;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_CHESTPLATE;
	}

	@Override
	public @Nullable String getName() {
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
	public void shieldStartEffect(World world, Player mPlayer, double radius) {
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1f, 1.35f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.9f, 0.75f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1.25f, 0.55f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.75f, 0.4f);

		Location mCenter = loc.clone().add(0, 0.125, 0);
		PPCircle ppc = new PPCircle(Particle.REDSTONE, mCenter, 0).data(LIGHT_COLOR).ringMode(true);
		int rings = (int) Math.ceil(radius * 1.25);
		for (int i = 1; i <= rings; i++) {
			ppc.count(i * 15).radius(radius * i / rings).spawnAsPlayerActive(mPlayer);
		}

		// Draw 土
		int units1 = (int) Math.ceil(radius * 2.4);
		int units2 = (int) Math.ceil(radius * 3.2);
		Vector mFront = loc.getDirection().clone().setY(0).normalize().multiply(radius);
		ParticleUtils.drawCurve(mCenter, -units1, units1, mFront,
			t -> 0.125,
				t -> 0, t -> 0.625 * t / units1,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.2, 0, 0.2, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, -units2, units2, mFront,
			t -> -0.8,
				t -> 0, t -> 0.75 * t / units1,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.2, 0, 0.2, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, -units2, units2, mFront,
			t -> 0.8 * t / units2,
				t -> 0, t -> 0,
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.2, 0, 0.2, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer)
		);

	}

	@Override
	public void shieldWallDot(Player mPlayer, Location l, double degree, double angle, int y, int height) {
		if (goldCheck(degree / angle, 1.0 * y / height)) {
			new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.2, 0.1, 0, GOLD_COLOR).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		} else {
			new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.2, 0.1, 0, LIGHT_COLOR).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);
		}
		if (FastUtils.RANDOM.nextDouble() < 0.3) {
			new PartialParticle(Particle.REDSTONE, l, 1, 0.1, 0.1, 0.1, 0, LIGHT_COLOR).spawnAsPlayerActive(mPlayer);
		}
	}

	private boolean goldCheck(double x, double y) {
		return Math.abs(1 - (Math.abs(x - 0.5) * 2) - y) <= 0.2;
	}

	@Override
	public void shieldOnBlock(World world, Location eLoc, Player mPlayer) {
		new PartialParticle(Particle.SPELL_INSTANT, eLoc, 5, 0, 0, 0, 0.3f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, eLoc, 3, 0.1, 0.1, 0.1, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
		world.playSound(eLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.75f, 1.25f);
		world.playSound(eLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.85f, 0.75f);
	}

	@Override
	public void shieldOnHit(World world, Location eLoc, Player mPlayer) {
		new PartialParticle(Particle.CLOUD, eLoc, 30, 0.2, 0.2, 0.2, 0.35f).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, eLoc, 15, 0.5, 0.8, 0.5, 0, GOLD_COLOR).spawnAsPlayerActive(mPlayer);
		world.playSound(eLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.8f, 1.4f);
		world.playSound(eLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.9f, 1.6f);
		world.playSound(eLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.95f, 1.75f);
	}
}
