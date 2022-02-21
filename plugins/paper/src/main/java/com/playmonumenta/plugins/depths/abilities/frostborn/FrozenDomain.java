package com.playmonumenta.plugins.depths.abilities.frostborn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.md_5.bungee.api.ChatColor;

public class FrozenDomain extends DepthsAbility {

	public static final String ABILITY_NAME = "Frozen Domain";
	public static final double[] EXTRA_SPEED_PCT = {.1, .125, .15, .175, .20, .25};
	public static final double[] REGEN_TIME = {2, 1.75, 1.5, 1.25, 1, .75}; //seconds
	private static final int DURATION_TICKS = 100;
	private static final double PERCENT_HEAL = .05;
	private static final String ATTR_NAME = "FrozenDomainExtraSpeedAttr";
	private boolean mWasOnIce = false;
	private int mSecondWhenIce = 0;
	private int mSeconds = 0;

	public FrozenDomain(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.IRON_BOOTS;
		mTree = DepthsTree.FROSTBORN;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mPlayer == null) {
			return;
		}
		if (twoHertz && isOnIce(mPlayer)) {
			mPlayer.getLocation().getWorld().spawnParticle(Particle.SNOW_SHOVEL, mPlayer.getLocation(), 8, 0, 0, 0, 0.65);
		}
		if (oneSecond) {
			if (mPlayer.isOnGround() && isOnIce(mPlayer)) {
				mWasOnIce = true;
				mSecondWhenIce = mSeconds;
				handleSpeed();
				handleHeal();
			} else {
				offIceHeal();
			}
			if (mSeconds >= mSecondWhenIce + DURATION_TICKS / 20) {
				mWasOnIce = false;
			}
			mSeconds++;
		}

	}

	public void handleParticles() {
		if (mPlayer == null) {
			return;
		}
		mPlayer.getLocation().getWorld().spawnParticle(Particle.HEART, mPlayer.getLocation().add(0, 1, 0), 5, 0, 0, 0, 0.65);
		new BukkitRunnable() {
			int mCount = 0;

			@Override
			public void run() {
				mPlayer.getLocation().getWorld().spawnParticle(Particle.SNOW_SHOVEL, mPlayer.getLocation().add(0, 1, 0), 8, 0, 0, 0, 0.65);
				if (mCount >= 5) {
					this.cancel();
				}
				mCount++;
			}
		}.runTaskTimer(mPlugin, 0, 4);
	}

	public void handleHeal() {
		if (mSeconds % REGEN_TIME[mRarity - 1] == 0) {
			applyHealing();
		}
	}

	public void offIceHeal() {
		if (mWasOnIce && mSeconds % REGEN_TIME[mRarity - 1] == 0) {
			applyHealing();
		}
	}

	public void applyHealing() {
		double maxHealth = EntityUtils.getMaxHealth(mPlayer);
		PlayerUtils.healPlayer(mPlugin, mPlayer, PERCENT_HEAL * maxHealth, mPlayer);
		handleParticles();
	}

	public void handleSpeed() {
		mPlugin.mEffectManager.addEffect(mPlayer, "FrozenDomainExtraSpeed", new PercentSpeed(DURATION_TICKS, EXTRA_SPEED_PCT[mRarity - 1], ATTR_NAME));
	}

	public boolean isOnIce(LivingEntity entity) {
		Location loc = entity.getLocation();
		if (loc.getBlock().getRelative(BlockFace.DOWN).getType() == DepthsUtils.ICE_MATERIAL && DepthsUtils.iceActive.containsKey(loc.getBlock().getRelative(BlockFace.DOWN).getLocation())) {
			return true;
		}
		return false;
	}

	@Override
	public String getDescription(int rarity) {
		String s = "s";
		if (REGEN_TIME[rarity - 1] == 20) {
			s = "";
		}
		return "When standing on ice, gain " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(EXTRA_SPEED_PCT[rarity - 1]) + "%" + ChatColor.WHITE + " speed and regain " + (int) DepthsUtils.roundPercent(PERCENT_HEAL) + "% of your max health every "
				+ DepthsUtils.getRarityColor(rarity) + REGEN_TIME[rarity - 1] + ChatColor.WHITE + " second" + s + ". Effects last for " + DURATION_TICKS / 20 + " seconds after leaving ice.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FROSTBORN;
	}
}

