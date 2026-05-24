module ir.ac.kashanu.jbattle {
    requires javafx.controls;
    requires javafx.fxml;

    exports ir.ac.kashanu.jbattle;
    // Lets JavaFX reflectively instantiate the Application subclass.
    opens ir.ac.kashanu.jbattle to javafx.graphics;
    // Lets FXMLLoader instantiate controllers and inject their @FXML members.
    opens ir.ac.kashanu.jbattle.ui to javafx.fxml;
}
