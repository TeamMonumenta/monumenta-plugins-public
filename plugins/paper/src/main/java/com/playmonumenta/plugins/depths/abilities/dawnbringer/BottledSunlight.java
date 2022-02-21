package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BottledSunlight extends DepthsAbility {

	public static final String ABILITY_NAME = "Bottled Sunlight";
	private static final int COOLDOWN = 12 * 20;
	private static final int[] ABSORPTION = {8, 10, 12, 14, 16, 20};
	private static final int BOTTLE_THROW_COOLDOWN = 10 * 20;
	private static final int BOTTLE_USE_COOLDOWN = 30 * 20;
	private static final int BOTTLE_ABSORPTION_DURATION = 20 * 30;
	private static final double BOTTLE_VELOCITY = 0.7;
	private static final int BOTTLE_TICK_PERIOD = 2;
	private static final int EFFECT_DURATION_REDUCTION = 10 * 20;

	public BottledSunlight(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayMaterial = Material.HONEY_BOTTLE;
		mTree = DepthsTree.SUNLIGHT;
		mInfo.mLinkedSpell = ClassAbility.BOTTLED_SUNLIGHT;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
	}

	@Override
	public void cast(Action action) {

		Location loc = mPlayer.getEyeLocation();
		ItemStack itemTincture = new ItemStack(Material.HONEY_BOTTLE);
		ItemUtils.setPlainName(itemTincture, "Bottled Sunlight");
		ItemMeta tinctureMeta = itemTincture.getItemMeta();
		tinctureMeta.displayName(Component.text("Bottled Sunlight", NamedTextColor.WHITE)
				.decoration(TextDecoration.ITALIC, false));
		itemTincture.setItemMeta(tinctureMeta);
		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_SNOWBALL_THROW, 1, 0.15f);
		Item tincture = world.dropItem(loc, itemTincture);
		tincture.setPickupDelay(Integer.MAX_VALUE);

		Vector vel = mPlayer.getEyeLocation().getDirection().normalize();
		vel.multiply(BOTTLE_VELOCITY);

		tincture.setVelocity(vel);
		tincture.setGlowing(true);

		mInfo.mCooldown = BOTTLE_USE_COOLDOWN;
		// Full duration cooldown - is shortened if not picked up
		putOnCooldown();

		new BukkitRunnable() {
			int mTinctureDecay = 0;

			@Override
			public void run() {
				world.spawnParticle(Particle.SPELL, tincture.getLocation(), 3, 0, 0, 0, 0.1);

				for (Player p : PlayerUtils.playersInRange(tincture.getLocation(), 1, true)) {
					// Prevent players from picking up their own tincture instantly
					if (p == mPlayer && tincture.getTicksLived() < 12) {
						continue;
					}

					world.playSound(tincture.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.85f);
					world.spawnParticle(Particle.BLOCK_DUST, tincture.getLocation(), 50, 0.1, 0.1, 0.1, 0.1, Material.GLASS.createBlockData());
					world.spawnParticle(Particle.FIREWORKS_SPARK, tincture.getLocation(), 30, 0.1, 0.1, 0.1, 0.2);
					tincture.remove();

					execute(mPlayer);
					if (p != mPlayer) {
						execute(p);
					}

					mPlugin.mTimers.removeCooldown(mPlayer, mInfo.mLinkedSpell);
					putOnCooldown();

					this.cancel();
					return;
				}

				mTinctureDecay += BOTTLE_TICK_PERIOD;
				if (mTinctureDecay >= BOTTLE_THROW_COOLDOWN || !tincture.isValid() || tincture.isDead()) {
					tincture.remove();
					this.cancel();

					// Take the skill off cooldown (by setting to 0)
					mPlugin.mTimers.addCooldown(mPlayer, mInfo.mLinkedSpell, 0);
				}
			}

		}.runTaskTimer(mPlugin, 0, BOTTLE_TICK_PERIOD);
	}

	private void execute(Player player) {
		int absorption = ABSORPTION[mRarity - 1];

		AbsorptionUtils.addAbsorption(player, absorption, absorption, BOTTLE_ABSORPTION_DURATION);

		//Cleanse debuffs
		for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
			PotionEffect effect = player.getPotionEffect(effectType);
			if (effect != null) {
				player.removePotionEffect(effectType);
				// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
				player.addPotionEffect(new PotionEffect(effectType, Math.max(effect.getDuration() - EFFECT_DURATION_REDUCTION, 0), effect.getAmplifier()));
			}
		}

		World world = player.getWorld();
		world.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.2f, 1.0f);
		world.spawnParticle(Particle.FLAME, player.getLocation(), 30, 0.25, 0.1, 0.25, 0.125);
		new BukkitRunnable() {
			double mRotation = 0;
			double mY = 0.15;
			final double mRadius = 1.15;
			@Override
			public void run() {
				Location loc = player.getLocation();
				mRotation += 15;
				mY += 0.175;
				for (int i = 0; i < 3; i++) {
					double degree = Math.toRadians(mRotation + (i * 120));
					loc.add(FastUtils.cos(degree) * mRadius, mY, FastUtils.sin(degree) * mRadius);
					world.spawnParticle(Particle.FLAME, loc, 1, 0.05, 0.05, 0.05, 0.05);
					world.spawnParticle(Particle.SPELL_INSTANT, loc, 2, 0.05, 0.05, 0.05, 0);
					loc.subtract(FastUtils.cos(degree) * mRadius, mY, FastUtils.sin(degree) * mRadius);
				}

				if (mY >= 1.8) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean runCheck() {
		return mPlayer.isSneaking() && DepthsUtils.isWeaponItem(mPlayer.getInventory().getItemInMainHand());
	}


	@Override
	public String getDescription(int rarity) {
		return "Right click while sneaking to throw a luminescent bottle. If you or an ally walk over it, you both gain " + DepthsUtils.getRarityColor(rarity) + ABSORPTION[rarity - 1] / 2 + ChatColor.WHITE + " absorption hearts for " + BOTTLE_ABSORPTION_DURATION / 20 + " seconds and the durations of negative potion effects get reduced by " + EFFECT_DURATION_REDUCTION / 20 + " seconds. If the bottle isn't grabbed, it quickly comes off cooldown. Cooldown: " + BOTTLE_USE_COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SUNLIGHT;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_RIGHT_CLICK;
	}
}

