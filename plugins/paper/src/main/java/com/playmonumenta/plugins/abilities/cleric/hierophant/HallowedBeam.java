package com.playmonumenta.plugins.abilities.cleric.hierophant;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.AbilityManager;

public class HallowedBeam extends MultipleChargeAbility {

	private static final int HALLOWED_1_MAX_CHARGES = 2;
	private static final int HALLOWED_2_MAX_CHARGES = 3;
	private static final int HALLOWED_1_COOLDOWN = 20 * 16;
	private static final int HALLOWED_2_COOLDOWN = 20 * 12;
	private static final double HALLOWED_HEAL_PERCENT = 0.2;
	private static final double HALLOWED_DAMAGE_REDUCTION_PERCENT = 0.1;
	private static final int HALLOWED_DAMAGE_REDUCTION_DURATION = 20 * 5;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "HallowedPercentDamageResistEffect";
	private static final int HALLOWED_RADIUS = 4;
	private static final int HALLOWED_UNDEAD_STUN = 10; // 20 * 0.5
	private static final int HALLOWED_LIVING_STUN = 20 * 2;
	private static final int CAST_RANGE = 30;
	
	private Crusade mCrusade;
	private boolean mCountsHumanoids = false;

	public HallowedBeam(Plugin plugin, Player player) {
		super(plugin, player, "Hallowed Beam", HALLOWED_1_MAX_CHARGES, HALLOWED_2_MAX_CHARGES);
		mInfo.mScoreboardId = "HallowedBeam";
		mInfo.mShorthandName = "HB";
		mInfo.mDescriptions.add("Left-click the air with a bow or crossbow while looking directly at a player or mob to shoot a beam of light. If aimed at a player, the beam instantly heals them for 20% of their max health, knocking back enemies within 4 blocks. If aimed at an Undead, it instantly deals the equipped projectile weapon's damage to the target, and stuns them for half a second. If aimed at a non-undead mob, it instantly stuns them for 2s. Two charges. Cooldown: 16s each charge.");
		mInfo.mDescriptions.add("Hallowed Beam gains a third charge, the cooldown is reduced to 12 seconds, and players healed by it gain 10% damage resistance for 5 seconds.");
		mInfo.mLinkedSpell = Spells.HALLOWED_BEAM;
		mInfo.mCooldown = getAbilityScore() == 1 ? HALLOWED_1_COOLDOWN : HALLOWED_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mIgnoreCooldown = true;
		
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player != null) {
				mCrusade = AbilityManager.getManager().getPlayerAbility(mPlayer, Crusade.class);
				if (mCrusade != null) {
					mCountsHumanoids = mCrusade.getAbilityScore() == 2;
				}
			}
		});
	}

	@Override
	public void cast(Action action) {
		Player player = mPlayer;
		LivingEntity e = EntityUtils.getEntityAtCursor(player, CAST_RANGE, true, true, true);

		PlayerInventory inventory = mPlayer.getInventory();
		ItemStack inMainHand = inventory.getItemInMainHand();

		if (InventoryUtils.isBowItem(inMainHand) && !ItemUtils.isShootableItem(inventory.getItemInOffHand())) {
			if (!consumeCharge()) {
				return;
			}

			//Unsure why the runnable needs to exist, but it breaks if I don't have it
			new BukkitRunnable() {
				@Override
				public void run() {
					Location loc = player.getEyeLocation();
					Vector dir = loc.getDirection();
					World world = mPlayer.getWorld();
					world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1, 0.85f);
					world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1, 0.9f);

					if (e == null) {
						for (int i = 0; i < CAST_RANGE; i++) {
							loc.add(dir);
							world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.25, 0.25, 0.25, 0);
							world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f);
						}
						this.cancel();
						return;
					}

					for (int i = 0; i < CAST_RANGE; i++) {
						loc.add(dir);
						world.spawnParticle(Particle.VILLAGER_HAPPY, loc, 5, 0.25, 0.25, 0.25, 0);
						world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.05f, 0.05f, 0.05f, 0.025f);
						if (loc.distance(e.getEyeLocation()) < 1.25) {
							loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.35f);
							loc.getWorld().playSound(loc, Sound.ENTITY_ARROW_HIT, 1, 0.9f);
							break;
						}
					}
					LivingEntity applyE = e;
					//Check if heal should override damage
					for (Entity en : e.getNearbyEntities(1.5, 1.5, 1.5)) {
						if (en instanceof Player && ((Player) e).getGameMode() != GameMode.SPECTATOR && en.getUniqueId() != mPlayer.getUniqueId()) {
							Player newP = EntityUtils.getNearestPlayer(en.getLocation(), 1.5);
							// Don't count if the caster is the closest, can't do a self-heal
							if (newP.getUniqueId() != mPlayer.getUniqueId()) {
								applyE = newP;
							}
						}
					}
					if ((applyE instanceof Player && ((Player) applyE).getGameMode() != GameMode.SPECTATOR)) {
						Player pe = (Player) applyE;
						Location eLoc = pe.getLocation().add(0, pe.getHeight() / 2, 0);
						world.spawnParticle(Particle.SPELL_INSTANT, pe.getLocation(), 500, 2.5, 0.15f, 2.5, 1);
						world.spawnParticle(Particle.VILLAGER_HAPPY, pe.getLocation(), 150, 2.55, 0.15f, 2.5, 1);
						world.playSound(player.getEyeLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 2, 1.5f);
						AttributeInstance maxHealth = pe.getAttribute(Attribute.GENERIC_MAX_HEALTH);
						if (maxHealth != null) {
							PlayerUtils.healPlayer(pe, maxHealth.getValue() * HALLOWED_HEAL_PERCENT);
						}
						if (getAbilityScore() == 2) {
							mPlugin.mEffectManager.addEffect(pe, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(HALLOWED_DAMAGE_REDUCTION_DURATION, HALLOWED_DAMAGE_REDUCTION_PERCENT));
						}
						for (LivingEntity le : EntityUtils.getNearbyMobs(eLoc, HALLOWED_RADIUS)) {
							MovementUtils.knockAway(pe, le, 0.65f);
						}
					} else if (EntityUtils.isUndead(applyE) || (mCountsHumanoids && EntityUtils.isHumanoid(applyE))) {
						Arrow arrow = mPlayer.launchProjectile(Arrow.class);
						if (inMainHand.containsEnchantment(Enchantment.ARROW_FIRE)) {
							arrow.setFireTicks(20 * 5);
						}
						arrow.setCritical(true);
						arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
						arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(100.0));
						ProjectileLaunchEvent eventLaunch = new ProjectileLaunchEvent(arrow);
						Bukkit.getPluginManager().callEvent(eventLaunch);

						EntityUtils.applyStun(mPlugin, HALLOWED_UNDEAD_STUN, applyE);
						Location eLoc = applyE.getLocation().add(0, applyE.getHeight() / 2, 0);
						world.spawnParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f);
						world.spawnParticle(Particle.FIREWORKS_SPARK, eLoc, 75, 0, 0, 0, 0.3f);

						//Delete bow if durability is 0 and isn't shattered.
						//This is needed because Hallowed doesn't consume durability, but there is a high-damage uncommon bow
						//with 0 durability that should not be infinitely usable.
						Damageable damageable = (Damageable)inMainHand.getItemMeta();
						if ((damageable.getDamage() >= inMainHand.getType().getMaxDurability()) && !ItemUtils.isItemShattered(inMainHand)) {
							inMainHand.setAmount(0);
						}
					} else if (EntityUtils.isHostileMob(applyE)) {
						EntityUtils.applyStun(mPlugin, HALLOWED_LIVING_STUN, applyE);
						if (inMainHand.containsEnchantment(Enchantment.ARROW_FIRE)) {
							EntityUtils.applyFire(mPlugin, 20 * 15, applyE, player);
						}
						Location eLoc = applyE.getLocation().add(0, applyE.getHeight() / 2, 0);
						world.spawnParticle(Particle.SPIT, eLoc, 40, 0, 0, 0, 0.25f);
						world.spawnParticle(Particle.CRIT_MAGIC, loc, 30, 1, 1, 1, 0.25);
					}
					this.cancel();
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
			cast(Action.LEFT_CLICK_AIR);
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		LivingEntity targetEntity = EntityUtils.getEntityAtCursor(mPlayer, CAST_RANGE, true, true, true);
		if (targetEntity instanceof Player && ((Player) targetEntity).getGameMode() != GameMode.SPECTATOR
				|| targetEntity != null && EntityUtils.isHostileMob(targetEntity)) {
			return !mPlayer.isSneaking();
		}

		return false;
	}

}
