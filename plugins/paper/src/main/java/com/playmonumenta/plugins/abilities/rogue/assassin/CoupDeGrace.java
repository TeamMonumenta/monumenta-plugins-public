package com.playmonumenta.plugins.abilities.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/*
 * Coup De Grâce: If you melee attack a normal enemy and that attack
 * brings it under 10% health, they die instantly. If you melee attack
 * an elite enemy and that attack brings it under 20% health, they die
 * instantly. At level 2, the threshold increases to 15% health for
 * normal enemies and 30% health for elite enemies.
 */
public class CoupDeGrace extends Ability {

	private static final double COUP_1_NORMAL_THRESHOLD = 0.1;
	private static final double COUP_2_NORMAL_THRESHOLD = 0.15;
	private static final double COUP_1_ELITE_THRESHOLD = 0.2;
	private static final double COUP_2_ELITE_THRESHOLD = 0.3;

	private final double mNormalThreshold;
	private final double mEliteThreshold;

	public static final String CHARM_THRESHOLD = "Coup de Grace Threshold";
	public static final String CHARM_NORMAL = "Coup de Grace Normal Enemy Threshold";
	public static final String CHARM_ELITE = "Coup de Grace Elite Threshold";

	public static final AbilityInfo<CoupDeGrace> INFO =
		new AbilityInfo<>(CoupDeGrace.class, "Coup de Grace", CoupDeGrace::new)
			.scoreboardId("CoupDeGrace")
			.shorthandName("CdG")
			.descriptions(
				String.format("If melee damage you deal brings a normal mob under %s%% health, they die instantly. The threshold for elites is %s%% health.",
					(int) (COUP_1_NORMAL_THRESHOLD * 100),
					(int) (COUP_1_ELITE_THRESHOLD * 100)),
				String.format("The health threshold is increased to %s%% for normal enemies and %s%% for elites.",
					(int) (COUP_2_NORMAL_THRESHOLD * 100),
					(int) (COUP_2_ELITE_THRESHOLD * 100)))
			.displayItem(new ItemStack(Material.WITHER_SKELETON_SKULL, 1))
			.priorityAmount(5000); // after all damage modifiers to get the proper final damage

	public CoupDeGrace(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		double sharedThreshold = CharmManager.getLevelPercentDecimal(player, CHARM_THRESHOLD);
		mNormalThreshold = (isLevelOne() ? COUP_1_NORMAL_THRESHOLD : COUP_2_NORMAL_THRESHOLD) + CharmManager.getLevelPercentDecimal(player, CHARM_NORMAL) + sharedThreshold;
		mEliteThreshold = (isLevelOne() ? COUP_1_ELITE_THRESHOLD : COUP_2_ELITE_THRESHOLD) + CharmManager.getLevelPercentDecimal(player, CHARM_ELITE) + sharedThreshold;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)
			    && !EntityUtils.isBoss(enemy)
			    && !DamageUtils.isImmuneToDamage(enemy)
			    && (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH
				        || event.getAbility() == ClassAbility.QUAKE || event.getAbility() == ClassAbility.SKIRMISHER)) {
			// Cannot currently get the real final damage, as some effects like vulnerability will modify it later.
			// Thus, just check the mob's health a tick later.
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				if (enemy.isValid() && enemy.getHealth() < EntityUtils.getMaxHealth(enemy) * (EntityUtils.isElite(enemy) ? mEliteThreshold : mNormalThreshold)) {
					execute(enemy);
				}
			});
		}
		return false;
	}

	private void execute(LivingEntity le) {
		DamageUtils.damage(mPlayer, le, DamageType.OTHER, 9001, ClassAbility.COUP_DE_GRACE, true, false);
		World world = mPlayer.getWorld();
		world.playSound(le.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.75f, 0.75f);
		world.playSound(le.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.5f);
		new PartialParticle(Particle.BLOCK_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 20, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65, Material.REDSTONE_WIRE.createBlockData()).spawnAsPlayerActive(mPlayer);
		if (isLevelTwo()) {
			new PartialParticle(Particle.SPELL_WITCH, le.getLocation().add(0, le.getHeight() / 2, 0), 10, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.BLOCK_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 20, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65, Material.REDSTONE_BLOCK.createBlockData()).spawnAsPlayerActive(mPlayer);
		}
	}

}
