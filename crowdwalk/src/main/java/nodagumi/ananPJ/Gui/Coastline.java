package nodagumi.ananPJ.Gui;

import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.misc.JsonicHashMapGetter;
import nodagumi.Itk.*;

/**
 * 国土地理院の海岸線データを使って海面領域を表すポリゴンを作成する
 */
public class Coastline extends JsonicHashMapGetter {
    /**
     * 閉じた海岸線(島)の座標リスト
     */
    private ArrayList<ArrayList<Point2D>> islands = new ArrayList();

    /**
     * 閉じていない海岸線の先頭座標をキーとするハッシュテーブル
     */
    private HashMap<String, ArrayList<Point2D>> linesFirstReference = new HashMap();

    /**
     * 閉じていない海岸線の末尾座標をキーとするハッシュテーブル
     */
    private HashMap<String, ArrayList<Point2D>> linesLastReference = new HashMap();

    /**
     * 海岸線の座標リスト
     */
    private ArrayList<ArrayList<Point2D>> outerBoundaries = new ArrayList();

    /**
     * 交差する辺の情報を持つ座標リスト
     */
    private class DividingLine<T> extends ArrayList {
        public int entranceSide;
        public int exitSide;

        /**
         * get(int index) のオーバーライド
         */
        public Point2D get(int index) {
            return (Point2D)super.get(index);
        }

        /**
         * 開始座標を返す
         */
        public Point2D getEntrancePoint() {
            if (isEmpty()) {
                return null;
            }
            return get(0);
        }

        /**
         * 終端座標を返す
         */
        public Point2D getExitPoint() {
            if (isEmpty()) {
                return null;
            }
            return get(size() - 1);
        }

        /**
         * 開始座標の X 値または Y 値を返す
         */
        public Double getEntranceValue() {
            if (isEmpty()) {
                return 0.0;
            }
            Point2D point = get(0);
            return Double.valueOf(entranceSide % 2 == 0 ? point.getX() : point.getY());
        }

        /**
         * 終端座標の X 値または Y 値を返す
         */
        public Double getExitValue() {
            if (isEmpty()) {
                return 0.0;
            }
            Point2D point = get(size() - 1);
            return Double.valueOf(exitSide % 2 == 0 ? point.getX() : point.getY());
        }
    }

    /**
     * コンストラクタ
     */
    public Coastline() {}

    /**
     * 海岸線データの GeoJSON ファイル(平面直角座標系)を読み込んで座標データを取り出す
     */
    public void read(String filePath, Rectangle2D boundary) throws Exception {
        Itk.logInfo("Read coastline file", filePath);
        setParameters(JSON.decode(new FileInputStream(filePath)));
        ArrayList<HashMap> features = (ArrayList<HashMap>)getArrayListParameter("features", null);
        if (features == null) {
            throw new Exception("File parsing error - \"" + filePath + "\" : \"features\" elements are missing.");
        }

        for (HashMap feature : features) {
            setParameters(feature);
            HashMap geometry = getHashMapParameter("geometry", null);
            if (geometry == null) {
                throw new Exception("File parsing error - \"" + filePath + "\" : \"geometry\" elements are missing.");
            }
            setParameters(geometry);
            ArrayList<ArrayList<BigDecimal>> coordinates = (ArrayList<ArrayList<BigDecimal>>)getArrayListParameter("coordinates", null);
            if (coordinates == null) {
                throw new Exception("File parsing error - \"" + filePath + "\" : \"coordinates\" elements are missing.");
            }
            ArrayList<Point2D> coords = new ArrayList();
            for (ArrayList<BigDecimal> coordinate : coordinates) {
                if (coordinate.size() != 2) {
                    throw new Exception("File parsing error - \"" + filePath + "\" : invalid coordinates: " + coordinate);
                }
                double east = coordinate.get(0).doubleValue();
                double north = coordinate.get(1).doubleValue();
                // CrowdWalk 座標系に変換して格納
                coords.add(new Point2D.Double(east, -north));
            }
            if (! coords.isEmpty()) {
                if (coords.size() == 1) {
                    throw new Exception("File parsing error - \"" + filePath + "\" : invalid size of coordinates: " + coords);
                }
                appendCoordinates(coords);
            }
        }
    }

    /**
     * 海岸線データを閉じたものと閉じていないものに分類して保存する.
     *
     * @param _coordinates 繋がった海岸線の座標リスト
     */
    private void appendCoordinates(ArrayList<Point2D> _coordinates) throws Exception {
        ArrayList<Point2D> coordinates = (ArrayList<Point2D>)_coordinates.clone();
        int lastIndex = coordinates.size() - 1;
        Point2D firstPoint = coordinates.get(0);
        Point2D lastPoint = coordinates.get(lastIndex);

        if (firstPoint.equals(lastPoint)) {
            // 閉じた海岸線(島)
            islands.add(eliminateRedundancy(coordinates));
        } else {
            ArrayList<Point2D> lineFirstReference = linesFirstReference.get(lastPoint.toString());
            ArrayList<Point2D> lineLastReference = linesLastReference.get(firstPoint.toString());

            if (lineFirstReference != null && lineLastReference != null) {
                // coordinates を間に挿入して line と line を接続し、閉じた場合には islands に移動する
                if (lineFirstReference == lineLastReference) {
                    // 閉じた海岸線(島)だった
                    lineLastReference.addAll(coordinates);
                    islands.add(eliminateRedundancy(lineLastReference));
                    linesFirstReference.remove(lastPoint.toString());
                    linesLastReference.remove(firstPoint.toString());
                } else {
                    coordinates.remove(lastIndex);
                    coordinates.remove(0);
                    // lineLastReference = lineLastReference + coordinates + lineFirstReference
                    lineLastReference.addAll(coordinates);
                    lineLastReference.addAll(lineFirstReference);

                    // lineFirstReference を削除
                    linesFirstReference.remove(lineFirstReference.get(0).toString());
                    linesLastReference.remove(lineFirstReference.get(lineFirstReference.size() - 1).toString());

                    // lineLastReference の linesLastReference 側を新しい末尾で更新
                    linesLastReference.remove(firstPoint.toString());
                    linesLastReference.put(lineLastReference.get(lineLastReference.size() - 1).toString(), lineLastReference);
                }
            } else if (lineFirstReference != null) {
                // coordinates を line の前部に挿入する
                coordinates.remove(lastIndex);
                lineFirstReference.addAll(0, coordinates);  // 前方に伸ばす。終端点は変わらない
                linesFirstReference.remove(lastPoint.toString());
                linesFirstReference.put(firstPoint.toString(), lineFirstReference);
            } else if (lineLastReference != null) {
                // coordinates を line の後部に追加する
                coordinates.remove(0);
                lineLastReference.addAll(coordinates);      // 後方に伸ばす。開始点は変わらない
                linesLastReference.remove(firstPoint.toString());
                linesLastReference.put(lastPoint.toString(), lineLastReference);
            } else {
                if (linesFirstReference.containsKey(firstPoint.toString())) {
                    Itk.logWarn("Duplicated coastline coordinate was found(firstPoint)", "(" + firstPoint.getX() + ", " + firstPoint.getY() + ")");
                    if (linesLastReference.containsKey(lastPoint.toString())) {
                        Itk.logWarn("Duplicated coastline coordinate was found(lastPoint)", "(" + lastPoint.getX() + ", " + lastPoint.getY() + ")");
                    }
                    return;
                }
                if (linesLastReference.containsKey(lastPoint.toString())) {
                    Itk.logWarn("Duplicated coastline coordinate was found(lastPoint)", "(" + lastPoint.getX() + ", " + lastPoint.getY() + ")");
                    return;
                }
                linesFirstReference.put(firstPoint.toString(), coordinates);
                linesLastReference.put(lastPoint.toString(), coordinates);
            }
        }
    }

    /**
     * 海岸線の必要な範囲を切り取ってポリゴン化する.
     *
     * @param boundary ポリゴン化する矩形範囲
     * @param upperLeftIsOnTheSea boundary の左上点は海上か?
     */
    public void polygonization(Rectangle2D boundary, boolean upperLeftIsOnTheSea) {
        Line2D[] boundaryLines = new Line2D[4];     // { 上, 右, 下, 左 } 座標は右回りを描く
        boundaryLines[0] = new Line2D.Double(boundary.getX(), boundary.getY(), boundary.getMaxX(), boundary.getY());
        boundaryLines[1] = new Line2D.Double(boundary.getMaxX(), boundary.getY(), boundary.getMaxX(), boundary.getMaxY());
        boundaryLines[2] = new Line2D.Double(boundary.getMaxX(), boundary.getMaxY(), boundary.getX(), boundary.getMaxY());
        boundaryLines[3] = new Line2D.Double(boundary.getX(), boundary.getMaxY(), boundary.getX(), boundary.getY());
        Area boundaryArea = new Area(boundary);
        ArrayList<ArrayList<Point2D>> coastlines = new ArrayList(linesFirstReference.values());

        // boundary 内部に完全に収まっていない島の扱い
        for (int index = islands.size() - 1; index >= 0; index--) {
            ArrayList<Point2D> coordinates = islands.get(index);
            Area original = new Area(pointListToPath2D(coordinates));
            Area clippedIsland = (Area)original.clone();
            clippedIsland.intersect(boundaryArea);
            // boundary の外側にある島は削除する
            if (clippedIsland.isEmpty()) {
                islands.remove(index);
            }
            // boundary からはみ出す島は、始点を boundary の外に移動して閉じていない海岸線として扱う
            else if (! clippedIsland.equals(original)) {
                ArrayList<Point2D> wCoordinates = new ArrayList(coordinates);
                wCoordinates.addAll(coordinates);
                int startIndex = -1;
                for (int idx = 0; idx < wCoordinates.size(); idx++) {
                    if (! boundary.contains(wCoordinates.get(idx)) && boundary.contains(wCoordinates.get(idx + 1))) {
                        startIndex = idx;
                        break;
                    }
                }
                ArrayList<Point2D> _coordinates = new ArrayList();
                for (int idx = 0; idx < coordinates.size() - 1; idx++) {
                    _coordinates.add(wCoordinates.get(startIndex + idx));
                }
                islands.remove(index);
                coastlines.add(_coordinates);
            }
        }

        if (coastlines.isEmpty()) {
            // 四方を海に囲まれて、boundary の四辺上に島が存在していない
            if (upperLeftIsOnTheSea) {
                ArrayList<Point2D> outerBoundary = new ArrayList();
                outerBoundary.add(new Point2D.Double(boundary.getX(), boundary.getY()));
                outerBoundary.add(new Point2D.Double(boundary.getMaxX(), boundary.getY()));
                outerBoundary.add(new Point2D.Double(boundary.getMaxX(), boundary.getMaxY()));
                outerBoundaries.add(outerBoundary);
            }
            // 完全に陸上なので海面は存在しない
            else {
                // 島が存在していたら削除する(念のため)
                if (! islands.isEmpty()) {
                    islands.clear();
                }
            }
            return;
        }

        // 閉じていない海岸線を、boundary を分割する線に分解する
        // TreeMap を使っているのでラインの始点が boundary の辺の端点に近い順にソートされる
        TreeMap<Double, DividingLine<Point2D>>[] dividingLines = new TreeMap[4];
        for (int side = 0; side < 4; side++) {
            dividingLines[side] = new TreeMap();
        }
        for (ArrayList<Point2D> coastline : coastlines) {
            int lastIndex = coastline.size() - 1;
            boolean inside = false;
            DividingLine<Point2D> dividingLine = new DividingLine();
            // ラインセグメントごとに処理する
            Point2D firstPoint = null;
            for (Point2D lastPoint : coastline) {
                if (firstPoint == null) {
                    firstPoint = lastPoint;
                    continue;
                }

                // ラインセグメントが完全に boundary の内部にある
                if (inside && boundary.contains(lastPoint)) {
                    dividingLine.add(lastPoint);
                    firstPoint = lastPoint;
                    continue;
                }

                // ラインセグメントが boundary と交差する
                Line2D lineSegment = new Line2D.Double(firstPoint, lastPoint);
                for (int side = 0; side < 4; side++) {
                    if (lineSegment.intersectsLine(boundaryLines[side])) {
                        // 交点
                        Point2D iPoint = intersection(lineSegment, boundaryLines, side);
                        // 内部 -> 外部
                        if (inside) {
                            dividingLine.add(iPoint);
                            dividingLine.exitSide = side;
                            dividingLines[side].put(dividingLine.getExitValue(), dividingLine);
                            dividingLine = new DividingLine<Point2D>();
                            inside = false;
                        }
                        // 外部 -> 内部
                        else if (boundary.contains(lastPoint)) {
                            dividingLine.add(iPoint);
                            dividingLine.add(lastPoint);
                            dividingLine.entranceSide = side;
                            dividingLines[side].put(dividingLine.getEntranceValue(), dividingLine);
                            inside = true;
                        }
                        // 外部 -> 内部 -> 外部
                        else {
                            Point2D otherPoint = null;
                            int otherSide = side + 1;
                            while (otherSide < 4) {
                                if (lineSegment.intersectsLine(boundaryLines[otherSide])) {
                                    otherPoint = intersection(lineSegment, boundaryLines, otherSide);
                                    break;
                                }
                                otherSide++;
                            }
                            if (firstPoint.distance(iPoint) < firstPoint.distance(otherPoint)) {
                                dividingLine.add(iPoint);
                                dividingLine.add(otherPoint);
                                dividingLine.entranceSide = side;
                                dividingLine.exitSide = otherSide;
                            } else {
                                dividingLine.add(otherPoint);
                                dividingLine.add(iPoint);
                                dividingLine.entranceSide = otherSide;
                                dividingLine.exitSide = side;
                            }
                            dividingLines[dividingLine.entranceSide].put(dividingLine.getEntranceValue(), dividingLine);
                            dividingLines[dividingLine.exitSide].put(dividingLine.getExitValue(), dividingLine);
                            dividingLine = new DividingLine<Point2D>();
                        }
                        break;
                    }
                }
                firstPoint = lastPoint;
            }
        }

        ArrayList<Point2D> outerBoundary = makeOuterBoundary(boundaryLines, upperLeftIsOnTheSea, dividingLines);
        while (! outerBoundary.isEmpty()) {
            int length = outerBoundary.size();
            if (length > 1 && outerBoundary.get(0).equals(outerBoundary.get(length - 1))) {
                outerBoundary.remove(length - 1);
            }
            outerBoundaries.add(outerBoundary);
            // 二回目以降の呼び出しでは必ず陸地からスタートする
            outerBoundary = makeOuterBoundary(boundaryLines, false, dividingLines);
        }
    }

    /**
     * boundary を分割する線に分解された海岸線を元に outerBoundary を作る.
     *
     * boundary の辺を右回りに辿りながら、見つかった海面領域を右回りに囲んでいく
     * outerBoundary に使用された海岸線は dividingLines から削除される
     */
    private ArrayList<Point2D> makeOuterBoundary(Line2D[] boundaryLines, boolean startFromTheSea, TreeMap<Double, DividingLine<Point2D>>[] dividingLines) {
        ArrayList<Point2D> outerBoundary = new ArrayList<Point2D>();
        int side = 0;

        int totalDividingLines = 0;
        for (int _side = 0; _side < 4; _side++) {
            totalDividingLines += dividingLines[_side].size();
        }
        if (totalDividingLines <= 1) {
            return outerBoundary;
        }

        // 左上点が海上なので outerBoundary の始点とする
        if (startFromTheSea) {
            outerBoundary.add(new Point2D.Double(boundaryLines[0].getX1(), boundaryLines[0].getY1()));
        }
        // 陸->海の境界点を見つけて outerBoundary の始点とする
        else {
            Double value = null;
            for (; side < 4; side++) {
                double cornerPoint = (side % 2 == 0 ? boundaryLines[side].getX1() : boundaryLines[side].getY1());
                if (side < 2) {
                    value = dividingLines[side].higherKey(cornerPoint);
                } else {
                    value = dividingLines[side].lowerKey(cornerPoint);
                }
                if (value != null) {
                    break;
                }
            }
            // 海岸線はもう残っていない
            if (value == null) {
                return outerBoundary;
            }

            DividingLine<Point2D> dividingLine = dividingLines[side].get(value);
            Point2D point = dividingLine.get(0);
            if (value.equals(dividingLine.getEntranceValue())) {
                outerBoundary.add(dividingLine.getEntrancePoint());
            } else {
                outerBoundary.add(dividingLine.getExitPoint());
            }
        }
        while (true) {
            Point2D point = outerBoundary.get(outerBoundary.size() - 1);
            // outerBoundary が閉じたら終了
            if (outerBoundary.size() > 1 && point.equals(outerBoundary.get(0))) {
                break;
            }
            // この辺に接する海岸線は残っていない
            if (dividingLines[side].isEmpty()) {
                outerBoundary.add(boundaryLines[side].getP2());
                side = (side + 1) % 4;
                continue;
            }
            // 辺上で最も近くにある開始点(または終端点)を見つける
            Double lastValue = (side % 2 == 0 ? point.getX() : point.getY());
            Double value = null;
            if (side < 2) {
                value = dividingLines[side].higherKey(lastValue);
            } else {
                value = dividingLines[side].lowerKey(lastValue);
            }
            // 見つからなければ次の辺に移る
            if (value == null) {
                outerBoundary.add(boundaryLines[side].getP2());
                side = (side + 1) % 4;
                continue;
            }

            // 開始点(または終端点)が boundary の辺上に存在して、辺の端点に最も近い海岸線
            DividingLine<Point2D> dividingLine = dividingLines[side].get(value);
            Double entranceValue = dividingLine.getEntranceValue();
            Double exitValue = dividingLine.getExitValue();
            // 終端点で見つけた海岸線だったら座標を逆順に並べ替える
            if (value.equals(exitValue)) {
                Collections.reverse(dividingLine);
            }
            point = dividingLine.get(0);
            // その海岸線を outerBoundary につなげる
            outerBoundary.addAll(dividingLine);
            // つなげた海岸線を元の map から削除して、その海岸線の反対側の辺に移る
            dividingLines[side].remove(value);
            if (value.equals(entranceValue)) {
                side = dividingLine.exitSide;
                dividingLines[side].remove(exitValue);
            } else {
                side = dividingLine.entranceSide;
                dividingLines[side].remove(entranceValue);
            }
        }

        return outerBoundary;
    }

    /**
     * 座標リストから Path2D を作成する.
     *
     * 先頭と末尾の座標が同じならば末尾の座標は除外する
     */
    public static Path2D pointListToPath2D(ArrayList<Point2D> coordinates) {
        int length = coordinates.size();
        if (length > 1 && coordinates.get(0).equals(coordinates.get(length - 1))) {
            length--;
        }
        Path2D path = new Path2D.Double();
        for (int index = 0; index < length; index++) {
            Point2D point = coordinates.get(index);
            if (path.getCurrentPoint() == null) {
                path.moveTo(point.getX(), point.getY());
            } else {
                path.lineTo(point.getX(), point.getY());
            }
        }
        path.closePath();
        return path;
    }

    /**
     * 島のリストを取得する
     */
    public ArrayList<ArrayList<Point2D>> getIslands() {
        return islands;
    }

    /**
     * 海岸線のリストを取得する
     */
    public ArrayList<ArrayList<Point2D>> getOuterBoundaries() {
        return outerBoundaries;
    }

    /**
     * outerBoundary の内側に存在する閉じた海岸線(島)のリストを取得する
     */
    public ArrayList<ArrayList<Point2D>> getInnerBoundaries(ArrayList<Point2D> outerBoundary) {
        ArrayList<ArrayList<Point2D>> innerBoundaries = new ArrayList();
        Area outerArea = new Area(pointListToPath2D(outerBoundary));

        for (ArrayList<Point2D> island : islands) {
            Area islandArea = new Area(pointListToPath2D(island));
            Area _islandArea = (Area)islandArea.clone();
            _islandArea.intersect(outerArea);
            if (_islandArea.equals(islandArea)) {
                innerBoundaries.add(island);
            }
        }
        return innerBoundaries;
    }

    /**
     * 先頭と末尾の座標が同じならば末尾の座標を削除する
     */
    public static ArrayList<Point2D> eliminateRedundancy(ArrayList<Point2D> coordinates) {
        int length = coordinates.size();
        if (length > 1 && coordinates.get(0).equals(coordinates.get(length - 1))) {
            coordinates.remove(length - 1);
        }
        return coordinates;
    }

    /**
     * line と boundary の交点座標を求める
     */
    public static Point2D intersection(Line2D line, Line2D[] boundaryLines, int side) {
        Point2D point = intersection(line, boundaryLines[side]);
        if (side == 0 || side == 2) {
            return new Point2D.Double(point.getX(), boundaryLines[side].getY1());
        } else {
            return new Point2D.Double(boundaryLines[side].getX1(), point.getY());
        }
    }

    /**
     * 交点座標を求める
     */
    public static Point2D intersection(Line2D l1, Line2D l2) {
        double f1 = l1.getX2() - l1.getX1();
        double g1 = l1.getY2() - l1.getY1();
        double f2 = l2.getX2() - l2.getX1();
        double g2 = l2.getY2() - l2.getY1();
        double dx = l2.getX1() - l1.getX1();
        double dy = l2.getY1() - l1.getY1();
        double t1 = (f2 * dy - g2 * dx) / (f2 * g1 - f1 * g2);
        return new Point2D.Double(l1.getX1() + f1 * t1, l1.getY1() + g1 * t1);
    }
}
