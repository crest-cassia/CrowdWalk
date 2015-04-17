#!/usr/local/rvm/rubies/default/bin/ruby --
# -*- coding: utf-8 -*-

require '../libs/gtoc'

#include 'Geographic'

# テスト
p Geographic::gtoc(36.1037747916666, 140.087855041666, 9)
#p gtoc(latitude: 36.10377479166666, longitude: 140.087855041666, number: 9, type: "world")

#p gtoc(latitude: 33.0, longitude: 129.5, number: 1, type: "world")

p Geographic::ctog(11543.688, 22916.244, 9)


