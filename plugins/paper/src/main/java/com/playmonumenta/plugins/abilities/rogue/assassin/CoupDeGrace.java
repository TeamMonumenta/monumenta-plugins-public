package com.playmonumenta.plugins.abilities.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

	public CoupDeGrace(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Coup de Grace");
		mInfo.mScoreboardId = "CoupDeGrace";
		mInfo.mShorthandName = "CdG";
		mInfo.mDescriptions.add("If melee damage you deal brings a normal mob under 10% health, they die instantly. The threshold for elites is 20% health.");
		mInfo.mDescriptions.add("The health threshold is increased to 15% for normal enemies and 30% for elites.");
		mDisplayItem = new ItemStack(Material.WITHER_SKELETON_SKULL, 1);
		mNormalThreshold = getAbilityScore() == 1 ? COUP_1_NORMAL_THRESHOLD : COUP_2_NORMAL_THRESHOLD;
		mEliteThreshold = getAbilityScore() == 1 ? COUP_1_ELITE_THRESHOLD : COUP_2_ELITE_THRESHOLD;
	}

	@Override
	public double getPriorityAmount() {
		return 5000; // after all damage modifiers to get the proper final damage
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH) {
			for (PotionEffect effect : enemy.getActivePotionEffects()) {
				if (effect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE) && effect.getAmplifier() >= 4) {
					return false;
				}
			}

			double maxHealth = EntityUtils.getMaxHealth(enemy);
			if (EntityUtils.isElite(enemy)) {
				if (enemy.getHealth() - (event.getDamage() * EntityUtils.vulnerabilityMult(enemy)) < maxHealth * mEliteThreshold) {
					execute(event);
				}
			} else if (!EntityUtils.isBoss(enemy)) {
				if (enemy.getHealth() - (event.getDamage() * EntityUtils.vulnerabilityMult(enemy)) < maxHealth * mNormalThreshold) {
					execute(event);
				}
			}
		}
		return false; // only increases event damage, thus no recursion
	}

	private void execute(DamageEvent event) {
		LivingEntity le = event.getDamagee();
		event.setDamage(event.getDamage() + 9001);
		World world = mPlayer.getWorld();
		world.playSound(le.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.75f, 0.75f);
		world.playSound(le.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.5f);
		world.spawnParticle(Particle.BLOCK_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 20, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65, Material.REDSTONE_WIRE.createBlockData());
		if (getAbilityScore() > 1) {
			world.spawnParticle(Particle.SPELL_WITCH, le.getLocation().add(0, le.getHeight() / 2, 0), 10, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65);
			world.spawnParticle(Particle.BLOCK_DUST, le.getLocation().add(0, le.getHeight() / 2, 0), 20, le.getWidth() / 2, le.getHeight() / 3, le.getWidth() / 2, 0.65, Material.REDSTONE_BLOCK.createBlockData());
		}
	}

	@Override
	public boolean runCheck() {
		return InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer);
	}
}
