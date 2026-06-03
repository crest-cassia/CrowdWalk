include Math


#定数の定義
#ベッセル楕円体
$aB = 6377397.155 #長半径
$FB = 299.152813  #逆扁平率
#GRS80楕円体
$aG = 6378137.0  #長半径
$FG = 298.257222101 #逆扁平率
#共通定数
$sbyS = 0.9999 #原点における縮尺係数
$a = 0.0 #長半径
$F = 0.0 #逆扁平率
$e = 0.0 #第一離心率
$phi0 = 0.0 #原点緯度
$lambda0 = 0.0 #原点経度
$bits = ARGV[2] #出力値(0 = 緯度 or X, 1 = 経度 or Y)

#世界測地系で緯度経度を平面直角座標に変換するメソッド
def blxy(phi1, lamda1,k)
   $a = $aG #GRS80楕円体の定数を代入
   $F = $FG
   genten(k)

   bl2xy(phi1, lamda1)

end

#世界測地系で平面直角座標を緯度経度に変換するメソッド
def xybl(phi1,lamda1,k)
   $a = $aG #GRS80楕円体の定数を代入
   $F = $FG
   genten(k)

   xy2bl(phi1, lamda1)
end

#緯度経度を平面直角座標に変換する計算
def bl2xy(phi, lamda)
   $e = sqrt(2.0 * $F - 1.0) / $F

   phi1 = deg2rad(phi)
   lamda1 = deg2rad(lamda)

   s0 = kocyou($phi0)
   s1 = kocyou(phi1)

   ut = $a / sqrt(1.0 - $e ** 2.0 * sin(phi1) ** 2.0)
   conp = cos(phi1)
   t1 = tan(phi1)
   dlamda = lamda1 - $lamda0
   eta2 = ($e ** 2.0 / (1.0 - $e ** 2.0)) * conp ** 2.0

   #X座標値の算出
   v1 = 5.0 - t1 ** 2.0 + 9.0 * eta2 + 4.0 * eta2 ** 2.0
   v2 = -61.0 + 58.0 * t1 ** 2.0 - t1 ** 4.0 - 270.0 * eta2 + 330.0 * t1 ** 2.0 * eta2
   v3 = -1385.0 + 3111.0 * t1 ** 2.0 - 543.0 * t1 ** 4.0 + t1 ** 6.0

   x = ((s1 - s0) + ut * conp ** 2.0 * t1 * dlamda ** 2.0 / 2.0 + ut * conp ** 4.0 * t1 * v1 * dlamda ** 4.0 / 24.0 - ut * conp ** 6.0 * t1 * v2 * dlamda ** 6.0 / 720.0 - ut * conp ** 8.0 * t1 * v3 * dlamda ** 8.0 / 40320.0) * $sbyS

   #Y座標値の算出
   v1 = -1.0 + t1 ** 2.0 - eta2
   v2 = -5.0 + 18.0 * t1 ** 2.0 - t1 ** 4.0 - 14.0 * eta2 + 58.0 * t1 ** 2.0 * eta2
   v3 = -61.0 + 479.0 * t1 ** 2.0 - 179.0 * t1 ** 4.0 + t1 ** 6.0

   y = (ut * conp * dlamda - ut * conp ** 3.0 * v1 * dlamda ** 3.0 / 6.0 - ut * conp ** 5.0 * v2 * dlamda ** 5.0 / 120.0 - ut * conp ** 7.0 * v3 * dlamda ** 7.0 / 5040.0) * $sbyS

   return [x,y]

end

#平面直角座標から緯度経度に変換する計算
def xy2bl(x,y)
   $e = sqrt(2.0 * $F - 1.0) / $F

   phi1 = suisen(x)

   ut = $a / sqrt( 1.0 - $e ** 2.0 * sin(phi1) ** 2.0)
   conp = cos(phi1)
   t1 = tan(phi1)
   eta2 = ($e ** 2.0 / (1.0 - $e ** 2.0)) * conp ** 2.0

   #緯度算出
   yy = y / $sbyS
   v1 = 1.0 + eta2
   v2 = 5.0 + 3.0 * t1 ** 2.0 + 6.0 * eta2 - 6.0 * t1 ** 2.0 * eta2 - 3.0 * eta2 ** 2.0 - 9.0 * t1 ** 2.0 * eta2 ** 2.0
   v3 = 61.0 + 90.0 * t1 ** 2.0 + 45.0 * t1 ** 4.0 + 107.0 * eta2 - 162.0 * t1 ** 2.0 * eta2 - 45.0 * t1 ** 4.0 * eta2
   v4 = 1385.0 + 3633.0 * t1 ** 2.0 + 4095.0 * t1 ** 4.0 + 1575.0 * t1 ** 6.0

   phir = -(v1 / (2.0 * ut ** 2.0)) * yy ** 2.0
   phir = phir + (v2 / (24.0 * ut ** 4.0)) * yy ** 4.0
   phir = phir - (v3 / (720.0 * ut ** 6.0)) * yy ** 6.0
   phir = phir + (v4 / (40320.0 * ut ** 8.0)) * yy ** 8.0
   phir = phir * t1
   phir = phir + phi1
   phir = rad2deg(phir)

   #経度算出
   v1 = ut * conp
   v2 = 1.0 + 2.0 * t1 ** 2.0 + eta2
   v3 = 5.0 + 28.0 * t1 ** 2.0 + 24.0 * t1 ** 4.0 + 6.0 * eta2 + 8.0 * t1 ** 2.0 * eta2
   v4 = 61.0 + 662.0 * t1 ** 2.0 + 1320.0 * t1 ** 4.0 + 720.0 * t1 ** 6.0

   lamdar = (1.0 / v1) * yy
   lamdar = lamdar - (v2 / (6.0 * ut ** 2.0 * v1)) * yy ** 3.0
   lamdar = lamdar + (v3 / (120.0 * ut ** 4.0 * v1)) * yy ** 5.0
   lamdar = lamdar - (v4 / (5040.0 * ut ** 6.0 * v1)) * yy ** 7.0
   lamdar = lamdar + $lamda0

   lamdar = rad2deg(lamdar)

   return [phir,lamdar]
end

#座標系原点の緯度経度
def genten(k)

   err = "座標系が正しく指定されていません"

   case k
   when 1
      degen = [330000.0,1293000.0]
   when 2
      degen = [330000.0,1310000.0]
   when 3
      degen = [360000.0,1321000.0]
   when 4
      degen = [330000.0,1333000.0]
   when 5
      degen = [360000.0,1342000.0]
   when 6
      degen = [360000.0,1360000.0]
   when 7
      degen = [360000.0,1371000.0]
   when 8
      degen = [360000.0,1383000.0]
   when 9
      degen = [360000.0,1395000.0]
   when 10
      degen = [400000.0,1405000.0]
   when 11
      degen = [440000.0,1401500.0]
   when 12
      degen = [440000.0,1421500.0]
   when 13
      degen = [440000.0,1441500.0]
   when 14
      degen = [260000.0,1420000.0]
   when 15
      degen = [260000.0,1273000.0]
   when 16
      degen = [260000.0,1240000.0]
   when 17
      degen = [260000.0,1310000.0]
   when 18
      degen = [200000.0,1360000.0]
   when 19
      degen = [260000.0,1540000.0]
   else
      degen = err
   end

   unless degen == err
      $phi0 = (deg2rad degen[0])
      $lamda0 = (deg2rad degen[1])
      return [$phi0,$lamda0]
   else
      return err
   end
end



#度分秒からラジアンへの変換
def deg2rad(deg)
   radbase = PI / 180.0
   if deg < 0
      fugou  = -1.0
   else
      fugou = 1.0
   end

   deg = deg.abs

   angle = (deg / 10000.0).to_i
   minute = ((deg / 100.0).to_i - (angle * 100.0)).to_f
   second =  deg - (angle * 10000.0) - (minute * 100.0)
   rad = fugou * (angle + (minute + (second / 60.0)) / 60.0)
   rad = rad * radbase

end

#ラジアンから度分秒への変換
def rad2deg(rad)

   degbase = 180.0 / PI

   if rad < 0
      fugou = -1.0
   else
      fugou = 1.0
   end

   rad = rad.abs

   rad = rad * degbase
   angle = rad.to_i
   rad = (rad - angle).to_f * 60.0
   minute = rad.to_i
   second = (rad - minute).to_f * 60.0

   deg = fugou * (angle * 10000.0 + minute * 100.0 + second)
end

#緯度から赤道への子午線弧長を計算する
def kocyou(ido)
   e2 = $e ** 2.0
   e4 = $e ** 4.0
   e6 = $e ** 6.0
   e8 = $e ** 8.0
   e10 = $e ** 10.0
   e12 = $e ** 12.0
   e14 = $e ** 14.0
   e16 = $e ** 16.0

   a = 1.0 + 3.0 / 4.0 * e2 + 45.0 / 64.0 * e4 + 175.0 / 256.0 * e6 + 11025.0 / 16384.0 * e8 + 43659.0 / 65536.0 * e10 + 693693.0 / 1048576.0 * e12 + 19324305.0 / 29360128.0 * e14 + 4927697775.0 / 7516192768.0 * e16
   b = 3.0 / 4.0 * e2 + 15.0 / 16.0 * e4 + 525.0 / 512.0 * e6 + 2205.0 / 2048.0 * e8 + 72765.0 / 65536.0 * e10 + 297297.0 / 262144.0 * e12 + 135270135.0 / 117440512.0 * e14 + 547521975.0 / 469762048.0 * e16
   c = 15.0 / 64.0 * e4 + 105.0 / 256.0 * e6 + 2205.0 / 4096.0 * e8 + 10395.0 / 16384.0 * e10 + 1486485.0 / 2097152.0 * e12 + 45090045.0 / 58720256.0 * e14 + 766530765.0 / 939524096.0 * e16
   d = 35.0 / 512.0 * e6 + 315.0 / 2048.0 * e8 + 31185.0 / 131072.0 * e10 + 165165.0 / 524288.0 * e12 + 45090045.0 / 117440512.0 * e14 + 209053845.0 / 469762048.0 * e16
   e = 315.0 / 16384.0 * e8 + 3465.0 / 65536.0 * e10 + 99099.0 / 1048576.0 * e12 + 4099095.0 / 29360128.0 * e14 + 348423075.0 / 1879048192.0 * e16
   f = 693.0 / 131072 * e10 + 9009.0 / 524288.0 * e12 + 4099095.0 / 117440512.0 * e14 + 26801775.0 / 469762048.0 * e16
   g = 3003 / 2097152.0 * e12 + 315315.0 / 58720256.0 * e14 + 11486475.0 / 939524096.0 * e16
   h = 45045.0 / 117440512.0 * e14 + 765765.0 / 469762048.0 * e16
   i = 765765.0 / 7516192768.0 * e16

   sigosen = $a * (1.0 - e2) * (a * ido - b * sin(ido * 2.0) / 2.0 + c * sin(ido * 4.0) / 4.0 - d * sin(ido * 6.0) / 6.0 + e * sin(ido * 8.0) / 8.0 - f * sin(ido * 10.0) / 10.0 + g * sin(ido * 12.0) / 12.0 - h * sin(ido * 14.0) / 14.0 + i * sin(ido * 16.0) / 16.0)
end

def suisen(x)
   s0 = kocyou($phi0)
   m = s0 + x / $sbyS
   cnt = 0
   phin = $phi0
   e2 = $e ** 2.0
   phi0 = phin

   while
      cnt = cnt + 1
      phi0 = phin
      sn = kocyou(phin)
      v1 = 2.0 * (sn - m) * ((1.0 - e2 * sin(phin) ** 2.0) ** 1.5)
      v2 = 3.0 * e2 * (sn - m) * sin(phin) * cos(phin) * sqrt(1.0 - e2 * sin(phin) ** 2.0) - 2.0 * $a * (1.0 - e2)
      phin = phin +v1 / v2
      if ((phin - phi0).abs < 0.00000000000001) or cnt > 100
         break
      end
   end
   return phin

end
