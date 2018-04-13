package pe.project.classes;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.EntityUtils;
import pe.project.utils.ItemUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.PotionUtils;
import pe.project.utils.PotionUtils.PotionInfo;
import pe.project.utils.ScoreboardUtils;

/*
	GruesomeAlchemy
	PutridFumes
	CausticMixture
	BasiliskPoison
	PowerInjection
	InvigoratingOdor
PoisonTrail

*/

public class AlchemistClass extends BaseClass {
	private static final double GRUESOME_ALCHEMY_CHANCE = 0.25f;
	private static final int GRUESOME_ALCHEMY_1_STACK_SIZE = 16;
	private static final int GRUESOME_ALCHEMY_2_STACK_SIZE = 32;
	private static final int GRUESOME_ALCHEMY_COOLDOWN = 3 * 60 * 20;

	private static final String PUTRID_FUMES_1_TAG = "PutridFumes1";
	private static final String PUTRID_FUMES_2_TAG = "PutridFumes2";
	private static final float PUTRID_FUMES_1_RADIUS = 3;
	private static final float PUTRID_FUMES_2_RADIUS = 5;
	private static final int PUTRID_FUMES_DURATION = 15 * 20;
	private static final int PUTRID_FUMES_COOLDOWN = 10 * 20;

	private static final int CAUSTIC_MIXTURE_1_DAMAGE = 6;
	private static final int CAUSTIC_MIXTURE_2_DAMAGE = 12;
	private static final String CAUSTIC_MIXTURE_TAG = "CausticMixture";

	private static final int BASILISK_POISON_1_EFFECT_LVL = 0;
	private static final int BASILISK_POISON_2_EFFECT_LVL = 1;
	private static final int BASILISK_POISON_1_DURATION = 15 * 20;
	private static final int BASILISK_POISON_2_DURATION = 12 * 20;

	private static final int POWER_INJECTION_RANGE = 8;
	private static final int POWER_INJECTION_1_STRENGTH_EFFECT_LVL = 1;
	private static final int POWER_INJECTION_2_STRENGTH_EFFECT_LVL = 2;
	private static final int POWER_INJECTION_SPEED_EFFECT_LVL = 0;
	private static final int POWER_INJECTION_DURATION = 20 * 20;
	private static final int POWER_INJECTION_COOLDOWN = 30 * 20;

	private static final int INVIGORATING_ODOR_RESISTANCE_EFFECT_LVL = 0;
	private static final int INVIGORATING_ODOR_SPEED_EFFECT_LVL = 0;
	private static final int INVIGORATING_ODOR_REGENERATION_EFFECT_LVL = 0;
	private static final int INVIGORATING_ODOR_1_DURATION = 12 * 20;
	private static final int INVIGORATING_ODOR_2_DURATION = 15 * 20;


	//	POISON_TRAIL

	public AlchemistClass(Plugin plugin, Random random) {
		super(plugin, random);
	}


	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity, DamageCause cause, boolean shouldGenDrops) {
		//	GruesomeAlchemy
		if (shouldGenDrops) {
			int gruesomeAlchemy = ScoreboardUtils.getScoreboardValue(player, "GruesomeAlchemy");
			if (gruesomeAlchemy > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.GRUESOME_ALCHEMY)) {
					if (mRandom.nextFloat() < GRUESOME_ALCHEMY_CHANCE) {
						int count = gruesomeAlchemy == 1 ? GRUESOME_ALCHEMY_1_STACK_SIZE : GRUESOME_ALCHEMY_2_STACK_SIZE;
						ItemStack stack = new ItemStack(Material.SPLASH_POTION, count);

						int rand = mRandom.nextInt(4);
						if (rand == 0) {
							ItemUtils.setPotionMeta(stack, "Splash Potion of Harming", PotionEffectType.INCREASE_DAMAGE.getColor());
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.HARM, 0, 1, false, true));
						} else if (rand == 1) {
							ItemUtils.setPotionMeta(stack, "Potion of Decay", Color.fromRGB(6178631));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.SLOW, 32 * 20, 0, false, true));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.WEAKNESS, 32 * 20, 0, false, true));
						} else if (rand == 2) {
							ItemUtils.setPotionMeta(stack, "Scorpion Venom", Color.fromRGB(4610355));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.WITHER, 18 * 20, 1, false, true));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.POISON, 18 * 20, 0, false, true));
						} else {
							ItemUtils.setPotionMeta(stack, "Lesser Frost Bomb", Color.fromRGB(3294553));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.SLOW, 20 * 20, 1, false, true));
							ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.WITHER, 20 * 20, 0, false, true));
						}

						World world = Bukkit.getWorld(player.getWorld().getName());
						world.dropItemNaturally(killedEntity.getLocation(), stack);

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.GRUESOME_ALCHEMY, GRUESOME_ALCHEMY_COOLDOWN);
					}
				}
			}
		}
	}

	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		//	BasiliskPoison
		{
			int basiliskPoison = ScoreboardUtils.getScoreboardValue(player, "BasiliskPoison");
			if (basiliskPoison > 0) {
				int effectLvl = basiliskPoison == 1 ? BASILISK_POISON_1_EFFECT_LVL : BASILISK_POISON_2_EFFECT_LVL;
				int duration = basiliskPoison == 1 ? BASILISK_POISON_1_DURATION : BASILISK_POISON_2_DURATION;
				damagee.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, effectLvl, false, true));
			}
		}
	}

	@Override
	public void PlayerShotArrowEvent(Player player, Arrow arrow) {
		//	PowerInjection
		{
			if (arrow.isCritical() && player.isSneaking()) {
				int powerInjection = ScoreboardUtils.getScoreboardValue(player, "PowerInjection");
				if (powerInjection > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.POWER_INJECTION)) {
						LivingEntity targetEntity = EntityUtils.GetEntityAtCursor(player, POWER_INJECTION_RANGE, true, true, true);
						if (targetEntity != null && targetEntity instanceof Player) {
							int effectLvl = powerInjection == 1 ? POWER_INJECTION_1_STRENGTH_EFFECT_LVL : POWER_INJECTION_2_STRENGTH_EFFECT_LVL;

							mPlugin.mPotionManager.addPotion((Player)targetEntity, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, POWER_INJECTION_DURATION, effectLvl, false, true));

							if (powerInjection > 1) {
								mPlugin.mPotionManager.addPotion((Player)targetEntity, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, POWER_INJECTION_DURATION, POWER_INJECTION_SPEED_EFFECT_LVL, false, true));
							}

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.POWER_INJECTION, POWER_INJECTION_COOLDOWN);

							arrow.remove();
							return;
						}
					}
				}
			}
		}

		//	BasiliskPoison
		{
			int basiliskPoison = ScoreboardUtils.getScoreboardValue(player, "BasiliskPoison");
			if (basiliskPoison > 0) {
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.TOTEM);
			}
		}
	}

	@Override
	public void PlayerThrewSplashPotionEvent(Player player, SplashPotion potion) {
		if (player.isSneaking()) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.PUTRID_FUMES)) {
				if (!PotionUtils.hasPositiveEffects(potion.getEffects())) {
					int putridFumes = ScoreboardUtils.getScoreboardValue(player, "PutridFumes");
					if (putridFumes > 0) {
						String meta = putridFumes == 1 ? PUTRID_FUMES_1_TAG : PUTRID_FUMES_2_TAG;
						potion.setMetadata(meta, new FixedMetadataValue(mPlugin, 0));

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.PUTRID_FUMES, PUTRID_FUMES_COOLDOWN);
					}
				}
			}
		}
	}

	@Override
	public boolean PlayerSplashPotionEvent(Player player, Collection<LivingEntity> affectedEntities,
										   ThrownPotion potion, PotionSplashEvent event) {
		//  All affected players need to have the effect added to their potion manager.
		for (LivingEntity entity : affectedEntities) {
			if (entity instanceof Player) {
				// Thrown by a player - negative effects are not allowed for other players
				// Special for alchemist - don't apply negative effects to self either
				if (PotionUtils.hasNegativeEffects(potion.getEffects())) {
					// If a thrown potion contains any negative effects, don't apply *any* effects to other players
					event.setIntensity(entity, 0);
				}

				mPlugin.mPotionManager.addPotion((Player)entity, PotionID.APPLIED_POTION, potion.getEffects(),
												 event.getIntensity(entity));
			}
		}

		if (potion.hasMetadata(PUTRID_FUMES_1_TAG)) {
			AreaEffectCloud cloud = EntityUtils.spawnAreaEffectCloud(player.getWorld(), potion.getLocation(), potion.getEffects(), PUTRID_FUMES_1_RADIUS, PUTRID_FUMES_DURATION);
			cloud.setSource(player);
		} else if (potion.hasMetadata(PUTRID_FUMES_2_TAG)) {
			AreaEffectCloud cloud = EntityUtils.spawnAreaEffectCloud(player.getWorld(), potion.getLocation(), potion.getEffects(), PUTRID_FUMES_2_RADIUS, PUTRID_FUMES_DURATION);
			cloud.setSource(player);
		}

		World world = player.getWorld();
		int causticMixture = ScoreboardUtils.getScoreboardValue(player, "CausticMixture");
		int invigoratingOdor = ScoreboardUtils.getScoreboardValue(player, "InvigoratingOdor");

		boolean hitMonster = false;

		if (affectedEntities != null) {
			for (LivingEntity entity : affectedEntities) {
				if (EntityUtils.isHostileMob(entity)) {
					//	Caustic Mixture

					if (causticMixture > 0) {
						int damage = causticMixture == 1 ? CAUSTIC_MIXTURE_1_DAMAGE : CAUSTIC_MIXTURE_2_DAMAGE;
						if (!entity.hasMetadata(CAUSTIC_MIXTURE_TAG)) {
							EntityUtils.damageEntity(mPlugin, entity, damage, player);
							entity.setMetadata(CAUSTIC_MIXTURE_TAG, new FixedMetadataValue(mPlugin, 0));

							Location loc = entity.getLocation();
							ParticleUtils.playParticlesInWorld(world, Particle.TOTEM, loc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.001);
						}
					}

					//	Invigorating Odor
					if (invigoratingOdor > 0 && !hitMonster) {
						int duration = (invigoratingOdor == 1) ? INVIGORATING_ODOR_1_DURATION : INVIGORATING_ODOR_2_DURATION;

						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SPEED, duration, INVIGORATING_ODOR_SPEED_EFFECT_LVL, true, false));
						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, INVIGORATING_ODOR_RESISTANCE_EFFECT_LVL, true, false));

						if (invigoratingOdor > 1) {
							mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.REGENERATION, duration, INVIGORATING_ODOR_REGENERATION_EFFECT_LVL, true, false));
						}
					}

					hitMonster = true;
				}
			}
		}

		return true;
	}

	@Override
	public void AreaEffectCloudApplyEvent(Collection<LivingEntity> entities, Player player) {
		int causticMixture = ScoreboardUtils.getScoreboardValue(player, "CausticMixture");
		if (causticMixture > 0) {
			World world = player.getWorld();

			int damage = causticMixture == 1 ? CAUSTIC_MIXTURE_1_DAMAGE : CAUSTIC_MIXTURE_2_DAMAGE;
			for (LivingEntity entity : entities) {
				if (EntityUtils.isHostileMob(entity)) {
					if (!entity.hasMetadata(CAUSTIC_MIXTURE_TAG)) {
						EntityUtils.damageEntity(mPlugin, entity, damage, player);
						entity.setMetadata(CAUSTIC_MIXTURE_TAG, new FixedMetadataValue(mPlugin, 0));

						Location loc = entity.getLocation();
						ParticleUtils.playParticlesInWorld(world, Particle.TOTEM, loc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.001);
					}
				}
			}
		}
	}
}
