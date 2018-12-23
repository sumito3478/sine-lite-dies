---
title: "自作言語 Orphos の実装 Mullos を支える技術（2018）"
date: 2018-12-23T14:23:10+09:00
draft: false
---

この記事は、[言語実装 Advent Calendar 2018](https://qiita.com/advent-calendar/2018/lang_dev)の23日の記事。

自作言語 Orphos （おるぽす）の実装 Mullos （むっろす）を支える（予定の）技術を雑多に紹介する。

ちなみに、Mullosの実装は少ししか進んでいない。

# 実装処理系
OCamlで実装している。比較的慣れているのと、Orphosと言語仕様が似ている（ようになる予定な）ので、セルフホスティングしたくなったときに書き直しが容易だろうという理由。

構文解析にMenhir、字句解析にSedlexを使用している。

# ビルド環境

## Alpine
Alpine LinuxのDockerコンテナ上でビルドしている。

## CodeBuild
ビルドスクリプトはDockerfileとして書かれており、
ENTRYPOINTでテストを実行する。
CodeBuild上でdocker build→docker run
することでビルド→テストが実行される。

AWS CodeBuildを使うのは高速化のため（かねがないのにかねで殴っていくスタイル）。
でも最大構成でも8コア/16GiBなのであまり殴ってる感じにはなっていない。
AWS Batchとかで殴る方が良いのかもしれない。


# Mullos とは
Mullosは自作言語Orphosの最初の実装です。
