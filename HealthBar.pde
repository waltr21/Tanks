public class HealthBar{
    private int size;
    private int w;
    private int incr;
    private int MAX_SIZE;

    public HealthBar(int h){
        incr = 20;
        size = h * incr;
        MAX_SIZE = h * incr;
        w = 20;

    }

    public void decreaseSize(){
        size -= incr;
        if (size < 0)
            size = 0;
    }

    public void increaseSize(int times){
        size += incr * times;
        if (size > MAX_SIZE)
                size = MAX_SIZE;
    }

    public void show(){
        pushMatrix();
        //if (size/incr > 100)

        fill(0,200,0);
        rect(50, 10, size, w);
        fill(0);
        textSize(20);
        text(size/2, 5 , 26 );
        popMatrix();
    }
}
