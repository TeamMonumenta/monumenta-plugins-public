package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;



public class CleansingRain extends Ability {

	private static final int CLEANSING_DURATION = 15 * 20;
	private static final double PERCENT_DAMAGE_RESIST = -0.2;
	private static final int CLEANSING_EFFECT_DURATION = 3 * 20;
	private static final int CLEANSING_APPLY_PERIOD = 1;
	private static final int CLEANSING_RADIUS = 4;
	private static final int CLEANSING_RADIUS_ENHANCED = 6;
	private static final int CLEANSING_1_COOLDOWN = 45 * 20;
	private static final int CLEANSING_2_COOLDOWN = 30 * 20;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "CleansingPercentDamageResistEffect";
	public static final String CHARM_REDUCTION = "Cleansing Rain Damage Reduction";
	public static final String CHARM_DURATION = "Cleansing Rain Duration";
	public static final String CHARM_RANGE = "Cleansing Rain Range";
	public static final String CHARM_COOLDOWN = "Cleansing Rain Cooldown";

	public static final AbilityInfo<CleansingRain> INFO =
		new AbilityInfo<>(CleansingRain.class, "Cleansing Rain", CleansingRain::new)
			.linkedSpell(ClassAbility.CLEANSING_RAIN)
			.scoreboardId("Cleansing")
			.shorthandName("CR")
			.descriptions(
				"Right click while sneaking and looking upwards to summon a \"cleansing rain\" that follows you, " +
					"removing negative effects from players within %s blocks, including yourself, and lasts for %s seconds. Cooldown: %ss."
						.formatted(CLEANSING_RADIUS, StringUtils.ticksToSeconds(CLEANSING_DURATION), StringUtils.ticksToSeconds(CLEANSING_1_COOLDOWN)),
				"Additionally grants %s%% Damage Reduction to all players in the radius. Cooldown: %ss."
					.formatted(StringUtils.multiplierToPercentage(Math.abs(PERCENT_DAMAGE_RESIST)), StringUtils.ticksToSeconds(CLEANSING_2_COOLDOWN)),
				"The radius increases to %s blocks, and each player touched by the rain keeps its effect for the cast duration."
					.formatted(CLEANSING_RADIUS_ENHANCED))
			.cooldown(CLEANSING_1_COOLDOWN, CLEANSING_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CleansingRain::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true).lookDirections(AbilityTrigger.LookDirection.UP),
				AbilityTriggerInfo.NOT_HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(new ItemStack(Material.NETHER_STAR, 1));

	private final double mRadius;

	public CleansingRain(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(player, CHARM_RANGE, isEnhanced() ? CLEANSING_RADIUS_ENHANCED : CLEANSING_RADIUS);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.45f, 0.8f);
		putOnCooldown();

		// Run cleansing rain here until it finishes
		new BukkitRunnable() {
			int mTicks = 0;
			final List<Player> mCleansedPlayers = new ArrayList<>();

			@Override
			public void run() {

				if (!mPlayer.isOnline() || mPlayer.isDead()) {
					this.cancel();
					return;
				}

				double ratio = mRadius / CLEANSING_RADIUS;
				double smallRatio = ratio / 3;
				new PartialParticle(Particle.CLOUD, mPlayer.getLocation().add(0, 4, 0), (int) (5 * ratio * ratio), 2.5 * ratio, 0.35, 2.5 * ratio, 0).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.WATER_DROP, mPlayer.getLocation().add(0, 2, 0), (int) (15 * ratio * ratio), 2.5 * ratio, 2, 2.5 * ratio, 0.001).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.VILLAGER_HAPPY, mPlayer.getLocation().add(0, 2, 0), (int) (1 * ratio * ratio), 2 * ratio, 1.5, 2 * ratio, 0.001).minimumMultiplier(false).spawnAsPlayerActive(mPlayer);

				List<Player> rainPlayers = PlayerUtils.playersInRange(mPlayer.getLocation(), mRadius, true);
				for (Player player : rainPlayers) {
					if (isEnhanced() && !mCleansedPlayers.contains(player)) {
						mCleansedPlayers.add(player);
						continue;
					}
					PotionUtils.clearNegatives(mPlugin, player);
					EntityUtils.setWeakenTicks(mPlugin, player, 0);
					EntityUtils.setSlowTicks(mPlugin, player, 0);

					if (player.getFireTicks() > 1) {
						player.setFireTicks(1);
					}

					if (isLevelTwo()) {
						mPlugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(CLEANSING_EFFECT_DURATION, PERCENT_DAMAGE_RESIST - CharmManager.getLevelPercentDecimal(mPlayer, CHARM_REDUCTION)));
					}
				}
				//Loop through already affected players for enhanced cleansing rain
				if (isEnhanced()) {
					for (Player player : mCleansedPlayers) {
						if (!rainPlayers.contains(player) && player != mPlayer) {
							new PartialParticle(Particle.CLOUD, player.getLocation().add(0, 4, 0), (int) (5 * smallRatio * smallRatio), 2.5 * smallRatio, 0.35, 2.5 * smallRatio, 0).spawnAsPlayerActive(player);
							new PartialParticle(Particle.WATER_DROP, player.getLocation().add(0, 2, 0), (int) (15 * smallRatio * smallRatio), 2.5 * smallRatio, 2, 2.5 * smallRatio, 0.001).spawnAsPlayerActive(player);
							new PartialParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 2, 0), (int) (1 * smallRatio * smallRatio), 2 * smallRatio, 1.5, 2 * smallRatio, 0.001).minimumMultiplier(false).spawnAsPlayerActive(player);
						}

						PotionUtils.clearNegatives(mPlugin, player);
						EntityUtils.setWeakenTicks(mPlugin, player, 0);
						EntityUtils.setSlowTicks(mPlugin, player, 0);

						if (player.getFireTicks() > 1) {
							player.setFireTicks(1);
						}

						if (isLevelTwo()) {
							mPlugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(CLEANSING_EFFECT_DURATION, PERCENT_DAMAGE_RESIST));
						}
					}
				}

				mTicks += CLEANSING_APPLY_PERIOD;
				if (mTicks > CharmManager.getDuration(mPlayer, CHARM_DURATION, CLEANSING_DURATION)) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, CLEANSING_APPLY_PERIOD);
	}

}
