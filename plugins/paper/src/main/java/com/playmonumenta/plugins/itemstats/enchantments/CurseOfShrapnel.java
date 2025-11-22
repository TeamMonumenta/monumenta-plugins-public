package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class CurseOfShrapnel implements Enchantment {
	private static final double PICKAXE_DAMAGE = 1;
	private static final double PROJECTILE_DAMAGE = 1;
	private static final double MELEE_DAMAGE = 0.5;

	@Override
	public String getName() {
		return "Curse of Shrapnel";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_SHRAPNEL;
	}

	@Override
	public double getPriorityAmount() {
		return 1;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double level, BlockBreakEvent event) {
		if (ItemUtils.isPickaxe(player.getInventory().getItemInMainHand()) && event.getBlock().getType() == Material.SPAWNER) {
			if (!SpawnerUtils.tryBreakSpawner(event.getBlock(), 1 + Plugin.getInstance().mItemStatManager.getEnchantmentLevel(event.getPlayer(), EnchantmentType.DRILLING), false)) {
				return;
			}
			new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.1).spawnAsPlayerActive(player);
			DamageUtils.damage(null, player, DamageEvent.DamageType.TRUE, level * PICKAXE_DAMAGE, null, true, false);
		}
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double level, ProjectileLaunchEvent event, Projectile proj) {
		// need once per tick check for multishot and volley
		if (MetadataUtils.checkOnceThisTick(plugin, player, "ShrapnelProjectileThisTick")) {
			DamageUtils.damage(null, player, DamageType.TRUE, level * PROJECTILE_DAMAGE, null, true, false);
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity target) {
		if (event.getType() != DamageType.MELEE || !EntityUtils.isHostileMob(target)) {
			return;
		}

		DamageUtils.damage(null, player, DamageType.TRUE, level * MELEE_DAMAGE, null, true, false);
	}

}
