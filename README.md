# Fridge Helper

Fridge Helperは、Notionで管理している冷蔵庫の在庫情報をもとに、消費期限が近い食材の可視化と、現在の食材で作りやすい楽天レシピの推薦を行うSpring Bootアプリケーションです。

ひとり暮らしや少人数世帯で発生しやすい「食材の使い忘れ」「買い足し判断の迷い」「献立決めの負担」を軽減することを目的としています。

## 主な機能

- Notionデータベースから在庫中の食材を取得
- 食材名、使用期限、残日数をAPIレスポンスとして提供
- 使用期限が7日以内の食材をフロント画面で強調表示
- 冷蔵庫内の食材名を楽天レシピカテゴリへマッピング
- 楽天レシピから人気レシピを取得し、食材一致率と不足食材を算出
- 総合人気ランキングと高タンパク系ランキングを表示
- ThymeleafとTailwind CSSによるシンプルなWeb UI

## 技術スタック

- Java 21
- Spring Boot 4.0.6
- Spring Web MVC / WebFlux
- Thymeleaf
- Jackson
- Jsoup
- Maven
- JUnit / Spring Boot Test

## アプリケーション構成

```text
src/main/java/com/nagoya/fridge
  config      Notion、楽天レシピ関連の設定
  ingredient  冷蔵庫食材APIとNotionデータ変換
  notion      Notion APIクライアント

src/main/java/com/nagoya/recipe
  controller  Web画面ルーティング
  dto         レシピ表示用DTO
  mapping     食材名正規化、カテゴリマッピング
  rakuten     楽天レシピAPI設定、例外
  scraper     楽天レシピページのスクレイピング
  service     レシピ推薦、ランキング取得

src/main/resources/templates
  index.html  フロント画面
```

## セットアップ

### 1. 前提条件

- Java 21以上
- Maven Wrapperを利用できるシェル環境
- Notionインテグレーションと対象データベース
- 楽天レシピAPIのアプリID、アクセスキー

### 2. 環境変数

アプリケーションは、環境変数または`application.properties`から設定値を読み込みます。ローカル開発ではプロジェクトルートの`.env`も利用できます。

```bash
NOTION_API_KEY=secret_xxxxxxxxxxxxxxxxx
NOTION_DATABASE_ID=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
NOTION_BASE_URL=https://api.notion.com
NOTION_VERSION=2026-03-11
NOTION_INGREDIENT_NAME_PROPERTY=食材名
NOTION_EXPIRATION_DATE_PROPERTY=使用期限
NOTION_STOCK_STATUS_PROPERTY=在庫ステータス
NOTION_IN_STOCK_VALUE=在庫あり

RAKUTEN_RECIPE_APPLICATION_ID=xxxxxxxxxxxxxxxx
RAKUTEN_RECIPE_ACCESS_KEY=xxxxxxxxxxxxxxxx
RAKUTEN_RECIPE_BASE_URL=https://app.rakuten.co.jp
```

`NOTION_INGREDIENT_NAME_PROPERTY`を未設定にした場合は、Notionページ内のtitleプロパティを自動的に探索します。

### 3. 起動

```bash
./mvnw spring-boot:run
```

デフォルトのポートは`8081`です。

```text
http://localhost:8081/
```

### 4. テスト

```bash
./mvnw test
```

## APIエンドポイント

| Method | Path | 内容 |
| --- | --- | --- |
| GET | `/` | Web UI |
| GET | `/api/fridge/ingredients/raw` | Notionから取得した食材データのRawレスポンス |
| GET | `/api/fridge/ingredients/names` | 在庫中の食材名一覧 |
| GET | `/api/fridge/ingredients/items` | 食材名、使用期限、残日数を含む食材一覧 |
| GET | `/api/recipes/recommend` | 現在の食材に基づくおすすめレシピ |
| GET | `/api/recipes/ranking/general` | 楽天レシピの総合人気ランキング |
| GET | `/api/recipes/ranking/protein` | 高タンパク系カテゴリの人気ランキング |
| GET | `/api/recipes/test` | 楽天レシピスクレイピングの動作確認用 |

## レシピ推薦ロジック

1. Notionから在庫中の食材名を取得します。
2. 食材名を正規化し、楽天レシピカテゴリIDへマッピングします。
3. 対象カテゴリの人気レシピを取得します。
4. 各レシピ詳細ページから材料を取得します。
5. 冷蔵庫内の食材とレシピ材料を照合し、食材一致率と不足食材を算出します。
6. 一致率の高い順にレシピを返却します。

食材名の照合では、全角・半角、ひらがな・カタカナ、一部の漢字表記揺れを吸収し、近似一致も考慮します。

## Notionデータベースの想定

対象データベースには、少なくとも以下のプロパティを用意してください。

| 用途 | 既定プロパティ名 | 型 |
| --- | --- | --- |
| 食材名 | `NOTION_INGREDIENT_NAME_PROPERTY`で指定、またはtitle型プロパティ | title |
| 使用期限 | `使用期限` | date |
| 在庫ステータス | `在庫ステータス` | status |

在庫抽出条件は、`在庫ステータス`が`在庫あり`のページです。プロパティ名やステータス値は環境変数で変更できます。

## 注意事項

- Notion APIキー、楽天APIキーなどの認証情報はGitにコミットしないでください。
- 楽天レシピのランキング取得には、楽天側のAPI仕様や利用制限が影響します。
- レシピ詳細の材料取得にはHTMLスクレイピングを利用しているため、対象サイトの構造変更により調整が必要になる場合があります。
- 本アプリケーションはローカル開発・個人利用を主目的とした構成です。本番運用では認証、監視、レート制御、エラーハンドリングの追加を推奨します。
