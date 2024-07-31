package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Sundrops extends DepthsAbility {

	//Technical implementation of this ability is handled in the depths listener, so that any member of the party can benefit from it

	public static final String ABILITY_NAME = "Sundrops";
	public static final double[] DROP_CHANCE = {0.2, 0.25, 0.3, 0.35, 0.4, 0.6};
	private static final int LINGER_TIME = 10 * 20;
	private static final int DURATION = 8 * 20;
	private static final double PERCENT_SPEED = .2;
	private static final double PERCENT_DAMAGE_RECEIVED = 0.2;
	private static final String PERCENT_SPEED_EFFECT_NAME = "SundropsPercentSpeedEffect";
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "SundropsPercentDamageReceivedEffect";

	public static final DepthsAbilityInfo<Sundrops> INFO =
		new DepthsAbilityInfo<>(Sundrops.class, ABILITY_NAME, Sundrops::new, DepthsTree.DAWNBRINGER, DepthsTrigger.SPAWNER)
			.displayItem(Material.HONEYCOMB_BLOCK)
			.descriptions(Sundrops::getDescription);

	private final double mDropChance;
	private final int mLingerTime;
	private final int mEffectDuration;
	private final double mSpeed;
	private final double mDamageReduction;

	public Sundrops(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDropChance = DROP_CHANCE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.SUNDROPS_DROP_CHANCE.mEffectName);
		mLingerTime = CharmManager.getDuration(mPlayer, CharmEffects.SUNDROPS_LINGER_TIME.mEffectName, LINGER_TIME);
		mEffectDuration = CharmManager.getDuration(mPlayer, CharmEffects.SUNDROPS_EFFECT_DURATION.mEffectName, DURATION);
		mDamageReduction = PERCENT_DAMAGE_RECEIVED + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.SUNDROPS_RESISTANCE_AMPLIFIER.mEffectName);
		mSpeed = PERCENT_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.SUNDROPS_SPEED_AMPLIFIER.mEffectName);
	}

	public void summonSundrop(Location loc) {
		World world = loc.getWorld();
		Item sundrop = AbilityUtils.spawnAbilityItem(world, loc, Material.HONEYCOMB_BLOCK, "Sundrop", true, 0, true, true);

		new BukkitRunnable() {
			int mT = 0;
			final BlockData mFallingDustData = Material.HONEYCOMB_BLOCK.createBlockData();
			@Override
			public void run() {
				mT++;
				Location l = sundrop.getLocation();
				new PartialParticle(Particle.FALLING_DUST, l, 1, 0.2, 0.2, 0.2, mFallingDustData).spawnAsOtherPlayerActive();
				//Other player
				for (Player p : new Hitbox.UprightCylinderHitbox(l, 0.7, 0.7).getHitPlayers(true)) {
					Plugin plugin = Plugin.getInstance();
					//Give speed and resistance
					plugin.mEffectManager.addEffect(p, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME, new PercentDamageReceived(mEffectDuration, -mDamageReduction));
					plugin.mEffectManager.addEffect(p, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(mEffectDuration, mSpeed, PERCENT_SPEED_EFFECT_NAME));

					world.playSound(loc, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
					world.playSound(loc, Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1, 0.75f);
					world.playSound(loc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1, 1f);
					new PartialParticle(Particle.BLOCK_CRACK, l, 30, 0.15, 0.15, 0.15, 0.75F, Material.HONEYCOMB_BLOCK.createBlockData()).spawnAsOtherPlayerActive();
					new PartialParticle(Particle.TOTEM, l, 20, 0, 0, 0, 0.35F).spawnAsOtherPlayerActive();

					this.cancel();
					sundrop.remove();
					break;
				}
				if (mT >= mLingerTime || sundrop.isDead()) {
					this.cancel();
					sundrop.remove();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public double getChance() {
		return mDropChance;
	}

	private static Description<Sundrops> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Sundrops>(color)
			.add("Whenever a player in your party breaks a spawner, there is a ")
			.addPercent(a -> a.mDropChance, DROP_CHANCE[rarity - 1], false, true)
			.add(" chance of spawning a sundrop that lasts ")
			.addDuration(a -> a.mLingerTime, LINGER_TIME)
			.add(" seconds. Picking up a sundrop gives ")
			.addPercent(a -> a.mSpeed, PERCENT_SPEED)
			.add(" speed and ")
			.addPercent(a -> a.mDamageReduction, PERCENT_DAMAGE_RECEIVED)
			.add(" resistance for ")
			.addDuration(a -> a.mEffectDuration, DURATION)
			.add(" seconds. Spawn chance stacks with other players in your party who have the skill, up to 100%.");
	}

}

