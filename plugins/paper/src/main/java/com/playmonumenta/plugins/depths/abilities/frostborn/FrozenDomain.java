package com.playmonumenta.plugins.depths.abilities.frostborn;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FrozenDomain extends DepthsAbility {

	public static final String ABILITY_NAME = "Frozen Domain";
	public static final double[] SPEED_PERCENT = {.1, .125, .15, .175, .20, .25};
	public static final int[] REGEN_INTERVAL = {40, 35, 30, 25, 20, 15};
	private static final int DURATION_TICKS = 100;
	private static final double PERCENT_HEAL = .05;
	private static final String ATTR_NAME = "FrozenDomainExtraSpeedAttr";

	public static final DepthsAbilityInfo<FrozenDomain> INFO =
		new DepthsAbilityInfo<>(FrozenDomain.class, ABILITY_NAME, FrozenDomain::new, DepthsTree.FROSTBORN, DepthsTrigger.PASSIVE)
			.displayItem(Material.IRON_BOOTS)
			.descriptions(FrozenDomain::getDescription)
			.singleCharm(false);

	private final int mDuration;
	private final double mSpeedPercent;
	private final double mPercentHeal;

	private boolean mWasOnIce = false;
	private int mTickWhenIce = 0;
	private int mTicks = 0;

	public FrozenDomain(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.FROZEN_DOMAIN_DURATION.mEffectName, DURATION_TICKS);
		mSpeedPercent = CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.FROZEN_DOMAIN_SPEED_AMPLIFIER.mEffectName) + SPEED_PERCENT[mRarity - 1];
		mPercentHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.FROZEN_DOMAIN_HEALING.mEffectName, PERCENT_HEAL);
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		boolean isOnIce = isOnIce(mPlayer);
		if (isOnIce) {
			new PartialParticle(Particle.SNOW_SHOVEL, mPlayer.getLocation(), 4, 0, 0, 0, 0.65).spawnAsPlayerPassive(mPlayer);
		}

		if (PlayerUtils.isOnGround(mPlayer) && isOnIce) {
			mWasOnIce = true;
			mTickWhenIce = mTicks;
			handleSpeed();
			handleHeal();
		} else if (mWasOnIce) {
			handleHeal();
		}
		if (mTicks >= mTickWhenIce + mDuration) {
			mWasOnIce = false;
		}
		mTicks += 5;

		if (isOnIce && mPlayer.getFireTicks() > 1) {
			mPlayer.setFireTicks(1);
		}

	}

	public void handleParticles() {
		new PartialParticle(Particle.HEART, mPlayer.getLocation().add(0, 1, 0), 5, 0, 0, 0, 0.65).spawnAsPlayerPassive(mPlayer);
		new BukkitRunnable() {
			int mCount = 0;

			@Override
			public void run() {
				new PartialParticle(Particle.SNOW_SHOVEL, mPlayer.getLocation().add(0, 1, 0), 8, 0, 0, 0, 0.65).spawnAsPlayerPassive(mPlayer);
				if (mCount >= 5) {
					this.cancel();
				}
				mCount++;
			}
		}.runTaskTimer(mPlugin, 0, 4);
	}

	public void handleHeal() {
		if (mTicks % REGEN_INTERVAL[mRarity - 1] == 0) {
			double maxHealth = EntityUtils.getMaxHealth(mPlayer);
			PlayerUtils.healPlayer(mPlugin, mPlayer, mPercentHeal * maxHealth, mPlayer);
			handleParticles();
		}
	}

	public void handleSpeed() {
		mPlugin.mEffectManager.addEffect(mPlayer, "FrozenDomainExtraSpeed", new PercentSpeed(mDuration, mSpeedPercent, ATTR_NAME));
	}

	public boolean isOnIce(LivingEntity entity) {
		Location loc = entity.getLocation();
		return DepthsUtils.isIce(loc.getBlock().getRelative(BlockFace.DOWN).getType()) &&
			DepthsUtils.iceActive.containsKey(loc.getBlock().getRelative(BlockFace.DOWN).getLocation());
	}

	private static Description<FrozenDomain> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<FrozenDomain>(color)
			.add("When standing on ice, gain ")
			.addPercent(a -> a.mSpeedPercent, SPEED_PERCENT[rarity - 1], false, true)
			.add(" speed and heal ")
			.addPercent(a -> a.mPercentHeal, PERCENT_HEAL)
			.add(" of your max health every ")
			.addDuration(a -> REGEN_INTERVAL[rarity - 1], REGEN_INTERVAL[rarity - 1], true, true)
			.add("s. Effects last for ")
			.addDuration(a -> a.mDuration, DURATION_TICKS)
			.add(" seconds after leaving ice. Standing on ice also extinguishes you if you are on fire.");
	}
}

