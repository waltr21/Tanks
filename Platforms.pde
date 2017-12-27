public class Platforms{
    private ArrayList<Platform> plats = new ArrayList<Platform>();

    public ArrayList<Platform> getPlats(){
        return plats;
    }

    public Platforms(){
        int midHeight = 25;
        int midWidth = 70;
        plats.add(new Platform(width/2 - midWidth/2, height/2, midWidth, midHeight, true));
        plats.add(new Platform(0, height/2 - 200, 200, 25, false));
        plats.add(new Platform(0, height/2 + 200, 200, 25, false));
        plats.add(new Platform(width - 200, height/2 + 200, 200, 25, false));
        plats.add(new Platform(width - 200, height/2 - 200, 200, 25, false));
    }

    public void showPlatforms(){
        for (Platform p : plats){
            p.show();
        }
    }
}

class Platform{
    private float x, y;
    private int w, h, speed;
    private boolean moving, right;

    public Platform(float tempX, float tempY, int tempW, int tempH, boolean move){
        x = tempX;
        y = tempY;
        w = tempW;
        h = tempH;
        moving = move;
        right = true;
        speed = 2;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public int getW(){
        return w;
    }

    public int getH(){
        return h;
    }

    public void setX(float tempX){
        x = tempX;
    }

    public void show(){
        pushMatrix();
        fill(0);
        if (moving){
            if (right)
                x += speed;
            else
                x -= speed;
            if (x < 0 || x + w > width)
                right = !right;
        }
        rect(x, y, w, h);
        popMatrix();
    }
}
