package Map;

import Map.Enums.DestinationType;
import Map.Enums.ImageType;
import Map.Enums.MapState;
import Map.Enums.UpdateType;
import Map.EventHandlers.EditDestinationEventHandler;
import Map.Exceptions.DefaultFileDoesNotExistException;
import Map.Exceptions.FloorDoesNotExistException;
import Map.Exceptions.NoPathException;
import Map.Exceptions.NodeDoesNotExistException;
import Map.Exceptions.*;
import Map.Memento.*;
import Map.SearchAlgorithms.AStar;
import Map.SearchAlgorithms.Dijkstras;
import Map.SearchAlgorithms.ISearchAlgorithm;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.apache.log4j.lf5.util.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;


public class Map implements Observer {

    private ObservableList icons = FXCollections.observableArrayList();

    private String name;

    // Unique ID for this Map
    private UUID uniqueID;

    private LocationNode startLocationNode;

    private ArrayList<Building> mapBuildings;

    // Building UUIDs for serialization
    private ArrayList<UUID> buildingIdList;

    private ISearchAlgorithm searchAlgorithm;

    private MapState currentMapState;

    // TODO DELETE EVENTUALLY:
    // I'm not sure who wrote this, why are we deleting it?  - Binam
    // To do was added on commit f32ef4d, "Merged with little success"
    private ObservableList<Destination> directoryList;

    private Path currentPath;

    //||\\ Current Destination //||\\

    private Destination currentDestination;

    //||\\ Current LocationNode //||\\

    private LocationNode currentLocationNode;

    //||\\ Current adjacent LocationNode //||\\

    private LocationNode currentAdjacentNode;


    //||\\ Current LocationNodeEdge //||\\

    private LocationNodeEdge currentLocationNodeEdge;

    //
    private ObservableList<LocationNode> currentAdjacentLocationNodes;

    //
    private ObservableList<Destination> currentLocationNodeDestinations;


    //||\\ Current Floor //||\\

    private Floor currentFloor;

    //
    private ObservableList<LocationNode> currentFloorLocationNodes;

    //
    private ObservableList<Destination> currentFloorDestinations;

    //
    private Pane currentFloorLocationNodePane;

    //
    private Pane currentFloorEdgePane;

    //
    private ImageView currentFloorImage;

    //||\\ Current Building //||\\

    private Building currentBuilding;

    //
    private ObservableList<Floor> currentBuildingFloors;

    //
    private ObservableList<Destination> currentBuildingDestinations;

    //
    private ObservableList<LocationNode> currentKioskLocationNodes;


    // Logger for this class
    private static final Logger LOGGER = LoggerFactory.getLogger(Map.class);

    /*
    TODO create exception to throw when adding something to the map when something of the sametype already
     ... has that name for all types except hallway, elevator, and stairs
    */
    public Map(String name) {

        this.name = name;
        this.uniqueID = UUID.randomUUID();
        this.startLocationNode = null;
        this.mapBuildings = new ArrayList<>();
        this.buildingIdList = new ArrayList<>();
        this.searchAlgorithm = new AStar();
        this.currentMapState = MapState.NORMAL;
        this.directoryList = FXCollections.observableArrayList();
        this.currentLocationNode = null;
        this.currentAdjacentLocationNodes = FXCollections.observableArrayList();
        this.currentLocationNodeDestinations = FXCollections.observableArrayList();
        this.currentFloor = null;
        this.currentFloorLocationNodes = FXCollections.observableArrayList();
        this.currentFloorDestinations = FXCollections.observableArrayList();
        this.currentFloorLocationNodePane = new Pane();
        this.currentFloorEdgePane = new Pane();
        this.currentFloorImage = new ImageView();
        this.currentBuilding = null;
        this.currentBuildingFloors = FXCollections.observableArrayList();
        this.currentBuildingDestinations = FXCollections.observableArrayList();
        this.currentKioskLocationNodes = FXCollections.observableArrayList();

    }


    public void addBuilding(String name) {

        for (Building building : this.mapBuildings) {

            if (building.getName().equals(name)) {

                LOGGER.debug("There is already a building with the name: ", name);

                return;
            }

        }

        Building newBuilding = new Building(name, this);

        this.setCurrentBuilding(newBuilding);
        this.mapBuildings.add(newBuilding);
        this.buildingIdList.add(newBuilding.getUniqueID());

    }


    public void addFloor(String name, String resourceFileName) {

        if (this.currentBuilding == null) {

            LOGGER.debug("Floor could not be added because the currentBuilding was null");

            return;
        }

        this.setCurrentFloor(this.currentBuilding.addFloor(name, resourceFileName));

    }


    public void addLocationNode(String name, Location location, ImageType imageType) {

        if (this.currentFloor == null) {

            LOGGER.debug("LocationNode could not be added because the currentFloor was null");

            return;
        }

        this.setCurrentLocationNode(this.currentFloor.addLocationNode(name, location, imageType));

        this.currentLocationNode.drawAdmin(this.currentFloorLocationNodePane);
        this.currentLocationNode.drawEdgesAdmin(this.currentFloorEdgePane);
        this.currentLocationNode.adminDrawCurrent();

        this.currentFloorLocationNodes.add(this.currentLocationNode);

    }

    public void addMultiLevelLocationNode(String name, Location location, ImageType imageType, ArrayList<Floor> floors) {


        LocationNode current = floors.get(0).addLocationNode(name, location, imageType);
        LocationNode next;

        if (this.currentFloor.equals(floors.get(0))) {

            setCurrentLocationNode(current);

        }

        for (int i = 0; i < floors.size() - 1; i++) {

            next = floors.get(i+1).addLocationNode(name, location, imageType);

            try {

                current.addEdge(next);

            } catch (NodeDoesNotExistException e) {

                LOGGER.debug("Unable to add all off the Location Nodes to the multi level path.", e);

                break;

            } catch (EdgeAlreadyExistsException e) {

                LOGGER.debug("Edge already exists.");

                break;
            }

            current = next;

            if (this.currentFloor.equals(floors.get(i+1))) {

                setCurrentLocationNode(current);

            }

        }

    }


    public void addLocationNodeEdge() throws NodeDoesNotExistException {

        if(this.currentLocationNode == null) {

            LOGGER.debug("Edge could not be added because the currentLocationNode was null");

            return;

        }

        if(this.currentAdjacentNode == null) {

            LOGGER.debug("Edge could not be added because the currentAdjacentNode was null");

            return;

        }

        if(this.currentLocationNode.equals(currentAdjacentNode)) {

            LOGGER.debug("Edge could not be added because the currentAdjacentNode and the currentLocationNode are the same");

            return;

        }

        try {

            this.currentLocationNode.addEdge(currentAdjacentNode);

            // Redraw Edge
            this.currentLocationNode.drawAdmin(this.currentFloorLocationNodePane);
            this.currentLocationNode.drawEdgesAdmin(this.currentFloorEdgePane);

            // Update Observers
            this.currentAdjacentLocationNodes.add(currentAdjacentNode);

        } catch(EdgeAlreadyExistsException e) {

            e.printStackTrace();
        }

    }


    public void addDestination(String name, DestinationType destinationType) {

        if (this.currentLocationNode == null) {

            LOGGER.debug("Destination could not be added because the currentLocationNode was null");

            return;
        }

        this.setCurrentDestination(this.currentLocationNode.addDestination(name, destinationType));

    }

    public void removeDestination() {

        if (this.currentFloor == null) {

            LOGGER.debug("Current floor is null. Can't remove destination.");

            return;
        }

        if(currentDestination == null){

            LOGGER.debug("Current destination is null. Can't remove destination.");

            return;
        }

        this.getCurrentBuildingDestinations().remove(currentDestination);
        this.getCurrentFloorDestinations().remove(currentDestination);
        this.getCurrentLocationNodeDestinations().remove(currentDestination);
        this.getCurrentLocationNode().removeDestination(currentDestination);

        this.currentDestination = null;

        return;
    }

    public void removeLocationNodeEdge() {

        if (this.currentFloor == null) {

            LOGGER.debug("Current floor is null. Can't remove edge.");

            return;
        }

        if(currentLocationNodeEdge == null){

            LOGGER.debug("currentLocationNodeEdge is null. Can't remove edge.");

            return;
        }

        this.getCurrentLocationNode().removeEdgeConnection(this.currentLocationNodeEdge);

        this.currentAdjacentLocationNodes.remove(this.currentAdjacentNode);
        this.currentLocationNodeEdge = null;
        this.currentAdjacentNode = null;

        return;
    }

    public void removeLocationNode() {

        if (this.currentFloor == null) {

            LOGGER.debug("Current floor is null. Can't remove location node.");

            return;
        }

        if (this.currentLocationNode == null) {

            LOGGER.debug("currentLocationNode is null. Can't remove location node.");

            return;
        }

        LOGGER.info("Removing Location Node: " + this.currentLocationNode.toString());

        this.currentLocationNodeDestinations.removeAll(this.currentLocationNode.getDestinations());
        this.currentAdjacentLocationNodes.removeAll(this.currentLocationNode.getAdjacentLocationNodes());
        this.currentFloorLocationNodes.remove(this.currentLocationNode);
        this.currentFloorDestinations.removeAll(this.currentLocationNode.getDestinations());
        this.currentBuildingDestinations.removeAll(this.currentLocationNode.getDestinations());
        this.currentDestination = null;

        this.currentLocationNode.undrawLocationNode(this.currentFloorLocationNodePane, this.currentFloorEdgePane);

        this.currentFloor.removeLocationNode(this.currentLocationNode);

    }

    public void removeFloor() {

        // TODO fill in
        // TODO create debug message

    }

    public void physicianDirectory() {

        this.directoryList.clear();

        for (Building building : this.mapBuildings) {



            this.directoryList.setAll(building.getBuildingDestinations(DestinationType.PHYSICIAN));

        }

    }


    public List<Destination> getPhysicianDirectory() {

        this.directoryList.clear();

        for (Building building : this.mapBuildings) {

            this.directoryList.setAll(building.getBuildingDestinations(DestinationType.PHYSICIAN));

        }

        return directoryList;
    }


    public List<Destination> allDirectory() {

        this.directoryList.clear();

        for (Building building : this.mapBuildings) {

            this.directoryList.setAll(building.getBuildingDestinations());
        }

        return this.directoryList;
    }


    public void departmentDirectory() {

        this.directoryList.clear();

        for (Building building : this.mapBuildings) {

            this.directoryList.addAll(building.getBuildingDestinations(DestinationType.DEPARTMENT));
            // TODO ascending order by name (create comparator that uses Destination.toString())
        }

    }


    public List<Destination> getDepartmentDirectory() {

        this.directoryList.clear();

        for (Building building : this.mapBuildings) {

            this.directoryList.setAll(building.getBuildingDestinations(DestinationType.DEPARTMENT));

        }
        return directoryList;
    }

    // TODO do we need this method and the getServiceDirectory() method? If not, refactor
    public void serviceDirectory() {

        this.directoryList.clear();

        for (Building building : this.mapBuildings) {

            this.directoryList.addAll(building.getBuildingDestinations(DestinationType.SERVICE));

        }

    }

    /**
     * Translate services and departments
     * @param destType DestinationType enum
     * @param currentLocale Locale to translate to
     * @return
     */
    public void translateDirectory(DestinationType destType, Locale currentLocale) {

        List<Destination> directoryList;

        if(DestinationType.SERVICE.equals(destType)) {

            // Get the directory of services for all buildings
            directoryList = getServiceDirectory();

        } else if(DestinationType.DEPARTMENT.equals(destType)) {

            // Get the directory of services for all buildings
            directoryList = getDepartmentDirectory();

        } else {

            // Only service and department translation is supported
            return;
        }

        // Check if locale specified default language
        if(currentLocale.getDisplayLanguage() == "en") {

            // Directory does not require translation
            return;
        }

        // English value string
        String enValue;

        // Locale specific string value
        String translation;

        // Resource Bundle with translatable text
        ResourceBundle labels;

        // Resource Bundle with English text
        ResourceBundle enLabels;

        // English Resource Bundle keys
        Set<String> enKeys;

        Destination aDestination;

        // Keys with values associated with Service destination names
        ArrayList<String> destKeys = new ArrayList<>();

        // HashMap key is value associated with bundle key; bundle key is HashMap value
        HashMap<String, String> enHashMap = new HashMap<>();

        // HashMap key is a bundle key; HashMap value is a Destination with associated name
        HashMap<String, Destination> keyDestHashMap = new HashMap<>();

        // English, United States locale
        Locale enLocale = new Locale("en", "US");



        // Create ResourceBundle containing locale-specific translatable text
        labels = ResourceBundle.getBundle("LabelsBundle", currentLocale);

        // Create ResourceBundle containing English-locale specific text
        enLabels = ResourceBundle.getBundle("LabelsBundle", enLocale);

        // Get English resource bundle keys
        enKeys = enLabels.keySet();

        // For each English key in key set
        for(String enKey : enKeys) {

            // get value associated with key
            enValue = enLabels.getString(enKey);

            // add enValue as key and enKey as value in HashMap
            enHashMap.put(enValue, enKey);

        }

        // For each destination in the directory list
        for(Destination dest : directoryList) {

            String destName = dest.getName();

            // Check if destination name is a key in the enHashMap
            if(enHashMap.containsKey(destName)) {

                // This service destination as an available translation
                // Get value from enHashMap
                String val = enHashMap.get(destName);

                // Add value from enHashMap corresponding to key and associated Destination to HashMap
                keyDestHashMap.put(val, dest);

                // Add val to array of service keys
                destKeys.add(val);

            }
        }

        // For each key in the key in the keyDestHashMap
        for(String key: destKeys) {

            // Destination associated with key
            aDestination = keyDestHashMap.get(key);

            // Get translation associated with key
            translation = labels.getString(key);

            if(translation != null) {

                // Set destination name translation
                aDestination.setTranslation(translation);

            } else {

                // No translation available; using existing name
                aDestination.setTranslation(aDestination.getName());
            }
        }

        //getServiceDirectory();

    }


    public List<Destination> getServiceDirectory() {

        this.directoryList.clear();



        for (Building building : this.mapBuildings) {

            this.directoryList.setAll(building.getBuildingDestinations(DestinationType.SERVICE));

        }

        return directoryList;
    }

    /**
     * TODO
     *
     * @param stackPane
     */
    public void setupAdminStackPane(StackPane stackPane) {


        this.currentMapState = MapState.ADMIN;

        stackPane.getChildren().clear();
        stackPane.getChildren().addAll(this.currentFloorImage, this.currentFloorEdgePane, this.currentFloorLocationNodePane);


        this.currentFloorImage.setPreserveRatio(true);
        this.currentFloorImage.setSmooth(true);
        this.currentFloorImage.setCache(true);
        this.currentFloorImage.boundsInParentProperty().addListener(new ChangeListener<Bounds>() {

            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {

                LOGGER.info("Image Bounds changed, updating pane bounds");
                LOGGER.info("Old Image Bounds: " + newValue.toString());
                LOGGER.info("New Image Bounds: " + newValue.toString());

                currentFloorLocationNodePane.setPrefWidth(newValue.getWidth());
                currentFloorLocationNodePane.setPrefHeight(newValue.getHeight());


                LOGGER.info("" + currentFloorEdgePane.getPrefWidth());

                currentFloorEdgePane.setPrefWidth(newValue.getWidth());
                currentFloorEdgePane.setPrefHeight(newValue.getHeight());

                LOGGER.info("" + currentFloorEdgePane.getPrefWidth());


            }

        });

        this.currentFloorLocationNodePane.getChildren().clear();
        this.currentFloorEdgePane.getChildren().clear();
        this.currentBuildingDestinations.setAll(this.currentBuilding.getBuildingDestinations());

        this.currentFloorLocationNodePane.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {

                if (currentMapState != MapState.MOVENODE) {

                    return;
                }
                if(currentLocationNode == null){

                    return;
                }

                if (event.getTarget().equals(currentLocationNode.getIconLabel())) {

                    currentLocationNode.getLocation().setX(event.getX());
                    currentLocationNode.getLocation().setY(event.getY());

                    LOGGER.info("Moving location node to x: " + event.getX() + " y: " + event.getY());
                }

            }

        });

    }

    // TODO enable and test searchAlgorithm.getPath() - commented because it has not been tested
    public void setupPathStackPane(StackPane stackPane) {

        ArrayList<LocationNode> path;

        try {

            //path = this.searchAlgorithm.getPath(this.startLocationNode, this.currentLocationNode);
            path = (new AStar()).getPath(this.startLocationNode, this.currentLocationNode);


        } catch (NoPathException e) {

            LOGGER.error("Unable to get a path", e);

            return;
        }

        stackPane.getChildren().clear();
        stackPane.getChildren().addAll(this.currentFloorImage, this.currentFloorEdgePane, this.currentFloorLocationNodePane);

        this.currentPath = new Path(this.currentFloorImage, this.currentFloorLocationNodePane, this.currentFloorEdgePane,
                path);

        this.currentPath.setup();

    }

    public void setupDirections(ListView textualDirections) {

        ObservableList<Direction> destinations = FXCollections.observableArrayList();
        destinations.addAll(this.currentPath.getDirections());

        textualDirections.setItems(destinations);

    }


    public ArrayList<LocationNode> getPathFromKiosk(LocationNode destination) throws NoPathException {

        return this.searchAlgorithm.getPath(this.startLocationNode, destination);

    }


    public void useAStar() {

        this.searchAlgorithm = new AStar();

    }


    public void useDijkstras() {

        this.searchAlgorithm = new Dijkstras();

    }

    public void pathNextFloor() {

        this.currentPath.drawNextFloor();

    }

    public void pathPreviousFloor() {

        this.currentPath.drawPreviousFloor();

    }


    @Override
    public void update(Observable o, Object arg) {

        // Check to see if the argument is null
        if (arg == null) {

            LOGGER.debug("Observer was updated but the argument was null");

            return;
        }

        LOGGER.info("Updating the Map");

        UpdateType updateType = ((UpdateType) arg);

        LOGGER.info(updateType.toString());

        switch (updateType) {

            case DESTINATIONCHANGE:

                // TODO cleanup by only modifying one destination

                this.currentKioskLocationNodes.clear();
                this.currentKioskLocationNodes.addAll(this.currentBuilding.getBuildingLocationNodes(ImageType.KIOSK));
                if (this.currentLocationNode != null) {

                    this.currentLocationNodeDestinations.clear();
                    this.currentLocationNodeDestinations.addAll(this.currentLocationNode.getDestinations());

                }

//
//                // remove current location node destinations from current floor destinations and building destinations
                this.currentFloorDestinations.removeAll(this.currentLocationNodeDestinations);
               this.currentBuildingDestinations.removeAll(this.currentLocationNodeDestinations);

//                // Update currentLocationNodeDestinations by clearing the list, and replacing it with the getDestinations function
//                this.currentLocationNodeDestinations.clear();
//                this.currentLocationNodeDestinations.addAll(this.currentLocationNode.getDestinations());

//                // Add current location node destinations from current floor and building destinations
                this.currentFloorDestinations.addAll(this.currentLocationNodeDestinations);
                this.currentBuildingDestinations.addAll(this.currentLocationNodeDestinations);


                break;

            case LOCATIONNODEPOSITION:

                this.currentLocationNode.drawAdmin(this.currentFloorLocationNodePane);
                this.currentLocationNode.drawEdgesAdmin(this.currentFloorEdgePane);

                break;

            case LOCATIONNODEADDED:

                if (this.currentLocationNode != null) {

                    this.currentLocationNode.drawAdmin(this.currentFloorLocationNodePane);
                    this.currentLocationNode.drawEdgesAdmin(this.currentFloorEdgePane);

                }

                // TODO decide whether or not we are going to redraw the entire floor
                break;

            case FLOORADDED:

                this.currentBuildingFloors.clear();
                this.currentBuildingFloors.addAll(this.currentBuilding.getFloors());

                break;


            case LOCATIONNODEREMOVED:

//                this.currentFloorLocationNodePane.getChildren().remove(this.currentLocationNode.;
                this.currentLocationNode.undrawLocationNode(this.currentFloorLocationNodePane, this.currentFloorEdgePane);
                this.currentLocationNode.getEdges().clear();
                this.setCurrentLocationNode(null);

                break;

            case BUILDINGADDED:

                this.buildingChangeUpdater(this.currentBuilding);


                this.currentKioskLocationNodes.clear();
                this.currentKioskLocationNodes.addAll(this.currentBuilding.getBuildingLocationNodes(ImageType.KIOSK));

                break;

            case LOCATIONNODEEDGE:

                if (this.currentLocationNode != null) {

                    this.currentLocationNode.drawAdmin(this.currentFloorLocationNodePane);
                    this.currentLocationNode.drawEdgesAdmin(this.currentFloorEdgePane);

                }

                break;

            case EDGEREMOVED:

                if (this.currentLocationNodeEdge != null) {

                    this.currentLocationNodeEdge.undrawEdge(this.currentFloorEdgePane);

                }

                break;

            default:

                break;
        }
    }

    @Override
    public String toString() {

        return this.name;
    }

    /**
     * Save this map to a JSON file
     *
     * @param file The JSON file you want to save to
     */
    public void saveToFile(File file) throws IOException, URISyntaxException {

        ObjectMapper objectMapper = new ObjectMapper();


        MapMemento mapMemento = saveStateToMemento();

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, mapMemento);

        System.out.println(objectMapper.writeValueAsString(mapMemento));

        LOGGER.info("Saving the map to the file: " + file.toString());

    }


    /**
     * Load a map from a JSON file. It does this by loading the Json object into a Map Memento object, and then
     *      change the memento to a map using the loadStateFromMemento(mapMemento) method
     *
     * @param specifiedFilePath The JSON file you want to load from
     */
    public static Map loadFromFile(URL specifiedFilePath) throws IOException, FloorDoesNotExistException, DefaultFileDoesNotExistException {

        // Initialize the mapMemento object
        MapMemento mapMemento = null;

        // The URL set to the default.json file. Currently the same as spcifiedFilePath
        URL defaultFilePath = null;

        // Set up an ObjectMapper for deserialization (Change Json to Java object)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {

            // TODO: change function so that it is actually using in the taken specfiedFilePath object
            // Set the defaultFilePath
            defaultFilePath = new URL("file:///" + System.getProperty("user.dir") + "/resources/" + "default.json");
            specifiedFilePath = new URL("file:///" + System.getProperty("user.dir") + "/resources/" + "default.json");

        } catch (MalformedURLException e) {

            // If the URL is broken/malformed, print what happened.
            e.printStackTrace();

        }

        try {

            // File objects associated with the URL's
            File specifiedFile = new File(specifiedFilePath.toURI());

            File defaultFile = new File(defaultFilePath.toURI());

            // If the specified file exists, and it has stuff in i
            if (specifiedFile.exists() && specifiedFile.length() > 0) {

                // Load the file into the MapMemento Object
                mapMemento = objectMapper.readValue(specifiedFile, MapMemento.class);
                LOGGER.info("Loaded map from file " + specifiedFile.toString());

            } else {

                //This will be caught by starterMap and will cause the map to be loaded with hardcoded code
                throw new DefaultFileDoesNotExistException();

            }
        } catch (IOException e) {

            e.printStackTrace();

        } catch (URISyntaxException e) {

            e.printStackTrace();
        }

        // Return a map version of the mapMemento, ceated by the loadStateFromMemento method.
        return loadStateFromMemento(mapMemento);
    }

    /**
     * This method creates and returns a MapMemento version of this map.
     *
     * @return MapMemento
     */
    public MapMemento saveStateToMemento() {

        return new MapMemento(this.name, this.uniqueID, this.startLocationNode, this.mapBuildings);

    }

    /**
     * This method loads the MapMemento object into the Map. Note that this loads the whole object. Everytime that the
     *   object is loaded, all the UUID's are reset because a new Map object is created, with the exception of the
     *   UUID's of the locationNodes, which were preserved so that the edge values could be loaded in without much hassle.
     *
     * @param mapMemento
     * @return
     */
    public static Map loadStateFromMemento(MapMemento mapMemento) {

        // Create a hashmap of <UUID, LocationNode> as the key value pair. This will be used to lookup what the
        //   corresponding locationNode objects are to certain UUID's
        HashMap<UUID, LocationNode> locationNodeHashMap= new HashMap<UUID, LocationNode>();

        // Create a new Map by using the stored mapMemento's name as maps name;
        Map map = new Map(mapMemento.getName());

        // Loop through the building memento arraylist in the mapMemento
        for(BuildingMemento buildingMemento : mapMemento.getBuildingMementos()) {

            // Add a building to the map
            map.addBuilding(buildingMemento.getName());

            // Get the last element in the array (which will be the element just added)
            // Set the added Building as the current building
            map.currentBuilding = map.getMapBuildings().get(map.getMapBuildings().size() - 1);

            // Loop through the floorMementos
            for(FloorMemento floorMemento : buildingMemento.getFloorMomentos()) {

                // Create a new floor based on the corresponding floorMemento
                map.addFloor(floorMemento.getFloorName(), floorMemento.getResourceFileName());

                // Get the last element in the array (which will be the element just added)
                // Set the added Floor as the current floor
                map.currentFloor = map.currentBuilding.getFloors().get(map.currentBuilding.getFloors().size() - 1);

                // For each of the locationNodeMemntosj
                for (LocationNodeMemento locationNodeMemento : floorMemento.getLocationNodeMomentos()) {

                    // Reload the ImageType enum using the value of to convert the string to enum type
                    ImageType imageType = ImageType.valueOf(locationNodeMemento.getAssociatedImageString());

                    // Createa new loaction with a specified UUID. and save it to locationNode
                    LocationNode locationNode = new LocationNode(locationNodeMemento.getName(), locationNodeMemento.getUniqueID(), locationNodeMemento.getLocation(), map.getCurrentFloor(), imageType);

                    // For each of the destinationMementos in the locationNodeMemento
                    for(DestinationMemento destinationMemento : locationNodeMemento.getDestinationMementos()) {

                        // Reload the DestinationType enum using the value of to convert the string to enum type
                        DestinationType destinationType = DestinationType.valueOf(destinationMemento.getDestinationTypeString());

                        // Add the destination to the object
                        locationNode.addDestination(destinationMemento.getName(), destinationType);

                    }


                    // Now that the locationNode is done loading, put it into a hashmap
                    locationNodeHashMap.put(locationNode.getUniqueID(), locationNode);

                    // Link the locationNode with it's corresponding locationNodeMemento, used later for edges
                    locationNodeMemento.setAssociatedLocationNode(locationNode);

                    // Add the locationNode to the currentFloor
                    map.getCurrentFloor().addLocationNode(locationNode);

                }

            }

        }


        // At this point all the location nodes have been added, so we can start adding the edges

        // Since the startLocation node has already been added, use the hashmap to retrieve the respective locationNode.
        map.setStartLocationNode(locationNodeHashMap.get(mapMemento.getStartLocationNodeID()));


        //Loop through the existing Building, floor, then locationNode
        for(BuildingMemento buildingMemento : mapMemento.getBuildingMementos()) {

            // TODO check if the currentBuilding is actually changing or not, since map already has all it's buildings.
            map.setCurrentBuilding(map.getMapBuildings().get(map.getMapBuildings().size() - 1));

            for(FloorMemento floorMemento : buildingMemento.getFloorMomentos()) {

                map.currentFloor = map.currentBuilding.getFloors().get(map.currentBuilding.getFloors().size() - 1);

                for(LocationNodeMemento locationNodeMemento : floorMemento.getLocationNodeMomentos()) {

                    // Get the associatedLocatioNode with the current locationNodeMemento
                    LocationNode associatedLocationNode = locationNodeMemento.getAssociatedLocationNode();

                    // For each of the edges in the memento
                    for(LocationNodeEdgeMemento locationNodeEdgeMemento : locationNodeMemento.getEdgeMomentos()) {

                        //Store the endpoint locatoinNodes of each of the edges.
                        LocationNode locationNode1 = locationNodeHashMap.get(locationNodeEdgeMemento.getLocationNode1ID());
                        LocationNode locationNode2 = locationNodeHashMap.get(locationNodeEdgeMemento.getLocationNode2ID());


                        try {

                            //If the assiocatedLocationNode does not equal locationNode1
                            if(!associatedLocationNode.equals(locationNode1)) {

                                // Add the edge to locationNode 1
                                try {

                                    // Prevent EdgeAlreadyExistsException from being thrown on load
                                    if (associatedLocationNode.getEdgeBetween(locationNode1) == null) {

                                        associatedLocationNode.addEdge(locationNode1);

                                    }

                                } catch (EdgeAlreadyExistsException e) {

                                    e.printStackTrace();

                                }

                            }
                            //If the assiciatedLocationNode does not equal locationNode2
                            else if (!associatedLocationNode.equals(locationNode2)) {

                                // Add the edge to locationNode 2
                                try {

                                    // Prevent EdgeAlreadyExistsException from being thrown on load
                                    if (associatedLocationNode.getEdgeBetween(locationNode2) == null) {

                                        associatedLocationNode.addEdge(locationNode2);

                                    }


                                } catch (EdgeAlreadyExistsException e) {

                                    e.printStackTrace();
                                }

                            }

                        } catch (NodeDoesNotExistException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return map;

    }


    //||\\ Getters And Setters //||\\


    /**
     * Reinitialize null fields in Map object and subclass objects after loading from file
     */
    //TODO do we still need this? If not, refactor
    public Map initMapComponents() {

        return this;
    }

    public UUID getUniqueID() {

        return uniqueID;
    }

    public String getName() {

        return name;
    }

    public ArrayList<Building> getMapBuildings() {

        return mapBuildings;
    }

    public LocationNode getCurrentLocationNode() {

        return currentLocationNode;
    }

    public Floor getCurrentFloor() {

        return currentFloor;
    }

    public Building getCurrentBuilding() {

        return currentBuilding;
    }

    public ObservableList<LocationNode> getCurrentAdjacentLocationNodes() {

        return currentAdjacentLocationNodes;
    }

    public ObservableList<Destination> getCurrentLocationNodeDestinations() {

        return currentLocationNodeDestinations;
    }

    public ObservableList<LocationNode> getCurrentFloorLocationNodes() {

        return currentFloorLocationNodes;
    }

    public ObservableList<Destination> getCurrentFloorDestinations() {

        return currentFloorDestinations;
    }

    public Pane getCurrentFloorLocationNodePane() {

        return currentFloorLocationNodePane;
    }

    public Pane getCurrentFloorEdgePane() {

        return currentFloorEdgePane;
    }

    public ImageView getCurrentFloorImage() {

        return currentFloorImage;
    }

    public ObservableList<Floor> getCurrentBuildingFloors() {

        return currentBuildingFloors;
    }

    public ObservableList<Destination> getCurrentBuildingDestinations() {

        return currentBuildingDestinations;
    }

    public void setStartLocationNode(LocationNode locationNode) {

        this.startLocationNode = locationNode;

    }

    public void setCurrentDestination(Destination destination) {

        // TODO possibly refresh the observable lists
        // TODO possible highlight the current LocationNode

//        LOGGER.info("set current destination:" + destination.toString());
        this.destinationChangeUpdater(destination);
        this.currentDestination = destination;
        this.locationNodeUpdater(currentDestination.getCurrentLocationNode());
        this.currentLocationNode = currentDestination.getCurrentLocationNode();

        this.floorChangeUpdater(currentLocationNode.getCurrentFloor());
        this.currentFloor = currentLocationNode.getCurrentFloor();
        this.buildingChangeUpdater(currentFloor.getCurrentBuilding());
        this.currentBuilding = currentFloor.getCurrentBuilding();

        this.destinationChangeUpdater(destination);

    }

    private void destinationChangeUpdater(Destination newDestination) {

        if (currentMapState.equals(MapState.NORMAL)) {

            return;
        }

        if (newDestination == null) {

            return;
        }

//        if (f) {
//
//            this.currentBuildingDestinations.remove(newDestination);
//            this.currentLocationNodeDestinations.add(newDestination);
//
//            this.currentFloorDestinations.remove(newDestination);
//            this.currentFloorDestinations.add(newDestination);
//
//            this.currentLocationNodeDestinations.remove(newDestination);
//            this.currentLocationNodeDestinations.add(newDestination);
//
//        }

    }

    public void setCurrentLocationNode(LocationNode locationNode) {

        // TODO possibly refresh the observable lists
        // TODO possible highlight the current LocationNode

        this.currentDestination = null;
        locationNodeUpdater(locationNode);
        this.currentLocationNode = locationNode;

        if (currentLocationNode == null) {

            return;
        }

        floorChangeUpdater(currentLocationNode.getCurrentFloor());
        this.currentFloor = currentLocationNode.getCurrentFloor();
        this.buildingChangeUpdater(currentFloor.getCurrentBuilding());
        this.currentBuilding = currentFloor.getCurrentBuilding();

    }

    private void locationNodeUpdater(LocationNode newLocationNode) {

        if (currentMapState.equals(MapState.NORMAL)) {

            return;

        }

        if (newLocationNode == null) {

            return;
        }

        if ((this.currentLocationNode == null) || (!this.currentLocationNode.equals(newLocationNode))) {

            LOGGER.info("Rebuilding current location node destinations and connected location nodes");

            newLocationNode.adminDrawCurrent();

            if (this.currentLocationNode != null) {

                this.currentLocationNode.adminUndrawCurrent();

            }



            this.currentLocationNodeDestinations.clear();
            this.currentLocationNodeDestinations.addAll(newLocationNode.getDestinations());

            this.currentAdjacentLocationNodes.clear();
            this.currentAdjacentLocationNodes.addAll(newLocationNode.getAdjacentLocationNodes());

            newLocationNode.drawAdmin(this.currentFloorLocationNodePane);
            newLocationNode.drawEdgesAdmin(this.currentFloorEdgePane);

        }

    }

    public void setCurrentFloor(Floor floor) {

        // TODO possibly refresh the observable lists
        // TODO possible highlight the current LocationNode

        this.floorChangeUpdater(floor);

        this.currentDestination = null;
        this.currentLocationNode = null;
        this.currentFloor = floor;
        this.buildingChangeUpdater(currentFloor.getCurrentBuilding());
        this.currentBuilding = currentFloor.getCurrentBuilding();

    }

    private void floorChangeUpdater(Floor newFloor) {

        if (currentMapState.equals(MapState.NORMAL)) {

            return;
        }

        if ((this.currentFloor == null) || (!this.currentFloor.equals(newFloor))) {

            LOGGER.info("Rebuilding current floor destinations and location nodes");

            this.currentFloorDestinations.clear();
            this.currentFloorDestinations.addAll(newFloor.getFloorDestinations());

            this.currentFloorLocationNodes.clear();
            this.currentFloorLocationNodes.addAll(newFloor.getLocationNodes());

            newFloor.drawFloorAdmin(this.currentFloorImage, this.currentFloorLocationNodePane, this.currentFloorEdgePane);

        }

    }

    public void getXmax(){
        currentPath.getxMax();
    }
    public void getXmin(){
        currentPath.getxMin();
    }
    public void getYmax(){
        currentPath.getyMax();
    }
    public void getYmin(){
        currentPath.getyMin();
    }
    public double getXAverage(){

       return  currentPath.getxAverage();
    }
    public double getYAverage(){

       return  currentPath.getYAverage();
    }

    public void setCurrentBuilding(Building building) {

        // TODO possibly refresh the observable lists
        // TODO possible highlight the current LocationNode

        this.currentDestination = null;
        this.currentLocationNode = null;
        this.currentFloor = null;
        this.buildingChangeUpdater(building);
        this.currentBuilding = building;

    }

    private void buildingChangeUpdater(Building newBuilding) {

        if (currentMapState.equals(MapState.NORMAL)) {

            return;
        }

        if ((this.currentBuilding == null) || (!this.currentBuilding.equals(newBuilding))) {


            this.currentBuildingDestinations.clear();
            this.currentBuildingDestinations.addAll(newBuilding.getBuildingDestinations());

            this.currentBuildingFloors.clear();
            this.currentBuildingFloors.addAll(newBuilding.getFloors());

        }


    }

    public void setCurrentMapState(MapState currentMapState) {

        this.currentMapState = currentMapState;

    }

    public MapState getCurrentMapState() {

        return currentMapState;
    }

    public ObservableList<Destination> getDirectoryList() {

        return directoryList;
    }

    public ArrayList<UUID> getBuildingIdList() {
        return buildingIdList;
    }


    public void setCurrentAdjacentNode(LocationNode currentAdjacentNode) {

        this.currentAdjacentNode = currentAdjacentNode;
    }

    public void setCurrentLocationNodeEdge(LocationNode currentAdjacentNode) {

        this.currentLocationNodeEdge = this.currentLocationNode.getEdgeBetween(currentAdjacentNode);
    }

    public LocationNode getStartLocationNode() {

        return this.startLocationNode;

    }

    public ObservableList<LocationNode> getCurrentKioskLocationNodes() {

        return currentKioskLocationNodes;
    }

    public Path getCurrentPath() {
        return currentPath;
    }

    public Destination getCurrentDestination() {

        return currentDestination;
    }
}