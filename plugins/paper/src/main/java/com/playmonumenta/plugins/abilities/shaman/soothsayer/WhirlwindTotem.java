package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.shaman.TotemAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.ShamanCooldownDecreasePerSecond;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public class WhirlwindTotem extends TotemAbility {

	private static final int COOLDOWN = 25 * 20;
	private static final int INTERVAL = 2 * 20;
	private static final int AOE_RANGE = 5;
	private static final int DURATION_1 = 8 * 20;
	private static final int DURATION_2 = 12 * 20;
	private static final double CDR_PERCENT = 0.025;
	private static final int CDR_MAX_PER_SECOND = 20;
	private static final double SPEED_PERCENT = 0.1;
	private static final String WHIRLWIND_SPEED_EFFECT_NAME = "WhirlwindSpeedEffect";
	public static final String TOTEM_NAME = "Whirlwind Totem";

	private final int mDuration;

	public static final AbilityInfo<WhirlwindTotem> INFO =
		new AbilityInfo<>(WhirlwindTotem.class, "Whirlwind Totem", WhirlwindTotem::new)
			.linkedSpell(ClassAbility.WHIRLWIND_TOTEM)
			.scoreboardId("WhirlwindTotem")
			.shorthandName("WWT")
			.descriptions(
				String.format("Swap with a melee weapon to fire a projectile which will summon a Whirlwind Totem. Every %ss, all players in a %s block radius " +
						"have their class cooldowns reduced by %s%% (maximum %ss). Cannot decrease the cooldown on any player's whirlwind totem, and does not stack with other whirlwind totems. (%ss duration, %ss cooldown)",
					INTERVAL / 20,
					AOE_RANGE,
					(int) (CDR_PERCENT * 100),
					CDR_MAX_PER_SECOND / 20,
					DURATION_1 / 20,
					COOLDOWN / 20
				),
				String.format("Totem duration increased to %ss, and now gives %s%% speed to players within range.",
					DURATION_2 / 20,
					(int) (SPEED_PERCENT * 100))
			)
			.simpleDescription("Summon a totem that provides cooldown reduction to players within its radius.")
			.cooldown(COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", WhirlwindTotem::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
				.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.BLUE_STAINED_GLASS);

	public WhirlwindTotem(Plugin plugin, Player player) {
		super(plugin, player, INFO, "Whirlwind Totem Projectile", "WhirlwindTotem");
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mDuration = isLevelOne() ? DURATION_1 : DURATION_2;
	}

	@Override
	public int getTotemDuration() {
		return mDuration;
	}

	@Override
	public void onTotemTick(int ticks, ArmorStand stand, World world, Location standLocation, ItemStatManager.PlayerItemStats stats) {
		if (ticks % INTERVAL == 0) {
			List<Player> affectedPlayers = new ArrayList<>(PlayerUtils.playersInRange(standLocation, AOE_RANGE, true));

			for (Player p : affectedPlayers) {
				mPlugin.mEffectManager.addEffect(p, "WhirlwindTotemCDR", new ShamanCooldownDecreasePerSecond(50, CDR_PERCENT, CDR_MAX_PER_SECOND, mPlayer, mPlugin));
				if (isLevelTwo()) {
					mPlugin.mEffectManager.addEffect(p, WHIRLWIND_SPEED_EFFECT_NAME, new PercentSpeed(50, SPEED_PERCENT, WHIRLWIND_SPEED_EFFECT_NAME));
				}
			}

			PPSpiral windSpiral = new PPSpiral(Particle.SPELL_INSTANT, standLocation, AOE_RANGE).distancePerParticle(.05).ticks(5).count(1).delta(0);
			windSpiral.spawnAsPlayerActive(mPlayer);
			world.playSound(standLocation, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.3f, 0.5f);
		}
	}

	@Override
	public void onTotemExpire(World world, Location standLocation) {
		new PartialParticle(Particle.HEART, standLocation, 45, 0.2, 1.1, 0.2, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(standLocation, Sound.BLOCK_WOOD_BREAK, 0.7f, 0.5f);
	}
}
