---
title: "Pthread版Postgres"
date: 2018-12-26T19:51:56+09:00
draft: true
---

# プロセスではなくスレッドを立ち上げるPostgres

https://www.postgresql.org/message-id/9defcb14-a918-13fe-4b80-a0b02ff85527%40postgrespro.ru

- ほとんどのスタティック変数とグローバル変数をスレッドローカルに
- GUCの実装をスレッドローカルに
- forkの呼び出しをpthread_createに
- ファイルディスクリプタのキャッシュをグローバルに（スレッド間で共有される）

1. Add session_local (defined as __thread) to definition of most of 
static and global variables.
I leaved some variables pointed to shared memory as static. Also I have 
to changed initialization of some static variables,
because address of TLS variable can not be used in static initializers.
2. Change implementation of GUCs to make them thread specific.
3. Replace fork() with pthread_create
4. Rewrite file descriptor cache to be global (shared by all threads).

I have not changed all Postgres synchronization primitives and shared 
memory.
It took me about one week of work.

- シグナルハンドリング
- スレッド終了時にメモリとファイルを解放する
- postmasterとバックエンドの連携　autovacuum, bgwriter, checkpointer, stat collector

What is  not done yet:
1. Handling of signals (I expect that Win32 code can be somehow reused 
here).
2. Deallocation of memory and closing files on backend (thread) termination.
3. Interaction of postmaster and backends with PostgreSQL auxiliary 
processes (threads), such as autovacuum, bgwriter, checkpointer, stat 
collector,...gg


# 埋め込みデータベース(database as a library)化したい
サーバとクライアントの両方で同じSQL実装が使えて、
かつリッチなSQLの機能と、Serializable Snapshot Isolationを持つと良いという気持ちがあり、

MariaDBにはlibmysqldという埋め込みデータベースk
