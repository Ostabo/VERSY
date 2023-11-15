package aqua.blatt1.broker;

import aqua.blatt1.common.msgtypes.*;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Broker {

    private static final int THREAD_POOL_SIZE = 10;
    private static final int PORT = 4711;
    private static final Endpoint ENDPOINT = new Endpoint(PORT);
    private static final String ID_PREFIX = "tank";

    private final ClientCollection<InetSocketAddress> clients = new ClientCollection<>();
    private final ReadWriteLock clientLock = new ReentrantReadWriteLock();
    private final Map<String, InetSocketAddress> nameResolutionTable = new HashMap<>();
    private volatile boolean stopRequested = false;

    public static void main(String[] args) {
        new Broker().broker();
    }

    private void broker() {
        var executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        while (!stopRequested) {
            final Message msg = ENDPOINT.nonBlockingReceive();
            if (msg != null)
                executor.execute(new BrokerTask(msg));
        }
        executor.shutdown();
    }

    private enum MsgType {
        DEREGISTER,
        HANDOFF,
        REGISTER,
        POISON,
        NAMEREQUEST,
        UNKNOWN;

        public static MsgType valueOf(Serializable classType) {
            if (classType instanceof DeregisterRequest) return DEREGISTER;
            if (classType instanceof HandoffRequest) return HANDOFF;
            if (classType instanceof RegisterRequest) return REGISTER;
            if (classType instanceof PoisonPill) return POISON;
            if (classType instanceof NameResolutionRequest) return NAMEREQUEST;
            return UNKNOWN;
        }
    }

    public class BrokerTask implements Runnable {

        private final Message msg;

        public BrokerTask(Message msg) {
            this.msg = msg;
        }

        @Override
        public void run() {
            switch (MsgType.valueOf(msg.getPayload())) {
                case REGISTER -> register(msg.getSender());
                case DEREGISTER ->
                        deregister(((DeregisterRequest) msg.getPayload()).id(), ((DeregisterRequest) msg.getPayload()).hadToken());
                case POISON -> stopRequested = true;
                case NAMEREQUEST -> {
                    final NameResolutionRequest request = (NameResolutionRequest) msg.getPayload();
                    ENDPOINT.send(
                            msg.getSender(),
                            new NameResolutionResponse(nameResolutionTable.get(request.tankId()), request.reqId())
                    );
                }
                case UNKNOWN ->
                        System.err.println("Unknown message type: " + msg.getPayload().getClass().getSimpleName());
            }
        }

        private void register(InetSocketAddress client) {
            clientLock.readLock().lock();
            final int index = clients.size() + 1;
            clientLock.readLock().unlock();
            final String id = ID_PREFIX + index;

            clientLock.writeLock().lock();
            clients.add(id, client);
            clientLock.writeLock().unlock();

            clientLock.readLock().lock();
            final InetSocketAddress leftNeighbor = clients.getLeftNeighorOf(clients.indexOf(client));
            final InetSocketAddress rightNeighbor = clients.getRightNeighorOf(clients.indexOf(client));
            clientLock.readLock().unlock();

            ENDPOINT.send(client, new RegisterResponse(id, new NeighborUpdate(leftNeighbor, rightNeighbor)));
            nameResolutionTable.put(id, client);
            ENDPOINT.send(leftNeighbor, new NeighborUpdate(null, client));
            ENDPOINT.send(rightNeighbor, new NeighborUpdate(client, null));

            if (index == 1)
                ENDPOINT.send(client, new Token());
        }

        private void deregister(String clientId, boolean hadToken) {
            clientLock.readLock().lock();
            final int index = clients.indexOf(clientId);
            if (index == -1) {
                System.err.println("Client not registered...");
                clientLock.readLock().unlock();
                return;
            }

            final InetSocketAddress leftNeighbor = clients.getLeftNeighorOf(index);
            final InetSocketAddress rightNeighbor = clients.getRightNeighorOf(index);
            clientLock.readLock().unlock();

            clientLock.writeLock().lock();
            clients.remove(index);
            clientLock.writeLock().unlock();

            ENDPOINT.send(leftNeighbor, new NeighborUpdate(null, rightNeighbor));
            ENDPOINT.send(rightNeighbor, new NeighborUpdate(leftNeighbor, null));

            clientLock.readLock().lock();
            if (hadToken && clients.size() > 0)
                ENDPOINT.send(clients.getClient(0), new Token());
            clientLock.readLock().unlock();
        }
    }

}
