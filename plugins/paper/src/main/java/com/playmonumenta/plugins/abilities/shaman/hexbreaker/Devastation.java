package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.abilities.shaman.TotemicEmpowerment;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker.DevastationCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Devastation extends Ability {
	public static final int COOLDOWN = 15 * 20;
	public static final int RADIUS_1 = 6;
	public static final int RADIUS_2 = 8;
	public static final int DAMAGE_1 = 23;
	public static final int DAMAGE_2 = 28;
	public static final int CDR_ON_KILL = 3 * 20;

	public static final String CHARM_DAMAGE = "Devastation Damage";
	public static final String CHARM_RADIUS = "Devastation Radius";
	public static final String CHARM_COOLDOWN = "Devastation Cooldown";
	public static final String CHARM_CDR = "Devastation Cooldown Reduction";

	public static final AbilityInfo<Devastation> INFO =
		new AbilityInfo<>(Devastation.class, "Devastation", Devastation::new)
			.linkedSpell(ClassAbility.DEVASTATION)
			.scoreboardId("Devastation")
			.shorthandName("DV")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Punch with a bow to destroy your nearest totem, dealing massive damage within a medium radius.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Devastation::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.COAL_BLOCK);

	public double mDamage;
	private final double mRadius;
	private final int mCooldownReduction;
	private final DevastationCS mCosmetic;

	public Devastation(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, isLevelOne() ? RADIUS_1 : RADIUS_2);
		mCooldownReduction = CharmManager.getDuration(mPlayer, CHARM_CDR, CDR_ON_KILL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DevastationCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		List<LivingEntity> totemList = TotemicEmpowerment.getTotemList(mPlayer);
		if (totemList.isEmpty()) {
			return false;
		}

		Hitbox hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), 20, Math.toRadians(18))
			.union(Hitbox.approximateCone(mPlayer.getEyeLocation(), 3, Math.toRadians(30)));
		List<ArmorStand> totemsInHitbox = hitbox.getHitEntitiesByClass(ArmorStand.class);
		totemsInHitbox.removeIf(totem -> !totemList.contains(totem));
		LivingEntity totemToNuke;
		if (!totemsInHitbox.isEmpty()) {
			totemToNuke = totemsInHitbox.get(0);
			for (LivingEntity totem : totemsInHitbox) {
				if (!totemToNuke.equals(totem) && mPlayer.getLocation().distance(totemToNuke.getLocation()) > mPlayer.getLocation().distance(totem.getLocation())) {
					totemToNuke = totem;
				}
			}
		} else {
			totemToNuke = totemList.get(0);
			for (LivingEntity totem : totemList) {
				if (!totemToNuke.equals(totem) && mPlayer.getLocation().distance(totemToNuke.getLocation()) > mPlayer.getLocation().distance(totem.getLocation())) {
					totemToNuke = totem;
				}
			}
		}
		putOnCooldown();

		Location targetLoc = totemToNuke.getLocation();
		for (Ability abil : mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities()) {
			if (abil instanceof TotemAbility totemAbility
				&& totemAbility.getRemainingAbilityDuration() > 0
				&& totemAbility.mDisplayName.equalsIgnoreCase(totemToNuke.getName())) {
				ClassAbility linkedSpell = abil.getInfo().getLinkedSpell();
				if (linkedSpell != null) {
					mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, mCooldownReduction);
				}
			}
		}
		TotemicEmpowerment.removeTotem(mPlayer, totemToNuke);

		mCosmetic.devastationCast(mPlugin, targetLoc, mPlayer, mRadius);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(targetLoc, mRadius)) {
			DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
		}

		return true;
	}

	private static Description<Devastation> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to destroy the nearest totem, dealing ")
			.add(a -> a.mDamage, DAMAGE_1, false, Ability::isLevelOne)
			.add(" magic damage to mobs within ")
			.add(a -> a.mRadius, RADIUS_1, false, Ability::isLevelOne)
			.add(" blocks of the totem. The totem's cooldown is reduced by ")
			.addDuration(a -> a.mCooldownReduction, CDR_ON_KILL)
			.add(" seconds.")
			.addCooldown(COOLDOWN);
	}

	private static Description<Devastation> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mDamage, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" and the radius is increased to ")
			.add(a -> a.mRadius, RADIUS_2, false, Ability::isLevelTwo)
			.add(" blocks.");
	}
}
