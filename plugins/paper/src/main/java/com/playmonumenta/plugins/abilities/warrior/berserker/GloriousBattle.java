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
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class GloriousBattle extends Ability implements AbilityWithChargesOrStacks {
	private static final int DAMAGE_1 = 20;
	private static final int DAMAGE_2 = 25;
	private static final double VELOCITY_1 = 1.3;
	private static final double VELOCITY_2 = 1.6;
	private static final double VERTICAL_SPEED_CAP = 0.3;
	private static final double RADIUS = 3;
	private static final float KNOCK_AWAY_SPEED = 0.4f;
	private static final String KBR_EFFECT = "GloriousBattleKnockbackResistanceEffect";

	private static final EnumSet<ClassAbility> AFFECTED_ABILITIES = EnumSet.of(
		ClassAbility.BRUTE_FORCE_AOE,
		ClassAbility.COUNTER_STRIKE_AOE,
		ClassAbility.SHIELD_BASH_AOE,
		ClassAbility.METEOR_SLAM,
		ClassAbility.RAMPAGE);

	public static final String CHARM_CHARGES = "Glorious Battle Charges";
	public static final String CHARM_DAMAGE = "Glorious Battle Damage";
	public static final String CHARM_RADIUS = "Glorious Battle Radius";
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
					 "Shift and swap hands to consume a stack and charge forwards at %s blocks per second, gaining full knockback resistance until landing. " +
					 "Vertical movement speed is capped at %s blocks per second. " +
					 "When you land, deal %s damage to the nearest mob within %s blocks. " +
					 "Additionally, knock back all mobs within %s blocks.")
					.formatted(
						VELOCITY_1,
						VERTICAL_SPEED_CAP,
						DAMAGE_1,
						RADIUS,
						RADIUS
					),
				"Damage is increased to %s. Velocity is increased to %s. Vertical speed cap is removed."
					.formatted(
						DAMAGE_2,
						VELOCITY_2
					))
			.simpleDescription("Lunge forward, dealing damage upon landing.")
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", GloriousBattle::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true),
				new AbilityTriggerInfo.TriggerRestriction("holding a sword or axe", p -> {
					ItemStack mainhand = p.getInventory().getItemInMainHand();
					return ItemUtils.isSword(mainhand) || ItemUtils.isAxe(mainhand);
				})))
			.displayItem(Material.IRON_SWORD);

	private int mStacks;
	private final int mStackLimit;
	private final int mSpellDelay = 10;
	private final double mDamage;
	private final double mVelocity;
	private @Nullable BukkitRunnable mRunnable;
	private final GloriousBattleCS mCosmetic;

	public GloriousBattle(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mVelocity = isLevelOne() ? VELOCITY_1 : VELOCITY_2;
		mStacks = 0;
		mStackLimit = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GloriousBattleCS());
	}

	public void cast() {
		if (mStacks < 1 || ZoneUtils.hasZoneProperty(mPlayer.getLocation(), ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return;
		}

		mStacks--;
		Vector dir = mPlayer.getLocation().getDirection();
		dir.multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, mVelocity));
		// vertical speed cap for level 1
		if (isLevelOne() && (dir.getY() > VERTICAL_SPEED_CAP || dir.getY() < -VERTICAL_SPEED_CAP)) {
			double sign = Math.signum(dir.getY());
			dir.setY((VERTICAL_SPEED_CAP + 0.2) * sign);
		} else {
			if (Math.signum(dir.getY()) > 0) {
				// +0.22 to allow for horizontal movement
				dir.setY(dir.getY() * 0.4 + 0.22);
			} else {
				// do not -0.3, otherwise will affect planned change for meteor slam (blocks fallen -> velocity)
				dir.setY(dir.getY() * 0.4);
			}
		}
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
				if (PlayerUtils.isOnGround(mPlayer)) {
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
		cancelOnDeath(mRunnable.runTaskTimer(mPlugin, mSpellDelay, 1));
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (AFFECTED_ABILITIES.contains(event.getAbility()) && MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, "GloriousBattleStackIncrease") && !enemy.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			int previousStacks = mStacks;
			if (mStacks < mStackLimit) {
				mStacks++;
				if (mStackLimit > 1) {
					showChargesMessage();
				} else {
					sendActionBarMessage("Glorious Battle is ready!");
				}
			}
			if (mStacks != previousStacks) {
				ClientModHandler.updateAbility(mPlayer, this);
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
