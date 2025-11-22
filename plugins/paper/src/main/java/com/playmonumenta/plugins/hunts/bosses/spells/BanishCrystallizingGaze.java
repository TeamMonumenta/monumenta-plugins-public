package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.Quarry;
import com.playmonumenta.plugins.hunts.bosses.TheImpenetrable;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class BanishCrystallizingGaze extends Spell {
	// Lifetime of the spell in ticks
	private static final int BANISH_DURATION = 4 * 20;

	// Maximum range for players to be in the banish
	private static final int MAXIMUM_RANGE = 25;

	private final Plugin mPlugin;

	private final LivingEntity mBoss;

	private final TheImpenetrable mImpenetrable;

	private final ChargeUpManager mChargeUp;

	public BanishCrystallizingGaze(Plugin plugin, LivingEntity boss, TheImpenetrable impenetrable) {
		mPlugin = plugin;
		mBoss = boss;
		mImpenetrable = impenetrable;
		mChargeUp = new ChargeUpManager(mBoss, BANISH_DURATION, Component.text(""), BossBar.Color.BLUE, BossBar.Overlay.PROGRESS, 60);
	}

	@Override
	public void run() {
		mImpenetrable.banishStarted();

		mChargeUp.reset();
		mChargeUp.setTitle(Component.text("Revealing ", TextColor.color(32, 119, 145)).append(Component.text(String.format("Crystallizing Gaze (%s)", Quarry.BANISH_CHARACTER), TextColor.color(85, 169, 194))));
		mChargeUp.update();

		mImpenetrable.openShell();
		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 2.0f, 0.5f);

		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;
			final List<Player> mWatchedPlayers = new ArrayList<>();

			@Override
			public void run() {
				if (mTicks < BANISH_DURATION) {
					// Fancy particles
					Location particleLoc1 = mBoss.getLocation().add(FastUtils.cos((double) mTicks / 10) * 2, FastUtils.cos((double) mTicks / 15) / 1.3 + 1, FastUtils.sin((double) mTicks / 10) * 2);
					Location particleLoc2 = mBoss.getLocation().add(FastUtils.cos((double) (mTicks + 20) / 10) * 2, FastUtils.cos((double) (mTicks + 20) / 15) / 1.3 + 1, FastUtils.sin((double) (mTicks + 20) / 10) * 2);
					Location particleLoc3 = mBoss.getLocation().add(FastUtils.cos((double) (mTicks + 40) / 10) * 2, FastUtils.cos((double) (mTicks + 40) / 15) / 1.3 + 1, FastUtils.sin((double) (mTicks + 40) / 10) * 2);
					new PartialParticle(Particle.WAX_OFF, particleLoc1).spawnAsBoss();
					new PartialParticle(Particle.SNOWFLAKE, particleLoc1).extra(0).spawnAsBoss();
					new PartialParticle(Particle.WAX_OFF, particleLoc2).spawnAsBoss();
					new PartialParticle(Particle.SNOWFLAKE, particleLoc2).extra(0).spawnAsBoss();
					new PartialParticle(Particle.WAX_OFF, particleLoc3).spawnAsBoss();
					new PartialParticle(Particle.SNOWFLAKE, particleLoc3).extra(0).spawnAsBoss();
					if (mTicks % 16 == 0) {
						new PPParametric(Particle.WAX_OFF, mBoss.getLocation().clone().add(0, 0.2, 0),
							(param, builder) -> {
								double theta = param * Math.PI * 2;

								Vector dir = new Vector(FastUtils.cos(theta) * 2, 0, FastUtils.sin(theta) * 2);

								builder.offset(dir.getX(), 0, dir.getZ());
								builder.location(mBoss.getLocation());
							})
							.count(160)
							.directionalMode(true)
							.extra(2.4)
							.spawnAsBoss();
					}

					mChargeUp.setProgress((float) mTicks / BANISH_DURATION);

					if (mTicks % 10 == 0) {
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_MEDIUM_AMETHYST_BUD_PLACE, SoundCategory.HOSTILE, 2.0f, ((float) mTicks / BANISH_DURATION) * 1.1f + 0.5f);
					}

					// Recalculate players in danger of being banished
					mWatchedPlayers.clear();
					PlayerUtils.playersInRange(mBoss.getLocation(), MAXIMUM_RANGE, true).forEach(player -> {
						if (hasLineOfSight(player, mBoss.getEyeLocation())) {
							mWatchedPlayers.add(player);
						}
					});

					mWatchedPlayers.forEach(player -> {
						new PartialParticle(Particle.SPELL_WITCH, player.getLocation().clone().add(0, 0.2, 0)).count(5).delta(0.2, 0, 0.2).spawnAsBoss();
						if (mTicks % 8 == 0) {
							player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, SoundCategory.HOSTILE, 1.5f, 0.5f);
						}
					});
				}

				// Run the banish
				if (mTicks == BANISH_DURATION) {
					for (Player player : mWatchedPlayers) {
						player.sendMessage(Component.text("The Impenetrable stares into your soul, banishing you to the lodge.", NamedTextColor.DARK_PURPLE));
						mImpenetrable.banish(player);
					}

					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.HOSTILE, 3.0f, 1.0f);

					mImpenetrable.closeShell();
					mImpenetrable.banishFinished();
				}

				mTicks++;
				if (mTicks > BANISH_DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private boolean hasLineOfSight(Player player, Location location) {
		Location fromLocation = player.getEyeLocation();
		int range = (int) fromLocation.distance(location);
		Vector direction = location.toVector().subtract(fromLocation.toVector()).normalize();

		try {
			BlockIterator bi = new BlockIterator(fromLocation.getWorld(), fromLocation.toVector(), direction, 0, range);

			while (bi.hasNext()) {
				Block b = bi.next();

				// If block is occluding (shouldn't include transparent blocks, liquids etc.),
				// line of sight is broken, return false
				if (BlockUtils.isPathBlockingBlock(b.getType())) {
					return false;
				}
			}
		} catch (IllegalStateException e) {
			// Thrown sometimes when chunks aren't loaded at exactly the right time
			return false;
		}

		return true;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	@Override
	public boolean persistOnPhaseChange() {
		return true;
	}
}
