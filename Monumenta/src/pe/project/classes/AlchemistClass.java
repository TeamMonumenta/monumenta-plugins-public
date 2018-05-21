package pe.project.classes;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.EntityUtils;
import pe.project.utils.InventoryUtils;
import pe.project.utils.MovementUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.PlayerUtils;
import pe.project.utils.ScoreboardUtils;
import pe.project.utils.particlelib.ParticleEffect;
import pe.project.utils.particlelib.ParticleEffect.BlockData;

/*
    BasiliskPoison
    Unstable Arrows
    PowerInjection
    IronTincture
    Gruesome Alchemy
    Brutal Alchemy
    Enfeebling Elixir
*/

public class AlchemistClass extends BaseClass {
	private static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;

	private static final int BRUTAL_ALCHEMY_DAMAGE_1 = 2;
	private static final int BRUTAL_ALCHEMY_DAMAGE_2 = 4;
	private static final int BRUTAL_ALCHEMY_WITHER_1_DURATION = 3 * 20;
	private static final int BRUTAL_ALCHEMY_WITHER_2_DURATION = 5 * 20;

	private static final int ENFEEBLING_COOLDOWN_1 = 15 * 20;
	private static final int ENFEEBLING_COOLDOWN_2 = 10 * 20;
	private static final int ENFEEBLING_DURATION = 5 * 20;
	private static final float ENFEEBLING_KNOCKBACK_1_SPEED = 0.3f;
	private static final float ENFEEBLING_KNOCKBACK_2_SPEED = 0.45f;
	private static final int ENFEEBLING_RADIUS = 3;

	private static final int IRON_TINCTURE_THROW_COOLDOWN = 10 * 20;
	private static final int IRON_TINCTURE_USE_COOLDOWN = 40 * 20;
	private static final int IRON_TINCTURE_DURATION = 40 * 20;
	private static final double IRON_TINCTURE_VELOCITY = 0.7;

	private static final int BOMB_ARROW_COOLDOWN = 16 * 20;
	private static final int BOMB_ARROW_TRIGGER_RANGE = 32;
	private static final int BOMB_ARROW_ID = 67;
	public static final String BOMB_ARROW_TAG_NAME = "TagBearer";
	private static final int BOMB_ARROW_DURATION = 4 * 20;
	private static final float BOMB_ARROW_KNOCKBACK_SPEED = 0.55f;
	private static final int BOMB_ARROW_1_DAMAGE = 12;
	private static final int BOMB_ARROW_2_DAMAGE = 20;
	private static final int BOMB_ARROW_RADIUS = 3;

	private static final int BASILISK_POISON_1_EFFECT_LVL = 1;
	private static final int BASILISK_POISON_2_EFFECT_LVL = 2;
	private static final int BASILISK_POISON_1_DURATION = 7 * 20;
	private static final int BASILISK_POISON_2_DURATION = 6 * 20;

	private static final int POWER_INJECTION_RANGE = 16;
	private static final int POWER_INJECTION_1_STRENGTH_EFFECT_LVL = 0;
	private static final int POWER_INJECTION_2_STRENGTH_EFFECT_LVL = 1;
	private static final int POWER_INJECTION_SPEED_EFFECT_LVL = 0;
	private static final int POWER_INJECTION_DURATION = 20 * 20;
	private static final int POWER_INJECTION_COOLDOWN = 30 * 20;

	Arrow blinkArrow = null;

	public AlchemistClass(Plugin plugin, Random random) {
		super(plugin, random);
	}


	@Override
	public void LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		//  BasiliskPoison
		int basiliskPoison = ScoreboardUtils.getScoreboardValue(player, "BasiliskPoison");
		if (basiliskPoison > 0) {
			int effectLvl = basiliskPoison == 1 ? BASILISK_POISON_1_EFFECT_LVL : BASILISK_POISON_2_EFFECT_LVL;
			int duration = basiliskPoison == 1 ? BASILISK_POISON_1_DURATION : BASILISK_POISON_2_DURATION;
			damagee.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration, effectLvl, false, true));
			ParticleUtils.playParticlesInWorld(damagee.getWorld(), Particle.TOTEM, damagee.getLocation().add(0, 1.6, 0), 12, 0.4, 0.4, 0.4, 0.1);
		}
	}

	@Override
	public void PlayerShotArrowEvent(Player player, Arrow arrow) {
		//  PowerInjection
		if (arrow.isCritical()) {
			int powerInjection = ScoreboardUtils.getScoreboardValue(player, "PowerInjection");
			if (powerInjection > 0) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.POWER_INJECTION)) {
					LivingEntity targetEntity = EntityUtils.GetEntityAtCursor(player, POWER_INJECTION_RANGE, true, true, true);
					if (targetEntity != null && targetEntity instanceof Player) {
						Player targetPlayer = (Player) targetEntity;
						if (targetPlayer.getGameMode() != GameMode.SPECTATOR) {

							World zaWarudo = player.getWorld();

							ParticleUtils.playParticlesInWorld(zaWarudo, Particle.FLAME, targetPlayer.getLocation().add(0, 1, 0), 30, 1.0, 1.0, 1.0, 0.001);
							zaWarudo.playSound(targetPlayer.getLocation(), "entity.illusion_illager.prepare_blindness", 1.2f, 1.0f);
							zaWarudo.playSound(player.getLocation(), "entity.illusion_illager.prepare_blindness", 1.2f, 1.0f);

							int effectLvl = powerInjection == 1 ? POWER_INJECTION_1_STRENGTH_EFFECT_LVL : POWER_INJECTION_2_STRENGTH_EFFECT_LVL;

							mPlugin.mPotionManager.addPotion(targetPlayer, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.INCREASE_DAMAGE, POWER_INJECTION_DURATION, effectLvl, false, true));
							mPlugin.mPotionManager.addPotion(targetPlayer, PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.SPEED, POWER_INJECTION_DURATION, POWER_INJECTION_SPEED_EFFECT_LVL, false, true));

							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.POWER_INJECTION, POWER_INJECTION_COOLDOWN);

							arrow.remove();
							return;
						}
					}
				}
			}
		}

		//  BasiliskPoison
		int basiliskPoison = ScoreboardUtils.getScoreboardValue(player, "BasiliskPoison");
		if (basiliskPoison > 0) {
			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.TOTEM);
		}
	}

	@Override
	public void ProjectileHitEvent(Player player, Arrow arrow) {
		int bombArrow = ScoreboardUtils.getScoreboardValue(player, "BombArrow");
		if (bombArrow > 0 && player.getGameMode() != GameMode.ADVENTURE && player.isSneaking()) {
			if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.BOMB_ARROW)) {
				double range = arrow.getLocation().distance(player.getLocation());
				if (range <= BOMB_ARROW_TRIGGER_RANGE) {
					mPlugin.mPulseEffectTimers.AddPulseEffect(player, this, BOMB_ARROW_ID, BOMB_ARROW_TAG_NAME, BOMB_ARROW_DURATION, 20, arrow.getLocation(), 0, false);

					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.BOMB_ARROW, BOMB_ARROW_COOLDOWN);
					arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
					blinkArrow = arrow;
				}
			}
		}
	}

	private ItemStack getAlchemistPotion() {
		ItemStack stack = new ItemStack(Material.SPLASH_POTION, 1);

		PotionMeta meta = (PotionMeta)stack.getItemMeta();
		meta.setBasePotionData(new PotionData(PotionType.AWKWARD));
		meta.setColor(Color.WHITE);
		meta.setDisplayName(ChatColor.AQUA + "Alchemist's Potion");
		List<String> lore = Arrays.asList(new String[] {
			ChatColor.GRAY + "A unique potion for Alchemists",
		});
		meta.setLore(lore);
		stack.setItemMeta(meta);
		return stack;
	}

	@Override
	public void EntityDeathEvent(Player player, LivingEntity killedEntity, DamageCause cause, boolean shouldGenDrops) {
		int brutalAlchemy = ScoreboardUtils.getScoreboardValue(player, "BrutalAlchemy");
		int gruesomeAlchemy = ScoreboardUtils.getScoreboardValue(player, "GruesomeAlchemy");

		boolean added = false;
		int potCount = 0;

		if (brutalAlchemy > 0 || gruesomeAlchemy > 0) {
			Inventory inv = player.getInventory();
			for (ItemStack item : inv.getContents()) {
				if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
					int amount = item.getAmount();
					potCount += amount;
				}
			}

			if (potCount > 0 && potCount < 32) {
				for (ItemStack item : inv.getContents()) {
					if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
						int amount = item.getAmount();
						item.setAmount(amount + 1);
						added = true;
						break;
					}
				}
			} else if (!added && potCount == 0) {
				inv.addItem(getAlchemistPotion());
			}
		}
	}

	@Override
	public void PlayerThrewSplashPotionEvent(Player player, SplashPotion potion) {
		ItemStack item = potion.getItem();
		if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
			new BukkitRunnable() {

				@Override
				public void run() {
					ParticleEffect.SPELL.display(0.1f, 0.1f, 0.1f, 0, 3, potion.getLocation(), 40);
					if (potion.isDead() || potion == null) {
						this.cancel();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);

			potion.setMetadata("AlchemistPotion", new FixedMetadataValue(mPlugin, 0));
		}
	}

	@Override
	public boolean PlayerSplashPotionEvent(Player player, Collection<LivingEntity> affectedEntities,
	                                       ThrownPotion potion, PotionSplashEvent event) {
		if (potion.hasMetadata("AlchemistPotion")) {
			if (affectedEntities != null && !affectedEntities.isEmpty()) {
				int brutalAlchemy = ScoreboardUtils.getScoreboardValue(player, "BrutalAlchemy");
				if (brutalAlchemy > 0) {
					for (LivingEntity entity : affectedEntities) {
						if (EntityUtils.isHostileMob(entity)) {
							int damage = (brutalAlchemy == 1) ? BRUTAL_ALCHEMY_DAMAGE_1 : BRUTAL_ALCHEMY_DAMAGE_2;
							int duration = (brutalAlchemy == 1) ? BRUTAL_ALCHEMY_WITHER_1_DURATION : BRUTAL_ALCHEMY_WITHER_2_DURATION;
							EntityUtils.damageEntity(mPlugin, entity, damage, player);
							entity.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, duration + 10, 1, false, true));
						}
					}
				}

				int gruesomeAlchemy = ScoreboardUtils.getScoreboardValue(player, "GruesomeAlchemy");
				if (gruesomeAlchemy > 0) {
					for (LivingEntity entity : affectedEntities) {
						if (EntityUtils.isHostileMob(entity)) {
							entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, GRUESOME_ALCHEMY_DURATION, 1 + gruesomeAlchemy, false, true));
							if (gruesomeAlchemy > 1) {
								entity.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, GRUESOME_ALCHEMY_DURATION, 2, false, true));
							}
						}
					}
				}
			}
		}
		return true;
	}


	// =================
	// = IRON TINCTURE =
	// =================

	@Override
	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		if (player.isSneaking()) {
			if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
				int ironTincture = ScoreboardUtils.getScoreboardValue(player, "IronTincture");
				if (ironTincture > 0) {
					ItemStack mainHand = player.getInventory().getItemInMainHand();
					if (!InventoryUtils.isBowItem(mainHand) || mainHand == null) {
						if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.IRON_TINCTURE)) {

							Location loc = player.getLocation().add(0, 1.8, 0);
							ItemStack itemTincture = new ItemStack(Material.SPLASH_POTION);
							player.getWorld().playSound(loc, Sound.ENTITY_SNOWBALL_THROW, 1, 0.15f);
							Item tincture = (player.getWorld()).dropItem(loc, itemTincture);
							tincture.setPickupDelay(Integer.MAX_VALUE);

							Vector vel = player.getEyeLocation().getDirection().normalize();
							vel.multiply(IRON_TINCTURE_VELOCITY);

							tincture.setVelocity(vel);
							tincture.setGlowing(true);

							new BukkitRunnable() {
								int tinctureDecay = 10 * 10;

								@Override
								public void run() {
									if (tincture.isDead() || tincture == null) {
										this.cancel();
									}
									ParticleEffect.SPELL.display(0, 0, 0, 0.1f, 3, tincture.getLocation(), 40);
									if (tincture.getTicksLived() < 12) {
										return;
									}
									for (Player p : PlayerUtils.getNearbyPlayers(tincture.getLocation(), 1)) {
										tincture.getWorld().playSound(tincture.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.85f);
										ParticleEffect.BLOCK_DUST.display(new BlockData(Material.GLASS, (byte) 0), 0.1f, 0.1f, 0.1f, 0.1f, 250, tincture.getLocation(), 40);
										tincture.remove();

										int ironTincture = ScoreboardUtils.getScoreboardValue(player, "IronTincture");
										p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, IRON_TINCTURE_DURATION, ironTincture));

										World zaWarudo = player.getWorld();

										if (p != player) {
											player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, IRON_TINCTURE_DURATION, ironTincture));
											ParticleUtils.playParticlesInWorld(zaWarudo, Particle.LAVA, player.getLocation().add(0, 1, 0), 15, 1.0, 1.0, 1.0, 0.001);
											zaWarudo.playSound(player.getLocation(), "entity.illusion_illager.prepare_mirror", 1.2f, 1.0f);
										}
										this.cancel();
										mPlugin.mTimers.removeCooldown(player.getUniqueId(), Spells.IRON_TINCTURE);
										mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.IRON_TINCTURE, IRON_TINCTURE_USE_COOLDOWN);

										ParticleUtils.playParticlesInWorld(zaWarudo, Particle.LAVA, p.getLocation().add(0, 1, 0), 15, 1.0, 1.0, 1.0, 0.001);
										zaWarudo.playSound(p.getLocation(), "entity.illusion_illager.prepare_mirror", 1.2f, 1.0f);
									}

									tinctureDecay--;
									if (tinctureDecay <= 0) {
										tincture.remove();
									}
								}

							}.runTaskTimer(mPlugin, 0, 2);


							mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.IRON_TINCTURE, IRON_TINCTURE_THROW_COOLDOWN);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(Player player, LivingEntity damagee, double damage, DamageCause cause) {
		int enfeeblingElixir = ScoreboardUtils.getScoreboardValue(player, "EnfeeblingElixir");
		if (enfeeblingElixir > 0 && EntityUtils.isHostileMob(damagee)) {
			if (player.isSneaking()) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), Spells.ENFEEBLING_ELIXIR)) {

					int cooldown = enfeeblingElixir == 1 ? ENFEEBLING_COOLDOWN_1 : ENFEEBLING_COOLDOWN_2;
					float kbSpeed = (enfeeblingElixir == 1) ? ENFEEBLING_KNOCKBACK_1_SPEED : ENFEEBLING_KNOCKBACK_2_SPEED;

					List<Entity> entities = damagee.getNearbyEntities(ENFEEBLING_RADIUS, ENFEEBLING_RADIUS, ENFEEBLING_RADIUS);
					for (Entity e : entities) {
						if (EntityUtils.isHostileMob(e)) {
							LivingEntity mob = (LivingEntity)e;

							MovementUtils.KnockAway(damagee, mob, kbSpeed);
							mob.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ENFEEBLING_DURATION, 0, true, false));
						}
					}

					MovementUtils.KnockAway(player, damagee, kbSpeed);
					damagee.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ENFEEBLING_DURATION, 0, true, false));

					ParticleEffect.SPELL_MOB.display(2, 1.5f, 2, 0, 100, damagee.getLocation(), 40);
					damagee.getWorld().playSound(damagee.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1, 0);
					mPlugin.mTimers.AddCooldown(player.getUniqueId(), Spells.ENFEEBLING_ELIXIR, cooldown);
				}
			}
		}

		return true;
	}

	@Override
	public void PulseEffectApplyEffect(Player owner, Location loc, Entity effectedEntity, int abilityID) {
		if (abilityID == BOMB_ARROW_ID) {
			int bombArrow = ScoreboardUtils.getScoreboardValue(owner, "BombArrow");
			if (bombArrow > 0) {
				ParticleUtils.playParticlesInWorld(owner.getWorld(), Particle.FLAME, loc, 8, 0.3, 0.3, 0.3, 0.001);
				ParticleUtils.playParticlesInWorld(owner.getWorld(), Particle.SMOKE_NORMAL, loc, 30, 0.5, 0.5, 0.5, 0.001);
				owner.getWorld().playSound(loc, "block.lava.extinguish", 5.0f, 0.25f);
			}
		}
	}

	@Override
	public void PulseEffectComplete(Player owner, Location loc, Entity marker, int abilityID) {
		if (abilityID == BOMB_ARROW_ID) {
			int bombArrow = ScoreboardUtils.getScoreboardValue(owner, "BombArrow");
			if (bombArrow > 0) {
				if (blinkArrow != null) {
					loc = blinkArrow.getLocation();
					blinkArrow.remove();
					blinkArrow = null;
				}

				loc = loc.add(0, 1.2, 0);
				owner.getWorld().playSound(loc, "entity.generic.explode", 0.7f, 1.0f);
				owner.getWorld().playSound(loc, "entity.generic.explode", 0.9f, 1.0f);

				ParticleUtils.playParticlesInWorld(owner.getWorld(), Particle.EXPLOSION_HUGE, loc, 3, 0.02, 0.02, 0.02, 0.001);
				List<Entity> entities = marker.getNearbyEntities(BOMB_ARROW_RADIUS, BOMB_ARROW_RADIUS, BOMB_ARROW_RADIUS);

				int baseDamage = (bombArrow == 1) ? BOMB_ARROW_1_DAMAGE : BOMB_ARROW_2_DAMAGE;

				for (int i = 0; i < entities.size(); i++) {
					Entity e = entities.get(i);
					if (EntityUtils.isHostileMob(e)) {
						LivingEntity mob = (LivingEntity)e;
						EntityUtils.damageEntity(mPlugin, mob, baseDamage, owner);
						MovementUtils.KnockAway((LivingEntity)marker, mob, BOMB_ARROW_KNOCKBACK_SPEED);
					}
				}
			}
		}
	}
}
