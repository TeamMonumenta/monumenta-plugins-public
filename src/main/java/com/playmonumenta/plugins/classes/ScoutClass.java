package com.playmonumenta.plugins.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
    Agility
    Swiftness
    Exploration
    BowMastery
    Tinkering
    Volley
    StandardBearer
*/

public class ScoutClass extends BaseClass {
	private static final int SWIFTNESS_EFFECT_SPEED_LVL = 0;
	private static final int SWIFTNESS_EFFECT_JUMP_LVL = 2;

	private static final int AGILITY_1_EFFECT_LVL = 0;
	private static final int AGILITY_2_EFFECT_LVL = 1;
	private static final int AGILITY_1_DAMAGE_BONUS = 1;
	private static final int AGILITY_2_DAMAGE_BONUS = 2;

	private static final int BOW_MASTER_1_DAMAGE = 3;
	private static final int BOW_MASTER_2_DAMAGE = 6;

	private static final int EAGLE_EYE_EFFECT_LVL = 0;
	private static final int EAGLE_EYE_DURATION = 10 * 20;
	private static final int EAGLE_EYE_COOLDOWN = 24 * 20;	//Was 30
	private static final int EAGLE_EYE_1_VULN_LEVEL = 3; //20%
	private static final int EAGLE_EYE_2_VULN_LEVEL = 6; //35%
	private static final int EAGLE_EYE_RADIUS = 20;
	private static final double EAGLE_EYE_DOT_ANGLE = 0.33;

	private static final int VOLLEY_COOLDOWN = 15 * 20;
	private static final int VOLLEY_1_ARROW_COUNT = 7;
	private static final int VOLLEY_2_ARROW_COUNT = 10;
	private static final double VOLLEY_1_DAMAGE_INCREASE = 0.75;
	private static final double VOLLEY_2_DAMAGE_INCREASE = 1.5;

	private static final int STANDARD_BEARER_ID = 67;
	public static final int STANDARD_BEARER_FAKE_ID = 10078;
	public static final String STANDARD_BEARER_TAG_NAME = "TagBearer";
	private static final double STANDARD_BEARER_ARMOR = 2;
	private static final double STANDARD_BEARER_TRIGGER_RANGE = 2;
	private static final int STANDARD_BEARER_COOLDOWN = 60 * 20;
	private static final int STANDARD_BEARER_DURATION = 30 * 20;
	private static final int STANDARD_BEARER_TRIGGER_RADIUS = 12;
	private static final double STANDARD_BEARER_DAMAGE_MULTIPLIER = 1.25;

	private static float PASSIVE_ARROW_SAVE = 0.20f;

	public ScoutClass(Plugin plugin, Random random) {
		super(plugin, random);
	}

	@Override
	public void setupClassPotionEffects(Player player) {
		_testForAgility(player);
		_testForSwiftness(player);
	}

	@Override
	public void PlayerRespawnEvent(Player player) {
		_testForAgility(player);
		_testForSwiftness(player);
	}

	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee,
	                                          EntityDamageByEntityEvent event) {
		//  Volley
		int volley = ScoreboardUtils.getScoreboardValue(player, "Volley");
		if (volley > 0) {
			if (arrow.hasMetadata("Volley")) {
				double damageMultiplier = volley == 1 ? VOLLEY_1_DAMAGE_INCREASE : VOLLEY_2_DAMAGE_INCREASE;
				double oldDamage = event.getDamage();

				double newDamage = oldDamage + (oldDamage * damageMultiplier);
				event.setDamage(newDamage);
			}
		}

		//  Bow Mastery
		int bowMastery = ScoreboardUtils.getScoreboardValue(player, "BowMastery");
		if (bowMastery > 0) {
			if (arrow.isCritical()) {
				int bonusDamage = bowMastery == 1 ? BOW_MASTER_1_DAMAGE : BOW_MASTER_2_DAMAGE;
				EntityUtils.damageEntity(mPlugin, damagee, bonusDamage, player);
			}
		}
	}

	@Override
	public void PlayerShotArrowEvent(Player player, Arrow arrow) {
		List<Projectile> projectiles;

	// PASSIVE : 25% chance of not consuming an arrow
		if (mRandom.nextFloat() < PASSIVE_ARROW_SAVE) {
			ItemStack mainHand = player.getInventory().getItemInMainHand();
			ItemStack offHand = player.getInventory().getItemInOffHand();
			if (InventoryUtils.isBowItem(mainHand) || InventoryUtils.isBowItem(offHand)) {
				int infLevel = Math.max(mainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE), offHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE));
				if (infLevel == 0) {
					arrow.setPickupStatus(Arrow.PickupStatus.ALLOWED);
					Inventory playerInv = player.getInventory();
					int firstArrow = playerInv.first(Material.ARROW);
					int firstTippedArrow = playerInv.first(Material.TIPPED_ARROW);

					player.getWorld().playSound(player.getLocation(), "entity.arrow.hit_player", 0.3f, 1.0f);

					int arrowSlot = -1;
					if (firstArrow == -1 && firstTippedArrow > -1) {
						arrowSlot = firstTippedArrow;
					} else if (firstArrow > - 1 && firstTippedArrow == -1) {
						arrowSlot = firstArrow;
					} else {
						arrowSlot = Math.min(firstArrow, firstTippedArrow);
					}

					ItemStack arrowStack = playerInv.getItem(arrowSlot);
					int arrowQuantity = arrowStack.getAmount();
					if (arrowQuantity < 64) {
						arrowStack.setAmount(arrowQuantity);
						arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
					}
					playerInv.setItem(arrowSlot, arrowStack);
				}
			}
		}

		boolean wasCritical = arrow.isCritical();

		//  Volley
		if (player.isSneaking()) {
			int volley = ScoreboardUtils.getScoreboardValue(player, "Volley");
			if (volley > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.VOLLEY)) {
					boolean isCritical = arrow.isCritical();
					int fireTicks = arrow.getFireTicks();
					int knockbackStrength = arrow.getKnockbackStrength();
					int numArrows = (volley == 1) ? VOLLEY_1_ARROW_COUNT : VOLLEY_2_ARROW_COUNT;

					// Store PotionData from the original arrow only if it is weakness or slowness
					PotionData tArrowData = null;
					if (arrow instanceof TippedArrow) {
						TippedArrow tArrow = (TippedArrow)arrow;

						tArrowData = tArrow.getBasePotionData();
						if (tArrowData.getType() != PotionType.SLOWNESS && tArrowData.getType() != PotionType.WEAKNESS) {
							// This arrow isn't weakness or slowness - don't store the potion data
							tArrowData = null;
						}
					}

					if (tArrowData == null) {
						projectiles = EntityUtils.spawnArrowVolley(mPlugin, player, numArrows, 1.5, 5, Arrow.class);
					} else {
						projectiles = EntityUtils.spawnArrowVolley(mPlugin, player, numArrows, 1.5, 5, TippedArrow.class);
					}

					for (Projectile proj : projectiles) {
						Arrow _arrow = (Arrow)proj;

						proj.setMetadata("Volley", new FixedMetadataValue(mPlugin, 0));

						// If the base arrow's potion data is still stored, apply it to the new arrows
						if (tArrowData != null && _arrow instanceof TippedArrow) {
							((TippedArrow)_arrow).setBasePotionData(tArrowData);
						}

						_arrow.setCritical(isCritical);
						_arrow.setFireTicks(fireTicks);
						_arrow.setKnockbackStrength(knockbackStrength);

						mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SMOKE_NORMAL);
					}

					//  I hate this so much, you don't even know... [Rock]
					Location jankWorkAround = player.getLocation();
					jankWorkAround.setY(-15);
					arrow.teleport(jankWorkAround);

					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.VOLLEY, VOLLEY_COOLDOWN);
				} else {
					projectiles = new ArrayList<Projectile>();
					projectiles.add(arrow);
				}
			} else {
				projectiles = new ArrayList<Projectile>();
				projectiles.add(arrow);
			}
		} else {
			projectiles = new ArrayList<Projectile>();
			projectiles.add(arrow);
		}

		//  Bow Mastery
		int bowMastery = ScoreboardUtils.getScoreboardValue(player, "BowMastery");
		if (bowMastery > 0) {
			if (wasCritical) {
				for (Projectile proj : projectiles) {
					mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.CLOUD);
				}
			}
		}
	}

	@Override
	public void ProjectileHitEvent(Player player, Arrow arrow) {
		int standardBearer = ScoreboardUtils.getScoreboardValue(player, "StandardBearer");
		if (standardBearer > 0 && player.getGameMode() != GameMode.ADVENTURE) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.STANDARD_BEARER)) {
				double range = arrow.getLocation().distance(player.getLocation());
				if (range <= STANDARD_BEARER_TRIGGER_RANGE) {
					mPlugin.mPulseEffectTimers.AddPulseEffect(player, this, STANDARD_BEARER_ID,
					                                          STANDARD_BEARER_TAG_NAME, STANDARD_BEARER_DURATION, 1, player.getLocation(),
					                                          STANDARD_BEARER_TRIGGER_RADIUS, true);

					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.STANDARD_BEARER, STANDARD_BEARER_COOLDOWN);

					Location L = player.getLocation();
					while(L.getBlock().getType() != Material.AIR){
						L.add(0, 0.25, 0);
					}

					Block block = L.getBlock();
					block.setType(Material.STANDING_BANNER);

					if (block.getState() instanceof Banner) {
						Banner banner = (Banner)block.getState();
						banner.setBaseColor(DyeColor.CYAN);

						banner.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.STRAIGHT_CROSS));
						banner.addPattern(new Pattern(DyeColor.BLUE, PatternType.CIRCLE_MIDDLE));
						banner.addPattern(new Pattern(DyeColor.BLACK, PatternType.FLOWER));
						banner.addPattern(new Pattern(DyeColor.BLUE, PatternType.TRIANGLES_BOTTOM));
						banner.addPattern(new Pattern(DyeColor.BLUE, PatternType.TRIANGLES_TOP));

						banner.update();
					}
				}
			}
		}
	}

	@Override
	public void PulseEffectApplyEffect(Player owner, Location loc, Player effectedPlayer, int abilityID) {
		//  StandardBearer
		{
			if (abilityID == STANDARD_BEARER_ID) {
				int standardBearer = ScoreboardUtils.getScoreboardValue(owner, "StandardBearer");
				if (standardBearer > 0) {
					double x = loc.getX();
					double y = loc.getY() + 1;
					double z = loc.getZ();
					Location newLoc = new Location(loc.getWorld(), x, y, z);

					ParticleUtils.playParticlesInWorld(owner.getWorld(), Particle.VILLAGER_HAPPY, newLoc, 5, 0.75,
					                                   0.75, 0.75, 0.001);

					if (standardBearer > 1) {
						AttributeInstance att = effectedPlayer.getAttribute(Attribute.GENERIC_ARMOR);
						double baseValue = att.getBaseValue();
						att.setBaseValue(Math.min(baseValue + STANDARD_BEARER_ARMOR, 30));
					}
				}
			}
		}
	}

	@Override
	public void PulseEffectRemoveEffect(Player owner, Location loc, Player effectedPlayer, int abilityID) {
		//  StandardBearer
		{
			if (abilityID == STANDARD_BEARER_ID) {
				int standardBearer = ScoreboardUtils.getScoreboardValue(owner, "StandardBearer");
				if (standardBearer > 1) {
					AttributeInstance att = effectedPlayer.getAttribute(Attribute.GENERIC_ARMOR);
					double baseValue = att.getBaseValue();
					att.setBaseValue(Math.max(baseValue - STANDARD_BEARER_ARMOR, 0));
				}
			}
		}
	}

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (player.isSneaking()) {
			if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
				int eagleEye = ScoreboardUtils.getScoreboardValue(player, "Tinkering");
				if (eagleEye > 0) {
					if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.EAGLE_EYE)) {
						Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
						World world = player.getWorld();

						List<Entity> entities = player.getNearbyEntities(EAGLE_EYE_RADIUS, EAGLE_EYE_RADIUS, EAGLE_EYE_RADIUS);
						for (Entity e : entities) {
							if (e instanceof LivingEntity && EntityUtils.isHostileMob(e)) {
								LivingEntity mob = (LivingEntity)e;

								Vector toMobVector = mob.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
								if (playerDir.dot(toMobVector) > EAGLE_EYE_DOT_ANGLE) {
									mob.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, EAGLE_EYE_DURATION, EAGLE_EYE_EFFECT_LVL, true, false));

									int eagleLevel = (eagleEye == 1) ? EAGLE_EYE_1_VULN_LEVEL : EAGLE_EYE_2_VULN_LEVEL;
									mob.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, EAGLE_EYE_DURATION, eagleLevel, true, false));

									ParticleUtils.playParticlesInWorld(world, Particle.FIREWORKS_SPARK, mob.getLocation().add(0, 1, 0),
									                                   10, 0.7, 0.7, 0.7, 0.001);
									world.playSound(player.getLocation(), "entity.parrot.imitate.shulker", 0.4f, 1.7f);
								}
							}
						}

						mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.EAGLE_EYE, EAGLE_EYE_COOLDOWN);
					}
				}
			}
		}
	}

	@Override
	public void ModifyDamage(Player player, BaseClass owner, EntityDamageByEntityEvent event) {
		if (player.hasMetadata(STANDARD_BEARER_TAG_NAME)) {
			int standardBearer = ScoreboardUtils.getScoreboardValue(player, "StandardBearer");
			if (standardBearer > 0) {
				double damage = event.getDamage();
				damage *= STANDARD_BEARER_DAMAGE_MULTIPLIER;
				event.setDamage(damage);
			}
		}

		// Agility
		int agility = ScoreboardUtils.getScoreboardValue(player, "Agility");
		if (agility > 0) {
			int extraDamage = (agility == 1) ? AGILITY_1_DAMAGE_BONUS : AGILITY_2_DAMAGE_BONUS;
			event.setDamage(event.getDamage() + extraDamage);
		}
	}

	public void _testForAgility(Player player) {
		int agility = ScoreboardUtils.getScoreboardValue(player, "Agility");
		if (agility > 0) {
			int effectLevel = agility == 1 ? AGILITY_1_EFFECT_LVL : AGILITY_2_EFFECT_LVL;
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.FAST_DIGGING, 1000000, effectLevel, true, false));
		}
	}

	public void _testForSwiftness(Player player) {
		int swiftness = ScoreboardUtils.getScoreboardValue(player, "Swiftness");
		if (swiftness > 0) {
			mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.SPEED, 1000000, SWIFTNESS_EFFECT_SPEED_LVL, true, false));

			if (swiftness > 1) {
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF,
				                                 new PotionEffect(PotionEffectType.JUMP, 1000000, SWIFTNESS_EFFECT_JUMP_LVL, true, false));
			}
		}
	}
}
