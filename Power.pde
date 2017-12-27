public class Power{
    private float x;
    private float y;
    private int type;
    private float size;
    private boolean increase;
    private color c;
    private Platform midPlat;

    public Power(Platform mid){
        midPlat = mid;
        increase = true;
        size = 20;
        c = color(200, 0, 0);
        x = midPlat.getX() + (midPlat.getW()/2);
        y = midPlat.getY() - (midPlat.getH());
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getSize(){
        return size;
    }

    public void setColor(color tempC){
        c = tempC;
    }

    public void show(){
        pushMatrix();
        x = midPlat.getX() + (midPlat.getW()/2);
        y = midPlat.getY() - (midPlat.getH());
        if (increase)
            size += 0.3;
        else
            size -= 0.3;
        if (size > 40 || size < 20)
            increase = !increase;

        fill(c);
        ellipse(x, y, size, size);
        popMatrix();
    }
}

class PowerSpeed extends Power{
    public PowerSpeed(Platform mid){
        super(mid);
    }


}
