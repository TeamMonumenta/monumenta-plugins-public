package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.ImpactVulnerability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Impact implements Enchantment {

	private static final Particle.DustOptions DUST_OPTIONS = new Particle.DustOptions(Color.WHITE, 1.2f);
	private static final String EFFECT_ID = "ImpactVulnerability";

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.IMPACT;
	}

	@Override
	public String getName() {
		return "Impact";
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {

		if (event.getType() != DamageEvent.DamageType.MELEE || player.getCooledAttackStrength(0) <= 0.9) {
			return;
		}

		double damage = event.getFinalDamage(true);
		Vector kbDirection = player.getEyeLocation().getDirection();


		List<LivingEntity> targetStack = new ArrayList<>();

		EntityUtils.getStackedMobsAbove(enemy, targetStack);
		if (enemy.getVehicle() != null) {
			EntityUtils.getStackedMobsBelow(enemy.getVehicle(), targetStack);
		}

		for (LivingEntity target : targetStack) {
			plugin.mEffectManager.addEffect(target, EFFECT_ID, new ImpactVulnerability(40));
		}

		new BukkitRunnable() {

			double mFallDistanceLastTick = EntityUtils.getEntityStackBase(enemy).getFallDistance();

			@Override
			public void run() {

				LivingEntity primaryTarget = enemy;

				if (primaryTarget.isDead()) {

					for (LivingEntity target : targetStack) {
						if (!target.isDead()) {
							primaryTarget = target;
						}
					}
				}

				Effect impactEffect = plugin.mEffectManager.getActiveEffect(primaryTarget, EFFECT_ID);


				if (impactEffect == null || impactEffect.getDuration() <= 0) {
					this.cancel();
					return;
				}

				if (checkForImpact(mFallDistanceLastTick, kbDirection, primaryTarget, targetStack)) {

					onImpact(player, primaryTarget, targetStack, damage, (int) value);

					for (LivingEntity target : targetStack) {
						plugin.mEffectManager.clearEffects(target, EFFECT_ID);
					}

					this.cancel();
				}

				mFallDistanceLastTick = EntityUtils.getEntityStackBase(primaryTarget).getFallDistance();

				double widthDelta = PartialParticle.getWidthDelta(enemy);
				double heightDelta = PartialParticle.getHeightDelta(enemy);

				new PartialParticle(
					Particle.REDSTONE,
					LocationUtils.getHeightLocation(enemy, 0.6),
					4,
					widthDelta,
					heightDelta / 2,
					widthDelta,
					DUST_OPTIONS
				).spawnAsEnemy();


			}

		}.runTaskTimer(plugin, 0, 2);

	}

	private boolean checkForImpact(double fallDistanceLastTick, Vector direction, LivingEntity primaryTarget, List<LivingEntity> targetStack) {

		Entity bottomEntity = EntityUtils.getEntityStackBase(targetStack.get(0));

		if (fallDistanceLastTick > 2.8 && bottomEntity.isOnGround()) {

			new PartialParticle(
				Particle.EXPLOSION_LARGE,
				LocationUtils.getHeightLocation(bottomEntity, 0.1),
				1
			).spawnAsEnemy();

			return true;
		}


		BoundingBox offsetBox = primaryTarget.getBoundingBox().clone();
		offsetBox.shift(direction.getX(), 0.15, direction.getZ());
		offsetBox.expand(-0.05, -0.4, -0.05);

		if (LocationUtils.collidesWithBlocks(offsetBox, primaryTarget.getWorld())) {

			new PartialParticle(
				Particle.EXPLOSION_LARGE,
				LocationUtils.getHeightLocation(primaryTarget, 0.65),
				1
			).spawnAsEnemy();

			return true;
		}


		return false;
	}

	private void onImpact(Player player, LivingEntity primaryTarget, List<LivingEntity> targets, double originalDamage, int level) {

		double finalDamage = originalDamage * 0.1 * level;

		for (LivingEntity e : targets) {
			DamageUtils.damage(player, e, DamageEvent.DamageType.MELEE_ENCH, finalDamage, null, true);
		}

		World world = primaryTarget.getWorld();
		world.playSound(primaryTarget.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.5f, 1.35f);
		world.playSound(primaryTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(primaryTarget.getLocation(), Sound.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 1.7f, 0.5f);
		world.playSound(primaryTarget.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.7f, 0.5f);

	}

}
