package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.MagmaShieldCS;
import com.playmonumenta.plugins.effects.PercentAbilityDamageReceived;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class MagmaShield extends Ability {

	public static final String NAME = "Magma Shield";

	public static final int DAMAGE_1 = 6;
	public static final int DAMAGE_2 = 12;
	private static final int HEIGHT = 6;
	private static final int RADIUS = 7;
	public static final int FIRE_SECONDS = 6;
	public static final int FIRE_TICKS = FIRE_SECONDS * 20;
	public static final float KNOCKBACK = 0.5f;
	// 70° on each side of look direction for XZ-plane (flattened Y),
	// so 140° angle of effect
	private static final int ANGLE = 70;
	public static final int COOLDOWN_SECONDS = 12;
	public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;
	public static final float ENHANCEMENT_FIRE_DAMAGE_BONUS = 0.5f;
	public static final float ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS = 0.35f;
	public static final String ENHANCEMENT_FIRE_DAMAGE_BONUS_EFFECT_NAME = "MagmaShieldFireDamageBonus";
	public static final String ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS_EFFECT_NAME = "MagmaShieldFireAbilityDamageBonus";
	public static final int ENHANCEMENT_BONUS_DURATION = 6 * 20;

	public static final String CHARM_DAMAGE = "Magma Shield Damage";
	public static final String CHARM_RANGE = "Magma Shield Range";
	public static final String CHARM_COOLDOWN = "Magma Shield Cooldown";
	public static final String CHARM_DURATION = "Magma Shield Fire Duration";
	public static final String CHARM_KNOCKBACK = "Magma Shield Knockback";
	public static final String CHARM_CONE = "Magma Shield Cone";

	public static final AbilityInfo<MagmaShield> INFO =
		new AbilityInfo<>(MagmaShield.class, NAME, MagmaShield::new)
			.linkedSpell(ClassAbility.MAGMA_SHIELD)
			.scoreboardId("Magma")
			.shorthandName("MS")
			.descriptions(
				String.format(
					"While sneaking, right-clicking with a wand summons a torrent of flames, dealing %s fire magic damage to all enemies in front of you within %s blocks," +
						" setting them on fire for %ss, and knocking them away. The damage ignores iframes. Cooldown: %ss.",
					DAMAGE_1,
					RADIUS,
					FIRE_SECONDS,
					COOLDOWN_SECONDS
				),
				String.format(
					"Damage is increased from %s to %s.",
					DAMAGE_1,
					DAMAGE_2
				),
				String.format(
					"Enemies hit by this ability take %s%% extra damage by fire and %s%% by fire based abilities for %ss.",
					(int) (100 * ENHANCEMENT_FIRE_DAMAGE_BONUS),
					(int) (100 * ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS),
					ENHANCEMENT_BONUS_DURATION / 20
				))
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", MagmaShield::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(new ItemStack(Material.MAGMA_CREAM, 1));

	private final float mLevelDamage;

	private final MagmaShieldCS mCosmetic;

	public MagmaShield(Plugin plugin, @Nullable Player player) {
		super(plugin, player, INFO);

		mLevelDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new MagmaShieldCS(), MagmaShieldCS.SKIN_LIST);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		putOnCooldown();

		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
		double radius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RADIUS);
		double angle = Math.min(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CONE, ANGLE), 180);
		Hitbox hitbox = Hitbox.approximateCylinderSegment(
			LocationUtils.getHalfHeightLocation(mPlayer).add(0, -HEIGHT, 0), 2 * HEIGHT, radius, Math.toRadians(angle));
		for (LivingEntity target : hitbox.getHitMobs()) {
			EntityUtils.applyFire(mPlugin, FIRE_TICKS + CharmManager.getExtraDuration(mPlayer, CHARM_DURATION), target, mPlayer);
			DamageUtils.damage(mPlayer, target, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, false);
			MovementUtils.knockAway(mPlayer, target, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK), true);
			if (isEnhanced()) {
				mPlugin.mEffectManager.addEffect(target, ENHANCEMENT_FIRE_DAMAGE_BONUS_EFFECT_NAME,
					new PercentDamageReceived(ENHANCEMENT_BONUS_DURATION, ENHANCEMENT_FIRE_DAMAGE_BONUS, EnumSet.of(DamageType.FIRE)));
				mPlugin.mEffectManager.addEffect(target, ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS_EFFECT_NAME,
					new PercentAbilityDamageReceived(ENHANCEMENT_BONUS_DURATION, ENHANCEMENT_FIRE_ABILITY_DAMAGE_BONUS,
						EnumSet.of(ClassAbility.MAGMA_SHIELD, ClassAbility.ELEMENTAL_ARROWS_FIRE, ClassAbility.ELEMENTAL_SPIRIT_FIRE,
							ClassAbility.STARFALL, ClassAbility.CHOLERIC_FLAMES)));
			}
		}

		mCosmetic.magmaEffects(mPlayer.getWorld(), mPlayer, radius, angle);
	}

}
