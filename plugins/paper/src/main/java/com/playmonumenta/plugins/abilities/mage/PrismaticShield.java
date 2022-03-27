package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PrismaticShield extends Ability {

	private static final float RADIUS = 4.0f;
	private static final int TRIGGER_HEALTH = 6;
	private static final int ABSORPTION_HEALTH_1 = 4;
	private static final int ABSORPTION_HEALTH_2 = 8;
	private static final int DURATION = 12 * 20;
	private static final int COOLDOWN_1 = 90 * 20;
	private static final int COOLDOWN_2 = 70 * 20;
	private static final float KNOCKBACK_SPEED = 0.7f;
	private static final int STUN_DURATION = 20;
	private static final int HEAL_DURATION = 5 * 20;
	private static final int HEAL_PERCENT = 5;
	private static final String HEALED_THIS_TICK_METAKEY = "PrismaticShieldHealedThisTick";

	private final int mAbsorptionHealth;

	private int mLastActivation = -1;
	private final Set<ClassAbility> mHealedFromAbilitiesThisTick = new HashSet<>();
	private boolean mHealedFromBlizzard = false;

	public PrismaticShield(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Prismatic Shield");
		mInfo.mLinkedSpell = ClassAbility.PRISMATIC_SHIELD;
		mInfo.mScoreboardId = "Prismatic";
		mInfo.mShorthandName = "PS";
		mInfo.mDescriptions.add("When your health drops below 3 hearts (including if the attack would've killed you), you receive 2 Absorption hearts which lasts up to 12 s. In addition enemies within four blocks are knocked back. Cooldown: 90s.");
		mInfo.mDescriptions.add("The shield is improved to 4 Absorption hearts. Enemies within four blocks are knocked back and stunned for 1 s. Cooldown: 70s.");
		mInfo.mDescriptions.add(
			String.format("After Prismatic Shield is activated, in the next %ss, every spell that lands successfully will heal you for %s%% of your max health.",
				HEAL_DURATION / 20,
				HEAL_PERCENT)
		);
		mInfo.mCooldown = isLevelOne() ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mIgnoreCooldown = true;
		mAbsorptionHealth = isLevelOne() ? ABSORPTION_HEALTH_1 : ABSORPTION_HEALTH_2;
		mDisplayItem = new ItemStack(Material.SHIELD, 1);
	}

	@Override
	public double getPriorityAmount() {
		return 10000;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!event.isBlocked() && mPlayer != null && !isTimerActive()) {
			// Calculate whether this effect should not be run based on player health.
			// It is intentional that Prismatic Shield saves you from death if you take a buttload of damage somehow.
			double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

			// Health is less than 0 but does not penetrate the absorption shield
			boolean dealDamageLater = healthRemaining < 0 && healthRemaining > -4 * (mAbsorptionHealth + 1);


			if (healthRemaining > TRIGGER_HEALTH) {
				return;
			} else if (dealDamageLater) {
				// The player has taken fatal damage BUT will be saved by the absorption, so set damage to 0 and compensate later
				event.setCancelled(true);
			}

			// Put on cooldown before processing results to prevent infinite recursion
			putOnCooldown();
			mLastActivation = mPlayer.getTicksLived();
			mHealedFromAbilitiesThisTick.clear();
			mHealedFromBlizzard = false;

			// Conditions match - prismatic shield
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), RADIUS, mPlayer)) {
				MovementUtils.knockAway(mPlayer, mob, KNOCKBACK_SPEED, true);
				if (isLevelTwo()) {
					EntityUtils.applyStun(mPlugin, STUN_DURATION, mob);
				}
			}

			AbsorptionUtils.addAbsorption(mPlayer, mAbsorptionHealth, mAbsorptionHealth, DURATION);
			World world = mPlayer.getWorld();
			new PartialParticle(Particle.FIREWORKS_SPARK, mPlayer.getLocation().add(0, 1.15, 0), 150, 0.2, 0.35, 0.2, 0.5).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation().add(0, 1.15, 0), 100, 0.2, 0.35, 0.2, 1).spawnAsPlayerActive(mPlayer);
			world.playSound(mPlayer.getLocation(), Sound.ITEM_TOTEM_USE, 1, 1.35f);
			MessagingUtils.sendActionBarMessage(mPlayer, "Prismatic Shield has been activated");

			if (dealDamageLater) {
				mPlayer.setHealth(1);
				AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) healthRemaining);
			}
		}
	}

	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (isEnhanced()
			    && mPlayer != null
			    && mPlayer.getTicksLived() <= mLastActivation + HEAL_DURATION
			    && event.getAbility() != null
			    && event.getAbility() != ClassAbility.SPELLSHOCK
			    && event.getAbility() != ClassAbility.ASTRAL_OMEN) {
			if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, HEALED_THIS_TICK_METAKEY)) {
				// new tick, clear abilities encountered this tick
				mHealedFromAbilitiesThisTick.clear();
			}
			if (!mHealedFromAbilitiesThisTick.add(event.getAbility())) {
				// already healed for this ability this tick, abort
				return false;
			}
			if (event.getAbility() == ClassAbility.BLIZZARD) {
				// Blizzard needs special handling as it deals damage multiple times. Only one damage tick can heal over the entire duration.
				// Elementalist should not be able to re-cast Blizzard quickly, so this should also be a correct limit to number of casts (1).
				if (mHealedFromBlizzard) {
					return false;
				}
				mHealedFromBlizzard = true;
			}
			PlayerUtils.healPlayer(mPlugin, mPlayer, HEAL_PERCENT / 100.0 * EntityUtils.getMaxHealth(mPlayer));
		}
		return false; // there may be multiple spells cast in the same tick, need to check them all. No recursion possible as this doesn't deal damage.
	}
}
