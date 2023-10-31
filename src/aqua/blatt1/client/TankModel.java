package aqua.blatt1.client;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TankModel extends Observable implements Iterable<FishModel> {

    public static final int WIDTH = 600;
    public static final int HEIGHT = 350;
    protected static final int MAX_FISHIES = 5;
    protected static final Random rand = new Random();
    protected final Set<FishModel> fishies;
    protected final ClientCommunicator.ClientForwarder forwarder;
    protected String id;
    protected int fishCounter = 0;
    protected InetSocketAddress leftNeighbor;
    protected InetSocketAddress rightNeighbor;
    protected volatile boolean hasToken = false;
    protected Timer timer = new Timer();

    public TankModel(ClientCommunicator.ClientForwarder forwarder) {
        this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.forwarder = forwarder;
    }

    synchronized void onRegistration(String id, InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
        this.id = id;
        this.updateNeighbors(leftNeighbor, rightNeighbor);
        newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
    }

    public synchronized void newFish(int x, int y) {
        if (fishies.size() < MAX_FISHIES) {
            x = Math.min(x, WIDTH - FishModel.getXSize() - 1);
            y = Math.min(y, HEIGHT - FishModel.getYSize());

            FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
                    rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

            fishies.add(fish);
        }
    }

    synchronized void receiveFish(FishModel fish) {
        fish.setToStart();
        fishies.add(fish);
    }

    public String getId() {
        return id;
    }

    public synchronized int getFishCounter() {
        return fishCounter;
    }

    public synchronized Iterator<FishModel> iterator() {
        return fishies.iterator();
    }

    private synchronized void updateFishies() {
        for (Iterator<FishModel> it = iterator(); it.hasNext(); ) {
            FishModel fish = it.next();

            fish.update();

            if (fish.hitsEdge()) {
                if (hasToken())
                    forwarder.handOff(fish, switch (fish.getDirection()) {
                        case LEFT -> leftNeighbor;
                        case RIGHT -> rightNeighbor;
                    });
                else
                    fish.reverse();
            }

            if (fish.disappears())
                it.remove();
        }
    }

    private synchronized void update() {
        updateFishies();
        setChanged();
        notifyObservers();
    }

    protected void run() {
        forwarder.register();

        try {
            while (!Thread.currentThread().isInterrupted()) {
                update();
                TimeUnit.MILLISECONDS.sleep(10);
            }
        } catch (InterruptedException consumed) {
            // allow method to terminate
        }
    }

    public synchronized void finish() {
        forwarder.deregister(id, hasToken());
    }

    synchronized void updateNeighbors(InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
        if (leftNeighbor != null)
            this.leftNeighbor = leftNeighbor;
        if (rightNeighbor != null)
            this.rightNeighbor = rightNeighbor;
    }

    public boolean hasToken() {
        return hasToken;
    }

    public synchronized void receiveToken() {
        final int DELAY = 2000; // 2 seconds

        if (!hasToken)
            this.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    hasToken = false;
                    forwarder.handOffToken(rightNeighbor);
                }
            }, DELAY);

        this.hasToken = true;
    }
}
