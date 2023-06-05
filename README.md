# 仮想電子商取引 : ACME Fitness Web お問い合わせページ

## プロジェクトのディレクトリ構成

このプロジェクトは下記の内容が含まれています。

```text
.
├── HELP.md
├── README.md
├── image
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── yoshio3
    │   │           ├── AppMain.java
    │   │           ├── SSEOpenAIController.java
    │   │           ├── request
    │   │           │   ├── Message.java
    │   │           │   └── OpenAIMessages.java
    │   │           └── response
    │   │               ├── ChatCompletionChunk.java
    │   │               ├── Choice.java
    │   │               └── Delta.java
    │   └── resources
    │       ├── META-INF
    │       ├── application.properties
    │       ├── static
    │       └── templates
    │           └── index.html
    └── test
        └── java
            └── com
                └── yoshio3
```

## 必要なツール

このアプリケーションをお試して動かすためには、下記のコマンドやツールが必要です。

1. Java 17
2. Maven 3.6.3
3. Azure Account

## 環境設定

`src/main/resources/application.properties` ファイルに下記の内容が記載されています。  
下記の OPENAI のインスタンス名と接続キーを編集してください。

```text
azure.openai.url=https://YOUR_OWN_OPENAI.openai.azure.com
azure.openai.model.path=/openai/deployments/gpt-4/chat/completions?api-version=2023-03-15-preview
azure.openai.api.key=********************

logging.group.mycustomgroup=com.yoshio3
logging.level.mycustomgroup=INFO
logging.level.root=INFO
```

## アプリケーションの実行

```bash
mvn spring-boot:run
```

実行すると下記のメッセージが出力されます。

```text
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.1.0)

2023-06-05T16:52:27.596+09:00  INFO 4081 --- [           main] com.yoshio3.AppMain                      : Starting AppMain using Java 17.0.6 with PID 4081 (/Users/teradayoshio/JavaOnAzureDay2023/Chat-GPT-4-sample/target/classes started by teradayoshio in /Users/teradayoshio/JavaOnAzureDay2023/Chat-GPT-4-sample)
2023-06-05T16:52:27.598+09:00  INFO 4081 --- [           main] com.yoshio3.AppMain                      : No active profile set, falling back to 1 default profile: "default"
2023-06-05T16:52:28.648+09:00  INFO 4081 --- [           main] o.s.b.web.embedded.netty.NettyWebServer  : Netty started on port 8080
2023-06-05T16:52:28.655+09:00  INFO 4081 --- [           main] com.yoshio3.AppMain                      : Started AppMain in 1.287 seconds (process running for 1.53)
```

### ブラウザから動作確認

`http://localhost:8080` にアクセスしてください。
すると下記のような画面が表示されます。

![OpenAI-SSE-Chat](https://live.staticflickr.com/65535/52952318155_79f600f97c_c.jpg=800x373)

ここで下記のような問い合わせを試してください。

```text
先日こちらで購入した、スマートウォッチ(注文番号 : 12345)が壊れていました。すぐに交換してください
最近、こちらで父の日にジョギングシューズを購入しましたが、父が履き心地が良く、運動しやすいと言っていました。ありがとうございます
新宿のおすすめのレストランを教えてください。
先日購入した商品が自宅に届いた際、置き配設定していないのに玄関の前においていかれました。高額商品で危ないので２度とやめてください
```
