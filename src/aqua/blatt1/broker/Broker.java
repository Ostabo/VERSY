package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Broker {

    private static final int PORT = 4711;
    private static final Endpoint ENDPOINT = new Endpoint(PORT);
    private static final String ID_PREFIX = "tank";

    private final ClientCollection<InetSocketAddress> clients = new ClientCollection<>();

    public static void main(String[] args) {
        new Broker().broker();
    }

    private void broker() {
        while (true) {
            final Message mes = ENDPOINT.blockingReceive();

            switch (MsgType.valueOf(mes.getPayload())) {
                case REGISTER -> register(mes.getSender());
                case DEREGISTER -> deregister(((DeregisterRequest) mes.getPayload()).getId());
                case HANDOFF -> handoffFish(mes.getSender(), (HandoffRequest) mes.getPayload());
                case UNKNOWN -> {
                    System.err.println("Unknown message type: " + mes.getPayload().getClass().getSimpleName());
                    System.exit(1);
                }
            }
        }
    }

    private void register(InetSocketAddress client) {
        final String id = ID_PREFIX + (clients.size() + 1);
        clients.add(id, client);
        ENDPOINT.send(client, new RegisterResponse(id));
    }

    private void deregister(String clientId) {
        final int index = clients.indexOf(clientId);
        if (index == -1) {
            System.err.println("Client not registered...");
            return;
        }
        clients.remove(index);
    }

    private void handoffFish(InetSocketAddress client, HandoffRequest req) {
        final int index = clients.indexOf(client);
        ENDPOINT.send(switch (req.getFish().getDirection()) {
            case LEFT -> clients.getLeftNeighorOf(index);
            case RIGHT -> clients.getRightNeighorOf(index);
        }, req);
    }

    private enum MsgType {
        DEREGISTER,
        HANDOFF,
        REGISTER,
        UNKNOWN;

        public static MsgType valueOf(Serializable classType) {
            if (classType instanceof DeregisterRequest) return DEREGISTER;
            if (classType instanceof HandoffRequest) return HANDOFF;
            if (classType instanceof RegisterRequest) return REGISTER;
            return UNKNOWN;
        }
    }

}
