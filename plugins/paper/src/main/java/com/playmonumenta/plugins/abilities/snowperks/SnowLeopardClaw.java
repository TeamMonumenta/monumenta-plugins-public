package com.playmonumenta.plugins.abilities.snowperks;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.guis.SnowPerkGui;
import com.playmonumenta.plugins.itemstats.enchantments.CritScaling;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class SnowLeopardClaw extends Ability {
	private static final String SCOREBOARD = "SnowLeopardClaw";
	private static final int POINT_COST = 4;
	private static final double RADIUS = 3;
	private static final double SLASH_DAMAGE = 0.20;
	private static final int BASE_SLASH_AMOUNT = 1;
	private static final int KILLS_PER_UPGRADE = 15;

	public static final AbilityInfo<SnowLeopardClaw> INFO =
		new SnowPerkGui.SnowPerkInfo<>(SnowLeopardClaw.class, "Snow Leopard Claw", SnowLeopardClaw::new)
			.snowPointCost(POINT_COST)
			.scoreboardId(SCOREBOARD)
			.displayItem(Material.QUARTZ)
			.description(getDescription());

	private int mKills;
	private int mSlashes;

	public SnowLeopardClaw(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mKills = 0;
		mSlashes = BASE_SLASH_AMOUNT;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MELEE && mPlayer.getCooledAttackStrength(0.5f) >= 0.9) {
			// adapted from Brute Force
			double damage = SLASH_DAMAGE * event.getFlatDamage();
			if (event.getIsCrit()) {
				boolean weaponHasCumbersome = ItemStatUtils.hasEnchantment(mPlayer.getInventory().getItemInMainHand(), EnchantmentType.CUMBERSOME);
				damage *= weaponHasCumbersome ? 1 : CritScaling.CRIT_BONUS;
			}

			slashFX(event.getDamagee(), 0);

			double finalDamage = damage;
			cancelOnDeath(new BukkitRunnable() {
				private int mSlashesDone = 0;
				private final int mTotalSlashes = mSlashes;
				private final double mDamage = finalDamage;
				private final LivingEntity mTarget = event.getDamagee();
				private final Map<LivingEntity, Integer> mHitMap = new HashMap<>(); // Only hits mobs up to 3 times!
				{
					mHitMap.put(mTarget, 1); // Count the original hit as 1
				}

				@Override
				public void run() {
					mSlashesDone++;

					List<LivingEntity> mobs = new Hitbox.SphereHitbox(mTarget.getLocation(), RADIUS).getHitMobs();
					mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG) || mHitMap.getOrDefault(mob, 0) >= 3);
					if (!mobs.isEmpty()) {
						Collections.shuffle(mobs);
						LivingEntity mob = mobs.getFirst();

						DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MELEE_SKILL, mDamage, ClassAbility.SNOW_LEOPARD_CLAW, true);
						MovementUtils.knockAway(mPlayer, mob, 0.18f, 0.18f, true);

						slashFX(mob, mSlashesDone);

						mHitMap.merge(mob, 1, Integer::sum);
					}

					if (mSlashesDone >= mTotalSlashes) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, getInterval(mSlashes), getInterval(mSlashes)));
		}
		return false;
	}

	private void slashFX(LivingEntity mob, int slashNumber) {
		World world = mob.getWorld();
		Location loc = LocationUtils.getEntityCenter(mob);

		float volumeMult = 1 - slashNumber * 0.15f;

		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.8f * volumeMult, 1.7f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.9f * volumeMult, 1.4f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.35f * volumeMult, 2.0f);
		world.playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.5f * volumeMult, 1.4f);
		world.playSound(loc, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.7f * volumeMult, 1.2f);

		new PartialParticle(Particle.SWEEP_ATTACK, loc).count(2).delta(0, 0.25, 0).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.SNOWFLAKE, loc).count(10).delta(0.2).extra(0.02).spawnAsPlayerActive(mPlayer);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (ScoreboardUtils.checkTag(event.getEntity(), AbilityUtils.IGNORE_TAG)) {
			return;
		}
		mKills++;
		if (mKills % KILLS_PER_UPGRADE == 0) {
			mSlashes++;
			sendActionBarMessage("Snow Leopard Claw upgraded to %d slashes!".formatted(mSlashes));

			mPlayer.playSound(mPlayer, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1f, 1.25f);
			mPlayer.playSound(mPlayer, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1f, 2f);

			new PartialParticle(Particle.VILLAGER_HAPPY, LocationUtils.getEntityCenter(mPlayer)).count(50).delta(0.5).spawnAsPlayerActive(mPlayer);
		}
	}

	private int getInterval(int slashes) {
		return switch (slashes) {
			case 1 -> 4;
			case 2 -> 3;
			case 3, 4, 5 -> 2;
			default -> 1;
		};
	}

	public static Description<SnowLeopardClaw> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO).arrowColor(SnowPerkGui.SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Attacking a mob causes %d slash that hits").statValues(stat(BASE_SLASH_AMOUNT))
			.addLine("that mob or other mobs within %d blocks").statValues(stat(RADIUS))
			.addLine("and deals %p (m) of the original hit's damage.").statValues(stat(SLASH_DAMAGE))
			.addLine()
			.addLine("For every %d mobs you kill, permanently").statValues(stat(KILLS_PER_UPGRADE))
			.addLine("increase the number of slashes by +%d.").statValues(stat(1))
			.addLine("(Can hit the same mob up to 3 times)")
			.addLine()
			.addStat("Cost: %d Snow Points").statValues(stat(POINT_COST))
			.addDashedLine();
	}
}
