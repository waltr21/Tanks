public class MenuButton{
    private float w, h, x, y;
    private boolean highlight;
    private String text;

    public MenuButton(int x, int y, String t){
        w = 600;
        h = 40;
        this.x = x - w/2;
        this.y = y;
        text = t;
        highlight = false;
    }

    public void setHighlight(boolean b){
        highlight = b;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getW(){
        return w;
    }

    public float getH(){
        return h;
    }

    public void show(){
        pushMatrix();

        textSize(30);
        noStroke();
        if (!highlight){
            fill(0);
            rect(x, y, w, h);
            fill(236,236,236);
            text(text, x + (w/2 - textWidth(text)/2), y + (h/2 + textAscent()/2) - 5);
        }
        else{
            fill(236,236,236);
            rect(x, y, w, h);
            fill(0);
            text(text, x + (w/2 - textWidth(text)/2), y + (h/2 + textAscent()/2) - 5);
        }
        popMatrix();
    }


}
