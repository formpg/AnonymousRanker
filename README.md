# AnonymousRanker

仲間内で匿名投票を行い、ランキングをルーレット演出で発表できるサイトです。

## 主な機能

- 会（タイトル・開催日・メンバー・パスワード）の作成
- 会の中で複数のお題を作成
- お題ごとに匿名で投票（1本の投票用URLを共有、パスワード不要）
- 投票ルールを会ごとに設定（持ち票数、自己投票の可否、上位何人まで発表するか、投票数の公開有無、発表順、発表ペース）
- パスワードで保護された管理画面から、ルーレット演出付きでランキングを発表

## 必要なもの

- JDK 21以上（[Eclipse Temurin](https://adoptium.net/) など）
- Mavenのインストールは不要です（Maven Wrapperを同梱しています）

## ローカルでの実行

```powershell
.\mvnw.cmd spring-boot:run
```

起動後、ブラウザで `http://localhost:8080` にアクセスしてください。
データはプロジェクト直下の `data/anonranker.mv.db`（H2ファイルDB）に保存されます。

## テストの実行

```powershell
.\mvnw.cmd test
```

## ビルド

```powershell
.\mvnw.cmd clean package
```

`target/anonymous-ranker-0.0.1-SNAPSHOT.jar` が生成されます。

## Dockerでの実行

```powershell
docker build -t anonymous-ranker .
docker run -p 8080:8080 anonymous-ranker
```

## デプロイについて

GitHub PagesはHTML/CSS/JSなどの静的ファイルしかホストできず、Javaのサーバーサイド処理は実行できません。
そのためこのリポジトリは **コードの管理・バージョン管理用にGitHub** を使い、**実際にアプリを動かすのは別のJVM対応ホスティングサービス**（例: [Render](https://render.com/)、[Railway](https://railway.app/)、[Fly.io](https://fly.io/) など）にデプロイする想定です。

デプロイ時は以下の環境変数を設定してください（`prod`プロファイルを使用）。

| 環境変数 | 説明 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` を指定 |
| `DATABASE_URL` | PostgreSQLの接続URL（例: `jdbc:postgresql://host:5432/dbname`） |
| `DATABASE_USERNAME` | DBユーザー名 |
| `DATABASE_PASSWORD` | DBパスワード |
| `PORT` | ホスティングサービスが指定するポート（多くの場合自動設定） |

多くのホスティングサービスは、リポジトリ内の `Dockerfile` を検出して自動的にビルド・デプロイできます。

## 匿名性について

投票記録（誰が誰に投票したか）はデータベースには保存されますが、二重投票防止のためだけに使われ、画面・API・エクスポートのいずれからも参照できません。ランキング発表では集計後の「候補者名と得票数」のみが使われます。
