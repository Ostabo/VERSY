package aqua.blatt1.client;

import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;
import aqua.blatt7.endpoint.SecureEndpoint;
import messaging.Endpoint;
import messaging.Message;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class ClientCommunicator {
    private final Endpoint endpoint;

    public ClientCommunicator() {
        endpoint = new SecureEndpoint();
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

        public void deregister(String id, boolean hadToken) {
            endpoint.send(broker, new DeregisterRequest(id, hadToken));
        }

        public void handOff(FishModel fish, InetSocketAddress receiver) {
            endpoint.send(receiver, new HandoffRequest(fish));
        }

        public void handOffToken(InetSocketAddress receiver) {
            endpoint.send(receiver, new Token());
        }

        public void sendSnapshotMarker(InetSocketAddress... receivers) {
            for (InetSocketAddress receiver : receivers) {
                endpoint.send(receiver, new SnapshotMarker());
            }
        }

        public void sendSnapshotResult(Set<FishModel> snapshot, InetSocketAddress receiver) {
            endpoint.send(receiver, new SnapshotResult(new HashSet<>(snapshot)));
        }

        public void sendLocationRequest(String fishId, InetSocketAddress receiver) {
            endpoint.send(receiver, new LocationRequest(fishId));
        }

        public void sendNameResolutionRequest(String tankId, String fishId) {
            endpoint.send(broker, new NameResolutionRequest(tankId, fishId));
        }

        public void sendLocationUpdate(InetSocketAddress address, String reqId) {
            endpoint.send(address, new LocationUpdate(reqId));
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
                            ((RegisterResponse) msg.getPayload()).neighborUpdate().rightNeighbor(),
                            ((RegisterResponse) msg.getPayload()).leaseDuration());

                if (msg.getPayload() instanceof HandoffRequest)
                    tankModel.receiveFish(((HandoffRequest) msg.getPayload()).fish());

                if (msg.getPayload() instanceof NeighborUpdate)
                    tankModel.updateNeighbors(((NeighborUpdate) msg.getPayload()).leftNeighbor(),
                            ((NeighborUpdate) msg.getPayload()).rightNeighbor());

                if (msg.getPayload() instanceof Token)
                    tankModel.receiveToken();

                if (msg.getPayload() instanceof SnapshotMarker)
                    tankModel.receiveSnapshotMarker(msg.getSender());

                if (msg.getPayload() instanceof SnapshotResult)
                    tankModel.receiveSnapshotResult(((SnapshotResult) msg.getPayload()).snapshotResult());

                if (msg.getPayload() instanceof LocationRequest)
                    tankModel.locateFishLocally(((LocationRequest) msg.getPayload()).fishId());

                if (msg.getPayload() instanceof NameResolutionResponse)
                    tankModel.receiveNameResolutionResponse((NameResolutionResponse) msg.getPayload());

                if (msg.getPayload() instanceof LocationUpdate)
                    tankModel.receiveLocationUpdate(msg.getSender(), ((LocationUpdate) msg.getPayload()).reqId());
            }
            System.out.println("Receiver stopped.");
        }
    }

}
