package net.osmand.data;

import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransportStop extends MapObject {
	private int[] referencesToRoutes = null;
	private Amenity amenity;
	public int distance;
	public int x31;
	public int y31;
	private List<TransportStopExit> exits;
	private HashMap<String,String> names;

	public TransportStop(){
	}
	
	public int[] getReferencesToRoutes() {
		return referencesToRoutes;
	}
	
	public void setReferencesToRoutes(int[] referencesToRoutes) {
		this.referencesToRoutes = referencesToRoutes;
	}

	public Amenity getAmenity() {
		return amenity;
	}

	public void setAmenity(Amenity amenity) {
		this.amenity = amenity;
	}
	
	@Override
	public void setLocation(double latitude, double longitude) {
		super.setLocation(latitude, longitude);
	}

	public void setLocation(int zoom, int dx, int dy) {
		x31 = dx << (31 - zoom);
		y31 = dy << (31 - zoom);
		setLocation(MapUtils.getLatitudeFromTile(zoom, dy), MapUtils.getLongitudeFromTile(zoom, dx));
	}

	public void addExit(TransportStopExit transportStopExit) {
		if (exits == null) {
			exits = new ArrayList<>();
		}
		exits.add(transportStopExit);
	}

	public List<TransportStopExit> getExits () {
		if (exits == null) {
			return Collections.emptyList();
		}
		return this.exits;
	}

	public String getExitsString () {
		String exitsString = "";
		String refString = "";
		if (this.exits != null) {
			int i = 1;
			exitsString = exitsString +  " Exits: [";
			for (TransportStopExit e : this.exits ) {
				if (e.getRef() != null) {
					refString = " [ref:" + e.getRef() + "] ";
				}
				exitsString = exitsString + " " + i + ")" + refString + e.getName() + " " + e.getLocation() + " ]";
				i++;
			}
		}
		return exitsString;
	}
}
