package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.shaman.ShamanPassiveManager;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.soothsayer.TotemicConsecrationCS;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class TotemicConsecration extends MultipleChargeAbility {
	private static final int CHARGES = 2;
	private static final int COOLDOWN = 20 * 20;
	private static final int COOLDOWN_2 = 16 * 20;
	private static final double DAMAGE_1 = 10;
	private static final double DAMAGE_2 = 13;
	private static final double BONUS_DAMAGE = 1;
	private static final int DURATION_PER_BONUS = 50; // 20 * 2.5 = 50 ticks
	private static final double ABSORPTION_PERCENT = 0.2;
	private static final double HEALTH_THRESHOLD = 0.4;
	private static final double TOTEM_RADIUS_AMPLIFIER = 0.15;
	private static final int ABSORPTION_DURATION = 20 * 20;
	private static final double RESISTANCE = 0.15;
	private static final int RES_BUFF_DURATION = 20;
	private static final int SILENCE_DURATION = 5 * 20;
	private static final String CONSECRATION_RESISTANCE_SOURCE = "Totemic Consecration Resistance";

	public static final String CHARM_CHARGES = "Totemic Consecration Charges";
	public static final String CHARM_COOLDOWN = "Totemic Consecration Cooldown";
	public static final String CHARM_DAMAGE = "Totemic Consecration Base Damage";
	public static final String CHARM_BONUS_DAMAGE = "Totemic Consecration Bonus Damage";
	public static final String CHARM_DURATION_PER_BONUS = "Totemic Consecration Duration Required Per Bonus Damage";
	public static final String CHARM_ABSORPTION = "Totemic Consecration Absorption Amplifier";
	public static final String CHARM_HP_THRESHOLD = "Totemic Consecration Health Threshold";
	public static final String CHARM_RADIUS_AMPLIFIER = "Totemic Consecration Totem Radius Amplifier";
	public static final String CHARM_RESISTANCE = "Totemic Consecration Resistance Amplifier";
	public static final String CHARM_SILENCE_DURATION = "Totemic Consecration Silence Duration";

	public static final Style SACRED_COLOR = Style.style(TextColor.color(0xDED3B1));

	public static final AbilityInfo<TotemicConsecration> INFO =
		new AbilityInfo<>(TotemicConsecration.class, "Totemic Consecration", TotemicConsecration::new)
			.linkedSpell(ClassAbility.TOTEMIC_CONSECRATION)
			.scoreboardId("TotemicConsecration")
			.shorthandName("TC")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Mark totems as Sacred, instantly dealing damage around them and protecting players within their range.")
			.cooldown(COOLDOWN, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", TotemicConsecration::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false).doubleClick()
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.addAltPresetTrigger(new AbilityTriggerInfo<>("cast", "cast", TotemicConsecration::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false).onGround(true).lookDirections(AbilityTrigger.LookDirection.LEVEL, AbilityTrigger.LookDirection.UP)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.YELLOW_CANDLE);

	private final double mBaseDamage;
	private final double mBonusDamage;
	private final int mDurationPerBonus;
	private final double mAbsorption;
	private final double mHealthThreshold;
	private final double mRadiusAmplifier;
	private final double mResistance;
	private final int mSilenceDuration;
	private final TotemicConsecrationCS mCosmetic;

	private final List<TotemAbility> mBlessedTotems = new ArrayList<>();
	private int mLastCastTicks;

	public TotemicConsecration(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = CHARGES + (int) CharmManager.getLevel(player, CHARM_CHARGES);
		mCharges = getTrackedCharges();
		mBaseDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mBonusDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_BONUS_DAMAGE, BONUS_DAMAGE);
		mDurationPerBonus = CharmManager.getDuration(player, CHARM_DURATION_PER_BONUS, DURATION_PER_BONUS);
		mAbsorption = ABSORPTION_PERCENT + CharmManager.getLevelPercentDecimal(player, CHARM_ABSORPTION);
		mHealthThreshold = HEALTH_THRESHOLD + CharmManager.getLevelPercentDecimal(player, CHARM_HP_THRESHOLD);
		mRadiusAmplifier = isLevelOne() ? 0 : (TOTEM_RADIUS_AMPLIFIER + CharmManager.getLevelPercentDecimal(player, CHARM_RADIUS_AMPLIFIER));
		mResistance = RESISTANCE + CharmManager.getLevelPercentDecimal(player, CHARM_RESISTANCE);
		mSilenceDuration = CharmManager.getDuration(player, CHARM_SILENCE_DURATION, SILENCE_DURATION);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TotemicConsecrationCS());
	}

	public boolean cast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || mCharges <= 0) {
			return false;
		}
		mLastCastTicks = ticks;

		// Get all the player's totem entities and order them by distance
		List<LivingEntity> totemEntitiesByDistance = ShamanPassiveManager.getTotemList(mPlayer).stream()
			.sorted(Comparator.comparingDouble(totem -> mPlayer.getLocation().distance(totem.getLocation())))
			.toList();
		if (totemEntitiesByDistance.isEmpty()) {
			return false;
		}

		// Search for totem entities in a cone in front of the player, filter to just the player's totems, then order those by distance too
		Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), 20, Math.toRadians(18))
			.union(Hitbox.approximateCone(mPlayer.getEyeLocation(), 3, Math.toRadians(30)));
		List<LivingEntity> totemsInHitbox = hitbox.getHitEntitiesByClass(ArmorStand.class).stream()
			.map(armorStand -> (LivingEntity) armorStand)
			.filter(totemEntitiesByDistance::contains)
			.sorted(Comparator.comparingDouble(totem -> mPlayer.getLocation().distance(totem.getLocation())))
			.toList();

		// Get the first non-blessed totem in the hitbox, or the nearest non-blessed totem if that fails
		TotemAbility targetTotem = Optional.ofNullable(getFirstNonBlessedTotem(totemsInHitbox))
			.orElseGet(() -> getFirstNonBlessedTotem(totemEntitiesByDistance));
		if (targetTotem == null) {
			return false;
		}
		consumeCharge();

		targetTotem.mIsBlessed = true;
		mBlessedTotems.add(targetTotem);
		if (isLevelTwo()) {
			targetTotem.setTotemRadiusMultiplier(1 + mRadiusAmplifier);
		}

		double totemRadius = targetTotem.getTotemRadius();
		double remainingDuration = targetTotem.getRemainingAbilityDuration();
		double finalDamage = mBaseDamage + mBonusDamage * (int) (remainingDuration / mDurationPerBonus);
		Location targetLoc = targetTotem.getTotemLocation();

		mCosmetic.consecrationAction(mPlayer, targetLoc, totemRadius);
		List<LivingEntity> affectedMobs = new Hitbox.SphereHitbox(targetLoc, totemRadius).getHitMobs();

		for (LivingEntity mob : affectedMobs) {
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC,
				mInfo.getLinkedSpell()), finalDamage, true, true, false);

			if (isLevelOne() || EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				continue;
			}
			EntityUtils.applySilence(mPlugin, mSilenceDuration, mob);
		}

		return true;
	}

	private @Nullable TotemAbility getFirstNonBlessedTotem(List<LivingEntity> totemEntities) {
		for (LivingEntity totemEntity : totemEntities) {
			TotemAbility totemAbility = ShamanPassiveManager.getTotemAbility(totemEntity);
			if (totemAbility != null && !totemAbility.mIsBlessed) {
				return totemAbility;
			}
		}
		return null;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		manageChargeCooldowns();
		mBlessedTotems.removeIf(totem -> !totem.mIsBlessed);

		if (twoHertz) {
			for (TotemAbility blessedTotem : mBlessedTotems) {
				for (Player player : blessedTotem.getPlayersInRange()) {
					mPlugin.mEffectManager.addEffect(player, CONSECRATION_RESISTANCE_SOURCE, new PercentDamageReceived(RES_BUFF_DURATION, -mResistance).displaysTime(false).deleteOnAbilityUpdate(true));
				}
			}
		}

		for (TotemAbility blessedTotem : mBlessedTotems) {
			for (Player player : blessedTotem.getPlayersInRange()) {
				if (player.getHealth() / EntityUtils.getMaxHealth(player) < mHealthThreshold) {
					if (isLevelTwo()) {
						blessedTotem.setTotemRadiusMultiplier(1);
					}
					blessedTotem.mIsBlessed = false;
					Location loc = blessedTotem.getTotemLocation();
					if (loc == null) {
						continue;
					}

					double absorption = mAbsorption * EntityUtils.getMaxHealth(player);
					AbsorptionUtils.addAbsorption(player, absorption, absorption * 2, ABSORPTION_DURATION);
					mCosmetic.absorptionTriggered(player, loc, blessedTotem.getTotemRadius());
					mBlessedTotems.remove(blessedTotem);
					break;
				}
			}
			if (!blessedTotem.mIsBlessed) { // if not blessed then the code above has run
				break;
			}
		}
	}

	private static Description<TotemicConsecration> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Consecrate a targeted *Totem* as a *Sacred Totem*.").styles(Shaman.TOTEM_COLOR, SACRED_COLOR)
			.addLine()
			.addLine("The *Sacred Totem* immediately deals damage to all").styles(SACRED_COLOR)
			.addLine("mobs in its area, plus bonus damage based on its")
			.addLine("remaining duration.")
			.addLine()
			.addStat("Damage: %d1 (s), +%d1 per %t of duration left").styles(Shaman.TOTEM_COLOR)
				.statValues(stat(a -> a.mBaseDamage, DAMAGE_1), stat(a -> a.mBonusDamage, BONUS_DAMAGE), stat(a -> a.mDurationPerBonus, DURATION_PER_BONUS))
			.addStat("Charges: %d")
				.statValues(stat(a -> a.mMaxCharges, CHARGES))
			.addStat("Cooldown: %t1 (per charge)")
				.statValues(cooldown(COOLDOWN))
			.addLine()
			.addLine("*Sacred Totems* grant resistance to players inside.").styles(SACRED_COLOR)
			.addLine("If a player's health drops below %p HP, the *Totem*").styles(Shaman.TOTEM_COLOR)
				.statValues(stat(a -> a.mHealthThreshold, HEALTH_THRESHOLD))
			.addLine("loses its *Sacred* power to grant them absorption.").styles(SACRED_COLOR)
			.addLine()
			.addStat("Effect: +%p Resistance")
				.statValues(stat(a -> a.mResistance, RESISTANCE))
			.addStat("Effect: +%p Absorption for %t")
				.statValues(stat(a -> a.mAbsorption, ABSORPTION_PERCENT), stat(ABSORPTION_DURATION))
			.addDashedLine();
	}

	private static Description<TotemicConsecration> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Totemic Consecration*'s damage.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (s)")
				.statValues(stat(DAMAGE_1), stat(a -> a.mBaseDamage, DAMAGE_2))
			.addLine()
			.addLine("Consecrating a *Totem* as a *Sacred Totem*").styles(Shaman.TOTEM_COLOR, SACRED_COLOR)
			.addLine("now increases its radius and silences all")
			.addLine("mobs inside.")
			.addLine("(Elites/Bosses are immune)")
			.addLine()
			.addStat("Radius Increase: +%p")
				.statValues(stat(a -> a.mRadiusAmplifier, TOTEM_RADIUS_AMPLIFIER))
			.addStat("Effect: Silence for %t")
				.statValues(stat(a -> a.mSilenceDuration, SILENCE_DURATION))
			.addDashedLine();
	}
}
