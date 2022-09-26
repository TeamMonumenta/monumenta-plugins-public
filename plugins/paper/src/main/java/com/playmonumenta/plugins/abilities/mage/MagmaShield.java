package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.MagmaShieldCS;
import com.playmonumenta.plugins.effects.PercentAbilityDamageReceived;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.EnumSet;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class MagmaShield extends Ability {

	public static final String NAME = "Magma Shield";
	public static final ClassAbility ABILITY = ClassAbility.MAGMA_SHIELD;

	public static final int DAMAGE_1 = 6;
	public static final int DAMAGE_2 = 12;
	public static final int SIZE = 6;
	public static final int FIRE_SECONDS = 6;
	public static final int FIRE_TICKS = FIRE_SECONDS * 20;
	public static final float KNOCKBACK = 0.5f;
	// 70° on each side of look direction for XZ-plane (flattened Y),
	// so 140° angle of effect
	public static final int ANGLE = 70;
	public static final int COOLDOWN_SECONDS = 12;
	public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;
	public static final float ENHANCEMENT_FIRE_DAMAGE_BONUS = 0.5f;
	public static final float ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS = 0.2f;
	public static final String ENHANCEMENT_FIRE_DAMAGE_BONUS_EFFECT_NAME = "MagmaShieldFireDamageBonus";
	public static final String ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS_EFFECT_NAME = "MagmaShieldFireAbilityDamageBonus";
	public static final int ENHANCEMENT_BONUS_DURATION = 6 * 20;

	public static final String CHARM_DAMAGE = "Magma Shield Damage";
	public static final String CHARM_RANGE = "Magma Shield Range";
	public static final String CHARM_COOLDOWN = "Magma Shield Cooldown";
	public static final String CHARM_DURATION = "Magma Shield Fire Duration";
	public static final String CHARM_KNOCKBACK = "Magma Shield Knockback";
	public static final String CHARM_CONE = "Magma Shield Cone";

	private final float mLevelDamage;

	private boolean mHasBlizzard;

	private final MagmaShieldCS mCosmetic;

	public MagmaShield(Plugin plugin, @Nullable Player player) {
		super(plugin, player, NAME);
		mInfo.mLinkedSpell = ABILITY;

		mInfo.mScoreboardId = "Magma";
		mInfo.mShorthandName = "MS";
		mInfo.mDescriptions.add(
			String.format(
				"While sneaking, right-clicking with a wand summons a torrent of flames, dealing %s fire magic damage to all enemies in front of you within a %s-block cube around you, setting them on fire for %ss, and knocking them away. The damage ignores iframes. Cooldown: %ss.",
				DAMAGE_1,
				SIZE,
				FIRE_SECONDS,
				COOLDOWN_SECONDS
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Damage is increased from %s to %s.",
				DAMAGE_1,
				DAMAGE_2
			)
		);
		mInfo.mDescriptions.add(
			String.format(
				"Enemies hit by this ability take %s%% extra damage by fire and %s%% by fire based abilities for %ss.",
				(int) (100 * ENHANCEMENT_FIRE_DAMAGE_BONUS),
				(int) (100 * ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS),
				ENHANCEMENT_BONUS_DURATION / 20
			)
		);
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, COOLDOWN_TICKS);
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mDisplayItem = new ItemStack(Material.MAGMA_CREAM, 1);

		mLevelDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new MagmaShieldCS(), MagmaShieldCS.SKIN_LIST);

		mHasBlizzard = false;
		if (ServerProperties.getClassSpecializationsEnabled()) {
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				mHasBlizzard = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Blizzard.class) != null;
			});
		}
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null) {
			return;
		}
		putOnCooldown();

		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
		Vector flattenedLookDirection = mPlayer.getEyeLocation().getDirection().setY(0);
		for (LivingEntity potentialTarget : EntityUtils.getNearbyMobs(mPlayer.getLocation(), CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, SIZE), mPlayer)) {
			Vector flattenedTargetVector = potentialTarget.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0);
			if (
				VectorUtils.isAngleWithin(
					flattenedLookDirection,
					flattenedTargetVector,
					CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CONE, ANGLE)
				)
			) {
				EntityUtils.applyFire(mPlugin, FIRE_TICKS + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION), potentialTarget, mPlayer);
				DamageUtils.damage(mPlayer, potentialTarget, DamageType.MAGIC, damage, mInfo.mLinkedSpell, true, false);
				MovementUtils.knockAway(mPlayer, potentialTarget, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK), true);
				if (isEnhanced()) {
					mPlugin.mEffectManager.addEffect(potentialTarget, ENHANCEMENT_FIRE_DAMAGE_BONUS_EFFECT_NAME,
						new PercentDamageReceived(ENHANCEMENT_BONUS_DURATION, ENHANCEMENT_FIRE_DAMAGE_BONUS, EnumSet.of(DamageType.FIRE)));
					mPlugin.mEffectManager.addEffect(potentialTarget, ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS_EFFECT_NAME,
						new PercentAbilityDamageReceived(ENHANCEMENT_BONUS_DURATION, ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS,
							EnumSet.of(ClassAbility.MAGMA_SHIELD, ClassAbility.ELEMENTAL_ARROWS_FIRE, ClassAbility.ELEMENTAL_SPIRIT_FIRE,
								ClassAbility.STARFALL, ClassAbility.CHOLERIC_FLAMES)));
				}
			}
		}

		World world = mPlayer.getWorld();
		new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;

			@Override
			public void run() {
				if (mRadius == 0) {
					mLoc.setDirection(mPlayer.getLocation().getDirection().setY(0).normalize());
				}
				Vector vec;
				mRadius += 1.25;
				for (double degree = 30; degree <= 150; degree += 10) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * mRadius, 0.125, FastUtils.sin(radian1) * mRadius);
					vec = VectorUtils.rotateXAxis(vec, mLoc.getPitch());
					vec = VectorUtils.rotateYAxis(vec, mLoc.getYaw());

					Location l = mLoc.clone().add(0, 0.1, 0).add(vec);
					mCosmetic.magmaParticle(mPlayer, l);
				}

				if (mRadius >= CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, SIZE + 1)) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);

		mCosmetic.magmaEffects(world, mPlayer);
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && mPlayer.isSneaking() && ItemUtils.isWand(mPlayer.getInventory().getItemInMainHand()) && !(mHasBlizzard && mPlayer.getLocation().getPitch() < Blizzard.ANGLE);
	}
}
