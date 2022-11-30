package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.CholericFlamesCS;
import com.playmonumenta.plugins.effects.SpreadEffectOnDeath;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class CholericFlames extends Ability {

	private static final int RANGE = 9;
	private static final int DAMAGE_1 = 3;
	private static final int DAMAGE_2 = 5;
	private static final int DURATION = 7 * 20;
	private static final int COOLDOWN = 10 * 20;
	private static final int MAX_DEBUFFS = 3;
	private static final String SPREAD_EFFECT_ON_DEATH_EFFECT = "CholericFlamesSpreadEffectOnDeath";
	private static final int SPREAD_EFFECT_DURATION = 30 * 20;
	private static final int SPREAD_EFFECT_DURATION_APPLIED = 5 * 20;
	private static final double SPREAD_EFFECT_RADIUS = 3;
	public static final float KNOCKBACK = 0.5f;

	public static final String CHARM_DAMAGE = "Choleric Flames Damage";
	public static final String CHARM_RANGE = "Choleric Flames Range";
	public static final String CHARM_COOLDOWN = "Choleric Flames Cooldown";
	public static final String CHARM_DURATION = "Choleric Flames Duration";
	public static final String CHARM_KNOCKBACK = "Choleric Flames Knockback";
	public static final String CHARM_INFERNO_CAP = "Choleric Flames Inferno Cap";
	public static final String CHARM_ENHANCEMENT_RADIUS = "Choleric Flames Enhancement Radius";

	public static final AbilityInfo<CholericFlames> INFO =
		new AbilityInfo<>(CholericFlames.class, "Choleric Flames", CholericFlames::new)
			.linkedSpell(ClassAbility.CHOLERIC_FLAMES)
			.scoreboardId("CholericFlames")
			.shorthandName("CF")
			.descriptions(
				("Sneaking and right-clicking while not looking down while holding a scythe knocks back and ignites mobs within %s blocks of you for %ss, " +
					 "additionally dealing %s magic damage. Cooldown: %ss.")
					.formatted(RANGE, StringUtils.ticksToSeconds(DURATION), DAMAGE_1, StringUtils.ticksToSeconds(COOLDOWN)),
				"The damage is increased to %s, and also afflict mobs with Hunger I."
					.formatted(DAMAGE_2),
				("Mobs ignited by this ability are inflicted with an additional level of Inferno for each debuff they have prior to this ability, up to %s. " +
					 "Additionally, when these mobs die, they explode, applying all Inferno they have at the time of death to all mobs within a %s block radius for %ss.")
					.formatted(MAX_DEBUFFS, SPREAD_EFFECT_RADIUS, StringUtils.ticksToSeconds(SPREAD_EFFECT_DURATION_APPLIED)))
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CholericFlames::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_SCYTHE_RESTRICTION))
			.displayItem(new ItemStack(Material.FIRE_CHARGE, 1));

	private final double mDamage;

	private final CholericFlamesCS mCosmetic;

	public CholericFlames(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CholericFlamesCS(), CholericFlamesCS.SKIN_LIST);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		double range = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mCosmetic.flameEffects(mPlayer, mPlayer.getWorld(), mPlayer.getLocation(), range);

		int maxDebuffs = (int) (MAX_DEBUFFS + CharmManager.getLevel(mPlayer, CHARM_INFERNO_CAP));
		double spreadRadius = CharmManager.getRadius(mPlayer, CHARM_ENHANCEMENT_RADIUS, SPREAD_EFFECT_RADIUS);
		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), range);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell(), true);
			MovementUtils.knockAway(mPlayer, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK));

			// Gets a copy so modifying the inferno level does not have effect elsewhere
			ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
			if (isEnhanced()) {
				int debuffs = Math.min(AbilityUtils.getDebuffCount(mPlugin, mob), maxDebuffs);
				if (debuffs > 0) {
					playerItemStats.getItemStats().add(ItemStatUtils.EnchantmentType.INFERNO.getItemStat(), debuffs);
				}
				mPlugin.mEffectManager.addEffect(mob, SPREAD_EFFECT_ON_DEATH_EFFECT, new SpreadEffectOnDeath(SPREAD_EFFECT_DURATION, Inferno.INFERNO_EFFECT_NAME, spreadRadius, SPREAD_EFFECT_DURATION_APPLIED, false));
			}

			int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
			EntityUtils.applyFire(mPlugin, duration, mob, mPlayer, playerItemStats);
			if (isLevelTwo()) {
				PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(PotionEffectType.HUNGER, duration, 0, false, true));
			}
		}

		putOnCooldown();
	}

}
