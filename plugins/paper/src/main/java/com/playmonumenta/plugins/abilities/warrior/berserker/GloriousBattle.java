package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class GloriousBattle extends Ability implements AbilityWithChargesOrStacks {
	private static final int DAMAGE_1 = 20;
	private static final int DAMAGE_2 = 30;
	private static final double RADIUS = 3;
	private static final double BLEED_PERCENT = 0.2;
	private static final int BLEED_TIME = 4 * 20;
	private static final float KNOCK_AWAY_SPEED = 0.4f;
	private static final String KBR_EFFECT = "GloriousBattleKnockbackResistanceEffect";
	private static final double DAMAGE_PER = 0.05;
	private static final int MAX_TARGETING = 6;
	private static final double TARGET_RANGE = 8;

	private static final EnumSet<ClassAbility> AFFECTED_ABILITIES = EnumSet.of(
		ClassAbility.BRUTE_FORCE,
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

	private int mStacks;
	private final int mStackLimit;
	private final double mDamage;

	public GloriousBattle(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Glorious Battle");
		mInfo.mLinkedSpell = ClassAbility.GLORIOUS_BATTLE;
		mInfo.mCooldown = 0;
		mInfo.mScoreboardId = "GloriousBattle";
		mInfo.mShorthandName = "GB";
		mInfo.mDescriptions.add("Dealing indirect damage with an ability grants you a Glorious Battle stack. Shift and swap hands to consume a stack and charge forwards, gaining full knockback resistance until landing. When you land, deal " + DAMAGE_1 + " damage to the nearest mob within 3 blocks and " +
				"apply " + (int) DepthsUtils.roundPercent(BLEED_PERCENT) + "% bleed for " + (BLEED_TIME / 20) + " seconds. Additionally, knock back all mobs within 3 blocks.");
		mInfo.mDescriptions.add("Damage increased to 30. Additionally, you now passively gain 5% melee damage for each mob targeting you within 8 blocks, up to 6 mobs.");
		mDisplayItem = new ItemStack(Material.IRON_SWORD, 1);
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;
		mStacks = 0;
		mStackLimit = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		event.setCancelled(true);

		if (mPlayer == null || mStacks < 1 || !mPlayer.isSneaking()) {
			return;
		}

		mStacks--;
		Vector dir = mPlayer.getLocation().getDirection();
		dir.multiply(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, 1.4));
		dir.setY(dir.getY() * 0.5 + 0.4);
		mPlayer.setVelocity(dir);
		mPlugin.mEffectManager.addEffect(mPlayer, KBR_EFFECT, new PercentKnockbackResist(200, 1, KBR_EFFECT));
		ClientModHandler.updateAbility(mPlayer, this);
		Location location = mPlayer.getLocation();
		World world = mPlayer.getWorld();
		world.playSound(location, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2f, 0.5f);
		new PartialParticle(Particle.CRIMSON_SPORE, location, 25, 1, 0, 1, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.CRIT, location, 15, 1, 0, 1, 0).spawnAsPlayerActive(mPlayer);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT++;
				if (mPlayer.isOnGround()) {
					mPlugin.mEffectManager.clearEffects(mPlayer, KBR_EFFECT);

					double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
					Location location = mPlayer.getLocation();
					World world = mPlayer.getWorld();
					List<LivingEntity> mobs = EntityUtils.getNearbyMobs(location, radius);
					mobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

					LivingEntity nearest = EntityUtils.getNearestMob(location, mobs);
					if (nearest == null) {
						this.cancel();
						return;
					}

					EntityUtils.applyBleed(mPlugin, BLEED_TIME + CharmManager.getExtraDuration(mPlayer, CHARM_BLEED_DURATION), BLEED_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BLEED_AMPLIFIER), nearest);
					DamageUtils.damage(mPlayer, nearest, DamageType.MELEE_SKILL, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mDamage), ClassAbility.GLORIOUS_BATTLE, true);

					float knockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCK_AWAY_SPEED);
					for (LivingEntity mob : mobs) {
						MovementUtils.knockAway(mPlayer, mob, KNOCK_AWAY_SPEED, true);
					}

					world.playSound(location, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1f, 1f);
					new PartialParticle(Particle.SWEEP_ATTACK, location, 20, 1, 0, 1, 0).spawnAsPlayerActive(mPlayer);

					this.cancel();
				}

				//Logged off or something probably
				if (mT > 200) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 10, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (AFFECTED_ABILITIES.contains(event.getAbility()) && MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, "GloriousBattleStackIncrease")) {
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
			for (LivingEntity le : EntityUtils.getNearbyMobs(mPlayer.getLocation(), TARGET_RANGE)) {
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
