package pe.project.tracking;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatColor;
import pe.project.Constants;
import pe.project.locations.safezones.SafeZoneConstants;
import pe.project.locations.safezones.SafeZoneConstants.SafeZones;
import pe.project.managers.LocationManager;
import pe.project.point.Point;
import pe.project.utils.InventoryUtils;
import pe.project.utils.ParticleUtils;
import pe.project.utils.ScoreboardUtils;

public class PlayerTracking implements EntityTracking {

	@Override
	public void addEntity(Entity entity) {
		mEntities.add((Player)entity);
	}
	
	@Override
	public void removeEntity(Entity entity) {
		mEntities.remove(entity);
	}
	
	public Set<Player> getPlayers() {
		return mEntities;
	}

	@Override
	public void update(World world) {
		Iterator<Player> playerIter = mEntities.iterator();
		while (playerIter.hasNext()) {
			Player player = playerIter.next();
			
			boolean inSafeZone = false;
			boolean inCapital = false;
			boolean applyEffects = true;
			GameMode mode = player.getGameMode();
			
			if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
				Point loc = new Point(player.getLocation());
				
				//	First we'll check if the player is too high, if so they shouldn't be here.
				if (loc.mY >= 255 && player.isOnGround()) {
					int strikes = ScoreboardUtils.getScoreboardValue(player, "Strikes");
					strikes++;
					ScoreboardUtils.setScoreboardValue(player, "Strikes", strikes);
					
					String OOBLoc = "[" + (int)loc.mX + ", " + (int)loc.mY + ", " + (int)loc.mZ + "]";
					
					player.sendMessage(ChatColor.RED + "You've recieved a strike for breaking rule #5, leaving the bounds of the map at " + OOBLoc);
					player.sendMessage(ChatColor.YELLOW + "If you feel that this strike is unjustified feel free to send a message and screenshot of this to a moderator on the Discord.");
					
					player.teleport(new Location(player.getWorld(), Constants.SPAWN_POINT.mX, Constants.SPAWN_POINT.mY, Constants.SPAWN_POINT.mZ));
				} else {
					SafeZones safeZone = LocationManager.withinAnySafeZone(loc);	
					inSafeZone = (safeZone != SafeZones.None);
					inCapital = (safeZone == SafeZones.Capital);
					applyEffects = (inSafeZone && SafeZoneConstants.safeZoneAppliesEffects(safeZone));
					
					if (inSafeZone) {
						if (safeZone == SafeZones.Capital) {
							Material mat = world.getBlockAt((int)loc.mX, 10, (int)loc.mZ).getType();
							boolean neededMat = mat == Material.SPONGE || mat == Material.OBSIDIAN;
							
							if (mode == GameMode.SURVIVAL && !neededMat) {
								_transitionToAdventure(player);
							} else if (mode == GameMode.ADVENTURE && neededMat && loc.mY > 95) {
								int apartment = ScoreboardUtils.getScoreboardValue(player, "Apartment");
								if (apartment == 0) {
									player.setGameMode(GameMode.SURVIVAL);
								}
							}
						} else {
							_transitionToAdventure(player);
						}
					}
				}
				
				//	Give potion effects to those in a City;
				if (inSafeZone) {
					if (applyEffects) {
						if (inCapital) {
							player.addPotionEffect(Constants.CAPITAL_SPEED_EFFECT, true);
						}
						
						player.addPotionEffect(Constants.CITY_RESISTENCE_EFFECT, true);
						
						PotionEffect effect = player.getPotionEffect(PotionEffectType.JUMP);
						if (effect != null) {
							if (effect.getAmplifier() <= 5) {
								player.removePotionEffect(PotionEffectType.JUMP);
							}
						}
						
						int food = ScoreboardUtils.getScoreboardValue(player, "Food");
						if (food <= 17) {
							player.addPotionEffect(Constants.CITY_SATURATION_EFFECT, true);
						}
					}
				} else {
					if (mode == GameMode.ADVENTURE) {
						player.setGameMode(GameMode.SURVIVAL);
					}
				}
			}
			
			//	Extra Effects.
			_updateExtraEffects(player, world);
		}
	}
	
	void _transitionToAdventure(Player player) {
		player.setGameMode(GameMode.ADVENTURE);
		
		Entity vehicle = player.getVehicle();
		if (vehicle != null) {
			if (vehicle instanceof Boat) {
				vehicle.remove();
			}
		}
	}
	
	void _updateExtraEffects(Player player, World world) {
		_updatePatreonEffects(player, world);
		_updateItemEffects(player, world);
	}
	
	//	TODO: We should move this out of being ticked and into an event based system as well as store all
	//	Patrons in a list so we're not testing against every player 4 times a second.
	void _updatePatreonEffects(Player player, World world) {
		int patreon = ScoreboardUtils.getScoreboardValue(player, "Patreon");
		if (patreon > 0) {
			int shinyWhite = ScoreboardUtils.getScoreboardValue(player, "ShinyWhite");
			if (shinyWhite == 1 && patreon >= 5) {
				ParticleUtils.playParticlesInWorld(world, Particle.SPELL_INSTANT, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}
			
			int shinyPurple = ScoreboardUtils.getScoreboardValue(player, "ShinyPurple");
			if (shinyPurple == 1 && patreon >= 10) {
				ParticleUtils.playParticlesInWorld(world, Particle.DRAGON_BREATH, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}
			
			int shinyGreen = ScoreboardUtils.getScoreboardValue(player, "ShinyGreen");
			if (shinyGreen == 1 && patreon >= 10) {
				ParticleUtils.playParticlesInWorld(world, Particle.VILLAGER_HAPPY, player.getLocation().add(0, 0.2, 0), 4, 0.25, 0.25, 0.25, 0);
				return;
			}
		}
	}
	
	//	TODO: This is the incorrect way to handle this, this should only be test against when an event triggers that might
	//	cause a change in the inventory that might change the item. (Item Break, Item Moved, Item Dropped, etc)
	void _updateItemEffects(Player player, World world) {
		ItemStack chest = player.getInventory().getChestplate();
		if (InventoryUtils.testForItemWithLore(chest, "* Stylish *")) {
			ParticleUtils.playParticlesInWorld(world, Particle.SMOKE_NORMAL, player.getLocation().add(0, 1.5, 0), 5, 0.4, 0.4, 0.4, 0);
		}
	}
	
	private Set<Player> mEntities = new HashSet<Player>();
}
