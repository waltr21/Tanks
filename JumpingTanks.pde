import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;

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
void setup(){
    size(1300,900, P2D);
    noSmooth();

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
void draw(){
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
        powerDisplayW += 0.5;
    else
        powerDisplayW -= 0.5;
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
    showEnemyBullets();
    enemy.showBody();
    player.showBody();
    player.showArm();
    checkHit();
    landPlats();
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
void keyReleased(){
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
void keyPressed(){
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
void mouseClicked(){
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
