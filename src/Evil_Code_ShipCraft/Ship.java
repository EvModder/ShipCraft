package Evil_Code_ShipCraft;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

public class Ship {
	final static int MAX_STACK_DEPTH = 300;
	enum ShipType {AIRCRAFT, SEACRAFT, LAVABOAT, SUBMARINE, NONE};
	
	final BlockData[] data;
	final ShipType type;
	final int MAX_LOAD = 100;//defaults to 100
	final int BLOCK_COUNT;
	private Location controlBlock;
	BlockFace directionFacing;
	
	public Ship(BlockData[] data, ShipType type, Location controlBlock, BlockFace direction){
		this.data = data;
		this.type = type;
		this.controlBlock = controlBlock;
		BLOCK_COUNT = data.length;
		directionFacing = direction;
	}
	
	public Location getControl(){return controlBlock;}
	
	public String toString(){
		StringBuilder builder = new StringBuilder("Ship:{");
		builder.append("Type:"); builder.append(type.ordinal()); builder.append(',');
		builder.append("Max_Load:"); builder.append(MAX_LOAD); builder.append(',');
		builder.append("Block_Count:"); builder.append(BLOCK_COUNT); builder.append(',');
		builder.append("Facing:"); builder.append(directionFacing.ordinal()); builder.append(',');
		builder.append("ControlBlock:{");
			builder.append(controlBlock.getWorld().getName()); builder.append('.');
			builder.append(controlBlock.getBlockX()); builder.append('.');
			builder.append(controlBlock.getBlockY()); builder.append('.');
			builder.append(controlBlock.getBlockZ()); builder.append('}'); builder.append(',');
			
		builder.append("BlockData:{");
		for(BlockData bd : data){
			builder.append('{');
			builder.append(bd.id); builder.append('.');
			builder.append(bd.x); builder.append('.');
			builder.append(bd.y); builder.append('.');
			builder.append(bd.z); builder.append('}');
		}
		builder.append('}');
		
		return builder.toString();
	}
	//Ship:{Type:0,Max_Load:345,Block_Count:450,Facing:3,
	//ControlBlock:{world.45.10.68},BlockData:{{43:1.1.1.1}{43.1.1.2}{43.1.1.3}{43.1.2.1}}}
	
	public static Ship fromString(String str){
		str = str.toLowerCase().trim();
		str = str.substring(6, str.length()-1);
		// Variables
		Location control = null;
		BlockFace facing = null;
		BlockData[] bd = null;
		ShipType type = null;
		int max_load = -1, block_count = -1;
		
		String tag, value;
		for(String setting : str.split(",")){
			tag = setting.split(":")[0]; value = setting.split(":")[1].replace("_", "");
			
			if(tag.equals("type") && type == null) type = ShipType.values()[Integer.parseInt(value)];
			else if(tag.equals("maxload") && max_load == -1) max_load = Integer.parseInt(value);
			else if(tag.equals("blockcount") && block_count == -1) block_count = Integer.parseInt(value);
			else if(tag.equals("facing") && facing == null) facing = BlockFace.values()[Integer.parseInt(value)];
			else if(tag.equals("controlblock")){
				String[] locData = value.substring(1, value.length()-1).split("\\.");
				
				if(Bukkit.getWorld(locData[0]) != null) control = new Location(Bukkit.getWorld(locData[0]),
										Integer.parseInt(locData[1]), Integer.parseInt(locData[2]), Integer.parseInt(locData[3]));
			}
			else if(tag.equals("blockdata")){//clip off the ":{{" and the "}}"
				String[] data = setting.substring(tag.length()+3, setting.length()-1).split("\\}\\{");
				
				bd = new BlockData[data.length];
				for(int i=0; i<data.length; i++){
					String[] bdData = data[i].split("\\.");
					
					bd[i] = new BlockData(bdData[0],
							Integer.parseInt(bdData[1]), Integer.parseInt(bdData[2]), Integer.parseInt(bdData[3]));
				}
			}
		}
		if(bd == null) Bukkit.getLogger().info("bd is null");
		if(facing == null) Bukkit.getLogger().info("facing is null");
		if(type == null) Bukkit.getLogger().info("type is null");
		if(control == null) Bukkit.getLogger().info("control is null");
		
		if(bd != null && type != null && control != null && facing != null && control.getBlock().getType() == ShipCraft.controlBlockType){
			if(max_load < 0) max_load = 0;
			if(block_count < 0) block_count = bd.length;
			
			return new Ship(bd, type, control, facing);
		}
		else return null;
	}
	
	//===================================== Movement Management =====================================//
	/** Overloaded function **/
	public List<Location> move(Move move){
		return move(move, getBlocksInShip(this, ShipCraft.getIgnoredCountMaterialsByShipType(type)));
	}
	@SuppressWarnings("deprecation")
	public List<Location> move(Move move, List<Location> blocksToMove){
		if(move.degreeAngle%360 == 0) return simpleMove(move, blocksToMove);
		directionFacing = getNewDirection(directionFacing, move.degreeAngle);
		
		// Calculate some constants
		Rotator rotateRelCenter = new Rotator(controlBlock.getBlockX(), controlBlock.getBlockZ(), move.getRadianAngle());
		World world = controlBlock.getWorld();
		
		//Convert to <BlockState> list to prevent data loss when manipulating blocks
		List<BlockState> blockData = new ArrayList<BlockState>();
		for(Location loc : blocksToMove) blockData.add(loc.getBlock().getState());
		
		//Clear the previous ship location before moving
		for(BlockState oldBlock : blockData) oldBlock.getBlock().setType(Material.AIR);
		
		//Declare a list of "newblocks" to hold the ->to locations of all the blocks to return at the end of the method
		List<Location> newBlocks = new ArrayList<Location>();
		
		//loop though all the BlockStates and move them to the new locations
		int[] newCoords;
		for(BlockState oldBlock : blockData){
			newCoords = rotateRelCenter.rotatePoint(oldBlock.getX(), oldBlock.getZ());
			
			Block newBlock = world.getBlockAt(newCoords[0] + move.changeX, oldBlock.getY() + move.changeY, newCoords[1] + move.changeZ);
			
			newBlock.setType(oldBlock.getType());
			//
			newBlock.setData(oldBlock.getRawData());
			MaterialData oldData = oldBlock.getData();
			if(oldData instanceof MaterialData) newBlock.getState().setData(oldData);
			//
			if(oldBlock.getData() instanceof Directional){
				Directional ddata = (Directional) oldBlock.getData();
				ddata.setFacingDirection(getNewDirection(ddata.getFacing(), move.degreeAngle));
				newBlock.getState().setData((MaterialData) ddata);
				newBlock.getState().update();
			}
			if(oldBlock instanceof InventoryHolder){
				Bukkit.getLogger().info("Container: "+oldBlock.getType().name());
//				InventoryHolder ih = (InventoryHolder) oldBlock;
//				InventoryHolder newIh = (InventoryHolder) newBlock.getState();
//				newIh.getInventory().setContents(ih.getInventory().getContents());
			}
			newBlock.getState().update();
		}
		
		//---------------------------------- Move the saved record of the ship's location ----------------------------------//
		for(int i = 0; i < data.length; i++){
			newCoords = rotateRelCenter.rotatePoint(data[i].x, data[i].z);
			
			Block newBlock = world.getBlockAt(newCoords[0] + move.changeX, data[i].y + move.changeY, newCoords[1] + move.changeZ);
			
			String id = newBlock.getType().name();
			if(newBlock.getData() != 0) id += ':'+newBlock.getData();
			data[i] = new BlockData(id, newBlock.getX(), newBlock.getY(), newBlock.getZ());
		}
		// The control block doesn't need to rotate - It is already at the center of rotation
		controlBlock = controlBlock.add(move.changeX, move.changeY, move.changeZ);
		//------------------------------------------------------------------------------------------------------------------//
		
		// Fill in behind lavaboats
		if(type == ShipType.LAVABOAT)
		for(Location prevLoc : blocksToMove){
			if(prevLoc.getBlock().isEmpty() && newBlocks.contains(prevLoc) == false){
				Block prevBlock = prevLoc.getBlock();
				
				if(	prevBlock.getRelative(BlockFace.NORTH).getType() == Material.LAVA ||
					prevBlock.getRelative(BlockFace.SOUTH).getType() == Material.LAVA ||
					prevBlock.getRelative(BlockFace.EAST ).getType() == Material.LAVA ||
					prevBlock.getRelative(BlockFace.WEST ).getType() == Material.LAVA){
					prevBlock.setType(Material.LAVA);
				}
			}
		}
		
		// return the new list of blocks composing the ship
		return newBlocks;
	}
	
	@SuppressWarnings("deprecation")
	private List<Location> simpleMove(Move move, List<Location> blocksToMove){
		World world = controlBlock.getWorld();
		
		//Convert to <BlockState> list to prevent data loss when manipulating blocks
		List<BlockState> blockData = new ArrayList<BlockState>();
		for(Location loc : blocksToMove) blockData.add(loc.getBlock().getState());
		
		//Clear the previous ship location before moving
		for(BlockState oldBlock : blockData) oldBlock.getBlock().setType(Material.AIR);
		
		//Declare a list of "newblocks" to hold the ->to locations of all the blocks to return at the end of the method
		List<Location> newBlocks = new ArrayList<Location>();
		
		//loop though all the BlockStates and move them to the new locations
		for(BlockState oldBlock : blockData){
			Block newBlock = world.getBlockAt(
					oldBlock.getX() + move.changeX, oldBlock.getY() + move.changeY, oldBlock.getZ() + move.changeZ);
			
			newBlock.setType(oldBlock.getType());
			//
			newBlock.setData(oldBlock.getRawData());
			MaterialData oldData = oldBlock.getData();
			if(oldData instanceof MaterialData) newBlock.getState().setData(oldData);
			//
			if(oldBlock instanceof Directional){
				Directional ddata = (Directional) oldBlock.getData();
				ddata.setFacingDirection(getNewDirection(ddata.getFacing(), move.degreeAngle));
				newBlock.getState().setData((MaterialData) ddata);
			}
			if(oldBlock instanceof InventoryHolder){
//				InventoryHolder ih = (InventoryHolder) oldBlock;
//				InventoryHolder newIh = (InventoryHolder) newBlock.getState();
//				newIh.getInventory().setContents(ih.getInventory().getContents());
			}
			newBlock.getState().update();
			newBlocks.add(newBlock.getLocation());
		}
		
		//---------------------------------- Move the saved record of the ship's location ----------------------------------//
		for(int i = 0; i < data.length; i++){
			Block newBlock = world.getBlockAt(data[i].x + move.changeX, data[i].y + move.changeY, data[i].z +  move.changeZ);
			
			String id = newBlock.getType().name();
			if(newBlock.getData() != 0) id += ':'+newBlock.getData();
			data[i] = new BlockData(id, newBlock.getX(), newBlock.getY(), newBlock.getZ());
		}
		controlBlock = controlBlock.add(move.changeX, move.changeY, move.changeZ);
		//------------------------------------------------------------------------------------------------------------------//
		
		// Fill in behind lavaboats
		if(type == ShipType.LAVABOAT)
		for(Location bLoc : newBlocks){
			Block prevBlock = bLoc.add(-move.changeX, -move.changeY, -move.changeZ).getBlock();
			if(prevBlock.isEmpty() && newBlocks.contains(prevBlock.getLocation()) == false){
				if(	prevBlock.getRelative(BlockFace.NORTH).getType() == Material.LAVA ||
					prevBlock.getRelative(BlockFace.SOUTH).getType() == Material.LAVA ||
					prevBlock.getRelative(BlockFace.EAST ).getType() == Material.LAVA ||
					prevBlock.getRelative(BlockFace.WEST ).getType() == Material.LAVA){
					prevBlock.setType(Material.LAVA);
				}
			}
		}
		
		// return the new list of blocks composing the ship
		return newBlocks;
	}
	
	/** Overloaded function **/
	public List<Location> getCollisions(Move move){
		//Ignored Materials for onMove
		Material[] ignoredMaterials = ShipCraft.getIgnoredCollisionMaterialsByShipType(type);
		
		//Calculate blocks to move
		List<Location> blockList = getBlocksInShip(this, ignoredMaterials);
		
		//Call function
		return getCollisions(move, blockList, ignoredMaterials);
	}
	public List<Location> getCollisions(Move move, List<Location> blockList, Material[] ignoredMaterials){
		World world = controlBlock.getWorld();
		
		Rotator rotateRelCenter = new Rotator(controlBlock.getBlockX(), controlBlock.getBlockZ(), move.getRadianAngle());
		
		//Define list of collision locations
		List<Location> collisions = new ArrayList<Location>();
		
		//Check the blockTo locations to see if they are empty, and if so add a collision.
		for(Location loc : blockList){
			int[] newCoords = rotateRelCenter.rotatePoint(loc.getBlockX(), loc.getBlockZ());
			
			Location colLoc = new Location(world, newCoords[0] + move.changeX, loc.getY() + move.changeY, newCoords[1] + move.changeZ);
			
			// If the block's type causes a collision and the blockList does not contain the block's location
			if(colLoc.getBlock() == null ||
					(!isFromMatList(colLoc.getBlock().getType(), ignoredMaterials) && !blockList.contains(colLoc))){
				collisions.add(colLoc);
			}
		}
		return collisions;
	}
	
	public static List<Location> getBlocksInShip(Ship ship, Material[] ignoredTypes){
		List<Location> blockList = new ArrayList<Location>();
		World world = ship.controlBlock.getWorld();
		
		@SuppressWarnings("unused")
		boolean tooMuchCargo;
		
		// Load all the blocks in the blockdata list
		for(int i = 0; i < ship.data.length; i++){
			BlockData bd = ship.data[i];
			Block b = world.getBlockAt(bd.x, bd.y, bd.z);
			if(b.isEmpty() == false) blockList.add(b.getLocation());
		}
		
		// Also load any blocks that are on/touching the ship if they are not connected to any other large mass
		// (and if the ship can carry them all -- cargo levels)
		//====================================================================
		List<Location> notCargoBlocks = new ArrayList<Location>(blockList);
		List<Location> cargoBlocks = new ArrayList<Location>();
		List<Location> totalBlockList = new ArrayList<Location>(blockList);
		
		for(Location blockInShip : blockList){
			getConnectedBlocksTo(blockInShip.getBlock(), ignoredTypes, cargoBlocks, notCargoBlocks, 0);
			
			if(((totalBlockList.size() + cargoBlocks.size()) - ship.BLOCK_COUNT) <= ship.MAX_LOAD){
				totalBlockList.addAll(cargoBlocks);
				cargoBlocks.clear();
			}
			else{
				tooMuchCargo = true;
				break;
			}
		}//===================================================================
		
		return totalBlockList;
	}
	
	private static void getConnectedBlocksTo(Block startBlock, Material[] ignoredTypes, 
												List<Location> blockList, List<Location> notCounted, int stackDepth) {
		if(stackDepth > MAX_STACK_DEPTH) return;
		
		for(Block b : new Block[]{
				startBlock.getRelative(BlockFace.UP), startBlock.getRelative(BlockFace.DOWN),
				startBlock.getRelative(BlockFace.NORTH), startBlock.getRelative(BlockFace.SOUTH),
				startBlock.getRelative(BlockFace.EAST), startBlock.getRelative(BlockFace.WEST),
		}){
			if(b != null && !isFromMatList(b.getType(), ignoredTypes)
					&& !blockList.contains(b.getLocation()) && !notCounted.contains(b.getLocation())){
				
				if(b.getType() == Material.BEDROCK || b.getType() == Material.EMERALD_BLOCK){
					blockList.clear();
					return;
				}
				
				notCounted.add(b.getLocation());//<-- Now that this block has been added, don't count it from now on.
				blockList.add(b.getLocation());
				try{
					getConnectedBlocksTo(b, ignoredTypes, blockList, notCounted, stackDepth+1);
				}
				catch(StackOverflowError error){
					blockList.clear();
					return;
				}
			}
		}
	}
	
	public static BlockFace getNewDirection(BlockFace face, int angle){
		int newAngle = (int) (ShipCraft.toDegreeAngle(face) + angle) % 360;
		if(newAngle < 0) newAngle += 360;
		
		if(newAngle == 0) return face;
		if(newAngle == 180) return face.getOppositeFace();

		BlockFace newFace = ShipCraft.fromDegreeAngle(newAngle);
		return (newFace == null) ? face : newFace;
	}
	
	static boolean isFromMatList(Material type, Material[] list){
		for(Material m : list) if(type == m) return true;
		return false;
	}
}
