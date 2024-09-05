package com.playmonumenta.plugins.abilities.cleric.paladin;

import com.playmonumenta.plugins.Constants;
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
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class LuminousInfusion extends Ability {
	public static final int DAMAGE_1 = 12;
	public static final int DAMAGE_2 = 16;
	public static final int DAMAGE_UNDEAD_1 = 25;
	public static final int DAMAGE_UNDEAD_2 = 32;
	public static final int INFUSED_HITS = 1;
	public double mLastPassiveDJDamage = 0;

	private static final Set<DamageType> PHYSICAL_DMG_TYPE = Set.of(
		DamageType.MELEE,
		DamageType.MELEE_ENCH,
		DamageType.PROJECTILE
	);
	private static final double RADIUS = 4;
	private static final int FIRE_DURATION_2 = Constants.TICKS_PER_SECOND * 3;
	private static final int COOLDOWN_1 = Constants.TICKS_PER_SECOND * 12;
	private static final int COOLDOWN_2 = Constants.TICKS_PER_SECOND * 10;
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
				("While sneaking, pressing the swap key charges your hands with holy light. " +
					"The next attack or ability you perform against an undead enemy causes a %s block radius explosion " +
					"that deals %d magic damage to it and other undead or %d damage against non-undead. " +
					"Enemies are propelled away from the hit undead. Cooldown: %ss.")
					.formatted(
						RADIUS,
						DAMAGE_UNDEAD_1,
						DAMAGE_1,
						(COOLDOWN_1 / Constants.TICKS_PER_SECOND)
					),
				("The damage is increased to %d against undead and %d against non-undead. " +
					"Undead are passively set on fire for %ss when damaged, but Inferno is applied at %s%% efficiency " +
					"for magic and projectile attacks. Cooldown: %ss.")
					.formatted(
						DAMAGE_UNDEAD_2,
						DAMAGE_2,
						(FIRE_DURATION_2 / Constants.TICKS_PER_SECOND),
						(INFERNO_SCALE * 100),
						(COOLDOWN_2 / Constants.TICKS_PER_SECOND)
					))
			.simpleDescription("Upon activating, the next damage dealt to an Undead enemy causes an explosion.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", LuminousInfusion::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(Material.BLAZE_POWDER);

	private final LuminousInfusionCS mCosmetic;
	private final boolean mIsLevelTwo;
	private boolean mActive = false;
	private int mHits = INFUSED_HITS;
	private @Nullable Crusade mCrusade;

	public LuminousInfusion(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mIsLevelTwo = isLevelTwo();
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
		if (mIsLevelTwo && event.getAbility() == ClassAbility.DIVINE_JUSTICE) {
			mLastPassiveDJDamage = event.getFlatDamage();
			return false;
		}

		boolean enemyTriggersAbilities = Crusade.enemyTriggersAbilities(enemy, mCrusade);

		if (mActive && enemyTriggersAbilities) {
			execute(enemy);
		}

		if (mIsLevelTwo && enemyTriggersAbilities && (PHYSICAL_DMG_TYPE.contains(event.getType()) || (event.getType() == DamageType.MAGIC && event.getAbility() != mInfo.getLinkedSpell()))) {
			if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_ENCH) {
				EntityUtils.applyFire(Plugin.getInstance(), FIRE_DURATION_2, enemy, mPlayer);
			} else {
				// nerf magic/proj flame spreader
				ItemStatManager.PlayerItemStats stats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
				EntityUtils.applyFire(Plugin.getInstance(), FIRE_DURATION_2, enemy, mPlayer, stats, INFERNO_SCALE);
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

		final int undeadDamage = mIsLevelTwo ? DAMAGE_UNDEAD_2 : DAMAGE_UNDEAD_1;
		final int nonUndeadDamage = mIsLevelTwo ? DAMAGE_2 : DAMAGE_1;

		DamageUtils.damage(mPlayer, damagee, DamageType.MAGIC, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, undeadDamage), mInfo.getLinkedSpell(), true);
		mCosmetic.infusionHitEffect(mPlayer.getWorld(), mPlayer, damagee, CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS));
		ClientModHandler.updateAbility(mPlayer, this);

		// Exclude the damagee so that the knockaway is valid
		List<LivingEntity> affected = new Hitbox.SphereHitbox(damagee.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS)).getHitMobs(damagee);
		for (LivingEntity e : affected) {
			// Reduce overall volume of noise the more mobs there are, but still make it louder for more mobs
			double volume = 0.6 / Math.sqrt(affected.size());
			mCosmetic.infusionSpreadEffect(mPlayer.getWorld(), mPlayer, damagee, e, (float) volume);

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
				if (mIsLevelTwo) {
					EntityUtils.applyFire(Plugin.getInstance(), FIRE_DURATION_2, e, mPlayer);
				}
				DamageUtils.damage(mPlayer, e, DamageType.MAGIC, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, undeadDamage), mInfo.getLinkedSpell(), true);
			} else {
				DamageUtils.damage(mPlayer, e, DamageType.MAGIC, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, nonUndeadDamage), mInfo.getLinkedSpell(), true);
				Crusade.addCrusadeTag(e, mCrusade);
			}
			MovementUtils.knockAway(damagee.getLocation(), e, KNOCKBACK_SPEED, KNOCKBACK_SPEED / 2, true);
		}
	}

	@Override
	public @Nullable String getMode() {
		return mActive ? "active" : null;
	}
}
