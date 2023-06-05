package com.yoshio3;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.yoshio3.request.Message;
import com.yoshio3.request.OpenAIMessages;
import com.yoshio3.response.ChatCompletionChunk;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;

@Controller
@Component
public class SSEOpenAIController {
    
    private final Logger LOGGER = LoggerFactory.getLogger(SSEOpenAIController.class);

    // Azure OpenAI のインスタンスの URL
    @Value("${azure.openai.url}")
    private String OPENAI_URL;

    // 使用するモデル名
    @Value("${azure.openai.model.path}")
    private String MODEL_URI_PATH;

    // Azure OpenAI の API キー
    @Value("${azure.openai.api.key}")
    private String OPENAI_API_KEY;

    // 接続してきたクライアントの情報を保持する Map
    private static Map<UUID, Sinks.Many<String>> userSinks;

    // Azure OpenAI に接続する Client
    private WebClient webClient;

    // 静的イニシャライザ
    static {
        userSinks = new ConcurrentHashMap<>();
    }

    // Client の初期化
    @PostConstruct
    private void init() {
        // WebClient の生成
        webClient = WebClient.builder().baseUrl(OPENAI_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .defaultHeader("api-key", OPENAI_API_KEY).build();
    }

    // index.html を返す
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // ページにアクセスした際に、クライアント毎に UUID を作成 (1 対 1 で送受信を行う場合のため)
    // チャットのように (1対多）で同じ内容を更新したい場合は、この部分の処理は不要
    @GetMapping(path = "/openai-gpt4-sse-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<String> sseStream(@RequestParam UUID userId) {
        Sinks.Many<String> userSink = getUserSink(userId);
        if (userSink == null) {
            userSink = createUserSink(userId);
        }
        LOGGER.debug("USER ID IS ADDED: {}}", userId);
        return userSink.asFlux().delayElements(Duration.ofMillis(10));
    }

    // SSE Client からのリクエストを受け取り、Azure OpenAI に送信する
    // OpenAI からの返却値を SSE Client に送信する
    @PostMapping("/openai-gpt4-sse-submit")
    @ResponseBody
    public void openaiGpt4Sse(@RequestBody String inputText, @RequestParam UUID userId) {
        LOGGER.debug(inputText);
        OpenAIMessages messages = createMessages(inputText);

        Sinks.Many<String> userSink = getUserSink(userId);
        webClient.post().uri(MODEL_URI_PATH).body(BodyInserters.fromValue(messages)).retrieve()
                .bodyToFlux(String.class).subscribe(data -> {
                    // チャンクデータを送受信するため、数十ミリ秒待機
                    // これを入れておかないと、SSE Client に対してチャンクデータが送信されず
                    // Overflow を引き起こす
                    sleepPreventFromOverflow();

                    if (data.contains("[DONE]")) {
                        LOGGER.debug("DONE");
                    } else {
                        try {
                            invokeOpenAIAndSendMessageToClient(userSink, data);
                        } catch (IOException e) {
                            LOGGER.error("Error: {}", e.getMessage());
                        }
                    }
                });
    }

    // 実際に OpenAI を呼び出し、SSE でデータを返すメソッド
    private void invokeOpenAIAndSendMessageToClient(Sinks.Many<String> userSink, String data) throws IOException{
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // GPT-4 の返却値の JSON データを ChatCompletionChunk クラスに変換
            ChatCompletionChunk inputData =
                    objectMapper.readValue(data, ChatCompletionChunk.class);
            if (inputData.getChoices().get(0).getFinish_reason() != null
                    && inputData.getChoices().get(0).getFinish_reason()
                            .equals("stop")) {
                // GPT-4 の返却値の内、返却する文字列の部分のみ取得
            } else {
                // GPT-4 の返却値の内、返却する文字列の部分のみ取得
                String returnValue =
                        inputData.getChoices().get(0).getDelta().getContent();
                if (returnValue != null) {
                    LOGGER.debug(returnValue);
                    // 空白文字対応 (HTML で空白文字を表示するため、特殊文字列に置換して送信)
                    returnValue =
                            returnValue.replace(" ", "<SPECIAL_WHITE_SPACEr>");
                    returnValue = returnValue.replace("\n", "<SPECIAL_LINE_SEPARATOR>");
                    LOGGER.debug(returnValue);
                    // Server Sent Event で SSE Client に対してチャンクデータを送信
                    EmitResult result = userSink.tryEmitNext(returnValue);
                    // SSE でメッセージを送信した際の、エラー発生時の理由を表示
                    showDetailErrorReasonForSSE(result, returnValue, data);
                } else {
                    userSink.tryEmitNext("");
                }
            }
        } catch (IOException e) {
            throw e;
        }
    }

    // SSE Client に対しての送信に失敗した際のエラー内容を確認するためのメソッド
    private void showDetailErrorReasonForSSE(EmitResult result, String returnValue, String data) {
        if (result.isFailure()) {
            LOGGER.error("Failure: {}", returnValue + " " + data);
            if (result == EmitResult.FAIL_OVERFLOW) {
                LOGGER.error("Overflow: {}", returnValue + " " + data);
            } else if (result == EmitResult.FAIL_NON_SERIALIZED) {
                LOGGER.error("Non-serialized: {}", returnValue + " " + data);
            } else if (result == EmitResult.FAIL_ZERO_SUBSCRIBER) {
                LOGGER.error("Zero subscriber: {}", returnValue + " " + data);
            } else if (result == EmitResult.FAIL_TERMINATED) {
                LOGGER.error("Terminated: {}", returnValue + " " + data);
            } else if (result == EmitResult.FAIL_CANCELLED) {
                LOGGER.error("Cancelled: {}", returnValue + " " + data);
            }
        }
    }

    // OpenAI から帰ってきたデータを一度に SSE に対して送信すると
    // バッファオーバフローになるため、数十ミリ秒待機する
    private void sleepPreventFromOverflow(){
        try {
            TimeUnit.MILLISECONDS.sleep(20);
        } catch (InterruptedException e) {
            LOGGER.warn("Thread Intrrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    // ユーザーごとの Sinks を生成する
    private Sinks.Many<String> createUserSink(UUID userId) {
        Sinks.Many<String> userSink = Sinks.many().multicast().directBestEffort();
        userSinks.put(userId, userSink);
        return userSink;
    }

    // ユーザーの Sinks を取得する
    private Sinks.Many<String> getUserSink(UUID userId) {
        return userSinks.get(userId);
    }

    // Azure OpenAI に送信する JSON 文字列の生成
    private OpenAIMessages createMessages(String inputText) {
        // 過去の履歴も残してチャットをしたい場合は
        // ここの部分の List を追加していく必要があり
        List<Message> messages = new ArrayList<>();
        Message message = new Message();
        message.setRole("system");
        message.setContent(SYSTEM_DEFINITION);
        Message message2 = new Message();
        message2.setRole("user");
        message2.setContent(USER1);
        Message message3 = new Message();
        message3.setRole("assistant");
        message3.setContent(ASSISTANT1);
        Message message4 = new Message();
        message4.setRole("user");
        message4.setContent(USER2);
        Message message5 = new Message();
        message5.setRole("assistant");
        message5.setContent(ASSISTANT2);

        Message message6 = new Message();
        message6.setRole("user");
        message6.setContent(inputText);

        messages.add(message);
        messages.add(message2);
        messages.add(message3);
        messages.add(message4);
        messages.add(message5);
        messages.add(message6);
        // Azure OpenAI に送信する JSON 文字列の生成用クラス
        OpenAIMessages oaimessage = new OpenAIMessages();
        oaimessage.setMessages(messages);
        oaimessage.setMax_tokens(3000);
        oaimessage.setTemperature(0.1);
        oaimessage.setFrequency_penalty(0);
        oaimessage.setPresence_penalty(0);
        oaimessage.setTop_p(0.95);
        oaimessage.setStop(null);
        oaimessage.setStream(true);

        Gson gson = new Gson();
        LOGGER.debug(gson.toJson("REQUESTED JSON: " + messages));

        return oaimessage;
    }

    private final static String SYSTEM_DEFINITION = """
            私は、ACME Fitness というオンライン・電子商取 Web サイトのサポート対応担当者です。
            オンライン、電子商取引で扱う商品以外の問い合わせには、お答えできません。
            """;
    private final static String USER1 =
            "こちらで購入した、スマートウォッチ(注文番号 : 12345)がまだ届きません。現在どのような状況か教えてください。";
    private final static String ASSISTANT1 = "状況を確認しますので、しばらくお待ちください";
    private final static String USER2 = "今日は何日ですか？";
    private final static String ASSISTANT2 = """
                申し訳ございませんが、私は ACME Fitness のオンラインサポート担当であり、
                お問い合わせ内容に関する情報は提供できません。
                お客様の購入商品やサービス、フィードバックに関するお問い合わせには喜んでお答えいたしますので、
                どうぞお気軽にお問い合わせください。
                どうぞよろしくお願いいたします。
            """;

}
