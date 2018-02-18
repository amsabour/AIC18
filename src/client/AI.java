package client;

import client.model.*;
import client.model.Map;
import common.util.Log;
import javafx.util.Pair;

import java.awt.image.AreaAveragingScaleFilter;
import java.lang.reflect.Array;
import java.util.*;

import static common.network.JsonSocket.TAG;

import java.lang.Math;

/**
 * AI class.
 * You should fill body of the method {@link }.
 * Do not change name or modifiers of the methods or fields
 * and do not add constructor for this class.
 * You can add as many methods or fields as you want!
 * Use world parameter to access and modify game's
 * world!
 * See World interface for more details.
 */
public class AI {

    private Random random = new Random();
    private static final int ARCHER_TOWER_IMPACT = 5;
    private static final int ATTACK_TURN = 10;
    private static final int RISK_WHEN_AT_END = 50;
    private boolean initialize = true;
    private static final int RISK_DECREASE_BY_BEING_IN_RANGE = 3; // TODO: 2/18/2018 this needs tweaking
    private static ArrayList<ArrayList<Integer>> myPaths;


    private static int firstPlayer = 1;


    void simpleTurn(World game) {
        Log.d(TAG, "lightTurn Called" + " Turn:" + game.getCurrentTurn());
        if (firstPlayer == 1 && initialize) {
            init(game);
            initialize = false;
        } else if (firstPlayer == 1) {
            updateMyPaths(game);
        }

        if (firstPlayer == 0) {
            int money = game.getMyInformation().getMoney();
            while (money >= LightUnit.INITIAL_PRICE) {
                game.createLightUnit(0);
                money -= LightUnit.INITIAL_PRICE;
            }
            return;
        }
        int move = whatToDo(game);
        if (move == -1) { // Defence
            simpleDefenceTurn(game);
        } else if (move == 1) { // Attack //TODO
            if (firstPlayer == 0) {
                int req = LightUnit.INITIAL_PRICE;
                int money = game.getMyInformation().getMoney();
                while (true) {
                    if (money < req) {
                        break;
                    }
                    int rnd = random.nextInt(game.getAttackMapPaths().size());
                    game.createLightUnit(rnd);
                    money -= req;
                }
            }
        } else if (move == 0) { // Save money //TODO
            return;
        }


    }


    private void updateMyPaths(World game) {
        for (int i = 0; i < game.getDefenceMapPaths().size(); i++) {
            int size = 0;
            size = game.getDefenceMapPaths().get(i).getRoad().get(3).getUnits().size();
            myPaths.get(i).remove(myPaths.get(i).size() - 1);
            myPaths.get(i).add(0, size);
        }
    }

    private void init(World game) {
        myPaths = new ArrayList<ArrayList<Integer>>(game.getDefenceMapPaths().size());
        ArrayList<Integer> tempPath = new ArrayList<>();

        for (int i = 0; i < game.getDefenceMapPaths().size(); i++) {
            for (RoadCell roadCell : game.getDefenceMapPaths().get(i).getRoad()) {
                tempPath.add(0);
            }
            myPaths.add(tempPath);
        }
    }

    void complexTurn(World game) {
        Log.d(TAG, "HeavyTurn Called" + " Turn:" + game.getCurrentTurn());
        if (firstPlayer == 1) {
            int sum = 0;
            for (int i = 0; i < myPaths.get(0).size(); i++) {
                sum += myPaths.get(0).get(i);
            }
        }
    }

    private int whatToDo(World game) { // Decide whether to attack(1) , defence(-1) or save money (0)
        if (firstPlayer == 1) {
            return -1; // Defence for now //TODO
        } else {
            return 1;
        }
    }

    private void simpleDefenceTurn(World game) {
        int move = makeOrUpgrade(game);
        if (move == 1) {
            ArrayList<Cell> allAvailableCells = getAllAvailableCells(game);

            ArrayList<Cell> worthy = new ArrayList<>();
            for (Cell allAvailableCell : allAvailableCells) {
                if (isTowerWorthy(allAvailableCell, game)) {
                    worthy.add(allAvailableCell);
                }
            }
            int money = game.getMyInformation().getMoney();
            int maxTowersAllowedToMake = 2;
            while (money >= ArcherTower.INITIAL_PRICE && maxTowersAllowedToMake > 0) {
                if (worthy.size() <= 0) {
                    break;
                }
                int rnd = random.nextInt(worthy.size());

                if (money >= CannonTower.INITIAL_PRICE) {
                    money -= CannonTower.INITIAL_PRICE;
                    game.createCannonTower(1, worthy.get(rnd).getLocation().getX(), worthy.get(rnd).getLocation().getY());
                } else {
                    money -= ArcherTower.INITIAL_PRICE;
                    game.createArcherTower(1, worthy.get(rnd).getLocation().getX(), worthy.get(rnd).getLocation().getY());
                }
                maxTowersAllowedToMake--;
                worthy.remove(rnd);
            }
            while (money >= ArcherTower.INITIAL_LEVEL_UP_PRICE && game.getMyTowers().size() > 0) {
                money -= ArcherTower.INITIAL_LEVEL_UP_PRICE;
                int rnd = random.nextInt(game.getMyTowers().size());
                //game.upgradeTower(game.getMyTowers().get(rnd));
            }

        } else if (move == 2) {

        } else {

        }


    }

    private double getPathRisk(World game, Path path) { // TODO: 2/16/2018 Higher level and heavier units cause more risk (must add later)
        double risk = 0;
        int counter = 0;
        for (RoadCell roadCell : path.getRoad()) {
            counter++;
            risk += 1.0 * roadCell.getUnits().size() * (1.0 * counter * RISK_WHEN_AT_END / path.getRoad().size() + 1);
        }
        for (Tower tower : game.getMyTowers()) {
            if (doesTowerAttackPath(tower, path, game)) {
                risk -= ARCHER_TOWER_IMPACT;
            }
        }
        return risk;
    }

    private boolean isValidAndWithinRange(int x, int y, int i, int j, Map map) {
        return Math.abs(i) + Math.abs(j) <= 2 && isPointValid(x + i, y + j, map);
    }

    private boolean doesTowerAttackPath(Tower tower, Path path, World game) {
        int x = tower.getLocation().getX();
        int y = tower.getLocation().getY();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) + Math.abs(j) <= 2) {
                    if (isPointValid(x + i, y + j, game.getDefenceMap())) {
                        for (RoadCell roadCell : path.getRoad()) {
                            if (game.getDefenceMap().getCell(x + i, y + j).getLocation().getX() == roadCell.getLocation().getX() &&
                                    game.getDefenceMap().getCell(x + i, y + j).getLocation().getY() == roadCell.getLocation().getY()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private ArrayList<Cell> getAllAvailableCells(World game) {
        ArrayList<Cell> availableCells = new ArrayList<>();
        ArrayList<Path> paths = game.getDefenceMapPaths();
        for (Path path : paths) {
            ArrayList<RoadCell> road = path.getRoad();
            Map map = game.getDefenceMap();
            for (RoadCell roadCell : road) {
                int x = roadCell.getLocation().getX();
                int y = roadCell.getLocation().getY();
                for (int a = -2; a <= 2; a++) {
                    for (int b = -2; b <= 2; b++) {
                        if (Math.abs(a) + Math.abs(b) <= 2) {
                            if (isPointValid(x + a, y + b, map)) {
                                if (game.isTowerConstructable(map.getCell(x + a, y + b))) {
                                    availableCells.add(map.getCell(x + a, y + b));
                                }
                            }
                        }
                    }
                }
            }
        }
        return availableCells;
    }

    private void complexDefenceTurn(World game) {
        simpleDefenceTurn(game);
    }

    private int makeOrUpgrade(World game) { // Decide whether to Make(1) or Upgrade(2) or Lightning(3)
        return 1; // Make for now //TODO
    }

    private boolean isPointValid(int x, int y, Map map) {
        return x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
    }


    private static final double WEIGHTED_SUM_WEIGHT = 6.0;
    private static final double ENEMIES_TO_SEE_WEIGHT = 3.0;
    private static final double LENGTH_FROM_START_WEIGHT = -2.0;
    private static final int IS_TOWER_WORTHY = 30; // TODO: 2/18/2018 this need tweaking

    private boolean isTowerWorthy(Cell cell, World game) {
        int x = cell.getLocation().getX();
        int y = cell.getLocation().getY();
        int lengthsFromStarts = 0;
        int enemiesLeftToSee = 0;
        int weightedSum = 0;
        for (int i = 0; i < game.getDefenceMapPaths().size(); i++) {
            int temp = 0;
            int enemiesIHaveSeen = 0;
            int lastCellThatSawMe = -1;
            if (doesCellSeePath(cell, game.getDefenceMapPaths().get(i), game)) {
                for (int j = 3; j < game.getDefenceMapPaths().get(i).getRoad().size(); j++) {
                    RoadCell roadCell = game.getDefenceMapPaths().get(i).getRoad().get(j);
                    enemiesIHaveSeen += myPaths.get(i).get(j); // TODO: 2/18/2018 Regular and high level units must differ
                    if (Math.abs(x - roadCell.getLocation().getX()) + Math.abs(y - roadCell.getLocation().getY()) <= 2) {
                        temp = enemiesIHaveSeen;
                        lastCellThatSawMe = i;
                        weightedSum += (Math.abs(x - roadCell.getLocation().getX()) + Math.abs(y - roadCell.getLocation().getY()));
                    }
                }
            }
            enemiesLeftToSee += temp;
            if (lastCellThatSawMe != -1) {
                lengthsFromStarts += lastCellThatSawMe;
            }
        }

        double riskDecrease = 0;
        if (enemiesLeftToSee == 0) {
            return false;
        }
        riskDecrease = weightedSum * WEIGHTED_SUM_WEIGHT;
        riskDecrease += enemiesLeftToSee * ENEMIES_TO_SEE_WEIGHT;
        riskDecrease += lengthsFromStarts * LENGTH_FROM_START_WEIGHT;
        return riskDecrease >= IS_TOWER_WORTHY;
    }

    private boolean doesCellSeePath(Cell cell, Path path, World game) {
        int x = cell.getLocation().getX();
        int y = cell.getLocation().getY();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (isValidAndWithinRange(x, y, i, j, game.getDefenceMap())) {
                    for (RoadCell roadCell : path.getRoad()) {
                        if (game.getDefenceMap().getCell(x + i, y + j).getLocation().getX() == roadCell.getLocation().getX() &&
                                game.getDefenceMap().getCell(x + i, y + j).getLocation().getY() == roadCell.getLocation().getY()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }


}