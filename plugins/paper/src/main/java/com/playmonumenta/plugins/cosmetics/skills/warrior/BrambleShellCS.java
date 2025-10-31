package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.DepthsCS;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BrambleShellCS extends CounterStrikeCS implements DepthsCS {
	//Earthy counter strike. Depth set: earth

	public static final String NAME = "Bramble Shell";

	private static final Color EARTH_COLOR = Color.fromRGB(51, 102, 0);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Earthen thorns envelop your body,",
			"ready to thrash out at anything that",
			"threatens them.");
	}

	@Override
	public Material getDisplayItem() {
		return Material.SWEET_BERRIES;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public String getToken() {
		return TALISMAN_EARTH;
	}

	@Override
	public void onPrime(Player player, Location loc) {
		player.playSound(loc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.6f, 0.9f);
		player.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.6f, 0.9f);
		player.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.PLAYERS, 0.4f, 0.3f);
	}

	@Override
	public void onCounterStrike(Player player, LivingEntity enemy, Location enemyLoc) {
		enemyLoc.setPitch(0);
		player.playSound(enemyLoc, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 0.6f, 0.1f);
		player.playSound(enemyLoc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 0.6f, 0.1f);
		player.playSound(enemyLoc, Sound.ENTITY_TURTLE_HURT_BABY, SoundCategory.PLAYERS, 0.8f, 2.0f);
		player.playSound(enemyLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		player.playSound(enemyLoc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.3f, 1.8f);
		for (int i = 0; i < 2; i++) {
			createSpike(player, enemyLoc, false, false);
			createSpike(player, enemyLoc, true, false);
			createSpike(player, enemyLoc, false, true);
			createSpike(player, enemyLoc, true, true);
		}

		for (int i = 0; i < 2; i++) {
			ParticleUtils.drawParticleCircleExplosion(player, enemyLoc.clone().add(0, 0.15, 0), -30, 1, 0, 0, 60, 0.25f,
				true, 0, 0, Particle.SMOKE_NORMAL);
			ParticleUtils.drawParticleCircleExplosion(player, enemyLoc.clone().add(0, 0.15, 0), 30, 1, 0, 0, 60, 0.25f,
				true, 0, 0, Particle.SMOKE_NORMAL);
		}
	}

	private void createSpike(Player player, Location loc, boolean xNeg, boolean zNeg) {
		double x = FastUtils.randomDoubleInRange(-1, 0) * (xNeg ? -1 : 1);
		double z = FastUtils.randomDoubleInRange(-1, 0) * (zNeg ? -1 : 1);
		Vector dir = new Vector(
			x, FastUtils.randomDoubleInRange(0, 0.5), z
		).normalize().multiply(0.2);
		Location l = loc.clone().subtract(0, 0.1, 0);
		new BukkitRunnable() {

			int mT = 0;
			int mSizeCounter = 0;
			final int DURATION = 4;
			final int ITERATIONS = 4;

			@Override
			public void run() {
				mT++;

				for (int i = 0; i < ITERATIONS; i++) {
					l.add(dir);
					mSizeCounter++;
					float progress = (float) mSizeCounter / (DURATION * ITERATIONS);
					new PartialParticle(Particle.ITEM_CRACK, l, 1, 0.1, 0.1, 0.1, 0.05,
						new ItemStack(Material.SPRUCE_LOG)).spawnAsPlayerActive(player);
					new PartialParticle(Particle.REDSTONE, l, 5, 0.1 * (1 - progress),
						0.1 * (1 - progress), 0.1 * (1 - progress), 0.1,
						new Particle.DustOptions(
							EARTH_COLOR, 1.125f * (1F - (0.75f * progress))
						))

						.spawnAsPlayerActive(player);
				}

				if (mT >= DURATION) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
