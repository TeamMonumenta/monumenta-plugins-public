package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hexfall.HexfallUtils;
import com.playmonumenta.plugins.particle.PPCircle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellDestroyTotemPlatform extends Spell {

	private final Plugin mPlugin;
	private final boolean mLifeOrDeath;
	private final Entity mArmorStand;
	private final Location mCenterLoc;

	public SpellDestroyTotemPlatform(Plugin plugin, boolean lifeOrDeath, Entity armorStand, Location centerLoc) {
		mPlugin = plugin;
		mLifeOrDeath = lifeOrDeath;
		mArmorStand = armorStand;
		mCenterLoc = centerLoc;
	}

	@Override
	public void run() {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			final int mBreakTicks = 60;
			final TextDisplay mCastBar = mArmorStand.getWorld().spawn(mArmorStand.getLocation().add(0, 3, 0), TextDisplay.class);

			@Override
			public void run() {
				if (mArmorStand.getScoreboardTags().contains("Hycenea_TotemicDestruction_ShieldActive")) {
					return;
				}

				if (mT == 0) {
					mCastBar.setAlignment(TextDisplay.TextAlignment.CENTER);
					mCastBar.setBillboard(Display.Billboard.CENTER);
					mCastBar.addScoreboardTag("HexfallDisplay");
				}

				Component indicatorName = Component.empty().append(Component.text("[", NamedTextColor.WHITE))
					.append(Component.text("|".repeat(mBreakTicks - mT), NamedTextColor.GREEN))
					.append(Component.text("|".repeat(mT), NamedTextColor.RED))
					.append(Component.text("]", NamedTextColor.WHITE));
				mCastBar.text(indicatorName);

				if (mT % 5 == 0) {
					for (Player player : HexfallUtils.getPlayersInHycenea(mCenterLoc)) {
						player.playSound(mArmorStand.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 1f, 1f);
					}

					for (int i = 1; i < 9; i++) {
						new PPCircle(Particle.BLOCK_DUST, mArmorStand.getLocation(), i)
							.data(mLifeOrDeath ? Material.MOSS_BLOCK.createBlockData() : Material.DIRT.createBlockData())
							.count(10)
							.spawnAsBoss();
					}
				}

				if (mArmorStand.getScoreboardTags().contains("Hycenea_StranglingRupture_Target") || !mArmorStand.getNearbyEntities(1, 1, 1)
					.stream().filter(entity -> entity.getScoreboardTags().contains("boss_totemplatform") && !entity.isDead()).toList().isEmpty()
					|| HexfallUtils.getPlayersInHycenea(mCenterLoc).isEmpty()) {
					this.cancel();
				}

				if (mT++ >= mBreakTicks) {
					this.cancel();

					for (Player player : HexfallUtils.getPlayersInHycenea(mCenterLoc)) {
						player.playSound(mArmorStand.getLocation(), Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 1f, 2f);
					}

					HexfallUtils.clearPlatformAndAbove(mArmorStand.getLocation().clone());
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				mCastBar.remove();
				super.cancel();
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
