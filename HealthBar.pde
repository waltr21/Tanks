public class HealthBar{
    private int size;
    private int w;
    private int incr;

    public HealthBar(int h){
        incr = 20;
        size = h * incr;
        w = 20;

    }

    public void decreaseSize(){
        size -= incr;
        if (size < 0)
            size = 0;
    }

    public void show(){
        pushMatrix();
        //if (size/incr > 100)
        fill(0,200,0);
        rect(10, 10, size, w);
        popMatrix();
    }
}
