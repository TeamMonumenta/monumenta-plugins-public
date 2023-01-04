package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.PrismaticShieldCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PrismaticShield extends Ability {

	private static final float RADIUS = 4.0f;
	private static final int TRIGGER_HEALTH = 6;
	private static final int OVERKILL_PROTECTION_MULTIPLIER = 4;
	private static final int ABSORPTION_HEALTH_1 = 4;
	private static final int ABSORPTION_HEALTH_2 = 8;
	private static final int DURATION = 12 * 20;
	private static final int COOLDOWN = 90 * 20;
	private static final float KNOCKBACK_SPEED = 0.7f;
	private static final int STUN_DURATION = 20;
	private static final int HEAL_DURATION = 5 * 20;
	private static final int HEAL_PERCENT = 5;
	private static final float DAMAGE_BUFF_PERCENT = 30;
	private static final String DAMAGE_BUFF_NAME = "PrismaticShieldDamageBuff";
	private static final String HEALED_THIS_TICK_METAKEY = "PrismaticShieldHealedThisTick";

	public static final String CHARM_ABSORPTION = "Prismatic Shield Absorption Health";
	public static final String CHARM_COOLDOWN = "Prismatic Shield Cooldown";
	public static final String CHARM_KNOCKBACK = "Prismatic Shield Knockback";
	public static final String CHARM_STUN = "Prismatic Shield Stun Duration";
	public static final String CHARM_DURATION = "Prismatic Shield Absorption Duration";
	public static final String CHARM_TRIGGER = "Prismatic Shield Trigger Health";

	public static final AbilityInfo<PrismaticShield> INFO =
		new AbilityInfo<>(PrismaticShield.class, "Prismatic Shield", PrismaticShield::new)
			.linkedSpell(ClassAbility.PRISMATIC_SHIELD)
			.scoreboardId("Prismatic")
			.shorthandName("PS")
			.descriptions(
				String.format("When your health drops below %s hearts you receive %s Absorption hearts which lasts up to %ss." +
					              " If damage taken would kill you but could have been prevented by up to %s times this skill's absorption, it will save you from death." +
					              " In addition enemies within %s blocks are knocked back. Cooldown: %ss.",
					TRIGGER_HEALTH / 2,
					ABSORPTION_HEALTH_1 / 2,
					DURATION / 20,
					OVERKILL_PROTECTION_MULTIPLIER,
					(int) RADIUS,
					COOLDOWN / 20
				),
				String.format("The shield is improved to %s Absorption hearts. Enemies within %s blocks are knocked back and stunned for %ss.",
					ABSORPTION_HEALTH_2 / 2,
					(int) RADIUS,
					STUN_DURATION / 20),
				String.format("After Prismatic Shield is activated, in the next %ss, you deal %s%% more damage and every spell that deals damage to at least one enemy will heal you for %s%% of your max health.",
					HEAL_DURATION / 20,
					DAMAGE_BUFF_PERCENT,
					HEAL_PERCENT)
			)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(new ItemStack(Material.SHIELD, 1))
			.priorityAmount(10000);

	private final int mAbsorptionHealth;

	private int mLastActivation = -1;
	private final Set<ClassAbility> mHealedFromAbilitiesThisTick = new HashSet<>();
	private boolean mHealedFromBlizzard = false;

	private final PrismaticShieldCS mCosmetic;

	public PrismaticShield(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAbsorptionHealth = (int) CharmManager.calculateFlatAndPercentValue(player, CHARM_ABSORPTION, isLevelOne() ? ABSORPTION_HEALTH_1 : ABSORPTION_HEALTH_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new PrismaticShieldCS(), PrismaticShieldCS.SKIN_LIST);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!event.isBlocked() && !isOnCooldown() && !event.isCancelled() && event.getType() != DamageEvent.DamageType.TRUE) {
			// Calculate whether this effect should not be run based on player health.
			// It is intentional that Prismatic Shield saves you from death if you take a buttload of damage somehow.
			double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

			// Health is less than 0 but does not penetrate the absorption shield
			boolean dealDamageLater = healthRemaining < 0 && healthRemaining > -OVERKILL_PROTECTION_MULTIPLIER * (mAbsorptionHealth + 1);

			if (healthRemaining <= CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_TRIGGER, TRIGGER_HEALTH)) {
				mPlugin.mEffectManager.damageEvent(event);
				event.setLifelineCancel(true);
				if (event.isCancelled() || event.isBlocked()) {
					return;
				}

				if (dealDamageLater) {
					event.setCancelled(true);
				}

				// Put on cooldown before processing results to prevent infinite recursion
				putOnCooldown();
				mLastActivation = Bukkit.getServer().getCurrentTick();
				mHealedFromAbilitiesThisTick.clear();
				mHealedFromBlizzard = false;

				// Conditions match - prismatic shield
				Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), RADIUS);
				for (LivingEntity mob : hitbox.getHitMobs()) {
					float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK_SPEED);
					MovementUtils.knockAway(mPlayer, mob, knockback, true);
					if (isLevelTwo()) {
						EntityUtils.applyStun(mPlugin, CharmManager.getDuration(mPlayer, CHARM_STUN, STUN_DURATION), mob);
						mCosmetic.prismaOnStun(mob, STUN_DURATION, mPlayer);
					}
				}

				AbsorptionUtils.addAbsorption(mPlayer, mAbsorptionHealth, mAbsorptionHealth, CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION));
				World world = mPlayer.getWorld();
				mCosmetic.prismaEffect(world, mPlayer, RADIUS);
				MessagingUtils.sendActionBarMessage(mPlayer, "Prismatic Shield has been activated");

				if (dealDamageLater) {
					mPlayer.setHealth(1);
					AbsorptionUtils.subtractAbsorption(mPlayer, 1 - (float) healthRemaining);
				}
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
			    && Bukkit.getServer().getCurrentTick() <= mLastActivation + HEAL_DURATION
			    && event.getAbility() != null
			    && !event.getAbility().isFake()
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
			mPlugin.mEffectManager.addEffect(mPlayer, DAMAGE_BUFF_NAME, new PercentDamageDealt(HEAL_DURATION, DAMAGE_BUFF_PERCENT / 100));
			PlayerUtils.healPlayer(mPlugin, mPlayer, HEAL_PERCENT / 100.0 * EntityUtils.getMaxHealth(mPlayer));
			mCosmetic.prismaOnHeal(mPlayer);
		}
		return false; // there may be multiple spells cast in the same tick, need to check them all. No recursion possible as this doesn't deal damage.
	}
}
