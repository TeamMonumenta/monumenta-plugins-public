package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SoothsayerPassive;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CleansingTotem extends TotemAbility {

	private static final String ATTR_NAME = "CleansingTotemHealing";
	private static final int EFFECT_DURATION = 2 * 20;

	public static final Particle.DustOptions DUST_CLEANSING_RING = new Particle.DustOptions(Color.fromRGB(0, 87, 255), 1.25f);

	private static final int COOLDOWN = 30 * 20;
	private static final int AOE_RANGE = 6;
	private static final double HEAL_PERCENT = 0.06;
	private static final int DURATION_1 = 8 * 20;
	private static final int DURATION_2 = 12 * 20;
	public static String TOTEM_NAME = "Cleansing Totem";

	private final int mDuration;
	private final double mHealPercent;

	public static final AbilityInfo<CleansingTotem> INFO =
		new AbilityInfo<>(CleansingTotem.class, "Cleansing Totem", CleansingTotem::new)
			.linkedSpell(ClassAbility.CLEANSING_TOTEM)
			.scoreboardId("CleansingTotem")
			.shorthandName("CT")
			.descriptions(
				String.format("Press left click with a melee weapon while sneaking to summon a cleansing totem. Players within %s blocks of this totem " +
						"heal for %s%% of their maximum health per second. Duration: %ss. Cooldown: %ss.",
					AOE_RANGE,
					(int) (HEAL_PERCENT * 100),
					DURATION_1 / 20,
					COOLDOWN / 20
				),
				String.format("Duration is increased to %ss and now cleanses debuffs for players in range at half and full duration.",
					DURATION_2 / 20)
			)
			.simpleDescription("Summon a totem that heals and cleanses players over its duration.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CleansingTotem::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.BLUE_STAINED_GLASS);

	public CleansingTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Cleansing Totem Projectile", "CleansingTotem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mDuration = isLevelOne() ? DURATION_1 : DURATION_2;
		mHealPercent = HEAL_PERCENT + (SoothsayerPassive.healingBuff(mPlayer) - 1);
	}

	@Override
	public int getTotemDuration() {
		return mDuration;
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks % 20 == 0) {
			List<Player> affectedPlayers = new ArrayList<>(PlayerUtils.playersInRange(standLocation, AOE_RANGE, true));

			for (LivingEntity p : affectedPlayers) {
				double maxHealth = EntityUtils.getMaxHealth(p);
				mPlugin.mEffectManager.clearEffects(p, ATTR_NAME);
				mPlugin.mEffectManager.addEffect(p, ATTR_NAME, new CustomRegeneration(EFFECT_DURATION, maxHealth * mHealPercent, mPlayer, mPlugin));
			}

			PPCircle cleansingRing = new PPCircle(Particle.REDSTONE, standLocation, AOE_RANGE).ringMode(true).count(40).delta(0).extra(0.05).data(DUST_CLEANSING_RING);
			PPSpiral cleansingSpiral = new PPSpiral(Particle.REDSTONE, standLocation, AOE_RANGE).distancePerParticle(0.075).ticks(5).count(1).delta(0).extra(0.05).data(DUST_CLEANSING_RING);
			cleansingRing.spawnAsPlayerActive(mPlayer);
			cleansingSpiral.spawnAsPlayerActive(mPlayer);

			world.playSound(standLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.3f, 0.5f);
		}
		if (isLevelTwo() && (ticks == mDuration / 2 || ticks == mDuration - 1)) {
			List<Player> cleansePlayers = PlayerUtils.playersInRange(mPlayer.getLocation(), AOE_RANGE, true);
			for (Player player : cleansePlayers) {
				PotionUtils.clearNegatives(mPlugin, player);
				EntityUtils.setWeakenTicks(mPlugin, player, 0);
				EntityUtils.setSlowTicks(mPlugin, player, 0);

				if (player.getFireTicks() > 1) {
					player.setFireTicks(1);
				}
			}
			new PPCircle(Particle.HEART, standLocation, AOE_RANGE).ringMode(false).count(30).spawnAsPlayerActive(mPlayer);
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		new PartialParticle(Particle.HEART, standLocation, 45, 0.2, 1.1, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(standLocation, Sound.BLOCK_WOOD_BREAK, 0.7f, 0.5f);
	}
}
