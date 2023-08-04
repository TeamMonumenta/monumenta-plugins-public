package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.ShamanCooldownDecreasePerSecond;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class WhirlwindTotem extends TotemAbility {

	private static final int COOLDOWN = 25 * 20;
	private static final int INTERVAL = 2 * 20;
	private static final int AOE_RANGE = 5;
	private static final int DURATION_1 = 8 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final double CDR_PERCENT = 0.025;
	private static final int CDR_MAX_PER_SECOND = 20;
	private static final double SPEED_PERCENT = 0.1;
	private static final String WHIRLWIND_SPEED_EFFECT_NAME = "WhirlwindSpeedEffect";
	public static final int SILENCE_DURATION = 8 * 20;
	public static final double DURATION_BOOST = 0.25;

	public static final String CHARM_DURATION = "Whirlwind Totem Duration";
	public static final String CHARM_RADIUS = "Whirlwind Totem Radius";
	public static final String CHARM_COOLDOWN = "Whirlwind Totem Cooldown";
	public static final String CHARM_CDR = "Whirlwind Totem Cooldown Reduction Per Second";
	public static final String CHARM_MAX_CDR = "Whirlind Totem Maximum Cooldown Reduction Per Second";
	public static final String CHARM_SPEED = "Whirlwind Totem Speed";
	public static final String CHARM_SILENCE_DURATION = "Whirlwind Totem Adhesion Silence Duration";
	public static final String CHARM_DURATION_BOOST = "Whirlwind Totem Duration Buff";

	public static final AbilityInfo<WhirlwindTotem> INFO =
		new AbilityInfo<>(WhirlwindTotem.class, "Whirlwind Totem", WhirlwindTotem::new)
			.linkedSpell(ClassAbility.WHIRLWIND_TOTEM)
			.scoreboardId("WhirlwindTotem")
			.shorthandName("WWT")
			.descriptions(
				String.format("Swap with a melee weapon to fire a projectile which will summon a Whirlwind Totem. Every %ss, all players in a %s block radius " +
						"have their class cooldowns reduced by %s%% (maximum %ss). Cannot decrease the cooldown on any player's whirlwind totem, and does not stack " +
						"with other whirlwind totems (%ss duration, %ss cooldown). Additionally, apply a %s%% duration boost to other totems existing at any point " +
						"in this totem's duration.",
					StringUtils.ticksToSeconds(INTERVAL),
					AOE_RANGE,
					StringUtils.multiplierToPercentage(CDR_PERCENT),
					StringUtils.ticksToSeconds(CDR_MAX_PER_SECOND),
					StringUtils.ticksToSeconds(DURATION_1),
					StringUtils.ticksToSeconds(COOLDOWN),
					StringUtils.multiplierToPercentage(DURATION_BOOST)
				),
				String.format("Totem duration increased to %ss, and now gives %s%% speed to players within range.",
					StringUtils.ticksToSeconds(DURATION_2),
					StringUtils.multiplierToPercentage(SPEED_PERCENT))
			)
			.simpleDescription("Summon a totem that provides cooldown reduction to players within its radius.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WhirlwindTotem::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.BLUE_STAINED_GLASS);

	private final int mDuration;
	private final double mRadius;
	private final double mCDRPerSecond;
	private final int mCDRMax;
	private final double mSpeed;
	private final double mDurationBoost;

	public WhirlwindTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Whirlwind Totem Projectile", "WhirlwindTotem", "Whirlwind Totem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, AOE_RANGE);
		mCDRPerSecond = CDR_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CDR);
		mCDRMax = CharmManager.getDuration(mPlayer, CHARM_MAX_CDR, CDR_MAX_PER_SECOND);
		mSpeed = isLevelOne() ? 0 : (SPEED_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED));
		mDurationBoost = DURATION_BOOST + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DURATION_BOOST);
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks == 0) {
			world.playSound(standLocation, Sound.ENTITY_ENDER_EYE_LAUNCH, 2.0f, 0.1f);
			world.playSound(standLocation, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 2.0f, 0.7f);
			world.playSound(standLocation, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 2.0f, 1.3f);
			applyWhirlwindDurationBoost();
		}
		if (ticks % INTERVAL == 0) {
			List<Player> affectedPlayers = PlayerUtils.playersInRange(standLocation, mRadius, true);

			for (Player p : affectedPlayers) {
				mPlugin.mEffectManager.addEffect(p, "WhirlwindTotemCDR", new ShamanCooldownDecreasePerSecond(50, mCDRPerSecond, mCDRMax, mPlugin));
				if (isLevelTwo()) {
					mPlugin.mEffectManager.addEffect(p, WHIRLWIND_SPEED_EFFECT_NAME, new PercentSpeed(50, mSpeed, WHIRLWIND_SPEED_EFFECT_NAME));
				}
			}

			PPSpiral windSpiral = new PPSpiral(Particle.SPELL_INSTANT, standLocation, mRadius).distancePerParticle(.05).ticks(5).count(1).delta(0);
			windSpiral.spawnAsPlayerActive(mPlayer);
			world.playSound(standLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.3f, 0.5f);
			dealSanctuaryImpacts(EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null), INTERVAL + 20);
			applyWhirlwindDurationBoost();
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		new PartialParticle(Particle.HEART, standLocation, 45, 0.2, 1.1, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(standLocation, Sound.BLOCK_WOOD_BREAK, 0.7f, 0.5f);
	}

	@Override
	public void onAdhereToMob(LivingEntity hitMob) {
		EntityUtils.applySilence(mPlugin, CharmManager.getDuration(mPlayer, CHARM_SILENCE_DURATION, SILENCE_DURATION), hitMob);
	}

	public void applyWhirlwindDurationBoost() {
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			if (abil instanceof TotemAbility totemAbility && totemAbility.getRemainingAbilityDuration() > 0 && !(abil instanceof WhirlwindTotem)) {
				totemAbility.mWhirlwindBuffPercent = 1 + mDurationBoost;
			}
		}
	}
}
