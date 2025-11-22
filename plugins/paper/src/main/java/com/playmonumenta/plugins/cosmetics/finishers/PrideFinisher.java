package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.AceArtilleryCS;
import com.playmonumenta.plugins.cosmetics.skills.cleric.EnbyRainCS;
import com.playmonumenta.plugins.cosmetics.skills.mage.AroShockCS;
import com.playmonumenta.plugins.cosmetics.skills.rogue.TransgenderCombosCS;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.PanBombCS;
import com.playmonumenta.plugins.cosmetics.skills.shaman.GayTotemCS;
import com.playmonumenta.plugins.cosmetics.skills.warlock.LesbianClawsCS;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.BiLineCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PrideFinisher implements EliteFinisher {
	// Stealing colors from the skill cosmetics
	public static final List<List<Color>> PRIDE_FLAG_COLORS = List.of(
		BiLineCS.BI_COLORS,
		GayTotemCS.GAY_COLORS,
		LesbianClawsCS.LESBIAN_COLORS,
		PanBombCS.PAN_COLORS,
		TransgenderCombosCS.TRANS_COLORS,
		EnbyRainCS.NB_COLORS,
		AroShockCS.ARO_COLORS,
		AceArtilleryCS.ACE_COLORS
	);
	public static final String NAME = "Pride";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = killedMob.getWorld();

		world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.PLAYERS, 1.2f, 0.25f);
		world.playSound(loc, Sound.ENTITY_BREEZE_DEATH, SoundCategory.PLAYERS, 0.8f, 0.7f);

		List<Color> flagColors = FastUtils.getRandomElement(PRIDE_FLAG_COLORS);

		new BukkitRunnable() {
			int mTicks = 0;
			final Location mLocation = loc.clone();

			@Override
			public void run() {
				new PartialParticle(Particle.FIREWORKS_SPARK, mLocation).spawnAsPlayerActive(p);
				for (int i = 0; i < 5; i++) {
					mLocation.add(0, 0.1, 0);

					double d = mTicks * 20 + i * 4;

					Vector vec = new Vector(FastUtils.sinDeg(d), 0, FastUtils.cosDeg(d)).multiply(1 - mTicks / 10.0);
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, mLocation.clone().add(vec))
						.data(new Particle.DustTransition(Color.WHITE, flagColors.get(0), 1.3f))
						.directionalMode(true)
						.spawnAsPlayerActive(p);

					new PartialParticle(Particle.DUST_COLOR_TRANSITION, mLocation.clone().subtract(vec))
						.data(new Particle.DustTransition(Color.WHITE, flagColors.get(flagColors.size() - 1), 1.3f))
						.directionalMode(true)
						.spawnAsPlayerActive(p);
				}

				mTicks++;
				if (mTicks >= 10) {
					this.cancel();

					world.playSound(loc, Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM, SoundCategory.PLAYERS, 1.2f, 0.4f);
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.PLAYERS, 1.6f, 0.5f);
					world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.PLAYERS, 2.0f, 0.5f);

					ParticleUtils.drawFlag(p, mLocation, flagColors, 2.1f);
					new PartialParticle(Particle.FLASH, mLocation).minimumCount(1).spawnAsPlayerActive(p);

					new BukkitRunnable() {
						int mI = 0;

						@Override
						public void run() {
							Vector axis1 = VectorUtils.rotateZAxis(new Vector(1, 0, 0), FastUtils.randomIntInRange(0, 360));
							Vector axis2 = VectorUtils.rotateZAxis(new Vector(0, 0, 1), FastUtils.randomIntInRange(0, 360));

							new PPCircle(Particle.REDSTONE, mLocation, mI + 1)
								.countPerMeter(3)
								.data(new Particle.DustOptions(flagColors.get(mI), 1.4f))
								.axes(axis1, axis2)
								.spawnAsPlayerActive(p);

							mI++;
							if (mI >= flagColors.size()) {
								this.cancel();
							}
						}
					}.runTaskTimer(Plugin.getInstance(), 0, 2);
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIREWORK_STAR;
	}
}
