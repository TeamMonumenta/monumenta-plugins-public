package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
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

public class TransgenderCombosCS extends ViciousCombosCS {
	//These will be puns

	public static final String NAME = "Transgender Combos";
	private static final Color TRANS_BLUE = Color.fromRGB(0x6ED2FA);
	private static final Color TRANS_PINK = Color.fromRGB(0xF4B2C0);
	private static final Color TRANS_WHITE = Color.WHITE;
	public static final List<Color> TRANS_COLORS = List.of(TRANS_BLUE, TRANS_PINK, TRANS_WHITE, TRANS_PINK, TRANS_BLUE);


	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The transgender one",
			"will be bested by none. ⚧");
	}

	@Override
	public Material getDisplayItem() {
		return Material.PINK_WOOL;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void comboOnKill(World world, Location loc, Player player, double range, LivingEntity target) {
		Location eLoc = LocationUtils.getHalfHeightLocation(target);
		eLoc.setPitch(0);

		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.7f, 1.2f);
		world.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 0.7f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.6f, 0.9f);
		world.playSound(loc, Sound.ENTITY_VEX_DEATH, SoundCategory.PLAYERS, 0.6f, 1.8f);

		drawX(eLoc, player, 1.5);
	}


	@Override
	public void comboOnElite(World world, Location loc, Player player, double range, LivingEntity target) {
		Location eLoc = LocationUtils.getHalfHeightLocation(target);
		eLoc.setPitch(0);
		eLoc.setYaw(player.getLocation().getYaw());

		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 0.7f, 1.3f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.7f, 1.25f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.75f);
		world.playSound(loc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 0.7f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.6f, 0.9f);
		world.playSound(loc, Sound.ENTITY_VEX_DEATH, SoundCategory.PLAYERS, 0.6f, 1.8f);
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.7f, 0.8f);

		world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.8f, 1.0f);
		world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.8f, 1.5f);
		world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.8f, 2.0f);

		drawX(eLoc, player, 3);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks > 6) {
					ParticleUtils.drawParticleCircleExplosion(player, eLoc, 0, 0.5, 0, 0, 50, (float) range / 10, false, 0, Particle.SOUL_FIRE_FLAME);
					ParticleUtils.drawFlag(player, eLoc.clone().add(0, 2, 0), TRANS_COLORS, 2);

					Location pLoc = player.getLocation();
					loc.getWorld().playSound(pLoc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
					loc.getWorld().playSound(pLoc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.1f);
					loc.getWorld().playSound(pLoc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.5f);
					loc.getWorld().playSound(pLoc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 0.5f);
					loc.getWorld().playSound(pLoc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 1.0f);
					loc.getWorld().playSound(pLoc, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 2f, 1.0f);

					this.cancel();
					return;
				}
				Vector offset = VectorUtils.randomUnitVector().multiply(1.5);

				new PPLine(Particle.REDSTONE, eLoc.clone().subtract(offset), eLoc.clone().add(offset))
					.countPerMeter(4)
					.delay(4)
					.data(new Particle.DustOptions(TRANS_COLORS.get(mTicks % TRANS_COLORS.size()), 1.5f))
					.spawnAsPlayerActive(player);

				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.7f, 1.2f);
				world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.7f, 1.25f);
				world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1f, 0.85f);

				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 3);

	}

	private void drawX(Location loc, Player player, double length) {
		loc.setPitch(0);
		loc.setYaw(player.getLocation().getYaw());
		Vector dir = VectorUtils.rotateTargetDirection(player.getLocation().getDirection(), 90, 90 - 40);
		ParticleUtils.drawParticleLineSlash(loc, dir, 0, length, 0.05, 4,
			(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
				float size = (float) (0.5f + (0.3f * middleProgress));
				new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
					new Particle.DustOptions(selectColor(endProgress), size)).spawnAsPlayerActive(player);
			});

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			Vector d = VectorUtils.rotateTargetDirection(player.getLocation().getDirection(), 90, 90 + 40);
			ParticleUtils.drawParticleLineSlash(loc, d, 0, length, 0.05, 4,
				(Location lineLoc, double middleProgress, double endProgress, boolean middle) -> {
					float size = (float) (0.5f + (0.3f * middleProgress));
					new PartialParticle(Particle.REDSTONE, lineLoc, 3, 0.05, 0.05, 0.05, 0.25,
						new Particle.DustOptions(selectColor(endProgress), size)).spawnAsPlayerActive(player);
				});
		}, 2);
	}

	private static Color selectColor(double endProgress) {
		return endProgress > 0.66 ? TRANS_WHITE : (endProgress > 0.33 ? TRANS_PINK : TRANS_BLUE);
	}
}
