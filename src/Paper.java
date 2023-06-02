import javax.swing.*;
import javax.swing.Timer;
import java.awt.event.*;
import java.awt.*;
import javax.swing.JPanel;

public class Paper
{    
    static int nrOfPlayers = 2; // 1 - 5 players allowed
    static int deathTime = 3;
    static int gameSpeed = 100;
    static boolean gameRunning = false;
    static int tilesX = 60;
    static int tilesY = 30;
    static int tileSizeX = 12;
    static int tileSizeY = 12;
    static int margX = 32;
    static int margY = 16;
    static int marginText = 170;
    static int width = margX * 2 + tileSizeX * tilesX + marginText;
    static int height = margY * 2 + margX * 2 + tileSizeY * tilesY;
    
    // Different directions
    static Point[] directions = { new Point (-1, 0), new Point (0, -1), new Point (1, 0), new Point (0, 1) };

    // Player 
    //static Point[] head = new Point[nrOfPlayers];
    static Point[] dir = new Point[nrOfPlayers]; 
    static int[] snakeLength = new int [nrOfPlayers];
    static Point[][] snake = new Point [nrOfPlayers][tilesX * tilesY];
    static int[] playerScore = new int [nrOfPlayers];
    static int[] playerTileCount = new int [nrOfPlayers];
    static int[] playerResurrectTimer = new int [nrOfPlayers];

    // Control keys for the players

    static int[][] key = { { KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN },
                            { KeyEvent.VK_A, KeyEvent.VK_W, KeyEvent.VK_D, KeyEvent.VK_S },
                            { KeyEvent.VK_N, KeyEvent.VK_J, KeyEvent.VK_COMMA, KeyEvent.VK_M },
                            { KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_ENTER, KeyEvent.VK_HOME, KeyEvent.VK_PAGE_UP } };
    // Map
    static Color[][] tiles = new Color [tilesX][tilesY];
    static Color[][] playerColors = {
        { new Color (235, 240, 25), new Color (244, 250, 60  ), new Color (150, 190, 30 ) },
        { new Color (244, 50, 250 ), new Color (180, 25, 180 ), new Color (90, 10, 90 ) },
        { new Color (50, 220, 220 ), new Color (25, 180, 180 ), new Color (20, 120, 120 ) },
        { new Color (25, 50, 220 ), new Color (50, 25, 220 ), new Color (20, 20, 180 ) },
        { new Color (50, 220, 50 ), new Color (25, 180, 25 ), new Color (10, 180, 10 ) }
    };
    static Color paperColor = new Color (230, 230, 230);
    static Color backgroundColor = new Color (20, 2, 20 );

    public static void main (String[] args) {
        
        JFrame frame = new JFrame ("Paper");

        JPanel panel = new JPanel () {
            public void paint (Graphics g) {
                super.paint(g);

                // Bakgrunden
                for (int y = 0; y < tilesY; y++)
                    for (int x = 0; x < tilesX; x++) {
                        g.setColor(tiles[x][y]);
                        g.fillRect(margX + x * tileSizeX,
                                margY + y * tileSizeY, tileSizeX - 2, tileSizeY - 2);

                    }

                // Snakes
                for (int p = 0; p < nrOfPlayers; p++) {
                    g.setColor(playerColors[p][2]);
                    for (int i = 0; i < snakeLength[p]; i++)
                        g.fillRect(margX + snake[p][i].x * tileSizeX,
                                margY + snake[p][i].y * tileSizeY, tileSizeX - 1, tileSizeY - 1);
                }
                // Print score and status
                for (int p = 0; p < nrOfPlayers; p++) {
                    g.setColor (playerColors[p][0]);
                    String playerText = "Player " + (p + 1) + ": " +
                                    (playerTileCount[p]  * 100 / (tilesX * tilesY)) +
                                    "% kills: "+ playerScore[p];
                    if (playerResurrectTimer[p] > 0) {
                        playerText += " - " + playerResurrectTimer[p] + " -";
                    }
                    g.drawString(playerText ,width - marginText, margY + (g.getFontMetrics().getHeight() + 2) * p);
                }
            }
        };
        frame.setSize (width, height);
        frame.setContentPane (panel);
        frame.getContentPane ().setBackground (backgroundColor);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        newGame ();
        frame.setVisible (true);
        
        frame.addKeyListener (new KeyListener () {
            public void keyTyped (KeyEvent e) { }
            public void keyPressed (KeyEvent keyEvent) {
                //System.out.println (keyEvent.getKeyCode ());
                for (int p = 0; p < nrOfPlayers; p++) {
                    for (int i = 0; i < 4; i++) {
                        if (keyEvent.getKeyCode() == key[p][i]) {
                            // Kolla så att du inte trycket åt motsatta håll när du är i "snake mode"
                            if (snakeLength[p] > 1) {
                                if (dir[p] == directions[(i+2)%4])
                                    continue;
                            }
                            dir[p] = directions[i];
                        }
                        // For more keys
                        //if (keyEvent.getKeyCode () == key[p][4]);
                    }
                }
            }
            public void keyReleased (KeyEvent e) {}
        });
        
        ActionListener timeActionL = actionEvent -> {
            if (!gameRunning) {
                panel.repaint ();
                return;
            }

            for (int p = 0; p < nrOfPlayers; p ++) {
                if (playerResurrectTimer[p] > 0) {
                    if (playerResurrectTimer[p] == 1)
                        spawnPlayer (p);
                    playerResurrectTimer[p] --;
                    continue;
                }


                Point newHead = new Point (snake[p][0].x + dir[p].x, snake[p][0].y + dir[p].y);

                if (newHead.x >= tilesX || newHead.x < 0 ||
                    newHead.y >= tilesY || newHead.y < 0) {
                        // Kört ut i kanten
                        killPlayer(p);
                        return;
                    }

                // Kolla om du krockat med din egen svan;
                for (int i = 1; i < snakeLength[p]; i ++) {
                    if (newHead.x == snake[p][i].x &&
                        newHead.y == snake[p][i].y ) {
                        //gameRunning = false;
                        killPlayer (p);
                        return;
                        //dir[p].setLocation (0, 0);
                    }
                }
                // Kolla om du krockat med någon annans svans;
                for (int q = 0; q < nrOfPlayers; q ++) {
                    if (q == p) continue; // Kolla inte med dig själv här.
                    for (int i = 1; i < snakeLength[q]; i ++) {
                        if (newHead.x == snake[q][i].x &&
                            newHead.y == snake[q][i].y ) {
                            // p har dödat q
                            killPlayer (q);
                            playerScore[p]++;
                        }
                    }
                }


                if (tiles[newHead.x][newHead.y] != playerColors[p][0]) {
                    if (snakeLength[p] == 1 && tiles[snake[p][0].x][snake[p][0].y] == playerColors[p][0]) {
                        // Tar första steget ut
                        snakeLength[p]++;
                        snake[p][snakeLength[p]-1] = new Point ();

                    } else {
                        // Öka "ormen"
                        snakeLength[p]++;
                        snake[p][snakeLength[p]-1] = new Point ();
                    }
                } else {
                    if (snakeLength[p] > 1) { // 1 ??
                        // Kommit hem efter en tur -> Fyll kartan
                        // kopiera ormen till kartan:
                        for (int i = 0; i < snakeLength[p]; i ++) {
                            tiles[snake[p][i].x][snake[p][i].y] = playerColors[p][0];
                        }
                        fillArea (p, newHead);
                        calculateProcent();
                    }
                    snakeLength[p] = 1;
                }

                for (int i = snakeLength[p]-1; i > 0 ; i --) {
                    snake[p][i].setLocation (snake[p][i-1]);
                }
                snake[p][0].setLocation (newHead);
            }
            frame.repaint ();
        };
        Timer timer = new Timer(gameSpeed, timeActionL);
        timer.start();
    }

    private static void calculateProcent() {
        // Clear
        for (int p = 0; p < nrOfPlayers; p++) {
            playerTileCount[p] = 0;
        }

        // Recount
        for (int y = 0; y < tilesY; y++) {
            for (int x = 0; x < tilesX; x++) {
                for (int p = 0; p < nrOfPlayers; p++) {
                    for (int c = 0; c < 2; c++) {
                        if (tiles[x][y] == playerColors[p][c]) {
                            playerTileCount[p]++;
                        }
                    }
                }
            }
        }
    }

    private static void killPlayer (int q) {
        playerResurrectTimer[q] = deathTime * 1000 / gameSpeed;
        snakeLength[q] = 1;
    }

    private static void newGame () {
        // Clear map
        for (int y = 0; y < tilesY; y ++)
            for (int x = 0; x < tilesX; x++) {
                tiles[x][y] = paperColor;
            }

        for (int p = 0; p < nrOfPlayers; p ++) {
            spawnPlayer(p);
        }
        calculateProcent();
        gameRunning = true;
    }

    private static void spawnPlayer (int p) {
        dir[p] = new Point (1, 0);

        snake[p][0]  = findStartBox (p);
        // Create 3x3 start box
        for (int y1 = snake[p][0].y-1; y1 < snake[p][0].y + 2; y1 ++) {
            for (int x1 = snake[p][0].x-1; x1 < snake[p][0].x + 2; x1++) {
                tiles[x1][y1] = playerColors[p][0];
            }
        }
        // Set Direction
        int distanceX = snake[p][0].x - tilesX / 2;
        int distanceY = snake[p][0].y - tilesY / 2;
        if (distanceX > distanceY) {
            // X
            if (distanceX <0) dir[p] = directions[2];
            else dir[p] = directions[0];
        } else {
            // Y
            if (distanceY <0) dir[p] = directions[3];
            else dir[p] = directions[1];
        }

        snakeLength[p] = 1;
    }

    private static Point findStartBox (int p) {
        int spawnSizeMargin = 2;
        int tryCount = 0;
        Point ret = new Point (spawnSizeMargin, spawnSizeMargin);
        // find random area

        again:
        for (int x = (int) (Math.random () * (tilesX - spawnSizeMargin * 2) + spawnSizeMargin),
                 y = (int) (Math.random () * (tilesY - spawnSizeMargin * 2) + spawnSizeMargin),
                 y1 = y-spawnSizeMargin; y1 < y + spawnSizeMargin + 1; y1++) {
            for (int x1 = x - spawnSizeMargin; x1 < x + spawnSizeMargin + 1; x1++) {
                    if (tiles[x1][y1] != paperColor && tiles[x1][y1] != playerColors[p][0] && tiles[x1][y1] != playerColors[p][1]) {
                        tryCount ++;
                        if (tryCount > 200) {
                            return ret;
                        }
                        continue again;
                    }
            }
            ret.x = x;
            ret.y = y;
        }

        return ret;
    }
    private static void fillArea (int p, Point newHead) {      
        // Try to fill different from different points
        for (int j = 0; j < 4; j ++) {
            //tiles[snake[p][i].x + directions[j].x] [ snake[p][i].y + directions[j].x] = new Color (0, 0, 100);
            tryFill (p, newHead.x + directions[j].x, newHead.y + directions[j].x);      
        }
      
        for (int i = snakeLength[p] - 1; i >= 0; i -- ) {
            for (int j = 0; j < 4; j ++) {
                //tiles[snake[p][i].x + directions[j].x] [ snake[p][i].y + directions[j].x] = new Color (0, 0, 100);
                tryFill (p, snake[p][i].x + directions[j].x, snake[p][i].y + directions[j].x);      
            }
        }
    }
    private static void copyMap (Color[][] sourceMap, Color[][] destMap) {
        // Copy m1 to m2
        if (sourceMap == null || destMap == null) return;
        for (int y = 0; y < tilesY; y++)
            for (int x = 0; x < tilesX; x++) {
                destMap[x][y] = sourceMap[x][y];
            }
    }
    
    private static boolean tryFill (int p, int x, int y) {
        Color[][] tilesTest = new Color [tilesX][tilesY];
        copyMap (tiles, tilesTest);
        if (recursiveSearch (tilesTest, p, x, y)) {
            copyMap (tilesTest, tiles);
            return true;
        }
        return false;
    }
    
    private static boolean recursiveSearch (Color[][] tilesMap, int p, int x, int y) {
        if (x < 0 || x >= tilesX ||
            y < 0 || y >= tilesY ) {
            return false; // Reached edge of screen.    
        }
        if (tilesMap[x][y] == playerColors[p][0] )
            return true;
        tilesMap[x][y] = playerColors[p][0];

        if (!recursiveSearch (tilesMap, p, x + 1, y)) return false;
        if (!recursiveSearch (tilesMap, p, x - 1, y)) return false;
        if (!recursiveSearch (tilesMap, p, x, y + 1)) return false;
        return recursiveSearch (tilesMap, p, x, y - 1);
    }
}
