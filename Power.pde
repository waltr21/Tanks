public class Power{
    private float x;
    private float y;
    private int type;
    private float size;
    private boolean increase;
    private color c;
    private Platform midPlat;
    private Tank p;
    private HealthBar h;
    private Bullet b;

    public Power(Platform mid, Tank p, HealthBar h){
        midPlat = mid;
        this.p = p;
        this.h = h;
        this.b = b;
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

    public int getType(){
        return type;
    }

    private float getSize(){
        return size;
    }

    private Tank getTank(){
        return p;
    }

    private HealthBar getBar(){
        return h;
    }

    private void setColor(color tempC){
        c = tempC;
    }

    private void setType(int t){
        type = t;
    }

    public void usePower(){

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

class PowerHealth extends Power{
    public PowerHealth(Platform mid, Tank p, HealthBar h){
        super(mid, p, h);
        color healthColor = color(200, 0, 0);
        super.setColor(healthColor);
        super.setType(0);
    }

    public void usePower(){
        int tempHealth = super.getTank().getHealth() + 3;
        super.getTank().setHealth(tempHealth);
        super.getBar().increaseSize(3);
    }
}

class PowerShot extends Power{
    public PowerShot(Platform mid, Tank p, HealthBar h){
        super(mid, p, h);
        color shotColor = color(255, 145, 12);
        super.setColor(shotColor);
        super.setType(1);
    }

    public void usePower(){
        //System.out.println("Power used");
        super.getTank().setFastBullet(true);
    }
}
