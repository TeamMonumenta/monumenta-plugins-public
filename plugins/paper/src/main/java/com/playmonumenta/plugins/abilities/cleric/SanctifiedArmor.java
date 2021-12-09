package com.playmonumenta.plugins.abilities.cleric;

import java.util.EnumSet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;


public class SanctifiedArmor extends Ability {
	public static class SanctifiedArmorDamageEnchantment extends BaseAbilityEnchantment {
		public SanctifiedArmorDamageEnchantment() {
			super("Sanctified Armor Damage", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

	private static final double PERCENT_DAMAGE_RETURNED_1 = 1.5;
	private static final double PERCENT_DAMAGE_RETURNED_2 = 2.5;
	private static final double SLOWNESS_AMPLIFIER_2 = 0.2;
	private static final int SLOWNESS_DURATION = 20 * 3;
	private static final float KNOCKBACK_SPEED = 0.4f;

	private final double mPercentDamageReturned;

	private @Nullable Crusade mCrusade;

	public SanctifiedArmor(Plugin plugin, Player player) {
		super(plugin, player, "Sanctified Armor");
		mInfo.mLinkedSpell = ClassAbility.SANCTIFIED_ARMOR;
		mInfo.mScoreboardId = "Sanctified";
		mInfo.mShorthandName = "Sa";
		mInfo.mDescriptions.add("Whenever a non-boss undead enemy hits you with a melee or projectile attack, it takes 1.5 times the final damage you took and is knocked away from you.");
		mInfo.mDescriptions.add("Deal 2.5 times the final damage instead, and the undead enemy is also afflicted with 20% Slowness for 3 seconds (even if you are blocking).");
		mPercentDamageReturned = getAbilityScore() == 1 ? PERCENT_DAMAGE_RETURNED_1 : PERCENT_DAMAGE_RETURNED_2;
		mDisplayItem = new ItemStack(Material.IRON_CHESTPLATE, 1);

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, Crusade.class);
			});
		}
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		LivingEntity mob = (LivingEntity) event.getDamager();
		if (event.getCause() == DamageCause.ENTITY_ATTACK && Crusade.enemyTriggersAbilities(mob, mCrusade) && !EntityUtils.isBoss(mob)) {
			trigger(mob, event);
		}

		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		ProjectileSource source = ((Projectile) event.getDamager()).getShooter();
		if (source instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) source;
			if (
				Crusade.enemyTriggersAbilities(mob, mCrusade)
				&& !EntityUtils.isBoss(mob)
			) {
				trigger(mob, event);
			}
		}

		return true;
	}

	private void trigger(LivingEntity mob, EntityDamageByEntityEvent event) {
		Location loc = mob.getLocation();
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.FIREWORKS_SPARK, loc.add(0, mob.getHeight() / 2, 0), 7, 0.35, 0.35, 0.35, 0.125);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.7f, 1.2f);

		double damage = mPercentDamageReturned * EntityUtils.getRealFinalDamage(event);
		damage = SanctifiedArmorDamageEnchantment.getExtraPercentDamage(mPlayer, SanctifiedArmorDamageEnchantment.class, (float) damage);
		MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED, KNOCKBACK_SPEED);
		if (!mPlayer.isBlocking() || event.getFinalDamage() > 0) {
			EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer, MagicType.HOLY, true, mInfo.mLinkedSpell);
		}

		if (getAbilityScore() > 1) {
			EntityUtils.applySlow(mPlugin, SLOWNESS_DURATION, SLOWNESS_AMPLIFIER_2, mob);
		}
	}
}
