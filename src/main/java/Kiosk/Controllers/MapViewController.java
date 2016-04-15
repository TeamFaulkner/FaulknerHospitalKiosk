package Kiosk.Controllers;

import Map.Building;
import Map.Floor;
import Map.LocationNode;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import Kiosk.KioskApp;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class MapViewController {

    // Reference to the main application.

    private boolean okClicked = false;
    private KioskApp kioskApp;
    private Building building;
    private LocationNode startNode;
    private LocationNode destinationNode;
    private int numThreads = 0;

    private static final Logger LOGGER = LoggerFactory.getLogger(MapViewController.class);
    @FXML
    private TextField searchTextBox;
    @FXML
    private StackPane imageStackPane;

    @FXML
    private Button confirmButton;

    @FXML
    private ScrollPane scrollPane;

    Timer timer;
    Timer atimer;

    int counter = 0;
    private volatile boolean running = true;

    TimerTask timerTask = new TimerTask() {

        @Override
        public void run() {
            counter++;
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            while (running) {
                try {
                    System.out.println(counter + " seconds have passed.");
                    if (counter == 60) {
                        System.out.println("Timed Out.");
                        running = false;
                        timer.cancel();
                        timer.purge();
                        atimer.cancel();
                        atimer.purge();
                        timerTask.cancel();
                        Platform.runLater(resetKiosk);
                        break;
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException exception) {
                    System.out.println("I'm outta here");
                    atimer.cancel();
                    timer.cancel();
                    timerTask.cancel();
                    running = false;
                    exception.printStackTrace();
                    break;
                }

            }
        }
    };

    Thread timerThread;

    Runnable resetKiosk = new Runnable() {

        @Override
        public void run() {
            handleBack();
        }
    };


    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {

        confirmButton.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {

                counter = 0;
                building.drawShortestPath(startNode, destinationNode);

            }

        });


        searchTextBox.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {

                if (event.getCode().equals(KeyCode.ENTER)) {

                    timer.cancel();
                    running = false;
                    timerThread.interrupt();
                    LOGGER.info("Blah " + searchTextBox.getText());
                    kioskApp.showSearch(searchTextBox.getText());

                } else {

                    counter = 0;

                }
            }
        });

        scrollPane.setOnMouseMoved(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                if(numThreads == 0) {
                    numThreads +=1;
                    running = true;
                    timer = new Timer("A Timer");
                    atimer = new Timer("A Timer2");
                    timerThread = new Thread(runnable);
                    timer.scheduleAtFixedRate(timerTask, 30, 1000);
                    timerThread.start();
                }
                counter = 0;
            }
        });


        //timer.scheduleAtFixedRate(timerTask, 30, 1000);

        //timerThread.start();


    }

    /**
     * Is called by the main application to give a reference back to itself.
     *
     * @param kioskApp
     */
    public void setKioskApp(KioskApp kioskApp) {
        this.kioskApp = kioskApp;
    }

    /**
     * Returns true if the user clicked OK, false otherwise.
     *
     * @return
     */
    public boolean isOkClicked() {
        return okClicked;
    }


    /**
     * Called when the user clicks back.
     */
    // TODO: handleBack should have an if statement, which will go to
    // either userUI2 or userUI3 depending on which screen userUI4 was
    // accessed from
    @FXML
    private void handleBack() {

        handleCancel();
    }

    /**
     * Called when the user clicks cancel.
     */
    @FXML
    private void handleCancel() {

        atimer.cancel();
        atimer.purge();
        timer.cancel();
        timer.purge();
        running = false;
        timerThread.interrupt();
        kioskApp.reset();
    }


    public void setBuilding(Building building) {
        this.building = building;
    }

    public void setDestinationNode(LocationNode destinationNode){

/*      atimer.cancel();
        atimer.purge();
        timer.cancel();
        timer.purge();*/
        running = false;
        //timerThread.interrupt();
        this.destinationNode = destinationNode;
        destinationNode.getNodeFloor().drawFloorNormal(this.imageStackPane);
    }

    public void setStartNode(LocationNode startNode) {
        this.startNode = startNode;
    }

}