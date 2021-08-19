package com.playmonumenta.plugins.depths.abilities.windwalker;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;

import net.md_5.bungee.api.ChatColor;

public class LastBreath extends DepthsAbility {
	public static final String ABILITY_NAME = "Last Breath";
	public static final int COOLDOWN = 90 * 20;
	private static final int TRIGGER_HEALTH = 6;
	public static final double[] COOLDOWN_REDUCTION = {0.4, 0.5, 0.6, 0.7, 0.8};
	private static final double[] SPEED = {0.1, 0.125, 0.15, 0.175, 0.2};
	private static final int SPEED_DURATION = 6 * 20;
	private static final String SPEED_EFFECT_NAME = "LastBreathSpeedEffect";
	public static final int RADIUS = 5;
	public static final double KNOCKBACK_SPEED = 2;

	private List<ClassAbility> mWindAbilities = new ArrayList<ClassAbility>();

	public LastBreath(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.DRAGON_BREATH;
		mTree = DepthsTree.WINDWALKER;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.LAST_BREATH;

		mWindAbilities.add(ClassAbility.GUARDING_BOLT);
		mWindAbilities.add(ClassAbility.SKYHOOK);
		mWindAbilities.add(ClassAbility.SLIPSTREAM);
		mWindAbilities.add(ClassAbility.UPDRAFT);
		mWindAbilities.add(ClassAbility.WHIRLWIND);
		mWindAbilities.add(ClassAbility.HOWLINGWINDS);
	}

	@Override
	public boolean playerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return true;
		}

		execute(event);
		return true;
	}

	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return true;
		}

		execute(event);
		return true;
	}

	/*
	 * Works against all types of damage
	 */
	@Override
	public boolean playerDamagedEvent(EntityDamageEvent event) {
		// Do not process cancelled damage events
		if (event.isCancelled() || event instanceof EntityDamageByEntityEvent) {
			return true;
		}

		execute(event);
		return true;
	}

	private void execute(EntityDamageEvent event) {
		if (AbilityUtils.isBlocked(event)) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		double healthRemaining = mPlayer.getHealth() + AbsorptionUtils.getAbsorption(mPlayer) - EntityUtils.getRealFinalDamage(event);

		if (healthRemaining > TRIGGER_HEALTH) {
			return;
		}

		putOnCooldown();

		for (Ability abil : AbilityManager.getManager().getPlayerAbilities(mPlayer).getAbilities()) {
			AbilityInfo info = abil.getInfo();
			ClassAbility spell = info.mLinkedSpell;
			if (spell == mInfo.mLinkedSpell) {
				continue;
			}
			int totalCD = info.mCooldown;
			int reducedCD = 0;
			if (mWindAbilities.contains(spell)) {
				reducedCD = totalCD;
			} else {
				reducedCD = (int) (totalCD * COOLDOWN_REDUCTION[mRarity - 1]);
			}
			mPlugin.mTimers.updateCooldown(mPlayer, spell, reducedCD);
		}

		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();

		mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT_NAME, new PercentSpeed(SPEED_DURATION, SPEED[mRarity - 1], SPEED_EFFECT_NAME));
		for (LivingEntity e : EntityUtils.getNearbyMobs(loc, RADIUS)) {
			Vector knockback = e.getVelocity().add(e.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(KNOCKBACK_SPEED));
			knockback.setY(knockback.getY() * 2);
			e.setVelocity(knockback.add(new Vector(0, 0.25, 0)));
			world.spawnParticle(Particle.EXPLOSION_NORMAL, e.getLocation(), 5, 0, 0, 0, 0.35);
		}

		world.spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 20, 1.25, 1.25, 1.25);
		world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 20, 1.25, 1.25, 1.25);
		world.spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(0, 1, 0), 20, 1.25, 1.25, 1.25);

		world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.2f);
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 2.0f, 0.4f);

		MessagingUtils.sendActionBarMessage(mPlugin, mPlayer, "Last Breath has been activated!");
	}

	@Override
	public String getDescription(int rarity) {
		return "When your health drops below " + TRIGGER_HEALTH / 2 + " hearts, all your other Windwalker abilities' cooldowns are reset, and abilities from other trees have their cooldowns reduced by " + DepthsUtils.getRarityColor(rarity) + (int) DepthsUtils.roundPercent(COOLDOWN_REDUCTION[rarity - 1]) + "%" + ChatColor.WHITE + ". You gain " + DepthsUtils.getRarityColor(rarity) + DepthsUtils.roundPercent(SPEED[rarity - 1]) + "%" + ChatColor.WHITE + " speed for " + SPEED_DURATION / 20 + " seconds and mobs within " + RADIUS + " blocks are knocked away. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.WINDWALKER;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.LIFELINE;
	}
}
