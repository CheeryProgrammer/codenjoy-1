package com.codenjoy.dojo.minesweeper.client.ai.logic;

import com.codenjoy.dojo.services.Dice;
import com.codenjoy.dojo.services.Point;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.codenjoy.dojo.services.PointImpl.pt;

public class Field {
    public final int amount;
    public final int width;
    public final int height;
    private final Cell[][] field;
    private final List<Cell> cells;
    double minPossibility;
    private Point myCoord;
    private PlayField playField;
    private final List<Group> groups;
    private final List<Island> islands;
    private final List<Cell> toOpen;
    private final List<Cell> toMark;
    private final Dice dice;

    public Field(PlayField playField, Dice dice) {
        this(playField.width, playField.height, playField.amount, dice);
        this.playField = playField;
        scanPlayField();
    }

    public Field(int width, int height, int amount1, Dice dice) {
        this.dice = dice;
        groups = new ArrayList();
        toOpen = new ArrayList();
        toMark = new ArrayList();
        amount = amount1;
        this.width = width;
        this.height = height;
        cells = new LinkedList();
        islands = new ArrayList();
        field = new Cell[width][height];
        createCells();
        setCellsNeighbours();
    }

    public void setMyCoord(Point myCoord) {
        this.myCoord = myCoord;
    }


    private void createCells() {
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                field[x][y] = new Cell(x, y);
                cells.add(field[x][y]);
            }
        }

    }

    private void setCellsNeighbours() {
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                if (x > 0) {
                    field[x][y].addNeighbour(field[x - 1][y]);
                }

                if (y > 0) {
                    field[x][y].addNeighbour(field[x][y - 1]);
                }

                if (x > 0 && y > 0) {
                    field[x][y].addNeighbour(field[x - 1][y - 1]);
                }

                if (x < width - 1) {
                    field[x][y].addNeighbour(field[x + 1][y]);
                }

                if (y < height - 1) {
                    field[x][y].addNeighbour(field[x][y + 1]);
                }

                if (x < width - 1 && y < height - 1) {
                    field[x][y].addNeighbour(field[x + 1][y + 1]);
                }

                if (x > 0 && y < height - 1) {
                    field[x][y].addNeighbour(field[x - 1][y + 1]);
                }

                if (x < width - 1 && y > 0) {
                    field[x][y].addNeighbour(field[x + 1][y - 1]);
                }
            }
        }

    }

    private void scanPlayField() {

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                int value = playField.get(x, y);
                if (value != 10 && value != 12) {
                    if (value == 9) {
                        field[x][y].setUnknown();
                    } else if (value == 11) {
                        field[x][y].setMine();
                    } else {
                        field[x][y].setValue(value);
                    }
                }
            }
        }

    }

    private void setGroups() {
        groups.clear();
        Iterator i$ = cells.iterator();

        while (i$.hasNext()) {
            Cell cell = (Cell) i$.next();
            if (cell.isValued() && cell.hasUnknownAround()) {
                groups.add(new Group(cell.getUnknownCells(), cell.getValue()));
            }
        }

    }

    private void optimizeIslands() {
        Iterator i$ = islands.iterator();

        while (i$.hasNext()) {
            Island island = (Island) i$.next();
            island.optimize();
        }

    }

    private void divideGroupsToIslands(List<Group> groups) {
        islands.clear();
        Iterator i$ = groups.iterator();

        while (i$.hasNext()) {
            Group group = (Group) i$.next();
            boolean added = false;
            Island addedTo = null;

            for (int i = 0; i < islands.size(); ++i) {
                Island currentIsland = islands.get(i);
                if (currentIsland.isCross(group)) {
                    if (!added) {
                        currentIsland.add(group);
                        added = true;
                        addedTo = currentIsland;
                    } else {
                        addedTo.add(currentIsland);
                        islands.remove(i);
                    }
                }
            }

            if (!added) {
                islands.add(new Island(group));
            }
        }

    }

    public void play() {
        islands.clear();
        setGroups();
        divideGroupsToIslands(groups);
        optimizeIslands();
        determineMarkOpenIndefinite();
        filterReachableCells(toMark);
        filterReachableCells(toOpen);
        if (!hasDecision()) {
            Iterator i$ = islands.iterator();

            while (i$.hasNext()) {
                Island island = (Island) i$.next();
                island.resolve();
            }

            List<Cell> deepCells = getDeepCells();
            setPossibility(deepCells, 100.0D);
            List<Cell> minPosCells = getMinPosCells();
            minPossibility = minPosCells.size() == 0 ? 100.0D : minPosCells.get(0).getPossibility();
            toOpen.addAll(minPosCells);

//            toOpen = getUnknownCells();
//            filterReachableCells(toOpen);
        }

    }

    private void filterReachableCells(List<Cell> cells) {
        for (int i = 0; i < cells.size(); ++i) {
            if (!isReachableCell(cells.get(i))) {
                cells.remove(i--);
            }
        }

    }

    private boolean isReachableCell(Cell cell) {
        int x = cell.getX();
        int y = cell.getY();
        if (x > 0 && !field[x - 1][y].isUnknown()) {
            return true;
        } else if (y > 0 && !field[x][y - 1].isUnknown()) {
            return true;
        } else if (x < width - 1 && !field[x + 1][y].isUnknown()) {
            return true;
        } else {
            return y < height - 1 && !field[x][y + 1].isUnknown();
        }
    }

    public Point[] getToOpen() {
        Point[] result = new Point[toOpen.size()];

        for (int i = 0; i < toOpen.size(); ++i) {
            result[i] = pt(toOpen.get(i).getX(), toOpen.get(i).getY());
        }

        return result;
    }

    public Point[] getToMark() {
        Point[] result = new Point[toMark.size()];

        for (int i = 0; i < toMark.size(); ++i) {
            result[i] = pt(toMark.get(i).getX(), toMark.get(i).getY());
        }

        return result;
    }

    private List<Cell> getMinPosCells() {
        List<Cell> result = new ArrayList();
        double min = 100.0D;
        Iterator i$ = cells.iterator();

        while (true) {
            Cell cell;
            do {
                do {
                    do {
                        if (!i$.hasNext()) {
                            return result;
                        }

                        cell = (Cell) i$.next();
                    } while (!cell.isUnknown());
                } while (!isReachableCell(cell));
            } while (cell.getCoords().getKey() == myCoord.getX() && cell.getCoords().getValue() == myCoord.getY());

            if (cell.getPossibility() == min) {
                result.add(cell);
            } else if (cell.getPossibility() < min) {
                min = cell.getPossibility();
                result.clear();
                result.add(cell);
            }
        }
    }

    private void setPossibility(List<Cell> list, double possibility) {
        Iterator i$ = list.iterator();

        while (i$.hasNext()) {
            Cell cell = (Cell) i$.next();
            cell.setPossibility(possibility);
        }

    }

    private List<Cell> getUnknownCells() {
        List<Cell> res = new ArrayList();
        Iterator i$ = cells.iterator();

        while (i$.hasNext()) {
            Cell cell = (Cell) i$.next();
            if (cell.isUnknown()) {
                res.add(cell);
            }
        }

        return res;
    }

    private List<Cell> getDeepCells() {
        List<Cell> unknown = getUnknownCells();
        Iterator i$ = islands.iterator();

        while (i$.hasNext()) {
            Island island = (Island) i$.next();
            unknown.removeAll(island.getIndefiniteCells());
        }

        return unknown;
    }

    private void determineMarkOpenIndefinite() {
        Iterator i$ = islands.iterator();

        while (i$.hasNext()) {
            Island island = (Island) i$.next();
            toOpen.addAll(island.getToOpen());
            toMark.addAll(island.getToMark());
        }

    }

    private boolean hasDecision() {
        return toMark.size() > 0 || toOpen.size() > 0;
    }

    public void print() {
        System.out.println(fieldToString());
    }

    public String fieldToString() {
        StringBuilder result = new StringBuilder("0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9\n");

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                Cell cell = field[x][y];
                if (cell.isMine()) {
                    result.append("* ");
                } else if (cell.isUnknown()) {
                    result.append("  ");
                } else {
                    result.append(cell.getValue()).append(" ");
                }
            }

            result.append(y).append('\n');
        }

        result.append("0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9\n");
        result.append('\n');
        return result.toString();
    }

    public double getMinPossibility() {
        return minPossibility;
    }
}