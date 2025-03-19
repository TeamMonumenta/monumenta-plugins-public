package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Eruption implements Enchantment {

	private static final float RADIUS = 5.0f;
	private static final float SAPPER_RADIUS = 8.0f;
	private static final float DAMAGE_PER_LEVEL = 4.0f;

	@Override
	public String getName() {
		return "Eruption";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ERUPTION;
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double level, BlockBreakEvent event) {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (ItemUtils.isPickaxe(item) && event.getBlock().getType() == Material.SPAWNER) {
			if (!SpawnerUtils.tryBreakSpawner(event.getBlock(), 1 + Plugin.getInstance().mItemStatManager.getEnchantmentLevel(event.getPlayer(), EnchantmentType.DRILLING), false)) {
				return;
			}
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(event.getBlock().getLocation(), RADIUS);

			//Get enchant levels on pickaxe
			int fire = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FIRE_ASPECT);
			int ice = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ICE_ASPECT);
			int thunder = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.THUNDER_ASPECT);
			int decay = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.DECAY);
			int bleed = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.BLEEDING);
			int sapper = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.SAPPER) > 0 ? plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.SAPPER) : 0;
			int adrenaline = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ADRENALINE) > 0 ? plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ADRENALINE) : 0;
			int wind = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.WIND_ASPECT) > 0 ? plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.WIND_ASPECT) : 0;

			double damage = DAMAGE_PER_LEVEL * level;
			//Damage any mobs in the area
			for (LivingEntity mob : mobs) {
				DamageUtils.damage(player, mob, DamageType.OTHER, damage, ClassAbility.ERUPTION, false, true);
			}

			//Sapper Interaction
			if (sapper > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_TRIAL_SPAWNER_DETECT_PLAYER, SoundCategory.PLAYERS, 2f, 1.6f);
				player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.05f, 1.5f);
				new PPCircle(Particle.TOTEM, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 0)
					.delta(1, 0.1, 0).extra(2).rotateDelta(true).directionalMode(true).count(32).spawnAsPlayerActive(player);
				for (Player p : PlayerUtils.playersInRange(event.getBlock().getLocation(), SAPPER_RADIUS, true)) {
					new PartialParticle(Particle.VILLAGER_HAPPY, p.getEyeLocation(), 5, 1, 1, 1).spawnAsPlayerActive(player);
					if (p == player) {
						continue;
					}
					PlayerUtils.healPlayer(plugin, p, sapper, player);
				}
			}

			//Adrenaline Interaction
			if (adrenaline > 0) {
				for (Player p : PlayerUtils.playersInRange(event.getBlock().getLocation(), SAPPER_RADIUS, true)) {
					if (p == player) {
						continue;
					}
					// on block break adrenaline is reduced by 50%
					double speed = Adrenaline.PERCENT_SPEED_PER_LEVEL * adrenaline * 0.5;
					int duration = Adrenaline.SPAWNER_DURATION;
					plugin.mEffectManager.addEffect(p, Adrenaline.PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(duration, speed, Adrenaline.PERCENT_SPEED_EFFECT_NAME));
				}
			}

			//Visual feedback
			if (ice > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 0.6f, 1f);
				player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 1.3f);
				new PPCircle(Particle.SNOWFLAKE, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 0)
					.delta(1, 0, 0).extra(0.5).rotateDelta(true).directionalMode(true).count(32).spawnAsPlayerActive(player);
			}
			if (thunder > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 0.6f, 1.5f);
				new PPCircle(Particle.ELECTRIC_SPARK, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 0)
					.delta(5, 0, 0).extra(0.5).rotateDelta(true).directionalMode(true).count(32).spawnAsPlayerActive(player);
			}
			if (decay > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_PUFFER_FISH_DEATH, SoundCategory.PLAYERS, 0.6f, 0.75f);
				player.playSound(player.getLocation(), Sound.BLOCK_GRASS_BREAK, SoundCategory.PLAYERS, 0.6f, 0.5f);
				new PPCircle(Particle.SQUID_INK, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 0)
					.delta(1, 0, 0).extra(1).rotateDelta(true).directionalMode(true).count(32).spawnAsPlayerActive(player);
			}
			if (adrenaline > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_INHALE, SoundCategory.PLAYERS, 0.7f, 1f);
				player.playSound(player.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.PLAYERS, 0.7f, 0.75f);
				new PPCircle(Particle.TRIAL_SPAWNER_DETECTION, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 0)
					.delta(1, 0, 0).extra(0.35).rotateDelta(true).directionalMode(true).count(32).spawnAsPlayerActive(player);
			}
			if (bleed > 0) {
				player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.7f, 0.7f);
				player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.7f, 2f);
				new PPCircle(Particle.CRIT, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 0)
					.delta(1, 0, 0).extra(2.5).rotateDelta(true).directionalMode(true).count(32).spawnAsPlayerActive(player);
			}
			if (wind > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.25f);
				new PPCircle(Particle.CLOUD, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 0)
					.delta(1, 0, 0).extra(0.4).rotateDelta(true).directionalMode(true).count(32).spawnAsPlayerActive(player);
			}
			if (fire > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.6f, 0.9f);
				new PPCircle(Particle.FLAME, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 0)
					.delta(1, 0, 0).extra(0.35).rotateDelta(true).directionalMode(true).count(32).spawnAsPlayerActive(player);
			}
			if (fire + ice + thunder + decay + bleed + adrenaline + wind + sapper == 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.5f, 1.25f);
				new PPCircle(Particle.END_ROD, event.getBlock().getLocation().add(0.5, 0.5, 0.5), 0)
					.delta(1, 0, 0).extra(0.5).rotateDelta(true).directionalMode(true).count(32).spawnAsPlayerActive(player);
			}
		}
	}
}
