package com.playmonumenta.plugins.abilities.alchemist;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;



public class BasiliskPoison extends Ability {

	private static final double BASILISK_POISON_1_PERCENT_DAMAGE = 0.05;
	private static final double BASILISK_POISON_2_PERCENT_DAMAGE = 0.08;
	private static final int DURATION = 6 * 20;
	private static final int PERIOD = 1 * 20;

	private static final String DAMAGE_EFFECT_NAME = "BasiliskPoisonDamageEffect";

	private final double mPercent;

	public BasiliskPoison(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Basilisk Poison");
		mInfo.mScoreboardId = "BasiliskPoison";
		mInfo.mShorthandName = "BP";
		mInfo.mDescriptions.add("Equips your arrows with a damage over time that deals 5% of your bow shot every 1s for 6s.");
		mInfo.mDescriptions.add("Damage over time is improved to 8%.");
		mInfo.mLinkedSpell = ClassAbility.BASILISK_POISON;
		mPercent = getAbilityScore() == 1 ? BASILISK_POISON_1_PERCENT_DAMAGE : BASILISK_POISON_2_PERCENT_DAMAGE;
		mDisplayItem = new ItemStack(Material.POISONOUS_POTATO, 1);
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		if (mPlayer == null) {
			return true;
		}
		if (proj instanceof Arrow || proj instanceof SpectralArrow) {
			World world = mPlayer.getWorld();
			mPlugin.mEffectManager.addEffect(damagee, DAMAGE_EFFECT_NAME, new CustomDamageOverTime(DURATION, event.getDamage() * mPercent, PERIOD, mPlayer, MagicType.ALCHEMY, ClassAbility.BASILISK_POISON, Particle.TOTEM, mPlugin));
			world.spawnParticle(Particle.TOTEM, damagee.getLocation().add(0, 1.6, 0), 12, 0.4, 0.4, 0.4, 0.1);
			world.playSound(damagee.getLocation(), Sound.ENTITY_CREEPER_HURT, 1, 1.6f);
		}

		return true;
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.TOTEM);
		return true;
	}
}
