package hk.ust.cse.comp3021.pa3.controller;

import hk.ust.cse.comp3021.pa3.model.*;
import hk.ust.cse.comp3021.pa3.util.GameStateSerializer;
import hk.ust.cse.comp3021.pa3.util.Robot;
import hk.ust.cse.comp3021.pa3.util.TimeIntervalGenerator;
import hk.ust.cse.comp3021.pa3.view.UIServices;
import hk.ust.cse.comp3021.pa3.view.panes.MainGamePane;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.testfx.api.FxRobot;
import org.testfx.api.FxService;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.framework.junit5.Stop;
import org.testfx.service.query.EmptyNodeQueryException;


public class GameGUIConcurrencyTest {
    private static GameState[] loadGame(String resourcePath) throws FileNotFoundException {
        var resource = GameGUIConcurrencyTest.class.getResource(resourcePath);
        assert resource != null;
        var path = Path.of(resource.getPath());
        return GameStateSerializer.loadFrom(path);
    }

    private static MainGamePane setup(Stage stage, String map) throws FileNotFoundException {
        Robot.timeIntervalGenerator = TimeIntervalGenerator.veryFast();
        var gameStates = loadGame(map);
        final URL styleSheet = Objects.requireNonNull(MainGamePane.class.getResource("/styles/style.css"));
        var gamePane = new MainGamePane(gameStates, null);
        gamePane.initializeComponents();
        var scene = new Scene(gamePane);
        scene.getStylesheets().add(styleSheet.toExternalForm());
        stage.setScene(scene);
        stage.show();
        return gamePane;
    }

    private static boolean isGameEnded(MainGamePane gamePane) {
        return gamePane.getGameController().getWinners() != null;
    }

    private static void assertGame(FxRobot robot, MainGamePane gamePane) throws InterruptedException {
        var robotButtons = robot.lookup(".toggle-button").queryAllAs(ToggleButton.class);
        robot.lookup(node -> node instanceof ToggleButton);
        for (var btn :
                robotButtons) {
            robot.clickOn(btn);
        }
        var hasWon = false;
        var numAlerts = 0;
        var end = false;
        while (!end) {
            if (isGameEnded(gamePane)) end = true;
            Thread.sleep(100);
            try {
                var alert = robot.lookup(".dialog-pane").queryAs(DialogPane.class);
                numAlerts++;
                if (alert.getHeaderText().equals(UIServices.WIN_ALERT_TITLE)) {
                    assertFalse(hasWon);
                    hasWon = true;
                }
                robot.clickOn(alert.lookupButton(ButtonType.OK));
            } catch (EmptyNodeQueryException ignored) {
            }
        }
        assertThat(numAlerts, Matchers.allOf(Matchers.greaterThan(0),
                Matchers.lessThanOrEqualTo(gamePane.getGameController().getGameStates().length)));
        // no more alerts should be shown
        Thread.sleep(500);
        assertThrows(EmptyNodeQueryException.class, () -> robot.lookup(".dialog-pane").queryAs(DialogPane.class));
    }


    @ExtendWith(ApplicationExtension.class)
    public static class Map04 {
        private MainGamePane gamePane;
        private Stage stage;

        @Start
        private void start(Stage stage) throws FileNotFoundException {
            String map = "/maps/04.multiplayer.game";
            this.gamePane = setup(stage, map);
            this.stage = stage;
        }

        @Test
        @Tag("gui")
        @Timeout(5)
        void runGame(FxRobot robot) throws InterruptedException {
            assertGame(robot, gamePane);
            Platform.runLater(() -> stage.close());
        }
    }

    @ExtendWith(ApplicationExtension.class)
    public static class Map05 {
        private MainGamePane gamePane;
        private Stage stage;

        @Start
        private void start(Stage stage) throws FileNotFoundException {
            String map = "/maps/05.multiplayer.game";
            gamePane = setup(stage, map);
            this.stage = stage;
        }

        @Test
        @Tag("gui")
        @Timeout(5)
        void runGame(FxRobot robot) throws InterruptedException {
            assertGame(robot, gamePane);
            Platform.runLater(() -> stage.close());
        }
    }
}
