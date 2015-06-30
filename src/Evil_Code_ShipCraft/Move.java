package Evil_Code_ShipCraft;

public class Move {
	int changeX, changeY, changeZ;
	int degreeAngle;
	
	public Move(int x, int y, int z, int degree){
		changeX = x; changeY = y; changeZ = z;
		degreeAngle = degree;
	}
	
	int changeX(){return changeX;}
	int changeY(){return changeY;}
	int changeZ(){return changeZ;}
	
	double getTravelDistance(){
		return Math.sqrt(changeX^2 + changeY^2 + changeZ^2);
	}
	double getTravelDistanceSquared(){
		return (changeX^2 + changeY^2 + changeZ^2);
	}
	
	void scale(float percent){//float is smaller then double
		changeX *= percent; changeY *= percent; changeZ *= percent;
	}
	
	void scaleMaintainMin(float percent, int min){//float is smaller then double
		if(Math.abs(changeX) > min){
			if(Math.abs(changeX * percent) < min) changeX = min * ((changeX < 0) ? -1 : 1);
			else changeX *= percent;
		}
		if(Math.abs(changeY) > min){
			if(Math.abs(changeY * percent) < min) changeY = min * ((changeY < 0) ? -1 : 1);
			else changeY *= percent;
		}
		if(Math.abs(changeZ) > min){
			if(Math.abs(changeZ * percent) < min) changeZ = min * ((changeZ < 0) ? -1 : 1);
			else changeZ *= percent;
		}
	}
	void scaleMaintainMinY(float percent, int min){//float is smaller then double
		if(Math.abs(changeY) > min){
			if(Math.abs(changeY * percent) < min) changeY = min * ((changeY < 0) ? -1 : 1);
			else changeY *= percent;
		}
	}
	
	void add(Move move2){
		changeX += move2.changeX;
		changeY += move2.changeY;
		changeZ += move2.changeZ;
		degreeAngle += move2.degreeAngle;
	}
	
	int getDegreeAngle(){return degreeAngle;}
	double getRadianAngle(){return degreeAngle * (Math.PI/180);}
}
