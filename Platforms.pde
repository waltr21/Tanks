public class Platforms{
    private ArrayList<Platform> plats = new ArrayList<Platform>();
    private boolean right;

    public ArrayList<Platform> getPlats(){
        return plats;
    }

    public Platforms(){
        int midHeight = 25;
        int midWidth = 70;
        right = true;
        plats.add(new Platform(width/2 - midWidth/2, height/2, midWidth, midHeight));
        plats.add(new Platform(0, height/2 - 200, 200, 25));
        plats.add(new Platform(0, height/2 + 200, 200, 25));
        plats.add(new Platform(width - 200, height/2 + 200, 200, 25));
        plats.add(new Platform(width - 200, height/2 - 200, 200, 25));


    }

    public void showPlatforms(){

        if (right)
            plats.get(0).setX(plats.get(0).getX() + 2);

        else
            plats.get(0).setX(plats.get(0).getX() - 2);

        if (plats.get(0).getX() < 0 || plats.get(0).getX() + plats.get(0).getW() > width)
            right = !right;
        for (Platform p : plats){
            p.show();
        }
    }
}

class Platform{
    private float x;
    private float y;
    private int w;
    private int h;

    public Platform(float tempX, float tempY, int tempW, int tempH){
        x = tempX;
        y = tempY;
        w = tempW;
        h = tempH;
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
        fill(0);
        rect(x, y, w, h);
    }
}
