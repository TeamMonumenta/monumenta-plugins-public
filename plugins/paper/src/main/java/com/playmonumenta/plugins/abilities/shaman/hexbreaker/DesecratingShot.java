package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;

public class DesecratingShot extends Ability {
	private static final int COOLDOWN = 6 * 20;
	private static final double DAMAGE_1 = 0.3;
	private static final double DAMAGE_2 = 0.5;
	private static final double WEAKNESS_1 = 0.15;
	private static final double WEAKNESS_2 = 0.3;
	private static final int WEAKNESS_DURATION = 6 * 20;
	private static final int RADIUS = 3;

	private double mDamagePercent;
	private final double mWeaknessPercent;


	public static final AbilityInfo<DesecratingShot> INFO =
		new AbilityInfo<>(DesecratingShot.class, "Desecrating Shot", DesecratingShot::new)
			.linkedSpell(ClassAbility.DESECRATING_SHOT)
			.scoreboardId("DesecratingShot")
			.shorthandName("DS")
			.descriptions(
				String.format("Your projectiles now deal an extra %s%% of your projectile damage as magic damage to the target, and apply %s%% weaken in a %s block radius for %ss. %ss cooldown.",
					(int) (DAMAGE_1 * 100),
					(int) (WEAKNESS_1 * 100),
					RADIUS,
					WEAKNESS_DURATION / 20,
					COOLDOWN / 20
				),
				String.format("Damage is increased to %s%% of your bow shot and weaken increased to %s%%.",
					(int) (DAMAGE_2 * 100),
					(int) (WEAKNESS_2 * 100))
			)
			.simpleDescription("Deal extra magic damage to mobs hit with your projectiles and apply a weakness debuff to mobs within a short range.")
			.cooldown(COOLDOWN)
			.displayItem(Material.TNT);

	public DesecratingShot(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mDamagePercent = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mWeaknessPercent = isLevelOne() ? WEAKNESS_1 : WEAKNESS_2;
		mDamagePercent *= HexbreakerPassive.damageBuff(mPlayer);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (!isOnCooldown() && event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile projectile && EntityUtils.isAbilityTriggeringProjectile(projectile, true)) {
			putOnCooldown();
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, event.getDamage() * mDamagePercent, ClassAbility.DESECRATING_SHOT, false, true);
			enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_VEX_DEATH, 2.0f, 0.4f);
			enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_ARROW_HIT, 2.0f, 0.6f);
			enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_ARROW_HIT, 2.0f, 1.4f);
			enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_ARROW_HIT, 2.0f, 1.8f);
			enemy.getWorld().playSound(enemy.getLocation(), Sound.ITEM_TRIDENT_THROW, 2.0f, 0.0f);
			enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_VEX_HURT, 2.0f, 1.2f);
			enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_PHANTOM_DEATH, 0.7f, 0.1f);

			Set<LivingEntity> affectedMobs = new HashSet<>(EntityUtils.getNearbyMobs(enemy.getLocation(), RADIUS));
			for (LivingEntity mob : affectedMobs) {
				EntityUtils.applyWeaken(mPlugin, WEAKNESS_DURATION, mWeaknessPercent, mob);
				new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {

						PPCircle lowerRing = new PPCircle(Particle.SPELL_WITCH, mob.getLocation().clone().add(0, 0.5, 0), 1).ringMode(true).count(10).delta(0).extra(0.03);
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
