package client;

import client.model.*;
import client.model.Map;
import common.util.Log;

import java.awt.*;
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
    private static final int MAX_TOWERS_PER_TURN = 1;
    private static final int MAX_UPGRADES_PER_TURN = 1;
    private static final int MAX_ATTACK_IN_DEFENCE_TURN = 2;
    private static int firstPlayer = 1;


    void simpleTurn(World game) {
        Log.d(TAG, "lightTurn Called" + " Turn:" + game.getCurrentTurn());
        if (firstPlayer == 1 && initialize) {
            init(game);
            initialize = false;
        } else if (firstPlayer == 1) {

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
                int req1 = LightUnit.INITIAL_PRICE;
                int money = game.getMyInformation().getMoney();
                while (true) {
                    if (money < req1) {
                        break;
                    }
                    int rnd = random.nextInt(game.getAttackMapPaths().size());
                    game.createLightUnit(rnd);
                    money -= req1;
                }
            }
        } else if (move == 0) { // Save money //TODO
            return;
        }


    }

    private void init(World game) {

    }

    void complexTurn(World game) {
        Log.d(TAG, "HeavyTurn Called" + " Turn:" + game.getCurrentTurn());
    }

    private int whatToDo(World game) { // Decide whether to attack(1) , defence(-1) or save money (0)
        if (firstPlayer == 1) {
            if (game.getCurrentTurn() % 2 == 0) {
                return -1; // Defence for now //TODO
            } else {
                return 1;
            }
        } else {
            return 1;
        }
    }

    private void simpleDefenceTurn(World game) {
        int move = makeOrUpgrade(game);
        if (move == 1) {

            ArrayList<Cell> worthy = cellsSortedByRisk(game);
            ArrayList<Tower> worthyTowers = towersSortedByRisk(game);


            int money = game.getMyInformation().getMoney();
            int built = 0;
            int whichOne;
            while (money >= ArcherTower.INITIAL_PRICE && built <= MAX_TOWERS_PER_TURN) {
                if (worthy.size() <= 0) {
                    break;
                }
                whichOne = worthy.size() - 1;
                while (money >= CannonTower.INITIAL_PRICE && built <= MAX_TOWERS_PER_TURN && whichOne >= 0) {
                    if (howManyCellsDoISee(worthy.get(whichOne), game) > 2) {
                        money -= CannonTower.INITIAL_PRICE;
                        game.createCannonTower(3, worthy.get(whichOne).getLocation().getX(), worthy.get(whichOne).getLocation().getY());
                        worthy.remove(whichOne);
                        built++;
                    }
                    whichOne--;

                }
                while (money >= ArcherTower.INITIAL_PRICE && built <= MAX_TOWERS_PER_TURN && whichOne >= 0) {
                    if (howManyCellsDoISee(worthy.get(whichOne), game) <= 2) {
                        money -= ArcherTower.INITIAL_PRICE;
                        game.createArcherTower(1, worthy.get(whichOne).getLocation().getX(), worthy.get(whichOne).getLocation().getY());
                        worthy.remove(whichOne);
                        built++;
                    }
                    whichOne--;

                }
            }
            built = 0;
            while (money >= ArcherTower.INITIAL_LEVEL_UP_PRICE && worthyTowers.size() > 0 && built <= MAX_UPGRADES_PER_TURN) {
                game.upgradeTower(worthyTowers.get(worthyTowers.size() - 1));
                worthyTowers.remove(worthyTowers.size() - 1);
                built++;
            }
            built = 0;
            while (money >= LightUnit.INITIAL_PRICE && built <= MAX_ATTACK_IN_DEFENCE_TURN) {
                if(money >= HeavyUnit.INITIAL_PRICE){
                    money -= HeavyUnit.INITIAL_PRICE;
                    game.createHeavyUnit(0);
                }else {
                    money -= LightUnit.INITIAL_PRICE;
                    game.createLightUnit(0);
                }
                built++;
            }
        } else if (move == 2) {

        } else {

        }


    }

    private ArrayList<Cell> cellsSortedByRisk(World game) {
        ArrayList<Cell> allAvailableCells = getAllAvailableCells(game);
        ArrayList<Cell> worthy = new ArrayList<>();
        ArrayList<Double> availableWorths = new ArrayList<>();
        for (Cell allAvailableCell : allAvailableCells) {
            double worth = isTowerWorthy(allAvailableCell, game);
            if (worth >= IS_TOWER_WORTHY) {
                worthy.add(allAvailableCell);
                availableWorths.add(worth);
            }
        }

        for (int i = 0; i < worthy.size(); i++) {
            for (int j = i + 1; j < worthy.size(); j++) {
                if (availableWorths.get(i) > availableWorths.get(j)) {
                    double temp = availableWorths.get(i);
                    availableWorths.set(i, availableWorths.get(j));
                    availableWorths.set(j, temp);
                    Cell cell = worthy.get(i);
                    worthy.set(i, worthy.get(j));
                    worthy.set(j, cell);
                }
            }
        }
        return worthy;
    }

    private ArrayList<Tower> towersSortedByRisk(World game) {
        ArrayList<Tower> worthyTowers = new ArrayList<>();
        ArrayList<Double> worths = new ArrayList<>();
        for (Tower tower : game.getMyTowers()) {
            Cell cell = game.getDefenceMap().getCell(tower.getLocation().getX(), tower.getLocation().getY());
            double worth = isTowerWorthy(cell, game);
            if (worth >= IS_TOWER_WORTHY) {
                worthyTowers.add(tower);
                worths.add(worth);
            }
        }
        for (int i = 0; i < worthyTowers.size(); i++) {
            for (int j = i + 1; j < worthyTowers.size(); j++) {
                if (worths.get(i) > worths.get(j)) {
                    Tower tower = worthyTowers.get(i);
                    worthyTowers.set(i, worthyTowers.get(j));
                    worthyTowers.set(j, tower);
                    double worth = worths.get(i);
                    worths.set(i, worths.get(j));
                    worths.set(j, worth);
                }
            }
        }
        return worthyTowers;
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
        int turn = game.getCurrentTurn() % 10;
        if (game.getMyInformation().getStormsLeft() > 0) {
            HashMap<RoadCell, Integer> lastones = new HashMap<>();
            int crucial = 0;
            for (Path path : game.getDefenceMapPaths()) {
                RoadCell roadCell = path.getRoad().get(path.getRoad().size() - 1);
                if (roadCell.getUnits().size() > 0) {
                    if (!lastones.containsKey(roadCell)) {
                        lastones.put(roadCell, roadCell.getUnits().size());
                    } else {
                        lastones.put(roadCell, lastones.get(roadCell) + roadCell.getUnits().size());
                    }
                }
            }
            RoadCell maxOne = null;
            int max = 0;
            for (RoadCell roadCell : lastones.keySet()) {
                if (lastones.get(roadCell) > max) {
                    maxOne = roadCell;
                    max = lastones.get(roadCell);
                }
            }
            if (maxOne != null && max >= 5) {
                game.createStorm(maxOne.getLocation().getX(), maxOne.getLocation().getY());
            }
        }
        return 1; // Make for now //TODO
    }

    private boolean isPointValid(int x, int y, Map map) {
        return x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
    }


    private static final double WEIGHTED_SUM_WEIGHT = 3.0;
    private static final double ENEMIES_TO_SEE_WEIGHT = 8.0;
    private static final double LENGTH_FROM_START_WEIGHT = -2.0;
    private static final int IS_TOWER_WORTHY = 30; // TODO: 2/18/2018 this need tweaking

    private double isTowerWorthy(Cell cell, World game) {
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
                for (int j = 1; j < game.getDefenceMapPaths().get(i).getRoad().size(); j++) {
                    RoadCell roadCell = game.getDefenceMapPaths().get(i).getRoad().get(j);
                    enemiesIHaveSeen += game.getDefenceMapPaths().get(i).getRoad().get(j).getUnits().size(); // TODO: 2/18/2018 Regular and high level units must differ
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
        int check = 0;
        for (int i = -2; i <= 2 ; i++){
            for (int j = -2; j < 3; j++) {
                if(isValidAndWithinRange(cell.getLocation().getX(), cell.getLocation().getY(), i, j, game.getDefenceMap())){
                    Cell cell1 = game.getDefenceMap().getCell(cell.getLocation().getX() + i, cell.getLocation().getY() + j);
                    if(cell1 instanceof RoadCell){
                        check += ((RoadCell) cell1).getUnits().size();
                    }
                }
            }
        }
        if(check == 0){
            return 0;
        }
        double riskDecrease = 0;
        if (enemiesLeftToSee == 0) {
            return 0;
        }
        riskDecrease = weightedSum * WEIGHTED_SUM_WEIGHT;
        riskDecrease += enemiesLeftToSee * ENEMIES_TO_SEE_WEIGHT;
        riskDecrease += lengthsFromStarts * LENGTH_FROM_START_WEIGHT;
        return riskDecrease;
    }

    private int howManyCellsDoISee(Cell cell, World game) {
        int seen = 0 ;
        int x = cell.getLocation().getX(), y = cell.getLocation().getY();
        for (Cell doiseeyou: game.getDefenceMap().getCellsList()){
            if(doiseeyou instanceof RoadCell){
                int a = doiseeyou.getLocation().getX();
                int b = doiseeyou.getLocation().getY();
                if(Math.abs(x - a) + Math.abs(y - b) <= 2){
                    seen++;
                }
            }
        }
        return seen;
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

    private boolean arePathsIdentical(Path path1, Path path2) {
        if (path1.getRoad().size() != path2.getRoad().size()) {
            return false;
        }
        int size = path1.getRoad().size();

        for (int i = 0; i < size / 2; i++) {
            if (path1.getRoad().get(i).getLocation().getX() == path2.getRoad().get(i).getLocation().getX() &&
                    path1.getRoad().get(i).getLocation().getY() == path2.getRoad().get(i).getLocation().getY()) {

            } else {
                return false;
            }
        }
        return true;

    }


}