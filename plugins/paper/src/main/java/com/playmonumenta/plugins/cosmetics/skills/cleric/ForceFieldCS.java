package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ForceFieldCS extends SanctifiedArmorCS {

	public static final String NAME = "Force Field";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A protective shield surrounds your body,",
			"ready to repel any incoming strike."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.HEART_OF_THE_SEA;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(0, 220, 220), 1.1f);

	@Override
	public void sanctOnTrigger1(World world, Player player, Location loc, LivingEntity source) {
		Vector v = LocationUtils.getDirectionTo(source.getLocation(), player.getLocation());
		Location l = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)) - 1.5, 0);
		double[] d = VectorUtils.vectorToRotation(v);
		world.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.PLAYERS, 2f, 2f);
		world.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 0.8f, 1.7f);
		world.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.5f, 0.7f);
		world.playSound(player.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_BREAK, SoundCategory.PLAYERS, 2.0f, 0.8f);
		world.playSound(player.getLocation(), Sound.BLOCK_SHROOMLIGHT_BREAK, SoundCategory.PLAYERS, 2.0f, 0.6f);
		world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_HURT, SoundCategory.PLAYERS, 0.8f, 0.8f);
		new BukkitRunnable() {
			int mInt = 0;
			@Override
			public void run() {
				new PPCircle(Particle.REDSTONE, l.clone().add(0, 0.5 * mInt, 0), 1.2 - 0.04 * mInt * mInt).arcDegree(d[0] + 22.5 - 7.5 * mInt, d[0] + 157.5 + 7.5 * mInt).countPerMeter(6).delta(0.03, 0.03, 0.03).data(CYAN).spawnAsPlayerActive(player);
				new PPCircle(Particle.REDSTONE, l.clone().subtract(0, 0.5 * mInt, 0), 1.2 - 0.04 * mInt * mInt).arcDegree(d[0] + 22.5 - 7.5 * mInt, d[0] + 157.5 + 7.5 * mInt).countPerMeter(6).delta(0.03, 0.03, 0.03).data(CYAN).spawnAsPlayerActive(player);
				new PPCircle(Particle.SCULK_CHARGE_POP, l.clone().add(0, 0.5 * mInt, 0), 1.1 - 0.04 * mInt * mInt).arcDegree(d[0] + 22.5 - 7.5 * mInt, d[0] + 157.5 + 7.5 * mInt).countPerMeter(5).delta(0.03, 0.03, 0.03).spawnAsPlayerActive(player);
				new PPCircle(Particle.SCULK_CHARGE_POP, l.clone().subtract(0, 0.5 * mInt, 0), 1.1 - 0.04 * mInt * mInt).arcDegree(d[0] + 22.5 - 7.5 * mInt, d[0] + 157.5 + 7.5 * mInt).countPerMeter(5).delta(0.03, 0.03, 0.03).spawnAsPlayerActive(player);
				mInt++;
				if (mInt == 4) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public void sanctOnTrigger2(World world, Player player, Location loc, LivingEntity source) {
		sanctOnTrigger1(world, player, loc, source);
	}

	@Override
	public void sanctOnHeal(Player player, Location loc, LivingEntity enemy) {
		Location middleLoc = player.getLocation().add(enemy.getLocation()).multiply(0.5);
		Vector dir = LocationUtils.getDirectionTo(player.getLocation(), enemy.getLocation());
		double distance = enemy.getLocation().distance(loc);
		ParticleUtils.drawParticleLineSlash(middleLoc, dir, 0, 0.5 * distance, 0.1, 5,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) ->
				new PartialParticle(Particle.CRIT_MAGIC, lineLoc, 10, 0.08, 0.08, 0.08, 0.25).spawnAsPlayerActive(player));
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			player.getWorld().playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 1.9f, 1.3f);
			player.getWorld().playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT_SHORT, SoundCategory.PLAYERS, 1.9f, 2f);
		}, 5);
	}
}
