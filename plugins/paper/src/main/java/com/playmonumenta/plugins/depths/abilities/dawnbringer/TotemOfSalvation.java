package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;

public class TotemOfSalvation extends DepthsAbility {

	public static final String ABILITY_NAME = "Totem of Salvation";
	public static final int COOLDOWN = 20 * 40;
	public static final int[] TICK_FREQUENCY = {40, 35, 30, 25, 20, 10};
	private static final double VELOCITY = 0.5;
	public static final int DURATION = 15 * 20;
	private static final int EFFECT_RADIUS = 5;
	private static final double PARTICLE_RING_HEIGHT = 1.0;
	private static final double PERCENT_HEALING = 0.08;
	private static final int MAX_ABSORPTION = 4;
	private static final int ABSORPTION_DURATION = 5 * 20;
	private static final Particle.DustOptions PARTICLE_COLOR = new Particle.DustOptions(Color.fromRGB(254, 212, 38), 1.0f);

	private static final Collection<Map.Entry<Double, SpawnParticleAction>> PARTICLES =
			Arrays.asList(new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.4, (Location loc) -> loc.getWorld().spawnParticle(Particle.REDSTONE, loc, 1, 0.1, 0.1, 0.1, PARTICLE_COLOR)));

	public TotemOfSalvation(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.TOTEM_OF_UNDYING;
		mTree = DepthsTree.SUNLIGHT;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mLinkedSpell = ClassAbility.TOTEM_OF_SALVATION;
		mInfo.mIgnoreCooldown = true;
	}

	@Override
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {

		event.setCancelled(true);

		if ((!isTimerActive() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand()))) {
			putOnCooldown();

			Location loc = mPlayer.getEyeLocation();
			ItemStack itemTincture = new ItemStack(Material.TOTEM_OF_UNDYING);
			ItemUtils.setPlainName(itemTincture, "Totem of Salvation");
			ItemMeta sundropMeta = itemTincture.getItemMeta();
			sundropMeta.displayName(Component.text("Totem of Salvation", NamedTextColor.WHITE)
	                .decoration(TextDecoration.ITALIC, false));
			itemTincture.setItemMeta(sundropMeta);
			World world = mPlayer.getWorld();
			Item item = world.dropItem(loc, itemTincture);
			item.setPickupDelay(Integer.MAX_VALUE);
			item.setInvulnerable(true);

			Vector vel = mPlayer.getEyeLocation().getDirection().normalize();
			vel.multiply(VELOCITY);

			item.setVelocity(vel);
			item.setGlowing(true);
			world.playSound(loc, Sound.ENTITY_SNOWBALL_THROW, 1.0f, 2.5f);

			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (!item.getLocation().isChunkLoaded() || item.isDead() || !item.isValid()) {
						this.cancel();
						return;
					}

					//Particles once per second
					if (mTicks % 20 == 0) {
						ParticleUtils.explodingRingEffect(mPlugin, item.getLocation().add(0, 0.5, 0), EFFECT_RADIUS, PARTICLE_RING_HEIGHT, 20, PARTICLES);
					}

					//Heal nearby players once per rarity frequency
					if (mTicks % TICK_FREQUENCY[mRarity - 1] == 0) {
						for (Player p : PlayerUtils.playersInRange(item.getLocation(), EFFECT_RADIUS, true)) {
							AttributeInstance maxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
							if (maxHealth != null) {
								double maxHealthValue = maxHealth.getValue();
								double healthFromFull = maxHealthValue - p.getHealth();
								double healthToHeal = maxHealthValue * PERCENT_HEALING;

								PlayerUtils.healPlayer(p, healthToHeal);

								double remainingHealing = healthToHeal - healthFromFull;
								if (remainingHealing > 0) {
									AbsorptionUtils.addAbsorption(p, remainingHealing, MAX_ABSORPTION, ABSORPTION_DURATION);
								}
							}
						}
					}

					mTicks += 5;

					if (mTicks >= DURATION) {
						item.remove();
						this.cancel();
					}

					// Very infrequently check if the item is still actually there
					if (mTicks % 100 == 0) {
						if (!EntityUtils.isStillLoaded(item)) {
							this.cancel();
						}
					}
				}
			}.runTaskTimer(mPlugin, 0, 5);
		}
	}

	@Override
	public String getDescription(int rarity) {
		String s = "s";
		if (TICK_FREQUENCY[rarity - 1] == 20) {
			s = "";
		}
		return "Swap hands while holding a weapon to summon a totem that lasts " + DURATION / 20 + " second. The totem heals all players within " + EFFECT_RADIUS + " blocks by " + (int) DepthsUtils.roundPercent(PERCENT_HEALING) + "% of their max health every " + DepthsUtils.getRarityColor(rarity) + TICK_FREQUENCY[rarity - 1] / 20.0 + ChatColor.WHITE + " second" + s + ". If a player has full health, the healing will be converted into absorption that lasts " + ABSORPTION_DURATION / 20 + " seconds and caps at " + MAX_ABSORPTION / 2 + " hearts. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SUNLIGHT;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SWAP;
	}
}

