module com.example.hello {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires transitive javafx.graphics;
    requires jeromq;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.util;

    opens com.example.hello to javafx.fxml;
    exports com.example.hello;
}