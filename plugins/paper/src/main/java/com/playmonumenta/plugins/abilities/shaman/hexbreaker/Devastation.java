package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.shaman.CleansingTotem;
import com.playmonumenta.plugins.abilities.shaman.FlameTotem;
import com.playmonumenta.plugins.abilities.shaman.LightningTotem;
import com.playmonumenta.plugins.abilities.shaman.ShamanPassiveManager;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker.DevastationCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Devastation extends Ability {
	private static final int COOLDOWN = 15 * 20;
	private static final int RADIUS_1 = 6;
	private static final int RADIUS_2 = 8;
	private static final int DAMAGE_1 = 28;
	private static final int DAMAGE_2 = 36;
	private static final int CDR_ON_KILL = 3 * 20;
	private static final int FIRE_STRENGTH_DURATION = 6 * 20;
	private static final double FIRE_STRENGTH = 0.15;
	private static final int LIGHTNING_DAMAGE = 8;
	private static final int LIGHTNING_STUN_DURATION = 30;
	private static final int CLEANSE_DURATION = 10 * 20;
	private static final double CLEANSE_WEAKEN = 0.3;
	private static final double DECAY_DAMAGE = 6;
	private static final float KNOCKBACK = 0.5f;
	private static final String STRENGTH_SOURCE = "Devastation Strength";

	public static final String CHARM_DAMAGE = "Devastation Damage";
	public static final String CHARM_RADIUS = "Devastation Radius";
	public static final String CHARM_COOLDOWN = "Devastation Cooldown";
	public static final String CHARM_CDR = "Devastation Cooldown Reduction";
	public static final String CHARM_FIRE_STRENGTH_DURATION = "Devastation Flame Totem Strength Duration";
	public static final String CHARM_FIRE_STRENGTH = "Devastation Flame Totem Strength Amplifier";
	public static final String CHARM_LIGHTNING_DAMAGE = "Devastation Lightning Totem Damage";
	public static final String CHARM_LIGHTNING_STUN = "Devastation Lightning Totem Stun Duration";
	public static final String CHARM_CLEANSE_DURATION = "Devastation Cleansing Totem Weakness Duration";
	public static final String CHARM_CLEANSE_WEAKEN = "Devastation Cleansing Totem Weakness Amplifier";
	public static final String CHARM_DECAY_DAMAGE = "Devastation Decayed Totem Bonus Damage";

	public static final AbilityInfo<Devastation> INFO =
		new AbilityInfo<>(Devastation.class, "Devastation", Devastation::new)
			.linkedSpell(ClassAbility.DEVASTATION)
			.scoreboardId("Devastation")
			.shorthandName("DV")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Destroy your nearest totem, dealing massive damage within a medium radius.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Devastation::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false).doubleClick()
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.addAltPresetTrigger(new AbilityTriggerInfo<>("cast", "cast", Devastation::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false).onGround(true).lookDirections(AbilityTrigger.LookDirection.LEVEL, AbilityTrigger.LookDirection.UP)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.COAL_BLOCK);

	public double mDamage;
	private final double mRadius;
	private final int mCooldownReduction;
	private final int mFireStrengthDuration;
	private final double mFireStrengthPotency;
	private final double mLightningDamage;
	private final int mLightningStunDuration;
	private final int mCleanseDuration;
	private final double mCleanseWeaken;
	private final double mDecayDamage;
	private final DevastationCS mCosmetic;

	public Devastation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelOne() ? RADIUS_1 : RADIUS_2);
		mCooldownReduction = CharmManager.getDuration(mPlayer, CHARM_CDR, CDR_ON_KILL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DevastationCS());
		mFireStrengthDuration = CharmManager.getDuration(mPlayer, CHARM_FIRE_STRENGTH_DURATION, FIRE_STRENGTH_DURATION);
		mFireStrengthPotency = FIRE_STRENGTH + CharmManager.getLevelPercentDecimal(player, CHARM_FIRE_STRENGTH);
		mLightningDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_LIGHTNING_DAMAGE, LIGHTNING_DAMAGE);
		mLightningStunDuration = CharmManager.getDuration(mPlayer, CHARM_LIGHTNING_STUN, LIGHTNING_STUN_DURATION);
		mCleanseDuration = CharmManager.getDuration(mPlayer, CHARM_CLEANSE_DURATION, CLEANSE_DURATION);
		mCleanseWeaken = CLEANSE_WEAKEN + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_CLEANSE_WEAKEN);
		mDecayDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DECAY_DAMAGE, DECAY_DAMAGE);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		List<LivingEntity> totemList = ShamanPassiveManager.getTotemList(mPlayer);
		if (totemList.isEmpty()) {
			return false;
		}

		Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), 20, Math.toRadians(18))
			.union(Hitbox.approximateCone(mPlayer.getEyeLocation(), 3, Math.toRadians(30)));
		List<ArmorStand> totemsInHitbox = hitbox.getHitEntitiesByClass(ArmorStand.class);
		totemsInHitbox.removeIf(totem -> !totemList.contains(totem));
		LivingEntity totemToNuke;
		if (!totemsInHitbox.isEmpty()) {
			totemToNuke = totemsInHitbox.getFirst();
			for (LivingEntity totem : totemsInHitbox) {
				if (!totemToNuke.equals(totem) && mPlayer.getLocation().distance(totemToNuke.getLocation()) > mPlayer.getLocation().distance(totem.getLocation())) {
					totemToNuke = totem;
				}
			}
		} else {
			totemToNuke = totemList.getFirst();
			for (LivingEntity totem : totemList) {
				if (!totemToNuke.equals(totem) && mPlayer.getLocation().distance(totemToNuke.getLocation()) > mPlayer.getLocation().distance(totem.getLocation())) {
					totemToNuke = totem;
				}
			}
		}
		Location targetLoc = totemToNuke.getLocation();
		TotemAbility targetTotem = null;
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			if (abil instanceof TotemAbility totemAbility
				&& totemAbility.getRemainingAbilityDuration() > 0
				&& totemAbility.mDisplayName.equalsIgnoreCase(totemToNuke.getName())) {
				ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
				if (linkedSpell != null) {
					putOnCooldown();
					targetTotem = totemAbility;
					mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, mCooldownReduction);
				}
			}
		}
		if (targetTotem == null) {
			return false;
		}
		ShamanPassiveManager.removeTotem(mPlayer, totemToNuke);

		mCosmetic.devastationCast(mPlugin, targetLoc, mPlayer, mRadius);

		double devastationDamage = mDamage;

		if (isLevelTwo()) {
			if (targetTotem instanceof FlameTotem) {
				mPlugin.mEffectManager.addEffect(mPlayer, STRENGTH_SOURCE, new PercentDamageDealt(mFireStrengthDuration, mFireStrengthPotency).deleteOnAbilityUpdate(true));
			} else if (targetTotem instanceof LightningTotem) {
				List<LivingEntity> targetsSortedByHP = new Hitbox.SphereHitbox(targetLoc, mRadius).getHitMobs()
					.stream()
					.sorted(Comparator.comparingDouble(Damageable::getHealth).reversed())
					.toList();

				targetsSortedByHP.forEach(mob -> EntityUtils.applyStun(mPlugin, mLightningStunDuration, mob));

				DamageUtils.damage(mPlayer, targetsSortedByHP.getFirst(), DamageEvent.DamageType.MAGIC, mLightningDamage, mInfo.getLinkedSpell(), true);
			} else if (targetTotem instanceof CleansingTotem) {
				for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, mRadius)) {
					EntityUtils.applyWeaken(mPlugin, mCleanseDuration, mCleanseWeaken, mob);
				}
			} else if (targetTotem instanceof DecayedTotem) {
				devastationDamage += mDecayDamage;
			}
		}

		for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, mRadius)) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, devastationDamage, INFO.getLinkedSpell(), true);
			if (!EntityUtils.isCCImmuneMob(mob)) {
				MovementUtils.knockAway(targetLoc, mob, KNOCKBACK, true);
			}
		}

		return true;
	}

	private static Description<Devastation> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Destroy a targeted *Totem*, dealing damage").styles(Shaman.TOTEM_COLOR)
			.addLine("to mobs near it and knocking them away.")
			.addLine()
			.addLine("Reduce the cooldown of the destroyed *Totem*.").styles(Shaman.TOTEM_COLOR)
			.addLine()
			.addStat("Damage: %d1 (s)").statValues(stat(a -> a.mDamage, DAMAGE_1))
			.addStat("Radius: %r").statValues(stat(a -> a.mRadius, RADIUS_1))
			.addStat("Cooldown Reduction: %t").statValues(stat(a -> a.mCooldownReduction, CDR_ON_KILL))
			.addStat("Cooldown: %t").statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<Devastation> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Devastation*'s damage and radius.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (s)").statValues(stat(DAMAGE_1), stat(a -> a.mDamage, DAMAGE_2))
			.addStatComparison("Radius: %r1 -> %r2").statValues(stat(RADIUS_1), stat(a -> a.mRadius, RADIUS_2))
			.addLine()
			.addLine("*Devastation* performs additional effects").styles(UNDERLINED)
			.addLine("depending on which *Totem* was destroyed.").styles(Shaman.TOTEM_COLOR)
			.addLine()
			.addLine("*Cleansing Totem*: Devastation now weakens mobs.").styles(UNDERLINED)
			.addStat("Effect: %p Weakness for %t")
				.statValues(stat(a -> a.mCleanseWeaken, CLEANSE_WEAKEN), stat(a -> a.mCleanseDuration, CLEANSE_DURATION))
			.addLine()
			.addLine("*Flame Totem*: You gain a damage boost.").styles(UNDERLINED)
			.addStat("Effect: +%p Damage for %t")
				.statValues(stat(a -> a.mFireStrengthPotency, FIRE_STRENGTH), stat(a -> a.mFireStrengthDuration, FIRE_STRENGTH_DURATION))
			.addLine()
			.addLine("*Lightning Totem*: Damage the mob with the highest").styles(UNDERLINED)
			.addLine("HP and Devastation now stuns mobs.")
			.addStat("Damage: %d (s)")
				.statValues(stat(a -> a.mLightningDamage, LIGHTNING_DAMAGE))
			.addStat("Effect: Stun for %t")
				.statValues(stat(a -> a.mLightningStunDuration, LIGHTNING_STUN_DURATION))
			.addLine()
			.addLine("*Decayed Totem*: Devastation deals more damage.").styles(UNDERLINED)
			.addStat("Damage Boost: +%d (s)")
				.statValues(stat(a -> a.mDecayDamage, DECAY_DAMAGE))
			.addDashedLine();
	}
}
