package Map;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

import Map.Exceptions.FloorDoesNotExistException;
import com.fasterxml.jackson.annotation.*;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableListValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

import static Map.AStar.constructPath;
import static Map.AStar.getShortestPathTo;

/**
 * A class the represents a building.
 */

@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="uniqueID", scope=Building.class)
public class Building extends Observable {

    @JsonIgnore
    private BuildingState state;
    private UUID uniqueID; // A randomly generated UUID associated with the current building
    private ArrayList<Floor> floors; // A list of all of the floors in the building
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
    public void saveToFile(String filePath) throws IOException, URISyntaxException {

        File file = new File(getClass().getClassLoader().getResource(filePath).toURI());
        ObjectToJsonToJava.saveToFile(file, this);

        LOGGER.info("Saving the building to the file: " + filePath);
    }

    /**
     * TODO
     *
     * @param startNode
     * @param destinationNode
     */
    public void drawShortestPath(LocationNode startNode, LocationNode destinationNode) {

        for (Floor currentFloor : floors) {

            currentFloor.getNodePane().getChildren().clear();

        }

        constructPath(startNode); // run Dijkstra
        System.out.println("Distance to " + destinationNode + ": " + destinationNode.minDistance);

        ArrayList<LocationNode> path = new ArrayList<>();
        path.addAll(getShortestPathTo(destinationNode));
        System.out.println("Path: " + path);

        LOGGER.info("Drawing Shortest Path");

        for (int i = 0; i < path.size() - 1; i++) {

            path.get(i).drawAdjacentNode(path.get(i + 1).getCurrentFloor().getNodePane(), path.get(i + 1));

        }

    }

    /**


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

    public void addFloorsToListView(ListView listView) {

        ObservableList<Floor> Observedfloors = FXCollections.observableArrayList();

        Observedfloors.addAll(this.floors);

        listView.setItems(Observedfloors);

    }

    public void addBuildingDestinationsToListView(ListView listView) {

        ObservableList<String> Observedfloors = FXCollections.observableArrayList();

        Observedfloors.addAll(this.getDestinations());

        listView.setItems(Observedfloors);

    }

    /**
     * TODO
     *
     * @param destinationType
     * @return
     */
    public ArrayList<String> getDestinations(Destination destinationType) {

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
    @JsonGetter
    public ArrayList<String> getDestinations() {

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
     * @param
     * @return
     */
    @JsonIgnore
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

    public Floor addFloor(int floorNumber, String imagePath) {


        for (Floor currentFloor : floors) {

            // if a floor exists with the specified floor number, don't add a new floor.
            if (currentFloor.getFloor() == floorNumber) {

                //return current floor
                return currentFloor;
            }

        }

        Floor newFloor = new Floor(floorNumber, this, imagePath);

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
//          // run aStar algorithm
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
     * TODO
     *
     * @param startNode
     * @param destinationNode
     */
    /*public void drawShortestPath(LocationNode startNode, LocationNode destinationNode) {

        for (Floor currentFloor : floors) {

            currentFloor.getFloor().stackPane.getChildren().removeAll();

        }

        ArrayList<LocationNode> path = LocationNode.getShortestPath(startNode, destinationNode);

        LOGGER.info("Drawing Shortest Path");

        for (int i = 0; i < path.size() - 1; i++) {

            path.get(i).drawAdjacentNode(path.get(i + 1).getCurrentFloor().getNodePane(), path.get(i + 1));

        }

    }*/

    /**
     * Getter for the building's observer.
     *
     * @return The current building's observer.
     */
    @JsonIgnore
    public BuildingObserver getBuildingObserver(){

        return observer;
    }

    @Override
    public String toString() {

        return this.uniqueID.toString();
    }

    @JsonIgnore
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
