package com.game.memorygame;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

class Card extends Region {
    private enum CardState {
        ACTIVE,
        INACTIVE,
        DISABLED
    }

    private final ObjectProperty<CardState> stateProperty = new SimpleObjectProperty<>(CardState.INACTIVE);
    private final Color inactive = Color.ANTIQUEWHITE;
    private final Color disabled = Color.GRAY;
    private final Color active;

    Card(Color active) {
        this.active = active;

        ObjectBinding<Background> backgroundBinding = Bindings.createObjectBinding(
                () -> colorToBackground(switch (stateProperty.get()) {
                    case ACTIVE -> active;
                    case INACTIVE -> inactive;
                    case DISABLED -> disabled;
                }),
                stateProperty);

        backgroundProperty().bind(backgroundBinding);
    }

    private Background colorToBackground(Color color) {
        return new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY));
    }

    public Color getActiveColor() {
        return active;
    }

    private CardState getState() {
        return stateProperty.get();
    }

    public void setState(CardState cardState) {
        stateProperty.set(cardState);
    }

    public void activate() {
        setState(CardState.ACTIVE);
    }

    public void deactivate() {
        setState(CardState.INACTIVE);
    }

    public void disable() {
        setState(CardState.DISABLED);
    }

    public boolean isDisabledState() {
        return getState() == CardState.DISABLED;
    }
}

class CardSelectedEvent extends Event {
    public static final EventType<? extends CardSelectedEvent> EVENT_TYPE = new EventType<>(Event.ANY, "CARD_SELECTED");
    private final Card selectedCard;

    public CardSelectedEvent(Card card) {
        super(EVENT_TYPE);
        selectedCard = card;
    }

    public Card getSelectedCard() {
        return selectedCard;
    }
}



class BoardComponent extends GridPane {
    private static final int MAX_NUM_OF_SIMULTANEOUSLY_SELECTABLE_CARDS = 2;
    private static final Random RANDOM = new Random(1);

    private final int nCols;
    private final int nRows;
    private final List<Card> cards;
    private final List<Card> selectedCards;

    private final PauseTransition pauseTransition = new PauseTransition(Duration.seconds(1));

    BoardComponent() {
        this(6, 5);
    }

    BoardComponent(int nCols, int nRows) {
        super();
        this.nCols = nCols;
        this.nRows = nRows;
        this.cards = new ArrayList<>(nCols * nRows);
        this.selectedCards = new ArrayList<>(MAX_NUM_OF_SIMULTANEOUSLY_SELECTABLE_CARDS);

        setStyle("-fx-background-color: blue;");
        applyConstraints();
        setup();
        startGame();
    }

    public void startGame() {
        enableCardInteractions();
    }

    private void setup() {
        int nCards = nCols * nRows;
        int nDistinctColors = nCards / MAX_NUM_OF_SIMULTANEOUSLY_SELECTABLE_CARDS;
        List<Color> colors = generateRandomColors(nDistinctColors, MAX_NUM_OF_SIMULTANEOUSLY_SELECTABLE_CARDS);
        Iterator<Color> colorIterator = colors.iterator();

        for (int col = 0; col < nCols; col++) {
            for (int row = 0; row < nRows; row++) {
                Color color = colorIterator.next();
                Card card = new Card(color);
                setCard(card, col, row);
            }
        }

        addEventHandler(CardSelectedEvent.EVENT_TYPE, event -> {
            Card selectedCard = event.getSelectedCard();
            if (selectedCard.isDisabledState()) return;
            if (selectedCards.contains(selectedCard)) return;

            selectedCard.activate();
            selectedCards.add(selectedCard);
            if (selectedCards.size() < MAX_NUM_OF_SIMULTANEOUSLY_SELECTABLE_CARDS) return;

            disableCardInteractions();

            pauseTransition.setOnFinished(actionEvent -> {
                boolean isMatch = evaluateCardSelections(selectedCards);
                if (isMatch) {
                    selectedCards.forEach(Card::disable);
                } else {
                    selectedCards.forEach(Card::deactivate);
                }
                selectedCards.clear();
                enableCardInteractions();
            });

            pauseTransition.playFromStart();
        });
    }

    private void enableCardInteractions() {
        for (Card card : cards) {
            card.setOnMouseClicked(mouseEvent -> {
                fireEvent(new CardSelectedEvent(card));
            });
        }
    }

    private void disableCardInteractions() {
        for (Card card : cards) {
            card.setOnMouseClicked(null);
        }
    }

    /**
     * Generates a shuffled list of nDistinctColors distinct colors which are duplicated repeat times
     * @param nDistinctColors number of distinct colors to generate
     * @param repeat numbers of duplicates to include for each generated color
     * @return a shuffled list of colors
     */
    private static List<Color> generateRandomColors(int nDistinctColors, int repeat) {
        int size = nDistinctColors * repeat;
        List<Color> colors = new ArrayList<>(size);
        for (int i = 0; i < nDistinctColors; i++) {
            Color color = new Color(
                    (RANDOM.nextDouble() / 2f) + 0.375, // not too bright and not too dark
                    (RANDOM.nextDouble() / 2f) + 0.375,
                    (RANDOM.nextDouble() / 2f) + 0.375,
                    1
            );
            for (int j = 0; j < repeat; j++) {
                colors.add(color);
            }
        }
        Collections.shuffle(colors, RANDOM);
        return colors;
    }

    private boolean evaluateCardSelections(Collection<Card> cards) {
        return cards.stream().map(Card::getActiveColor).distinct().count() == 1;
    }

    private void applyConstraints() {
        ObservableList<RowConstraints> rowConstraints = getRowConstraints();
        for (int i = 0; i < nRows; i++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setPercentHeight(100.0 / nRows);
            rowConstraints.add(constraints);
        }

        ObservableList<ColumnConstraints> columnConstraints = getColumnConstraints();
        for (int i = 0; i < nCols; i++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0/ nCols);
            columnConstraints.add(constraints);
        }
    }

    private void setCard(Card card, int col, int row) {
        int index = (col * nRows) + row;
        cards.add(index, card);

        AnchorPane.setTopAnchor(card, 4.0);
        AnchorPane.setBottomAnchor(card, 4.0);
        AnchorPane.setLeftAnchor(card, 4.0);
        AnchorPane.setRightAnchor(card, 4.0);
        AnchorPane ap = new AnchorPane(card);
        add(ap, col, row);
    }
}

public class Main extends Application {
    private Parent createContent() {
        BoardComponent board = new BoardComponent();

        Button restart = new Button("New Game");
        restart.setOnMouseClicked(mouseEvent -> System.out.println("NOT YET IMPLEMENTED"));

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(board);

        HBox top = new HBox();
        top.setMinHeight(50);
        HBox bottom = new HBox(restart);
        bottom.setMinHeight(50);
        VBox left = new VBox();
        left.setMinWidth(50);
        VBox right = new VBox();
        right.setMinWidth(50);

        borderPane.setTop(top);
        borderPane.setBottom(bottom);
        borderPane.setLeft(left);
        borderPane.setRight(right);

        return borderPane;
    }

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(createContent(), 800, 640);
        stage.setTitle("Memory Game");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}