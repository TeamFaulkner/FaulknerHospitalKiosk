package Map.Memento;

import Map.*;
import Map.Enums.MapState;
import Map.SearchAlgorithms.ISearchAlgorithm;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by binam on 4/24/16.
 */
@JsonSerialize
public class MapMemento {

//    HashMap<UUID, Object> UUIDtoObject; //Maybe use for quick loading

    String name;

    UUID uniqueID;

    UUID startLocationNodeID;

    // Make an array list of BuildingMomentos, because, then you won't have
    //  circular dependency issue (Building would hold a Mapz)
    ArrayList <BuildingMemento> buildingMementos;

    MapState currentMapState;


    public MapMemento(String name, UUID uniqueID, LocationNode startLocationNode, ArrayList < Building > mapBuildings) {

        this.name = name;
        this.uniqueID = uniqueID;
        this.startLocationNodeID = startLocationNode.getUniqueID();
        this.buildingMementos = new ArrayList<BuildingMemento>();


        for(Building building : mapBuildings) {

            buildingMementos.add(new BuildingMemento(building.getName(), building.getUniqueID(), building.getFloors(), building.getCurrentMap()));

        }


//        this.searchAlgorithm = searchAlgorithm;

//        this.directoryList = directoryList;
    }

    public String getName() {

        return name;
    }

    public UUID getUniqueID() {

        return uniqueID;
    }

    public MapState getCurrentMapState() {

        return currentMapState;
    }

    public UUID getStartLocationNodeID() {

        return startLocationNodeID;
    }

    public ArrayList<BuildingMemento> getBuildingMementos() {

        return buildingMementos;
    }
}
