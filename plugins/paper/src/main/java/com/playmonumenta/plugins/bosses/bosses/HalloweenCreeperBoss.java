package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class HalloweenCreeperBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_halloween_creeper";

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new HalloweenCreeperBoss(plugin, boss);
	}

	public HalloweenCreeperBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Creeper creeper)) {
			throw new Exception(identityTag + " only works on mobs!");
		}

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), 100, null);

		if (!creeper.isIgnited()) {
			creeper.setCustomName(ChatColor.GOLD + "Tricky Creeper");
			creeper.setGlowing(true);
			creeper.setExplosionRadius(20);
			creeper.setMaxFuseTicks(100);
			creeper.setIgnited(true);
			creeper.addScoreboardTag(CrowdControlImmunityBoss.identityTag);
			creeper.addScoreboardTag(AbilityUtils.IGNORE_TAG);

			new BukkitRunnable() {
				private int mTicks = 0;

				@Override
				public void run() {
					Location loc = boss.getLocation().add(0, 1, 0);
					World world = loc.getWorld();
					world.playSound(loc, Sound.ENTITY_CREEPER_HURT, SoundCategory.HOSTILE, 1.0f, 0.9f);
					switch (mTicks) {
						case 12 -> {
							Block block = world.getBlockAt(loc);
							if (block.getType() == Material.AIR || block.isLiquid()) {
								block.setType(Material.CHEST);
								if (block.getState() instanceof Chest chest) {
									chest.setCustomName(ChatColor.GOLD + "" + ChatColor.BOLD + "Creeperween Chest");
									chest.setLootTable(Bukkit.getLootTable(NamespacedKeyUtils.fromString("epic:event/halloween2019/tricked_creeper")));
									chest.update();
								}
							}
						}
						case 2, 6 -> summonFirework(loc, true);
						case 9 -> summonFirework(loc, false);
						case 13 -> {
							summonFirework(loc, false);
							this.cancel();
						}
						default -> {
						}
					}
					mTicks++;
				}
			}.runTaskTimer(plugin, 0, 10);
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		// Reduce the damage but don't cancel the hit
		event.setDamage(0);
	}

	private void summonFirework(Location loc, boolean first) {
		loc.getWorld().spawn(loc, Firework.class, firework -> {
			FireworkMeta fwm = firework.getFireworkMeta();
			FireworkEffect.Builder fwBuilder = FireworkEffect.builder();
			fwBuilder.withColor(Color.fromRGB(255, 106, 31));
			if (first) {
				fwBuilder.with(FireworkEffect.Type.BALL_LARGE);
			} else {
				fwBuilder.withFlicker();
				fwBuilder.with(FireworkEffect.Type.CREEPER);
			}
			FireworkEffect fwEffect = fwBuilder.build();
			fwm.addEffect(fwEffect);
			firework.setFireworkMeta(fwm);
			firework.setSilent(true);
			firework.detonate();
		});

	}
}
