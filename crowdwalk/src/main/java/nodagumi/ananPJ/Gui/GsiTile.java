package nodagumi.ananPJ.Gui;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;

import nodagumi.ananPJ.misc.GsiAccessor;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.Itk.*;

/**
 * 地理院タイル
 */
public class GsiTile {
    /**
     * 標準地図のデータID
     */
    public static String DATA_ID_STD = "std";

    /**
     * 淡色地図のデータID
     */
    public static String DATA_ID_PALE = "pale";

    /**
     * English のデータID
     */
    public static String DATA_ID_ENGLISH = "english";

    /**
     * 数値地図25000（土地条件）のデータID
     */
    public static String DATA_ID_LCM25K_2012 = "lcm25k_2012";

    /**
     * 白地図のデータID
     */
    public static String DATA_ID_BLANK = "blank";

    /**
     * 色別標高図のデータID
     */
    public static String DATA_ID_RELIEF = "relief";

    /**
     * 写真のデータID
     */
    public static String DATA_ID_ORT = "ort";

    /**
     * 地理院タイルの画像サイズ(画像オブジェクトのサイズとは異なる)
     */
    public static int GSI_TILE_SIZE = 256;

    /**
     * JGD2000 の平面直角座標系の系番号に対応する空間座標系コード
     */
    public static String[] JGD2000_JPR_EPSG_NAMES = {
        null,
        "EPSG:2443",    // I
        "EPSG:2444",    // II
        "EPSG:2445",    // III
        "EPSG:2446",    // IV
        "EPSG:2447",    // V
        "EPSG:2448",    // VI
        "EPSG:2449",    // VII
        "EPSG:2450",    // VIII
        "EPSG:2451",    // IX
        "EPSG:2452",    // X
        "EPSG:2453",    // XI
        "EPSG:2454",    // XII
        "EPSG:2455",    // XIII
        "EPSG:2456",    // XIV
        "EPSG:2457",    // XV
        "EPSG:2458",    // XVI
        "EPSG:2459",    // XVII
        "EPSG:2460",    // XVIII
        "EPSG:2461"     // XIX
    };

    /**
     * タイル番号 X
     */
    private int tileNumberX;

    /**
     * タイル番号 Y
     */
    private int tileNumberY;

    /**
     * 画像ファイルパス
     */
    private String filePath;

    /**
     * 画像オブジェクト
     */
    private BufferedImage image;

    /**
     * 表示座標(左上)
     */
    private Point2D point;

    /**
     * 右上座標
     */
    private Point2D upperRightPoint;

    /**
     * 左下座標
     */
    private Point2D lowerLeftPoint;

    /**
     * 右下座標
     */
    private Point2D lowerRightPoint;

    /**
     * X方向の表示スケール
     */
    private double scaleX;

    /**
     * Y方向の表示スケール
     */
    private double scaleY;

    public GsiTile() {}

    /**
     * タイルの種類に該当する拡張子を返す
     */
    public static String getExt(String tileName) {
        // 写真のみ jpg
        if (tileName.equals(DATA_ID_ORT)) {
            return "jpg";
        }
        return "png";
    }

    /**
     * 正しいゾーンか?
     */
    public static boolean isCorrectZone(int zone) {
        return zone >= 1 && zone <= JGD2000_JPR_EPSG_NAMES.length - 1;
    }

    /**
     * 地理院タイル画像を必要ならダウンロードして読み込む
     */
    public boolean loadImage(String cachePath, String tileName, int x, int y, int zoom, int zone) {
        String ext = getExt(tileName);

        if (! GsiAccessor.downloadGsiTileImage(cachePath, tileName, x, y, zoom, ext)) {
            return false;
        }

        filePath = GsiAccessor.getFileNameOfGsiTileImage(cachePath, tileName, x, y, zoom, ext);
        try {
            BufferedImage tileImage = ImageIO.read(new FileInputStream(filePath));
            // 隣のタイルとの間に隙間が出来るのを防ぐため1ピクセル分コピーして拡張する
            image = new BufferedImage(GSI_TILE_SIZE + 1, GSI_TILE_SIZE + 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.drawImage(tileImage, 1, 1, null);
            g2.drawImage(tileImage, 0, 1, null);
            g2.drawImage(tileImage, 1, 0, null);
            g2.drawImage(tileImage, 0, 0, null);
        } catch (IOException e) {
	    Itk.dumpStackTraceOf(e) ;
            return false;
        }

        // 経緯度(WGS84 地理座標系)を平面直角座標に換算する
        CoordinateTransform transform = createCoordinateTransform("EPSG:4326", JGD2000_JPR_EPSG_NAMES[zone]);

        // 地理院タイルの左上点に当たる CrowdWalk 座標を求める
        Rectangle2D tileRect = GsiAccessor.tile2boundingBox(x, y, zoom);
        Point2D jpr = transformCoordinate(transform, tileRect.getX(), tileRect.getY());
        point = convertJPR2CW(jpr.getY(), jpr.getX());

        // 地理院タイルの右上点に当たる CrowdWalk 座標を求める
        tileRect = GsiAccessor.tile2boundingBox(x + 1, y, zoom);
        jpr = transformCoordinate(transform, tileRect.getX(), tileRect.getY());
        upperRightPoint = convertJPR2CW(jpr.getY(), jpr.getX());

        // 地理院タイルの左下点に当たる CrowdWalk 座標を求める
        tileRect = GsiAccessor.tile2boundingBox(x, y + 1, zoom);
        jpr = transformCoordinate(transform, tileRect.getX(), tileRect.getY());
        lowerLeftPoint = convertJPR2CW(jpr.getY(), jpr.getX());

        // 地理院タイルの右下点に当たる CrowdWalk 座標を求める
        tileRect = GsiAccessor.tile2boundingBox(x + 1, y + 1, zoom);
        jpr = transformCoordinate(transform, tileRect.getX(), tileRect.getY());
        lowerRightPoint = convertJPR2CW(jpr.getY(), jpr.getX());

        scaleX = (lowerRightPoint.getX() - point.getX()) / GSI_TILE_SIZE;
        scaleY = (lowerRightPoint.getY() - point.getY()) / GSI_TILE_SIZE;
        tileNumberX = x;
        tileNumberY = y;

        return true;
    }

    /**
     * タイル番号 X を取得する
     */
    public int getTileNumberX() {
        return tileNumberX;
    }

    /**
     * タイル番号 Y を取得する
     */
    public int getTileNumberY() {
        return tileNumberY;
    }

    /**
     * 画像ファイルパスを取得する
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 画像オブジェクトを取得する
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * 表示座標(左上)を取得する
     */
    public Point2D getPoint() {
        return point;
    }

    /**
     * 右上座標を取得する
     */
    public Point2D getUpperRightPoint() {
        return upperRightPoint;
    }

    /**
     * 左下座標を取得する
     */
    public Point2D getLowerLeftPoint() {
        return lowerLeftPoint;
    }

    /**
     * 右下座標を取得する
     */
    public Point2D getLowerRightPoint() {
        return lowerRightPoint;
    }

    /**
     * X方向の表示スケールを取得する
     */
    public double getScaleX() {
        return scaleX;
    }

    /**
     * Y方向の表示スケールを取得する
     */
    public double getScaleY() {
        return scaleY;
    }

    /**
     * 平面直角座標を CrowdWalk 座標に換算する
     */
    public static Point2D convertJPR2CW(double x, double y) {
        return new Point2D.Double(y, -x);
    }

    /**
     * CrowdWalk 座標を平面直角座標に換算する
     */
    public static Point2D convertCW2JPR(double x, double y) {
        return new Point2D.Double(-y, x);
    }

    /**
     * Proj4J の座標変換オブジェクトを生成する
     */
    public static CoordinateTransform createCoordinateTransform(String srcEpsgName, String targetEpsgName) {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem sourceCRS = crsFactory.createFromName(srcEpsgName);
        CoordinateReferenceSystem targetCRS = crsFactory.createFromName(targetEpsgName);
        CoordinateTransformFactory transformFactory = new CoordinateTransformFactory();
        return transformFactory.createTransform(sourceCRS, targetCRS);
    }

    /**
     * 座標変換.
     *
     * ※Proj4J は平面直角座標系のX軸/Y軸ルールなど関知しないため、座標を指定する時は注意
     */
    public static Point2D transformCoordinate(CoordinateTransform transform, double x, double y) {
        ProjCoordinate gcsPoint = new ProjCoordinate(x, y);
        ProjCoordinate pcsPoint = new ProjCoordinate();
        pcsPoint = transform.transform(gcsPoint, pcsPoint);
        return new Point2D.Double(pcsPoint.x, pcsPoint.y);
    }

    /**
     * 座標変換.
     *
     * ※Proj4J は平面直角座標系のX軸/Y軸ルールなど関知しないため、座標を指定する時は注意
     */
    public static Point2D transformCoordinate(CoordinateTransform transform, Point2D point) {
        return transformCoordinate(transform, point.getX(), point.getY());
    }

    /**
     * キャッシュディレクトリのパス文字列を生成する(CrowdWalk/crowdwalk/cache)
     */
    public static String makeCachePath() {
	String fileSeparator = System.getProperty("file.separator");
        File jarFile = new File(System.getProperty("java.class.path"));
        return jarFile.getAbsolutePath().replace(fileSeparator, "/")
            .replaceFirst("(\\.\\/)?build\\/libs\\/.*$", "cache");
    }

    /**
     * マップを覆う範囲の地理院タイルをロードする
     */
    public static ArrayList<GsiTile> loadGsiTiles(NetworkMap networkMap, String tileName, int zoom, int zone) {
        String cachePath = makeCachePath();
        ArrayList<GsiTile> gsiTiles = new ArrayList();

        // マップの外接矩形座標を求める
        Rectangle2D region = networkMap.calcRectangle();
        double north = region.getY();
        double south = north + region.getHeight();
        double west = region.getX();
        double east = west + region.getWidth();
        Itk.logInfo("region(CrowdWalk)", String.format("(%f, %f)-(%f, %f)", west, north, east, south));

        // CrowdWalk 座標を経緯度(WGS84 地理座標系)に換算する
        CoordinateTransform transform = createCoordinateTransform(JGD2000_JPR_EPSG_NAMES[zone], "EPSG:4326");
        Point2D jpr1 = convertCW2JPR(west, north);
        Point2D point1 = transformCoordinate(transform, jpr1.getY(), jpr1.getX());
        Point2D jpr2 = convertCW2JPR(east, south);
        Point2D point2 = transformCoordinate(transform, jpr2.getY(), jpr2.getX());
        north = point1.getY();
        west = point1.getX();
        south = point2.getY();
        east = point2.getX();
        Itk.logInfo("region(longitude,latitude)", String.format("(%f, %f)-(%f, %f)", west, north, east, south));

        Point northWest = GsiAccessor.getTileNumber(north, west, zoom);
        Point southEast = GsiAccessor.getTileNumber(south, east, zoom);
        // 一回り大きな範囲を取得する
        northWest.setLocation(northWest.x - 1, northWest.y - 1);
        southEast.setLocation(southEast.x + 1, southEast.y + 1);
        Itk.logInfo("Load GSI tiles", String.format("(%d, %d)-(%d, %d)", northWest.x, northWest.y, southEast.x, southEast.y));

        // 地理院タイルをロードする
        for (int y = northWest.y; y <= southEast.y; y++) {
            for (int x = northWest.x; x <= southEast.x; x++) {
                GsiTile gsiTile = new GsiTile();
                if (! gsiTile.loadImage(cachePath, tileName, x, y, zoom, zone)) {
                    Itk.logError("Load GSI tile error", String.format("(%d, %d)", x, y));
                    return gsiTiles;
                }
                gsiTiles.add(gsiTile);
            }
        }

        return gsiTiles;
    }
}
