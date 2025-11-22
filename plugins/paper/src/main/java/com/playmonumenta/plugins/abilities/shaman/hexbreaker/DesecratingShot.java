package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker.DesecratingShotCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import java.util.List;
import java.util.NavigableSet;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

public class DesecratingShot extends Ability {
	private static final int COOLDOWN = 4 * 20;
	private static final double DAMAGE_1 = 0.4;
	private static final double DAMAGE_2 = 0.6;
	private static final double WEAKNESS_1 = 0.15;
	private static final double WEAKNESS_2 = 0.3;
	private static final int WEAKNESS_DURATION = 6 * 20;
	private static final int RADIUS = 4;

	public static final String CHARM_COOLDOWN = "Desecrating Shot Cooldown";
	public static final String CHARM_DAMAGE = "Desecrating Shot Damage";
	public static final String CHARM_WEAKNESS = "Desecrating Shot Weakness Amplifier";
	public static final String CHARM_DURATION = "Desecrating Shot Duration";
	public static final String CHARM_RADIUS = "Desecrating Shot Radius";

	public static final AbilityInfo<DesecratingShot> INFO =
		new AbilityInfo<>(DesecratingShot.class, "Desecrating Shot", DesecratingShot::new)
			.linkedSpell(ClassAbility.DESECRATING_SHOT)
			.scoreboardId("DesecratingShot")
			.shorthandName("DS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Deal extra magic damage to mobs hit with your projectiles and apply a weakness debuff to mobs within a short range.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.TNT);

	private final double mDamagePercent;
	private final double mWeaknessPercent;
	private final int mDuration;
	private final double mRadius;
	private final DesecratingShotCS mCosmetic;

	public DesecratingShot(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mWeaknessPercent = (isLevelOne() ? WEAKNESS_1 : WEAKNESS_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKNESS);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, WEAKNESS_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DesecratingShotCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!isOnCooldown() && event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
			putOnCooldown();

			mCosmetic.desecratingShotTrigger(mPlayer, enemy);
			Location loc = enemy.getLocation();

			List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobs(loc, mRadius);

			ItemStatManager.PlayerItemStats projectileItemStats = DamageListener.getProjectileItemStats(projectile);
			if (projectileItemStats == null) { // should hopefully never be null but just avoid the null check error :)
				return false;
			}
			double projDamageAdd = projectileItemStats.getMainhandAddStats().get(AttributeType.PROJECTILE_DAMAGE_ADD);

			for (LivingEntity mob : affectedMobs) {
				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, projDamageAdd * mDamagePercent, ClassAbility.DESECRATING_SHOT, false, true);
				EntityUtils.applyWeaken(mPlugin, mDuration, mWeaknessPercent, mob);
				mCosmetic.desecratingShotEffect(mPlayer, mob, mPlugin);
			}
		}
		if (event.getAbility() == ClassAbility.DESECRATING_SHOT) {
			//Check if totemic projection u is active as otherwise ordering doesn't work.
			NavigableSet<PercentDamageDealt> effects = mPlugin.mEffectManager.getEffects(mPlayer, PercentDamageDealt.class);
			for (PercentDamageDealt effect : effects) {
				EnumSet<DamageType> types = effect.getAffectedDamageTypes();
				if (types == null || types.contains(DamageType.PROJECTILE)) {
					double magnitude = 1 + effect.getMagnitude();
					event.updateDamageWithMultiplier(magnitude);
				}
			}
		}
		return false;
	}

	private static Description<DesecratingShot> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Shooting a mob with a critical projectile deals ")
			.addPercent(a -> a.mDamagePercent, DAMAGE_1, false, Ability::isLevelOne)
			.add(" of the projectile damage as magic damage to mobs within ")
			.add(a -> a.mRadius, RADIUS)
			.add(" blocks and applies ")
			.addPercent(a -> a.mWeaknessPercent, WEAKNESS_1, false, Ability::isLevelOne)
			.add(" weaken for ")
			.addDuration(a -> a.mDuration, WEAKNESS_DURATION)
			.add(" seconds to them.")
			.addCooldown(COOLDOWN);
	}

	private static Description<DesecratingShot> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.addPercent(a -> a.mDamagePercent, DAMAGE_2, false, Ability::isLevelTwo)
			.add(" of the projectile damage and weaken is increased to ")
			.addPercent(a -> a.mWeaknessPercent, WEAKNESS_2, false, Ability::isLevelTwo)
			.add(".");
	}
}
