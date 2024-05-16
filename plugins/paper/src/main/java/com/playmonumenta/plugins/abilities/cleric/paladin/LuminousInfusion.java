package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.paladin.LuminousInfusionCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;


public class LuminousInfusion extends Ability {
	public static final int DAMAGE_1 = 12;
	public static final int DAMAGE_UNDEAD_1 = 25;
	public static final double DAMAGE_MULTIPLIER_2 = 0.15;
	public static final int INFUSED_HITS = 1;

	private final boolean mDoMultiplierAndFire;

	// Passive damage to share with Holy Javelin
	public double mLastPassiveMeleeDamage = 0;
	public double mLastPassiveDJDamage = 0;

	private static final double RADIUS = 4;
	private static final int FIRE_DURATION_2 = 20 * 3;
	private static final int COOLDOWN = 20 * 12;
	private static final float KNOCKBACK_SPEED = 0.7f;
	private static final double INFERNO_SCALE = 0.5;

	public static final String CHARM_DAMAGE = "Luminous Infusion Damage";
	public static final String CHARM_COOLDOWN = "Luminous Infusion Cooldown";
	public static final String CHARM_RADIUS = "Luminous Infusion Radius";
	public static final String CHARM_HITS = "Luminous Infusion Infused Hits";

	public static final AbilityInfo<LuminousInfusion> INFO =
		new AbilityInfo<>(LuminousInfusion.class, "Luminous Infusion", LuminousInfusion::new)
			.linkedSpell(ClassAbility.LUMINOUS_INFUSION)
			.scoreboardId("LuminousInfusion")
			.shorthandName("LI")
			.descriptions(
				"While sneaking, pressing the swap key charges your hands with holy light. " +
					"The next attack or ability you perform against an undead enemy is infused with explosive power, " +
					"dealing 25 magic damage to it and all other undead enemies in a 4 block radius around it, or 12 against non-undead, " +
					"and knocking other enemies away from it. Cooldown: 12s.",
				"Your melee attacks now passively deal 15% magic damage to undead enemies, " +
					"and Divine Justice now passively deals 15% more total damage. " +
					"Damaging an undead enemy now passively sets it on fire for 3s. " +
					"Applies inferno at 50% efficiency for magic and projectile attacks.")
			.simpleDescription("Upon activating, the next damage dealt to an Undead enemy causes an explosion.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", LuminousInfusion::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(Material.BLAZE_POWDER);

	private boolean mActive = false;
	private int mHits = INFUSED_HITS;
	private final LuminousInfusionCS mCosmetic;

	private @Nullable Crusade mCrusade;

	public LuminousInfusion(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDoMultiplierAndFire = isLevelTwo();

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new LuminousInfusionCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Crusade.class);
		});
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		mActive = true;
		mHits = INFUSED_HITS + (int) CharmManager.getLevel(mPlayer, CHARM_HITS);
		putOnCooldown();

		World world = mPlayer.getWorld();
		mCosmetic.infusionStartEffect(world, mPlayer, mPlayer.getLocation());

		cancelOnDeath(new BukkitRunnable() {
			int mT = 0;
			final int EXPIRE_TICKS = getModifiedCooldown();

			@Override
			public void run() {
				mT++;
				mCosmetic.infusionTickEffect(mPlayer, mT);
				if (mT >= EXPIRE_TICKS || (!mActive && mHits <= 0)) {
					mActive = false;
					if (mT >= EXPIRE_TICKS) {
						mCosmetic.infusionExpireMsg(mPlayer);
						ClientModHandler.updateAbility(mPlayer, LuminousInfusion.this);
					}
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 1));
		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		// Divine Justice integration
		if (mDoMultiplierAndFire && event.getAbility() == ClassAbility.DIVINE_JUSTICE) {
			double originalDamage = event.getDamage();
			mLastPassiveDJDamage = originalDamage * DAMAGE_MULTIPLIER_2;
			event.setDamage(originalDamage + mLastPassiveDJDamage);
			return false;
		}

		boolean enemyTriggersAbilities = Crusade.enemyTriggersAbilities(enemy, mCrusade);

		if (mActive && enemyTriggersAbilities) {
			execute(enemy);
		}

		if (mDoMultiplierAndFire && (event.getType() == DamageType.MELEE || event.getType() == DamageType.PROJECTILE || event.getType() == DamageType.MELEE_ENCH || (event.getType() == DamageType.MAGIC && event.getAbility() != mInfo.getLinkedSpell())) && enemyTriggersAbilities) {
			if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_ENCH) {
				EntityUtils.applyFire(Plugin.getInstance(), FIRE_DURATION_2, enemy, mPlayer);
			} else {
				// nerf magic/proj flame spreader
				ItemStatManager.PlayerItemStats stats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
				EntityUtils.applyFire(Plugin.getInstance(), FIRE_DURATION_2, enemy, mPlayer, stats, INFERNO_SCALE);
			}

			if (event.getType() != DamageType.MAGIC && event.getType() != DamageType.PROJECTILE) {
				double originalDamage = event.getDamage();
				// Store the raw pre-event damage.
				// When it is used by Holy Javelin later,
				// the custom damage event will fire including this raw damage,
				// then event processing runs for it from there
				mLastPassiveMeleeDamage = originalDamage * DAMAGE_MULTIPLIER_2;
				DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, mLastPassiveMeleeDamage, mInfo.getLinkedSpell(), true);
			}
		}
		return false;
	}

	public void execute(LivingEntity damagee) {
		mHits--;
		mActive = false;

		// if there are hits remaining, turn back on after a tick to prevent looping
		if (mHits > 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					mActive = true;
				}
			}.runTaskLater(Plugin.getInstance(), 1);
		}

		ClientModHandler.updateAbility(mPlayer, this);

		DamageUtils.damage(mPlayer, damagee, DamageType.MAGIC, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_UNDEAD_1), mInfo.getLinkedSpell(), true);

		Location loc = damagee.getLocation();
		World world = mPlayer.getWorld();
		mCosmetic.infusionHitEffect(world, mPlayer, damagee);

		// Exclude the damagee so that the knockaway is valid
		List<LivingEntity> affected = new Hitbox.SphereHitbox(loc, CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS)).getHitMobs(damagee);
		for (LivingEntity e : affected) {
			// Reduce overall volume of noise the more mobs there are, but still make it louder for more mobs
			double volume = 0.6 / Math.sqrt(affected.size());
			mCosmetic.infusionSpreadEffect(world, mPlayer, damagee, e, (float) volume);

			if (Crusade.enemyTriggersAbilities(e, mCrusade)) {
				/*
				 * Annoying thing to fix eventually: there's some stuff with how the AbilityManager
				 * currently works (to infinite loop safe against certain abilities like Brute Force)
				 * where only one damage event per tick is counted. This means that there's not really
				 * a self-contained way for Luminous Infusion level 2 to make AoE abilities light all
				 * enemies on fire (instead of just the first hit) without some restructuring, which
				 * is planned (but I have no time to do that right now). Luckily, the only multi-hit
				 * abilities Paladin has are Holy Javelin (already lights things on fire) and this,
				 * and so the fire, though it should be generically applied to all abilities, is
				 * hard coded for Luminous Infusion level 2 and has the same effect, so this workaround
				 * will be in place until the AbilityManager gets restructured.
				 * - Rubiks
				 */
				if (mDoMultiplierAndFire) {
					EntityUtils.applyFire(Plugin.getInstance(), FIRE_DURATION_2, e, mPlayer);
				}
				DamageUtils.damage(mPlayer, e, DamageType.MAGIC, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_UNDEAD_1), mInfo.getLinkedSpell(), true);
			} else {
				DamageUtils.damage(mPlayer, e, DamageType.MAGIC, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE_1), mInfo.getLinkedSpell(), true);
				Crusade.addCrusadeTag(e, mCrusade);
			}
			MovementUtils.knockAway(loc, e, KNOCKBACK_SPEED, KNOCKBACK_SPEED / 2, true);
		}
	}

	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}
}
