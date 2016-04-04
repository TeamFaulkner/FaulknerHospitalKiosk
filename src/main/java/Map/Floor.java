package Map;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.UUID;

public class Floor {

    private final int floor; // the level number associated with the floor
    private final UUID uniqueID; // A randomly generated UUID associated with the current floor
    private ArrayList<Node> nodes; //
    private final Building currentBuilding;

    /**
     * Constructor for a new floor with a new randomly generated UUID and an empty list of nodes on the current floor.
     *
     * @param floor           The number that is associated with the current floor.
     * @param currentBuilding The building that the floor is located in.
     */
    public Floor(int floor, Building currentBuilding) {

        this.floor = floor;
        this.uniqueID = UUID.randomUUID();
        this.nodes = new ArrayList<>();
        this.currentBuilding = currentBuilding;

    }

    /**
     * Constructor for a new floor that has been created previously. By default the floor is created without any Nodes.
     *
     * @param floor The number that is associated with the current floor.
     * @param uniqueID An UUID that is associated with the floor.
     * @param currentBuilding The building that the floor is located in.
     */
    public Floor(int floor, UUID uniqueID, Building currentBuilding) {

        this.floor = floor;
        this.uniqueID = uniqueID;
        this.nodes = new ArrayList<>();
        this.currentBuilding = currentBuilding;

    }

    /**
     * Add a new node to the current floor. By default the heuristic cost is set to 0.
     *
     * @param location The x and y coordinate in which the node was placed on the current floor.
     * @return The newly created node
     */
    public Node addNode(Location location) {

        // Create a new node
        Node newNode = new Node(0, location, this);

        // Add the node to the list of nodes on the current floor
        this.nodes.add(newNode);

        // Return the new Node
        return newNode;
    }

    /**
     * Adds a previously generated node to the current floor.
     *
     * @param heuristicCost Cost of the current node.
     * @param uniqueID An UUID that is associated with the node.
     * @param location The x and y coordinate in which the node was placed on the current floor.
     * @param destinations The destinations that are associated with the current node.
     * @return The newly created node
     */
    public Node addNode(double heuristicCost, UUID uniqueID, Location location,
                        EnumMap<Destination, ArrayList<String>> destinations) {

        // Create a new node
        Node newNode = new Node(heuristicCost, uniqueID, location, this, destinations);

        // Add the node to the list of nodes on the current floor
        this.nodes.add(newNode);

        // Return the new Node
        return newNode;
    }

    /**
     * Gets a list of all of the destinations of the given type located on this floor.
     *
     * @param destinationType Type of destination that you want to get.
     * @return A list of all of the destinations of the given type on the current floor.
     */
    public ArrayList<Destination> getFloorDestinations(Destination destinationType) {

        // List of destinations of the given type on the current floor
        ArrayList<Destination> floorDestinations = new ArrayList<>();

        // Go through all of the nodes on the current floor and add destinations of the given type to the list
        for (Node n : nodes) {

            // add all of the destinations of the given type at the current node to the list
            floorDestinations.addAll(n.getDestinations(destinationType));

        }

        // Return all of the destinations of the given type on the current floor
        return floorDestinations;
    }

    /**
     * Gets a list of all of the destinations on the current floor.
     *
     * @return A List of all the destinations on the current floor.
     */
    public ArrayList<Destination> getFloorDestinations() {

        // List of all the destinations on the current floor
        ArrayList<Destination> floorDestinations = new ArrayList<>();

        for (Node n : nodes) {

            // add all of the destinations at the current node to the list
            floorDestinations.addAll(n.getDestinations());

        }

        // Return all of the destinations on the current floor
        return floorDestinations;
    }

    /**
     * Get all of the nodes on the current floor.
     *
     * @return A list of all of the nodes on the current floor.
     */
    public ArrayList<Node> getFloorNodes() {

        return this.nodes;
    }


}
