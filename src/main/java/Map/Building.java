package Map;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import Map.Exceptions.FloorDoesNotExistException;
import Map.Exceptions.NoPathException;
import com.fasterxml.jackson.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Observable;
import java.util.UUID;

/**
 * A class the represents a building.
 */

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "uniqueID")
public class Building extends Observable {

    @JsonIgnore
    private BuildingState state;
    private UUID uniqueID; // A randomly generated UUID associated with the current building
    //TODO check if this needs to be public - changed for JSON
    public ArrayList<Floor> floors; // A list of all of the floors in the building
    @JsonIgnore
    private final AStar aStarSearch; // The AStar algorithm associated with the current building
    @JsonIgnore
    private static BuildingObserver observer = new BuildingObserver(); // Observer for all of the buildings
    @JsonIgnore
    private static final Logger LOGGER = LoggerFactory.getLogger(Building.class); // Logger for this class

    /**
     * Default constructor for the building class.
     */
    public Building() {

        this.uniqueID = UUID.randomUUID();
        this.floors = new ArrayList<>();
        this.aStarSearch = new AStar(this);
        this.state = BuildingState.NORMAL;

        LOGGER.info("Created new Building: " + this.toString());

        // start observing the building
        observer.observeBuilding(this);

    }

    /**
     * TODO
     *
     * @param filePath
     * @throws IOException
     */
    public void loadFromFile(String filePath) throws IOException, URISyntaxException, FloorDoesNotExistException {
        File file = new File(getClass().getClassLoader().getResource(filePath).toURI());
        ObjectToJsonToJava.loadFromFile(file, this);
        LOGGER.info("Loading the building from the file: " + filePath.toString());
    }

    /**
     * TODO
     *
     * @param filePath
     * @throws IOException
     */
    public void saveToFile(String filePath) throws IOException, URISyntaxException {

        File file = new File(getClass().getClassLoader().getResource(filePath).toURI());
        ObjectToJsonToJava.saveToFile(file, this);

        LOGGER.info("Saving the building to the file: " + filePath);
    }

    /**
     * TODO
     *
     * @param floor
     * @param location
     * @return
     */
    public LocationNode addNode(int floor, Location location) throws FloorDoesNotExistException {

        // Attempts to add a node to the specified floor
        Floor currentFloor = getFloor(floor);
        LocationNode newLocationNode = currentFloor.addNode(location);

        // mark as value changed
        hasChanged();

        // trigger notification
        notifyObservers();

        return newLocationNode;
    }

    /**
     * TODO
     *
     * @param destinationType
     * @return
     */
    @JsonIgnore
    public ArrayList<String> getBuildingDestinations(Destination destinationType) {

        //ArrayList to hold the entire list of destinations
        ArrayList<String> destinations = new ArrayList<>();

        // Iterate through each floor
        for (Floor currentFloor : floors) {

            // Add the destinations of the current type at the current floor to the list of destinations
            destinations.addAll(currentFloor.getFloorDestinations(destinationType));

        }

        // Return the entire list of destinations of the given type
        return destinations;
    }

    /**
     * TODO
     *
     * @return
     */
    @JsonIgnore
    public ArrayList<String> getBuildingDestinations() {

        //ArrayList to hold the entire list of destinations
        ArrayList<String> dests = new ArrayList<>();

        //temp variable that is used to hold return value of
        ArrayList<String> temp;

        //iterate through each floor
        for(int i = 0; i < floors.size(); i++){

            //get a list of destinations on current floor
            temp = floors.get(i).getFloorDestinations();

            //iterate through the list of destinations
            for (int j = 0; j < temp.size(); j++) {

                //add each destination string to the list of destinations
                dests.add(temp.get(j));
            }

        }

        //return list of destinations
        return dests;
    }

    /**
     * TODO
     *
     * @param floorNumber
     * @return
     */
    public Floor getFloor(int floorNumber) throws FloorDoesNotExistException {

        // iterate through array of floors and get each floorNumber from the array
        for (Floor currentFloor : floors) {

            //if a floorNumber exists with the specified floorNumber number, return that floorNumber
            if (currentFloor.getFloor() == floorNumber) {

                //mark as value changed
                setChanged();

                //return current floorNumber
                return currentFloor;
            }

        }

        // floor does not exist
        throw new FloorDoesNotExistException(floorNumber);
    }

    public Floor addFloor(int floorNumber) {


        for (Floor currentFloor : floors) {

            // if a floor exists with the specified floor number, don't add a new floor.
            if (currentFloor.getFloor() == floorNumber) {

                //return current floor
                return currentFloor;
            }

        }

        Floor newFloor = new Floor(floorNumber, this);

        floors.add(newFloor);

        notifyObservers();

        return newFloor;

    }


    /**
     * TODO
     *
     * @param startNode
     * @param destinationNode
     * @return
     */
//    public ArrayList<LocationNode> getShortestPath(LocationNode startNode, LocationNode destinationNode) throws NoPathException {
//
//        LOGGER.info("Getting the shortest path between " + startNode.toString() + " and " + destinationNode.toString());
//
//        try {
//
//            // run aStar algorithm
//          //  return aStarSearch.getPath(startNode, destinationNode);
//
//        } catch (NoPathException e) {
//
//            LOGGER.error("NoPathException: ", e);
//
//            throw e;
//
//        }
//
//    }

    /**
     * Getter for the building's observer.
     *
     * @return The current building's observer.
     */
    @JsonIgnore
    public BuildingObserver getBuildingObserver(){

        return this.observer;
    }

    @Override
    public String toString() {

        return this.uniqueID.toString();
    }

    public BuildingState getState() {
        return state;
    }

    public void setState(BuildingState state) {
        this.state = state;
    }

    @JsonGetter
    public UUID getUniqueID() {
        return uniqueID;
    }

    @JsonGetter
    public ArrayList<Floor> getFloors() {
        return floors;
    }

}