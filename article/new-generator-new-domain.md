---
title: このサイトの静的サイトジェネレータとドメインを刷新した
author: sumito3478
createdAt: 2015-06-15
date: 2015-06-15
lang: ja
tags: [programming, nodejs]
---

# 静的サイトジェネレータを刷新

このサイトは自作の静的サイトジェネレータで生成されている。もともとは Scala で書いたものを使っていたが、Node.js ベースに書き直した。
前のジェネレータは、暫定的実装ということで、[このサイト自体のレポジトリ](https://github.com/sumito3478/sine-lite-dies)にコードが入っていたが、
今度は[別レポジトリ](https://github.com/katastemattic/contenttic)に分離した。

ECMAScript 6 と、それに加えて ECMAScript 7 で提案されている async/await の機能を取り入れた言語で書かれている。
ECMAScript 6 は最終ドラフトの段階にあり、内容的にはリリースされたも同然だろう。
練習のため、ECMAScript 6/7 で書くことにしたのだ。
最終的には、開発中の自作の言語（未公開）から ECMAScript 6/7 へのコンパイラを書くための練習になる予定だ。
ECMAScript 6/7 から、現行の ECMAScript への変換には [Babel](https://github.com/babel/babel) を使っている。

Jade から HTML への変換には、今まで [Scalate](https://github.com/scalate/scalate) を使っていたが、[本家Jade](https://github.com/jadejs/jade)に移行した。
フィード（Atom）の生成にも使っている。もう XML を人間が読み書きするときには、全部 Jade のようなフォーマットで読み書きすれば良いのではないかと思った。
もしこういう構文が XML で採用されていたら、もっと XML は使われ続けていただろうに。

Markdown から HTML への変換には、[Pandoc](http://pandoc.org/) を使っていたが、[remarkable](https://github.com/jonschlinkert/remarkable) に移行した。

以前のジェネレータは異常に遅かったのだが、書き直しにより（はからずも）高速化された。
遅かった理由はよく調べていないが、たぶん Scalate で Jade のソースをコンパイルするのが遅かったようだ。
さらに、Pandoc を外部プロセスとして呼び出すときにハングしたようになっていた気がする。
プロセス API の使い方がまずかったのかもしれない。でも Node.js に移行したからもういいや。

Node.js で使えるライブラリやプログラミング言語は豊富で、JVM を使う理由は減ってきている。
かつて Pure Java の理想のために多くのライブラリが書きなおされ、多くの JVM 言語が実装された。
今度は、ブラウザで JavaScript しか動かないという理由で、同じことが JavaScript に起きている。

# ドメインを sineli.ttic.press に

最近 ttic.press というドメインを取得した。
たとえば sta.ttic.press のように、サブドメインを工夫すれば面白いドメインになるというもくろみだ。
このサイトも sineli.ttic.press に移行する。以前のドメインからはリダイレクトする。

TLD が .press なのは、最近出版（特に電子出版）に興味があり、出版会のようなものを始めたいという意気込みからきたものだ。
その意気込みが形になるかどうかは未定だが。

