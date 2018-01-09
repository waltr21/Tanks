import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.io.*; 
import java.net.*; 
import java.nio.*; 
import java.nio.channels.*; 
import java.util.ArrayList; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class JumpingTanks extends PApplet {







Tank player;
EnemyTank enemy;
Platforms plats;
HealthBar bar;
Power power;
ArrayList<MenuButton> buttons = new ArrayList<MenuButton>();
ArrayList<Bullet> bullets = new ArrayList<Bullet>();
ArrayList<EnemyBullet> enemyBullets = new ArrayList<EnemyBullet>();
float recentAngle = 30;
int dir = 0;
int gravity = 10;
boolean holdingR, holdingL, playGame, displayIncreasing;
float powerDisplayW = 30;
boolean firstClient = false;
DatagramChannel dc;
String address;
int portNum;
int speedCount = 0;
int scene = 0;
PImage titleText, controlImage, powerImage, networkImage;

/**
 * Initial setUp for the game.
 */
public void setup(){
    
    

    String[] networkStuff = loadStrings("Network.txt");

    address = networkStuff[0].trim();
    portNum = Integer.parseInt(networkStuff[1].trim());

    titleText  = loadImage("Title.png");
    controlImage = loadImage("Controls.png");
    powerImage = loadImage("PowerUps.png");
    networkImage = loadImage("netInfo.png");

    buttons.add(new MenuButton(width/2, height/2, "Play"));
    buttons.add(new MenuButton(width/2, height/2 + 50, "Controls"));
    buttons.add(new MenuButton(width/2, height/2 + 100, "Power-Ups"));
    buttons.add(new MenuButton(width/2, height/2 + 150, "Network"));

    displayIncreasing = true;

    resetGame();

    //Open the channel.
    try{
        dc = DatagramChannel.open();
    }
    catch(Exception e){
        System.out.println("Error in setup: " + e);
    }

    //Create a thread for handling packets coming in.
    Thread myThread = new Thread(new Runnable() {
        public void run() {
            runThread();
        }
    });
    myThread.start();
}

/**
 * Set the game to its initial values.
 */
public void resetGame(){
    scene = 0;
    player = new Tank();
    enemy = new EnemyTank();
    plats = new Platforms();
    bar = new HealthBar(player.getHealth());
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    enemyBullets.add(new EnemyBullet(-1000, -1000));
    power = null;
    if (firstClient){
        plats.getPlats().get(0).setMove(true);
    }
}

/**
 * Draw function is broken up into four parts to choose whihc scene to draw.
 */
public void draw(){
    background(236,236,236);
    switch(scene){
        case 0:
            drawMenu();
            break;
        case 1:
            drawGame();
            break;
        case 2:
            drawControls();
            break;
        case 3:
            drawPowers();
            break;
        case 4:
            drawNetwork();
            break;
    }
}

/**
 * Drawing of the main menu.
 */
public void drawMenu(){
    showButton();
    pushMatrix();
    fill(0);
    textSize(92);
    int titleW = 600;
    int titleH = 270;
    image(titleText, width/2 - (titleW/2), 50, titleW, titleH);
    popMatrix();
    player.move(dir);
    player.gravity();
    player.bound();
    recentAngle = calculateArmAngle();
    player.setAngle(recentAngle);
    player.showBody();
    player.showArm();
}

/**
 * Show the buttons in the main menu.
 */
public void showButton(){
    for (MenuButton m : buttons){
        m.setHighlight(false);
        if (mouseX > m.getX() && mouseX < m.getX() + m.getW()){
            if (mouseY > m.getY() && mouseY < m.getY() + m.getH())
                m.setHighlight(true);
        }
        m.show();
    }
}

/**
 * Show the image of the controls.
 */
public void drawControls(){
    image(controlImage, width/2 - 450, 0, 900, 900);
}

/**
 * Show the image of the powers and display their animations/colors.
 */
public void drawPowers(){
    if (displayIncreasing)
        powerDisplayW += 0.5f;
    else
        powerDisplayW -= 0.5f;
    if (powerDisplayW > 60 || powerDisplayW < 30)
        displayIncreasing = !displayIncreasing;

    //Health display
    fill(200, 0, 0);
    ellipse(800, 60, powerDisplayW, powerDisplayW);
    //Shield display
    fill(0,0,200);
    ellipse(800, 220, powerDisplayW, powerDisplayW);
    //Speed bullet display
    fill(214, 123, 12);
    ellipse(880, 410, powerDisplayW, powerDisplayW);
    //Jump display
    fill(46, 195, 209);
    ellipse(805, 600, powerDisplayW, powerDisplayW);
    //Tank speed display
    fill(11, 168, 11);
    ellipse(805, 760, powerDisplayW, powerDisplayW);

    image(powerImage, width/2 - 450, 0, 900, 900);
}

/**
 * Show the network image.
 */
public void drawNetwork(){
    image(networkImage, width/2-450, 0, 900, 900);
    fill(0);
    textSize(20);
    //Display cuurent configs.
    text(address, width/2 + 100, height-135);
    text(portNum, width/2 + 100, height-100);
}

/**
 * Draw the actual game.
 */
public void drawGame(){
    //Bound and move the player
    player.move(dir);
    player.gravity();
    player.bound();

    //Calculate the angle of the arm.
    recentAngle = calculateArmAngle();
    player.setAngle(recentAngle);

    //Show the graphics
    bar.show();
    showPowerList();
    plats.showPlatforms();
    showPower();
    showAndBoundBullets();
    landPlats();
    showEnemyBullets();
    enemy.showBody();
    player.showBody();
    player.showArm();
    checkHit();
    hitPower();

    boolean reset = false;
    if (player.displayDead())
        reset = true;

    //Pack the appropriate coordinates into strings and send them.
    String loc = player.getX() + "," + player.getY() + "," + player.getAngle();
    if (player.isShield())
        loc += "," + "S";
    else
        loc += "," + "N";
    //If we are the first client we handle the position of the platform.
    if (firstClient)
        loc += "," + plats.getPlats().get(0).getX() + "," + plats.getPlats().get(0).getY();
    else
        loc += "," + "F" + "," + "F";
    for (Bullet b : bullets){
        loc += "," + b.getX() + "/" + b.getY();
    }
    try{
        ByteBuffer buff = ByteBuffer.wrap(loc.getBytes());
        dc.send(buff, new InetSocketAddress(address, portNum));
    }
    catch(Exception e){
        System.out.println("Error in draw: " + e);
    }

    //Go back to the main if player dies/
    if (reset){
        //Send death packet so the players health resets.
        try{
            ByteBuffer buff = ByteBuffer.wrap("D".getBytes());
            dc.send(buff, new InetSocketAddress(address, portNum));
        }
        catch(Exception e){
            System.out.println("Error in the sendDeath packet: " + e);
        }
        resetGame();
    }
}

public void showAndBoundBullets(){
    //Loop through all of our bullets.
    for (int i = 0; i < bullets.size(); i++){
        boolean removed = false;
        //Check to see if it has hit a platform.
        for (Platform p : plats.getPlats()){
            if (bullets.get(i).getX() > p.getX() && bullets.get(i).getX() < p.getX() + p.getW()){
                if (bullets.get(i).getY() > p.getY() && bullets.get(i).getY() < p.getY() + p.getH()){
                    removed = true;
                }
            }
        }
        if (removed){
            bullets.remove(i);
        }
        //Check to see if the bullet is out of bounds.
        else if (bullets.get(i).getY() > height || bullets.get(i).getY() < 0){
            bullets.remove(i);
        }
        else if (bullets.get(i).getX() > width || bullets.get(i).getX() < 0){
            bullets.remove(i);
        }
        //Travel if in bounds.
        else{
            bullets.get(i).travel();
        }
    }
}

/**
 * Checks to see if the enemy bullets hit the current players tank.
 * (Called each frame)
 */
public void checkHit(){
    for (EnemyBullet b : enemyBullets){
        if (b.getX() > player.getX() && b.getX() < player.getX() + player.getTankW()){
            if (b.getY() > player.getY() && b.getY() < player.getY() + player.getTankH()){
                //System.out.println("HIT!");
                if (player.takeHit())
                    bar.decreaseSize();
                break;
            }
        }
    }
}

/**
 * Display the power on the plat only if it is not null.
 * (Called each frame)
 */
public void showPower(){
    if (power != null)
        power.show();
}

/**
 * Check to see if we made connection with the power.
 * (Called each frame)
 */
public void hitPower(){
    if (power != null){
        if (power.getX() > player.getX() && power.getX() < player.getX() + player.getTankW()){
            if (power.getY() > player.getY() && power.getY() < player.getY() + player.getTankH()){
                //If we pick up health it is used immediately.
                if (power.getType() == 0)
                    power.usePower();
                //Added power to the players inventory.
                else
                    player.givePower(power);
                //Send a packet that we have taken the power up.
                try{
                    ByteBuffer powerBuff = ByteBuffer.wrap("0".getBytes());
                    dc.send(powerBuff, new InetSocketAddress(address, portNum));
                }
                catch(Exception e){
                    System.out.println("Exception in hitPower: " + e);
                }
                //Reset the power.
                power = null;
            }
        }
    }
}

/**
 * Creates a new power object to display depending on the type we recieve.
 * @param int type number to represent which type of powerup to create.
 */
public void setPower(int type){
    if (type == 0)
        power = new PowerHealth(plats.getPlats().get(0), player, bar);
    else if (type == 1)
        power = new PowerShot(plats.getPlats().get(0), player, bar);
    else if (type == 2)
        power = new PowerShield(plats.getPlats().get(0), player, bar);
    else if (type == 3)
        power = new PowerSpeed(plats.getPlats().get(0), player, bar);
    else if (type == 4)
        power = new PowerJump(plats.getPlats().get(0), player, bar);

    else
        System.out.println("Inavlid type");
}

/**
 * Bound the tank to the playforms.
 */
public void landPlats(){
    for (Platform p : plats.getPlats()){
        //Temp floats for important points on the tank.
        float playerCenter = player.getX() + player.getTankW()/2;
        float playerTempY = player.getY() + player.getTankH();
        //If we are traveling down.
        if (player.getVelocity() > 0){
            //Compare the Y values
            if (playerTempY > p.getY() && playerTempY < p.getY() + p.getH()){
                //Compare the X values.
                if (playerCenter > p.getX() && playerCenter < p.getX() + p.getW()){
                    player.resetVelocity();
                    player.resetCount();
                    player.setY(p.getY() - player.getTankH());
                }
            }
        }
    }
}

/**
 * Show all of the power ups the user has in their inventory.
 */
public void showPowerList(){
    int startX = width - 30;
    int startY = 30;
    for (Power p : player.getPowers()){
        fill(p.getColor());
        //stroke();
        ellipse(startX, startY, 30, 30);
        startX -= 40;
    }
}

/**
 * Show all of the enemy bullets.
 */
public void showEnemyBullets(){
    for (EnemyBullet b : enemyBullets){
        b.showBullet();
    }
}

/**
 * Calculate the angle at which the player arm should be rotated.
 * @return a float between 0 and 2 PI (angle in radians of how the arm should be
 *  rotated)
 */
public float calculateArmAngle(){
    //Make a right triangle with the mouse and tank pos.
    float opp = (mouseY - player.getArmY());
    float adj = (mouseX - player.getArmX());
    //Handle divide by 0 errors.
    if (adj == 0)
        return recentAngle;

    //Take the arc tan of our angle (returns in radians)
    float newAngle = atan(opp/adj);

    //If we are in the 1 or 3 quadrant we have to add pi.
    if (mouseX < player.getArmX()){
        newAngle += PI;
    }
    return newAngle;
}

/**
 * Separate thread for recieving packets.
 * (We don't call this in the same thread because packet delay could cause the
 * framerate to drop below 60)
 */
public void runThread(){
    try{
        System.out.println("Thread created.");
        while (true){
            ByteBuffer buffer = ByteBuffer.allocate(1024);
    		dc.receive(buffer);
            String message = new String(buffer.array());
            message = message.trim();
            String[] coordinates = message.split(",");

            //Have we recieved a packet telling us to control the
            //plat form movement?
            if (coordinates[0].equals("F")){
                firstClient = true;
                plats.getPlats().get(0).setMove(true);
            }
            //Have we recieved a death packet?
            else if(coordinates[0].equals("D")){
                power = null;
                player.setHealth(10);
                bar.increaseSize(10);
                player.clearPowers();
            }
            //Have we recieved a packet telling us the powerup has been taken by
            //the other player.
            else if (coordinates[0].equals("0")){
                power = null;
            }
            //Have we recieved a packet telling us a new powerup should be generated?
            else if(coordinates[0].equals("1")){
                setPower(Integer.parseInt(coordinates[1]));
            }
            //We have recieved a normal packet.
            else{

                /*
                 *[0] Tank X pos.
                 *[1] Tank Y pos.
                 *[2] Tank arm Angle pos.
                 *[3] Player shield.
                 *[4] Plat X pos
                 *[5] Plat Y pos
                 *[6-X] Bullet X and Y pos.
                 */

                enemy.setX(Float.parseFloat(coordinates[0]));
                enemy.setY(Float.parseFloat(coordinates[1]));
                enemy.setAngle(Float.parseFloat(coordinates[2]));

                if (coordinates[3].equals("S"))
                    enemy.setShield(true);
                else
                    enemy.setShield(false);


                if (!firstClient){
                    plats.getPlats().get(0).setX(Float.parseFloat(coordinates[4]));
                    plats.getPlats().get(0).setY(Float.parseFloat(coordinates[5]));
                }

                int bulletCount = 0;
                for (int i = 6; i < coordinates.length; i++){
                    String[] bulletCoor = coordinates[i].split("/");
                    float locX = Float.parseFloat(bulletCoor[0]);
                    float locY = Float.parseFloat(bulletCoor[1]);
                    enemyBullets.get(bulletCount).setX(locX);
                    enemyBullets.get(bulletCount).setY(locY);
                    bulletCount++;
                }

                for (int i = bulletCount; i < enemyBullets.size(); i++){
                    enemyBullets.get(i).setY(-1000);
                    enemyBullets.get(i).setX(-1000);
                }

            }
        }
    }
    catch(Exception e){
        System.out.println("Exception in the tank thread: " +  e);
    }
}

/**
 * Key listeners
 */
public void keyReleased(){
    if (keyCode == RIGHT || key == 'd' || key == 'D')
        holdingR = false;

    if (keyCode == LEFT || key == 'a' || key == 'A')
        holdingL = false;

    //Check that we have both keys lifted up to stop moving.
    if (!holdingL && !holdingR)
        dir = 0;
}

/**
 * Key listeners
 */
public void keyPressed(){
    //Move the player left and right.
    if (keyCode == RIGHT || key == 'd' || key == 'D'){
        dir = 3;
        holdingR = true;
    }
    if (keyCode == LEFT || key == 'a' || key == 'A'){
        dir = -3;
        holdingL = true;
    }
    //Jump for the player.
    if (keyCode == UP || key == 'w' || key == 'W')
        player.jump();

    //Add a bullet to the ArrayList when the player fires.
    if (key == ' '){
        if (bullets.size() < 3){
            if (speedCount >= 10){
                speedCount = 0;
                player.setFastBullet(false);
            }
            if (player.isFastBullet())
                speedCount++;

            //Calculate the x and y coordinates of the bullet to be displayed at
            //the end of the arm.
            float newX =  (player.getArmW() * cos(recentAngle)) + player.getArmX();
            float newY = (player.getArmW() * sin(recentAngle)) + player.getArmY();

            if (player.isFastBullet())
                bullets.add(new Bullet(newX, newY, recentAngle, true));
            else
                bullets.add(new Bullet(newX, newY, recentAngle, false));
        }
    }
    if (keyCode == ENTER || keyCode == TAB){
        player.usePower();
    }
}

/**
 * Click listener.
 */
public void mouseClicked(){
    if (scene == 0){
        //Have we clicked on the play button?
        if (mouseX > buttons.get(0).getX() && mouseX < buttons.get(0).getX() + buttons.get(0).getW()){
            if (mouseY > buttons.get(0).getY() && mouseY < buttons.get(0).getY() + buttons.get(0).getH())
                scene = 1;
        }
        //Have we clicked on the control button?
        if (mouseX > buttons.get(1).getX() && mouseX < buttons.get(1).getX() + buttons.get(1).getW()){
            if (mouseY > buttons.get(1).getY() && mouseY < buttons.get(1).getY() + buttons.get(1).getH())
                scene = 2;
        }
        //Have we clicked on the powerup button?
        if (mouseX > buttons.get(2).getX() && mouseX < buttons.get(2).getX() + buttons.get(2).getW()){
            if (mouseY > buttons.get(2).getY() && mouseY < buttons.get(2).getY() + buttons.get(2).getH())
                scene = 3;
        }
        //Have we clicked on the network button?
        if (mouseX > buttons.get(3).getX() && mouseX < buttons.get(3).getX() + buttons.get(3).getW()){
            if (mouseY > buttons.get(3).getY() && mouseY < buttons.get(3).getY() + buttons.get(3).getH())
                scene = 4;
        }
    }
    //Reset the scene to exit to the main.
    else if (scene == 2 || scene == 3 || scene == 4){
        scene = 0;
    }

    //Same logic to represent the bullet being shot.
    else{
        if (bullets.size() < 3){
            if (bullets.size() < 3){
                if (speedCount >= 5){
                    speedCount = 0;
                    player.setFastBullet(false);
                }
                if (player.isFastBullet())
                    speedCount++;
            }
            //Calculate the x and y coordinates of the bullet to be displayed at
            //the end of the arm.
            float newX =  (player.getArmW() * cos(recentAngle)) + player.getArmX();
            float newY = (player.getArmW() * sin(recentAngle)) + player.getArmY();

            if (player.isFastBullet())
                bullets.add(new Bullet(newX, newY, recentAngle, true));
            else
                bullets.add(new Bullet(newX, newY, recentAngle, false));
        }
    }
}
public class Bullet{
    private float x;
    private float y;
    private float angle;
    private PVector pos;
    private PVector velocity;
    private int speed;
    private boolean fast;

    public Bullet(float x, float y, float angle, boolean s){
        this.x = x;
        this.y = y;
        this.angle = angle;
        fast = s;
        if (s)
            speed = 30;
        else
            speed = 15;
        pos = new PVector(x,y);
        velocity = PVector.fromAngle(angle);
    }

    public float getY(){
        return pos.y;
    }

    public float getX(){
        return pos.x;
    }

    public void travel(){
        pushMatrix();
        if(fast)
            fill(214, 123, 12);
        else
            fill(0);
        ellipse(pos.x, pos.y, 10, 10);
        pos.add(velocity.x * speed, velocity.y * speed);
        popMatrix();
    }

}
public class EnemyBullet{
    private float x;
    private float y;

    public EnemyBullet(float conX, float conY){
        x = conX;
        y = conY;
    }

    public void setX(float newX){
        x = newX;
    }

    public void setY(float newY){
        y = newY;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public void showBullet(){
        fill(0);
        ellipse(x, y, 10, 10);
    }
}
public class EnemyTank{
    private float x = -1000;
    private float y = -1000;
    private int bodyW = 120;
    private int bodyH = 30;
    private int armW = 80;
    private int armH = 8;
    private float angle = 0;
    private boolean shield = false;
    private PImage img = loadImage("tank2.png");

    public void setAngle(float a){
        angle = a;
    }

    public void setX(float tempX){
        x = tempX;
    }

    public void setShield(boolean b){
        shield = b;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getAngle(){
        return angle;
    }

    public void setY(float tempY){
        y = tempY;
    }

    public void showBody(){
        rotate(0);
        pushMatrix();

        //translate according to the position of the tank arm.
        translate(x+(bodyW/2), y);

        if (shield){
            noFill();
            strokeWeight(3);
            stroke(0,0,200);
            ellipse(0, 0, 140, 140);
        }
        noStroke();

        //Draw the body to the adjusted translate pos.
        image(img, -(bodyW/2), 0, bodyW, bodyH);

        rotate(angle);
        fill (119, 8, 0);
        //Draw the recentAngle slightly adjusted so it can
        //rotate from its center point.
        rect(0, -armH/2, armW, armH);

        popMatrix();
    }
}
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
public class Platforms{
    private ArrayList<Platform> plats = new ArrayList<Platform>();

    public ArrayList<Platform> getPlats(){
        return plats;
    }

    public Platforms(){
        int midHeight = 25;
        int midWidth = 70;
        plats.add(new Platform(width/2 - midWidth/2, height/2, midWidth, midHeight));
        plats.add(new Platform(0, height/2 - 200, 200, 25));
        plats.add(new Platform(0, height/2 + 200, 200, 25));
        plats.add(new Platform(width - 200, height/2 + 200, 200, 25));
        plats.add(new Platform(width - 200, height/2 - 200, 200, 25));
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

    public Platform(float tempX, float tempY, int tempW, int tempH){
        x = tempX;
        y = tempY;
        w = tempW;
        h = tempH;
        moving = false;
        right = true;
        speed = 2;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public void setX(float tempX){
        x = tempX;
    }

    public void setY(float tempY){
        y = tempY;
    }

    public void setMove(boolean m){
        moving = m;
    }

    public int getW(){
        return w;
    }

    public int getH(){
        return h;
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
public class Power{
    private float x;
    private float y;
    private int type;
    private float size;
    private boolean increase;
    private int c;
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

    public void setX(float tempX){
        x = tempX;
    }

    public void setY(float tempY){
        y = tempY;
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

    private void setColor(int tempC){
        c = tempC;
    }

    public int getColor(){
        return c;
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
            size += 0.3f;
        else
            size -= 0.3f;
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
        int healthColor = color(200, 0, 0);
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
        int shotColor = color(214, 123, 12);
        super.setColor(shotColor);
        super.setType(1);
    }

    public void usePower(){
        //System.out.println("Power used");
        super.getTank().setFastBullet(true);
    }
}

class PowerShield extends Power{
    public PowerShield(Platform mid, Tank p, HealthBar h){
        super(mid, p, h);
        int shieldColor = color(0, 0, 200);
        super.setColor(shieldColor);
        super.setType(2);
    }

    public void usePower(){
        super.getTank().giveShield();
    }
}

class PowerSpeed extends Power{
    public PowerSpeed(Platform mid, Tank p, HealthBar h){
        super(mid, p, h);
        int speedColor = color(11, 168, 11);
        super.setColor(speedColor);
        super.setType(3);
    }

    public void usePower(){
        super.getTank().setSpeed(true);
    }
}

class PowerJump extends Power{
    public PowerJump(Platform mid, Tank p, HealthBar h){
        super(mid, p, h);
        int jumpColor = color(46, 195, 209);
        super.setColor(jumpColor);
        super.setType(4);
    }

    public void usePower(){
        super.getTank().setJump(10000);
    }
}
public class Tank{
    //X value for the take and rotations
    private float x = width/2;
    //Y value for the tank and rotations
    private float y = height/2;
    //Width of the arm.
    private int armW = 80;
    //Height of the arm.
    private int armH = 8;
    //Angle to rotate the arm
    private float armAngle = 0;
    //Width of the body of the tank.
    private int bodyW = 120;
    //Height of the body of the tank.
    private int bodyH = 30;
    //Force of gravity in the game.
    private float gravity = 0.8f;
    //Variable to keep track of the velocity in the game.
    private float velocity = 0;
    //Count to limit the jumps to one.
    private int count = 0;
    //Health for the player.
    private int health = 10;
    //Time slot to make sure one bullet can't do more than one hit.
    private long pastTime = 0;
    //Image for the tank to draw.
    private PImage img = loadImage("tank1.png");
    //Boolean for the speed of the bullet
    private boolean speed = false;
    //Boolean for the shield of the tank.
    private boolean shield = false;
    //Boolean for the speed of the tank
    private boolean tankFast = false;
    //Number of hits the shield has taken
    private int shieldCount = 0;
    //Time stamp for when the speed power was used
    private int speedTimeStamp = 0;
    //Max amount of time allowed for a power to be used
    private int maxTime = 20000;
    //Max amount of jumps for the player
    private int maxJump = 2;
    //Time stamp for when the jump power was used.
    private int jumpTimeStamp = 0;
    //List to hold the power ups.
    private ArrayList<Power> powerUps = new ArrayList<Power>();

    public void setAngle(float a){
        armAngle = a;
    }

    public void setX(float tempX){
        x = tempX;
    }

    public void setY(float tempY){
        y = tempY;
    }

    public float getX(){
        return x-(bodyW/2);
    }

    public float getY(){
        return y;
    }

    public float getVelocity(){
        return velocity;
    }

    public float getArmX(){
        return x;
    }

    public float getArmY(){
        return y;
    }

    public float getArmW(){
        return armW;
    }

    public float getArmH(){
        return armH;
    }

    public int getHealth(){
        return health;
    }

    public int getTankH(){
        return bodyH;
    }

    public int getTankW(){
        return bodyW;
    }

    public void clearPowers(){
        powerUps.clear();
    }

    public boolean isFastBullet(){
        return speed;
    }

    public void setFastBullet(boolean b){
        speed = b;
    }

    public ArrayList<Power> getPowers(){
        return powerUps;
    }

    public void setHealth(int tempHealth){
        health = tempHealth;
        if (health > 10)
            health = 10;
    }

    public void givePower(Power pUp){
        powerUps.add(pUp);
    }

    public void usePower(){
        if (powerUps.size() > 0){
            powerUps.get(0).usePower();
            powerUps.remove(0);
        }
    }

    public void giveShield(){
        shield = true;
        shieldCount = 0;
    }

    public boolean isShield(){
        return shield;
    }

    public void setSpeed(boolean b){
        tankFast = b;
        if (tankFast){
            speedTimeStamp = millis();
        }
    }

    public void setJump(int j){
        maxJump = j;
        jumpTimeStamp = millis();
    }

    public boolean takeHit(){
        if (System.currentTimeMillis() - pastTime > 200){
            pastTime = System.currentTimeMillis();
            if (!shield){
                health--;
                return true;
            }
            if (shield){
                shieldCount++;
            }
            if (shield && shieldCount > 2){
                shield = false;
            }
        }
        return false;
    }

    public void move(int dir){
        if (tankFast && millis() - speedTimeStamp > maxTime)
            tankFast = false;
        if (maxJump > 2 && millis() - jumpTimeStamp > maxTime)
            maxJump = 2;

        if (tankFast)
            dir *= 2;
        x += dir;
    }

    public float getAngle(){
        return armAngle;
    }

    public void gravity(){
        velocity += gravity;
        y += velocity;
    }

    public void jump(){
        if (count < maxJump)
            velocity = -20.5f;
        count++;
    }

    public void resetCount(){
        count = 0;
    }

    public boolean displayDead(){
        if (health <= 0){
            x = -1000;
            y = -1000;
            return true;
        }
        return false;
    }

    public void bound(){
        if (y + bodyH > height){
            y = height - bodyH;
            resetVelocity();
            resetCount();
        }
        if (y < 0)
            y = 0;
        if (x - bodyW/2 < 0)
            x = 0 + bodyW/2;
        if (x + bodyW/2 > width)
            x = width - bodyW/2;
    }

    public void resetVelocity(){
        velocity = 0;
    }

    public void showArm(){
        rotate(0);
        translate(x,y);
        rotate(armAngle);
        fill (106, 108, 0);
        rect(0, -armH/2, armW, armH);
    }

    public void showBody(){
        pushMatrix();
        rotate(0);
        image(img, x-(bodyW/2), y, bodyW, bodyH);
        if (shield){
            noFill();
            strokeWeight(3);
            stroke(0,0,200);
            ellipse(x, y, 140, 140);

        }
        popMatrix();
        noStroke();

    }
}
    public void settings() {  size(1300,900);  noSmooth(); }
    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[] { "JumpingTanks" };
        if (passedArgs != null) {
          PApplet.main(concat(appletArgs, passedArgs));
        } else {
          PApplet.main(appletArgs);
        }
    }
}
