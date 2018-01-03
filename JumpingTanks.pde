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
String address = "127.0.0.1";
int portNum = 8765;
int speedCount = 0;
int scene = 0;
PImage titleText, controlImage, powerImage;



void setup(){
    size(1300,900, P2D);
    noSmooth();
    titleText  = loadImage("Title.png");
    controlImage = loadImage("Controls.png");
    powerImage = loadImage("PowerUps.png");

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
    }
}

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

public void drawControls(){
    image(controlImage, width/2 - 450, 0, 900, 900);
}

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

public void drawGame(){
    //System.out.println(frameRate);
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

    if (reset){
        resetGame();
    }
}

public void showAndBoundBullets(){
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

public void showPower(){
    if (power != null)
        power.show();
}

public void hitPower(){
    if (power != null){
        if (power.getX() > player.getX() && power.getX() < player.getX() + player.getTankW()){
            if (power.getY() > player.getY() && power.getY() < player.getY() + player.getTankH()){
                if (power.getType() == 0)
                    power.usePower();
                else
                    player.givePower(power);
                try{
                    ByteBuffer powerBuff = ByteBuffer.wrap("0".getBytes());
                    dc.send(powerBuff, new InetSocketAddress(address, portNum));
                }
                catch(Exception e){
                    System.out.println("Exception in hitPower: " + e);
                }
                power = null;
            }
        }
    }
}

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

public void showEnemyBullets(){
    for (EnemyBullet b : enemyBullets){
        b.showBullet();
    }
}

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

public void runThread(){
    try{
        System.out.println("Thread created.");
        while (true){
            ByteBuffer buffer = ByteBuffer.allocate(1024);
    		dc.receive(buffer);
            String message = new String(buffer.array());
            message = message.trim();
            String[] coordinates = message.split(",");

            if (coordinates[0].equals("F")){
                firstClient = true;
                plats.getPlats().get(0).setMove(true);
            }
            else if (coordinates[0].equals("0")){
                power = null;
            }
            else if(coordinates[0].equals("1")){
                setPower(Integer.parseInt(coordinates[1]));
            }
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

void keyReleased(){
    if (keyCode == RIGHT || key == 'd' || key == 'D')
        holdingR = false;

    if (keyCode == LEFT || key == 'a' || key == 'A')
        holdingL = false;

    //Check that we have both keys lifted up to stop moving.
    if (!holdingL && !holdingR)
        dir = 0;
}

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

            //Calculate the x and y coordinates of the bullet before
            float newX =  (player.getArmW() * cos(recentAngle)) + player.getArmX();
            float newY = (player.getArmW() * sin(recentAngle)) + player.getArmY();

            if (player.isFastBullet())
                bullets.add(new Bullet(newX, newY, recentAngle, true));
            else
                bullets.add(new Bullet(newX, newY, recentAngle, false));
        }
    }
    if (keyCode == ENTER){
        player.usePower();
    }
}

void mouseClicked(){
    if (scene == 0){
        if (mouseX > buttons.get(0).getX() && mouseX < buttons.get(0).getX() + buttons.get(0).getW()){
            if (mouseY > buttons.get(0).getY() && mouseY < buttons.get(0).getY() + buttons.get(0).getH())
                scene = 1;
        }
        if (mouseX > buttons.get(1).getX() && mouseX < buttons.get(1).getX() + buttons.get(1).getW()){
            if (mouseY > buttons.get(1).getY() && mouseY < buttons.get(1).getY() + buttons.get(1).getH())
                scene = 2;
        }
        if (mouseX > buttons.get(2).getX() && mouseX < buttons.get(2).getX() + buttons.get(2).getW()){
            if (mouseY > buttons.get(2).getY() && mouseY < buttons.get(2).getY() + buttons.get(2).getH())
                scene = 3;
        }
    }
    else if (scene == 2 || scene == 3){
        scene = 0;
    }

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

            //Calculate the x and y coordinates of the bullet before
            float newX =  (player.getArmW() * cos(recentAngle)) + player.getArmX();
            float newY = (player.getArmW() * sin(recentAngle)) + player.getArmY();
            if (player.isFastBullet())
                bullets.add(new Bullet(newX, newY, recentAngle, true));
            else
                bullets.add(new Bullet(newX, newY, recentAngle, false));
        }
    }
}
