package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class Apocalypse extends DepthsAbility {
	public static final String ABILITY_NAME = "Apocalypse";
	public static final int COOLDOWN = 75 * 20;
	private static final double TRIGGER_HEALTH = 0.25;
	public static final int[] DAMAGE = {40, 50, 60, 70, 80, 100};
	public static final int RADIUS = 5;
	public static final double HEALING = 0.3; //percent health per kill
	public static final double MAX_ABSORPTION = 0.25;
	public static final int ABSORPTION_DURATION = 30 * 20;

	public static final DepthsAbilityInfo<Apocalypse> INFO =
		new DepthsAbilityInfo<>(Apocalypse.class, ABILITY_NAME, Apocalypse::new, DepthsTree.FLAMECALLER, DepthsTrigger.LIFELINE)
			.linkedSpell(ClassAbility.APOCALYPSE)
			.cooldown(COOLDOWN)
			.displayItem(new ItemStack(Material.ORANGE_DYE))
			.descriptions(Apocalypse::getDescription)
			.priorityAmount(10000);

	public Apocalypse(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlocked() || isOnCooldown() || event.getType() == DamageType.TRUE) {
			return;
		}

		// Calculate whether this effect should not be run based on player health.
		double healthRemaining = mPlayer.getHealth() - event.getFinalDamage(true);

		double maxHealth = EntityUtils.getMaxHealth(mPlayer);
		if (healthRemaining > maxHealth * TRIGGER_HEALTH) {
			return;
		}

		putOnCooldown();

		Location loc = mPlayer.getLocation();
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(loc, RADIUS);
		int count = 0;
		for (LivingEntity mob : nearbyMobs) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, DAMAGE[mRarity - 1], mInfo.getLinkedSpell(), true);
			if (mob == null || mob.isDead() || mob.getHealth() <= 0) {
				count++;
			}
		}

		double totalHealing = maxHealth * count * HEALING;
		double healed = PlayerUtils.healPlayer(mPlugin, mPlayer, maxHealth * count * HEALING);

		double absorption = totalHealing - healed;
		AbsorptionUtils.addAbsorption(mPlayer, absorption, maxHealth * MAX_ABSORPTION, ABSORPTION_DURATION);

		World world = mPlayer.getWorld();
		new PartialParticle(Particle.EXPLOSION_HUGE, loc, 10, 2, 2, 2).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.FLAME, loc, 100, 3.5, 3.5, 3.5, 0).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 1);
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1, 0.5f);

		new PartialParticle(Particle.HEART, loc.clone().add(0, 1, 0), count * 7, 0.5, 0.5, 0.5).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.5f * count, 1.6f);
		MessagingUtils.sendActionBarMessage(mPlayer, "Apocalypse has been activated!");
		event.setCancelled(true);
	}

	@Override
	public void onHurtFatal(DamageEvent event) {
		onHurt(event, null, null);
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("When your health drops below " + StringUtils.multiplierToPercentage(TRIGGER_HEALTH) + "%, ignore the hit and instead deal ")
			.append(Component.text(DAMAGE[rarity - 1], color))
			.append(Component.text(" magic damage in a " + RADIUS + " block radius. For each mob that is killed, heal " + StringUtils.multiplierToPercentage(HEALING) + "% of your max health. Healing above your max health is converted into absorption, up to absorption equal to " + StringUtils.multiplierToPercentage(MAX_ABSORPTION) + "% of your max health that lasts " + ABSORPTION_DURATION / 20 + " seconds. Cooldown: " + COOLDOWN / 20 + "s."));
	}


}
