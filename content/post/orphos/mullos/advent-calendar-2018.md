+++
title = "自作言語 Orphos の実装 Mullos を支える（予定の）言語実装技術（2018）"
date = 2018-12-23T14:23:10+09:00
draft = false
summary = "言語実装 Advent Calendar 2018の23日の記事。自作言語Orphosの実装Mullosを支える技術を紹介する。のだが、さっぱり実装が進んでいないので、支える予定の技術ということになる。"
tags = [
  "orphos",
  "mullos",
  "programming",
  "llvm",
  "gc"
]
+++

この記事は、[言語実装 Advent Calendar 2018](https://qiita.com/advent-calendar/2018/lang_dev)の23日の記事だ。

自作言語 Orphos （おるぽす）の実装 Mullos （むっろす）を支える（予定の）技術のうち、言語実装と関係の深いものを雑多に紹介する。
本当は紹介したうえで「こちらが実践例になります」とやりたかったのだがさっぱり実践できていない。まあ年末年始進めよう。

## 最適化とコード生成 {#optimization-and-code-generation}
最適化とコードジェネレータには[LLVM](https://llvm.org/)を使う。その理由は、

- CPUベンダがオープンソースのコンパイラフレームワークにアーキチェクチャ依存の最適化をコントリビュートする場合、GCCとLLVMを優先的に対象にするだろう。
  その恩恵を受けるためにはこれらをコードジェネレータとして使うのが望ましい。
  [libgccjit](https://gcc.gnu.org/onlinedocs/gcc-8.2.0/jit/index.html)は未だに'"Alpha" quality'らしいので避けるとして、LLVMを使うことになる。
- [GHCのLLVMバックエンド](https://ghc.haskell.org/trac/ghc/wiki/Commentary/Compiler/Backends/LLVM)や[HiPE](http://erlang.org/doc/man/HiPE_app.html)など末尾呼び出しが必要な処理系のバックエンドとして実績がある。
- WebAssemblyに対応している（たぶんmusttail/tailに対応してないと思うけれども）。
- [OMR](https://github.com/eclipse/omr)などのJIT前提のフレームワークと違って、LLVMはJITとAOTの両方で使われている。

### 呼び出し規約 {#calling-convention}
デフォルトの呼び出し規約として、cc 11（HiPEの呼び出し規約）を使う。

[LLVM Language Reference](https://llvm.org/docs/LangRef.html)に書かれている呼び出し規約のうち、
cc 10（GHCの呼び出し規約）とcc 11（HiPEの呼び出し規約）は末尾呼び出しをサポートしていて、かつCallee savedなレジスタがない。
cc 10は引数の数に制限があり、浮動小数点数を渡せないらしいので、cc 11の方を選ぶ。

Callee savedなレジスタがないことの利点の一つとして、例外の送出がスタックポインタをずらしてジャンプするだけですむ。
[OCamlもこの恩恵を受けているらしい](https://github.com/whitequark/ocaml-llvm-ng/blob/master/doc/abi.md#caller-save-registers)。

Orphosには[OCaml Multicore風のAlgebraic Effect Handler](https://github.com/ocamllabs/ocaml-effects-tutorial)を入れる。
Algebraic Effect Handlerの機構は再開可能な例外機構みたいなものなので、例外とか継続的なものの高速化は重要だ。
ワンショットの継続の実装の参考にするためにBoostのfcontext（アセンブラで実装された、ucontextに似たAPI）の
[実装](https://github.com/boostorg/context/blob/boost-1.69.0/src/asm/jump_x86_64_sysv_elf_gas.S)を眺めてみたのだが、
レジスタの保存・復元が行われていた。
ジャンプを実行する関数の呼び出し規約をcc 11にすれば、退避は呼び出し側でLLVMが行ってくれる。
呼び出し側で使われていないレジスタがあれば退避をスキップできる。

### 型パラメータの実装 {#type-parameter}
C++のテンプレートのように、LLVMのlinkonce_odrというLinkage Typeをつけた関数を型パラメータごとに生成する。
また、型クラスの実装も暗黙引数[^implicit-param]とかにせずに、静的に解決して直接呼び出しする。

[^implicit-param]: 実は[言語実装Advent Calendar 2016](https://qiita.com/advent-calendar/2016/lang_dev)の17日にImlicit Calculusという題名で予定を入れている（最初はたしかTruffleについて書くとしていて、その後変えた）のだが、未だに書いていない（すみません…）。Orphosでは暗黙引数自体は実装しないつもりだが、型クラスの理解にかかわるのでいずれ論文をちゃんと読んで記事を投稿するつもり。

性能もさることながら、いったん特殊化してしまえば、具象型だけ考えればよくなるのでコード生成の実装も簡単になるのではないかと思っている。
コンパイル時間やコードサイズは長期的な課題とする。

## GC {#gc}
デフォルトで正確なGCを行う。

保守的GCは[データ構造がGC-robustかどうか気にしないといけない](http://www.hpl.hp.com/techreports/2001/HPL-2001-251.pdf)。
GC-robustでないデータ構造をつくってしまうと、制限なしのリークが起きる可能性がある。
たとえば無限リストをキューとして使っていると、その途中のノードをにせポインタが指していてリークしたら、それから先の無限リストが無限にリークする！　
書き手に気をつけさせるのは望ましくないので、言語としてGC-robustでないデータ構造を作れないようにするべきだが、その気はない。
正確なGCをするしかない。

### スタックの走査・リロケーション {#stack-scan-and-relocation}
正確なスタック走査・リロケーションのため、LLVMのGC機構を使う。
シャドウスタックとSafepointsのふたつのGC機構が候補になる。

#### シャドウスタック {#shadow-stack}
スタックに配置したポインタ（へのポインタ）のリストを管理する。
スタックを走査するときはリストをたどれば良いし、リロケーション時にはスタック上のポインタを更新すれば良い。

LLVMには[シャドウスタックを実現するためのサポートがある](https://llvm.org/docs/GarbageCollection.html#the-shadow-stack-gc)。
対象にしたい関数に`gc "shadow-stack"`をつけておき、
イントリンシック関数`@llvm.gcroot`でスタック上のポインタへのポインタをマークする。
するとグローバル変数の`llvm_gc_root_chain`でスタック上のポインタへのポインタのリストが管理される。
マルチスレッド（Orphos言語にはファイバー的なものを入れたいので、マルチファイバー）に対応するためには
`llvm_gc_root_chain`をスレッドローカル変数かローカル変数にする必要がありそうなど、いろいろと手を入れる必要がありそうだが、いちから実装するよりは楽だろう。

LLVMのもうひとつのGCサポートであるStatepointsのドキュメントには、
[gcrootの機構には歴史的関心しか残されていないが、例外としてシャドウスタックの実装はサポートされるとある](https://llvm.org/docs/Statepoints.html#status)。

#### Statepoints {#state-points}
[スタックマップをつくるらしい](https://llvm.org/docs/Statepoints.html#stack-map-format)。
AzulのなプロプライエタリなJVMのLLVMベースの高性能JITコンパイラFalconで使われているらしいので、速いのだろう。

OCamlのGCもスタックマップを使っているらしいので[要出典]、OCamlと性能で競争するためにはこちらを使いたいところだが、
あまりサンプルコードが見つからないしドキュメントに
[There are a couple places where bugs might still linger](https://llvm.org/docs/Statepoints.html#status)とあるなど
面倒なので長期的目標とする。

### ヒープの分離 {#heap-separation}
ファイバーごとにヒープを分離する。
他のファイバーにメッセージを送るときにはコピーが行われる。

ファイバーが終了すればヒープごと解放できるので、短命なファイバーならGCなしで完了できるだろう（リージョンみたいなものだ）。
また、ファイバーごとにGCのアルゴリズムを変えられるようにできるので、パフォーマンスチューニングの余地が増えるという利点もある。

巨大なバイナリなどをいちいちコピーすると遅いので、
ポインタを含まない大きな（ページサイズ以上とか）オブジェクトは独立したヒープに確保して、
何個のファイバーから参照されているかの参照カウントを管理するようにする。

### Semispace copying GC {#semispace-copying-gc}
ファイバーごとにヒープを分離することにより、semispaceなコピーGCが充分実用的になるかもしれない。
GCが他のファイバーを止めないし、GC中のファイバーのぶんしかメモリ使用量が2倍にならない。

## 自動定理証明 {#automatic-theorem-proving}
### 型クラスの法則の検査 {#type-class-law-checking}
Orphos言語に実装したい型クラスの機能として、

- 型クラスの定義・インスタンスの定義・呼び出し。
- 継承（ある型クラスのインスタンスから別の型クラスのインスタンスへの変換を定義する）。
- 自動導出（インスタンスをマクロで生成する）。
- インスタンスが常に満たすべき法則(law)を記述する（C++0xに入らなかったaxiomみたいに）。
  コンパイラは可能なら反例を見つけて警告を発する。
 
がある。
4つ目のlawのチェックのために定理証明器の[Z3](https://github.com/Z3Prover/z3)を使う。
コンパイラはZ3を使ってlawの反例を探し、見つけたら警告を出す
（警告ではなくコンパイルエラーということにしてしまうと、互換性のため、時間のかかるチェックを打ち切るわけにはいかなくなるし、すべてのOrphos処理系でチェックの実装が必須になってしまう）。

Z3のC APIには証明に使う時間やリソースの制限を指定する機能があったはずなので（今ちょっと探したら見つからなかった。まあなんとかなるだろう）、その閾値をコンパイラオプションで指定するようにする。
CIでビルドするときだけ閾値を上げて高性能ハードウェアで殴るとか、安定版リリース前に閾値を上げて時間で殴るという運用が可能だろう。

### ふるい型(refined type) {#refined-type}
`Int > 0`みたく型に制約をつける（集合をふるいにかける）らしい。
これもZ3で殴って警告を出す機能にしたい。

[type-systems/refined-types](https://github.com/tomprimozic/type-systems/tree/master/refined_types)
:    ふるい型の検査をZ3などのSMTソルバで行う実装例。


## レベルベースの型推論 {#level-based-type-inference}
[sound_lazy.ml](http://okmij.org/ftp/ML/generalization/sound_lazy.ml)を参考にレベルベース型推論を実装する。

[How OCaml type checker works -- or what polymorphism and garbage collection have in common](http://okmij.org/ftp/ML/generalization.html)
:    前述のsound_lazy.mlが載っている記事。

[OCaml でも採用されているレベルベースの多相型型推論とは](https://rhysd.hatenablog.com/entry/2017/12/16/002048)
:    去年の言語実装Advent Calendarの記事。
     レベルベースの型推論が紹介されている。

## おわり {#conclusion}
ブログ復活させたばかりでコメントシステムをまだつけてないので、何かあったら[マストドンアカウント](https://mstdn.res.ac/@tomoaki3478)まで。
