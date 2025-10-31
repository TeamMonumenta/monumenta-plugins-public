package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.GalleryCS;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.GameMode;
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

public class DecapitationCS extends ByMyBladeCS implements GalleryCS {
	//Gallery theme: blood

	public static final String NAME = "Decapitation";

	private static final Particle.DustOptions BLOOD_DUST1 = new Particle.DustOptions(Color.fromRGB(161, 30, 27), 0.8f);
	private static final Particle.DustOptions BLOOD_DUST2 = new Particle.DustOptions(Color.fromRGB(202, 6, 0), 1.2f);
	private static final Color BLOOD_COLOR1 = Color.fromRGB(159, 23, 15);
	private static final Color BLOOD_COLOR2 = Color.fromRGB(103, 5, 0);
	private static final double CLEAVE_RADIUS = 2;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"All roads lead to the bloodstained dream.",
			"Trapped in the nightmare, death is never",
			"the end of those souls under the doom."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.CREEPER_HEAD;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public GalleryCS.GalleryMap getMap() {
		return GalleryCS.GalleryMap.SANGUINE;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, GALLERY_COMPLETE_SCB).orElse(0) >= 1
			|| player.getGameMode() == GameMode.CREATIVE;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("Complete Sanguine Halls to unlock!").toArray(new String[0]);
	}

	@Override
	public void bmbDamage(World world, Player player, LivingEntity enemy, int level) {
		Location loc = enemy.getLocation();
		world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.5f, 2f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 1.0f, 2f);
		world.playSound(loc, Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 5f, 0.5f);
		new PartialParticle(Particle.CRIT_MAGIC, loc, level * 20, 0.3, 0.6, 0.3, 0.01).spawnAsPlayerActive(player);

		Vector vec = VectorUtils.rotationToVector(player.getLocation().getYaw(), player.getLocation().getPitch()).multiply(CLEAVE_RADIUS * -0.96);
		Location arcCenter = enemy.getEyeLocation().clone().add(vec).add(0, -0.075, 0);
		arcCenter.setYaw(player.getLocation().getYaw());
		arcCenter.setPitch(player.getLocation().getPitch());

		if (level > 1) {
			ParticleUtils.drawHalfArc(arcCenter, CLEAVE_RADIUS, 37, 30, 150, 3, 0.15,
				(Location l, int ring, double angleProgress) -> {
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, l, 1, 0, 0, 0, 0,
						new Particle.DustTransition(BLOOD_COLOR1, BLOOD_COLOR2, 0.7f + (ring * 0.05f)))
						.spawnAsPlayerActive(player);
				});
			ParticleUtils.drawHalfArc(arcCenter, CLEAVE_RADIUS, 143, 30, 150, 3, 0.15,
				(Location l, int ring, double angleProgress) -> {
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, l, 1, 0, 0, 0, 0,
						new Particle.DustTransition(BLOOD_COLOR1, BLOOD_COLOR2, 0.7f + (ring * 0.05f)))
						.spawnAsPlayerActive(player);
				});
		} else {
			int angle = FastUtils.RANDOM.nextDouble() < 0.5 ? 0 : 180;
			ParticleUtils.drawHalfArc(arcCenter, CLEAVE_RADIUS, angle, 30, 150, 2, 0.1,
				(Location l, int ring, double angleProgress) -> {
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, l, 1, 0, 0, 0, 0,
						new Particle.DustTransition(BLOOD_COLOR1, BLOOD_COLOR2, 0.7f + (ring * 0.05f)))
						.spawnAsPlayerActive(player);
				});
		}
	}

	@Override
	public void bmbDamageLv2(Player player, LivingEntity enemy) {
		new PartialParticle(Particle.REDSTONE, enemy.getEyeLocation(), 25, 0.25, 0.25, 0.25, 0.75, BLOOD_DUST1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.REDSTONE, enemy.getEyeLocation(), 25, 0.25, 0.25, 0.25, 1.25, BLOOD_DUST2).spawnAsPlayerActive(player);
	}

	@Override
	public void bmbHeal(Player player, Location loc) {

		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.PLAYERS, 0.8f, 2f);

		Location l = player.getLocation().clone().add(0, 0.1, 0);
		new BukkitRunnable() {
			double mRadius = 1.5;

			@Override
			public void run() {

				for (int i = 0; i < 3; i++) {
					mRadius -= 0.3;
					for (int degree = 0; degree < 360; degree += 30) {
						double radian = Math.toRadians(degree);
						Vector vec = new Vector(FastMath.cos(radian) * mRadius, 0.05 * i,
							FastMath.sin(radian) * mRadius);
						Location loc = l.clone().add(vec);
						if (i % 2 == 1) {
							new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, BLOOD_DUST2).spawnAsPlayerActive(player);
						} else {
							new PartialParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, 0, BLOOD_DUST1).spawnAsPlayerActive(player);
						}
					}
					if (mRadius <= 0) {
						this.cancel();
						return;
					}
				}

			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
