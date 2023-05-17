package com.yoshio3;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoshio3.request.Message;
import com.yoshio3.request.OpenAIMessages;
import com.yoshio3.response.ChatCompletionChunk;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;

@Controller
public class SSEOpenAIController {
    // Azure OpenAI のインスタンスの URL
    private final static String OPENAI_URL = "https://openai-yosshi.openai.azure.com";
    // 使用するモデルのパス (GPT-4 のチャットモデル)
    private final static String MODEL_URI_PATH = "/openai/deployments/gpt-4/chat/completions?api-version=2023-03-15-preview";
    // Azure OpenAI の API キー
    private final static String OPENAI_API_KEY = "***********************";
    // ユーザーごとの Sinks を保持する Map
    private final Map<UUID, Sinks.Many<String>> userSinks;

    // WebClient の生成
    private final WebClient webClient = WebClient.builder()
            .baseUrl(OPENAI_URL)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
            .defaultHeader("api-key", OPENAI_API_KEY)
            .build();

    // コンストラクタ
    public SSEOpenAIController() {
        this.userSinks = new HashMap<>();
    }

    // index.html を返す
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // SSE Client からのリクエストを受け取り、Azure OpenAI に送信する
    // OpenAI からの返却値を SSE Client に送信する
    @PostMapping("/openai-gpt4-sse-submit")
    @ResponseBody
    public void openaiGpt4Sse(@RequestBody String inputText, @RequestParam UUID userId) {
        System.out.println(inputText);

        OpenAIMessages messages = createMessages(inputText);
        Sinks.Many<String> userSink = getUserSink(userId);
        webClient.post()
                .uri(MODEL_URI_PATH)
                .body(BodyInserters.fromValue(messages))
                .retrieve()
                .bodyToFlux(String.class)
                .subscribe(data -> {
                    // GPT-4 からの返却値（チャンクデータ）をコンソールに出力
                    System.out.println(data);

                    // チャンクデータを送受信するため、15ミリ秒待機
                    // これを入れておかないと、SSE Client に対してチャンクデータが送信されない
                    // Overflow を引き起こす
                    try {
                        TimeUnit.MILLISECONDS.sleep(15);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (data.contains("[DONE]")) {
                        // userSink.tryEmitNext("[DONE]");
                    } else {
                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            // GPT-4 の返却値の JSON データを ChatCompletionChunk クラスに変換
                            ChatCompletionChunk inputData = objectMapper.readValue(data,
                                    ChatCompletionChunk.class);
                            if (inputData.getChoices().get(0).getFinish_reason() != null &&
                                    inputData.getChoices().get(0).getFinish_reason().equals("stop")) {
                                // userSink.tryEmitNext("");
                            } else {
                                // GPT-4 の返却値の内、返却する文字列の部分のみ取得
                                String returnValue = inputData.getChoices().get(0).getDelta().getContent();
                                if (returnValue != null) {
                                    // Server Sent Event で SSE Client に対してチャンクデータを送信
                                    EmitResult result = userSink.tryEmitNext(returnValue);
                                    if (result.isFailure()) {
                                        System.err.println("Failure: " + returnValue + " " + data);
                                        if (result == EmitResult.FAIL_OVERFLOW) {
                                            System.err.println("Overflow: " + returnValue + " " + data);
                                        } else if (result == EmitResult.FAIL_NON_SERIALIZED) {
                                            System.err.println("Non-serialized: " + returnValue + " " + data);
                                        } else if (result == EmitResult.FAIL_ZERO_SUBSCRIBER) {
                                            System.err.println("Zero subscriber: " + returnValue + " " + data);
                                        } else if (result == EmitResult.FAIL_TERMINATED) {
                                            System.err.println("Terminated: " + returnValue + " " + data);
                                        } else if (result == EmitResult.FAIL_CANCELLED) {
                                            System.err.println("Cancelled: " + returnValue + " " + data);
                                        }
                                    }
                                } else {
                                    userSink.tryEmitNext("");
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    // SSE Client からのリクエストを受け取り、ユーザーごとの Sinks を生成する
    @GetMapping(path = "/openai-gpt4-sse-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<String> sseStream(@RequestParam UUID userId) {
        Sinks.Many<String> userSink = getUserSink(userId);
        if (userSink == null) {
            userSink = createUserSink(userId);
        }
        return userSink.asFlux().delayElements(Duration.ofMillis(10));
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
        message.setContent("私は Microsoft のエンジニアです。Microsoft 製品やサービス以外の質問には答えることができません。");
        Message message2 = new Message();
        message2.setRole("user");
        message2.setContent(inputText);
        messages.add(message);
        messages.add(message2);

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
        return oaimessage;
    }
}
