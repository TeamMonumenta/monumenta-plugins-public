package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;


public class ByMyBlade extends Ability {

	private static final int BY_MY_BLADE_1_HASTE_AMPLIFIER = 1;
	private static final int BY_MY_BLADE_2_HASTE_AMPLIFIER = 3;
	private static final int BY_MY_BLADE_HASTE_DURATION = 4 * 20;
	private static final int BY_MY_BLADE_1_DAMAGE = 10;
	private static final int BY_MY_BLADE_2_DAMAGE = 20;
	private static final int BY_MY_BLADE_COOLDOWN = 10 * 20;
	private static final double ENHANCEMENT_HEAL_PERCENT = 0.05;
	private static final double ENHANCEMENT_HEAL_PERCENT_ELITE = 0.15;

	public static final String CHARM_DAMAGE = "By My Blade Damage";
	public static final String CHARM_COOLDOWN = "By My Blade Cooldown";
	public static final String CHARM_HASTE_AMPLIFIER = "By My Blade Haste Amplifier";
	public static final String CHARM_HASTE_DURATION = "By My Blade Haste Duration";

	private final double mDamageBonus;
	private final int mHasteAmplifier;

	public ByMyBlade(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "By My Blade");
		mInfo.mLinkedSpell = ClassAbility.BY_MY_BLADE;
		mInfo.mScoreboardId = "ByMyBlade";
		mInfo.mShorthandName = "BmB";
		mInfo.mDescriptions.add(
			String.format("While holding two swords, attacking an enemy with a critical attack deals an extra %s melee damage to that enemy, and grants you Haste %s for %ss. Cooldown: %ss.",
				BY_MY_BLADE_1_DAMAGE,
				StringUtils.toRoman(BY_MY_BLADE_1_HASTE_AMPLIFIER + 1),
				BY_MY_BLADE_HASTE_DURATION / 20,
				BY_MY_BLADE_COOLDOWN / 20));
		mInfo.mDescriptions.add(
			String.format("Damage is increased from %s to %s. Haste level is increased from %s to %s.",
				BY_MY_BLADE_1_DAMAGE,
				BY_MY_BLADE_2_DAMAGE,
				StringUtils.toRoman(BY_MY_BLADE_1_HASTE_AMPLIFIER + 1),
				StringUtils.toRoman(BY_MY_BLADE_2_HASTE_AMPLIFIER + 1)));
		mInfo.mDescriptions.add(
			String.format("Killing an enemy with this ability heals you for %s%% of your max health, increased to %s%% if the target was an elite or boss.",
				(int)(ENHANCEMENT_HEAL_PERCENT * 100),
				(int)(ENHANCEMENT_HEAL_PERCENT_ELITE * 100)));
		mInfo.mCooldown = CharmManager.getCooldown(player, CHARM_COOLDOWN, BY_MY_BLADE_COOLDOWN);
		mDisplayItem = new ItemStack(Material.SKELETON_SKULL, 1);
		mDamageBonus = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? BY_MY_BLADE_1_DAMAGE : BY_MY_BLADE_2_DAMAGE);
		mHasteAmplifier = (isLevelOne() ? BY_MY_BLADE_1_HASTE_AMPLIFIER : BY_MY_BLADE_2_HASTE_AMPLIFIER) + (int) CharmManager.getLevel(mPlayer, CHARM_HASTE_AMPLIFIER);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.FAST_DIGGING, BY_MY_BLADE_HASTE_DURATION + CharmManager.getExtraDuration(mPlayer, CHARM_HASTE_DURATION), mHasteAmplifier, false, true));

			DamageUtils.damage(mPlayer, enemy, DamageType.MELEE_SKILL, mDamageBonus, mInfo.mLinkedSpell, true);

			Location loc = enemy.getLocation();
			World world = mPlayer.getWorld();
			loc.add(0, 1, 0);
			int count = 15;
			if (isLevelTwo()) {
				new PartialParticle(Particle.SPELL_WITCH, loc, 45, 0.2, 0.65, 0.2, 1.0).spawnAsPlayerActive(mPlayer);
				count = 30;
			}
			if (isEnhanced()) {
				// This might be a bit scuffed... but hopefully it feels better this way.
				// As BMB applies first before melee hit, if the enemy survives BMB but dies to melee
				// It doesn't heal the player. So we delay this check by 1 tick.
				new BukkitRunnable() {
					@Override
					public void run() {
						if (enemy.isDead() || !enemy.isValid()) {
							// Heal Player - 5% normal, 15% elite or boss
							if (EntityUtils.isElite(enemy) || EntityUtils.isBoss(enemy)) {
								PlayerUtils.healPlayer(mPlugin, mPlayer, EntityUtils.getMaxHealth(mPlayer) * ENHANCEMENT_HEAL_PERCENT_ELITE);
							} else {
								PlayerUtils.healPlayer(mPlugin, mPlayer, EntityUtils.getMaxHealth(mPlayer) * ENHANCEMENT_HEAL_PERCENT);
							}
							new PartialParticle(Particle.HEART, mPlayer.getLocation().add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
						}
					}
				}.runTaskLater(mPlugin, 1);
			}
			new PartialParticle(Particle.SPELL_MOB, loc, count, 0.25, 0.5, 0.5, 0.001).spawnAsPlayerActive(mPlayer);
			new PartialParticle(Particle.CRIT, loc, 30, 0.25, 0.5, 0.5, 0.001).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.ITEM_SHIELD_BREAK, 2.0f, 0.5f);
			putOnCooldown();
		}
		return false;
	}

	@Override
	public boolean runCheck() {
		return mPlayer != null && PlayerUtils.isFallingAttack(mPlayer) && InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer);
	}

}
