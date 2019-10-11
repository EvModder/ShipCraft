package Evil_Code_ShipCraft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.material.Directional;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import Evil_Code_ShipCraft.Ship.ShipType;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;

public final class ShipCraft extends JavaPlugin implements Listener{
	private ArrayList<Ship> ships = new ArrayList<Ship>();
	private Map<Location, Move> sMoves = new HashMap<Location, Move>();//scheduled moves
	final static Material controlBlockType = Material.EMERALD_BLOCK;
	final static int maxShipSize = 10000;
	private boolean moving;

	@Override public void onEnable(){
		loadShips();
		getServer().getPluginManager().registerEvents(this, this);
	}
	@Override public void onDisable(){
		StringBuilder builder = new StringBuilder();
		
		for(Ship ship : ships){
			builder.append(ship.toString()); builder.append('\n'); builder.append('\n');
		}
		File conf = new File("./plugins/EvFolder/ships.txt");
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(conf));
			writer.write(builder.toString()); writer.close();
		}
		catch(IOException e){getLogger().info(e.getMessage());}
	}
	
	private void loadShips(){
		BufferedReader reader = null;
		//Load the conf -----------------------------------------------------------------------------------------------------------------
		try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/ships.txt"));}
		catch(FileNotFoundException e){
			
			//Create Directory
			File dir = new File("./plugins/EvFolder");
			if(!dir.exists()){dir.mkdir(); getLogger().info("Directory Created!");}
			
			//Create the file
			File shipFile = new File("./plugins/EvFolder/ships.txt");
			try{shipFile.createNewFile();} catch(IOException e1){}
			
			//Attempt again to load the file
			try{reader = new BufferedReader(new FileReader("./plugins/EvFolder/ships.txt"));}
			catch(FileNotFoundException e2){getLogger().info(e2.getStackTrace().toString());}
		}
		if(reader != null){
			String line = null;
			try{while((line = reader.readLine()) != null){
				line = line.trim();
				if(!line.isEmpty() && !line.startsWith("#") && !line.startsWith("//") && line.contains(",")){
					Ship newShip = Ship.fromString(line);
					if(newShip != null) ships.add(newShip);
				}
			}}
			catch(IOException e){getLogger().info(e.getMessage());}
			
			getLogger().info("Loaded "+ ships.size() +" ships from the file!");
		}//------------------------------------------------------------------------------------------------------------------------------
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]){
		if(sender instanceof Player == false){
			sender.sendMessage("This command can only be run by in-game players");
			return false;
		}
		if(cmd.getName().equalsIgnoreCase("listships")){
			Player p = (Player) sender;
			for(Ship ship : ships){
				p.sendMessage("CB="+ship.getControl().getX()+','+ship.getControl().getY()+','+ship.getControl().getZ()
						+ " Type="+ship.type+" Facing="+ship.directionFacing.toString()+" World="+ship.getControl().getWorld().getName());
			}
			return true;
		}
		if(cmd.getName().equalsIgnoreCase("setship") && args.length >= 2){
			Player p = (Player) sender;
			WorldEdit pl = WorldEdit.getInstance();
			LocalSession pSession;
			pSession = pl.getSessionManager().findByName(p.getName());
			if(pSession.isSelectionDefined(pSession.getSelectionWorld()) == false){
				p.sendMessage("Please set a //pos1 and //pos2 first.");
				return true;
			}
			Region region;
			try{region = pSession.getSelection(pSession.getSelectionWorld());}
			catch(IncompleteRegionException e){
				p.sendMessage("Please set a //pos1 and //pos2 first.");
				return true;
			}
			Location pos1 = new Location(getServer().getWorld(region.getWorld().getName()),
					region.getMinimumPoint().getBlockX(), region.getMinimumPoint().getBlockY(), region.getMinimumPoint().getBlockZ());
			Location pos2 = new Location(getServer().getWorld(region.getWorld().getName()),
					region.getMaximumPoint().getBlockX(), region.getMaximumPoint().getBlockY(), region.getMaximumPoint().getBlockZ());
			
			//
			BlockFace facing = null;
			try{facing = BlockFace.valueOf(args[1].toUpperCase());}
			catch(IllegalArgumentException ex){}catch(NullPointerException ex){}
			if(facing == null){
				p.sendMessage("§cPlease review your command and make sure it matches this format:");
				p.sendMessage("§f/setship §2[§7Type: Air/Sea/Lava/Sub/etc.§2] [§7Direction (Facing): NORTH/SOUTH/EAST/WEST§2]");
				return true;
			}
			
			if(args[0].toLowerCase().contains("sea") ||
			   args[0].toLowerCase().contains("ocean") ||
			   args[0].toLowerCase().contains("river"))addShip(pos1, pos2, ShipType.SEACRAFT, facing);
				
			else if(args[0].toLowerCase().contains("air") ||
					args[0].toLowerCase().contains("sky") ||
					args[0].toLowerCase().contains("plane") ||
					args[0].toLowerCase().contains("jet"))addShip(pos1, pos2, ShipType.AIRCRAFT, facing);
			
			else if(args[0].toLowerCase().contains("lava") ||
			   args[0].toLowerCase().contains("magma") ||
			   args[0].toLowerCase().contains("volcano"))addShip(pos1, pos2, ShipType.LAVABOAT, facing);
			
			else if(args[0].toLowerCase().contains("sub") ||
			   args[0].toLowerCase().contains("under") ||
			   args[0].toLowerCase().contains("depths"))addShip(pos1, pos2, ShipType.SUBMARINE, facing);
			
			else{
				p.sendMessage("§cPlease specify a valid ship type");
				return true;
			}
			//If here, success!
			p.sendMessage("§7>>§a Ship Saved!");
			return true;
		}
		
		return false;
	}

	@SuppressWarnings("deprecation")
	private void addShip(Location pos1, Location pos2, ShipType type, BlockFace facing){
		int maxX, maxY, maxZ, minX, minY, minZ;
		
		if(pos1.getBlockX() > pos2.getBlockX()){maxX = pos1.getBlockX(); minX = pos2.getBlockX();}
		else{maxX = pos2.getBlockX(); minX = pos1.getBlockX();}
		
		if(pos1.getBlockY() > pos2.getBlockY()){maxY = pos1.getBlockY(); minY = pos2.getBlockY();}
		else{maxY = pos2.getBlockY(); minY = pos1.getBlockY();}
		
		if(pos1.getBlockZ() > pos2.getBlockZ()){maxZ = pos1.getBlockZ(); minZ = pos2.getBlockZ();}
		else{maxZ = pos2.getBlockZ(); minZ = pos1.getBlockZ();}
		
		//---------------------------------------------------------------------------------------------------------------
		
		World w = pos1.getWorld();
		ArrayList<BlockData> data = new ArrayList<BlockData>();
		Material[] ignoredMaterials = getIgnoredCountMaterialsByShipType(type);
		Block controlBlock = w.getBlockAt(pos1);
				
		for(int x = minX; x <= maxX; x++){
			for(int y = minY; y <= maxY; y++){
				for(int z = minZ; z <= maxZ; z++){
					Block b = w.getBlockAt(x, y, z);
					if(b.isEmpty() == false){
						//if the block to add is of a generally accepted type
						if(Ship.isFromMatList(b.getType(), ignoredMaterials) == false){
							String id = b.getType().name();
							if(b.getData() != 0) id += ":"+b.getData();
							data.add(new BlockData(id, x, y, z));
							
							if(b.getType() == controlBlockType)controlBlock = b;
						}
						else if((type == ShipType.AIRCRAFT || type == ShipType.SEACRAFT)
							&& Ship.isFromMatList(b.getRelative(BlockFace.DOWN).getType(), ignoredMaterials) == false){
							
							String id = b.getType().name();
							if(b.getData() != 0) id += ":"+b.getData();
							data.add(new BlockData(id, x, y, z));
						}
						else{//(if the block is on the ignored list)
							Block[] touchingBlocks = new Block[]{
									b.getRelative(BlockFace.UP),
									b.getRelative(BlockFace.NORTH),
									b.getRelative(BlockFace.SOUTH),
									b.getRelative(BlockFace.EAST),
									b.getRelative(BlockFace.WEST)
							};
							int numTouching = 0;
							for(Block touching : touchingBlocks){
								if(Ship.isFromMatList(touching.getType(), ignoredMaterials) == false)numTouching++;
							}
							if(numTouching >= 3){
								String id = b.getType().name();
								if(b.getData() != 0) id += ":"+b.getData();
								data.add(new BlockData(id, x, y, z));
							}
						}//else (if the block to add is not of a generally accepted type)
					}//if the block is not empty
				}//for z
			}//for y
		}//for x
		ships.add(new Ship(data.toArray(new BlockData[]{}), type, controlBlock.getLocation(), facing));
	}
	
	@EventHandler
	public void onRedstoneEvent(BlockRedstoneEvent evt){
		// Does not run if: 1. The signal was not 'off' previously
		// 2. If more then 3 ships are already in the moving process.
		// 3. The redstone item that was power is not pointing in any direction
		// 4. The block that the redstone item is pointing into is not a ControlBlock
		if(evt.getOldCurrent() != 0 || sMoves.size() > 3) return;
		
		// If not facing into an EmeraldBlock
		BlockFace facing;
		if((facing = getFacingControlBlock(evt.getBlock())) == null) return;
		getLogger().info("Facing Into Emerald Block");
		
		Ship ship = getShip(evt.getBlock().getRelative(facing).getLocation());
		if(ship == null) return;
		
		Move move = getRedstoneMove(ship,// the ship to calculate the move for
									evt.getBlock().getType(),// the type of the redstone event block
									facing,// the direction the redstone event block is facing
									evt.getNewCurrent());// the current strength the redstone block is sending
		
		getLogger().info("Calculating Move..");
		if(move != null && (move.getTravelDistanceSquared() > 0 || move.getDegreeAngle() == 0)) scheduleMove(ship.getControl(), move);
		else getLogger().info("Did not receive a valid move.");
	}
	
	public BlockFace getFacingControlBlock(Block b){
		BlockFace facing;
		
		if(b.getState().getData() instanceof Directional){
			getLogger().info("Directional Block");
			facing = ((Directional)b.getState().getData()).getFacing();
			
			if(b.getRelative(facing).getType() == controlBlockType) return facing;
			else if(b.getRelative(facing.getOppositeFace()).getType() == controlBlockType) return facing.getOppositeFace();
		}
		else if(b.getType() == Material.REDSTONE_WIRE){
			if(b.getRelative(BlockFace.DOWN).getType() == controlBlockType) return BlockFace.DOWN;
			else if(b.getRelative(BlockFace.NORTH).getType() == controlBlockType) return BlockFace.NORTH;
			else if(b.getRelative(BlockFace.SOUTH).getType() == controlBlockType) return BlockFace.SOUTH;
			else if(b.getRelative(BlockFace.EAST ).getType() == controlBlockType) return BlockFace.EAST;
			else if(b.getRelative(BlockFace.WEST ).getType() == controlBlockType) return BlockFace.WEST;
		}
		else if(b.getType() == Material.STONE_PRESSURE_PLATE){
			if(b.getRelative(BlockFace.DOWN).getType() == controlBlockType) return BlockFace.DOWN;
		}
		else if(b.getType() == Material.REPEATER){
			getLogger().info("diode here?");
		}
		return null;
	}
	
	public Move getRedstoneMove(Ship ship, Material type, BlockFace direction, int signalStrength){
		Move move = new Move(0, 0, 0, 0);
		
		// The 'speed' is the number of blocks the ship will travel on a single redstone signal
		int speed = 0;
		if(type == Material.REDSTONE_WIRE){
			speed = 16 - signalStrength;
		}
		else if(type == Material.COMPARATOR){
			speed = 15 - signalStrength;
			// Check to see whether to rotate the ship
			getLogger().info("powered diode/comparator facing controlblock, speed is "+speed);
			if(speed == 0){
				int angle1 = (int) toDegreeAngle(ship.directionFacing), angle2 = (int) toDegreeAngle(direction);
				
				if(angle1 == -1 || angle2 == -1 || (angle2-angle1) % 90 != 0) return null;
				move.degreeAngle = angle2 - angle1;
				
				getLogger().info("Turning angle set to "+move.degreeAngle);
				if(move.degreeAngle == 0 && type == Material.REPEATER) speed = 3;
			}
		}
		else if(type == Material.STONE_BUTTON) speed = 4;
		else if(type == Material.OAK_BUTTON) speed = 6;
		
		else if(type == Material.LEVER) speed = 3;
		else if(type == Material.REDSTONE_TORCH) speed = 2;
		
		else if((type == Material.OAK_PRESSURE_PLATE || type == Material.STONE_PRESSURE_PLATE)) speed = 1;
		
		else return null;
		
		getLogger().info("speed is "+ speed);
		
		if(direction == BlockFace.UP) move.changeY = speed;
		else if(direction == BlockFace.DOWN) move.changeY = -speed;
		else if(direction == BlockFace.NORTH) move.changeZ = -speed;
		else if(direction == BlockFace.SOUTH) move.changeZ = speed;
		else if(direction == BlockFace.EAST) move.changeX = speed;
		else if(direction == BlockFace.WEST) move.changeX = -speed;
		return move;
	}
	
	@SuppressWarnings("serial") final static Map<BlockFace, Float> blockFaceAngles = new HashMap<BlockFace, Float>(){{
			put(BlockFace.NORTH, 0F); put(BlockFace.NORTH_NORTH_WEST, 22.5F);
			put(BlockFace.NORTH_WEST, 45F); put(BlockFace.WEST_NORTH_WEST, 77.5F);
			put(BlockFace.WEST, 90F); put(BlockFace.WEST_SOUTH_WEST, 112.5F);
			put(BlockFace.SOUTH_WEST, 135F); put(BlockFace.SOUTH_SOUTH_WEST, 157.5F);
			put(BlockFace.SOUTH, 180F); put(BlockFace.SOUTH_SOUTH_EAST, 202.5F);
			put(BlockFace.SOUTH_EAST, 225F); put(BlockFace.EAST_SOUTH_EAST, 247.5F);
			put(BlockFace.EAST, 270F); put(BlockFace.EAST_NORTH_EAST, 292.5F);
			put(BlockFace.NORTH_EAST, 315F); put(BlockFace.NORTH_NORTH_EAST, 337.5F);
	}};
	static float toDegreeAngle(BlockFace face){
		if(blockFaceAngles.containsKey(face)) return blockFaceAngles.get(face);
		else return -1;
	}
	static BlockFace fromDegreeAngle(float angle){
		for(BlockFace face : blockFaceAngles.keySet()){
			if(blockFaceAngles.get(face) == angle) return face;
		}
		return null;
	}
	
	private void scheduleMove(Location control, Move move){
		if(sMoves.containsKey(control)){
			//TODO: Test this, might be simpler with "sMoves.get(control).add(move);"
			Move m = sMoves.get(control);
			m.add(move); sMoves.put(control, m);
			getLogger().info("Combining Moves");
		}
		else{
			sMoves.put(control, move);
			new BukkitRunnable(){@Override public void run(){
				Location locKey = sMoves.keySet().iterator().next();
				
				getLogger().info("Moving Ship");
				String result = attemptMoveShip(getShip(locKey), sMoves.get(locKey));
				getLogger().info(result);
				
				sMoves.remove(locKey);
				//TODO: REMOVE THIS AWEFUL HACK
				sMoves.clear();
			}}.runTaskLater(this, 1);//1 tick delay
		}
	}
	
	public String attemptMoveShip(Ship ship, Move move){
		if(ship == null) return "Failed to move ship! Error: Ship not found";
		
		List<Location> blockList = Ship.getBlocksInShip(ship, getIgnoredCountMaterialsByShipType(ship.type));
		if(blockList == null) return "Failed to move ship! Error: Too many blocks in ship / overload error";
		
		// Check to see if this ship has a player onboard
		boolean hasPlayer = false;
		for(Player p : getServer().getOnlinePlayers()) if(isOnboardShip(p.getLocation(), blockList)) hasPlayer = true;
		if(hasPlayer == false) return "Could not move ship! Error: No player onboard";
		
		getLogger().info("Before Scaling: "+move.changeX+','+move.changeY+','+move.changeZ);
		//--------------- Adjust velocity ---------------//
		if(move.getTravelDistanceSquared() != 0){
			boolean goingStraight;
			if(ship.directionFacing == BlockFace.NORTH || ship.directionFacing == BlockFace.SOUTH)
				goingStraight = (move.changeX == 0 && move.changeY == 0);
			else if(ship.directionFacing == BlockFace.EAST || ship.directionFacing == BlockFace.WEST)
				goingStraight = (move.changeZ == 0 && move.changeY == 0);
			else goingStraight = false;
			
			if(ship.type == ShipType.AIRCRAFT){
				if(goingStraight) move.scale(1.1F);// increase movement to 110% when going straight
				else move.scaleMaintainMin(.75F, 1);// reduce movement to 75% when not going straight
				move.scaleMaintainMinY(.5F, 1);//scale 'Y' to 50%
			}
			if(ship.type == ShipType.SUBMARINE){
				if(goingStraight) move.scale(1.25F);// increase movement to 125% when going straight
				else move.scaleMaintainMin(.25F, 1);// reduce movement to 25% when not going straight
			}
			if(ship.type == ShipType.SEACRAFT){
				if(goingStraight)/*leave alone when going straight*/;
				else move.scaleMaintainMin(.4F, 1);// reduce movement to 40% when not going straight
				move.changeY=0;// No change in 'Y' at all.
			}
			if(ship.type == ShipType.LAVABOAT){
				if(goingStraight) move.scaleMaintainMin(.5F, 1);// reduce movement to 50% when going straight
				else move.scaleMaintainMin(.20F, 1);// reduce movement to 20% when not going straight
			}
		}
		//-----------------------------------------------//
		getLogger().info("After Scaling: "+move.changeX+','+move.changeY+','+move.changeZ);
		
		
		
		getLogger().info("Before Collision Check: "+move.changeX+','+move.changeY+','+move.changeZ);
		//-----------------------------------------------//
		
		// The "collisions" list is re-initialized inside the CollisionCheck functions
		List<Location> collisions = new ArrayList<Location>();
		if(blockList.size() > 500){
			collisions = ship.getCollisions(move, blockList, getIgnoredCollisionMaterialsByShipType(ship.type));
			if(tooManyCollisions(collisions, blockList.size())) move = null;
		}
		else if(blockList.size() > 250) move = fastCollisionCheck(ship, move, blockList, collisions);
		else move = fullCollisionCheck(ship, move, blockList, collisions);
		if(move == null) return "Could not move ship! Error: Unmovable obstacles";
		
		//-----------------------------------------------//
		getLogger().info("After Collision Check: "+move.changeX+','+move.changeY+','+move.changeZ);
		
		// Create explosions at collision locations
		for(final Location loc : collisions){
			if(ship.type == ShipType.AIRCRAFT) loc.getWorld().createExplosion(loc, 2.0F);
			if(ship.type == ShipType.SEACRAFT || ship.type == ShipType.SUBMARINE || ship.type == ShipType.LAVABOAT){
				//------------------ Drain surrounding liquids within the radius ------------------//
				int drainRadius = 2;
				for(int x = loc.getBlockX()-drainRadius; x <= loc.getBlockX()+drainRadius; x++)
				for(int y = loc.getBlockY()-drainRadius; y <= loc.getBlockY()+drainRadius; y++)
				for(int z = loc.getBlockZ()-drainRadius; z <= loc.getBlockZ()+drainRadius; z++){
					Block b = loc.getWorld().getBlockAt(x, y, z);
					if(b.isLiquid() || b.isEmpty()) b.setType(Material.AIR);
				}
				if(loc.getBlock().isLiquid() == false)
				loc.getWorld().createExplosion(loc, 1.5F);
				//---------------------------------------------------------------------------------//
			}
		}
		//============== Move the Ship ==============//
		moving=true;
/*		List<Location> newBlocks =*/ ship.move(move, blockList);
		moving=false;
		

		//===================================== Move any onboard players =====================================
		Rotator rotator = new Rotator(ship.getControl().getBlockX(), ship.getControl().getBlockZ(), move.getRadianAngle());
		Location fromLoc, toLoc;
		for(Player p : getServer().getOnlinePlayers()){
			if(isOnboardShip(p.getLocation(), blockList)){
				fromLoc = p.getLocation();
				double[] newPos = rotator.rotatePointExact(fromLoc.getX(), fromLoc.getZ());
				toLoc = new Location(p.getWorld(),
							newPos[0] + move.changeX,
							fromLoc.getY() + move.changeY,
							newPos[1] + move.changeZ,
							fromLoc.getYaw(), fromLoc.getPitch());
				
				// Less-Glitchy then teleporting, but requires open air between players's current location and the 'To' location
				if(clearPathTo(fromLoc, toLoc)){
					double deltaX = toLoc.getX() - fromLoc.getX(),
						   deltaY = toLoc.getY() - fromLoc.getY(),
						   deltaZ = toLoc.getZ() - fromLoc.getZ();
					
					p.setVelocity(p.getVelocity().add(new Vector(deltaX, (deltaY == 0) ? .2 : deltaY, deltaZ)));
				}
				else p.teleport(toLoc);
			}
		}
		return "Successfully moved the ship!";
	}
	
	public Ship getShip(Location control){
		if(control.getBlock().getType() != controlBlockType) return null;
		
		for(Ship ship : ships) if(ship.getControl().equals(control)) return ship;
		return null;
	}
		
	public Move fullCollisionCheck(Ship ship, Move move, List<Location> blockList, List<Location> collisions){
		
		// Very slow method, but checks every possibility.
		int startY = move.changeY, startZ = move.changeZ;
		do{
			move.changeY = startY;
			do{
				move.changeZ = startZ;
				do{
					collisions = ship.getCollisions(move, blockList, getIgnoredCollisionMaterialsByShipType(ship.type));
					tooManyCollisions(collisions, blockList.size());
					if(tooManyCollisions(collisions, blockList.size()) == false) return move;
					
					if(move.changeZ != 0) move.changeZ += (move.changeZ > 0) ? -1 : 1;
					
				}while(move.changeZ != 0);
				if(move.changeY != 0) move.changeY += (move.changeY > 0) ? -1 : 1;
				
			} while(move.changeY != 0);
			if(move.changeX != 0) move.changeX += (move.changeX > 0) ? -1 : 1;
			
		} while(move.changeX != 0);
		
		// Couldn't avoid tooManyCollisions() == true
		return null;
	}
	
	public Move fastCollisionCheck(Ship ship, Move move, List<Location> blockList, List<Location> collisions){
		
		// Much faster method, but can result in a space on some sides of the ship between the ship and the obstacles
		while(tooManyCollisions(collisions, blockList.size())){
			
			//Scale down 1 block from the greatest change[Var] (and some portion for the others);
			float dist = (float) move.getTravelDistance();
			
			if(dist > 1) move.scale((dist-1)/dist);
			else return null;
			collisions = ship.getCollisions(move, blockList, getIgnoredCollisionMaterialsByShipType(ship.type));
		}
		return move;
	}
	
	private boolean tooManyCollisions(List<Location> collisions, int shipSize){
		if(collisions.size() > shipSize/20) return true;;
		
		for(Location loc : collisions){//TODO: other unmovable blocks
			if(loc.getBlock() == null ||
			   loc.getBlock().getType() == Material.BEDROCK || loc.getBlock().getType() == Material.END_PORTAL_FRAME ||
			   loc.getBlock().getType() == Material.END_PORTAL|| loc.getBlock().getType() == Material.NETHER_PORTAL)
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isOnboardShip(Location loc, List<Location> shipBlocks){
		if(loc.getWorld().getName().equals(shipBlocks.get(0).getWorld().getName()) == false) return false;
		
		return shipBlocks.contains(loc.getBlock().getRelative(BlockFace.DOWN).getLocation()) ||
			   shipBlocks.contains(loc) ||
			   shipBlocks.contains(loc.getBlock().getRelative(BlockFace.DOWN,2).getLocation());
	}
	
	public boolean clearPathTo(Location loc1, Location loc2){
		//TODO: Write this function!!
		return false;
	}
	
	// Returns materials that will not move along with the ship (unless distinctly connected to the ship by it's blockdata list)
	public static Material[] getIgnoredCountMaterialsByShipType(ShipType type){
		if(type == ShipType.AIRCRAFT)return new Material[]{Material.AIR,Material.WATER,Material.LAVA,Material.BEDROCK};
		//
		else if(type == ShipType.SEACRAFT)return new Material[]{
				Material.AIR,Material.WATER,Material.LAVA,Material.GRAVEL,Material.SAND,Material.DIRT,Material.BEDROCK};
		//
		else if(type == ShipType.LAVABOAT)return new Material[]{Material.AIR,Material.WATER,Material.LAVA,Material.STONE,
				Material.COAL_ORE,Material.IRON_ORE,Material.GOLD_ORE,Material.REDSTONE_ORE,Material.LAPIS_ORE,Material.DIAMOND_ORE,
				Material.EMERALD_ORE,Material.SAND,Material.GRAVEL,Material.BEDROCK};
		//
		else if(type == ShipType.SUBMARINE)return new Material[]{Material.AIR,Material.WATER,Material.LAVA,Material.STONE,
				Material.SAND,Material.GRAVEL,Material.DIRT,Material.BEDROCK};
		//
		//
		else return new Material[]{Material.AIR,Material.WATER,Material.LAVA,Material.BEDROCK};
	}
	
	//Returns materials that will not count as a collision when hit by the ship
	public static Material[] getIgnoredCollisionMaterialsByShipType(ShipType type){
		if(type == ShipType.AIRCRAFT)return new Material[]{Material.AIR};
		//
		else if(type == ShipType.SEACRAFT)return new Material[]{
				Material.AIR,Material.WATER,Material.LAVA,Material.SAND};
		//
		else if(type == ShipType.LAVABOAT)return new Material[]{Material.AIR,Material.WATER,Material.LAVA};
		//
		else if(type == ShipType.SUBMARINE)return new Material[]{Material.AIR,Material.WATER,Material.LAVA,Material.GRAVEL};
		//
		//
		else return new Material[]{Material.AIR,Material.WATER,Material.LAVA};
	}
	
	@EventHandler public void blockPhysicsEvent(BlockPhysicsEvent evt){if(moving) evt.setCancelled(true);}
}//End of class




