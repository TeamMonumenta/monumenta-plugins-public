package com.playmonumenta.plugins.depths.abilities.steelsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class DepthsSplitArrow extends DepthsAbility {

	private static final int SPLIT_ARROW_CHAIN_RANGE = 5;
	public static final String ABILITY_NAME = "Split Arrow";
	public static final double[] DAMAGE_MOD = {.40, .50, .60, .70, .80, 1.00};
	private static final PotionEffect SPECTRAL_ARROW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 200, 0);
	private static final int IFRAMES = 10;

	public static final DepthsAbilityInfo<DepthsSplitArrow> INFO =
		new DepthsAbilityInfo<>(DepthsSplitArrow.class, ABILITY_NAME, DepthsSplitArrow::new, DepthsTree.STEELSAGE, DepthsTrigger.PASSIVE)
			.displayItem(new ItemStack(Material.CHAIN))
			.descriptions(DepthsSplitArrow::getDescription);

	public DepthsSplitArrow(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile proj && EntityUtils.isAbilityTriggeringProjectile(proj, false) && !proj.hasMetadata(RapidFire.META_DATA_TAG) && EntityUtils.isHostileMob(enemy)) {
			LivingEntity nearestMob = EntityUtils.getNearestMob(enemy.getLocation(), SPLIT_ARROW_CHAIN_RANGE, enemy);

			if (nearestMob != null && !nearestMob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
				Location loc = enemy.getEyeLocation();
				Location eye = nearestMob.getEyeLocation();
				Vector dir = LocationUtils.getDirectionTo(eye, loc);
				World world = mPlayer.getWorld();
				for (int i = 0; i < 50; i++) {
					loc.add(dir.clone().multiply(0.1));
					new PartialParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0).spawnAsPlayerActive(mPlayer);
					if (loc.distance(eye) < 0.4) {
						break;
					}
				}

				if (!EntityUtils.hasArrowIframes(mPlugin, nearestMob)) {
					new PartialParticle(Particle.CRIT, eye, 30, 0, 0, 0, 0.6).spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.CRIT_MAGIC, eye, 20, 0, 0, 0, 0.6).spawnAsPlayerActive(mPlayer);
					world.playSound(eye, Sound.ENTITY_ARROW_HIT, SoundCategory.PLAYERS, 1, 1.2f);

					if (proj instanceof SpectralArrow) {
						nearestMob.addPotionEffect(SPECTRAL_ARROW_EFFECT);
					}

					DamageUtils.damage(mPlayer, nearestMob, DamageType.OTHER, event.getDamage() * DAMAGE_MOD[mRarity - 1], mInfo.getLinkedSpell(), true);
					MovementUtils.knockAway(enemy, nearestMob, 0.125f, 0.35f, true);
					EntityUtils.applyArrowIframes(mPlugin, IFRAMES, nearestMob);
				}
			}
		}
		return false; // applies damage of type OTHER for damage of type PROJECTILE, which should not cause recursion with any other ability (or itself)
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("When you shoot an enemy with a projectile, the nearest enemy within " + SPLIT_ARROW_CHAIN_RANGE + " blocks takes ")
			.append(Component.text(StringUtils.multiplierToPercentage(DAMAGE_MOD[rarity - 1]) + "%", color))
			.append(Component.text(" of the projectile's damage."));
	}
}

