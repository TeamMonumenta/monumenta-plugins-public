package pe.project.classes.Classes;

import java.util.Collection;
import java.util.List;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.classes.Main;
import pe.project.classes.Utils.EntityUtil;
import pe.project.classes.Utils.InventoryUtil;
import pe.project.classes.Utils.ItemUtils;
import pe.project.classes.Utils.ParticleUtil;
import pe.project.classes.Utils.PotionUtil.PotionInfo;
import pe.project.classes.Utils.ScoreboardUtil;

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
	private static double GRUESOME_ALCHEMY_CHANCE = 0.05f;
	private static int GRUESOME_ALCHEMY_1_STACK_SIZE = 16;
	private static int GRUESOME_ALCHEMY_2_STACK_SIZE = 32;
	
	private static String PUTRID_FUMES_1_TAG = "PutridFumes1";
	private static String PUTRID_FUMES_2_TAG = "PutridFumes2";
	private static float PUTRID_FUMES_1_RADIUS = 3;
	private static float PUTRID_FUMES_2_RADIUS = 5;
	private static int PUTRID_FUMES_DURATION = 15 * 20;
	
	private static int CAUSTIC_MIXTURE_1_DAMAGE = 6;
	private static int CAUSTIC_MIXTURE_2_DAMAGE = 12;
	private static String CAUSTIC_MIXTURE_TAG = "CausticMixture";
	
	private static int BASILISK_POISON_1_EFFECT_LVL = 0;
	private static int BASILISK_POISON_2_EFFECT_LVL = 1;
	private static int BASILISK_POISON_1_DURATION = 15 * 20;
	private static int BASILISK_POISON_2_DURATION = 12 * 20;
	
	private static int POWER_INJECTION_ID = 55;
	private static int POWER_INJECTION_RANGE = 8;
	private static int POWER_INJECTION_1_STRENGTH_EFFECT_LVL = 1;
	private static int POWER_INJECTION_2_STRENGTH_EFFECT_LVL = 2;
	private static int POWER_INJECTION_SPEED_EFFECT_LVL = 0;
	private static int POWER_INJECTION_DURATION = 20 * 20;
	private static int POWER_INJECTION_COOLDOWN = 30 * 20;
	
	private static int INVIGORATING_ODOR_RESISTENCE_EFFECT_LVL = 1;
	private static int INVIGORATING_ODOR_SPEED_EFFECT_LVL = 0;
	private static int INVIGORATING_ODOR_REGENERATION_EFFECT_LVL = 0;
	private static int INVIGORATING_ODOR_JUMP_EFFECT_LVL = 2;
	
	//	POISON_TRAIL
	
	public AlchemistClass(Main plugin, Random random) {
		super(plugin, random);
	}
	
	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity) {
		//	GruesomeAlchemy
		{
			int gruesomeAlchemy = ScoreboardUtil.getScoreboardValue(player, "GruesomeAlchemy");
			if (gruesomeAlchemy > 0) {
				if (mRandom.nextFloat() < GRUESOME_ALCHEMY_CHANCE) {
					int count = gruesomeAlchemy == 1 ? GRUESOME_ALCHEMY_1_STACK_SIZE : GRUESOME_ALCHEMY_2_STACK_SIZE;
					ItemStack stack = new ItemStack(Material.SPLASH_POTION, count);
					
					int rand = mRandom.nextInt(4);
					if (rand == 0) {
						ItemUtils.setPotionMeta(stack, "Splash Potion of Harming", PotionEffectType.INCREASE_DAMAGE.getColor());
						ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.HARM, 0, 1));
					} else if (rand == 1) {
						ItemUtils.setPotionMeta(stack, "Potion of Decay", Color.fromRGB(6178631));
						ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.SLOW, 32 * 20, 0));
						ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.WEAKNESS, 32 * 20, 0));
					} else if (rand == 2) {
						ItemUtils.setPotionMeta(stack, "Scorpion Venom", Color.fromRGB(4610355));
						ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.WITHER, 18 * 20, 1));
						ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.POISON, 18 * 20, 0));
					} else {
						ItemUtils.setPotionMeta(stack, "Lesser Frost Bomb", Color.fromRGB(3294553));
						ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.SLOW, 20 * 20, 1));
						ItemUtils.addPotionEffect(stack, new PotionInfo(PotionEffectType.WITHER, 20 * 20, 0));
					}
					
					World world = Bukkit.getWorld(player.getWorld().getName());
					world.dropItemNaturally(killedEntity.getLocation(), stack);
				}
			}
		}
	}
	
	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		//	BasiliskPoison
		{
			int basiliskPoison = ScoreboardUtil.getScoreboardValue(player, "BasiliskPoison");
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
				int powerInjection = ScoreboardUtil.getScoreboardValue(player, "PowerInjection");
				if (powerInjection > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), POWER_INJECTION_ID)) {
						LivingEntity targetEntity = EntityUtil.GetEntityAtCursor(player, POWER_INJECTION_RANGE, true, true, true);
						if (targetEntity != null && targetEntity instanceof Player) {
							int effectLvl = powerInjection == 1 ? POWER_INJECTION_1_STRENGTH_EFFECT_LVL : POWER_INJECTION_2_STRENGTH_EFFECT_LVL;
							
							targetEntity.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, POWER_INJECTION_DURATION, effectLvl, false, true));
							
							if (powerInjection > 1) {
								targetEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, POWER_INJECTION_DURATION, POWER_INJECTION_SPEED_EFFECT_LVL, false, true));
							}
							
							mPlugin.mTimers.AddCooldown(player.getUniqueId(), POWER_INJECTION_ID, POWER_INJECTION_COOLDOWN);
							
							arrow.remove();
							return;
						}
					}
				}
			}
		}
		
		//	BasiliskPoison
		{
			int basiliskPoison = ScoreboardUtil.getScoreboardValue(player, "BasiliskPoison");
			if (basiliskPoison > 0) {
				mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.TOTEM);
			}
		}
	}
	
	@Override
	public void PlayerItemHeldEvent(Player player) {
		_testInvigoratingOdor(player);
	}
	
	@Override
	public void PlayerDropItemEvent(Player player) {
		_testInvigoratingOdor(player);
	}
	
	@Override
	public void PlayerItemBreakEvent(Player player) {
		_testInvigoratingOdor(player);
	}
	
	@Override
	public void PlayerThrewSplashPotionEvent(Player player, SplashPotion potion) {
		if (player.isSneaking()) {
			int putridFumes = ScoreboardUtil.getScoreboardValue(player, "PutridFumes");
			if (putridFumes > 0) {
				String meta = putridFumes == 1 ? PUTRID_FUMES_1_TAG : PUTRID_FUMES_2_TAG;
				potion.setMetadata(meta, new FixedMetadataValue(mPlugin, 0));
			}
		}
	}
	
	@Override
	public boolean PlayerSplashPotionEvent(Player player, Collection<LivingEntity> affectedEntities, ThrownPotion potion) {
		if (potion.hasMetadata(PUTRID_FUMES_1_TAG)) {
			AreaEffectCloud cloud = EntityUtil.spawnAreaEffectCloud(player.getWorld(), potion.getLocation(), potion.getEffects(), PUTRID_FUMES_1_RADIUS, PUTRID_FUMES_DURATION);
			cloud.setSource(player);
		} else if (potion.hasMetadata(PUTRID_FUMES_2_TAG)) {
			AreaEffectCloud cloud = EntityUtil.spawnAreaEffectCloud(player.getWorld(), potion.getLocation(), potion.getEffects(), PUTRID_FUMES_2_RADIUS, PUTRID_FUMES_DURATION);
			cloud.setSource(player);
		}
		
		int causticMixture = ScoreboardUtil.getScoreboardValue(player, "CausticMixture");
		if (causticMixture > 0) {
			World world = player.getWorld();
			
			int damage = causticMixture == 1 ? CAUSTIC_MIXTURE_1_DAMAGE : CAUSTIC_MIXTURE_2_DAMAGE;
			for (LivingEntity entity : affectedEntities) {
				if (!(entity instanceof Player)) {
					if (!entity.hasMetadata(CAUSTIC_MIXTURE_TAG)) {
						entity.damage(damage);
						entity.setMetadata(CAUSTIC_MIXTURE_TAG, new FixedMetadataValue(mPlugin, 0));
						
						Location loc = entity.getLocation();
						ParticleUtil.playParticlesInWorld(world, Particle.TOTEM, loc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.001);
					}
				}
			}
		}
		
		return false;
	}
	
	@Override
	public void AreaEffectCloudApplyEvent(List<LivingEntity> entities, Player player) {
		int causticMixture = ScoreboardUtil.getScoreboardValue(player, "CausticMixture");
		if (causticMixture > 0) {
			World world = player.getWorld();
			
			int damage = causticMixture == 1 ? CAUSTIC_MIXTURE_1_DAMAGE : CAUSTIC_MIXTURE_2_DAMAGE;
			for (LivingEntity entity : entities) {
				if (!(entity instanceof Player)) {
					if (!entity.hasMetadata(CAUSTIC_MIXTURE_TAG)) {
						entity.damage(damage);
						entity.setMetadata(CAUSTIC_MIXTURE_TAG, new FixedMetadataValue(mPlugin, 0));
						
						Location loc = entity.getLocation();
						ParticleUtil.playParticlesInWorld(world, Particle.TOTEM, loc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.001);
					}
				}
			}
		}
	}
	
	private void _testInvigoratingOdor(Player player) {
		int invigoratingOdor = ScoreboardUtil.getScoreboardValue(player, "InvigoratingOdor");
		if (invigoratingOdor > 0) {
			if (_testPotionInHand(player)) {
				player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000, INVIGORATING_ODOR_RESISTENCE_EFFECT_LVL, true, false));
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1000000, INVIGORATING_ODOR_SPEED_EFFECT_LVL, true, false));
				
				if (invigoratingOdor > 1) {
					player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1000000, INVIGORATING_ODOR_REGENERATION_EFFECT_LVL, true, false));
					player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1000000, INVIGORATING_ODOR_JUMP_EFFECT_LVL, true, false	));
				}
			} else {
				player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
				player.removePotionEffect(PotionEffectType.SPEED);
				
				if (invigoratingOdor > 1) {
					player.removePotionEffect(PotionEffectType.REGENERATION);
					player.removePotionEffect(PotionEffectType.JUMP);
				}
			}
		}
	}
	
	private boolean _testPotionInHand(Player player) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		return InventoryUtil.isPotionItem(mainHand);
	}
}
