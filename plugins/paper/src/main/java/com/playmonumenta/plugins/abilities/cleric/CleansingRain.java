package com.playmonumenta.plugins.abilities.cleric;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.enchantments.abilities.BaseAbilityEnchantment;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;



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
	private static final double PERCENT_DAMAGE_RESIST = -0.2;
	private static final int CLEANSING_EFFECT_DURATION = 3 * 20;
	private static final int CLEANSING_APPLY_PERIOD = 1;
	private static final int CLEANSING_RADIUS = 4;
	private static final int CLEANSING_1_COOLDOWN = 45 * 20;
	private static final int CLEANSING_2_COOLDOWN = 30 * 20;
	private static final int ANGLE = -45; // Looking straight up is -90. This is 45 degrees of pitch allowance
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "CleansingPercentDamageResistEffect";

	public CleansingRain(Plugin plugin, Player player) {
		super(plugin, player, "Cleansing Rain");
		mInfo.mLinkedSpell = ClassAbility.CLEANSING_RAIN;
		mInfo.mScoreboardId = "Cleansing";
		mInfo.mShorthandName = "CR";
		mInfo.mDescriptions.add("Right click while sneaking and looking upwards to summon a \"cleansing rain\" that follows you, removing negative effects from players within 4 blocks, including yourself, and lasts for 15 seconds. (Cooldown: 45 seconds)");
		mInfo.mDescriptions.add("Additionally grants 20% Damage Reduction to all players in the radius. Cooldown: 30s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? CLEANSING_1_COOLDOWN : CLEANSING_2_COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {
		int cd = getAbilityScore() == 1 ? CLEANSING_1_COOLDOWN : CLEANSING_2_COOLDOWN;
		mInfo.mCooldown = (int) CleansingRainCooldownEnchantment.getCooldown(mPlayer, cd, CleansingRainCooldownEnchantment.class);
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.45f, 0.8f);
		putOnCooldown();

		int cleansing = getAbilityScore();

		// Run cleansing rain here until it finishes
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				world.spawnParticle(Particle.CLOUD, mPlayer.getLocation().add(0, 4, 0), 5, 2.5, 0.35, 2.5, 0);
				world.spawnParticle(Particle.WATER_DROP, mPlayer.getLocation().add(0, 2, 0), 15, 2.5, 2, 2.5, 0.001);
				world.spawnParticle(Particle.VILLAGER_HAPPY, mPlayer.getLocation().add(0, 2, 0), 1, 2, 1.5, 2, 0.001);

				float radius = CleansingRainRadiusEnchantment.getRadius(mPlayer, CLEANSING_RADIUS, CleansingRainRadiusEnchantment.class);

				for (Player player : PlayerUtils.playersInRange(mPlayer, radius, true)) {
					PotionUtils.clearNegatives(mPlugin, player);

					if (player.getFireTicks() > 1) {
						player.setFireTicks(1);
					}

					if (cleansing > 1) {
						mPlugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(CLEANSING_EFFECT_DURATION, PERCENT_DAMAGE_RESIST));
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
				&& mPlayer.getLocation().getPitch() < ANGLE
				&& mainHand.getType() != Material.BOW
				&& offHand.getType() != Material.BOW;
	}
}
