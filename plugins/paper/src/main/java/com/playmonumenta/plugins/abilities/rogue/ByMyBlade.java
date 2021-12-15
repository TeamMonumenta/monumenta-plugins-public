package com.playmonumenta.plugins.abilities.rogue;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;



public class ByMyBlade extends Ability {

	public static class ByMyBladeHasteEnchantment extends BaseAbilityEnchantment {
		public ByMyBladeHasteEnchantment() {
			super("By My Blade Haste Level", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}
	}

	public static class ByMyBladeDamageEnchantment extends BaseAbilityEnchantment {
		public ByMyBladeDamageEnchantment() {
			super("By My Blade Damage", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}
	}

	public static class ByMyBladeCooldownEnchantment extends BaseAbilityEnchantment {
		public ByMyBladeCooldownEnchantment() {
			super("By My Blade Cooldown", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

	public static class ByMyBladeDurationEnchantment extends BaseAbilityEnchantment {
		public ByMyBladeDurationEnchantment() {
			super("By My Blade Haste Duration", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}
	}

	private static final int BY_MY_BLADE_1_HASTE_AMPLIFIER = 1;
	private static final int BY_MY_BLADE_2_HASTE_AMPLIFIER = 3;
	private static final int BY_MY_BLADE_HASTE_DURATION = 4 * 20;
	private static final int BY_MY_BLADE_1_DAMAGE = 12;
	private static final int BY_MY_BLADE_2_DAMAGE = 24;
	private static final int BY_MY_BLADE_COOLDOWN = 10 * 20;

	private final int mDamageBonus;

	public ByMyBlade(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "By My Blade");
		mInfo.mLinkedSpell = ClassAbility.BY_MY_BLADE;
		mInfo.mScoreboardId = "ByMyBlade";
		mInfo.mShorthandName = "BmB";
		mInfo.mDescriptions.add("While holding two swords, attacking an enemy with a critical attack deals 12 more damage, and grants you Haste 2 for 4s. Cooldown: 10s.");
		mInfo.mDescriptions.add("Damage is increased from 12 to 24. Haste level is increased from 2 to 4.");
		mInfo.mCooldown = BY_MY_BLADE_COOLDOWN;
		mDisplayItem = new ItemStack(Material.SKELETON_SKULL, 1);
		mDamageBonus = getAbilityScore() == 1 ? BY_MY_BLADE_1_DAMAGE : BY_MY_BLADE_2_DAMAGE;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mPlayer != null && event.getCause() == DamageCause.ENTITY_ATTACK) {
			int duration = BY_MY_BLADE_HASTE_DURATION + (int) ByMyBladeDurationEnchantment.getExtraDuration(mPlayer, ByMyBladeDurationEnchantment.class);
			int hasteAmplifier = getAbilityScore() == 1 ? BY_MY_BLADE_1_HASTE_AMPLIFIER : BY_MY_BLADE_2_HASTE_AMPLIFIER;
			hasteAmplifier += ByMyBladeHasteEnchantment.getLevel(mPlayer, ByMyBladeHasteEnchantment.class);
			int extraDamage = mDamageBonus + (int) ByMyBladeDamageEnchantment.getExtraDamage(mPlayer, ByMyBladeDamageEnchantment.class);

			LivingEntity damagee = (LivingEntity) event.getEntity();
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.FAST_DIGGING, duration, hasteAmplifier, false, true));

			// Since RoguePassive uses a Custom Damage Event, I'll just put the modifier here
			if (EntityUtils.isElite(damagee)) {
				extraDamage *= RoguePassive.PASSIVE_DAMAGE_ELITE_MODIFIER;
			} else if (EntityUtils.isBoss(damagee)) {
				extraDamage *= RoguePassive.PASSIVE_DAMAGE_BOSS_MODIFIER;
			}

			event.setDamage(event.getDamage() + extraDamage);

			Location loc = damagee.getLocation();
			World world = mPlayer.getWorld();
			loc.add(0, 1, 0);
			int count = 15;
			if (getAbilityScore() > 1) {
				world.spawnParticle(Particle.SPELL_WITCH, loc, 45, 0.2, 0.65, 0.2, 1.0);
				count = 30;
				if (damagee instanceof Player) {
					MovementUtils.knockAway(mPlayer, damagee, 0.3f);
					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
					                                 new PotionEffect(PotionEffectType.SPEED, BY_MY_BLADE_HASTE_DURATION, 0, false, true));
				}
			}
			world.spawnParticle(Particle.SPELL_MOB, loc, count, 0.25, 0.5, 0.5, 0.001);
			world.spawnParticle(Particle.CRIT, loc, 30, 0.25, 0.5, 0.5, 0.001);
			world.playSound(loc, Sound.ITEM_SHIELD_BREAK, 2.0f, 0.5f);
			putOnCooldown();
		}
		return true;
	}

	@Override
	public boolean runCheck() {
		if (mPlayer != null && PlayerUtils.isFallingAttack(mPlayer)) {
			ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
			ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
			return InventoryUtils.rogueTriggerCheck(mainHand, offHand);
		}
		return false;
	}

	@Override
	public Class<? extends BaseAbilityEnchantment> getCooldownEnchantment() {
		return ByMyBladeCooldownEnchantment.class;
	}
}
