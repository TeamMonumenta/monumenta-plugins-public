package com.playmonumenta.plugins.abilities.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.EnchantedPrayerAoE;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class EnchantedPrayer extends Ability {

	private static final int ENCHANTED_PRAYER_COOLDOWN = 20 * 15;
	private static final int ENCHANTED_PRAYER_1_DAMAGE = 9;
	private static final int ENCHANTED_PRAYER_2_DAMAGE = 15;
	private static final double ENCHANTED_PRAYER_1_HEAL = 0.1;
	private static final double ENCHANTED_PRAYER_2_HEAL = 0.2;
	private static final int ENCHANTED_PRAYER_RANGE = 15;
	private static final double ENCHANTED_PRAYER_EFFECT_SIZE = 3.5;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
			DamageType.MELEE,
			DamageType.PROJECTILE
	);

	private final double mDamage;
	private final double mHeal;

	private @Nullable Crusade mCrusade;

	public static final String CHARM_DAMAGE = "Enchanted Prayer Damage";
	public static final String CHARM_HEAL = "Enchanted Prayer Healing";
	public static final String CHARM_RANGE = "Enchanted Prayer Range";
	public static final String CHARM_EFFECT_RANGE = "Enchanted Prayer Attack Range";
	public static final String CHARM_COOLDOWN = "Enchanted Prayer Cooldown";

	public static final AbilityInfo<EnchantedPrayer> INFO =
		new AbilityInfo<>(EnchantedPrayer.class, "Enchanted Prayer", EnchantedPrayer::new)
			.linkedSpell(ClassAbility.ENCHANTED_PRAYER)
			.scoreboardId("EPrayer")
			.shorthandName("EP")
			.descriptions(
				"Swapping while shifted enchants the weapons of all players in a 15 block radius with holy magic. " +
					"Their next melee or projectile attack deals an additional 9 damage in a 3-block radius while healing the player for 10% of max health. Cooldown: 15s.",
				"Damage is increased to 15. Healing is increased to 20% of max health.")
			.cooldown(ENCHANTED_PRAYER_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EnchantedPrayer::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(new ItemStack(Material.CHORUS_FRUIT, 1));

	public EnchantedPrayer(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? ENCHANTED_PRAYER_1_DAMAGE : ENCHANTED_PRAYER_2_DAMAGE);
		mHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEAL, isLevelOne() ? ENCHANTED_PRAYER_1_HEAL : ENCHANTED_PRAYER_2_HEAL);
		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class);
		});
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 1);
		putOnCooldown();
		cancelOnDeath(new BukkitRunnable() {
			final Location mLoc = mPlayer.getLocation().add(0, 0.15, 0);
			double mRadius = 0;

			@Override
			public void run() {
				mRadius += 0.25;
				new PPCircle(Particle.SPELL_INSTANT, mLoc, mRadius).count(72).delta(0.15).spawnAsPlayerActive(mPlayer);
				if (mRadius >= 5) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1));
		for (Player p : PlayerUtils.playersInRange(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RANGE, ENCHANTED_PRAYER_RANGE), true)) {
			p.playSound(p.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 1.0f);
			new PartialParticle(Particle.SPELL_INSTANT, mPlayer.getLocation(), 50, 0.25, 0, 0.25, 0.01).spawnAsPlayerActive(mPlayer);
			mPlugin.mEffectManager.addEffect(p, "EnchantedPrayerEffect",
					new EnchantedPrayerAoE(mPlugin, ENCHANTED_PRAYER_COOLDOWN, mDamage, mHeal, p, AFFECTED_DAMAGE_TYPES, CharmManager.getRadius(mPlayer, CHARM_EFFECT_RANGE, ENCHANTED_PRAYER_EFFECT_SIZE), mCrusade));
		}
	}
}
