package client;

import client.model.*;
import client.model.Map;
import client.model.Point;
import com.sun.jdi.IntegerType;
import common.util.Log;
import javafx.util.Pair;

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
    private ArrayList<Cell> roadCells;
    private static final int RISK_WHEN_AT_END = 50;
    private boolean initialize = true;
    private static final int MINIMUM_RISK_DECREASE_FOR_TOWER = 50; // TODO: 2/18/2018 this need tweaking
    private static final int RISK_DECREASE_BY_BEING_IN_RANGE = 3; // TODO: 2/18/2018 this needs tweaking
    private static Pair<Integer, Integer> zero = new Pair<>(0, 0);


    private static int firstPlayer = 0;

    void simpleTurn(World game) {
        Log.d(TAG, "lightTurn Called" + " Turn:" + game.getCurrentTurn());
        if (firstPlayer == 1 && initialize) {
            init(game);
            initialize = false;
        }
        if(firstPlayer == 0){
            int money = game.getMyInformation().getMoney();
            while (money >= LightUnit.INITIAL_PRICE){
                game.createLightUnit(random.nextInt(game.getAttackMapPaths().size()));
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
            } else {
                System.out.println("Why Doesnt this bitch send any troops!!!!!!!!!!!!");
                int money = game.getMyInformation().getMoney();
                while (money >= 500) {
                    int pathToAttack = random.nextInt(game.getAttackMapPaths().size());
                    game.createLightUnit(pathToAttack);
                    money -= LightUnit.INITIAL_PRICE;
                }
            }
        } else if (move == 0) { // Save money //TODO
            return;
        }


    }

    private void init(World game) {
        for (Path path : game.getDefenceMapPaths()) {
            roadCells.addAll(path.getRoad());
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
//            if (game.getCurrentTurn() % 3 == 0) {
//                System.out.println("Attack Now!"); todo Next stage here!!!
//                return 1;
//            }
            return -1; // Defence for now //TODO
        } else {
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

            ArrayList<Cell> worthyTowers = new ArrayList<Cell>();
            java.util.Map<Cell, Pair<Integer, Integer>> cellRisks = new HashMap<Cell, Pair<Integer, Integer>>();
            for (Cell cell : game.getDefenceMap().getCellsList()) {
                cellRisks.put(cell, zero);
            }

            for (Path path : game.getDefenceMapPaths()) {
                int enemiesSoFar = 0;
                for (RoadCell cell : path.getRoad()) {
                    enemiesSoFar += cell.getUnits().size(); // TODO: 2/18/2018 We assume that all units are the same
                    int x = cell.getLocation().getX(), y = cell.getLocation().getY();
                    for (int i = -2; i <= 2; i++) {
                        for (int j = -2; j <= 2; j++) {
                            if (isValidAndWithinRange(x, y, i, j, game.getDefenceMap())) {
                                if (game.isTowerConstructable(game.getDefenceMap().getCell(x + i, y + j))) {
                                    Cell goodCell = game.getDefenceMap().getCell(x + i, y + j);
                                    Pair<Integer, Integer> pair = new Pair<>(cellRisks.get(goodCell).getKey() + RISK_DECREASE_BY_BEING_IN_RANGE, enemiesSoFar);
                                    cellRisks.put(goodCell, pair);
                                }
                            }
                        }
                    }
                }
            }

            for (Cell cell : game.getDefenceMap().getCellsList()) {
                int cellRisk = cellRisks.get(cell).getKey() + cellRisks.get(cell).getValue();
                if(cellRisk >= MINIMUM_RISK_DECREASE_FOR_TOWER){
                    game.createArcherTower(1, cell.getLocation().getX(), cell.getLocation().getY());
                }
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