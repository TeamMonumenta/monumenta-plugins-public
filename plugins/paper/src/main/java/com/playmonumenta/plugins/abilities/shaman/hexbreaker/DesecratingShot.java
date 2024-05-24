package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.hexbreaker.DesecratingShotCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import org.bukkit.*;
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
			.descriptions(
				String.format("Upon hitting a mob with a fully charge projectile, apply %s%% weakness for %ss and deal %s%% magic damage of the projectile's damage in a %s block radius. %ss cooldown.",
					StringUtils.multiplierToPercentage(WEAKNESS_1),
					StringUtils.ticksToSeconds(WEAKNESS_DURATION),
					StringUtils.multiplierToPercentage(DAMAGE_1),
					RADIUS,
					StringUtils.ticksToSeconds(COOLDOWN)
				),
				String.format("Damage is increased to %s%% of your bow shot and weaken increased to %s%%.",
					StringUtils.multiplierToPercentage(DAMAGE_2),
					StringUtils.multiplierToPercentage(WEAKNESS_2))
			)
			.simpleDescription("Deal extra magic damage to mobs hit with your projectiles and apply a weakness debuff to mobs within a short range.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.TNT);

	private double mDamagePercent;
	private final double mWeaknessPercent;
	private final int mDuration;
	private final double mRadius;
	private final DesecratingShotCS mCosmetic;

	public DesecratingShot(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamagePercent *= DestructiveExpertise.damageBuff(mPlayer);
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
			for (LivingEntity mob : affectedMobs) {
				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, event.getDamage() * mDamagePercent, ClassAbility.DESECRATING_SHOT, false, true);
				EntityUtils.applyWeaken(mPlugin, mDuration, mWeaknessPercent, mob);
				mCosmetic.desecratingShotEffect(mPlayer, mob, mPlugin);
			}
		}
		return true;
	}
}
