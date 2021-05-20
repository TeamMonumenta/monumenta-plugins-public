package com.playmonumenta.plugins.abilities.alchemist;

import java.util.EnumSet;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.FlatDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

public class PowerInjection extends Ability {

	private static final String FLAT_DAMAGE_DEALT_EFFECT_NAME = "PowerInjectionFlatDamageDealtEffect";
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "PowerInjectionPercentDamageDealtEffect";
	private static final String PERCENT_SPEED_EFFECT_NAME = "PowerInjectionPercentSpeedEffect";
	private static final int DURATION = 20 * 15;
	private static final int FLAT_DAMAGE_DEALT_EFFECT_1 = 1;
	private static final int FLAT_DAMAGE_DEALT_EFFECT_2 = 3;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT_1 = 0.15;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT_2 = 0.3;
	private static final double PERCENT_SPEED_EFFECT = 0.15;
	private static final EnumSet<DamageCause> AFFECTED_DAMAGE_CAUSES = EnumSet.of(
			DamageCause.ENTITY_ATTACK,
			DamageCause.ENTITY_SWEEP_ATTACK
	);

	private static final int COOLDOWN = 20 * 30;
	private static final int CAST_RANGE = 16;

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(150, 0, 0), 1.2f);

	private final int mFlatDamageDealtEffect;
	private final double mPercentDamageDealtEffect;

	private Player mTargetPlayer;

	public PowerInjection(Plugin plugin, Player player) {
		super(plugin, player, "Power Injection");
		mInfo.mLinkedSpell = ClassAbility.POWER_INJECTION;
		mInfo.mScoreboardId = "PowerInjection";
		mInfo.mShorthandName = "PI";
		mInfo.mDescriptions.add("Left-clicking while holding an Alchemist Potion gives you +15% speed, +1 melee damage, and +15% melee damage for 15 seconds. If you were looking at another player, also give that player the effects. Cooldown: 30s.");
		mInfo.mDescriptions.add("Give +3 melee damage and +30% melee damage instead.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mFlatDamageDealtEffect = getAbilityScore() == 1 ? FLAT_DAMAGE_DEALT_EFFECT_1 : FLAT_DAMAGE_DEALT_EFFECT_2;
		mPercentDamageDealtEffect = getAbilityScore() == 1 ? PERCENT_DAMAGE_DEALT_EFFECT_1 : PERCENT_DAMAGE_DEALT_EFFECT_2;
	}

	@Override
	public void cast(Action action) {
		applyEffects(mPlayer);

		if (mTargetPlayer != null) {
			applyEffects(mTargetPlayer);

			Location loc = mPlayer.getEyeLocation();
			World world = mPlayer.getWorld();
			Vector dir = loc.getDirection();
			for (int i = 0; i < 50; i++) {
				loc.add(dir.clone().multiply(0.5));
				world.spawnParticle(Particle.CRIT_MAGIC, loc, 5, 0.2, 0.2, 0.2, 0.35);
				world.spawnParticle(Particle.CRIT, loc, 1, 0.2, 0.2, 0.2, 0.35);
				world.spawnParticle(Particle.FLAME, loc, 2, 0.11, 0.11, 0.11, 0.025);

				if (loc.distance(mTargetPlayer.getLocation().add(0, 1, 0)) < 1.25) {
					break;
				}
			}

			Location locPlayer = mPlayer.getEyeLocation();
			world.playSound(locPlayer, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 1.25f);
			world.playSound(locPlayer, Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.75f);
			world.spawnParticle(Particle.FLAME, locPlayer.add(locPlayer.getDirection().multiply(0.75)), 20, 0, 0, 0, 0.25);

			mTargetPlayer = null;
		}

		putOnCooldown();
	}

	private void applyEffects(Player player) {
		Location loc = player.getLocation().add(0, 1, 0);
		World world = mPlayer.getWorld();
		world.spawnParticle(Particle.FLAME, loc, 15, 0.4, 0.45, 0.4, 0.025);
		world.spawnParticle(Particle.SPELL_INSTANT, loc, 50, 0.25, 0.45, 0.25, 0.001);
		world.spawnParticle(Particle.REDSTONE, loc, 45, 0.4, 0.45, 0.4, COLOR);
		world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.2f, 1.25f);
		world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.1f);

		mPlugin.mEffectManager.addEffect(player, FLAT_DAMAGE_DEALT_EFFECT_NAME, new FlatDamageDealt(DURATION, mFlatDamageDealtEffect, AFFECTED_DAMAGE_CAUSES));
		mPlugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(DURATION, mPercentDamageDealtEffect, AFFECTED_DAMAGE_CAUSES));
		mPlugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED_EFFECT, PERCENT_SPEED_EFFECT_NAME));
	}

	@Override
	public boolean runCheck() {
		ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		if (InventoryUtils.testForItemWithName(inMainHand, "Alchemist's Potion")) {
			LivingEntity targetEntity = EntityUtils.getEntityAtCursor(mPlayer, CAST_RANGE, true, true, true);
			if (targetEntity instanceof Player && ((Player) targetEntity).getGameMode() != GameMode.SPECTATOR) {
				mTargetPlayer = (Player) targetEntity;
			}

			return true;
		}

		return false;
	}

}
