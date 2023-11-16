package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CleansingTotem extends TotemAbility {

	private static final String HEAL_EFFECT_NAME = "CleansingTotemHealing";
	private static final int EFFECT_DURATION = 2 * 20;

	public static final Particle.DustOptions DUST_CLEANSING_RING = new Particle.DustOptions(Color.fromRGB(0, 87, 255), 1.25f);

	private static final int COOLDOWN = 30 * 20;
	private static final int AOE_RANGE = 6;
	private static final double HEAL_PERCENT = 0.06;
	private static final int INTERVAL = 20;
	private static final int DURATION_1 = 8 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final int CLEANSES = 2;
	private static final double ENHANCE_HEALING_PERCENT = 0.2;
	private static final int ENHANCE_RADIUS = 3;

	public static String CHARM_DURATION = "Cleansing Totem Duration";
	public static String CHARM_RADIUS = "Cleansing Totem Radius";
	public static String CHARM_COOLDOWN = "Cleansing Totem Cooldown";
	public static String CHARM_HEALING = "Cleansing Totem Healing";
	public static String CHARM_CLEANSES = "Cleansing Totem Cleanses";
	public static String CHARM_ENHANCE_HEALING = "Cleansing Totem Enhance Healing";
	public static String CHARM_PULSE_DELAY = "Cleansing Totem Pulse Delay";

	private final int mDuration;
	private final double mRadius;
	private final int mInterval;

	public static final AbilityInfo<CleansingTotem> INFO =
		new AbilityInfo<>(CleansingTotem.class, "Cleansing Totem", CleansingTotem::new)
			.linkedSpell(ClassAbility.CLEANSING_TOTEM)
			.scoreboardId("CleansingTotem")
			.shorthandName("CT")
			.descriptions(
				String.format("Left click while holding a melee weapon and sneaking to fire a projectile that summons a Cleansing Totem. Players within %s blocks of this totem " +
					"heal for %s%% of their maximum health per second. No chargeup time, Duration: %ss. Cooldown: %ss.",
					AOE_RANGE,
					StringUtils.multiplierToPercentage(HEAL_PERCENT),
					StringUtils.ticksToSeconds(DURATION_1),
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("Duration is increased to %ss and now cleanses debuffs for players %s times evenly throughout it's duration.",
					StringUtils.ticksToSeconds(DURATION_2),
					CLEANSES),
				String.format("Throwing the totem cleanses the shaman and heals %s%% max health instantly. " +
					"Players within %s blocks of the landing location receive the same effects on impact.",
					StringUtils.multiplierToPercentage(ENHANCE_HEALING_PERCENT),
					ENHANCE_RADIUS
				)
			)
			.simpleDescription("Summon a totem that heals and cleanses players over its duration.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CleansingTotem::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.BLUE_STAINED_GLASS);

	public CleansingTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Cleansing Totem Projectile", "CleansingTotem", "Cleansing Totem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, INTERVAL);
		mChargeUpTicks = 0;
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks == 0) {
			world.playSound(standLocation, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 2.0f, 1.3f);
			world.playSound(standLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 2.0f);
			if (isEnhanced()) {
				List<Player> players = PlayerUtils.playersInRange(standLocation, ENHANCE_RADIUS, true);
				cleanseTargets(players);
				players.removeIf(player -> player == mPlayer);
				for (Player player : players) {
					PlayerUtils.healPlayer(mPlugin, player, CharmManager.calculateFlatAndPercentValue(mPlayer,
						CHARM_ENHANCE_HEALING, ENHANCE_HEALING_PERCENT * EntityUtils.getMaxHealth(player)), mPlayer);
					new PPCircle(Particle.HEART, player.getLocation(), 1).ringMode(true)
						.countPerMeter(0.8).spawnAsPlayerActive(mPlayer);
				}
			}
		}
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
		}
		if (isLevelTwo() && ticks == mDuration / (CLEANSES + (int) CharmManager.getLevel(mPlayer, CHARM_CLEANSES)) - 1) {
			List<Player> cleansePlayers = PlayerUtils.playersInRange(standLocation, mRadius, true);
			cleanseTargets(cleansePlayers);
			new PPCircle(Particle.HEART, standLocation, mRadius).ringMode(false).countPerMeter(0.8).spawnAsPlayerActive(mPlayer);
		}
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean bonusAction) {
		if (bonusAction) {
			List<Player> players = PlayerUtils.playersInRange(standLocation, mRadius, true);
			for (Player p : players) {
				PlayerUtils.healPlayer(mPlugin, p,
					CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_HEALING,
						EntityUtils.getMaxHealth(p) * HEAL_PERCENT * ChainLightning.ENHANCE_POSITIVE_EFFICIENCY));
			}
			if (isLevelTwo()) {
				cleanseTargets(players);
				new PPCircle(Particle.HEART, standLocation, mRadius)
					.ringMode(false).countPerMeter(0.8).spawnAsPlayerActive(mPlayer);
			}
		} else {
			List<Player> affectedPlayers = PlayerUtils.playersInRange(standLocation, mRadius, true);

			for (LivingEntity p : affectedPlayers) {
				double maxHealth = EntityUtils.getMaxHealth(p);
				mPlugin.mEffectManager.clearEffects(p, HEAL_EFFECT_NAME);
				mPlugin.mEffectManager.addEffect(p, HEAL_EFFECT_NAME, new CustomRegeneration(EFFECT_DURATION,
					CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING,
						maxHealth * HEAL_PERCENT), mPlayer, mPlugin));
			}

			PPCircle cleansingRing = new PPCircle(Particle.REDSTONE, standLocation, mRadius)
				.countPerMeter(1.05).delta(0).extra(0.05).data(DUST_CLEANSING_RING);
			PPSpiral cleansingSpiral = new PPSpiral(Particle.REDSTONE, standLocation, mRadius)
				.distancePerParticle(0.075).ticks(5).count(1).delta(0).extra(0.05).data(DUST_CLEANSING_RING);
			cleansingRing.spawnAsPlayerActive(mPlayer);
			cleansingSpiral.spawnAsPlayerActive(mPlayer);
			dealSanctuaryImpacts(EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null), 40);
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		new PartialParticle(Particle.HEART, standLocation, 45, 0.2, 1.1, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(standLocation, Sound.BLOCK_WOOD_BREAK, 0.7f, 0.5f);
	}

	@Override
	public void onTotemCast() {
		if (isEnhanced()) {
			cleanseTargets(List.of(mPlayer));
			PlayerUtils.healPlayer(mPlugin, mPlayer, CharmManager.calculateFlatAndPercentValue(mPlayer,
				CHARM_ENHANCE_HEALING, ENHANCE_HEALING_PERCENT * EntityUtils.getMaxHealth(mPlayer)), mPlayer);
			new PPCircle(Particle.HEART, mPlayer.getLocation(), 1).ringMode(true)
				.countPerMeter(0.8).spawnAsPlayerActive(mPlayer);
		}
	}

	private void cleanseTargets(List<Player> cleansePlayers) {
		for (Player player : cleansePlayers) {
			PotionUtils.clearNegatives(mPlugin, player);
			EntityUtils.setWeakenTicks(mPlugin, player, 0);
			EntityUtils.setSlowTicks(mPlugin, player, 0);

			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}
	}
}
