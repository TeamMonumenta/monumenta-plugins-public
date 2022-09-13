package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.ViciousCombosCS;
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

	private final ViciousCombosCS mCosmetic;

	public ViciousCombos(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Vicious Combos");
		mInfo.mScoreboardId = "ViciousCombos";
		mInfo.mShorthandName = "VC";
		mInfo.mDescriptions.add("Passively, killing an enemy with melee or ability damage refreshes the cooldown of your abilities by 1 second. Killing an Elite or Boss enemy instead resets the cooldown of your abilities.");
		mInfo.mDescriptions.add("Killing an enemy now refreshes your ability cooldowns by 2 seconds. Killing an Elite or Boss enemy inflicts nearby enemies within 5 blocks with 15% weaken and 15% Vulnerability for 5 seconds.");
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
			int viciousCombos = getAbilityScore();

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

						if (viciousCombos > 1) {
							for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, VICIOUS_COMBOS_RANGE, mPlayer)) {
								new PartialParticle(Particle.SPELL_MOB, mob.getLocation().clone().add(0, 1, 0), 10, 0.35, 0.5, 0.35, 0).spawnAsPlayerActive(mPlayer);
								EntityUtils.applyVulnerability(mPlugin, VICIOUS_COMBOS_CRIPPLE_DURATION, VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL, mob);
								EntityUtils.applyWeaken(mPlugin, VICIOUS_COMBOS_CRIPPLE_DURATION, VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL, mob);
							}
						}
						mCosmetic.comboOnElite(world, loc, mPlayer, VICIOUS_COMBOS_RANGE);

					} else if (EntityUtils.isHostileMob(killedEntity)) {
						int timeReduction = (viciousCombos == 1) ? VICIOUS_COMBOS_COOL_1 : VICIOUS_COMBOS_COOL_2;
						if (killedEntity instanceof Player) {
							timeReduction *= 2;
						}

						mPlugin.mTimers.updateCooldowns(mPlayer, timeReduction);
						mCosmetic.comboOnKill(world, loc, mPlayer, VICIOUS_COMBOS_RANGE);
					}

				}

			}.runTaskLater(mPlugin, 1);
		}
	}
}
