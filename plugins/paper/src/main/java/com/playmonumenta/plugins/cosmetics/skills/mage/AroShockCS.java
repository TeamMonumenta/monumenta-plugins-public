package com.playmonumenta.plugins.cosmetics.skills.mage;

import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class AroShockCS extends SpellshockCS {
	public static final String NAME = "Aro Shock";

	public static final Color ARO_GREEN = Color.fromRGB(0x3DA542);
	public static final Color ARO_LIME = Color.fromRGB(0xA7D379);
	public static final Color ARO_WHITE = Color.WHITE;
	public static final Color ARO_GRAY = Color.fromRGB(0xA9A9A9);
	public static final Color ARO_BLACK = Color.BLACK;

	public static final List<Color> ARO_COLORS = List.of(ARO_GREEN, ARO_LIME, ARO_WHITE, ARO_GRAY, ARO_BLACK);
	private static final int[] ANGLES = {-10, 190};
	private static final double[] ROTATIONS = {25, -25};

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A union of two is not always romantic.",
			"Some pairings are just friends."
		);
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public Material getDisplayItem() {
		return Material.LILAC;
	}

	// This is just here to make sure no duplicate colors, yes I know it is global for all entities
	private int mTicks = 0;

	@Override
	public void tickEffect(Entity entity) {
		new PPPillar(Particle.REDSTONE, entity.getLocation(), entity.getHeight() + 1)
			.count(10)
			.data(new Particle.DustOptions(ARO_COLORS.get(mTicks % ARO_COLORS.size()), 1.2f))
			.delta(0.4, 0, 0.4)
			.spawnAsEnemyBuff();

		mTicks++;
	}

	@Override
	public void meleeClearStatic(Player player, LivingEntity enemy) {
		Location loc = enemy.getLocation().add(0, 1, 0);
		Location pLoc = player.getLocation().add(0, 1, 0);
		Vector dir = LocationUtils.getDirectionTo(loc, pLoc.clone().subtract(0, 0.5, 0)).multiply(3);
		int random = FastUtils.randomIntInRange(0, 1);
		loc.setDirection(VectorUtils.rotateYAxis(dir, ROTATIONS[random])).subtract(dir);

		ParticleUtils.drawHalfArc(loc, 3, ANGLES[random] + FastUtils.randomDoubleInRange(-20, 20), 10, 100, 6, 0.2, false, 40,
			(location, ring, angleProgress) -> {
				new PartialParticle(Particle.REDSTONE, location)
					.data(new Particle.DustOptions(getTransition(Math.pow(angleProgress, 2)), 1.1f + (float) angleProgress * ring / 18.0f))
					.spawnAsPlayerActive(player);
			});
		World world = loc.getWorld();

		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.5f, 1.0f);
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.4f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.3f, 0.9f);
		world.playSound(loc, Sound.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.6f, 0.33f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.6f, 0.6f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2f, 0.6f);

		mTicks = 0;
	}

	private static Color getTransition(double x) {
		if (x > 0.5) {
			return ParticleUtils.getTransition(ARO_WHITE, ARO_GREEN, (x - 0.5) * 2);
		}
		return ParticleUtils.getTransition(ARO_BLACK, ARO_WHITE, x * 2);
	}

	@Override
	public void spellshockEffect(Player player, LivingEntity enemy) {
		Location entityLocation = LocationUtils.getHalfHeightLocation(enemy);
		for (Color aroColor : ARO_COLORS) {
			Vector vec = VectorUtils.randomUnitVector();
			new PPLine(Particle.REDSTONE, entityLocation.clone().subtract(vec), entityLocation.clone().add(vec))
				.data(new Particle.DustOptions(aroColor, 1.3f))
				.countPerMeter(2)
				.spawnAsEnemyBuff();
		}
		World world = player.getWorld();
		Location loc = enemy.getLocation();

		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.4f, 1.0f);
		world.playSound(loc, Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 0.9f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.1f, 0.9f);
		world.playSound(loc, Sound.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.5f, 0.4f);
		world.playSound(loc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 0.3f, 1.5f);

		mTicks = 0;
	}
}
