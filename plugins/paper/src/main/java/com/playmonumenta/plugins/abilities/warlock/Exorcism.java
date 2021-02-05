package com.playmonumenta.plugins.abilities.warlock;

import java.util.ArrayList;
import java.util.List;

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

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Exorcism  extends Ability {

	private static final int RANGE = 12;
	private static final int DURATION = 20 * 15;
	private static final int AMPLIFIER_CAP = 1;
	private static final int COOLDOWN_1 = 20 * 25;
	private static final int COOLDOWN_2 = 20 * 15;
	private static final double EXORCISM_ANGLE = 50.0;

	public Exorcism(Plugin plugin, Player player) {
		super(plugin, player, "Exorcism");
		mInfo.mLinkedSpell = Spells.EXORCISM;
		mInfo.mScoreboardId = "Exorcism";
		mInfo.mShorthandName = "Ex";
		mInfo.mDescriptions.add("Right click while looking down without sneaking removes all your debuffs and applies them to enemies within 12 blocks of you. Slowness is converted to 10% Slowness per level. Level of debuffs is preserved up to level 2. Cooldown: 25s.");
		mInfo.mDescriptions.add("Also apply the corresponding debuff to enemies for every buff you have. Cooldown: 15s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	private final List<PotionEffect> mDebuffs = new ArrayList<>();

	@Override
	public void cast(Action action) {
		//	needs better sound
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.7f, -3f);

		putOnCooldown();

		boolean onFire = false;
		for (PotionEffect effect : mPlayer.getActivePotionEffects()) {
			if (PotionUtils.hasNegativeEffects(effect.getType())) {
				mDebuffs.add(effect.getType().createEffect(DURATION, Math.min(AMPLIFIER_CAP, effect.getAmplifier())));
			} else if (getAbilityScore() > 1) {
				if (effect.getType().equals(PotionEffectType.SPEED)) {
					mDebuffs.add(new PotionEffect(PotionEffectType.SLOW, DURATION, Math.min(AMPLIFIER_CAP, effect.getAmplifier())));
				} else if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
					mDebuffs.add(new PotionEffect(PotionEffectType.WEAKNESS, DURATION, Math.min(AMPLIFIER_CAP, effect.getAmplifier())));
				} else if (effect.getType().equals(PotionEffectType.REGENERATION)) {
					mDebuffs.add(new PotionEffect(PotionEffectType.WITHER, DURATION, Math.min(AMPLIFIER_CAP, effect.getAmplifier())));
				} else if (effect.getType().equals(PotionEffectType.FAST_DIGGING)) {
					mDebuffs.add(new PotionEffect(PotionEffectType.SLOW_DIGGING, DURATION, Math.min(AMPLIFIER_CAP, effect.getAmplifier())));
				} else if (effect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE)) {
					mDebuffs.add(new PotionEffect(PotionEffectType.UNLUCK, DURATION, (Math.min(AMPLIFIER_CAP, effect.getAmplifier())*2) + 1));
				} else if (effect.getType().equals(PotionEffectType.FIRE_RESISTANCE)) {
					onFire = true;
				}
			}
		}
		PotionUtils.clearNegatives(mPlugin, mPlayer);
		if (mPlayer.getFireTicks() > 1) {
			onFire = true;
			mPlayer.setFireTicks(1);
		}

		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RANGE)) {
			for (PotionEffect debuff : mDebuffs) {
				// Custom slow interaction
				if (debuff.getType() == PotionEffectType.SLOW) {
					double slowLevel = debuff.getAmplifier() >= 1 ? 0.2 : 0.1;
					EntityUtils.applySlow(mPlugin, DURATION, slowLevel, mob);
				} else {
					PotionUtils.applyPotion(mPlayer, mob, debuff);
				}
			}
			if (onFire) {
				EntityUtils.applyFire(mPlugin, DURATION, mob, mPlayer);
			}
			world.spawnParticle(Particle.SQUID_INK, mob.getLocation(), 40, 0.1, 0.2, 0.1, 0.15);
		}

		// a cool particle effect on the player would be nice too
		mDebuffs.clear();

	}

	@Override
	public boolean runCheck() {
		ItemStack offHand = mPlayer.getInventory().getItemInOffHand();
		return !mPlayer.isSneaking()
		       && mPlayer.getLocation().getPitch() > EXORCISM_ANGLE
		       && InventoryUtils.isScytheItem(mPlayer.getInventory().getItemInMainHand())
		       && offHand.getType() != Material.BOW
		       && (!mPlayer.getActivePotionEffects().isEmpty()
		           || (mPlayer.getFireTicks() > 1));
	}

}
