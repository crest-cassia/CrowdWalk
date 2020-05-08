#  -*- coding: utf-8 -*-
=begin rdoc
:markup: markdown
:title: CrowdWalk Ruby Facilities

# Ruby Facilities for CrowdWalk

* Author:: Itsuki Noda <noda50@gmail.com>
* Copyright:: Copyright (c) 2020 AIST & I.Noda
* License::   Distributes under the same terms as CrowdWalk
* Version:: 0.0 2020/05/08 I.Noda

#### History
* [2020/05/08] Create This File.

## Overview

CrowdWalk は、
シミュレーションの途中でその動作を変更・追加することを、
Ruby のプログラムを追加することで可能にしている。

Ruby Facility を有効にするには、
property 設定ファイルにおいて、

```
  "use_ruby": true,
  "ruby_load_path": [ 
    <RubyLibPath>, 
    <RubyLibPath>, 
    ...],
  "ruby_init_script": [ 
    <RubyScript>, 
    <RubyScript>, 
    ...],
```
とする必要がある。

この中で、`<RubyLibPath>` は、Ruby のライブラリへの path を示すライブラリで、
Ruby の `$LOAD_PATH` に順に push される。
また、`<RubyScript>` は、
ruby 実行系の準備が整った後に実行される ruby の script であり、
順に実行される。
この実行は、上記の `$LOAD_PATH` の設定が終わった後に行われる。
これらの script は主として、
Ruby のプログラムのロード (`require`) など、
Ruby の実行環境の初期化を想定している。

これらの準備が整ったところでCrowdWalk のシミュレーションが開始されると、
以下に上げる Ruby のクラスのインスタンスが、
対応する CrowdWalk の Java のクラスの wrapper として呼び出される。
wrapper となるクラスの指定及び呼び出される method については、
各クラスの説明参照のこと。

## Ruby くらす

* class CrowdWalkWrapper :
  シミュレーションのメインルーチンへの割り込みを提供。
* class AgentFactoryBase :
  Agent の生成ルールを Ruby で記述する機能を提供。
* class RubyAgentBase :
  RationalAgent の挙動を Ruby で記述できる機能を提供。
* class RubyEventBase :
  シナリオの Event の1種として、Eventによる状態変化を Ruby で記述できる機能を提供。
* class RubyGateBase :
  シナリオの Event で制御する Gate の動作の詳細を Ruby で記述できる機能を提供。
* module CwIrb :
  CrowdWalk のシミュレーションに、irb を使って対話的に割り込める機能を提供。
* class NetworkMap :
  Ruby script の中からネットワークの各データを参照・操作する機能を提供。
* class ItkUtility :
  Java との各種やり取りを簡易にするための機能を提供。
* class ItkTerm :
  Java の `Itk::Term` クラスのインスタンスへのアクセスを用意にする機能を提供。
=end

module Top_RubyForCrowdWalk
end
