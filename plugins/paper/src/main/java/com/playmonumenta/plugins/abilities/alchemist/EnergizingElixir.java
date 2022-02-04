package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemUtils;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;



public class EnergizingElixir extends Ability {

	private static final int COOLDOWN = 2 * 20;
	private static final int DURATION = 6 * 20;
	private static final double SPEED_AMPLIFIER_1 = 0.1;
	private static final double SPEED_AMPLIFIER_2 = 0.2;
	private static final String PERCENT_SPEED_EFFECT_NAME = "EnergizingElixirPercentSpeedEffect";
	private static final int JUMP_LEVEL = 1;
	private static final double DAMAGE_AMPLIFIER_2 = 0.1;
	private static final String PERCENT_DAMAGE_EFFECT_NAME = "EnergizingElixirPercentDamageEffect";

	private final double mSpeedAmp;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable UnstableAmalgam mUnstableAmalgam;

	public EnergizingElixir(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Energizing Elixir");
		mInfo.mLinkedSpell = ClassAbility.ENERGIZING_ELIXIR;
		mInfo.mScoreboardId = "EnergizingElixir";
		mInfo.mShorthandName = "EE";
		mInfo.mDescriptions.add("Left click while holding an Alchemist's Bag to consume a potion to apply 10% Speed and Jump Boost 2 to yourself for 6s. Cooldown: 2s.");
		mInfo.mDescriptions.add("Speed is increased to 20%; additionally, gain a 10% damage buff from all sources for the same duration.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.RABBIT_FOOT, 1);

		mSpeedAmp = getAbilityScore() == 1 ? SPEED_AMPLIFIER_1 : SPEED_AMPLIFIER_2;
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mAlchemistPotions = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
			mUnstableAmalgam = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, UnstableAmalgam.class);
		});
	}

	@Override
	public void cast(Action action) {
		if (mPlayer != null
			&& ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand())
			&& !(mUnstableAmalgam != null && mPlayer.isSneaking())
			&& (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
			if (mAlchemistPotions != null) {
				if (!mAlchemistPotions.decrementCharge()) {
					// If no charges, do not activate ability
					return;
				}
			}

			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, mSpeedAmp, PERCENT_SPEED_EFFECT_NAME));
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.JUMP, DURATION, JUMP_LEVEL));
			if (getAbilityScore() > 1) {
				mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_EFFECT_NAME, new PercentDamageDealt(DURATION, DAMAGE_AMPLIFIER_2));
			}

			World world = mPlayer.getWorld();
			Location loc = mPlayer.getLocation();
			world.spawnParticle(Particle.TOTEM, loc, 50, 1.5, 1, 1.5, 0);
			world.playSound(loc, Sound.BLOCK_LAVA_EXTINGUISH, 1, 0);

			putOnCooldown();
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}
}
