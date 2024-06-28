package com.playmonumenta.plugins.cosmetics.skills.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class FaespiritsCS extends RestlessSoulsCS {
	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Mischievous spirits are attracted",
			"to the chaos in your wake."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.SUGAR;
	}

	@Override
	public @Nullable String getName() {
		return "Faespirits";
	}

	@Override
	public void vexTick(Player player, LivingEntity vex, int ticks) {
		Location loc = LocationUtils.getHalfHeightLocation(vex);
		Vector direction = vex.getLocation().getDirection();

		new PartialParticle(Particle.SCRAPE, loc, 1)
			.delta(-direction.getX(), -direction.getY() * 3.5, -direction.getZ())
			.directionalMode(true)
			.extra(7)
			.spawnAsPlayerActive(player);
		new PartialParticle(Particle.BUBBLE_POP, loc.clone().subtract(direction.clone().multiply(0.5)), 6, 0.3, 0.3, 0.3, 0).spawnAsPlayerActive(player);
	}

	@Override
	public void vexTarget(Player player, LivingEntity vex, LivingEntity target) {
		World world = player.getWorld();
		Location loc = vex.getLocation();

		world.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 1.25f, 2f);
		world.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 1.25f, 1.85f);

		Vector dir = LocationUtils.getHalfHeightLocation(target).subtract(loc).toVector().normalize();
		new PPLine(Particle.SCRAPE, loc, LocationUtils.getEntityCenter(target))
			.countPerMeter(2)
			.directionalMode(true)
			.delta(dir.getX(), dir.getY(), dir.getZ())
			.extra(10)
			.includeEnd(false)
			.spawnAsPlayerActive(player);

		Vector front = LocationUtils.getDirectionTo(player.getLocation(), target.getLocation()).setY(0).normalize();
		Vector up = new Vector(0, 1, 0);
		Vector right = VectorUtils.crossProd(up, front);

		Location loc1 = target.getEyeLocation().add(0, 1, 0);
		Location loc2 = loc1.clone().add(up.clone().multiply(0.25)).add(right.clone().multiply(0.35));
		Location loc3 = loc1.clone().add(up.clone().multiply(0.25)).add(right.clone().multiply(-0.35));
		Location loc4 = loc1.clone().add(up.clone().multiply(0.6)).add(right.clone().multiply(-0.35));
		Location loc5 = loc1.clone().add(up.clone().multiply(0.6)).add(right.clone().multiply(0.35));
		Location loc6 = loc1.clone().add(up.clone().multiply(0.775));

		new PPLine(Particle.SCRAPE, loc1, new Vector(0, 1, 0), 0.175).countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.SCRAPE, loc2, loc4).countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.SCRAPE, loc3, loc5).countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.SCRAPE, loc4, loc6).countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
		new PPLine(Particle.SCRAPE, loc5, loc6).countPerMeter(10).groupingDistance(0).spawnAsPlayerActive(player);
	}

	@Override
	public void vexAttack(Player player, LivingEntity vex, LivingEntity enemy, double radius) {
		World world = player.getWorld();
		world.playSound(vex.getLocation(), Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 1.25f, 0.8f);
		world.playSound(vex.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 1.25f, 1.35f);

		new PartialParticle(Particle.SCRAPE, LocationUtils.getEntityCenter(enemy), 6, 0, 0, 0, 16).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BUBBLE_POP, LocationUtils.getEntityCenter(enemy), 50, 0.35, 0.35, 0.35, 0.15).spawnAsPlayerActive(player);

		for (int i = 0; i < 4; i++) {
			Location loc = enemy.getEyeLocation().add(FastUtils.randomDoubleInRange(-0.75, 0.75), FastUtils.randomDoubleInRange(0.25, 1.25), FastUtils.randomDoubleInRange(-0.75, 0.75));
			new PartialParticle(Particle.SCRAPE, loc, 6, 0, -1, 0).extraRange(6, 14).directionalMode(true).spawnAsPlayerActive(player);
		}
	}

	@Override
	public void vexDespawn(Player player, LivingEntity vex) {
		World world = player.getWorld();
		Location loc = vex.getLocation();

		world.playSound(loc, Sound.ENTITY_ALLAY_DEATH, SoundCategory.PLAYERS, 0.75f, 0.8f);

		new PartialParticle(Particle.SCRAPE, loc, 8, 0.3, 0.3, 0.3, 12).spawnAsPlayerActive(player);
		new PartialParticle(Particle.BUBBLE_POP, loc, 25, 0.2, 0.2, 0.2, 0.15).spawnAsPlayerActive(player);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(),
			() -> new PartialParticle(Particle.BUBBLE_POP, loc, 25, 0.2, 0.2, 0.2, 0.1).spawnAsPlayerActive(player), 2);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(),
			() -> new PartialParticle(Particle.BUBBLE_POP, loc, 25, 0.2, 0.2, 0.2, 0.1).spawnAsPlayerActive(player), 4);
	}

	@Override
	public Team createTeam() {
		return ScoreboardUtils.getExistingTeamOrCreate("faespiritsColor", NamedTextColor.DARK_AQUA);
	}
}
