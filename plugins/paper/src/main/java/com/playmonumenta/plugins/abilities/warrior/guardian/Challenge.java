package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.List;

public class Challenge extends Ability {

	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "ChallengePercentDamageDealtEffect";
	private static final int DURATION = 20 * 10;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT_1 = 0.15;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT_2 = 0.3;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
			DamageType.MELEE
	);

	private static final int ABSORPTION_PER_MOB_1 = 1;
	private static final int ABSORPTION_PER_MOB_2 = 2;
	private static final int MAX_ABSORPTION_1 = 4;
	private static final int MAX_ABSORPTION_2 = 8;
	private static final int CHALLENGE_RANGE = 12;
	private static final int COOLDOWN = 20 * 20;

	private final double mPercentDamageDealtEffect;
	private final int mAbsorptionPerMob;
	private final int mMaxAbsorption;

	public Challenge(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Challenge");
		mInfo.mScoreboardId = "Challenge";
		mInfo.mShorthandName = "Ch";
		mInfo.mDescriptions.add("Left-clicking while sneaking makes all enemies within 12 blocks target you. You gain 1 Absorption per affected mob (up to 4 Absorption) for 10 seconds and +15% melee damage for 10 seconds. Cooldown: 20s.");
		mInfo.mDescriptions.add("You gain 2 Absorption per affected mob (up to 8 Absorption) and +30% melee damage instead.");
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mInfo.mLinkedSpell = ClassAbility.CHALLENGE;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.IRON_AXE, 1);
		mPercentDamageDealtEffect = getAbilityScore() == 1 ? PERCENT_DAMAGE_DEALT_EFFECT_1 : PERCENT_DAMAGE_DEALT_EFFECT_2;
		mAbsorptionPerMob = getAbilityScore() == 1 ? ABSORPTION_PER_MOB_1 : ABSORPTION_PER_MOB_2;
		mMaxAbsorption = getAbilityScore() == 1 ? MAX_ABSORPTION_1 : MAX_ABSORPTION_2;
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null || mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell) || !mPlayer.isSneaking()) {
			return;
		}

		Location loc = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 1);
		world.spawnParticle(Particle.FLAME, loc, 25, 0.4, 1, 0.4, 0.7f);
		loc.add(0, 1.25, 0);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 250, 0, 0, 0, 0.425);
		world.spawnParticle(Particle.CRIT, loc, 300, 0, 0, 0, 1);
		world.spawnParticle(Particle.CRIT_MAGIC, loc, 300, 0, 0, 0, 1);

		List<LivingEntity> mobs = EntityUtils.getNearbyMobs(mPlayer.getLocation(), CHALLENGE_RANGE, mPlayer);
		AbsorptionUtils.addAbsorption(mPlayer, mAbsorptionPerMob * mobs.size(), mMaxAbsorption, DURATION);
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(DURATION, mPercentDamageDealtEffect, AFFECTED_DAMAGE_TYPES));

		for (LivingEntity mob : mobs) {
			if (mob instanceof Mob) {
				EntityUtils.applyTaunt(mPlugin, mob, mPlayer);
			}
		}

		putOnCooldown();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			cast(Action.LEFT_CLICK_AIR);
		}
		return false;
	}
}
