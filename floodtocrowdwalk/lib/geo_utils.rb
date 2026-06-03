include Math

# Radian
RAD = PI / 180.0

# 日本測地系における赤道半径、扁平率、第一離心率
TOKYO_DATUM_A = 6377397.155
TOKYO_DATUM_F = 1.0 / 299.152813
TOKYO_DATUM_E2 = TOKYO_DATUM_F * (2.0 - TOKYO_DATUM_F)

# 世界測地系における赤道半径、扁平率、第一離心率
WGS84_A = 6378137.0
WGS84_F = 1.0 / 298.257223
WGS84_E2 = WGS84_F * (2.0 - WGS84_F)

# 日本測地系から世界測地系に変換する際の並行移動量[m]
DX = -148.0
DY = 507.0
DZ = 681.0

# 楕円体座標から直行座標へ
def llh_to_xyz(b, l, h, a, e2)
  b *= RAD
  l *= RAD
  rn = a / sqrt(1 - e2 * (sin(b) ** 2.0))

  x = (rn + h) * cos(b) * cos(l)
  y = (rn + h) * cos(b) * sin(l)
  z = (rn * (1 - e2) + h) * sin(b)

  [x, y, z]
end

# 直行座標から楕円体座標へ
def xyz_to_llh(x, y, z, a, e2)
  bda = sqrt(1 - e2)

  p = sqrt(x * x + y * y)
  t = atan2(z, p * bda)

  b = atan2(z + e2 * a / bda * (sin(t) ** 3.0), p - e2 * a * (cos(t) ** 3.0))
  l = atan2(y, x)
  h = p / cos(b) - a / sqrt(1.0 - e2 * (sin(b) ** 2.0))

  [l / RAD, b / RAD, h]
end

# 日本測地系 -> 世界測地系に経緯度変換
def convert_japan_to_world(x, y, z)     # (longitude, latitude, height)
  rcs_x, rcs_y, rcs_z = llh_to_xyz(y, x, z, TOKYO_DATUM_A, TOKYO_DATUM_E2)
  xyz_to_llh(rcs_x + DX, rcs_y + DY, rcs_z + DZ, WGS84_A, WGS84_E2)
end
