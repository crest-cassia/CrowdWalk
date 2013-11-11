#!/usr/bin/env ruby
# -*- coding: utf-8 -*-


# This library converts a geographic coordinate that consists of a north
# latitude and a east longitude, to a cartesian coordinate that consists of
# a reference geographic coordinate, a x coordinate and a y coordinate.
# The unit of latitude and longitude is decimal degree. The unit of x and y is
# meter.
# Unfortunately this library currently supports Japanese coordinate only.
#
# The algorithms are refered by
# http://vldb.gsi.go.jp/sokuchi/surveycalc/algorithm/


module Geographic

  # default semi-major axis

  # semi-major axis in world geodetic datum
  A_WORLD = 6378137.0
  # semi-major axis in japanese geodetic datum
  A_JAPAN = 6377397.155

  # default flattening

  # flattening in world geodetic datum
  F_WORLD = 1.0 / 298.257222101
  # flattening in japanese geodetic datum
  F_JAPAN = 1.0 / 299.152813

  # references of default latitude and longitude from a number(1-19)
  # http://vldb.gsi.go.jp/sokuchi/patchjgd/download/Help/jpc/jpc.htm
  JAPANESE_COORDINATES = [
    {number: 1, latitude: 33.0, longitude: 129.5},
    {number: 2, latitude: 33.0, longitude: 131.0},
    {number: 3, latitude: 36.0, longitude: 132.16666666},
    {number: 4, latitude: 33.0, longitude: 133.5},
    {number: 5, latitude: 36.0, longitude: 134.33333333},
    {number: 6, latitude: 36.0, longitude: 136.0},
    {number: 7, latitude: 36.0, longitude: 137.16666666},
    {number: 8, latitude: 36.0, longitude: 138.5},
    {number: 9, latitude: 36.0, longitude: 139.83333333},
    {number: 10, latitude: 40.0, longitude: 140.83333333},
    {number: 11, latitude: 44.0, longitude: 140.25},
    {number: 12, latitude: 44.0, longitude: 142.25},
    {number: 13, latitude: 44.0, longitude: 144.25},
    {number: 14, latitude: 26.0, longitude: 142.0},
    {number: 15, latitude: 26.0, longitude: 127.5},
    {number: 16, latitude: 26.0, longitude: 124.0},
    {number: 17, latitude: 26.0, longitude: 131.0},
    {number: 18, latitude: 20.0, longitude: 136.0},
    {number: 19, latitude: 26.0, longitude: 154.0}
  ]

  # currently the threshold of the gap of point scale factor m is 0.0002
  THRESHOLD_POINT_SCALE_FACTOR = 0.0002

  #=== semi-major axis
  #type :: geodetic datum
  def semi_major_axis(type = "world")
    case type
    when "japan" then A_JAPAN
    else A_WORLD
    end
  end

  #=== flattening
  #type :: geodetic datum
  def flattening(type = "world")
    case type
    when "japan" then F_JAPAN
    else F_WORLD
    end
  end

  #=== eccentricity
  #type :: geodetic datum
  def eccentricity(type = "world")
    f = flattening(type)
    Math.sqrt(2.0 * f - f**2)
  end

  #=== second eccentricity
  #type :: geodetic datum
  def second_eccentricity(type = "world")
    f = flattening(type)
    Math.sqrt(2.0 / f - 1) / (1.0 / f - 1)
  end

  #=== meridian arc from Equator to the specified latitude.
  #latitude :: latitude in decimal. 33,30,00 -> 33.5
  #type :: geodetic datum
  def meridian_arc(latitude, type = "world")
    a = semi_major_axis(type)
    e = eccentricity(type)
    la = 1.0 + 3.0 / 4 * e**2 +
      45.0 / 64 * e**4 +
      175.0 / 256 * e**6 +
      11025.0 / 16384 * e**8 +
      43659.0 / 65536 * e**10 +
      693693.0 / 1048576.0 * e**12 +
      193224305.0 / 29360128 * e**14 +
      4927697775.0 / 7516192768 * e**16
    lb = 3.0 / 4 * e**2 +
      15.0 / 16 * e**4 +
      525.0 / 512 * e**6 +
      2205.0 / 2048 * e**8 +
      72765.0 / 65536 * e**10 +
      297297.0 / 262144 * e**12 +
      135270135.0 / 117440512 * e**14 +
      547521975.0 / 469762048 * e**16
    lc = 15.0 / 64 * e**4 +
      105.0 / 256 * e**6 +
      2205.0 / 4096 * e**8 +
      10395.0 / 16384 * e**10 +
      1486485.0 / 2097152 * e**12 +
      45090045.0 / 58720256 * e**14 +
      766530765.0 / 939524096 * e**16
    ld = 35.0 / 512 * e**6 +
      315.0 / 2048 * e**8 +
      31185.0 / 131072 * e**10 +
      165165.0 / 524288 * e**12 +
      45090045.0 / 117440512 * e**14 +
      209053845.0 / 469762048 * e**16
    le = 315.0 / 16384 * e**8 +
      3465.0 / 65536 * e**10 +
      99099.0 / 1048576 * e**12 +
      4099095.0 / 29360128 * e**14 +
      348423075.0 / 1879048192 * e**16
    lf = 693.0 / 131072 * e**10 +
      9009.0 / 524288 * e**12 +
      4099095.0 / 117440512 * e**14 +
      26801775.0 / 469762048 * e** 16
    lg = 3003.0 / 2097152 * e** 12 +
      315315.0 / 58720256 * e**14 +
      11486475.0 / 939524096 * e**16
    lh = 45045.0 / 117440512 * e**14 +
      765765.0 / 469762048 * e**16
    li = 765765.0 / 7516192768 * e**16

    b_1 = a * (1.0 - e**2) * la
    b_2 = a * (1.0 - e**2) * (- lb / 2.0)
    b_3 = a * (1.0 - e**2) * (lc / 4.0)
    b_4 = a * (1.0 - e**2) * (- ld / 6.0)
    b_5 = a * (1.0 - e**2) * (le / 8.0)
    b_6 = a * (1.0 - e**2) * (- lf / 10.0)
    b_7 = a * (1.0 - e**2) * (lg / 12.0)
    b_8 = a * (1.0 - e**2) * (- lh / 14.0)
    b_9 = a * (1.0 - e**2) * (li / 16.0)

    b_1 * latitude + b_2 * Math.sin(2.0 * latitude) +
      b_3 * Math.sin(4.0 * latitude) + b_4 * Math.sin(6.0 * latitude) +
      b_5 * Math.sin(8.0 * latitude) + b_6 * Math.sin(10.0 * latitude) +
      b_7 * Math.sin(12.0 * latitude) + b_8 * Math.sin(14.0 * latitude) +
      b_9 * Math.sin(16.0 * latitude)
  end

  #=== prime vertical curvature
  #latitude :: latitude in decimal. 33,30,00 -> 33.5
  #type :: geodetic datum
  def prime_vertical_curvature(latitude, type = "world")
    a = semi_major_axis(type)
    e = eccentricity(type)
    a / Math.sqrt(1.0 - e**2 * Math.sin(latitude)**2)
  end

  #=== meridian curvature
  #latitude :: latitude in decimal. 33,30,00 -> 33.5
  #type :: geodetic datum
  def meridian_curvature(latitude, type = "world")
    a = semi_major_axis(type)
    e = eccentricity(type)
    a * (1.0 - e**2) / Math.sqrt(1.0 - e**2 * Math.sin(latitude)**2)**3
  end

  #=== convert a decimal degree to a radian.
  #r :: decimal degree
  def to_radian(r)
    r % 180.0 if r >= 180.0 || r <= -180.0
    r * Math::PI / 180.0
  end

  #=== Convert a geographic coordinates to a cartesian coordinate.
  #
  #The function simply converts a geographic coordinates consisted of a latitude
  #and a longitude, to a cartesian coordinate consisted of x and y coordinates
  #from a point of reference.
  #
  #args :: an array of hash. hash includes keys as follows:
  #latitude :: latitude in decimal. 33,30,00 -> 33.5
  #longitude :: longitude in decimal. 134,10,00 -> 134.166666
  #number :: referenced number to default latitude and logitude.
  #default_latitude :: if you want to specify your own.
  #default_longitude :: if you want to specify your own.
  #type :: geodetic datum
  #returned_value :: On success, a hash including x and y coordinates and point
  #scale factor is returned.  #On error, the values: x and y are nil.
  def gtoc(*args)

    hargs = args.inject({}) { |h, a| a.each_pair { |k, v| h[k.to_sym] = v } }
    latitude, longitude, number, default_latitude, default_longitude, type =
      hargs[:latitude], hargs[:longitude], hargs[:number],
      hargs[:default_latitude], hargs[:default_longitude], hargs[:type]

    latitude = to_radian(latitude)
    longitude = to_radian(longitude)

    if !default_latitude.nil? && !default_longitude.nil?
      default_latitude = to_radian(default_latitude)
      default_longitude = to_radian(default_longitude)
    elsif (match = JAPANESE_COORDINATES.select { |i| i[:number] == number })
        .length != 1
      STDERR.puts "invalid default map number is inputted. #{number}"
      STDERR.puts "default map number must be from 1 to 19."
      return {x: nil, y: nil, m: nil}
    else
      default_latitude = to_radian(match[0][:latitude])
      default_longitude = to_radian(match[0][:longitude])
    end

    s = meridian_arc(latitude, type)
    s_0 = meridian_arc(default_latitude, type)
    n = prime_vertical_curvature(latitude, type)
    t = Math.tan(latitude)
    dlambda = longitude - default_longitude
    eta = second_eccentricity ** 2 * Math.cos(latitude)
    m_0 = 0.9999

    x = m_0 * (
      (s - s_0) +
      dlambda**2 * 0.5 * n * Math.cos(latitude)**2 * t +
      dlambda**4 * 1.0 / 24 * n * Math.cos(latitude)**4 * t *
        (5.0 - t**2 + 9.0 * eta**2 + 4.0 * eta**4) +
      dlambda**6 * 1.0 / 720 * n * Math.cos(latitude)**6 * t *
        (61.0 - 58.0 * t**2 + t**4 + 270.0 * eta**2 - 330.0 * t**2 * eta**2) +
      dlambda**8 * 1.0 / 40320 * n * Math.cos(latitude)**8 * t *
        (1385.0 - 3111.0 * t**2 + 543.0 * t**4 - t**6)
    )
    y = m_0 * (
      dlambda * n * Math.cos(latitude) +
      dlambda**3 * 1.0 / 6 * n * Math.cos(latitude)**3 * (1.0 - t**2 + eta**2) +
      dlambda**5 * 1.0 / 120 * n * Math.cos(latitude)**5 *
        (5.0 - 18.0 * t**2 + t**4 + 14.0 * eta**2 - 58.0 * t**2 * eta**2) +
      dlambda**7 * 1.0 / 5040 * n * Math.cos(latitude)**7 *
        (61.0 - 479.0 * t**2 + 179.0 * t**4 - t**6)
    )
    # check the distortion from meridian curvature
    lm = meridian_curvature(latitude, type)
    m = m_0 * (1.0 + y**2 / (2.0 * lm * n * m_0**2) +
               y**4 / (24.0 * lm**2 * n**2 * m_0**4))
    if (m - 1.0).abs > THRESHOLD_POINT_SCALE_FACTOR
      STDERR.puts "point scale factor: (#{m}) is larger than 1 / 10,000."
      STDERR.puts "please check your inputs!"
      return {x: nil, y: nil, m: m}
    end
    {x: x, y: y, m: m}
  end

  module_function :gtoc, :semi_major_axis, :to_radian, :eccentricity,
    :second_eccentricity, :flattening, :prime_vertical_curvature,
    :meridian_arc, :meridian_curvature
end
