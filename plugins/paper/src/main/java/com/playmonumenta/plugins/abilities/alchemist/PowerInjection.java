package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class PowerInjection extends Ability {
	private static final int POWER_INJECTION_RANGE = 16;
	private static final int POWER_INJECTION_SELF_STRENGTH_AMP = 0;
	private static final int POWER_INJECTION_1_STRENGTH_AMP = 1;
	private static final int POWER_INJECTION_2_STRENGTH_AMP = 2;
	private static final int POWER_INJECTION_SPEED_AMP = 0;
	private static final int POWER_INJECTION_DURATION = 20 * 15;
	private static final int POWER_INJECTION_COOLDOWN = 20 * 30;
	private static final Particle.DustOptions PI_COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.2f);

	private final int mStrengthAmp;

	private Player mTargetPlayer;

	public PowerInjection(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Power Injection");
		mInfo.linkedSpell = Spells.POWER_INJECTION;
		mInfo.scoreboardId = "PowerInjection";
		mInfo.mShorthandName = "PI";
		mInfo.mDescriptions.add("Left-clicking, while looking at another player within 16 blocks, while holding an Alchemist Potion gives that player 15 seconds of Strength II. If you left click while looking down, given yourself Strength I instead. Cooldown 30s");
		mInfo.mDescriptions.add("The buff is increased to Strength III and Speed I, and self buffs increased to Strength I and Speed 1.");
		mInfo.cooldown = POWER_INJECTION_COOLDOWN;
		mInfo.trigger = AbilityTrigger.LEFT_CLICK;
		mStrengthAmp = getAbilityScore() == 1 ? POWER_INJECTION_1_STRENGTH_AMP : POWER_INJECTION_2_STRENGTH_AMP;
	}

	@Override
	public void cast(Action action) {
		Location loc = mPlayer.getEyeLocation();

		// Self cast
		if (mTargetPlayer == null) {
			mWorld.spawnParticle(Particle.FLAME, loc, 15, 0.4, 0.45, 0.4, 0.025);
			mWorld.spawnParticle(Particle.SPELL_INSTANT, loc, 50, 0.25, 0.45, 0.25, 0.001);
			mWorld.spawnParticle(Particle.REDSTONE, loc, 45, 0.4, 0.45, 0.4, PI_COLOR);
			mWorld.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.2f, 1.25f);
			mWorld.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.1f);

			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_OTHER,
					new PotionEffect(PotionEffectType.INCREASE_DAMAGE, POWER_INJECTION_DURATION, POWER_INJECTION_SELF_STRENGTH_AMP, false, true));
			if (getAbilityScore() > 1) {
				mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_OTHER,
						new PotionEffect(PotionEffectType.SPEED, POWER_INJECTION_DURATION, POWER_INJECTION_SPEED_AMP, false, true));
			}

			putOnCooldown();
			return;
		}

		Vector dir = loc.getDirection();
		for (int i = 0; i < 50; i++) {
			loc.add(dir.clone().multiply(0.5));
			mWorld.spawnParticle(Particle.CRIT_MAGIC, loc, 5, 0.2, 0.2, 0.2, 0.35);
			mWorld.spawnParticle(Particle.CRIT, loc, 1, 0.2, 0.2, 0.2, 0.35);
			mWorld.spawnParticle(Particle.FLAME, loc, 2, 0.11, 0.11, 0.11, 0.025);

			if (loc.distance(mTargetPlayer.getLocation().add(0, 1, 0)) < 1.25) {
				break;
			}
		}
		Location tLoc = mTargetPlayer.getLocation().add(0, 1, 0);
		mWorld.spawnParticle(Particle.FLAME, tLoc, 15, 0.4, 0.45, 0.4, 0.025);
		mWorld.spawnParticle(Particle.SPELL_INSTANT, tLoc, 50, 0.25, 0.45, 0.25, 0.001);
		mWorld.spawnParticle(Particle.REDSTONE, tLoc, 45, 0.4, 0.45, 0.4, PI_COLOR);
		mWorld.playSound(tLoc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.2f, 1.25f);
		mWorld.playSound(tLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.1f);

		Location pLoc = mPlayer.getLocation();
		mWorld.playSound(pLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 1.25f);
		mWorld.playSound(pLoc, Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.75f);
		mWorld.spawnParticle(Particle.FLAME, mPlayer.getEyeLocation().add(mPlayer.getLocation().getDirection().multiply(0.75)), 20, 0, 0, 0, 0.25);

		mPlugin.mPotionManager.addPotion(mTargetPlayer, PotionID.ABILITY_OTHER,
				new PotionEffect(PotionEffectType.INCREASE_DAMAGE, POWER_INJECTION_DURATION, mStrengthAmp, false, true));
		if (getAbilityScore() > 1) {
			mPlugin.mPotionManager.addPotion(mTargetPlayer, PotionID.ABILITY_OTHER,
					new PotionEffect(PotionEffectType.SPEED, POWER_INJECTION_DURATION, POWER_INJECTION_SPEED_AMP, false, true));
		}

		mTargetPlayer = null;
		putOnCooldown();
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.testForItemWithName(inMainHand, "Alchemist's Potion")) {
			LivingEntity targetEntity = EntityUtils.getEntityAtCursor(mPlayer, POWER_INJECTION_RANGE, true, true, true);
			if (targetEntity != null && targetEntity instanceof Player && ((Player) targetEntity).getGameMode() != GameMode.SPECTATOR) {
				mTargetPlayer = (Player) targetEntity;
				return true;
			} else {
				// If player is not looking at someone, check if they're looking down
				return mPlayer.getLocation().getPitch() > 70;
			}
		}

		return false;
	}

}
