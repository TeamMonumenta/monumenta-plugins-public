package com.playmonumenta.plugins.abilities.cleric.hierophant;

import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.EnchantedPrayerAoE;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;



public class EnchantedPrayer extends Ability {

	private static final int ENCHANTED_PRAYER_COOLDOWN = 20 * 18;
	private static final int ENCHANTED_PRAYER_1_DAMAGE = 7;
	private static final int ENCHANTED_PRAYER_2_DAMAGE = 12;
	private static final double ENCHANTED_PRAYER_1_HEAL = 0.1;
	private static final double ENCHANTED_PRAYER_2_HEAL = 0.2;
	private static final int ENCHANTED_PRAYER_RANGE = 15;
	private static final EnumSet<DamageCause> AFFECTED_DAMAGE_CAUSES = EnumSet.of(
			DamageCause.ENTITY_ATTACK,
			DamageCause.PROJECTILE
	);

	private final int mDamage;
	private final double mHeal;

	public EnchantedPrayer(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Enchanted Prayer");
		mInfo.mScoreboardId = "EPrayer";
		mInfo.mShorthandName = "EP";
		mInfo.mDescriptions.add("Swapping while shifted enchants the weapons of all players in a 15 block radius with holy magic. Their next melee or projectile attack deals an additional 7 damage in a 3-block radius while healing the player for 10% of max health. Cooldown: 18s.");
		mInfo.mDescriptions.add("Damage is increased to 12. Healing is increased to 20% of max health.");
		mInfo.mLinkedSpell = ClassAbility.ENCHANTED_PRAYER;
		mInfo.mCooldown = ENCHANTED_PRAYER_COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.CHORUS_FRUIT, 1);
		mDamage = getAbilityScore() == 1 ? ENCHANTED_PRAYER_1_DAMAGE : ENCHANTED_PRAYER_2_DAMAGE;
		mHeal = getAbilityScore() == 1 ? ENCHANTED_PRAYER_1_HEAL : ENCHANTED_PRAYER_2_HEAL;
	}

	public static final String ENCHANTED_PRAYER_METAKEY = "EnchantedPrayerMetakey";

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		if (mPlayer == null) {
			return;
		}
		if (mPlayer.isSneaking()) {
			event.setCancelled(true);
			if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
				return;
			}
		} else {
			return;
		}

		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		if (ItemUtils.isSomeBow(mainHand)) {
			return;
		}

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
		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), ENCHANTED_PRAYER_RANGE, true)) {
			p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 1.0f);
			world.spawnParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.25, 0, 0.25, 0.01);
			mPlugin.mEffectManager.addEffect(p, "EnchantedPrayerEffect",
					new EnchantedPrayerAoE(mPlugin, ENCHANTED_PRAYER_COOLDOWN, mDamage, mHeal, p, AFFECTED_DAMAGE_CAUSES));
		}
	}
}
