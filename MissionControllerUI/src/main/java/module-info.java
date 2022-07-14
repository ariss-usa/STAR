module com.example.hello {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires jeromq;

    opens com.example.hello to javafx.fxml;
    exports com.example.hello;
}