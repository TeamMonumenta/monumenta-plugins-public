package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;

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
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!isOnCooldown() && event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
			putOnCooldown();

			World world = enemy.getWorld();
			Location loc = enemy.getLocation();
			world.playSound(loc, Sound.ENTITY_VEX_DEATH,
				SoundCategory.PLAYERS, 2.0f, 0.4f);
			world.playSound(loc, Sound.ENTITY_ARROW_HIT,
				SoundCategory.PLAYERS, 2.0f, 0.6f);
			world.playSound(loc, Sound.ENTITY_ARROW_HIT,
				SoundCategory.PLAYERS, 2.0f, 1.4f);
			world.playSound(loc, Sound.ENTITY_ARROW_HIT,
				SoundCategory.PLAYERS, 2.0f, 1.8f);
			world.playSound(loc, Sound.ITEM_TRIDENT_THROW,
				SoundCategory.PLAYERS, 2.0f, 0.0f);
			world.playSound(loc, Sound.ENTITY_VEX_HURT,
				SoundCategory.PLAYERS, 2.0f, 1.2f);
			world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH,
				SoundCategory.PLAYERS, 0.7f, 0.1f);

			List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobs(loc, mRadius);
			for (LivingEntity mob : affectedMobs) {
				DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, event.getDamage() * mDamagePercent, ClassAbility.DESECRATING_SHOT, false, true);
				EntityUtils.applyWeaken(mPlugin, mDuration, mWeaknessPercent, mob);
				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {

						PPCircle lowerRing = new PPCircle(Particle.SPELL_WITCH, mob.getLocation().clone().add(0, 0.5, 0), 1).count(10).delta(0).extra(0.03);
						lowerRing.spawnAsPlayerActive(mPlayer);

						if (mTicks >= 4 * 20 || mob.isDead()) {
							this.cancel();
						}

						mTicks += 5;
					}
				}.runTaskTimer(mPlugin, 0, 5);
			}
		}
		return true;
	}
}
