module ir.ac.kashanu.jbattle {
    requires javafx.controls;

    exports ir.ac.kashanu.jbattle;
    // Lets JavaFX reflectively instantiate the Application subclass.
    opens ir.ac.kashanu.jbattle to javafx.graphics;
}
