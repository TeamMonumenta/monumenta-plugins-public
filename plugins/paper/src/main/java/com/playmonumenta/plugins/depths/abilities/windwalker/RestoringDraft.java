package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public final class RestoringDraft extends DepthsAbility {

	public static final String ABILITY_NAME = "Restoring Draft";
	public static final double[] HEALING = {0.1, 0.15, 0.2, 0.25, 0.3, 0.75};
	public static final int HEIGHT_CAP = 12;
	private static final String SLAM_ONCE_THIS_TICK_METAKEY = "RestoringDraftTickSlammed";
	public static final int AUTOMATIC_THRESHOLD = 4;

	public static final DepthsAbilityInfo<RestoringDraft> INFO =
		new DepthsAbilityInfo<>(RestoringDraft.class, ABILITY_NAME, RestoringDraft::new, DepthsTree.WINDWALKER, DepthsTrigger.PASSIVE)
			.displayItem(Material.GOLDEN_BOOTS)
			.descriptions(RestoringDraft::getDescription)
			.singleCharm(false);

	private final double mHeightCap;
	private final double mHealing;

	private final BukkitRunnable mSlamAttackRunner;
	private double mFallFromY = -7050;

	public RestoringDraft(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHeightCap = HEIGHT_CAP + CharmManager.getLevel(mPlayer, CharmEffects.RESTORING_DRAFT_BLOCK_CAP.mEffectName);
		mHealing = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.RESTORING_DRAFT_HEALING.mEffectName, HEALING[mRarity - 1]);

		mSlamAttackRunner = new BukkitRunnable() {
			@Override
			public void run() {
				checkSlamAttack();
			}
		};
		cancelOnDeath(mSlamAttackRunner.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public void invalidate() {
		if (mSlamAttackRunner != null) {
			mSlamAttackRunner.cancel();
		}
	}

	private void checkSlamAttack() {
		Player player = mPlayer;
		if (player == null) {
			if (mSlamAttackRunner != null) {
				mSlamAttackRunner.cancel();
			}
			return;
		}
		if (
			DepthsManager.getInstance().getPlayerLevelInAbility(ABILITY_NAME, player) == 0
				|| player.isDead()
				|| !player.isOnline()
		) {
			// If reached this point but not silenced, then proceed with cancelling
			// If silenced, only return to not run anything, but don't cancel runnable
			if (!AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
				if (mSlamAttackRunner != null) {
					mSlamAttackRunner.cancel();
				}
			}
			return;
		}

		if (!PlayerUtils.isOnGround(player)) {
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

	private void updateFallFrom() {
		if (mPlayer.getFallDistance() <= 0) {
			mFallFromY = -10000;
		} else {
			mFallFromY = Math.max(mFallFromY, mPlayer.getLocation().getY());
		}
	}

	private double calculateFallDistance() {
		double currentY = mPlayer.getLocation().getY();
		double fallDistance = mFallFromY - currentY;
		return Math.max(fallDistance, 0);
	}

	private void heal() {
		double fallDistance = calculateFallDistance();
		double cappedDistance = Math.min(mHeightCap, fallDistance);
		double healing = cappedDistance * mHealing;
		PlayerUtils.healPlayer(mPlugin, mPlayer, healing);

		World world = mPlayer.getWorld();
		Location location = mPlayer.getLocation();
		world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.2f);
		new PartialParticle(Particle.FIREWORKS_SPARK, location, 40, 0F, 0F, 0F, 0.2F).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.HEART, location.add(0, 1, 0), (int) (5 * healing), 0.3, 0.3, 0.3, 0.3F).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageType.FALL) {
			event.setCancelled(true);
		}
	}


	private static Description<RestoringDraft> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<RestoringDraft>(color)
			.add("Falling more than ")
			.add(AUTOMATIC_THRESHOLD)
			.add(" blocks heals you by ")
			.add(a -> a.mHealing, HEALING[rarity - 1], false, null, true)
			.add(" health per block fallen (up to ")
			.add(a -> a.mHeightCap, HEIGHT_CAP)
			.add(" blocks). All fall damage is canceled.");
	}
}

