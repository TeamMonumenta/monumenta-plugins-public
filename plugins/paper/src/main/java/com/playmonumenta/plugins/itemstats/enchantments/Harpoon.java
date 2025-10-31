package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

public class Harpoon implements Enchantment {
	private static final float KB_VEL_BASE = 0.6f;
	private static final float KB_VEL_PER_LEVEL = 0.6f;
	private static final float VERTICAL_LAUNCH = 0.28f;

	@Override
	public String getName() {
		return "Harpoon";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.HARPOON;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 32;
	}

	@Override
	public void onProjectileHit(Plugin plugin, Player player, double level, ProjectileHitEvent event, Projectile projectile) {
		com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin;
		scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin) Bukkit.getPluginManager().getPlugin("ScriptedQuests");
		if (!(event.getHitEntity() instanceof LivingEntity enemy)
			|| event.getHitEntity() instanceof Villager
			|| event.getHitEntity() instanceof ArmorStand
			|| scriptedQuestsPlugin.mNpcManager.isQuestNPC(event.getHitEntity())) {
			return;
		}
		float speed = -KB_VEL_BASE - KB_VEL_PER_LEVEL * (float) level;
		// Enemy is Harpooned with fixed Y velocity, in the horizontal direction the arrow was travelling, with fixed speed.
		Vector vector = projectile.getVelocity().clone()
			.setY(0);
		if (vector.length() < 0.001) {
			vector = new Vector(0, VERTICAL_LAUNCH, 0);
		} else {
			vector.normalize()
				.multiply(speed)
				.setY(VERTICAL_LAUNCH);
		}
		// TODO: Override the Minecraft Punch behaviour so that it doesn't perform a normal amount of KB via... mixin? Then rewrite this section so that it doesn't delay the KB by one tick
		// Sorry. Java requires that I input a "final" into the lambda expression.
		final Vector dir = vector.clone();
		Bukkit.getScheduler().runTask(plugin, () -> MovementUtils.knockAwayDirection(dir, enemy, 0.5f, true, true));
	}
}
