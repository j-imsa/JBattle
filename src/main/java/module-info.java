module ir.ac.kashanu.jbattle {
    requires javafx.controls;
    requires javafx.fxml;


    opens ir.ac.kashanu.jbattle to javafx.fxml;
    exports ir.ac.kashanu.jbattle;
}