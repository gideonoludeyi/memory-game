module com.game.memorygame {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.game.memorygame to javafx.fxml;
    exports com.game.memorygame;
}