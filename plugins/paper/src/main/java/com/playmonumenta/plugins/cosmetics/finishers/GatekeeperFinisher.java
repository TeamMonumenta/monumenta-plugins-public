package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.mage.TransfiguredSpikeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class GatekeeperFinisher implements EliteFinisher {

	public static final String NAME = "Gatekeeper";

	private static final Color TWIST_COLOR_LIGHT = Color.fromRGB(130, 66, 66);
	private static final Color TWIST_COLOR_DARK = Color.fromRGB(127, 0, 0);
	private static final Color FLESH_COLOR_LIGHT = Color.fromRGB(168, 89, 113);
	private static final Color FLESH_COLOR_DARK = Color.fromRGB(120, 61, 81);

	private static final Particle.DustOptions TWIST_COLOR_DARK_DUST = new Particle.DustOptions(TWIST_COLOR_DARK, 1.4f);
	private static final Particle.DustOptions BLACK = new Particle.DustOptions(Color.BLACK, 1.7f);

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		if (!(killedMob instanceof LivingEntity le)) {
			return;
		}

		new BukkitRunnable() {
			int mTicks = 0;
			@Nullable LivingEntity mClonedKilledMob;

			@Override
			public void run() {
				if (mTicks == 0) {
					killedMob.remove();
					mClonedKilledMob = EliteFinishers.createClonedMob(le, p, NamedTextColor.DARK_RED, false, false, true);
				} else if (mTicks > 0 && mTicks < 50) {
					new PPCircle(Particle.REDSTONE, loc, 2.6).ringMode(true).data(BLACK).countPerMeter(1).spawnAsPlayerActive(p);
					new PPCircle(Particle.REDSTONE, loc, 2.4).ringMode(false).data(TWIST_COLOR_DARK_DUST).countPerMeter(5).spawnAsPlayerActive(p);
					new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 25, 3, 2, 3).spawnAsPlayerActive(p);

					if (mTicks % 2 == 0) {
						double angle = FastUtils.RANDOM.nextDouble() * 2 * Math.PI;
						double radius = FastUtils.RANDOM.nextDouble() * 2.6;
						Location startLoc = loc.clone().add(radius * FastUtils.cos(angle), 0, radius * FastUtils.sin(angle));
						Location endLoc = startLoc.clone().add(FastUtils.randomDoubleInRange(-0.3, 0.3), FastUtils.randomDoubleInRange(1, 3), FastUtils.randomDoubleInRange(-0.3, 0.3));
						Vector direction = endLoc.toVector().subtract(startLoc.toVector());
						startLoc.setDirection(direction);
						TransfiguredSpikeCS.spawnTendril(startLoc, endLoc, p, TWIST_COLOR_DARK, TWIST_COLOR_LIGHT);
						TransfiguredSpikeCS.spawnTendril(startLoc, endLoc, p, FLESH_COLOR_DARK, FLESH_COLOR_LIGHT);
					}

					if (mTicks >= 35 && mClonedKilledMob != null) {
						double progress = (mTicks - 35.0) / 14.0;
						double sinkAmount = progress * 3.0;
						mClonedKilledMob.teleport(loc.clone().subtract(0, sinkAmount, 0));
					}
				}
				if (mTicks >= 50) {
					if (mClonedKilledMob != null) {
						mClonedKilledMob.remove();
					}
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_WART_BLOCK;
	}
}
