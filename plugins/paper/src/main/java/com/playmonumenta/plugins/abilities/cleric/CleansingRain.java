package com.playmonumenta.plugins.abilities.cleric;

import java.util.EnumSet;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.enchantments.BaseAbilityEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class CleansingRain extends Ability {

	public static class CleansingRainCooldownEnchantment extends BaseAbilityEnchantment {
		public CleansingRainCooldownEnchantment() {
			super("Cleansing Rain Cooldown", EnumSet.of(ItemSlot.ARMOR));
		}
	}

	public static class CleansingRainRadiusEnchantment extends BaseAbilityEnchantment {
		public CleansingRainRadiusEnchantment() {
			super("Cleansing Rain Range", EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND, ItemSlot.ARMOR));
		}
	}

	private static final int CLEANSING_DURATION = 15 * 20;
	private static final int CLEANSING_RESIST_LEVEL = 0;
	private static final int CLEANSING_EFFECT_DURATION = 3 * 20;
	private static final int CLEANSING_APPLY_PERIOD = 1;
	private static final int CLEANSING_RADIUS = 4;
	private static final int CLEANSING_1_COOLDOWN = 45 * 20;
	private static final int CLEANSING_2_COOLDOWN = 30 * 20;
	private static final double CLEANSING_ANGLE = 50.0;

	public CleansingRain(Plugin plugin, Player player) {
		super(plugin, player, "Cleansing Rain");
		mInfo.mLinkedSpell = Spells.CLEANSING;
		mInfo.mScoreboardId = "Cleansing";
		mInfo.mShorthandName = "CR";
		mInfo.mDescriptions.add("Right click while sneaking and looking upwards to summon a \"cleansing rain\" that follows you, removing negative effects from players within 4 blocks, including yourself, and lasts for 15 seconds. (Cooldown: 45 seconds)");
		mInfo.mDescriptions.add("Additionally grants Resistance I. Cooldown: 30s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? CLEANSING_1_COOLDOWN : CLEANSING_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {
		int cd = getAbilityScore() == 1 ? CLEANSING_1_COOLDOWN : CLEANSING_2_COOLDOWN;
		mInfo.mCooldown = (int) CleansingRainCooldownEnchantment.getCooldown(mPlayer, cd, CleansingRainCooldownEnchantment.class);
		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.45f, 0.8f);
		putOnCooldown();

		int cleansing = getAbilityScore();

		// Run cleansing rain here until it finishes
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mPlayer.getWorld().spawnParticle(Particle.CLOUD, mPlayer.getLocation().add(0, 4, 0), 5, 2.5, 0.35, 2.5, 0);
				mPlayer.getWorld().spawnParticle(Particle.WATER_DROP, mPlayer.getLocation().add(0, 2, 0), 15, 2.5, 2, 2.5, 0.001);
				mPlayer.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, mPlayer.getLocation().add(0, 2, 0), 1, 2, 1.5, 2, 0.001);

				float radius = CleansingRainRadiusEnchantment.getRadius(mPlayer, CLEANSING_RADIUS, CleansingRainRadiusEnchantment.class);

				for (Player player : PlayerUtils.playersInRange(mPlayer, radius, true)) {
					PotionUtils.clearNegatives(mPlugin, player);

					if (player.getFireTicks() > 1) {
						player.setFireTicks(1);
					}

					if (cleansing > 1) {
						mPlugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, CLEANSING_EFFECT_DURATION, CLEANSING_RESIST_LEVEL, true, true));
					}
				}

				mTicks += CLEANSING_APPLY_PERIOD;
				if (mTicks > CLEANSING_DURATION) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, CLEANSING_APPLY_PERIOD);
	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		return mPlayer.isSneaking()
				&& mPlayer.getLocation().getPitch() < -CLEANSING_ANGLE
				&& mainHand.getType() != Material.BOW
				&& offHand.getType() != Material.BOW;
	}

}
