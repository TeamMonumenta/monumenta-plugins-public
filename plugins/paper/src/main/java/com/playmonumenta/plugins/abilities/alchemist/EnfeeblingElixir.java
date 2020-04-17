package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class EnfeeblingElixir extends Ability {
	private static final int ENFEEBLING_1_COOLDOWN = 20 * 15;
	private static final int ENFEEBLING_2_COOLDOWN = 20 * 10;
	private static final int ENFEEBLING_DURATION = 5 * 20;
	private static final float ENFEEBLING_1_KNOCKBACK_SPEED = 0.35f;
	private static final float ENFEEBLING_2_KNOCKBACK_SPEED = 0.5f;
	private static final int ENFEEBLING_1_WEAKNESS_AMP = 0;
	private static final int ENFEEBLING_2_WEAKNESS_AMP = 1;
	private static final int ENFEEBLING_1_SPEED_AMP = 0;
	private static final int ENFEEBLING_2_SPEED_AMP = 1;
	private static final int ENFEEBLING_JUMP_LEVEL = 1;
	private static final int ENFEEBLING_RADIUS = 3;

	private final int mWeaknessAmp;
	private final int mSpeedAmp;
	private final float mKnockbackSpeed;

	public EnfeeblingElixir(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Enfeebling Elixir");
		mInfo.linkedSpell = Spells.ENFEEBLING_ELIXIR;
		mInfo.scoreboardId = "EnfeeblingElixir";
		mInfo.mShorthandName = "EE";
		mInfo.mDescriptions.add("When you crouch and attack a mob or left click, all mobs within 3 blocks are knocked back several blocks and gain Weakness I for 5s. You gain Jump Boost II and Speed 1 for 5s. Cooldown: 15s.");
		mInfo.mDescriptions.add("The knockback increases by 50% and Weakness I and Speed I are both increased to II. Cooldown: 10s.");
		mInfo.cooldown = getAbilityScore() == 1 ? ENFEEBLING_1_COOLDOWN : ENFEEBLING_2_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mWeaknessAmp = getAbilityScore() == 1 ? ENFEEBLING_1_WEAKNESS_AMP : ENFEEBLING_2_WEAKNESS_AMP;
		mSpeedAmp = getAbilityScore() == 1 ? ENFEEBLING_1_SPEED_AMP : ENFEEBLING_2_SPEED_AMP;
		mKnockbackSpeed = getAbilityScore() == 1 ? ENFEEBLING_1_KNOCKBACK_SPEED : ENFEEBLING_2_KNOCKBACK_SPEED;
	}

	@Override
	public void cast(Action action) {
		ItemStack mHand = mPlayer.getInventory().getItemInMainHand();

		if (mHand.getType() != Material.BOW && mHand.getType() != Material.SPLASH_POTION) {
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), ENFEEBLING_RADIUS, mPlayer)) {
				MovementUtils.knockAway(mPlayer, mob, mKnockbackSpeed);
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.WEAKNESS, ENFEEBLING_DURATION, mWeaknessAmp, true, false));
			}

			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.SPEED, ENFEEBLING_DURATION, mSpeedAmp));
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.JUMP, ENFEEBLING_DURATION, ENFEEBLING_JUMP_LEVEL));

			mWorld.spawnParticle(Particle.SPELL_MOB, mPlayer.getLocation(), 100, 2, 1.5, 2, 0);
			mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1, 0);
			putOnCooldown();
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK && event.getEntity() instanceof LivingEntity) {
			cast(Action.LEFT_CLICK_AIR);
		}

		return true;
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking();
	}

}
