public class HealthBar{
    private int size;
    private int w;
    private int incr;

    public HealthBar(int h){
        incr = 40;
        size = h * incr;
        w = 20;

    }

    public void decreaseSize(){
        size -= incr;
    }

    public void show(){
        pushMatrix();
        fill(0,256,0);
        rect(10, 10, size, w);
        popMatrix();
    }
}
