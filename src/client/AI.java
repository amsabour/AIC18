package client;

import client.model.*;
import client.model.Map;
import common.util.Log;

import java.awt.*;
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
    private static int MAX_TOWERS_PER_TURN;
    private static final int MAX_UPGRADES_PER_TURN = 1;
    private static final int MAX_ATTACK_IN_DEFENCE_TURN = 2;
    private static int firstPlayer = 1;
    private static final int MAX_UNITS_SENT = 30;


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
                int rnd1 = random.nextInt(game.getAttackMapPaths().size());
                int rnd2 = random.nextInt(2);
                rnd1 = 0;
                if (rnd2 == 0) {
                    game.createLightUnit(rnd1);
                    money -= LightUnit.INITIAL_PRICE;
                } else {
                    if (money >= HeavyUnit.INITIAL_PRICE) {
                        game.createHeavyUnit(rnd1);
                        money -= HeavyUnit.INITIAL_PRICE;
                    }
                }

            }
            return;
        }
        int move = whatToDo(game);
        if (move == -1) { // Defence
            simpleDefenceTurn(game);
        } else if (move == 1) { // Attack //TODO
            simpleAttackTurn(game);
        } else if (move == 0) { // Save money //TODO
            return;
        }


    }

    private void simpleAttackTurn(World game) {
        int worstCase = -1;
        int counter = -1;
        for (int i = 0; i < game.getAttackMapPaths().size(); i++) {
            Path path = game.getAttackMapPaths().get(i);
            int ct = 0;
            for (Tower tower : game.getVisibleEnemyTowers()) {
                Cell cell = game.getAttackMap().getCell(tower.getLocation().getX(), tower.getLocation().getY());
                if (doesTowerAttackPath(tower, path, game.getAttackMap())) {
                    ct++;
                }
            }
            if(ct > counter){
                worstCase = i;
                counter = ct;
            }
        }
        if(worstCase != -1) {
            int money = game.getMyInformation().getMoney();
            while (money > HeavyUnit.INITIAL_PRICE) {
                money -= HeavyUnit.INITIAL_PRICE;
                game.createHeavyUnit(worstCase);
            }
            while (money > LightUnit.INITIAL_PRICE){
                money -= LightUnit.INITIAL_PRICE;
                game.createLightUnit(worstCase);
            }
        }
    }

    private void init(World game) {


    }

    void complexTurn(World game) {
        Log.d(TAG, "HeavyTurn Called" + " Turn:" + game.getCurrentTurn());
        if (game.getCurrentTurn() == 10) {
            MAX_TOWERS_PER_TURN = game.getDefenceMapPaths().size();
            ArrayList<Path> paths = new ArrayList<>();
            for (Path path : game.getDefenceMapPaths()) {
                int sum = 0;
                for (int i = 1; i < path.getRoad().size(); i++) {
                    sum += path.getRoad().get(i).getUnits().size();
                }
                if (sum > 0) {
                    paths.add(path);
                }
            }
            ArrayList<RoadCell> firstOnes = new ArrayList<>();
            for (Path path : game.getDefenceMapPaths()) {
                if (paths.contains(path)) {
                    RoadCell roadCell = path.getRoad().get(0);
                    if (!firstOnes.contains(roadCell)) {
                        firstOnes.add(roadCell);
                    }
                }
            }
            ArrayList<Cell> attackers = new ArrayList<>();
            for (RoadCell roadCell : firstOnes) {
                int x = roadCell.getLocation().getX(), y = roadCell.getLocation().getY();
                for (int i = -2; i <= 2; i++) {
                    for (int j = -2; j < 3; j++) {
                        if (isValidAndWithinRange(x, y, i, j, game.getDefenceMap())) {
                            Cell cell = game.getDefenceMap().getCell(x + i, y + j);
                            if (game.isTowerConstructable(cell)) {
                                if (!attackers.contains(cell)) {
                                    attackers.add(cell);
                                }
                            }
                        }
                    }
                }
            }

            for (Cell cell : attackers) {
                game.createCannonTower(1, cell.getLocation().getX(), cell.getLocation().getY());

            }
        }
    }

    private int whatToDo(World game) { // Decide whether to attack(1) , defence(-1) or save money (0)
        if (firstPlayer == 1) {
            int turn = game.getCurrentTurn() % 10;
            if (turn == 7 || turn == 8 ||turn == 9) {
                return 1; // Defence for now //TODO
            } else {
                return -1;
            }
        } else {
            return 1;
        }
    }

    private void simpleDefenceTurn(World game) {
        int move = makeOrUpgrade(game);
        if (move == 1) {

            ArrayList<Cell> worthy = cellsSortedByRisk(game);

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
                        game.createCannonTower(1, worthy.get(whichOne).getLocation().getX(), worthy.get(whichOne).getLocation().getY());
                        worthy.remove(whichOne);
                        built++;
                    }
                    whichOne--;

                }
                while (money >= ArcherTower.INITIAL_PRICE && built <= MAX_TOWERS_PER_TURN && whichOne >= 0) {
                    if (howManyCellsDoISee(worthy.get(whichOne), game) == 1) {
                        money -= ArcherTower.INITIAL_PRICE;
                        game.createArcherTower(1, worthy.get(whichOne).getLocation().getX(), worthy.get(whichOne).getLocation().getY());
                        worthy.remove(whichOne);
                        built++;
                    }
                    whichOne--;

                }
            }

            built = 0;
            while (money >= LightUnit.INITIAL_PRICE && built <= MAX_ATTACK_IN_DEFENCE_TURN) {
                money -= LightUnit.INITIAL_PRICE;
                int rnd = random.nextInt(game.getAttackMapPaths().size());
                game.createLightUnit(rnd);
                built++;
            }
        } else if (move == 2) {
            ArrayList<Tower> worthyTowers = towersSortedByRisk(game);
            int built = 0;
            int money = game.getMyInformation().getMoney();
            while (money >= ArcherTower.INITIAL_LEVEL_UP_PRICE && worthyTowers.size() > 0 && built <= MAX_UPGRADES_PER_TURN) {
                game.upgradeTower(worthyTowers.get(worthyTowers.size() - 1));
                worthyTowers.remove(worthyTowers.size() - 1);
                built++;
            }
        } else {

        }


    }

    private ArrayList<Cell> cellsSortedByRisk(World game) {
        ArrayList<Cell> allAvailableCells = getAllAvailableCells(game);
        ArrayList<Cell> worthy = new ArrayList<>();
        ArrayList<int[]> availableWorths = new ArrayList<>();
        for (Cell cell : allAvailableCells) {
            int[] result = isTowerWorthy(cell, game);
            if (result[1] != 0) {
                worthy.add(cell);
                availableWorths.add(result);
            }
        }

        for (int i = 0; i < worthy.size(); i++) {
            for (int j = i + 1; j < worthy.size(); j++) {
                if (decideBetweenCells(availableWorths.get(i), availableWorths.get(j))) {
                    int[] temp = availableWorths.get(i);
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

    private boolean decideBetweenCells(int[] firstCell, int[] secondCell) {
        if (firstCell[1] != secondCell[1]) {
            return firstCell[1] > secondCell[1];
        } else {
            if (firstCell[2] != secondCell[2]) {
                return firstCell[2] < secondCell[2];
            } else {
                if (firstCell[4] != secondCell[4]) {
                    return firstCell[4] > secondCell[4];
                } else {
                    if (firstCell[0] != secondCell[0]) {
                        return firstCell[0] < secondCell[0];
                    } else {
                        return firstCell[3] != secondCell[3] && firstCell[3] > secondCell[3];
                    }
                }
            }
        }
    }


    private ArrayList<Tower> towersSortedByRisk(World game) {
        ArrayList<Tower> worthyTowers = new ArrayList<>();
        ArrayList<Double> worths = new ArrayList<>();
        for (Tower tower : game.getMyTowers()) {
            double worth = shouldIUpgradeTower(tower, game);
            if (worth >= REQUIRED_FOR_UPGRADE) {
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

    private boolean isValidAndWithinRange(int x, int y, int i, int j, Map map) {
        return Math.abs(i) + Math.abs(j) <= 2 && isPointValid(x + i, y + j, map);
    }

    private boolean doesTowerAttackPath(Tower tower, Path path, Map map) {
        int x = tower.getLocation().getX();
        int y = tower.getLocation().getY();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) + Math.abs(j) <= 2) {
                    if (isPointValid(x + i, y + j, map)) {
                        for (RoadCell roadCell : path.getRoad()) {
                            if (map.getCell(x + i, y + j).getLocation().getX() == roadCell.getLocation().getX() &&
                                    map.getCell(x + i, y + j).getLocation().getY() == roadCell.getLocation().getY()) {
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

    private int makeOrUpgrade(World game) { // Decide whether to Make(1) or Upgrade(2) or Lightning(3
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
        if(game.getMyInformation().getBeansLeft() > 0){
            Tower destroy = null;
            int level = 0;
            int price = 0;
            for (Tower tower: game.getVisibleEnemyTowers() ){
                if(tower.getLevel() > level){
                    if(tower.getPrice() > price){
                        destroy = tower;
                        price = tower.getPrice();
                        level = tower.getLevel();
                    }
                }else if (tower.getLevel() == level){
                    if(tower.getPrice() > price){
                        if(tower.getPrice() > price){
                            destroy = tower;
                            price = tower.getPrice();
                            level = tower.getLevel();
                        }
                    }
                }
            }
            if(level >= 5){
                game.plantBean(destroy.getLocation().getX(), destroy.getLocation().getY());
            }
        }
        if (turn == 1 || turn == 2 || turn == 3) {
            return 2;
        } else if (turn == 4 || turn == 5 || turn == 6 || turn == 7) {
            return 1;
        }

        return 1; // Make for now //TODO
    }

    private boolean isPointValid(int x, int y, Map map) {
        return x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
    }


    private static final double WEIGHTED_SUM_WEIGHT = 3.0;
    private static final double ENEMIES_TO_SEE_WEIGHT = 5.0;
    private static final double LENGTH_FROM_START_WEIGHT = -4.0;
    private static final int IS_TOWER_WORTHY = 30; // TODO: 2/18/2018 this need tweaking

    private int[] isTowerWorthy(Cell cell, World game) {
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
        int enemiesISee = 0;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (isValidAndWithinRange(x, y, i, j, game.getDefenceMap())) {
                    Cell cell1 = game.getDefenceMap().getCell(x + i, y + j);
                    if (cell1 instanceof RoadCell) {
                        enemiesISee += ((RoadCell) cell1).getUnits().size();
                    }
                }
            }
        }
        int good = 1;
        outer:
        for (Path path : game.getDefenceMapPaths()) {
            if (doesCellSeePath(cell, path, game)) {
                int seen = 0;
                for (RoadCell roadCell : path.getRoad()) {
                    int a = roadCell.getLocation().getX(), b = roadCell.getLocation().getY();
                    if (Math.abs(x - a) + Math.abs(y - b) <= 2) {
                        if (roadCell.getUnits().size() > 0) {
                            if (seen == 0) {
                                seen = 1;
                            } else {
                                good = 0;
                                break outer;
                            }
                        }
                    }
                }
            }
        }

        return new int[]{enemiesISee, enemiesLeftToSee, lengthsFromStarts, weightedSum, good};
    }

    private int howManyCellsDoISee(Cell cell, World game) {
        int seen = 0;
        int x = cell.getLocation().getX(), y = cell.getLocation().getY();
        for (Cell doiseeyou : game.getDefenceMap().getCellsList()) {
            if (doiseeyou instanceof RoadCell) {
                int a = doiseeyou.getLocation().getX();
                int b = doiseeyou.getLocation().getY();
                if (Math.abs(x - a) + Math.abs(y - b) <= 2) {
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


    private static final double UNITS_WEIGHT_FOR_UPGRADE = 4.0;
    private static final double LENGTH_FROM_START_FOR_UPGRADE = -2.0;
    private static final double REQUIRED_FOR_UPGRADE = 20.0;

    private double shouldIUpgradeTower(Tower tower, World game) {
        int x = tower.getLocation().getX(), y = tower.getLocation().getY();
        Cell cell = game.getDefenceMap().getCell(x, y);
        int unitsSeen = 0;
        int lengthFromStarts = 0;
        for (Path path : game.getDefenceMapPaths()) {
            int length = 0;
            int counter = 0;
            if (doesCellSeePath(cell, path, game)) {
                for (RoadCell roadCell : path.getRoad()) {
                    counter++;
                    int a = roadCell.getLocation().getX(), b = roadCell.getLocation().getY();
                    if (Math.abs(x - a) + Math.abs(y - b) <= 2) {
                        unitsSeen += roadCell.getUnits().size();
                        length = counter;
                    }
                }
            }
            lengthFromStarts += length;
        }
        double worth = 0;
        worth += unitsSeen * UNITS_WEIGHT_FOR_UPGRADE;
        worth += lengthFromStarts * LENGTH_FROM_START_FOR_UPGRADE;
        return worth;

    }
}