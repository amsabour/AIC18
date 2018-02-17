package client;

import client.model.*;
import client.model.Map;
import client.model.Point;
import common.util.Log;

import javax.swing.text.MutableAttributeSet;
import java.awt.*;
import java.text.CollationElementIterator;
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
    private static int firstPlayer =  1;

    void simpleTurn(World game) {
        Log.d(TAG, "lightTurn Called" + " Turn:" + game.getCurrentTurn());
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
                    int rnd = random.nextInt(game.getDefenceMapPaths().size());
                    game.createLightUnit(rnd);
                    money -= req;
                }
            } else {
                System.out.println("Why Doesnt this bitch send any troops!!!!!!!!!!!!");
                int money = game.getMyInformation().getMoney();
                while (money >= 500){
                    int pathToAttack = random.nextInt(game.getAttackMapPaths().size());
                    game.createLightUnit(pathToAttack);
                    money -= LightUnit.INITIAL_PRICE;
                }
            }
        } else if (move == 0) { // Save money //TODO
            return;
        }


    }

    void complexTurn(World game) {
        Log.d(TAG, "HeavyTurn Called" + " Turn:" + game.getCurrentTurn());
        int move = whatToDo(game);
        if (move == -1) { // Defence
            complexDefenceTurn(game);
        } else if (move == 1) {// Attack // TODO
            return;
        } else if (move == 0) {// Save money // TODO
            return;
        }
    }

    private int whatToDo(World game) { // Decide whether to attack(1) , defence(-1) or save money (0)
        if (firstPlayer == 1) {
            if (game.getCurrentTurn() % 3 == 0) {
                System.out.println("Attack Now!");
                return 1;
            }
            return -1; // Defence for now //TODO
        }else{
            return 1;
        }
    }

    private void simpleDefenceTurn(World game) {
        int move = makeOrUpgrade(game);
        if (move == 1) {

            Path riskiestPath = null;
            double highestRisk = Double.MIN_VALUE;
            for (Path path : game.getDefenceMapPaths()) {
                double risk = getPathRisk(game, path);
                if (risk > highestRisk) {
                    riskiestPath = path;
                    highestRisk = risk;
                }
            }

            ArrayList<Cell> riskDecreasingCells = new ArrayList<Cell>();
            Cell bestCellForTower = null;
            int bestCellRiskDecrease = 0;

            assert riskiestPath != null;
            int enemiesSoFar = 0;
            for (RoadCell roadCell : riskiestPath.getRoad()) {
                enemiesSoFar += roadCell.getUnits().size();
                int x = roadCell.getLocation().getX(), y = roadCell.getLocation().getY();
                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j <= 2; j++) {
                        if (isValidAndWithinRange(x, y, i, j, game.getDefenceMap())) {
                            if (game.isTowerConstructable(game.getDefenceMap().getCell(x + i, y + j))) {
                                if (bestCellRiskDecrease < enemiesSoFar) {
                                    bestCellForTower = game.getDefenceMap().getCell(x + i, y + j);
                                    bestCellRiskDecrease = enemiesSoFar;
                                }
                                riskDecreasingCells.add(game.getDefenceMap().getCell(x + i, y + j));
                            }
                        }
                    }
                }
            }
            assert riskDecreasingCells.size() > 0;
            if (bestCellForTower == null) {
//                System.out.println("Fuck");
                return;
            }
            game.createArcherTower(1, bestCellForTower.getLocation().getX(), bestCellForTower.getLocation().getY()); // TODO: 2/16/2018 This shouldn't be random some cells decrease risk more than others

        } else if (move == 2) {

        } else {

        }


    }

    private double getPathRisk(World game, Path path) { // TODO: 2/16/2018 Higher level and heavier units cause more risk (must add later)
        double risk = 0;
        int counter = 0;
        for (RoadCell roadCell : path.getRoad()) {
            counter++;
            risk += 1.0 * roadCell.getUnits().size() * (1.0 * counter * 30 / path.getRoad().size() + 1);
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
                            if (game.getDefenceMap().getCell(x + i, y + j).getLocation() == roadCell.getLocation()) {
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

    int getValue(Cell cell) {
        return 1;
    }
}