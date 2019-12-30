---
title: "自作のML方言Orphosを支えつつある技術"
date: 2019-12-20T19:47:35+09:00
draft: false
tags: [
  "orphos",
  "mullos",
  "programming",
  "llvm",
  "gc"
]
summary: "言語実装 Advent Calendar 2019の20日目の記事。1年かけて知見が増えたので、去年の記事を半ばアップデートするように今回の記事を書く。ただし今回はOCamlである程度実装したものがある（まともに動くとは言っていない）"

---

この記事は、[言語実装 Advent Calendar 2019](https://qiita.com/advent-calendar/2019/lang_dev)の20日目の記事だ。

去年は[「自作言語 Orphos （おるぽす）の実装 Mullos （むっろす）を支える（予定の）技術を雑多に紹介」](https://sld.res.ac/post/orphos/mullos/advent-calendar-2018/)した。
1年かけて知見が増えたので、半ばアップデートするように今回の記事を書く。
本当は明確なトピックを対象に書きたいのだがまた雑多になってしまった…。

しかし今回は、字句解析・構文解析・elaboration[^elab-translation]まで、OCamlである程度[実装](https://github.com/orphos/orphos/tree/v1.0.0-m.3/mullos)[^orphos-license]したものがある（まともに動くとは言っていない）。
しばらく実装が中断していたが再開するので、 この記事は年内は実装や知見の進みに応じて更新するかもしれない[^git-history]。
年が変わったら（もはや2019ではないので）別記事にしていく。

[^elab-translation]: elaborationの定訳ってあるのかな。

[^orphos-license]: ライセンスとしてLGPLv3-or-laterと書いてあるがApache-2.0 WITH LLVM-exceptionに変更するつもり。

[^git-history]: 履歴は https://github.com/sumito3478/sine-lite-dies/commits/bleeding/content/post/orphos/mullos/advent-calendar-2019.md にある。

# 字句解析
字句解析には[Sedlex](https://github.com/ocaml-community/sedlex)を使っている。

## セミコロン自動挿入
前提として、Orphosの文法は、式（`let ... = ...; ...`とか`{ f (); g () }`）または宣言（`type ... = ... ; val ... = ...; let ... = ...` など）の区切りにセミコロンを使うよう定義している。
他の場所では使っていない。

Scalaなどセミコロンの省略が許される言語に慣れてしまったので、Orphos言語でもセミコロンを省略したい！
そこでScalaを参考にセミコロンの自動挿入ルールを定めた。

字句解析器だけで自動挿入が完結するので比較的簡単（lexer hackってやつ？）。

具体的なルールは、

- セミコロン自動挿入が有効・無効な領域を定める。領域はネストする。
  + 一番外側は有効。
  + (と)の間、caseと=>の間は無効。
  + {と}の間は有効。
- セミコロン自動挿入が有効な領域では、式または宣言の最後にありえる種類の字句と、最初にありえる種類の字句の間に改行（のみ）がある場合、セミコロンを挿入する。
  具体的には以下の通り（この一覧は当然Scalaとは異なっている）。
  + 最後にありえる字句
    ```
    end ] } ++ -- IDENTIFIER ) TEXT NUMBER BOOL
    ```
  + 最初にありえる字句
    ```
    if match fn let [ # { ` + - ! & * ++ -- raise
    lazy ~ IDENTIFIER ( TEXT NUMBER BOOL type val
    exception
    ```
たとえば
```
let f () = {
    a b
    c
    d
}
```
は
```
let f () = { a b; c; d }
```
だが
```
let f () = {
    (a b
      c
      d)
}
```
は
```
let f () = { a b c d }
```
になる。

今後Orphosのコードを書いていって、明らかに挿入してほしくないところで挿入される、みたいなことがしばしば起きるみたいなことになったら、文法を変えて対応するかもしれない。

# 構文解析
構文解析には[Menhir](http://gallium.inria.fr/~fpottier/menhir/)を使っている。

## %parameter
Menhirでは冒頭に`%parameter<S: Sig>`みたいに書くとパーサがファンクタになる。

そこで、構文木に任意のミュータブルな追加データをつけられるようにして（elabortionで使うのだ）、その追加データの型などをまとめたモジュールを引数として渡すようにした。

これで、パーサをelaboration以外のところから使用するときに、elaboration専用のへんてこなフィールドがついてくるみたいなことがなくなるってわけ。

## %inline
Menhirではルールに`%inline`をつけるとルールが呼び出し元へインライン化される。
これにより`%inline`を付けない場合に生じるはずだった衝突を回避できたりする。

## parameterized rule
Menhirのルールはパラメータを持てる。
このパラメータは文法のコンパイル時に展開されるので展開後の文法は依然としてLR(1)である。

## 演算子
演算子とその優先順位はCに似せている。
Cにはない演算子として`|>`（パイプライン）、`:+:`（結合）、`:-:`（削除）、`:+`（append）、`+:`（prepend）、`:-`（要素の削除）、`-:`（先頭から要素を削除）など。

OCamlのようにビット論理和を`lor`とするのではなく、Cのように`|`にしてしまったので、
マッチルールの先頭を`|`ではなく`case`にした（でないと次のルールの開始なのかビット論理和なのか区別がつかない）。
```orphos
match xs with
case Sugoi | Tuyoi => true
case Yowai => false
end
```
見た目は`|`が好みなのだが…。

## if-without-else
```orphos
if cond then
  raise_some_effect ()
end
```
曖昧な文脈自由文法の例としてよく挙げられるif-without-elseをendを書かせることで解決。end end end end...

if-without-elseで生じた重大な脆弱性もあったし（あとで出典を書く）もうみんなendを書きまくれば良いと思う。

# 単相化
MLtonがやっているように、型引数ごとに単相な実体をつくって多相letを消してしまおう（未実装）。

単相化前
```orphos
let id x = x

id 1 // => 1

id "one" // => "one"
```

単相化後
```orphos
let id$i64 (x: i64) = x

id$i64 1 // => 1

let id$string (x: string) = x

id$string "one" // => "one"
```

単相化は呼び出し側のモジュールで行うので複数のモジュールから同じ関数を同じ型引数で呼び出すとそれぞれのモジュールに同じ実体がつくられる。
LLVMバックエンドの場合、関数に`linkonce_odr`をつければリンカが同じ名前の関数をまとめてくれる（と思う）。

## コードサイズ
去年は「（単相かという戦略を選ぶことによる）コンパイル時間やコードサイズは長期的な課題とする」と書いたが、
少なくともMLtonの場合、単相化によりコードが単純化され、かえってコードサイズは小さいらしい[^mlton-codesize]。

[^mlton-codesize]: http://mlton.org/Performance#CodeSize , https://news.ycombinator.com/item?id=7965709

## オーバーローディングのある言語へのFFI
将来C++やJVM言語や.NET言語へのFFIを実装する場合、オーバーロードされた関数をどうやって呼ぶかという問題がある。OCaml-Java等では引数の型を明示しなければならない。しかしできればある程度は推論して、自動で選択ができない場合にのみ型を書くようにしたい。

そこで型推論の段階では`'a -> 'a`（あるいは、たとえば、返り値の型がすべてのオーバーロードでstringなら、`a -> string`）であるかのように推論して、単相化後、実際のオーバーロードに、推論された型に適合するものがあるか別途チェックしようかと思う。FFI限定ならこれで充分だろうというもくろみ。

# LLVMについて

## GC

### スタックの走査・リロケーション
正確なスタック走査・リロケーションのため、LLVMのGC機構を使う。
シャドウスタックとStatepointのふたつのGC機構が候補になる。

#### ShadowStack
スタックに配置したポインタ（へのポインタ）のリストを管理する。
スタックを走査するときはリストをたどれば良いし、リロケーション時にはスタック上のポインタを更新すれば良い。スタックへの直接アクセスが不要なので移植性が高そうだ（たとえばWebAssembly MVPってスタックには直接アクセスできないよね？）。

LLVMには[シャドウスタックを実現するためのサポートがある](https://llvm.org/docs/GarbageCollection.html#the-shadow-stack-gc)ので、
これを使おうと思っていたが、
- データ構造をスレッドローカルにする
- 末尾呼び出し対応（tailまたはmusttailのついたcallの前にスタックを破棄する処理を行う）

は、変換を行うパスの[ShadowStackGCLowering.cpp](https://github.com/llvm/llvm-project/blob/llvmorg-9.0.1/llvm/lib/CodeGen/ShadowStackGCLowering.cpp)自体を変更せずにできない（たぶん）ことがわかったので、ShadowStackGCLowering.cppを参考にパスを自作する予定。
また、LLVMのパスにする利点が薄いとわかった場合は、Orphosコンパイラが生成するLLVM IRを最初からシャドウスタック対応にするつもり（パスになっていれば最適化の途中で変換ができるので意味はあるかもしれないが、なければコンパイラ側でやってしまおうということ）。

#### スタックマップ
[LLVMにはスタックマップを作るための機能](https://llvm.org/docs/Statepoints.html#stack-map-format)もある。
AzulのなプロプライエタリなJVMのLLVMベースの高性能JITコンパイラFalconで使われているらしいので、速いのだろう。

使い方を調べるの大変だなと思ってたが、LLVMのスタックマップを扱いやすくするためのライブラリ[llvm-statepoint-utils](https://github.com/kavon/llvm-statepoint-utils)の実装やテストコードを読めばわかりやすかろう、ということに気づいたので使うつもり。

### 参照カウント + 循環コレクタ
場合によっては参照カウントに、循環コレクタ（もしくはbackup tracer）を組み合わせたGCが使えるかもしれない。

参照カウントGCはトレーシングGCに性能で負けているように見えるが、
Immix GCなどの効率の良いメモリレイアウトを使えば差が大きく縮まるとの
研究があり[^refcnt-paper]、Jikes RVMというJVM向けのGCの実装（Jikes RVMではJava言語でJVMがかけるのだ）が[RCImmix](https://github.com/rifatshahriyar/rcimmixpatch)の名で公開されている。

[^refcnt-paper]: https://www.researchgate.net/publication/254006302_Down_for_the_Count_Getting_reference_counting_back_in_the_ring

また、GCの研究にはJVMを対象とするものが多く[要出典]、ミューテータがマルチスレッドで動くことを前提としているわけだが、Orphosではヒープをファイバーごとに分けるので参照カウントの更新にアトミック命令が必要ない。

また、参照カウントGCには、イミュータブルなオブジェクトの一部をコピーオンライトの要領で更新するときに、オブジェクトの参照カウントが直後に0になることを確認できれば、もとのオブジェクトを破壊的に更新できるという利点がある。

以上から参照カウントGCが有利になりうる状況は充分ありえると思う。

# 末尾呼び出し
すべてのバックエンドに末尾呼び出しのネイティブ対応を求めるのは厳しいので
末尾呼び出しできないターゲットでなんとか末尾呼び出ししたい。

呼び出しが直接呼び出しなら、依存関係にある関数を一つにまとめて末尾呼び出しをgotoに変換できる。
しかし間接呼び出しの場合gotoにはできないし、computed gotoか数値を割り当ててswitchかをするとしても、間接末尾呼び出しされうる型の合う関数すべてをまとめないといけない。

実装したいバックエンドとして
- LLVMとX64/AArch64/WebAssembly MVP/その他 の組み合わせ
- JVM
- .NET
- ECMAScript

がある。このうちLLVM/X64/AArch64ではtailをつければ末尾呼び出しできる。

.NETでも[Monoの制限](https://github.com/mono/mono/issues/6914#issuecomment-475037714)に気をつければtailプレフィクスをつけることで末尾呼び出しできる。

WebAssembly MVPでは末尾呼び出しができない。WebAssemblyで末尾再帰するための命令return_call(_indirect)のサポートがLLVMに入っているようだが[^llvm-wasm-tail]、WebAssembly MVPでは使えないので処理系のサポートを待つか非ネイティブ（？）な解決策をとる必要がある。例外がネイティブサポートされておらず遅いので例外を使う解決方法も遅そうなのがつらい。

[^llvm-wasm-tail]: https://github.com/llvm/llvm-project/blob/llvmorg-9.0.1/llvm/lib/Target/WebAssembly/WebAssemblyISelLowering.cpp#L791

JVMは[Project Loom](https://wiki.openjdk.java.net/display/loom/Main)が使えるようになれば末尾呼び出しができるようになる。それどころか限定継続さえできるようになるらしい。[JDK13にProject Loomを見据えたSocket APIの新実装が追加されている](https://bugs.openjdk.java.net/browse/JDK-8218559)ので本気度は高そうだ。
が、Project Loomが本家に入ってその入ったJDKがLTSになって…となるとだいぶ先かもしれない。ぼくはJVMをよく使っているのでそれまでOrphos言語が使えないのはつらい。あとAndroid。

ECMAScriptには末尾呼び出しがあるがJavaScriptCore以外のメジャーな処理系で無視されている。標準準拠する気配がない（ぼくが知らないだけかもしれないが）。WebAssemblyで事が足りるならいらないかもしれない。

ネイティブに末尾呼び出しできない環境での実装で有望視しているのには2案ある。

1. [First-Class Continuations on the Java Virtual Machine: An Implementation within the Kawa Scheme Compiler](https://andrebask.github.io/thesis/)を参考に、例外でスタックを巻き戻しながらcatch節で必要なものをヒープにコピーする方式で継続を実装する。あとはコールスタックが深くなったら継続をキャプチャして、その継続をそのまま呼ぶ。この方式は非末尾再帰もヒープに余裕がある限り実行できる。
2. 最初は中間言語をインタプリタで実行し、よく実行される部分をターゲットのネイティブなコード（たとえばJVMならJavaバイトコード）の巨大なwhileループにtracing jitの要領でコンパイルする。call-returnとかなしにひたすらループし続けるコードになるので、末尾呼び出しどころかコールスタック自体がない。当然スタックは枯渇しない。

# 呼び出し規約
去年はcc 11（HiPEの呼び出し規約）を使うと言っていたが、HiPE特有の
コードを関数プロローグに挿入するらしく使えない（コードは[この辺](https://github.com/llvm/llvm-project/blob/llvmorg-9.0.1/llvm/lib/Target/X86/X86FrameLowering.cpp#L2254)）。

もう、当面はfastccを使うことにする。
レジスタのcallee/caller savedを変えつつtailに対応するだけなら自前の呼び出し規約を追加するのも大変ではないと思うので、もしfastccで継続の性能などに不満があるようなら自分で呼び出し規約をつくる。