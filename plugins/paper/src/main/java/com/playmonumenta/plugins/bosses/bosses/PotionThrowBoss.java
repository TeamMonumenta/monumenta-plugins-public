package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PotionThrowBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_potion_throw";
	private static final String potionTag = "ThrowPotionBossTag";


	public static class Parameters extends BossParameters {
		public int COOLDOWN = (int) (20 * 1.5);

		public Color COLOR = Color.BLACK;

		public int DAMAGE = 0;
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;
		@BossParam(help = "not written")
		public double DAMAGE_PERCENTAGE = 0.0;
		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		public EffectsList EFFECTS_ENEMY = EffectsList.EMPTY;
		public EffectsList EFFECTS_ALLY = EffectsList.EMPTY;

		public EntityTargets THROWING_TARGET = EntityTargets.GENERIC_ONE_PLAYER_TARGET.clone().setRange(15);
		public EntityTargets ENEMIES_TARGET = EntityTargets.GENERIC_PLAYER_TARGET.clone().setRange(3);
		public EntityTargets ALLY_TARGET = EntityTargets.GENERIC_MOB_TARGET.clone().setRange(3);

		public float MIN_POTION_SPEED = 0;
		public float MAX_POTION_SPEED = 0.75f;

		//Particle & Sounds!
		@BossParam(help = "Particle that follow the potion")
		public ParticlesList PARTICLE_TRAIL = ParticlesList.fromString("[(FIREWORKS_SPARK,1)]");

		public ParticlesList PARTICLE_ENEMY = ParticlesList.fromString("[(SPELL,1)]");
		public ParticlesList PARTICLE_ALLY = ParticlesList.fromString("[(SPELL,1)]");

		@BossParam(help = "Sound played when the boss launch a potion")
		public SoundsList SOUND = SoundsList.fromString("[(ENTITY_SPLASH_POTION_THROW,3,0.7)]");

	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PotionThrowBoss(plugin, boss);
	}

	private final Parameters mParams;

	public PotionThrowBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mParams = Parameters.getParameters(boss, identityTag, new Parameters());
		SpellManager spell = new SpellManager(List.of(
			new Spell() {
				@Override
				public void run() {
					World world = mBoss.getWorld();
					Location bossLocation = mBoss.getLocation();
					boolean first = true;
					for (LivingEntity target : mParams.THROWING_TARGET.getTargetsList(mBoss)) {
						if (first) {
							mParams.SOUND.play(mBoss.getEyeLocation());
							first = false;
						}
						Vector targetVec = target.getLocation().toVector();
						Vector targetSpeedVec = target.getVelocity();
						double potVelocityX = targetVec.getX() + targetSpeedVec.getX() - bossLocation.getX();
						double potVelocityY = targetVec.getY() + target.getEyeHeight() - 1.2 - bossLocation.getY();
						double potVelocityZ = targetVec.getZ() + targetSpeedVec.getZ() - bossLocation.getZ();
						double distance = Math.sqrt(potVelocityX * potVelocityX + potVelocityZ * potVelocityZ);

						Entity pot = world.spawnEntity(mBoss.getEyeLocation(), EntityType.SPLASH_POTION);
						if (pot instanceof ThrownPotion tp) {
							ItemStack stack = new ItemStack(Material.SPLASH_POTION);
							ItemMeta meta = stack.getItemMeta();
							if (meta instanceof PotionMeta ptMeta) {
								ptMeta.setColor(mParams.COLOR);
							}
							stack.setItemMeta(meta);
							tp.setItem(stack);
							tp.setShooter(mBoss);
							double distanceSpeed = distance * 0.1;
							Vector potVelocity = new Vector(potVelocityX, potVelocityY + distanceSpeed, potVelocityZ);
							double potSpeed = Math.max(Math.min(mParams.MAX_POTION_SPEED, distanceSpeed), mParams.MIN_POTION_SPEED);
							tp.setVelocity(potVelocity.normalize().multiply(potSpeed));
							tp.setMetadata(potionTag, new FixedMetadataValue(mPlugin, null));
							new BukkitRunnable() {
								@Override
								public void run() {
									if (!tp.isValid() || tp.isDead()) {
										cancel();
										return;
									}
									mParams.PARTICLE_TRAIL.spawn(boss, tp.getLocation());
								}
							}.runTaskTimer(mPlugin, 0, 1);
						}
					}
				}

				@Override
				public int cooldownTicks() {
					return mParams.COOLDOWN;
				}

				@Override
				public boolean canRun() {
					return !mParams.THROWING_TARGET.getTargetsList(mBoss).isEmpty();
				}
			}
		));

		super.constructBoss(spell, Collections.emptyList(), (int) (mParams.THROWING_TARGET.getRange() * 2), null);
	}


	@Override
	public void bossSplashPotion(PotionSplashEvent event) {
		if (!event.getPotion().hasMetadata(potionTag) || event.isCancelled()) {
			return;
		}
		event.setCancelled(true);
		Location loc = event.getPotion().getLocation();
		event.getPotion().remove();
		if (mParams.ENEMIES_TARGET.getRange() > 0) {
			//aesthetic action
			if (!mParams.PARTICLE_ENEMY.isEmpty()) {
				new BukkitRunnable() {
					double mCurrentRadius = mParams.ENEMIES_TARGET.getRange() / 10;
					final double mRadiusIncrease = mCurrentRadius;

					@Override
					public void run() {
						for (int i = 0; i < 360; i += 15) {
							double radian1 = Math.toRadians(FastUtils.randomDoubleInRange(-i, i) + i);
							Location newLoc = loc.clone().add(FastUtils.cos(radian1) * mCurrentRadius, 0.2 + mCurrentRadius / 2, FastUtils.sin(radian1) * mCurrentRadius);
							mParams.PARTICLE_ENEMY.spawn(mBoss, newLoc);
						}

						mCurrentRadius += mRadiusIncrease;

						if (mCurrentRadius >= mParams.ENEMIES_TARGET.getRange()) {
							cancel();
						}

					}
				}.runTaskTimer(mPlugin, 0, 1);
			}

			//damage action
			for (LivingEntity enemy : mParams.ENEMIES_TARGET.getTargetsListByLocation(mBoss, loc)) {
				if (mParams.DAMAGE > 0) {
					BossUtils.blockableDamage(mBoss, enemy, mParams.DAMAGE_TYPE, mParams.DAMAGE, mParams.SPELL_NAME, mBoss.getLocation());
				}

				if (mParams.DAMAGE_PERCENTAGE > 0.0) {
					BossUtils.bossDamagePercent(mBoss, enemy, mParams.DAMAGE_PERCENTAGE, mParams.SPELL_NAME);
				}
				mParams.EFFECTS_ENEMY.apply(enemy, mBoss);
			}
		}

		if (mParams.ALLY_TARGET.getRange() > 0) {
			//aesthetic action
			if (!mParams.PARTICLE_ALLY.isEmpty()) {
				new BukkitRunnable() {
					double mCurrentRadius = mParams.ALLY_TARGET.getRange() / 10;
					final double mRadiusIncrease = mCurrentRadius;

					@Override
					public void run() {
						for (int i = 0; i < 360; i += 15) {
							double radian1 = Math.toRadians(FastUtils.randomDoubleInRange(-i, i) + i);
							Location newLoc = loc.clone().add(FastUtils.cos(radian1) * mCurrentRadius, 0.2 + mCurrentRadius / 2, FastUtils.sin(radian1) * mCurrentRadius);
							mParams.PARTICLE_ENEMY.spawn(mBoss, newLoc);
						}

						mCurrentRadius += mRadiusIncrease;

						if (mCurrentRadius >= mParams.ENEMIES_TARGET.getRange()) {
							cancel();
						}

					}
				}.runTaskTimer(mPlugin, 0, 1);
			}

			for (LivingEntity allay : mParams.ALLY_TARGET.getTargetsListByLocation(mBoss, loc)) {
				mParams.EFFECTS_ALLY.apply(allay, mBoss);
			}
		}


	}
}
