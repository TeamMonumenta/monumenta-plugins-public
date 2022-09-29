package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.ViciousCombosCS;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ViciousCombos extends Ability {

	private static final int VICIOUS_COMBOS_RANGE = 5;
	private static final int VICIOUS_COMBOS_COOL_1 = 20;
	private static final int VICIOUS_COMBOS_COOL_2 = 2 * 20;
	private static final int VICIOUS_COMBOS_CRIPPLE_DURATION = 5 * 20;
	private static final double VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL = 0.15;
	private static final double VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL = 0.15;
	private static final int ENHANCEMENT_COOLDOWN_REDUCTION = 1 * 20;
	private static final int ENHANCEMENT_CHARGE_LIFETIME = 3 * 20;
	private static final double ENHANCEMENT_DAMAGE_INCREASE = 0.1;

	public static final String CHARM_CDR = "Vicious Combos Cooldown Reduction";

	private ClassAbility mLastAbility = null;
	private int mAbilityCastTime = 0;

	private final ViciousCombosCS mCosmetic;

	public ViciousCombos(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Vicious Combos");
		mInfo.mScoreboardId = "ViciousCombos";
		mInfo.mShorthandName = "VC";
		mInfo.mDescriptions.add(
			String.format("Passively, killing an enemy with melee or ability damage refreshes the cooldown of your abilities by %s second. Killing an Elite or Boss enemy instead resets the cooldown of your abilities.",
				VICIOUS_COMBOS_COOL_1 / 20));
		mInfo.mDescriptions.add(
			String.format("Killing an enemy now refreshes your ability cooldowns by %s seconds. Killing an Elite or Boss enemy inflicts nearby enemies within %s blocks with %s%% weaken and %s%% Vulnerability for %s seconds.",
				VICIOUS_COMBOS_COOL_2 / 20,
				VICIOUS_COMBOS_RANGE,
				(int)(VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL * 100),
				(int)(VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL * 100),
				VICIOUS_COMBOS_CRIPPLE_DURATION / 20));
		mInfo.mDescriptions.add(
			String.format("When an ability goes on cooldown, your next melee attack in %ss deals %s%% more melee damage and that ability's cooldown is refreshed by %ss, prioritizing the last ability.",
				ENHANCEMENT_CHARGE_LIFETIME / 20,
				(int)(ENHANCEMENT_DAMAGE_INCREASE * 100),
				ENHANCEMENT_COOLDOWN_REDUCTION / 20));
		mInfo.mLinkedSpell = ClassAbility.VICIOUS_COMBOS;
		mDisplayItem = new ItemStack(Material.ZOMBIE_HEAD, 1);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ViciousCombosCS(), ViciousCombosCS.SKIN_LIST);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		EntityDamageEvent e = event.getEntity().getLastDamageCause();
		if (e != null
			&& (e.getCause() == DamageCause.ENTITY_ATTACK
			|| e.getCause() == DamageCause.ENTITY_SWEEP_ATTACK
			|| e.getCause() == DamageCause.CUSTOM)) {
			LivingEntity killedEntity = event.getEntity();

			//Run the task 1 tick later to let everything go on cooldown (ex. BMB)
			new BukkitRunnable() {

				@Override
				public void run() {
					Location loc = killedEntity.getLocation();
					loc = loc.add(0, 0.5, 0);
					World world = mPlayer.getWorld();

					if (EntityUtils.isElite(killedEntity) || EntityUtils.isBoss(killedEntity)) {
						mPlugin.mTimers.removeAllCooldowns(mPlayer);
						MessagingUtils.sendActionBarMessage(mPlayer, "All your cooldowns have been reset");

						if (isLevelTwo()) {
							for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, VICIOUS_COMBOS_RANGE, mPlayer)) {
								new PartialParticle(Particle.SPELL_MOB, mob.getLocation().clone().add(0, 1, 0), 10, 0.35, 0.5, 0.35, 0).spawnAsPlayerActive(mPlayer);
								EntityUtils.applyVulnerability(mPlugin, VICIOUS_COMBOS_CRIPPLE_DURATION, VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL, mob);
								EntityUtils.applyWeaken(mPlugin, VICIOUS_COMBOS_CRIPPLE_DURATION, VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL, mob);
							}
						}
						mCosmetic.comboOnElite(world, loc, mPlayer, VICIOUS_COMBOS_RANGE, killedEntity);

					} else if (EntityUtils.isHostileMob(killedEntity)) {
						int timeReduction = isLevelOne() ? VICIOUS_COMBOS_COOL_1 : VICIOUS_COMBOS_COOL_2;
						if (killedEntity instanceof Player) {
							timeReduction *= 2;
						}

						mPlugin.mTimers.updateCooldowns(mPlayer, timeReduction);
						mCosmetic.comboOnKill(world, loc, mPlayer, VICIOUS_COMBOS_RANGE, killedEntity);
					}

				}

			}.runTaskLater(mPlugin, 1);
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		// If:
		// Is Enhanced,
		// Is a Melee Attack,
		// LastAbility does exist
		// The LastAbility cast time is within charge's lifetime.
		if (isEnhanced()
			&& event.getType() == DamageEvent.DamageType.MELEE
			&& mLastAbility != null
			&& mPlayer.getTicksLived() < mAbilityCastTime + ENHANCEMENT_CHARGE_LIFETIME) {

			event.setDamage(event.getDamage() * (1 + ENHANCEMENT_DAMAGE_INCREASE));
			mPlugin.mTimers.updateCooldown(mPlayer, mLastAbility, ENHANCEMENT_COOLDOWN_REDUCTION);

			// mPlayer.sendMessage(mLastAbility.getName() + " has been reduced!");

			clearState();
		}
		return false;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (isEnhanced()) {
			// Run this 1 tick late to prevent ByMyBlade triggering it immediately.
			new BukkitRunnable() {
				@Override
				public void run() {
					// Get the index of the ability in mAbilities, add to the order.
					mLastAbility = event.getAbility();
					mAbilityCastTime = mPlayer.getTicksLived();
					// mPlayer.sendMessage(mLastAbility.getName() + " is Selected");
				}
			}.runTaskLater(mPlugin, 1);
		}

		return true;
	}

	public void clearState() {
		mLastAbility = null;
		mAbilityCastTime = 0;
		// mPlayer.sendMessage("Cleared");
	}
}
