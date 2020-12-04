package com.playmonumenta.plugins.abilities.warlock;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Harvester extends Ability {

	private static final int VULNERABILITY_DURATION = 5 * 20;
	private static final int VULNERABILITY_AMPLIFIER_1 = 1;
	private static final int VULNERABILITY_AMPLIFIER_2 = 3;
	private static final int HARVESTER_CAP_1 = 4;
	private static final int HARVESTER_CAP_2 = 8;
	private static final double HARVESTER_DAMAGE = 0.5;

	private static final List<PotionEffectType> DEBUFFS = Arrays.asList(
            PotionEffectType.WITHER,
            PotionEffectType.SLOW,
            PotionEffectType.WEAKNESS,
            PotionEffectType.SLOW_DIGGING,
            PotionEffectType.POISON,
            PotionEffectType.UNLUCK,
            PotionEffectType.BLINDNESS,
            PotionEffectType.CONFUSION,
            PotionEffectType.HUNGER
        );

	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(155, 0, 255), 1.0f);

	private int mHarvesterDeaths = 0;
	private int mHexDeaths = 0;
	private boolean mHexActive = false;
	private ConsumingFlames mFlames = AbilityManager.getManager().getPlayerAbility(mPlayer, ConsumingFlames.class);

	public Harvester(Plugin plugin, Player player) {
		super(plugin, player, "Harvester of the Damned");
		mInfo.mScoreboardId = "Harvester";
		mInfo.mShorthandName = "HotD";
		mInfo.mDescriptions.add("Enemies you damage with an ability are afflicted with 10% vulnerability for 5 seconds. Additionally, for each mob that dies within 8 blocks of the player, your next cast of Amplifying Hex does an additional 0.5 damage per debuff, capped at +4.");
		mInfo.mDescriptions.add("Vulnerability is increased to 20%, and the cap is increased to +8 damage per debuff.");
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		//Apply Vulnerability
		int vuln = getAbilityScore() == 1 ? VULNERABILITY_AMPLIFIER_1 : VULNERABILITY_AMPLIFIER_2;
		PotionUtils.applyPotion(mPlayer, event.getDamaged(), new PotionEffect(PotionEffectType.UNLUCK, VULNERABILITY_DURATION, vuln, false, true));

		//Modify Hex Damage
		mHexActive = true;
		int cap = getAbilityScore() == 1 ? HARVESTER_CAP_1 : HARVESTER_CAP_2;
		double bonusDamage = Math.min(HARVESTER_DAMAGE * mHarvesterDeaths, cap);
		LivingEntity damagee = event.getDamaged();
		Location locD = damagee.getLocation().add(0, 1, 0);
		if (event.getSpell() == Spells.AMPLIFYING) {
			int debuffCount = 0;
			for (PotionEffectType effectType: DEBUFFS) {
				PotionEffect effect = damagee.getPotionEffect(effectType);
				if (effect != null) {
					debuffCount++;
				}
			}
			if (mFlames != null)	{
				if (mFlames.getAbilityScore() > 0 && damagee.getFireTicks() > 0) {
					debuffCount++;
				}
			}
			if (EntityUtils.isStunned(damagee)) {
				debuffCount++;
			}
			if (debuffCount > 0) {
				EntityUtils.damageEntity(mPlugin, damagee, debuffCount * bonusDamage,
						mPlayer, MagicType.DARK_MAGIC, true, mInfo.mLinkedSpell);
			}

			World world = mPlayer.getWorld();
			world.spawnParticle(Particle.REDSTONE, locD, 35, 0.4, 0.4, 0.4, COLOR);
			world.spawnParticle(Particle.SPELL_WITCH, locD, 10, 0.35, 0.45, 0.35, 0.001);
		}
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (event.getAbility() == Spells.AMPLIFYING) {
			new BukkitRunnable() {
				@Override
				public void run() {
					mHarvesterDeaths = mHexDeaths;
					mHexDeaths = 0;
					mHexActive = false;
				}
			}.runTaskLater(mPlugin, 1);
		}
		return true;
	}


	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mPlayer.getWorld().spawnParticle(Particle.SPELL_WITCH, mPlayer.getLocation().add(0, 1, 0), 10, 0.35, 0.45, 0.35, 0.001);
		if (mHexActive) {
			mHexDeaths++;
		} else {
			mHarvesterDeaths++;
		}

	}

	@Override
	public double entityDeathRadius() {
		return 8;
	}

}
