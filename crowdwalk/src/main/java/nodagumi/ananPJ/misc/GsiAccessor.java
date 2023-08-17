package nodagumi.ananPJ.misc;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import javax.imageio.ImageIO;

import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.CrowdWalkLauncher;
import nodagumi.Itk.*;

/**
 * 国土地理院の Web サービスを利用するためのインターフェイスを提供する
 */
public class GsiAccessor {
    /**
     * 緯度経度から Slippy map のタイル番号を求める
     */
    public static Point getTileNumber(double lat, double lon, int zoom) {
        int xtile = (int)Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int)Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = ((1 << zoom) - 1);
        return new Point(xtile, ytile);
    }

    /**
     * Slippy map のタイル番号から緯度経度とタイルサイズを求める
     */
    public static Rectangle2D tile2boundingBox(int x, int y, int zoom) {
        double north = tile2lat(y, zoom);
        double south = tile2lat(y + 1, zoom);
        double west = tile2lon(x, zoom);
        double east = tile2lon(x + 1, zoom);
        return new java.awt.geom.Rectangle2D.Double(west, north, east - west, north - south);
    }

    private static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    private static double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    /**
     * キャッシュされた地理院タイル画像のファイル名を取得する
     */
    public static String getFileNameOfGsiTileImage(String cachePath, String tileName, int x, int y, int zoom, String ext) {
        String _cachePath = cachePath.replaceFirst("[/\\\\]$", "");
        return String.format("%s/%s/%d/%d/%d.%s", _cachePath, tileName, zoom, x, y, ext);
    }

    /**
     * 地理院タイル画像を cachePath ディレクトリにダウンロードする
     */
    public static boolean downloadGsiTileImage(String cachePath, String tileName, int x, int y, int zoom, String ext) {
        String _cachePath = cachePath.replaceFirst("[/\\\\]$", "");
        String dirPath = String.format("%s/%s/%d/%d", _cachePath, tileName, zoom, x);
        File dir = new File(dirPath);
        if (! dir.exists()) {
            Itk.logInfo("Make directory", dirPath);
            dir.mkdirs();
        }

        String filePath = String.format("%s/%d.%s", dirPath, y, ext);
        File file = new File(filePath);
        if (file.exists()) {
            return true;
        }
        if (CrowdWalkLauncher.offline) {
            return false;
        }
        // インターネット接続を確認して以降の動作に反映
        if (! CrowdWalkLauncher.internetEnabled) {
            if (CrowdWalkLauncher.isInternetEnabled()) {
                CrowdWalkLauncher.internetEnabled = true;
            } else {
                CrowdWalkLauncher.offline = true;
                return false;
            }
        }

        String url = String.format("https://cyberjapandata.gsi.go.jp/xyz/%s/%d/%d/%d.%s", tileName, zoom, x, y, ext);
        Itk.logInfo("Download", url);
        try {
            BufferedImage image = ImageIO.read(new URL(url));
            Itk.logInfo("Save", filePath);
            ImageIO.write(image, ext, file);
        } catch (IOException e) {
            Itk.dumpStackTraceOf(e) ;
            return false;
        }
        try {
            // サーバーに負荷を掛けすぎてアクセス拒否されないために毎秒2回とする
            Thread.sleep(500);
        } catch(InterruptedException e) {}
        return true;
    }

    /**
     * 地理院タイルの左上点の標高値を取得する
     */
    public static double getElevation(String cachePath, int x, int y, int zoom) {
        String _cachePath = cachePath.replaceFirst("[/\\\\]$", "");
        String filePath = String.format("%s/elevation_%d_%d_%d.json", _cachePath, zoom, x, y);
        File file = new File(filePath);
        if (! file.exists()) {
            if (CrowdWalkLauncher.offline) {
                return 0.0;
            }
            // インターネット接続を確認して以降の動作に反映
            if (! CrowdWalkLauncher.internetEnabled) {
                if (CrowdWalkLauncher.isInternetEnabled()) {
                    CrowdWalkLauncher.internetEnabled = true;
                } else {
                    CrowdWalkLauncher.offline = true;
                    return 0.0;
                }
            }
            // 国土地理院Webの標高APIにアクセスして標高値を取得し、キャッシュファイルに保存する
            Rectangle2D tileRect = tile2boundingBox(x, y, zoom);
            String url = String.format("http://cyberjapandata2.gsi.go.jp/general/dem/scripts/getelevation.php?lon=%s&lat=%s&outtype=JSON", tileRect.getX(), tileRect.getY());
            try {
                Itk.logInfo("Access to", url);
                Files.copy(new URL(url).openStream(), file.toPath());
            } catch (IOException e) {
                Itk.logError("IOException", e.getMessage());
                return 0.0;
            }
        }
        String elevation = "";
        try {
            Itk.logInfo("Read elevation file", filePath);
            HashMap<String, Object> json = JSON.decode(new FileInputStream(filePath));
            elevation = json.get("elevation").toString();
            return Double.valueOf(elevation).doubleValue();
        } catch (Exception e) {
            Itk.logError(e.getMessage(), elevation);
        }
        return 0.0;
    }
}
