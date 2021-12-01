package com.playmonumenta.plugins.abilities.warrior.berserker;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;



public class RecklessSwing extends Ability {

	private static final double DAMAGE_PERCENT_PER_4_HEALTH_1 = 0.05;
	private static final double DAMAGE_PERCENT_PER_4_HEALTH_2 = 0.1;
	private static final double MAX_DAMAGE_PERCENT_1 = 0.25;
	private static final double MAX_DAMAGE_PERCENT_2 = 0.5;
	private static final int DAMAGE_INCREMENT_THRESHOLD_HEALTH = 4;
	private static final int SELF_DAMAGE = 4;
	private static final int DAMAGE_1 = 12;
	private static final int DAMAGE_2 = 24;
	private static final double RADIUS = 3.5;

	private final double mDamagePercentPer4Health;
	private final double mMaxDamagePercent;
	private final int mDamage;

	private Rampage mRampage;

	public RecklessSwing(Plugin plugin, Player player) {
		super(plugin, player, "Reckless Swing");
		mInfo.mLinkedSpell = ClassAbility.RECKLESS_SWING;
		mInfo.mScoreboardId = "RecklessSwing";
		mInfo.mShorthandName = "RS";
		mInfo.mDescriptions.add("Passively, every 4 health you fall below your maximum health, gain +5% damage (capped at +25%) on sword and axe hits. Sneak left click with an axe or a sword (including when attacking enemies) to deal 12 damage in a 3.5 block radius at the cost of taking 4 damage (not reduced by any kind of damage resistance, bypasses absorption). Reckless Swing damage counts towards Rampage stacks.");
		mInfo.mDescriptions.add("Gain +10% damage (capped at +50%) instead. Damage from the active increased to 24.");
		mInfo.mIgnoreCooldown = true;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mDisplayItem = new ItemStack(Material.IRON_SWORD, 1);
		mDamagePercentPer4Health = getAbilityScore() == 1 ? DAMAGE_PERCENT_PER_4_HEALTH_1 : DAMAGE_PERCENT_PER_4_HEALTH_2;
		mMaxDamagePercent = getAbilityScore() == 1 ? MAX_DAMAGE_PERCENT_1 : MAX_DAMAGE_PERCENT_2;
		mDamage = getAbilityScore() == 1 ? DAMAGE_1 : DAMAGE_2;

		if (player != null) {
			Bukkit.getScheduler().runTask(plugin, () -> {
				mRampage = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(mPlayer, Rampage.class);
			});
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			event.setDamage(computeDamageUsingHealth(event.getDamage()));
		}
		return true;
	}

	@Override
	public void cast(Action action) {
		if (mPlayer.isSneaking()) {
			// Run at the end of this tick so it'll apply after any damage dealt to mobs
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				double preReduceHealthDamage = computeDamageUsingHealth(mDamage);
				if (mPlayer.getHealth() <= SELF_DAMAGE) {
					mPlayer.damage(9001);
				} else {
					mPlayer.setHealth(Math.min(mPlayer.getHealth() - SELF_DAMAGE, mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
					mPlayer.damage(0);
				}

				Location loc = mPlayer.getLocation();
				World world = mPlayer.getWorld();
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
				world.spawnParticle(Particle.SWEEP_ATTACK, loc, 25, 2, 0, 2, 0);

				for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RADIUS)) {
					// Only ignore iframes if the mob is *not* a boss
					boolean ignoreIframes = !EntityUtils.isBoss(mob);
					EntityUtils.damageEntity(mPlugin, mob, mDamage, mPlayer, MagicType.PHYSICAL, true, mInfo.mLinkedSpell, true, true, ignoreIframes, false);
					if (mRampage != null) {
						mRampage.customRecklessSwingInteraction(preReduceHealthDamage);
					}
				}
			});
		}
	}

	private double computeDamageUsingHealth(double baseDamage) {
		int missingHealthChunks = (int)((mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() - mPlayer.getHealth()) / DAMAGE_INCREMENT_THRESHOLD_HEALTH);
		return baseDamage * (1 + Math.min(mMaxDamagePercent, mDamagePercentPer4Health * missingHealthChunks));
	}

	@Override
	public boolean runCheck() {
		ItemStack mainhand = mPlayer.getInventory().getItemInMainHand();
		return ItemUtils.isSword(mainhand) || ItemUtils.isAxe(mainhand);
	}
}
