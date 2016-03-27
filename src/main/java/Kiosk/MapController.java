package Kiosk;

import AStar.Location;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Circle;
import org.graphstream.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Created by Matt on 3/26/2016.
 */
public class MapController implements Initializable {

    final Logger logger = LoggerFactory.getLogger(MapController.class);

    private Circle startClicked;
    private Circle lastClicked;

    @FXML
    private AnchorPane anchor;

    @FXML
    private ImageView mapImage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        mapImage.setFitWidth(Main.primaryStage.getWidth());
        mapImage.setFitHeight(Main.primaryStage.getHeight());

        Image image = new Image(getClass().getResource("f11.png").toString());
        mapImage.setImage(image);
        anchor.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                mapClicked(event.getX(), event.getY());
            }
        });

//        anchor.addEventHandler(MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent event) {
//                mapClicked(event.getX(), event.getY());
//
//                try {
//                    Main.map.saveGraph();
//                } catch (IOException e) {
//                    logger.error("Could not Save Graph", e);
//                }
//
//            }
//        });


        ArrayList<Node>  nodes =  Main.map.getNodes();

        for (Node n : nodes) {

            mapClicked(1.0 * n.getAttribute("x", Integer.class), 1.0 * n.getAttribute("y", Integer.class));
        }

    }

    private void DrawMap(GraphicsContext graphicsContext) {

//        mapCanvas.setWidth(Main.primaryStage.getWidth());
//        mapCanvas.setHeight(Main.primaryStage.getHeight());


        double canvasWidth = graphicsContext.getCanvas().getWidth();
        double canvasHeight = graphicsContext.getCanvas().getHeight();

        Image image = new Image(getClass().getResource("f11.png").toString());

        graphicsContext.drawImage(image, 0, 0, canvasWidth, canvasHeight);
    }

    private void dragged() {

        assert startClicked != null;
        assert lastClicked != null;

        Location l1 = new Location((int) Math.round(startClicked.getCenterX()), (int) Math.round(startClicked.getCenterY()));
        Location l2 = new Location((int) Math.round(lastClicked.getCenterX()), (int) Math.round(lastClicked.getCenterY()));

        logger.debug("l1 =  x: {} and y: {}", startClicked.getCenterX(), startClicked.getCenterY());
        logger.debug("l2 =  x: {} and y: {}", lastClicked.getCenterX(), lastClicked.getCenterY());



        Main.map.addEdge(l1, l2);

        try {
            Main.map.saveGraph();
        } catch (IOException e) {
            logger.error("Could not Save Graph", e);
        }

        startClicked = null;
        lastClicked = null;

    }

    private void mapClicked(double x, double y) {

        logger.debug("The map was clicked at the position x: {} and y: {}", x, y);

        Circle newNode = new Circle(x, y, 5);

        anchor.getChildren().add(newNode);

        Location location = new Location((int) Math.round(x), ((int) Math.round(y)));

        Main.map.addLocation(location);

    }

}
