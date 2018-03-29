package pe.project.point;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import pe.project.utils.LocationUtils;

public class Raycast {

	private final Vector dir;

	public int iterations = 10;
	public double hitRange = 0.5;
	public double dirMultiplier = 1;
	public double distanceCheck = 1;

	public boolean targetPlayers = false;
	public boolean targetNonPlayers = true;
	public boolean throughBlocks = false;
	public boolean throughNonOccluding = false;
	public boolean noIterations = false;


	//Locations
	private final Location start;
	private Location end = null;

	public Raycast(Location p1, Location p2){
		this.dir = LocationUtils.getDirectionTo(p2, p1);
		this.start = p1;
		this.end = p2;

		//If we're going from one point to another,
		//ignore using iteration by default.
		this.noIterations = true;
	}

	public Raycast(Location start, Vector dir, int iterations){
		this.dir = dir;
		this.iterations = iterations;
		this.start = start;
	}

 /**
  * Fires the raycast with the given properties
  * @return All entities hit by the raycast
  */
	public RaycastData shootRaycast(){
		int i = 0;
		double dist = 0;
		RaycastData data = new RaycastData();
		List<LivingEntity> entities = data.getEntities();
		if (end != null){
			dist = start.distance(end);
		}
		while (true){

			start.add(dir.clone().multiply(dirMultiplier));
			data.getBlocks().add(start.getBlock());


			if (!throughBlocks){
				if (start.getBlock().getType().isSolid()){
					if (!throughNonOccluding){
						break;
					}else{
						if (start.getBlock().getType().isOccluding()){
							break;
						}
					}
				}
			}

			for (Entity e : start.getWorld().getNearbyEntities(start,hitRange,hitRange,hitRange)){
				if( e instanceof LivingEntity ) {
					//	Make sure we should be targeting this entity.
					if( (targetPlayers && (e instanceof Player)) || (targetNonPlayers && (!(e instanceof Player))) ) {
						if (!entities.contains(e)){
							 data.getEntities().add((LivingEntity)e);
						}
					}
				}
			}

			if (end != null){
				if (start.distance(end) < distanceCheck){
					break;
				}

				//This is to prevent an infinite loop should somehow
				//the raycast goes further past the end point
				if (start.distance(end) > dist){
					break;
				}else{
					dist = start.distance(end);
				}
			}

			if (!noIterations){
				i++;
				if (i >= iterations){
					break;
				}
			}
		}

		return data;
	}


}
