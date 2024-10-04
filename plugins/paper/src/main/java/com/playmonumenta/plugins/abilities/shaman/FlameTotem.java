package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.FlameTotemCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FlameTotem extends TotemAbility {

	private static final int COOLDOWN = 22 * 20;
	private static final int INTERVAL = 20;
	private static final int DURATION_1 = 10 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final int AOE_RANGE_1 = 6;
	private static final int AOE_RANGE_2 = 7;
	private static final int FIRE_DURATION = 2 * 20;
	private static final int DAMAGE_1 = 5;
	private static final int DAMAGE_2 = 6;
	private static final double BOMB_RADIUS = 3;
	private static final int BOMB_COUNT = 1;
	private static final int ENHANCE_BOMB_BONUS = 1;
	private static final double ENHANCE_INFERNO_SCALE = 0.5;

	public static final String CHARM_DURATION = "Flame Totem Duration";
	public static final String CHARM_RADIUS = "Flame Totem Radius";
	public static final String CHARM_COOLDOWN = "Flame Totem Cooldown";
	public static final String CHARM_DAMAGE = "Flame Totem Damage";
	public static final String CHARM_FIRE_DURATION = "Flame Totem Fire Duration";
	public static final String CHARM_BOMB_RADIUS = "Flame Totem Explosion Radius";
	public static final String CHARM_BOMB_COUNT = "Flame Totem Explosion Count";
	public static final String CHARM_ENHANCE_INFERNO_SCALE = "Flame Totem Enhancement Inferno Efficiency";
	public static final String CHARM_PULSE_DELAY = "Flame Totem Pulse Delay";

	private final double mDamage;
	private final double mRadius;
	private final double mBombRadius;
	private final int mBombCount;
	private final int mFireDuration;
	private final double mEnhanceInfernoScale;
	private final int mInterval;

	public double mDecayedTotemBuff = 0;

	public static final AbilityInfo<FlameTotem> INFO =
		new AbilityInfo<>(FlameTotem.class, "Flame Totem", FlameTotem::new)
			.linkedSpell(ClassAbility.FLAME_TOTEM)
			.scoreboardId("FlameTotem")
			.shorthandName("FT")
			.descriptions(
				String.format("Right click while holding a melee weapon and sneaking to fire a projectile that summons a flame totem. The totem throws explosive fireballs "
						+ "at a target within range, dealing %s damage in a %s block radius and sets mobs on fire"
						+ ", without inferno damage, for %s seconds every second. Charge up time: %ss. Duration: %ss. Cooldown: %ss.",
					DAMAGE_1,
					AOE_RANGE_1,
					StringUtils.ticksToSeconds(FIRE_DURATION),
					StringUtils.ticksToSeconds(TotemAbility.PULSE_DELAY),
					StringUtils.ticksToSeconds(DURATION_1),
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("The totem deals %s magic damage per hit, radius is increased to %s, " +
						"and duration is extended to %ss.",
					DAMAGE_2,
					AOE_RANGE_2,
					StringUtils.ticksToSeconds(DURATION_2)),
				String.format("Gains %s extra bomb per pulse and applies inferno at %s%% efficiency.",
					ENHANCE_BOMB_BONUS,
					StringUtils.multiplierToPercentage(ENHANCE_INFERNO_SCALE))
			)
			.simpleDescription("Summon a totem that deals damage and sets mobs on fire within its range.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", FlameTotem::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.MAGMA_BLOCK);

	private final FlameTotemCS mCosmetic;

	public FlameTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Flame Totem Projectile", "FlameTotem", "Flame Totem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE,
			(isLevelOne() ? DAMAGE_1 : DAMAGE_2));

		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, isLevelOne() ? DURATION_1 : DURATION_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelTwo() ? AOE_RANGE_2 : AOE_RANGE_1);
		mFireDuration = CharmManager.getDuration(mPlayer, CHARM_FIRE_DURATION, FIRE_DURATION);
		mBombRadius = CharmManager.getRadius(mPlayer, CHARM_BOMB_RADIUS, BOMB_RADIUS);
		mBombCount = BOMB_COUNT + (int) CharmManager.getLevel(mPlayer, CHARM_BOMB_COUNT)
			+ (isEnhanced() ? ENHANCE_BOMB_BONUS : 0);
		mEnhanceInfernoScale = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_INFERNO_SCALE, ENHANCE_INFERNO_SCALE);
		mInterval = CharmManager.getDuration(mPlayer, CHARM_PULSE_DELAY, INTERVAL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new FlameTotemCS());
	}

	@Override
	public void placeTotem(Location standLocation, Player player, ArmorStand stand) {
		mCosmetic.flameTotemSpawn(standLocation, player, stand, mRadius);
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (isEnhanced()) {
			mCosmetic.flameTotemTickEnhanced(mPlayer, standLocation, mRadius);
		} else {
			mCosmetic.flameTotemTick(mPlayer, standLocation, mRadius);
		}
		if (ticks % mInterval == 0) {
			pulse(standLocation, stats, false);
			dealSanctuaryImpacts(EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null), 40);
		}
	}

	@Override
	public void pulse(Location standLocation, ItemStatManager.PlayerItemStats stats, boolean bonusAction) {
		List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobsInSphere(standLocation, mRadius, null);
		double damageApplied = (mDamage + mDecayedTotemBuff)
			* (bonusAction ? ChainLightning.ENHANCE_NEGATIVE_EFFICIENCY : 1);
		Collections.shuffle(affectedMobs);
		List<LivingEntity> impactedMobs = new ArrayList<>();
		List<LivingEntity> targetMobs = new ArrayList<>();
		if (!affectedMobs.isEmpty()) {
			for (int i = 0; i < mBombCount; i++) {
				LivingEntity finalTarget = null;
				for (LivingEntity target : affectedMobs) {
					if (!impactedMobs.contains(target)) {
						finalTarget = target;
						break;
					}
				}
				if (finalTarget == null) {
					// No target that hasn't been hit yet, so just pick a random one to bomb
					finalTarget = affectedMobs.get(0);
				}
				targetMobs.add(finalTarget);
				Location targetLocation = finalTarget.getLocation();

				List<LivingEntity> newImpactedMobs = EntityUtils.getNearbyMobsInSphere(targetLocation, mBombRadius, null);
				for (LivingEntity mob : newImpactedMobs) {
					DamageUtils.damage(mPlayer, mob,
						new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), stats),
						damageApplied, true, false, false);
					EntityUtils.applyFire(mPlugin, mFireDuration, mob, mPlayer,
						stats, mEnhanceInfernoScale);
					impactedMobs.add(mob);
				}
			}
			if (isEnhanced()) {
				mCosmetic.flameTotemBombEnhanced(mPlayer, targetMobs, standLocation, mPlugin, mBombRadius);
			} else {
				mCosmetic.flameTotemBomb(mPlayer, targetMobs, standLocation, mPlugin, mBombRadius);
			}
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		mDecayedTotemBuff = 0;
		mCosmetic.flameTotemExpire(world, mPlayer, standLocation);
	}
}
