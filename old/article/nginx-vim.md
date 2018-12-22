---
title: nginx-vim-syntaxを導入した
author: sumito3478
createdAt: 2015-11-22
date: 2015-11-22
lang: ja
tags: [programming, vim, nginx]
---

2015/11/22、このサイトを https://sld.u8.nu/ という新しいドメイン+httpsに移行した。
その時にNginxの設定ファイルをいじっていたのだが、Vimでハイライト・インデントがされないのが
流石に不便になってきたので、nginx-vim-syntaxというプラグインを導入することにした。

[nginx-vim-syntax](https://github.com/evanmiller/nginx-vim-syntax)

導入後、`vim /etc/nginx/nginx.conf`してハイライトされることを確認してよろこぶ。
そして`sudoedit /etc/nginx/nginx.conf`したのだが、今度はハイライトされなかった。

`sudoedit`は、`sudo -e`と同じである。
`sudo -e`は、編集対象のファイルのコピーを一時ファイルとして作成する。
sudoを起動したユーザでエディタを起動し、それで一時ファイルを編集し、
変種が終わったら一時ファイルからオリジナルのファイルに内容を書き戻す。
sudoでエディタを起動するよりは、sudoeditでエディタを起動するほうが良いだろう。

nginx.confがハイライトされなかったのは、sudoがつくった一時ファイルのファイル名が`nginx<ランダムな文字列>.conf`になるからだ。
nginx-vim-syntaxのソースのftdetect/nginx.vimは次のようになっている。

```vim
au BufRead,BufNewFile *.nginx set ft=nginx
au BufRead,BufNewFile */etc/nginx/* set ft=nginx
au BufRead,BufNewFile */usr/local/nginx/conf/* set ft=nginx
au BufRead,BufNewFile nginx.conf set ft=nginx
```

これでは`nginx<ランダムな文字列>.conf`という名前のファイルの種別はnginxに判定されない。
そこでさしあたって次のような行を.vimrcに追加した。

```vim
au BufRead,BufNewFile nginx*.conf set ft=nginx
```

これでsudoがつくった一時ファイルをnginxの設定ファイルとvimが認識するようになった。
