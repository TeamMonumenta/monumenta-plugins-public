package com.playmonumenta.plugins.cosmetics.skills.mage.arcanist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
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

public class CacophonyCS extends AstralOmenCS {
	public static final String NAME = "Cacophony";
	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "OmenTick";
	public static final Color ROSE_COLOR = Color.fromRGB(140, 6, 46);
	public static final Color BLACK_COLOR = Color.fromRGB(0, 0, 0);
	private static final Particle.DustOptions FIRE_COLOR = new Particle.DustOptions(Color.fromRGB(235, 59, 54), 1.1f);
	private static final Particle.DustOptions THUNDER_COLOR = new Particle.DustOptions(Color.fromRGB(196, 23, 81), 1.2f);
	public static final Particle.DustOptions ARCANE_COLOR = new Particle.DustOptions(Color.fromRGB(162, 36, 173), 1.3f);
	public static final Particle.DustOptions ICE_COLOR = new Particle.DustOptions(Color.fromRGB(64, 62, 224), 1.4f);
	private static final Particle.DustOptions DARK_RED_LARGE = new Particle.DustOptions(Color.fromRGB(64, 4, 27), 1f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Energy flows through the world as harmoniously",
			"as seasons come and pass. A disruption to its",
			"natural current would be nothing short of",
			"catastrophic."
		);
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.ASTRAL_OMEN;
	}

	@Override
	public Material getDisplayItem() {
		return Material.MUSIC_DISC_11;
	}

	public static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(100, 50, 170), 1f);

	@Override
	public void clearEffect(Player player, LivingEntity enemy, Map.Entry<AstralOmen.Type, Integer> entry, double radius) {
		Location loc = enemy.getEyeLocation().add(0, -0.5, 0);
		loc.setPitch(0);
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, CHECK_ONCE_THIS_TICK_METAKEY)) {
			new PPLightning(Particle.DUST_COLOR_TRANSITION, loc.clone()).init(8, 10, 1, 0.35).hopsPerBlock(1.33).data(new Particle.DustTransition(BLACK_COLOR, ROSE_COLOR, 1.2f)).count(8).delta(0.005).duration(3).spawnAsPlayerActive(player);
			new PPLightning(Particle.DUST_COLOR_TRANSITION, loc.clone()).init(8, 10, 1, 0.35).hopsPerBlock(1.33).data(new Particle.DustTransition(BLACK_COLOR, ROSE_COLOR, 1.2f)).count(8).delta(0.005).duration(3).spawnAsPlayerActive(player);
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () ->
				ParticleUtils.explodingRingEffect(Plugin.getInstance(), enemy.getLocation().add(0, 0.2, 0), radius, 0, 3,
					List.of(
						new AbstractMap.SimpleEntry<>(1.0, (Location location) -> new PartialParticle(Particle.REDSTONE, location, 2, 0.1, 0.05, 0.1, 0).data(DARK_RED_LARGE).spawnAsPlayerActive(player)))
				), 3);
			player.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.5f, 2f);
			player.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1f, 0.75f);
			player.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.PLAYERS, 1f, 0.75f);
			player.getWorld().playSound(enemy.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 1.2f, 1.6f);
		}
	}

	@Override
	public void arcaneStack(Player player, Entity entity, Particle.DustOptions color) {
		Location location = entity.getLocation().add(0, 1, 0);
		location.setDirection(location.getDirection().setY(0).normalize());
		new PartialParticle(Particle.CRIMSON_SPORE, entity.getLocation().add(0, 1, 0), 5, 0, 0, 0, 0.005).spawnAsEnemyBuff();
		if (Bukkit.getCurrentTick() % 4 == 0) {
			ParticleUtils.drawCleaveArc(location, 1.6, 0, 10, 170, 1, 0, 0, 0.2, 30,
				(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, ARCANE_COLOR).spawnAsPlayerActive(player), 10);
			ParticleUtils.drawCleaveArc(location, 1.6, 0, 190, 350, 1, 0, 0, 0.2, 30,
				(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, ARCANE_COLOR).spawnAsPlayerActive(player), 10);
		}
	}

	@Override
	public void fireStack(Player player, Entity entity, Particle.DustOptions color) {
		Location location = entity.getLocation().add(0, 1, 0);
		location.setDirection(location.getDirection().setY(0).normalize());
		new PartialParticle(Particle.CRIMSON_SPORE, entity.getLocation().add(0, 1, 0), 5, 0, 0, 0, 0.005).spawnAsEnemyBuff();

		if (Bukkit.getCurrentTick() % 4 == 0) {
			ParticleUtils.drawCleaveArc(location, 0.8, 0, 10, 170, 1, 0, 0, 0.2, 30,
				(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, FIRE_COLOR).spawnAsPlayerActive(player), 12);
			ParticleUtils.drawCleaveArc(location, 0.8, 0, 190, 350, 1, 0, 0, 0.2, 30,
				(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, FIRE_COLOR).spawnAsPlayerActive(player), 12);
		}
	}

	@Override
	public void iceStack(Player player, Entity entity, Particle.DustOptions color) {
		Location location = entity.getLocation().add(0, 1, 0);
		location.setDirection(location.getDirection().setY(0).normalize());
		new PartialParticle(Particle.CRIMSON_SPORE, entity.getLocation().add(0, 1, 0), 5, 0, 0, 0, 0.005).spawnAsEnemyBuff();
		if (Bukkit.getCurrentTick() % 4 == 0) {
			ParticleUtils.drawCleaveArc(location, 2, 180, 10, 170, 1, 0, 0, 0.2, 30,
				(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, ICE_COLOR).spawnAsPlayerActive(player), 6);
			ParticleUtils.drawCleaveArc(location, 2, 180, 190, 350, 1, 0, 0, 0.2, 30,
				(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, ICE_COLOR).spawnAsPlayerActive(player), 6);
		}
	}

	@Override
	public void thunderStack(Player player, Entity entity) {
		Location location = entity.getLocation().add(0, 1, 0);
		location.setDirection(location.getDirection().setY(0).normalize());
		new PartialParticle(Particle.CRIMSON_SPORE, entity.getLocation().add(0, 1, 0), 5, 0, 0, 0, 0.005).spawnAsEnemyBuff();
		if (Bukkit.getCurrentTick() % 4 == 0) {
			ParticleUtils.drawCleaveArc(location, 1.2, 180, 10, 170, 1, 0, 0, 0.2, 30,
				(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, THUNDER_COLOR).spawnAsPlayerActive(player), 8);
			ParticleUtils.drawCleaveArc(location, 1.2, 180, 190, 350, 1, 0, 0, 0.2, 30,
				(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.REDSTONE, l, 1, 0, 0, 0, 0, THUNDER_COLOR).spawnAsPlayerActive(player), 8);
		}
	}

	@Override
	public void bonusDamage(Player player, Entity entity, Particle.DustOptions color) {
		World world = entity.getWorld();
		Location loc = entity.getLocation().add(0, 1, 0);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1f, 1.25f);
		world.playSound(loc, Sound.ENTITY_SHULKER_SHOOT, SoundCategory.PLAYERS, 1f, 1.75f);
		new PartialParticle(Particle.CRIT, loc, 8, 0.25, 0.5, 0.25, 0.4).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, loc, 8, 0.2, 0.2, 0.2, 0.1, color).spawnAsPlayerActive(player);
	}

	@Override
	public void bonusDamageTick(Player player, Entity entity, Particle.DustOptions color) {
		Location loc = entity.getLocation().add(0, 1, 0);
		Location finalLoc = loc.add(VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(-0.8, 0.8)));
		for (int i = 0; i < 3; i++) {
			sparkParticle(finalLoc, VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(0.6, 1.2)), player);
		}
	}

	private void sparkParticle(Location loc, Vector dir, Player player) {
		Location location = loc.clone();
		Vector direction = dir.clone();

		for (int i = 0; i < 2; i++) {
			Location oldLocation = location.clone();
			location.add(direction.multiply(0.1)).add(FastUtils.randomDoubleInRange(-0.5, 0.5), FastUtils.randomDoubleInRange(-0.5, 0.5), FastUtils.randomDoubleInRange(-0.5, 0.5));

			new PPLine(Particle.DUST_COLOR_TRANSITION, oldLocation, location).data(new Particle.DustTransition(BLACK_COLOR, ROSE_COLOR, (float) 0.6))
				.countPerMeter(5).groupingDistance(0).spawnAsPlayerActive(player);
		}
	}
}
