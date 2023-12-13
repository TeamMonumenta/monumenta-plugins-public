package com.playmonumenta.plugins.abilities.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant.EnchantedPrayerCS;
import com.playmonumenta.plugins.effects.EnchantedPrayerAoE;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class EnchantedPrayer extends Ability {

	private static final int ENCHANTED_PRAYER_COOLDOWN = 20 * 15;
	private static final int ENCHANTED_PRAYER_1_DAMAGE = 12;
	private static final int ENCHANTED_PRAYER_2_DAMAGE = 20;
	private static final double ENCHANTED_PRAYER_1_HEAL = 0.1;
	private static final double ENCHANTED_PRAYER_2_HEAL = 0.2;
	private static final int ENCHANTED_PRAYER_RANGE = 15;
	private static final double ENCHANTED_PRAYER_EFFECT_SIZE = 3.5;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
			DamageType.MELEE,
			DamageType.PROJECTILE
	);

	public static final String CHARM_DAMAGE = "Enchanted Prayer Damage";
	public static final String CHARM_HEAL = "Enchanted Prayer Healing";
	public static final String CHARM_RANGE = "Enchanted Prayer Range";
	public static final String CHARM_EFFECT_RANGE = "Enchanted Prayer Attack Range";
	public static final String CHARM_COOLDOWN = "Enchanted Prayer Cooldown";

	private final double mDamage;
	private final double mHeal;

	private @Nullable Crusade mCrusade;

	private final EnchantedPrayerCS mCosmetic;

	public static final AbilityInfo<EnchantedPrayer> INFO =
		new AbilityInfo<>(EnchantedPrayer.class, "Enchanted Prayer", EnchantedPrayer::new)
			.linkedSpell(ClassAbility.ENCHANTED_PRAYER)
			.scoreboardId("EPrayer")
			.shorthandName("EP")
			.descriptions(
				"Swapping while sneaking enchants the weapons of all players in a 15 block radius with holy magic. " +
					"Their next melee or projectile attack deals an additional 12 damage in a 3-block radius while healing the player for 10% of max health. Cooldown: 15s.",
				"Damage is increased to 20. Healing is increased to 20% of max health.")
			.simpleDescription("The next attack by you and nearby players causes an explosion and heals the player.")
			.cooldown(ENCHANTED_PRAYER_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EnchantedPrayer::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.displayItem(Material.CHORUS_FRUIT);

	public EnchantedPrayer(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? ENCHANTED_PRAYER_1_DAMAGE : ENCHANTED_PRAYER_2_DAMAGE);
		mHeal = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEAL, isLevelOne() ? ENCHANTED_PRAYER_1_HEAL : ENCHANTED_PRAYER_2_HEAL);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EnchantedPrayerCS());
		Bukkit.getScheduler().runTask(plugin, () -> {
			mCrusade = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class);
		});
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		mCosmetic.onCast(mPlugin, mPlayer, world, loc);

		for (Player p : PlayerUtils.playersInRange(loc, CharmManager.getRadius(mPlayer, CHARM_RANGE, ENCHANTED_PRAYER_RANGE), true)) {
			mCosmetic.applyToPlayer(p, mPlayer);
			mPlugin.mEffectManager.addEffect(p, "EnchantedPrayerEffect",
					new EnchantedPrayerAoE(mPlugin, ENCHANTED_PRAYER_COOLDOWN, mDamage, mHeal, p, AFFECTED_DAMAGE_TYPES, CharmManager.getRadius(mPlayer, CHARM_EFFECT_RANGE, ENCHANTED_PRAYER_EFFECT_SIZE), mPlayer, mCrusade, mCosmetic));
		}
		return true;
	}
}
