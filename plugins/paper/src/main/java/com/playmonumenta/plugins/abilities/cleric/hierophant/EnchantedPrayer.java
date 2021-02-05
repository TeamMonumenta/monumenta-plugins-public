package com.playmonumenta.plugins.abilities.cleric.hierophant;

import com.playmonumenta.plugins.utils.FastUtils;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.effects.EnchantedPrayerAoE;

/*
 * Enchanted Prayer: Jump and shift right-click to enchant the
 * weapons of all players in a 15-block radius (including
 * yourself) with holy magic, making their next melee attack
 * release light energy. The amplified attack deals additional
 * 7 / 12 damage in a 3.5 block radius around the target,
 * while healing the player for 2 / 4 hearts. (Cooldown: 18 s)
 *
 * TODO: The enchanting portion of this ability is not currently very
 * organized/efficient with our current systems setup. A workaround
 * will be used for now but I recommend we get some sort of Custom Effect
 * system implemented for current and future effects like this. - Fire
 */
public class EnchantedPrayer extends Ability {

	private static final int ENCHANTED_PRAYER_COOLDOWN = 20 * 18;
	private static final int ENCHANTED_PRAYER_1_DAMAGE = 7;
	private static final int ENCHANTED_PRAYER_2_DAMAGE = 12;
	private static final int ENCHANTED_PRAYER_1_HEAL = 2;
	private static final int ENCHANTED_PRAYER_2_HEAL = 4;
	private static final int ENCHANTED_PRAYER_RANGE = 15;
	private static final EnumSet<DamageCause> AFFECTED_DAMAGE_CAUSES = EnumSet.of(
			DamageCause.ENTITY_ATTACK,
			DamageCause.PROJECTILE
	);
	
	private final int mDamage;
	private final int mHeal;

	public EnchantedPrayer(Plugin plugin, Player player) {
		super(plugin, player, "Enchanted Prayer");
		mInfo.mScoreboardId = "EPrayer";
		mInfo.mShorthandName = "EP";
		mInfo.mDescriptions.add("Left-clicking in the air while shifted enchants the weapons of all players in a 15 block radius with holy magic. Their next melee or projectile attack deals an additional 7 damage in a 3-block radius while healing the player for 2 hp. Cooldown: 18s.");
		mInfo.mDescriptions.add("Damage is increased to 12. Healing is increased to 4 hp.");
		mInfo.mLinkedSpell = Spells.ENCHANTED_PRAYER;
		mInfo.mTrigger = AbilityTrigger.LEFT_CLICK;
		mInfo.mCooldown = ENCHANTED_PRAYER_COOLDOWN;
		mDamage = getAbilityScore() == 1 ? ENCHANTED_PRAYER_1_DAMAGE : ENCHANTED_PRAYER_2_DAMAGE;
		mHeal = getAbilityScore() == 1 ? ENCHANTED_PRAYER_1_HEAL : ENCHANTED_PRAYER_2_HEAL;
	}

	public static final String ENCHANTED_PRAYER_METAKEY = "EnchantedPrayerMetakey";

	@Override
	public void cast(Action action) {
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 1);
		putOnCooldown();
		new BukkitRunnable() {
			double mRotation = 0;
			final Location mLoc = mPlayer.getLocation();
			double mRadius = 0;

			@Override
			public void run() {

				mRadius += 0.25;
				for (int i = 0; i < 36; i += 1) {
					mRotation += 10;
					double radian1 = Math.toRadians(mRotation);
					mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
					world.spawnParticle(Particle.SPELL_INSTANT, mLoc, 2, 0.15, 0.15, 0.15, 0);
					mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);

				}
				if (mRadius >= 5) {
					this.cancel();
				}

			}

		}.runTaskTimer(mPlugin, 0, 1);
		int enchantedPrayer = getAbilityScore();
		for (Player p : PlayerUtils.playersInRange(mPlayer, ENCHANTED_PRAYER_RANGE, true)) {
			p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 1.0f);
			world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.25, 0, 0.25, 0.01);
			mPlugin.mEffectManager.addEffect(p, "EnchantedPrayerEffect", 
					new EnchantedPrayerAoE(mPlugin, ENCHANTED_PRAYER_COOLDOWN, mDamage, mHeal, p, AFFECTED_DAMAGE_CAUSES));
		}
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && !mPlayer.isOnGround();
	}
}
