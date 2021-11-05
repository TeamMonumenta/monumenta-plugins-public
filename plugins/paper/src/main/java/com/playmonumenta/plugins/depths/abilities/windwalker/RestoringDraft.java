package com.playmonumenta.plugins.depths.abilities.windwalker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class RestoringDraft extends DepthsAbility {

	public static final String ABILITY_NAME = "Restoring Draft";
	public static final double[] HEALING = {0.2, 0.3, 0.4, 0.5, 0.6, 0.8};
	public static final int HEIGHT_CAP = 12;
	private static final String SLAM_ONCE_THIS_TICK_METAKEY = "RestoringDraftTickSlammed";
	public static final int AUTOMATIC_THRESHOLD = 4;

	private final BukkitRunnable mSlamAttackRunner;
	private double mFallFromY = -7050;

	public RestoringDraft(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.GOLDEN_BOOTS;
		mTree = DepthsTree.WINDWALKER;

		mInfo.mIgnoreCooldown = true;

		mSlamAttackRunner = new BukkitRunnable() {
			@Override
			public void run() {
				if (player == null) {
					this.cancel();
					return;
				}
				if (
					DepthsManager.getInstance().getPlayerLevelInAbility(ABILITY_NAME, player) == 0
					|| !player.isValid() // Ensure player is not dead, is still online?
				) {
					// If reached this point but not silenced, then proceed with cancelling
					// If silenced, only return to not run anything, but don't cancel runnable
					if (!AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
						this.cancel();
					}
					return;
				}

				if (!player.isOnGround()) {
					updateFallFrom(); // Vanilla fall distance would be 0 if on ground
				} else {
					// Currently on ground

					// If first tick landing, should still have old mFallFromY to calculate using
					// Therefore can damage if eligible
					if (
						calculateFallDistance() > AUTOMATIC_THRESHOLD
					) {
						// Only for checking in LivingEntityDamagedByPlayerEvent below,
						// so doesn't slam twice, since this doesn't yet set fall distance to 0
						MetadataUtils.checkOnceThisTick(mPlugin, player, SLAM_ONCE_THIS_TICK_METAKEY);

						heal();
					}

					// Whether or not did attack, now that on ground, forget mFallFromY
					mFallFromY = -7050;
				}
			}
		};
		mSlamAttackRunner.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void invalidate() {
		if (mSlamAttackRunner != null) {
			mSlamAttackRunner.cancel();
		}
	}

	private void updateFallFrom() {
		double currentY = mPlayer.getLocation().getY();
		double fallDistance = mPlayer.getFallDistance();
		mFallFromY = currentY + fallDistance;
	}

	private double calculateFallDistance() {
		double currentY = mPlayer.getLocation().getY();
		double fallDistance = mFallFromY - currentY;
		return Math.max(fallDistance, 0);
	}

	private void heal() {
		double fallDistance = calculateFallDistance();
		double healing = Math.min(HEIGHT_CAP, fallDistance) * HEALING[mRarity - 1];
		PlayerUtils.healPlayer(mPlayer, healing);

		World world = mPlayer.getWorld();
		Location location = mPlayer.getLocation();
		world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.2f);
		world.spawnParticle(Particle.FIREWORKS_SPARK, location, 40, 0F, 0F, 0F, 0.2F);
		world.spawnParticle(Particle.HEART, location.add(0, 1, 0), (int) (5 * healing), 0.3, 0.3, 0.3, 0.3F);
	}

	@Override
	public boolean playerDamagedEvent(EntityDamageEvent event) {
		if (event.getCause() == DamageCause.FALL) {
			event.setCancelled(true);
		}
		return true;
	}


	@Override
	public String getDescription(int rarity) {
		return "Falling more than " + AUTOMATIC_THRESHOLD + " blocks heals you by " + DepthsUtils.getRarityColor(rarity) + HEALING[rarity - 1] + ChatColor.WHITE + " health per block fallen (up to " + HEIGHT_CAP + " blocks). All fall damage is canceled.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.WINDWALKER;
	}
}

