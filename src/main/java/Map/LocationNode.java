package Map;


import Map.Enums.CardinalDirection;
import Map.Enums.DestinationType;
import Map.Enums.ImageType;
import Map.Enums.UpdateType;
import Map.EventHandlers.LocationNodeClickedEventHandler;
import Map.Exceptions.EdgeAlreadyExistsException;
import Map.Exceptions.NodeDoesNotExistException;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static Map.Enums.CardinalDirection.*;

public class LocationNode extends Observable implements Observer, Comparable<LocationNode> {

    // Name of this node
    private String name;

    // Unique ID for this node
    private UUID uniqueID;

    // Location of this node
    private Location location;

    // Floor this node is located on
    private Floor currentFloor;

    // Type of image icon to display with this
    private ImageType associatedImage;

    // Edges from this node to adjacent nodes
    private ArrayList<LocationNodeEdge> edges;

    // Destinations at this node
    private ArrayList<Destination> destinations;

    // Variables for Dijkstra's and A* search:

    // the cost of getting from the start node to this node - changes during shortest path search
    private double gScore;

    // the total cost of getting from the start node to the goal - changes during shortest path search
    private double fScore;

    // the node this node came from - changes during shortest path search
    private LocationNode cameFrom;

    private ImageView iconImageView;

    private Label iconLabel;

    // Logger for this class
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationNode.class);

    private ArrayList<Destination> translatedDestinations;

    public LocationNode(String name, Location location, Floor currentFloor, ImageType associatedImage) {

        this.name = name;
        this.uniqueID = UUID.randomUUID();
        this.location = location;
        this.location.addObserver(this);
        this.currentFloor = currentFloor;
        this.associatedImage = associatedImage;
        this.edges = new ArrayList<>();
        this.destinations = new ArrayList<>();
        this.fScore = Double.POSITIVE_INFINITY;
        this.gScore = Double.POSITIVE_INFINITY;
        this.translatedDestinations = new ArrayList<>();

        this.addObserver(this.currentFloor);

        setChanged();
        notifyObservers(UpdateType.LOCATIONNODEADDED);
    }


    /**
     *
     * Constructor used only for loading purposes.
     *   Note that the arraylists of locationNodeEdges and destinations must be
     *   added on afterwards, as they have not been loaded yet.
     * @param name
     * @param location
     * @param currentFloor
     * @param associatedImage
     */
    public LocationNode(String name, UUID uniqueID, Location location, Floor currentFloor, ImageType associatedImage) {

        this.name = name;
        this.uniqueID = uniqueID;
        this.location = location;
        this.location.addObserver(this);
        this.currentFloor = currentFloor;
        this.associatedImage = associatedImage;
        this.edges = new ArrayList<>();
        this.destinations = new ArrayList<>();
        this.fScore = Double.POSITIVE_INFINITY;
        this.gScore = Double.POSITIVE_INFINITY;

        this.addObserver(this.currentFloor);

        setChanged();
        notifyObservers(UpdateType.LOCATIONNODEADDED);
    }

    /**
     * TODO
     *
     * @param destinationType
     * @param name
     */
    public Destination addDestination(String name, DestinationType destinationType) {

        Destination newDestination = new Destination(name, destinationType, this);

        this.destinations.add(newDestination);

        setChanged();
        notifyObservers(UpdateType.DESTINATIONCHANGE);

        return newDestination;
    }

    /**
     * Remove a destination from this node
     *
     * @param destination
     */
    public void removeDestination(Destination destination) {

        this.destinations.remove(destination);

        setChanged();
        notifyObservers(UpdateType.DESTINATIONCHANGE);

    }

    /**
     * Get all of the destinations of the specified destination type at this node
     *
     * @param destinationType
     * @return ArrayList of destinations of specified type
     */
    public ArrayList<Destination> getDestinations(DestinationType destinationType) {

        ArrayList<Destination> nodeDestinations = new ArrayList<>();

        for (Destination d : this.destinations) {

            if (d.isDestinationType(destinationType)) {

                nodeDestinations.add(d);

            }

        }

        return nodeDestinations;
    }

    //
    //
    //
    //
    public ArrayList<Destination> getAllDestinations(DestinationType destinationType) {

        ArrayList<Destination> nodeDestinations = new ArrayList<>();

        for (Destination d : this.destinations) {

                nodeDestinations.add(d);

        }

        return nodeDestinations;
    }
    //
    //
    //
    //
    /**
     * Get all destinations at the node
     *
     * @return ArrayList of destinations
     */
    public ArrayList<Destination> getDestinations() {


        return this.destinations;

    }

    /**
     * Compute the straight line distance between two nodes
     *
     * @param destinationLocationNode The destination to calculate the distance to from this node
     * @return Computed distance
     */
    public double getDistanceBetweenNodes(LocationNode destinationLocationNode) {

        // location of destination node
        Location destinationLocation = destinationLocationNode.getLocation();

        // return the distance between the nodes
        return this.location.getDistanceBetween(destinationLocation);
    }

    /**
     * Draw all of the nodes as they should appear for the Administrative Dashboard View
     *
     * @param pane
     */
    public void drawAdmin(Pane pane) {

        if (pane.getChildren().contains(this.iconLabel)) {

            this.iconLabel.setLayoutX(this.location.getX() - (this.iconLabel.getPrefWidth() / 2));
            this.iconLabel.setLayoutY(this.location.getY() - (this.iconLabel.getPrefHeight() / 2));

            return;
        }

        this.iconLabel = new Label();

        try {

            Image icon = new Image(new URL("file:///" + System.getProperty("user.dir") + "/resources/" +
                    this.associatedImage.getResourceFileName()).toString());

            this.iconLabel.setPrefWidth(icon.getWidth());
            this.iconLabel.setPrefHeight(icon.getHeight());

            this.iconImageView = new ImageView(icon);

        } catch (MalformedURLException e) {

            LOGGER.error("Unable to load the image file for the Location Node: " + this.toString(), e);

        }

        iconLabel.setGraphic(this.iconImageView);

        this.iconLabel.setLayoutX(this.location.getX() - (this.iconLabel.getPrefWidth() / 2));
        this.iconLabel.setLayoutY(this.location.getY() - (this.iconLabel.getPrefHeight() / 2));

        pane.getChildren().add(this.iconLabel);

        this.iconLabel.addEventHandler(MouseEvent.MOUSE_CLICKED, new LocationNodeClickedEventHandler(this));

    }

    /**
     * Draw all edges to neighbors of this node
     *
     * @param locationNodeEdgePane
     */
    public void drawEdgesAdmin(Pane locationNodeEdgePane) {

        // If any edges exist
        if (this.edges.size() != 0) {

            LOGGER.info("Drawing edges for the Node: " + this.toString());

            // Draw lines for edges
            for (LocationNodeEdge edge : this.edges) {

                edge.drawEdgeAdmin(locationNodeEdgePane);

            }

        }

    }

    /**
     * Draw edges between nodes in path
     *
     * @param pane
     * @param path
     */
    public void drawEdgesNormal(Pane pane, ArrayList<LocationNode> path) {

        LocationNode current;
        LocationNode next;

        // For each node in the path
        // Number of edges = number of nodes in path - 1
        for (int i = 0; i < path.size() - 1; i++) {

            current = path.get(i);
            next = path.get(i+1);

            // Find edge between specified nodes
            for (LocationNodeEdge edge : current.getEdges()) {

                if(edge.isEdgeBetweenNodes(current, next)) {

                    // Found edge between two specified nodes
                    edge.drawEdge(pane);
                    break;

                }

            }

        }

    }

    public void drawNormal(Pane pane, int x, int y) {

        this.drawNormal(pane, this.associatedImage, x, y);
        
    }

    public void drawNormal(Pane pane, ImageType imageType, int x, int y) {

        if (pane.getChildren().contains(this.iconLabel)) {

            return;
        }

        this.iconLabel = new Label();

        try {

            Image icon = new Image(new URL("file:///" + System.getProperty("user.dir") + "/resources/" +
                    imageType.getResourceFileName()).toString());

            this.iconImageView = new ImageView(icon);

        } catch (MalformedURLException e) {

            LOGGER.error("Unable to load the image file for the Location Node: " + this.toString(), e);

        }

        this.iconImageView.setFitWidth(20);
        this.iconImageView.setPreserveRatio(true);

        this.iconLabel.setPrefWidth(this.iconImageView.getFitWidth());
        this.iconLabel.setPrefHeight(this.iconImageView.getFitHeight());

        this.iconLabel.setGraphic(this.iconImageView);

        this.iconLabel.setLayoutX(this.location.getX() - (this.iconLabel.getPrefWidth() + x));
        this.iconLabel.setLayoutY(this.location.getY() - (this.iconLabel.getPrefHeight() + y));

        pane.getChildren().add(this.iconLabel);


    }

    public void adminDrawCurrent() {

        if (this.iconLabel != null) {

            iconLabel.setBorder(
                    new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                            new BorderWidths(4))));

            this.iconLabel.setLayoutX(this.location.getX() - (this.iconLabel.getPrefWidth() / 2) + 1);
            this.iconLabel.setLayoutY(this.location.getY() - (this.iconLabel.getPrefHeight() / 2) + 1);

        }


    }

    public void adminUndrawCurrent() {

        if (this.iconLabel != null) {

            iconLabel.setBorder(null);

            this.iconLabel.setLayoutX(this.location.getX() - (this.iconLabel.getPrefWidth() / 2));
            this.iconLabel.setLayoutY(this.location.getY() - (this.iconLabel.getPrefHeight() / 2));

        }

    }



    public void undrawLocationNode(Pane locationNodePane, Pane locationNodeEdgePane) {

        locationNodePane.getChildren().remove(this.iconLabel);

        for (LocationNodeEdge edge : this.edges) {

            edge.undrawEdge(locationNodeEdgePane);

        }


        this.iconImageView = null;

    }


     //Note: iterators returned by this class's iterator will throw ConcurrentModificationException
     // if this is modified any time after the iterator is created

    /**
     * Remove all edges from this location node
     */
    public void deleteLocationNodeEdgeConnections() {

        ArrayList<LocationNodeEdge> tempEdges = this.edges;
        int numEdges = tempEdges.size();

        for(int i = numEdges - 1; i >= 0; i--) {

            tempEdges.get(i).getOtherNode(this).removeEdgeConnection(tempEdges.get(i));

            this.removeEdgeConnection(tempEdges.get(i));

        }

        this.edges = tempEdges;
    }

    /**
     * TODO
     *
     * @param edge
     */
    public void removeEdgeConnection(LocationNodeEdge edge) {

        this.edges.remove(edge);

        setChanged();
        notifyObservers(UpdateType.EDGEREMOVED);
    }

    /**
     * Add edge between this node and a neighboring node
     * @param adjacentNode
     */
    public void addEdge(LocationNode adjacentNode) throws NodeDoesNotExistException, EdgeAlreadyExistsException {

        if (adjacentNode == null) {

            // Adjacent Node does not exist
            throw new NodeDoesNotExistException("Specified adjacent node does not exist.");
        }

        // Check if edge between nodes already exists
        for(LocationNodeEdge edge : edges) {

            if (edge.edgeExists(this, adjacentNode)) {

                // Edge has already been added
               throw new EdgeAlreadyExistsException(this, adjacentNode);

            }
        }

        // Create new edge
        LocationNodeEdge newEdge = new LocationNodeEdge(this, adjacentNode);

        // Add new edge between nodes - add edges reference to both node's list of edges
        edges.add(newEdge);
        adjacentNode.getEdges().add(newEdge);

        setChanged();
        notifyObservers(UpdateType.LOCATIONNODEEDGE);

    }

    /**
     * Given an adjacent node, get find the connecting edge if it exists
     */
    public LocationNodeEdge getEdgeBetween(LocationNode adjacentNode) {
        for (LocationNodeEdge edge : edges) {

            if (edge.isEdgeBetweenNodes(this, adjacentNode)) {

                return edge;

            }
        }

        // Edge does not exist
        return null;
    }


    public ArrayList<LocationNode> getAdjacentLocationNodes() {

        ArrayList<LocationNode> adjacentNodes = new ArrayList<>();

        for (LocationNodeEdge edge : this.edges) {

            adjacentNodes.add(edge.getOtherNode(this));

        }

        return adjacentNodes;
    }

    /**
     * This function returns a CardinalDirection which tells the relation of the location between
     *   this node and the nextNode. It does this using angles.
     *
     * @param nextNode The node to which the the CardinalDirection should be calculated
     * @return Enum CardinalDirection
     */
    public CardinalDirection getDirectionsTo(LocationNode nextNode) {

        //Enum CardinalDirection which currently has NORTH, EAST, SOUTH, WEST
        CardinalDirection cardinalDirection;

        //x1, y1 is the position of the this node
        double x1 = this.getLocation().getX();
        double y1 = this.getLocation().getY();

        //x2, y2 is the position of nextNode
        double x2 = nextNode.getLocation().getX();
        double y2 = nextNode.getLocation().getY();

        //Get the difference between the x and y
        double xDiff = x2-x1;
        double yDiff = y2-y1;

        //Calculate the angle between the nodes using atan() function
        double angle = Math.toDegrees( Math.atan(yDiff/xDiff) );

        //If nextNode is in the II quadrant (>= was set to deal with edge case)
        if (xDiff < 0 && yDiff >= 0) {

            angle = 180 + angle;

        }
        //If nextNode is in the III quadrant
        else if (xDiff < 0 && yDiff < 0) {

            angle = 180 + angle;

        }
        //If nextNode is in the IV quadrant
        else if (xDiff >= 0 && yDiff < 0) {

            angle  = 360 + angle;

        }
        //Angle is between 225-315, including 315
        if((225 < angle) && (angle <= 315)){
            cardinalDirection = NORTH;
        }
        //Angle is between 315-360, or 0-45, including 360, 0, and 45
        else if (((315 < angle) && (angle <= 360)) || ((0 <= angle) && (angle <=45))) {
            cardinalDirection = EAST;
        }
        //Angle is between 45-135, including 135
        else if ((45 < angle) && (angle <= 135)) {
            cardinalDirection = SOUTH;
        }
        //Angle is between 135-225, including 225
        else if ((135 < angle) && (angle <= 225)) {
            cardinalDirection = WEST;
        }
        //Angle is lower than 0 or higher than 360
        else {
            cardinalDirection = null;
        }

        return cardinalDirection;
    }


    public boolean onSameFloor(LocationNode otherLocationNode) {

        return this.currentFloor.equals(otherLocationNode.getCurrentFloor());
    }



    //||\\ Getters and Setters //||\\

    public Location getLocation() {

        return location;
    }

    public ArrayList<LocationNodeEdge> getEdges() {

        return edges;
    }

    @Override
    public String toString() {

        return this.name;
    }

    @Override
    public int compareTo(LocationNode other) {

        // TODO
        return 0;
    }

    @Override
    public void update(Observable o, Object arg) {



        setChanged();
        notifyObservers(arg);
    }

    public String getName() {

        return name;
    }

    public UUID getUniqueID() {

        return uniqueID;
    }

    public Floor getCurrentFloor() {

        return currentFloor;
    }

    public ImageType getAssociatedImage() {

        return associatedImage;
    }

    public double getfScore() {

        return fScore;

    }

    public void setfScore(double fScore) {

        this.fScore = fScore;

    }

    public double getgScore() {

        return gScore;

    }

    public void setgScore(double gScore) {

        this.gScore = gScore;

    }

    public LocationNode getCameFrom() {

        return cameFrom;

    }

    public void setCameFrom(LocationNode cameFrom) {

        this.cameFrom = cameFrom;

    }

    public boolean isSameFloor(LocationNode locationNode) {

        return this.currentFloor.equals(locationNode.getCurrentFloor());
    }

    public Label getIconLabel() {

        return iconLabel;
    }

    public ArrayList<Destination> getTranslatedDestinations() {

        return translatedDestinations;
    }

    public void setTranslatedDestinations(ArrayList<Destination> translatedDestinations) {

        this.translatedDestinations = translatedDestinations;
    }

}
