package aqua.blatt2.broker;

import aqua.blatt1.common.Properties;
import aqua.blatt7.endpoint.SecureEndpoint;
import messaging.Endpoint;

import javax.swing.*;
import java.net.InetSocketAddress;

public class Poisoner {
    private final Endpoint endpoint;
    private final InetSocketAddress broker;

    public Poisoner() {
        this.endpoint = new SecureEndpoint();
        this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
    }

    public static void main(String[] args) {
        JOptionPane.showMessageDialog(null, "Press OK button to poison server.");
        new Poisoner().sendPoison();
    }

    public void sendPoison() {
        endpoint.send(broker, new PoisonPill());
    }
}
