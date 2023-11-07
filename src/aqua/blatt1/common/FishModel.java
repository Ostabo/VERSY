package aqua.blatt1.common;

import aqua.blatt1.client.TankModel;

import java.io.Serializable;
import java.util.Random;

public final class FishModel implements Serializable {
    private final static int xSize = 100;
    private final static int ySize = 50;
    private final static Random rand = new Random();

    private final String id;
    private int x;
    private int y;
    private Direction direction;

    private boolean toggled;

    public FishModel(String id, int x, int y, Direction direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    public static int getXSize() {
        return xSize;
    }

    public static int getYSize() {
        return ySize;
    }

    public String getId() {
        return id;
    }

    public String getTankId() {
        return id.substring(id.indexOf("@") + 1);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Direction getDirection() {
        return direction;
    }

    public void reverse() {
        direction = direction.reverse();
    }

    public void toggle() {
        toggled = !toggled;
    }

    public boolean isToggled() {
        return toggled;
    }

    public boolean hitsEdge() {
        return (direction == Direction.LEFT && x == 0)
                || (direction == Direction.RIGHT && x == TankModel.WIDTH - xSize);
    }

    public boolean disappears() {
        return (direction == Direction.LEFT && x == -xSize)
                || (direction == Direction.RIGHT && x == TankModel.WIDTH);
    }

    public void update() {
        x += direction.getVector();

        double discreteSin = Math.round(Math.sin(x / 30.0));
        discreteSin = rand.nextInt(10) < 8 ? 0 : discreteSin;
        y += (int) discreteSin;
        y = y < 0 ? 0 : Math.min(y, TankModel.HEIGHT - FishModel.getYSize());
    }

    public void setToStart() {
        x = direction == Direction.LEFT ? TankModel.WIDTH : -xSize;
    }

    public boolean isDeparting() {
        return (direction == Direction.LEFT && x < 0)
                || (direction == Direction.RIGHT && x > TankModel.WIDTH - xSize);
    }

    @Override
    public String toString() {
        return id;
    }
}
