package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class Smokescreen extends Ability {

	private static final int SMOKESCREEN_RANGE = 6;
	private static final int SMOKESCREEN_DURATION = 8 * 20;
	private static final double SMOKESCREEN_SLOWNESS_AMPLIFIER = 0.2;
	private static final double WEAKEN_EFFECT_1 = 0.2;
	private static final double WEAKEN_EFFECT_2 = 0.4;
	private static final int SMOKESCREEN_COOLDOWN = 20 * 20;
	private static final int ENHANCEMENT_SMOKECLOUD_DURATION = 8 * 20;
	private static final int ENHANCEMENT_SMOKECLOUD_EFFECT_DURATION = 2 * 20;
	private static final int ENHANCEMENT_SMOKECLOUD_RADIUS = 4;

	public static final String CHARM_SLOW = "Smokescreen Slowness Amplifier";
	public static final String CHARM_WEAKEN = "Smokescreen Weakness Amplifier";
	public static final String CHARM_COOLDOWN = "Smokescreen Cooldown";
	public static final String CHARM_RANGE = "Smokescreen Range";

	private final double mWeakenEffect;

	public Smokescreen(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Smoke Screen");
		mInfo.mLinkedSpell = ClassAbility.SMOKESCREEN;
		mInfo.mScoreboardId = "SmokeScreen";
		mInfo.mShorthandName = "Smk";
		mInfo.mDescriptions.add(
			String.format("When holding two swords, right-click while sneaking and looking down to release a cloud of smoke, afflicting all enemies in a %s block radius with %ss of %s%% Weaken and %s%% Slowness. Cooldown: %ss.",
				SMOKESCREEN_RANGE,
				SMOKESCREEN_DURATION / 20,
				(int)(WEAKEN_EFFECT_1 * 100),
				(int)(SMOKESCREEN_SLOWNESS_AMPLIFIER * 100),
				SMOKESCREEN_COOLDOWN / 20));
		mInfo.mDescriptions.add(
			String.format("The Weaken debuff is increased to %s%%.",
				(int)(WEAKEN_EFFECT_2 * 100)));
		mInfo.mDescriptions.add(
			String.format("Leave a %s block radius persistent cloud on the ground for %s seconds after activating. Enemies in the cloud gain the same debuffs for %s seconds, pulsing every second.",
				ENHANCEMENT_SMOKECLOUD_RADIUS,
				ENHANCEMENT_SMOKECLOUD_DURATION / 20,
				ENHANCEMENT_SMOKECLOUD_EFFECT_DURATION / 20));
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, SMOKESCREEN_COOLDOWN);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.DEAD_TUBE_CORAL, 1);
		mWeakenEffect = CharmManager.getLevelPercentDecimal(player, CHARM_WEAKEN) + (isLevelOne() ? WEAKEN_EFFECT_1 : WEAKEN_EFFECT_2);
	}

	@Override
	public void cast(Action action) {
		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 750, 4.5, 0.8, 4.5, 0.05).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 1500, 4.5, 0.2, 4.5, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.35f);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, CharmManager.getRadius(mPlayer, CHARM_RANGE, SMOKESCREEN_RANGE), mPlayer)) {
			EntityUtils.applySlow(mPlugin, SMOKESCREEN_DURATION, SMOKESCREEN_SLOWNESS_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOW), mob);
			EntityUtils.applyWeaken(mPlugin, SMOKESCREEN_DURATION, mWeakenEffect, mob);
		}
		putOnCooldown();

		if (isEnhanced()) {
			new BukkitRunnable() {
				Location mCloudLocation = loc.clone();
				int mT = 0;

				@Override
				public void run() {
					if (mT > ENHANCEMENT_SMOKECLOUD_DURATION) {
						this.cancel();
						return;
					} else {
						if (mT > 0) {
							// Visuals are based off of Hekawt's UndeadRogue Smokescreen Spell
							new PartialParticle(Particle.SMOKE_NORMAL, mCloudLocation, 3, 0.3, 0.05, 0.3, 0.075).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.SMOKE_NORMAL, mCloudLocation, 75, 3.5, 0.2, 4.5, 0.05).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.SMOKE_LARGE, mCloudLocation, 2, 0.3, 0.05, 0.3, 0.075).spawnAsPlayerActive(mPlayer);
							new PartialParticle(Particle.SMOKE_LARGE, mCloudLocation, 30, 3.5, 0.8, 4.5, 0.025).spawnAsPlayerActive(mPlayer);

							world.playSound(mCloudLocation, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1, 0.7f);

							for (LivingEntity mob : EntityUtils.getNearbyMobs(mCloudLocation, ENHANCEMENT_SMOKECLOUD_RADIUS, mPlayer)) {
								EntityUtils.applySlow(mPlugin, ENHANCEMENT_SMOKECLOUD_EFFECT_DURATION, SMOKESCREEN_SLOWNESS_AMPLIFIER, mob);
								EntityUtils.applyWeaken(mPlugin, ENHANCEMENT_SMOKECLOUD_EFFECT_DURATION, mWeakenEffect, mob);
							}
						}
						mT += 20;
					}
				}
			}.runTaskTimer(mPlugin, 0, 20);
		}
	}

	@Override
	public boolean runCheck() {
		if (mPlayer.isSneaking() && mPlayer.getLocation().getPitch() > 50) {
			return InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer);
		}
		return false;
	}

}
