package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

public class Punch implements Enchantment {
	private static final float KB_VEL_BASE = 0.2f;
	private static final float KB_VEL_PER_LEVEL = 0.6f;
	private static final float VERTICAL_LAUNCH = 0.28f;

	public static void applyPunch(Plugin plugin, double level, Entity entity, Vector projVelocity) {
		if (level <= 0) {
			return;
		}
		com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin) Bukkit.getPluginManager().getPlugin("ScriptedQuests");
		if (!(entity instanceof LivingEntity enemy)
			|| enemy instanceof Villager
			|| enemy instanceof ArmorStand
			|| enemy instanceof Player
			|| (scriptedQuestsPlugin != null && scriptedQuestsPlugin.mNpcManager.isQuestNPC(entity))) {
			return;
		}
		float speed = KB_VEL_BASE + KB_VEL_PER_LEVEL * (float) level;
		// Enemy is punched with fixed Y velocity, in the horizontal direction the arrow was travelling, with fixed speed.
		Vector vector = projVelocity.clone()
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
		Bukkit.getScheduler().runTask(plugin, () -> MovementUtils.knockAwayDirection(dir, enemy, 0.5f, true, false));
	}

	@Override
	public String getName() {
		return "Punch";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.PUNCH;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 31;
	}

	@Override
	public void onProjectileHit(Plugin plugin, Player player, double level, ProjectileHitEvent event, Projectile projectile) {
		applyPunch(plugin, level, event.getHitEntity(), projectile.getVelocity());
	}
}
