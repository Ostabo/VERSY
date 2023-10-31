package aqua.blatt1.client;

import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;
import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;

public class ClientCommunicator {
    private final Endpoint endpoint;

    public ClientCommunicator() {
        endpoint = new Endpoint();
    }

    public ClientForwarder newClientForwarder() {
        return new ClientForwarder();
    }

    public ClientReceiver newClientReceiver(TankModel tankModel) {
        return new ClientReceiver(tankModel);
    }

    public class ClientForwarder {
        private final InetSocketAddress broker;

        private ClientForwarder() {
            this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
        }

        public void register() {
            endpoint.send(broker, new RegisterRequest());
        }

        public void deregister(String id) {
            endpoint.send(broker, new DeregisterRequest(id));
        }

        public void handOff(FishModel fish, InetSocketAddress receiver) {
            endpoint.send(receiver, new HandoffRequest(fish));
        }
    }

    public class ClientReceiver extends Thread {
        private final TankModel tankModel;

        private ClientReceiver(TankModel tankModel) {
            this.tankModel = tankModel;
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                Message msg = endpoint.blockingReceive();

                if (msg.getPayload() instanceof RegisterResponse)
                    tankModel.onRegistration(((RegisterResponse) msg.getPayload()).id(),
                            ((RegisterResponse) msg.getPayload()).neighborUpdate().leftNeighbor(),
                            ((RegisterResponse) msg.getPayload()).neighborUpdate().rightNeighbor());

                if (msg.getPayload() instanceof HandoffRequest)
                    tankModel.receiveFish(((HandoffRequest) msg.getPayload()).fish());

                if (msg.getPayload() instanceof NeighborUpdate)
                    tankModel.updateNeighbors(((NeighborUpdate) msg.getPayload()).leftNeighbor(),
                            ((NeighborUpdate) msg.getPayload()).rightNeighbor());
            }
            System.out.println("Receiver stopped.");
        }
    }

}
