package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Brutalize extends DepthsAbility {

	public static final String ABILITY_NAME = "Brutalize";
	public static final double[] DAMAGE = {0.12, 0.15, 0.18, 0.21, 0.24, 0.30};
	public static final int RADIUS = 2;

	public static final DepthsAbilityInfo<Brutalize> INFO =
		new DepthsAbilityInfo<>(Brutalize.class, ABILITY_NAME, Brutalize::new, DepthsTree.SHADOWDANCER, DepthsTrigger.PASSIVE)
			.displayItem(new ItemStack(Material.STONE_SWORD))
			.descriptions(Brutalize::getDescription);

	public Brutalize(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(mPlayer)) {
			double originalDamage = event.getDamage();
			double brutalizeDamage = DAMAGE[mRarity - 1] * originalDamage;
			event.setDamage(originalDamage + brutalizeDamage);
			Location loc = enemy.getLocation();
			mPlayer.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.75f, 1.65f);
			mPlayer.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.75f, 0.5f);
			new PartialParticle(Particle.SPELL_WITCH, loc, 15, 0.5, 0.2, 0.5, 0.65).spawnAsPlayerActive(mPlayer);
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RADIUS, enemy)) {
				DamageUtils.damage(mPlayer, mob, DamageType.OTHER, brutalizeDamage, null, false, true);
				MovementUtils.knockAway(enemy, mob, 0.5f, true);

				new PartialParticle(Particle.SPELL_WITCH, mob.getLocation(), 10, 0.5, 0.2, 0.5, 0.65).spawnAsPlayerActive(mPlayer);
			}
			return true;
		}
		return false;
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("When you critically strike you deal ")
			.append(Component.text(StringUtils.multiplierToPercentage(DAMAGE[rarity - 1]), color))
			.append(Component.text(" of the damage to all enemies in a " + RADIUS + " block radius and knock other enemies away from the target."));
	}


}

