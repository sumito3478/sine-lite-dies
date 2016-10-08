---
title: レビュー『Java パフォーマンス』
author: sumito3478
createdAt: 2015-06-22
date: 2015-06-28
lang: ja
tags: [programming, jdk]
---

JVMで動くコードを書く人全員に是非とも読んでもらいたい本。
ということは他の人が既によく言っているだろうから、今更僕が言うまでもあるまい。

- `premature optimization`（早すぎる最適化）という言葉の初出はクヌースらしい。初めて知った。
- 「マイクロベンチマークが役立つ場面は限られる」とはっきり言っている。
  もちろんマイクロベンチマークというものがしばしば誤解を招くことは知っていたが、
  役に立つことがほとんどないみたいなことを言っているのは初めて見た。もっと言ってやれ。
- 「メソベンチマーク」という言葉を初めて知った。実際のひとまとまり処理を行うが、
  完全なアプリケーションが使われるわけではないベンチマークらしい。
  たしかにマイクロベンチマークと呼ぶにもマクロベンチマークと呼ぶにもそぐわない気がする。
- JVMにおけるベンチマークでは、コードをウォームアップしてから計測する必要があるが、
  その理由としてJITコンパイルのためだけでなく、キャッシュをロードするためにも必要、とあった。
  気づかないうちに何らかのキャッシュのロードを計測してたということはあり得るかもね。
- レスポンスタイムの計測。
  

- 'premature optimazation' ってクヌースが最初に使った言葉だったんだ
- マイクロベンチマークが役立つ場面は限られる。
-


1.3.3
Donald Knuth premature optimization

2.1.1
マイクロベンチマークが役に立つ場面は限られる

2.1.2
マクロベンチーマーク　
完全なアプリケーションを測定すべし

2.1.3
メゾベンチマーク
実際の処理を行うけれども、完全なアプリケーションが使われるわけではないというもの

2.2
スループット、バッチ、レスポンスタイムの測定
2.2.1
ウォームアップはJITコンパイルのためだけでなく、キャッシュをロードするためにも必要
2.2.3
平均レスポンスタイムと90パーセンタイル値

2.3
t検定
Apache Commons Math: TTest
パフォーマンスリグレッションテスト
「標本がベースラインと異なっている確率は〜%であり、その差異は25%と見積もられる」
統計的な有意差は統計的な重要さとは異なる。

2.4
早期から頻繁にテストを行う

3.1
オペレーティングシステム付属のツールも使って分析すべし
vmstat
typeperf
iostat
nicstat

3.2 Javaの監視ツール
jcmd
jconsole
jhat
jmap
jinfo
jstack
jstat
jvisualvm

3.3.4 ネイティブなプロファイラ
Oracle Solaris Studio（Solarisとあるが、Linuxでも使える

3.4 Java Mission Control
商用版のJavaに付属

3.4.1 JFR
Java Flight Recorder
JVM上で発生したイベントが履歴として記録される

4.4.1
コードキャッシュが尽きるとJITコンパイルが無効化される
-XX:ReservedCodeCacheSize=N
-XX:InitialCodeCacheSize=N

4.4.2
スタンダードコンパイルの閾値 -XX:CompileThreshold=N
OSR（on-stack replacement)コンパイル：一定回数以上繰り返されたループをコンパイル
JVMがセーフポイントに到達するたびにメソッド実行回数のカウンタが減らされるため、
「ホットスポット」とみなされないが「生ぬるい」メソッドがいつまでもコンパイルされない場合、閾値を変える必要がある

4.4.3
-XX:PrintCompilation

4.5.1
-XX:CICompilerCount=N 並列コンパイラの数を指定

4.5.2
-XX:MaxFreqInlineSize=N インライン化するメソッドのバイトコードのバイト数の上限
-XX:MaxInlineSize=N 無条件でインライン化するメソッドのバイト数の上限

4.5.3
-XX:DoEscapeAnalysis エスケープ解析

5.1.3.4 CMSとG1の選択
シンプルな場合（ヒープサイズが4GB以下の場合など）はCMSが高速

5.2.4
-XX:+UseParallelGCThreads=N

5.3
ガベージコレクション関連のツール
-XX:PrintGC
-XX:PrintGCDetails
-XX:PrintGCTimeStamps
-XX:PrintGCDateStamps
-XX:UseGCLogFileRotation
-XX:NumberOfGCLogFiles=N
-XX:GCLogFileSize=N
GC Histogram
jconsole
jstat -gcutil

6.1.1
GCTimeRatio GC時間がどのぐらいかかってよいか指定する
アプリケーションスレッドの割合 = 1 - 1 / (1 + GCTimeRatio)

6.2
CMSでconcurent mode failureが起きるとアプリケーションスレッドが長時間停止する

6.2.1.1　バックグラウンドのスレッドの時刻頻度を上げる
-XX:CMSInitiatingOccupancyFraction=N
-XX:UseCMSInitiatingOccupancyOnly old領域の使用率のみに基づいてバックグラウンドのスレッドの開始を判断する

6.2.1.2 バックグラウンドのスレッドを増やす
ConcGCThreads = (3 + ParallelGCThreads) / 4

6.3 G1GC

6.3.1.1 バックグラウンドのスレッドのチューニング
ConcGCThreads = (ParallelGCThreads + 2) / 4

6.3.1.2 G1の実行頻度を増減させる
-XX:InitiatingHeapCoccupancyPercednt=N ヒープ全体の使用率

7.1.1 ヒープヒストグラム
jcmd pid GC.class\_histogram
jmap -histo pid
jmap -histo:live pid

7.1.2 ヒープダンプ
jcmd pid GC.heap\_dump path
jmap -dump:live,file=path pid
\*.hprof
jhat
jvisualvm
mat

7.1.3.3
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=path
-XX:HeapDumpAfterFullGC
-XX:HeapDumpBeforeFullGC

7.1.3.4
ガベージコレクションに時間がかかりすぎてOOMが起きる条件
1. -XX:GCTimeLimit=Nを超えた
2. 解放されたメモリの量が-XX:GCHeapFreeLimit=Nを下回った
3. 連続5回 1. と 2. を満たした
4. -XX:+UseGCOverheadLimitがtrue（デフォルト）

7.3.2.3
jcmd pid GC.run\_finalization
jmap -finalizerinfo pid

8.1.4 NMT
-XX:NativeMemoryTracking=off, summary, detail
jcmd pid VM.native\_memory summary

8.1.4.1
jcmd pid VM.native\_memory baseline
jcmd pid VM.native\_memory summary.diff

8.2.2 OOPの圧縮
-XX:+UseCompressedOops

9.1 スレッドプールとThreadPoolExecutor
9.2 ForkJoinPool
9.3.3 false sharing
フィールドのキャッシュの競合
VTunes
@Contentedアノテーション

9.4.2 バイアスロック
デフォルト有効化
スレッドプールを使うアプリケーションでしばしばパフォーマンスを悪化させる
-XX:-UseBiasedLockingで無効化

9.5 スレッドとロックの監視
9.5.1 スレッドの可視化
jconsoleのThreadsパネル
9.5.2.1 ブロックされたスレッドとJFR
Java Mission ControlのHistogramパネル





