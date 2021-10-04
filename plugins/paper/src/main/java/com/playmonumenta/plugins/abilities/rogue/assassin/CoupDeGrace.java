package com.playmonumenta.plugins.abilities.rogue.assassin;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

/*
 * Coup De Grâce: If you melee attack a normal enemy and that attack
 * brings it under 10% health, they die instantly. If you melee attack
 * an elite enemy and that attack brings it under 20% health, they die
 * instantly. At level 2, the threshold increases to 15% health for
 * normal enemies and 30% health for elite enemies.
 */
public class CoupDeGrace extends Ability {

	private static final double COUP_1_NORMAL_THRESHOLD = 0.1;
	private static final double COUP_2_NORMAL_THRESHOLD = 0.15;
	private static final double COUP_1_ELITE_THRESHOLD = 0.2;
	private static final double COUP_2_ELITE_THRESHOLD = 0.3;

	private final double mNormalThreshold;
	private final double mEliteThreshold;

	public CoupDeGrace(Plugin plugin, Player player) {
		super(plugin, player, "Coup de Grace");
		mInfo.mScoreboardId = "CoupDeGrace";
		mInfo.mShorthandName = "CdG";
		mInfo.mDescriptions.add("If you melee attack a normal enemy and they get under 10% health they die instantly. The threshold for elites is 20% health");
		mInfo.mDescriptions.add("The health threshold is increased to 15% for normal enemies and 30% for elites.");
		mDisplayItem = new ItemStack(Material.WITHER_SKELETON_SKULL, 1);
		mNormalThreshold = getAbilityScore() == 1 ? COUP_1_NORMAL_THRESHOLD : COUP_2_NORMAL_THRESHOLD;
		mEliteThreshold = getAbilityScore() == 1 ? COUP_1_ELITE_THRESHOLD : COUP_2_ELITE_THRESHOLD;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if ((event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) && event.getEntity() instanceof LivingEntity) {
			LivingEntity le = (LivingEntity) event.getEntity();
			for (PotionEffect effect : le.getActivePotionEffects()) {
				if (effect.getType() == PotionEffectType.DAMAGE_RESISTANCE && effect.getAmplifier() >= 4) {
					return true;
				}
			}
			AttributeInstance maxHealth = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			if (maxHealth != null) {
				double maxHealthValue = maxHealth.getValue();
				if (EntityUtils.isElite(le)) {
					if (le.getHealth() - (event.getFinalDamage() * EntityUtils.vulnerabilityMult(le)) < maxHealthValue * mEliteThreshold) {
						execute(event);
					}
				} else if (!EntityUtils.isBoss(le)) {
					if (le.getHealth() - (event.getFinalDamage() * EntityUtils.vulnerabilityMult(le)) < maxHealthValue * mNormalThreshold) {
						execute(event);
					}
				}
			}
		}

		return true;
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		if (event.getDamaged() instanceof LivingEntity && event.getMagicType() == MagicType.ENCHANTMENT) {
			LivingEntity le = event.getDamaged();
			AttributeInstance maxHealth = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			if (maxHealth != null) {
				double maxHealthValue = maxHealth.getValue();
				if (EntityUtils.isElite(le)) {
					if (le.getHealth() - (event.getDamage() * EntityUtils.vulnerabilityMult(le)) < maxHealthValue * mEliteThreshold) {
						execute(event);
					}
				} else if (!EntityUtils.isBoss(le)) {
					if (le.getHealth() - (event.getDamage() * EntityUtils.vulnerabilityMult(le)) < maxHealthValue * mNormalThreshold) {
						execute(event);
					}
				}
			}
		}
	}

	private void execute(EntityDamageByEntityEvent event) {
		LivingEntity le = (LivingEntity) event.getEntity();
		EntityUtils.damageEntity(mPlugin, le, 9001, mPlayer, null, true, null, false, false, true, true);
		World world = mPlayer.getWorld();
		world.playSound(le.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.75f, 0.75f);
		world.playSound(le.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.5f);
		world.spawnParticle(Particle.BLOCK_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 20, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65, Material.REDSTONE_WIRE.createBlockData());
		if (getAbilityScore() > 1) {
			world.spawnParticle(Particle.SPELL_WITCH, le.getLocation().add(0, le.getHeight() / 2, 0), 10, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65);
			world.spawnParticle(Particle.BLOCK_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 20, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65, Material.REDSTONE_BLOCK.createBlockData());
		}
	}

	private void execute(CustomDamageEvent event) {
		LivingEntity le = event.getDamaged();
		event.setDamage(event.getDamage() + 9001);
		World world = mPlayer.getWorld();
		world.playSound(le.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.75f, 0.75f);
		world.playSound(le.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.5f);
		world.spawnParticle(Particle.BLOCK_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 20, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65, Material.REDSTONE_WIRE.createBlockData());
		if (getAbilityScore() > 1) {
			world.spawnParticle(Particle.SPELL_WITCH, le.getLocation().add(0, le.getHeight() / 2, 0), 10, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65);
			world.spawnParticle(Particle.BLOCK_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 20, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65, Material.REDSTONE_BLOCK.createBlockData());
		}
	}

	@Override
	public boolean runCheck() {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();
		ItemStack oHand = mPlayer.getInventory().getItemInOffHand();
		return InventoryUtils.rogueTriggerCheck(mHand, oHand);
	}
}
