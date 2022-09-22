package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BrambleShellCS extends CounterStrikeCS {
	//Earthy counter strike. Depth set: earth

	public static final String NAME = "Bramble Shell";

	private static final Color EARTH_COLOR = Color.fromRGB(51, 102, 0);

	@Override
	public Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"Earthen thorns envelop your body,",
			"ready to thrash out at anything that",
			"threatens them.");
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.COUNTER_STRIKE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SWEET_BERRIES;
	}

	@Override
	public void counterOnHurt(Player mPlayer, Location loc, LivingEntity source) {
		loc.setPitch(0);
		mPlayer.playSound(loc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 1.25f, 0.85f);
		mPlayer.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1.25f, 0.65f);
		for (int i = 0; i < 2; i++) {
			createSpike(mPlayer, loc, false, false);
			createSpike(mPlayer, loc, true, false);
			createSpike(mPlayer, loc, false, true);
			createSpike(mPlayer, loc, true, true);
		}

		for (int i = 0; i < 2; i++) {
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc.clone().add(0, 0.15, 0), -30, 1, 0, 0, 60, 0.25f,
				true, 0, 0, Particle.SMOKE_NORMAL);
			ParticleUtils.drawParticleCircleExplosion(mPlayer, loc.clone().add(0, 0.15, 0), 30, 1, 0, 0, 60, 0.25f,
				true, 0, 0, Particle.SMOKE_NORMAL);
		}
	}

	private void createSpike(Player mPlayer, Location loc, boolean xNeg, boolean zNeg) {
		Vector dir;
		double x = xNeg ? FastUtils.randomDoubleInRange(-1, 0) : FastUtils.randomDoubleInRange(0, 1);
		double z = zNeg ? FastUtils.randomDoubleInRange(-1, 0) : FastUtils.randomDoubleInRange(0, 1);
		dir = new Vector(
			x, FastUtils.randomDoubleInRange(-0.5, 0.5), z
		).normalize().multiply(0.2);
		Location l = loc.clone().subtract(0, 0.1, 0);
		new BukkitRunnable() {

			int mT = 0;
			int mSizeCounter = 0;
			final int DURATION = 4;
			final int ITERATIONS = 5;
			@Override
			public void run() {
				mT++;

				for (int i = 0; i < ITERATIONS; i++) {
					l.add(dir);
					mSizeCounter++;
					float progress = (float) mSizeCounter / (DURATION * ITERATIONS);
					new PartialParticle(Particle.ITEM_CRACK, l, 1, 0.1, 0.1, 0.1, 0.05,
						new ItemStack(Material.SPRUCE_LOG)).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, l, 5, 0.1 * (1 - progress),
						0.1 * (1 - progress), 0.1 * (1 - progress), 0.1,
						new Particle.DustOptions(
							EARTH_COLOR, 1.125f * (1F - (0.75f * progress))
						))
						.spawnAsPlayerActive(mPlayer);
				}

				if (mT >= DURATION) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
