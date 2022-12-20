package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.GloriousBattleCS;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class GloriousBattle extends Ability implements AbilityWithChargesOrStacks {
	private static final int DAMAGE_1 = 20;
	private static final int DAMAGE_2 = 25;
	private static final double RADIUS = 3;
	private static final double BLEED_PERCENT = 0.2;
	private static final int BLEED_TIME = 4 * 20;
	private static final float KNOCK_AWAY_SPEED = 0.4f;
	private static final String KBR_EFFECT = "GloriousBattleKnockbackResistanceEffect";
	private static final double DAMAGE_PER = 0.05;
	private static final int MAX_TARGETING = 6;
	private static final double TARGET_RANGE = 10;

	private static final EnumSet<ClassAbility> AFFECTED_ABILITIES = EnumSet.of(
		ClassAbility.BRUTE_FORCE_AOE,
		ClassAbility.COUNTER_STRIKE_AOE,
		ClassAbility.SHIELD_BASH_AOE,
		ClassAbility.METEOR_SLAM,
		ClassAbility.RAMPAGE);

	public static final String CHARM_CHARGES = "Glorious Battle Charges";
	public static final String CHARM_DAMAGE = "Glorious Battle Damage";
	public static final String CHARM_RADIUS = "Glorious Battle Radius";
	public static final String CHARM_BLEED_AMPLIFIER = "Glorious Battle Bleed Amplifier";
	public static final String CHARM_BLEED_DURATION = "Glorious Battle Bleed Duration";
	public static final String CHARM_VELOCITY = "Glorious Battle Velocity";
	public static final String CHARM_KNOCKBACK = "Glorious Battle Knockback";
	public static final String CHARM_DAMAGE_MODIFIER = "Glorious Battle Damage Modifier";

	public static final AbilityInfo<GloriousBattle> INFO =
		new AbilityInfo<>(GloriousBattle.class, "Glorious Battle", GloriousBattle::new)
			.linkedSpell(ClassAbility.GLORIOUS_BATTLE)
			.scoreboardId("GloriousBattle")
			.shorthandName("GB")
			.descriptions(
				("Dealing indirect damage with an ability grants you a Glorious Battle stack. " +
					 "Shift and swap hands to consume a stack and charge forwards, gaining full knockback resistance until landing. " +
					 "When you land, deal %s damage to the nearest mob within %s blocks and apply %s%% bleed for %s seconds. " +
					 "Additionally, knock back all mobs within %s blocks.")
					.formatted(DAMAGE_1, RADIUS, StringUtils.multiplierToPercentage(BLEED_PERCENT), StringUtils.ticksToSeconds(BLEED_TIME), RADIUS),
				"Damage increased to %s. Additionally, you now passively gain %s%% melee damage for each mob targeting you within %s blocks, up to %s mobs."
					.formatted(DAMAGE_2, StringUtils.multiplierToPercentage(DAMAGE_PER), TARGET_RANGE, MAX_TARGETING))
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", GloriousBattle::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(new ItemStack(Material.IRON_SWORD, 1));

	private int mStacks;
	private final int mStackLimit;
	private final int mSpellDelay = 10;
	private final double mDamage;
	private @Nullable BukkitRunnable mRunnable;
	private final GloriousBattleCS mCosmetic;

	public GloriousBattle(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mStacks = 0;
		mStackLimit = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GloriousBattleCS(), GloriousBattleCS.SKIN_LIST);
	}

	public void cast() {
		if (mStacks < 1 || ZoneUtils.hasZoneProperty(mPlayer.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return;
		}

		mStacks--;
		Vector dir = mPlayer.getLocation().getDirection();
		dir.multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, 1.05));
		dir.setY(dir.getY() * 0.4 + 0.3);
		mPlayer.setVelocity(dir);
		mPlugin.mEffectManager.addEffect(mPlayer, KBR_EFFECT, new PercentKnockbackResist(200, 1, KBR_EFFECT).displaysTime(false));
		ClientModHandler.updateAbility(mPlayer, this);
		Location location = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		mCosmetic.gloryStart(world, mPlayer, location, mSpellDelay);

		if (mRunnable != null) {
			mRunnable.cancel();
		}

		mRunnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;
				mCosmetic.gloryTick(mPlayer, mT);
				if (mPlayer.isOnGround()) {
					mPlugin.mEffectManager.clearEffects(mPlayer, KBR_EFFECT);
					double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
					Location location = mPlayer.getLocation();
					World world = mPlayer.getWorld();
					mCosmetic.gloryOnLand(world, mPlayer, location, radius);
					List<LivingEntity> mobs = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), radius).getHitMobs();
					mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

					LivingEntity nearest = EntityUtils.getNearestMob(location, mobs);
					if (nearest == null) {
						this.cancel();
						return;
					}

					EntityUtils.applyBleed(mPlugin, CharmManager.getDuration(mPlayer, CHARM_BLEED_DURATION, BLEED_TIME), BLEED_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BLEED_AMPLIFIER), nearest);
					DamageUtils.damage(mPlayer, nearest, DamageType.MELEE_SKILL, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mDamage), ClassAbility.GLORIOUS_BATTLE, true);
					mCosmetic.gloryOnDamage(world, mPlayer, nearest);

					float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCK_AWAY_SPEED);
					for (LivingEntity mob : mobs) {
						MovementUtils.knockAway(mPlayer, mob, knockback, true);
					}
					this.cancel();
				}

				//Logged off or something probably
				if (mT > 200) {
					this.cancel();
				}
			}
		};
		mRunnable.runTaskTimer(mPlugin, mSpellDelay, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (AFFECTED_ABILITIES.contains(event.getAbility()) && MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, "GloriousBattleStackIncrease") && !enemy.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			int previousStacks = mStacks;
			if (mStacks < mStackLimit) {
				mStacks++;
				if (mStackLimit > 1) {
					MessagingUtils.sendActionBarMessage(mPlayer, "Glorious Battle Stacks: " + mStacks);
				} else {
					MessagingUtils.sendActionBarMessage(mPlayer, "Glorious Battle is ready!");
				}
			}
			if (mStacks != previousStacks) {
				ClientModHandler.updateAbility(mPlayer, this);
			}
		}

		DamageEvent.DamageType type = event.getType();
		if (isLevelTwo() && (type == DamageType.MELEE || type == DamageType.MELEE_SKILL || type == DamageType.MELEE_ENCH)) {
			int count = 0;
			for (LivingEntity le : new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), TARGET_RANGE).getHitMobs()) {
				if (le instanceof Mob mob && mob.getTarget() == mPlayer) {
					count++;
				}
			}
			if (count > 0) {
				if (count > MAX_TARGETING) {
					count = MAX_TARGETING;
				}
				event.setDamage(event.getDamage() * (1 + count * (DAMAGE_PER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_MODIFIER))));
			}
		}

		return false;
	}

	@Override
	public int getCharges() {
		return mStacks;
	}

	@Override
	public int getMaxCharges() {
		return mStackLimit;
	}
}
